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

import org.jasypt.util.text.StrongTextEncryptor
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor


object Jasypt {

  def main(args:Array[String]) {
  }

}

/**
 * @author kenl
 */
class JasyptCryptor extends BaseCryptor {

  def decrypt(pwd:String,cipherText:String) = decr(pwd,cipherText)
  def decrypt(cipherText:String) = decr(key ,cipherText)

  def encrypt(pwd:String, clearText:String) = encr(pwd,clearText)
  def encrypt(clearText:String) = encr( key ,clearText)

  private def decr(pwd:String, cipherText:String) = {
    val ec=new StrongTextEncryptor()
    ec.setPassword(pwd)
    ec.decrypt(cipherText)
  }

  private def encr(pwd:String,clearText:String) = {
    val ec=new StrongTextEncryptor()
    ec.setPassword(pwd)
    ec.encrypt(clearText)
  }


}
