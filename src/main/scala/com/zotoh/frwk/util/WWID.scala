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

import scala.collection.JavaConversions._
import scala.math._
import java.net.InetAddress

import com.zotoh.frwk.util.CoreUtils.newRandom
import com.zotoh.frwk.util.ByteUtils._
import org.apache.commons.lang3.{StringUtils=>STU}


/**
 * Generates a unique GUID.
 * Length = 48
 *
 * @author kenl
 *
 */
object WWID {

  private var _IP= newRandom().nextLong

  try {
    val net= InetAddress.getLocalHost
    val b= net.getAddress
    if ( ! net.isLoopbackAddress) {
      _IP = abs( if (b.length == 4) readAsInt(b) else readAsLong(b))
    }
  } catch {
    case e:Throwable => e.printStackTrace()
  }

  /**
   * @return
   */
  def newWWID() =  format

  private def format() = {
    val seed = newRandom().nextInt(Integer.MAX_VALUE)
    val ts= splitHiLoTime()
    ts._1 + formatAsString(_IP) +
      formatAsString(seed) + formatAsString(seqno()) + ts._2
  }

  private def seqno() = SeqNumGen.nextInt

  private def formatAsString(n:Long) = {
    fmt("0000000000000000", n.toHexString)
  }

  private def formatAsString(n:Int) = {
    fmt("00000000", n.toHexString)
  }

  private def fmt(pad:String,mask:String) = {
    val mlen=mask.length
    val plen=pad.length
    if (mlen >= plen) {
      mask.substring(0,plen)
    } else {
      new StringBuilder(pad).replace(plen - mlen, plen,mask).toString
    }
  }

  private def splitHiLoTime(): (String,String) = {
    val s= formatAsString(System.currentTimeMillis)
    val n = s.length
    ( STU.left(s, n/2), STU.right(s, max(0, n - n/2 )))
  }

}
