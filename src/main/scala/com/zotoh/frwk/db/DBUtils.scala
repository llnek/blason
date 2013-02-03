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

import java.sql.{Connection,DatabaseMetaData=>DBMD,Driver,DriverManager,PreparedStatement=>PPS}
import java.sql.{ResultSet=>RSET,ResultSetMetaData=>RSMD,SQLException,Statement}

import java.util.{Properties=>JPS}

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.db.DBVendor._
import com.zotoh.frwk.db.JDBCUtils._
import org.slf4j._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.dbutils.{DbUtils=>DBU}


/**
 * Helper db functions.
 *
 * @author kenl
 *
 */
object DBUtils extends Constants with CoreImplicits {

  private val _log= LoggerFactory.getLogger(classOf[DBUtils])
  def tlog() = _log

  /**
   * @param jp
   * @return
   */
  def mkConnection(jp:JdbcInfo) = {
/*
    Class<?> c= loadDriver(jp.getDriver());
    if (c == null) {
      throw new SQLException("Failed to load jdbc-driver class: " + jp.getDriver() ) ;
    }
            */
    val con= if (STU.isEmpty(jp.user)) DriverManager.getConnection(jp.url) else safeGetConn(jp)
    if (con == null) {
      throw mkSQLErr("Failed to create db connection: " +
        jp.url )
    }
    con.setTransactionIsolation( jp.isolation )
    con
  }

  /**
   * @param jp
   */
  def testConnection(jp:JdbcInfo) {
    DBU.closeQuietly( mkConnection(jp))
  }

  /**
   * @param jp
   * @return
   */
  def vendor(jp:JdbcInfo):DBVendor = {
    using( mkConnection(jp)) { (con) => vendor(con) }
  }

  def vendor(con:Connection):DBVendor = {
    val md= con.getMetaData()
    val name= md.getDatabaseProductName()
    val v= md.getDatabaseProductVersion()
    maybeGetVendor(name) match {
      case v@DBVendor.NOIDEA => v
      case dbv =>
        dbv.productName= name
        dbv.productVer= v
        dbv.setCase(md.storesUpperCaseIdentifiers(),
          md.storesLowerCaseIdentifiers(),
          md.storesMixedCaseIdentifiers())
        dbv
    }
  }

  /**
   * @param jp
   * @param table
   * @return
   */
  def tableExists(jp:JdbcInfo, table:String):Boolean = {
    using(mkConnection(jp)) { (con) => tableExists(con,table) }
  }

  def tableExists(con:Connection, table:String):Boolean = {
    var ok=false
    try {
      val mt= con.getMetaData()
      val tbl= nsb(table) match {
        case s if mt.storesUpperCaseIdentifiers() => s.uc
        case s if mt.storesLowerCaseIdentifiers() => s.lc
        case s => s
      }
      using(mt.getColumns(null,null, tbl, null)) { (res) =>
        if (res != null && res.next()) { ok=true }
      }
    } catch {
      case e:SQLException => ok=false
      case e:Throwable => throw e
    }
    ok
  }

  /**
   * @param jp
   * @param table
   * @return
   */
  def rowExists(jp:JdbcInfo, table:String):Boolean = {
    using(mkConnection(jp)) { (con) =>
      rowExists(con,table)
    }
  }

  def rowExists(con:Connection, table:String):Boolean = {
    var ok=false
    try {
      val sql="SELECT COUNT(*) FROM  " + nsb(table).uc
      using(con.createStatement) { (stm) =>
      using(stm.executeQuery(sql)) { (res) =>
        if (res != null && res.next()) { ok = res.getInt(1) > 0 }
      }}
    } catch {
      case e:SQLException => ok=false
      case e:Throwable => throw e
    }
    ok
  }

  /**
   * @param jp
   * @param sql
   * @return
   */
  def firstRow(jp:JdbcInfo, sql:String):Option[DBRow] = {
    using(mkConnection(jp)) { (con) =>
      firstRow(con,sql)
    }
  }

  def firstRow(con:Connection, sql:String):Option[DBRow] = {
    var row:DBRow = null
    using(con.createStatement()) { (stm) =>
    using(stm.executeQuery(sql)) { (res) =>
      if (res != null && res.next) {
        val md= res.getMetaData
        row= new DBRow()
        (1 to md.getColumnCount).foreach { (pos) =>
          row.add(md.getColumnName(pos), res.getObject(pos))
        }
      }
    }}
    if (row==null) None else Some(row)
  }

def call(isFunc:Boolean, con:Connection, name:String, outs: () => Array[Class[_]], pms:Any* ) = {
    val rc= mutable.ArrayBuffer[Any]()
    val w= new StringBuilder(256)
    pms.foreach { (p) => if ( p != null) { addAndDelim(w, ",", "?" ) } }
    val sql =  " { ?= CALL " +  name + "(" + w +  ") }"
    val cs= con.prepareCall(sql)
    var inc=1
    var rs:RSET=null
    try {
      pms.foreach { (v) =>
        if (v != null) {
          setStatement(cs, inc, v)
          inc += 1
        }
      }
      inc=1
      outs().foreach { (z) =>
        if (z != null) {
          cs.registerOutParameter( inc, toSqlType( z))
          inc += 1
        }
      }
      cs.execute()
      rs = cs.getResultSet()
      // add any function returned value
      if (rs != null && rs.next()) {
          rc += rs.getObject(1) 
      }
      // add out params
      inc=1
      outs().foreach { (z) =>
        if (z != null) {
          rc += cs.getObject( inc )
          inc += 1
        }
      }
      rc
    } finally {
      DBU.close(rs)
      DBU.close(cs)
    }
  }

