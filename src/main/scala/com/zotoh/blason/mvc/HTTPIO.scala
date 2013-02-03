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

package com.zotoh.blason
package mvc

import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.ChannelHandlerContext
import java.io.RandomAccessFile
import java.io.File
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.HttpHeaders.Names
import org.eclipse.jetty.http.MimeTypes
import org.jboss.netty.handler.codec.http.HttpHeaders.Values
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.mime.MimeUtils._
import java.io.IOException
import org.jboss.netty.handler.stream.ChunkedFile
import com.zotoh.blason.io.NettyHplr._

/**
 * @author kenl
 */
object HTTPIO {

  def getFile( src:NettyMVC, ctx:ChannelHandlerContext, req:HttpRequest, rsp:HttpResponse,file:File) {

    var raf= new RandomAccessFile(file, "r")
    val clen = raf.length()
    val ch= ctx.getChannel
    val ct=guessContentType(file, "utf-8", "text/plain")
    tlog.debug("Serving file: {} with clen= {}, ctype={}", file.getName, asJObj(clen), ct)
    
    try {
      if ( rsp.getStatus() != HttpResponseStatus.NOT_MODIFIED) {
        rsp.setHeader( Names.CONTENT_LENGTH, clen.toString )
      }
      rsp.addHeader(Names.ACCEPT_RANGES, Values.BYTES)
      rsp.setHeader(Names.CONTENT_TYPE, ct )

      val wf= if ( req.getMethod() != HttpMethod.HEAD) {
        val inp = getFileInput(raf, ct, req, rsp)
        ch.write(rsp)
        ch.write(inp)
      } else {
        try { ch.write(rsp) } finally {
          raf.close()
          raf=null
        }
      }

      closeCF( !chKeepAlive(req) , wf )

    } catch {
      case e:Throwable =>
        tlog.error("",e)
        block { () => if (raf != null) { raf.close }}
        block { () => ch.close }
    }
  }

  private def getFileInput( raf:RandomAccessFile, ct:String , req:HttpRequest , rsp:HttpResponse ) = {
    if( HTTPRangeInput.accepts(req)) {
      val inp = new HTTPRangeInput(raf, ct, req)
      inp.prepareNettyResponse(rsp)
      inp
    } else {
      new ChunkedFile(raf)
    }
  }

  private val _log= LoggerFactory.getLogger(classOf[HTTPIO])
  def tlog() = _log
}

sealed class HTTPIO {}


@SerialVersionUID(834758934753475L)
case class WebCookie() extends scala.Serializable {
  
    private var _httpOnly = false
    private var _secure = false
    private var _maxAge= -1
    private var _name=""
    private var _domain=""
    private var _path= ""
    private var _value=""
      
    def maxAge_=( n:Int) { _maxAge = n }
    def maxAge = _maxAge
    
    def setSecure(b:Boolean) { _secure= b }
    def isSecure() = _secure
    
    def domain_=(s:String) { _domain = s }
    def domain = _domain
    
    def path_=(s:String) { _path = s }
    def path = _path
    
    def name_=(s:String) { _name = s }
    def name = _name
    
    def value_=(s:String) { _value = s }
    def value = _value
    
    def setHttpOnly(b:Boolean) { _httpOnly= b }
    def isHttpOnly() = _httpOnly
    
}

class ByteRange(private val _file:RandomAccessFile, private var _start:Long, private var _end:Long,
    private val _cType:String, private val _incHeader:Boolean) {    

  private var _servedHeader = 0
  private var _servedRange = 0
  private var _header= if ( _incHeader) {
    fmtRangeHeader( _start, _end, _file.length , _cType, "DEFAULT_SEPARATOR"  )
  } else {
    Array[Byte]()
  }
  
  def start= _start
  def end= _end
  
  def size() = _end - _start + 1

  def remaining() = _end - _start + 1 - _servedRange

  def computeTotalLengh = size() + _header.length
  
  def fill(out:Array[Byte] , offset:Int) = {    
    var count = 0
    var pos=offset
    while ( pos < out.length && _servedHeader < _header.length ) {
        out(pos) = _header( _servedHeader)
        pos += 1
        _servedHeader += 1
        count += 1
    }

    if ( pos < out.length) {
      _file.seek( _start + _servedRange)
      var maxToRead = if ( remaining() > ( out.length - pos) ) (out.length - pos) else remaining()
      if (maxToRead > Int.MaxValue) {
          maxToRead = Int.MaxValue.toLong
      }
      val c = _file.read( out, pos, maxToRead.toInt)
      if(c < 0) {
          throw new IOException("error while reading file : no more to read ! length=" + _file.length() + 
              ", seek=" + ( _start + _servedRange))
      }
      _servedRange += c
      count += c
    }
    count
  }

  private def fmtRangeHeader(start:Long, end:Long, flen:Long, cType:String, boundary:String) = {
    val s= "--" + boundary + "\r\n" + "content-type: " + cType + "\r\n" +
    "content-range: bytes " + start + "-" + end + "/" + flen + "\r\n" +
    "\r\n"
    s.getBytes("utf-8")
  }
  
}

