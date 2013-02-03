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
package xml

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.FileUtils._

import java.io.FilenameFilter
import java.io.InputStream
import java.net.URL
import java.io.File

import org.scalatest.Assertions._
import org.scalatest._

import org.w3c.dom.Document



class FwkXmlJUT extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with Constants  {

  override def beforeAll(configMap: Map[String, Any]) {}

  override def afterAll(configMap: Map[String, Any]) {}

  override def beforeEach() {}

  override def afterEach() {}

  test("testToDOM") {
    using(rc2Stream("com/zotoh/frwk/xml/simple.xml")) { (inp) =>
      assert(XMLUtils.toDOM(inp) != null)
    }
  }

  test("testWriteDOM") {
    using(rc2Stream("com/zotoh/frwk/xml/simple.xml")) { (inp) =>
      val s= DOMWriter.writeOneDoc( XMLUtils.toDOM(inp) )
      assert(s != null && s.length() > 0)
    }
  }

  test("testXmlScanner") {
    var doc= rc2Url("com/zotoh/frwk/xml/malformed.xml")
    val s= new XMLScanner()
    assert( ! s.scan(doc))
    doc= rc2Url("com/zotoh/frwk/xml/simple.xml")
    s.reset()
    assert( s.scan(doc))
  }

  test("testDTDValidator") {
    val dtd= rc2Url("com/zotoh/frwk/xml/test.dtd")
    val v= new DTDValidator()
    using(rc2Stream("com/zotoh/frwk/xml/bad.dtd.xml")) { (inp) =>
      assert( v.scanForErrors(inp, dtd))
    }
    v.reset()
    using(rc2Stream("com/zotoh/frwk/xml/good.dtd.xml")) { (inp) =>
      assert(! v.scanForErrors(inp, dtd))
    }
  }

  test("testXSDValidator") {
    val xsd= rc2Url("com/zotoh/frwk/xml/test.xsd")
    val v= new XSDValidator()
    using(rc2Stream("com/zotoh/frwk/xml/bad.xsd.xml")) { (inp) =>
      assert( ! v.scan(inp, xsd))
    }
    v.reset()
    using(rc2Stream("com/zotoh/frwk/xml/good.xsd.xml")) { (inp) =>
      assert( v.scan(inp, xsd))
    }
  }

  private class FF extends FilenameFilter {
    def accept(dir:File, fname:String) = fname.endsWith(".xml")
  }

}

