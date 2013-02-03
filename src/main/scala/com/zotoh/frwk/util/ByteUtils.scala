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
package util

import java.io.{ByteArrayInputStream=>ByteArrayIS,ByteArrayOutputStream=>ByteArrayOS,DataInputStream,DataOutputStream}
import com.zotoh.frwk.util.CoreUtils._

import java.nio.charset.Charset
import java.nio._

/**
 * Utililties for handling byte[] conversions to/from numbers.
 *
 * @author kenl
 *
 */
object ByteUtils {

  /**
   * @param ca
   * @return
   */
  def convertCharsToBytes(ca:Array[Char], enc:Charset) = {
    enc.encode(CharBuffer.wrap(ca)).array()
  }

  /**
   * @param ba
   * @return
   */
  def convertBytesToChars(ba:Array[Byte], enc:Charset) = {
    enc.decode( ByteBuffer.wrap(ba)).array()
  }

  /**
   * @param bits
   * @return
   */
  def readAsLong(bits:Array[Byte]) = {
    new DataInputStream( new ByteArrayIS(bits)).readLong
  }

  /**
   * @param bits
   * @return
   */
  def readAsInt(bits:Array[Byte]) = {
    new DataInputStream( new ByteArrayIS(bits)).readInt
  }

  /**
   * @param n
   * @return
   */
  def readAsBytes(n:Long) = {
    using(new ByteArrayOS()) { (baos) =>
      val ds= new DataOutputStream( baos)
      ds.writeLong(n)
      ds.flush()
      baos.toByteArray
    }
  }

  /**
   * @param n
   * @return
   */
  def readAsBytes(n:Int) = {
    using( new ByteArrayOS()) { (baos) =>
      val ds= new DataOutputStream( baos)
      ds.writeInt(n)
      ds.flush()
      baos.toByteArray
    }
  }

}

