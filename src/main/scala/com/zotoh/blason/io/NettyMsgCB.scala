/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import scala.math._
import scala.collection.JavaConversions._
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{IOUtils=>IOU}
import com.zotoh.frwk.util.StrUtils._
import org.jboss.netty.handler.codec.http.HttpHeaders.{isKeepAlive => JbossIsKeepAlive}
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE
import java.io.File
import java.io.IOException
import java.io.{OutputStream,ByteArrayOutputStream=>ByteArrayOS}
import java.util.Set
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.apache.commons.io.{IOUtils=>IOU}
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.XData
import org.jboss.netty.handler.codec.http.DefaultHttpResponse

/**
 * @author kenl
 */
class NettyMsgCB protected[io](private val _src:NettyIO) extends NIOCB {

  @transient private var _os:OutputStream= null
  private var _clen:Long=0L
  private var _request:HttpRequest= null
  private var _keepAlive=false

  private var _cookie:CookieEncoder= null
  private var _event:HTTPEvent= null
  private var _fout:File= null

  def destroy() {
    IOU.closeQuietly(_os)
    _os=null
    _fout=null
    _request= null
    _event= null
    _cookie= null
  }

  def isKeepAlive() = _keepAlive
  def cookie() = _cookie
  def emitter() = _src

  protected[io] def setCookie(rsp:HttpResponse) {
    if (_cookie != null) {
      rsp.addHeader(SET_COOKIE, _cookie.encode )
    }
  }

  def event() = _event

  def preEnd(future:ChannelFuture) {
    // Close the non-keep-alive connection after the write operation is done.
    if ( ! _keepAlive ) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def send100Continue(e:MessageEvent) {         
    import org.jboss.netty.handler.codec.http.HttpResponseStatus._
    import  org.jboss.netty.handler.codec.http.HttpVersion._
    e.getChannel().write( new DefaultHttpResponse(HTTP_1_1, CONTINUE))
  }
  
  def onREQ(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val ch= ev.getChannel()
    if (this._request != null) {
      throw new IOException("NettyMsgCB.onREQ: expected to be called once only")
    }
    _request = ev.getMessage().asInstanceOf[HttpRequest]
    if (is100ContinueExpected(_request)) {
      send100Continue(ev )
    }        
    _event= NettyHplr.extract(_src, _request)
    _src.tlog.debug("NettyIO: Received HTTP Request :{}", _request.getMethod )
    _src.tlog.debug("NettyIO: Received Request:\n{}", _event.toString )
    ctx.getPipeline()

//    _event.setScheme(_src.isSSL())
    _keepAlive = JbossIsKeepAlive( _request)

    if (_request.isChunked()) {
      _src.tlog().debug("NettyIO request is chunked")
    } else {
      sockBytes(_request.getContent )
      onMsgFinal(ctx,ev)
    }
  }

  def onChunk(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val chunk = ev.getMessage().asInstanceOf[HttpChunk]
//    HttpChunkTrailer trailer;
    sockBytes(chunk.getContent )
    if (chunk.isLast ) {
      onMsgFinal(ctx,ev)
    }
  }

  private def onMsgFinal(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val cookie = _request.getHeader(COOKIE)
    val data = new XData ( _os match {
      case x:ByteArrayOS => x
      case _ => _fout
    } )

    IOU.closeQuietly(_os)
    _os=null
    _event.setData(data)

    if ( ! STU.isEmpty(cookie)) {
      val cookies = new CookieDecoder().decode(cookie)
      if (!cookies.isEmpty ) {
        // reset the cookies if necessary.
        val enc = new CookieEncoder(true)
        cookies.foreach{ (c) => enc.addCookie(c) }
        _cookie= enc
      }
    }

    val w= new AsyncWaitEvent( _event, new NettyTrigger(_event, ctx.getChannel() ) )
    val evt = w.inner()

    w.timeoutMillis( _src.waitMillis())
    _src.hold(w)
    _src.dispatch(evt)
  }

  private def sockBytes(cbuf:ChannelBuffer) {
    var c=0
    if (cbuf != null) do {
      c=cbuf.readableBytes()
      if (c > 0) {
        sockit_down(cbuf, c)
      }
    } while (c > 0)
  }

  private def sockit_down(cbuf:ChannelBuffer, count:Int) {
    val bits= new Array[Byte](4096)
    val thold= _src.threshold
    var total=count
    while (total > 0) {
      val len = min(4096, total)
      cbuf.readBytes(bits, 0, len)
      _os.write(bits, 0, len)
      total -= len
    }
    _os.flush()
    if (_clen >= 0L) { _clen += count }
    if (_clen > 0L && _clen > thold) {
      swap()
    }
  }

  private def swap() {
    val baos= _os.asInstanceOf[ByteArrayOS]
    val t= newTempFile(true)
    _os=t._2
    _os.write(baos.toByteArray )
    _os.flush()
    _clen= -1L
    _fout= t._1
  }

}

