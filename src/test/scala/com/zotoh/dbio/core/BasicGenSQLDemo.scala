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

import com.zotoh.frwk.db.DBVendor
import com.zotoh.dbio.sql.GenSQLFile
import com.zotoh.frwk.db.DDLUtils


/**
 * @author kenl
 *
 */
class BasicGenSQLDemo(proc:CompositeSQLr) extends Demo(proc) {

  def run() {
    val s= new Schema {
      def getModels = List(
          classOf[Person],
          classOf[Employee],
          classOf[Company],
          classOf[Department],
          classOf[Address],
          classOf[DeptEmp]
        )
    }
    val ddl= GenSQLFile.genDDL(DBVendor.H2, s)
    DDLUtils.loadDDL( _db.getDB.getInfo , ddl)
  }

}
