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
import org.apache.commons.lang3.{StringUtils=>STU}

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils._
import java.net.URL
import com.zotoh.blason.impl.DefaultConfiguration
import org.slf4j._
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.frwk.util.INIConf
import java.io.File
import com.zotoh.frwk.util.Section



/**
 * @author kenl
 */
object BlockMeta {
  private val _log= LoggerFactory.getLogger(classOf[BlockMeta])
}

/**
 * @author kenl
 */
class BlockMeta(private var _url:URL) extends Component with Initializable with Loggable {
  // url points to the meta file

  private var _desc:BlockDescriptor= null
  private var _meta:INIConf = null

  def tlog() = BlockMeta._log

  def compose(r:ComponentRegistry, arg:Any*) = {
    val obj= mkRef(_desc.typeId)
    obj match {
      case x:Composable => x.compose(r, arg:_* )
      case _ => /* noop */
    }
    Some( obj)
  }

  def version() = _desc.version()
  def name() = _desc.typeId()
  def descriptor() = _desc

  def initialize() {
    _meta= new INIConf(new File(_url.toURI ) )
    verify()
  }

  private def verify() {
    _meta.section("info") match {
      case Some(m) => parseInfo(m)
      case _ => throw new ConfigError("Missing Block-Info")
    }
  }

  private def parseInfo( root:Map[String,String] ) {
    val t= strim( root.getOrElse("block-type","") )
    val v= strim( root.getOrElse("version","") )
    val n= strim( root.getOrElse("name","") )
    _desc=new BlockDescriptor(t,n,v)
  }

}

