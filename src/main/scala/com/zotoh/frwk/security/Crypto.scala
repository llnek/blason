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

import java.io.{ByteArrayInputStream => ByteArrayIS}
import java.io.{ByteArrayOutputStream => ByteArrayOS}
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.KeyStore.PrivateKeyEntry
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.{Certificate => JCert}
import java.security.cert.{X509Certificate => XCert}
import java.util.{Date => JDate}
import java.util.Random
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.math._
import org.bouncycastle.asn1.x509.X509Extension
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSSignedGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMReader
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.DigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.slf4j._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.util.CoreUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal
import org.apache.commons.codec.binary.Base64



sealed class SigningStyle() {}
case object EXPLICIT extends SigningStyle
case object IMPLICIT extends SigningStyle

sealed class CertFormat() {}
case object DER extends CertFormat
case object PEM extends CertFormat

/**
 * @author kenl
 *
 */
object Crypto {

  private val _log= LoggerFactory.getLogger(classOf[Crypto])
  def tlog() = _log
  private var _prov:Provider =new BouncyCastleProvider()

  private val DEF_ALGO= "SHA1WithRSAEncryption"
  private val BFISH="BlowFish"

  private val PKCS12= "PKCS12"
  private val JKS= "JKS"
  private val SHA1= "SHA1"
  private val MD5= "MD5"
  private val AES256_CBC = "AES256_CBC"
  private val RAS = "RAS"
  private val DES = "DES"
  private var _jceTested=false

  Security.addProvider( _prov)
  maybe_assert_jce()

  
  def genMAC( key:Array[Byte], data:String, algo:String = "HmacSHA512") = {
    val mac= Mac.getInstance(algo, Crypto.provider )
    mac.init(new SecretKeySpec(key, algo) )
    mac.update( data.getBytes("utf-8"))    
    bytesToHexString(mac.doFinal )
  }
  
  def genHash(data:String, algo:String= "SHA-512") {  
    val dig = MessageDigest.getInstance( algo)
    val b= dig.digest( data.getBytes("utf-8"))
    Base64.encodeBase64String(b)
  }  
  
  /**
   * @param keyLength
   * @param dnStr
   * @param fmt
   * @return
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws NoSuchProviderException
   * @throws OperatorCreationException
   * @throws SignatureException
   * @throws IOException
   */
  def mkCSR( keyLength:Int, dnStr:String, fmt:CertFormat) = {

    tlog.debug("{}: mkCSR: dnStr= {}, key-len= {}", "Crypto", dnStr, asJObj(keyLength))

    val kp= mkKeyPair("RSA", keyLength)
    val k= kp.getPrivate
    val cs=new JcaContentSignerBuilder(DEF_ALGO).setProvider( bouncyCastle).build(k)

      /*
      new PKCS10CertificationRequest(  DEF_ALGO, new X500Principal(dnStr),
        kp.getPublic(), null, k )
    .getEncoded();
    */

    var bits =new JcaPKCS10CertificationRequestBuilder(new X500Principal(dnStr),kp.getPublic).build(cs).getEncoded
    fmt match {
      case PEM =>
        bits= fmtPEM("-----BEGIN CERTIFICATE REQUEST-----\n",
          "\n-----END CERTIFICATE REQUEST-----\n", bits)
      case _ =>
    }

    (bits, asBytes(k, fmt))
  }

  /**
   * @param key
   * @param fmt
   * @return
   * @throws IOException
   */
  def asBytes(key:PrivateKey, fmt:CertFormat) = {
    var bits= key.getEncoded
    fmt match {
      case PEM =>
        bits= fmtPEM("-----BEGIN RSA PRIVATE KEY-----\n",
          "\n-----END RSA PRIVATE KEY-----\n", bits)
      case _ =>
    }
    bits
  }

  /**
   * @param cert
   * @param fmt
   * @return
   * @throws CertificateEncodingException
   * @throws IOException
   */
  def asBytes(cert:XCert, fmt:CertFormat) = {
    var bits= cert.getEncoded
    fmt match {
      case PEM =>
        bits= fmtPEM("-----BEGIN CERTIFICATE-----\n",
          "-----END CERTIFICATE-----\n", bits)
      case _ =>
    }
    bits
  }


