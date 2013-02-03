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

import java.sql.{Driver,DriverManager,SQLException,Connection=>JConn}
import java.util.{Properties=>JPS}

import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._

import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCP

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.WWID
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.frwk.db.DBUtils._

object JDBCPoolMgr {
  private val _log = LoggerFactory.getLogger(classOf[JDBCPoolMgr])
}

/**
 * @author kenl
 *
 */
sealed class JDBCPoolMgr extends Constants with CoreImplicits {

  import JDBCPoolMgr._
  def tlog() = _log

  private var _ps= mutable.HashMap[String,JDBCPool]()

  /**
   * @param pl
   * @param pms
   * @param pps
   * @return
   */
  def mkPool(pl:String, pms:JdbcInfo, pps:JPS) = create(pl, pms, pps)

  /**
   * @param pms
   * @param pps
   * @return
   */
  def mkPool(pms:JdbcInfo, pps:JPS) = create( uid(), pms, pps)

  /**
   * @param param
   * @return
   */
  def mkPool(pms:JdbcInfo):JDBCPool = mkPool( uid(), pms)

  /**
   * @param pl
   * @param pms
   * @return
   */
  def mkPool(pl:String, pms:JdbcInfo) = {
    create(pl, pms,
      new JPS().add("username", pms.user).
      add("user", pms.user).
      add("password", pms.pwd) )
  }

  private def create(pl:String, pms:JdbcInfo, pps:JPS) = synchronized {

    tlog.debug("JDBCPoolMgr: Driver : {}" , pms.driver)
    tlog.debug("JDBCPoolMgr: URL : {}" ,  pms.url)

    if (existsPool(pl)) {
      throw mkSQLErr("Jdbc Pool already exists: " + pl)
    }

    if (! STU.isEmpty(pms.driver )) {
      MetaUtils.forName(pms.driver)
    }

    val cpds = new BoneCPConfig()
    val v=vendor(pms)
    val ts= v.getTestSQL()

    cpds.setLogStatementsEnabled(tlog.isDebugEnabled)
    cpds.setPartitionCount(4)
    cpds.setJdbcUrl( pms.url )
    cpds.setUsername( pms.user)
    cpds.setPassword( pms.pwd )

    cpds.setIdleMaxAgeInSeconds(60*60*24) // 1 day
    cpds.setMaxConnectionsPerPartition(5)
    cpds.setMinConnectionsPerPartition(2)
    //cpds.setAcquireIncrement(2)
    cpds.setPoolName(pl)

    if (! STU.isEmpty(ts)) {
      cpds.setConnectionTestStatement(ts)
    }

    // cpds is now a fully configured and usable pooled DataSource

    val j= new JDBCPool(vendor(pms), pms, new BoneCP(cpds))
    _ps.put(pl, j)
    tlog.debug("{}: Added db pool: {}, info= {}", "JDBCPoolMgr", pl, pms)
    j

  }

  /**
   *
   */
  def finz() {
    this.synchronized {
      _ps.foreach ( t => t._2.finz )
      _ps.clear
    }
  }

  /**
   * @param n
   * @return
   */
  def existsPool(n:String) = _ps.isDefinedAt(nsb(n))

  /**
   * @param n
   * @return
   */
  def getPool(n:String) = _ps.get(nsb(n))

}
