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

import java.util.{Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.frwk.util.CoreUtils._

/**
 * @author kenl
 */
class AsyncWaitEvent(ev:AbstractEvent,private val _trigger:AsyncWaitTrigger) extends WaitEvent(ev) {

  private var _timer:JTimer = null

  override def resumeOnResult(res:AbstractResult) {
    if(_timer!=null) { _timer.cancel() }
    _timer=null
    inner().emitter().release(this)
    setResult(res)
    _trigger.resumeWithResult(res)
  }

  override def timeoutMillis(millisecs:Long) {
    _timer = new JTimer(true)
    _timer.schedule(
            new JTTask() {
              def run() { onExpiry() }
            },
      millisecs)
  }

  override def timeoutSecs(secs:Int) { timeoutMillis(1000L * secs) }

  private def onExpiry() {
    inner().emitter().release(this)
    _timer=null
    _trigger.resumeWithError()
  }

}

