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

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.blason.core.Component
import com.zotoh.blason.core.ComponentRegistry
import org.apache.commons.io.{FileUtils=>FUT}
import java.io.File
import com.zotoh.blason.core.Context
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.blason.core.ConfigError
import com.zotoh.blason.core.BlockMeta
import com.zotoh.blason.core.ContextError


/**
 * @author kenl
 */
class DefaultBlockRegistry extends AbstractRegistry(null) {

  private var _blocksDir:File= null

  def reify(cid:String): Option[Any] = {
    val a= lookup(cid) match {
      case Some(c) => c.compose(this)
      case _ => None
    }
    if (a.isDefined) {
  //    maybeTweak(a.get)
    }
    a
  }

  override def compose(r:ComponentRegistry, arg:Any*) = {
    setParent(r)
    None
  }

  def contextualize(c:Context) {
    c.get(K_BASEDIR) match {
      case Some(x:String) => _baseDir=new File( x)
      case Some(x:File) => _baseDir=x
      case _ => throw new ContextError(K_BASEDIR+" is undefined.")
    }
    c.get(K_BKSDIR) match {
      case Some(x:String) => _blocksDir=new File( x)
      case Some(x:File) => _blocksDir=x
      case _ => throw new ContextError(K_BKSDIR+" is undefined.")
    }
  }

  // look for block files with .meta suffix
  def initialize() {
    FUT.listFiles(_blocksDir, Array("meta"),false).foreach { (f) =>
      val b= new BlockMeta(f.toURI.toURL)
      b.initialize
      add(b.name, b)
      tlog.info("Added one block: {}", b.name )
    }
  }

}

