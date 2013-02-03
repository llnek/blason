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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

/**
 * @author kenl
 *
 */
object HTTPStatus {

  val CONTINUE= HTTPStatus(100, "Continue")
  val SWITCHING_PROTOCOLS=HTTPStatus(101, "Switching Protocols")
  val PROCESSING=HTTPStatus(102, "Processing")
  val OK=HTTPStatus(200, "OK")
  val CREATED=HTTPStatus(201, "Created")
  val ACCEPTED=HTTPStatus(202, "Accepted")
  val NON_AUTHORITATIVE_INFORMATION=HTTPStatus(203, "Non-Authoritative Information")
  val NO_CONTENT=HTTPStatus(204, "No Content")
  val RESET_CONTENT=HTTPStatus(205, "Reset Content")
  val PARTIAL_CONTENT=HTTPStatus(206, "Partial Content")
  val MULTI_STATUS=HTTPStatus(207, "Multi-Status")
  val MULTIPLE_CHOICES=HTTPStatus(300, "Multiple Choices")
  val MOVED_PERMANENTLY=HTTPStatus(301, "Moved Permanently")
  val FOUND=HTTPStatus(302, "Found")
  val SEE_OTHER=HTTPStatus(303, "See Other")
  val NOT_MODIFIED=HTTPStatus(304, "Not Modified")
  val USE_PROXY=HTTPStatus(305, "Use Proxy")
  val TEMPORARY_REDIRECT=HTTPStatus(307, "Temporary Redirect")
  val BAD_REQUEST=HTTPStatus(400, "Bad Request")
  val UNAUTHORIZED=HTTPStatus(401, "Unauthorized")
  val PAYMENT_REQUIRED=HTTPStatus(402, "Payment Required")
  val FORBIDDEN=HTTPStatus(403, "Forbidden")
  val NOT_FOUND=HTTPStatus(404, "Not Found")
  val METHOD_NOT_ALLOWED=HTTPStatus(405, "Method Not Allowed")
  val NOT_ACCEPTABLE=HTTPStatus(406, "Not Acceptable")
  val PROXY_AUTHENTICATION_REQUIRED=HTTPStatus(407, "Proxy Authentication Required")
  val REQUEST_TIMEOUT=HTTPStatus(408, "Request Timeout")
  val CONFLICT=HTTPStatus(409, "Conflict")
  val GONE=HTTPStatus(410, "Gone")
  val LENGTH_REQUIRED=HTTPStatus(411, "Length Required")
  val PRECONDITION_FAILED=HTTPStatus(412, "Precondition Failed")
  val REQUEST_ENTITY_TOO_LARGE=HTTPStatus(413, "Request Entity Too Large")
  val REQUEST_URI_TOO_LONG=HTTPStatus(414, "Request-URI Too Long")
  val UNSUPPORTED_MEDIA_TYPE=HTTPStatus(415, "Unsupported Media Type")
  val REQUESTED_RANGE_NOT_SATISFIABLE=HTTPStatus(416, "Requested Range Not Satisfiable")
  val EXPECTATION_FAILED=HTTPStatus(417, "Expectation Failed")
  val UNPROCESSABLE_ENTITY=HTTPStatus(422, "Unprocessable Entity")
  val LOCKED=HTTPStatus(423, "Locked")
  val FAILED_DEPENDENCY=HTTPStatus(424, "Failed Dependency")
  val UNORDERED_COLLECTION=HTTPStatus(425, "Unordered Collection")
  val UPGRADE_REQUIRED=HTTPStatus(426, "Upgrade Required")
  val INTERNAL_SERVER_ERROR=HTTPStatus(500, "Internal Server Error")
  val NOT_IMPLEMENTED=HTTPStatus(501, "Not Implemented")
  val BAD_GATEWAY=HTTPStatus(502, "Bad Gateway")
  val SERVICE_UNAVAILABLE=HTTPStatus(503, "Service Unavailable")
  val GATEWAY_TIMEOUT=HTTPStatus(504, "Gateway Timeout")
  val HTTP_VERSION_NOT_SUPPORTED=HTTPStatus(505, "HTTP Version Not Supported")
  val VARIANT_ALSO_NEGOTIATES=HTTPStatus(506, "Variant Also Negotiates")
  val INSUFFICIENT_STORAGE=HTTPStatus(507, "Insufficient Storage")
  val NOT_EXTENDED=HTTPStatus(510, "Not Extended")

