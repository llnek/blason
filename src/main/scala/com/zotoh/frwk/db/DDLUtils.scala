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

import scala.collection.JavaConversions._
import scala.collection.mutable

import java.io.{File,FileOutputStream,InputStream,OutputStream,IOException}
import java.sql.{Connection,SQLException,Statement,DatabaseMetaData}

import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.DBUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.IOUtils


/**
 * Utility functions related to DDL creation/execution via JDBC.
 *
 * @author kenl
 *
 */
object DDLUtils extends Constants with CoreImplicits {

  private val _log= LoggerFactory.getLogger(classOf[DDLUtils])
  def tlog() = _log

  /**
   * Write the ddl resource to output stream.
   *
   * @param resourcePath e.g. "com/acme/ddl.sql"
   * @param out
   * @param cl optional.
   */
  def ddlToStream(out:OutputStream, resourcePath:String,
    cl:ClassLoader = null) {
    if (out != null) {
      out.write( asBytes(rc2Str(resourcePath, "utf-8", cl)))
      out.flush()
    }
  }

  /**
   * Write ddl resource to file.
   *
   * @param fpOut
   * @param resourcePath
   * @param cl optional.
   */
  def ddlToFile(fpOut:File, resourcePath:String, cl:ClassLoader=null) {
    if (fpOut != null) {
      using(new FileOutputStream(fpOut)) { (out) =>
        ddlToStream(out, resourcePath, cl)
      }
    }
  }

  /**
   * Load a ddl from file and run that against a database.
   *
   * @param jp
   * @param fp
   */
  def loadDDL(jp:JdbcInfo, fp:File) {
    if (fp != null) using( open(fp)) { (inp) =>
      loadDDL(jp, inp)
    }
  }

  /**
   * Load ddl from stream and run that against a database.
   *
   * @param jp
   * @param inp
   */
  def loadDDL( jp:JdbcInfo, inp:InputStream) {
    loadDDL(jp, asString( bytes(inp)) )
  }

  /**
   * Load ddl from string and run that against a database.
   *
   * @param jp
   * @param ddl
   */
  def loadDDL(jp:JdbcInfo, ddl:String) {
    if ( ! STU.isEmpty(ddl)) {
      using(mkConnection(jp)) { (con) =>
        loadDDL(con, ddl)
      }
      tlog().debug(ddl)
    }
  }

  private def loadDDL(con:Connection, ddl:String) {
    val oldc= con.getAutoCommit()
    var ee:Throwable=null
    val lines= splitLines(ddl)
    con.setAutoCommit(true)
    try {
      lines.foreach { (line) =>
        STU.strip( STU.trim(line), ";") match {
          case ln if (! STU.isEmpty(ln) && !ln.eqic("go") ) =>
            try {
              using(con.createStatement()) { (stmt) =>
                stmt.executeUpdate(ln)
              }
            } catch {
              case e:SQLException =>
                maybeOK(con.getMetaData().getDatabaseProductName(), e)
              case e:Throwable => throw e
            }
        }
      }
    } catch {
      case e:Throwable => tlog().error("", e); ee=e; throw e
    } finally {
      try { if (ee != null) con.rollback() } catch { case e:Throwable => }
      con.setAutoCommit(oldc)
    }
  }

  private def splitLines(lines:String) = {
    var pos = nsb(lines).indexOf(S_DDLSEP)
    var rc= mutable.ArrayBuffer[String]()
    val w= S_DDLSEP.length
    var ddl=lines
    while (pos >= 0) {
      rc += STU.trim(ddl.substring(0,pos))
      ddl= ddl.substring(pos+w)
      pos= ddl.indexOf(S_DDLSEP)
    }
    STU.trim(ddl) match {
      case s:String if s.length > 0 => rc += s
      case _ =>
    }
    rc.toArray
  }

  private def maybeOK(dbn:String, e:SQLException) = {

    val db= nsb(dbn).lc
    val oracle=db.has("oracle")
    val db2=db.has("db2")
    val derby=db.has("derby")

    if ( ! (oracle || db2 || derby)) { throw e }

    val ee=e.getCause() match {
      case x:SQLException => x
      case x:Throwable => e
    }

    ee.getErrorCode match {
      case ec if (oracle && (942==ec || 1418==ec || 2289==ec || 0==ec)) => true
      case ec if (db2 && (-204==ec)) => true
      case ec if (derby && (30000==ec)) => true
      case _ => throw e
    }
  }

}

sealed class DDLUtils {}

