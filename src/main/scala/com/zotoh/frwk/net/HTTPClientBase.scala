/*??
 * COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import com.zotoh.frwk.util.CoreUtils._
import java.net.URI
import org.slf4j._
import com.zotoh.frwk.io.XData


/**
 * @author kenl
 */
object HTTPClientBase {
  private val _log= LoggerFactory.getLogger(classOf[HTTPClientBase])
}


/**
 * @author kenl
 */
abstract class HTTPClientBase {

  protected var _remote:URI = null
  protected var _ssl=false
  protected var _soctout=1000
  
  def tlog() = HTTPClientBase._log
  
  def connect(remote:URI): this.type = {
    setUri(remote)
    val host= remote.getHost
    var port= remote.getPort
    if (port < 0) { port = if(_ssl) 443 else 80 }
    tlog.debug("HTTPClientBase: host: {}, port: {}{}", host, asJObj(port),"")
    connect(host,port)
    tlog.debug("HTTPClient: connected OK to host: {}, port: {}{}", host, asJObj(port),"")
    this
  }

  def finz() {
    tlog.debug("HTTPClient: finz()")
    close()
  }

  def withSocTOutMillis(n:Int): this.type = {
    _soctout=n
    this
  }
  
  
  def close(): Unit
  
  protected def connect(host:String,port:Int):Unit
  protected def setUri(r:URI) {
    _ssl= "https" == r.getScheme()
    _remote=r
  }


}
