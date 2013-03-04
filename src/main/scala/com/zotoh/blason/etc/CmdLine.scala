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
package etc

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.StrUtils._
import java.io.File
import java.util.{Properties=>JPS}
import java.util.ResourceBundle
import com.zotoh.frwk.i18n.Resources
import com.zotoh.blason.core.Constants

/**
 * (Internal use only).
 *
 * @author kenl
 */
abstract class CmdLine protected(private val _home:File, private val _cwd:File,
    protected val _rcb:Resources) extends Constants with CoreImplicits {

  def evalArgs(args:Array[String] ) {
    eval(args)
    println("")
  }

  protected def eval(args:Array[String] ): Unit

  def getCmds(): Set[String]

  protected def runTargetExtra(target:String, ps:JPS ) {
    val arr= mutable.ArrayBuffer[String]()
    arr += "-Dblason_home="+ niceFPath(getHomeDir)
    ps.keys.foldLeft( arr ) { (z, k) =>
       z += "-D" + k + "=" + nsb(ps.get(k))
    }
    arr += "-buildfile"
    arr += getBuildFilePath
    //arr += "-quiet"
    arr += target
    org.apache.tools.ant.Main.start( arr.toArray, null, getCZldr())
  }

  protected def runTargetInProc(target:String ) {
    new AntMainXXX().startAnt( Array(
      "-buildfile",
      getBuildFilePath,
//    "-quiet",
      target
    ), null, getCZldr() )
  }


  /**
   * @param target
   * @throws Exception
   */
  protected def runTarget(target:String ) {
    org.apache.tools.ant.Main.main( Array(
        "-Dblason_home="+ niceFPath(getHomeDir),
      "-buildfile",
      getBuildFilePath,
//    "-quiet",
      target
    ))
  }

  protected def getHomeDir() = _home
  protected def getCwd() = _cwd

  protected def assertAppDir() {
  }

  private def getBuildFilePath() = {
    niceFPath(new File(new File(getHomeDir, DN_CFG+"/app"), "ant.xml"))
  }

}

class AntMainXXX extends org.apache.tools.ant.Main {
  override def exit(exitCode:Int) {
  }
  def startAnt(args:Array[String], cl:ClassLoader) {
    super.startAnt(args, null,cl)
  }
}

