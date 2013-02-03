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
import scala.math._

import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.StrUtils._

object SQLStmt {
  private val _log= LoggerFactory.getLogger(classOf[SQLStmt])
}

/**
 * Abstract a SQL statement.
 *
 * @author kenl
 *
 */
abstract class SQLStmt protected(sql:String="") extends CoreImplicits {
  protected val _values= mutable.ArrayBuffer[Any]()
  private var _tbl=""
  private var _sql=""
  import SQLStmt._
  setSQL(sql)

  override def toString() =  _sql

  def tlog() = _log

  def setParams( pms:Any*):this.type = {
    _values.clear()
    addParams(pms:_*)
    this
  }

  def params() = _values.toSeq

  /**
   * @param sql
   */
  protected def setSQL(sql:String) {
    _sql= nsb(sql)
    _tbl=""
    table
  }

  /**
   * @param pms
   */
  def addParams(pms:Any*):this.type = {
    pms.foreach{ (a) => _values += a }
    this
  }

  def table_=(tbl:String) { _tbl= nsb(tbl) }

  def table = {
    if ( STU.isEmpty(_tbl)) {
      val sql= toString()
      var s= sql.lc
      var pos= s.indexOf("from")
      if (pos > 0) {
        s= sql.substring(pos+4).trim()
        val b= s.indexOf('\t')
        val a= s.indexOf(' ')
        if (b < 0) { pos = a }
        else
        if (a < 0) { pos = b }
        else {
          pos= min(a, b)
        }
      }
      if (pos > 0) {
        _tbl= s.substring(0,pos)
      }
    }
    _tbl
  }

}

