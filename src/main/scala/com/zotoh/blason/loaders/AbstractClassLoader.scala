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
//import org.apache.commons.io.{FileUtils=>FUT}
import java.net.URLClassLoader
import java.io.File
import com.zotoh.blason.core.Configurable
import com.zotoh.blason.core.Configuration
import java.net.URL
import com.zotoh.blason.core.Constants
import java.io.FilenameFilter

/**
 * @author kenl
 */
abstract class AbstractClassLoader(par:ClassLoader)  extends URLClassLoader( Array[URL]() ,par) with Constants with Configurable {
  protected var _loaded=false

  protected[loaders] def findUrls(dir:File) = {
    dir.listFiles( new FilenameFilter() {
      def accept(f:File,n:String) = n.endsWith(".jar")
    }).foreach { (f) =>
      addUrl(f)
    }
    this
  }

  protected[loaders] def addUrl(f:File, cz:Boolean=false) = {
    var url=f.toURI.toURL
//    if (cz) {
//      url = new URL( url.toString() + "/")
//    } else {
//    }
    addURL( url)
    //println(getClass().getSimpleName() + ": loadUrl: " + url.toString)
    this
  }


}
