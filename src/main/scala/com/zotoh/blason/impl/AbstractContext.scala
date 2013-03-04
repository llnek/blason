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

package com.zotoh.blason
package impl

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.blason.core.Context
import com.zotoh.blason.core.Resolvable

import org.slf4j._

object AbstractContext {
  private val _log=LoggerFactory.getLogger(classOf[AbstractContext])
}

/**
 * @author kenl
 */
abstract class AbstractContext(private val _par:Context = null ) extends Context {

  private val _data= new mutable.HashMap[Any,Any] with mutable.SynchronizedMap[Any,Any]
  def tlog() = AbstractContext._log

  def get( key:Any) = {
    _data.get(key) match {
      case Some(r:Resolvable) => r.resolve(this)
      case r@Some(x) => r
      case _ => if (_par ==null) None else _par.get(key)
    }
  }

  def put( key:Any, value:Any) {
    if (value != null) {
      _data += key -> value
    }
  }

  def remove(key:Any) = {
    if (_data.isDefinedAt(key)) {
      _data.remove(key)
    } else {
      None
    }
  }

  def put(m:Map[_,_] ) {
    m.foreach { (t:Tuple2[_,_]) =>
      put(t._1,t._2)
    }
  }

  def clear() {
    _data.clear()
  }

  def dbgShow() {
    _data.foreach { (en) =>
      tlog.info("key: {} , value: {}", en._1, en._2)
    }
  }

}

