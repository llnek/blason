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
package wflow

import com.zotoh.blason.wflow.Reifier._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.blason.kernel.Job
import org.slf4j._
import com.zotoh.blason.core.Identifiable
import com.zotoh.blason.core.Loggable



/**
 * @author kenl
 *
 */
abstract class FlowStep protected[wflow](protected var _parent:Pipeline) extends Runnable with Identifiable with Loggable {

  private val _log:Logger= LoggerFactory.getLogger(classOf[FlowStep])
  def tlog() = _log

  private var _nextPtr:FlowStep = null
  private var _defn:Activity = null
  private var _closure:Any= null
  private var _pid=0L

  /**
   * @param s
   * @param a
   */
  protected def this(s:FlowStep, a:Activity) {
    this(s.flow)
    _nextPtr=s
    _defn=a
    _pid=_parent.nextAID()
  }

  def eval(j:Job ):FlowStep

  def id() = _pid

  def realize() {
    getDef.realize(this)
    postRealize
  }

  protected def postRealize() {}

  def nextStep() = _nextPtr
  def getDef() = _defn

  def attachClosureArg(c:Any) {
    _closure=c
  }

  protected def clsClosure() { _closure=null }
  def popClosureArg() = {
    try { val c=_closure; c } finally { _closure=null }
  }

  def forceNext(n:FlowStep) {
    _nextPtr=n
  }
  
  def flow() = _parent

  def rerun() {
    flow().core().reschedule(this)
  }

  def run() {
    var n= nextStep()
    val f= flow()

    var err:Activity = null
    var rc:FlowStep =null

    try {
      rc=eval( f.job )
    } catch {
      case e:Throwable => err= flow().onError(e)
    }

    if (err != null) {
      if (n==null) { n= reifyZero(f) }
      rc= err.reify(n)
    }

    if (rc==null) {
      tlog().debug("FlowStep: rc==null => skip.")
      // indicate skip, happens with joins
    } else {
      runAfter(f,rc)
    }

  }

  private def runAfter(f:Pipeline, rc:FlowStep) {
    rc match {
      case x:NihilStep => f.stop()
      case x:AsyncWaitStep => f.core().hold( rc.nextStep())
      case x:DelayStep => f.core().delay(rc.nextStep(), x.delayMillis())
      case _ => f.core().run(rc)
    }

  }

}
