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
package db

import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}

import com.zotoh.frwk.util.CoreImplicits
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

object DBRow {
  private val _log = LoggerFactory.getLogger(classOf[DBRow])
}

/**
 * Wrapper for a row of SQL table data.
 *
 * @author kenl
 *
 */

@SerialVersionUID(-1112175967176488069L)
class DBRow(private var _tbl:String="") extends Constants with CoreImplicits with Serializable {

  private val _map= mutable.HashMap[String,Any]()
  import DBRow._
  def tlog() = _log

  _tbl= nsb(_tbl)

  /**
   * Add col & data to the row.
   *
   * @param  nameVals
   */
  def add(colVals:Map[String,Any]):this.type = {
    colVals.foreach { (t) => add( t._1, t._2) }
    this
  }

  /**
   * Add a column & value.
   *
   * @param col
   * @param value
   */
  def add(col:String, value:Any):this.type = {
    if (! STU.isEmpty(col)) {
      _map += col.uc -> nilToNichts(value)
    }
    this
  }

  /**
   * Add a column with NULL value.
   *
   * @param col
   */
  def add(col:String):this.type =  add(col, null)

  /**
   * @return
   */
  def isEmpty() = _map.size == 0

  /**
   *
   * @param col
   * @return
   */
  def remove(col:String) = {
    if (col==null) None else _map.remove(col.uc)
  }

  /**
   * @param col
   * @return
   */
  def exists(col:String) = {
    if (col==null) false else _map.isDefinedAt( col.uc)
  }

  /**
   * @return Table name.
   */
  def sqlTable = _tbl

  /**
   *
   */
  def clear = { _map.clear() ; this }

  /**
   * @return immutable map
   */
  def values = _map.toMap

  /**
   * Get value of column, if column is DB-NULL, returns null.
   *
   * @param col
   * @return
   */
  def get(col:String) = {
    if (col==null) None else _map.get(col.uc)
  }

  def dbg() {
    if ( tlog.isDebugEnabled ) {
      val bf = new StringBuilder(1024)
      _map.foreach { (t) =>
        if (bf.length() > 0) { bf.append("\n") }
        bf.append( t._1 ).
        append("=\"").
        append(nsb( t._2 )).
        append( "\"" )
      }
      tlog.debug(bf.toString )
    }
  }

  /**
   * Constructor.
   *
   * @param bagOfNameValues columns & values.
   */
  def this(bagOfNameValues:Map[String,Any]) {
    this()
    add(bagOfNameValues)
  }

  /**
   * @return
   */
  def size() = _map.size

}

