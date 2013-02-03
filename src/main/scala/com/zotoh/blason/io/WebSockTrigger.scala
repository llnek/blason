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

import com.zotoh.frwk.util.StrUtils._
import java.nio.ByteBuffer

import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame

/**
 * @author kenl
 */
class WebSockTrigger( private val _ctx:ChannelHandlerContext, src:EventEmitter) extends AsyncTrigger(src) {

  override def resumeWithResult(result:AbstractResult) {
    val res= result.asInstanceOf[WebSockResult]
    val f = if (res.isBinary) {
      new BinaryWebSocketFrame( new ByteBufferBackedChannelBuffer( 
          ByteBuffer.wrap(res.data.javaBytes )) )
    } else {
      new TextWebSocketFrame( res.data.toString )
    }
    _ctx.getChannel().write(f)
  }

  override def resumeWithError() {
  }

}
