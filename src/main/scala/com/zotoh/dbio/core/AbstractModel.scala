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

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.dbio.meta.Column
import java.util.{Date=>JDate}
import java.sql.{Timestamp=>JTS,Time=>JTime}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.JSONUtils
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.Nichts
import com.zotoh.dbio.core.DBPojo.COL_ROWID
import com.zotoh.dbio.core.DBPojo.COL_VERID
import com.zotoh.dbio.core.{Utils=>DBU}
import java.util.{Date =>JDate}
import java.util.{Arrays,Calendar }
import java.util.GregorianCalendar
import java.util.TimeZone
import org.slf4j._
import org.json.JSONObject
import java.lang.reflect.Method
import org.json.JSONArray

object AbstractModel {
  private val _log= LoggerFactory.getLogger(classOf[AbstractModel])
}

/**
 * @author kenl
 */
abstract class AbstractModel extends DBPojo with CoreImplicits {

  import ClassMetaHolder._
  import DBPojo._
  import DBAction._
  
  def tlog() = AbstractModel._log
  
  private val _storage= mutable.HashMap[String,Any]()
  private val _refs= mutable.HashMap[String,Any]()
  private val _dirtyFields= mutable.HashSet[String]()
  private var _isDBRow=false
  private def iniz() {
    setVerID(0L)
  }
  iniz()

  def isTransient() = ! _isDBRow
  def isDB() = _isDBRow
  
  def postEvent(db:SQLProc, act:DBAction ) {}
  def preEvent(db:SQLProc, act:DBAction) {}

  def getSeq(mtd:String): Seq[_] = _refs.get(mtd) match {
    case Some(x:Seq[_])  => x
    case _ => List()
  }
  def getRef(col:String) = _refs.get(col) match {
    case Some(x:DBPojo) => x
    case _ => null
  }
  protected def setRef(col:String,r:Any) {
    r match {
      case null => _refs.remove(col)
      case _ => _refs.put(col, r)
    }
  }
  
