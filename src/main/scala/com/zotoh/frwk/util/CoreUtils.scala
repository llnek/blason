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
package util

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import org.apache.commons.lang3.text.{StrSubstitutor=>STS}
import scala.collection.mutable
import org.slf4j._
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.FilenameUtils._
import org.apache.commons.lang3.{SerializationUtils=>SER}
import java.io.{ByteArrayInputStream=>ByteArrayIS,ByteArrayOutputStream=>ByteArrayOS}
import java.rmi.server.UID
import java.nio.charset.Charset
import java.io.File
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.URL
import java.security.SecureRandom
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.{TimeZone=>JTZone}
import java.util.GregorianCalendar
import java.util.{Map=>JMap,Properties=>JPS}
import java.util.StringTokenizer
import scala.collection.JavaConversions.asScalaSet
import com.zotoh.frwk.util.Nichts.NICHTS
import com.zotoh.frwk.util.WWID.newWWID
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.ByteUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import java.io.FileInputStream
import java.net.InetAddress
import java.net.URLEncoder
import java.net.URLDecoder
import java.util.Arrays
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonFactory
import java.util.Calendar


class ZString(str:String) {
  def uc() = str.toUpperCase
  def lc() = str.toLowerCase
  def eqic(s:String) = str.equalsIgnoreCase(s)
  def has(s:String) = str.indexOf(s) >= 0
  def has(c:Char) = str.indexOf(c) >= 0
  def hasic(s:String) = str.toLowerCase().indexOf(s.toLowerCase()) >= 0
  def swic(s:String) = str.toLowerCase().startsWith(s.toLowerCase())
}

class ZProperties(p:JPS) {
  def gets(s:String) = p.getProperty(s)
  def cls() = { p.clear; p }
  def geti(k:String) = {
    val rc=p.get(k)
    if (rc.isInstanceOf[Int]) {
      rc.asInstanceOf[Int]
    } else { 0 }
  }
  def getb(k:String) = {
    val rc=p.get(k)
    if (rc.isInstanceOf[Boolean]) {
      rc.asInstanceOf[Boolean]
    } else {
      false
    }
  }
  def add(k:Any,v:Any) =  {
    p.put( CoreUtils.asJObj(k) , CoreUtils.asJObj(v))
    p
  }
  def addAll(pp:JPS) = {
    p.putAll(pp)
    p
  }

}

trait CoreImplicits {
  implicit def  str2mystring(str:String) = new ZString(str)
  implicit def jps2myps(p:JPS) = new ZProperties(p)
}


/**
 * @author kenl
 */
object CoreUtils extends Constants  with CoreImplicits {

  private val _log= LoggerFactory.getLogger(classOf[CoreUtils])
  def tlog() = _log
  private var _isUNIX=false

  cacheEnvVars()

  def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B = {
    try {
      f(param)
    } catch {
      case e:Throwable => tlog.warn("", e); throw e
    } finally {
      block { () => param.close }
    }
  }

  /**
   *
   */
  def block ( f:  ()  => Unit ) {
    try {
      f()
    } catch { case e:Throwable =>  }
  }

  def thaw(lock:AnyRef, all:Boolean=false) {
    if (lock != null) lock.synchronized {
      if (all) lock.notifyAll() else lock.notify()
    }
  }

  def freeze(lock:AnyRef) {
    if (lock != null) lock.synchronized {
      lock.wait()
    }
  }

  def nowJTS() = new java.sql.Timestamp(nowJDate().getTime )
  def nowJDate() = new java.util.Date()
  def nowCal(): Calendar = new GregorianCalendar()

  /**
   *
   */
  def chSet(enc:String="UTF-8") =  Charset.forName(enc)

  /**
   * Convert the file path into nice format without backslashes.
   *
   * @param fp File path.
   * @return Path in string format.
   */
  def niceFPath(fp:File): String = {
    if (fp==null)  "" else niceFPath(fp.getCanonicalPath )
  }

  /**
   * Convert the file path into nice format without backslashes.
   *
   * @param fpath File path.
   * @return Normalized path.
   */
  def niceFPath(fpath:String) = normalizeNoEndSeparator(fpath,true)

  /**
   * @param props
   * @param envs
   * @return
   */
  def filterEnvVars(props:JPS, envs:Seq[String] ): JPS = {
    (new JPS() /: props.keySet ) { (bc, keyobj) =>
      val key= keyobj.toString
      bc.put(key, filterEnvVars( props.gets( key) , envs) )
      bc
    }
  }

