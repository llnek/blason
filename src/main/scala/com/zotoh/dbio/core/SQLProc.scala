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

import java.sql.{SQLException=>SQLEx, Connection, ResultSet}
import java.util.{Date=>JDate}
import scala.collection.mutable
import org.slf4j._
import com.zotoh.frwk.db.TableMetaHolder
import com.zotoh.dbio.meta.Table
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.DBVendor
import com.zotoh.frwk.db.JDBCUtils._
import org.apache.commons.dbutils.{DbUtils=>DBU}
import java.sql.Statement
import com.zotoh.frwk.util.Nichts
import com.zotoh.frwk.db.JDBCInfo


/**
 * @author kenl
*/
trait SQLProc extends CoreImplicits {

  protected val _meta:MetaCache
  protected val _log:Logger
  def tlog() = _log

  import DBPojo._

  def getDB(): DB
  
  def select[T]( sql:String, params:Any* )(f: ResultSet => T): Seq[T]
  def execute(sql:String, params:Any* ): Int
  def insert( obj:DBPojo): Int
  def delete( obj:DBPojo): Int
  def update( obj:DBPojo, cols:Set[String] ): Int
  def purge(cz:Class[_]) {
    doPurge("delete from " + throwNoTable(cz).table.uc )
  }
  def count(z:Class[_]) = {
    val f = { (rset:ResultSet) =>
      if (rset != null && rset.next()) rset.getInt(1) else 0    
    }
    doCount( "SELECT COUNT(*) FROM " + throwNoTable(z).table().uc , f)
  }
  
  def update( obj:DBPojo): Int = {
    update(obj, Set("*"))
  }

  private def row2Obj[T](z:Class[T], row:ResultSet) = {
    val obj:T = z.getConstructor().newInstance()
    obj
  }

  def findSome[T](cz:Class[T], filter:NameValues): Seq[T] = {
    val s= "SELECT * FROM " + throwNoTable(cz).table().uc
    val wc= filter.toFilterClause
    val cb: ResultSet => T = { row : ResultSet => row2Obj(cz, row) }
    if ( !STU.isEmpty(wc._1) ) {
      select[T]( s + " WHERE " + wc._1, wc._2:_* )(cb)
    } else {
      select[T]( s)(cb)
    }
  }
  def findSome[T](cz:Class[T]): Seq[T] = findSome(cz, new NameValues)
  def findOne[T](cz:Class[T], filter:NameValues): Option[T] = {
    val rc = findSome(cz, filter)
    if (rc.size == 0) None else Option( rc(0) )
  }  
  
  def findAll[T](cz:Class[T]): Seq[T] = {
    findSome(cz, new NameValues )
  }

  protected def doUpdate(obj:DBPojo, cols:Set[String]): Int = {
    val (all,none) = if (cols.size==0) (false,true) else (cols.head=="*",false)
    val lst= mutable.ArrayBuffer[Any]()
    val sb1= new StringBuilder(1024)
    val cz= throwNoCZMeta(obj.getClass)
    if (none) {} else {
      obj.setLastModified(new JDate)
      cz.getFldMetas.values.filter( f => all || f.isInternal || cols.contains(f.getId) ).foreach { (fld) =>
        if ( fld.isPK || fld.isAutoGen) {} else {
          addAndDelim(sb1, ",", fld.getId)
          obj.get(fld.getId) match {
            case Some(Nichts.NICHTS) | None => sb1.append("=NULL")
            case Some(v) =>
              sb1.append("=?")
              lst += v
          }
        }
      }
    }
    if (sb1.length > 0) {
      lst += obj.getRowID
      execute("UPDATE " + cz.getTable.uc + " SET " + sb1 + " WHERE " + COL_ROWID + "=?" , lst:_*)
    }
    else {
      0
    }
  }

  protected def doDelete( obj:DBPojo): Int = {
    val t= throwNoTable(obj.getClass)
    execute( "DELETE FROM " + t.table.uc +
            " WHERE " + COL_ROWID + "=?" , obj.getRowID )
  }

  protected def doInsert(obj:DBPojo): Int = {
    val lst = mutable.ArrayBuffer[Any]()
    val s2 = new StringBuilder(1024)
    val s1= new StringBuilder(1024)
    val cz= throwNoCZMeta(obj.getClass)
    val t= throwNoTable(obj.getClass)

    obj.setLastModified(new JDate)
    cz.getFldMetas.foreach { (en) =>

      if (en._2.isPK ||en._2.isAutoGen ) {} else {
        addAndDelim(s1, ",", en._1)
        if (s2.length > 0) { s2.append(",") }
        obj.get(en._1) match {
          case Some(Nichts.NICHTS) | None => s2.append("NULL")
          case Some(v) =>
            s2.append("?")
            lst += v
        }
      }

    }

    if (s1.length > 0) {
      execute( "INSERT INTO " + t.table.uc + 
        "(" + s1 + ") VALUES (" + s2 + ")" , lst:_* )
    } else {
      0
    }
  }

  private def fetchObjs[T]( target:Class[T], values:NameValues ) = {
    findSome( target, values)
  }

//////////////// assoc-stuff

  private def throwNoCZMeta(z:Class[_]) = {
    _meta.getClassMeta(z) match {
      case Some(x) => x
      case _ =>
      throw new SQLEx("" + z + " has no meta data." )
    }
  }
  private def throwNoTable(z:Class[_]) = {
    val t= Utils.getTable(z)
    if (t==null) { throw new SQLEx("" + z + " has no Table annotation." ) }
    t
  }

  def getO2O[T](lhs:DBPojo, rhs:Class[T], fkey:String): Option[T] = {
    val rc = findSome( rhs, new NameValues(COL_ROWID, lhs.get(fkey)) )    
    if (rc.size == 0) None else Option( rc(0) )
  }

