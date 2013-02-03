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

import org.apache.commons.io.{FileUtils=>FUS}
import org.apache.commons.io.{IOUtils=>IOU}

import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.util.CoreUtils
import com.zotoh.frwk.util.CoreUtils._

import com.zotoh.frwk.security.CryptoUtils._
import com.zotoh.frwk.security.Crypto._

import java.io.{File,InputStream,ByteArrayOutputStream=>ByteArrayOS}
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.KeyStore.PrivateKeyEntry
import java.security.KeyStore.TrustedCertificateEntry
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.{Certificate=>JCert}
import java.security.cert.CertificateFactory
import java.security.cert.{X509Certificate=>XCert}
import java.util.{Date=>JDate}
import java.util.Random

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.BodyPart
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import org.bouncycastle.asn1.ASN1InputStream

import com.zotoh.frwk.io.{XData,XStream}
import com.zotoh.frwk.mime.MimeUtils
import com.zotoh.frwk.util.{FileUtils,CoreImplicits,Constants}


import org.scalatest.Assertions._
import org.scalatest._


class FwkCryptoJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with CoreImplicits  {

  override def beforeAll(configMap: Map[String, Any]) {
    val cs= new PKCSStore().init(new File( rc2Url("com/zotoh/frwk/crypto/test1.p12").getFile()), PWD1)
    val pk= cs.keyEntity( cs.keyAliases()(0), PWD1)
    assert(pk != None)
    _PKE=pk.get

    BaseCryptor.setKey("use-this-as-the-key-for-testing" )
  }

  override def afterAll(configMap: Map[String, Any]) {}

  override def beforeEach() {}

  override def afterEach() {}

  private val DNSTR2= "C=AU,ST=NSW,L=Sydney,O=Test (Sample Only),CN=www.yoyo.com"
  private val DNSTR= "C=US,ST=California,L=San Francisco,O=Test (Sample Only),CN=Bouncy Castle Root CA"

  private var _PKE:PrivateKeyEntry = _

  private val m3= 1000L*60*60*24*90; // 3mths
  private val now= new JDate().getTime()
  private val PWD2= "Password2"
  private val PWD1= "Password1"

  private var _start = new JDate(now - m3 )
  private var _end = new JDate(now + m3)

  test("testCaesar") {
    val base= "I am convinced that He (God) does not play dice."
    val r= new Random()
    (1 to 18).foreach { (i) =>
      val cc= new CaesarCipher( r.nextInt(1024) )
      expectResult( base)( cc.decode( cc.encode(base) ) )
    }
    (1 to 18).foreach { (i) =>
      val cc=new CaesarCipher( -1 * r.nextInt(1024) )
      expectResult( base)( cc.decode( cc.encode(base) ) )
    }
  }

  test("testBCOfuscator") {
    val data= Array( "holy batman", "", null )
    val bo= new BouncyCryptor()
    data.foreach { (c) =>
      val e= bo.encrypt(c)
      if (c==null) {
        assert(e==null)
      } else if (e==null) {
        assert(c==null)
      } else {
        assert( ! ( c.length() > 0 && c == e) )
        expectResult(c)( bo.decrypt(e) )
      }
    }
  }

  test("testJaystOfuscator") {
    val data= Array( "holy batman", "", null )
    val bo= new JasyptCryptor()
    data.foreach { (c) =>
      val e= bo.encrypt(c)
      if (c==null) {
        assert(e==null)
      } else if (e==null) {
        assert(c==null)
      } else {
        assert( ! ( c.length() > 0 && c == e) )
        expectResult(c)( bo.decrypt(e) )
      }
    }
  }
  
  test("testCryptoCSR") {
    var t= mkCSR( 1024, DNSTR,  PEM)
    assert(t._1.length > 0)
    var pemStr= asString(t._1)
    assert(pemStr.has("BEGIN CERTIFICATE REQUEST"))
    assert(pemStr.has("END CERTIFICATE REQUEST") )
    t = mkCSR( 1024,  DNSTR, DER)
    assert(t._1.length > 0)
  }

