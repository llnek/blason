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

import com.zotoh.frwk.util.StrUtils._
import com.zotoh.dbio.meta._
import com.zotoh.frwk.db.JDBCUtils._


@Table(table="TBL_COMPANY", uniques=Array("companyid"))
class Company extends AbstractModel {

  def dbio_getAddress_fkey = "FK_ADDRESS"
  @One2One(rhs=classOf[Address] )
  def getAddress(db:SQLProc) = {
    db.getO2O(this, classOf[Address], dbio_getAddress_fkey )
  }
  def setAddress(db:SQLProc, a:Address) {
    db.setO2O(this, a, dbio_getAddress_fkey  )
  }

  def dbio_getCompanyName_column = "COMPANY_ID"
  @Column(data=classOf[String],size=255, index="companyid")
  def getCompanyName() = {
    readData( dbio_getCompanyName_column ) match {
      case Some(v) => nsb(v)
      case _ => ""
    }
  }
  def setCompanyName(n:String ) {
    writeData( dbio_getCompanyName_column , Option(n))
  }

  def dbio_getRevenue_column = "revenue"
  @Column(data=classOf[Double])
  def getRevenue() = {
    readData( dbio_getRevenue_column ) match {
      case Some(v) => javaToSQLDouble(v)
      case _ => 0.0
    }
  }
  def setRevenue(d:Double ) {
    writeData( dbio_getRevenue_column , Option(d))
  }

  def dbio_getLogo_column = "logo"
  @Column(data=classOf[Array[Byte]])
  def getLogo() = {
    readData( dbio_getLogo_column ) match {
      case Some(v) => javaToBytes(v)
      case _ => Array[Byte]()
    }
  }
  def setLogo(b:Array[Byte]) {
    writeData( dbio_getLogo_column , Option(b))
  }


  def dbio_getEmployees_fkey = "FK_COMPANY"
  @One2Many(rhs=classOf[Employee])
  def getEmployees(db:SQLProc) = {
    db.getO2M(this, classOf[Employee], dbio_getEmployees_fkey )
  }
  def removeEmployee(db:SQLProc, e:Employee ) {
    db.unlinkO2M(this, e, dbio_getEmployees_fkey )
  }
  def addEmployee(db:SQLProc, e:Employee ) {
    db.linkO2M(this, e, dbio_getEmployees_fkey  )
  }


  def dbio_getDepts_fkey = "FK_COMPANY"
  @One2Many(rhs=classOf[Department] )
  def getDepts(db:SQLProc) = {
    db.getO2M(this, classOf[Department], dbio_getDepts_fkey  )
  }
  def removeDept(db:SQLProc, d:Department ) {
    db.unlinkO2M(this, d, dbio_getDepts_fkey  )
  }
  def addDept(db:SQLProc, d:Department ) {
    db.linkO2M(this, d, dbio_getDepts_fkey   )
  }

}
