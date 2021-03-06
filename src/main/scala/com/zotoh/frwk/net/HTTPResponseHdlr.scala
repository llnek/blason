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

import org.jboss.netty.channel.group.ChannelGroup
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrArr

/**
 * @author kenl
 *
 */
class HTTPResponseHdlr(g:ChannelGroup) extends BasicChannelHandler(g) {

  private var _cb:HTTPMsgIO = null

  /**
   * @param cb
   * @return
   */
  def bind(cb:HTTPMsgIO): this.type = {
    _cb= cb; this
  }

  override def doResFinal(ctx:HTTPMsgInfo , out:XData) {
    if (_cb != null)  {
      _cb.onOK( ctx, out)
    }
  }

  override def onResError(code:Int, reason:String) {
    if (_cb != null) {
      _cb.onError(code, reason)
    }
  }


}

