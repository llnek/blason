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
import org.slf4j._



object ClassMetaHolder {
  private val _log= LoggerFactory.getLogger(classOf[ClassMetaHolder])
}

/**
 * Holds annotated information associated with this class.
 *
 * @author kenl
 *
 */
class ClassMetaHolder(private val _meta:MetaCache, z:Class[_]) extends CoreImplicits {

  private val _info= new FMap()
  private var _table=""

  def tlog() =  ClassMetaHolder._log
  
  import MetaCache._
  import DBPojo._
  import Utils._
  iniz(z)

  /**
   *
   */
  def this(m:MetaCache) { this(m,null) }

  def getCZ() = z 
    
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
      if ( mn.startsWith("dbio_") && mn.endsWith("_column") && mn.length > 12 ) {} else {
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

  private def ensureFKeyType(m:Method) = {
    if ( !isString( m.getReturnType() ) ) {
        throw new Exception("Expected marker type (string) for: " + m.getName )               
    }
  }
  private def ensureAssoc(mn:String) = {
      if ( mn.startsWith("dbio_") && mn.endsWith("_fkey") && mn.length > 10 ) {} else {
        throw new Exception("Invalid assoc-fkey marker:  found : " + mn)         
      }
      mn
  } 
  private def chompAssoc(mn:String) = {
    mn.substring(5, mn.length - 5)
  }
  private def fmtAssocKey(mn:String) = {
    "dbio_" + mn + "_fkey"
  }
  private def fmtMarkerKey(mn:String) = {
    "dbio_" + mn + "_column"
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
      val kn= fmtMarkerKey(m.getName )
      val km= allMtds.get(kn) match {
        case Some(x) => x
        case _ => throw new Exception("Column getter not found: " + kn)
      }
      ensureMarkerType( km )
      val cn = getCol(obj, km).toUpperCase()
      val rt = m.getReturnType()
      val gn= m.getName
      val c = getColumn(m)      
      _info.get(cn) match {
        case Some(x) =>throw new Exception("Found duplicate col-def: " + cn)
        case _ => _info.put(cn, mkFldMeta(c, cn,gn))
      }
      _info.get(cn).get.setGetter(m)

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

  private def mkM2M(z:Class[_], m:Method, fkey:String) {
    val mm= getM2M(m)
    val jc= mm.joined()
    val rhs = mm.rhs()
    val rt= Utils.getTable(rhs)
    if ( rt==null) {
      throw new Exception("RHS of assoc must have Table annotated: " + rhs)
    }
    val lt= Utils.getTable(z)    
    val jn= sortAndJoin(rt.table.uc, lt.table().uc)
    _meta.getMMS().get(jn) match {
      case Some(x) =>
      case _ =>
        _meta.putMMS(jn, jc)
    }
  }
  
  private def mkX2X(z:Class[_], m:Method, fkey:String) {
    val rhs = if (hasO2M(m)) { getO2M(m).rhs() } else if (hasO2O(m)) {
      getO2O(m).rhs()
    } else { throw new Exception("never!" ) }
    
    // assoc target is a parent of this class, switch to be this class instead
    // to do with inheritance
    val lt = Utils.getTable(z)
    val rt = if (rhs.isAssignableFrom(z)) { lt } else {  Utils.getTable( rhs)  }
    
    if ( rt==null) {
      throw new Exception("RHS of assoc must have Table annotated: " + rhs)
    }
    
    if (STU.isEmpty(fkey)) {      
      throw new Exception("Invalid foreign key on method: " + m.getName)
    }
    val (owner, fcz) = if (hasO2O(m) && getO2O(m).bias() < 0) { ( lt, z) } else { ( rt, rhs) }
    val akey= owner.table.toUpperCase()
    if (! _meta.getAssocMetas().contains( akey)) {
      _meta.putAssocMeta( akey, new AssocMetaHolder() )      
    }
    
    _meta.getAssocMetas().get(akey).get.add( z, rhs, fkey)
  }
  
    // scan for "assoc(s)" ...
  private def scanAssocs(z:Class[_], obj:Any, ms:Array[Method], allMtds:Map[String, Method] ) {
    ms.filter( (m) => m.getName.startsWith("get") && hasAssoc(m) ).foreach { (m) =>
      val mn= m.getName()
      allMtds.get( fmtAssocKey(mn) ) match {
        case Some(x) =>  ensureFKeyType(x)
          val fk=nsb( x.invoke( obj ))
          if (hasM2M(m)) {
            mkM2M(z,m,fk)
          } else {
            mkX2X(z, m, fk)            
          }
        case _ => throw new Exception("Missing assoc-fkey getter for: " + mn)
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

