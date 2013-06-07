(ns com.zotoh.frwk.io.xdata
  (:import (java.io ByteArrayOutputStream File InputStream))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.io FileUtils))
  (:require [com.zotoh.frwk.util.coreUtils :as CU])
  (:require [com.zotoh.frwk.util.strutils :as SU])
  (:require [com.zotoh.frwk.io.ioutils :as IO])
  )

;;
;; Wrapper structure to abstract a piece of data which can be a file
;; or a memory byte[].  If the data is byte[], it will also be
;; compressed if a certain threshold is exceeded.
;;
;; @author kenl
;;


(defprotocol IXData
  ())

(defrecord XData [ _fp _data _encoding _delFlag ] IXData
  (javaBytes [this] )
  (setEncoding! [this enc] (reset! _encoding enc))
  (encoding [this] @_encoding)
  (isZiped? [this] @_cmpz)
  (setDeleteFlag! [this del] (reset! _delFlag del))
  (isDeleteFlag? [this] @_delFlag)
  (finz! [this]
    (do
      (if (and (SU/hgl @_fp) @_delFlag) (FileUtils/deleteQuietly (File. @_fp)))
      (reorg!)))
  (isDiskFile? [this] (SU/hgl @_fp))
  (resetData! 
    ( [ this obj ] (resetData! obj true))
    ( [ this obj delflag] 
      (do
        (finz!)
        (cond
          (instance? ByteArrayOutputStream obj) (maybeCmpz (.toByteArray obj))
          (instance? CharArrayWriter obj) ()
          (instance? (Class/forName "[B") obj) (maybeCmpz obj)
          (instance? File obj) (reset! _fp (CU/niceFPath obj))
          (instance? String obj) (maybeCmpz (.getBytes obj @_encoding))
          (not (nil? obj)) (throw IOException. (str "Unsupported data type" (.getClass obj)))
          :else nil)
        (setDeleteFlag! delFlag))))

(defn content
  ""
  [this]
  (if (isDiskFile) (File. @_fp) @_data))

(defn hasContent?
  ""
  [this]
  (if (isDiskFile) true (not (nil? _data))))

(defn javaBytes
  ""
  [this]
  (if (isDiskFile) (FileUtils/readFileToByteArray(File. @_fp))
    (cond
      (instance? (CU/bytesClass) _data) _data
      (instance? String _data) (.getBytes _data @_encoding)
      (instance? (CU/charsClass) _data) (BU/toByteArray _data (Charset/forName @_encoding))
      :else nil)))

(defn fileRef
  ""
  [this]
  (if (StringUtils/isEmpty @_fp) nil (File. @_fp)))

(defn filePath
  ""
  [this]
  (if (StringUtils/isEmpty @_fp) "" else @_fp))

(defn size
  ""
  [this]
  (if (isDiskFile) 
    (.length (File. @_fp))
    (let [ b (javaBytes) ]
      (if (nil? b) 0 (alength b)))))


  override def finalize() {
    destroy()
  }

  def stringify() = {
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


