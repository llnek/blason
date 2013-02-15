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

  def getTable() = _table.toUpperCase()

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
        rc += ii.getId().uc
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
        rc.put( en._2.getId().uc, m)
      }
    }
    rc.toMap
  }

  def getFldMetas() = _info.toMap

  private def iniz( z:Class[_] ) {
    
    Utils.getTable(z) match {
      case t:Table =>
        tstArgIsType( z.getName() , z, classOf[DBPojo])    
        val mtds= z.getMethods()
        val ms = mtds.foldLeft( new mutable.HashMap[String,Method] ) { (out, m) => 
          out += m.getName -> m
        }.toMap
        val obj= mkRef(z)
        scanAllMarkers(obj, mtds, ms)
        scanSetters(z,ms)
        scanAssocs( z, obj, mtds, ms)
        _table= t.table().toUpperCase()
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
  
  private def mkFldMeta(c:Column, col:String, getter:String) = {
    if (getter == "getRowID") {
      new FldMetaHolder( col,c ) {
        override def isPK() = true
      }
    } else {
      new FldMetaHolder( col,c )
    }
  }
  
    // scan for "markers(s)", all column defs are bound to markers
  private def scanAllMarkers( obj:Any, ms:Array[Method], allMtds:Map[String,Method]) {
    
    ms.filter( hasColumn(_) ).foreach { (m) =>
      val mn= ensureMarker( m.getName)
      ensureMarkerType( m )
      val cn = getCol(obj, m).toUpperCase()
      val gn= chompMarker(mn)
      val c = getColumn(m)      
      _info.get(cn) match {
        case Some(x) =>throw new Exception("Found duplicate marker: " + mn)
        case _ => _info.put(cn, mkFldMeta(c, cn,gn))
      }
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
  private def scanSetters( z:Class[_], ms:Map[String,Method] ) {    
    _info.foreach { (en) =>
      val gn = en._2.getGetter.getName()
      val cn = en._1
      val sn = "set" + gn.substring(3)
      ms.get(sn) match {
        case Some(x) => en._2.setSetter(x)
        case _ => tlog.warn("No setter defined for getter: " + gn + " for class: " + z)
      }
    }
  }

  private def mkAssoc(z:Class[_], obj:Any, m:Method, rhs:Class[_], m2m:Boolean) {
    val fkey = nsb( m.invoke( obj ))
    val rt = if (rhs.isAssignableFrom(z)) {
      // assoc target is a parent of this class, switch to be this class instead
      Utils.getTable(z)
    } else {
      Utils.getTable( rhs)
    }
    if (rt == null) {
      throw new Exception("RHS of assoc must have Table annotated: " + rhs)
    }
    if (STU.isEmpty(fkey)) {      
      throw new Exception("Invalid foreign key on method: " + m.getName)
    }    
    val rtb= rt.table.toUpperCase()
    if (! _assocs.contains(rtb)) {
      _assocs.put(rtb, new AssocMetaHolder() )      
    }
    _assocs.get(rtb).get.add(m2m, z, rhs, fkey)
  }
  
    // scan for "assoc(s)" ...
  private def scanAssocs(z:Class[_], obj:Any, ms:Array[Method], allMtds:Map[String, Method] ) {
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
        throw new Exception("Too many assocs bound to: " + mn)
      }
      if (count==1) allMtds.get(mn+"FKey") match {
        case Some(x) => mkAssoc(z, obj, x, rhs, m2m!=null)
        case _ =>
      }
      
    }


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

