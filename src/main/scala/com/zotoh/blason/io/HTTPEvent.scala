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

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.{StrArr}
import java.net.HttpCookie
import org.jboss.netty.handler.codec.http.Cookie
import com.zotoh.frwk.net.ULFileItem


@SerialVersionUID(4177245480803037339L)
class HTTPEvent(src:EventEmitter) extends AbstractEvent(src) with CoreImplicits {

  private val _params= mutable.HashMap[String,StrArr]()
  private val _hdrs= mutable.HashMap[String,StrArr]()
  private val _attrs= mutable.HashMap[String,Any]()
  private val _atts= mutable.HashMap[String,ULFileItem]()

  private var _servletPath=""
  private var _url=""
  private var _uri= ""
  private var _queryString=""
  private var _method= ""

  private var _contextPath=""
  private var _ctype=""
  private var _remoteAddr=""
  private var _remoteHost=""
  private var _localAddr=""
  private var _localHost=""
  private var _protocol=""
  private var _server=""
  private var _domain=""
  private var _scheme=""
  private var _encoding="utf-8"

  private var _remotePort=0
  private var _localPort=0
  private var _serverPort=0

  private var _data:XData=null
  private var _cLen=0L
  private var _cTypeLine=""

  private var _cookies:Map[String,Cookie] = null
  private var _keepAlive= false

  override def setResult(r:AbstractResult) {
    super.setResult(r)
  }

  def setCookies(c:Map[String,Cookie]) {
    _cookies = c
  }

  def getCookies(): Map[String,Cookie] = {
    if (_cookies == null) Map() else _cookies
  }

  def getCookie(name:String): Option[Cookie] = {
    if (_cookies==null || STU.isEmpty(name)) None else _cookies.get(name)
  }

  def setData(s:String): this.type = {
    _data=new XData(s)
    this
  }

  def setData(b:Array[Byte]): this.type = {
    _data=new XData(b)
    this
  }

  def isKeepAlive() = _keepAlive
  def setKeepAlive(b:Boolean) { _keepAlive=b }

  def setData(s:XData): this.type =  { _data=s; this }
  def data() = _data

  def hasData() = _data != null

  def setContentLength(len:Long) { _cLen = len  }
  def contentLength() = _cLen

  def setContentTypeLine(s:String) {
    _cTypeLine= nsb(s)
  }

  def contentTypeLine() = _cTypeLine

  def setContentType(ct:String) {
    _ctype = nsb(ct)
  }
  def contentType() = _ctype

  def setEncoding(enc:String) {
    _encoding = nsb(enc)
  }
  def encoding() = _encoding

  def setContextPath(c:String) {
    _contextPath=nsb(c)
  }
  def contextPath() = _contextPath

  def addFile(fi:ULFileItem) {
    _atts.put(fi.getFieldName, fi)
  }

  def addAttr(n:String, v:Any) {
    if ( ! STU.isEmpty(n)) {
      _attrs += Tuple2(n,v)
    }
  }

  def addHeader(hdr:String, v:String) {
    val h=hdr.lc
    if ( ! _hdrs.isDefinedAt(hdr) ) {
      _hdrs.put(h, new StrArr() )
    }
    _hdrs.get(h).get.add(v)
  }

  def headers() = _hdrs.toMap

  def header(nm:String): Option[StrArr] = {
    if ( nm==null) None else _hdrs.get(nm.lc )
  }

  def setDomain(d:String) { _domain = d }
  def domain() = _domain

  def setLocalAddr(addr:String) { _localAddr= nsb(addr) }
  def localAddr() = _localAddr

  def setLocalHost(host:String) { _localHost= nsb(host) }
  def localHost()= _localHost

  def setLocalPort(port:Int) { _localPort= port }
  def localPort() = _localPort

  def setMethod(m:String) { _method= nsb(m) }
  def method() = _method

  def params() = _params.toMap
  def param(p:String) = {
    if ( STU.isEmpty(p)) None else _params.find { (t) =>
      t._1.eqic(p)
    } match {
      case Some(x) => Some(x._2)
      case _ => None
    }
  }

  def addParam(p:String, vs:Seq[String]) {
    if ( ! STU.isEmpty(p) && vs != null) vs.foreach { (s) =>
      addParam(p, s)
    }
  }

  def addParam(p:String, v:String) {
    val r= param(p) match {
      case Some(s) => s
      case _ =>
        val s= new StrArr()
        _params += Tuple2(p, s)
        s
    }
    r.add(nsb(v) )
  }

  def attrs() = _attrs.toMap
  def attr(p:String) = {
    if ( STU.isEmpty(p)) None else _attrs.find { (t) =>
      t._1.eqic(p)
    } match {
      case Some(x) => Some(x._2)
      case _ => None
    }
  }

  def setProtocol(p:String) { _protocol= nsb(p) }
  def protocol() = _protocol

  def setQueryString(q:String) { _queryString= nsb(q) }
  def queryString() =  _queryString

  def setRemoteAddr(addr:String) {  _remoteAddr= nsb(addr) }
  def remoteAddr() =  _remoteAddr

  def setRemoteHost(host:String) { _remoteHost= nsb(host) }
  def remoteHost() = _remoteHost

  def setRemotePort(port:Int) {   _remotePort= port }
  def remotePort() = _remotePort

  def setScheme(scheme:String) { _scheme= nsb(scheme) }
  def scheme() = _scheme

  def setServer(server:String) { _server= nsb(server) }
  def server() = _server

  def setServerPort(port:Int) { _serverPort= port }
  def serverPort() = _serverPort

  def setServletPath(path:String) { _servletPath= path }
  def servletPath() = _servletPath

  def isSSL() = {
    nsb(scheme()).startsWith("https")
  }

  def setUri(uri:String) { _uri= nsb(uri) }
  def uri() = _uri

  def setUrl(url:String) { _url= nsb(url) }
  def url() = _url

  override def toString() = {
    var bf= "" +
    "servlet-path=" + _servletPath + ", " +
    "url=" + _url + ", " +
    "uri=" + _uri + ", " +
    "queryString=" + _queryString + ", " +
    "method=" + _method + ", " +
    "ctype=" + _ctype + ", " +
    "clen=" + _cLen + ", " + "\n" +
    "domain=" + _domain + ", " +
    "remote-addr=" + _remoteAddr + ", " +
    "remote-host=" + _remoteHost + ", " +
    "remote-port=" + _remotePort + ", " +
    "local-addr=" + _localAddr + ", " +
    "local-host=" + _localHost + ", " +
    "local-port=" + _localPort + ", " +
    "protocol=" + _protocol + ", " +
    "server=" + _server + ", " +
    "serverPort=" + _serverPort + ", " +
    "scheme=" + _scheme + ", " +
    "ssl=" + isSSL() + "\n" +
    "data=" +
    (if(_data==null) "null" else if (_data.isDiskFile()) _data.filePath() else "byte[]")  +
     "\n"

    var ts="\n"
    _params.foreach{ (t) => ts += t._1 + "=[" + t._2.toString() + "]" }
    bf += "params=>>>" + ts + "\n"
    ts="\n"
    _hdrs.foreach{ (t) => ts += t._1 + ": " + t._2.toString() + "\n" }
    bf += "headers=>>>" + ts + "\n"
    bf
  }

  
}