  test("testCryptoSSV1") {
    var out= mkTempFile()
    mkSSV1PKCS12("ssv1",  _start, _end,  DNSTR, PWD1, 1024, out)
    assert(out.exists())
    assert(out.length() > 0)
    FUS.deleteQuietly(out)

    out= mkTempFile()
    mkSSV1JKS("ssv1", _start, _end,  DNSTR, PWD1, 1024, out)
    assert(out.exists())
    assert(out.length() > 0)
    FUS.deleteQuietly(out)
  }

  test("testCryptoSSV3PKCS12") {
    val root= mkTempFile()
    Crypto.mkSSV1PKCS12("ssv1",  _start, _end,  DNSTR, PWD1, 1024, root)
    assert(root.exists())
    assert(root.length() > 0)
    val ks= KeyStore.getInstance("PKCS12")
    using(IOUtils.open(root)) { (inp) =>
      ks.load(inp, PWD1.toCharArray())
    }
    assert(ks.aliases().hasMoreElements())

    val rk= ks.getEntry( ks.aliases().nextElement(), new PasswordProtection(PWD1.toCharArray())).asInstanceOf[PrivateKeyEntry]
    val cc= rk.getCertificateChain()
    val s3=  mkTempFile()
    Crypto.mkSSV3PKCS12("ssv3", _start, _end, DNSTR2, PWD2, 1024,
        cc, rk.getPrivateKey(), s3)
    assert(s3.exists())
    assert(s3.length() > 0)

    FUS.deleteQuietly(root)
    FUS.deleteQuietly(s3)
  }

  test("testCryptoSSV3JKS") {
    val root= mkTempFile()
    Crypto.mkSSV1JKS("ssv1", _start, _end,  DNSTR, PWD1, 1024, root)
    assert(root.exists())
    assert(root.length() > 0)

    val ks= KeyStore.getInstance("JKS")
    using( IOUtils.open(root)) { (inp) =>
      ks.load(inp, PWD1.toCharArray())
    }
    assert(ks.aliases().hasMoreElements())

    val rk= ks.getEntry( ks.aliases().nextElement(), new PasswordProtection(PWD1.toCharArray())).asInstanceOf[PrivateKeyEntry]
    val cc= rk.getCertificateChain()
    val s3= mkTempFile()
    Crypto.mkSSV3JKS("ssv3", _start, _end, DNSTR2, PWD2, 1024,
        cc,   rk.getPrivateKey(),   s3)
    assert(s3.exists())
    assert(s3.length() > 0)

    FUS.deleteQuietly(root)
    FUS.deleteQuietly(s3)
  }

  test("testDigestAlgo") {

    val s= Array( SigningAlgo.MD_5, SigningAlgo.SHA_1, SigningAlgo.SHA_256, SigningAlgo.SHA_512 )

    s.foreach { (s) =>
      assert( Crypto.newDigestInstance(s) != null  )
    }

    try {
      newDigestInstance(null)
      assert(false,"UnexpectResulted new digest for (xxx) was OK!" )
    } catch {
      case e:Throwable => assert(true)
    }

  }

  test("testCertBytes") {
    val ks= KeyStore.getInstance("PKCS12")
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.p12")) { (inp) =>
      ks.load(inp, PWD1.toCharArray())
    }
    assert( ks.aliases().hasMoreElements())

    val pk= ks.getEntry( ks.aliases().nextElement(),
        new PasswordProtection(PWD1.toCharArray()) ).asInstanceOf[PrivateKeyEntry]
    val c= pk.getCertificate()
    assert( c.isInstanceOf[XCert])

    val x= c.asInstanceOf[XCert]
    val p= Crypto.asBytes( x, PEM)
    assert(p != null && p.length > 0)

    val pem= asString(p)
    assert(pem.has("BEGIN CERTIFICATE") )
    assert(pem.has("END CERTIFICATE") )

