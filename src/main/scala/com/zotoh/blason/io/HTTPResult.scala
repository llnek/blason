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

package com.zotoh.blason
package io

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.io.IOException

import com.zotoh.frwk.io.{XData}
import com.zotoh.frwk.net.HTTPStatus


/**
 * @author kenl
 *
 */
@SerialVersionUID(568732487697863245L)
class HTTPResult extends AbstractResult() {

  private val _headers= mutable.HashMap[String,String]()
  private var _data:XData= null
  private var _text:String= "OK"
  private var _code= 200

  def this(s:HTTPStatus) {
    this()
    setStatus(s)
  }

  def setData(data:String) { setData( new XData(data)) }
  def setData(d:XData) { _data=d }
  def data() = {
    if (_data==null) None else Some(_data)
  }

  def setErrorMsg(msg:String) {
    _text= nsb(msg)
    setError(true)
  }

  def errorMsg() = {
    if ( hasError()) {
      if ( STU.isEmpty(_text)) statusText() else _text
    } else {
      ""
    }
  }

  def setStatus(s:HTTPStatus) {
    setStatusText( s.reasonPhrase() )
    setStatusCode(s.code())
  }

  def setStatusCode(c:Int) { _code= c }
  def statusCode() = _code

  def setStatusText(s:String) { _text= nsb(s) }
  def statusText() = _text

  def clearAllHeaders() { _headers.clear() }
  def setHeader(h:String, v:String) {
    if (! STU.isEmpty(h)) { _headers += Tuple2(h, nsb(v)) }
  }
  def headers() = _headers.toMap


}

