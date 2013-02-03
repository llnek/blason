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
import scala.collection.JavaConversions._
import scala.collection.mutable

import com.zotoh.frwk.mime.MimeUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.CoreImplicits

import java.io.{IOException,InputStream,PrintStream}
import java.security._
import java.security.KeyStore._
import java.security.cert.{Certificate=>JCert,X509Certificate=>XCert}

import java.util.{Date=>JDate}

import javax.activation.CommandMap
import javax.activation.DataHandler
import javax.activation.MailcapCommandMap
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.ContentType
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility
import javax.security.auth.x500.X500Principal

import org.apache.commons.mail.DefaultAuthenticator
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute
import org.bouncycastle.asn1.smime.SMIMECapability
import org.bouncycastle.asn1.smime.SMIMECapabilityVector
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.{X509CertificateHolder=>XCertHdr}
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSCompressedDataParser
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSProcessable
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSProcessableFile
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.CMSTypedStream
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator
import org.bouncycastle.cms.Recipient
import org.bouncycastle.cms.RecipientInfoGenerator
import org.bouncycastle.cms.RecipientInformation
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.cms.jcajce.ZlibExpanderProvider
import org.bouncycastle.mail.smime.SMIMECompressedGenerator
import org.bouncycastle.mail.smime.SMIMEEnveloped
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator
import org.bouncycastle.mail.smime.SMIMEException
import org.bouncycastle.mail.smime.SMIMESigned
import org.bouncycastle.mail.smime.SMIMESignedGenerator
import org.bouncycastle.mail.smime.SMIMESignedParser
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.util.Store
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import javax.security.auth.x500.X500Principal
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId
import org.bouncycastle.mail.smime.SMIMEEnvelopedParser

import org.slf4j._



/**
 * Helper functions related to Crypto.
 *
 * @author kenl
 *
 */
object CryptoUtils extends CoreImplicits {

  private val _log = LoggerFactory.getLogger(classOf[CryptoUtils])
  def tlog() = _log

  inizMapCap()

  /**
   * @param inp
   * @return
   * @throws MessagingException
   */
  def newMimeMsg(inp:InputStream) = new MimeMessage(newSession(), inp)

  /**
   * @param user
   * @param pwd
   * @return
   */
  def newMimeMsg(user:String, pwd:String) = new MimeMessage(newSession(user, pwd))

  /**
   * @return
   */
  def newMimeMsg() = new MimeMessage(newSession())

  /**
   * @param user
   * @param pwd
   * @return
   */
  def newSession(user:String, pwd:String) = {
    Session.getInstance( System.getProperties(),
        if (STU.isEmpty(user)) null else new DefaultAuthenticator(user, pwd) )
  }

  /**
   * @return
   */
  def newSession():Session = newSession("","")

  /**
   * @param obj
   * @return
   * @throws Exception
   */
  def isSigned(obj:Any): Boolean = {
    val inp= MimeUtils.maybeAsStream(obj)
    if (inp==null) obj match {
      case mp:Multipart => return MimeUtils.isSigned( mp.getContentType)
      case _ => throw new IOException("Invalid content: " + safeGetClzname(obj))
    }
    //else
    try {
      isSigned(newMimeMsg(inp).getContentType)
    } finally {
      safeReset(inp)
    }
  }

  /**
   * @param obj
   * @return
   * @throws Exception
   */
  def isCompressed(obj:Any): Boolean = {
    val inp= MimeUtils.maybeAsStream(obj)
    if (inp==null) obj match {
      case mp:Multipart => return MimeUtils.isCompressed(mp.getContentType)
      case bp:BodyPart => return MimeUtils.isCompressed(bp.getContentType)
      case _ => throw new IOException("Invalid content: " + safeGetClzname(obj))
    }
    // else
    try {
      MimeUtils.isCompressed(newMimeMsg(inp).getContentType)
    } finally {
      safeReset(inp)
    }
  }

  /**
   * @param obj
   * @return
   * @throws Exception
   */
  def isEncrypted(obj:Any): Boolean = {
    val inp= MimeUtils.maybeAsStream(obj)
    if (inp==null) obj match {
      case mp:Multipart => return MimeUtils.isEncrypted( mp.getContentType)
      case bp:BodyPart => return MimeUtils.isEncrypted( bp.getContentType)
      case _ => throw new IOException("Invalid content: " + safeGetClzname(obj))
    }
    // else
    try {
      MimeUtils.isEncrypted(newMimeMsg(inp).getContentType)
    } finally {
      safeReset(inp)
    }
  }

