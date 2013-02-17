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

import java.util.{Properties=>JPS}
import java.sql.Connection


/**
 * @author kenl
 */
class TLocalDBIO(private val _ji:JDBCInfo, private val _props:JPS) {
  private var _pool:JDBCPool=null
  
  def finz() {
    if (_pool != null) {
      _pool.finz
    }
    _pool=null
  }
  
  private def mkPool() = {
    if (_pool != null) {
      _pool.finz
      _pool=null
    }
    if (_ji != null) {
      _pool = JDBCPool.mkSingularPool(_ji, _props)
    }
    _pool
  }
  
  def getPool() = {
    if (_pool == null) mkPool() else _pool
  }
  
}

/**
 * @author kenl
 */
class TLocalJDBC() extends ThreadLocal[TLocalDBIO] {
  
  //override def initialValue() = new TLocalDBIO(ji,new JPS() )
  
}

