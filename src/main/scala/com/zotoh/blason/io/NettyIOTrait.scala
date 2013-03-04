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

import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.frwk.util.WWID
import com.zotoh.frwk.util.CoreUtils._
import java.io.IOException
import java.net.InetSocketAddress
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException
import java.util.concurrent.Executors
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.ServerSocketChannelFactory
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.handler.ssl.SslHandler
import com.zotoh.blason.util.Observer


/**
 * @author kenl
 */
abstract class NettyIOTrait protected(evtHdlr:Observer, nm:String, ssl:Boolean=false)
extends BaseHttpIO(evtHdlr, nm,ssl) {

  private var _fac:ServerSocketChannelFactory=null
  private var _chGrp:ChannelGroup=null

  override def isAsync() = true

  def onStop() {
    asyncExec(new Runnable(){
      def run() { block { () =>
        _chGrp.close().awaitUninterruptibly()
        _fac.releaseExternalResources()
      }}
    })
  }

  def pushOneChannel(c:Channel) {
    if (_chGrp != null && c != null) { _chGrp.add(c) }
  }

  def popOneChannel(c:Channel) {
    if (_chGrp != null && c != null) { _chGrp.remove(c) }
  }

  protected def maybeCfgSSL(pl:ChannelPipeline) {
    keyURL match {
      case Some(u) if (isSSL ) =>
        val t= HTTPIOTrait.cfgSSL(true, sslType, u, keyPwd )
        val engine = t._2.createSSLEngine()
        engine.setUseClientMode(false)
        pl.addLast("ssl", new SslHandler(engine) )
      case _ => /* noop */
    }
  }

  protected def onStart_0() = {
    _chGrp= new DefaultChannelGroup( uid() )
    _fac= new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
    new ServerBootstrap(_fac)
  }

  protected def onStart_1(boot:ServerBootstrap) {
    boot.setOption("reuseAddress", true)
    // Options for its children
    boot.setOption("child.receiveBufferSize", 1024*1024) // 1MEG
    boot.setOption("child.tcpNoDelay", true)
    val c=boot.bind(new InetSocketAddress( ipAddr, port ))
//    c.getConfig().setConnectTimeoutMillis(millis)
    _chGrp.add(c)
  }

}
