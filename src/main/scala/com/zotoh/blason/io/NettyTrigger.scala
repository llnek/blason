/*??
 * COPYRIGHT (C) 2010-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.handler.codec.http.HttpHeaders._
import scala.collection.JavaConversions._
import java.io.InputStream
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.stream.ChunkedStream
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.net.HTTPStatus
import org.jboss.netty.handler.codec.http.CookieEncoder
import java.nio.channels.ClosedChannelException


/**
 * @author kenl
 */
class NettyTrigger private(src:EventEmitter) extends AsyncTrigger(src)  {

  private var _channel:Channel=null
  private var _evt:HTTPEvent=null

  def this( evt:HTTPEvent, c:Channel) {
    this( evt.emitter )
    _channel=c
    _evt=evt
  }

  override def resumeWithResult(rs:AbstractResult) {
    rs match {
      case res:HTTPResult =>
        try {
          reply(res)
        } catch {
          case e:Throwable => tlog.error("", e)
        }
      case _ => /* never */
    }
  }

  override def resumeWithError() {
    resumeWithResult(
      new HTTPResult(HTTPStatus.INTERNAL_SERVER_ERROR) )
  }

  private def reply(res:HTTPResult) {
    val rsp = new DefaultHttpResponse(HTTP_1_1,
          new HttpResponseStatus(res.statusCode, res.statusText ))
    var clen = 0L

    res.headers().foreach { (t) => rsp.setHeader( t._1,t._2) }
    val inp= res.data match {
      case Some(d) if d.hasContent =>
        clen= d.size()
        d.stream()
      case _ => null
    }
    rsp.setHeader("content-length", clen.toString)
    res.getCookies().foreach { (c) =>
      val enc= new CookieEncoder(true)
      enc.addCookie(c)
      rsp.addHeader( Names.SET_COOKIE, enc.encode )
    }

    //TODO: this throw NPE some times !
    var cf:ChannelFuture = try { _channel.write(rsp) } catch {
      case e:ClosedChannelException => 
        tlog.error("ClosedChannelException thrown: NettyTrigger @line 97") 
        null
      case e:Throwable =>
        tlog.error("", e) 
        null
    }
    
    if (inp != null) try {
      cf= _channel.write(new ChunkedStream(inp))
    } catch {
      case e:ClosedChannelException => 
        tlog.error("ClosedChannelException thrown: NettyTrigger @line 107") 
      case e:Throwable =>
        tlog.error("", e) 
    }
    
    maybeClose(cf)
  }

  private def maybeClose(cf:ChannelFuture) {
    if ( ! _evt.isKeepAlive ) {
        if (cf != null) cf.addListener(ChannelFutureListener.CLOSE)
    }          
  }
  
  
}

