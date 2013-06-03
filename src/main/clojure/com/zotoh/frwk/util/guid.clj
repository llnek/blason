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

(ns com.zotoh.frwk.util.guid 
  (:import (java.net InetAddress) )
  (:import (java.lang StringBuilder) )
  (:import (java.lang Math) )
  (:require [ com.zotoh.frwk.util.coreutils :as CU ] )
  (:require [ com.zotoh.frwk.util.strutils :as STU ] )
  (:require [ com.zotoh.frwk.util.byteutils :as BU ] )
  (:require [ com.zotoh.frwk.util.seqnumgen :as SQ ] )
  )

;;
;; One way to generate an unique id.
;;
;; @author kenl
;;
;;
(declare splitHiLoTime)
(declare fmtXXX)
(declare fmt)

(defn- maybeSetIP []
  (try
    (let [ neta (InetAddress/getLocalHost) 
           b (.getAddress neta) ]
      (if (.isLoopbackAddress neta ) 
        (.nextLong (CU/newRandom))
        (if (= 4 (alength b)) (long (BU/readInt b)) (BU/readLong b) )
        ))
    (catch Throwable e (.printStackTrace e))))

(def ^:private _IP (Math/abs (maybeSetIP)) )

(defn newWWID
  "Return a new guid based on time and ip-address."
  [] 
  (let [ seed (.nextInt (CU/newRandom) (Integer/MAX_VALUE)) 
         ts (splitHiLoTime) ]
      (str (nth ts 0) (fmtXXX _IP) (fmtXXX seed) (fmtXXX (SQ/nextInt)) (nth ts 1)) ))

(defmulti ^:private fmtXXX  class )

(defmethod ^:private fmtXXX Long [ num ]
    (fmt "0000000000000000"  (Long/toHexString num)) )

(defmethod ^:private  fmtXXX Integer [ num ]
    (fmt "00000000"  (Integer/toHexString num)) )

(defn- fmt [ pad mask ]
  (let [ mlen (.length mask)
         plen (.length pad) ]
    (if (>= mlen plen) (.substring mask 0 plen)
      (.toString (.replace (StringBuilder. pad) (- plen mlen) plen mask ) ))))

(defn- splitHiLoTime []
  (let [ s (fmtXXX (System/currentTimeMillis))
         n (.length s) ]
    [ (STU/left s (/ n 2)) (STU/right s (max 0 (- n (/ n 2 )) )) ] ))    


(def ^:private guid-eof  nil)

