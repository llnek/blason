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

import java.sql.SQLException
import java.io.File
import java.io.IOException


/**
 * @author kenl
 *
 */
object H2DBSQL extends HxxDBSQL {

  val _furl="jdbc:h2"
  val _app="h2db"
  val _db="h2"

  import H2DB._

  /**
   * @param args
   */
  def main(args:Array[String] )  {
    sys.exit( runMain(H2DBSQL, args))
  }

  def xxCreateDB(dbFileDir:File, dbid:String, user:String, pwd:String) = {
    mkDB(dbFileDir, dbid, user, pwd)
  }

  def xxLoadSQL(dbUrl:String, user:String, pwd:String, sql:File) {
    loadSQL(dbUrl, user, pwd, sql)
  }

  def xxCloseDB(dbUrl:String, user:String, pwd:String) {
    closeDB(dbUrl, user, pwd)
  }



}
