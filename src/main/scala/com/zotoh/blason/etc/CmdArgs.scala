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
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{FileUtils=>FUT}
import java.io.File
import java.util.{Properties=>JPS}
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.security._
import com.zotoh.frwk.i18n.Resources
import com.zotoh.blason.core.CLIMain
import com.zotoh.blason.core.Constants
import com.zotoh.frwk.util.CmdLineMust
import com.zotoh.frwk.util.CmdLineSeq


/**
 * @author kenl
 */
object CmdArgs extends CoreImplicits {
  protected val _ARGS= Map(
    "create" -> "onCreate" ,
    "ide" -> "onIDE",
    "build" -> "onBuild",
    "podify" -> "onPodify",
    "test" -> "onTest",
    "debug" -> "onDebug",
    "start" -> "onStart",
    "demo" -> "onDemo",
    "generate" -> "onGenerate",
    "encrypt" -> "onEncrypt",
    "testjce" -> "onTestJCE",
    "version" -> "onVersion",
    "help" -> "onHelp"
  )
}

/**
 * @author kenl
 */
class CmdArgs(home:File,cwd:File,rc:Resources) extends CmdLine(home,cwd,rc) with CoreImplicits with Constants {
  import CmdArgs._

  def getCmds() = _ARGS.keys.toSet

  def eval(args:Array[String] ) {
    _ARGS.get(args(0)) match {
      case Some(mtd) =>
        getClass().getDeclaredMethod(mtd, classOf[Array[String]]).invoke(this, args)
      case _ =>
        throw new CmdHelpError()
    }
  }

  private def onCreate(args:Array[String]) {
    if (args.size >= 1 ) {
      onCreatePrompt() match {
        case (true, x) =>
          if ( STU.isEmpty( x.gets(PF_BLASON_APPDOMAIN)) || 
              STU.isEmpty( x.gets(PF_BLASON_APPID)) ) {
            throw new CmdHelpError()
          } 
          onCreateApp(args, x)
        case (false, x) => // canceled
      }      
    } else {
      throw new CmdHelpError()
    }
  }
  
  private def onCreateApp(args:Array[String], ps:JPS) {
    val cb = { (target:String, p:JPS) =>
      runTargetExtra( target, p)
    }
    if (args.size > 1) {
        args(1) match {
          case "web/jetty" => cb("create-jetty",ps)
          case "web" => cb("create-web",ps)
          case _ => throw new CmdHelpError()
        }
      } else {
        cb("create-app",ps)
      }    
  }  
  
  private def onCreatePrompt() = {
    val domain= "com."  + userName()
    val q1= new CmdLineMust("domain", "What is the application domain", domain, domain) {
      def onRespSetOut(a:String, ps:JPS) = {
        ps.put(PF_BLASON_APPDOMAIN, a)
        "app"
      }
    }
    val q2= new CmdLineMust("app", "What is the application name", "", "") {
      def onRespSetOut(a:String, ps:JPS) = {
        ps.put( PF_BLASON_APPID, a)
        ""
      }
    }
     val seq= new CmdLineSeq(q1,q2){
            def onStart() = q1.label
      }
     val ps= new JPS()
     if ( seq.start(ps).isCanceled ) ( false, ps) else (true, ps)
  }
  
  private def onIDE(args:Array[String]) {
    if (args.size > 2) {
      args(1) match {
        case "eclipse" => genEclipseProj( args(2))
        case _ => throw new CmdHelpError()
      }
    } else {
      throw new CmdHelpError()
    }
  }

  private def onBuild(args:Array[String]) {
    if (args.size >=2 ) {
      val t = if (args.size > 2) args(2) else "devmode"
      runTargetExtra("build-app", 
          new JPS().add(PF_BLASON_APPID, args(1)).add(PF_BLASON_APPTASK, t) )
    } else {
      throw new CmdHelpError()
    }
  }

