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
package kernel

import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.{Properties=>JPS,Map=>JMap}
import java.net.URL
import java.util.{Map=>JMap,HashMap=>JHMap}
import com.zotoh.blason.loaders.RootClassLoader
import com.zotoh.blason.loaders.AppClassLoader
import com.zotoh.blason.impl.PropsConfiguration
import com.zotoh.blason.core.Startable
import com.zotoh.blason.core.Initializable
import com.zotoh.blason.core.Disposable
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.blason.core.Contextualizable
import com.zotoh.blason.core.Context
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import com.fasterxml.jackson.databind.ObjectMapper
import com.zotoh.blason.core.Constants
import com.fasterxml.jackson.core.JsonFactory
import com.zotoh.blason.impl.DefaultConfiguration
import java.io.FileInputStream
import com.zotoh.blason.core.Composable
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.blason.impl.DefaultKernel
import com.zotoh.blason.impl.AbstractConfiguration
import com.zotoh.blason.impl.DefaultServiceRegistry
import com.zotoh.blason.core.BlockMeta
import com.zotoh.blason.core.ServiceError
import com.zotoh.blason.core.Loggable
import org.slf4j.LoggerFactory
import com.zotoh.blason.core.Service
import com.zotoh.blason.impl.AbstractRegistry
import com.zotoh.blason.core.PODMeta
import com.zotoh.blason.impl.EmptyConfiguration
import java.io.File
import com.zotoh.blason.core.Component
import java.lang.reflect.Method
import com.zotoh.jmx.support.JMXResource
import com.zotoh.jmx.support.JMXMethod
import com.zotoh.jmx.support.JMXProperty
import com.zotoh.frwk.util.Coroutine
import com.zotoh.frwk.util.AsyncProc
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.blason.mvc.RouteInfo
import com.zotoh.blason.mvc.WebPage
import freemarker.template.{Configuration=>FTLCfg}
import freemarker.template.DefaultObjectWrapper
import freemarker.template.Template
import java.io.Writer
import java.io.OutputStreamWriter
import java.io.StringWriter
import com.zotoh.frwk.io.XData



/**
 * @author kenl
 */
object Container extends CoreImplicits with Constants {

  private val _log= LoggerFactory.getLogger(classOf[Container])

  def create( appReg:AbstractRegistry, pod:PODMeta ) = {

    val c=new Container(pod)
    c.configure( mkCfg(pod) )

    new AsyncProc().withClassLoader(pod.getCZldr).setDaemon(true).fork { () =>
      if (c.isEnabled) {
        c.tlog.info("Created app: {}" , pod.name )
        c.initialize
        c.compose(appReg)
        c.start
      }
    }

    c
  }

  private def mkCfg(pod:PODMeta) = {
    new PropsConfiguration( new JPS().add(K_APPDIR, new File( pod.getSpaceUrl.toURI ) ))
  }

}

/**
 * @author kenl
 */
@JMXResource(desc="Container for user application",beanName="container",
    domain="com.zotoh",
    paths=Array("product=blason","component=containers")
      )
class Container( private val _meta:PODMeta ) extends Initializable with Configurable with Startable with Disposable with Loggable with Component with Constants {

  private val _svcReg= new DefaultServiceRegistry()
  private var _active=false

  private var _routes:Array[RouteInfo] = null
  private var _manifest:Configuration= null
  private var _appConf:Configuration= null
  private var _envConf:Configuration= null

  private var _schr:Scheduler = null
  private var _jc:JobCreator=null
  private var _appDir:File=null

  private var _ftlCfg:FTLCfg=null

  private var _mainCZ:Class[_] = null
  private var _mainObj:Any = null

  def version() = _meta.version
  def tlog() = Container._log

  @JMXProperty(desc="Name of the application.")
  def getName() = name()

  def getRoutes() = {
    _routes
  }

  override def restartable() = true
  def name() = _meta.name
  def appDir() = _appDir

  private def maybeCfg() {
    var mtd:Method = null

    block { () =>
      mtd=_mainCZ.getDeclaredMethod("configure", classOf[Configuration])
    }
    if (mtd != null) { mtd.invoke(_mainObj, _appConf) }

    mtd=null
    block { () =>
      mtd=_mainCZ.getDeclaredMethod("initialize")
    }
    if (mtd != null) { mtd.invoke(_mainObj) }

  }