  def setO2O(lhs:DBPojo, rhs:DBPojo, fkey:String) {
    lhs.set(fkey, if (rhs==null) None else Option(rhs.getRowID ) )
  }

  def purgeO2O(lhs:DBPojo, rhs:Class[_], fkey:String) = {
    val t= throwNoTable(rhs)
    val sql="DELETE FROM " + t.table.uc + " WHERE " + COL_ROWID + "=?"
    lhs.get(fkey) match {
      case Some(v) => execute(sql, v)
      case _ => 0
    }
  }

  def getO2M[T](lhs:DBPojo, rhs:Class[T], fkey:String): Seq[T] = {
    findSome( rhs, new NameValues(fkey, lhs.getRowID ))
  }

  def unlinkO2M(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    if (rhs != null) {
      rhs.set(fkey, None)
    }
    0
  }

  def purgeO2M(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    delete(rhs)
  }

  def purgeO2M(lhs:DBPojo, rhs:Class[_], fkey:String) = {
    val t= throwNoTable(rhs)
    val sql="DELETE FROM " + t.table.uc + " WHERE " + fkey + "=?"
    execute(sql, lhs.getRowID )
  }

  def linkO2M(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    rhs.set(fkey, Some(lhs.getRowID) )
    update(rhs, Set(fkey))
  }

  def getM2M[T](lhs:DBPojo, rhs:Class[T]): Seq[T] = {
    val jc= _meta.findJoined(lhs.getClass,rhs)
    val jn= throwNoTable(jc).table.uc
    val rn= Utils.getTable(rhs).table.uc
    val ln= Utils.getTable( lhs.getClass ).table.uc

    val sql = "SELECT DISTINCT RES.* FROM " + rn + " RES JOIN " + jn + " MM  ON " +
    "MM." + COL_LHS + "=? AND " + "MM." + COL_RHS + "=? AND " +
    "MM." + COL_LHSOID + "=? AND " + "MM." + COL_RHSOID + " = RES." + COL_ROWID

    //fetchViaSQL(rhs, sql, ln, rn, lhs.getRowID)
    Nil
  }

  def unlinkM2M(lhs:DBPojo, rhs:DBPojo): Int = {
    val jc= _meta.findJoined(lhs,rhs)
    val jn= throwNoTable(jc).table.uc
    val rn= Utils.getTable(rhs.getClass).table.uc
    val ln= Utils.getTable( lhs.getClass ).table.uc

    val sql ="DELETE FROM " + jn +
            " WHERE " + COL_RHS + "=? AND " + COL_LHS + "=? AND " +
            COL_RHSOID + "=? AND " + COL_LHSOID + "=?"

    execute(sql, rn, ln, rhs.getRowID, lhs.getRowID)
  }

  def purgeM2M(lhs:DBPojo, rhs:Class[_]): Int = {
    val jc= _meta.findJoined(lhs.getClass,rhs)
    val jn= throwNoTable(jc).table.uc
    val rn= Utils.getTable(rhs).table.uc
    val ln= Utils.getTable( lhs.getClass ).table.uc

    val sql ="DELETE FROM " + jn +
            " WHERE " + COL_RHS + "=? AND " + COL_LHS + "=? AND " +
            COL_LHSOID + "=?"

    execute(sql, rn, ln, lhs.getRowID )
  }

  def linkM2M(lhs:DBPojo, rhs:DBPojo): Int = {
    val jc= _meta.findJoined(lhs,rhs)
    val jn= throwNoTable(jc).table.uc
    val rn= Utils.getTable(rhs.getClass).table.uc
    val ln= Utils.getTable( lhs.getClass ).table.uc

    val sql ="INSERT INTO " + jn +
            " ( " + COL_VERID + ", " + COL_RHS + ", " + COL_LHS + ", " +
            COL_RHSOID + ", " + COL_LHSOID + ") VALUES (?,?,?,?,?)"

    execute(sql, 1L, rn, ln, rhs.getRowID, lhs.getRowID )
  }

  private def getMetas( conn:Connection, z:Class[_]) = {
    val tm= getTableMeta(conn, z)
    _meta.getClassMeta(z) match {
      case Some(zm) => (zm,tm)
      case _ =>
        throw new SQLEx("Failed to locate class info: " + z)
    }
  }

  private def getTableMeta( conn:Connection, z:Class[_] ) = {
    val t= z.getAnnotation(classOf[Table] )
    if (t==null) {
      throw new SQLEx("No DBTable-Annotation for class: " + z.getName )
    }
    _meta.getTableMeta(conn,t.table) match {
      case Some(tm) => tm
      case _ =>
      throw new SQLEx("No Table: " + t.table + " in DB.")
    }
  }

  private def lockError(cnt:Int, table:String, rowID:Long ) {
    if (cnt==0) {
       throw new SQLEx("Possible Optimistic lock failure for table: " +
               table + ", rowid= " + rowID)
    }
  }

  private def jiggleSQL( sql:String, ty:Int) = {

      val v:DBVendor = null//v= _pool.getVendor()

      val rc = ty match {
          case 0 => v.tweakDELETE(sql)
          case 1 => v.tweakSELECT(sql)
          case 2 => v.tweakUPDATE(sql)
        case -1 => v.tweakSQL(sql)
      }

      tlog().debug("jggleSQL= {}", rc)
      rc
  }

  protected def doExecute(conn:Connection, sql:String, pms:Any*): Int  = {
        new SQuery(conn, sql, pms.toSeq ).execute()  
  }

  protected def doCount(sql:String, f: ResultSet => Int ): Int
  protected def doPurge(sql:String): Unit
  
}

