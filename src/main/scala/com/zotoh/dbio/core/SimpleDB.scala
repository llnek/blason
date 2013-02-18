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
import com.zotoh.frwk.util.CoreImplicits
import org.slf4j._

/**
 * @author kenl
 */
class SimpleDBFactory extends DBFactory {
  def apply(ji:JDBCInfo, s:Schema, ps:JPS): DB = new SimpleDB(ji,s, ps)
}

object SimpleDB {
  private var _jdb = new TLocalJDBC()
}

/**
 * @author kenl
 */
class SimpleDB(ji:JDBCInfo, s:Schema, pps:JPS) extends DB with CoreImplicits {
  import SimpleDB._
  val _log= LoggerFactory.getLogger(classOf[SimpleDB])
  val _meta= new MetaCache(s)
  private val _props = new JPS()
  _props.putAll(pps)
  
  if (!STU.isEmpty(ji.user)) {
    _props.put("username", ji.user)
    _props.put("user", ji.user)
    _props.put("password", nsb( ji.pwd))
  }

  def supportsOptimisticLock() = {
    true || pps.getb("opt_lock")
  }
  
  def getProperties() = _props
  def getInfo() = ji
  
  def finz() {
    Option(_jdb.get) match {
      case Some(x:TLocalDBIO) => x.finz
      case _ =>
    }
  }

  def open() = {
    Option(_jdb.get) orElse { _jdb.set(new TLocalDBIO(ji,_props)) ; Option(_jdb.get) } match {
      case Some(x:TLocalDBIO) => x.getPool.nextFree
      case _ => throw new SQLException("No available sql-connection.")
    }
  }

}



