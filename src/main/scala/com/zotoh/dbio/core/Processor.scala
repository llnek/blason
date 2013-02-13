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

package com.zotoh.dbio
package core

import java.sql.{SQLException, Connection, ResultSet}
import scala.collection.mutable
import org.slf4j._


/**
 * @author kenl
*/
sealed trait SQLBlockType
object SQLComplexType extends SQLBlockType
object SQLSimpleType extends SQLBlockType

/**
 * @author kenl
*/
trait SQLProcessor {

  type DMap = Map[String, Any]

  protected val _log:Logger
  def tlog() = _log

  def select[X]( sql: String, params: Seq[Any])(f: ResultSet => X): Seq[X]
  def select[X]( sql: String)(f: ResultSet => X): Seq[X]

  def execute(sql: String, params: Seq[Any]): Int
  def execute(sql: String): Int

  def insert( obj : SRecord): Int
  def delete( obj : SRecord): Int

  def update( obj : SRecord, cols : Set[String] ): Int
  def update( obj : SRecord): Int = {
    update(obj, obj.getSchemaFactory.getUpdatableCols )
  }

  def findSome(fac : SRecordFactory, filter : DMap): Seq[SRecord]
  def findAll(fac : SRecordFactory) : Seq[SRecord]


  protected def doFindSome(fac : SRecordFactory, filter : DMap): Seq[SRecord] = {
    val s= "SELECT * FROM " + fac.getTableName
    val lst = mutable.ArrayBuffer[Any]()
    val wb= new StringBuilder(512)
    filter.foreach { kv =>
      if (wb.length > 0) { wb.append(" AND ") }
      wb.append(kv._1)
      kv._2 match {
        case null | NullAny => wb.append("=NULL")
        case _ => 
          wb.append("=?")
          lst += kv._2
      }
    }
    val cb : ResultSet => SRecord = { row : ResultSet => fac.create(row)  }
    if (wb.length > 0) {
      select( s + " WHERE " + wb, lst.toSeq )(cb)
    } else {
      select( s)(cb)
    }
  }

  protected def doFindAll(fac : SRecordFactory): Seq[SRecord] = {
    val cb : ResultSet => SRecord = { row : ResultSet => fac.create(row)  }
    select("SELECT * FROM " + fac.getTableName)(cb)
  }

  protected def pkeys(obj : SRecord): (String, Seq[Any]) = {
    val lst = mutable.ArrayBuffer[Any]()
    val sb1= new StringBuilder(512)
    val sf= obj.getSchemaFactory
    sf.getPrimaryKeys.foreach { k =>
      if (sb1.length > 0) { sb1.append(" AND ") }
      sb1.append(k).append("=?")
      obj.getVal(k) match {
        case Some(NullAny) => throw new SQLException("Primary key has NULL value")
        case null | None => throw new SQLException("Primary key has no value")
        case v => lst += v.get
      }
    }

    (sb1.toString, lst.toSeq)
  }

  protected def doUpdate(obj : SRecord, cols : Set[String]): Int = {
    val lst= mutable.ArrayBuffer[Any]()
    val sb1= new StringBuilder(1024)
    val sf = obj.getSchemaFactory
    val pks= pkeys(obj)
    cols.foreach { k =>
      if (sb1.length > 0) { sb1.append(",") }
      sb1.append(k)
      obj.getVal(k) match {
        case null | None => sb1.append("=NULL")
        case v =>
          sb1.append("=?")
          lst += v.get
      }
    }
    if (sb1.length > 0) {
      lst.appendAll(pks._2)
      execute("UPDATE " + sf.getTableName + " SET " + sb1 + " WHERE " + pks._1 , lst.toSeq)
    }
    else {
      0
    }
  }

  protected def doDelete(c : Connection, obj : SRecord): Int = {
    val pks = pkeys(obj)
    if (pks._1.length > 0) {
      execute( "DELETE FROM " + obj.getSchemaFactory.getTableName +
            " WHERE " + pks._1 , pks._2.toSeq )
    } else {
      0
    }
  }