  /**
   * @param cType
   * @param deFcs
   * @return
   */
  def charSet(cType:String, deFcs:String): String = {
    val cs = charSet(cType)
    if (STU.isEmpty(cs)) MimeUtility.javaCharset(deFcs) else cs
  }

  /**
   * @param cType
   * @return
   */
  def charSet(cType:String): String = {
    var rc=""
    if ( ! STU.isEmpty(cType)) try {
      rc= MimeUtility.javaCharset( (new ContentType(cType)).getParameter("charset") )
    } catch {
      case e:Throwable => tlog.warn("", e)
    }
    rc
  }

  /**
   * @param key
   * @param certs
   * @param algo
   * @param mp
   * @return
   * @throws NoSuchAlgorithmException
   * @throws CertStoreException
   * @throws InvalidAlgorithmParameterException
   * @throws MessagingException
   * @throws CertificateEncodingException
   * @throws GeneralSecurityException
   */
  def smimeDigSig(key:PrivateKey, certs:Array[JCert], algo:SigningAlgo, mp:Multipart) = {

    var mm= newMimeMsg()
    mm.setContent(mp)
    val rc= mkSignerGentor(key, certs, algo).generate(mm, Crypto.provider)
/*
    MimeBodyPart dummy= new MimeBodyPart()
    dummy.setContent(mp)
    mp= gen.generate(dummy, PROV)
*/
    rc
  }

  /**
   * @param key
   * @param certs
   * @param algo
   * @param bp
   * @return
   * @throws NoSuchAlgorithmException
   * @throws CertStoreException
   * @throws InvalidAlgorithmParameterException
   * @throws CertificateEncodingException
   * @throws GeneralSecurityException
   */
  def smimeDigSig(key:PrivateKey, certs:Array[JCert], algo:SigningAlgo, bp:BodyPart) = {
    mkSignerGentor(key, certs, algo).generate(
        bp.asInstanceOf[MimeBodyPart], Crypto.provider)
  }

  /**
   * @param key
   * @param part
   * @return
   * @throws MessagingException
   * @throws GeneralSecurityException
   * @throws IOException
   */
  def smimeDecrypt(key:PrivateKey, part:BodyPart) = {
    val cms= smime_decrypt(key, new SMIMEEnveloped( part.asInstanceOf[MimeBodyPart]) )
    if (cms == null) {
      throw new GeneralSecurityException("No matching decryption key")
    }
    //else
    readBytes(cms.getContentStream)
  }

  private def smime_decrypt(key:PrivateKey, env:SMIMEEnveloped): CMSTypedStream = {
    //var  recId = new JceKeyTransRecipientId(cert.asInstanceOf[XCert])
    val rec= new JceKeyTransEnvelopedRecipient(key).setProvider(Crypto.provider)
    val it= env.getRecipientInfos().getRecipients().iterator
    var rc:CMSTypedStream = null
    while ( rc == null && it.hasNext()) {
      rc = it.next().asInstanceOf[RecipientInformation].getContentStream(rec)
    }
    rc
  }

  /**
   * @param keys
   * @param msg
   * @return
   * @throws GeneralSecurityException
   * @throws MessagingException
   * @throws IOException
   */
  def smimeDecryptAsStream(keys:Seq[PrivateKey], msg:MimeMessage): XData = {
    val env=new SMIMEEnveloped(msg)
    var rc:XData=null

    keys.find { (k) =>
      val cms=smime_decrypt(k,env)
      if (cms == null) false else {
        rc= readBytes(cms.getContentStream)
        true
      }
    }
    if (rc==null) {
      throw new GeneralSecurityException("No matching decryption key")
    } else {
      rc
    }
  }

  /**
   * @param mp
   * @return
   * @throws IOException
   * @throws MessagingException
   * @throws GeneralSecurityException
   */
  def peekSmimeSignedContent(mp:Multipart): Any = {
    //tstArgIsType("mulitpart", mp, classOf[MimeMultipart])
    new SMIMESignedParser(new BcDigestCalculatorProvider(),
      mp.asInstanceOf[MimeMultipart],
      charSet( mp.getContentType(), "binary")).getContent().getContent
  }


