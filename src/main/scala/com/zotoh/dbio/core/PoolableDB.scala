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

package com.zotoh.dbio
package core

import org.apache.commons.lang3.{StringUtils=>STU}
import java.sql.{DriverManager, SQLException, Connection}
import java.util.{Properties=>JPS}
import com.jolbox.bonecp.BoneCPConfig
import com.jolbox.bonecp.BoneCP
import com.zotoh.frwk.db.{JDBCPool,JDBCInfo,TLocalDBIO,TLocalJDBC,DBUtils}
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.CoreImplicits
import org.slf4j._


/**
 * @author kenl
 */
class PoolableDBFactory extends DBFactory {

  override def apply( ji:JDBCInfo, s:Schema, pps:JPS ): DB = {
    new PoolableDB(ji, s, pps)
  }

}

/**
 * @author kenl
 */
class PoolableDB( ji:JDBCInfo,
//  private val minConns: Int,
//  private val maxConns: Int,
//  private val maxPartitions: Int,
//  private val maxWaitForConnMillis: Long,
  s:Schema,
  pps:JPS) extends DB with CoreImplicits {

  val _log= LoggerFactory.getLogger(classOf[PoolableDB])
  val _meta= new MetaCache(s)
  private val _vendor=DBUtils.vendor(ji)

  private val _props = new JPS()
  //private var _optLock=false

  _props.putAll(pps)

  if (!STU.isEmpty(ji.user)) {
    _props.put("username", ji.user)
    _props.put("user", ji.user)
    _props.put("password", nsb( ji.pwd))
  }

  private val _pool = JDBCPool.mkSingularPool( ji, _props)

  def getProperties() = _props
  def getInfo() = ji
  def getVendor() = _vendor
  
  def open()  = _pool.nextFree
  def finz() {
    _pool.finz
  }

  def supportsOptimisticLock() = {
    if ( _props.containsKey("opt_lock") )  _props.getb("opt_lock") else true
  }

}

