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

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.Nichts
import com.zotoh.dbio.core.DBPojo.COL_ROWID
import com.zotoh.dbio.core.DBPojo.COL_VERID
import java.util.{Date => JDate}


/**
 * @author kenl
 */
abstract class AbstractModel extends DBPojo with CoreImplicits {
  import DBPojo._
  
  private val _storage= mutable.HashMap[String,Any]()
  private val _dirtyFields= mutable.HashSet[String]()
  private def iniz() {
  }
  iniz()

  def set(field:String, value: Option[Any] ) {
    writeData( field,value)
  }
  
  def get(field:String): Option[Any] = {
    _storage.get( field.uc)    
  }
  
  protected def writeData(col:String, value:Option[Any]) {
    col.uc match {
      case s =>
      _storage.put(s, value.getOrElse(Nichts.NICHTS))
      _dirtyFields += s
    }
  }
  
  protected def readData(col:String): Option[Any] = {
    _storage.get(col.uc)
  }
  
  def isDirty= _dirtyFields.size > 0
  
  @Column(autogen=true,optional=false,system=true)
  def dbio_getRowID_column = COL_ROWID
  def getRowID()  = {
    readData( dbio_getRowID_column ) match {
      case Some(x:Long) => x
      case _ => -1L
    }
  }
  def setRowID(n:Long ) {
    writeData( dbio_getRowID_column , Option(n))
  }

  
  @Column( optional=false,system=true)
  def dbio_getVerID_column = COL_VERID
  def getVerID() = {
    readData(dbio_getVerID_column ) match {
      case Some(x:Long) => x
      case _ => -1L
    }
  }
  def setVerID(v:Long ) {
    writeData( dbio_getVerID_column, Option(v))
  }
  
  @Column( optional=false, system=true)
  def dbio_getLastModified_column = "dbio_lastchanged"
  def getLastModified() = {
    readData( dbio_getLastModified_column) match {
      case Some(x:JDate) => x
      case _ =>new JDate()
    }
  }
  def setLastModified(t:JDate) {
    writeData( dbio_getLastModified_column, Option(t) )
  }

  @Column( optional=false, system=true, dft="current_timestamp")
  def dbio_getCreated_column = "dbio_created_on"
  def getCreated() = {
    readData( dbio_getCreated_column) match {
      case Some(x:JDate) => x
      case _ =>new JDate()
    }
  }
  def setCreated(t:JDate) {
    writeData( dbio_getCreated_column, Option(t) )
  }

  @Column()
  def dbio_getCreator_column = "dbio_created_by"
  def getCreator() = {
    readData( dbio_getCreator_column ) match {
      case Some(s:String) => s
      case _ => ""
    }
  }
  def setCreator(s:String) {
    writeData(dbio_getCreator_column, Option(s) )
  }

}
