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
import scala.collection.mutable
import java.net.URL
import java.util.{Date=>JDate,Map=>JMap,List=>JList,LinkedHashMap=>JLMap}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.blason.core.Configuration
import com.zotoh.frwk.util.JSONUtils
import org.json.{JSONArray, JSONObject}

/**
 * @author kenl
 */
class DefaultConfiguration(private var _node:JSONObject, p:Configuration=null) extends AbstractConfiguration(p) {

  if (_node==null) { _node= JSONUtils.newJSON()  }

  def this(cfgUrl:URL) {
    this(null,null)
    iniz(cfgUrl)
  }

  def this() {
    this(null,null)
  }

  def contains(n:String) = _node.has(n)
  def size() = _node.keySet().size()

  def asJavaMap() = JSONUtils.asJavaMap(_node )
  def asMap() = JSONUtils.asMap(_node )

  def initialize() { }

  def dispose() {
    setParent(null)
    _node=null
  }

  def getLong(n:String, dft:Long) = {
    if (_node.has(n)) {
      _node.optLong(n,dft)
    } else {
      dft
    }
  }

  def getDouble(n:String, dft:Double) = {
    if (_node.has(n)) {
      _node.optDouble(n,dft)
    } else {
      dft
    }
  }

  def getBool(n:String,dft:Boolean) = {
    if ( _node.has(n)) {
      _node.optBoolean(n,dft)
    } else {
      dft
    }
  }

  def getDate(n:String) :Option[JDate] = {
    if ( _node.has(n)) {
      _node.optString(n,"") match {
        case s if s.length > 0 => parseDate(s)
        case _ => None
      }
    } else {
      None
    }
  }

  def getString(n:String, dft:String) =  {
    if ( _node.has(n)) {
      _node.optString(n,dft)
    } else {
      dft
    }
  }

  def getKeys() = {
    _node.keySet().toArray.map { (k) => k.toString
    }.toSeq
  }

  def getChild(n:String) = {
    if (_node.has(n)) {
      _node.get(n) match {
        case m:JSONObject =>
          Some( new DefaultConfiguration(m,this))
        case _ =>
          None
      }
    } else {
      None
    }
  }

  def getSequence(n:String) = {
    val empty= List()
    val rc= if (_node.has(n)) {
      _node.get(n) match {
        case s:JSONArray => jiggleList(s)
        case _ => empty
      }
    } else {
      empty
    }

    rc.toSeq
  }

  private def jiggleList(lst:JSONArray) : List[_] = {
    val rc=mutable.ArrayBuffer[Any]()
    for ( i <- 0 until lst.length ) {
      lst.get(i) match {
        case x:JSONObject => rc += new DefaultConfiguration(x,this)
        case x:JSONArray => rc += jiggleList(x)
        case z => rc += z
      }
    }
    rc.toList
  }

  private def iniz(cfgUrl:URL) {
    using( cfgUrl.openStream ) { (inp) =>
      _node = JSONUtils.read(inp)
    }
  }

}


class EmptyConfiguration extends DefaultConfiguration(null,null) {
}

