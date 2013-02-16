/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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


package com.zotoh.dbio
package sql

import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import com.zotoh.frwk.db.DBVendor
import org.apache.commons.io.{IOUtils=>IOU}
import com.zotoh.frwk.io.IOUtils
import com.zotoh.frwk.xml.XMLUtils
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.dbio.core.Schema



/**
 * @author kenl
 *
 */
object GenSQLFile {

  def main(args:Array[String]) {
    try {
      if (args.length != 3) {
        usage()
      } else {
        start(args(0), args(1), new File( args(2)))
      }
    } catch {
      case e:Throwable => e.printStackTrace()
    }
  }

  private def usage() {
    println("GenSQLFile <manifest-file> [ h2 | mysql | oracle | mssql | postgresql ] <output-file>")
    println("e.g.")
    println("GenSQLFile sample.txt h2 sample.sql")
    println("")
  }

  private def start(file:String , db:String , out:File ) {
    val v= DBVendor.fromString(db)
    if (v==null) { usage() } else {
      var inp:InputStream = null
      try {
        inp= new FileInputStream(file)
        writeDDL(v, out, new Schema() { def getModels = readFile(inp)  } )
      } catch {
        case e:Throwable =>
          println("Failed to parse manifest file : " + file)
      } finally {
        IOU.closeQuietly(inp)
      }
    }
  }

  def genDDL( v:DBVendor , s:Schema ) = {
    DBDriver.newDriver(v).withSchema(s).getDDL()
  }

  def writeDDL( v:DBVendor, out:File ,s:Schema ) {
    IOUtils.writeFile( out, genDDL(v, s) )
  }

  private def readFile(inp:InputStream ) = {
    val root= XMLUtils.parseXML(inp).getDocumentElement()
    readClasses( getFirst( root, "classes") )
  }

  private def readClasses( top:Element ) = {

    val lst= mutable.ArrayBuffer[ Class[_] ]()
    val nl= top.getElementsByTagName("class")
    if (nl != null) for (i <- 0 until nl.getLength) {
      val em= nl.item(i).asInstanceOf[Element]
      val s= STU.trim( em.getAttribute("id") )
      val z= MetaUtils.forName(s)
      if (z==null) {
        throw new ClassNotFoundException()
      }
      lst += z
    }
    lst.toSeq
  }

  private def getFirst(top:Element , tag:String ): Element = {
    val nl= if(top==null) null else top.getElementsByTagName( tag)
    if (nl != null && nl.getLength > 0) {
      nl.item(0).asInstanceOf[Element]
    } else {
      null
    }
  }

}
