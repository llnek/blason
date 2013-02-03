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

package com.zotoh.blason
package io

import com.zotoh.blason.core.Configuration
import com.zotoh.blason.util.Observer
import scala.collection.mutable
import com.zotoh.frwk.util.CoreUtils._

/**
 * @author kenl
 *
 */
trait NIOCB {
  def destroy():Unit
}

/**
 * @author kenl
 */
abstract class BaseHttpIO protected(evtHdlr:Observer, nm:String, ssl:Boolean) 
extends HTTPIOTrait(evtHdlr, nm,ssl) {

  private val _cbs= mutable.HashMap[Any,NIOCB]()
  private var _socTOutMillis=0
  private var _thsHold=0L
  private var _waitMillis=0L

  private var _async=true
  private var _workers=0
  
  def threshold() = _thsHold
  def isAsync() = _async

  def socetTimeoutMillis() = _socTOutMillis
  def waitMillis() = _waitMillis
  def workers() = _workers

  def removeCB(key:Any) = _cbs.remove(key)
  def cb(key:Any) = _cbs.get(key)
  def addCB(key:Any, cb:NIOCB) {
    _cbs += Tuple2(key, cb)
  }

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    val socto= cfg.getLong("soctoutmillis", 0L)  // no timeout
    val nio = cfg.getBool("async",true)
    val wks= cfg.getLong("workers", 6)
    val thold= cfg.getLong("thresholdkb", 8L*1024)  // 8 Meg
    val wait= cfg.getLong("waitmillis", 300000L)   // 5 mins

    tstNonNegLongArg("socket-timeout-millis", socto)
    _socTOutMillis = socto.toInt

    tstNonNegLongArg("threshold", thold)
    _thsHold = 1024L * thold

    tstPosLongArg("wait-millis", wait)
    _waitMillis = 1L *wait

    tstPosLongArg("workers", wks)
    _workers = wks.toInt

    _async=nio
  }

}


