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
      f ( new Transaction(c,_db) )
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
class Transaction(private val _conn : Connection, private val _db: DB ) extends SQLProc {

  val _log= LoggerFactory.getLogger(classOf[Transaction])
  val _meta =  _db.getMeta
  
  def insert(obj : DBPojo): Int = {
    doInsert(obj)
  }

  def select[T]( sql: String, params:Any* )(f: ResultSet => T ): Seq[T] = {
    new SQuery(_conn, sql, params.toSeq ).select(f)
  }

  def execute( sql: String, params:Any* ): Int = {
    new SQuery(_conn, sql, params.toSeq ).execute()
  }

  def delete( obj : DBPojo): Int = {
    doDelete(obj)    
  }

  def update(obj : DBPojo, cols : Set[String]): Int = {
    doUpdate(obj, cols)    
  }

}

