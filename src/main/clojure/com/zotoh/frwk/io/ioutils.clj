(ns com.zotoh.frwk.io.ioutils
  (:require [ com.zotoh.frwk.util.byteutils :as BU])
  (:require [ com.zotoh.frwk.util.strutils :as SU])
  (:require [ com.zotoh.frwk.util.coreutils :as CU])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream))
  (:import (java.io File FileInputStream FileOutputStream CharArrayWriter OutputStreamWriter))
  (:import (java.io InputStream InputStreamReader OutputStream Reader Writer))
  (:import (java.util.zip GZIPInputStream GZIPOutputStream))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.codec.binary Base64))
  (:import (org.apache.commons.io IOUtils))
  (:import (org.xml.sax InputSource))
  (:import (java.nio.charset Charset))
  )

;;
;; Util functions related to stream/io.
;;
;; @author kenl
;;

(def ^:private HEX_CHS ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' ])
(def ^:dynamic READ_STREAM_LIMIT (* 1024 1024 8)) ;; if > 8M switch to file

;;(defn streamLimit_=(n:Int) { READ_STREAM_LIMIT=n }
;;(defn streamLimit= READ_STREAM_LIMIT

(defn bytesToHexString
  ""
  [bits]
  (if (nil? bits) nil (String. (bytesToHexChars  bits))))

(defn bytesToHexChars
  ""
  [bits]
  (let [ len (* 2 (if (nil? bits) 0 (alength bits)))
         out (char-array len) ]
    (if-not (<= len 0)
      (loop [  k 0 pos 0 ]
        (if (>= pos len)
          nil
          (let [ n (bit-and (aget bits k) 0xff) ]
            (aset-char out pos (nth HEX_CHS (bit-shift-right n 4))) ;; high 4 bits
            (aset-char out (+ pos 1) (nth HEX_CHS (bit-and n 0xf))) ;; low 4 bits
            (recur (inc k) (+ 2 pos)) ))))
    out))

(defmulti readBytes class)

(defmethod ^{ :doc "Read bytes from file." } readBytes [f] File
  (with-open [ inp (FileInputStream. f) ]
    (readBytes inp)))

(defmethod ^{ :doc "Read bytes from file path." } readBytes [fp] String
  (with-open [ inp (FileInputStream. (File. fp)) ]
    (readBytes inp)))

(defmethod ^{ :doc "Read bytes from inputstream." } readBytes [inp] InputStream
  )

(defn gzip
  "Gzip a string to bytes."
  ( [astr] (gzip "UTF-8"))
  ( [astr encoding]
    (if (or (nil? s)(nil? encoding)) nil (gzipBytes (.getBytes astr encoding)))))

  /**
   * Calls InputStream.reset().
   *
   * @param inp
   */
  def safeReset(inp:InputStream) {
    block { () => if (inp != null) inp.reset() }
  }

  /**
   * @param bits
   * @return
   */
  def gzip(bits:Array[Byte]): Array[Byte] = {
    if (bits==null) null else using(new ByteArrayOS(4096)) { (baos) =>
      if (bits!=null) using(new GZIPOutputStream(baos)) { (g) =>
          g.write(bits, 0, bits.length)
      }
      baos.toByteArray
    }
  }

  /**
   * @param bits
   * @return
   */
  def gunzip(bits:Array[Byte]) = {
    if (bits==null) null else bytes(new GZIPInputStream( asStream(bits)))
  }

  /**
   * @param bits
   * @return
   */
  def asStream(bits:Array[Byte]): InputStream = {
    if (bits==null) null else new ByteArrayIS(bits)
  }

  /**
   * @param ins
   * @return
   */
  def gunzip(ins:InputStream): InputStream = {
    if (ins==null) null else new GZIPInputStream(ins)
  }

  /**
   * @param ins
   * @return
   */
  def bytes(ins:InputStream): Array[Byte] = {
    using( new ByteArrayOS(4096)) { (baos) =>
      val cb= new Array[Byte](4096)
      var n=0
      do {
        n = ins.read(cb)
        if (n>0) { baos.write(cb, 0, n) }
      } while (n>0)
      baos.toByteArray
    }
  }

  /**
   * @param fp
   * @return
   */
  def open(fp:File): InputStream = {
    if ( fp==null ) null else new XStream(fp)
  }

  /**
   * @param gzb64
   * @return
   */
  def fromGZipedB64(gzb64:String) = {
    if (gzb64==null) null else gunzip(Base64.decodeBase64(gzb64))
  }

  /**
   * @param bits
   * @return
   */
  def toGZipedB64(bits:Array[Byte]) = {
    if (bits == null) null else Base64.encodeBase64String( gzip(bits))
  }

  /**
   * @param inp
   * @return
   */
  def available(inp:InputStream) = {
    if (inp==null) 0 else inp.available()
  }

  /**
   * @param inp
   * @param useFile if true always use file-backed data.
   * @return
   */
  def readBytes(inp:InputStream, useFile:Boolean=false): XData = {
    var lmt= if (useFile) 1 else READ_STREAM_LIMIT
    val baos= new ByteArrayOS(10000)
    val bits= new Array[Byte](4096)
    var os:OutputStream= baos
    val rc= new XData()
    var cnt=0
    var loop=true

    try {

      while (loop) {
        loop= inp.read(bits) match {
          case c if c > 0 =>
            os.write(bits, 0, c)
            cnt += c
            if ( lmt > 0 && cnt > lmt) {
              os=swap(baos, rc)
              lmt= -1
            }
            true
          case _ => false
        }
      }

      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetMsgContent(baos)
      }

    } finally {
      close(os)
    }

    rc
  }

  /**
   * @param rdr
   * @param useFile if true always use file-backed data.
   * @return
   */
  def readChars(rdr:Reader, useFile:Boolean=false) = {
    var lmt = if (useFile) 1  else READ_STREAM_LIMIT
    val wtr= new CharArrayWriter(10000)
    val bits= new Array[Char](4096)
    var w:Writer=wtr
    val rc= new XData()
    var cnt=0
    var loop=true

    try {
      while(loop) {
        loop = rdr.read(bits) match {
          case c if c > 0 =>
            w.write(bits, 0, c)
            cnt += c
            if ( lmt > 0 && cnt > lmt) {
              w=swap(wtr, rc)
              lmt= -1
            }
            true
          case _ => false
        }
      }

      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetMsgContent(wtr.toString)
      }

    } finally {
      close(w)
    }

    rc
  }

  /**
   * @param out
   * @param s
   * @param enc
   */
  def writeFile(out:File, s:String, enc:String="utf-8") {
    if (s != null) { writeFile( out, s.getBytes( enc)) }
  }

  /**
   * @param out
   * @param bits
   */
  def writeFile(out:File, bits:Array[Byte]) {
    if (bits != null) {
      using(new FileOutputStream(out)) { (os) =>
        os.write(bits)
      }
    }
  }

  /**
   * @param src
   * @return
   */
  def copy(src:InputStream): File = {
    var t=newTempFile(true)
    using(t._2) { (os) =>
      IOU.copy(src, os)
    }
    t._1
  }

  /**
   * @param src
   * @param out
   * @param bytesToCopy
   */
  def copy(src:InputStream, out:OutputStream, bytesToCopy:Long) {
    IOU.copyLarge(src,out,0,bytesToCopy)
  }

  /**
   * @param iso
   */
  def resetInputSource(iso:InputSource) {
    if (iso != null) {
      val rdr= iso.getCharacterStream()
      val ism = iso.getByteStream()
      block { () => if (ism != null) ism.reset() }
      block { () => if (rdr != null) rdr.reset() }
    }
  }

  /**
   * @param pfx
   * @param sux
   * @return
   */
  def mkTempFile(pfx:String="", sux:String=""): File = {
    File.createTempFile(
      if ( STU.isEmpty(pfx)) "temp-" else pfx,
      if ( STU.isEmpty(sux)) ".dat" else sux,
      workDir)
  }

  /**
   * @return
   */
  def mkFSData() = new XData( mkTempFile()).setDeleteFile(true)

  /**
   * @param open
   * @return
   */
  def newTempFile(open:Boolean=false): (File,OutputStream) = {
    val f= mkTempFile()
    (f, if (open) new FileOutputStream(f) else null)
  }

  /**
   * @param r
   */
  def close(r:Reader) {
    IOU.closeQuietly(r)
  }

  /**
   * @param o
   */
  def close(o:OutputStream) {
    IOU.closeQuietly(o)
  }

  /**
   * @param w
   */
  def close(w:Writer) {
    IOU.closeQuietly(w)
  }

  /**
   * @param i
   */
  def close(i:InputStream) {
    IOU.closeQuietly(i)
  }

  /**
   * @param b Array[Byte] to be converted.
   * @param count Number of Array[Byte] to read.
   * @param cs Charset.
   * @return Converted char array.
   */
  def bytesToChars(b:Array[Byte], count:Int, cs:Charset) = {
    val bb= if (count != b.length) {
      b.slice(0, min(b.length, count))
    } else { b }
    convertBytesToChars(bb,cs)

//    (1 to min(b.length, count)).foreach { (i) =>
//      val b1 = b(i-1)
//      ch(i-1) = (if (b1 < 0) { 256 + b1 } else b1 ).asInstanceOf[Char]
//    }
//    ch
  }

  /**
   * Tests if both streams are the same or different at byte level.
   *
   * @param s1
   * @param s2
   * @return
   */
  def different(s1:InputStream, s2:InputStream): Boolean = {
    ! IOU.contentEquals(s1,s2)
  }

  def readText(fn:File, enc:String="utf-8"): String = {
    val sb= new StringBuilder(4096)
    val cs= new Array[Char](4096)

    using(new InputStreamReader(open(fn), enc)) { (rdr) =>
      var loop=true
      while (loop) {
        loop = rdr.read(cs) match {
          case n if n > 0 => sb.appendAll(cs, 0, n); true
          case _ => false
        }
      }
    }

    sb.toString
  }

  private def swap(baos:ByteArrayOS, data:XData): OutputStream = {
    val bits=baos.toByteArray
    val t= newTempFile(true)
    if ( !isNilSeq(bits)) {
      t._2.write(bits)
      t._2.flush()
    }
    baos.close
    data.resetMsgContent(t._1)
    t._2
  }

  private def swap(wtr:CharArrayWriter, data:XData) = {
    val t= newTempFile(true)
    val w= new OutputStreamWriter(t._2)
    data.resetMsgContent(t._1)
    w.write( wtr.toCharArray)
    w.flush()
    w
  }

}

sealed class IOUtils {}


