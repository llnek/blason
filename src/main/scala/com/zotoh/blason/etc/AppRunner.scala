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
package etc

import scala.collection.JavaConversions._
import java.util.{Arrays,Locale,ResourceBundle}
import java.util.{Properties=>JPS}
import java.io.File
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import com.zotoh.frwk.i18n.Resources
import com.zotoh.blason.loaders.RootClassLoader
import com.zotoh.blason.impl.PropsConfiguration
import com.zotoh.blason.core.Constants
import com.zotoh.blason.loaders.ExecClassLoader


/**
 * (Internal use only).
 *
 * @author kenl
 */
object AppRunner {

  def main(args:Array[String]) {
    new AppRunner().start(args)
  }

  private def drawHelpLines(fmt:String, arr:Array[ (String,String) ]) {
    for ( (a,b) <- arr) {
      System.out.format(fmt, a,b)
    }
  }
}

/**
 * @author kenl
 */
class AppRunner extends CoreImplicits with Constants {

  private val _rcb=new Resources("com/zotoh/blason/etc/Resources", Locale.getDefault)
  import AppRunner._

  def start(args:Array[String]) {
    if (!parseArgs(args)) usage else {
    }
  }

  private def usage() {
    println(mkString('=',78))
    println("> blason <commands & options>")
    println("> -----------------")
    val a=Array(
      ("create web[/jetty]",  "e.g. create app as a webapp."),
      ("create",  "e.g. create an app."),
      ("podify <app-name>",  "e.g. package app as a pod file"),

      ("ide eclipse <app-name>", "Generate eclipse project files."),
      ("build <app-name> [target]", "Build app."),
      ("test <app-name>", "Run test cases."),

      ("debug", "Start & debug the application."),
      ("start [bg]", "Start the application."),

      ("generate serverkey", "Create self-signed server key (pkcs12)."),
      ("generate password", "Generate a random password."),
      ("generate csr", "Create a Certificate Signing Request."),
      ("encrypt <password> <some-text>", "e.g. encrypt SomeSecretData"),
      ("testjce", "Check JCE  Policy Files."),

      ("demo samples", "Generate a set of samples."),
      ("version", "Show version info.")
    )
    drawHelpLines("> %-35s\' %s\n", a)
    println(">")
    println("> help - show standard commands")
    println(mkString('=',78))
  }

  private def parseArgs(args:Array[String] ) = {
    // arg(0) is blason-home
    if (args.size < 2) false else {
      val home=STU.stripEnd(new File(args(0)).getCanonicalPath(), System.getProperty("file.separator"))
      val h= new File(home)
      require(h.exists() && h.isDirectory())
      //println("#### apprunner loader = " + getClass().getClassLoader().getClass().getName())
      //println("#### sys loader = " + ClassLoader.getSystemClassLoader().getClass().getName())
      //mkCZldrs(home)
      val cwd= getCWD
      val c=new CmdArgs(h,cwd,_rcb)
      if ( !c.getCmds.contains(args(1))) false else {
        try { c.eval( Arrays.copyOfRange(args, 1, args.size)); true } catch { case _:Throwable => false }
      }
    }
  }

  private def mkCZldrs(baseDir:String) {
    val r=new RootClassLoader( Thread.currentThread().getContextClassLoader )
    val ps=new JPS().add(K_BASEDIR, baseDir)
    val cfg=new PropsConfiguration(ps)
    r.configure(cfg)
    val x=new ExecClassLoader(r)
    x.configure(cfg)
    Thread.currentThread().setContextClassLoader(x)
  }

}
