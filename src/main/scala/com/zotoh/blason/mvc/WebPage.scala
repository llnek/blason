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
package mvc

import java.io.File
import com.zotoh.blason.core.Constants

import com.zotoh.frwk.mime.MimeUtils._
import com.zotoh.frwk.io.IOUtils._


/**
 * @author kenl
 */
object WebPage extends Constants {

  private var _appDir:File = null

  def setup(appDir:File) {
    _appDir= appDir
  }

  def getTemplate(id:String) = {
    mkPage(DN_PAGES + "/" + DN_TEMPLATES + "/" + id )
  }

  def getView(id:String) = {
    mkPage( DN_PAGES + "/" + DN_VIEWS + "/" + id )
  }

  private def mkPage(part:String) = {
    val f = new File(_appDir, part)
    if (f.canRead ) {
      Some( new WebPage( readText(f) , guessContentType(f, "utf-8") ) )
    } else {
      None
    }
  }

}

/**
 * @author kenl
 */
class WebPage(private val _body:String, private val _cType:String) {

  def contentType = _cType
  def body = _body

}
