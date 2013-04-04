/*??
 * COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import java.sql.{Timestamp=>JTS}

object DBAction extends Enumeration {
  type DBAction= Value
  val INSERT = Value(0, "INSERT")
  val DELETE = Value(1, "DELETE")
  val UPDATE = Value( 2, "UPDATE")
  val QUERY = Value(3, "QUERY")
}

object DBPojo {

  val COL_ROWID= "DBIO_ROWID"
  val COL_VERID= "DBIO_VERID"
  val COL_RHS= "DBIO_RHS"
  val COL_LHS= "DBIO_LHS"
  val COL_RHSOID= "DBIO_RHSOID"
  val COL_LHSOID= "DBIO_LHSOID"

}

trait DBPojo {

  import DBAction._
  
  def setRowID(n:Long ): Unit
  def setVerID(n:Long): Unit

  def getRowID(): Long
  def getVerID(): Long

  def set(field:String, value: Option[Any] ): Unit
  def get(field:String): Option[Any]

  def setLastModified(dt:JTS): Unit
  def getLastModified(): JTS

  def postEvent(db:SQLProc, act:DBAction ):Unit
  def preEvent(db:SQLProc, act:DBAction):Unit
  
}


