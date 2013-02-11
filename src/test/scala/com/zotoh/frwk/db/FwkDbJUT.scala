/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUtils IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUtilsD IN THE HOPE THAT IT WILL BE USEFUL,
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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._

import org.apache.commons.io.{FileUtils=>FUS}
import org.apache.commons.io.{IOUtils=>IOU}
import java.util.concurrent.Callable


import java.io.{ByteArrayOutputStream=>ByteArrayOS}
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

import com.zotoh.frwk.util.FileUtils

import org.scalatest.Assertions._
import org.scalatest._

object FwkDbJUT {
  private val LJDBC = new TLocalJDBC()
  def getJDBC = LJDBC
}

class FwkDbJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with Constants {
  import FwkDbJUT._
  
  override def beforeAll(configMap: Map[String, Any]) {}

  override def afterAll(configMap: Map[String, Any]) {
    FUS.cleanDirectory( new File(_dbDir))
  }

  override def beforeEach() {}

  override def afterEach() {}

  private val DATABASE_URL = "jdbc:h2:mem:account"
  private val SQL="""
CREATE CACHED TABLE star (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
firstname VARCHAR(20),
lastname VARCHAR(20))
-- :
INSERT INTO star (id, firstname, lastname) VALUES (DEFAULT, 'Felix', 'the Cat')
"""

  private val _dbDir=niceFPath( genTmpDir())
  private val _dbID="test007"
  private val _user="zeus"
  private val _pwd="zeus123"
    
  test("testSingularJDBC") {

    val dbUrl= H2DB.mkMemDB(_dbID, _user, _pwd)
    val j= new JDBCInfo(_user, _pwd, dbUrl, H2_DRIVER)

    if (!DBUtils.tableExists(j, "user_accounts")) {
      DDLUtils.loadDDL(j, getDDLStr())
    }

    assert( DBUtils.tableExists(j, "user_accounts"))

    LJDBC.set(new TLocalDBIO(j))
    
    var jj= LJDBC.get.getPool.newJdbc()
    var m= jj.getTableMetaData("user_accounts")
    expectResult(m.size)(14)
    expectResult(0)( jj.countRows("user_accounts"))

    // insert
    var r= new DBRow("user_accounts")
    r.add("user_id", "id1")
    expectResult( 1)( jj.insertOneRow(r))

    // update
    r.clear
    r.add("user_role", "admin")
    expectResult(1)( jj.updateOneRow(r, "user_id=?", "id1" ))

    // select
    var s = SELECTStmt.simpleSelect("user_accounts")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==14)

    s= new SELECTStmt("select * from user_accounts where user_id=?").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert( r != null && r.size()==14)

    s= new SELECTStmt("select * from user_accounts where user_id='id1' ")
    r=jj.fetchOneRow(s).get
    assert(r!=null && r.size()==14)

