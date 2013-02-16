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
  val _meta= _db.getMeta()
  
  def update(obj : DBPojo, cols : Set[String]): Int = {
    doUpdate(obj, cols)
  }

  def delete(obj : DBPojo): Int = {
    doDelete(obj)
  }

  def insert(obj : DBPojo): Int = {
    doInsert(obj)
  }

  def select[T]( sql: String, params:Any* )(f: ResultSet => T): Seq[T] = {
    val c= _db.open
    try {
      new SQuery(c, sql, params).select(f)
    }  finally {
      _db.close(c)
    }
  }

  def execute( sql: String, params:Any* ): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      doExecute(c, sql, params:_*)
    } finally {
      _db.close(c)
    }
  }

}
