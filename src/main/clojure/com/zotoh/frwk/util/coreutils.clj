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
  (:import (java.io File ByteArrayInputStream ByteArrayOutputStream))
  (:import (java.util Properties Date GregorianCalendar ))
  (:import (java.sql Timestamp))
  (:import (org.apache.commons.lang3.text StrSubstitutor))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.io FilenameUtils))
  (:import (org.apache.commons.lang3 SerializationUtils))
)

(def ^:private _BOOLS #{ "true" "yes"  "on"  "ok"  "active"  "1"} )
(defn- nsb [s]  (if (nil? s) (str "") s))

(defn new-random
  "Return a new random object."
  []
  (SecureRandom. (SecureRandom/getSeed 20)) )

(defn nowJTS
  "Return a sql Timestamp."  
  []
  (Timestamp. (.getTime (Date.))))

(defn nowJDate 
  "Return a java Date."
  [] 
  (Date. ) )

(defn nowCal
  "Return a Gregorian Calendar."
  []
  (GregorianCalendar. ))

(defn chSet
  "Return a java Charset of the encoding."
  ([] (chSet "UTF-8"))
  ([enc] (Charset/forName enc)) )


(defmulti niceFPath class)

(defmethod ^{ :doc "Convert the file path into nice format without backslashes." }
  niceFPath File 
  [ fp ]
  (if (nil? fp) (str "") (niceFPath (.getCanonicalPath fp)) ))

(defmethod ^{ :doc "Convert the file path into nice format without backslashes."  }
  niceFPath String 
  [fpath]
  (FilenameUtils/normalizeNoEndSeparator (nsb fpath) true) )

(defn filterVar
  "Replaces all the variables in the given value with their matching values from the system and env vars."
  [ value ]
  (if (nil? value) ""
    (.replace (StrSubstitutor. (System/getenv)) (StrSubstitutor/replaceSystemProperties value))))    

(defn filterSysVar
  "Expand any sys-var found inside the string value."
  [value]
  (if (nil? value) "" (StrSubstitutor/replaceSystemProperties value)) )  

(defn filterEnvVar
  "Expand any env-var found inside the string value."
  [value]
  (if (nil? value) "" (.replace (StrSubstitutor. (System/getenv)) value)) )    

(defn filterExpandVars 
  "Expand any env & sys vars found inside the property values."
  [ props ]
  (reduce 
    (fn [bc k]
      (.put bc k (filterVar (.get props k))) bc )
    (Properties.) (.keySet props) ))

(defn sysQuirk
  "Get the value of a system property."
  [prop]
  (System/getProperty (nsb prop) ""))

(defn userHomeDir
  "Get the user's home directory."
  []
  (File. (sysQuirk "user.home")) )

(defn userName
  "Get the current user login name."
  []
  (sysQuirk "user.name"))

(defn trimLastPathSep
  "Get rid of trailing dir paths."
  [path]  
  (.replaceFirst (nsb path) "[/\\\\]+$"  ""))

(defn serialize
  "Object serialization."
  [obj]
  (if (nil? obj) nil (SerializationUtils/serialize obj)) )

(defn deserialize
  "Object deserialization."
  [bits]
  (if (nil? bits) nil (SerializationUtils/deserialize bits)))

(defn safeGetClzname
  "Get the object's class name."
  [obj]
  (if (nil? obj) "null" (.getName (.getClass obj))))

(defn filePath
  "Get the file path."
  [fp]
  (if (nil? fp) "" (niceFPath fp)))

(defn isUnix
  "Returns true if platform is *nix."
  []
  (= (.indexOf (.toLowerCase (sysQuirk "os.name")) "windows") -1 )) 
                                                     
(defn isWindows
  "Returns true if platform is windows."
  []
  (not isUnix))

(defn asLong
  "Parse string as a long value."
  [ s dftLongVal ]
  (try
    (Long/parseLong s)
    (catch Throwable e dftLongVal)))

(defn asInt
  "Parse string as an int value."
  [ s dftIntVal ]
  (try
    (Integer/parseInt s)
    (catch Throwable e dftIntVal)))

(defn asDouble
  "Parse string as a double value."
  [ s dftDblVal ]
  (try
    (Double/parseDouble s)
    (catch Throwable e dftDblVal)))

(defn asFloat
  "Parse string as a double value."
  [ s dftFltVal ]
  (try
    (Float/parseFloat s)
    (catch Throwable e dftFltVal)))

(defn asBool
  ""
  [s]
  (contains? _BOOLS (.toLowerCase (nsb s))))
  
(defmulti asQuirks class)
(defmethod ^{ :doc "" } 
  asQuirks byte[]
  [bits]
  (asQuirks (ByteArrayInputStream. bits)))

(defmethod ^{ :doc "" } 
  asQuirks ByteArrayInputStream
  [inp]
  (let [ ps (Properties.) ]
    (.load ps inp)
    ps))







(def ^:private coreutils-eof nil)

