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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.SeqNumGen
import com.zotoh.blason.core.Loggable
import org.slf4j._
import java.util.EventObject
import org.apache.commons.lang3.{StringUtils=>STU}

/**
 * @author kenl
 */
@SerialVersionUID(-1928500078786458743L)
abstract class AbstractEvent protected(src:EventEmitter) extends EventObject(src) with Loggable {

  private val _log= LoggerFactory.getLogger(classOf[AbstractEvent])
  def tlog() = _log

  private var _waitEvent:WaitEvent=null
  private var _res:AbstractResult=null  
  private var _routerClass=""
  private var _sess:SessionIO=null  
  private var _id= SeqNumGen.next()

  def bindSession(s:SessionIO) {
    _sess=s
  }
  
  def getSession() = _sess
  
  def setResult(r:AbstractResult) {
    if (_sess != null) {
      _sess.handleResult(this, r)
    }
    _res= r
    if ( _waitEvent != null) try {
      _waitEvent.resumeOnResult(r)
    } finally {
      emitter().release(_waitEvent)
      _waitEvent=null
    }
  }

  def emitter() = source.asInstanceOf[EventEmitter]

  def result() = {
    if (_res==null) None else Some(_res)
  }

  def routerClass_=(s:String) { _routerClass= s }
  def routerClass = _routerClass
  def hasRouter() = !STU.isEmpty(_routerClass)
  
  def id() = _id

  def destroy() {  }

  protected[io] def bindWait(w:WaitEvent) {
    _waitEvent=w
  }

}

