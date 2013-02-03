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

import com.zotoh.frwk.net.NettyHplr.newServerSSLContext
import com.zotoh.frwk.net.NettyHplr.newServerBoot
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._

import java.util.{Properties=>JPS}
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL

import javax.net.ssl.SSLEngine
import org.slf4j._

import org.jboss.netty.channel.group.ChannelGroupFutureListener
import org.jboss.netty.channel.group.ChannelGroupFuture
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.group.ChannelGroup



object MemXXXServer extends CoreImplicits {

  private val _log= LoggerFactory.getLogger(classOf[MemXXXServer])

  protected[net] def xxx_main(block:Boolean, cz:String, args:Array[String]) {
    try {
      val m=parseArgs(args)
      val z=loadClass(cz)
      val host=nsb(m.gets("host"))
      val key=nsb(m.gets("key"))
      val pwd=nsb(m.gets("pwd"))
      val vdir=nsb(m.gets("vdir"))
      val port= nsb(m.gets("port")).toInt
      val svr = if (! STU.isEmpty(key)) {
        z.getConstructor(classOf[String],classOf[URL], classOf[String], classOf[String], classOf[Int]).
        newInstance(vdir,new URI(key).toURL(), pwd, host, asJObj(port))
      } else {
        z.getConstructor(classOf[String],classOf[String], classOf[Int]).
        newInstance(vdir,host, asJObj(port))
      }
      svr match {
        case x:MemXXXServer => x.start(block)
        case _ =>
      }
    } catch {
      case t:Throwable => t.printStackTrace()
    }
  }

  private def parseArgs(args:Array[String]) = {
    val m= new JPS()
    var i=0
    while (i < args.length) {
      args(i) match {
        case "-host" => m.put("host", args(i+1)); i += 1
        case "-port" => m.put("port", args(i+1)); i += 1
        case "-key" => m.put("key", args(i+1)); i += 1
        case "-pwd" => m.put("pwd", args(i+1)); i += 1
        case "-vdir" => m.put("vdir", args(i+1)); i += 1
        case _ =>
      }
      i += 1
    }
    m
  }

}

abstract class MemXXXServer extends Constants {

  import MemXXXServer._
  def tlog() = _log

  protected var _lock:Object = null
  protected var _boot:ServerBootstrap = null
  protected var _vdir:File = null
  protected var _host=""
  protected var _keyPwd=""
  protected var _chs:ChannelGroup = null
  protected var _root:Channel = null
  protected var _port= -1
  protected var _keyFile:URL = null

  /**
   * @param vdir
   * @param host
   * @param port
   */
  protected def this(vdir:String, host:String, port:Int) {
    this()
    _vdir=new File(vdir)
    _vdir.mkdirs()
    _host=host
    _port=port
  }

  /**
   * @param vdir
   * @param key
   * @param pwd
   * @param host
   * @param port
   */
  protected def this(vdir:String, key:URL, pwd:String, host:String, port:Int) {
    this(vdir,host,port)
    setKeyAuth(key,pwd)
  }

  protected def setKeyAuth(key:URL,pwd:String): this.type = {
    _keyPwd= pwd
    _keyFile=key
    this
  }

    /**
   * @return
   */
  def channels() = _chs

  def start( block:Boolean) {

    tlog.debug("MemXXXServer: starting...")
    start_0()
    start_1()
    start_2()

    if (block) {
      _lock=new Object()
      freeze(_lock)
    }
  }

    /**
   *
   */
  def stop() {

    val me=this

    _chs.close().addListener(new ChannelGroupFutureListener(){
      def operationComplete(f:ChannelGroupFuture ) {
        me.stop_final()
      }
    })

  }

  private def stop_final() {

    tlog.debug("MemXXXServer: stopped")

    _boot.releaseExternalResources()
    thaw(_lock)
    reset()
  }

  private def reset() {
    _lock= null
    _boot=null
    _host=null
    _keyPwd=null
    _chs=null
    _root=null
    _port= -1
    _keyFile=null
  }

  private def start_2() {
    tlog.debug("{}: running on host {}, port {}", "MemFileServer", _host, asJObj(_port) )
    _root= _boot.bind(new InetSocketAddress( _host, _port))
    _chs.add(_root)
  }

  private def start_1() {
    _boot.setOption("child.receiveBufferSize", 2*1024*1024)
    _boot.setOption("child.tcpNoDelay", true)
    _boot.setOption("reuseAddress", true)
  }

  private def start_0() {

    val eg:SSLEngine= if (_keyFile == null) null else newServerSSLContext(_keyFile,_keyPwd)
    val t= newServerBoot()
    _boot= t._1
    _chs= t._2
    _boot.setPipelineFactory( pipelineFac(eg) )
  }


  /**
   * @param eg
   * @return
   */
  protected def pipelineFac(eg:SSLEngine): ChannelPipelineFactory

}
