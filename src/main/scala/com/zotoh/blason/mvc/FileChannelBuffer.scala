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

import java.io.File
import java.io.FileInputStream
import java.io.{InputStream,OutputStream}
import java.nio.ByteBuffer
import java.nio.channels.GatheringByteChannel
import java.nio.channels.ScatteringByteChannel

import org.jboss.netty.buffer._

/**
 * @author kenl
 * 
 * original from playframework- 1.2.5
 */
class FileChannelBuffer(private val _fp:File) extends AbstractChannelBuffer with WrappedChannelBuffer {

  private lazy val _inp:FileInputStream = new FileInputStream(_fp)

  def getInputStream() = _inp
  def file() = _fp
  def isDirect() = true
  def hasArray() = false
  override def readerIndex() = 0

  def getBytes( index:Int, out:GatheringByteChannel, length:Int ) = {
    val b = new Array[Byte](length)
    _inp.read(b, index, length)
    val bb = ByteBuffer.wrap(b)
    out.write(bb)
  }

  def getBytes( index:Int, out:OutputStream , length:Int ) {
    val b = new Array[Byte](length )
    _inp.read(b, index, length)
    out.write(b, index, length)
  }

  def getBytes( index:Int, dst:Array[Byte] , dstIndex:Int, length:Int ) {
    val b = new Array[Byte](length)
    _inp.read(b, index, length)
    System.arraycopy(b, 0, dst, dstIndex, length)
  }

  def getBytes( index:Int, dst:ChannelBuffer, dstIndex:Int, length:Int) {
    val b = new Array[Byte](length)
    _inp.read(b, index, length)
    dst.writeBytes(b, dstIndex, length)
  }

  def getBytes( index:Int, dst:ByteBuffer ) {
    val b = new Array[Byte]( _inp.available() - index )
    _inp.read(b, index, _inp.available() - index)
    dst.put(b)
  }

  def capacity() = _inp.available()

  override def readBytes( dst:Array[Byte] , dstIndex:Int, length:Int) {
    checkReadableBytes(length)
    getBytes(0, dst, dstIndex, length)
  }

  override def readBytes( dst:Array[Byte] ) {
    readBytes(dst, 0, dst.length)
  }

  override def readBytes(dst:ChannelBuffer ) {
    readBytes(dst, dst.writableBytes )
  }

  override def readBytes( dst:ChannelBuffer, length:Int) {
    if (length > dst.writableBytes ) {
        throw new IndexOutOfBoundsException()
    }
    readBytes(dst, dst.writerIndex(), length)
    dst.writerIndex(dst.writerIndex() + length)
  }

  override def readBytes( dst:ChannelBuffer, dstIndex:Int, length:Int) {
    getBytes(0, dst, dstIndex, length)
  }

  override def readBytes( dst:ByteBuffer) {
    val length = dst.remaining()
    checkReadableBytes(length)
    getBytes(0, dst)
  }

  override def readBytes( out:GatheringByteChannel, length:Int) = {
    checkReadableBytes(length)
    getBytes(0, out, length)
  }

  override def readBytes( out:OutputStream , length:Int) {
    checkReadableBytes(length)
    getBytes(0, out, length)
  }

  override def unwrap() = { IAE }
  override def factory() = { IAE }
  override def order() = { IAE }
  override def array() = { IAE }
  override def arrayOffset() = { IAE }
  override def discardReadBytes() { IAE }
  def setByte( index:Int, value:Byte) { IAE }
  def setBytes( index:Int, src:ChannelBuffer, srcIndex:Int, length:Int ) { IAE }
  def setBytes( index:Int, src:Array[Byte] , srcIndex:Int, length:Int) { IAE }
  def setBytes( index:Int, src:ByteBuffer ) { IAE }
  def setShort( index:Int, value:Short ) { IAE }
  def setMedium( index:Int,  value:Int) { IAE }
  def setInt( index:Int,  value:Int) { IAE }
  def setLong( index:Int,  value:Long ) { IAE }
  def setBytes( index:Int, inp:InputStream , length:Int ) = { IAE }
  def setBytes( index:Int, inp:ScatteringByteChannel, length:Int ) = { IAE }
  def setByte( i:Int , j:Int ) { IAE }
  def duplicate() = { IAE }
  def copy(  index:Int, length:Int) = { IAE }
  def slice( index:Int, length:Int ) = { IAE }
  def getByte( index:Int ) = { IAE }
  override def getShort( index:Int) = { IAE }
  override def getUnsignedMedium( index:Int) = { IAE }
  def getInt(index:Int) = { IAE }
  def getLong(index:Int) = { IAE }
  def toByteBuffer( index:Int, length:Int) = { IAE }
  override def toByteBuffers( index:Int, length:Int) = { IAE }
  override def readBytes( length:Int) = { IAE }
  override def readBytes(c:ChannelBufferIndexFinder) = { IAE }
  override def readSlice( length:Int) = { IAE }
  override def readSlice(c:ChannelBufferIndexFinder) = { IAE }
  override def toString(a:Int, b:Int, s:String) = { IAE }
  def setShort(a:Int, b:Int ) { IAE }

  private def IAE() = { throw new IllegalAccessException() }
}
