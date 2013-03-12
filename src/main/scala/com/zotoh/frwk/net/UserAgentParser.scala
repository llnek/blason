/*??
 * COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.StrUtils._
import java.util.regex.Pattern

/**
 * @author kenl
 */
object UserAgentParser {

  def main(args:Array[String]) {

    var p= Pattern.compile("MSIE\\s*(\\S+)\\s*")
    var m = p.matcher( "MSIE   10.0;   " )
    if ( false && m.matches() ) {
      val c = m.groupCount()
      println (c)
      println( m.group(1) )
    }
    p= Pattern.compile("Windows\\s*Phone\\s*(\\S+)\\s*")
    m = p.matcher( "Windows Phone 8.0" )
    if ( m.matches() ) {
      val c = m.groupCount()
      println (c)
      println( m.group(1) )
    }


  }



}

object DeviceType extends Enumeration {
  type DeviceType=Value
  val PC = Value(0, "pc")
  val PHONE = Value(1, "phone")
  val TABLET = Value(2, "tablet")
  
}

/**
 * @author kenl
 */
class UserAgentParser(private val _uaStr:String) {
  import DeviceType._
  private var _browserVer=""
  private var _browser=""
  private var _deviceType= PC
  private var _deviceMoniker= "computer"

  parse(_uaStr.toLowerCase)

  def browserVersion = _browserVer
  def browser = _browser

  def deviceType = _deviceType
  def device = _deviceMoniker
  
  private def parse(ua:String) {
    if ( parse_ie(ua)) {}
    else
    if (parse_chrome(ua)) {}
    else
    if (parse_safari(ua)) {}
    else
    if (parse_ffox(ua)) {}
  }

  private def parse_chrome(ua:String) = {
    if ( _uaStr.indexOf("AppleWebKit/") > 0 && _uaStr.indexOf("Safari/") > 0 &&
    ua.indexOf("Chrome/") > 0 ) {
      var pos=_uaStr.indexOf("Chrome/")
      var s=_uaStr.substring(pos+7)
      pos=s.indexOf(" ")
      _browserVer = if (pos > 0) s.substring(0,pos) else s
      _browser="Chrome"
      true
    } else {
      false
    }
  }

  private def parse_ie(ua:String) = {
    if ( _uaStr.indexOf("Windows") > 0 &&  _uaStr.indexOf("Trident/") > 0 ) {
      var pos= _uaStr.indexOf("MSIE")
      var s=""
      if (pos > 0) {
        s = strim(_uaStr.substring(pos+4)  )
        pos= STU.indexOfAny(s, "; ")
        _browserVer = if (pos > 0) s.substring(0,pos) else s
      }
      if (ua.indexOf("windows phone")  > 0) { _deviceMoniker="Windows Phone" ; _deviceType= PHONE }
      if (ua.indexOf("iemobile")  > 0) { _deviceType= PHONE }
      _browser= "IE"
      true
    } else {
      false
    }
  }

  private def parse_ffox(ua:String) = {
    if ( _uaStr.indexOf("Gecko/") > 0 && _uaStr.indexOf("Firefox/") > 0 ) {
      var pos= _uaStr.indexOf("Firefox/")
      var s= _uaStr.substring(pos+8)
      pos= s.indexOf(" ")
      _browserVer= if (pos > 0) s.substring(0,pos) else s
      _browser="Firefox"
      true
    } else {
      false
    }
  }

  private def parse_safari(ua:String) = {
    if (_uaStr.indexOf("Safari/") > 0 && _uaStr.indexOf("Mac OS X") > 0) {
      var pos= _uaStr.indexOf("iPhone")
      var s=""
      if (pos > 0) { _deviceMoniker = "iPhone"; _deviceType= PHONE } else {
        pos= _uaStr.indexOf("iPad")
        if (pos > 0) { _deviceMoniker="iPad"; _deviceType = TABLET   }
      }
      pos= _uaStr.indexOf("Version/")
      if (pos > 0) {
        s= _uaStr.substring(pos+8)
        pos= s.indexOf(" ")
        _browserVer= if (pos > 0) s.substring(0,pos) else s
      }
      _browser="Safari"
      true
    } else {
      false
    }
  }

}