  /**
   * @param friendlyName
   * @param keyPEM
   * @param certPEM
   * @param pwd
   * @param out
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   * @throws UnrecoverableEntryException
   * @throws InvalidKeySpecException
   */
  def mkPKCS12(friendlyName:String, keyPEM:Array[Byte], certPEM:Array[Byte],
    pwd:String, out:File) {

    val ct= CryptoUtils.bitsToCert(certPEM) match {
      case Some(c) => c.getTrustedCertificate
      case _ => null
    }
    val rdr=new InputStreamReader( new ByteArrayIS(keyPEM))
    val ss= new PKCSStore().createKeyStore
    val kp = new PEMReader(rdr).readObject().asInstanceOf[KeyPair]
    ss.setKeyEntry(friendlyName, kp.getPrivate, pwd.toCharArray, Array(ct))
    val baos= new ByteArrayOS()
    ss.store(baos, pwd.toCharArray)
    IOUtils.writeFile(out, baos.toByteArray)
  }

  /**
   * Create a ROOT CA  PKCS-12 file.
   *
   * @param friendlyName
   * @param start
   * @param end
   * @param dnStr
   * @param password
   * @param keyLength
   * @param out
   * @throws InvalidKeyException
   * @throws IllegalStateException
   * @throws NoSuchAlgorithmException
   * @throws SignatureException
   * @throws CertificateException
   * @throws NoSuchProviderException
   * @throws GeneralSecurityException
   * @throws KeyStoreException
   * @throws IOException
   */
  def mkSSV1PKCS12(friendlyName:String, start:JDate, end:JDate,
    dnStr:String, pwd:String, keyLength:Int, out:File) {

    val ks= KeyStore.getInstance(PKCS12, provider)
    ks.load(null, null)
    val kp= mkKeyPair("RSA", keyLength)
    mk_SSV1(ks, kp, DEF_ALGO, friendlyName, start,end,dnStr,pwd,keyLength, out)
  }

  /**
   * Create a ROOT CA  JKS file.
   *
   * @param friendlyName
   * @param start
   * @param end
   * @param dnStr
   * @param password
   * @param keyLength
   * @param out
   * @throws InvalidKeyException
   * @throws IllegalStateException
   * @throws NoSuchAlgorithmException
   * @throws SignatureException
   * @throws CertificateException
   * @throws NoSuchProviderException
   * @throws GeneralSecurityException
   * @throws KeyStoreException
   * @throws IOException
   */
  def mkSSV1JKS(friendlyName:String, start:JDate, end:JDate, dnStr:String, pwd:String,
      keyLength:Int, out:File) {
    val ks= KeyStore.getInstance(JKS, "SUN")
    ks.load(null, null)
    val kp= mkKeyPair("DSA", keyLength)
    mk_SSV1(ks, kp, "SHA1withDSA", friendlyName, start,end, dnStr,pwd,keyLength, out)
  }

  private def mk_SSV1(ks:KeyStore, kp:KeyPair, algo:String, friendlyName:String,
    start:JDate, end:JDate, dnStr:String, pwd:String,
      keyLength:Int, out:File) {

    tlog.debug("{}:createSSV1: dn={}, key-len={}", "Crypto", dnStr, asJObj(keyLength))

    val props= mkSSV1Cert(ks.getProvider, kp, start, end, dnStr, keyLength, algo)
    val ca= pwd.toCharArray
    val baos= new ByteArrayOS()

    ks.setKeyEntry(friendlyName, props._2, ca, Array(props._1))
    ks.store(baos, ca)
    writeFile(out, baos.toByteArray)
  }

  /**
   * Create a server PKCS12 file.
   *
   * @param friendlyName
   * @param start
   * @param end
   * @param dnStr
   * @param password
   * @param keyLength
   * @param issuerCerts
   * @param issuerKey
   * @param out
   * @throws InvalidKeyException
   * @throws IllegalStateException
   * @throws NoSuchAlgorithmException
   * @throws GeneralSecurityException
   * @throws SignatureException
   * @throws CertificateException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws IOException
   */
  def mkSSV3PKCS12(friendlyName:String, start:JDate, end:JDate, dnStr:String,
      pwd:String, keyLength:Int, issuerCerts:Array[JCert],
      issuerKey:PrivateKey, out:File) {
    var ks= KeyStore.getInstance(PKCS12, provider)
    ks.load(null, null)
    var kp= mkKeyPair("RSA", keyLength)
    mk_SSV3(ks, kp, DEF_ALGO, friendlyName,start,end,dnStr,pwd,keyLength,issuerCerts,issuerKey,out)
  }


