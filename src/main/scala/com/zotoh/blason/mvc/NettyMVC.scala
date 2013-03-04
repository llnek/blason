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
package mvc

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.zotoh.blason.io.NettyIO
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import com.zotoh.blason.core.Configuration

/**
 * @author kenl
 */
class NettyMVC extends NettyIO(null, "") {

  private val _wfiles = mutable.ArrayBuffer[String]()
  private var _useETags=false
  private var _maxAge=3600L

  def getWelcomeFiles(): Set[String] = _wfiles.toSet

  def getCacheMaxAgeSecs() = _maxAge

  def getUseETag() = _useETags

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    cfg.getSequence("welcomeFiles").foreach { _ match {
      case s:String =>_wfiles += s
      case _ =>
    }}
    _maxAge= cfg.getLong("cacheMaxAgeSecs", 3600L)
    _useETags= cfg.getBool("useEtags", false)
  }

  override def cfgPipeline(boot:ServerBootstrap) = {
    boot.setPipelineFactory(new ChannelPipelineFactory() {
      override def getPipeline() = {
        var pl = org.jboss.netty.channel.Channels.pipeline()
        maybeCfgSSL( pl)
        pl.addLast("decoder", new HttpRequestDecoder())
        pl.addLast("aggregator", new NettyChunkAdder())
        pl.addLast("encoder", new HttpResponseEncoder())
        //pipeline.addLast("deflater", new HttpContentCompressor())
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", getHandler )
        pl
      }
    })
    boot
  }

  override def getHandler() = new MVCHandler(this)

}




