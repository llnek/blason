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

import java.sql.{SQLException=>SQLEx, Connection, ResultSet}
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



/**
 * @author kenl
*/
sealed trait SQLBlockType
object SQLComplexType extends SQLBlockType
object SQLSimpleType extends SQLBlockType

/**
 * @author kenl
*/
trait SQLProcessor extends CoreImplicits {

  protected val _meta:MetaCache
  protected val _log:Logger
  def tlog() = _log
  
  import DBPojo._
  
  def select[T]( sql:String, params:Any* )(f: ResultSet => T): Seq[T]

  def execute(sql:String, params:Any* ): Int

  def insert( obj:DBPojo): Int
  def delete( obj:DBPojo): Int

  def update( obj:DBPojo, cols:Set[String] ): Int
  def update( obj:DBPojo): Int = {
    //update(obj, obj.getSchemaFactory.getUpdatableCols )
    //TODO, uncomment
    //update(obj, Set()) 
    0
  }

  def findSome(cz:ClassMetaHolder, filter:NameValues): Seq[DBPojo]
  def findAll(cz:ClassMetaHolder): Seq[DBPojo]

  protected def doFindSome(cz:ClassMetaHolder, filter:NameValues): Seq[DBPojo] = {
    val lst = mutable.ArrayBuffer[Any]()
    val s= "SELECT * FROM " + cz.getTable()
    val wc= filter.toWhereClause()
    val cb= { row : ResultSet => null }
    if ( !STU.isEmpty(wc._1) ) {
      select( s + " WHERE " + wc._1, wc._2:_* )(cb)
    } else {
      select( s)(cb)
    }
  }

  protected def doFindAll(cz:ClassMetaHolder): Seq[DBPojo] = {
    val cb = { row : ResultSet => null  }
    select("SELECT * FROM " + cz.getTable )(cb)
  }

