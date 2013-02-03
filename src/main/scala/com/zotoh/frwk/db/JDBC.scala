/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

package com.zotoh.frwk
package db

import scala.collection.mutable

import java.io.{IOException,InputStream,Reader}
import java.sql.{Blob,Clob,Connection,PreparedStatement,ResultSet}
import java.sql.{ResultSetMetaData,SQLException}
import java.util.{Properties=>JPS}

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.io.IOUtils

import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.JDBCUtils._
import com.zotoh.frwk.db.DBUtils._
import com.zotoh.frwk.util.CoreUtils._

object JDBC {
  private val _log= LoggerFactory.getLogger(classOf[JDBC])
}

/**
 * Higher level abstraction of a java jdbc object.
 *
 * @author kenl
 *
 */
class JDBC(private val _pool:JDBCPool) extends Constants with CoreImplicits {
  import JDBC._

  /**
   * @param tbl Table.
   * @return
   */
  def getTableMetaData(table:String):Map[String,JPS] = {
    val tbl= _pool.vendor().assureTableCase(table)
    val ret= mutable.HashMap[String,JPS]()
    val jc= _pool.nextFree
    try {
      using( jc.getMetaData.getColumns(null,null,tbl,null)) { (rset) =>
        if (rset!=null) while (rset.next()) {
          var i=rset.getInt("COLUMN_SIZE")
          val props= new JPS().add("COLUMN_SIZE",  asJObj(i))
          i=rset.getInt("DATA_TYPE")
          props.put("DATA_TYPE", asJObj(i))
          ret += rset.getString("COLUMN_NAME").uc -> props
        }
      }
    } finally {
      closeTX(jc)
    }
    ret.toMap
  }

  /**
   * Do a "select count(*) from tbl where....".
   *
   * @param tbl
   * @param where
   * @param pms
   * @return
   */
  def existRows(tbl:String, where:String, pms:Any*) = countRows(tbl, where, pms:_* ) > 0

  /**
   * Do a "select count(*) from tbl".
   *
   * @param tbl Table.
   * @return
   */
  def existRows(tbl:String) = countRows(tbl) > 0

  /**
   * Do a "select count(*) from tbl".
   *
   * @param tbl Table.
   * @return
   */
  def countRows(tbl:String):Int = countRows(tbl, "" )

  /**
   * Do a "select count(*) from tbl where....".
   *
   * @param tbl
   * @param where
   * @param params
   * @return
   */
  def countRows(tbl:String, where:String, pms:Any*):Int = {
    val sql= new SELECTStmt("COUNT(*)", tbl, where).setParams(pms:_*)
    var rc=0
    val jc= _pool.nextFree
    try {
      using(jc.prepareStatement(prepareSQL(sql))) { (stmt) =>
        (1 /: sql.params) { (cnt,a) =>
          setStatement(stmt, cnt, a )
          cnt + 1
        }
        using(stmt.executeQuery) { (rset) =>
          rc= if (rset != null && rset.next()) rset.getInt(1) else 0
        }
      }
    } finally {
      closeTX(jc)
    }
    rc
  }

  /**
   * Get &amp; prepare a connection for a transaction.
   *
   * @return
   */
  def beginTX() = {
    val c=_pool.nextFree
    c.setAutoCommit(false)
    c
  }

  /**
   * Commit the transaction bound to this connection.
   *
   * @param c
   */
  def commitTX(c:Connection) {
    if (c != null) {
      c.commit()
    }
  }

  /**
   * Rollback the transaction bound to this connection.
   *
   * @param c
   */
  def cancelTX(c:Connection) {
    if (c != null) {
      c.rollback()
    }
  }

  /**
   * Close the transaction.  The connection SHOULD not be used afterwards.
   *
   * @param c
   */
  def closeTX(c:Connection) {
    if (c != null) {
      c.close()
    }
  }

  /**
   * Do a "select * ...".
   *
   * @param sql
   * @return
   */
  def fetchOneRow(sql:SELECTStmt): Option[DBRow] = {
    val rows= fetchRows(sql)
    if (rows.length == 0) None else Some(rows(0))
  }

  /**
   * Do a "select * ...".
   *
   * @param sql
   * @return
   */
  def fetchRows(sql:SELECTStmt): Seq[DBRow] = {
    val jc = _pool.nextFree
    try {
      selectXXX(jc, sql)
    } finally {
      closeTX(jc)
    }
  }

  /**
   * Do a "delete from ...".
   *
   * @param jc
   * @param sql
   * @return
   */
  def deleteRows(jc:Connection, sql:DELETEStmt) = delete(jc, sql)

  /**
   * Do a "delete from ...".
   *
   * @param sql
   * @return
   */
  def deleteRows(sql:DELETEStmt): Int = {
    val jc = beginTX()
    try {
      val rc=delete(jc, sql)
      commitTX(jc)
      rc
    } catch {
      case e:Throwable => cancelTX(jc); throw e
    } finally {
      closeTX(jc)
    }
  }