  private def onPodify(args:Array[String]) {
    if (args.size > 1) {
      runTargetExtra("bundle-app", 
          new JPS().add(PF_BLASON_APPID, args(1)).add( PF_BLASON_APPTASK,"release") )
    } else {
      throw new CmdHelpError()
    }
  }

  private def onTest(args:Array[String]) {
    if (args.size > 1) {
      runTargetExtra("test-code", new JPS().add( PF_BLASON_APPID, args(1)) )
    } else {
      throw new CmdHelpError()
    }
  }

  private def onDebug(args:Array[String]) {
    onStart(args)
    //runTarget( if (isWindows() ) "run-dbg-app-w32" else "run-dbg-app-nix")
  }

  private def onStart(args:Array[String]) {
    val s2=if (args.size > 1) args(1) else ""
    s2 match {
      case "bg"  if isWindows =>
            runTarget( "run-app-bg-w32")
      case _ =>
            new CLIMain().start( Array( niceFPath(getHomeDir) ) )
    }
  }

  private def onDemo(args:Array[String]) {
    if (args.size > 1 ) {
      args(1) match {
        case "samples" =>runTarget("create-samples")
        case s =>runTargetExtra("create-demo",  new JPS().add( "demo.id", s) )
      }      
    } else {
      throw new CmdHelpError()
    }
  }

  private def onGenerate(args:Array[String]) {
    if (args.size > 1) {
      val c=new CmdCrypto(getHomeDir,getCwd,_rcb)
      args(1) match {
        case "password" => c.generatePassword
        case "serverkey" => c.keyfile
        case "csr" => c.csrfile
        case _ => throw new CmdHelpError()
      }
    } else {
      throw new CmdHelpError()
    }
  }

  private def onEncrypt(args:Array[String]) {
    if (args.size > 2) {
      new CmdCrypto(getHomeDir,getCwd,_rcb).encrypt( args(1), args(2)  )
    } else {
      throw new CmdHelpError()
    }
  }

  private def onTestJCE(args:Array[String]): Unit = new CmdCrypto(getHomeDir,getCwd,_rcb).testjce()

  private def onVersion(args:Array[String]) {
    readText(new File( getHomeDir, "VERSION"), "utf-8") match {
      case s if s.length > 0 => println(s)
      case _ => println("Unknown version.")
    }
  }

  private def onHelp(args:Array[String]) {
    throw new CmdHelpError()
  }

  private def genEclipseProj(app:String) {
    val cwd= new File(getHomeDir, DN_BOXX+"/"+app)
    val ec= new File( cwd, "eclipse.projfiles")
    val lang="scala"
    ec.mkdirs()
    FUT.cleanDirectory(ec)
    var str=rc2Str("com/zotoh/blason/eclipse/"+lang+"/project.txt", "utf-8")
    str=STU.replace(str, "${APP.NAME}", app)
    str=STU.replace(str, "${"+lang.uc+".SRC}", niceFPath(new File(cwd, "src/main/"+lang)))
    str=STU.replace(str, "${TEST.SRC}", niceFPath(new File(cwd, "src/test/"+lang)))
    var out= new File(ec, ".project")
    writeFile(out, str, "utf-8")
    str=rc2Str("com/zotoh/blason/eclipse/"+lang+"/classpath.txt", "utf-8")
    val sb= new StringBuilder(512)
    scanJars(new File(getHomeDir, DN_DIST), sb)
    scanJars(new File(getHomeDir, DN_LIB), sb)
    scanJars(new File(cwd, POD_CLASSES), sb)
    scanJars(new File(cwd, POD_LIB), sb)
    str=STU.replace(str, "${CLASS.PATH.ENTRIES}", sb.toString)
    out= new File(ec, ".classpath")
    writeFile(out, str, "utf-8")
  }

  private def scanJars(dir:File , out:StringBuilder ) {
    val sep= System.getProperty("line.separator")
    FUT.listFiles(dir,Array("jar"),false).foreach { (f) =>
      var p=niceFPath(f)
      out.append( s"""<classpathentry  kind="lib" path="$p"/>""" ).append(sep)
    }
  }

}
