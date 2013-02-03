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

package com.zotoh.jmx
package support

import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.{Properties=>JPS}

/**
 * @author kenl
 */
class JMXMethodInfo(private val _name:String, private val _desc:String = "") {

  private var _type:JMXBeanAction = null
  private var _pms:JPS = null

  def this(name:String, info:JMXBeanAction, pms:JPS, desc:String = "" ) {
    this(name,desc)
    _pms= pms
    _type = info
  }

  def this(name:String , mtd:JMXMethod) {
    this(name, mtd.desc )
    val ds= mtd.paramDescs()
    val dl=ds.length
    var i=0
    val m=new JPS()
    mtd.params().foreach { (p) =>
      m.put(p, if (i >= dl) "" else ds(i) )
      i += 1
    }
    _type= mtd.action()
    _pms= m
  }

  def name() = _name


  def info() = _type

  def desc() = _desc

  def params() = _pms.toMap[String,String]
  
}
