/*??
 * COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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
import org.slf4j._
import java.net.URL
import java.io.InputStream
import java.io.File
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import java.io.StringReader
import org.jdom2.Document


/**
 * @author kenl
 */
object DOMDoc {
  private val _log=LoggerFactory.getLogger(classOf[DOMDoc] )
  val _sax= new ThreadLocal[SAXBuilder]() {
    override def initialValue() = new SAXBuilder( XMLReaders.NONVALIDATING )
  }

}

/**
 * @author kenl
 */
class DOMDoc {

  def tlog() = DOMDoc._log
  import DOMDoc._

  def parse(fp:File)( cb: ( Document ) => Any ) { parse(fp.toURI.toURL) ( cb ) }

  def parse(fp:URL)( cb: ( Document ) => Any ) {
    using (fp.openStream ) { (inp) =>
      parse(inp) (cb)
    }
  }

  def parse(s:String) ( cb: ( Document ) => Any) {
    using ( new StringReader(s) ) { (r) =>
      cb( _sax.get.build(r) )
    }
  }

  def parse(inp:InputStream) ( cb: ( Document ) => Any) {
    cb( _sax.get.build(inp) )
  }


}


