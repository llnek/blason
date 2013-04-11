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
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.DBVendor
import com.zotoh.frwk.db.JDBCUtils._
import org.apache.commons.dbutils.{DbUtils=>DBU}
import java.sql.Statement
import com.zotoh.frwk.util.Nichts
import com.zotoh.frwk.db.JDBCInfo
import java.sql.Blob
import java.sql.Clob
import java.io.InputStream
import java.io.Reader
import com.zotoh.frwk.io.IOUtils
import java.util.GregorianCalendar
import java.util.TimeZone


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
  def executeWithOutput( sql: String, params:Any* ): (Int, Seq[Any])
  def execute(sql:String, params:Any* ): Int
  def insert( obj:DBPojo): Int
  def delete( obj:DBPojo): Int
  def update( obj:DBPojo, cols:Set[String] ): Int
  def purge(cz:Class[_]) {
    doPurge("delete from " + throwNoTable(cz).table.uc )
  }
  def count(z:Class[_]) = {
    val f = { (rset:ResultSet) =>      if (rset != null )  rset.getInt(1)   else 0    }
    doCount( "SELECT COUNT(*) FROM " + throwNoTable(z).table().uc , f)    
  }

  def update( obj:DBPojo): Int = {
    update(obj, Set("*"))
  }

  private def row2Obj[T <: DBPojo](z:Class[T], res:ResultSet) = {
    val obj:T = z.getConstructor().newInstance()
    buildOneRow(z,obj, res)
    obj
  }

  def findSome[T <: DBPojo ](cz:Class[T], filter:NameValues, orderby:String = ""): Seq[T] = {
    val s= "SELECT * FROM " + throwNoTable(cz).table().uc
    val wc= filter.toFilterClause
    val cb: ResultSet => T = { row : ResultSet => row2Obj(cz, row) }
    val extra= if (hgl(orderby)) { " ORDER BY " + orderby } else ""
    if ( !STU.isEmpty(wc._1) ) {
      select[T]( s + " WHERE " + wc._1 + extra, wc._2:_* )(cb)
    } else {
      select[T]( s + extra )(cb)
    }
  }

  def findSome[T <: DBPojo ](cz:Class[T], orderby:String = ""): Seq[T] = findSome(cz, new NameValues, orderby)
  def findOne[T <: DBPojo ](cz:Class[T], filter:NameValues): Option[T] = {
    val rc = findSome(cz, filter, "")
    if (rc.size == 0) None else Option( rc(0) )
  }

  def findAll[T <: DBPojo ](cz:Class[T], orderby:String = ""): Seq[T] = {
    findSome(cz, new NameValues, orderby )
  }

  def findViaSQL[T <: DBPojo ](cz:Class[T], sql:String, params:Any* ): Seq[T] = {
    val cb: ResultSet => T = { row : ResultSet => row2Obj(cz, row) }
    select(sql,params:_*)(cb)
  }

  protected def doUpdate(pojo:DBPojo, updates:Set[String]): Int = {
    val (all,none) = if (updates.size==0) (false,true) else (updates.head=="*",false)
    if (none) { return 0 }
    val obj= pojo.asInstanceOf[AbstractModel]
    val ds= obj.getDirtyFields
    val lst= mutable.ArrayBuffer[Any]()
    val sb1= new StringBuilder(1024)
    val cz= throwNoCZMeta(obj.getClass)
    val flds= cz.getFldMetas
    val cver= pojo.getVerID
    val nver= cver+1
    val lock= getDB().supportsOptimisticLock()
    val cols= updates.map( _.toUpperCase )

    obj.setLastModified(nowJTS )
    ds.filter( all || cols.contains(_) ).foreach { (dn) =>
      val go= flds.get(dn) match {
        case Some(fld) =>
          if ( ! fld.isUpdatable|| fld.isAutoGen ) false else true
        case _ => true
      }
      if (go) {
        addAndDelim(sb1, ",", dn)
        obj.get(dn) match {
          case Some(Nichts.NICHTS) | None => sb1.append("=NULL")
          case Some(v) =>
            sb1.append("=?")
            lst += v
        }
      }
    }

    if (sb1.length > 0) {
      addAndDelim(sb1, ",", obj.dbio_getLastModified_column.uc).append("=?")
      lst += obj.getLastModified
      if (lock) {
        addAndDelim(sb1, ",", COL_VERID).append("=?")
        lst += nver
      }
      // for where clause
      lst += obj.getRowID
      if (lock) { lst += cver }
      val tbl= cz.getTable.uc
      val cnt= execute("UPDATE " + tbl + " SET " + sb1 + " WHERE " +
        fmtUpdateWhere(lock) , lst:_*)
      if (lock) {
        lockError(cnt, tbl, obj.getRowID )
      }
      cnt
    } else {
      0
    }
  }

  private def fmtUpdateWhere(lock:Boolean) = {
    val s1= COL_ROWID + "=?"
    if (lock) s1 + " AND " + COL_VERID + "=?" else s1
  }

  protected def doDelete( obj:DBPojo): Int = {
    val lock= getDB().supportsOptimisticLock()
    val t= throwNoTable(obj.getClass)
    val p= mutable.ArrayBuffer[Any]()
    var w= COL_ROWID + "=?"
    p += obj.getRowID
    if (lock) {
      w += " AND " + COL_VERID + "=?"
      p += obj.getVerID
    }
    execute("DELETE FROM " + t.table.uc + " WHERE " + w,  p:_* )
  }

  protected def doInsert(pojo:DBPojo): Int = {
    val lst = mutable.ArrayBuffer[Any]()
    val s2 = new StringBuilder(1024)
    val s1= new StringBuilder(1024)
    val cz= throwNoCZMeta(pojo.getClass)
    val t= throwNoTable(pojo.getClass)
    val obj= pojo.asInstanceOf[AbstractModel]
    val flds= cz.getFldMetas

    obj.setLastModified(nowJTS )
    obj.getDirtyFields.foreach { (dn) =>
      val go = flds.get(dn) match {
        case Some(fld) =>
          if (fld.isPK ||fld.isAutoGen || fld.isInternal ) false else true
        case _ => true
      }
      if (go) {
        addAndDelim(s1, ",", dn)
        if (s2.length > 0) { s2.append(",") }
        obj.get(dn) match {
          case Some(Nichts.NICHTS) | None => s2.append("NULL")
          case Some(v) =>
            s2.append("?")
            lst += v
        }
      }

    }

    if (s1.length > 0) {
      val ( rc, out) = executeWithOutput( "INSERT INTO " + t.table.uc + "(" + s1 + ") VALUES (" + s2 + ")" , lst:_* )
      if (out.length == 0) {
        throw new SQLEx("Insert require row-id to be returned.")
      }
      out(0) match {
        case n:Long => obj.setRowID(n)
        case _ => throw new SQLEx("RowID data-type must be Long.")
      }
      rc
    } else {
      0
    }
  }


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

  def getO2O[T <: DBPojo ](lhs:DBPojo, rhs:Class[T], fkey:String): Option[T] = {
    val rc= lhs.get(fkey) match {
      case Some(x:Long) =>  findSome( rhs, new NameValues(COL_ROWID, x), "" )
      case _ => List()
    }
    if (rc.size == 0) None else Option( rc(0) )
  }

  def setO2O(lhs:DBPojo, rhs:DBPojo, fkey:String) {
    lhs.set(fkey, if (rhs==null) None else Option(rhs.getRowID ) )
  }

  def purgeO2O(lhs:DBPojo, rhs:Class[_], fkey:String) = {
    // don't care about lock here for now
    val t= throwNoTable(rhs)
    val sql="DELETE FROM " + t.table.uc + " WHERE " + COL_ROWID + "=?"
    lhs.get(fkey) match {
      case Some(x:Nichts) => 0
      case Some(v) => execute(sql, v)
      case _ => 0
    }
  }

  def getO2M[T <: DBPojo ](lhs:DBPojo, rhs:Class[T], fkey:String, orderby:String = ""): Seq[T] = {
    findSome( rhs, new NameValues(fkey, lhs.getRowID ), orderby)
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
    0
  }

  def getM2M[T <: DBPojo](lhs:DBPojo, rhs:Class[T], orderby:String = ""): Seq[T] = {
    val jc= _meta.findJoined(lhs.getClass,rhs)
    val jn= throwNoTable(jc).table.uc
    val rn= Utils.getTable(rhs).table.uc
    val ln= Utils.getTable( lhs.getClass ).table.uc

    val sql = "SELECT DISTINCT RES.* FROM " + rn + " RES JOIN " + jn + " MM  ON " +
    "MM." + COL_LHS + "=? AND " + "MM." + COL_RHS + "=? AND " +
    "MM." + COL_LHSOID + "=? AND " + "MM." + COL_RHSOID + " = RES." + COL_ROWID
    findViaSQL(rhs, sql, ln, rn, lhs.getRowID)
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
       throw new OptLockError("Possible Optimistic lock failure for table: " +
               table + ", rowid= " + rowID)
    }
  }

  private def jiggleSQL( sql:String, ty:Int) = {
    val v:DBVendor = getDB.getVendor()
    val rc = if (DBVendor.SQLSERVER != v) sql else {
      ty match {
        case 0 => v.tweakDELETE(sql)
        case 1 => v.tweakSELECT(sql)
        case 2 => v.tweakUPDATE(sql)
        //case -1 => v.tweakSQL(sql)
        case _ => v.tweakSQL(sql)
      }
    }

    tlog().debug("jggleSQL= {}", rc)
    rc
  }

  protected def doExecuteWithOutput(conn:Connection, sql:String, pms:Any*): (Int, Seq[Any])  = {
    val s = new SQuery(conn, sql, pms.toSeq )
    val rc= s.execute()
    (rc, s.getOutput )
  }

  protected def doExecute(conn:Connection, sql:String, pms:Any*): Int  = {
    new SQuery(conn, sql, pms.toSeq ).execute()
  }

  protected def doCount(sql:String, f: ResultSet => Int ): Int
  protected def doPurge(sql:String): Unit


  private def buildOneRow[T <: DBPojo](z:Class[T], row:DBPojo, rset:ResultSet) {
    val flds= throwNoCZMeta(z).getFldMetas
    val meta= rset.getMetaData

    (1 to meta.getColumnCount ).foreach { (i) =>
      val cn= meta.getColumnName(i).uc
      readOneCol(meta.getColumnType(i), cn, i, row, rset)
    }

    row.asInstanceOf[AbstractModel].built()
  }

  private def gmtCal() = {
    new GregorianCalendar( TimeZone.getTimeZone("GMT") )
  }

  private def readOneCol(sqlType:Int, cn:String, pos:Int, row:DBPojo,rset:ResultSet) {
    import java.sql.Types._
    val obj = sqlType match {
      case TIMESTAMP =>rset.getTimestamp(pos,  gmtCal)
      case DATE =>rset.getDate(pos, gmtCal )
      case _ => readCol( sqlType, cn, pos, row,rset )
    }
    row.set(cn, Option(obj))
  }

  private def readCol(sqlType:Int, cn:String, pos:Int, row:DBPojo,rset:ResultSet) = {
      var obj=rset.getObject(pos)
      var inp = obj match {
        case bb:Blob => bb.getBinaryStream()
        case s:InputStream => s
        case _ => null
      }
      var rdr = obj match {
        case cc:Clob => cc.getCharacterStream()
        case r:Reader => r
        case _ => null
      }
      if (inp != null) using(inp) { (inp) =>
        obj= IOUtils.readBytes(inp)
      }
      if (rdr != null) using(rdr) { (rdr) =>
        obj= IOUtils.readChars( rdr)
      }

      obj
  }


}

