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
import com.zotoh.dbio.meta.Column
import java.util.{Date=>JDate}
import java.sql.{Timestamp=>JTS,Time=>JTime}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.Nichts
import com.zotoh.dbio.core.DBPojo.COL_ROWID
import com.zotoh.dbio.core.DBPojo.COL_VERID
import java.util.{Date =>JDate}
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone


/**
 * @author kenl
 */
abstract class AbstractModel extends DBPojo with CoreImplicits {

  import ClassMetaHolder._
  import DBPojo._

  private val _storage= mutable.HashMap[String,Any]()
  private val _dirtyFields= mutable.HashSet[String]()
  private var _isDBRow=false
  private def iniz() {
    setVerID(0L)
  }
  iniz()

  def isTransient() = ! _isDBRow
  def isDB() = _isDBRow

  protected def readString(col:String, dft:String="") = {
    readData(col) match {
      case Some(s:String) => s
      case Some(x) => nsb(x)
      case _ => dft
    }
  }
  protected def readInt(col:String, dft:Int = 0) = {
    readData(col) match {
      case Some(n:Int) => n
      case Some(x) => asInt(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readLong(col:String, dft:Long = 0L) = {
    readData(col) match {
      case Some(n:Long) => n
      case Some(x) => asLong(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readDouble(col:String, dft:Double = 0.0) = {
    readData(col) match {
      case Some(d:Double) => d
      case Some(d:Float) => d.toDouble
      case Some(x) => asDouble(nsb(x), dft)
      case _ => dft
    }
  }
  protected def readFloat(col:String, dft:Float= 0.0f) = {
    readData(col) match {
      case Some(f:Double) => f.toDouble
      case Some(f:Float) => f
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
  protected def readCalendar(col:String, dft:Calendar) = {
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

  protected def setO2O(rhs:DBPojo, fkey:String) {
    set(fkey, if (rhs==null) None else Option(rhs.getRowID ) )
  }

  def commit() {
    setVerID( getVerID() + 1)
    setAsRow()
    reset()
  }

  def reset() {
    _dirtyFields.clear()
  }

  def built() {
    setAsRow()
    reset()
  }

  def isDirty= _dirtyFields.size > 0

  def dbio_getRowID_column = COL_ROWID
  @Column(autogen=true,optional=false,system=true,updatable=false)
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
  @Column( optional=false,system=true,dft=true,updatable=false,dftValue="0")
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
  @Column( optional=false, system=true, dft=true)
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
  @Column( optional=false, system=true, dft=true,updatable=false)
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
  @Column()
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

}
