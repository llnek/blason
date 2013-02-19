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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.{Date=>JDate,TimeZone }
import com.zotoh.frwk.util.DateUtils
import java.util.GregorianCalendar


/**
 * @author kenl
 *
 */
class BasicCRUDDemo(io:CompositeSQLr) extends Demo(io) {

  def run() {

      // create an employee- Joe Bloggs
      demo_basic_object_create()

      // read back the object- Joe Bloggs
      demo_fetch_object()

      // update the object- Joe Bloggs
      demo_update_object()

      // delete the object- Joe Bloggs
      demo_delete_object()

      // failed to read back the object- Jow Bloggs is gone
      demo_no_objects()
  }

  private def demo_basic_object_create() {

    _db.execWith { (tx) =>
      val cal= new GregorianCalendar( TimeZone.getTimeZone("Europe/Paris"))
      cal.setTime(new SimpleDateFormat("yyyyMMdd hh:mm:ss").parse("19990601 13:14:15"))
      println( DateUtils.dbgCal(cal) )

      val employee=  iniz_employee("Joe", "Blogg", "jblogg") 
      employee.setBDay(cal)
      employee.setIQ(5)
      employee.setSex( "male")
      tx.insert(employee)
    }

    println("Created Employee: Joe Blogg. OK.")
  }

  private def demo_fetch_object() {

    _db.execWith { (tx) =>

      val rc= tx.findSome(classOf[Employee],
              new NameValues("LOGIN", "jblogg") )        
      if (rc.size == 0) {
          throw new Exception("Joe Blogg not in database")
      }

      println("Fetched Employee: Joe Blogg. OK.")
      println( DateUtils.dbgCal(  rc(0).getBDay ) )
      
      println("Joe Blogg's object-id is: " + rc(0).getRowID() )

    }
  }

  private def demo_update_object() {

    _db.execWith { (tx) =>

      val employee= tx.findSome(classOf[Employee],
              new NameValues("LOGIN", "jblogg") )(0)
      employee.setIQ( 25)
      tx.update(employee)
      println("Updated Employee: Joe Blogg. OK.")
    }

  }

  private def demo_delete_object() {

    _db.execWith { (tx) =>

      val employee= tx.findSome(classOf[Employee],
              new NameValues("LOGIN", "jblogg") )(0)
      tx.delete(employee)
      println("Deleted Employee: Joe Blogg. OK.")
    }
  }

  override def demo_no_objects() {
    
    _db.execWith { (tx) =>
      val rc= tx.findAll(classOf[Employee] )
      if (rc.size==0)
      println("Employee: Joe Blogg is no longer in the database. OK.")
      else
      println("Employee: database still has data. Not OK.")
    }

  }

}
