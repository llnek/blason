/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package com.zotoh.dbio
package core

import org.apache.commons.lang3.{StringUtils=>STU}
import scala.collection.mutable
import java.lang.reflect.Method
import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._




object ClassMetaHolder {
  private val _assocs= mutable.HashMap[ String,AssocMetaHolder]()
  def getAssocMetas() = _assocs.toMap
}

/**
 * Holds annotated information associated with this class.
 *
 * @author kenl
 *
 */
class ClassMetaHolder(z:Class[_]) extends CoreImplicits {

  private val _info= new FMap()
  private var _table=""

  import ClassMetaHolder._
  import MetaCache._
  import DBPojo._
  import Utils._
  iniz(z)

  /**
   *
   */
  def this() { this(null) }

  def scan(z:Class[_] ): this.type = {
    iniz(z)
    this
  }

  def getTable() = _table

  def getGetter( col:String ) = {
    _info.getViaCol(col) match {
      case Some(x) => x.getGetter
      case _ => null
    }
  }

  def getSetter( col:String ) = {
    _info.getViaCol(col) match {
      case Some(x) => x.getSetter
      case _ => null
    }
  }

  def isKey( col:String ) = {
    _info.getViaCol(col) match {
      case Some(x) => x.isPK
      case _ => false
    }
  }

  def getUniques() = {
    val rc= mutable.HashSet[String]()
    _info.foreach { (en) =>
      val ii= en._2
      val m = if (ii.isUniqueKey()) ii.getGetter() else null
      val c= if (m==null) null else m.getAnnotation(classOf[Column])
      if (c != null) {
        rc += c.id().uc
      }
    }
    rc.toSeq
  }

  def getGetters() = {
    val rc= mutable.HashMap[String, Method]()
    _info.foreach { (en) =>
      val m= en._2.getGetter
      val c= if (m==null) null else m.getAnnotation(classOf[Column])
      if (c != null) {
        rc.put( c.id().uc, m)
      }
    }
    rc.toMap
  }

  def getFldMetas() = _info.toMap

  private def iniz( z:Class[_] ) {
    
    tstArgIsType( z.getName() , z, classOf[DBPojo])    
    Utils.getTable(z) match {
      case t:Table =>
        val mtds= z.getMethods()
        val ms = mtds.foldLeft( new mutable.HashMap[String,Method] ) { (out, m) => 
          out += m.getName -> m
        }.toMap
        val obj= mkRef(z)
        scanAllMarkers(obj, mtds, ms)
        scanSetters(ms)
        scanAssocs( z, mtds, ms)
        _table= t.table()
  //      injectSysCols()        
      case _ =>
    }
    
  }
  
  private def ensureMarker(mn:String) = {
      if ( mn.startsWith("dbio_") && mn.endsWith("_column") && mn.length >12 ) {} else {
        throw new Exception("Invalid marker:  found : " + mn)         
      }
      mn
  }
  
  private def chompMarker(mn:String) = {
    mn.substring(5, mn.length - 7)
  }
  
  private def ensureMarkerType(m:Method) = {
    if ( !isString( m.getReturnType() ) ) {
        throw new Exception("Expected marker type (string) for: " + m.getName )               
    }
  }
  
  private def getCol(obj:Any, m:Method) = {
    nsb ( m.invoke(obj) )
  }
  
    // scan for "markers(s)", all column defs are bound to markers
  private def scanAllMarkers( obj:Any, ms:Array[Method], allMtds:Map[String,Method]) {
    
    ms.filter( hasColumn(_) ).foreach { (m) =>
      val mn= ensureMarker( m.getName)
      ensureMarkerType( m )
      val cn = getCol(obj, m).toUpperCase()
      val c = getColumn(m)      
      _info.get(cn) match {
        case Some(x) =>throw new Exception("Found duplicate marker: " + mn)
        case _ => _info.put(cn, new FldMetaHolder( cn ) )
      }
      val gn= chompMarker(mn)
      val rt= allMtds.get( gn) match {
        case Some(x) =>
          _info.get(cn).get.setGetter(x)
          x.getReturnType()
        case _ =>throw new Exception("Missing getter: " + gn)
      }

      if (c.autogen) if ( ! isInt(rt) &&  ! isLong(rt) ) {
          throw new Exception("Invalid return-type for: " + gn) 
      }
      
    }    
    
  }

    // scan for corresponding "setter(s)"
  private def scanSetters( getters:Map[String,Method], ms:Map[String,Method] ) {
    
    getters.foreach { (en) =>
      val sn= "set" + en._1.substring(3)
      ms.get(sn) match {
        case Some(x) =>
          val c = getColumn(en._2)
          val n= maybeGetCID(c, en._1)
          val h= _info.getViaCol(n).get
          if ( h.getSetter() != null) {
            throw new Exception("Can only have one setter  :  existing setter : " +
                              h.getSetter().getName + " , found another : " + sn ) 
          }
          h.setSetter(x)
        case _ =>
      }
    }
    
  }

    // scan for "assoc(s)" ...
  private def scanAssocs(z:Class[_], ms:Array[Method], allMtds:Map[String, Method] ) {
    ms.filter(_.getName().startsWith("get")).foreach { (m) =>
      val m2m= m.getAnnotation(classOf[Many2Many])
      val o2o= m.getAnnotation(classOf[One2One])
      val o2m= m.getAnnotation(classOf[One2Many])
      val mn= m.getName()
      var count=0
      var rhs:Class[_] = null

      if (m2m != null) { count += 1; rhs=m2m.rhs()  }
      if (o2o != null) { count += 1; rhs= o2o.rhs() }
      if (o2m != null) { count += 1; rhs= o2m.rhs() }

      if (count > 1 ) {
        throw new Exception("Cannot have multiple assoc types bound to same getter")
      }

      if (count == 1) { 
        allMtds.get(mn + "FKey") match {
          case Some(m2) =>
            val fkey = m2.invoke( m2.getDeclaringClass().newInstance() )
            val rt = if (rhs.isAssignableFrom(z)) {
              // assoc target is a parent of this class, switch to be this class instead
              z.getAnnotation(classOf[Table] )
            } else {
              rhs.getAnnotation(classOf[Table] )
            }
            if (rt == null) {
              throw new Exception("RHS of assoc must have Table annotated")
            }
            val rtb= rt.table().uc
            val am= _assocs.get(rtb) match {
              case Some(x) => x
              case _ =>
                val a= new AssocMetaHolder()
                _assocs.put(rtb, a)
                a
            }
            am.add(m2m != null, z, rhs, nsb(fkey))
          case _ =>
            throw new Exception("Missing foreign key column getter for assoc : mtd = " + mn) 
        }

      }

    }


  }

  // add in internal cols
  private def injectSysCols() {
    var h = new FldMetaHolder() {
      override def isAutoGen() = true
      override def getId() = COL_ROWID
      override def isNullable() = false
      override def isPK() = true
      override def getColType() = classOf[Long]
    }
    _info.put(COL_ROWID, h)

    h = new FldMetaHolder() {
      override def isAutoGen() = false
      override def getId() = COL_VERID
      override def isNullable() = false
      override def isPK() = false
      override def getColType() = classOf[Long]
    }
    _info.put(COL_VERID, h)
  }

}


/**
 * @author kenl
 *
 */
@SerialVersionUID(-1L)
class FMap extends mutable.HashMap[String, FldMetaHolder] with CoreImplicits {

  def getViaCol(col:String ) = {
    val h= if (col == null) None else get( col.uc )
    h match {
      case None => Option( FldMetaHolder.DUMBO)
      case r => r
    }
  }

}

