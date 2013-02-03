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

/**
 * Constants.
 *
 * @author kenl
 *
 */
trait Constants {

  val XSD_NSP = "http://www.w3.org/2001/XMLSchema"
  val XSD_PFX = "xsd"
  val XML_NSP = "http://www.w3.org/XML/1998/namespace"
  val XML_PFX = "xml"
  val XML_LANG= "xml:lang"

  val XMLHDLINE= """
  <?xml version="1.0" encoding="UTF-8"?>
"""

  val XERCES= "org.apache.xerces.parsers.SAXParser"
  //val SLASH= "/"
  val COMMA= ","
  val DOT= "."
  val HASHHASH= "##"
  val HASH= "#"
  val LPAREN= "("
  val RPAREN= ")"
  val DOLLAR= "$"
  val CSEP= "?"

  val DV_NONE= "None"
  val DV_XSD= "XSD"
  val DV_DTD= "DTD"
}

