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

import java.util.{Properties=>JPS}
import java.io.File
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.db.JDBCInfo
import com.zotoh.frwk.db.HxxDB
import com.zotoh.frwk.db.H2DB


/**
 * @author kenl
 *
 */
object SampleApp {
  private val _log= LoggerFactory.getLogger(classOf[SampleApp] )
  private var _db:DB= null

  def main(args:Array[String]) {
    try {
      if ( !parseArgs(args)) {
        usage()
      } else {
        start(args)
      }
    } catch {
      case e:Throwable => e.printStackTrace()
    }
  }

  private def parseArgs(args:Array[String]) = {
    val m= Set("all","crud","sql","o2o","o2m","m2m","ddl")
    val opt= if (args.length > 0) args(0) else ""
    m.contains(opt)
  }

  private def usage() {
    println("Usage: SampleApp < all | sql | crud | 020 | o2m | m2m >")
    println("options:")
    println("all - all the demos.")
    println("sql - basic sql operations demo.")
    println("crud - CRUD demo.")
    println("o2o - one 2 one association demo.")
    println("o2m - one 2 many association demo.")
    println("m2m - many 2 many association demo.")
  }

  private def start(args:Array[String]) {

    _log.info("Starting sample application") 

    val dbdir= if (args.length > 1) new File(args(1)) else genTmpDir()
    initialize(dbdir)

    val all = "all" == args(0)
    val proc= new CompositeSQLr(_db)

    new BasicGenSQLDemo(proc).start()
    // load DDL only ?

    if (args(0) == "ddl") {} else {
      if (all || "sql"==args(0)) { new BasicSQLDemo( proc).start() }
//      if ( all || "crud"== args(0)) { new BasicCRUDDemo(io).start() }
//      if (all || "o2o"== args(0)) { new BasicO2ODemo(io).start() }
//      if ( all || "o2m" == args(0)) { new BasicO2MDemo(io).start() }
//      if ( all || "m2m" == args(0)) { new BasicM2MDemo(io).start() }
    }
    _log.info("Done.") 
  }

  private def initialize(dbdir:File ) {
    val schema= new Schema {
      def getModels = List(
        classOf[Address],
        classOf[Company],
        classOf[Department],
        classOf[Employee],
        classOf[Person],
        classOf[DeptEmp]
      )
    }
    val dbid= uid().substring(0,6)
    val path = niceFPath( dbdir)
    val url= "jdbc:h2:"+ path +"/data/"+dbid
    H2DB.dropDB(url)
    val ji= new JDBCInfo("sa","", url, "org.h2.Driver")
    _db = ScalaDB( new SimpleDBFactory )(ji, schema, new JPS() )
  }





}

sealed class SampleApp {}
