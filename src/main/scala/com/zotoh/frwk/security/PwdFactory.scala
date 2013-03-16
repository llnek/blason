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


/**
 * Creates passwords.
 *
 * @author kenl
 *
 */
object PwdFactory {
  import Password._
  /**
   * Create a Password object from the given text.  If the text is prefixed
   * with "CRYPT:", then it is treated as encrypted content.
   *
   * @param text
   * @return
   * @throws Exception
   */
  def mk(text:String) = {
    if ( text != null && text.startsWith( PWD_PFX)) {
      mkFromCrypto(text)
    } else {
      mkFromText(text)
    }
  }
  
  def encrypt(pwd:String, txt:String) = {
    PWD_PFX + new JasyptCryptor().encrypt(pwd,txt)
  }
  
  def decrypt(pwd:String, blob:String) = {
    val bs= if ( blob != null && blob.startsWith( PWD_PFX)) {
      blob.substring(PWD_PFXLEN)
    }    else {
      blob
    }
    new JasyptCryptor().decrypt(pwd, bs)
  }
  

  /**
   * Make a clone/copy of an existing Password object.
   *
   * @param p
   * @return
   */
  def copy(p:Password) = new Password(p)

  /**
   * Create a new password which is considered strong, i.e,
   * the password will have a mixture of lower/uppercase
   * characters, numbers, and punctuations.
   *
   * @param length the length of the target password.
   * @return the new password in plain-text.
   * @throws Exception
   */
  def mkStrongPassword(length:Int) = mk(PwdFacImpl.createStrong(length))

  /**
   * Create a text string which has random characters.
   *
   * @param length the length of the string.
   * @return a string with random characters(letters).
   */
  def mkRandomText(length:Int) = PwdFacImpl.createRandom(length)

  private def mkFromCrypto(encoded:String) = new Password(encoded, -1)
  private def mkFromText(text:String) = new Password(text)

}
