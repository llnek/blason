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

import scala.collection.mutable
import java.io.{InputStream,OutputStream,IOException, Reader}
import java.math.{BigDecimal,BigInteger}
import java.sql.{Clob,Blob,PreparedStatement,ResultSet=>RSET}
import java.sql.{ResultSetMetaData,SQLException}
import java.sql.{Time=>JSTime,Timestamp=>JSTStamp,Date=>JSDate}
import java.util.{Date=>JUDate}
import com.zotoh.frwk.io.{IOUtils,XData}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import com.zotoh.frwk.io.IOUtils._
import org.apache.commons.io.{FileUtils=>FUS}
import org.apache.commons.io.{IOUtils=>IOU}
import com.zotoh.frwk.util.CoreImplicits
import org.slf4j._
import com.zotoh.frwk.security.Password
import com.zotoh.frwk.util.Nichts._
import java.sql.Types._
import java.util.Calendar
import java.util.TimeZone
import java.util.GregorianCalendar



/**
 * @author kenl
 *
 */
object JDBCUtils extends CoreImplicits {

  private val _log = LoggerFactory.getLogger(classOf[JDBCUtils])
  def tlog() = _log

  import DBUtils._

  /**
   * @param z
   * @return
   */
  def toSqlType(z:Class[_]) = {
    z.getName() match {
      case "boolean"|"Boolean"|"java.lang.Boolean" => BOOLEAN
      case "int"|"Int"|"java.lang.Integer" => INTEGER
      case "long"|"Long"|"java.lang.Long" => BIGINT
      case "float"|"Float"|"java.lang.Float" => FLOAT
      case "double"|"Double"|"java.lang.Double" => DOUBLE
      case "String"|"java.lang.String" => VARCHAR
      case "byte[]"|"[B" => BINARY
      case _ =>
        if (z == classOf[BigDecimal]) { DECIMAL }
        else if (z == classOf[JSDate]) { DATE }
        else if (z == classOf[JSTime]) { TIME }
        else if (z == classOf[JSTStamp]) { TIMESTAMP }
        else if (z == classOf[JUDate]) { DATE }
        else {
          throw mkSQLErr("JDBC Type not supported: " + z.getName)
        }
    }
//    if (BigInteger.class==z) return java.sql.Types.DECIMAL ;
  }

  /**
   * @param rs
   * @param col
   * @param javaSql
   * @param target
   * @return
   */
  def getObject(rs:RSET, col:Int, javaSql:Int, target:Class[_]) = {
    val cval = javaSql match {
      case SMALLINT | TINYINT =>  sql_short(rs, col)
      case INTEGER =>  sql_int(rs, col)
      case BIGINT =>  sql_long(rs, col)
      case REAL | FLOAT =>  sql_float(rs, col)
      case DOUBLE =>  sql_double(rs, col)
      case NUMERIC | DECIMAL =>  sql_bigdec(rs, col)
      case BOOLEAN =>  sql_bool(rs, col)
      case TIME => sql_time(rs, col)
      case DATE =>  sql_date(rs, col)
      case TIMESTAMP =>  sql_timestamp(rs, col)
      case LONGVARCHAR | VARCHAR =>  sql_string(rs, col)
      case LONGVARBINARY => sql_stream(rs, col)
      case VARBINARY | BINARY =>  sql_bytes(rs, col)
      case BLOB =>  sql_blob(rs, col)
      case BIT =>  sql_bit(rs, col)
      case NULL =>  sql_null(rs, col)
      //case LONGNVARCHAR || NVARCHAR || CLOB =>
      case _ => sql_notimpl( rs.getMetaData, col)
    }

    safe_coerce(cval, target)
  }

  private def sql_short(rs:RSET, col:Int): Any = rs.getShort(col)

  private def sql_int(rs:RSET, col:Int):Any = rs.getInt(col)

  private def sql_long(rs:RSET, col:Int):Any = rs.getLong(col)

  private def sql_float(rs:RSET, col:Int):Any = rs.getFloat(col)

  private def sql_double(rs:RSET, col:Int):Any = rs.getDouble(col)

  private def sql_bigdec(rs:RSET, col:Int):Any = rs.getBigDecimal(col)

  private def sql_bool(rs:RSET, col:Int):Any = rs.getBoolean(col)