    val d= Crypto.asBytes( x, DER)
    assert(d != null && d.length > 0)
    assert(p.length != d.length)
  }

  test("testCreateStores") {
    var c:CryptoStore= new PKCSStore()
    c.init("xxx")
    expectResult(c.certAliases().length)(0)
    expectResult(c.keyAliases().length)(0)
    c= new JKSStore()
    c.init("xxx")
    expectResult(c.certAliases().length)(0)
    expectResult(c.keyAliases().length)(0)
  }

  test("testAddPKey") {
    val crs = Array( (new PKCSStore(),"zotoh.p12"), ( new JKSStore(), "zotoh.jks") )
    crs.foreach { (cs) =>
      cs._1.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/" + cs._2)) { (inp) =>
        cs._1.addKeyEntity( inp, PWD1)
      }
      val o= cs._1.keyEntity(cs._1.keyAliases()(0), PWD1)
      assert(o != None && o.get.isInstanceOf[PrivateKeyEntry])
    }
  }

  test("testRemoveXXX") {
    val crs = Array( (new PKCSStore(),"zotoh.p12"), (new JKSStore(),"zotoh.jks") )
    crs.foreach { (cs) =>
      cs._1.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/" + cs._2)) { (inp) =>
        cs._1.addKeyEntity( inp, PWD1)
      }
      val nm= cs._1.keyAliases()(0)
      val o= cs._1.keyEntity(nm, PWD1)
      assert(o != null && o.get.isInstanceOf[PrivateKeyEntry])
      cs._1.removeEntity(nm)
      expectResult(cs._1.keyAliases().length)(0)
    }
  }

  test("testAddP7b") {
    val crs = Array( new PKCSStore() )
    crs.foreach { (cs) =>
      cs.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/test2.p7b")) { (inp) =>
        cs.addPKCS7Entity(inp)
      }
      val o= cs.certEntity(cs.certAliases()(0))
      assert(o != None && o.get.isInstanceOf[TrustedCertificateEntry] )
    }
  }

  test("testAddCert") {
    val crs = Array( new PKCSStore() )
    crs.foreach { (cs) =>
      cs.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/zotoh.cer")) { (inp) =>
        cs.addCertEntity( inp)
      }
      val o= cs.certEntity(cs.certAliases()(0))
      assert(o != None && o.get.isInstanceOf[TrustedCertificateEntry] )
    }
  }

  test("testGetRootCAs") {
    val crs = Array( (new PKCSStore(),"zotoh.p12") , (new JKSStore(),"zotoh.jks") )

    crs.foreach { (cs) =>
      cs._1.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/" + cs._2)) { (inp) =>
        cs._1.addKeyEntity(inp, PWD1)
      }
      assert(cs._1.rootCAs().length > 0)
      expectResult(cs._1.intermediateCAs().length)(0)
    }
  }

  test("testGetTrustedCerts") {
    val crs = Array( (new PKCSStore(),"zotoh.p12"), ( new JKSStore(), "zotoh.jks") )

    crs.foreach { (cs) =>
      cs._1.init("xxx")
      using(rc2Stream("com/zotoh/frwk/crypto/" + cs._2)) { (inp) =>
        cs._1.addKeyEntity(inp, PWD1)
      }
      assert(cs._1.trustedCerts().length > 0)
    }
  }

  test("testInitVerify") {

    val crs = Array( ( new PKCSStore(), "zotoh.p12") , ( new JKSStore(), "zotoh.jks") )
    val sigs= Array( "MD2withRSA", "MD5withRSA", "SHA1withRSA", "SHA256withRSA", "SHA384withRSA", "SHA512withRSA"  )

    crs.foreach { (cs) =>
      cs._1.init("xxx")
      using( rc2Stream("com/zotoh/frwk/crypto/" + cs._2)) { (inp) =>
        cs._1.addKeyEntity( inp, PWD1)
      }
      var cc= cs._1.keyEntity(cs._1.keyAliases()(0), PWD1).get.getCertificate()
      // just test pkcs12, most algo don't work with SUN
      cs._1 match {
        case p:PKCSStore =>
          sigs.foreach { (sig) =>
            val s = Signature.getInstance(sig, Crypto.provider())
            s.initVerify(cc)
        }
        case _ =>
      }
    }
  }

  test("testKeyFac") {
    val crs = Array( new PKCSStore(), new JKSStore() )
    crs.foreach { (cs) =>
      cs.init("xxx")
      assert( cs.keyManagerFactory() != null)
    }
  }

  def testCertFac() {
    val crs = Array( new PKCSStore(), new JKSStore() )
    crs.foreach { (cs) =>
      cs.init("xxx")
      assert( cs.trustManagerFactory() != null)
    }
  }

  test("testListCertsInP7B") {
    val cf = CertificateFactory.getInstance("X.509", Crypto.provider())
    using(rc2Stream("com/zotoh/frwk/crypto/test2.p7b")) { (inp) =>
      assert( cf.generateCertificates(inp).size() > 0)
    }
  }

  test("testReadASN1Object") {
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.p12")) { (inp) =>
      assert(new ASN1InputStream(inp).readObject() != null)
    }
  }

  test("testBitsToCert") {
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.cer")) { (inp) =>
      assert(CryptoUtils.bitsToCert(bytes(inp)) != None)
    }
  }

  test("testBitsToKey") {
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.p12")) { (inp) =>
      assert(CryptoUtils.bitsToKey(bytes(inp), PWD1) != None)
    }
  }

  test("testGetCertDesc") {

    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.cer")) { (inp) =>
      val bits= bytes(inp)
      var tc= CryptoUtils.bitsToCert(bits)
      assert(tc != None)
      val c= tc.get.getTrustedCertificate()
      assert(c != null)
      val props= CryptoUtils.certDesc(bits)
      assert(props._1 != null)
      val props2= CryptoUtils.certDesc(c)
      assert(props == props2)
    }

  }

  test("testNewRandom") {
    assert(CryptoUtils.newRandom() != null)
  }

  test("testFingerPrints") {

    val s1 = CryptoUtils.fingerPrintSHA1(CoreUtils.asBytes("hello world"))
    assert(s1 != null && s1.length() > 0)
    val s2 = CryptoUtils.fingerPrintMD5(CoreUtils.asBytes("hello world"))
    assert(s2 != null && s2.length() > 0)
    assert(s1 != s2)
  }

  test("testTstCertValid") {
    // zotoh.cer is valid between 6/27/2010 -> 6/27/2110
    // so should be true, unless you are stilling using this lib 100 years from now.
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.cer")) { (inp) =>
      val ok=CryptoUtils.tstCertValid(bytes(inp))
      assert(ok)
    }
  }

  test("testTstKeyValid") {
    // zotoh.p12 is valid between 6/27/2010 -> 6/27/2110
    // so should be true, unless you are stilling using this lib 100 years from now.
    using(rc2Stream("com/zotoh/frwk/crypto/zotoh.p12")) { (inp) =>
      val ok=CryptoUtils.tstPKeyValid(bytes(inp), PWD1)
      assert(ok)
    }
  }

  test("testSessions") {
    val s0= CryptoUtils.newSession("popeye", PWD1)
    val s1= CryptoUtils.newSession()
    assert(s0 != null)
    assert(s1 != null)
  }

  test("testMimeMsgs") {
    val m0= newMimeMsg("popeye", PWD1)
    val m2= newMimeMsg()
    assert(m0 != null)
    assert(m2 != null)
  }

  test("testDSigMimeMsg") {
    var m= newMimeMsg(rc2Stream("com/zotoh/frwk/crypto/mime.txt"))
    var c:Any= m.getContent() match {
      case x:Multipart => x
      case _ =>
        assert(false,"expectResulting object to be Multipart"); null
    }
    var mp=smimeDigSig(_PKE.getPrivateKey(), _PKE.getCertificateChain(),
        SigningAlgo.SHA512, c.asInstanceOf[Multipart])
    assert(MimeUtils.isSigned(mp.getContentType()))

//    ByteOStream os= new ByteOStream()
//    m=CryptoUtils.newMimeMsg()
//    m.setContent(mp)
//    m.saveChanges()
//    m.writeTo(os)
//    System.out.println(new String(os.asBytes()))

    m=newMimeMsg(rc2Stream("com/zotoh/frwk/crypto/mime.txt"))
    mp= m.getContent().asInstanceOf[MimeMultipart]
    val bp=mp.getBodyPart(0)
    mp= smimeDigSig( _PKE.getPrivateKey(), _PKE.getCertificateChain(), SigningAlgo.MD5, bp)
    assert( MimeUtils.isSigned(mp.getContentType()) )

    val os= new ByteArrayOS()
    m=newMimeMsg()
    m.setContent(mp)
    m.saveChanges()
    m.writeTo(os)

    m=newMimeMsg(asStream(os.toByteArray()))
    mp= m.getContent().asInstanceOf[MimeMultipart]
    c=peekSmimeSignedContent(mp)
    assert( c.isInstanceOf[String] )

    val t= verifySmimeDigSig(mp, _PKE.getCertificateChain())
    assert(t._1.isInstanceOf[String])
    assert(t._2 != null)

  }

  test("testEncryptMimeMsg") {

    var s=new SmDataSource( CoreUtils.asBytes("hello world"), "text/plain")
    var bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    // encrypt one part
    bp=smimeEncrypt( _PKE.getCertificateChain()(0), EncryptionAlgo.DES_EDE3_CBC, bp)

    var msg= newMimeMsg()
    msg.setContent(bp.getContent(), bp.getContentType())
    val os= new ByteArrayOS()
    msg.saveChanges()
    msg.writeTo(os)

    msg= newMimeMsg( asStream(os.toByteArray()))
    assert(MimeUtils.isEncrypted( msg.getContentType() ))

    var dd= smimeDecryptAsStream(Array[PrivateKey]( _PKE.getPrivateKey()), msg)

    assert(dd != null)
    assert(asString(dd.javaBytes()).has("hello world"))

    s=new SmDataSource(CoreUtils.asBytes("hello world"), "text/plain")
    bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    bp=smimeEncrypt( _PKE.getCertificate(), EncryptionAlgo.DES_EDE3_CBC, bp)
    dd=smimeDecrypt( _PKE.getPrivateKey(), bp)
    assert(dd != null)

    msg= newMimeMsg( asStream(os.toByteArray()))
    dd=smimeDecryptAsStream(Array[PrivateKey]( _PKE.getPrivateKey()), msg)
    assert(dd != null)
    assert(asString(dd.javaBytes()).has("hello world") )

    // encrypt many parts
    var mp= new MimeMultipart()
    s=new SmDataSource( CoreUtils.asBytes("hello world"), "text/plain")
    bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    mp.addBodyPart(bp)
    s=new SmDataSource(CoreUtils.asBytes("hello hello hello"), "text/plain")
    bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    mp.addBodyPart(bp)
    bp=smimeEncrypt( _PKE.getCertificate(), EncryptionAlgo.AES256_CBC, mp)
    assert(MimeUtils.isEncrypted(bp.getContentType()))

    dd=smimeDecrypt( _PKE.getPrivateKey(), bp)
    assert(dd != null)
    assert(asString(dd.javaBytes()).has("hello hello hello") )

    // encrypt one message
    mp= new MimeMultipart()
    s=new SmDataSource(CoreUtils.asBytes("hello world"), "text/plain")
    bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    mp.addBodyPart(bp)
    s=new SmDataSource(CoreUtils.asBytes("hello hello hello"), "text/plain")
    bp = new MimeBodyPart()
    bp.setDataHandler(new DataHandler( s))
    mp.addBodyPart(bp)
    msg = newMimeMsg()
    msg.setContent(mp)
    bp=smimeEncrypt( _PKE.getCertificate(), EncryptionAlgo.AES256_CBC, msg)
    //
//    msg=  CryptoUtils.newMimeMsg()
//    msg.setContent(bp.getContent(), bp.getContentType())
//    msg.saveChanges()
//    os= new ByteOStream()
//    msg.writeTo(os)
//    System.out.println(new String(os.asBytes()))
    //
    assert(MimeUtils.isEncrypted(bp.getContentType()))

    dd=smimeDecrypt( _PKE.getPrivateKey(), bp)
    assert(dd != null)
    assert(asString(dd.javaBytes()).has("hello hello hello") )

  }

  test("testPKCSDSig") {
    val dd= new XData("hello world")
    val sig=pkcsDigSig(_PKE.getPrivateKey(), _PKE.getCertificateChain(),
        SigningAlgo.SHA512, dd)
    val dig=verifyPkcsDigSig( _PKE.getCertificate(), dd, sig)
    assert(dig != null && dig.length > 0)
  }

  test("testCmpzion") {
    var msg= newMimeMsg()
    var bp= new MimeBodyPart()
    var s=new SmDataSource( CoreUtils.asBytes("hello world"), "text/plain")
    bp.setDataHandler(new DataHandler( s))
    msg.setContent(bp.getContent(), bp.getContentType())
    bp=compressContent(msg)
    assert(MimeUtils.isCompressed(bp.getContentType()))

//    msg= CryptoUtils.newMimeMsg()
//    msg.setContent(bp.getContent(), bp.getContentType());
//    msg.saveChanges()
//    os= new ByteOStream()
//    msg.writeTo(os)
//    System.out.println(new String(os.asBytes()))

    var dd= new XData("hello world")
    bp=compressContent("text/plain", dd)
    assert(MimeUtils.isCompressed(bp.getContentType()))

    dd=decompressAsStream( bp.getInputStream() )
    assert(asString(  dd.javaBytes()  ).has("hello world") )

    bp=compressContent("text/plain", "base64", "zzz", "mmm", dd)
    assert(MimeUtils.isCompressed(bp.getContentType()))

    dd=CryptoUtils.decompress(bp)
    assert(asString(  dd.javaBytes()  ).has("zzz") )

  }

  test("testOfuscator") {
    val bo = new JavaCryptor()
    val data= Array( "holy batman", "", null )

    data.foreach { (c) =>
      var e= bo.encrypt(c)
      if (c==null) {
        assert(e==null)
      } else if (e==null) {
        assert(c==null)
      } else {
        assert( ! (c.length() > 0 && c == e) )
        expectResult(c)( bo.decrypt(e) )
      }
    }
  }

  test("testPassword") {
    val data= Array( "holy batman", "", null )

    data.foreach { (d) =>
      var p1= PwdFactory.mk(d)
      var c= p1.text()
      var e= p1.encoded()
      if (d==null) {
        assert(c==null)
      }
      else if (c==null) {
        assert(d==null)
      }
      else if ( e != null && e.length() > 0) {
        assert(e.startsWith(Password.PWD_PFX))
        assert(d!=e)
      }
      assert(d==c)

      var p2= PwdFactory.copy(p1)
      assert( ! (p1 eq p2))
      assert(p1 == p2)

      p2= PwdFactory.mk(e)
      assert( !( p1 eq p2) )
      assert(p1 == p2)
    }
  }

  test("testPwdGen") {
    val data = Array( 0, -1, 15)
    data.foreach { (d) =>
      var p= PwdFactory.mkStrongPassword( d)
      var c= p.text()
      if (d < 0) assert(c==null)
      else
      if (d == 0) assert(c != null && c.length() ==0)
      else
      assert(c != null && c.length() == d)
    }
  }

  test("testRandomTextGen") {
    val data = Array( 0, -1, 255 )
    data.foreach { (d) =>
      var c=PwdFactory.mkRandomText( d)
      if (d < 0) assert(c==null)
      else
      if (d == 0) assert(c != null && c.length() ==0)
      else
      assert(c != null && c.length() == d)
    }
  }


}
