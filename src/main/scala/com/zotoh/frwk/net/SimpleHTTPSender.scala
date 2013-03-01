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
package net

import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.net.HTTPUtils._
import com.zotoh.frwk.io.IOUtils._

import org.slf4j._

import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.CoreUtils._



object SimpleHTTPSender  {

  /**
   * @param args
   */
  def main(args:Array[String])  {
    try {
      new SimpleHTTPSender().start(args)
    } catch {
      case e:Throwable => e.printStackTrace()
    }
  }

  private val _log= LoggerFactory.getLogger(classOf[SimpleHTTPSender])
}

/**
 * @author kenl
 *
 */
class SimpleHTTPSender  {

  import SimpleHTTPSender._
  def tlog() = _log

  private var _client:HTTPClient = null
  private var _doc=""
  private var _url=""

  private def start(args:Array[String]) {

    if ( !parseArgs(args)) usage() else {
      _client= send(new BasicHTTPMsgIO() {
        def onOK(code:Int, r:String, res:XData) {
          try {
            println("Response Status Code: " +  code)
            println("Response Data: " + nsn(res))
          } catch {
            case e:Throwable =>
          }
        }
        override def onError(code:Int, r:String) {
          println("Error: code =" + code + ", reason=" + r)
        }
      })
      _client.join()
    }
  }

  private def send(cb:HTTPMsgIO) = {
    val d:XData  = _doc match {
      case s:String => using(open(new File(s)))  { (inp) => readBytes(inp) }
      case _ => null
    }
    d match {
      case s:XData => simplePOST( new URI(_url), s, cb)
      case _ => simpleGET( new URI(_url), cb)
    }
  }

  private def usage()  {

    println("HTTPSender  <URL> [ <docfile> ]")
    println("e.g.")
    println("HTTPSender http://localhost:8080/SomeUri?x=y ")
    println("")
    println("")
    println("")

  }

  private def parseArgs(args:Array[String]) = {

    if (args.length < 1) false else {
      _url= args(0)
      if (args.length > 1) {
        _doc=args(1)
      }
      true
    }

  }

}
