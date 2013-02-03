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
package io

import java.io.{ByteArrayOutputStream=>ByteArrayOS,File,InputStream}
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{FileUtils=>FUT}
import org.slf4j._
//import com.zotoh.frwk.util.FileUtils
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._


/**
 * Wrapper structure to abstract a piece of data which can be a file
 * or a memory byte[].  If the data is byte[], it will also be
 * compressed if a certain threshold is exceeded.
 *
 * @author kenl
 *
 */

object XData {

  private val _log= LoggerFactory.getLogger(classOf[XData])
  private var _wd = tmpDir()

  /**
   * Get the current shared working directory.
   *
   * @return working dir.
   */
  def workDir = _wd

  /**
   * Working directory is where all the temporary/sharable files are created.
   *
   * @param fpDir a directory.
   */
  def workDir_=(fpDir:File) {
    block { () =>
      if (fpDir != null && fpDir.isDirectory &&
        fpDir.canRead && fpDir.canWrite) {
        _wd=fpDir
      }
    }
  }

}

@SerialVersionUID(-8637175588593032279L)
class XData() extends Serializable {

  import XData._
  def tlog() = _log

  private val CMPZ_THREADHOLD=1024*1024*4 // 4meg
  private var _cmpz=false
  private var _cls=true

  private var _encoding="UTF-8"
  private var _fp=""

  private var _bin:Array[Byte]= null
  private var _binSize= 0L

  /**
   *
   */
  def javaBytes() = bytes().getOrElse( Array[Byte]() )

  /**
   *
   */
  def this(p:AnyRef) {
    this()
    resetMsgContent(p)
  }

  /**
   * @param enc
   */
  def encoding_=(enc:String) { _encoding=enc }

  /**
   * @return
   */
  def encoding = _encoding

  /**
   *
   */
  def isZiped =  _cmpz

  /**
   * Control the internal file.
   *
   * @param del true to delete, false ignore.
   */
  def setDeleteFile(del:Boolean): this.type = { _cls= del; this }

  /**
   * Tests if the file is to be deleted.
   *
   * @return
   */
  def isDeleteFile() = _cls

  /**
   * Clean up.
   */
  def destroy() {
    if (! STU.isEmpty(_fp) && isDeleteFile ) {
      FUT.deleteQuietly( new File(_fp))
    }
    reset()
  }

  /**
   * Tests if the internal data is a file.
   *
   * @return
   */
  def isDiskFile() = ! STU.isEmpty( _fp)


  /**
   * @param obj
   */
  def resetMsgContent(obj:Any): XData = resetMsgContent(obj, true)


  /**
   * @param obj
   * @param delIfFile
   */
  def resetMsgContent(obj:Any, delIfFile:Boolean): this.type = {
    destroy()
    obj match {
      case fa:Array[File] => if (fa.length > 0) _fp= niceFPath( fa(0))
      case baos: ByteArrayOS => maybeCmpz( baos.toByteArray)
      case bits: Array[Byte] => maybeCmpz( bits)
      case f:File => _fp= niceFPath( f)
      case s:String => maybeCmpz( s.getBytes(_encoding))
      case _ =>
    }
    setDeleteFile(delIfFile)
  }

  /**
   * Get the internal data.
   *
   * @return
   */
  def content(): Option[Any] = {
    val a:Any = if (isDiskFile)  {
      new File( _fp)
    }
    else if ( _bin==null) {
      null
    }
    else if (! _cmpz) {
      _bin
    } else {
      gunzip( _bin)
    }

    if (a==null) None else Some(a)
  }

  /**
   * @return
   */
  def hasContent() = {
    if (isDiskFile) true else if ( _bin !=null) true else false
  }

  /**
   * Get the data as bytes.  If it's a file-ref, the entire file content will be read
   * in as byte[].
   *
   * @return
   * @throws IOException
   */
  def bytes(): Option[Array[Byte]] = {
    val b= if (isDiskFile) read(new File( _fp)) else binData()
    if (b==null) None else Some(b)
  }


  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or null.
   */
  def fileRef() =  if ( STU.isEmpty(_fp)) None else Some(new File( _fp))


  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or "".
   */
  def filePath() =  if ( STU.isEmpty(_fp)) "" else _fp


  /**
   * Get the internal data if it is in memory byte[].
   *
   * @return
   */
  def binData() = {
    if (_bin==null) null else ( if (!_cmpz) _bin else gunzip( _bin) )
  }

  /**
   * Get the size of the internal data (no. of bytes).
   *
   * @return
   */
  def size() = {
    if ( isDiskFile) new File(_fp).length() else _binSize
  }


  override def finalize() {
    destroy()
  }

  override def toString() = {
    new String ( javaBytes(), _encoding )
  }

  /**
   *
   */
  def stream(): InputStream = {
    if (isDiskFile) { new XStream(new File( _fp)) }
    else if ( _bin==null) { asStream(null) }
    else {
      asStream( if (_cmpz) gunzip(_bin) else _bin )
    }
  }

  private def maybeCmpz(bits:Array[Byte]) {
    _binSize= if (bits==null) 0L else bits.length
    _cmpz=false
    _bin=bits
    if (_binSize > CMPZ_THREADHOLD) {
      _cmpz=true
      _bin= gzip(bits)
    }
  }

  private def reset() {
    _cmpz=false
    _cls=true
    _bin=null
    _fp=null
    _binSize=0L
  }

}