  protected def pkeys(obj : SRecord): (String, Seq[Any]) = {
    val lst = mutable.ArrayBuffer[Any]()
    val sb1= new StringBuilder(512)
    val sf= obj.getSchemaFactory
    sf.getPrimaryKeys.foreach { k =>
      if (sb1.length > 0) { sb1.append(" AND ") }
      sb1.append(k).append("=?")
      obj.getVal(k) match {
        case Some(NullAny) => throw new SQLEx("Primary key has NULL value")
        case null | None => throw new SQLEx("Primary key has no value")
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

  def update( conn:Connection, obj:DBPojo ): Int = {
    val t= getMetas(conn, obj.getClass )
    update( conn, t._1, t._2, obj)
  }

  private def update( conn:Connection, zm:ClassMetaHolder, tm:TableMetaHolder,
    obj:DBPojo): Int = {

    val bf= new StringBuilder(1024).append( "UPDATE  " ).append( tm.getName ).append( " SET " )
    val vs= mutable.ArrayBuffer[ (Any,Int) ]() 
    val b1= new StringBuilder(512)
    val w= new StringBuilder(512)
    val cms= tm.getColMetas
    val flds=zm.getFldMetas
    val curVer= obj.getVerID
    val newVer= curVer+1

    cms.foreach { (en) =>
      val cn= en._1.uc
      val cm= en._2
      val fld=flds.get(cn)
      var gm = if (fld.isEmpty) null else fld.get.getGetter()
      var ct=0
      var arg:Any= null
      if (COL_ROWID == cn || gm == null || fld.get.isAutoGen) {} else {
        val ct = if (COL_VERID == cn) {
          arg = newVer
          java.sql.Types.BIGINT
        } else {
          try {
            arg= gm.invoke(obj)
          } catch {
            case e:SQLEx => throw e
            case e:Throwable => throw new SQLEx(e)
          }
          cm.getSQLType
        }
        vs += new Tuple2( arg ,   ct )
        addAndDelim(b1, ",", cn+"=?")
      }
    }

    w.append(COL_ROWID).append("=? AND ").append(COL_VERID).append("=?")
    bf.append(b1).append(" WHERE ").append(w)

    // add in the row id, primary key
    vs += new Tuple2( obj.getRowID, java.sql.Types.BIGINT)
    vs += new Tuple2( curVer, java.sql.Types.BIGINT)

    val cnt= update( conn, bf.toString, vs:_* )
    lockError(cnt, tm.getName, obj.getRowID )

    // update the version num
    obj.setVerID( newVer)
    cnt
  }

  private def update( conn:Connection, sql:String, values:(Any,Int)* ): Int = {

    val ps=conn.prepareStatement( jiggleSQL(sql,2) )
    var n=0
    try {
      values.foldLeft(1) { (pos,t) =>
        setStatement(ps, pos, t._2, t._1)
        pos+1
      }
      n= ps.executeUpdate()
    } finally {
      tlog.debug("update: {} row(s)" , n)
      DBU.close(ps)
    }
    n
  }


  def remove( conn:Connection, obj:DBPojo): Int = {
    val t= getMetas(conn, obj.getClass )
    remove( conn, t._1, t._2, obj)
  }

  private def remove( conn:Connection, zm:ClassMetaHolder, tm:TableMetaHolder,
    obj:DBPojo): Int = {

    val bd= new StringBuilder(512).
    append("DELETE FROM ").
    append( tm.getName).
    append( " WHERE ").
    append( COL_ROWID ).
    append(" =? AND ").
    append(COL_VERID).append("=?")
    val cnt= remove(conn, bd.toString, obj.getRowID, obj.getVerID )

    lockError(cnt, tm.getName, obj.getRowID )

    // do we need to do this?
    obj.setVerID( -1L )
    cnt
  }

  private def remove( conn:Connection, sql:String , rowID:Long, verID:Long ): Int = {

    val ps=conn.prepareStatement( jiggleSQL(sql, 0) )
    var n=0
    try {
      setStatement(ps, 1, java.sql.Types.BIGINT, rowID)
      setStatement(ps, 2, java.sql.Types.BIGINT, verID)
      n= ps.executeUpdate()
    } finally {
      DBU.close(ps)
    }
    tlog().debug("removed {} row(s)" , n )
    n
  }


  protected def create(conn:Connection, obj:DBPojo):DBPojo = {
    val t= getMetas( conn, obj.getClass())
    create( conn, t._1, t._2, obj ) match {
      case Tuple2(a:Int, None ) =>
      case Tuple2( a:Int, rc:Option[Any] ) => obj.setRowID( rc.get.asInstanceOf[Long])
    }
    obj
  }

  private def create( conn:Connection, zm:ClassMetaHolder, tm:TableMetaHolder, 
    obj:DBPojo): (Int,Option[Any])  = {
    val bf= new StringBuilder(1024).append("INSERT INTO  ").append(tm.getName).append(" (")
    val values= mutable.ArrayBuffer[(Any,Int)]()
    // column-name, db-type, value
    val b2= new StringBuilder(512)
    val b1= new StringBuilder(512)
    val flds= zm.getFldMetas()
    val cms= tm.getColMetas()

    // always set to initial value
    obj.setVerID(1L)

    cms.foreach { (en) =>
      val cn= en._1.toUpperCase()
      val cm= en._2
      val fld=flds.get(cn)
      var arg:Any = null

      if (COL_VERID == cn) {
        arg= 1L
      } else {
        val gm = if (fld.isEmpty) null else fld.get.getGetter()
        if (gm==null || fld.get.isAutoGen) {} else {
          try {
            arg= gm.invoke(obj)
          } catch {
            case e:SQLEx => throw e
            case e:Throwable => throw new SQLEx(e)
          }
        }
      }
      if (arg != null) {
        values += new Tuple2(arg, cm.getSQLType )
        addAndDelim(b1, ",", cn)
        addAndDelim(b2, ",", "?")
      }
    }

    bf.append(b1).append(") VALUES (").append(b2).append(")")
    create(conn, zm, tm, bf.toString(), values.toSeq )
  }

  private def create( conn:Connection, zm:ClassMetaHolder, tm:TableMetaHolder,
      sql:String, values:Seq[(Any,Int)] ): (Int,Option[Any]) = {

    tlog.debug("Insert: SQL = {}" , sql)

    val ps = if (tm.canGetGeneratedKeys) {
      conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    } else {
      conn.prepareStatement(sql)
    }
    var rc:Option[Any] = None
    var n=0
    try {
      values.foldLeft(1) { (pos,t) =>
        setStatement(ps, pos, t._2, t._1)
        pos+1
      }
      n= ps.executeUpdate()
      if (tm.canGetGeneratedKeys) {
        val rs=ps.getGeneratedKeys()
        val cnt = if (rs == null) 0 else {
          rs.getMetaData.getColumnCount()
        }
        if (cnt > 0 && rs.next ) {
          rc= Some(rs.getObject(1))
        }
      }
      (n,rc)
    } finally {
      tlog.debug("insert: inserted {} row(s)", n)
      DBU.close(ps)
    }
  }

  def execUpdateSQL(sql:String, params:Any*) = {
    0
  }
  
////////////////

  def getO2O[T](lhs:DBPojo, rhs:Class[T], fkey:String) = {
    val t= rhs.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx("RHS class " + rhs + " has no Table annotation" ) }
    //TODO
    //fetchObj(rhs, new NameValues(fkey, lhs.getRowID) )
    null
  }

  def setO2O(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    val t= rhs.getClass.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " +
              rhs.getClass + " has no Table annotation" ) }
    val curVer = rhs.getVerID
    val newVer = curVer+1
    val rid = rhs.getRowID
    val tn=t.table.uc
    val sql="UPDATE " + tn + " SET " + fkey + " =? , " + COL_VERID + "=? " + " WHERE " +
            COL_ROWID + "=? and " + COL_VERID + "=?"

    val cnt= execUpdateSQL(sql, lhs.getRowID, newVer, rid , curVer )
    lockError(cnt, tn, rid)

    // up the ver num
    rhs.setVerID( newVer)
    cnt
  }

