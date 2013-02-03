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
package xml

import javax.xml.transform.sax.SAXSource.sourceToInputSource

import java.io.IOException
import java.io.InputStream
import java.net.URL

import javax.xml.transform.stream.StreamSource

import org.xml.sax.SAXException
import org.xml.sax.XMLReader

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils

/**
 * Implementation of a simple XML scanner, which basically scans for xml syntax errors, nothing more.
 *
 * @author kenl
 *
 */
class XMLScanner extends SaxHandler  {

  /**
   * @param doc
   * @return
   */
  def scan(doc:URL): Boolean = {

    var ok=false
    try {
      using (doc.openStream) { (inp) =>
        ok=scan(inp)
      }
    } catch {
      case e:Exception => push(new SAXException(e))
    }

    ok
  }

  /**
   * @param doc
   * @return
   */
  def scan(doc:InputStream): Boolean = {

    try {
      val rdr= XMLUtils.newSaxParser.getXMLReader
      rdr.setContentHandler(this)
      rdr.setEntityResolver(this)
      rdr.setErrorHandler(this)
      rdr.parse(sourceToInputSource(new StreamSource(doc)))
    } catch {
      case e:SAXException => push(e)
      case e:Exception => push(new SAXException(e))
    }

    ! hasErrors
  }


}
