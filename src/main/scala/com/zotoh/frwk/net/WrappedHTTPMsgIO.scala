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
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrArr

/**
 * @author kenl
 *
 */
class WrappedHTTPMsgIO( private var _inner:HTTPMsgIO) extends HTTPMsgIO {

  override def onOK(code:Int, r:String, resOut:XData) {
    if (_inner != null) { _inner.onOK(code, r, resOut) }
  }

  override def onError(code:Int, reason:String) {
    if (_inner!=null) { _inner.onError(code, reason) }
  }

  override def configMsg(m:HttpMessage) {
    if ( _inner != null) { _inner.configMsg(m) }
  }

  override def keepAlive() = {
    if (_inner==null) false else _inner.keepAlive
  }

  override def onPreamble(mtd:String, uri:String, hds:Map[String, StrArr]) {
    if ( _inner != null) { _inner.onPreamble(mtd, uri, hds) }
  }

}
