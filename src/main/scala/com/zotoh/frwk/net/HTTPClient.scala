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

import org.jboss.netty.channel.Channels.pipeline
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.lang3.{StringUtils=>STU}
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import javax.net.ssl.SSLEngine
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedStream
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.handler.codec.http.HttpClientCodec



object HTTPClient {

  /**
   * test
   *
   */
  def main(args:Array[String]) {
    try {
      val data= new XData(new File("/tmp/play.zip")).setDeleteFile(false)
      val c= new HTTPClient()
      c.connect(new URI("http://localhost:8080/p.zip"))
      c.post(new BasicHTTPMsgIO(){
        def onOK(code:Int, reason:String, resOut:XData) {
          println("COOL")
          c.wake()
        }
      }, data)
      c.block()
      c.finz()
    } catch {
      case e:Throwable => e.printStackTrace()
    }
    sys.exit(0)
  }

  private val _log= LoggerFactory.getLogger(classOf[HTTPClient])
}

/**
 * @author kenl
 *
 */
class HTTPClient {

  import HTTPClient._
  def tlog() = _log

  private var _curScope:(URI,ChannelFuture) = null
  private var _boot:ClientBootstrap = null
  private var _chs:ChannelGroup = null
  private val _lock= new Object()

  iniz()

  /**
   *
   */
  def block()  {
    freeze(_lock)
  }

  /**
   *
   */
  def wake() {
    thaw(_lock)
  }

  override def finalize() {
    if (_boot != null) { _boot.releaseExternalResources }
    super.finalize()
  }

  /**
   * @param remote
   */
  def connect(remote:URI) {
    val ssl= "https" == remote.getScheme()
    val host= remote.getHost
    var port= remote.getPort
    if (port < 0) { port = if(ssl) 443 else 80 }

    tlog.debug("{}: connecting to host: {}, port: {}", "HTTPClient", host, asJObj(port))
    inizPipeline(ssl)

    val cf= _boot.connect(new InetSocketAddress(host, port))
    // wait until the connection attempt succeeds or fails.
    cf.awaitUninterruptibly

    if (cf.isSuccess) {
      _curScope= (remote, cf)
      _chs.add(cf.getChannel)
    } else {
      onError(cf.getCause)
    }

    tlog.debug("{}: connected OK to host: {}, port: {}", "HTTPClient", host, asJObj(port))

  }

  /**
   * @param cfg
   * @param data
   */
  def post(cfg:HTTPMsgIO, data:XData) {
    tstObjArg("scope-data", _curScope)
    tstObjArg("payload-data", data)
    send( create_request(HttpMethod.POST) , cfg, data)
  }

  /**
   * @param cfg
   */
  def get(cfg:HTTPMsgIO) {
    tstObjArg("scope-data", _curScope)
    send( create_request(HttpMethod.GET), cfg, new XData() )
  }

  private def send(req:HttpRequest, io:HTTPMsgIO, data:XData) {

    tlog.debug("{}: {} {}", "HTTPClient", (if(data.hasContent) "POST" else "GET"), _curScope._1)

    val clen= if (data.hasContent) data.size else 0L
    val uri= _curScope._1
    val cf= _curScope._2
    var cfg:HTTPMsgIO = if (io == null) {
      new BasicHTTPMsgIO() {
        def onOK(code:Int, reason:String, res:XData) {}
        //def onError(code:Int, reason:String) {}
      }
    } else {
      io
    }

    req.setHeader(HttpHeaders.Names.CONNECTION,
      if (cfg.keepAlive) HttpHeaders.Values.KEEP_ALIVE else HttpHeaders.Values.CLOSE)

    if (data.hasContent && STU.isEmpty( req.getHeader("content-type"))) {
      req.setHeader("content-type", "application/octet-stream")
    }

    tlog.debug("HTTPClient: content has length: {}", asJObj(clen))
    req.setHeader("content-length", clen.toString)

    req.setHeader(HttpHeaders.Names.HOST, uri.getHost)
    cfg.configMsg(req)

    val cc= cf.getChannel
    val h= cc.getPipeline().get("handler").asInstanceOf[HTTPResponseHdlr]
    h.bind(cfg)

    tlog.debug("HTTPClient: about to flush out request (headers)")
    var f= cc.write(req)
    f.addListener(new ChannelFutureListener() {
      def operationComplete(fff:ChannelFuture) {
        tlog.debug("HTTPClient: req headers flushed")
      }
    })

    if (clen > 0L) {
      f=if (clen > HTTPUtils.dftThreshold ) {
        cc.write(new ChunkedStream( data.stream))
      } else {
        data.bytes match {
          case Some(b) => cc.write( new ByteBufferBackedChannelBuffer( ByteBuffer.wrap( b) ))
          case _ => throw new IOException("Bad input data")
        }
      }
      f.addListener(new ChannelFutureListener() {
        def operationComplete(fff:ChannelFuture) {
          tlog.debug("HTTPClient: req payload flushed")
        }
      })
    }

  }

  /**
   *
   */
  def finz() {
    tlog.debug("HTTPClient: finz()")
    close()
    try { _boot.releaseExternalResources } finally { _boot=null }
  }

  /**
   *
   */
  def close() {
    tlog.debug("HTTPClient: close()")
    if (_curScope != null) try { _chs.close() } finally {  _curScope=null  }
  }

  private def create_request(m:HttpMethod) = {
    new DefaultHttpRequest( HttpVersion.HTTP_1_1, m, _curScope._1.toASCIIString())
  }

  private def inizPipeline(ssl:Boolean) {

    _boot.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl= org.jboss.netty.channel.Channels.pipeline()
        if (ssl) {
          val eng = HTTPUtils.clientSSL().createSSLEngine
          eng.setUseClientMode(true)
          pl.addLast("ssl", new SslHandler(eng))
        }
        pl.addLast("codec", new HttpClientCodec())
//        pipe.addLast("inflater", new HttpContentDecompressor())
        //pipeline.addLast("aggregator", new HttpChunkAggregator(1048576))
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", new HTTPResponseHdlr(_chs))
        pl
      }
    })

  }

  private def onError(t:Throwable) {
    t match {
      case e:Exception => 
      throw e
      case e:Throwable =>
      throw new IOException( if(e==null) "Failed to connect" else e.getMessage)
    }
  }

  private def iniz() {
    _boot = new ClientBootstrap( new NioClientSocketChannelFactory (
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()))
    _boot.setOption("tcpNoDelay" , true)
    _boot.setOption("keepAlive", true)
    _chs= new DefaultChannelGroup(uid )
  }

}
