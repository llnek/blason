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

import org.jboss.netty.channel.Channels.pipeline
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import com.zotoh.blason.util.Observer
import com.zotoh.blason.core.Configuration

/**
 * @author kenl
 */
class NettyIO(evtHdlr:Observer, nm:String,ssl:Boolean = false)
extends NettyIOTrait(evtHdlr, nm,ssl) {

  private var _contextPath=""

  def this() { this (null,"") }

  def getContextPath() = _contextPath

  def onStart() {
    onStart_1( cfgPipeline(  onStart_0 ) )
  }

  protected def getHandler(): SimpleChannelHandler = new NettyREQHdlr(this)

  def mkEvent() = new HTTPEvent(this)

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    _contextPath = cfg.getString("context","")
  }

  protected[io] def cfgPipeline(boot:ServerBootstrap) = {
    boot.setPipelineFactory(new ChannelPipelineFactory() {
      override def getPipeline() = {
        var pl = org.jboss.netty.channel.Channels.pipeline()
        maybeCfgSSL( pl)
        pl.addLast("decoder", new HttpRequestDecoder())
//        pipeline.addLast("aggregator", new HttpChunkAggregator(65536))
        pl.addLast("encoder", new HttpResponseEncoder())
        //pipeline.addLast("deflater", new HttpContentCompressor())
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", getHandler )
        pl
      }
    })
    boot
  }

}

