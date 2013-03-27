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

import java.net.URL
import com.zotoh.frwk.io.XData
import java.util.concurrent.atomic.AtomicLong
import com.zotoh.frwk.util.DateUtils._
import java.util.GregorianCalendar
import java.io.File
import com.zotoh.frwk.util.StrArr


object SimpleHTTPServer {
  
  private val _seed= new AtomicLong()
  
  /**
   * @param args
   */
  def main(args:Array[String])  {
    MemXXXServer.xxx_main("com.zotoh.frwk.net.SimpleHTTPServer", args) match {
      case Some(x:SimpleHTTPServer) => x.runServer(args)        
      case _ =>
        println("Unable to start server.")
    }
  }
}

/**
 * @author kenl
 *
 */
class SimpleHTTPServer(vdir:String, host:String, port:Int) extends MemHTTPServer(vdir,host,port)  {

  import SimpleHTTPServer._
  
  private var _sfx=".dat"
  private var _pfx=""
    
  /**
   * @param vdir
   * @param key
   * @param pwd
   * @param host
   * @param port
   */
  def this(vdir:String, key:URL, pwd:String, host:String, port:Int) {
    this(vdir, host,port)
    setKeyAuth(key,pwd)
  }

  private def runServer(args:Array[String]) {
    for (i <- 0 until args.length) {
      if ( "-prefix" == args(i) ) {
        _pfx= args(i+1)
      }
      if ( "-suffix" == args(i) ) {        
        _sfx= args(i+1)
      }
    }
    val me=this
    this.bind(new BasicHTTPMsgIO() {
      var theUri=""
        
      override def onPreamble(mtd:String, uri:String, hds:Map[String,StrArr]) {
        super.onPreamble(mtd, uri, hds)
        theUri=uri
      }
      
      def onOK(code:Int, reason:String, resOut:XData) {
        var f= toTimeStr( new GregorianCalendar) + "-" + _seed.incrementAndGet()
        f = _pfx + f + _sfx
        me.saveFile( f, resOut)
      }      
      
      override def recvRequest() = me.validate(theUri)
      
    })
    this.start(true)
  }
  
  private def validate(uri:String) = {
    true
  }
  
}
