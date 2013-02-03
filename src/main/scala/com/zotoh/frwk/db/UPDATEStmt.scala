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

import com.zotoh.frwk.security.Password
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


/**
 * Wrapper on top of a SQL update statement.
 *
 * @author kenl
 *
 */
class UPDATEStmt(obj:DBRow, where:String, pms:Any*) extends WritableStmt(obj) {

  val bf= new StringBuilder(512).append("UPDATE ").append( _obj.sqlTable ).append(" SET ")
  val b1= new StringBuilder(512)

  _obj.values.foreach { (t) =>
    addAndDelim(b1, " , ", t._1)
    val v= t._2 match {
      case pwd:Password => pwd.encoded()
      case a => a
    }
    if (isNichts(v)) b1.append("=NULL") else {
      b1.append("=?")
      addParams(v)
    }
  }

  bf.append(b1)

  if (! STU.isEmpty(where)) {
    bf.append(" WHERE ").append(where)
    // extra params for where clause
    addParams(pms:_*)
  }

  setSQL(bf.toString )
}
