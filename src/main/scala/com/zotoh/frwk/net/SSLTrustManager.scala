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
package net

import com.zotoh.frwk.security.CryptoStore
import com.zotoh.frwk.util.CoreUtils._
import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManager
import java.io.IOException
import java.security.cert.{X509Certificate=>XCert}
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException



/**
 * @author kenl
 *
 */
class SSLTrustManager extends X509TrustManager {

  private var _def:X509TrustManager = null

  def this(cs:CryptoStore) {
    this()
    iniz(cs)
  }

  def checkClientTrusted(chain:Array[XCert], authType:String) {

    if ( ! isNilSeq(chain)) try {
      _def.checkClientTrusted(chain, authType)
    } catch {
      case e:Throwable =>
    }
  }

  def checkServerTrusted(chain:Array[XCert], authType:String) {
    if ( !isNilSeq(chain)) try {
      _def.checkClientTrusted(chain, authType)
    } catch {
      case e:Throwable =>
    }
  }


  def getAcceptedIssuers()  = _def.getAcceptedIssuers

  private def iniz(cs:CryptoStore) {

    val tms= cs.trustManagerFactory().getTrustManagers
    if ( ! isNilSeq(tms)) {
      for {
        i <- 0 until tms.length
        if (_def == null)
      } {
        tms(i) match {
          case x:X509TrustManager => _def =x
          case _ =>
        }
      }
    }

    if (_def==null) throw new IOException("No SSL TrustManager available")
  }

}
