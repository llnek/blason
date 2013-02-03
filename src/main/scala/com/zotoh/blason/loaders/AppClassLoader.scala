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
package loaders

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.io.{FileUtils=>FUT}
import java.net.URLClassLoader
import java.io.File
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import java.net.URL
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.blason.core.Constants

/**
 * @author kenl
 */
class AppClassLoader(par:RootClassLoader) extends AbstractClassLoader( par)  {

  //println("AppClassLoader ctor!!!!!")
  //println("Parent = " + getParent().getClass().getName() )
  iniz()

  def configure(cfg:Configuration) {
    val base= cfg.getString(K_APPDIR,"")
    tstEStrArg(K_APPDIR, base)
    load(base)
  }

  private def load(baseDir:String) {
    val c= new File(baseDir, POD_CLASSES)
    val p= new File(baseDir, POD_PATCH)
    val b= new File(baseDir, POD_LIB)
    findUrls(p).addUrl(c,true).findUrls(b)
    if (new File(baseDir, WEB_INF).exists()) {
      addUrl(new File(baseDir, WEB_CLASSES),true).findUrls(new File(baseDir, WEB_LIB))
    }
    _loaded=true
  }


  private def iniz() {
  }

}