  private def sql_time(rs:RSET, col:Int):Any = rs.getTime(col)

  private def sql_date(rs:RSET, col:Int):Any = rs.getDate(col)

  private def sql_timestamp(rs:RSET, col:Int):Any = {
    rs.getTimestamp(col)
  }

  private def sql_string(rs:RSET, col:Int):Any = rs.getString(col)

  private def sql_bit(rs:RSET, col:Int):Any = rs.getByte(col)

  private def sql_null(rs:RSET, col:Int):Any = NICHTS

  private def sql_notimpl(rs:ResultSetMetaData, col:Int) {
    val ct = rs.getColumnType(col)
    val cn = rs.getColumnName(col)
    throw mkSQLErr("Unsupported SQL Type: " + ct + " for column: " + cn)
  }

  private def sql_clob(rs:RSET, col:Int) = {
    val c=rs.getClob(col)
    if (c==null) null else sql_rdr( c.getCharacterStream )
  }

  private def sql_blob(rs:RSET, col:Int):Any = {
    val b = rs.getBlob(col)
    if (b==null) null else sql_stream( b.getBinaryStream )
  }

  private def sql_stream(rs:RSET, col:Int):Any = {
    sql_stream( rs.getBinaryStream(col) )
  }

  private def sql_stream(inp:InputStream) = {
    val t = newTempFile(true)
    try {
      using(inp) { (inp) =>
      using(t._2) { (os) =>
          IOU.copy(inp, os)
      }}
      new XData(t._1)
    } catch {
      case e:Throwable => FUS.deleteQuietly(t._1); throw e
    }
  }

  private def sql_rdr(inp:Reader) = {
    val t = newTempFile(true)
    try {
      using(inp) { (inp) =>
      using(t._2) { (os) =>
          IOU.copy(inp, os, "utf-8")
      }}
      new XData(t._1)
    } catch {
      case e:Throwable => FUS.deleteQuietly(t._1); throw e
    }
  }

  private def sql_bytes(rs:RSET, col:Int):Any = {
    rs.getBytes(col)
  }

  //------------ coerce

  private def safe_coerce(cval:Any, target:Class[_]): Any = {
    if (target==null) { if (cval==null) NICHTS else cval }
    else if (cval == null ) { NICHTS }
    else if (classOf[BigDecimal] == target ||
        classOf[Number].isAssignableFrom(target)) {
      num_coerce(cval, target)
    }
    else if (target == classOf[Array[Byte]]) { bytes_coerce(cval,target) }
    else if (target == classOf[Boolean]) { bool_coerce(cval, target) }
    else if (target == classOf[String]) { string_coerce(cval,target) }
    else if (target == classOf[XData]) { stream_coerce(cval,target) }
    else if (target == classOf[JSTStamp]) { tstamp_coerce(cval, target) }
    else if (target == classOf[JSTime]) { time_coerce(cval, target) }
    else if (target == classOf[JSDate]) { date_coerce(cval,target) }
    else if (target == classOf[JUDate]) { date_coerce(cval,target) }
    else {
      throw mkSQLErr("Cannot coerce coltype: " + cval.getClass() +
      " to target-class: " + target)
    }

  }

  private def tstamp_coerce(cval:Any): JSTStamp = {
    cval match {
      case t:JSTStamp => t
      case _ => null
    }
  }

  private def time_coerce(cval:Any): JSTime = {
    cval match {
      case t:JSTime => t
      case t:JUDate => new JSTime( t.getTime)
      case _ => null
    }
  }

  private def date_coerce(cval:Any, target:Class[_]): JUDate = {
    cval match {
      case t:JSDate => new JUDate(t.getTime)
      case t:JUDate => t
      case _ => null
    }
  }

  private def string_coerce(cval:Any): String = {
    if (cval==null) null else cval.toString
  }

  private def stream_coerce(cval:Any): XData =  {
    cval match {
      case t:Clob => sql_rdr( t.getCharacterStream )
      case t:InputStream => sql_stream(t)
      case t:Blob => sql_stream( t.getBinaryStream )
      case t:Reader => sql_rdr(t)
      case t:Array[Byte] => new XData(t)
      case t:XData => t
      case _ => null
    }
  }

