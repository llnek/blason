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

import org.jboss.netty.handler.codec.http.HttpMessage
import org.slf4j._
import org.apache.commons.lang3.{StringUtils=>STU}
import scala.collection.mutable
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.WWID
import com.zotoh.frwk.io.XData
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import com.zotoh.frwk.util.StrArr



object FileUploader {
  private val _log=LoggerFactory.getLogger(classOf[FileUploader])
}


/**
 * @author kenl
 *
 */
class FileUploader {

  import HTTPUtils._

  private val _clientFnames= mutable.HashMap[File,String]()
  private val _fields= mutable.HashMap[String,String]()
  private val _files= mutable.ArrayBuffer[File]()
  private val _atts= mutable.ArrayBuffer[File]()
  private var _url=""

  /**
   * @param args
   * @throws Exception
   */
  def start(args:Array[String]) {
    if ( parseArgs(args)) {
      upload(new BasicHTTPMsgIO() {
        def onOK(ctx:HTTPMsgInfo, res:XData) {
          println("Done.")
        }
      })
    }
  }

  /**
   * @param name
   * @param value
   */
  def addField(name:String, value:String): this.type = {
    if ( name != null) {
      _fields.put(name, nsb(value))
    }
    this
  }

  /**
   * @param path
   * @param clientFname
   * @throws IOException
   */
  def addAtt(path:File, clientFname:String): this.type = {
    addOneAtt( path, nsb(clientFname))
  }


  /**
   * @param path
   * @throws IOException
   */
  def addAtt(path:File): this.type = addAtt(path, "")

  /**
   * @param path
   * @param clientFname
   * @throws IOException
   */
  def addFile(path:File, clientFname:String): this.type = {
    addOneFile( path, nsb(clientFname))
  }

  /**
   * @param path
   * @throws IOException
   */
  def addFile(path:File): this.type = addFile(path, "")

  /**
   * @param url
   */
  def setUrl(url:String): this.type = { _url= nsb(url); this }

  def send(cb:HTTPMsgIO) { upload(cb) }

  private def upload(cb:HTTPMsgIO) {

    tlog.debug("FileUploader: posting to url: {}" , _url)

    val t= preload()
    simplePOST(new URI(_url), new XData(t._1), new WrappedHTTPMsgIO(cb) {
      override def configMsg(m:HttpMessage) {
        super.configMsg(m)
        m.setHeader("content-type", t._2)
      }
    })
  }

  private def preload() = {
    val t= newTempFile(true)
    using(t._2) { (out) =>
      ( t._1, fmt(out) )
    }
  }

  private def fmt(out:OutputStream) = {
    val boundary = WWID.newWWID()

    // fields
    _fields.foreach { (t) =>
      writeOneField(boundary, t._1, t._2, out)
    }

    // files
    (1 /: _files) { (cnt, f) =>
      writeOneFile(boundary, "file."+cnt, f, "binary", out)
      cnt +1
    }

    // atts
    (1 /: _atts) { (cnt, f) =>
      writeOneFile(boundary, "att."+cnt, f, "binary", out)
      cnt +1
    }

    out.write( asBytes("--" + boundary + "--\r\n") )
    out.flush()

    "multipart/form-data; boundary=" + boundary
  }

  private def writeOneField(boundary:String, field:String, value:String, out:OutputStream) {

    out.write( asBytes("--" + boundary + "\r\n" +
      "Content-Disposition: form-data; " +
      "name=\"" + field +
      "\"\r\n" +
      "\r\n" +
      value + "\r\n"))
    out.flush()
  }

  private def writeOneFile(boundary:String, field:String,
      path:File, cte:String,  out:OutputStream) {

    val cfn = _clientFnames.get(path) match {
      case Some(s) => s
      case _ => ""
    }
    val fname=if (!STU.isEmpty(cfn)) cfn else path.getName()
    val clen= path.length

    out.write(asBytes("--" + boundary + "\r\n" +
    "Content-Disposition: form-data; " +
    "name=\"" + field + "\"; filename=\"" +
    fname + "\"\r\n" +
    "Content-Type: application/octet-stream\r\n" +
    "Content-Transfer-Encoding: " + cte + "\r\n" +
    "Content-Length: " + clen.toString + "\r\n" +
    "\r\n") )
    out.flush()

    using(new FileInputStream(path)) { (inp) =>
      copy(inp, out, clen)
    }

    out.write(asBytes("\r\n"))
    out.flush()

  }

  private def usage() =  {
    println("FileUpload url -p:a=b -p:c=d -f:f1 -f:f2 -a:a1 -a:a2 ...")
    println("e.g.")
    println("FileUpload http://localhost:8003/HelloWorld -p:a=b -f:/temp/a.txt -a:/temp/b.att")
    println("")
    false
  }

  private def parseArgs(av:Seq[String]): Boolean = {

    if (av.length < 2)    {   return usage()    }
    _url=av(0)

    for (i <- 1 until av.length) {
      val s= av(i)
      if (s.startsWith("-p:")) {
        val ss=s.substring(3).split("=")
        addField(ss(0),ss(1))
      }
      else
      if (s.startsWith("-f:")) {
        addFile(new File(s.substring(3)))
      }
      else
      if (s.startsWith("-a:")) {
        addAtt( new File(s.substring(3)))
      }
    }

    true
  }

  private def addOneFile(path:File, clientFname:String): this.type  = {
    tstObjArg("file-url", path)
    _clientFnames += path -> clientFname
    _files += path
    this
  }

  private def addOneAtt(path:File, clientFname:String): this.type  = {
    tstObjArg("file-url", path)
    _clientFnames += path -> clientFname
    _atts += path
    this
  }

  def tlog() = FileUploader._log

}
