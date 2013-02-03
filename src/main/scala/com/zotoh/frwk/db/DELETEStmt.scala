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

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


object DELETEStmt {

  def simpleDelete(tbl:String) = new DELETEStmt(tbl,"")

}

/**
 *
 * @author kenl
 *
 */
class DELETEStmt(s:String) extends SQLStmt(s) with CoreImplicits {

  /**
   * Create a delete stmt and construct the sql inside based on the parameters
   * provided.
   *
   * @param table e.g. XYZ
   * @param where e.g. name=? and age=?
   */
  def this(table:String, where:String) {
    this("")
    setWhere(table,where)
  }

  private def setWhere(tbl:String, where:String) {
    setSQL( "DELETE FROM " + tbl + (
      if (STU.isEmpty(where)) "" else { " WHERE " + where }
    ) )
  }

}