  private def bytes_coerce(cval:Any): Array[Byte] = {
    cval match {
      case v:XData => v.javaBytes
      case v:Array[Byte] => v
      case _ => null
    }
  }

  private def bool_coerce(cval:Any): Boolean = {
    cval match {
      case v:BigDecimal => v.intValue() > 0
      case v:Boolean => v
      case v:Number => v.intValue() > 0
      case _ => false
    }
  }

  private def num_coerce(cval:Any, target:Class[_]): Any = {
    var big:BigDecimal=null
    var b:Number=null
    var rc:Any=null
    cval match {
      case v:BigInteger => throw mkSQLErr("Don't support BigInteger class")
      case v:BigDecimal => big=v
      case v:Number => b=v
      case _ => null
    }

    if (target == classOf[Double]) {
      rc= if(big==null) b.doubleValue else big.doubleValue
    }
    else if (target==classOf[Float]) {
      rc= if(big== null) b.floatValue else big.floatValue
    }
    else if (target==classOf[Long]) {
      rc= if(big==null) b.longValue else big.longValue
    }
    else if (target==classOf[Int]) {
      rc= if(big==null) b.intValue else big.intValue
    }
    else if (target== classOf[Short]) {
      rc= if(big==null) b.shortValue else big.shortValue
    }
    else if (target== classOf[Byte]) {
      rc= if(big==null) b.byteValue else big.byteValue
    }

    rc
  }

  // ------------------------- set --------------

  def javaToBytes(obj:Any) = bytes_coerce(obj)

  def javaToSQLBoolean(obj:Any) = {
    obj match {
      case v:Number => v.intValue() > 0
      case v:Boolean => v
      case _ => false
    }
  }

  def javaToSQLInt(obj:Any) = {
    obj match {
      case v:Boolean => if (v) 1 else 0
      case v:Number => v.intValue()
      case _ => 0
    }
  }

  def javaToSQLLong(obj:Any) = {
    obj match {
      case v:Boolean => if (v) 1L else 0L
      case v:Number => v.longValue()
      case _ => 0L
    }
  }

  def javaToSQLDecimal(obj:Any) = {
    obj match {
      case v:BigDecimal => v
      case v:BigInteger =>
        throw mkSQLErr("Unsupport BigInteger value type")
      case v:Number => new BigDecimal( v.doubleValue )
      case _ => new BigDecimal(0.0)
    }
  }

  def javaToSQLDouble(obj:Any) = {
    obj match {
      case d:Double  => d.toDouble
      case f:Float => f.toDouble
      case n:Number => n.doubleValue
      case _ => 0.0
    }
  }

  def javaToSQLFloat(obj:Any) = {
    obj match {
      case d:Double  => d.toFloat
      case f:Float => f.toFloat
      case n:Number => n.floatValue
      case _ => 0.0f
    }
  }

  def javaToCalendar(obj:Any, tz:TimeZone) = {
    val cal= new GregorianCalendar(tz)
    obj match {
      case d:JSDate => 
        cal.setTimeInMillis(d.getTime)
        cal
      case d:JUDate =>
        cal.setTimeInMillis(d.getTime)
        cal
      case _ => null
    }
  }
  
  def javaToJDate(obj:Any) = {
    obj match {
      case d:JSDate => new JUDate(d.getTime())
      case d:JUDate => d
      case _ => null
    }
  }
  
  def javaToSQLDate(obj:Any) = {
    obj match {
      case d:JSDate => d
      case d:JUDate => new JSDate(d.getTime)
      case _ => null
    }
  }

  def javaToSQLTime(obj:Any) = {
    obj match {
      case t:JSTime => t
      case t:JUDate => new JSTime( t.getTime )
      case _ => null
    }
  }

  def javaToSQLTimestamp(obj:Any) = {
    obj match {
      case t:JSTStamp => t
      case t:JUDate => new JSTStamp( t.getTime )
      case _ => null
    }
  }

