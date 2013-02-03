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
import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils._
import java.io.File
import java.util.{Properties=>JPS,ResourceBundle,EnumSet}
import javax.net.ssl.SSLContext
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.NCSARequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.servlet.DefaultServlet
import javax.servlet.DispatcherType
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.util.Observer
import com.zotoh.blason.core.Constants
import org.eclipse.jetty.server.handler.ResourceHandler



/**
 * @author kenl
 */
class HTTPServletIO(evtHdlr:Observer, nm:String, ssl:Boolean=false)
extends BaseHttpIO(evtHdlr, nm, ssl) with Weblet with Constants {
  import WEBServlet._
  private var _jetty:Server=null
  private var _contextPath=""

  def this() {
    this (null,"")
  }
  
  override def onInit() {
    val svr= new Server()
    val cc = if (isSSL ) {
      val c= new SslSelectChannelConnector()
      val t= keyURL  match {
        case Some(u) => HTTPIOTrait.cfgSSL(true,sslType, u, keyPwd )
        case _ => (null,null)
      }
      val fac=c.getSslContextFactory()
      fac.setSslContext( t._2)
      fac.setWantClientAuth(false)
      fac.setNeedClientAuth(false)
      c
    } else {
      new SelectChannelConnector()
    }
    cc.setName(this.name )
    if (! STU.isEmpty(host )) { cc.setHost(host ) }
    cc.setPort(port )
    cc.setThreadPool(new QueuedThreadPool( workers  ))
    cc.setMaxIdleTime(30000)     // from jetty examples
    cc.setRequestHeaderSize(8192)  // from jetty examples
    svr.setConnectors(Array(cc))
    _jetty=svr
  }

  def setContextPath(path:String) = {
    _contextPath=nsb(path)
  }

  def contextPath() = _contextPath

  override def configure(cfg:Configuration) = {
    super.configure(cfg)
    _contextPath= cfg.getString("context", "")
    tstEStrArg("web context path", _contextPath)
  }

  def onStart() {
      onStart_War(_jetty)
    _jetty.start()
  }

  def onStop() {
    if (_jetty != null) block { () => _jetty.stop() }
    _jetty=null
  }

  private def onStart_War(svr:Server) {
    val app= container.appDir
    val me=this
    val webapp = new WebAppContext()  {
      override def setContextPath(s:String) {
        super.setContextPath(s)        
        _scontext.setAttribute(WEBSERVLET_DEVID, me )
      }
    }
    webapp.setDescriptor(  new File(app, WEB_XML).toURI().toURL().toString )
    val logDir= new File(app, WEB_LOG).toURI().toURL().toString
    val resBase= app.toURI().toURL().toString
    // static resources are based from this, regardless of context
    webapp.setParentLoaderPriority(true)
    webapp.setResourceBase( resBase )
    webapp.setContextPath(_contextPath) 
    val  rr=webapp.getWebInf()
    svr.setHandler(webapp)    
  }
  
  private def maybeREQLog(logDir:String):Handler = {
    if ( STU.isEmpty(logDir)) null else {
      val h= new RequestLogHandler()
      val dir=new File(logDir)
      dir.mkdirs()
      val path= niceFPath(dir) + "/jetty-yyyy_mm_dd.log"
      tlog().debug("Jetty: request-log output path {} ", path)
      val requestLog = new NCSARequestLog(path)
      requestLog.setRetainDays(90)
      requestLog.setAppend(true)
      requestLog.setExtended(false)
      requestLog.setLogTimeZone("GMT")

      h.setRequestLog(requestLog)
      h
    }
  }

}



