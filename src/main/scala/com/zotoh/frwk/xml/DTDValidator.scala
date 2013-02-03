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

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.xml.XMLUtils._
import javax.xml.transform.sax.SAXSource.sourceToInputSource

import java.io.IOException
import java.io.InputStream
import java.net.URL

import javax.xml.transform.stream.StreamSource

import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader


/**
 * A XML reader that is preconfigured to do schema validation.
 *
 * @author kenl
 *
 */
class DTDValidator extends SaxHandler {

  private var _dtd:StreamSource = null

  /**
   * @param doc
   * @param dtd
   * @return
   */
  def scanForErrors(doc:InputStream, dtd:URL) = check(doc, dtd)

  override def resolveEntity(publicId:String, systemId:String) = sourceToInputSource(_dtd)

  private def check(doc:InputStream, dtd:URL) = {

    try {
      using(dtd.openStream) { (inp) =>
        if (! hasErrors) try {
          val rdr= newSaxValidator.getXMLReader
          _dtd= new StreamSource(inp)
          rdr.setContentHandler(this)
          rdr.setEntityResolver(this)
          rdr.setErrorHandler(this)
          rdr.parse(sourceToInputSource(new StreamSource(doc)))
        } catch {
          case e:SAXException => push(e)
          case e:Exception => push(new SAXException(e))
        }
      }
    } catch {
      case e:Exception => push(new SAXException(e))
    } finally {
      _dtd=null
    }

    hasErrors
  }

}
