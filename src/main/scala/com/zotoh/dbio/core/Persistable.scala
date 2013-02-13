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

import scala.collection.mutable
import java.lang.{ Class => JClass }
import mutable.HashMap
import java.sql.{ResultSet, SQLException, Blob => SqlBlob, Clob =>SqlClob}
import java.util.{Date =>JDate}
import java.net.URL
import java.io.{CharArrayWriter, ByteArrayOutputStream=>ByteArrayOS, InputStream, Reader}
import java.math.{ BigDecimal => JBigDec , BigInteger => JBigInt }
import org.slf4j._

/**
 * @author kenl
 */
object SRecordFactory {
  private val _log= LoggerFactory.getLogger(classOf[SRecordFactory])
}

/**
 * @author kenl
 */
trait SRecordFactory  {
  
  protected val columns: Set[String]
  
  def tlog() = SRecordFactory._log
  
  protected def createEmptyRecord(): SRecord

  def create( m: Map[String,Any] ): SRecord = {
      val obj = createEmptyRecord()
      m foreach { case (k,v) =>
          v match {
              case null | NullAny => obj.setVal(k, None)
              case _ => obj.setVal(k, Some(v))
          }
      }
      obj
  }

  def create(row : ResultSet): SRecord = {
      val obj = createEmptyRecord()
      columns foreach { c =>
          getRowData(obj, c, row)
      }
      obj
  }

  def getPrimaryKeys(): scala.collection.Set[String]

  def getTableName(): String

  def getColMapping(col : String): Option[ Class[_] ] = {
      Some( classOf[Object])
  }

  def getUpdatableCols(): Set[String] = {
      columns
  }

  def getCreationCols(): Set[String] = {
      getUpdatableCols()
  }
    
  def contains(col : String) = {
      columns.contains(col.toUpperCase )
  }
                    /*
                case b : Boolean =>
                case st : Short =>
                case i : Int =>
                case ll : Long =>
                case f : Float =>
                case dd : Double =>
                case bt : Byte =>
                case bi : BigInt =>
                case bc : BigDecimal =>
                case str : String =>
                case dt : JDate =>
                case url : URL =>
                */
  def getRowData(target: SRecord, col : String, row : ResultSet) = {
      val data: Any = row.getObject(col)
      var rc:Option[Any]=None
      if ( row.wasNull ) {
        tlog.debug("Persistable:getRowData: Found col: {} is NULL" , col )
      }  else  {
          val v = data match {
              case cb : SqlClob => getCharsFromReader(cb.getCharacterStream)
              case bb : SqlBlob => getBytesFromStream(bb.getBinaryStream)
              case inp : InputStream => getBytesFromStream(inp)
              case rdr : Reader => getCharsFromReader(rdr)
              case _ => data
          }
          rc=Some(v)
      }
      target.setVal(col, rc)
  }

    def getCharsFromReader(rdr : Reader) = {
        val cw= new CharArrayWriter(5000)
        val chs= new Array[Char](4096)
        var cnt = rdr.read(chs)
        while (cnt != -1) {
            if (cnt > 0) { cw.write(chs,0,cnt) }
            cnt = rdr.read(chs)
        }
        cw.toCharArray
    }

    def getBytesFromStream(inp : InputStream) = {
        val  baos = new ByteArrayOS(5000)
        val bits= new Array[Byte](4096)
        var cnt = inp.read(bits)
        while (cnt != -1) {
            if (cnt > 0) { baos.write(bits, 0, cnt)}
            cnt = inp.read(bits)
        }
        baos.toByteArray
    }

}

/**
 * @author kenl
*/
trait SRecord {
  
    protected val data = mutable.HashMap[String, Any]()

    def getSchemaFactory(): SRecordFactory

    def getVal(col : String): Option[Any] = {
        val c= col.toUpperCase()
        val rc= data.get(c).getOrElse(None)
        rc match {
            case bd : JBigDec => Some(new BigDecimal(bd))
            case bi : JBigInt => Some(new BigInt(bi))
            case null | NullAny | None => None
            case _ => Some(rc)
        }
    }

    def setVal(col : String, value : Option[Any] ) {
        val c= col.toUpperCase
        if ( ! getSchemaFactory.contains(c)) { throw new SQLException("Column not defined: " + col) }
        val v= value.getOrElse(NullAny)
        v match {
            case NullAny => data += c -> NullAny
            case _ => { 
				      data += c -> v ;
				      //debug ("Persistable:setVal: v = " + v )
            }
        }
    }







}
