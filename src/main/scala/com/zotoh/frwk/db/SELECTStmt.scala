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

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.StrUtils._



object SELECTStmt {
  def simpleSelect(tbl:String) = new SELECTStmt("*", tbl)
}

/**
 * Wrapper on top of  a SQL select statement.
 *
 * @author kenl
 *
 */
class SELECTStmt(s:String) extends SQLStmt(s) {

  private var _select=""

  /**
   * Create Query.
   *
   * @param selects "col-1, col-2" or "*"
   * @param tbl from this table.
   */
  def this(selects:String, tbl:String) {
    this("")
    ctor(selects, tbl, "", "" )
  }

  /**
   * Create Query.
   *
   * @param selects "col-1, col-2" or "*"
   * @param tbl from this table.
   * @param where empty or "col-3=? and col-4=?"
   * @param vals empty or list of 2 values.
   */
  def this(selects:String, tbl:String, where:String ) {
    this("")
    ctor(selects,tbl,where,"" )
  }

  /**
   * Create Query.
   *
   * @param selects "col-1, col-2" or "*"
   * @param tbl from this table.
   * @param where empty or "col-3=? and col-4=?"
   * @param extra empty or " group by col-2 " or  " order by col-1 "
   * @param pms empty or list of 2 values.
   */
  def this(selects:String, tbl:String, where:String, extra:String ) {
    this("")
    ctor(selects,tbl,where,extra )
  }

  private def ctor(selects:String, tbl:String, where:String, extra:String ) {
    _select= selects
    table= tbl
    setWhere("SELECT "+_select+" FROM " + table , where, extra)
  }

  private def setWhere(sql:String, where:String, extra:String) {
    val bd= new StringBuilder(512).append(sql)
    if (! STU.isEmpty(where)) {
      bd.append(" WHERE ").append(where)
    }
    if (! STU.isEmpty(extra)) {
      bd.append(" ").append(extra)
    }
    setSQL(bd.toString )
  }

}

