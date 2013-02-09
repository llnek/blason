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

import java.util.List
import org.apache.commons.fileupload.FileItemFactory
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.FileUploadException
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.blason.io.HTTPEvent
import org.apache.commons.fileupload.RequestContext

/**
 * @author kenl
 */
object NettyFileUpload extends CoreImplicits {

  def isMultipartContent(evt:HTTPEvent) = {
    if ( "post" != evt.method.lc) false else {
      evt.contentType.lc.startsWith("multipart/")
    }
  }

}

/**
 * @author kenl
 */
class NettyFileUpload(fac:FileItemFactory) extends FileUpload(fac) {

  def this() {
    this(null)
  }

  def parseRequest(evt:HTTPEvent) = {
      super.parseRequest( new NettyRequestContext(evt))
  }

  def getItemIterator(evt:HTTPEvent) = {
    super.getItemIterator(new NettyRequestContext(evt))
  }

}
