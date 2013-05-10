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

import java.util.{Date=>JDate,Properties=>JPS,Map=>JMap}
import com.zotoh.blason.core.Configuration
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._



/**
 * @author kenl
 */
class PropsConfiguration(private val _props:JPS) extends Configuration {

  def asMap = { _props.toMap }
  def asJavaMap = {
    val rc= new java.util.HashMap[String,Any]()
    _props.keys().foreach { (key)  =>
      val k=nsb(key)
      rc.put(k, _props.get(key))
    }
    rc
  }

  def getString(name:String,dft:String) = {
    if (_props.containsKey(name)) {
      nsb( _props.get(name))
    } else {
      dft
    }
  }

  def getDouble(name:String,dft:Double) = {
    if (_props.containsKey(name)) {
      _props.get(name) match {
        case s:String => asDouble(s,dft)
        case z =>
          if (z.isInstanceOf[Double]) {
            z.asInstanceOf[Double]
          } else if (z.isInstanceOf[Float]) {
            z.asInstanceOf[Float].toDouble
          } else {
            dft
          }
      }
    } else {
      dft
    }
  }

  def getLong(name:String,dft:Long) = {
    if (_props.containsKey(name)) {
      _props.get(name) match {
        case s:String => asLong(s,dft)
        case z =>
          if (z.isInstanceOf[Long]) {
            z.asInstanceOf[Long]
          } else if (z.isInstanceOf[Int]) {
            z.asInstanceOf[Int].toLong
          } else {
            dft
          }
      }
    } else {
      dft
    }
  }

  def getKeys() = {
    _props.keySet().toArray().map { (k) => k.toString
    }.toSeq
  }

  def getBool(name:String,dft:Boolean) = {
    if (_props.containsKey(name)) {
      _props.get(name) match {
        case s:String => asBool(s,dft)
        case z =>
          if (z.isInstanceOf[Boolean]) {
            z.asInstanceOf[Boolean]
          } else {
            dft
          }
      }
    } else {
      dft
    }
  }

  def getDate(name:String) = {
    if (_props.containsKey(name)) {
      _props.get(name) match {
        case d:JDate => Some(d)
        case _ => None
      }
    } else {
      None
    }
  }

  def contains(n:String) = _props.containsKey(n)
  def size() = _props.size()

  def getSequence(name:String) = List[Any]().toSeq
  def getChild(name:String) = None

}
