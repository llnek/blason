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

import org.jboss.netty.handler.codec.http.HttpServerCodec
import org.jboss.netty.channel.Channels.pipeline

import java.io.File
import java.io.IOException
import java.net.URL

import javax.net.ssl.SSLEngine

import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelPipeline

import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.handler.ssl.SslHandler

import org.apache.commons.io.{FileUtils=>FUS}
import com.zotoh.frwk.util.FileUtils
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.io.IOUtils._



object MemFileServer{

  def main(args:Array[String]) {
    /*
    args= Array(
      "-host", "localhost", "-port", "8080", "-vdir", "/tmp/wdrive"
    )
    */
    MemXXXServer.xxx_main(true, "com.zotoh.frwk.net.MemFileServer", args)
  }
}

/**
 * @author kenl
 *
 */
class MemFileServer(vdir:String,host:String,port:Int) extends MemXXXServer(vdir,host,port) {

  /**
   *
   */
  def saveFile(file:String, data:XData) {
    val fp= new File(_vdir, file)
    FUS.deleteQuietly(fp)
    data.fileRef match {
      case Some(f) =>  FUS.moveFile(f, fp)
      case _ =>  writeFile(fp, data.bytes.getOrElse( Array[Byte]()))
    }
  }

  def getFile(file:String) = {
    val fp= new File(_vdir, file)
    if (fp.exists && fp.canRead) {
      val out= new XData()
      out.resetMsgContent(fp, false)
      Some(out)
    } else {
      None
    }
  }

  /**
   * @param vdir
   * @param key
   * @param pwd
   * @param host
   * @param port
   */
  def this(vdir:String, key:URL, pwd:String, host:String, port:Int) {
    this(vdir,host,port)
    setKeyAuth(key,pwd)
  }

  override def pipelineFac(eg:SSLEngine) = {
    val me=this
    new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl = org.jboss.netty.channel.Channels.pipeline()
        if (eg != null) {  pl.addLast("ssl", new SslHandler(eg)) }
        pl.addLast("decoder", new HttpServerCodec())
//        pipe.addLast("aggregator", new HttpChunkAggregator(65536));
//        pipe.addLast("deflater", new HttpContentCompressor())
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", new FileServerHdlr(me) )
        pl
      }
    }
  }

}


