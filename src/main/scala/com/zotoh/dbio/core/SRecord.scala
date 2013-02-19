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

import scala.collection.mutable
import java.lang.{ Class => JClass }
import mutable.HashMap
import java.sql.{ResultSet, SQLException, Blob => SqlBlob, Clob =>SqlClob}
import java.util.{Date =>JDate}
import java.net.URL
import java.io.{CharArrayWriter, ByteArrayOutputStream=>ByteArrayOS, InputStream, Reader}
import java.math.{ BigDecimal => JBigDec , BigInteger => JBigInt }
import org.slf4j._

object NullAny { val typeVal =  -1 }


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