  /**
   * @param mp
   * @param certs
   * @param cte
   * @return
   * @throws MessagingException
   * @throws GeneralSecurityException
   * @throws IOException
   * @throws CertificateEncodingException
   */
  def verifySmimeDigSig( mp:Multipart, certs:Array[JCert], cte:String=""): (Any,Array[Byte]) = {

    //tstArgIsType("multipart", mp, classOf[MimeMultipart])
    val mmp= mp.asInstanceOf[MimeMultipart]
    val sc= if (STU.isEmpty(cte)) new SMIMESigned( mmp) else new SMIMESigned(mmp, cte)
    val s= new JcaCertStore(certs.toList)
    sc.getSignerInfos().getSigners().foreach { (i) =>
      val si=i.asInstanceOf[SignerInformation]
      val c=s.getMatches(si.getSID)
      val it= c.iterator
      while ( it.hasNext) {
        val bdr=new JcaSimpleSignerInfoVerifierBuilder().setProvider(Crypto.provider)
        if (si.verify( bdr.build( it.next().asInstanceOf[XCertHdr]) )) {
          val digest=si.getContentDigest
          if (digest != null) {
            return (sc.getContentAsMimeMessage(newSession).getContent, digest)
          }
        }
      }
    }

    throw new GeneralSecurityException("Failed to verify signature: no matching cert." )
  }

  /**
   * @return
   * @throws NoSuchAlgorithmException
   */
  def newRandom(): SecureRandom = Crypto.secureRandom()

  /**
   * @param part
   * @return
   * @throws IOException
   * @throws MessagingException
   * @throws GeneralSecurityException
   */
  def decompress(part:BodyPart) = {
    decompressAsStream( if (part ==null) null else part.getInputStream)
  }

  /**
   * @param inp
   * @return
   * @throws GeneralSecurityException
   * @throws IOException
   */
  def decompressAsStream(inp:InputStream) = {
    if (inp != null) {
      val cms= new CMSCompressedDataParser(inp).getContent(new ZlibExpanderProvider())
      if (cms==null) { throw new GeneralSecurityException("Failed to decompress stream: corrupted content") }
      readBytes(cms.getContentStream)
    } else {
      new XData()
    }
  }

  /**
   * @param cert
   * @param algo
   * @param bp
   * @return
   * @throws NoSuchAlgorithmException
   * @throws CertificateEncodingException
   * @throws GeneralSecurityException
   */
  def smimeEncrypt(cert:JCert, algo:EncryptionAlgo, bp:BodyPart): MimeBodyPart = {
    val gen= new SMIMEEnvelopedGenerator()
    val p=Crypto.provider

    gen.addRecipientInfoGenerator(
        new JceKeyTransRecipientInfoGenerator(cert.asInstanceOf[XCert]).setProvider(p) )

    gen.generate( bp.asInstanceOf[MimeBodyPart],
          new JceCMSContentEncryptorBuilder(algo.oid).setProvider(p).build)
  }

  /**
   * @param cert
   * @param algo
   * @param msg
   * @return
   * @throws Exception
   */
  def smimeEncrypt(cert:JCert, algo:EncryptionAlgo, msg:MimeMessage): MimeBodyPart = {
    val gen= new SMIMEEnvelopedGenerator()
    val p=Crypto.provider
    val g=new JceKeyTransRecipientInfoGenerator(cert.asInstanceOf[XCert]).setProvider(p)
    gen.addRecipientInfoGenerator(g )
    gen.generate(msg,
        new JceCMSContentEncryptorBuilder(algo.oid).setProvider(p).build)
  }

  /**
   * @param cert
   * @param algo
   * @param mp
   * @return
   * @throws MessagingException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws GeneralSecurityException
   * @throws CertificateEncodingException
   */
  def smimeEncrypt( cert:JCert, algo:EncryptionAlgo, mp:Multipart): MimeBodyPart = {

    val gen= new SMIMEEnvelopedGenerator()
    val p=Crypto.provider
    val g=new JceKeyTransRecipientInfoGenerator(cert.asInstanceOf[XCert]).setProvider(p)
    gen.addRecipientInfoGenerator(g )
    val mm= newMimeMsg
    mm.setContent(mp)
    gen.generate(mm,
        new JceCMSContentEncryptorBuilder(algo.oid).setProvider(p).build)
  }

