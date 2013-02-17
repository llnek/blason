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
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.dbio.core.DBPojo._

/**
 * @author kenl
 *
 */
class BasicSQLDemo(io:CompositeSQLr) extends Demo(io) {

  def run() {

    demo_create_via_sql()

    demo_fetch_via_sql()

    demo_delete_via_sql()

    demo_no_objects()

    demo_call_func()

    demo_call_proc()

  }

  private def demo_create_via_sql() {

    _db.execWith { (tx) =>

      var sql= "insert into TBL_PERSON (FIRST_NAME,LAST_NAME,IQ,BDAY,SEX,$VER) VALUES (?,?,?,?,?,?)"
      sql= STU.replace(sql, "$VER", COL_VERID)

      tx.execute(sql, "John", "Smith", 195, new JDate(), "male", 1L)
      tx.execute(sql, "Mary", "Smith", 150, new JDate(), "female", 1L )
    }
    tlog.debug("Create John & Mary Smith(s). OK.")
  }

  private def demo_fetch_via_sql() {

    _db.execWith { (tx) =>
//      val sql= "select FIRST_NAME,IQ from TBL_PERSON"
      val lst = tx.findSome(classOf[Person])
      val f1= lst(0).getFirst()
      val f2= lst(1).getFirst()
      val n1= lst(0).getIQ
      val n2= lst(1).getIQ
      tlog.debug("Person (1) = " + f1 + ", IQ = " + n1) 
      tlog.debug("Person (2) = " + f2 + ", IQ = " + n2) 
    }

  }

  private def demo_delete_via_sql() {

    _db.execWith { (tx) =>
      val sql= "delete from TBL_PERSON where IQ=?"
      tx.execute(sql, 195)
      tx.execute(sql, 150)
    }
  }

  private def demo_call_func() {
  }

  private def demo_call_proc() {
  }






}
