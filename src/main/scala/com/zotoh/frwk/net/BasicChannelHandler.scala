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

package com.zotoh.frwk
package net

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.math._

import org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.{CoreImplicits,StrArr}
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._

import java.io.{ByteArrayOutputStream=>ByteArrayOS,OutputStream}
import java.io.{IOException,File}
import java.util.{Set,Properties=>JPS}

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.handler.codec.http.Cookie
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMessage
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.io.XData
import org.slf4j._
import org.apache.commons.io.{IOUtils=>IOU}

object BasicChannelHandler {
  private val _log= LoggerFactory.getLogger(classOf[BasicChannelHandler])
}

/**
 * @author kenl
 *
 */
class BasicChannelHandler( private var _grp:ChannelGroup) extends SimpleChannelHandler with CoreImplicits {

  def tlog() = BasicChannelHandler._log

  import HTTPUtils._
  
  private var _thold= dftThreshold()
  private val _props= new JPS()
  private var _clen=0L
  private var _keepAlive=false

  private var _cookies:CookieEncoder= null
  private var _fOut:File = null
  private var _os:OutputStream = null

  def isKeepAlive() = _keepAlive

  override def channelClosed(ctx:ChannelHandlerContext, ev:ChannelStateEvent) {
    val c= maybeGetChannel(ctx,ev)
    if (c != null) { _grp.remove(c) }
    super.channelClosed(ctx, ev)

    tlog.debug("BasicChannelHandler: channelClosed - ctx {}, channel {}",  ctx, if(c==null) "?" else c , "")
  }

  override def channelOpen(ctx:ChannelHandlerContext, ev:ChannelStateEvent) {
    val c= maybeGetChannel(ctx,ev)
    if (c != null) { _grp.add(c) }
    super.channelOpen(ctx, ev)

    tlog().debug("BasicChannelHandler: channelOpen - ctx {}, channel {}", ctx, if (c==null) "?" else c, "")
  }

  override def exceptionCaught(ctx:ChannelHandlerContext, ev:ExceptionEvent) {

    tlog().error("", ev.getCause)

    val c= maybeGetChannel(ctx, ev)
    if (c != null) try {
        c.close()
    } finally {
        _grp.remove(c)
    }
//    super.exceptionCaught(ctx, e)
  }

  protected def onRecvRequest( ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpRequest) = true
  // false to stop further processing

  override def messageReceived(ctx:ChannelHandlerContext, ev:MessageEvent) {

    val msg = ev.getMessage()
    msg match {
      case x:HttpMessage =>
        _os= new ByteArrayOS(4096)
        _props.clear
        msg_recv_0(x)
      case x:HttpChunk => // no op
    }

    msg match {
      case res:HttpResponse =>
        val s= res.getStatus()
        val r= s.getReasonPhrase()
        val c= s.getCode()
        tlog().debug("BasicChannelHandler: got a response: code {} {}", asJObj(c), asJObj(r), "")
        _props.add("reason", r).add("dir", -1).add("code", c)
        if (c >= 200 && c < 300) {
          onRes(s,ctx,ev,res)
        } else if (c >= 300 && c < 400) {
          // TODO: handle redirect
          onResError(ctx,ev)
        } else {
          onResError(ctx,ev)
        }

      case req:HttpRequest =>
        tlog().debug("BasicChannelHandler: got a request: ")
        onReqIniz(ctx,ev, req)
        _keepAlive = HttpHeaders.isKeepAlive(req)
        _props.put("dir", asJObj(1) )
        if ( onRecvRequest(ctx,ev,req) ) {
          onReq(ctx,ev,req)
        }

      case x:HttpChunk => onChunk(ctx,ev,x)

      case _ =>
        throw new IOException( "BasicChannelHandler:  unexpected msg type: " +  safeGetClzname(msg))
    }

  }

  private def onReq(ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpRequest) {
    if (msg.isChunked) {
      tlog.debug("BasicChannelHandler: request is chunked")
    } else {
      sockBytes(msg.getContent)
      onMsgFinal(ctx,ev)
    }
  }

  private def onRes(rc:HttpResponseStatus, ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpResponse) {
    onResIniz(ctx,ev,msg)
    if (msg.isChunked) {
      tlog.debug("BasicChannelHandler: response is chunked")
    } else {
      sockBytes(msg.getContent)
      onMsgFinal(ctx,ev)
    }
  }

  protected def onReqIniz(ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpRequest ) {
    val m= msg.getMethod().getName
    val uri= msg.getUri
    tlog.debug("BasicChannelHandler: onReqIniz: Method {}, Uri {}",m, uri, "")
    onReqPreamble(m, uri, iterHeaders(msg))
  }

