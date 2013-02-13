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
import com.zotoh.frwk.db.{JDBCPool,JDBCInfo,TLocalDBIO,TLocalJDBC}
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._
import org.slf4j._

/**
 * @author kenl
 */
class SimpleDBFactory extends DBFactory {
  def apply(ji:JDBCInfo, dbg:Boolean=false): DB = new SimpleDB(ji,dbg)
}

object SimpleDB {
  private var _jdb = new TLocalJDBC()
}

/**
 * @author kenl
 */
class SimpleDB(ji:JDBCInfo, dbg:Boolean=false) extends DB {
  import SimpleDB._
  val _log= LoggerFactory.getLogger(classOf[SimpleDB])
  private val _props = new JPS()
  if (!STU.isEmpty(ji.user)) {
    _props.put("username", ji.user)
    _props.put("user", ji.user)
    _props.put("password", nsb( ji.pwd))
  }
  if (dbg) { _props.put("debug", java.lang.Boolean.TRUE) }

  def finz() {
  }

  def open() = {
    Option(_jdb.get) orElse { _jdb.set(new TLocalDBIO(ji,_props)) ; Option(_jdb.get) } match {
      case Some(x:TLocalDBIO) => x.getPool.nextFree
      case _ => throw new SQLException("No available sql-connection.")
    }
  }

}


/**
 * @author kenl
 */
class PoolableDBFactory( private val minConns: Int,
  private val maxConns: Int,
  private val maxWaitForConnMillis: Long = 5000,
  private val dbg:Boolean=false) extends DBFactory {
  val _log= LoggerFactory.getLogger(classOf[PoolableDBFactory])
  override def apply(ji:JDBCInfo, dbg:Boolean): DB = {
    new PoolableDB(ji, minConns, maxConns, 1, maxWaitForConnMillis,dbg)
  }

}

/**
 * @author kenl
 */
class PoolableDB( ji:JDBCInfo,
  private val minConns: Int,
  private val maxConns: Int,
  private val maxPartitions: Int,
  private val maxWaitForConnMillis: Long,
  private val dbg:Boolean) extends DB {
  
  val _log= LoggerFactory.getLogger(classOf[PoolableDB])
  
  private val _props = new JPS()
  if (!STU.isEmpty(ji.user)) {
    _props.put("username", ji.user)
    _props.put("user", ji.user)
    _props.put("password", nsb( ji.pwd))
  }
  if (dbg) { _props.put("debug", java.lang.Boolean.TRUE) }

  private val _pool = JDBCPool.mkSingularPool( ji, minConns,maxConns,1,maxWaitForConnMillis,_props)

  def open()  = _pool.nextFree
  def finz() {
    _pool.finz
  }

}
