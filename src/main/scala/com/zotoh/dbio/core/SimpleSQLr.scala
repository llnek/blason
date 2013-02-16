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

import java.sql.{SQLException=>SQLEx, Connection, ResultSet}
import scala.collection.mutable
import org.slf4j._
import com.zotoh.frwk.db.TableMetaHolder
import com.zotoh.dbio.meta.Table
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.DBVendor
import com.zotoh.frwk.db.JDBCUtils._
import org.apache.commons.dbutils.{DbUtils=>DBU}
import java.sql.Statement



/**
 * @author kenl
*/
class SimpleSQLr(private val _db: DB) extends SQLProc {
  val _log= LoggerFactory.getLogger(classOf[SimpleSQLr])

  def findSome(fac : SRecordFactory, filter : NameValues): Seq[SRecord] = {
    //doFindSome(fac,filter)
    Nil
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
    //doFindAll(fac)
    Nil
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
    //doUpdate(obj, cols)
    0
  }

  def delete(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      //doDelete(c, obj)
      0
    }
    finally {
      _db.close(c)
    }
  }

  def insert(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      //doInsert(c, obj)
      0
    }
    finally {
      _db.close(c)
    }
  }

  def select[X]( sql: String, params: Seq[Any])(f: ResultSet => X): Seq[X] = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      new SQuery(c, sql, params).select(f)
    }
    finally {
      _db.close(c)
    }
  }

  def select[X]( sql: String)(f: ResultSet => X): Seq[X] = {
    select(sql, Nil)(f)
  }

  def execute( sql: String, params: Seq[Any]): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      new SQuery(c, sql, params).execute()
    }
    finally {
      _db.close(c)
    }
  }

  def execute( sql: String): Int = {
    execute(sql, Nil)
  }

}
