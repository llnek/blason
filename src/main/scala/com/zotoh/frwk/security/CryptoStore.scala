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

import scala.collection.JavaConversions._
import scala.collection.mutable


import com.zotoh.frwk.util.SeqNumGen
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.io.{File,FileInputStream,IOException,InputStream}
import java.security.KeyStore._

import java.security.cert.{CertificateFactory,X509Certificate=>XCert}
import java.security.KeyStore

import javax.net.ssl.{KeyManagerFactory,TrustManagerFactory}
import javax.security.auth.x500.X500Principal

import org.slf4j._

object CryptoStore {
  private val _log = LoggerFactory.getLogger(classOf[CryptoStore])
}

/**
 * @author kenl
 *
 */
abstract class CryptoStore {

  import CryptoStore._
  def tlog() = _log

  protected var _store:KeyStore=null
  private var _pwd:String=""

  //Crypto.getInstance()

  /**
   * @param bits
   * @param password
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws IOException
   */
  def addKeyEntity(bits:InputStream, pwd:String) {

    // we load the p12 content into an empty keystore, then extract the entry
    // and insert it into the current one.

    val tmp= createKeyStore()
    val ch= pwd.toCharArray
    tmp.load(bits, ch)
    val pp= new PasswordProtection(ch)
    val key= tmp.getEntry( tmp.aliases().nextElement, pp ).asInstanceOf[PrivateKeyEntry ]
    onNewKey( newAlias, key, pp)
  }

  /**
   * @param bits
   * @throws CertificateException
   * @throws KeyStoreException
   */
  def addCertEntity( bits:InputStream) {
    CertificateFactory.getInstance( "X.509").generateCertificate(bits) match {
      case x:XCert => _store.setCertificateEntry( newAlias , x)
      case _ =>
    }
  }

  /**
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  def trustManagerFactory() = {
    val m= TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm())
    m.init( _store )
    m
  }

  /**
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableKeyException
   * @throws KeyStoreException
   */
  def keyManagerFactory() = {
    var m= KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm())
    m.init( _store,  _pwd.toCharArray )
    m
  }

  /**
   * @return
   * @throws KeyStoreException
   */
  def certAliases() = {
    val rc= mutable.ArrayBuffer[String]()
    val en = _store.aliases
    while (en.hasMoreElements) {
      val alias=en.nextElement
      if ( _store.isCertificateEntry(alias)) {
        rc += alias
      }
    }
    rc.toSeq
  }

  /**
   * @return
   * @throws KeyStoreException
   */
  def keyAliases() = {
    val rc= mutable.ArrayBuffer[String]()
    val en = _store.aliases
    while (en.hasMoreElements) {
      val alias=en.nextElement
      if ( _store.isKeyEntry(alias)) {
        rc += alias
      }
    }
    rc.toSeq
  }


  /**
   * @param alias
   * @param password
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   */
  def keyEntity(alias:String, pwd:String) = {
    _store.getEntry( alias, new PasswordProtection( pwd.toCharArray) ) match {
      case x:PrivateKeyEntry  => Some(x)
      case _ => None
    }
  }

  /**
   * @param alias
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   */
  def certEntity(alias:String) = {
    _store.getEntry( alias, null ) match {
      case x:TrustedCertificateEntry => Some(x)
      case _ => None
    }
  }

  /**
   * @param alias
   * @throws KeyStoreException
   */
  def removeEntity(alias:String) {
    if ( alias != null && _store.containsAlias(alias) ) {
      _store.deleteEntry(alias)
    }
  }

  /**
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   */
  def intermediateCAs(): Seq[XCert] = getCAs(true, false)

  /**
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   */
  def rootCAs(): Seq[XCert] = getCAs(false, true)

  /**
   * returns a list of X509Certificates
   *
   * @return
   * @throws Exception
   */
  def trustedCerts(): Seq[XCert] = {
    val rc = mutable.ArrayBuffer[XCert]()
    val en= _store.aliases
    while (en.hasMoreElements) {
      val alias= en.nextElement
      if (_store.isCertificateEntry(alias)) {
        val obj= _store.getEntry(alias, null)
        obj.asInstanceOf[TrustedCertificateEntry].getTrustedCertificate match {
          case x:XCert => rc += x
          case _ =>
        }
      }
    }
    rc.toSeq
  }

  /**
   * @param file
   * @param password
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  def init(f:File, pwd:String): this.type = {
    if(f != null) using(new FileInputStream(f)) { (inp) =>
      init(inp,nsb(pwd))
    }
    this
  }


  /**
   * @param inp
   * @param password
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  def init( inp:InputStream, pwd:String): this.type = {
    init(pwd)
    _store.load( inp, nsb(pwd).toCharArray )
    this
  }


  /**
   * @param password
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  def init( pwd:String ): this.type = {
    _store= createKeyStore
    _pwd= nsb(pwd)
    _store.load( null, _pwd.toCharArray )
    this
  }

  /**
   * @return
   */
  def getAndDetach() {
    var ks= _store
    _store=null
    ks
  }

  /**
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   */
  protected def createKeyStore(): KeyStore

  private def getCAs( tca:Boolean, root:Boolean) = {
    val rc= mutable.ArrayBuffer[XCert]()
    val en = _store.aliases
    while (en.hasMoreElements ) {
      val alias= en.nextElement
      if ( _store.isCertificateEntry(alias)) {
        _store.getEntry(alias, null).asInstanceOf[TrustedCertificateEntry].getTrustedCertificate match {
          case c:XCert =>
            val issuer = c.getIssuerX500Principal
            val subj = c.getSubjectX500Principal
            val matched = issuer != null && issuer == subj
            var x=c
            if (root && !matched) { x= null }
            if (tca && matched) { x= null }
            if (x != null) rc += x
          case _ =>
        }
      }
    }
    rc.toSeq
  }

  /**
   * @return
   */
  protected def newAlias() =  {
    "" + System.currentTimeMillis() + SeqNumGen.next()
  }

  /**
   * @param alias
   * @param key
   * @param param
   * @throws KeyStoreException
   */
  private def onNewKey( alias:String, key:PrivateKeyEntry, pm:ProtectionParameter) {
    key.getCertificateChain.foreach { (c) =>
      _store.setCertificateEntry( newAlias, c)
    }
    _store.setEntry( alias, key, pm )
  }

}
