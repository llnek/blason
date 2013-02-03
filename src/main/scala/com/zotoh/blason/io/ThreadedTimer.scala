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

import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.blason.util.Observer

/**
 * @author kenl
 */
abstract class ThreadedTimer(evtHdlr:Observer, nm:String) extends RepeatTimer(evtHdlr,nm){

  @volatile private var _readyToLoop=false
  @volatile private var _tictoc=false

  override def onStart() {
    _readyToLoop= true
    _tictoc=true
    preLoop()
    schedule()
  }

  override def onStop() {
    _readyToLoop= false
    _tictoc=false
    endLoop()
  }

  override def schedule() {
    asyncExec(new Runnable() {
      def run() {
        while ( loopy ) try {
          onOneLoop()
        } catch {
          case e:Throwable => tlog().warn("",e)
        }
        return
      }
    })
  }

  protected def readyToLoop() = _readyToLoop
  protected def preLoop() {}
  protected def endLoop() {}
  protected def onOneLoop():Unit

  private def loopy() = {
    if ( ! _readyToLoop) false else {
      if (_tictoc) {
        _tictoc=false
        safeWait( delayMillis )
      }
      else {
        safeWait( intervalMillis )
      }
      _readyToLoop
    }
  }

}
