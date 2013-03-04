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

import org.slf4j._
import com.zotoh.blason.impl.DefaultAppRegistry
import com.zotoh.blason.impl.AbstractRegistry
import com.zotoh.blason.impl.DefaultKernel
import com.zotoh.blason.impl.DefaultDeployer
import java.util.{Date=>JDate}
import java.io.File
import com.zotoh.blason.impl.DefaultContext
import com.zotoh.blason.impl.AbstractContext
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.INIConf
import com.zotoh.jmx.support.JMXServer
import com.zotoh.jmx.support.JMXResource
import com.zotoh.jmx.support.JMXField
import com.zotoh.jmx.support.JMXProperty
import com.zotoh.jmx.support.JMXMethod
import com.zotoh.jmx.support.JMXBeanAction


/**
 * @author kenl
 */
object Execvisor {
  private val _log=LoggerFactory.getLogger(classOf[Execvisor])
}

/**
 * @author kenl
 */
@JMXResource(desc="Blason Platform: Execvisor.", domain="com.zotoh",
  beanName="execvisor",
  paths=Array("product=blason","component=kernel"))
class Execvisor extends Contextualizable with Startable with Initializable with Loggable with Constants {

  // order of construction => contextualize, initialize

  def tlog() = Execvisor._log

  import CLIMain._

  private val _startTS= new JDate().getTime

  // holds key components such as kernel, deployer...etc
  private var _svcs:AbstractRegistry=null

  private var _ctx:AbstractContext=null
  private var _conf:INIConf=null

  private var _jmx:JMXServer= null
  private var _jmxHost=""
  private var _jmxPort=0

  @JMXProperty(desc="Up time in milliseconds.")
  def getUpTimeInMillis() = System.currentTimeMillis - _startTS

  @JMXProperty(desc="Start time.")
  def getStartTime() = new JDate( _startTS)

  def contextualize(c:Context) {
    _ctx= c match {
      case x:AbstractContext => x
      case _ => throw new ContextError("Expecting AbstractContext.")
    }
  }

  def initialize() {
    _conf= _ctx.get(PF_PROPS) match {
      case Some(x:INIConf) => x
      case _ => throw new ContextError("Expecting INIConf.")
    }
    iniz_0()
  }

  private def iniz_0() {
    val comps = _conf.section(PF_COMPS).getOrElse(null)
    val regs = _conf.section(PF_REGS).getOrElse(null)
    val jmx = _conf.section(PF_JMXMGM).getOrElse(null)

    tstObjArg("conf file: components", comps)
    tstObjArg("conf file: jmx mgmt",jmx)
    tstObjArg("conf file: registries",regs)

    _jmxPort= asInt( jmx.getOrElse("port",""), 7777)
    //_jmxHost= jmx.getOrElse("host","localhost")
    _jmxHost= jmx.getOrElse("host","")
    _ctx.put(PF_EXECV, this)

    System.setProperty("file.encoding", "utf-8")

    inizAppEnv()
    startJMX()

    var s= regs.getOrElse(PF_SVCS,"")
    _svcs= mkRef(s) match {
      case x:AbstractRegistry => x
      case _ => throw new ComponentError("Null Services.")
    }
    tweak(_svcs)

    s=regs.getOrElse(PF_BLOCKS,"")
    _svcs.add(PF_BLOCKS, tweak( mkRef(s) match {
      case x:AbstractRegistry => x
      case _ => throw new ComponentError("Null Registry: Blocks.")
    } ))

    s=regs.getOrElse(PF_APPS,"")
    _svcs.add(PF_APPS, tweak( mkRef(s) match {
      case x:AbstractRegistry => x
      case _ => throw new ComponentError("Null Registry: Apps.")
    } ))

    s=comps.getOrElse(PF_DEPLOYER,"")
    _svcs.add( PF_DEPLOYER, tweak( mkRef(s) match {
      case x:DefaultDeployer => x
      case _ => throw new ComponentError("Null Deployer.")
    } ))

    s=comps.getOrElse(PF_KERNEL,"")
    _svcs.add( PF_KERNEL, tweak( mkRef(s) match {
      case x:DefaultKernel => x
      case _ => throw new ComponentError("Null Kernel.")
    } ))

  }

  @JMXMethod(desc="Start Execvisor.", action=JMXBeanAction.WRITE)
  def start() {
    _svcs.lookup(PF_KERNEL) match {
      case Some(x:Kernel) => x.start()
      case _ => throw new ConfigError("Nothing to start - kernel is absent.")
    }
  }

  @JMXMethod(desc="Stop Execvisor.", action=JMXBeanAction.WRITE)
  def stop() {
    _svcs.lookup(PF_KERNEL) match {
      case Some(x:Kernel) => x.stop()
      case _ => /* noop */
    }
    stopJMX()
  }

  @JMXMethod(desc="Kill Blason.", action=JMXBeanAction.WRITE)
  def kill9() {
    _ctx.get(PF_CLISH) match {
      case Some(x:CLIMain) => x.stop()
      case _ => /* noop */
    }
  }

  private def startJMX() {
    try {
      _jmx= new JMXServer(_jmxHost).setRegistryPort(_jmxPort)
      _jmx.start
      _jmx.register(this)
      println("JMXserver listening on: " + _jmxHost+" "+_jmxPort)
    } catch {
      case e:Throwable => tlog.error("",e)
    }
  }

  private def stopJMX() {
    if (_jmx != null) block { () =>
      _jmx.stop
      println("JMX connection terminated.")
    }
    _jmx=null
  }

  private def inizAppEnv() {
    val sandboxes=new File(homeDir, DN_BOXX)
    sandboxes.mkdir
    val pods=new File(homeDir, DN_PODS)
    pods.mkdir

    precondDir(sandboxes)
    precondDir(pods)

    _ctx.put(K_PLAYDIR, sandboxes)
    _ctx.put(K_PODSDIR, pods)

    val bks=new File(homeDir, DN_BLOCKS)
    val tmp=new File(homeDir, DN_TMP)
    val db=new File(homeDir, DN_DBS)
    val log=new File(homeDir, DN_LOGS)

    bks.mkdir
    log.mkdir
    tmp.mkdir
    db.mkdir

    precondDir(log)
    precondDir(tmp)
    precondDir(db)
    precondDir(bks)

    _ctx.put(K_LOGDIR, log)
    _ctx.put(K_TMPDIR, tmp)
    _ctx.put(K_DBSDIR, db)
    _ctx.put(K_BKSDIR, bks)

  }

  def homeDir() = maybeDir(K_BASEDIR)
  def confDir() = maybeDir(K_CFGDIR)
  def podsDir() = maybeDir(K_PODSDIR)
  def playDir() = maybeDir(K_PLAYDIR)
  def logDir() = maybeDir(K_LOGDIR)
  def tmpDir() = maybeDir(K_TMPDIR)
  def dbDir() = maybeDir(K_DBSDIR)
  def blocksDir() = maybeDir(K_BKSDIR)

  private def maybeDir(key:String) = {
    _ctx.get(key) match {
      case Some(s:String) => new File(s)
      case Some(f:File) => f
      case _ => throw new ConfigError("No such folder for key: " + key)
    }
  }

  private def tweak(c:Component): Component = {
    c.compose(_svcs,_jmx)
    c match {
      case x:Contextualizable => x.contextualize(_ctx)
      case _ =>
    }
    c match {
      case x:Configurable =>
      case _ =>
    }
    c match {
      case x:Initializable => x.initialize
      case _ =>
    }
    c
  }

}