  /**
   * @param contentType
   * @param msg
   * @return
   * @throws IOException
   * @throws MessagingException
   * @throws GeneralSecurityException
   */
  def compressContent(contentType:String, msg:XData): MimeBodyPart = {
    val gen= new SMIMECompressedGenerator()
    val bp= new MimeBodyPart()
    val ds= msg.fileRef match {
      case Some(f) => new  SmDataSource(f, contentType)
      case _ => new  SmDataSource(msg.bytes.getOrElse(Array[Byte]()), contentType)
    }
    bp.setDataHandler( new DataHandler(ds) )
    gen.generate(bp, SMIMECompressedGenerator.ZLIB)
  }

  /**
   * @param msg
   * @return
   * @throws GeneralSecurityException
   */
  def compressContent(msg:MimeMessage): MimeBodyPart = {
    new SMIMECompressedGenerator().generate(msg, SMIMECompressedGenerator.ZLIB)
  }

  /**
   * @param cType
   * @param cte
   * @param contentLoc
   * @param cid
   * @param msg
   * @return
   * @throws MessagingException
   * @throws IOException
   * @throws GeneralSecurityException
   */
  def compressContent(cType:String, cte:String, contentLoc:String, cid:String, msg:XData) = {
    val gen= new SMIMECompressedGenerator()
    var bp= new MimeBodyPart()
    var cID=cid
    val ds= msg.fileRef match {
      case Some(f) => new SmDataSource(f, cType )
      case _ => new SmDataSource(msg.bytes.getOrElse(Array[Byte]()), cType )
    }
    if ( ! STU.isEmpty(contentLoc)) { bp.setHeader("content-location", contentLoc) }
    bp.setHeader("content-id", cid)
    bp.setDataHandler( new DataHandler( ds) )
    bp= gen.generate(bp, SMIMECompressedGenerator.ZLIB)
    if (true) {
      val pos= cID.lastIndexOf(">")
      cID = if (pos >= 0) { cID.substring(0,pos) + "--z>" }
      else
      { cID + "--z" }
    }

    if ( ! STU.isEmpty(contentLoc)) { bp.setHeader( "content-location", contentLoc) }
    bp.setHeader( "content-id", cID)

    // always base64
    //cte="base64"
    bp.setHeader( "content-transfer-encoding", "base64")
    bp
  }

  /**
   * @param key
   * @param certs
   * @param algo
   * @param data
   * @return
   * @throws NoSuchAlgorithmException
   * @throws InvalidAlgorithmParameterException
   * @throws CertStoreException
   * @throws IOException
   * @throws CertificateEncodingException
   * @throws GeneralSecurityException
   */
  def pkcsDigSig(key:PrivateKey, certs:Array[JCert], algo:SigningAlgo, data:XData ) = {
    val gen=new CMSSignedDataGenerator()
    val p= Crypto.provider
    val lst= mutable.LinkedList[JCert](); certs.foreach{ (c) => lst :+ c }
    val cert= certs(0).asInstanceOf[XCert]

    val cs = new JcaContentSignerBuilder(algo.toString).setProvider(p).build(key)
    val bdr = new JcaSignerInfoGeneratorBuilder(
        new JcaDigestCalculatorProviderBuilder().setProvider(p).build)
    bdr.setDirectSignature(true)

    gen.addSignerInfoGenerator(bdr.build(cs, cert))
    gen.addCertificates( new JcaCertStore(lst))

    val cms = data.fileRef match {
      case Some(f) =>  new CMSProcessableFile( f)
      case _ =>  new CMSProcessableByteArray( data.bytes.getOrElse(Array[Byte]()))
    }

    gen.generate(cms, false).getEncoded
  }


