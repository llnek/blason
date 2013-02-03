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
package impl

import java.net.URL
import java.io.File
import com.zotoh.blason.core.Context
import org.apache.commons.io.{FileUtils=>FUT}
import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.blason.core.PODMeta
import com.zotoh.blason.core.Deployer
import com.zotoh.blason.core.RegistryError
import com.zotoh.blason.core.ConfigError
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.blason.core.ContextError


/**
 * @author kenl
 */
class DefaultAppRegistry extends AbstractRegistry(null) {

  private val _apps= mutable.ArrayBuffer[URL]()

  override def compose(r:ComponentRegistry, arg:Any*) = {
    r.lookup(PF_BLOCKS) match {
      case Some(r:ComponentRegistry) => setParent(r)
      case _ => /*noop*/
    }
    None
  }

  def contextualize(c:Context) {
    c.get(K_BASEDIR) match {
      case Some(x:String) => _baseDir=new File(x)
      case Some(x:File) => _baseDir=x
      case _ => throw new ContextError(K_BASEDIR+" undefined")
    }
  }

  // get all application pod files (.pod suffix)
  // and then inspect them and register them
  def initialize() {
    FUT.listFiles(_baseDir, Array("pod"),false).foreach { (f) =>
      _apps += f.toURI.toURL
    }
  }

}