  /**
   * Do a "insert into ...".
   *
   * @param jc
   * @param row
   */
  def insertOneRow(jc:Connection, row:DBRow) { insert(jc, row) }

  /**
   * Do a "insert into ...".
   *
   * @param row
   * @return
   */
  def insertOneRow(row:DBRow): Int = {
    val jc= beginTX()
    try  {
      val rc= insert(jc, row)
      commitTX(jc)
      rc
    } catch {
      case e:Throwable => cancelTX(jc); throw e
    } finally {
      closeTX(jc)
    }
  }

  /**
   * Do a "update set...".
   *
   * @param jc
   * @param row
   * @param where
   * @param pms
   * @return
   */
  def updateOneRow(jc:Connection, row:DBRow, where:String, pms:Any*): Int = update(jc, row, where, pms:_*)

  /**
   * Do a "update set...".
   *
   * @param row
   * @param where
   * @param pms
   * @return
   */
  def updateOneRow(row:DBRow, where:String, pms:Any*): Int = {
    val jc= beginTX()
    try {
      val rc= update(jc, row, where, pms:_*)
      commitTX(jc)
      rc
    } catch {
      case e:Throwable => cancelTX(jc); throw e
    } finally {
      closeTX(jc)
    }
  }

  def update(sql:UPDATEStmt) = {
    using( _pool.nextFree ) { (con) =>
      using(con.prepareStatement(prepareSQL(sql))) { (stmt) =>
        (1 /: sql.params) { (pos, a) =>
          setStatement( stmt, pos, a)
          pos + 1
        }
        stmt.executeUpdate
      }
    }
  }

  def update(jc:Connection,sql:UPDATEStmt) = {
    using(jc.prepareStatement( prepareSQL(sql)) ) { (stmt) =>
      (1 /: sql.params) { (pos, a) =>
        setStatement( stmt, pos, a)
        pos + 1
      }
      stmt.executeUpdate
    }
  }

  def call(isFunc:Boolean, name:String, outs: () => Array[Class[_]], pms:Any* ) = {
    using( _pool.nextFree ) { (con) =>
      DBUtils.call(isFunc, con,name,outs, pms:_* )
    }
  }

  private def update(jc:Connection, row:DBRow, where:String, pms:Any*): Int = {
    update(jc, new UPDATEStmt(row, where,pms:_*) )
  }

  private def buildRows(tbl:String, rset:ResultSet) = {
    val lst= mutable.ArrayBuffer[DBRow]()
    if (rset != null) while ( rset.next)  {
      lst += buildOneRow(tbl, rset)
    }
    tlog.debug("{}: Fetched from table: \"{}\" : rows= {}" , "JDBC", tbl, asJObj(lst.size) )
    lst.toSeq
  }

  private def buildOneRow(tbl:String, rset:ResultSet) = {
    val meta= rset.getMetaData()
    val row= new DBRow(tbl)

    (1 to meta.getColumnCount ).foreach { (i) =>
      var obj=rset.getObject(i)
      var inp = obj match {
        case bb:Blob => bb.getBinaryStream()
        case s:InputStream => s
        case _ => null
      }
      if (inp != null) using(inp) { (inp) =>
        obj= IOUtils.readBytes(inp)
      }
      var rdr = obj match {
        case cc:Clob => cc.getCharacterStream()
        case r:Reader => r
        case _ => null
      }
      if (rdr != null) using(rdr) { (rdr) =>
        obj= IOUtils.readChars( rdr)
      }

      row.add( meta.getColumnName(i).uc, obj)
    }

    row
  }

  private def prepareSQL(stmt:SQLStmt) = {
    val sql= STU.trim(stmt.toString)
    val v= _pool.vendor
    val rc= sql.lc match {
      case s if (s.startsWith("select")) => v.tweakSELECT(sql)
      case s if (s.startsWith("update")) => v.tweakUPDATE(sql)
      case s if (s.startsWith("delete")) => v.tweakDELETE(sql)
      case _ => sql
    }
    //tlog().debug(rc)
    rc
  }

  private def selectXXX(jc:Connection, sql:SELECTStmt): Seq[DBRow] = {
    using(jc.prepareStatement(prepareSQL(sql))) { (stmt) =>
      (1 /: sql.params) { (pos,a) =>
        setStatement( stmt, pos, a )
        pos + 1
      }
      using(stmt.executeQuery) { (rset) =>
        buildRows(sql.table, rset)
      }
    }
  }

  private def delete(jc:Connection, sql:DELETEStmt): Int = {
    using(jc.prepareStatement(prepareSQL(sql))) { (stmt) =>
      (1 /: sql.params) { (pos, a) =>
        setStatement( stmt, pos, a)
        pos + 1
      }
      stmt.executeUpdate
    }
  }

  private def insert(jc:Connection, row:DBRow): Int = {
    val sql= new INSERTStmt(row)
    using(jc.prepareStatement(prepareSQL(sql))) { (stmt) =>
      ( 1 /: sql.params) { (pos, a) =>
        setStatement( stmt, pos, a)
        pos + 1
      }
      stmt.executeUpdate
    }
  }

  def tlog() = _log

}


