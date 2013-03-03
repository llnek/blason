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


object ValidationStyle extends Enumeration {
  type ValidationStyle=Value
  val NONE= Value(0, "None" )
  val DTD= Value( 1, "DTD" )
  val XSD= Value(2, "XSD" )
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

  private val _dfac = DocumentBuilderFactory.newInstance
  private var _sfac = SAXParserFactory.newInstance

  def newSaxValidator() = newParser(true, true)

  def newSaxParser() = newParser(true, false)

  def mkInputSource(s:InputStream) = new InputSource(new BufferedInputStream(s))

  def attrsToLNMap(atts:Attributes) = {
    attrsToMap(atts, false)
  }

  def attrsToQNMap(atts:Attributes) = attrsToMap(atts, true)


  def attributesToString(atts:Map[String,String]) =  {
    if (atts==null) "" else atts.foldLeft(new StringBuilder) { (buf,t) =>
      buf.append(" ").append(t._1).append("=\"").append(t._2).append("\"")
    }.toString
  }


  def write(out:OutputStream, s:String, enc:String) {
    if (! STU.isEmpty(s)) {
      tstObjArg("output-stream", out)
      out.write(asBytes(s,enc))
      out.flush
    }
  }


  def write(out:OutputStream, s:String) {
    write(out,s, "utf-8")
  }


  def parseXML(inp:InputStream) = newDOMer(true,false).parse(inp)


  def parseXML(f:File) = newDOMer(true,false).parse( f)


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


  def iterChildren(em:Element, a:String) = {
    listChildren(em, a).iterator
  }


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


  def getAttr(em:Element, a:String) = {
    if(em==null || a==null) null else em.getAttribute(a)
  }


  def getElementName(em:Element) =  {
    if ( em==null) null else escape(getAttr(em, "name"))
  }


  def xmle(tag:String, obj:Object)  = {
    if (tag==null) "" else { starte(tag) + escape(nsb(obj)) + ende(tag) }
  }

  def starte(tag:String) =  "<" + nsb(tag) + ">"

  def ende(tag:String) = "</" + nsb(tag) + ">"


  def escape(inStr:String)  = {

    nsb(inStr).toCharArray.foldLeft(new StringBuilder) { (buf,c) =>
      c match {
        case '\n' => buf.append("&#10;")
        case '\r' => buf.append("&#13;")
        case '<' => buf.append("&lt;")
        case '>' => buf.append("&gt;")
        case '&' => buf.append("&amp;")
        case '\'' => buf.append("&apos;")
        case '"' => buf.append("&quot;")
        case _ => buf.append(c)
      }
      buf
    }.toString

  }

  /**
   * Parse the input stream and convert it to a DOM document object.
   *
   * @param inp the stream.
   * @return a DOM document.
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

