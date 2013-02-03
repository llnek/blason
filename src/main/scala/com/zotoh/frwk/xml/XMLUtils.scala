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

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.{TreeMap=>JTM}

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException


object ValidationStyle {
    val NONE= "None"
    val DTD="dtd"
    val XSD="xsd"
}

/**
 * Common Util functions.
 *
 * @author kenl
 *
 */
object XMLUtils extends Constants {

//  private final String NSP_FEATURE= "http://xml.org/sax/features/namespace-prefixes"

  private val _log= LoggerFactory.getLogger(classOf[XMLUtils])
  def tlog() = _log

  private val _dfac = DocumentBuilderFactory.newInstance()
  private var _sfac = SAXParserFactory.newInstance()

  /**
   * @return
   * @throws SAXException
   */
  def newSaxValidator() = newParser(true, true)

  /**
   * @return
   * @throws SAXException
   */
  def newSaxParser() = newParser(true, false)

  /**
   * @param s
   * @return
   */
  def mkInputSource(s:InputStream) =
    new InputSource(new BufferedInputStream(s))

  /**
   * @param atts
   * @return
   */
  def attrsToLNMap(atts:Attributes) = {
    attrsToMap(atts, false)
  }

  /**
   * @param atts
   * @return
   */
  def attrsToQNMap(atts:Attributes) = attrsToMap(atts, true)


  /**
   * @param atts
   * @return
   */
  def attributesToString(atts:Map[String,String]) =  {
    val buf= new StringBuilder(1024)
    if (atts != null) atts.foreach { (t) =>
      buf.append(" ").
      append(t._1).
      append("=\"").
      append(t._2).
      append("\"")
    }
    buf.toString
  }


  /**
   * @param out
   * @param s
   * @param enc
   * @throws IOException
   */
  def write(out:OutputStream, s:String, enc:String) {
    if (! STU.isEmpty(s)) {
      tstObjArg("output-stream", out)
      out.write(asBytes(s,enc))
      out.flush()
    }
  }


  /**
   * @param out
   * @param s
   * @throws IOException
   */
  def write(out:OutputStream, s:String) {
    write(out,s, "utf-8")
  }


  /**
   * @param inp
   * @return
   * @throws Exception
   */
  def parseXML(inp:InputStream) = newDOMer(true,false).parse(inp)


  /**
   * @param file
   * @return
   * @throws Exception
   */
  def parseXML(f:File) = newDOMer(true,false).parse( f)


  /**
   * @param xmlString
   * @return
   */
  def indexToProlog(xmlString:String)  = {
    var rc=xmlString
    if (! STU.isEmpty(xmlString))    {
      val pos = xmlString.indexOf("<")
      if (pos > 0) {
        rc = xmlString.substring(pos)
      }
    }
    rc
  }


  /**
   * @param em
   * @param a
   * @return
   */
  def iterChildren(em:Element, a:String) = {
    listChildren(em, a).iterator
  }


  /**
   * @param em
   * @param a
   * @return
   */
  def listChildren(em:Element, a:String) = {
    tstObjArg("input-element", em)
    tstEStrArg("tag", a)

    val lst= em.getElementsByTagName(a)
    val len= if(lst==null) 0 else lst.getLength

    val rc= new mutable.ArrayBuffer[Node]()
    for (i <- 0 until len) {
      rc += lst.item(i)
    }

    rc.toSeq
  }


  /**
   * @param em
   * @param a
   * @return
   */
  def getAttr(em:Element, a:String) = {
    if(em==null || a==null) null else em.getAttribute(a)
  }


  /**
   * @param em
   * @return
   */
  def getElementName(em:Element) =  {
    if ( em==null) null else escape(getAttr(em, "name"))
  }


  /**
   * @param tag
   * @param obj
   * @return
   */
  def xmle(tag:String, obj:Object)  = {
    if (tag==null) "" else { starte(tag) + escape(nsb(obj)) + ende(tag) }
  }


  /**
   * @param tag
   * @return
   */
  def starte(tag:String) =  "<" + nsb(tag) + ">"

  /**
   * @param tag
   * @return
   */
  def ende(tag:String) = "</" + nsb(tag) + ">"


  /**
   * @param inStr
   * @return
   */
  def escape(inStr:String)  = {
    val outBuf= new StringBuilder(256)
    nsb(inStr).toCharArray.foreach { (c) =>
      c match {
        case '\n' => outBuf.append("&#10;")
        case '\r' => outBuf.append("&#13;")
        case '<' => outBuf.append("&lt;")
        case '>' => outBuf.append("&gt;")
        case '&' => outBuf.append("&amp;")
        case '\'' => outBuf.append("&apos;")
        case '"' => outBuf.append("&quot;")
        case _ => outBuf.append(c)
      }
    }

    outBuf.toString
  }

  /**
   * Parse the input stream and convert it to a DOM document object.
   *
   * @param inp the stream.
   * @return a DOM document.
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  def toDOM(inp:InputStream) = newDOMer(true, false).parse(inp)

  private def newParser(nsAware:Boolean, validate:Boolean) = {

    val f= getSFac
    f.setNamespaceAware(nsAware)
    f.setValidating(validate)
    f.newSAXParser

  }

  private def newDOMer(nsAware:Boolean, validate:Boolean) = {

    val f= getDFac
    f.setNamespaceAware(nsAware)
    f.setValidating(validate)
    f.newDocumentBuilder
  }

  private def getDFac() = _dfac

  private def getSFac() = _sfac

  private def attrsToMap(atts:Attributes, fullyQ:Boolean) = {
    val len = if(atts==null) 0 else atts.getLength
    val ret = new JTM[String,String]()
    for ( i <- 0 until len) {
      ret.put(
          if(fullyQ) atts.getQName(i) else atts.getLocalName(i) ,
          nsb(atts.getValue(i)))
    }
    ret.toMap
  }



}

sealed class XMLUtils {}