  protected def onResIniz(ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpResponse ) {
    onResPreamble( iterHeaders(msg))
  }

  protected def onReqPreamble(mtd:String, uri:String, h:Map[String,StrArr] ) {}
  protected def onResPreamble(h:Map[String,StrArr]) {}

  protected def doReqFinal(code:Int, r:String, out:XData) {}
  protected def doResFinal(code:Int, r:String, out:XData) {}
  protected def onResError(code:Int, r:String) {}

  private def onResError(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val cc= maybeGetChannel(ctx,ev)
    onResError( _props.geti("code"), _props.gets("reason"))
    if ( !isKeepAlive && cc != null) {
      cc.close()
    }
  }

  private def sockBytes(cb:ChannelBuffer) {
    var loop=true
    if (cb != null) while (loop) {
      loop = cb.readableBytes() match {
        case c if c > 0 =>
          sockit(cb,c)
          true
        case _ => false
      }
    }
  }

  private def sockit(cb:ChannelBuffer, count:Int) {

    val bits= new Array[Byte](4096)
    var total=count

//    tlog().debug("BasicChannelHandler: socking it down {} bytes", count)

    while (total > 0) {
      val len = min(4096, total)
      cb.readBytes(bits, 0, len)
      _os.write(bits, 0, len)
      total -= len
    }

    _os.flush()

    if (_clen >= 0L) { _clen += count }

    if (_clen > 0L && _clen > _thold) {
      swap()
    }
  }

  private def swap() {
    _os match {
      case baos:ByteArrayOS =>
        val t= newTempFile(true)
        t._2.write(baos.toByteArray)
        t._2.flush()
        _fOut= t._1
        _os=t._2
        _clen= -1L
    }
  }

  protected def doReplyError(ctx:ChannelHandlerContext, ev:MessageEvent, err:HttpResponseStatus) {
    doReplyXXX(ctx,ev,err)
  }

  private def doReplyXXX(ctx:ChannelHandlerContext, ev:MessageEvent, s:HttpResponseStatus) {
    val res= new DefaultHttpResponse(HttpVersion.HTTP_1_1, s)
    val c= maybeGetChannel(ctx,ev)
    res.setChunked(false)
    res.setHeader("content-length", "0")
    c.write(res)
    if ( ! isKeepAlive && c != null ) {
      c.close()
    }
  }

  protected def doReqFinal(ctx:ChannelHandlerContext, ev:MessageEvent, inData:XData) {
    doReplyXXX(ctx,ev,HttpResponseStatus.OK)
  }

  private def onMsgFinal(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val dir = _props.geti("dir")
    val out= on_msg_final(ev)
    if ( dir > 0) {
      doReqFinal(ctx,ev,out)
    } else if (dir < 0) {
      doResFinal( _props.geti("code"), _props.gets("reason"),out)
    }
  }

  private def on_msg_final(ev:MessageEvent) = {
    val data= new XData()
    if (_fOut != null) {
      data.resetMsgContent(_fOut)
    } else {
      data.resetMsgContent(_os)
    }

    IOU.closeQuietly(_os)
    _fOut=null
    _os=null

    data
  }

  /**
   * @param t
   * @return
   */
  def withThreshold(t:Long): this.type = {
    _thold=t
    this
  }

  /**
   * @param ctx
   * @param ev
   * @return
   */
  protected def maybeGetChannel(ctx:ChannelHandlerContext, ev:ChannelEvent) = {
    val cc= ev.getChannel
    if (cc==null) ctx.getChannel else cc
  }

  private def msg_recv_0(msg:HttpMessage) {
    val s= msg.getHeader(COOKIE)
    if ( ! STU.isEmpty(s)) {
      val cookies = new CookieDecoder().decode(s)
      val enc = new CookieEncoder(true)
      cookies.foreach { (c) =>  enc.addCookie(c)  }
      _cookies= enc
    }
  }

  private def onChunk(ctx:ChannelHandlerContext, ev:MessageEvent, msg:HttpChunk ) {
    sockBytes(msg.getContent)
    if (msg.isLast) {
      onMsgFinal(ctx,ev)
    }
  }

  protected def iterHeaders(msg:HttpMessage) = {
    val hdrs= mutable.HashMap[String,StrArr]()

    msg.getHeaderNames().foreach { (n) =>
      hdrs += n -> new StrArr().add( msg.getHeaders(n).toSeq )
    }

    if (tlog.isDebugEnabled) {
      val dbg=new StringBuilder(1024)
      hdrs.foreach { (t) =>
        dbg.append(t._1).append(": ").append(t._2).append("\r\n")
      }
      tlog.debug("HttpResponseHdlr: headers\n{}", dbg.toString )
    }

    hdrs.toMap
  }

}
