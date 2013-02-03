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
import com.zotoh.frwk.util.StrUtils._

import java.io.IOException

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ChildChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpRequest

/**
 * @author kenl
 */
class NettyREQHdlr(private val _src:NettyIO) extends SimpleChannelHandler {

  override def channelOpen(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    val c= gCH(ctx,e)
    _src.pushOneChannel(c)
    try { super.channelOpen(ctx, e) } finally {
      lg(ctx,c, "ChannelOpen")
    }
  }

  override def channelDisconnected(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    val c= gCH(ctx,e)
    try { super.channelConnected(ctx, e) } finally {
      lg(ctx,c, "ChannelDisconnected")
    }
  }

  override def channelConnected(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    val c= gCH(ctx,e)
    try { super.channelConnected(ctx, e) } finally {
      lg(ctx,c, "ChannelConnected")
    }
  }

  override def channelClosed(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    val c= gCH(ctx,e)

    _src.removeCB(c)

    try { super.channelClosed(ctx, e) } finally {
      lg(ctx,c, "ChannelClosed")
    }
  }

  override def exceptionCaught(ctx:ChannelHandlerContext, e:ExceptionEvent) {
    val c= gCH(ctx,e)
    if (c != null) {
      _src.removeCB(c) match {
        case Some(cb) => cb.destroy()
        case _ => /* noop */
      }
      c.close()
    }
    _src.tlog().error("", e.getCause )
  }

  override def messageReceived(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val c= ctx.getChannel()
    ev.getMessage() match {

      case x:HttpRequest =>
        val cb= new NettyMsgCB(_src)
        _src.addCB(c, cb)
        cb.onREQ(ctx,ev)

      case x:HttpChunk =>
        _src.cb(c) match {
          case Some(y:NettyMsgCB) => y.onChunk(ctx,ev)
          case _ =>
            throw new IOException("Netty failed to reconcile http-chunked msg")
        }

      case msg =>
        throw new IOException("Netty unexpected msg type: " + safeGetClzname(msg))
    }
  }

  override def childChannelClosed(ctx:ChannelHandlerContext, e:ChildChannelStateEvent) {
    val c= gCH(ctx,e)

    _src.popOneChannel(c)

    try { super.childChannelClosed(ctx, e) } finally {
      lg(ctx,c, "ChildChannelClosed")
    }
  }

  override def childChannelOpen(ctx:ChannelHandlerContext, e:ChildChannelStateEvent) {
    var c= gCH(ctx,e)

    _src.pushOneChannel(c)

    try { super.childChannelOpen(ctx, e) } finally {
      lg(ctx,c, "ChildChannelOpen")
    }
  }

  private def gCH(ctx:ChannelHandlerContext, e: ChildChannelStateEvent) = {
    e.getChannel() match {
      case h:Channel => h
      case _ => ctx.getChannel()
    }
  }

  private def gCH(ctx:ChannelHandlerContext, e:ChannelStateEvent) = {
    e.getChannel() match {
      case h:Channel => h
      case _ => ctx.getChannel()
    }
  }
  
  private def gCH(ctx:ChannelHandlerContext, e:ExceptionEvent) = {
    e.getChannel() match {
      case h:Channel => h
      case _ => ctx.getChannel()
    }
  }
  
  private def lg(ctx:ChannelHandlerContext, c:Channel, msg:String) {
    _src.tlog().debug("{} - ctx {}, channel {}", msg, ctx, nsn(c))
  }


}
