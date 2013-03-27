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

import org.jboss.netty.handler.codec.http.HttpServerCodec
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.channel.Channels.pipeline
import javax.net.ssl.SSLEngine
import java.io.IOException
import java.net.URL
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.group.ChannelGroup
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrArr
import org.jboss.netty.handler.codec.http.HttpRequest


/**
 * @author kenl
 *
 */
class MemHTTPServer(vdir:String,host:String,port:Int) extends MemXXXServer(vdir,host,port) {

  private var _cb:HTTPMsgIO = null

  /**
   * @param vdir
   * @param key
   * @param pwd
   * @param host
   * @param port
   */
  def this(vdir:String, key:URL, pwd:String, host:String, port:Int) {
    this(vdir, host,port)
    setKeyAuth(key,pwd)
  }

  def bind(cb:HTTPMsgIO): this.type = {
    _cb=cb; this
  }

  override def pipelineFac(eg:SSLEngine) = {
    val g= this.channels()
    new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl= org.jboss.netty.channel.Channels.pipeline()
        if (eg != null) { pl.addLast("ssl", new SslHandler(eg)) }
        pl.addLast("decoder", new HttpServerCodec())
//        pipe.addLast("aggregator", new HttpChunkAggregator(65536));
//        pipe.addLast("deflater", new HttpContentCompressor())
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", new BasicChannelHandler(g){
          
          override def doReqFinal(ctx:HTTPMsgInfo,inData:XData)  {
            if (_cb != null) { _cb.onOK(ctx, inData) }
          }
          
          override def onRecvRequest(ctx:HTTPMsgInfo ) = {
            if (_cb == null) true else  _cb.validateRequest(ctx)             
          }
          
        } )
        pl
      }
    }
  }

}

