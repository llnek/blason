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

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.jboss.netty.handler.codec.http.HttpResponse
import com.zotoh.blason.io.NettyIO
import org.jboss.netty.handler.codec.http.HttpRequest
import java.io.File
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpHeaders.Names
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.channel.{ChannelHandlerContext=>CHContext}
import org.jboss.netty.handler.codec.http.HttpVersion
import org.apache.commons.lang3.{StringUtils => STU}
import java.text.SimpleDateFormat
import java.util.{Date=>JDate,TimeZone,Locale}
import com.zotoh.blason.io.NettyHplr._
import org.jboss.netty.buffer.ChannelBuffers

import java.net.InetSocketAddress
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.frwk.io.XData
import org.apache.commons.io.{IOUtils=>IOU}
import org.jboss.netty.handler.codec.http.CookieDecoder


/**
 * @author kenl
 */
object MVCHplr {
  
  def extract ( src:NettyMVC, ctx:CHContext, req:HttpRequest): HTTPEvent = {
    val (cType, enc) = parseContentType( req.getHeader(Names.CONTENT_TYPE) )
    val uri = req.getUri
    tlog.debug("MVC received RequestUri: {}", uri)
    val (path, qry) = uri.indexOf("?") match {
      case pos:Int if pos >= 0 =>
        ( uri.substring(0, pos) , uri.substring( pos+1) )
      case _ =>
        (uri, "")
    }
    val method = req.getHeader("X-HTTP-Method-Override") match {
      case s:String if !STU.isEmpty(s) => s
      case _ => req.getMethod().getName
    }
    val (remote, host, port ) = getRemoteAddr( ctx.getChannel().getRemoteAddress(), req)
    val evt= src.mkEvent()
    val ch= ctx.getChannel()

    evt.setKeepAlive( chKeepAlive(req) )
    evt.setContentType(cType)
    
    evt.setRemoteAddr(remote )
    evt.setRemoteHost(host)
    if (port < 0) {
      evt.setRemotePort( if (isSSL(ctx) ) 443 else 80 )      
    } else {
      evt.setRemotePort(port)      
    }
    
/*
    ch.getRemoteAddress() match {
      case x:InetSocketAddress =>
      case _ =>
    } */
    ch.getLocalAddress() match {
      case x:InetSocketAddress =>
        evt.setLocalHost(x.getHostName)
        evt.setLocalAddr(x.getAddress().getHostAddress )
        evt.setLocalPort(x.getPort)
      case _ =>
    }

    evt.setProtocol( req.getProtocolVersion.getProtocolName )
    evt.setScheme( if ( isSSL(ctx) ) "https" else "http" )
    evt.setMethod( method )
    evt.setDomain(host)
    evt.setQueryString(qry)
    evt.setUri(path)


    getHeaders(evt, req)
    getParams(evt, uri)

    req.getContent match {
      case f:FileChannelBuffer => new XData(f.file)
      case c =>
        val t=sockBytes(c, src.threshold )
        val x=new XData( if (t._4 != null) t._4 else t._1 )
        IOU.closeQuietly(t._1)
        x
    }

    evt
  }
  
  def sendRedirect(ctx:CHContext, perm:Boolean, targetUrl:String) = {
    val rsp=new DefaultHttpResponse(HttpVersion.HTTP_1_1,
        if (perm) HttpResponseStatus.MOVED_PERMANENTLY else HttpResponseStatus.TEMPORARY_REDIRECT
        )
    tlog.debug("Reroute {} to {}{}", asJObj(rsp.getStatus.getCode), targetUrl, "")
    rsp.setHeader("location", targetUrl)
    closeCF(true, ctx.getChannel().write(rsp) )
  }
    
  def addETag(src:NettyMVC, req:HttpRequest, rsp:HttpResponse, file:File )  {
    
    val maxAge = src.getCacheMaxAgeSecs()
    val lastTm = file.lastModified()
    val eTag = "\"" + lastTm + "-" + file.hashCode + "\""
    
    if (  isModified(eTag, lastTm, req)) {
      rsp.setHeader(Names.LAST_MODIFIED, getDF.format( new JDate(lastTm )))
    }
    else if (req.getMethod == HttpMethod.GET) {
      rsp.setStatus(HttpResponseStatus.NOT_MODIFIED)      
    }

    rsp.setHeader( Names.CACHE_CONTROL,
        if (maxAge == 0) "no-cache" else { "max-age=" + maxAge }
    )
    
    if (src.getUseETag ) {
      rsp.setHeader(Names.ETAG, eTag)
    }
    
  }