  /**
   * @param value
   * @param envs
   * @return
   */
  def filterEnvVars(value:String, envs:Seq[String] ) = {
    var v= value
    if (v != null) envs.foreach { (e) =>
      v= STU.replace(v, "${" +  e + "}",  nsb( System.getenv( e ))  )
    }
    v
  }

  def filterSysVars(value:String, envs:Seq[String] ) = {
    var v= value
    if (v != null) envs.foreach { (e) =>
      v= STU.replace(v, "${" +  e + "}",  nsb( System.getProperty( e ))  )
    }
    v
  }

  def filterVars(value:String) = {
    new STS( System.getenv()).replace( STS.replaceSystemProperties(value))
  }

  /**
   *
   */
  def sysQuirk(p:String) = System.getProperties().getProperty(p,"")

  /**
   * @return
   */
  def userHomeDir() = new File( sysQuirk("user.home"))
  def userName() = sysQuirk("user.name")
  /**
   * @param path
   * @return
   */
  def trimLastPathSep(path:String) = {
    nsb(path).replaceFirst( "[/\\\\]+$", "")
  }

  /**
   * @param obj
   * @return
   */
  def serialize(obj:Serializable): Array[Byte] = SER.serialize(obj)

  /**
   * @param bits
   * @return
   */
  def deserialize(bits:Array[Byte]): Any = SER.deserialize( bits)

  /**
   * Get the class name this object belongs to.
   *
   * @param o
   * @return
   */
  def safeGetClzname(obj:Any) =
    obj match {
      case x:Any => x.getClass().getName
      case _ => "null"
    }

  /**
   * Get the canonical path name.
   *
   * @param fp
   * @return
   */
  def filePath(fp:File) = niceFPath(fp)

  /**
   * @return
   */
  def isWindows() = ! isUnix

  /**
   * @return
   */
  def isUnix() = _isUNIX

  /**
   * @param lst
   * @return
   */
  def isNilSeq(lst:Seq[_]) = { lst == null || lst.size == 0 }

  /**
   * @param m
   * @return
   */
  def isNilMap(m:Map[_,_]) = { m == null || m.size == 0 }

  /**
   * @param props
   * @return
   */
  def isNilPS(props:JPS) = { props == null || props.isEmpty }

  def sortAndJoin(str:String*) = {
    val a= str.toArray[Object]
    Arrays.sort( a )
    StrUtils.join(a, "")
  }

  /**
   * Convert string to int.
   *
   * @param s
   * @param dft default if string is null/empty.
   * @return
   */
  def asInt(s:String, dft:Int) = {
    try  {
      s.toDouble.toInt
    } catch {
      case e:Throwable => dft
    }
  }

  /**
   * Convert string to long.
   *
   * @param s
   * @param dft default if string is null/empty.
   * @return
   */
  def asLong(s:String, dft:Long) = {
    try  {
      s.toDouble.toLong
    } catch {
      case e:Throwable => dft
    }
  }

  /**
   * Convert string to double.
   *
   * @param s
   * @param def default if string is null/empty.
   * @return
   */
  def asDouble(s:String, dft:Double) = {
    try  {
      s.toDouble
    } catch {
      case e:Throwable => dft
    }
  }

  /**
   * Convert string to float.
   *
   * @param s
   * @param dft default if string is null/empty.
   * @return
   */
  def asFloat(s:String, dft:Float) = {
    try  {
      s.toDouble.toFloat
    } catch {
      case e:Throwable => dft
    }
  }

  /**
   * Convert string to boolean.
   *
   *  Valid values for Boolean.TRUE => "true", "yes", "on", "ok", "active", "1".
   *
   * @param s
   * @param dft default if string is null/empty.
   * @return
   */
  def asBool(s:String, dft:Boolean) = if ( BOOLS.contains(s)) true else dft


  /**
   * Convert bits to Properties.
   *
   * @param bin
   * @return
   */
  def asQuirks(bin:Array[Byte]): JPS = asQuirks(new ByteArrayIS(bin))

  /**
   * Convert to Array[Byte][].
   *
   * @param p
   * @return
   */
  def asBytes(p:JPS) = {
    if (p == null) Array[Byte]() else using( new ByteArrayOS(4096)) { (baos) =>
      p.store(baos, null)
      baos.toByteArray
    }
  }

