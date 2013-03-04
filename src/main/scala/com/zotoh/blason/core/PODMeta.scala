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
import java.util.{Properties=>JPS}
import java.net.URL
import org.slf4j._
import com.zotoh.blason.impl.AbstractRegistry
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._
import java.io.File
import com.zotoh.blason.impl.PropsConfiguration
import com.zotoh.blason.loaders.AppClassLoader
import com.zotoh.blason.loaders.RootClassLoader




object PODMeta{
  private val _log = LoggerFactory.getLogger(classOf[PODMeta])
}


/**
 * @author kenl
 */
class PODMeta(name:String, version:String, podType:String,
    private val _src:URL) extends PODDescriptor(name,version,podType) with Component with Constants
    with Initializable with Contextualizable with Loggable {

  private var _cl:AppClassLoader=null
  private var _ctx:Context=null

  def tlog() = PODMeta._log

  def getSpaceUrl = _src
  def getCZldr() = _cl

  def contextualize(c:Context) {
    _ctx=c
  }

  def compose(r:ComponentRegistry, arg:Any*) = {
    None
  }

  def initialize() {
    val ps=new JPS().add(K_APPDIR , niceFPath( new File(_src.toURI)) )
    val c=new PropsConfiguration(ps)
    val root = _ctx.get(K_ROOT_CZLR) match {
      case Some(c:RootClassLoader) => c
      case _ => throw new ContextError(K_ROOT_CZLR +"  undefined.")
    }
    _cl=new AppClassLoader(root)
    _cl.configure(c)
  }

}
