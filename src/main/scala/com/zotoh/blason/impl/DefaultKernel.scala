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
package impl

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.io.{FileUtils=>FUT}
import org.apache.commons.io.filefilter
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import org.slf4j._
import com.zotoh.blason.core.Startable
import com.zotoh.blason.core.PODMeta
import com.zotoh.blason.core.Loggable
import com.zotoh.blason.core.Context
import com.zotoh.blason.core.Kernel
import com.zotoh.blason.core.Contextualizable
import com.zotoh.blason.core.Initializable
import com.zotoh.blason.core.ConfigError
import com.zotoh.blason.core.BlockMeta
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.core.Service
import java.io.File
import org.apache.commons.io.FilenameUtils
import com.zotoh.blason.core.ContextError
import org.apache.commons.io.filefilter.DirectoryFileFilter
import com.zotoh.blason.core.Constants
import java.io.FileFilter
import com.zotoh.blason.kernel.Container
import com.zotoh.blason.loaders.RootClassLoader
import com.zotoh.blason.core.Component
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.jmx.support.JMXServer
import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.frwk.mime.MimeUtils
import com.zotoh.blason.core.Execvisor
import com.zotoh.frwk.util.AsyncProc


object DefaultKernel {
  private var _log = LoggerFactory.getLogger(classOf[DefaultKernel])
}

/**
 * @author kenl
 */
class DefaultKernel extends Kernel  with Component
with Initializable with Contextualizable with Startable with Loggable with Constants {

  def tlog() = DefaultKernel._log

  private val _apps = mutable.HashMap[String, Container]()
//  private var _rootCZldr:RootClassLoader=null
  private var _appReg:AbstractRegistry= null
  private var _jmx:JMXServer= null
  private var _ctx:Context=null
  private var _baseDir:File=null
  private var _podsDir:File=null
  private var _playDir:File=null

  def getActiveItems() = _apps.size

  def name() = "kernel"
  def version() =""

  override def compose(r:ComponentRegistry, arg:Any*) = {
    if (arg.length > 0) arg(0) match {
      case j:JMXServer => _jmx=j
      case _ =>
    }
    r.lookup(PF_APPS) match {
      case Some(x:AbstractRegistry) => _appReg=x
      case _ => /* noop */
    }
    None
  }

  def contextualize(c:Context) {
    c.get(K_BASEDIR ) match {
      case Some(x:String) => _baseDir=new File(x)
      case Some(x:File) => _baseDir=x
      case _ => throw new ContextError(K_BASEDIR+" undefined")
    }
    c.get(K_PODSDIR ) match {
      case Some(x:String) => _podsDir=new File(x)
      case Some(x:File) => _podsDir=x
      case _ => throw new ContextError(K_PODSDIR+" undefined")
    }
    c.get(K_PLAYDIR ) match {
      case Some(x:String) => _playDir=new File(x)
      case Some(x:File) => _playDir=x
      case _ => throw new ContextError(K_PLAYDIR+" undefined")
    }

    MimeUtils.setupCache(new File(_baseDir, DN_CFG+"/app/mime.properties").toURI.toURL )
    _ctx=c
  }

  def initialize() {
    _playDir.listFiles(DirectoryFileFilter.DIRECTORY.asInstanceOf[FileFilter]).foreach { (f) =>
      maybePOD_OK(f)
    }
  }

  private def maybePOD_OK(pod:File) {
    try {
      inspect(pod)
    } catch {
      case e:Throwable => tlog.error("",e)
    }
  }

  private def inspect(des:File) = {
    val app = FilenameUtils.getBaseName( niceFPath(des))

    tstArg( isDirWR(new File(des,POD_INF)), "\"POD-INF\" sub-dir")
    tstArg( isDirWR(new File(des, POD_CLASSES)), "\"classes\" sub-dir")
    tstArg( isDirWR(new File(des, POD_LIB)), "\"lib\" sub-dir")

    tstArg( isDirWR(new File(des, META_INF)), "\"META-INF\" sub-dir")
//    tstArg( isFileWR(new File(des, MN_README)), "\"readme\" file")
    tstArg( isFileWR(new File(des, MN_NOTES)), "\"notes\" file")
    tstArg( isFileWR(new File(des, MN_LIC)), "\"license\" file")
    tstArg( isFileWR(new File(des, MN_RNOTES)), "\"release-notes\" file")

    tstArg( isFileWR(new File(des, CFG_APP_CF)), "\"app.conf\" file")
    tstArg( isFileWR(new File(des, CFG_ENV_CF)), "\"env.conf\" file")
    tstArg( isDirWR(new File(des, DN_CONF)), "\"conf\" sub-dir")

    chkManifest( app, des, new File(des, MN_FILE)) match {
      case p:PODMeta => _appReg.add(p.name, p)
      case _ => /* never */
    }
  }

  private def chkManifest( app:String, des:File, fp:File) = {

    tstArg( isFileWR(fp), "\"manifest\" file")

    val ps=asQuirks(fp)
    val ver=ps.gets("Implementation-Version")
    val cz=ps.gets("Main-Class")

    tstEStrArg("POD-MainClass",  cz)
    tstEStrArg("POD-Version",  ver)

    //ps.gets("Manifest-Version")
    ps.gets("Implementation-Title")
    ps.gets("Implementation-Vendor-URL")
    ps.gets("Implementation-Vendor")
    ps.gets("Implementation-Vendor-Id")

    val m= new PODMeta(app, ver, cz, des.toURI.toURL)
    m.contextualize(_ctx)
    m.initialize

    m
  }

  def start() {
    _appReg.getComponents().foreach { (c) =>
      c match {
        case m:PODMeta => maybeStartPOD(m)
        case _ => /* never */
      }
      // need this to prevent deadlocks amongst pods
      // when there are dependencies
      // TODO: need to handle this better
      safeWait(  Math.max(1,newRandom.nextInt(6) ) * 1000 )
    }
    //val me=this ; new AsyncProc().setDaemon(true).fork{ () => me.monitorApps() }
  }

  private def monitorApps() {
    while (true) {
      if (_apps.size == 0) {
        _ctx.get(PF_EXECV) match {
          case Some(x:Execvisor) =>
          tlog.warn("No active app is running, signal to shutdown.")
          x.kill9
          case _ =>
        }
      }
      safeWait(5000)
    }
  }

  def stop() {
    _apps.foreach { (t) => t._2.stop }
  }

  def maybeStartPOD(c:PODMeta) {
    try {
      val ctr=Container.create( _appReg,c)
      if (ctr.isEnabled) {
        _apps.put(c.name, ctr)
        _jmx.register(ctr,"", c.name)
      } else {
        tlog.info("Container \"{}\" is set offline.", c.name )
      }
    } catch {
      case e:Throwable => tlog.error("",e)
    }
  }

}

