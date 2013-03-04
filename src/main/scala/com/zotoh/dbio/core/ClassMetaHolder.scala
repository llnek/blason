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

import scala.language.existentials
import org.apache.commons.lang3.{StringUtils=>STU}
import scala.collection.mutable
import java.lang.reflect.Method
import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._
import org.slf4j._
import java.util.Calendar



object ClassMetaHolder {
  private val _log= LoggerFactory.getLogger(classOf[ClassMetaHolder])
  def toTZCol(col:String) = col + "_TZ"
}

/**
 * Holds annotated information associated with this class.
 *
 * @author kenl
 *
 */
class ClassMetaHolder(private val _meta:MetaCache) extends CoreImplicits {

  private var _cz:Class[_] = null
  private var _table=""
  private val _info= new FMap()

  def tlog() =  ClassMetaHolder._log

  import ClassMetaHolder._
  import MetaCache._
  import DBPojo._
  import Utils._

  def getCZ() = _cz

  def scan(z:Class[_] ): this.type = {
    iniz(z)
    this
  }

  def getTable() = _table.uc

  def getGetter( col:String ): Method = {
    _info.getViaCol(col) match {
      case Some(x) => x.getGetter
      case _ => null
    }
  }

  def getSetter( col:String ): Method = {
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

  def getIndexes( wantUnique:Boolean = false) = {
    val rc= mutable.HashMap[String,SIndex]()
    val t = Utils.throwNoTable( getCZ )
    // for many2many tables, we do an internal hack to pull in
    // the indexes defined in the abstract parent class
    val mmz= classOf[M2MTable]
    val arr = if ( mmz.isAssignableFrom(_cz)) {
      val tt=Utils.getTable(mmz)
      if (wantUnique) tt.uniques() else tt.indexes()
    } else {
      if (wantUnique) t.uniques() else t.indexes()
    }
    val ss= arr.map( _.toUpperCase ).toSet
    _info.values.filter( _.isIndex ).foreach { (fld) =>
      val nn= fld.getIndexName.uc
      if (ss.contains( nn )) {
        if ( rc.get(nn).isEmpty) {
          rc += nn -> new SIndex(nn, wantUnique)
        }
        rc.get(nn).get.add(fld.getId )
      } else {
//        throw new Exception("Unknown index referenced: " + nn + " on table: " + t.table )
      }
    }
    rc.toMap
  }

  // returns a map of column->the_getter_method
  def getGetters() = {
    val rc= mutable.HashMap[String, Method]()
    _info.foreach { (en) =>
      val m= en._2.getGetter
      val c= if (m==null) null else m.getAnnotation(classOf[Column])
      if (c != null) {
        rc.put( en._2.getId.uc, m)
      }
    }
    rc.toMap
  }

  def getFldMetas() = _info.toMap

  private def iniz( z:Class[_] ) {

    Utils.getTable(z) match {
      case t:Table =>
        tstArgIsType( z.getName , z, classOf[DBPojo])
        val mtds= z.getMethods
        // create a map of method_name->method
        val ms = ( new mutable.HashMap[String,Method] /: mtds ) { (out, m) =>
          out += m.getName -> m
        }.toMap
        val obj= mkRef(z)
        scanAllMarkers(obj, mtds, ms)
        scanSetters(z,ms)
        scanAssocs( z, obj, mtds, ms)
        _table= t.table.lc
      case _ =>
    }
    _cz = z
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
    if ( !isString( m.getReturnType ) ) {
      throw new Exception("Expected marker type (string) for: " + m.getName )
    }
  }

  private def ensureFKeyType(m:Method) = {
    if ( !isString( m.getReturnType ) ) {
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
    nsb( m.invoke(obj) )
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
      val cn = getCol(obj, km).toUpperCase
      val rt = m.getReturnType
      val gn= m.getName
      val c = getColumn(m)
      _info.get(cn) match {
        case Some(x) =>throw new Exception("Found duplicate col-def: " + cn)
        case _ => _info.put(cn, mkFldMeta(c, cn,gn))
      }
      _info.get(cn).get.setGetter(m)

      // something special for calendar type
      if ( classOf[Calendar].isAssignableFrom(rt)) {
        // need to add extra ad-hoc field to store timezone
        val n= toTZCol(cn)
        _info.put( n, new AdhocFldMetaHolder(n, classOf[String] ))
      }

      if (c.autogen) if ( ! isInt(rt) &&  ! isLong(rt) ) {
        throw new Exception("Invalid return-type for: " + gn)
      }

    }

  }

  // scan for corresponding "setter(s)"
  private def scanSetters( z:Class[_], ms:Map[String,Method] ) {
    _info.foreach { (en) =>
      en._2 match {
        case x:AdhocFldMetaHolder =>
        case x =>
          val gn = en._2.getGetter.getName
          val cn = en._1
          val sn = "set" + gn.substring(3)
          ms.get(sn) match {
            case Some(x) => en._2.setSetter(x)
            case _ => tlog.warn("No setter defined for getter: " + gn + " for class: " + z)
          }
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
        case Some(x) =>
          ensureFKeyType(x)
          val fk=nsb( x.invoke( obj ))
          if (hasM2M(m)) {
            mkM2M(z,m,fk)
          } else {
            mkX2X(z, m, fk)
          }
        case _ =>
          throw new Exception("Missing assoc-fkey getter for: " + mn)
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

class SIndex(private val _name:String, private val _unique:Boolean) {
  private val _cols= mutable.HashSet[String]()
  def name() = _name
  def unique() = _unique
  def add(s:String) {
    _cols += s.toUpperCase
  }
  def getCols() = _cols.toSet
}


