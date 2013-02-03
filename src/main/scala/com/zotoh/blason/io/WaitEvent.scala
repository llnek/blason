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
import org.slf4j._
import com.zotoh.blason.core.Loggable



/**
 * @author kenl
 */
abstract class WaitEvent protected(private val _event:AbstractEvent) extends Loggable {

  private val _log=LoggerFactory.getLogger(classOf[WaitEvent])
  def tlog() = _log

  private var _res:AbstractResult= null

  _event.bindWait(this)

  def resumeOnResult(res:AbstractResult):Unit

  def timeoutMillis(millisecs:Long):Unit

  def timeoutSecs(secs:Int):Unit

  def inner() = _event

  def id() =  _event.id()

  def setResult(obj:AbstractResult) {
    _res= obj
  }

  def result() = {
    if (_res==null) None else Some(_res)
  }

}

