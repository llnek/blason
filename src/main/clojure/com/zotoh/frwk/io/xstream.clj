(ns com.zotoh.frwk.io.xstream
  (:import (java.io File FileInputStream IOException InputStream))
  (:import (org.apache.commons.io FileUtils))
  (:import (org.apache.commons.io IOUtils))
  (:import (java.nio.charset Charset))
  (:import [com.zotoh.frwk.util.coreutils :as CU])
  (:import [com.zotoh.frwk.io.ioutils :as IO])
  )

;;
;; Wrapper on top of a File input stream such that it can
;; delete itself from the file system when necessary.
;;
;; @author kenl
;;
;;

class XStream(protected var _fn:File, protected var _deleteFile:Boolean = false) extends InputStream {

  @transient private var _inp:InputStream = null
  protected var _closed = true
  private var pos = 0L

  override def available() = {
    pre()
    _inp.available()
  }

  override def read() = {
    pre()
    val r = _inp.read()
    pos += 1
    r
  }

  override def read(b:Array[Byte]) = {
    if (b==null) -1 else read(b, 0, b.length)
  }

  override def read(b:Array[Byte], offset:Int, len:Int) = {
    if (b==null) -1 else {
      pre()
      val r = _inp.read(b, offset, len)
      pos = if (r== -1 ) -1 else { pos + r }
      r
    }
  }

  /**
   * @param ch
   * @return
   */
  def readChars(cs:Charset, len:Int): (Array[Char], Int) = {
    var rc= Array[Char]()
    if (len > 0) {
      val b = new Array[Byte](len)
      val c = read(b, 0, len)
      if (c>0) { rc = bytesToChars(b, c, cs) }
    }
    (rc, rc.length )
  }

  override def skip(n:Long) = {
    if (n < 0L) -1L else {
      pre()
      val r= _inp.skip(n)
      if (r > 0L) { pos +=  r }
      r
    }
  }

  override def close() {
    IOU.closeQuietly(_inp)
    _inp= null
    _closed= true
  }


  override def mark(readLimit:Int) {
    if (_inp != null) {
      _inp.mark(readLimit)
    }
  }

  override def reset() {
    close()
    _inp= new FileInputStream(_fn)
    _closed=false
    pos=0
  }

  override def markSupported() = true

  def setDelete(dfile:Boolean): this.type = { _deleteFile = dfile ; this }

  def delete() {
    close()
    if (_deleteFile && _fn != null) {
      FUT.deleteQuietly(_fn)
    }
  }

  def filename(): String = {
    if (_fn != null) niceFPath(_fn) else ""
  }

  override def toString() = filename()

  def getPosition() =  pos

  override def finalize() {
    delete()
  }

  private def pre() {
    if (_closed) { ready() }
  }

  private def ready() {
    reset()
  }

}