    s= new SELECTStmt("user_role", "user_accounts")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==1)

    s= new SELECTStmt("user_role", "user_accounts", "user_id=?").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==1)

    s= new SELECTStmt("user_role,user_id", "user_accounts", 
        "user_id=?", "order by user_role").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==2)

    assert(jj.existRows("user_accounts"))
    assert(jj.existRows("user_accounts", "user_id=?", "id1" ))

    expectResult(1)(jj.countRows("user_accounts" ))
    expectResult(1)(jj.countRows("user_accounts", "user_id=?", "id1"))

    // delete
    var d= DELETEStmt.simpleDelete("user_accounts")
    expectResult(1)(jj.deleteRows(d))

    d=new DELETEStmt("delete from user_accounts where user_id=?").addParams("id1")
    expectResult(0)(jj.deleteRows(d))

    d=new DELETEStmt("delete from user_accounts where user_id='id1' ")
    expectResult(0)(jj.deleteRows(d))

    d= new DELETEStmt("user_accounts", "user_id=?").setParams("id1")
    expectResult(0)(jj.deleteRows(d))

    d= new DELETEStmt("user_accounts", "user_id='id1' ")
    expectResult(0)(jj.deleteRows(d))

    assert(! jj.existRows("user_accounts"))
    
    LJDBC.get.finz
  }


  test("testCreateH2DB") {
    val path= _dbDir+ "/"+ _dbID
    H2DBSQL.createDatabase( Array(
        "-create:"+path,
        "-user:" + _user,
        "-pwd:" + _pwd
    ))
    assert(H2DB.existsDB(path))
  }

  test("testCreateH2WithSQL") {
    val f= new File( _dbDir+"/" + uid() + ".sql")
    writeFile(f, SQL)
    val path= _dbDir+ "/"+ _dbID
    try {
      H2DBSQL.createDatabase( Array(
          "-create:"+ path,
          "-user:" + _user,
          "-pwd:"+ _pwd,
          "-sql:"+ f.getCanonicalPath()
      ))
    } finally {
      f.delete()
    }
    assert(H2DB.existsDB(path))
  }

  test("testLoadH2WithSQL") {
    val f= new File( _dbDir+"/" + uid() + ".sql")
    writeFile(f, SQL)
    val path= _dbDir+ "/"+ _dbID

    try {
      val url = H2DBSQL.createDatabase( Array(
          "-create:"+path,
          "-user:"+_user,
          "-pwd:"+_pwd
      ))
      H2DBSQL.createDatabase( Array(
          "-url:"+url,
          "-user:"+_user,
          "-pwd:"+_pwd,
          "-sql:"+f.getCanonicalPath()
      ))
    } finally {
      f.delete()
    }
    assert(H2DB.existsDB(path))
  }

  test("testDBRow") {
    val m= mutable.HashMap[String,Any]()
    var r = new DBRow("table1")
    expectResult(r.sqlTable)( "table1")
    m += "c1" -> 25
    r= new DBRow("").add(m.toMap)
    assert(r.exists("c1"))
    expectResult(r.get("c1").get)( 25)
    r= new DBRow("t1")
    m += "c1" -> 33
    r.add(m.toMap)
    expectResult(r.get("c1").get)( 33)
    expectResult(r.values.size)( r.size())
    r.clear
    assert(r.isEmpty())
    m += "c1" -> 99
    r= new DBRow("").add(m.toMap)
    expectResult(r.remove("c1").get)( 99)
    expectResult(r.size())(0)
  }

  test("testMemDB") {
    val url= H2DB.mkMemDB(_dbID, _user, _pwd)
    assert( url != null && url.length() > 0)
  }

  test("testLoadDDL") {
    val url= H2DB.mkMemDB("aaa", _user, _pwd)
    try {
      DDLUtils.loadDDL( new JDBCInfo(_user, _pwd, url), getDDLStr())
      assert(true)
    } catch {
      case e:Throwable => assert(false, "loadddl failed")
    }
    // no errors assume works :)
  }

  private def getDDLStr() = {
    val baos= new ByteArrayOS()
    DDLUtils.ddlToStream(baos, "com/zotoh/frwk/db/ddl.sql")
    asString(baos.toByteArray() )
  }

  test("testJDBC") {

    val dbUrl= H2DB.mkMemDB(_dbID, _user, _pwd)
    val j= new JDBCInfo(_user, _pwd, dbUrl, H2_DRIVER)

    if (!DBUtils.tableExists(j, "user_accounts")) {
      DDLUtils.loadDDL(j, getDDLStr())
    }

    assert( DBUtils.tableExists(j, "user_accounts"))

    val pm= new JDBCPoolMgr()
    assert( !pm.existsPool("x"))
    var pp= pm.mkPool("x", j)
    assert(pp != null)
    assert(pm.existsPool("x"))
    pp= pm.getPool("x").get
    assert(pp != null)

    var jj= pp.newJdbc()
    var m= jj.getTableMetaData("user_accounts")
    expectResult(m.size)(14)
    expectResult(0)( jj.countRows("user_accounts"))

    // insert
    var r= new DBRow("user_accounts")
    r.add("user_id", "id1")
    expectResult( 1)( jj.insertOneRow(r))

    // update
    r.clear
    r.add("user_role", "admin")
    expectResult(1)( jj.updateOneRow(r, "user_id=?", "id1" ))

    // select
    var s = SELECTStmt.simpleSelect("user_accounts")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==14)

    s= new SELECTStmt("select * from user_accounts where user_id=?").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert( r != null && r.size()==14)

    s= new SELECTStmt("select * from user_accounts where user_id='id1' ")
    r=jj.fetchOneRow(s).get
    assert(r!=null && r.size()==14)

    s= new SELECTStmt("user_role", "user_accounts")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==1)

    s= new SELECTStmt("user_role", "user_accounts", "user_id=?").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==1)

    s= new SELECTStmt("user_role,user_id", "user_accounts", 
        "user_id=?", "order by user_role").setParams("id1")
    r=jj.fetchOneRow(s).get
    assert(r != null && r.size()==2)

    assert(jj.existRows("user_accounts"))
    assert(jj.existRows("user_accounts", "user_id=?", "id1" ))

    expectResult(1)(jj.countRows("user_accounts" ))
    expectResult(1)(jj.countRows("user_accounts", "user_id=?", "id1"))

    // delete
    var d= DELETEStmt.simpleDelete("user_accounts")
    expectResult(1)(jj.deleteRows(d))

    d=new DELETEStmt("delete from user_accounts where user_id=?").addParams("id1")
    expectResult(0)(jj.deleteRows(d))

    d=new DELETEStmt("delete from user_accounts where user_id='id1' ")
    expectResult(0)(jj.deleteRows(d))

    d= new DELETEStmt("user_accounts", "user_id=?").setParams("id1")
    expectResult(0)(jj.deleteRows(d))

    d= new DELETEStmt("user_accounts", "user_id='id1' ")
    expectResult(0)(jj.deleteRows(d))

    assert(! jj.existRows("user_accounts"))
  }

  test("testUtils") {
    val jp= new JDBCInfo(_user, _pwd, "jdbc:h2:mem:xxx;DB_CLOSE_DELAY=-1", H2_DRIVER)
    using(DBUtils.mkConnection(jp)) { (c) =>
      assert(c != null)
      assert( !DBUtils.tableExists(jp, "xyz"))
      using(c.createStatement()) { (stmt) =>
        try {
          stmt.executeUpdate("create table xyz ( fname varchar(255))")
        } catch {
          case e:Throwable => assert(false, "load-ddl-failed")
        }
      }
    }

    try {DBUtils.testConnection(jp) } catch {
      case e:Throwable => assert(false,"unexpectResulted error")
    }

    var v=DBUtils.vendor(jp)
    expectResult(v)( DBVendor.H2)
    assert(DBUtils.loadDriver(H2_DRIVER) != null)
    assert(DBUtils.tableExists(jp, "xyz"))
    assert(! DBUtils.rowExists(jp, "xyz"))

    using(DBUtils.mkConnection(jp)) { (c) =>
      using(c.createStatement()) { (stmt) =>
        try {
          stmt.executeUpdate("insert into xyz values('jerry')")
        } catch {
          case e:Throwable => assert(false,"load-ddl-failed")
        }
      }
    }

    assert(DBUtils.rowExists(jp, "xyz"))
    expectResult( DBUtils.firstRow(jp, "select * from xyz").get.size())(1)
  }

  test("testTransaction") {
    var jp= new JDBCInfo(_user, _pwd, "jdbc:h2:mem:zzz;DB_CLOSE_DELAY=-1", H2_DRIVER)
    using(DBUtils.mkConnection(jp)) { (c) =>
      assert(c != null)
      using(c.createStatement()) { (stmt) =>
        try {
          stmt.executeUpdate("create table xyz ( fname varchar(255))")
        } catch {
          case e:Throwable => assert(false,"load-ddl-failed")
        }
      }
    }
    val pm= new JDBCPoolMgr()
    val p=pm.mkPool(jp)
    val j= p.newJdbc()
    var r= new DBRow("xyz")
    var jc= j.beginTX()
    r.add("fname", "jerry")
    j.insertOneRow(jc, r)
    j.cancelTX(jc)
    j.closeTX(jc)
    assert(!DBUtils.rowExists(jp, "xyz"))

    jc= j.beginTX()
    r.add("fname", "jerry")
    j.insertOneRow(jc, r)
    j.commitTX(jc)
    j.closeTX(jc)
    assert(DBUtils.rowExists(jp, "xyz"))

  }

  
}