  def purgeO2O(lhs:DBPojo, rhs:Class[_], fkey:String) = {
    val t= rhs.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " +
                        rhs + " has no Table annotation" ) }
    val rn=t.table.uc
    val sql="DELETE from " + rn + " WHERE " + fkey + "=?"
    execUpdateSQL(sql, lhs.getRowID )
  }

  def getO2M[T](lhs:DBPojo, rhs:Class[T], fkey:String): Seq[T] = {
    val t= rhs.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " + rhs + " has no Table annotation" ) }
    // do a general select
    // TODO
    //fetchObjs(rhs, new NameValues(fkey, lhs.getRowID ))
    Nil
  }

  def removeO2M(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    val t= rhs.getClass().getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " +
              rhs.getClass() + " has no Table annotation" ) }
    val curVer= rhs.getVerID
    val newVer= curVer+1
    val rid = rhs.getRowID
    val rn=t.table.uc
    val sql= "UPDATE " + rn + " SET " + fkey + " =NULL , " + COL_VERID + "=? " +
            " WHERE " + COL_ROWID + "=? and " +
            COL_VERID + "=?"

    val cnt= execUpdateSQL(sql, newVer, rid, curVer )
    lockError(cnt, rn, rid)

    rhs.setVerID(newVer)
    cnt
  }

  def purgeO2M(lhs:DBPojo, rhs:Class[_], fkey:String) = {
    val t= rhs.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " +
                        rhs.getClass() + " has no Table annotation" ) }
    val rn=t.table.uc
    val sql="DELETE from " + rn + " WHERE " + fkey + "=?"
    execUpdateSQL(sql, lhs.getRowID )
  }

  def addO2M(lhs:DBPojo, rhs:DBPojo, fkey:String): Int = {
    val t= rhs.getClass.getAnnotation(classOf[Table] )
    if (t==null) { throw new SQLEx( "RHS class " +
              rhs.getClass + " has no Table annotation" ) }
    val tn=t.table.uc
    val curVer = rhs.getVerID
    val newVer = curVer+1
    val rid= rhs.getRowID
    val sql="UPDATE " + tn + " SET " + fkey + " =? , " + COL_VERID + "=? " + " WHERE " +
            COL_ROWID + "=? and " +  COL_VERID + "=?"

    val cnt= execUpdateSQL(sql, lhs.getRowID, newVer, rid , curVer )
    lockError(cnt, tn, rid)

    // up the ver num
    rhs.setVerID( newVer)
    cnt
  }

  def getM2M[T](lhs:DBPojo, rhs:Class[T]): Seq[T] = {

    var t=classOf[M2MTable].getAnnotation(classOf[Table] )
    val z:Class[_] = lhs.getClass
    var tn=t.table.uc
    t = rhs.getAnnotation(classOf[Table] )
    val rn= t.table.uc
    t=z.getAnnotation(classOf[Table] )
    val ln= t.table.uc
    val sql = "SELECT distinct res.* from " + rn + " res JOIN " + tn + " mm  ON " +
            "mm." + COL_LHS + "=? and " + "mm." + COL_RHS + "=? and " +
            "mm." + COL_LHSOID + "=? and " + "mm." + COL_RHSOID + " = res." + COL_ROWID
//TODO
    //fetchViaSQL(rhs, sql, ln, rn, lhs.getRowID)
            Nil
  }

  def removeM2M(lhs:DBPojo, rhs:DBPojo): Int = {
    var t=classOf[M2MTable].getAnnotation(classOf[Table] )
    val z:Class[_]= lhs.getClass
    val tn=t.table.uc
    t = rhs.getClass.getAnnotation(classOf[Table] )
    val rn= t.table.uc
    t=z.getAnnotation(classOf[Table] )
    val ln= t.table.uc

    val sql ="DELETE from " + tn +
            " where " + COL_RHS + "=? and " + COL_LHS + "=? and " +
            COL_RHSOID + "=? and " + COL_LHSOID + "=?"

    execUpdateSQL(sql, rn, ln, rhs.getRowID, lhs.getRowID)
  }

  def removeM2M(lhs:DBPojo, rhs:Class[_]): Int = {
    var t=classOf[M2MTable].getAnnotation(classOf[Table] )
    val z:Class[_]= lhs.getClass
    val tn=t.table.uc
    t = rhs.getAnnotation(classOf[Table] )
    val rn= t.table.uc
    t=z.getAnnotation(classOf[Table] )
    val ln= t.table.uc

    val sql ="DELETE from " + tn +
            " where " + COL_RHS + "=? and " + COL_LHS + "=? and " +
            COL_LHSOID + "=?"

    execUpdateSQL(sql, rn, ln, lhs.getRowID )
  }

  def addM2M(lhs:DBPojo, rhs:DBPojo): Int = {
    var t=classOf[M2MTable].getAnnotation(classOf[Table] )
    val z:Class[_]= lhs.getClass
    val tn=t.table.uc
    t = rhs.getClass.getAnnotation(classOf[Table] )
    val rn= t.table.uc
    t=z.getAnnotation(classOf[Table] )
    val ln= t.table.uc

    val sql ="INSERT into " + tn +
            " ( " + COL_VERID + ", " + COL_RHS + ", " + COL_LHS + ", " +
            COL_RHSOID + ", " + COL_LHSOID + ") values (?,?,?,?,?)"

    execUpdateSQL(sql, 1L, rn, ln, rhs.getRowID, lhs.getRowID )
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
        
  
}

