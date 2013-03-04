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
package kernel


import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.{Properties=>JPS,Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreImplicits
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.blason.core.Initializable
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.core.Startable
import com.zotoh.blason.core.Identifiable
import com.zotoh.blason.core.Disposable

import org.slf4j._

object Scheduler {
  private val _log=LoggerFactory.getLogger(classOf[Scheduler])
}

/**
 * @author kenl
 */
class Scheduler protected[kernel]() extends CoreImplicits
with Configurable with Startable with Disposable {

  protected val _holdQ= mutable.HashMap[Long,Runnable]()
  protected val _runQ= mutable.HashMap[Long,Runnable]()
  //protected val _parQ= mutable.HashMap[Long,Long]()
  def tlog() = Scheduler._log

  protected var _core:TCore= null
  private var _timer:JTimer=null

  def dispose() {
    if (_core != null) { _core.dispose }
  }

  /**
   * @param core
   * @param w
   */
  def run(w:Runnable) {
    preRun(w)
    _core.schedule(w)
  }

  def delay(w:Runnable, delayMillis:Long) {

    val me= this

    if (delayMillis == 0L) { run( w)  }
    else if (delayMillis < 0L) { hold( w) }
    else {
      addTimer(new JTTask() {
        def run() { me.wakeup(w) }
      }, delayMillis)

      tlog.debug("Delaying eval on process: {}, wait: {} {}" ,
        w, asJObj(delayMillis),"millisecs")
    }

  }

  def hold( w:Runnable) {
    xrefPID(w) match {
      case pid:Long if pid >= 0L =>  hold(pid,w)
      case _ =>
    }
  }

  def hold(pid:Long, w:Runnable) {
    _runQ.remove(pid)
    _holdQ.put(pid, w)
    tlog.debug("Moved to pending wait, process: {}", w)
  }

  def wakeup(w:Runnable) {
    xrefPID(w) match {
      case pid:Long if (pid >= 0L) =>  wakeAndRun( pid,w)
      case _ =>
    }
  }

  def wakeAndRun( pid:Long, w:Runnable) {
    _holdQ.remove(pid)
    _runQ.put(pid, w)
    run( w)
    tlog.debug("Waking up process: {}", w)
  }

  def reschedule(w:Runnable) {
    if (w != null) {
      tlog.debug("Restarting runnable: {}" , w)
      run( w)
    }
  }

  private def preRun(w:Runnable) {
    xrefPID(w) match {
      case n:Long if n >= 0L  =>
        _holdQ.remove( n )
        _runQ += n -> w
      case _ =>
    }
  }

  def start() { _core.start }

  def stop() {}

  def addCore(id:String, threads:Int) {
    _core= new TCore(id,threads)
  }

  def configure(c:Configuration) {
    addCore( uid(), c.getLong("threads", 4L).toInt )
    _timer= new JTimer("scheduler-timer", true)
  }

  def addTimer(t:JTTask, delay:Long) {
    _timer.schedule(t, delay)
  }

  private def xrefPID(w:Runnable) = {
    w match {
      case s:Identifiable => s.id()
      case _ => -1L
    }
  }

}
