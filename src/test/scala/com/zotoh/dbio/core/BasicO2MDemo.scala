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

class BasicO2MDemo(io:CompositeSQLr) extends Demo(io) {

  def run() {

    create_one_company()

    add_employees()

    verify_employees()

    demo_cascade_delete()

    demo_no_objects()
  }

  private def create_one_company() {
    _db.execWith { (tx) =>

      val company= new Company() 
      val bits= new Array[Byte](10000)
      company.setCompanyName("ACME Web 2.0 Inc.")
      company.setRevenue(99084534.00)
      company.setLogo(bits)
      tx.insert(company) 
    }

    println("Created Company: ACME Web 2.0 Inc. OK.")
  }

  private def add_employees() {
    _db.execWith { (tx) =>

      val company= fetch_company(tx).getOrElse(null)
      // add 3 employees
      val e1= iniz_employee("No1", "Coder", "no1")
      val e2= iniz_employee("No2", "Coder", "no2")
      val e3= iniz_employee("No3", "Coder", "no3")

      company.addEmployee(tx, e1) 
      company.addEmployee(tx, e2) 
      company.addEmployee(tx, e3) 

      tx.insert(e1)
      tx.insert(e2)
      tx.insert(e3)

    }
    println("Added 3 Employees to Company. OK.")
  }

  private def verify_employees() {
    _db.execWith { (tx) =>
      val company= fetch_company(tx).get
      val employees= company.getEmployees(tx)

      val e1= employees(0)
      val e2= employees(1)
      val e3= employees(2)

      println("Company has employee: " + e3.getFirst() + ". OK.")
      println("Company has employee: " + e2.getFirst() + ". OK.")
      println("Company has employee: " + e1.getFirst() + ". OK.")
    }
  }

  private def demo_cascade_delete() {
    _db.execWith { (tx) =>

      val company = fetch_company(tx).getOrElse(null)
      val o= company.getLogo
      val bits= o

      println("Company Logo size = " + bits.length + ". OK.")

      tx.purgeO2M(company, classOf[Employee], company.dbio_getEmployees_fkey ) 
      tx.delete(company) 

    }
    println("Deleted company and its employees. OK.")
  }

  private def fetch_company(tx:Transaction) = {
    tx.findOne(classOf[Company],
            new NameValues("COMPANY_ID", "ACME Web 2.0 Inc."))
  }




}
