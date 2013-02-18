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


@Table(table="TBL_EMPLOYEE", uniques=Array("loginid"))
class Employee extends Person {

  def dbio_getDepts_fkey=  "FK_EMPS"
  @Many2Many(rhs=classOf[Department], joined=classOf[DeptEmp])
  def getDepts( db:SQLProc) = {
    db.getM2M(this, classOf[Department] )
  }
  def removeDepts(db:SQLProc) {
    db.purgeM2M(this, classOf[Department] )
  }
  def removeDept(db:SQLProc, d:Department ) {
    db.unlinkM2M(this, d)
  }
  def addDept(db:SQLProc, d:Department ) {
    db.linkM2M(this, d)
  }

  def dbio_getLogin_column= "login"
  @Column(size=128, index="loginid")
  def getLogin() = {
    readData( dbio_getLogin_column) match {
      case Some(s) => nsb(s)
      case _ => ""
    }
  }
  def setLogin(s:String ) {
    writeData( dbio_getLogin_column, Option(s))
  }

  def dbio_getDesc_column = "descr"
  @Column()
  def getDesc() = {
    readData( dbio_getDesc_column ) match {
      case Some(s) => nsb(s)
      case _ => ""
    }
  }
  def setDesc(n:String ) {
    writeData( dbio_getDesc_column , Option(n))
  }

  def dbio_getPwd_column = "passcode"
  @Column()
  def getPwd() = {
    readData( dbio_getPwd_column ) match {
      case Some(s) => nsb(s)
      case _ => ""
    }
  }
  def setPwd(n:String ) {
    writeData( dbio_getPwd_column , Option(n))
  }

  def dbio_getPic_column = "picture"
  @Column()
  def getPic() = {
    readData( dbio_getPic_column ) match {
      case Some(v) => javaToBytes(v)
      case _ => Array[Byte]()
    }
  }
  def setPic(b:Array[Byte]) {
    writeData( dbio_getPic_column, Option(b))
  }

  def dbio_getSalary_column = "salary"
  @Column()
  def getSalary() = {
    readData( dbio_getSalary_column ) match {
      case Some(v) => javaToSQLFloat(v)
      case _ => 0.0f
    }
  }
  def setSalary(f:Float ) {
    writeData( dbio_getSalary_column , Option(f))
  }



}


