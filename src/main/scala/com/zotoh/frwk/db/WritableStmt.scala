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

/**
 * @author kenl
 *
 */
abstract class WritableStmt protected(protected val _obj:DBRow) extends SQLStmt("") {

  protected def dbgData(row:DBRow) {
    if ( ! tlog.isDebugEnabled ) {} else {
      val msg=new StringBuilder().append("DBRow: ").append("###############################################").append("\n")
      row.values.foreach { (t) =>
        msg.append("fld= ").append(t._1).append(",value= ").append(t._2).append("\n")
      }
      msg.append("###############################################")
      tlog.debug("{}", msg)
    }
  }

  dbgData(_obj)
}
