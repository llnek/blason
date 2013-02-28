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
package net

import org.jboss.netty.handler.codec.http.HttpMessage
import com.zotoh.frwk.util.StrArr
import com.zotoh.frwk.util.CoreUtils._

import org.slf4j._

/**
 * @author kenl
 */
object BasicHTTPMsgIO  {
  
  private val _log= LoggerFactory.getLogger(classOf[BasicHTTPMsgIO])
}

/**
 * @author kenl
 *
 */
abstract class BasicHTTPMsgIO protected[net]() extends HTTPMsgIO {

  def tlog() = BasicHTTPMsgIO._log
  
  def onPreamble(mtd:String, uri:String, hds:Map[String,StrArr]) {}

  def onError(code:Int, reason:String) = {
    tlog.error("Error: status= {}, reason= {}" , asJObj(code), reason, "")
  }

  def keepAlive() = false

  def configMsg(m:HttpMessage) {}

}
