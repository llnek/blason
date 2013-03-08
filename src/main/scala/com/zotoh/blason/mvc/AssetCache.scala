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


package com.zotoh.blason
package mvc

import scala.collection.mutable
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.mime.MimeUtils._
import com.zotoh.frwk.io.IOUtils._

import java.io.File
import org.slf4j._

/**
 * @author kenl
 */
object AssetCache {
  private val _log= LoggerFactory.getLogger(classOf[AssetCache])
  private val _files= mutable.HashMap[String, WebAsset]()
  def tlog() = _log
  
  // we are just getting stuff, so no need to worry about
  // concurrency

  def getAsset(path:String): Option[WebAsset] = getAsset( new File(path) )

  def getAsset(path:File): Option[WebAsset] = {
    val key= niceFPath(path)
    _files.get(key) match {
      case x@Some(b) => 
        tlog.debug("AssetCache: got cached file: {}", path)
        x
      case _ => 
        fetchFile(path)
    }
  }

  private def fetchFile(path:File) = {
      val b= if (path.exists && path.canRead) {   new WebAsset(path)   } else {
        tlog.warn("AssetCache: failed to read/find file: {}", path)
        null
      }      
      if (b == null ) None else {
        tlog.debug("AssetCache: cached new file: {}", path)
        _files.put(b.key, b)
        Some(b)        
      }
  }
  
  
}

class WebAsset(private val _file:File) {
  
  private var _cType=guessContentType(_file, "utf-8", "text/plain")
  private val _bytes= read(_file)
  
  def contentType_=(s:String)  {
    _cType= nsb(s)
  }
  def contentType = _cType
  def key = niceFPath(_file)
  def bytes= _bytes
  def size= if (_bytes==null) 0L else _bytes.length.toLong
  
}

sealed class AssetCache {}

