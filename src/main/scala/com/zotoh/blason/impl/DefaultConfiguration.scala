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
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.{JsonFactory,JsonParser}
import com.fasterxml.jackson.databind.JsonNode
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.blason.core.Configuration

/**
 * @author kenl
 */
class DefaultConfiguration(private var _node:JMap[_,_], p:Configuration=null) extends AbstractConfiguration(p) {

  if (_node==null) { _node= new JLMap()  }

  def this(cfgUrl:URL) {
    this(null,null)
    iniz(cfgUrl)
  }

  def this() {
    this(null,null)
  }

  def contains(n:String) = _node.containsKey(n)
  def size() = _node.size()

  def asJHM() = { _node }

  def initialize() { }

  def dispose() {
    setParent(null)
    _node=null
  }

  def getLong(n:String, dft:Long) = {
    if (_node.containsKey(n)) {
      _node.get(n) match {
        case s:String => asLong(s,dft)
        case g:Long => g
        case n:Int => n.toLong
        case _ => dft
      }
    } else {
      dft
    }
  }

  def getDouble(n:String, dft:Double) = {
    if (_node.containsKey(n)) {
      _node.get(n) match {
        case s:String => asDouble(s,dft)
        case d:Double => d
        case f:Float => f.toDouble
        case _ => dft
      }
    } else {
      dft
    }
  }

  def getBool(n:String,dft:Boolean) = {
    if ( _node.containsKey(n)) {
      _node.get(n) match {
        case s:String => asBool(s,dft)
        case b:Boolean => b
        case _ => dft
      }
    } else {
      dft
    }
  }

  def getDate(n:String) :Option[JDate] = {
    if ( _node.containsKey(n)) {
      _node.get(n) match {
        case s:String => parseDate(s)
        case _ => None
      }
    } else {
      None
    }
  }

  def getString(n:String, dft:String) =  {
    if ( _node.containsKey(n)) {
      nsb( _node.get(n))
    } else {
      dft
    }
  }

  def getKeys() = {
    _node.keySet().toArray.map { (k) => k.toString
    }.toSeq
  }

  def getChild(n:String) = {
    if (_node.containsKey(n)) {
      _node.get(n) match {
        case m:JMap[_,_] =>
          Some( new DefaultConfiguration(m,this))
        case _ =>
          None
      }
    } else {
      None
    }
  }

  def getSequence(n:String) = {
    val empty= List[Any]()
    val rc= if (_node.containsKey(n)) {
      _node.get(n) match {
        case s:JList[_] => jiggleList(s)
        case _ => empty
      }
    } else {
      empty
    }
    rc.toSeq
  }

  private def jiggleList(lst:JList[_]) : List[_] = {
    val rc=mutable.ArrayBuffer[Any]()
    lst.foreach { _ match {
      case x:JMap[_,_] => rc += new DefaultConfiguration(x,this)
      case x:JList[_] => rc += jiggleList(x)
      case z => rc += z
    }}
    rc.toList
  }

  private def iniz(cfgUrl:URL) {
    _node= new ObjectMapper().readValue(
      new JsonFactory().createParser( cfgUrl ),
      classOf[JMap[String,_]]
    )
  }

}


class EmptyConfiguration extends DefaultConfiguration(null,null) {
}

