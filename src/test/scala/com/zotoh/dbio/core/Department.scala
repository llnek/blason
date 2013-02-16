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

import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.StrUtils._


/**
 * @author kenl
 *
 */
@Table(table="TBL_DEPT")
class Department extends AbstractModel {

  def dbio_getDeptID_column = "dname"
  @Column(size=128,unique=true)
  def getDeptID() = {
    readData( dbio_getDeptID_column ) match {
      case Some(v) => nsb(v)
      case _ => ""
    }
  }
  def setDeptID(s:String ) {
    writeData( dbio_getDeptID_column , Option(s))
  }

  def dbio_getEmployees_fkey= "FK_DEPTS"
  @Many2Many(rhs=classOf[Employee] ,joined=classOf[DeptEmp])
  def getEmployees( db:SQLProc) = {
    db.getM2M(this, classOf[Employee] )
  }
  def removeEmployee(db:SQLProc, e:Employee ) {
    db.unlinkM2M(this, e)
  }
  def removeEmployees(db:SQLProc) {
    db.purgeM2M(this, classOf[Employee] )
  }
  def addEmployee(db:SQLProc, e:Employee ) {
    db.linkM2M(this, e)
  }


}
