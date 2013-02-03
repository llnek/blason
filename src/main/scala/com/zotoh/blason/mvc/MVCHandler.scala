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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import java.io.IOException
import java.util.regex._
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.{ChannelHandlerContext=>CHContext}
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ChildChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpHeaders
import com.zotoh.frwk.util.CoreImplicits
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.{WebSocketServerHandshaker=>WSHandshaker}
import com.zotoh.frwk.io.XData
import org.apache.commons.lang3.{StringUtils=>STU}
import java.io.File
import com.zotoh.blason.core.Constants
import com.zotoh.blason.io.NettyHplr._
import com.zotoh.blason.io.WebSockEvent
import com.zotoh.blason.io.NettyIO
import com.zotoh.blason.io.AsyncWaitEvent
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.blason.io.NettyHplr._
import com.zotoh.blason.io.WebSockTrigger
import com.zotoh.blason.io.NettyTrigger
import org.slf4j._


/**
 * @author kenl
 */
object MVCHandler {
  private val _log= LoggerFactory.getLogger(classOf[MVCHandler])
}

/**
 * @author kenl
 */
class MVCHandler(private val _src:NettyMVC) extends SimpleChannelHandler with CoreImplicits with Constants {

  private var _wsHandShaker: WSHandshaker = null
  import HttpHeaders._
  import MVCHplr._
  
  def tlog() = MVCHandler._log
  
  override def messageReceived(ctx:CHContext, ev:MessageEvent) {
    //val c= ctx.getChannel()
    ev.getMessage() match {

      case x:WebSocketFrame => handleWebSocketFrame(ctx, x)

      case x:HttpRequest =>
        if (x.getMethod == HttpMethod.GET && Values.WEBSOCKET.eqic( x.getHeader( Names.UPGRADE)) ) {
          websockHandshake(ctx, ev, x)
        } else {
          handleRequest(ctx,ev,x)
        }

      case msg =>
        throw new IOException("Netty unexpected msg type: " + safeGetClzname(msg))
    }
  }

  private def precond(ctx:CHContext, req:HttpRequest) {
    
    if  ( ! req.getUri().startsWith( _src.getContextPath() ) ) {
      serve403(ctx)
    }
    
  }
  
  private def handleRequest(ctx:CHContext, ev:MessageEvent, req:HttpRequest) {
    precond(ctx, req)
    val evt= extract(_src, ctx, req)
    val rc = routeCracker( evt)
    if (rc._1 && !STU.isEmpty(rc._4)) {
      sendRedirect(ctx,false, rc._4)
    }
    else if (rc._1) {
      tlog.debug("Matched one Route: {}", rc._2.path )
      if (rc._2.isStatic) { serveStatic(rc._2, rc._3, ctx, req,evt) } else {
        serveRoute(rc._2, rc._3, ctx, evt)
      }              
    } else {    
      serveWelcomeFile(req) match {
        case Some(fp) =>
          handleStatic( _src, ctx, req, fp)          
        case _ =>
          tlog.debug("Failed to match Uri: {}", req.getUri )
          serve404(ctx)
      }
    }
  }

  private def serveWelcomeFile(req:HttpRequest) = {
    if ( ! req.getUri().matches("/?")) None else { 
      _src.getWelcomeFiles.find { (fn) =>
        val f= new File(_src.container.appDir, DN_PUBLIC +"/"+fn )
        f.exists && f.canRead                  
      } match {
        case Some(fn) =>Some(new File(_src.container.appDir, DN_PUBLIC +"/"+fn ))
        case _ =>None
      }
    }    
  }
  
  private def serveStatic(ri:RouteInfo, mc:Matcher, ctx:CHContext,
      req:HttpRequest, evt:HTTPEvent) {
    val appDir= _src.container.appDir
    val uri= evt.uri()
    var mp= STU.replace( ri.mountPoint, "${app.dir}",  niceFPath(appDir))
    for ( i <- 1 to mc.groupCount ) {
      mp= STU.replace(mp, "{}", mc.group(i), 1)
    }
    // serve all static assets from *public folder*
    val ps=niceFPath(new File(appDir,DN_PUBLIC))
    mp=niceFPath(new File(mp))
    if (mp.startsWith(ps)) {
      handleStatic( _src, ctx, req, new File(mp))
    } else {
      tlog.warn("Attempt to access non public file-system: {}", mp)
      serve403(ctx)
    }
  }

  private def serveRoute(ri:RouteInfo, mc:Matcher, ctx:CHContext, evt:HTTPEvent) {
    val w= new AsyncWaitEvent( evt, new NettyTrigger(evt, ctx.getChannel) )
    val pms = ri.resolveMatched(mc)
    pms.foreach { (en) =>      evt.addAttr(en._1, en._2) }    
    w.timeoutMillis( _src.waitMillis)
    evt.routerClass = ri.pipeline()
    _src.hold(w)
    _src.dispatch(evt)
  }

  private def serve404(ctx:CHContext) {
    trap404(ctx)
  }

  private def serve403(ctx:CHContext) {
    trap403(ctx)
  }
  
  private def routeCracker(evt:HTTPEvent)  = {
    var rc: (Boolean, RouteInfo,Matcher,String) = (false, null,null, "")
    val cpath= evt.contextPath()
    val uri= evt.uri()
    _src.container().getRoutes().find { (r) =>
      r.resemble(uri) match {
        case Some(m) => rc=(true, r,m, ""); true
        case _ => false
      }
    }
        
    if (!rc._1 && !uri.endsWith("/") ) {
      val u= uri+"/"
      if (maybeRedirect(u)) {
        rc = (true,null,null, u )
      }
    }
    
    rc
  }
  
