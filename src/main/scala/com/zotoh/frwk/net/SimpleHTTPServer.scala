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

import java.io.File
import java.net.URL
import java.util.GregorianCalendar
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Matcher
import java.util.regex.Pattern
import scala.collection.mutable
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.frwk.util.INIConf
import com.zotoh.frwk.util.StrArr
import com.zotoh.frwk.util.ProcessUtils

/**
 * @author kenl
 */
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

  type T2 = Tuple2[String,Pattern]
  import SimpleHTTPServer._
  
  private val _regexs= mutable.ArrayBuffer[ T2 ]()
  private var _ini:INIConf= null
  
  
  
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
    val me=this
    
    for (i <- 0 until args.length) {
      if ( "-conf" == args(i) ) {
        _ini=new INIConf(new File(args(i+1)))
      }
    }
    
    if (_ini != null) {
      parseConf(_ini)
      start()
    } else {
      println("No conf file.")
    }
  }
  
  private def start() {
    val me=this
    
    bind(new BasicHTTPMsgIO() {
      
      def onOK(ctx:HTTPMsgInfo, resOut:XData) {
        me.maybeSaveFile( ctx, resOut)
      }      
      
      override def validateRequest(ctx:HTTPMsgInfo) = {
        me.validate(ctx) match {
          case Some(x) => true
          case _ => false
        }
      }
      
    })
    
    java.lang.Runtime.getRuntime().addShutdownHook( new Thread() {
      override def run() {
        block { () => me.stop()  }
      }      
    })
    
    this.start(true)
  }
  
  private def maybeSaveFile(ctx:HTTPMsgInfo, resOut:XData) {
    validate(ctx) match {
      case Some(t) =>
        var f= toTimeStr( new GregorianCalendar) + "-" + _seed.incrementAndGet()
        val m= _ini.section(t._1).get.asInstanceOf[Map[String,String]]
        val d=m.get("vdir").getOrElse("")
        val p=m.get("pfx").getOrElse("")
        val x=m.get("sfx").getOrElse(".dat")
        val dir=if (hgl(d)) new File(d) else _vdir
        saveFile( dir, p+f+x, resOut)
      case _ =>
    }
  }
  
  private def parseConf(ini:INIConf) {
    _ini.sections.foreach { (s) =>
      _ini.section(s).get.get("vdir") match {
        case Some(x) => new File(x).mkdirs()
        case _ =>
      }
      _regexs += Tuple2( s, Pattern.compile(s) )
    }    
  }
  
  private def validate(ctx:HTTPMsgInfo) = {
    _regexs.find{ (t) =>
      if ( t._2.matcher(ctx.uri).matches()) {
        val m= _ini.section(t._1).get.asInstanceOf[Map[String,String]]
        val v=m.get("verb").getOrElse("").toLowerCase
        if ("*" == v || v == ctx.method.toLowerCase) true else false        
      } else {
        false
      }
    }      
  }
  
}
