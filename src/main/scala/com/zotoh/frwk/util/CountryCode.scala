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

import scala.collection.mutable.HashMap

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

/**
 * A class that maps the country-code to the country-name.
 *
 * @author kenl
 *
 */
object CountryCode extends CCodeSet {

  private val _names=  (Map[String,String]() /: CCODES) { (rc,t) => rc + t  }
  
  def country(code:String) = CCODES.get(code)

  def code(country:String) = _names.get(country)

  def isUSA(code:String) =  { USACode() == code }

  def codes() = CCODES.keySet.toSeq

  def USACode() =  "US"

}

