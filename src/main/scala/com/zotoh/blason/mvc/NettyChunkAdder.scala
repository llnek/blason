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
package mvc

import java.io.{File,OutputStream}
import org.apache.commons.io.{IOUtils=>IOU}
import org.jboss.netty.buffer.ChannelBufferInputStream
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMessage
import com.zotoh.frwk.io.IOUtils
import org.slf4j._


/**
 * @author kenl
 */
object NettyChunkAdder {
  private val _log=LoggerFactory.getLogger(classOf[NettyChunkAdder])
  val MAX_CLEN= 1024*1024*1024*2 // 2G
}

/**
 * @author kenl
 */
class NettyChunkAdder extends SimpleChannelUpstreamHandler {

  import NettyChunkAdder._
  def tlog() = _log

  @volatile private var fRef:(File,OutputStream) = null
  @volatile private var curLen=0L
  @volatile private var theMsg:HttpMessage= null

  override def messageReceived(ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage match {
      case m:HttpMessage => onMsg(ctx,ev, m)
      case c:HttpChunk => onChunk(ctx,ev,c)
      case _ => ctx.sendUpstream(ev)
    }
  }

  private def onMsg(ctx:ChannelHandlerContext,ev:MessageEvent, msg:HttpMessage) {
    if (msg.isChunked ) {

        // do some cleaning up with the headers: xfer-encoding
        val enc = msg.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING)
        enc.remove(HttpHeaders.Values.CHUNKED)
        if (enc.size == 0) {
          msg.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING)
        }

        fRef= IOUtils.newTempFile(true)
        theMsg=msg

    } else {
      ctx.sendUpstream(ev)
    }
  }

  private def onChunk(ctx:ChannelHandlerContext,ev:MessageEvent, msg:HttpChunk) {
    val c= msg.getContent.readableBytes
    if (curLen + c > MAX_CLEN) {
      tlog.warn("Message is too large - chunk aggregation stopped.")
      theMsg.setHeader(HttpHeaders.Names.WARNING, "Message is too large")
    }
    IOU.copyLarge(new ChannelBufferInputStream(msg.getContent), fRef._2 )
    if (msg.isLast) {
      fRef._2.close()
      theMsg.setHeader( HttpHeaders.Names.CONTENT_LENGTH, fRef._1.length )
      theMsg.setContent(new FileChannelBuffer(fRef._1))
      Channels.fireMessageReceived(ctx, theMsg, ev.getRemoteAddress)
      theMsg=null
      fRef=null
    }
  }

}

