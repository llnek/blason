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
package util

import scala.collection.JavaConversions._


import org.apache.commons.lang3.{StringUtils=>STU}

import com.zotoh.frwk.util.DateUtils._
import com.zotoh.frwk.util.ByteUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.sql.{Timestamp=>JTSTMP}
import java.util.Calendar
import java.util.{Date=>JDate,Properties=>JPS}
import java.util.GregorianCalendar
import java.nio.charset.Charset
import java.nio._
import java.io.File
import scala.Serializable

import org.scalatest.Assertions._
import org.scalatest._

class FwkUtilJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with Constants  {

  private val STR= "hello"

  override def beforeAll(configMap: Map[String, Any]) {}

  override def afterAll(configMap: Map[String, Any]) {}

  override def beforeEach() {}

  override def afterEach() {}

  test("testByteUtils") {
    val bb= convertCharsToBytes(STR.toCharArray(), Charset.forName("utf-8") )
    val ss=asBytes(STR)
    assert(ss.sameElements( bb ))
    assert( STR == new String(bb, "utf-8") )

    expectResult(911L)(readAsLong( readAsBytes(911L)))
    expectResult(911)(readAsInt( readAsBytes(911)))
    try {
      readAsLong(readAsBytes(911L).slice(4,8))
      assert(false,"expectResult to fail but succeeded!!!")
    } catch {
      case e:Throwable => assert(true) // not enough bytes to read
    }
  }

  test("testDateUtils") {
    val gc= new GregorianCalendar(2050, 5, 20)
    val base= gc.getTime()
    var dt= DateUtils.addYears(base, -5)
    var g= new GregorianCalendar()
    g.setTime(dt)

    expectResult(g.get(Calendar.YEAR))( 2045)
    dt= DateUtils.addYears(base, 5)
    g= new GregorianCalendar(); g.setTime(dt)
    expectResult(g.get(Calendar.YEAR))( 2055)
    dt= DateUtils.addMonths(base, -2)
    g= new GregorianCalendar(); g.setTime(dt)
    expectResult(g.get(Calendar.MONTH))( 3)
    dt= DateUtils.addMonths(base, 2)
    g= new GregorianCalendar(); g.setTime(dt)
    expectResult(g.get(Calendar.MONTH))( 7)
    dt= DateUtils.addDays(base, -10)
    g= new GregorianCalendar(); g.setTime(dt)
    expectResult(g.get(Calendar.DAY_OF_MONTH))( 10)
    dt= DateUtils.addDays(base, 10)
    g= new GregorianCalendar(); g.setTime(dt)
    expectResult(g.get(Calendar.DAY_OF_MONTH))( 30)
  }

  test("testMiscStr") {
    expectResult(STR+"_0x24")(normalize(STR+"$") )
  }

  test("testZip") {
    var sa= STR.getBytes("utf-8")
    sa=deflate(sa)
    sa=inflate(sa)
    expectResult(STR)( new String(sa, "utf-8"))
  }

  test("testTrim") {
    var s=STU.strip("<"+STR+">", "<>")
    expectResult(STR)(s)
    s= STU.trim("   hello     ")
    expectResult(STR)(s)
  }

  test("testContainsChar") {
    var ok= STU.containsAny("this is amazing !!!", "^%!$*")
    assert(ok)
    ok= STU.containsAny("this is amazing !!!", "^%$*")
    assert(!ok)
    ok= STU.containsNone("this is amazing !!!", "^%$*")
    assert(ok)
  }

  test("testSplitChunks") {
    var ss= splitIntoChunks("1234567890", 5)
    assert(ss != null && ss.length==2)
    expectResult("12345" )( ss(0))
    expectResult("67890" )( ss(1))
  }

  test("testStrstr") {
    val s= STU.replace("this is a message to joe : hello joe", "joe", "bobby")
    expectResult("this is a message to bobby : hello bobby")(s)
  }

  test("testFmtDate") {
    val now= new GregorianCalendar(2000, 9, 2, 12, 13, 14)
    val n= now.getTime()
    var s= fmtDate(n, DT_FMT)
    assert(s != null && s.length() > 0)
    val a= parseDate(s, DT_FMT)
    expectResult(a.getOrElse(null))( n)
  }

  test("testFmtTS") {
    val now= new GregorianCalendar(2000, 9, 2, 12, 13, 14)
    val n= new JTSTMP(now.getTime().getTime())
    val s= n.toString()
    assert(s != null && s.length() > 0)
    val a= parseTimestamp(s)
    expectResult(a.getOrElse(null))(n)
  }

  test("testParseDate") {
    var d= parseDate("8764395345")
    assert(d==None)

    d= parseDate("2000-03-04 16:17:18")
    assert(d.getOrElse(null) != null)

    assert( !hasTZPart("2000-03-04 16:17:18"))
    assert( !hasTZPart("2000-03-04"))
    assert( !hasTZPart("2000-03-04 16:17:18.999"))
    assert( hasTZPart("2000-03-04 16:17:18 -099"))
    assert( hasTZPart("2000-03-04 16:17:18 +099"))
    assert( hasTZPart("2000-03-04 16:17:18 PDT"))
  }

  test("testParseTS") {
    var ts= parseTimestamp("43654kjljlfk")
    assert(ts==None)

    ts= parseTimestamp("2010-09-02 13:14:15")
    assert(ts.getOrElse(null) != null)
  }

