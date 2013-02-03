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
package mime

/**
 * @author kenl
 *
 */
trait Constants {

  val  CTE_QUOTED= "quoted-printable"
  val  CTE_7BIT= "7bit"
  val  CTE_8BIT= "8bit"
  val  CTE_BINARY= "binary"
  val  CTE_BASE64= "base64"

  val   MIME_USER_PROP  = "mime.rfc2822.user"
  val   MIME_USER_JAVAMAIL   = "javamail"
  val   DEF_USER  = "popeye"
  val   MIME_USER_PREFIX   = "zotoh"
  val   DEF_HOST  = "localhost"
  val    MIME_HEADER_MSGID  = "Message-ID"
  val  MIME_MULTIPART_BOUNDARY  = "boundary"
  val   DOT   = "."
  val   AT  = "@"
  val CH_DOT   = '.'
  val CH_AT  = '@'
  val   STR_LT   = "<"
  val  STR_GT  = ">"
  val  ALL   = -1
  val   ALL_ASCII   = 1
  val  MOSTLY_ASCII   = 2
  val  MOSTLY_NONASCII   = 3

  // Capitalized MIME constants to use when generating MIME headers
  // for messages to be transmitted.
  val  AS2_VER_ID    = "1.1"
  val  UA  = "user-agent"
  val  TO   = "to"
  val  FROM  = "from"
  val  AS2_VERSION    = "as2-version"
  val  AS2_TO   = "as2-to"
  val  AS2_FROM  = "as2-from"
  val  SUBJECT    = "subject"
  val  CONTENT_TYPE  = "content-type"
  val  CONTENT     = "content"
  val  CONTENT_NAME   = "content-name"
  val  CONTENT_LENGTH  = "content-length"
  val  CONTENT_LOC  = "content-Location"
  val  CONTENT_ID    = "content-id"
  val  CONTENT_TRANSFER_ENCODING  = "content-transfer-encoding"
  val  CONTENT_DISPOSITION   = "content-disposition"
  val  DISPOSITION_NOTIFICATION_TO  = "disposition-notification-to"
  val  DISPOSITION_NOTIFICATION_OPTIONS  = "disposition-notification-options"
  val  SIGNED_REC_MICALG= "signed-receipt-micalg"
  val  MESSAGE_ID   = "message-id"
  val  ORIGINAL_MESSAGE_ID   = "original-message-id"
  val  RECEIPT_DELIVERY_OPTION   = "receipt-delivery-option"
  val  DISPOSITION  = "disposition"
  val  DATE    = "date"
  val  MIME_VERSION   = "mime-version"
  val  FINAL_RECIPIENT   = "final-recipient"
  val  ORIGINAL_RECIPIENT   = "original-recipient"
  val  RECV_CONTENT_MIC   = "received-content-mic"

  val  RFC822= "rfc822"
  val  RFC822_PFX= RFC822 + "; "

  val  APP_XML= "application/xml"
  val  TEXT_PLAIN= "text/plain"
  val  APP_OCTET= "application/octet-stream"
  val  PKCS7SIG= "pkcs7-signature"
  val  TEXT_HTML = "text/html"
  val  TEXT_XML = "text/xml"
  val  MSG_DISP = "message/disposition-notification"

  val  ERROR   = "error"
  val  FAILURE = "failure"
  val  WARNING  = "warning"
  val  HEADERS  = "headers"

  val  ISO_8859_1 = "iso-8859-1"
  val  US_ASCII = "us-ascii"

  val  CRLF= "\r\n"
}