  def compose(r:ComponentRegistry, arg:Any* ) = {
    val mCZ = _manifest.getString("Main-Class","")
    tstEStrArg("Main-Class", mCZ)

    _jc = new JobCreator()
    _jc.compose(r, this)

    _svcReg.setParent( r.getParent.getOrElse(null) )
    _mainCZ= loadClass(mCZ)
    try {
      _mainObj = _mainCZ.getDeclaredConstructor(classOf[Container]).newInstance(this)
    } catch {
      case e:Throwable => tlog.warn("Main.Class: No ctor(Container) found.",e)
    }
    if (_mainObj == null) try {
      _mainObj = mkRef(_mainCZ)
    } catch {
      case e:Throwable => tlog.warn("Main.Class: No ctor() found.",e)
    }

    if (_mainObj == null) {
      throw new InstantiationException("Failed to create instance: " + _mainCZ.getName )
    }

    maybeCfg()

    _envConf.getChild("services") match {
      case Some(c:AbstractConfiguration) => reifySysServices(c)
      case _ => tlog.warn("No system service \"depend\" found in conf.")
    }

    try { Some(_mainObj) } finally {
      tlog.info("Composed app: {}" , name )
    }
  }

  def configure(cfg:Configuration) {
    val appDir = cfg.getString(K_APPDIR,"")
    tstEStrArg(K_APPDIR, appDir)
    val cfgDir=new File( appDir, DN_CONF)

    _manifest = new PropsConfiguration( asQuirks(new File(appDir, MN_FILE)) )
    _envConf=parseConf(new File(cfgDir, "env.conf").toURI.toURL)
    _appConf=parseConf(new File(cfgDir, "app.conf").toURI.toURL, _envConf)
    _appDir=new File(appDir)

    WebPage.setup(new File(appDir))
    maybeLoadRoutes(cfgDir)

    _ftlCfg = new FTLCfg()
    _ftlCfg.setDirectoryForTemplateLoading( new File(_appDir, DN_PAGES+"/"+DN_TEMPLATES))
    _ftlCfg.setObjectWrapper(new DefaultObjectWrapper())

    tlog.info("Configured app: {}" , name )
  }

  def processTemplate(ri:RouteInfo, model:JMap[_,_]): (XData, String)  = {
    processTemplate(ri.template, model)
  }

  def processTemplate(tpl:String, model:JMap[_,_]): (XData, String)  = {
    val s= if ( STU.isEmpty( tpl) ) "" else {
      resolveTemplate( tpl, model).toString()
    }
    ( new XData(s), "text/html" )
  }

  def resolveTemplate(tpl:String, model:JMap[_,_]): Writer = {
    tlog.debug("Resolve template: {}", tpl)
    val out = new StringWriter()
    val t= getTemplate( tpl)
    t.process( model, out)
    out.flush()
    out
  }

  def getTemplate(tpl:String) = {
    val s =join( List( if (tpl.startsWith("/")) { "" } else  { "/" } , tpl ,
        if ( tpl.endsWith(".ftl")) { "" } else { ".ftl" } ) , "" )
    _ftlCfg.getTemplate( s)
  }

  def getAppKey() = {
    _manifest.getString("Implementation-Vendor-Id", "")
  }

  def getAppMeta(): Configuration = {
    _appConf.getChild("meta") match {
      case Some(m) => m
      case _ => new DefaultConfiguration(null, null)
    }
  }

  def isEnabled() = {
    _envConf.getChild("container") match {
      case Some(c) => c.getBool("enabled", true)
      case _ => false
    }
  }

  def getAppMain: AnyRef = {
    _mainObj match {
      case r:AnyRef => r
      case _ => throw new ClassCastException("Expecting AnyRef object.")
    }
  }

  def getAppCfg(): Configuration = _appConf

  def scheduler() = _schr