  /**
   * Load the properties as a resource.
   *
   * @param rc
   * @param ldr
   * @return
   */
  def asQuirks(rc:String, cl:ClassLoader = null): JPS = {
    using (rc2Stream(rc, cl) ) { (inp) =>
      asQuirks(inp)
    }
  }

  /**
   * Read the stream as Properties.
   *
   * @param inp
   * @return
   */
  def asQuirks(inp:InputStream): JPS = {
    val p = new JPS()
    inp match {
      case i:InputStream => p.load(i)
      case _ =>
    }
    p
  }

  def asQuirks(f:File):JPS = {
    using(new FileInputStream(f)) { (inp) =>
      asQuirks(inp)
    }
  }

  /**
   * Load the resource as a stream.
   *
   * @param rc
   * @param cl
   * @return
   */
  def rc2Stream(rc:String, cl:ClassLoader = null ) = {
    if (STU.isEmpty(rc)) null else getCZldr(cl).getResourceAsStream(rc)
  }

  /**
   * @param rc
   * @param cl
   * @return
   */
  def rc2Url(rc:String, cl:ClassLoader= null) = {
    if (STU.isEmpty(rc)) null else getCZldr(cl).getResource(rc)
  }

  /**
   * Load resource as a string.
   *
   * @param rc
   * @param enc
   * @param cl
   * @return
   */
  def rc2Str(rc:String, enc:String, cl:ClassLoader= null) =
    using( rc2Stream(rc, cl)) { (inp) =>
      asString(bytes(inp),enc)
    }


  /**
   * Load resource as byte[].
   *
   * @param rc
   * @param cl
   * @return
   */
  def rc2Bytes(rc:String, cl:ClassLoader=null) =
    using( rc2Stream(rc, cl)) { (inp) =>
      bytes(inp)
    }

  /**
   * Compress the byte[].
   *
   * @param b
   * @return
   */
  def deflate(b:Array[Byte]): Array[Byte] = {
    b match {
      case bits:Array[Byte] if bits.length > 0 =>
        val buf = new Array[Byte](1024)
        val cpz = new Deflater()
        cpz.setLevel(Deflater.BEST_COMPRESSION)
        cpz.setInput(bits)
        cpz.finish
        using( new ByteArrayOS(bits.length)) { (bos) =>
          while ( !cpz.finished) {
            bos.write(buf, 0, cpz.deflate(buf))
          }
          bos.toByteArray
        }
      case _ => b
    }
  }

  /**
   * Decompress the byte[].
   *
   * @param b
   * @return
   */
  def inflate(b:Array[Byte]): Array[Byte] = {
    b match {
      case bits:Array[Byte] if bits.length > 0 =>
        var bos = new ByteArrayOS(bits.length)
        val buf = new Array[Byte](1024)
        val dec = new Inflater()
        dec.setInput(b)
        using(new ByteArrayOS(bits.length)) { (bos) =>
          while ( !dec.finished) {
            bos.write(buf, 0, dec.inflate(buf))
          }
          bos.toByteArray
        }
      case _ => b
    }
  }

  /**
   * Normalize the input by converting file-system unfriendly characters to hex values.
   *
   * @param fname
   * @return
   */
  def normalize(fname:String) = {
    (new StringBuilder /: (1 to nsb(fname).length) ) { (rc, i) =>
      val ch = fname.charAt(i-1)
      if (((ch >= 'A') && (ch <= 'Z'))
          || ((ch >= 'a') && (ch <= 'z'))
          || ((ch >= '0') && (ch <= '9'))
          || (ch == '_' || ch == '-' || ch == '.' || ch == ' '
          || ch == '(' || ch == ')')) {
        rc.append(ch)
      } else {
        rc.append("_0x" + Integer.toString(ch, 16))
      }
      rc
    } .toString

  }

  /**
   * Convert string to byte[], taking care of encoding.
   *
   * @param s
   * @param enc
   * @return
   */
  def asBytes(s:String, enc:String = "utf-8") = {
    if (s==null) null else s.getBytes(enc)
  }

  /**
   * Convert byte[] to string, using utf-8 as encoding.
   *
   * @param b
   * @return
   */
  def asString(b:Array[Byte], enc:String = "utf-8") = {
    if( b==null) null else new String(b, enc)
  }

  def asStr(b:ByteArrayOS, enc:String = "utf-8") = {
    if( b==null) null else new String(b.toByteArray, enc)
  }

