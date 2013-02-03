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

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.blason.core.Startable
import com.zotoh.blason.core.Suspendable
import com.zotoh.blason.core.Disposable
import com.zotoh.blason.core.Initializable
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import org.slf4j.Logger
import com.zotoh.blason.core.Component
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.blason.core.BackgroundService
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.blason.kernel.Job
import com.zotoh.blason.util.Observable
import org.slf4j._
import com.zotoh.blason.wflow.Pipeline
import com.zotoh.blason.util.Observer
import com.zotoh.blason.core.Loggable
import java.util.concurrent.atomic.AtomicInteger
import com.zotoh.blason.kernel.Container

/**
 * @author kenl
 */
abstract class EventEmitter(private var _evtHdlr:Observer, private var _name:String = "") extends Observable with BackgroundService with Loggable {

  private val _log:Logger= LoggerFactory.getLogger(classOf[EventEmitter])
  def tlog() = _log
  override var _obs:Observer=null

  private val _backlog=mutable.HashMap[Any,WaitEvent]()
  protected val _seed= new AtomicInteger(0)
  private var _ctr:Container= null
  private var _enabled=false
  private var _handlerCZ=""
  private var _version=""
  private var _active=false
  
  protected def setVersion(v:String) { _version=v }
  protected def setName(n:String) { _name=n }

  def version() = _version
  def name() = _name
  def container() = _ctr
  
  def release(w:WaitEvent) {
    if (w != null) { _backlog.remove(w.id()) }
  }

  def hold(w:WaitEvent) {
    if (w != null) { _backlog += Tuple2(w.id(), w) }
  }

  def compose(r:ComponentRegistry, arg:Any*) = {
    tstArg(arg.size > 0, "Invalid arg count while making emitter.")
    _evtHdlr = arg(1).asInstanceOf[Observer]
    _ctr = arg(0).asInstanceOf[Container]
    _name = if (arg.size > 2) nsb(arg(2) ) else ""
    if (STU.isEmpty(_name)) {
      _name = getClass().getSimpleName() + "-" + _seed.incrementAndGet()
    }
    tlog().info("Composed {} {} of type {}.", "Emitter", _name, getClass().getName())
    setObserver(_evtHdlr)    
    Some(this)
  }

  protected def onInit() {}

  def dispatch(ev:AbstractEvent) {
    try {
      setChanged()
      notifyObservers( ev, _handlerCZ)
    } catch {
      case e:Throwable => tlog().error("",e)
    }
  }

  def start() {
    tlog().info("EventEmitter {} starting...", _name)
    onStart()
    _active=true
  }

  def stop() {
    tlog().info("EventEmitter {} stopping...", _name)
    onStop()
    _active=false
  }

  def suspend() {
    throw new UnsupportedOperationException("EventEmitter.suspend()")
  }

  def resume() {
    throw new UnsupportedOperationException("EventEmitter.suspend()")
  }

  def configure(cfg:Configuration) {
    // get handler here
    _handlerCZ= cfg.getString("handler", "")
    if ( STU.isEmpty(_handlerCZ)) {
      tlog().warn("EventEmitter {} has no handler.", _handlerCZ)
    }
  }

  def initialize() {
    onInit()
  }

  def dispose() {
  }

  def isEnabled() = _enabled
  def isActive() = _active

  protected def onStart():Unit
  protected def onStop():Unit

  private def iniz() {
    val rc= STU.replace(getClass().getName(),".","/") + ".meta"
    rc2Bytes(rc)
  }



}
