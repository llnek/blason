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

import java.io.File
import scala.collection.mutable
import org.slf4j.LoggerFactory
import com.zotoh.blason.core.Component
import com.zotoh.blason.core.ComponentRegistry
import com.zotoh.blason.core.Loggable
import com.zotoh.blason.core.RegistryError
import com.zotoh.blason.core.Servicable
import com.zotoh.blason.core.Constants
import com.zotoh.blason.core.Contextualizable
import com.zotoh.blason.core.Initializable


object AbstractRegistry {
  private val _log= LoggerFactory.getLogger(classOf[AbstractRegistry])
}

/**
 * @author kenl
 */
abstract class AbstractRegistry( protected var _par:ComponentRegistry = null) extends ComponentRegistry with Component
    with Contextualizable with Initializable with Loggable  with Constants{

  protected val _components = new mutable.HashMap[Any,Component] with mutable.SynchronizedMap[Any,Component]
  protected var _baseDir:File=null
  protected var _name=""
  protected var _version=""

  import AbstractRegistry._
  def tlog() = _log

  def getParent() = if ( _par == null) None else Some(_par)
  def setParent(p:ComponentRegistry) { _par=p }

  def lookup( cid:String) = {
    _components.get(cid) match {
      case r@Some(x) => r
      case _ => if (_par != null) _par.lookup(cid) else None
    }
  }

  def hasComponent( cid:String) = {
    ! lookup(cid).isEmpty
  }

  def add( cid:String, c:Component) {
    if (cid != null && c != null) {
      if (hasComponent(cid))  {
        throw new RegistryError("Component-Id \"" + cid + "\" already exists" )
      }
      _components.put(cid,c)
    }
  }

  def release( c:Component ) {
    val name= if (c!=null) c.name else ""
    if (hasComponent(name)) {
      _components.remove(name)
      //TODO, do something to c ???
    }
  }

  def getComponents() = _components.values.toSeq

  def setVersion(v:String) { _version=v }
  def setName(n:String) { _name=n }

  def version() = _version
  def name() = _name

  def compose(r:ComponentRegistry, arg:Any*) = None

}
