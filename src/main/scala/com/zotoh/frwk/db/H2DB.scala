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
import org.apache.commons.io.{FileUtils=>FUS}
import org.slf4j._
import java.io.File
import java.sql.SQLException
import java.sql.Statement



/**
 * Utility functions relating to the management of a H2 instance.
 *
 * @author kenl
 *
 */
object H2DB extends HxxDB {

  val _log= LoggerFactory.getLogger(classOf[H2DB])

  override def getMemSfx() = ";DB_CLOSE_DELAY=-1"

  def getEmbeddedPfx() = H2_FILE_URL

  def getMemPfx() = H2_MEM_URL

  def onTestDB(dbPath:String) = {
    if(STU.isEmpty(dbPath)) false else new File(dbPath + ".h2.db").exists()
  }

  def onDropDB(dbPath:String)  = {
    val f= new File(dbPath+".h2.db")
    val rc=f.exists()
    FUS.deleteQuietly( new File(dbPath+".h2.lock"))
    FUS.deleteQuietly(f)
    rc
  }

  def onCreateDB(s:Statement) {
    if (s != null) { s.execute("SET DEFAULT_TABLE_TYPE CACHED") }
  }

}

sealed class H2DB {}

