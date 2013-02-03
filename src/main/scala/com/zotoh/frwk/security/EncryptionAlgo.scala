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

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.cms.CMSAlgorithm


sealed case class EncryptionAlgo private(private val _algo:ASN1ObjectIdentifier ) {
  def oid(): ASN1ObjectIdentifier  = _algo
}

/**
 * Encryption algo constants.
 *
 * @author kenl
 *
 */
object EncryptionAlgo {

  val DES_EDE3_CBC = EncryptionAlgo(CMSAlgorithm.DES_EDE3_CBC)
  val RC2_CBC = EncryptionAlgo(CMSAlgorithm.RC2_CBC)
  val IDEA_CBC = EncryptionAlgo(CMSAlgorithm.IDEA_CBC)
  val CAST5_CBC = EncryptionAlgo(CMSAlgorithm.CAST5_CBC)

  val AES128_CBC = EncryptionAlgo(CMSAlgorithm.AES128_CBC)
  val AES192_CBC = EncryptionAlgo(CMSAlgorithm.AES192_CBC)
  val AES256_CBC = EncryptionAlgo(CMSAlgorithm.AES256_CBC)

  val CAMELLIA128_CBC = EncryptionAlgo(CMSAlgorithm.CAMELLIA128_CBC)
  val CAMELLIA192_CBC = EncryptionAlgo(CMSAlgorithm.CAMELLIA192_CBC)
  val CAMELLIA256_CBC = EncryptionAlgo(CMSAlgorithm.CAMELLIA256_CBC)

  val SEED_CBC = EncryptionAlgo(CMSAlgorithm.SEED_CBC)

  val DES_EDE3_WRAP = EncryptionAlgo(CMSAlgorithm.DES_EDE3_WRAP)
  val AES128_WRAP = EncryptionAlgo(CMSAlgorithm.AES128_WRAP)
  val AES256_WRAP = EncryptionAlgo(CMSAlgorithm.AES256_WRAP)
  val CAMELLIA128_WRAP = EncryptionAlgo(CMSAlgorithm.CAMELLIA128_WRAP)
  val CAMELLIA192_WRAP = EncryptionAlgo(CMSAlgorithm.CAMELLIA192_WRAP)
  val CAMELLIA256_WRAP = EncryptionAlgo(CMSAlgorithm.CAMELLIA256_WRAP)
  val SEED_WRAP = EncryptionAlgo(CMSAlgorithm.SEED_WRAP)

  val ECDH_SHA1KDF = EncryptionAlgo(CMSAlgorithm.ECDH_SHA1KDF)

  //def apply(a:ASN1ObjectIdentifier) = new EncryptionAlgo(a)

}
