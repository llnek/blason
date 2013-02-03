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
 * Constants for Signing Algorithms.
 *
 * @author kenl
 *
 */
object SigningAlgo {

  val SHA512= SigningAlgo("SHA512withRSA")
  val SHA256= SigningAlgo("SHA256withRSA")
  val SHA1= SigningAlgo("SHA1withRSA")
  val SHA_512= SigningAlgo("SHA-512")
  val SHA_1= SigningAlgo("SHA-1")
  val SHA_256= SigningAlgo("SHA-256")
  val MD_5= SigningAlgo("MD5")
  val MD5= SigningAlgo("MD5withRSA")

}

sealed case class SigningAlgo private(private val _algo:String) {
  override def toString() = _algo
}

