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

import java.io.OutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.MessageDigest
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.math._
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.{StringUtils => STU}
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{ChannelHandlerContext=>CHContext}
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.slf4j._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.net.NetUtils._
import com.zotoh.frwk.util.StrUtils._
import org.jboss.netty.handler.ssl.SslHandler
import org.apache.tools.ant.taskdefs.condition.Http
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpMessage
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpHeaders
import java.io.{File,ByteArrayOutputStream=>ByteArrayOS}
import org.apache.commons.codec.net.Utils
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener
import java.text.SimpleDateFormat
import java.util.{Date=>JDate,Locale }
import java.util.TimeZone
import com.zotoh.frwk.io.XData
import org.apache.commons.io.{IOUtils=>IOU}
import org.jboss.netty.channel.ChannelFuture



/**
 * @author kenl
 */
object NettyHplr extends CoreImplicits {

  def getHeaders(evt:HTTPEvent, req:HttpRequest) {
    req.getHeaderNames().foreach { (n) =>
      req.getHeaders(n).foreach { (v) =>
        evt.addHeader(n, v)
      }
    }
  }

  def extract( src:NettyIO, req:HttpRequest) = {
    val ev= src.mkEvent()
    var uri= req.getUri()

    ev.setProtocol( req.getProtocolVersion.getProtocolName )
    ev.setMethod( req.getMethod.getName )

    val pos = uri.indexOf("?")
    if (pos >= 0) {
      ev.setQueryString( uri.substring(pos+1) )
      uri= uri.substring(0,pos)
    }
    ev.setUri( uri )

    getHeaders(ev,req)
    getParams(ev, uri)

    ev
  }

  def parseContentType( cType:String ) = {
    var ct="text/html"
    var enc=""
    if( ! STU.isEmpty(cType)) {
      val tokens = cType.split(";")
      ct = tokens(0).trim().lc
      if( tokens.length > 1 ) {
        val ss = tokens(1).split(("="))
        if( ss.length == 2 && ss(0).trim().lc == "charset") {
          enc = STU.strip(ss(1).trim , "\"\'" )
        }
      }
    }
    (ct, if (STU.isEmpty(enc)) "utf-8" else enc )
  }

  def getRemoteAddr(rdr:SocketAddress, req:HttpRequest) = {
    val inet= rdr.asInstanceOf[InetSocketAddress ].getAddress
    val addr= inet match {
      case x:Inet6Address =>
        val s= STU.strip(x.getHostAddress, "/")
        if (! s.matches(".*[%].*")) s else {
          s.substring(0, s.indexOf("%"))
        }
      case x:Inet4Address =>
        val s= STU.strip(x.getHostAddress, "/")
        if ( ! s.matches("[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+[:][0-9]+"))  s else {
          s.substring(0, s.indexOf(":"))
        }
      case _ => ""
    }

    val (host, port) = parseHostPort( nsb( req.getHeader(Names.HOST) ) )
    val h= strim( req.getHeader("x-forwarded-for"))
    if (!STU.isEmpty(h)) {      
      ( h, "", port)
    } else {
      ( if (STU.isEmpty(addr)) inet.getHostName else addr, host, port )      
    }
  }

  def isSSL(ctx:CHContext) = {
    ctx.getPipeline().get(classOf[SslHandler]) != null
  }

  def chKeepAlive(req:HttpRequest ) = {
    HttpHeaders.isKeepAlive(req) && req.getProtocolVersion() == HttpVersion.HTTP_1_1
  }

  def setContentLength(msg:HttpMessage, clen:Long) {
    msg.setHeader(Names.CONTENT_LENGTH, clen.toString )
  }

  def calcHybiSecKeyAccept(key:String) = {
    // add fix GUID according to
    // http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-10
    val k = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    Base64.encodeBase64String( MessageDigest.getInstance("SHA-1").
              digest(k.getBytes("utf-8")) )
  }

  def content(cbuf:ChannelBuffer, os:OutputStream) {
    //int pos= cbuf.readerIndex();
    val bits= new Array[Byte](4096)
    var cl= cbuf.readableBytes()
    while (cl > 0) {
      val len = min(4096, cl)
      cbuf.readBytes(bits, 0, len)
      os.write(bits, 0, len)
      cl -= len
    }
    os.flush()
  }

  def getParams(evt:HTTPEvent, uri:String ) {
    val params = new QueryStringDecoder( uri).getParameters()
    if (params != null) params.foreach{ (t) =>
      evt.addParam(t._1, t._2.toSeq )
    }
  }

  def sockBytes(cbuf:ChannelBuffer, thold:Long) = {
    var ctx: (OutputStream,Long,Long,File) = ( new ByteArrayOS(4096), 0L, thold,null)
    var c=0
    if (cbuf != null) do {
      c=cbuf.readableBytes()
      if (c > 0) {
        ctx = sockit_down(cbuf, c, ctx)
      }
    } while (c > 0)
    ctx
  }

  private def sockit_down(cbuf:ChannelBuffer, count:Int, ctx:(OutputStream,Long,Long,File) ) = {
    val bits= new Array[Byte](4096)
    var total=count
    var clen=ctx._2
    var os=ctx._1
    var fp=ctx._4

    while (total > 0) {
      val len = min(4096, total)
      cbuf.readBytes(bits, 0, len)
      os.write(bits, 0, len)
      os.flush()
      total -= len
      if (clen >= 0L) { clen += count }
      if (clen > 0L && clen > ctx._3) {
        val t=swap(ctx._1)
        fp=t._1
        os=t._2
        clen= -1L
      }
    }

    (os, clen, ctx._3, fp)
  }

  private def swap(os:OutputStream) = {
    val baos= os.asInstanceOf[ByteArrayOS]
    val t= newTempFile(true)
    val nos=t._2
    nos.write(baos.toByteArray )
    nos.flush()
    os.close()
    t
  }

  def closeCF(doit:Boolean, cf:ChannelFuture) {
    if (doit && cf != null) { cf.addListener(ChannelFutureListener.CLOSE) }
  }

  private val _log= LoggerFactory.getLogger(classOf[NettyHplr])
  def tlog() = _log

}

sealed class NettyHplr {}


