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
import com.zotoh.dbio.meta.Table

/**
 * @author kenl
 *
 */
class BasicO2ODemo(io:CompositeSQLr) extends Demo(io) {

  def run() {

      create_joe_bloggs_wife()

      create_joe_blogg()

      demo_attach_joe_to_wife()

      demo_verify_wedlock()

      demo_quick_delete()

      demo_no_objects()
  }


  private def create_joe_bloggs_wife() {

    _db.execWith { (tx) =>

      val person= new Person()
      person.setBDay( new SimpleDateFormat("yyyyMMdd").parse("19701220"))
      person.setFirst("Marian")
      person.setLast( "Jones")
      person.setIQ(250)
      person.setSex( "female")
      tx.insert(person)
    }
    println("Created Person: Marian Jones. OK.")
  }

  private def create_joe_blogg() {

    _db.execWith { (tx) =>

      val employee= iniz_employee("Joe", "Blogg", "jblogg")
      employee.setBDay(new SimpleDateFormat("yyyyMMdd").parse("19650202"))
      employee.setIQ(290)
      employee.setSex( "male")
      tx.insert(employee)
    }
    println("Created Employee: Joe Blogg. OK.")
  }

  private def demo_attach_joe_to_wife() {
    _db.execWith { (tx) =>

      val marian= fetch_person_object(tx, classOf[Person], "Marian", "Jones").getOrElse(null)
      val joe= fetch_person_object( tx, classOf[Employee], "Joe", "Blogg").getOrElse(null)

      joe.setSpouse(tx, marian)
      marian.setSpouse(tx, joe)

      tx.update(marian)
      tx.update(joe)

    }
    println("Joe & Marian are now married. OK.")
  }

  private def demo_verify_wedlock() {
    _db.execWith { (tx) =>
      
      val marian= fetch_person_object( tx, classOf[Person], "Marian", "Jones").getOrElse(null)
      val joe= fetch_person_object( tx, classOf[Employee], "Joe", "Blogg").getOrElse(null)

      // re-examine the spouse object for both Joe & Marian
      val j2 = marian.getSpouse(tx, classOf[Employee] )
      val p2= joe.getSpouse(tx, classOf[Person] )
      val s1=j2.get.getFirst()
      val s2=p2.get.getFirst()

      println("Marian's spouse is: " + s1)
      println("Joe's spouse is: " + s2)

      if (s1 == "Joe" && s2=="Marian") {
        println("Joe & Marian are married ? TRUE.")
      }
      else {
        println("Joe & Marian are married ? FALSE.")
      }
    }
  }

  private def demo_quick_delete() {
    _db.execWith { (tx) =>

      val marian= fetch_person_object( tx, classOf[Person], "Marian", "Jones").getOrElse(null)
      val joe= fetch_person_object( tx, classOf[Employee], "Joe", "Blogg").getOrElse(null)

      tx.delete(marian)
      tx.delete(joe)
    }
    println("Both Joe & Marian are removed. OK.")
  }

  private def fetch_person_object[T <: Person]( tx:SQLProc, z:Class[T], 
      fname:String, lname:String): Option[T] = {
    
    val f= new NameValues("first_name", fname)
    f.put("last_name",lname)
    val rc= tx.findSome(z, f)
    if ( rc.size == 0 ) None else Option( rc(0) )
    
  }

}
