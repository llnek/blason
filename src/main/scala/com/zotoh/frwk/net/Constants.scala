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

/**
 * @author kenl
 *
 */
trait Constants {

  val LHOST= "localhost"

  val WP_HTTP= "HTTP"
  val WP_SMTP= "SMTP"
  val WP_SFTP= "SFTP"
  val WP_FTP= "FTP"
  val WP_FILE= "FILE"

  /** 2XX: generally "OK" */
  val HTTP_OK = 200
  val HTTP_CREATED = 201
  val HTTP_ACCEPTED = 202
  val HTTP_NOT_AUTHORITATIVE = 203
  val HTTP_NO_CONTENT = 204
  val HTTP_RESET = 205
  val HTTP_PARTIAL = 206

  /** 3XX: relocation/redirect */
  val HTTP_MULT_CHOICE = 300
  val HTTP_MOVED_PERM = 301
  val HTTP_MOVED_TEMP = 302
  val HTTP_SEE_OTHER = 303
  val HTTP_NOT_MODIFIED = 304
  val HTTP_USE_PROXY = 305

  /** 4XX: client error */
  val HTTP_BAD_REQUEST = 400
  val HTTP_UNAUTHORIZED = 401
  val HTTP_PAYMENT_REQUIRED = 402
  val HTTP_FORBIDDEN = 403
  val HTTP_NOT_FOUND = 404
  val HTTP_BAD_METHOD = 405
  val HTTP_NOT_ACCEPTABLE = 406
  val HTTP_PROXY_AUTH = 407
  val HTTP_CLIENT_TIMEOUT = 408
  val HTTP_CONFLICT = 409
  val HTTP_GONE = 410
  val HTTP_LENGTH_REQUIRED = 411
  val HTTP_PRECON_FAILED = 412
  val HTTP_ENTITY_TOO_LARGE = 413
  val HTTP_REQ_TOO_LONG = 414
  val HTTP_UNSUPPORTED_TYPE = 415

  /** 5XX: server error */
  val HTTP_SERVER_ERROR = 500
  val HTTP_INTERNAL_ERROR = 501
  val HTTP_BAD_GATEWAY = 502
  val HTTP_UNAVAILABLE = 503
  val HTTP_GATEWAY_TIMEOUT = 504
  val HTTP_VERSION = 505

}