  def nocaseMatch(col:String, v:String) = {
    " UPPER(" + nsb(col) + ")" + " LIKE" + " UPPER('" + nsb(v) + "') "
  }

  def likeMatch(col:String, v:String) = {
    nsb(col) + " LIKE" + " '" + nsb(v) + "' "
  }

  def wildcardMatch(col:String, filter:String) = {
    " UPPER(" + nsb(col) + ")" +
    " LIKE" +
    " UPPER('" + nsb( STU.replace(  nsb(filter), "*", "%"))  + "') "
  }

  def loadDriver(s:String, cl:ClassLoader): Class[_] = {
    try {
      Class.forName(s,true, cl)
    } catch {
      case e:Throwable => throw mkSQLErr("Drive class not found: " + s)
    }
  }

  def loadDriver(s:String): Class[_] = {
    loadDriver(s, Thread.currentThread().getContextClassLoader)
  }

  def loadTableMeta( jp:JdbcInfo, table:String):Option[TableMetaHolder]  = {
    using(mkConnection(jp)) { (con) =>
      maybeLoadTable(con,table)
    }
  }

  def maybeLoadTable( con:Connection, table:String):Option[TableMetaHolder]  = synchronized {
    if ( tableExists(con, table)) {
      loadTableMeta(con,table)
    } else {
      None
    }
  }

  def loadTableMeta( con:Connection, table:String):Option[TableMetaHolder]  =  {
    val m= con.getMetaData()
    val dbv= vendor(con)
    val catalog=null
    val schema= dbv match {
      case DBVendor.ORACLE => "%"
      case _ => null
    }

    // not good, try mixed case... arrrrrrrrrrhhhhhhhhhhhhhh
    //rs = m.getTables( catalog, schema, "%", null)
    loadColumns(m, catalog, schema, dbv.assureTableCase(table) )
  }

  private def loadColumns(m:DBMD, catalog:String, schema:String, table:String) = {
    val keys= mutable.HashSet[String]()
    val tm= new TableMetaHolder(table)

    using(m.getPrimaryKeys(catalog, schema, table)) { (rs) =>
      while ( rs != null && rs.next()) {
        keys.add( rs.getString(4).uc )
      }
    }

    using( m.getColumns( catalog, schema, table, "%") ) { (rs) =>
      while ( rs != null && rs.next()) {
        val opt= rs.getInt(11) != DBMD.columnNoNulls
        val cn = rs.getString(4).uc
        val ctype = rs.getInt(5)
        val c= new ColMetaHolder(cn, ctype, opt)
        tm.addCol(c, keys.contains(cn))
      }
    }

    tm.setGetGeneratedKeys( m.supportsGetGeneratedKeys)
    tm.setTransact( m.supportsTransactions)

    Some(tm)
  }

  /**
   * Get a connection.
   *
   * @param z the driver class.
   * @param jp
   * @return
   */
  private def safeGetConn(jp:JdbcInfo) = {
    val props=new JPS()
    var d:Driver = if (STU.isEmpty(jp.url)) null else {
      DriverManager.getDriver(jp.url)
    }

    if (d==null) {
      throw mkSQLErr("Can't load Jdbc Url : " + jp.url)
    }

    if ( ! STU.isEmpty(jp.driver))  {
      var dz=d.getClass().getName()
      if ( jp.driver != dz) {
        tlog().warn("{}: Expected: {}, loaded with driver: {}", "DBUtils",  jp.driver, dz)
      }
    }

    if ( ! STU.isEmpty(jp.user)) {
      props.add("password", jp.pwd).
      add("username", jp.user).
      add("user", jp.user)
      //setProps(u, props)
    }

    d.connect(jp.url, props)
  }

  //private def setProps(url:String, props:Properties) :Unit = {}

  private def maybeGetVendor(product:String) = {
    nsb(product).lc match {
      case s if (s.has("microsoft")) => SQLSERVER
      case s if (s.has("hypersql")) => HSQLDB
      case s if (s.has("hsql")) => HSQLDB
      case s if (s.has("h2")) => H2
      case s if (s.has("oracle")) => ORACLE
      case s if (s.has("mysql")) => MYSQL
      case s if (s.has("derby")) => DERBY
      case s if (s.has("postgresql")) => POSTGRESQL
      case _ => DBVendor.NOIDEA
    }
  }

  def mkSQLErr(msg:String) = new SQLException(msg)

}

sealed class DBUtils {}
