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
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.blason.util.Observer

/**
 * @author kenl
 */
class RepeatTimer(evtHdlr:Observer, nm:String) extends AbstractTimer(evtHdlr,nm) {
  private var _intervals:Long= 0L

  def this() {
    this (null,"")
  }
  
  def intervalMillis() = _intervals
  def setIntervalMillis(n:Long) {
    _intervals=n
  }

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    _intervals = cfg.getLong(AbstractTimer.PSTR_INTVSECS,0L)
    tstPosLongArg("interval-secs", _intervals)
    _intervals = 1000L * _intervals
  }

  def schedule() {
    when() match {
      case Some(w) => scheduleRepeaterWhen( w, intervalMillis )
      case _ => scheduleRepeater( delayMillis, intervalMillis )
    }
  }

  def wakeup() {
    dispatch( new TimerEvent(this,true))
  }

}
