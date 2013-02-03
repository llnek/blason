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
import java.net.InetSocketAddress
import java.net.URI
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.Channels
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion
import org.slf4j._
import java.io.IOException
import com.zotoh.frwk.util.CoreUtils._
import org.jboss.netty.channel.ChannelFutureListener

/**
 * @author kenl
 */
object WebSocketClient {
    private val _log= LoggerFactory.getLogger( classOf[WebSocketClient] )
}

/**
 * @author kenl
 */
abstract class WebSocketClient extends SimpleChannelUpstreamHandler {

    import WebSocketClient._
    def tlog() = _log

    protected var _handshaker:WebSocketClientHandshaker = null
    protected var _cb:WebSocketClientCB = null
    private var _ch:Channel = null
    protected var _remote:URI= null
    protected var _bs:ClientBootstrap = null

    iniz()

    protected def iniz():Unit

    def start( ) {
        val s = _remote.getScheme()
        val me=this
        if (s != "ws" && s != "wss") {
            throw new IOException("Unsupported protocol: " + s)
        }

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.

        val handshaker = new WebSocketClientHandshakerFactory().newHandshaker(
                              _remote, WebSocketVersion.V13, null, false, Map[String,String]() )

        _bs.setPipelineFactory(new ChannelPipelineFactory() {
            def getPipeline() = {
                val pl = Channels.pipeline()
                pl.addLast("decoder", new HttpResponseDecoder())
                pl.addLast("encoder", new HttpRequestEncoder())
                pl.addLast("ws-handler", me)
                pl
            }
        })

        _bs.setOption("tcpNoDelay" , true)
        _bs.setOption("keepAlive", true)
        
        connect (handshaker)
    }

    //override  def channelConnected(ctx:ChannelHandlerContext , e:ChannelStateEvent) {}

    def stop() { disconnect() }

    override def channelClosed(ctx:ChannelHandlerContext , e:ChannelStateEvent ) {
        if (_cb != null) { _cb.onDisconnect(this) }
    }

    override def messageReceived(ctx:ChannelHandlerContext , e:MessageEvent ) {

      val ch = ctx.getChannel()

      e.getMessage match {
        case rsp:HttpResponse =>
          if ( ! _handshaker.isHandshakeComplete) {
              _handshaker.finishHandshake(ch, rsp)
          } else {
            throw new IOException("Unexpected HttpResponse (status=" + rsp.getStatus + ", content="
                  + rsp.getContent().toString() + ")")
          }
        case f:BinaryWebSocketFrame => if (_cb != null) { _cb.onFrame( this, f) }
        case f:TextWebSocketFrame => if (_cb != null) { _cb.onFrame( this, f) }
        case f:PongWebSocketFrame => 
        case f:CloseWebSocketFrame => ch.close()
        case _ =>
      }

    }

    override def exceptionCaught(ctx:ChannelHandlerContext , e:ExceptionEvent ) {
        val t= e.getCause()

        tlog.error("", t)

        block{ () => ctx.getChannel().close() }
        
        if (_cb != null) { _cb.onError(this, t) }
    }

    def connect(hs:WebSocketClientHandshaker ) = {
      val me=this
      val cb= { (f:ChannelFuture) =>
        _ch = f.getChannel()
        _handshaker=hs
        hs.handshake( _ch).addListener(new ChannelFutureListener() {
          def operationComplete(f:ChannelFuture) {
            if (f.isSuccess) {              
              _cb.onConnect(me)
            } else {
              tlog.error("", f.getCause)
              disconnect()
            }
          }          
        })        
      }
      val f = _bs.connect( new InetSocketAddress(_remote.getHost, _remote.getPort))
      f.addListener(new ChannelFutureListener() {
        def operationComplete(f:ChannelFuture) {
          if (f.isSuccess) { 
            cb(f)            
          } else {
            tlog.error("", f.getCause)
            disconnect()
          }
        }        
      })
    }

    def disconnect() = {
        block { () => if (_ch != null) _ch.close() }
        _ch=null
    }

    def send(fr:WebSocketFrame ) = {
        _ch.write(fr)
        tlog.debug("Sent msg")
    }

    def getUrl() = _remote

}
