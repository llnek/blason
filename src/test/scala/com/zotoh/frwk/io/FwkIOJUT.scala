/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUtils IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUtilsD IN THE HOPE THAT IT WILL BE USEFUL,
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
package io

import org.apache.commons.io.{FileUtils=>FUS}
import org.apache.commons.io.{IOUtils=>IOU}

import com.zotoh.frwk.util.FileUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.io.{ByteArrayInputStream=>ByteArrayIS,ByteArrayOutputStream=>ByteArrayOS}
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.{Properties=>JPS}


import org.scalatest.Assertions._
import org.scalatest._


class FwkIOJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll  {

  override def beforeAll(configMap: Map[String, Any]) {}

  override def afterAll(configMap: Map[String, Any]) {}

  override def beforeEach() {}

  override def afterEach() {}

  private val STRB="hello".getBytes("utf-8")
  private val STR="hello"

  test("testUtils") {
    var f=mkTempFile()
    writeFile(f, STR, "utf-8")

    var s = readText(f, "utf-8")
    expectResult(s.length())(5)

    var b=read(f)
    expectResult(b.length)(5)

    assert(gzip(STR,"utf-8").sameElements( gzip(STRB)))
    assert(gunzip(gzip(STR,"utf-8")).sameElements( STRB) )

    var inp=asStream(STRB)
    assert(inp != null)
    assert( bytes(inp).sameElements( STRB))

    inp=asStream(STRB)
    var dd=readBytes(inp)
    assert(dd != null)
    assert(dd.fileRef() == None)
    assert(dd.javaBytes().sameElements(STRB))

    using(IOUtils.open(f)) { (inp) =>
      assert(inp.isInstanceOf[XStream])
      assert(bytes(inp).sameElements( STRB))
    }
    assert(f.exists())
    assert(read(f).sameElements(STRB))

    s=toGZipedB64(STRB)
    assert(fromGZipedB64(s).sameElements( STRB))

    assert(read(f).sameElements(STRB))
    expectResult( 5)(available(asStream( STRB)))

    var out= new ByteArrayOS()
    IOU.copy( asStream(STRB), out)
    assert(out.toByteArray().sameElements( STRB))

    f=copy(asStream( STRB))
    expectResult(5)(f.length())
    f.delete()

    var baos= new ByteArrayOS()
    b=new Array[Byte](10000)
    copy(asStream(b), baos, 9492)
    expectResult(baos.toByteArray().length)(9492)

    assert(!different(asStream("abc".getBytes()), asStream("abc".getBytes())))
    assert(different(asStream("abc".getBytes()), asStream("ABC".getBytes())))

    IOUtils.streamLimit = 2
    
    dd=readBytes(asStream(STRB))
    assert(dd.fileRef != None)
  }

  test("testEmptyStreamData") {
    val s= new XData()
    expectResult(s.size())(0L)
    assert(s.fileRef() == None)
    assert(s.content() == None)
    assert(s.stream() == null)
    assert(s.filePath() == "")
    assert(s.binData() == null)
    assert(s.bytes() == None)
    assert(s.javaBytes().sameElements( Array[Byte]()) )
  }

  test("testBytesStreamData") {
    var s= new XData()
    var data= "hello world"
    s.resetMsgContent(data)
    assert(s.content().get == s.binData())
    assert(s.bytes().get == s.binData())
    expectResult(s.size())( asBytes(data).length)
    assert(!s.isZiped)
    assert(s.binData().sameElements( asBytes(data)) )
  }

  test("testLargeBytesStreamData") {
    var s= new XData()
    var b= mkString('x', 5000000)
    var bits= asBytes(b)
    s.resetMsgContent(b)
    assert(s.content().get.asInstanceOf[Array[Byte]].sameElements( s.binData()) )
    assert(s.bytes().get.sameElements(s.binData()) )
    expectResult(s.size())( bits.length)
    assert(s.isZiped)
    assert(s.bytes().get.sameElements( bits) )
  }

  test("testStreamStreamData") {
    var s= new XData()
    var data= "hello world"
    s.resetMsgContent(data)
    assert( s.stream().isInstanceOf[ByteArrayIS] )
    assert(bytes(s.stream()).sameElements( asBytes(data)) )
  }

  test("testFileStreamData") {
    var s= IOUtils.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    assert(s.stream().isInstanceOf[XStream])
    assert(bytes(s.stream()).sameElements( asBytes(data)) )
    assert(s.bytes().get.sameElements( asBytes(data)) )
    assert(s.binData()==null)
    expectResult(s.size())(data.length())
    s.destroy()
    assert( !fout.exists())
  }

  test("testFileRefStreamData") {
    var s= IOUtils.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    s.destroy()
    assert( !fout.exists())
  }

  test("testFileRefStreamData2") {
    var s= IOUtils.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      s.setDeleteFile( false)
      s.destroy
      assert(fout.exists())
    }
    finally {
      FUS.deleteQuietly(fout)
    }
  }

  test("testSmartFileIS") {
    var t= IOUtils.newTempFile( true)
    var fout= t._1
    var os= t._2
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      var bits= new Array[Byte](1024)
      var inp= new XStream(fout)
      var c=inp.read(bits)
      expectResult(c)(11)
      inp.close()
      c=inp.read(bits)
      expectResult(c)(11)
      inp.delete()
      assert(fout.exists())
    }
    finally {
      FUS.deleteQuietly(fout)
    }
  }

  test("testSmartFileIS2") {
    var t= IOUtils.newTempFile(true)
    var fout= t._1
    var os= t._2
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      var inp= new XStream(fout, true)
      var bits= new Array[Char](1024)
      var c=inp.readChars(chSet("utf-8"), 1024)
      expectResult(c._2)(11)
      inp.close()
      inp.delete()
      assert(! fout.exists())
    }
    finally {
    }
  }


}

