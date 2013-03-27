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
import com.zotoh.frwk.util.StrUtils._
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.channel.{ChannelHandlerContext=>CHContext}
import org.jboss.netty.handler.codec.http.HttpVersion
import org.apache.commons.lang3.{StringUtils => STU}
import java.text.SimpleDateFormat
import java.io.{ByteArrayOutputStream=>ByteArrayOS}
import java.util.{Date=>JDate,TimeZone,Locale}
import com.zotoh.blason.io.NettyHplr._
import org.jboss.netty.buffer.ChannelBuffers
import java.net.InetSocketAddress
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.frwk.io.XData
import org.apache.commons.io.{IOUtils=>IOU}
import org.jboss.netty.handler.codec.http.CookieDecoder
import java.net.HttpCookie
import org.jboss.netty.handler.codec.http.Cookie
import com.zotoh.frwk.util.CoreImplicits
import java.net.URLDecoder
import org.apache.commons.fileupload.ParameterParser
import org.apache.commons.fileupload.MultipartStream
import java.io.OutputStream
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.FileItem
import com.zotoh.frwk.net.ULFileFactory
import com.zotoh.frwk.net.ULFileItem


/**
 * @author kenl
 */
object MVCHplr extends CoreImplicits {

  import MVCSession._

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

    evt.setContentTypeLine(req.getHeader(Names.CONTENT_TYPE))
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

    evt.setCookies( getCookies(req) )
    getHeaders(evt, req)
    getParams(evt, uri)
    
    val payload= req.getContent match {
      case f:FileChannelBuffer => new XData(f.file)
      case c =>
        val t=sockBytes(c, src.threshold )
        val x=new XData( if (t._4 != null) t._4 else t._1 )
        IOU.closeQuietly(t._1)
        x
    }
    evt.setData(payload)

    if (isMultipart(evt)) {
      handleMultipart(evt)
    }
    else if (isFormPOST(evt)) {
      maybeGetFormData(evt)
    }

    try { resurrect( evt) } finally {
      tlog.debug("{}", evt.toString)
      if (evt.data != null) {
        tlog.debug("PAYLOAD:\n{}", new String( evt.data.javaBytes, "utf-8") )
      }
    }

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
      WebPage.getTemplate("notfound.html") match {
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

  def handleMultipart(evt:HTTPEvent) {
    val fac= new ULFileFactory()
    val upload = new NettyFileUpload(fac)
    upload.setSizeMax(10000000)
    val items = upload.parseRequest(evt)
    items.foreach { _ match {
      case fi:ULFileItem =>
        if (fi.isFormField) { 
          getFormField(fi,evt) 
        } else {
          getUploadFile(fi,evt)
        }
      case _ =>
    }}    
  }
  
  private def getUploadFile(fi:ULFileItem, evt:HTTPEvent) {
    evt.addFile(fi)
    fi.getContentType()
    fi.getFieldName()
    fi.getName()
  }
  
  private def getFormField(fi:ULFileItem, evt:HTTPEvent) {
    evt.addParam( nsb( fi.getFieldName ), nsb( fi.getString ))
  }
  
  private def isFormPOST(evt:HTTPEvent) = {
    evt.contentType.lc == "application/x-www-form-urlencoded"
  }
  
  private def isMultipart(evt:HTTPEvent) = {
    evt.contentType.lc.startsWith("multipart/")
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
    val rc= mutable.HashMap[String, Cookie]()
    if (cs != null) cs.foreach { (c) =>
      rc.put (c.getName, c)
    }
    rc.toMap
  }

  private def maybeGetFormData(evt:HTTPEvent) {
    if ( evt.hasData ) {
      val formData = nsb ( URLDecoder.decode( evt.data.stringify , evt.encoding) )
      formData.split("&").foreach { (s) =>
        val nv= s.split("=")
        var k=""
        var v=""
        if (nv.length > 0) {
          k=nv(0).trim()
        }
        if (nv.length > 1) {
          v=nv(1).trim()
        }
        evt.addParam(k, v)
      }
    }
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
