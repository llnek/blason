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
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.net.NetUtils._
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.security._
import java.io.{File,IOException,InputStream}
import java.net.{InetAddress,URL,UnknownHostException}
import java.security._
import java.util.{Properties=>JPS,ResourceBundle}
import javax.net.ssl.SSLContext
import com.zotoh.frwk.security.PKCSStore
import com.zotoh.frwk.security.PwdFactory
import com.zotoh.frwk.security.Crypto
import com.zotoh.frwk.security.JKSStore
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.util.Observer

/**
 * @author kenl
 */
object HTTPIOTrait {

  def cfgSSL(createContext:Boolean, sslType:String, key:URL, pwd:String) = {

    val s= if ( key.getFile().endsWith(".jks") ) new JKSStore() else new PKCSStore()

    using(key.openStream()) { (inp) =>
      s.init(pwd )
      s.addKeyEntity(inp, pwd )
    }

    val c= if (createContext) SSLContext.getInstance( sslType ) else null
    if (c!=null) {
      c.init( s.keyManagerFactory().getKeyManagers(),
          s.trustManagerFactory().getTrustManagers(),
          Crypto.secureRandom() )
    }

    (s, c)
  }
}


/**
 * @author kenl
 */
abstract class HTTPIOTrait protected(evtHdlr:Observer, nm:String, private var _secure:Boolean = false) extends EventEmitter(evtHdlr,nm) {

  private var _keyURL:URL = null
  private var _keyPwd=""
  private var _host=""
  private var _sslType=""
  private var _port=0

  def isSSL() = _secure
  def port() = _port
  def host() = _host

  def keyURL() =  if (_keyURL==null) None else Some(_keyURL)
  def sslType() = _sslType
  def keyPwd() = _keyPwd

  def ipAddr() = {
    if ( STU.isEmpty(_host)) InetAddress.getLocalHost() else InetAddress.getByName(_host)
  }

  override def configure(cfg:Configuration) = {
    super.configure(cfg)

    val key= cfg.getString("serverkey-file","")
    val pwd= cfg.getString("keypasswd","")
    var sslType= cfg.getString("flavor","")
    var host = cfg.getString("host","")
    val port= cfg.getLong("port",-1)

    tstNonNegLongArg("port", port)

    if (_secure && STU.isEmpty(key)) {
      tstEStrArg("ssl-key-file", key)
    }

    if ( STU.isEmpty(sslType)) {
      sslType= "TLS"
    }

    _sslType = sslType
    _port = port.toInt
    _host= host

    if ( ! STU.isEmpty(key)) {
      tstEStrArg("ssl-key-file-password", pwd)
      _keyURL = if (key.startsWith("file:")) new URL(key) else new File(key).toURI.toURL
      _keyPwd= PwdFactory.mk(pwd).text()
      _secure=true
    }

  }

}


