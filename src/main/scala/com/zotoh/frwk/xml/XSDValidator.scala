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

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL

import javax.xml.XMLConstants
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException

object XSDValidator {

  private val _xsdFac= SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

  def getSV(src:URL, err:ErrorHandler) = {
    val xsd= _xsdFac.newSchema( src)
    val v= xsd.newValidator
    v.setErrorHandler(err)
    v
  }

}

/**
 * A XML reader that is preconfigured to do schema validation.
 *
 * @author kenl
 *
 */
class XSDValidator extends SaxHandler  {


  /**
   * @param doc
   * @param sd
   * @return
   * @throws MalformedURLException
   */
  def scan(doc:InputStream, sd:File):Boolean = check(doc, sd.toURI().toURL() )


  /**
   * @param doc
   * @param sd
   * @return
   */
  def scan(doc:InputStream, sd:URL):Boolean = check(doc, sd)

  private def check(doc:InputStream, xsd:URL) = {
    try    {
      XSDValidator.getSV(xsd, this).
      validate( new SAXSource( sourceToInputSource( new StreamSource( doc))))
    } catch {
      case e:SAXException => push(e)
      case e:Exception => push( new SAXException(e))
    }

    ! hasErrors
  }


}
