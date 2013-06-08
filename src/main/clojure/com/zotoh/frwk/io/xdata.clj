;;
;; COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
;;
;; THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
;; MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE
;; VERSION 2.0 (THE "LICENSE").
;;
;; THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL
;; BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
;; MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
;;
;; SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
;; AND LIMITATIONS UNDER THE LICENSE.
;;
;; You should have received a copy of the Apache License
;; along with this distribution; if not you may obtain a copy of the
;; License at
;; http://www.apache.org/licenses/LICENSE-2.0
;;

(ns com.zotoh.frwk.io.xdata
  (:import (java.io ByteArrayOutputStream File InputStream))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.io FileUtils))
  (:require [com.zotoh.frwk.util.coreUtils :as CU])
  (:require [com.zotoh.frwk.util.strutils :as SU])
  (:require [com.zotoh.frwk.util.metautils :as MU])
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
  (setEncoding! [this enc])
  (encoding [this])
  (setDeleteFlag! [this del])
  (isDeleteFlag? [this])
  (finz! [this])
  (isDiskFile? [this])
  (resetData! [this] )
  (content [this] )
  (hasContent? [this] )
  (javaBytes [this] )
  (fileRef [this] )
  (filePath [this] )
  (size [this])
  (finalize [this] )
  (stringify [this] )
  (stream [this])
  (reorg! [this] ))

(defrecord XData [ _data _encoding _delFlag ] IXData
  (setEncoding! [this enc] (reset! _encoding enc))
  (encoding [this] @_encoding)
  (setDeleteFlag! [this del] (reset! _delFlag del))
  (isDeleteFlag? [this] @_delFlag)
  (finz! [this]
    (do
      (if (and (instance? File @_data) @_delFlag)
        (FileUtils/deleteQuietly @_data)
        (reorg!))))
  (isDiskFile? [this] (instance? File @_data))
  (resetData! 
    ( [ this obj ] (resetData! obj true))
    ( [ this obj delflag] 
      (do
        (finz!)
        (cond
          (instance? CharArrayWriter obj) (reset! _data (.toCharArray obj))
          (instance? ByteArrayOutputStream obj) (reset! _data (.toByteArray obj))
          (instance? (MU/bytesClass) obj) (reset! _data obj)
          (instance? File obj) (reset! _data obj)
          (instance? String obj) (reset! _data obj)
          (not (nil? obj)) (throw IOException. (str "Unsupported data type" (.getClass obj)))
          :else nil)
        (setDeleteFlag! delFlag))))
  (content [this] @_data)
  (hasContent?  [this] (not (nil? @_data)))
  (javaBytes [this]
    (if (isDiskFile)
      (FileUtils/readFileToByteArray @_data)
      (cond
        (instance? (MU/bytesClass) @_data) @_data
        (instance? String @_data) (.getBytes @_data @_encoding)
        (instance? (MU/charsClass) @_data) (BU/toByteArray @_data (Charset/forName @_encoding))
        :else nil)))
  (filePath [this] (if (isDiskFile) (CU/niceFPath @_data) nil))
  (fileRef [this] (if (isDiskFile) @_data nil))
  (size [this]
    (if (isDiskFile)
      (.length @_data)
      (let [ b (javaBytes) ]
        (if (nil? b) 0 (alength b)))))
  (finalize [this] (destroy))
  (stringify [this] (String. (javaBytes) @_encoding))
  (stream [this]
    (if (isDiskFile)
      (XStream. @_data)
      (if (nil? @_data) nil (IO/asStream (javaBytes)))))
  (reorg! [this]
    (do 
      (reset! _data nil) 
      (reset _encoding "utf-8") 
      (reset! _delFlag false) )))


