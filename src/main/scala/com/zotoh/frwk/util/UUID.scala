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

import java.security.SecureRandom

import com.zotoh.frwk.util.CoreUtils._


/**
 * @author kenl
 *
 */
object UUID {

  private val CHARS= "0123456789AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz".toCharArray()
  //private val CHARS= "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()
  private val WIDTH=36

  /**
   * rfc4122, version 4 form.

   * @return
   */
  def newUUID() = {
    // At i==19 set the high bits of clock sequence as per rfc4122, sec. 4.1.5
    val rc=new Array[Char](WIDTH)
    val rnd = newRandom()
    for (i <- 0 until WIDTH) {
      rc(i) = i match {
        case 8 | 13 | 18 | 23 => '-'
        case 14 => '4'
        case _ =>
          val r = 0 | ( rnd.nextDouble() * 16).toInt
          CHARS( if (i == 19) ((r & 0x3) | 0x8) else (r & 0xf) )
      }
    }
    new String(rc)
  }

  private def generate(length:Int, radix:Int) = {
    val rc=new Array[Char](length)
    val rnd = newRandom()

    for (i <- 0 until length) {
      rc(i) = CHARS(0 | (rnd.nextDouble() * radix).toInt )
    }

    new String(rc)
  }

}

