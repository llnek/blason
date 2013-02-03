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
package security


import org.apache.commons.lang3.{StringUtils=>STU}
import scala.math._
import com.zotoh.frwk.util.StrUtils._


/**
 * @author kenl
 *
 */
sealed class CaesarCipher(private val _shift:Int) {

  private val ALPHA_CHS= 26

  /**
   * Positive =&gt; right shift, Negative =&gt; left shift.
   *
   * @param shift
   */

  /**
   * @param txt
   * @return
   */
  def encode(txt:String) = {
    if (_shift==0 || STU.isEmpty(txt)) txt else {
      val delta= abs(_shift) % ALPHA_CHS
      val ca=txt.toCharArray
      val out= ca.clone
      //val out = java.util.Arrays.copyOf(ca, ca.length)
      (0 /: ca) { (i, ch) =>
        ch match {
          case c if (c >= 'A' && c <= 'Z') =>
            out(i) =shift_enc(delta, c, 'A', 'Z')
          case c if (c >= 'a' && c <= 'z') =>
            out(i)=shift_enc(delta, c, 'a', 'z')
          case _ => // others stay the same, continue
        }
        i+1
      }
      new String(out)
    }
  }

  private def shift_enc(delta:Int, c:Char, head:Char, tail:Char) = {
    if (_shift > 0) {
      shiftRight(ALPHA_CHS, delta, c.toInt, head.toInt, tail.toInt )
    } else {
      shiftLeft(ALPHA_CHS, delta, c.toInt, head.toInt, tail.toInt )
    }
  }

  private def shift_dec(delta:Int, c:Char, head:Char, tail:Char) = {
    if (_shift < 0) {
      shiftRight(ALPHA_CHS, delta, c.toInt, head.toInt,  tail.toInt )
    } else {
      shiftLeft(ALPHA_CHS, delta,  c.toInt,  head.toInt,  tail.toInt )
    }
  }

  private def shiftRight(width:Int, delta:Int, c:Int, head:Int, tail:Int) = {
    var ch = c + delta
    if (ch > tail) {
      ch = ch - width
    }
    ch.toChar
  }

  private def shiftLeft(width:Int, delta:Int, c:Int, head:Int, tail:Int) = {
    var ch = c - delta
    if (ch < head) {
      ch = ch + width
    }
    ch.toChar
  }

  /**
   * @param crypt
   * @return
   */
  def decode(crypt:String) = {
    if (_shift==0 || STU.isEmpty(crypt)) crypt else {
      val delta= abs(_shift) % ALPHA_CHS
      val ca=crypt.toCharArray
      val out=ca.clone
      //val out = java.util.Arrays.copyOf(ca, ca.length)
      (0 /: ca) { (i, ch) =>
        ch match {
          case c if (c >= 'A' && c <= 'Z') =>
            out(i)= shift_dec(delta, c, 'A', 'Z')
          case c if (c >= 'a' && c <= 'z') =>
            out(i)= shift_dec(delta, c, 'a', 'z')
          case _ =>  // others stay the same, continue
        }
        i + 1
      }
      new String(out)
    }
  }

}
