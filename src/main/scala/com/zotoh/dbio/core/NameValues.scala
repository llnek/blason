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

package com.zotoh.dbio
package core

import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import java.util.{Properties=>JPS}
import com.zotoh.frwk.util.CoreImplicits

/**
 * @author kenl
 *
 */
class NameValues extends CoreImplicits {

  private val _keys= mutable.HashMap[String,String]()
  private val _ps= mutable.HashMap[String,Any]()

  def this(key:String , value:Any) {
    this()
    put(key, value)
  }

  def entrySet() = {
    val rc= mutable.HashMap[String, Any]()
    _keys.foreach { (en) =>
      val nm= en._2
      val v= _ps.get( nm )
      rc.put( nm, if (v.isEmpty) null else v.get)
    }
    rc.toMap
  }

  def put(key:String, value:Any) {
    if (!STU.isEmpty(key) && value != null) {
      val k=key.uc
      _keys.put(k, key)
      _ps.put(k, value)
    }
  }

  def get(key:String ) = {
    if (STU.isEmpty(key)) None else { _ps.get(key.uc) }
  }

  def size() = _keys.size

  def contains(key:String) = {
    if (STU.isEmpty(key)) false else _ps.contains( key.uc)
  }

  def toFilterClause(): (String, Seq[Any]) = {
    if (size == 0 ) ("", Nil) else toClause()
  }

  private def toClause(): (String, Seq[Any]) = {
    val r= mutable.ArrayBuffer[Any]()
    val w= (new StringBuilder /: _ps) { (w, en) =>
      if (w.length > 0) { w.append(" AND ") }
      w.append(en._1).append("=?")
      r += en._2
      w
    }
    (w.toString, r.toSeq)
  }

}
