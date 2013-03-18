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

package com.zotoh.blason
package core

import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{FileUtils=>FUT}
import com.beust.jcommander.JCommander
import java.io.File
import java.util.{Properties=>JPS,Date=>JDate}
import com.zotoh.frwk.util.{CoreImplicits,INIConf}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.blason.loaders.RootClassLoader
import java.util.Locale
import com.zotoh.frwk.i18n.Resources
import java.net.URL
import java.io.FileInputStream
import com.zotoh.blason.impl.DefaultContext
import com.zotoh.blason.impl.DefaultAppRegistry
import com.zotoh.blason.impl.DefaultDeployer
import com.zotoh.blason.impl.DefaultKernel
import com.zotoh.blason.loaders.AppClassLoader
import com.zotoh.blason.loaders.ExecClassLoader
import com.zotoh.blason.impl.AbstractRegistry
import com.zotoh.frwk.util.INIConf
import java.net.ServerSocket
import com.zotoh.frwk.net.MemHTTPServer
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.util.CoreUtils
import com.zotoh.frwk.net.BasicHTTPMsgIO
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.ProcessUtils



/**
 * @author kenl
 */
object CLIMain {

  def precondFile(f:File): Unit = verifyFile( isFileWR, f )
  def precondDir(f:File): Unit = verifyFile( isDirWR, f )

  private def verifyFile(p: (File) => Boolean, f:File ) {
    tstArg( p(f), f.getCanonicalPath+" must be readable & writable" )
  }

  def initAsExtService(appDir: File)( f: (String) => Unit) {
    val pps= CoreUtils.asQuirks(new File(appDir, "META-INF/MANIFEST.MF"))
    val k=pps.getProperty("Implementation-Vendor-Id")    
    f(k)
  }

  def main(args:Array[String]) {
    new CLIMain().start(args)
  }

}

/**
 * @author kenl
 */
class CLIMain extends CoreImplicits with Constants {

  private val _envCtx= new DefaultContext()
  private val _mutex=new Object
  private var _pidFile:File = null
  private var _active=false
  private var _shutServer:MemHTTPServer = null
  private var _locale= Locale.getDefault()
  private var _exec:Execvisor= null
  private var _rc:Resources= null

  import CLIMain._

  def start(args:Array[String]) {
    if ( ! parseArgs(args)) usage() else start()
  }

  def stop() {
    if ( _active) {
      if (_pidFile != null) { FUT.deleteQuietly( _pidFile ) }
      println("About to stop Blason...")
      _exec.stop()
      println("Blason stopped.")
      thaw(_mutex)
      ProcessUtils.asyncExec( new Runnable() {
        def run() {
          block { () => if (_shutServer != null) _shutServer.stop }          
        }
      } ) 
    }
    _active=false
  }

  private def start() {

    println("About to start Blason...")

    primordial.start

    println("Blason started.")

    hookShutdown
    writePID
    
    _envCtx.dbgShow
    
    blockAndWait
  }

  private def primordial() = {
    val cz= _envCtx.get(PF_PROPS) match {
      case Some(w:INIConf) => w.getStr(PF_COMPS,PF_EXECV).getOrElse("")
      case _ => ""
    }

    tstEStrArg("conf file:exec-visor", cz)

    val cl = _envCtx.get(K_EXEC_CZLR) match {
      case Some(c:ClassLoader) => c
      case _ => null
    }

    _exec = mkRef(cz, cl) match {
      case x:Execvisor => x
      case _ =>
      throw new ConfigError("Execvisor class undefined.")
    }

    // order is important!
    _exec.contextualize(_envCtx)
    _exec.initialize()

    _exec
  }

  private def inizContext(baseDir:String) = {
    val cfg= new File( new File(baseDir), DN_CFG )
    val f= new File(cfg, "app/"+PF_PROPS)
    val home=cfg.getParentFile

    precondDir(home)
    precondDir(cfg)
    precondFile(f)

    _envCtx.put(K_BASEDIR, home)
    _envCtx.put(K_CFGDIR, cfg)

    _envCtx
  }

  private def parseArgs(args:Array[String]) = {
    // check cmdline args
    if (args.length < 1) false else {
      preParse(args)
      true
    }
  }

  private def preParse(args:Array[String]) {
    //System.getProperty(PF_HOMEDIR)
    val ctx=inizContext( args(0) )
    val b= new File( args(0))
    precondDir( new File(b,DN_CORE))
    precondDir(new File(b,DN_LIB))
    precondDir(new File(b, DN_PATCH))
    maybeInizLoaders()
    loadConf(b)
    setupResources()
  }

  private def loadConf(home:File) {
    val w=new INIConf( new File( home , DN_CFG+"/app/"+PF_PROPS))
    val lg=w.getStr(K_LOCALE,K_LANG).getOrElse("en").lc
    val cn=w.getStr(K_LOCALE,K_COUNTRY).getOrElse("").uc

    _locale = if (STU.isEmpty(cn)) { new Locale(lg) } else { new Locale(lg,cn) }
    _envCtx.put(PF_CLISH, this)
    _envCtx.put(PF_PROPS, w)
  }

  private def maybeInizLoaders() {
    getCZldr() match {
      case x:ExecClassLoader =>
        _envCtx.put(K_ROOT_CZLR, x.getParent.asInstanceOf[RootClassLoader] )
        _envCtx.put(K_EXEC_CZLR, x)
      case _ =>
        setupClassLoader(setupClassLoaderAsRoot)
    }
  }

  private def setupClassLoader(root:RootClassLoader) {
    val cl=new ExecClassLoader( root )
    _envCtx.put(K_EXEC_CZLR, cl)
    setCZldr( cl)
  }

  private def setupClassLoaderAsRoot() = {
    val root=new RootClassLoader( getCZldr())
    _envCtx.put(K_ROOT_CZLR, root)
    root
  }

  private def setupResources() {
    _rc= new Resources( getClass.getPackage.getName + ".Resources", _locale)
  }

  private def writePID() {
    _pidFile=new File(_exec.homeDir, "blason.pid")
    writeFile(_pidFile, pid() )
  }

  private def hookShutdown() {
    val rt=Runtime.getRuntime
    val me=this
    rt.addShutdownHook( new Thread() {
      override def run() {
        block { () => me.stop() }
      }
    } )
    enableRemoteShutdown()
//    println("Added shutdown hook.")
  }

  private def enableRemoteShutdown() {
    val port= asInt( System.getProperty("blason.kill.port"), 4444)
    _shutServer = new MemHTTPServer( CoreUtils.tmpDir.getCanonicalPath, "127.0.0.1", port)
    val me=this
    _shutServer.bind(new BasicHTTPMsgIO() {
      def onOK(code:Int, r:String, data:XData) {
        block { () =>
          me.stop()
        }
      }
    }).start(false)
    
  }
  
  private def blockAndWait() {
    _active=true
    println("Applications are now running...")
    freeze(_mutex)
    safeWait(1500)
//    blockForever()
    println("Bye.")
    sys.exit(0)
  }

  private def usage() {
  }

}
