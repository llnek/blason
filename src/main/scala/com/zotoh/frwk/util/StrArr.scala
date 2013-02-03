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

package com.zotoh.frwk
package util

import scala.collection.mutable.ArrayBuffer
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.lang3.{StringUtils=>STU}

object StrArr {
}

/**
 * Wrapper on top of a string[].
 *
 * @author kenl
 *
 */
@SerialVersionUID(981284723453L)
class StrArr(strs:String*) extends Serializable {

  private val _strs= ArrayBuffer[String]()
  add(strs.toSeq)

  /**
   * @param s
   */
  def add(s:String): this.type = {
    if (s != null) { _strs += s }
    this
  }

  /**
   * @param a
   */
  def add(a:Seq[String]): this.type = {
    a.foreach((i) => add(i))
    this
  }

  /**
   * @return
   */
  def toArray() = _strs.toArray

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = join( _strs.toSeq, "|")

  /**
   * @return
   */
  def size() = _strs.length

  /**
   * @return
   */
  def first() = {
    if (_strs.length == 0) None else Some(_strs(0))
  }

  /**
   * @return
   */
  def last() = {
    if (_strs.length == 0) None else Some(_strs( _strs.length-1))
  }

  /**
   * @param str
   * @return
   */
  def contains(s:String) = _strs.contains(s)

}

