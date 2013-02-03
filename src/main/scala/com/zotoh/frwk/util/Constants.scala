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

/**
 * General constants.
 *
 * @author kenl
 *
 */
trait Constants {

  val TS_REGEX= "^\\d\\d\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])\\s\\d\\d:\\d\\d:\\d\\d"
  val DT_REGEX= "^\\d\\d\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$"
  val TS_FMT_NANO="yyyy-MM-dd HH:mm:ss.fffffffff"
  val TS_FMT="yyyy-MM-dd HH:mm:ss"

  val DT_FMT_MICRO= "yyyy-MM-dd' 'HH:mm:ss.SSS"
  val DT_FMT= "yyyy-MM-dd' 'HH:mm:ss"
  val DATE_FMT= "yyyy-MM-dd"

  val ISO8601_FMT= "yyyy-MM-dd' 'HH:mm:ss.SSSZ"
    
  val USASCII= "ISO-8859-1"
  val UTF16="UTF-16"
  val UTF8="UTF-8"
  val SLASH = "/"
  val PATHSEP = SLASH

  val BOOLS=Set( "true", "yes", "on", "ok", "active", "1")

  val MONTHS = Array( "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" )

}

