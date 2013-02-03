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
package i18n

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.StrUtils._
import java.util.{ResourceBundle,Locale}

/**
 * @author kenl
 */
class Resources(private val _baseName:String, private val _loc:Locale, cldr:ClassLoader=null) {

  private val _bundle= getBundle(cldr)

  def getString( key:String, pms:Any*) = {
    ( nsb( _bundle.getString(key)) /: pms) { (bc, p) =>
      STU.replace(bc, "{}", p.toString(), 1)
    }
  }

  private def getBundle(cl:ClassLoader) = {
    val c= if (cl==null) Thread.currentThread().getContextClassLoader else cl
    ResourceBundle.getBundle( _baseName, _loc, c)
  }

}