  def initialize() {

    val m=_envConf.getChild("container") match {
      case Some(c:Configuration) => c
      case _ => new EmptyConfiguration()
    }

    _schr = new Scheduler()
    _schr.configure(m)  // configure threads mainly
    _schr.start

    tlog.info("Initialized app: {}" , name )
  }

  @JMXMethod(desc="Start Container")
  def start() {
    startSysServices()

    var mtd:Method= null
    block { () =>
      mtd=_mainCZ.getDeclaredMethod("start")
    }
    if (mtd != null) { mtd.invoke(_mainObj) }
    _active=true

    tlog.info("Started app: {}" , name )
  }

  @JMXMethod(desc="Stop Container")
  def stop() {
    stopSysServices()

    var mtd:Method= null
    block { () =>
      mtd=_mainCZ.getDeclaredMethod("stop")
    }
    if (mtd != null) { mtd.invoke(_mainObj) }

    _schr.stop()
    _active=false

    tlog.info("Stopped app: {}" , name )
  }

  def dispose() {

    if (_active) { stop() }

    disposeSysServices()

    var mtd:Method= null
    block { () =>
      mtd=_mainCZ.getDeclaredMethod("dispose")
    }
    if (mtd != null) { mtd.invoke(_mainObj) }

    _schr.dispose()
    tlog.info("Disposed app: {}" , name )
  }

  def getService(svcId:String): Option[Service] = {
    _svcReg.lookup(svcId) match {
      case Some(x) =>
        x match {
          case s:Service => Some(s)
          case _ => None
        }
      case _ => None
    }
  }

  private def reifySysServices(cfg:AbstractConfiguration) {
    cfg.getKeys().foreach { (k) =>
      cfg.getChild(k) match {
        case Some(c:AbstractConfiguration) => reifyOneSysService(k,c)
        case _ => /* no op */
      }
    }
  }

  private def reifyOneSysService(key:String, c:AbstractConfiguration) {
    val svc = c.getString("service","")
    val b= c.getBool("enabled", true)
    if ( ! b) {
        tlog.info("System service \"{}\" is disabled.", svc)
    } else {
      val rc = if ( ! STU.isEmpty(svc)) {
          tlog.info("Reify required system service \"{}\"", svc)
          reifyService(svc,c)
      } else {
        ("", null)
      }
      if (rc._2 != null) {
        _svcReg.add(key, rc._2 )
      } else {
        tlog.warn("Block \"{}\" is not a Service.", rc._1 )
      }
    }
  }

  private def reifyService(svc:String, cfg:Configuration) = {
    val sys = _svcReg.getParent.getOrElse(null)
    tstObjArg("sys services",sys)
    val bt = sys.lookup(svc) match {
      case Some(x:BlockMeta) => x
      case _ => throw new ServiceError("No such Service: " + svc + ".")
    }
    val obj = bt.compose(sys, this, _jc)
    obj match {
      case Some(x:Configurable) => x.configure(cfg)
      case _ => /* no op*/
    }
    obj match {
      case Some(x:Initializable) => x.initialize
      case _ => /* no op*/
    }
    val rc=obj match {
      case Some(x:Service) => x
      case _ =>  null
    }

    (svc,rc)
  }

  private def startSysServices() {
    _svcReg.getComponents.foreach { _ match {
        case x:Startable => x.start
        case _ =>
      }
    }
  }

  private def stopSysServices() {
    _svcReg.getComponents.foreach { _ match {
        case x:Startable => x.stop
        case _ =>
      }
    }
  }

  private def disposeSysServices() {
    _svcReg.getComponents.foreach { _ match {
        case x:Disposable => x.dispose
        case _ =>
      }
    }
  }

  private def maybeLoadRoutes(cfgDir:File) {
    import RouteInfo._
    val rc = loadRoutes(new File(cfgDir, "static-routes.conf")) ++ loadRoutes(new File(cfgDir, "routes.conf"))
    _routes= rc.toArray
  }

  private def parseConf(url:URL, par:Configuration = null ) = {
    val j = using( url.openStream ) { (inp) =>
      new ObjectMapper().readValue(
          new JsonFactory().createParser( inp),classOf[JMap[_,_]])
    }
    new DefaultConfiguration(j, par)
  }

}


