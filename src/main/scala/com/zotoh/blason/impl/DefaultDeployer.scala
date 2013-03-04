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
import org.slf4j._
import org.apache.commons.io.{FileUtils=>FUT}
import org.apache.commons.io.FilenameUtils
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import com.zotoh.blason.core.Deployer
import java.net.URL
import com.zotoh.blason.core.Contextualizable
import java.util.zip.ZipFile
import java.util.{Properties=>JPS}
import com.zotoh.blason.core.Initializable
import java.io.File
import com.zotoh.blason.core.Loggable
import com.zotoh.blason.core.Context
import java.io.File
import java.io.FileInputStream
import com.zotoh.blason.core.ContextError
import com.zotoh.blason.core.PODMeta
import java.io.IOException
import com.zotoh.blason.core.Component
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.blason.core.Constants


/**
 * @author kenl
 */
object DefaultDeployer{
  private val _log = LoggerFactory.getLogger(classOf[DefaultDeployer])
}

/**
 * @author kenl
 */
class DefaultDeployer extends Deployer with Component with Constants
with Initializable with Contextualizable with Loggable with CoreImplicits {

  // compose, contextualize,configure,initialize

  def tlog() = DefaultDeployer._log

  private var _baseDir:File=null
  private var _podsDir:File=null
  private var _playDir:File=null

  def name() = "deployer"
  def version() =""

  override def compose(r:ComponentRegistry, arg:Any*) = {
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
  }

  // get all application pod files (.pod suffix)
  // and then inspect them and deploy them
  def initialize() {
    FUT.listFiles(_podsDir, Array("pod"),false).foreach { (f) =>
      deploy(f.toURI.toURL)
    }
  }

  // src points to the pod file:/a/b/c.pod
  def deploy(src:URL ) {
    if ( "file" != src.getProtocol ) { throw new IOException("Unsupported url " + src) }
    val app=FilenameUtils.getBaseName(src.toString)
    val des=new File(_playDir, app)
    val fp=new File(src.toURI)

    if (!des.exists()) {
      unzip(des, new ZipFile(fp))
    } else {
      tlog.info("app: {} has already been deployed." , app )
    }

  }

  def undeploy(app:String) {
    val dir=new File(_playDir, app)
    if (dir.exists) {
      FUT.deleteDirectory(dir)
      tlog.info("Undeployed app: {}." , app )
    } else {
      tlog.warn("Cannot undeploy: app: {} doesn't exist, no operation taken.", app)
    }
  }

  private def unzip(des:File, zf:ZipFile) {
    using(zf) { (z) => extractAll(z,des) }
  }

}

