/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import scala.collection.mutable
import scala.util.Sorting

import java.io.{IOException,CharArrayWriter,File,OutputStream,OutputStreamWriter,Reader,Writer}
import java.util.{Arrays,Collection,Iterator,StringTokenizer}

import com.zotoh.frwk.io.IOUtils._

/**
 * @author kenl
 *
 */
object StrUtils extends CoreImplicits {

  def same(a: String, b: String) = {
    if (a.length != b.length) false else {
      Arrays.equals(a.toCharArray, b.toCharArray )
    }
  }
  
  def strim(obj:Any) = {
    nsb(obj).trim
  }

  /**
   * Append to a string-builder, optionally inserting a delimiter if the buffer is not
   * empty.
   *
   * @param buf
   * @param delim
   * @param item
   * @return
   */
  def addAndDelim(buf:StringBuilder, delim:String, item:String) = {
    if (item != null) {
      if (buf.length > 0 && delim != null) { buf.append(delim) }
      buf.append(item)
    }
    buf
  }

  def qsort(ids:Array[String]) = Sorting.quickSort(ids)

  /**
   * Split a large string into chucks, each chunk having a specific length.
   *
   * @param src
   * @param chunkLength
   * @return
   */
  def splitIntoChunks(original:String, chunkLength:Int) = {
    val ret= mutable.ArrayBuffer[String]()
    var src=original
    if (src != null) {
      while (src.length > chunkLength) {
        ret += src.substring(0, chunkLength)
        src = src.substring(chunkLength)
      }
      if (src.length > 0) {   ret += src }
    }
    ret.toSeq
  }

  /**
   * Tests String.indexOf() against a list of possible args.
   *
   * @param src
   * @param strs
   * @return
   */
  def hasWithin(src:String, strs:Seq[String]) = {
    if (src == null) false else strs.exists { (s) => src.has(s) }
  }

  /**
   * Tests startWith(), looping through the list of possible prefixes.
   *
   * @param src
   * @param pfxs
   * @return
   */
  def startsWith(src:String, pfxs:Seq[String]) = {
    if(src==null) false else pfxs.exists { (s) => src.startsWith(s) }
  }

  /**
   * Tests String.equals() against a list of possible args. (ignoring case)
   *
   * @param original
   * @param args
   * @return
   */
  def equalsOneOfIC(src:String, args:Seq[String]) = {
    if (src==null) false else args.exists { (s) => src.equalsIgnoreCase(s) }
  }

  /**
   * Tests String.equals() against a list of possible args.
   *
   * @param src
   * @param args
   * @return
   */
  def equalsOneOf(src:String, args:Seq[String]) = {
    if (src==null) false else args.exists { (s) => src==s }
  }

  /**
   * Tests String.indexOf() against a list of possible args. (ignoring case).
   *
   * @param src
   * @param strs
   * @return
   */
  def hasWithinIC( src:String, strs:Seq[String]) = {
    if (src == null) false else strs.exists { (s) => src.hasic(s) }
  }

  /**
   * Tests startsWith (ignore-case).
   *
   * @param original source string.
   * @param pfxs list of prefixes to test with.
   * @return
   */
  def startsWithIC(src:String, pfxs:Seq[String]) = {
    if(src==null) false else pfxs.exists { (s) => src.swic(s) }
  }

  /**
   * Safely call toString().
   *
   * @param o
   * @return "" if null.
   */
  def nsb(o:Any) = {
    if ( o == null ) "" else o.toString
  }

  /**
   * @param o
   * @return
   */
  def nsn(o:Any) = {
    if ( o == null ) "(null)" else o.toString
  }

  /**
   * @param iter
   * @param sep
   * @return
   */
  def join(objs:Seq[Any], sep:String="") = {
    (new StringBuilder /: objs) { (rc,obj) =>
      addAndDelim(rc, sep, nsn(obj))
    }.toString
  }

  /**
   * @param c
   * @param times
   * @return
   */
  def mkString(c:Char, times:Int) = {
    (new StringBuilder /: (1 to times) ) { (rc,i) => rc.append(c) } .toString
  }

}