/**
 * @author kenl
*/
class SimpleSQLr(private val _db: DB) { //}extends SQLProcessor {
  val _log= LoggerFactory.getLogger(classOf[SimpleSQLr])
  
  def findSome(fac : SRecordFactory, filter : NameValues): Seq[SRecord] = {
    //doFindSome(fac,filter)
    Nil
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
    //doFindAll(fac)
    Nil
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
    //doUpdate(obj, cols)
    0
  }

  def delete(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      //doDelete(c, obj)
      0
    }
    finally {
      _db.close(c)
    }
  }

  def insert(obj : SRecord): Int = {
    val c= _db.open
    try {
      c.setAutoCommit(true)
      //doInsert(c, obj)
      0
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
class Transaction(private val _conn : Connection ) { //}extends SQLProcessor {

  val _log= LoggerFactory.getLogger(classOf[Transaction])

  def insert(obj : SRecord): Int = {
//    doInsert(_conn, obj)
    0
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
//    doDelete(_conn, obj)
    0
  }

  def update(obj : SRecord, cols : Set[String]): Int = {
//    doUpdate(obj, cols)
    0
  }

  def findSome(fac : SRecordFactory, filter : NameValues): Seq[SRecord] = {
//    doFindSome(fac,filter)
    Nil
  }

  def findAll(fac : SRecordFactory): Seq[SRecord] = {
//    doFindAll(fac)
    Nil
  }

}