  /**
   * Get the current time in millisecs, using the supplied timezone.
   *
   * @param tz
   * @return
   */
  def nowMillis(tz:String = "") = {
    if ( STU.isEmpty(tz))  System.currentTimeMillis() else
      (new GregorianCalendar(JTZone.getTimeZone(tz))).getTimeInMillis
  }

  /**
   * @param fileUrl
   * @return
   */
  def asFilePathOnly(fileUrl:String) = new java.net.URL(fileUrl).getPath

  /**
   * @param path
   * @return
   */
  def asFileUrl(path:String):String = {
    if (path==null) null else (if (isWindows) "file:/" else "file:") + path
  }

  /**
   * @param path
   * @return
   */
  def asFileUrl(fp:File):String = {
    if (fp==null) null else asFileUrl(niceFPath(fp))
  }

  /**
   * Create a temporary directory.
   *
   * @return
   */
  def genTmpDir() = fetchTmpDir("/" + uid() )

  /**
   * @return
   */
  def tmpDir() = fetchTmpDir("")


  private def fetchTmpDir(extra:String) = {
    val dir=new File( sysQuirk("java.io.tmpdir") + extra )
    dir.mkdirs
    dir
  }

  /**
   * @param param
   * @param child
   * @param parent
   */
  def tstArgIsType(param:String, child:Class[_], par:Class[_]) {
    require ( child !=null && par.isAssignableFrom(child ) ,
        "" + param + " not-isa " + par.getName() )
  }

  /**
   * @param param
   * @param value
   * @param ref
   */
  def tstArgIsType(param:String, value:Any, ref:Class[_]) {
    require ( value!=null && ref.isAssignableFrom(value.getClass ) ,
        "" + param + " not-isa " + ref.getName )
  }

  /**
   * @param msg
   */
  def errBadArg(msg:String) {
      throw new IllegalArgumentException(msg)
  }

  /**
   * @param root
   * @return
   */
  def findRootCause(root:Throwable) = {
    var t:Throwable = if (root == null) null else root.getCause
    var r=root
    while (t != null) {
      r = t ; t = t.getCause
    }
    r
  }

  /**
   * @param root
   * @return
   */
  def findRootCauseMsgWithClassInfo(root:Throwable) = {
    val e = findRootCause(root)
    if (e==null) "" else  e.getClass().getName() + ": " + e.getMessage
  }

  /**
   * @param root
   * @return
   */
  def findRootCauseMsg(root:Throwable) = {
    val e = findRootCause(root)
    if ( e==null ) "" else  e.getMessage
  }

  /**
   * @param var
   * @return
   */
  def sysVar(v:String) = if (STU.isEmpty(v)) null else System.getProperty(v)
  def envVar(v:String) = if (STU.isEmpty(v)) null else System.getenv(v)

  /**
   *
   */
  def uid() = new UID().toString.replaceAll("[:\\-]+", "")

  /**
   * @param ch
   * @param cs
   * @return
   */
  def matchChar(ch:Char, cs:Array[Char]) = if (cs == null) false else cs.contains(ch)

  /**
   * @return
   */
  def newRandom() = new SecureRandom( readAsBytes( System.currentTimeMillis))

  def genNumsBetween(start:Int, end:Int, howMany:Int) = {
    if (start >= end || ((end-start) < howMany)) Array[Int]() else {
      val _end = if (end < Int.MaxValue) { end + 1 } else end
      val rc= mutable.ArrayBuffer[Int]()
      val r= newRandom
      var cnt=howMany
      while (cnt >0) {
        val n = r.nextInt(_end)
        if ( n > 0 && !rc.contains(n)) { rc += n; cnt -= 1 }
      }
      rc.toSeq
    }
  }

  def nilToNichts(obj:Any): Any = if (obj == null) NICHTS else obj

  def isNichts(obj:Any) = {
    obj match {
      case x:AnyRef => NICHTS eq x
      case _ => false
    }
  }

  /**
   * @return
   */
  def fileUrlPfx() = if (isWindows) "file:/" else "file:"

  def asJObj(a:Any) = {
    if (a==null) null else a.asInstanceOf[Object]
  }

  def serialize(obj:Serializable, out:OutputStream) {
    SER.serialize(obj,out)
  }

  def deserialize(inp:InputStream): Any = SER.deserialize(inp)

  /**
   * @param param
   * @param value
   */
  def tstEStrArg(param:String , v:String ) {
    require( !STU.isEmpty(v), "" + param + " is empty")
  }

