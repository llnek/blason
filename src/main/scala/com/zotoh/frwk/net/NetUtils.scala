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

import java.net._
import java.lang.{Character=>JCH}
import java.io.InputStream

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import scala.Some


object NetUtils extends CoreImplicits {

  def mkTinyUrl(url:URL) = {
    val target= "http://tinyurl.com/api-create.php?url="+url.toExternalForm()
    val cli= new SyncHTTPClient("text/plain").withSocTOutMillis( 5000 )
    cli.connect(new URI(target)).get match {
      case Some(x:String) => strim(x)
      case _ => ""
    }
  }

  /**
   * Get the current machine/interface's IP Address.
   *
   * @return
   */
  def localAddr() = InetAddress.getLocalHost().getHostAddress()

  /**
   * Get the current machine/interface's name.
   *
   * @return
   */
  def localHost() = InetAddress.getLocalHost().getHostName()

  def hostByName(name:String) = InetAddress.getByName(name)

  def close(s:ServerSocket) {
    using(s) { (s) => }
  }

  def close(s:Socket) {
    using(s) { (s) => }
  }

  /**
   * Parse the string into int.
   *
   * @param s
   * @return
   */
  def portAsInt(s:String) = asInt(s, -1)

  /**
   * Given a string of the form <host name>[:<TCP port>] return the host name
   * (or IP address)
   *
   * @param token
   * @return (host, port) as Tuple
   */
  def parseHostPort(token:String): (String,Int) = {
    val s= STU.trim(token)
    s.lastIndexOf(":") match {
      case pos if (pos >= 0) =>
        (s.substring(0,pos), asInt(s.substring(pos+1),-1))
      case _ => (s, -1)
    }
  }

  /**
   * Converts IPv4 address from its textual presentation form into its numeric
   * binary form.
   *
   * @param ipv4
   * @return
   */
  def ipv4AsBytes(ipv4:String) = {
    val srcb= STU.trim(ipv4).toCharArray
    val dst = new Array[Byte](4)
    var gotDigit = false
    var octets= 0
    var cur=0
    var i=0
    var error=false

    while (!error && i < srcb.length) {
      srcb(i) match {
      case ch if (JCH.isDigit(ch)) =>
        // java byte is signed, need to convert to int
        val sum = (dst(cur) & 0xff) * 10 + (JCH.digit(ch, 10) & 0xff)
        if (sum > 255) {  error=true   } else {
        dst(cur) = (sum & 0xff).asInstanceOf[Byte]
        if (! gotDigit) {
          octets += 1
          if (octets > 4) {  error=true } else {
          gotDigit = true
          }
        }
        }
      case ch if (ch == '.' && gotDigit) =>
        if (octets == 4) {  error=true   } else {
        cur += 1
        dst(cur) = 0
        gotDigit = false
        }
      case _ =>
        error=true
      }

      i += 1
    }

    if (error || octets < 4) null else dst
  }

  /**
   * @param url
   * @return
   */
  def resolveAndExpandFileUrl(url:String) = {
    if (url==null) null else
      if (url.startsWith("file:")) expandW32Url(expandUNXUrl(url)) else url
  }

  /**
   * Make the domain name lowercase, keeping the name-id part
   * case sensitive.
   *
   * @param email
   * @return
   */
  def canonicalizeEmailAddress(email:String) = {

    val pos=nsb(email).indexOf("@")
    if (pos > 0) {
      email.substring(0,pos) + email.substring(pos).lc
    } else {
      email
    }
  }

  /**
   * @param url
   * @return
   * @throws URISyntaxException
   */
  def getPort(url:String) = {
    if (url==null) -1 else new URI(url).getPort()
  }

  /**
   * @param uri
   * @return
   * @throws URISyntaxException
   */
  def getHostPartUri(uri:String) = {
    if (uri==null) null else new URI(uri).getHost()
  }

  /**
   * @param soc
   * @return
   * @throws IOException
   */
  def sockItAsBits(soc:InputStream) = readBytes(soc)

  private def expandW32Url(url:String):String = {

    var last = url.length() - 1
    var head = url.indexOf('%')
    var tail = if (head >= 0 && head < last) {
      url.indexOf('%', head + 1)
    } else {
      -1
    }
    if (head >= 0 && tail > head) {

      val v= url.substring(head + 1, tail)
      val rt = url.substring(tail + 1)
      val lf = url.substring(0, head)

      var env = System.getProperty(v)
      if (env == null) {
        env = System.getenv(v)
      }
      expandW32Url( lf + nsb(env) + rt )

    } else {
      url
    }
  }

  private def expandUNXUrl(url:String): String = {

    val head = url.indexOf("${")
    val last = url.length() - 1
    val tail = if (head  >= 0 && head < last) {
      url.indexOf('}', head + 2)
    } else { -1 }

    if (head >= 0 && tail > head) {

      var v = url.substring(head + 2, tail)
      val rt = url.substring(tail + 1)
      val lf = url.substring(0, head)

      var env = System.getProperty(v)
      if (env == null) {
        env = System.getenv(v)
      }
      expandUNXUrl(  lf + nsb(env) + rt)

    } else {
      url
    }

  }

}