  /**
   * @param friendlyName
   * @param start
   * @param end
   * @param dnStr
   * @param password
   * @param keyLength
   * @param issuerCerts
   * @param issuerKey
   * @param out
   * @throws InvalidKeyException
   * @throws IllegalStateException
   * @throws NoSuchAlgorithmException
   * @throws GeneralSecurityException
   * @throws SignatureException
   * @throws CertificateException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws IOException
   */
  def mkSSV3JKS(friendlyName:String, start:JDate, end:JDate, dnStr:String,
      pwd:String, keyLength:Int,
      issuerCerts:Array[JCert], issuerKey:PrivateKey, out:File) {
    val ks= KeyStore.getInstance(JKS, "SUN")
    ks.load(null, null)
    val kp= mkKeyPair("DSA", keyLength)
    mk_SSV3(ks, kp, "SHA1withDSA", friendlyName,start,end,dnStr,pwd,keyLength,issuerCerts,issuerKey,out)
  }

  private def mk_SSV3(ks:KeyStore, kp:KeyPair,algo:String,
    friendlyName:String, start:JDate, end:JDate, dnStr:String,
      pwd:String, keyLength:Int, issuerCerts:Array[JCert] ,
      issuerKey:PrivateKey , out:File) {

    tlog.debug("{}:createSSV3: dn={}, key-len={}", "Crypto", dnStr, asJObj(keyLength))

    val props= mkSSV3Cert(ks.getProvider, kp, start, end, dnStr, issuerCerts(0), issuerKey, keyLength, algo)
    val ca= pwd.toCharArray
    val baos= new ByteArrayOS()
    var cs=mutable.ArrayBuffer[JCert]()
    cs += props._1
    issuerCerts.foreach{ (t) => cs += t }

    ks.setKeyEntry(friendlyName, props._2, ca,  cs.toArray)
    ks.store(baos, ca)
    writeFile(out, baos.toByteArray)
  }

  /**
   * From the given PKCS12 file, generate a corresponding PKCS7 file.
   *
   * @param p12File
   * @param password
   * @param fileOut
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   * @throws CertificateException
   * @throws IOException
   * @throws InvalidAlgorithmParameterException
   * @throws CertStoreException
   * @throws GeneralSecurityException
   */
  def exportPKCS7( p12File:File, pwd:String, fileOut:File) {

    val key = loadPKCS12Key(p12File, pwd)
    val cc= key.getCertificateChain
    val cl= mutable.LinkedList[JCert]()
    cc.foreach { (a) => cl :+ a }
    val cp= new JcaDigestCalculatorProviderBuilder().setProvider(provider).build()
    val bdr = new JcaSignerInfoGeneratorBuilder(cp)
//    "SHA1withRSA"
    val cs= new JcaContentSignerBuilder(CMSSignedGenerator.DIGEST_SHA512).
      setProvider(provider).build(key.getPrivateKey)
    val gen = new CMSSignedDataGenerator()

    gen.addSignerInfoGenerator( bdr.build(cs, cc(0).asInstanceOf[XCert] ))
    gen.addCertificates(new JcaCertStore(cl))
    var bits = gen.generate( CMSSignedGenerator.DATA,
        new CMSProcessableByteArray("Hello".getBytes()), false, provider, false).getEncoded
    writeFile(fileOut, bits)
  }

  /**
   * @return
   */
  def provider(): Provider = maybe_assert_jce()

  /**
   * MD5, SHA-1, SHA-256, SHA-384, SHA-512.
   *
   * @param algo
   * @return
   * @throws NoSuchAlgorithmException
   */
  def newDigestInstance(algo:SigningAlgo) = CryptoUtils.newMsgDigest(algo.toString())

  /**
   * @return
   * @throws NoSuchAlgorithmException
   */
  def secureRandom(): SecureRandom = SecureRandom.getInstance( "SHA1PRNG" )

  /**
   * @return
   */
  /**
   * @return
   */
  def bouncyCastle(): Provider = Security.getProvider("BC")

  /**
   * @return
   */
  def aten(): Provider = Security.getProvider("SUN")

  /**
   * @return
   */
  def useBouncyCastle() {
    maybe_assert_jce
    _prov match {
      case x:BouncyCastleProvider =>
      case _ => _prov=Security.getProvider("BC")
    }
  }

  /**
   * @return
   */
  def useAten() {
    _prov= Security.getProvider("SUN")
  }

  private def loadPKCS12Key( p12File:File, pwd:String ): KeyStore.PrivateKeyEntry = {
    using(new FileInputStream(p12File)) { (inp) =>
      val ks= KeyStore.getInstance(PKCS12, provider )
      val ca= pwd.toCharArray
      ks.load(inp, ca)
      ks.getEntry( ks.aliases().nextElement, new PasswordProtection(ca)) match {
        case x:PrivateKeyEntry => x
        case _ => null
      }
    }
  }

