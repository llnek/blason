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

    val s= """
  Mozilla/5.0 (Linux; U; Android 4.1.1; en-us; SAMSUNG-SGH-I747 Build/JRO03L) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30


     """
    println( new UserAgentParser(s))

  }



}

/**
 * @author kenl
 */
object DeviceType extends Enumeration {
  type DeviceType=Value
  val MOBILE= Value(0, "mobile")
  val PC = Value(1, "pc")
  val PHONE = Value(2, "phone")
  val TABLET = Value(3, "tablet")
  
}

/**
 * @author kenl
 */
class UserAgentParser(uaStr:String) {
  import DeviceType._
  
  private val _uaStr = strim(uaStr)
  private var _browserVer=""
  private var _browser=""
  private var _deviceType= PC
  private var _deviceVer = ""
  private var _deviceMoniker= "computer"
  
  parse(_uaStr.toLowerCase )

  def browserVersion = _browserVer
  def browser = _browser

  def deviceType = _deviceType
  def deviceVersion = _deviceVer
  def device = _deviceMoniker
  
  override def toString() = {
    java.lang.String.format("%1$s:%2$s || %3$s:%4$s || %5$s", _browser, _browserVer, _deviceType, _deviceVer, _deviceMoniker)
  }
  
  private def parse(ua:String) {
    if ( parse_ie(ua)) {}
    else
    if (parse_chrome(ua)) {}
    else
    if (parse_android(ua)) {}
    else
    if (parse_kindle(ua)) {}
    else
    if (parse_safari(ua)) {}
    else
    if (parse_ffox(ua)) {}
  }

  private def parse_chrome(ua:String) = {
    if ( _uaStr.indexOf("AppleWebKit/") > 0 && _uaStr.indexOf("Safari/") > 0 &&
    _uaStr.indexOf("Chrome/") > 0 ) {
      var p= Pattern.compile(".*(Chrome/(\\S+)).*")
      var m=p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
        _browserVer = cleanStr( m.group(2))
      }
      _browser="Chrome"
      true
    } else {
      false
    }
  }

  private def parse_kindle(ua:String) = {
    if ( _uaStr.indexOf("AppleWebKit/") > 0 && _uaStr.indexOf("Safari/") > 0 &&
    _uaStr.indexOf("Silk/") > 0 ) {
      var p= Pattern.compile(".*(Silk/(\\S+)).*")
      var m=p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
        _browserVer = cleanStr( m.group(2))
      }
      _browser="Silk"
      _deviceType= TABLET
      _deviceMoniker="kindle"
      true
    } else {
      false
    }
  }
  
  private def parse_android(ua:String) = {
    if ( _uaStr.indexOf("AppleWebKit/") > 0 && _uaStr.indexOf("Safari/") > 0 &&
    _uaStr.indexOf("Android") > 0 ) {
      var p= Pattern.compile(".*(Android\\s*(\\S+)\\s*).*")
      var m=p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
        _browserVer = cleanStr( m.group(2))
      }
      _browser="Chrome"
      _deviceType= MOBILE
      _deviceMoniker="Android"
      true
    } else {
      false
    }
  }
  
  private def parse_ie(ua:String) = {
    if ( _uaStr.indexOf("Windows") > 0 &&  _uaStr.indexOf("Trident/") > 0 ) {
      var p= Pattern.compile(".*(MSIE\\s*(\\S+)\\s*).*")
      var m= p.matcher(_uaStr)
      var gc=0
      if (m.matches()) {
        gc=m.groupCount()
        _browserVer= cleanStr(m.group(2))
      }
      p= Pattern.compile(".*(Windows\\s*Phone\\s*(\\S+)\\s*).*")
      m= p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
         _deviceMoniker="Windows Phone"
         _deviceType= PHONE
         _deviceVer= cleanStr(m.group(2))
      }
      if (ua.indexOf("iemobile")  > 0) { _deviceType= PHONE }
      _browser= "IE"
      true
    } else {
      false
    }
  }

  private def parse_ffox(ua:String) = {
    if ( _uaStr.indexOf("Gecko/") > 0 && _uaStr.indexOf("Firefox/") > 0 ) {
      var p= Pattern.compile(".*(Firefox/(\\S+)\\s*).*")
      var m= p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
        _browserVer= cleanStr(m.group(2))
      }
      _browser="Firefox"
      true
    } else {
      false
    }
  }

  private def parse_safari(ua:String) = {
    if (_uaStr.indexOf("Safari/") > 0 && _uaStr.indexOf("Mac OS X") > 0) {
      var p= Pattern.compile(".*(Version/(\\S+)\\s*).*")
      var m= p.matcher(_uaStr)
      if (m.matches() && m.groupCount() == 2) {
        _browserVer= cleanStr(m.group(2))
      }
      if (_uaStr.indexOf("Mobile/") > 0) {
        _deviceType=MOBILE
      }
      if (_uaStr.indexOf("iPhone") > 0) {
        _deviceMoniker = "iPhone"
        _deviceType= PHONE         
      }
      else if (_uaStr.indexOf("iPad") > 0) {
        _deviceMoniker = "iPad"
        _deviceType= TABLET         
      }
      else if (_uaStr.indexOf("iPod") > 0) {
        _deviceMoniker = "iPod"
//        _deviceType= TABLET         
      }
      _browser="Safari"
      true
    } else {
      false
    }
  }

  private def cleanStr(s:String) = {
    STU.stripStart(STU.stripEnd(s, ";,"), ";,")
  }
  
}
