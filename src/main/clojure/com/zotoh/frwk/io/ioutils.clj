(ns com.zotoh.frwk.io.ioutils
  (:require [ com.zotoh.frwk.util.byteutils :as BU])
  (:require [ com.zotoh.frwk.util.strutils :as SU])
  (:require [ com.zotoh.frwk.util.coreutils :as CU])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream))
  (:import (java.io File FileInputStream FileOutputStream CharArrayWriter OutputStreamWriter))
  (:import (java.io InputStream InputStreamReader OutputStream Reader Writer))
  (:import (java.util.zip GZIPInputStream GZIPOutputStream))
  (:import (com.zotoh.frwk.io XStream))
  (:import (com.zotoh.frwk.io.IOUtils :as IO))
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

(defn mkTempFile
  ""
  ([] (mkTempFile "" ""))
  ([pfx sux]
    (File/createTempFile (if (StringUtils/isEmpty pfx) "temp-" pfx) (if (StringUtils/isEmpty sux) ".dat" sux) *work-dir*)))

(defn newTempFile
  ""
  ([] (newTempFile false))
  ([open]
    (let [ f (mkTempFile) ]
      (if open [ f (FileOutputStream. f) ] [ f nil ]))))

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
  (with-open [ baos (ByteArrayOutputStream. (int 4096)) ]
    (let [ buf (byte-array 4096) ]
      (loop [ n (.read inp) ]
        (if (< n 0)
          (.toByteArray baos)
          (do 
            (if-not (= n 0) (.write baos cb 0 n))
            (recur (.read inp))))))))

(defn gzip
  "Gzip a string to bytes."
  ( [astr] (gzip "UTF-8"))
  ( [astr encoding]
    (if (or (nil? s)(nil? encoding)) nil (IO/gzip (.getBytes astr encoding)))))

(defn safeReset
  "Call reset on this input stream."
  [inp]
  (try
    (if-not (nil? inp)  (.reset inp))
    (catch Throwable t nil)) )

(defn asStream
  ""
  [bits]
  (if (nil? bits) nil (ByteArrayInputStream. bits)))

(defmulti openFile class)

(defmethod ^{ :doc "Open this file." } openFile [ f] File
  (if (nil? f) nil (XStream. f)))

(defmethod ^{ :doc "Open this file path." } openFile [ fp] String
  (if (nil? f) nil (XStream. (File. fp))))

(defn fromGZipedB64
  "Unzip content which is base64 encoded + gziped."
  [gzb64]
  (if (nil? gzb64) nil (IO/gunzip (Base64/decodeBase64 gzb64))))

(defn toGZipedB64
  ""
  [bits]
  (if (nil? bits) nil (Base64/encodeBase64String (IO/gzip bits))))

(defn available
  "Get the available bytes in this stream."
  [inp]
  (if (nil? inp) 0 (.available inp)))

(defn writeFile
  ""
  ( [outFile astr] (writeFile outFile astr "UTF-8"))
  ( [outFile astr encoding]
    (if-not (nil? astr) (writeFileBytes out (.getBytes astr encoding)))))

(def writeFileBytes
  ""
  [outFile bits]
  (if-not (nil? bits)
    (with-open [ os (FileOutputStream. outFile) ]
      (.write os bits))))

(defn copyStreamToFile
  ""
  [inp]
  (let [ t (IO/newTempFile true) ]
    (with-open [ os (nth t 1) ]
      (IOUtils/copy inp os))
    (nth t 0)))

(defn copyCountedBytes
  ""
  [src out bytesToCopy]
  (IOUtils/copyLarge src out 0 bytesToCopy))

(defn resetInputSource
  ""
  [inpsrc]
  (if-not (nil? inpsrc)
    (let [ rdr (.getCharacterStream inpsrc)
           ism (.getByteStream inpsrc) ]
      (try
        (if-not (nil? ism) (.reset ism))
        (catch Throwable t nil))
      (try
        (if-not (nil? rdr) (.reset rdr))
        (catch Throwable t nil)) )))

(defn readBytes
  ""
  ( [inp] (read-bytes inp *stream-limit*))
  ( [inp useFile] read-bytes(inp (if useFile 1 *stream-limit*))))

(defn- swap-bytes [inp baos]
  (let [ bits (.toByteArray baos) t (newTempFile true) ]
    (.write (nth t 1) bits)
    (.flush (nth t 1))
    (.close baos)
    t))

(defn- swap-read-bytes [inp baos]
  (let [ t (swap-bytes inp baos)
         bits (byte-array 4096)
         os (nth t 1) ]
    (try
      (loop [c (.read inp bits) ]
        (if (< c 0)
          (XData. (nth t 0))
          (if (= c 0)
            (recur (.read inp bits))
            (do (.write os bits 0 c)
                (recur (.read inp bits))))))
      (finally
        (.close os))))

(defn- read-bytes [inp lmt]
  (let [ baos (ByteArrayOutputStream. (int 10000)) 
         bits (byte-array 4096) ]
    (loop [ c (.read inp bits) cnt 0 ]
      (if (< c 0)
        (XData. baos)
        (if (= c 0) (recur (.read inp bits) cnt)
          (do ;; some data
            (.write baos bits 0 c)
            (if (> (+ c cnt) lmt)
              (swap-read-bytes inp baos)
              (recur (.read inp bits) (+ c cnt)) )))))))

(defn- swap-read-chars [ inp wtr ]
  (let [ bits (.toCharArray wtr) t (newTempFile true) ]
    (.write (nth t 1) bits)
    (.flush (nth t 1))
    (.close wtr)
    t))

(defn- read-chars [inp lmt]
  (let [ wtr (CharArrayWriter. (int 10000))
         bits (char-array 4096) ]
    (loop [ c (.read inp bits) cnt 0 ]
      (if (< c 0)
        (XData. wtr)
        (if (= c 0)
          (recur (.read inp bits) cnt)
          (do
            (.write wtr bits 0 c)
            (if (> (+ c cnt) lmt)
              (swap-read-chars inp wtr)
              (recur (.read inp bits) (+ c cnt)))))))))

(defn readChars 
  ""
  ([rdr] (read-chars *stream-limit*))
  ([rdr useFile] (read-chars (if useFile 1 *stream-limit*))))

(def mkFSData
  ""
  []
  (.setDeleteFile (XData. (mkTempFile)) true))

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


