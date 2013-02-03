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

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

import java.io.{ByteArrayOutputStream=>ByteArrayOS}

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


/**
 * Obfuscation using SUN-java.
 *
 * @author kenl
 *
 */
sealed class JavaCryptor extends BaseCryptor  {

  
  def decrypt(pwd:String,cipherText:String) = decr(pwd,cipherText)
  def decrypt(cipherText:String) = decr( key, cipherText)

  def encrypt(pwd:String,clearText:String) = encr(pwd,clearText)
  def encrypt(clearText:String) = encr(key, clearText)

  private def encr(pwd:String,clearText:String) = {
    if ( STU.isEmpty(clearText)) clearText else {
      val c= getCipher(pwd,Cipher.ENCRYPT_MODE)
      val baos = new ByteArrayOS()
      val p = asBytes(clearText)
      val out= new Array[Byte]( max(4096, c.getOutputSize(p.length)) )
      var n= c.update(p, 0, p.length, out, 0)
      if (n > 0) { baos.write(out, 0, n) }
      n = c.doFinal(out,0)
      if (n > 0) { baos.write(out, 0, n) }
      Base64.encodeBase64URLSafeString(baos.toByteArray)
    }
  }

  private def decr(pwd:String,encoded:String) = {
    if ( STU.isEmpty(encoded)) encoded else {
      val c= getCipher(pwd,Cipher.DECRYPT_MODE)
      val baos = new ByteArrayOS()
      val p = Base64.decodeBase64(encoded)
      val out= new Array[Byte]( max(4096, c.getOutputSize(p.length)) )
      var n= c.update(p, 0, p.length, out, 0)
      if (n > 0) { baos.write(out, 0, n) }
      n = c.doFinal(out,0)
      if (n > 0) { baos.write(out, 0, n) }
      asString(baos.toByteArray)
    }
  }

  private def getCipher(pwd:String,mode:Int) = {
    val spec= new SecretKeySpec( keyAsBits(pwd), algo )
    val c= Cipher.getInstance( algo )
    c.init(mode, spec)
    c
  }

}