  def trap404( ctx:CHContext) {
    val rsp = mkHttpReply(HttpResponseStatus.NOT_FOUND)
    try {
      WebPage.getTemplate("404.html") match {
        case Some(tp) =>
          rsp.setHeader(Names.CONTENT_TYPE, tp.contentType )
          val bits = tp.body.getBytes("utf-8")
          setContentLength(rsp, bits.length ) 
          rsp.setContent( ChannelBuffers.copiedBuffer(bits) )
        case _ =>
      }
      closeCF(true, ctx.getChannel().write(rsp) )
    } catch {
      case e:Throwable =>
        tlog.warn("",e)
        ctx.getChannel.close()
    }

  }

  def trap403( ctx:CHContext) {
    val rsp = mkHttpReply(HttpResponseStatus.FORBIDDEN)
    try {
      WebPage.getTemplate("403.html") match {
        case Some(tp) =>
          rsp.setHeader(Names.CONTENT_TYPE, tp.contentType )
          val bits = tp.body.getBytes("utf-8")
          setContentLength(rsp, bits.length ) 
          rsp.setContent( ChannelBuffers.copiedBuffer(bits) )
        case _ =>
      }
      closeCF(true, ctx.getChannel().write(rsp) )
    } catch {
      case e:Throwable =>
        tlog.warn("",e)
        ctx.getChannel.close()
    }

  }
  
  def trapFatal( ctx:CHContext) {
    val rsp = mkHttpReply(HttpResponseStatus.INTERNAL_SERVER_ERROR)
    try {
      WebPage.getTemplate("500.html") match {
        case Some(tp) =>
          rsp.setHeader(Names.CONTENT_TYPE, tp.contentType )
          val bits = tp.body.getBytes("utf-8")
          setContentLength(rsp, bits.length ) 
          rsp.setContent( ChannelBuffers.copiedBuffer(bits) )
        case _ =>
      }
      closeCF(true, ctx.getChannel().write(rsp) )
    } catch {
      case e:Throwable =>
        tlog.warn("",e)
        ctx.getChannel.close()
    }

  }
  
  def handleStatic( src:NettyMVC, ctx:CHContext, req:HttpRequest, file:File) {      
    tlog.debug("MVC: serve static: {}", niceFPath(file))    
    val rsp = mkHttpReply(HttpResponseStatus.OK)
    try {
      if (file == null || !file.exists()) { trap404( ctx) } else {
        
        addETag(src, req, rsp, file)

        if (rsp.getStatus == HttpResponseStatus.NOT_MODIFIED ) {
          closeCF(  ! chKeepAlive(req), ctx.getChannel.write(rsp) )
        } else {
          HTTPIO.getFile( src, ctx, req, rsp, file)
        }
      }
    } catch {
      case e:Throwable =>
        tlog.error("Failed to get static resource \"{}\""+ req.getUri, e)
        block { () =>
          val error = mkHttpReply(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          val msg = "Internal Error (check logs)"
          val bytes = msg.getBytes( "utf-8") //TODO
          val buf = ChannelBuffers.copiedBuffer(bytes)
          setContentLength(error, bytes.length)
          error.setContent(buf)
          closeCF(true,ctx.getChannel().write(error))
        }
    }

  }

  private def mkHttpReply(s:HttpResponseStatus) = {
    new DefaultHttpResponse(HttpVersion.HTTP_1_1, s)
  }
  
  private def isModified(eTag:String, lastTm:Long, req:HttpRequest ) = {
    
    var mod= true
    
    if (req.containsHeader(Names.IF_NONE_MATCH)) {
        mod= eTag != req.getHeader(Names.IF_NONE_MATCH)        
    }
    else
    if (req.containsHeader(Names.IF_MODIFIED_SINCE)) {
        val s = req.getHeader(Names.IF_MODIFIED_SINCE)
        if ( ! STU.isEmpty(s)) block { () =>
          if ( getDF.parse(s).getTime >= lastTm ) {
            mod=false
          }
        }
    }
    
    mod
  }
  
  def getCookies( req:HttpRequest) = {
    val cs= req.getHeader( Names.COOKIE) match {
      case s if !STU.isEmpty(s) =>new CookieDecoder().decode(s )
      case _ =>null
    }
    val rc= mutable.HashMap[String, WebCookie]()
    cs.foreach { (c) =>
      val k = WebCookie()
      k.setHttpOnly( c.isHttpOnly )
      k.setSecure( c.isSecure )
      k.name = c.getName()
      k.path = c.getPath()
      k.value = c.getValue()
      k.domain = c.getDomain()
      rc.put(k.name, k)
    }
    rc.toMap
  }

  private val _fmt = new ThreadLocal[SimpleDateFormat]() {
    override def initialValue() = {
      val f=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US) 
      f.setTimeZone(TimeZone.getTimeZone("GMT"))
      f
    }
  }
  private def getDF() = _fmt.get
    
  private val _log = LoggerFactory.getLogger(classOf[MVCHplr])
  def tlog() = _log
}

sealed class MVCHplr {}