  test("testUpcaseFirstChar") {
    expectResult("Joe")( "joe".capitalize)
  }

  test("testArrToStr") {
    val arr= List(STR, "joe")
    var s:String= join( arr, ",")
    expectResult( "hello,joe" )(s)
    s= join( arr, null)
    expectResult( "hellojoe" )(s)
    s= join( arr )
    expectResult( "hellojoe" )(s)
  }

  test("testHasWithin") {
    var ok=hasWithin("hello joe, how are you?", Array("are"))
    assert(ok)

    ok=hasWithin("hello joe, how are you?", Array("hello"))
    assert(ok)

    ok=hasWithin("hello joe, how are you?", Array("jack"))
    assert(!ok)
  }

  test("testAddAndDelim") {
    val bf= new StringBuilder(256)
    addAndDelim(bf, ";", STR)
    expectResult(STR)(bf.toString())
    addAndDelim(bf, ";", "joe")
    expectResult(STR+";joe")( bf.toString())
  }

  test("testEqualsOneOf") {
    var ok=equalsOneOf("jim", Array[String]("Jack", "joe", "jim"))
    assert(ok)

    ok=equalsOneOf("Jim", Array("Jack", "joe", "jim"))
    assert(!ok)

    ok=equalsOneOfIC("Jim", Array("Jack", "joe", "jim") )
    assert(ok)
  }

  test("testStartsWith") {
    val ok=startsWithIC("hello joe", Array("joe", "HeLlo" ))
    assert(ok)
  }

  test("testZeroInteger") {
    val n= 0
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testOneInteger") {
    val n= 1
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testSmallInteger") {
    val n= 100
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testLargeInteger") {
    val n= Integer.MAX_VALUE
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegOneInteger") {
    val n= -1
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegSmallInteger") {
    val n= -100
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegLargeInteger") {
    val n= Integer.MIN_VALUE
    val m= ByteUtils.readAsInt( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testZeroLong") {
    val n= 0L
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testOneLong") {
    val n= 1L
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testSmallLong") {
    val n= 100L
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testLargeLong") {
    val n= java.lang.Long.MAX_VALUE
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegOneLong") {
    val n= -1L;
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegSmallLong") {
    val n= -100L
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  test("testNegLargeLong") {
    val n= java.lang.Long.MIN_VALUE
    val m= ByteUtils.readAsLong( ByteUtils.readAsBytes(n))
    expectResult(m)(n)
  }

  /*
  test("testMenu") {
    var m1= new TMenu("M1")
    var m2= new TMenu("M2")
    var cb=new TMenuCB() {
      def command(i:TMenuItem) { }}
    m1.add(new TMenuItem("m.i1", "New", cb))
    m1.add(new TMenuItem("m.i2", "Open", m2))
    m1.add(new TMenuItem("m.i3", "Close", cb))
    m1.show(null)
    m1=null
  }
  */

  test("testNiceFilePath") {
    expectResult(niceFPath("/c:\\windows\\temp"))( "/c:/windows/temp")
  }

  test("testNiceFilePath2") {
    expectResult(niceFPath(new File("/c:\\windows\\temp")))( "/c:/windows/temp")
  }

  test("serializeObj") {
    val d=new Dumb("joe")
    val n=deserialize(serialize(d)) match {
      case x:Dumb => x.name
      case _ => ""
    }
    expectResult("joe")( n)
  }

  test("asSomeNumbers") {
    expectResult(asInt("911",-1))( 911)
    expectResult(asLong("911", 0L))( 911)
    expectResult(asDouble("911", 3.3).toInt)( 911)
    expectResult(asFloat("911", 6.toFloat).toInt)( 911)
    expectResult(asInt("xxx",-1))( -1)
    expectResult(asLong("xxx",-1L))( -1L)
    assert(asBool("yes",false))
    assert(! asBool("xxx",false))
  }

  test("testZipJPS") {
    val p= new JPS()
    p.put("a", "hi")
    expectResult(asQuirks(asBytes(p)).getProperty("a"))("hi")
  }

  test("testFileUrl") {
    val p=new File("/tmp/abc.txt").getCanonicalPath()
    val s=asFileUrl(new File(p))
    if (isWindows())
      expectResult(s)( "file:/"+p)
    else
      expectResult(s)( "file:" + p)
  }


  /*
  test("testCmdLineSeq") {
    val q3= new CmdLineQ("bad", "oh dear, too bad") {
      def onRespSetOut(a:String, props:JPS) = {
        ""
      }}

    val q2= new CmdLineQ("ok", "great, bye") {
      def onRespSetOut( a:String , props:JPS) = {
        ""
      }}
    val q1= new CmdLineQ("a", "hello, how are you?", "ok/bad", "ok") {
      def onRespSetOut(a:String, props:JPS) = {
        if ("ok"==a) {
          props.put("state", asJObj(true))
          "ok"
        } else {
          props.put("state", asJObj(false))
          "bad"
        }
      }
    }

    val seq= new CmdLineSeq(Array(q1,q2,q3)) {
      def onStart() = "a"
    }

    val props= new JPS()
    seq.start(props)
    if (seq.isCanceled()) {
      assert(true)
    }
    else {
      assert(props.containsKey("state"))
    }

  }
  */

}

@SerialVersionUID(911L)
class Dumb(val name:String) extends Serializable {
}
