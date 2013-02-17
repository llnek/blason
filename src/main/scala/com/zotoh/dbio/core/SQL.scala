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

import java.lang.reflect.{Field, Modifier}
import java.util.regex.Pattern
import scala.collection.mutable
import java.sql._
import scala._
import org.slf4j._
import java.util.NoSuchElementException
import java.math.{BigDecimal => JBigDec, BigInteger => JBigInt}
import java.util.{Date => JDate}
import java.sql.{Date => SDate, Timestamp => STimestamp, Blob => Bloby, Clob => Cloby}
import java.io.{Reader, InputStream}
import org.apache.commons.dbutils.{DbUtils=>DBU}
import com.zotoh.frwk.db.SQLStmt



/**
 * @author kenl
*/
object SQuery {
  private val _log= LoggerFactory.getLogger(classOf[SQuery])
}

/**
 * @author kenl
 */
class SQuery( 
  private val _conn: Connection, 
  private val _sql: String,
  private val _params: Seq[Any] = Nil ) {

  def tlog() = SQuery._log

  private def using[X](f : PreparedStatement => X): X = {
    val stmt = buildStmt(_conn, _sql, _params)
    try {
      f(stmt)
    } finally {
      DBU.close(stmt)
    }
  }

  def select[X](f: ResultSet => X): Seq[X] = {
    using { stmt =>
      val rows= mutable.ArrayBuffer[X]()
      val rs= stmt.executeQuery()
      try {
        while (rs.next ) {
          rows += f(rs)
        }
        rows.toSeq
      } finally {
        DBU.close(rs)
      }
    }
  }

  def execute(): Int = {
    using { stmt =>
      stmt.executeUpdate()
    }
  }

  private def buildStmt(c: Connection, sql: String, params: Seq[Any] ): PreparedStatement = {
      tlog.debug("SQL Stmt: {}", sql)
      val ps = c.prepareStatement(sql)
      var pos=1
      params.foreach { p => setBindVar(ps, pos, p); pos += 1  }
      ps
  }

  private def setBindVar(ps : PreparedStatement, pos : Int, param : Any) = {
    param match {
        case s: String => ps.setString(pos, s)
        case l: Long => ps.setLong(pos, l)
        case i: Int => ps.setInt(pos, i)
        case si: Short => ps.setShort(pos, si)

        case bi : BigInt => ps.setBigDecimal(pos, new JBigDec(bi.bigInteger))
        case bd : BigDecimal => ps.setBigDecimal(pos, bd.bigDecimal)

        case jbd : JBigDec => ps.setBigDecimal(pos, jbd)
        case jbi : JBigInt => ps.setBigDecimal( pos, new JBigDec(jbi))

        case inp : InputStream => ps.setBinaryStream(pos, inp)
        case rdr : Reader => ps.setCharacterStream(pos, rdr)
        case bb : Bloby => ps.setBlob(pos, bb)
        case cb : Cloby => ps.setClob(pos, cb)

        case b: scala.Array[Byte] => ps.setBytes(pos, b)
        case b: Boolean => ps.setBoolean(pos, b)
        case d: Double => ps.setDouble(pos, d)
        case f: Float => ps.setFloat(pos, f)
        case t: STimestamp => ps.setTimestamp(pos, t)
        case dt: JDate => ps.setDate(pos, new SDate(dt.getTime()))

        case _ => throw new SQLException("Unsupported param type: " + param)
    }
  }

}

object NullAny { val typeVal =  -1 }

