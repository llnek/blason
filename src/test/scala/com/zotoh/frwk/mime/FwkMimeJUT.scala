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
package mime

import com.zotoh.frwk.mime.MimeUtils._

import org.scalatest.Assertions._
import org.scalatest._

class FwkMimeJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll  with Constants {

  override def beforeAll(configMap: Map[String, Any]) {}

  override def afterAll(configMap: Map[String, Any]) {}

  override def beforeEach() {}

  override def afterEach() {}

  test("testIsSigned") {
    assert( isSigned("sflsdkjf; multipart/signed; sdf;lsdk"))
    assert( isSigned("df;lsdkl;gs application/pkcs7-mime ; dsfsdf ; signed-data"))
    assert( isSigned("sfls x-application/pkcs7-mime ; dsfsdf ; signed-data"))
    assert( ! isSigned("sflsdkjf"))
  }

  test("testIsEncrypted") {
    assert( isEncrypted("df;lsdkl;gs application/pkcs7-mime ; dsfsdf ; enveloped-data"))
    assert( isEncrypted("sfls x-application/pkcs7-mime ; dsfsdf ; enveloped-data"))
    assert( ! isEncrypted("sflsdkjf"))
  }

  test("testIsCompressed") {
    assert( isCompressed("df;lsdkl;gs application/pkcs7-mime ; dsfsdf ; compressed-data"))
    assert( ! isCompressed("sflsdkjf"))
  }

  test("testIsMDN") {
    assert( isMDN("df;lsdkl;gs multipart/report ; dsfsdf ; disposition-notification"))
    assert( ! isMDN("sflsdkjf"))
  }

  test("testUrlEnc") {
    expectResult("abc")(urlEncode("abc"))
    assert("ab c" != urlEncode("ab c"))
  }

  test("testUrlDec") {
    expectResult("abc")(urlDecode("abc"))
    expectResult("ab c")(urlDecode( urlEncode("ab c")))
  }



}
