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

(ns com.zotoh.frwk.util.coreutils 
  ;;(:use [ clojure.tools.logging :as LOG ])
  (:import (java.security SecureRandom))
  (:import (java.nio.charset Charset))
  (:import (java.util Properties Date GregorianCalendar ))
  (:import (java.sql Timestamp))
  import org.apache.commons.lang3.text.{StrSubstitutor=>STS}
import scala.collection.mutable
import org.slf4j._
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.FilenameUtils._
import org.apache.commons.lang3.{SerializationUtils=>SER}

  )

(defn ^{
    :doc "Return a new random object."
  }
  new-random []
    (SecureRandom. (SecureRandom/getSeed 20)) )

(defn ^{
    :doc "Return a sql Timestamp."
  }
  nowJTS []
    (Timestamp. (.getTime (Date.))))

(defn ^{
    :doc "Return a java Date."
  }
  nowJDate [] 
    (Date. ) )

(defn ^{
    :doc "Return a Gregorian Calendar."
  }
  nowCal []
    (GregorianCalendar. ))


(defn ^{
    :doc "Return a java Charset of the encoding."
  }
  chSet
  ([] (chSet "UTF-8"))
  ([enc] (Charset/forName enc)) )


(defn ^{
    :doc "Convert the file path into nice format without backslashes."
  }
  niceFPath [ fp ]
    (if (nil? fp) (str "") (niceFPath (.getCanonicalPath fp)) ))

(declare normalizeNoEndSeparator)
(defn ^{
    :doc "Convert the file path into nice format without backslashes."
  }
  niceFPath [fpath]
    (normalizeNoEndSeparator fpath true) )

(defn ^{
    :doc ""
  }
  filterEnvVar [ pv envs ]
  pv)

(defn ^{
    :doc ""
  }
  filterEnvVars [ props envs ]
    (reduce 
      (fn [bc k]
        (.put bc k (filterEnvVar (.get props k) envs)) bc )
      (Properties.) (.keySet props) ))



(defn ^{
    :doc ""
  }
  normalizeNoEndSeparator [ fpath a ]
  nil )



(def ^:private coreutils-eof nil)

