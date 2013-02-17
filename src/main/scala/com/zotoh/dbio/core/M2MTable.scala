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

import com.zotoh.dbio.meta.Column
import com.zotoh.dbio.meta.Table


/**
 * @author kenl
 *
 */

abstract class M2MTable extends AbstractModel {
  import DBPojo._

  def dbio_getLHS_column = COL_LHS
  @Column( unique=true)
  def getLHS() = {
    readData( dbio_getLHS_column ) match {
      case Some(s:String) => s
      case _ => ""
    }
  }
  def setLHS(s:String ) {
    writeData( dbio_getLHS_column, Option(s))
  }

  def dbio_getRHS_column = COL_RHS
  @Column( unique=true)
  def getRHS() = {
    readData( dbio_getRHS_column ) match {
      case Some(s:String) => s
      case _ => ""
    }
  }
  def setRHS(s:String ) {
    writeData( dbio_getRHS_column, Option(s))
  }

  def dbio_getLHSObjID_column = COL_LHSOID
  @Column(unique=true)
  def getLHSObjID() = {
    readData( dbio_getLHSObjID_column ) match {
      case Some(n:Long) => n
      case _ => -1L
    }
  }
  def setLHSObjID(id:Long) {
    writeData( dbio_getLHSObjID_column, Option(id) )
  }

  def dbio_getRHSObjID_column = COL_RHSOID
  @Column(unique=true)
  def getRHSObjID() = {
    readData( dbio_getRHSObjID_column ) match {
      case Some(n:Long) => n
      case  _ => -1L
    }
  }
  def setRHSObjID(id:Long ) {
    writeData( dbio_getRHSObjID_column , Option(id) )
  }


}