  /**
   * @param param
   * @param value
   */
  def tstNStrArg(param:String, v:String) { tstObjArg(param,v) }

  /**
   * @param param
   * @param value
   */
  def tstObjArg(param:String, v:Any) {
    require(v != null, "" + param + " is null")
  }

  /**
   * @param param
   * @param value
   */
  def tstArg(cond:Boolean, msg: => String) {
    require(cond, "" + msg)
  }

  /**
   * @param param
   * @param value
   */
  def tstNonNegIntArg(param:String, v:Int) {
    require( v >= 0,  "" + param + " must be >= 0 ")
  }

  /**
   * @param param
   * @param value
   */
  def tstNonNegLongArg(param:String, v:Long) {
    require( v >= 0L , "" + param + " must be >= 0L ")
  }

  /**
   * @param param
   * @param value
   */
  def tstPosLongArg(param:String, v:Long) {
    require( v > 0L,  "" + param + " must be a positive long")
  }

  /**
   * @param param
   * @param value
   */
  def tstPosIntArg(param:String, v:Int) {
    require( v > 0,  "" + param + " must be a positive integer")
  }

  /**
   * @param param
   * @param value
   */
  def tstNEArray(param:String, v:Seq[_]) {
    require( v!=null && v.length > 0,  "" + param + " must be non empty")
  }

  def parseJSON(s:String) = {
    val p = new JsonFactory().createParser( s )
    new ObjectMapper().readValue(p, classOf[JMap[String,_]])
  }
  
  private def cacheEnvVars() {
    _isUNIX = ! sysQuirk("os.name").hasic("windows")
  }

}

sealed class CoreUtils {}


object BTest {
import jregex._
  private def t1(args:Array[String]) {
    val ini:INIConf= new INIConf("/tmp/test.ini")
    if (ini != null) ini.sections.foreach { (s) =>
      println("++++++++")
      println(s)
      val pp= java.util.regex.Pattern.compile(s)
//      val m=pp.matcher("/public/a/b/c/d.css")
      val m=pp.matcher("/favicon.png")
      m.matches()
      val gc=m.groupCount()
      val g=m.toMatchResult().group(1)
      println("++++++++")
    }
  }

  private def t2(args:Array[String]) {
    val z=MetaUtils.loadClass("com.zotoh.jmx.support.TestOnly")
//    val c=z.getConstructor(classOf[String])
//    val obj=c.newInstance("poo").asInstanceOf[com.zotoh.jmx.support.TestOnly]
    val r= z.getDeclaredMethod("voider").getReturnType().getName()
    println("r="+r)
    val b= z.getDeclaredMethod("booler").getReturnType().getName()
    println("b="+b)
    val v= classOf[Void].getName()
    println("v="+v)
    val u=classOf[Unit].getName()
    println("u="+u)
    val q=classOf[Int].getName()
    println("q="+q)
    val ii=classOf[Integer].getName()
    println("ii="+ii)
    val bbb=classOf[Array[Byte] ].getName()
    println("XXXXX => " + bbb )
    val xxxxx=args.length
    val vvv= "sss"
    val cccc= vvv.getClass()
    println("bye")

/*
r=void
b=java.lang.Boolean
v=java.lang.Void
u=void
q=int
ii=java.lang.Integer
*/
  }

  private def t3(args:Array[String]) {
    val pp = new Pattern("/test/hello/(a|b)(c*)/({p1}[^/]+)/({p2}[^/]+)")
    val mc=pp.matcher("/test/hello/a/poo/head")
    if ( mc.matches()) for ( i <- 0 until mc.groupCount() ) {
      println( "i=" + i + ") " + mc.group(i) )
    }
   println( mc.group("p1") )
   println( mc.group("p2") )
  }
  private def t4(args:Array[String]) {
    val pp = new Pattern("/test/?")
    val mc=pp.matcher("/testff")
    if ( mc.matches()) {
      println("ok")
    } else { println("NOK")}
  }
  private def t5(args:Array[String]) {
    var ss = STU.split( "\u0000abc\u0000zzzz", "\u0000")
    ss=null
    var s= URLEncoder.encode("\u0000abc:xxx\u0000zzz:hhh", "utf-8")
    println(s)
    s= URLDecoder.decode(s, "utf-8")
    println(s)
    ss = STU.split( s, "\u0000")
    s=null
    ss= STU.split(":", ":")
    s=null
  }
  def main(args:Array[String] ) {
    t5(args)
  }

}
