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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


/**
 * Wrapper abstracting a SQL Insert statement.
 *
 * @author kenl
 *
 */
class INSERTStmt(obj:DBRow) extends WritableStmt(obj) {
  iniz()
  private def iniz() {
    val bf= new StringBuilder(1024).append("INSERT INTO ").append(_obj.sqlTable).append(" (")
    val b2= new StringBuilder(512)
    val b1= new StringBuilder(512)

    _obj.values.foreach { (t) =>
      val s= if (isNichts(t._2)) {
        "NULL"
      } else {
        addParams( t._2)
        "?"
      }
      addAndDelim(b1, ",", t._1)
      addAndDelim(b2, ",", s)
    }

    bf.append(b1).append(") VALUES (").append(b2).append(")")
    setSQL(bf.toString )
  }

}

