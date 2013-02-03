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

package com.zotoh.frwk
package util

import java.util.Arrays
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.selector.ContextSelector

/**
 * @author kenl
 */
class LogbackCtxSelector(private val _dftCtx:LoggerContext) extends ContextSelector {

  def getLoggerContext() = getDefaultLoggerContext()

  def getDefaultLoggerContext() = {
    println("LogbackCtxSelector: getDefaultLoggerContext() ")
   _dftCtx 
  }

  def detachLoggerContext(loggerContextName:String) = _dftCtx

  def getContextNames() = Arrays.asList( _dftCtx.getName )

  def getLoggerContext(name:String ) = {
    println("LogbackCtxSelector: getLoggerContext() : " + name )
    if ( _dftCtx.getName == name) _dftCtx else null
  }

}