  private def mkKeyPair( algo:String, keyLength:Int) = {
    val kpg  = KeyPairGenerator.getInstance(algo, provider )
    kpg.initialize( keyLength, secureRandom )
    kpg.generateKeyPair
  }

  private def mkSSV1Cert(pv:Provider, kp:KeyPair, start:JDate, end:JDate,
    dnStr:String, keyLength:Int, algo:String) = {
    // generate self-signed cert
    val dnName = new X500Principal(dnStr)
    val prv= kp.getPrivate
    val pub= kp.getPublic
    // self signed-> issuer is self
    val bdr= new JcaX509v1CertificateBuilder(dnName, nextSerialNumber,
        start,end, dnName, pub)
    val cs= new JcaContentSignerBuilder(algo).setProvider(pv).build(prv)
    val cert=new JcaX509CertificateConverter().setProvider(pv).getCertificate(bdr.build( cs))
    cert.checkValidity(new JDate)
    cert.verify(pub)
    (cert, prv)
  }

  private def mkSSV3Cert(pv:Provider, kp:KeyPair, start:JDate, end:JDate,
    dnStr:String, issuer:JCert,  issuerKey:PrivateKey , keyLength:Int, algo:String) = {

    val subject= new X500Principal(dnStr)
    val top= issuer.asInstanceOf[XCert]
    val prv= kp.getPrivate
    val pub= kp.getPublic
    val bdr= new JcaX509v3CertificateBuilder(top, nextSerialNumber, start,end, subject, pub)
    val exUte=new JcaX509ExtensionUtils()
    val cs=new JcaContentSignerBuilder(algo).setProvider(pv).build(issuerKey)
    bdr.addExtension(X509Extension.authorityKeyIdentifier, false,
      exUte.createAuthorityKeyIdentifier(top))
    bdr.addExtension(X509Extension.subjectKeyIdentifier, false,
      exUte.createSubjectKeyIdentifier(pub))

    val cert=new JcaX509CertificateConverter().setProvider(pv).getCertificate(bdr.build( cs))
    cert.checkValidity(new JDate)
    cert.verify(top.getPublicKey)

    (cert,prv)
  }

  /**
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  def testJCEPolicy() {
    // this function should fail if the non-restricted (unlimited-strength) jce files are not placed in jre-home
    val kgen = KeyGenerator.getInstance(BFISH); kgen.init(256)
    val cipher = Cipher.getInstance( BFISH)
    cipher.init(Cipher.ENCRYPT_MODE,
      new SecretKeySpec(kgen.generateKey().getEncoded, BFISH))
    cipher.doFinal("This is just an example".getBytes())
  }

  private def nextSerialNumber() = {
    val r= new Random(new JDate().getTime)
    BigInteger.valueOf( abs(r.nextLong) )
  }

  private def fmtPEM(top:String, end:String, bits:Array[Byte]) = {
    val baos= new ByteArrayOS(); baos.write( CoreUtils.asBytes(top))
    val bs=Base64.encodeBase64(bits)
    val bb= Array[Byte](1)
    var pos=0
    for ( i <- 0 until bs.length) {
      if (pos > 0 && (pos % 64) == 0) {
        baos.write( CoreUtils.asBytes("\n"))
      }
      pos += 1
      bb(0)=bs(i)
      baos.write(bb)
    }
    baos.write( CoreUtils.asBytes(end))
    baos.toByteArray
  }

  private def maybe_assert_jce(): Provider = {

    if (!_jceTested) try {
      testJCEPolicy
    } catch {
      case e:Throwable =>
        System.err.println("JCE errors, probably due to jce policy not configured to be unlimited\n" +
      "Download the unlimited jce policy files, and place them in JRE_HOME")
      sys.exit(-99)
    } finally {
      _jceTested=true
    }
    _prov
  }

  private def main(args:Array[String]) = {
    try {
      // test code
      var ks= KeyStore.getInstance("PKCS12", new BouncyCastleProvider())
      ks.load(CoreUtils.rc2Stream("com/zotoh/crypto/zotoh.p12"), "Password1".toCharArray)
      val nm= ks.aliases().nextElement
      ks.getEntry( nm, new PasswordProtection("Password1".toCharArray)) match {
        case k:PrivateKeyEntry =>
          ks= KeyStore.getInstance("JKS")
          ks.load(null, null)
          ks.setKeyEntry(nm, k.getPrivateKey, "Password1".toCharArray, k.getCertificateChain)
          ks.store(new FileOutputStream("w:/zotoh.jks"), "Password1".toCharArray)
      }
    } catch {
      case t:Throwable => t.printStackTrace()
    }
  }

}

sealed class Crypto {}