  /**
   * @param stmt
   * @param pos
   * @param sqlType
   * @param value
   */
  def setStatement(stmt:PreparedStatement, pos:Int, sqlType:Int, value:Any) {
    val z:Class[_] = if (isNichts(value)) { stmt.setNull(pos, sqlType); null } else {
      value.getClass
    }

    if (z != null) sqlType match {

      case BOOLEAN =>
        stmt.setBoolean(pos, javaToSQLBoolean(value))

        // numbers
      case DECIMAL | NUMERIC =>
        stmt.setBigDecimal(pos, javaToSQLDecimal(value))

        // ints
      case BIGINT =>
        stmt.setLong(pos, javaToSQLLong(value))

      case INTEGER | TINYINT | SMALLINT =>
        stmt.setInt(pos, javaToSQLInt(value))

        // real numbers
      case DOUBLE =>
        stmt.setDouble(pos, javaToSQLDouble(value))

      case REAL | FLOAT =>
        stmt.setFloat(pos, javaToSQLFloat(value))

        // date time
      case DATE =>
        stmt.setDate(pos, javaToSQLDate(value))

      case TIME =>
        stmt.setTime(pos, javaToSQLTime(value))

      case TIMESTAMP =>
        stmt.setTimestamp(pos, javaToSQLTimestamp(value))

        // byte[]
      case VARBINARY | BINARY =>
        var b:Array[Byte]=null
        if (z==classOf[XData]) {
          b= value.asInstanceOf[XData].javaBytes
        } else if (z == classOf[ Array[Byte] ]) {
          b= value.asInstanceOf[Array[Byte]]
        }
        if (b==null) {
          throw mkSQLErr("Expecting byte[] , got : " + z)
        }
        stmt.setBytes(pos, b)

      case LONGNVARCHAR | CLOB | NVARCHAR =>
        throw mkSQLErr("Unsupported SQL type: " + sqlType)

        // strings
      case LONGVARCHAR | VARCHAR =>
        var s= value match {
          case pwd:Password =>  pwd.encoded
          case str:String => str
          case null => ""
          case _ => value.toString
        }
        stmt.setString(pos, s)

      case LONGVARBINARY | BLOB =>
        val inp = value match {
          case v:Array[Byte] => asStream(v)
          case v:XData => v.stream()
          case _ => null
        }
        if (inp==null) { throw mkSQLErr("Expecting byte[] , got : " + z) }
        stmt.setBinaryStream(pos, inp, inp.available )
    }

  }

  /**
   * @param stmt
   * @param pos
   * @param value
   */
  def setStatement(stmt:PreparedStatement, pos:Int, value:Any) {
    value match {
      case v:BigInteger => throw mkSQLErr("Don't support BigInteger class")
      case v:BigDecimal => stmt.setBigDecimal(pos, v)
      
      case v:Boolean =>stmt.setBoolean(pos, v)
      case v:Long => stmt.setLong(pos, v)
      case v:Int => stmt.setInt(pos, v)
      case v:Short => stmt.setShort(pos, v)
      case v:Double => stmt.setDouble(pos, v)
      case v:Float => stmt.setFloat(pos, v)
      case v:Array[Byte] => stmt.setBytes(pos, v)
//      case v:String => stmt.setString(pos, v) // no scala string
      case v:Byte => stmt.setByte(pos, v)
      case v:java.lang.String => stmt.setString(pos, v)
      
      case v:java.lang.Boolean =>stmt.setBoolean(pos, v)
      case v:java.lang.Long => stmt.setLong(pos, v)
      case v:java.lang.Integer => stmt.setInt(pos, v)
      case v:java.lang.Short => stmt.setShort(pos, v)
      case v:java.lang.Double => stmt.setDouble(pos, v)
      case v:java.lang.Float => stmt.setFloat(pos, v)
      case v:java.lang.Byte => stmt.setByte(pos, v)
      
      case v:JSTStamp => stmt.setTimestamp(pos, v)
      case v:JSTime => stmt.setTime(pos, v)
      case v:JSDate => stmt.setDate(pos, v)
      case v:JUDate => stmt.setDate(pos, new JSDate(v.getTime ))
      case v:Password => stmt.setString(pos, v.encoded)
      case v:XData =>
        if (v.isDiskFile) {
          stmt.setBinaryStream(pos, v.stream )
        } else {
          stmt.setBytes(pos, v.javaBytes)
        }
      case NICHTS | null => stmt.setObject(pos, null)
      case _ =>
        throw mkSQLErr("Unsupport value class: " + value.getClass )
    }

  }

}

sealed class JDBCUtils {}


