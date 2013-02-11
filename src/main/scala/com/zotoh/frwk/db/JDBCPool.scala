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

import java.sql.{Connection=>JConn,Statement,SQLException}
import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.DBUtils._
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCP
import com.zotoh.frwk.util.MetaUtils



object JDBCPool {
  private val _log= LoggerFactory.getLogger(classOf[JDBCPool])
  def tlog() = _log
  
  def mkSingularPool(ji:JDBCInfo) = {
    tlog.debug("JDBCPoolMgr: Driver : {}" , ji.driver)
    tlog.debug("JDBCPoolMgr: URL : {}" ,  ji.url)    
    if (! STU.isEmpty(ji.driver )) {
      MetaUtils.forName(ji.driver)
    }
    val cpds = new BoneCPConfig()
    val v=vendor(ji)
    val ts= v.getTestSQL()
    cpds.setLogStatementsEnabled(tlog.isDebugEnabled)
    cpds.setPartitionCount(1)
    cpds.setJdbcUrl( ji.url )
    cpds.setUsername( ji.user)
    cpds.setPassword( ji.pwd )
    cpds.setIdleMaxAgeInSeconds(60*60*24) // 1 day
    cpds.setMaxConnectionsPerPartition(2)
    cpds.setMinConnectionsPerPartition(1)
    cpds.setPoolName( uid )
    cpds.setAcquireRetryDelayInMs(5000)
    cpds.setConnectionTimeoutInMs(5000)
    cpds.setDefaultAutoCommit(false)
    cpds.setAcquireRetryAttempts(1)
    new JDBCPool(v, ji, new BoneCP(cpds))
  }
  
}

/**
 * Pool management of jdbc connections.
 *
 * @author kenl
 *
 */
sealed class JDBCPool(
  private val _vendor:DBVendor, private val _info:JDBCInfo,
  private val _source:BoneCP) extends Constants with CoreImplicits {

  import JDBCPool._

  def tlog() = _log

  /**
   * @param v
   * @param p
   */
  def this(p:BoneCP ) {
    this(DBVendor.NOIDEA, new JDBCInfo(), p)
  }

  /**
   * @return
   */
  def newJdbc() = new JDBC(this)

  /**
   * @return
   */
  def info = _info

  /**
   *
   */
  def finz() {
    this.synchronized {
      _source.shutdown()
    }
  }

  /**
   * @return
   */
  def nextFree() = {
    val jc= next()
    tlog.debug("JDBCPool: Got a free jdbc connection from pool")
    jc
  }

  /**
   * @return
   */
  def varCharMaxWidth() = VARCHAR_WIDTH

  /**
   * @return
   */
  def retries() = 2

  /**
   * @return
   */
  def vendor() = _vendor

  /**
   * @param e
   * @return
   */
  private def isBadConnection(e:Exception): Boolean = {

    val sqlState = e match {
      case x:SQLException => x.getSQLState()
      case _ => ""
    }

    sqlState match {
      case "08003" | "08S01" => true
      case _ =>
        // take a guess...
        equalsOneOfIC( nsb(e.getMessage).lc,Array(
        "reset by peer",
        "aborted by peer",
        "not logged on",
        "socket write error",
        "communication error",
        "error creating connection",
        "connection refused",
        "connection refused",
        "broken pipe"
        ))
    }
  }

  /**
   * @param conn
   * @return
   */
  private def isBadConnection(conn:JConn): Boolean = {
    var rc=false
    if (conn != null) try {
      val sql = vendor() match {
        case DBVendor.ORACLE  =>  "select count(*) from user_tables"
        case DBVendor.SQLSERVER =>  "select count(*) from sysusers"
        case DBVendor.DB2 => "select count(*) from sysibm.systables"
        case DBVendor.MYSQL => "select version()"
        case DBVendor.H2 => ""
        case _ => ""
      }
      if (! STU.isEmpty(sql)) using(conn.createStatement()) { (stmt) =>
        stmt.execute(sql)
      }
    } catch {
      case e:Throwable => rc=true
    }
    rc
  }

  private def next() = {
    try {
      _source.getConnection
    } catch {
      case e:Throwable => throw mkSQLErr("No free connection")
    }
  }

}
