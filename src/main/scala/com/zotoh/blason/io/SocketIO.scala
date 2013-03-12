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
package io

import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{IOUtils=>IOU}
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Properties
import java.util.ResourceBundle
import com.zotoh.blason.core.Configuration
import com.zotoh.frwk.net.NetUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.blason.util.Observer



/**
 * @author kenl
 */
class SocketIO(evtHdlr:Observer, nm:String) extends EventEmitter(evtHdlr,nm)  {
  private var _ssoc:ServerSocket = null
  private var _socTOutMillis= 0L
  private var _port= -1
  private var _backlog= 100
  private var _host=""

  def host() = _host
  def port() = _port

  def this() {
    this (null,"")
  }
  
  override def configure(cfg:Configuration) {
    super.configure(cfg)
    val tout= cfg.getLong("sock-timeout-millis",0L)
    val port= cfg.getLong("port",-1L)
    val blog= cfg.getLong("backlog",100L)

    tstNonNegLongArg("socket-port", port)
    _port= port.toInt
    tstNonNegLongArg("tcp-backlog", blog)
    _backlog= blog.toInt
    tstNonNegLongArg("socket-timeout-millis", tout)
    _socTOutMillis= tout
    _host= cfg.getString("host","")
  }

  override def onInit() {
    _ssoc= createSvrSockIt()
  }

  def onStart() {
    asyncExec( new Runnable() {
      def run() {
        while ( _ssoc != null) try {
          sockItDown( _ssoc.accept )
        } catch {
          case e:Throwable => closeSoc()
        }
      }
    })
  }

  def onStop() {
    closeSoc()
  }

  private def closeSoc() {
    IOU.closeQuietly(_ssoc)
    _ssoc=null
  }

  private def sockItDown(s:Socket) {
    dispatch( new SocketEvent(this, s) )
  }

  private def createSvrSockIt() = {
    val ip= if ( STU.isEmpty(_host)) InetAddress.getLocalHost() else InetAddress.getByName(_host)
    var soc= new ServerSocket(_port, _backlog, ip)
    var s:ServerSocket =null
    try {
      soc.setReuseAddress(true)
      s=soc
      soc=null
    } finally {
      IOU.closeQuietly(soc)
    }

    tlog().debug("Opened Server Socket: {} on host: {}" , _port , _host)
    s
  }


}