  /**
   * @param cert
   * @param data
   * @param signature
   * @return
   * @throws GeneralSecurityException
   * @throws IOException
   * @throws CertificateEncodingException
   */
  def verifyPkcsDigSig( cert:JCert, data:XData, signature:Array[Byte]): Array[Byte] = {
    val cproc= data.fileRef match {
      case Some(f) =>  new CMSProcessableFile( f)
      case _ => new CMSProcessableByteArray( data.bytes.getOrElse(Array[Byte]()))
    }
    val cms=new CMSSignedData(cproc, signature)
    val s= new JcaCertStore(List(cert))
    cms.getSignerInfos.getSigners.foreach { (i) =>
      val si=i.asInstanceOf[SignerInformation]
      val c=s.getMatches(si.getSID)
      val it= c.iterator

      while ( it.hasNext ) {
        val bdr=new JcaSimpleSignerInfoVerifierBuilder().setProvider(Crypto.provider)
        if ( si.verify( bdr.build( it.next().asInstanceOf[XCertHdr]) )) {
          val digest=si.getContentDigest
          if (digest != null) { return digest }
        }
      }
    }
    throw new GeneralSecurityException("Failed to decode signature: no matching cert.")
  }


  /**
   * @param data
   * @return
   * @throws NoSuchAlgorithmException
   */
  def fingerPrintSHA1(data:Array[Byte]) = fingerPrint(data, "SHA-1")

  /**
   * @param data
   * @return
   * @throws NoSuchAlgorithmException
   */
  def fingerPrintMD5(data:Array[Byte]) = fingerPrint(data, "MD5")

  private def signingAlgoAsString(algo:String) = {
    algo match {
      case "SHA-512" => SMIMESignedGenerator.DIGEST_SHA512
      case "SHA-1" => SMIMESignedGenerator.DIGEST_SHA1
      case "MD5" => SMIMESignedGenerator.DIGEST_MD5
      case _ => throw new IllegalArgumentException("Unsupported signing algo:  " + algo)
    }
  }

  /**
   * @param privateKeyBits
   * @param pwd
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws IOException
   */
  def certDesc(privateKeyBits:Array[Byte], pwd:String): (X500Principal,X500Principal,JDate,JDate) = {
    bitsToKey(privateKeyBits, pwd) match {
      case Some(k) => certDesc( k.getCertificate )
      case _ => ( null, null, null, null)
    }
  }

  /**
   * @param algo
   * @return
   * @throws NoSuchAlgorithmException
   */
  def newMsgDigest(algo:String) =  MessageDigest.getInstance(algo)

  /**
   * @param certBits
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws UnrecoverableEntryException
   * @throws IOException
   */
  def certDesc(certBits:Array[Byte]): (X500Principal,X500Principal,JDate,JDate) = {
    bitsToCert( certBits) match {
      case Some(c) => certDesc( c.getTrustedCertificate )
      case _ => (null,null,null,null)
    }

  }

  /**
   * @param cert
   * @return
   */
  def certDesc( cert:JCert): (X500Principal,X500Principal,JDate,JDate) = {
    //tstArgIsType("cert", cert, classOf[X509Certificate])
    cert match {
      case x509:XCert =>
        ( x509.getSubjectX500Principal, x509.getIssuerX500Principal, x509.getNotBefore, x509.getNotAfter )
      case _ =>
        (null,null,null,null)
    }
  }

  /**
   * @param os
   */
  def dbgProviderProps(os:PrintStream) {
    try {
      Crypto.provider().list(os)
    } catch {
      case t:Throwable => t.printStackTrace()
    }
  }

  /**
   * @param privateKeyBits
   * @param pwd
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws IOException
   */
  def bitsToKey(privateKeyBits:Array[Byte], pwd:String) = {
    val cs= new PKCSStore()
    cs.init("xxx")
    cs.addKeyEntity(asStream(privateKeyBits), pwd)
    cs.keyEntity( cs.keyAliases()(0), pwd)
  }

  /**
   * @param certBits
   * @return
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws IOException
   * @throws UnrecoverableEntryException
   */
  def bitsToCert(certBits:Array[Byte]) = {
    val cs= new PKCSStore()
    cs.init("xxx")
    cs.addCertEntity( asStream( certBits))
    cs.certEntity( cs.certAliases()(0) )
  }

  /**
   * @param bits
   * @param pwd
   * @return
   * @throws Exception
   */
  def tstPKeyValid( bits:Array[Byte], pwd:String) = {

    val c = bitsToKey(bits, pwd) match {
      case Some(kk) => kk.getCertificate.asInstanceOf[XCert]
      case _ => null
    }

    tstCertValid(c)
  }


