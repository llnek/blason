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

import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

import org.jboss.netty.bootstrap.Bootstrap
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpContentCompressor
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedWriteHandler

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.security.Crypto
import com.zotoh.frwk.security.CryptoStore
import com.zotoh.frwk.security.JKSStore
import com.zotoh.frwk.security.PKCSStore


/**
 * @author kenl
 *
 */
object NettyHplr {

  /**
   * @param boot
   * @param fac
   * @return
   */
  def inizServerPipeline(boot:Bootstrap, fac:ChannelPipelineFactory) = {
    boot.setPipelineFactory(fac)
    boot
  }

  /**
   * @param eng
   * @param chunk
   * @param zip
   * @param boot
   * @return
   * @throws Exception
   */
  def inizServerPipeline(eng:SSLEngine,
      chunk:Boolean, zip:Boolean, boot:Bootstrap): ChannelPipeline  = {

    boot.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl = org.jboss.netty.channel.Channels.pipeline()
        if (eng != null) {  pl.addLast("ssl", new SslHandler(eng)) }
        pl.addLast("decoder", new HttpRequestDecoder())
        if(chunk) { pl.addLast("aggregator", new HttpChunkAggregator(65536)) }
        pl.addLast("encoder", new HttpResponseEncoder())
        if (zip) { pl.addLast("deflater", new HttpContentCompressor()) }
        pl.addLast("chunker", new ChunkedWriteHandler())
//        pipeline.addLast("handler", null)
        pl
      }
    })

    boot.getPipelineFactory().getPipeline()
  }

  /**
   * @return
   * @throws Exception
   */
  def newServerBoot() = {
    var c= new DefaultChannelGroup(uid)
    var b= new ServerBootstrap( new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()) )
    (b,c)
  }

  def  newServerSSLContext(key:URL, pwd:String) = {

    var s:CryptoStore = null
    using(key.openStream) { (inp) =>
      s= if (key.getFile.endsWith(".jks")) new JKSStore() else new PKCSStore()
      s.init(pwd )
      s.addKeyEntity(inp, pwd )
    }

    val c = SSLContext.getInstance( "TLS")
    c.init( s.keyManagerFactory().getKeyManagers,
        s.trustManagerFactory().getTrustManagers,
        Crypto.secureRandom )

    val engine = c.createSSLEngine()
    engine.setUseClientMode(false)
    engine
  }


}

