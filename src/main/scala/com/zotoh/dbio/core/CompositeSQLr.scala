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
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.db.DBVendor
import com.zotoh.frwk.db.JDBCUtils._
import org.apache.commons.dbutils.{DbUtils=>DBU}
import java.sql.Statement
import org.slf4j._



/**
 * @author kenl
 */
object CompositeSQLr {
  private val _log= LoggerFactory.getLogger( classOf[ CompositeSQLr] )
}

/**
 * @author kenl
 */
class CompositeSQLr(private val _db : DB) {

  def tlog() = CompositeSQLr._log

  def execWith[T](f: Transaction => T) = {
    val c= begin
    try {
      f ( new Transaction(c) )
      commit(c)
    } catch {
      case e: Throwable => { rollback(c) ; tlog.warn("",e) }
    } finally {
      close(c)
    }
  }

  private def rollback(c :Connection) {
    block { () => c.rollback() }
  }

  private def commit(c : Connection) {
    c.commit()
  }

  private def begin(): Connection = {
    val c= _db.open
    c.setAutoCommit(false)
    c
  }

  private def close(c: Connection) {
    block { () => c.close() }
  }

}

/**
 * @author kenl
 */
class Transaction(private val _conn : Connection ) extends SQLProcessor {

  val _log= LoggerFactory.getLogger(classOf[Transaction])

  def insert(obj : SRecord): Int = {
//    doInsert(_conn, obj)
    0
  }

  def select[X]( sql: String, params: Seq[Any])(f: ResultSet => X): Seq[X] = {
    new SQuery(_conn, sql, params ).select(f)
  }

  def select[X]( sql: String)(f: ResultSet => X): Seq[X] = {
    select(sql, Nil) (f)
  }

  def execute( sql: String, params: Seq[Any]): Int = {
    new SQuery(_conn, sql, params ).execute()
  }

  def execute( sql: String): Int = {
    execute(sql, Nil)
  }

  def delete( obj : SRecord): Int = {
//    doDelete(_conn, obj)
    0
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
//    doUpdate(obj, cols)
    0
  }

  def findSome(fac : SRecordFactory, filter : NameValues): Seq[SRecord] = {
//    doFindSome(fac,filter)
    Nil
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
//    doFindAll(fac)
    Nil
  }

}

