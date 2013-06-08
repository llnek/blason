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

(ns com.zotoh.frwk.io.XStream
  (:import (java.io File FileInputStream IOException InputStream))
  (:import (org.apache.commons.io FileUtils))
  (:import (org.apache.commons.io IOUtils))
  (:import (java.nio.charset Charset))
  (:require [com.zotoh.frwk.util.coreutils :as CU])
  ;;(:require [com.zotoh.frwk.io.ioutils :as IO])
  )

;;
;; Wrapper on top of a File input stream such that it can
;; delete itself from the file system when necessary.
;;
;; @author kenl
;;
;;

(gen-class
  :name com.zotoh.frwk.io.XStream
  :prefix x-
  :extends java.io.InputStream
  :init iniz
  :state gstate
  :constructors { [File boolean] []
                 [File] [] }
  :methods [ [ ready! [] void ] 
             [ pre! [] void ]
             [ setDelete! [boolean] void ]
             [ delete! [] void ]
             [ getPosition [] long ] ]
)

(defn x-iniz
  ([fp] (x-iniz fp false))
  ([fp b] 
   (let [ flds (atom { :fn fp :delFlag b :closed true :inp nil :pos 0 }) ]
     ([] flds))))

(defn x-gstate [this] (.state this))

(defn x-close [this]
  (let [ flds @(x-gstate) ]
    (IOUtils/closeQuietly (:inp flds)) (swap! (.state this) assoc :inp nil) (swap! (.state this) assoc :closed true)))

(defn x-reset [this]
  (let [ flds @(x-gstate) fp (:fn flds) ]
    (.close this)
    (swap! (.state this) assoc :inp  (FileInputStream. fp))
    (swap! (.state this) assoc :closed false)
    (swap! (.state this) assoc :pos 0)))

(defn x-ready! [this] (.reset this))

(defn x-pre! [this]
  (let [ flds @(x-gstate) ]
    (if (:closed flds) (.ready! this))))

(defn x-available [this]
  (let [ flds @(x-gstate) ]
    (.pre! this)
    (.available (:inp flds))))

(defn x-read
  ( [this] 
    (let [ flds @(x-gstate) ]
      (.pre! this) 
      (let [ r (.read (:inp flds)) p (:pos flds) ]
        (swap! (.state this) assoc :pos (inc p))
        r)))
  ( [this b] (if (nil? b) -1 (read b 0 (alength b))))
  ( [this b offset len]
    (if (nil? b)
      -1
      (do
        (.pre! this)
        (let [ flds @(x-gstate)
               r (.read (:inp flds) b offset len)
               p (if (= r -1) -1 (+ (:pos flds) r)) ]
          (swap! (.state this) assoc :pos p)
          r)))))

(defn x-skip [this n]
  (let [ flds @(x-gstate) ]
    (if (< n 0)
      -1
      (do
        (.pre! this)
        (let [ r (.skip (:inp flds) n)  p (:pos flds) ]
          (if (> r 0) (swap! (.state this) assoc :pos (+ p r))) r)))))

(defn x-mark [ this readLimit ]
  (let [ flds @(x-gstate) inp (:inp flds) ]
    (if-not (nil? inp) (.mark inp readLimit))) )

(defn x-markSupported [this] true)

(defn x-setDelete! [ this dfile] 
  (let [ flds @(x-gstate) ]
    (swap! (.state this) assoc :deleteFlag dfile)) )

(defn x-delete! [this]
  (let [ flds @(x-gstate) fp (:fn flds) df (:deleteFlag flds) ]
    (.close this)
    (if df (FileUtils/deleteQuietly fp))))

(defn x-filename [this]
  (let [ flds @(x-gstate) fp (:fn flds) ]
    (if (nil? fp) "" (CU/niceFPath fp))) )

(defn x-toString [this] 
  (let [ flds @(x-gstate) fp (:fn flds) ]
    (.getCanonicalPath fp)) )

(defn x-getPosition [this]
  (let [ flds @(x-gstate) ] (:pos flds)))

(defn x-finalize [this] (.delete! this))









(def ^:private xstream-eof nil)

