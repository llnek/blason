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

import java.sql.{SQLException, Connection}
import com.zotoh.frwk.db.JDBCInfo
import org.slf4j._

import java.util.{Properties=>JPS}


/**
 * @author kenl
 */
object ScalaDB {
  def apply(fac:DBFactory) = fac
}

/**
 * @author kenl
 */
trait DBFactory {
  def apply(ji:JDBCInfo, s:Schema, props:JPS): DB
}

/**
 * @author kenl
 */
trait DB {

  protected val _meta:MetaCache
  protected val _log:Logger
  def tlog() = _log

  def newCompositeSQLProcessor() = new CompositeSQLr(this)
  def newSimpleSQLProcessor() = new SimpleSQLr(this)
  def getMeta() = _meta
  
  def close(c: Connection) {
    try {
      c.close()
    } catch {
      case e:Throwable => tlog.error("", e)
    }
  }

  def open(): Connection
  def finz(): Unit

  def getInfo(): JDBCInfo
  def getProperties(): JPS
  def supportsOptimisticLock(): Boolean
  
}
