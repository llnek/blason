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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.sql.SQLException
import java.io.File
import java.io.IOException


/**
 * @author kenl
 *
 */
trait HxxDBSQL {

  private val _app = ""
  private val _db = ""
  private val _furl=""

  /**
   * @param args
   * @return
   */
  protected def start(args:Array[String] ) = {

    var create= false
    var usage=false

    var dbpath=""
    var dbid=""
    var user=""
    var pwd=""
    var sql=""
    var url=""
    var s=""

    args.foreach { (s) =>

      if (s.startsWith("-help")) {
        usage=true
      } else if (s.startsWith("-create:")) {
        dbpath=niceFPath( s.substring(8))
        create=true
      } else if (s.startsWith("-url:")) {
        url=s.substring(5)
        dbpath=niceFPath(url)
      } else if (s.startsWith("-user:")) {
        user= s.substring(6)
      } else if (s.startsWith("-password:")) {
        pwd=s.substring(10)
      } else if (s.startsWith("-sql:")) {
        sql=s.substring(5)
      }

    }

    if (usage) { showUsage(); "" } else {

      if (create) {
        val pos= dbpath.lastIndexOf('/')
        if (pos >= 0 ) {
          dbid= dbpath.substring(pos+1)
          dbpath=dbpath.substring(0,pos)
        }
        url = xxCreateDB(new File(dbpath), dbid,user, pwd)
      }

      if (! STU.isEmpty(sql)) {
        xxLoadSQL(url, user, pwd, new File(sql))
      }

      xxCloseDB(url, user, pwd)
      url
    }
  }

  /**
   * @param fp
   * @param dbid
   * @param user
   * @param pwd
   * @return
   * @throws SQLException
   * @throws IOException
   */
  protected def xxCreateDB(fp:File, dbid:String, user:String, pwd:String ):String

  /**
   * @param url
   * @param user
   * @param pwd
   * @param sql
   * @throws SQLException
   * @throws IOException
   */
  protected def xxLoadSQL(url:String, user:String, pwd:String, sql:File):Unit

  /**
   * @param url
   * @param user
   * @param pwd
   * @throws SQLException
   * @throws IOException
   */
  protected def xxCloseDB(url:String, user:String, pwd:String):Unit

  /**
   * @param app
   * @param args
   * @return
   */
  protected def runMain(app:HxxDBSQL, args:Array[String]) = {
    var rc = -1
    try  {
      app.createDatabase(args)
      rc=0
    } catch {
      case e:Throwable => e.printStackTrace()
    }
    rc
  }

  /**
   * @param args
   * @return
   * @throws Exception
   */
  def createDatabase(args:Array[String]) = start(args)

  private def showUsage()  {
    println("Usage: " + _app + " { -create:<db-dir> | -url:<db-url> } -user:<user> -pwd:<password> -sql:<ddl-file>")
    println("Example:")
    println("create a " + _db + " database>")
    println("  -create:/home/user1/db -user:user1 -pwd:secret")
    println("create a " + _db + " database & load sql>")
    println("  -create:/home/user1/db -user:user1 -pwd:secret -sql:/home/user1/ddl.sql")
    println("load sql only>")
    println("  -url:" + _furl + ":/home/user1/db -user:user1 -pwd:secret -sql:/home/user1/ddl.sql")
  }

}
