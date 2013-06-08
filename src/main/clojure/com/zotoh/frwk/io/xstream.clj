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

(ns com.zotoh.frwk.io.xstream
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

(defn mkXStream
  ""
  ( [fp] (mkXStream fp false))
  ( [fp delFile]
    (let [ _fn (atom fp) 
           _deleteFlag (atom delFile) 
           _inp (atom nil) 
           _closed (atom true) 
           _pos (atom 0)
           obj  (proxy [InputStream] []
                  (close []
                    (do (IOUtils/closeQuietly @_inp) (reset! _inp nil) (reset! _closed true)))
                  (reset []
                    (do
                      (.close this)
                      (reset! _inp  (FileInputStream. @_fn))
                      (reset! _closed false)
                      (reset! _pos 0)))
                  (ready [] (.reset this))
                  (pre [] (if @_closed (.ready this)))
                  (available [] (do (.pre this)(.available @_inp)))
                  (read 
                    ( [] (do (.pre this)(let [ r (.read @_inp) ] (swap! _pos inc) r)))
                    ( [b] (if (nil? b) -1 (read b 0 (alength b))))
                    ( [b offset len]
                      (if (nil? b)
                        -1
                        (do
                          (.pre this)
                          (let [ r (.read @_inp b offset len)
                                 p (if (= r -1) -1 (+ @_pos r)) ]
                            (reset! _pos p)
                            r)))))
                  (skip [n]
                    (if (< n 0)
                      -1
                      (do
                        (.pre this)
                        (let [ r (.skip @_inp n) ]
                          (if (> r 0) (swap! _pos + r)) r))))                        
                  (mark [ readLimit ]
                    (if-not (nil? @_inp) (.mark @_inp readLimit)))
                  (markSupported [] true)
                  (setDelete! [dfile] (reset! _deleteFlag dfile))
                  (delete []
                    (do
                      (.close this)
                      (if (and (not (nil? @_fn)) @_deleteFlag) (FileUtils/deleteQuietly @_fn))))
                  (filename []
                    (if (nil? @_fn) "" (CU/niceFPath @_fn)))
                  (toString [] (.getCanonicalPath @_fn))
                  (getPosition [] @_pos)
                  (finalize [] (.delete this))
                  ) ]
    obj)))

