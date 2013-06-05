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

(ns com.zotoh.frwk.util.procutils 
  (:import (java.lang.management ManagementFactory))
  (:import (com.zotoh.frwk.util ProcUtils))
  (:require [ com.zotoh.frwk.util.coreutils :as CU])
  (:require [ com.zotoh.frwk.util.strutils :as SU])
  )


(defn asyncExec
  "Run the code (runnable) in a separate daemon thread."
  [r]
  (if (nil? r)
    nil
    (doto (Thread. r)
      (.setContextClassLoader (CU/getCZldr))
      (.setDaemon true)
      (.start))))

(defn safeWait
  "Block current thread for some millisecs."
  [millisecs]
  (try
    (if (> millisecs 0) (Thread/sleep millisecs))
    (catch Throwable t nil)))

(defn blockAndWait
  ""
  [lockr waitMillis]
  (do
    (ProcUtils/blockAndWait lockr waitMillis)))

(defn unblock
  ""
  [lockr]
  (ProcUtils/unblock lockr))

(defn pid
  ""
  []
  (let [ ss (.split (SU/nsb (.getName (ManagementFactory/getRuntimeMXBean))) "@") ]
    (if (or (nil? ss) (empty ss)) "" (first ss))))

(defn blockForever
  ""
  ( [] (blockForever 8000))
  ( [waitMillis]
    (loop []
      (safeWait waitMillis)
      (recur))) )








(def ^:private procutils-eof nil)