  private def maybeRedirect(uri:String) = {
    _src.container().getRoutes().find { (r) =>
      r.resemble(uri) match {
        case Some(m) =>true  
        case _ => false
      }
    } match {
      case Some(s) => true
      case _ => false
    }    

  }

  override def childChannelClosed(ctx:CHContext, e:ChildChannelStateEvent) {
    //_src.popOneChannel(c)
    super.childChannelClosed(ctx, e)
    lg( "ChildChannelClosed", ctx)
  }

  override def childChannelOpen(ctx:CHContext, e:ChildChannelStateEvent) {
    super.childChannelOpen(ctx, e)
    _src.pushOneChannel(gCH(ctx,e))
    lg("ChildChannelOpen", ctx)    
  }

  private def gCH(ctx:CHContext, e:ChannelEvent) = {
    ctx.getChannel() match {
      case h:Channel => h
      case _ => e.getChannel()
    }
  }

  private def lg( msg:String, ctx:CHContext ) {
    _src.tlog.debug("{} -{}{}", msg,nsn(ctx),"")
  }

  private def handleWebSocketFrame(ctx:CHContext, frame:WebSocketFrame) {
    val ev=new WebSockEvent(_src)
    val cc= ctx.getChannel
    val data= frame match {
      case x:CloseWebSocketFrame =>
        _wsHandShaker.close(cc, x)
        null
      case x:PingWebSocketFrame =>
        cc.write(new PongWebSocketFrame(frame.getBinaryData ))
        null
      case x:BinaryWebSocketFrame =>  ev.setData( x.getBinaryData().array )
      case x:TextWebSocketFrame => ev.setData( x.getText )
      case x:ContinuationWebSocketFrame =>
        if (!x.isFinalFragment ) null else {
          ev.setData( x.getAggregatedText )
        }
    }

    if (data != null) {
      val w= new AsyncWaitEvent( data, new WebSockTrigger(ctx,_src) )
      val evt = w.inner
      w.timeoutMillis(_src.waitMillis )
      _src.hold(w)
      _src.container.scheduler.run( new Runnable() {
        def run() { _src.dispatch(evt) }
      })
    }
  }

  private def websockHandshake(ctx:CHContext, ev:MessageEvent,
      req:HttpRequest ) {
    tlog.debug("NettyIO: about to perform websocket handshake.")
    // from playframework
    //ctx.getPipeline().addLast("fake-aggregator", new HttpChunkAggregator( 1024*64))
    try {
      doHandShake(ctx,ev,req)
    } finally {
      // block { () => ctx.getPipeline().remove("fake-aggregator") }
    }

  }

  private def doHandShake(ctx:CHContext, ev:MessageEvent,
      req:HttpRequest ) {
    val ssl= ctx.getPipeline().get(classOf[SslHandler]) != null
    val wsf = new WebSocketServerHandshakerFactory(
          getWebSockLoc(req,ssl), null, false)
    _wsHandShaker = wsf.newHandshaker(req)
    val me=this
    val cc= gCH(ctx,ev)
    if ( _wsHandShaker == null) {
      wsf.sendUnsupportedWebSocketVersionResponse(cc)
    } else {
      _wsHandShaker.handshake(cc, req).addListener(new ChannelFutureListener(){
        override def operationComplete(f:ChannelFuture) {
          if (!f.isSuccess()) {
            Channels.fireExceptionCaught(f.getChannel, f.getCause )
          }else{
            me.maybeSSL(ctx)
          }
        }
      })
    }
  }

  private def maybeSSL(ctx:CHContext) {
    tlog.debug("NettyIO: websocket handshake was successful.")
    // check if ssl ?
    // get the SslHandler in the current pipeline.
    val ssl= ctx.getPipeline.get( classOf[SslHandler] )
    if (ssl != null) {
      val cf= ssl.handshake
      cf.addListener( new ChannelFutureListener() {
        override def operationComplete(f:ChannelFuture) {
          if (!f.isSuccess ) {
            f.getChannel().close
          }
        }
      } )
    }
  }

  private def getWebSockLoc(req:HttpRequest,ssl:Boolean) = {
    ( if (ssl)  "wss://" else "ws://" ) + req.getHeader(Names.HOST) + req.getUri
  }

  override def channelOpen(ctx:CHContext, e:ChannelStateEvent) {
    super.channelOpen(ctx, e)
    _src.pushOneChannel( gCH(ctx,e) )
    lg("ChannelOpen",ctx)
  }

  override def channelDisconnected(ctx:CHContext, e:ChannelStateEvent) {
    super.channelDisconnected(ctx, e)
    lg("ChannelDisconnected", ctx)
  }

  override def channelConnected(ctx:CHContext, e:ChannelStateEvent) {
    super.channelConnected(ctx, e)
    lg("ChannelConnected", ctx)
  }

  override def channelClosed(ctx:CHContext, e:ChannelStateEvent) {
    //_src.removeCB(c)
    super.channelClosed(ctx, e)
    lg("ChannelClosed", ctx)
  }

  override def exceptionCaught(ctx:CHContext, e:ExceptionEvent) {
    _src.tlog.error("", e.getCause )
    val c= gCH(ctx,e)
    if (c != null) {
      c.close()
    }
  }

}