  protected def readString(col:String, dft:String="") = {
    readData(col) match {
      case Some(s:String) => s
      case Some(x:Nichts) => dft
      case Some(x) => nsb(x)
      case _ => dft
    }
  }
  protected def readInt(col:String, dft:Int = 0) = {
    readData(col) match {
      case Some(n:Int) => n
      case Some(x:Nichts) => dft
      case Some(x) => asInt(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readLong(col:String, dft:Long = 0L) = {
    readData(col) match {
      case Some(n:Long) => n
      case Some(x:Nichts) => dft
      case Some(x) => asLong(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readDouble(col:String, dft:Double = 0.0) = {
    readData(col) match {
      case Some(d:Double) => d
      case Some(d:Float) => d.toDouble
      case Some(x:Nichts) => dft
      case Some(x) => asDouble(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readFloat(col:String, dft:Float= 0.0f) = {
    readData(col) match {
      case Some(f:Double) => f.toDouble
      case Some(f:Float) => f
      case Some(x:Nichts) => dft
      case Some(x) => asFloat(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readBool(col:String, dft:Boolean = false) = {
    readData(col) match {
      case Some(n:Int) => if (n==0 ) false else true
      case Some(b:Boolean) => b
      case _ => dft
    }
  }
  protected def readCalendar(col:String, dft:Calendar= null) = {
    val cal = new GregorianCalendar( TimeZone.getTimeZone( readString(toTZCol(col),"GMT")))
    readData(col) match {
      case Some(x:java.sql.Timestamp) =>
        cal.setTimeInMillis( x.getTime)
        cal
      case Some(x:JDate) =>
        cal.setTimeInMillis( x.getTime)
        cal
      case Some(x:Calendar) => x
      case _ => dft
    }
  }
  protected def readTimestamp(cn:String, dft:java.sql.Timestamp) = {
    readData(cn) match {
      case Some(x:java.sql.Timestamp) => x
      case Some(x:JDate) => new java.sql.Timestamp( x.getTime)
      case _ => dft
    }
  }
  protected def readDate(cn:String, dft:JDate) = {
    readData(cn) match {
      case Some(x:java.sql.Timestamp) => new JDate( x.getTime )
      case Some(x:JDate) => x
      case _ => dft
    }
  }
  protected def readTime(cn:String, dft:JTime) = {
    readData(cn) match {
      case Some(x:java.sql.Time) => x
      case Some(x:JDate) => new JTime(x.getTime)
      case _ => dft
    }
  }

  def set(field:String, value: Option[Any] ) {
    writeData(field,value)
  }

  def get(field:String): Option[Any] = {
    _storage.get(field.uc)
  }

  def getDirtyFields() = _dirtyFields.toSet

  protected def writeData(col:String, value:Option[Any]) {

    val cuc = col.uc match {
      case s =>
      _storage.put(s, value.getOrElse(Nichts.NICHTS))
      _dirtyFields += s
      s
    }
    // when we store a calendar, we need to store the timezone also
    value match {
      case Some(x:Calendar) => writeData(toTZCol(cuc), Option(x.getTimeZone.getID))
      case _ =>
    }
  }

  protected def readData(col:String): Option[Any] = {
    _storage.get(col.uc)
  }

  def setO2O(rhs:DBPojo, fkey:String) {
    set(fkey, if (rhs==null) None else Option(rhs.getRowID ) )
    setRef(fkey , rhs)    
  }
  def linkO2M( rhs:DBPojo, fkey:String) =  {
    if (rhs == null) 0 else {
      rhs.set(fkey, Some(this.getRowID) )
      1
    }
  }
  def unlinkO2M(rhs:DBPojo, fkey:String): Int = {
    if (rhs == null) 0 else {
      rhs.set(fkey, None)
      1
    }
  }

  def commit() {
    setVerID( getVerID() + 1)
    setAsRow()
    reset()
  }

  def reset() {
    _dirtyFields.clear
    _refs.clear
  }

  def built() {
    setAsRow()
    reset()
  }

  def isDirty= _dirtyFields.size > 0

  def dbio_getRowID_column = COL_ROWID
  @Column(data=classOf[Long],autogen=true,optional=false,system=true,updatable=false)
  def getRowID()  = {
    readData( dbio_getRowID_column ) match {
      case Some(x:Long) => x
      case _ => -1L
    }
  }
  def setRowID(n:Long ) {
    writeData( dbio_getRowID_column , Option(n))
  }


  def dbio_getVerID_column = COL_VERID
  @Column( data=classOf[Long],optional=false,system=true,dft=true,updatable=false,dftValue="0")
  def getVerID() = {
    readData(dbio_getVerID_column ) match {
      case Some(x:Long) => x
      case _ => -1L
    }
  }
  def setVerID(v:Long ) {
    writeData( dbio_getVerID_column, Option(v))
  }

  def dbio_getLastModified_column = "dbio_lastchanged"
  @Column(data=classOf[JTS], optional=false, system=true, dft=true)
  def getLastModified() = {
    readData( dbio_getLastModified_column) match {
      case Some(x:JTS) => x
      case _ => nowJTS()
    }
  }
  def setLastModified(t:JTS) {
    writeData( dbio_getLastModified_column, Option(t) )
  }

  def dbio_getCreated_column = "dbio_created_on"
  @Column( data=classOf[JTS], optional=false, system=true, dft=true,updatable=false)
  def getCreated() = {
    readData( dbio_getCreated_column) match {
      case Some(x:JTS) => x
      case _ => nowJTS()
    }
  }
  def setCreated(t:JTS) {
    writeData( dbio_getCreated_column, Option(t) )
  }

  def dbio_getCreator_column = "dbio_created_by"
  @Column(data=classOf[String])
  def getCreator() = {
    readData( dbio_getCreator_column ) match {
      case Some(s:String) => s
      case _ => ""
    }
  }
  def setCreator(s:String) {
    writeData(dbio_getCreator_column, Option(s) )
  }

  private def setAsRow() { _isDBRow=true }

  def stringify(cache:MetaCache, db:SQLProc, skipDBIO:Boolean = true) = {
    JSONUtils.asString( getJSON(cache,db,skipDBIO) )
  }
  
  def getJSON(cache:MetaCache, db:SQLProc, skipDBIO:Boolean) = {
    val czmeta= cache.getClassMeta(this.getClass)
    tstArg( czmeta.isDefined, "Meta-class-holder for class: " + getClass  + " not found." )       
    val metas= czmeta.get.getFldMetas
    val refs = czmeta.get.getRefs
    val rc= if (skipDBIO) metas.keySet.filter(  ! isInternal( _ ) ) else metas.keySet
    val ks= rc.toArray[Object]
    val root= new JSONObject()
    
    Arrays.sort(ks); ks.foreach { (k) =>
      val kk= nsb(k)
      metas.get(kk) match {
        case Some(fld) => if ( includeJSON(kk) )  {
            val v= _storage.get( kk ).getOrElse(null) match {
              case x:String => nsb(x)
              case x:Long => x
              case x:Int => x
              case x:Double => x
              case x:Float => x
              case x:java.sql.Timestamp => x.getTime
              case x:JTime => x.getTime()
              case x:JDate => x.getTime()
              case x:Calendar => x.getTimeInMillis
              case _ => null
            }
            root.put(kk, v)
        }
        case _ =>
      }
    }
    
    if (refs.size > 0) {
      getRefJSON(cache, db, root, refs, skipDBIO)
    }
    
    root
  }

  private def getRefJSON(cache:MetaCache, db:SQLProc, root:JSONObject, refs:Map[String,Method], skipDBIO:Boolean) {
    val keys= refs.keySet.filter( includeJSON(_) ).toArray[Object]; Arrays.sort(keys);
    val t= asJObj(true)
    keys.foreach { (k) =>
      val kk= nsb(k)
      val m= refs.get(kk).get
      val s= if (DBU.hasM2M(m) || DBU.hasO2M(m)) {
        m.invoke(this, db, t) match {
          case Some(x:AbstractModel ) =>
            val a = x.getJSON(cache, db, skipDBIO)
            if (a!=null) { val arr= new JSONArray; arr.put(a); root.put(kk, arr) }
          case x:Seq[_] => 
            val arr = getJSONArray(x, cache, db, skipDBIO)
            root.put(kk,arr)
          case _ =>
        }
      } else if (DBU.hasO2O(m)) {             
        m.invoke(this, db, DBU.getO2O(m).rhs(),t) match {
          case Some(x:AbstractModel ) =>
            val a = x.getJSON(cache, db, skipDBIO)
            if (a!=null) { root.put(kk, a) }
          case _ =>
        }
      }      
    }
  }
  
  private def getJSONArray(seq:Seq[_], cache:MetaCache, db:SQLProc, skipDBIO:Boolean) = {
    val arr= new JSONArray()
    seq.foreach { _ match {
      case x:AbstractModel => 
        val a = x.getJSON(cache, db, skipDBIO)
        if (a!=null) { arr.put(a) }
      case _ => 
    }}    
    arr
  }
  
  protected def includeJSON(col:String) = true
  
  private def isInternal(col:String) = col.toLowerCase.startsWith("dbio_")
  private def isFK(col:String) = col.toLowerCase.startsWith("fk_")
  
  
}
