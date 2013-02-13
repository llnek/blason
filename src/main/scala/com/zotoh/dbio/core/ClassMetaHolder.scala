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

import scala.collection.mutable
import java.lang.reflect.Method
import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._


object ClassMetaHolder {
  private val _assocs= mutable.HashMap[String,AssocMetaHolder]()
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
      case Some(x) => x.isUniqueKey
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
    val t = if (z == null) null else { z.getAnnotation(classOf[Table] ) }
    if (t != null) {
      if ( ! classOf[DBPojo].isAssignableFrom(z)) {
        throw new IllegalArgumentException("Class " + z + " is not a DBPojo")
      }
      _table= t.table()
      val getters= mutable.HashMap[String,Method]()
      val mtds= mutable.HashMap[String,Method]()
      val ms= z.getMethods()
      ms.foreach { (m) => mtds.put(m.getName, m) }
      pass1(ms, getters)
      pass2(ms, getters)
      pass3(z, ms, mtds.toMap)
      pass4(ms)
      injectSysCols()
    }
  }

  private def pass1(ms:Array[Method], outGetters:mutable.HashMap[String,Method] ) {
    // scan for "getter(s)", all column defs are bound to getters
    val fc= { (m:Method,c:Column) =>

      val cn= c.id().toUpperCase
      val mn= m.getName
      val rt= m.getReturnType()

      if ( !mn.startsWith("get")) {
        throw new Exception("Can only annotate getter(s) :  found : " + mn) 
      }

      _info.get(cn) match {
        case None => _info.put(cn, new FldMetaHolder() )
        case Some(mi) =>
          if (mi.getGetter() != null)
            throw new Exception("Can only annotate column once :  existing getter : " + mi.getGetter().getName + " , found another : " + mn )
      }

      outGetters.put(mn, m)
      _info.get(cn).get.setGetter(m)

      if (c.autogen) {
      if ( !isInt(rt) &&  !isLong(rt) ) {
        throw new Exception("Using auto-gen, only int or long are allowed") 
      }}
      
    }

    ms.foreach { (m) =>
      val c= m.getAnnotation(classOf[Column])
      if (c!=null) { fc(m,c) }
    }
  }

  private def pass2(ms:Array[Method], curGetters:mutable.HashMap[String,Method] ) {
    // scan for corresponding "setter(s)"
    val fc = { (m:Method,g:Method) =>
      // ok, got a matching setter
      val col= g.getAnnotation(classOf[Column]).id()
      _info.getViaCol(col) match {
        case Some(h) =>
          if ( h.getSetter() != null) {
            throw new Exception("Can only have one setter  :  existing setter : " +
                              h.getSetter().getName + " , found another : " + m.getName ) 
          }
          h.setSetter(m)
        case _ =>
          throw new Exception("Setter found but no field meta: table = " + _table + ", col = " + col) 
      }
    }

    ms.foreach { (m) =>
      val mn= m.getName()
      if ( mn.startsWith("set")) { 
        curGetters.get( "get" + mn.substring(3) ) match {
          case Some(g) => fc(m,g)
          case _ =>
        }
      }
    }
  }

  private def pass3(z:Class[_], ms:Array[Method], allMtds:Map[String, Method] ) {
    // scan for "assoc(s)" ...
    val fc= { (m:Method,mn:String) =>
      val m2m= m.getAnnotation(classOf[Many2Many])
      val o2o= m.getAnnotation(classOf[One2One])
      val o2m= m.getAnnotation(classOf[One2Many])
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

    ms.foreach { (m) =>
      val mn= m.getName
      if ( mn.startsWith("get")) { fc(m,mn) }
    }

  }

  private def pass4(ms:Array[Method] ) {
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
      case None => Some( FldMetaHolder.DUMBO)
      case r@Some(x) => r
    }
  }

}

