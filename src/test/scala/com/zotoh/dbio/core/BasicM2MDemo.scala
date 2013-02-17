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


/**
 * @author kenl
 *
 */
class BasicM2MDemo(io:CompositeSQLr) extends Demo(io) {

  def run() {

      create_company()

      add_depts()

      add_employees()

      bind_dept_employees()

      bind_employee_depts()

      verify_m2m()

      cleanup()

      demo_no_objects()
  }

  private def create_company() {
    _db.execWith { (tx) =>

      val company= new Company()
      company.setCompanyName("ACME Web 2.0 Inc.")
      tx.insert(company) 
      println("Created company: " + company.getCompanyName() + " OK.")
    }
  }


  private def add_depts() {
    _db.execWith { (tx) =>

      val company= fetch_company(tx)
      val d1= new Department(); d1.setDeptID("Finance")
      val d2= new Department(); d2.setDeptID("Sales")
      val d3= new Department(); d3.setDeptID("Marketing")
      tx.insert(d1)
      tx.insert(d2)
      tx.insert(d3)

      company.addDept(tx, d1) 
      company.addDept(tx, d2) 
      company.addDept(tx, d3) 
      tx.update(d1)
      tx.update(d2)
      tx.update(d3)
      tx.update(company)

      println("Added 3 departments to Company. OK.")
    }
  }


  private def add_employees() {
    _db.execWith { (tx) =>

      val company= fetch_company(tx)

      val e1= iniz_employee( "No1", "Coder", "no1")
      val e2= iniz_employee( "No2", "Coder", "no2")
      val e3= iniz_employee("No3", "Coder", "no3")
      tx.insert(e1)
      tx.insert(e2)
      tx.insert(e3)

      company.addEmployee(tx, e1)
      company.addEmployee(tx, e2)
      company.addEmployee(tx, e3)

      tx.update(e1)
      tx.update(e2)
      tx.update(e3)
      tx.update(company)

      println("Added 3 employees to Company. OK.")
    }
  }


  private def bind_dept_employees() {
    _db.execWith { (tx) =>

      val dept= fetch_dept(tx,"Finance")

      // get the many2many association to link to employees

      val e1= fetch_emp(tx,"no1")
      val e2= fetch_emp(tx,"no2")
      val e3= fetch_emp(tx,"no3")

      dept.addEmployee(tx, e1) 
      dept.addEmployee(tx, e2) 
      dept.addEmployee(tx, e3) 
      
      println("Finance department now has 3 members. OK.")
    }
  }

  private def bind_employee_depts() {
    _db.execWith { (tx) =>

      // get the many2many association and bind to some departments
      val emp= fetch_emp(tx,"no3") 

      val d1= fetch_dept(tx,"Marketing")
      val d2= fetch_dept(tx,"Sales")
      val d3= fetch_dept(tx,"Finance")

      emp.addDept(tx, d1)
      emp.addDept(tx, d2)
      emp.addDept(tx, d3)

      println("Employee No3 now belongs to 3 departments. OK.")
    }
  }


  private def verify_m2m() {
//        val company= fetch_company()
    _db.execWith { (tx) =>
      val dept = fetch_dept(tx,"Finance") 
      val emps= dept.getEmployees(tx)
      if (emps.size==3) {
        println("Finance department indeed has 3 members. OK.")
      }
      emps.find { _.getLogin == "no3" } match {
        case Some(e3) =>
          val depts= e3.getDepts(tx)
          if ( depts.size == 3) {
            println("No3 Coder indeed belongs to 3 departments. OK.")
          }
        case _ =>
      }
    }
  }

  private def fetch_emp(tx:Transaction, login:String) = {
      val rc= tx.findSome(classOf[Employee], new NameValues("LOGIN", login))
      if (rc.size==0) null else rc(0)
  }

  private def fetch_dept( tx:Transaction, name:String ) = {
      val rc= tx.findSome(classOf[Department], new NameValues("DNAME", name))
      if (rc.size==0) null else rc(0)
  }

  private def cleanup()    {
    _db.execWith { (tx) =>

      val company= fetch_company(tx)
      val depts= company.getDepts(tx)
      depts.foreach { (d) =>
          d.removeEmployees(tx)
      }
      val emps= company.getEmployees(tx)
      emps.foreach { (e) =>
          e.removeDepts(tx)
      }

      tx.purgeO2M(company, classOf[Department], company.dbio_getDepts_fkey )
      tx.purgeO2M(company, classOf[Employee], company.dbio_getEmployees_fkey)

      tx.purge(classOf[Person] )
      tx.delete(company) 
    }
  }

  private def fetch_company(tx:Transaction) = {
      val rc = tx.findSome(classOf[Company], 
        new NameValues("COMPANY_ID", "ACME Web 2.0 Inc."))
      if (rc.size == 0 ) null else rc(0)
  }

}