  /**
   * @param bits
   * @return
   * @throws Exception
   */
  def tstCertValid(bits:Array[Byte]): Boolean = {

    val c= bitsToCert(bits) match {
      case Some(cc) => cc.getTrustedCertificate.asInstanceOf[XCert]
      case _ => null
    }

    tstCertValid(c)
  }

  /**
   * @param x
   * @return
   */
  def tstCertValid(x:XCert): Boolean = {
    try {
      x.checkValidity(new JDate())
      true
    } catch {
      case e:Throwable => false
    }
  }

  /**
   * @param certs
   * @return
   */
  def toCerts(certs:Array[TrustedCertificateEntry]) = {
    if (certs != null) {
      val cs = mutable.ArrayBuffer[JCert]()
      (0 /: certs) { (pos, c) =>
        cs(pos) = c.getTrustedCertificate
        pos+1
      }
      cs.toArray
    } else {
      Array[JCert]()
    }
  }

  /**
   * @param keys
   * @return
   */
  def toPKeys(keys:Array[PrivateKeyEntry]) =  {
    if (keys != null) {
      val ks= mutable.ArrayBuffer[PrivateKey]()
      (0 /: keys) { (pos, k) =>
        ks(pos) = k.getPrivateKey
        pos+1
      }
      ks.toArray
    } else {
      Array[PrivateKey]()
    }
  }

  private def mkSignerGentor(key:PrivateKey, certs:Array[JCert], algo:SigningAlgo):SMIMESignedGenerator = {
    val gen= new SMIMESignedGenerator("base64")
    val lst=mutable.LinkedList[JCert]()
    certs.foreach{ (c) => lst :+ c }
    val  signedAttrs = new ASN1EncodableVector()
    val caps = new SMIMECapabilityVector()
    caps.addCapability(SMIMECapability.dES_EDE3_CBC)
    caps.addCapability(SMIMECapability.rC2_CBC, 128)
    caps.addCapability(SMIMECapability.dES_CBC)
    signedAttrs.add(new SMIMECapabilitiesAttribute(caps))

    val x0= certs(0).asInstanceOf[XCert]
    var issuer= if (certs.length > 1) {
      certs(1).asInstanceOf[XCert]
    } else {
      x0
    }
    val issuerDN= issuer.getSubjectX500Principal
    val p=Crypto.provider
    //
    // add an encryption key preference for encrypted responses -
    // normally this would be different from the signing certificate...
    //
    val issAndSer = new IssuerAndSerialNumber(
        X500Name.getInstance(issuerDN.getEncoded),
        x0.getSerialNumber)
    signedAttrs.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer))

    val bdr = new JcaSignerInfoGeneratorBuilder(
          new JcaDigestCalculatorProviderBuilder().setProvider(p).build)
    bdr.setDirectSignature(true)

    val cs = new JcaContentSignerBuilder(algo.toString).setProvider(p).build(key)
    bdr.setSignedAttributeGenerator(
      new DefaultSignedAttributeTableGenerator(new AttributeTable(signedAttrs)))
    gen.addSignerInfoGenerator(bdr.build(cs, x0))
    gen.addCertificates( new JcaCertStore(lst))
    gen

  }

  private def fingerPrint(data:Array[Byte], algo:String) = {
    val md5 = MessageDigest.getInstance(algo)
    val ret = new StringBuilder(256)
    val hash= md5.digest(data)
    val tail= hash.length-1
    for( i <- 0 until hash.length) {
      val n = Integer.toString((hash(i)&0xff), 16).uc
      ret.append( if(n.length() == 1) ("0"+n) else n).append( if(i != tail) ":" else "" )
    }
    ret.toString
  }

  private def inizMapCap() {
    CommandMap.getDefaultCommandMap match {
      case mc:MailcapCommandMap =>
        mc.addMailcap("application/pkcs7-signature;; " +
          "x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature")
        mc.addMailcap("application/pkcs7-mime;; " +
          "x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime")
        mc.addMailcap("application/x-pkcs7-signature;; " + "" +
          "x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature")
        mc.addMailcap("application/x-pkcs7-mime;; " +
          "x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime")
        mc.addMailcap("multipart/signed;; " +
          "x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed")
    }
  }



}

sealed class CryptoUtils {}
