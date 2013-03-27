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
import com.zotoh.frwk.util.StrArr


/**
 * @author kenl
 */
object HTTPClient {

  val _netty = new ThreadLocal[NettyClientIO]() {
    lazy val _io=new NettyClientIO
    override def initialValue() = _io
    override def finalize {
      block { () => _io._http.releaseExternalResources }       
      block { () => _io._ssl.releaseExternalResources }       
    }
  }
  
  /**
   * test
   *
   */
  def main(args:Array[String]) {
    try {
      val data= new XData(new File("/tmp/play.zip")).setDeleteFile(false)
      val c= new HTTPClient()
      // uri input is assumed to be encoded!
      val uri = new URI("http://www.yahoo.com/?abc=123+567&c=d")
      println( uri.toString)
      println( uri.toASCIIString())
      c.connect(uri)
      c.get(new BasicHTTPMsgIO(){
        def onOK(ctx:HTTPMsgInfo, resOut:XData) {
          println( resOut.stringify )          
          c.wake()
        }
      } )
      c.join()
      c.finz()
    } catch {
      case e:Throwable => e.printStackTrace()
    }
    sys.exit(0)
  }

//  private val _log= LoggerFactory.getLogger(classOf[HTTPClient])
}

/**
 * @author kenl
 *
 */
class HTTPClient extends HTTPClientBase {

  private var _CH:Channel = null
  private val _lock= new Object()

  import HTTPClient._
  import HTTPUtils._

  /**
   * @param cfg
   * @param data
   */
  def post( contentType:String, data:XData, cfg:HTTPMsgIO ) {
    tstObjArg("payload-callback", cfg)
    tstObjArg("payload-data", data)
    send( contentType, create_request(HttpMethod.POST) , cfg, data)
  }

  /**
   * @param cfg
   */
  def get(cfg:HTTPMsgIO ) {
    tstObjArg("payload-callback", cfg)
    send( "", create_request(HttpMethod.GET), cfg, new XData() )
  }

  
  /**
   *
   */
  def join()  {    freeze(_lock)  }

  /**
   *
   */
  def wake() {    thaw(_lock)  }

//  override def finalize() {
//    if (_boot != null) { _boot.releaseExternalResources }
//    super.finalize()
//  }

  /**
   * @param remote
   */
  protected def connect(host:String,port:Int) {
    
    val (bs, cg) = inizPipeline()

    val cf= bs.connect(new InetSocketAddress(host, port))
    // wait until the connection attempt succeeds or fails.
    cf.awaitUninterruptibly

    if (cf.isSuccess) {
      cg.add( cf.getChannel )
      _CH= cf.getChannel 
      tlog.debug("HTTPClient: connected OK to host: {}, port: {}{}", host, asJObj(port),"")
    } else {
      onError(cf.getCause)
    }

  }

  private def send(contentType:String, req:HttpRequest, io:HTTPMsgIO, data:XData) {

    tlog.debug("HTTPClient: {} {}", (if(data.hasContent) "POST" else "GET"), _remote.toString, "")

    val clen= if (data.hasContent) data.size else 0L
    var cfg:HTTPMsgIO = if (io == null) {
      new BasicHTTPMsgIO() {
        def onOK(ctx:HTTPMsgInfo, res:XData) {}
      }
    } else {
      io
    }

    req.setHeader(HttpHeaders.Names.CONNECTION,
      if (cfg.keepAlive) HttpHeaders.Values.KEEP_ALIVE else HttpHeaders.Values.CLOSE)

    tlog.debug("HTTPClient: content has length: {}", asJObj(clen))
    req.setHeader("content-length", clen.toString)

    req.setHeader(HttpHeaders.Names.HOST, _remote.getHost)
    // allow for extra settings via the input object
    cfg.configMsg(req)

    if (data.hasContent &&   STU.isEmpty( req.getHeader("content-type")) &&
        !STU.isEmpty(contentType)) {        
      req.setHeader("content-type", contentType)
    }

    val h= _CH.getPipeline.get("handler").asInstanceOf[HTTPResponseHdlr]
    h.bind(cfg)

    tlog.debug("HTTPClient: about to flush out request (headers)")
    var f= _CH.write(req)
    f.addListener( newChFLnr ( (x) => tlog.debug("HTTPClient: req headers flushed")  ) )

    if (clen > 0L) {
      f= if (clen > HTTPUtils.dftThreshold ) {
        _CH.write(new ChunkedStream( data.stream))
      } else {
        data.bytes match {
          case Some(b) => _CH.write( new ByteBufferBackedChannelBuffer( ByteBuffer.wrap( b) ))
          case _ => throw new IOException("Bad input data")
        }
      }      
      f.addListener( newChFLnr ( (x) => tlog.debug("HTTPClient: req payload flushed")  ) )
    }

  }

  def close() {
    tlog.debug("HTTPClient: close()")
    block { () =>
      _CH.close
    }
  }

  private def create_request(m:HttpMethod) = {
    new DefaultHttpRequest( HttpVersion.HTTP_1_1, m, _remote.toASCIIString())
  }

  private def inizPipeline() = {

    val bs= if (_ssl) _netty.get._ssl else _netty.get._http
    val cg= if (_ssl) _netty.get._scg else _netty.get._pcg
    
    bs.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl= org.jboss.netty.channel.Channels.pipeline()
        if (_ssl) {
          val eng = HTTPUtils.clientSSL().createSSLEngine
          eng.setUseClientMode(true)
          pl.addLast("ssl", new SslHandler(eng))
        }
        pl.addLast("codec", new HttpClientCodec())
//        pipe.addLast("inflater", new HttpContentDecompressor())
        //pipeline.addLast("aggregator", new HttpChunkAggregator(1048576))
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", new HTTPResponseHdlr( cg))
        pl
      }
    })

    (bs, cg)
  }

  private def onError(t:Throwable) {
    t match {
      case e:Exception => 
      throw e
      case e:Throwable =>
      throw new IOException( if(e==null) "Failed to connect" else e.getMessage)
    }
  }

}

sealed class NettyClientIO {
    
    val _http = new ClientBootstrap( new NioClientSocketChannelFactory (
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()))
    val _ssl = new ClientBootstrap( new NioClientSocketChannelFactory (
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()))
    val _pcg= new DefaultChannelGroup(uid )
    val _scg= new DefaultChannelGroup(uid )
    
    _http.setOption("tcpNoDelay" , true)
    _http.setOption("keepAlive", true)    
    _ssl.setOption("tcpNoDelay" , true)
    _ssl.setOption("keepAlive", true)
    
}

