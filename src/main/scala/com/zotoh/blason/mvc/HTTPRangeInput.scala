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

import scala.collection.JavaConversions._
import scala.collection.mutable

import java.io.File
import java.io.RandomAccessFile
import java.util.{Arrays,Comparator}

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.handler.stream.ChunkedInput
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.handler.codec.http.HttpHeaders
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._



object HTTPRangeInput {
  def accepts(req:HttpRequest ) = req.containsHeader("range")
}

class HTTPRangeInput(private val _file:RandomAccessFile, private val _contentType:String,
private val _req:HttpRequest) extends ChunkedInput {

  private var _ranges:Array[ByteRange] = null
  private var _chunkSize = 8096
  private var _currentByteRange = 0
  private var _unsatisfiable = false
  private val _clen= _file.length
  initRanges()

  def prepareNettyResponse(rsp:HttpResponse ) {
    rsp.addHeader("accept-ranges", "bytes")
    if (_unsatisfiable) {
      rsp.setStatus(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
      rsp.setHeader("content-range", "bytes " + "0-" + (_clen-1) + "/" + _clen)
      rsp.setHeader("content-length", 0)
    } else {
      rsp.setStatus(HttpResponseStatus.PARTIAL_CONTENT)
      if(_ranges.length == 1) {
        val r= _ranges(0)
        rsp.setHeader("content-range", "bytes " + r.start + "-" + r.end + "/" + _clen)
      } else {
        rsp.setHeader("content-type", "multipart/byteranges; boundary="+ "DEFAULT_SEPARATOR")
      }
      val len = _ranges.foldLeft(0L) { (rc, r) =>
        rc + r.computeTotalLengh
      }
      rsp.setHeader("content-length", len.toString )
    }
  }

  override def nextChunk() = {

    val buff= new Array[Byte]( _chunkSize )
    var count = 0

    while ( count < _chunkSize && _currentByteRange < _ranges.length && 
        _ranges(_currentByteRange) != null) {
      if ( _ranges(_currentByteRange).remaining() > 0) {
          count += _ranges(_currentByteRange).fill(buff, count)
      } else {
          _currentByteRange += 1
      }
    }
    if (count == 0) null else wrappedBuffer(buff)
  }

  override def hasNextChunk() = {
    _currentByteRange < _ranges.size && _ranges(_currentByteRange).remaining() > 0
  }

  override def isEndOfInput() = !hasNextChunk()

  override def close() {
    _file.close()
  }

  private def initRanges() {
    try {
      val ranges = mutable.ArrayBuffer[ (Long,Long) ]()
      // strip off "bytes="
      val s = nsb(_req.getHeader("range") )
      val pos= s.indexOf("bytes=")
      val rvs= if (pos == -1) null else {
        s.substring(pos+6).trim().split(",")
      }

      if (rvs != null) rvs.foreach { (rv) =>
        val rs= rv.trim
        var start=0L
        var end=0L
        if (rs.startsWith("-")) {
          start = _clen - 1 -  rs.substring(1).trim.toLong
          end = _clen - 1
        } else {
          val range = rs.split("-")
          start = range(0).trim.toLong
          end = if (range.size > 1) range(1).trim.toLong else _clen - 1
        }
        if(end > (_clen - 1)) { end = _clen - 1 }
        if(start <= end) { ranges.add( ( start, end ) ) }
      }

      val bytes = mutable.ArrayBuffer[ByteRange]()
      val nrs = normalize(ranges.toArray)
      nrs.foreach { (rr) =>
        bytes += new ByteRange( _file, rr._1, rr._2, _contentType, nrs.size > 1)
      }
      _ranges = bytes.toArray
      _unsatisfiable = (_ranges.size == 0)
    } catch {
      case e:Throwable =>
        _unsatisfiable = true
        tlog.error("",e)
    }
  }

  private def maybeIntersect(r1:(Long,Long), r2:(Long,Long) ) = {
    r1._1 >= r2._1 && r1._1 <= r2._2 || r1._2 >= r2._1 && r1._1 <= r2._2
  }

  private def mergeRanges(r1:(Long,Long), r2:(Long,Long) ) = {
    ( if(r1._1 < r2._1) r1._1 else r2._1,
              if (r1._2 > r2._2) r1._2 else r2._2 )
  }

  private def normalize( chunks:Array[(Long,Long)]): Array[(Long,Long) ] = {

    if (chunks.length == 0) { return Array[ (Long,Long) ]() }

    def sortedChunks = Array[(Long,Long)]() ++ chunks

    Arrays.sort(sortedChunks, new Comparator[ (Long,Long)] () {
        def compare( t1:(Long,Long), t2:(Long,Long)) = {
          t1._1.compareTo(t2._1)
        }
    })

    val rc= mutable.ArrayBuffer[(Long,Long)]()
    rc.add(sortedChunks(0))
    for (i <- 1 until sortedChunks.length) {
        val c1 = sortedChunks(i)
        val r1 = rc.get(rc.size() - 1)
        if ( maybeIntersect(c1, r1)) {
            rc.set(rc.size() - 1, mergeRanges(c1, r1))
        } else {
            rc.add(c1)
        }
    }
    rc.toArray
  }

}