  protected def doInsert(c : Connection, obj: SRecord): Int = {
    val lst = mutable.ArrayBuffer[Any]()
    val s2 = new StringBuilder(1024)
    val s1= new StringBuilder(1024)
    obj.getSchemaFactory.getCreationCols.foreach { k =>
      if (s1.length > 0) { s1.append(",")}
      s1.append(k)
      if (s2.length > 0) { s2.append(",")}
      obj.getVal(k) match {
        case null | None => s2.append("NULL")
        case v@Some(_) =>
          s2.append("?")
          lst += v.get
      }
    }
    if (s1.length > 0) {
      execute( "INSERT INTO " + obj.getSchemaFactory.getTableName + "(" + s1 + ") VALUES (" + s2 + ")" , lst.toSeq )
    } else {
      0
    }
  }

}

/**
 * @author kenl
*/
class SimpleSQLr(private val _db: DB) extends SQLProcessor {
  val _log= LoggerFactory.getLogger(classOf[SimpleSQLr])
  
  def findSome(fac : SRecordFactory, filter : DMap): Seq[SRecord] = {
    doFindSome(fac,filter)
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
    doFindAll(fac)
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
    doUpdate(obj, cols)
  }

  def delete(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      doDelete(c, obj)
    }
    finally {
      _db.close(c)
    }
  }

  def insert(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      doInsert(c, obj)
    }
    finally {
      _db.close(c)
    }
  }

  def select[X]( sql: String, params: Seq[Any])(f: ResultSet => X): Seq[X] = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      new SQuery(c, sql, params).select(f)
    }
    finally {
      _db.close(c)
    }
  }

  def select[X]( sql: String)(f: ResultSet => X): Seq[X] = {
    select(sql, Nil)(f)
  }

  def execute( sql: String, params: Seq[Any]): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      new SQuery(c, sql, params).execute()
    }
    finally {
      _db.close(c)
    }
  }

  def execute( sql: String): Int = {
    execute(sql, Nil)
  }

}


/**
 * @author kenl
 */
class CompositeSQLr(private val _db : DB) {

  def execWith[X](f: Transaction => X) = {
    val c= begin
    try {
      f ( new Transaction(c) )
      commit(c)
    } catch {
      case e: Throwable => { rollback(c) ; e.printStackTrace }
    } finally {
      close(c)
    }
  }

  private def rollback(c :Connection) {
    try { c.rollback() } catch { case e:Throwable => }
  }

  private def commit(c : Connection) {
    c.commit()
  }

  private def begin(): Connection = {
    val c= _db.open
    c.setAutoCommit(false)
    c
  }

  private def close(c: Connection) {
    try { c.close() } catch { case e:Throwable => }
  }

}

/**
 * @author kenl
 */
class Transaction(private val _conn : Connection ) extends SQLProcessor {

  val _log= LoggerFactory.getLogger(classOf[Transaction])

  def insert(obj : SRecord): Int = {
    doInsert(_conn, obj)
  }

  def select[X]( sql: String, params: Seq[Any])(f: ResultSet => X): Seq[X] = {
    new SQuery(_conn, sql, params ).select(f)
  }

  def select[X]( sql: String)(f: ResultSet => X): Seq[X] = {
    select(sql, Nil) (f)
  }

  def execute( sql: String, params: Seq[Any]): Int = {
    new SQuery(_conn, sql, params ).execute()
  }

  def execute( sql: String): Int = {
    execute(sql, Nil)
  }

  def delete( obj : SRecord): Int = {
    doDelete(_conn, obj)
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
    doUpdate(obj, cols)
  }

  def findSome(fac : SRecordFactory, filter : DMap): Seq[SRecord] = {
    doFindSome(fac,filter)
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
    doFindAll(fac)
  }

}

