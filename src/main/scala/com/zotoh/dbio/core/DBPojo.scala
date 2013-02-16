/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

object DBPojo {

  val COL_ROWID= "DBIO_ROWID"
  val COL_VERID= "DBIO_VERID"
  val COL_RHS= "DBIO_RHS"
  val COL_LHS= "DBIO_LHS"
  val COL_RHSOID= "DBIO_RHSOID"
  val COL_LHSOID= "DBIO_LHSOID"

  
}

trait DBPojo {

  def setRowID(n:Long ): Unit
  def setVerID(n:Long): Unit

  def getRowID(): Long
  def getVerID(): Long
  
  def set(field:String, value: Option[Any] ): Unit
  def get(field:String): Option[Any]
  
}

