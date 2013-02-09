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

import java.io.InputStream
import org.apache.commons.fileupload.RequestContext
import com.zotoh.blason.io.HTTPEvent

/**
 * @author kenl
 */
class NettyRequestContext(private val _evt:HTTPEvent) extends RequestContext {

  def getCharacterEncoding() = _evt.encoding

  def getContentType() = _evt.contentTypeLine

  def getContentLength() = _evt.contentLength.toInt

  def getInputStream() = {
    if ( _evt.hasData ) _evt.data.stream() else null
  }

  override def toString() = {
    "ContentLength=" + 
          this.getContentLength() +
          ", ContentType=" +
          this.getContentType()
  }

}