  /**
   * @param code
   * @return
   */
  def xref(code:Int):HTTPStatus = {
    code match {
      case 100 => CONTINUE
      case 101 => SWITCHING_PROTOCOLS
      case 102 => PROCESSING
      case 200 => OK
      case 201 => CREATED
      case 202 => ACCEPTED
      case 203 => NON_AUTHORITATIVE_INFORMATION
      case 204 => NO_CONTENT
      case 205 => RESET_CONTENT
      case 206 => PARTIAL_CONTENT
      case 207 => MULTI_STATUS
      case 300 => MULTIPLE_CHOICES
      case 301 => MOVED_PERMANENTLY
      case 302 => FOUND
      case 303 => SEE_OTHER
      case 304 => NOT_MODIFIED
      case 305 => USE_PROXY
      case 307 => TEMPORARY_REDIRECT
      case 400 => BAD_REQUEST
      case 401 => UNAUTHORIZED
      case 402 => PAYMENT_REQUIRED
      case 403 => FORBIDDEN
      case 404 => NOT_FOUND
      case 405 => METHOD_NOT_ALLOWED
      case 406 => NOT_ACCEPTABLE
      case 407 => PROXY_AUTHENTICATION_REQUIRED
      case 408 => REQUEST_TIMEOUT
      case 409 => CONFLICT
      case 410 => GONE
      case 411 => LENGTH_REQUIRED
      case 412 => PRECONDITION_FAILED
      case 413 => REQUEST_ENTITY_TOO_LARGE
      case 414 => REQUEST_URI_TOO_LONG
      case 415 => UNSUPPORTED_MEDIA_TYPE
      case 416 => REQUESTED_RANGE_NOT_SATISFIABLE
      case 417 => EXPECTATION_FAILED
      case 422 => UNPROCESSABLE_ENTITY
      case 423 => LOCKED
      case 424 => FAILED_DEPENDENCY
      case 425 => UNORDERED_COLLECTION
      case 426 => UPGRADE_REQUIRED
      case 500 => INTERNAL_SERVER_ERROR
      case 501 => NOT_IMPLEMENTED
      case 502 => BAD_GATEWAY
      case 503 => SERVICE_UNAVAILABLE
      case 504 => GATEWAY_TIMEOUT
      case 505 => HTTP_VERSION_NOT_SUPPORTED
      case 506 => VARIANT_ALSO_NEGOTIATES
      case 507 => INSUFFICIENT_STORAGE
      case 510 => NOT_EXTENDED
      case n if (n < 100) => HTTPStatus(n, "Unknown Status" )
      case n if (n < 200) => HTTPStatus(n, "Informational" )
      case n if (n < 300) => HTTPStatus(n, "Successful" )
      case n if (n < 400) => HTTPStatus(n, "Redirection" )
      case n if (n < 500) => HTTPStatus(n, "Client Error" )
      //case n if (n < 600) => HTTPStatus(n, "Server Error" )
      case _ => HTTPStatus(code, "Server Error" )
    }
  }

//  def apply(code:Int,reason:String) = new HTTPStatus(code,reason)
}

sealed case class HTTPStatus private(private val _code:Int, var reason:String) {
  private val _reasonStr = nsb(reason)

  /**
   * @return
   */
  def code() = _code

  /**
   * @return
   */
  def reasonPhrase() = _reasonStr

  override def toString() = "" + _code + " " + _reasonStr

  /**
   * @param code
   * @return
   */
  def isServerError(code:String) = {
    if (code==null) false else code.startsWith("50")
  }

  /**
   * @param code
   * @return
   */
  def isClientError(code:String) = {
    if (code==null) false
        else (code.startsWith("40")||code.startsWith("41")||
            code.startsWith("42")|| code.startsWith("44"))
  }

  /**
   * @param code
   * @return
   */
  def isRedirected(code:String) = {
    if (code==null) false else code.startsWith("30")
  }

  /**
   * @param code
   * @return
   */
  def isSuccess(code:String) = {
    if (code==null) false else code.startsWith("20")
  }

}
