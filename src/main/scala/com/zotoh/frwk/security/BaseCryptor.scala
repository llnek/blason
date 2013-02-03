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

import org.apache.commons.codec.binary.Base64
import java.util.Arrays
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._


object BaseCryptor {

  private val KEY= "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"
  private val T3_DES= "TripleDES" //AES/ECB/PKCS5Padding/TripleDES
  private val ALGO= T3_DES // default javax supports this
  private var _key= ""

  setKey(KEY)

  /**
   * Set the encryption key for future obfuscation operations.  Typically this is
   * called once only at the start of the main application.
   *
   * @param key
   */
  def setKey(key:String) {
    var len= asBytes(key, "utf-8").length
    if (T3_DES == ALGO ) {
      if (len < 24) {
        errBadArg("Encryption key length must be 24, using TripleDES")
      }
    }
    _key=key
  }

  private val _log= LoggerFactory.getLogger(classOf[BaseCryptor])
}

/**
 * @author kenl
 *
 */
abstract class BaseCryptor protected() {

  import BaseCryptor._

  def tlog() = _log

  /**
   * Decrypt the given text.
   *
   * @param ciperText
   * @return
   */
  def decrypt(pwd:String,ciperText:String): String
  def decrypt(ciperText:String): String

  /**
   * Encrypt the given text string.
   *
   * @param clearText
   * @return
   */
  def encrypt(pwd:String,clearext:String): String
  def encrypt(clearText:String): String

  /**
   * @return
   */
  protected def algo() = ALGO

  /**
   * @return
   */
  protected def key() = _key

  protected def keyAsBits(pwd:String) = {
    var bits= asBytes(pwd, "utf-8")
    if (T3_DES == ALGO && bits.length > 24) {
      // len must be 24
      bits.slice(0,24)
    } else {
      bits
    }
  }

}
