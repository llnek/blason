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

import java.util.{Date=>JDate}
import org.slf4j._

object Demo {
  private val _log= LoggerFactory.getLogger(classOf[Demo] )
}

/**
 * @author kenl
 *
 */
abstract class Demo protected(protected val _db:SQLProc) {

  def tlog() = Demo._log

  def start() {
    tlog.debug("\n")
    tlog.debug("==============================================================")
    tlog.debug("Start Demo Run: " + getClass().getName())
    tlog.run()
    tlog.debug("==============================================================")
    tlog.debug("\n")
  }

  protected def run(): Unit

  protected def fetch_employee(login:String) = {
    val rc= _db.findSome(classOf[Employee],
              new NameValues("LOGIN", login) )
    if ( rc.size == 0) null else rc(0)
  }

  protected def iniz_employee(  fname:String , lname:String, login:String) = {
    val employee= new Employee()

    employee.setBDay(new JDate )
    employee.setLogin(login) 

    employee.setFirst(fname) 
    employee.setLast(lname) 

    employee.setSalary( 100.0f ) 
    employee.setIQ(21) 
    employee.setPwd("secret") 
    employee.setSex("male") 

    employee
  }

  protected def demo_no_objects() {

    var c= _db.count( classOf[Company] )
    tlog.debug("Company count = " + c)

    c= _db.count( classOf[Department] )
    tlog.debug("Department count = " + c)

    c= _db.count( classOf[Employee] )
    tlog.debug("Employee count = " + c)

    c= _db.count( classOf[Person] )
    tlog.debug("Person count = " + c)

  }


}
