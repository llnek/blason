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
package net

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.http.client._
import org.apache.http.impl.client.DefaultHttpClient
import com.zotoh.frwk.io.XData
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse
import java.io.IOException
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import com.zotoh.frwk.mime.MimeUtils
import org.apache.http.util.EntityUtils
import java.io.File
import java.net.URI
import org.apache.http.Header
import org.apache.http.entity.InputStreamEntity
import org.apache.http.client.methods.HttpPost

object SyncHTTPClient {
  def main(args:Array[String]) {
    try {
      val data= new XData(new File("/tmp/play.zip")).setDeleteFile(false)
      val c= new SyncHTTPClient()
      // uri input is assumed to be encoded!
      val uri = new URI("http://www.yahoo.com/?abc=123+456&d=f")
      
      println( uri.toString)
      println( uri.toASCIIString())
      println(uri.toURL().toExternalForm())
      println(uri.toURL().toString)
      
      c.connect(uri)
      c.get() match {
        case Some(s:String) => println(s)
        case _ =>
      }
      c.finz()
    } catch {
      case e:Throwable => e.printStackTrace()
    }
    sys.exit(0)
  }
  
}


class SyncHTTPClient extends HTTPClientBase with CoreImplicits {
  private var _cli:HttpClient = null

  def connect(host:String,port:Int) {
    _cli = new DefaultHttpClient()
  }

  def post(contentType:String, data:XData ) = {
    try {      
      val ent = new InputStreamEntity( data.stream, data.size)
      ent.setContentType(contentType)
      ent.setChunked(true)
      val p= new HttpPost(_remote)
      p.setEntity(ent)
      onResp(  _cli.execute( p  ) )            
    } finally {
      close()
    }
  }

  def get(): Option[Any] = {
    try {
      onResp(  _cli.execute( new HttpGet( _remote)   ) )
    } finally {
      close()
    }
  }
  
  private def onResp(rsp:HttpResponse): Option[Any] = {
    val (code, msg) = rsp.getStatusLine match {
      case x:StatusLine => ( x.getStatusCode() , nsb(x.getReasonPhrase) )
      case _ => (500, "Internal Server Error")
    }
    /*
    val ct= rsp.getHeaders("content-type").foldLeft(new StringBuilder) { (b,s) =>
      b.append("\r\n").append(s)
    }
    val cs= MimeUtils.getCharset(ct.toString)
    */
    val ent= rsp.getEntity()
    code match {
      case n if n >= 200 && n < 300 =>    onOK(ent)
      case n if n >= 300 && n < 400 => 
        onError(ent, new IOException("redirect not supported.\n" + msg ))
      case _ => // error
        onError(ent, new IOException( "" + code + " , " + msg ) )
    }
        
  }

  def onOK(ent:HttpEntity): Option[Any] = {
    
    val ct= ent.getContentType match {
      case x:Header => nsb ( x.getValue ).trim 
      case _ => "text/html"
    }
//    tlog.debug("content-length: {}", asJObj(ent.getContentLength) )
    tlog.debug("content-type: {}", ct )
    tlog.debug("content-encoding: {}", ent.getContentEncoding )
    ct.lc match {
      case s if s.startsWith("text/") => Option(EntityUtils.toString(ent, "utf-8") ) 
      case "???" => null
        //ent.getContent()
      case _ => Option(EntityUtils.toByteArray(ent) ) 
    }
    
  }
  
  def onError(ent:HttpEntity, t:Throwable): Option[Any] =  {
    EntityUtils.consumeQuietly(ent) // clear stuff ? from example code    
    throw t
  }

  def close() {
    if (_cli != null) block { () =>
      _cli.getConnectionManager().shutdown()
    }
    _cli=null
  }


}
