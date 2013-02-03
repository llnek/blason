/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL
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

import scala.collection.JavaConversions._
import scala.collection.mutable
import java.net.{URLDecoder,URI}

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrArr
import com.zotoh.frwk.util.StrUtils._



/**
 * @author kenl
 *
 */
class UriMatrixDecoder(private val _uri:String, private val _charset:String="utf-8") {

  private var _params:Map[String,StrArr]= null
  private var _path:String = null

  def this(uri:URI, charset:String) {
    this( uri.toASCIIString, charset)
  }

  def path() = {
    if ( _path==null) {
      val pos = _uri.indexOf(';')
      _path = nsb( if (pos < 0) _uri else _uri.substring(0, pos) )
    }
    _path
  }

  /**
   * Returns the decoded key-value parameter pairs of the URI.
   *
   * @return
   */
  def params() = {
    if (_params == null) {
      val pathLength = path().length
      _params = if (_uri.length == pathLength) Map[String,StrArr]() else {
        decodeParams(_uri.substring(pathLength + 1))
      }
    }
    _params
  }

  private def decodeParams(s:String) = {
    var params = mutable.LinkedHashMap[String, StrArr]()
    var name:String = null
    var pos = 0 // Beginning of the unprocessed region
    var i=0

    for (i <- 0 until s.length) {
      val c = s.charAt(i)
      if (c == '=' && name == null) {
        if (pos != i) {
          name = decodeComponent(s.substring(pos, i), _charset)
        }
        pos = i + 1
      } else if (c == ';') {
        if (name == null && pos != i) {
          // We haven't seen an `=' so far but moved forward.
          // Must be a param of the form ';a;' so add it with
          // an empty value.
          addParam(params, decodeComponent(s.substring(pos, i), _charset), "")
        } else if (name != null) {
          addParam(params, name, decodeComponent(s.substring(pos, i), _charset))
          name = null
        }
        pos = i + 1
      }
    }

    //i= s.length()

    if (pos != i) {  // Are there characters we haven't dealt with?
      if (name == null) {   // Yes and we haven't seen any `='.
        addParam(params, decodeComponent(s.substring(pos, i), _charset), "")
      } else {        // Yes and this must be the last value.
        addParam(params, name, decodeComponent(s.substring(pos, i), _charset))
      }
    } else if (name != null) {  // Have we seen a name without value?
      addParam(params, name, "")
    }

    params.toMap
  }

  private def decodeComponent(s:String, charset:String) = {
    if (s==null) "" else URLDecoder.decode(s, charset)
  }

  private def addParam( params: mutable.LinkedHashMap[String,StrArr], name:String, value:String) {
    params.get(name) match {
      case None => params += name ->  new StrArr()
      case _ =>
    }
    params(name).add(value)
  }




}
