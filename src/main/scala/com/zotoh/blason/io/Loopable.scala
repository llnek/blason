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
package io

import scala.math._
import java.util.{Date=>JDate,Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.frwk.util.CoreUtils._
import java.util.Observer


/**
 *
 * @author kenl
 */
trait Loopable {

  var _timer:JTimer

  def onStart() {
    _timer= new JTimer(true)
    schedule()
  }

  def onStop() {
    if ( _timer != null) { _timer.cancel() }
    _timer= null
  }

  protected def scheduleTriggerWhen(w:JDate) {
    tstObjArg("date", w)
    if ( _timer != null) {
      _timer.schedule( mkTask(), w)
    }
  }

  protected def scheduleTrigger(delay:Long) {
    if ( _timer != null) {
      _timer.schedule( mkTask(), max( 0L, delay ))
    }
  }

  protected def scheduleRepeaterWhen(w:JDate, interval:Long) {
    tstObjArg("when", w)
    if ( _timer != null) {
      _timer.schedule( mkTask(), w, interval)
    }
  }

  protected def scheduleRepeater(delay:Long, interval:Long) {
    tstPosLongArg("repeat-interval", interval)
    tstNonNegLongArg("delay", delay)
    if ( _timer != null) {
      _timer.schedule( mkTask(), max(0L, delay), interval)
    }
  }

  protected def schedule():Unit
  protected def wakeup():Unit


  private def mkTask() = {
    new JTTask() {
      def run() {
        try {
          wakeup()
        } catch {
          case e:Throwable => tlog().warn("",e)
        }
      }
    }
  }



}

