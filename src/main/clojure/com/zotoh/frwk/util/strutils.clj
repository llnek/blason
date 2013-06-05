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

(ns com.zotoh.frwk.util.strutils 
  (:import (java.io CharArrayWriter File OutputStream OutputStreamWriter Reader Writer))
  (:import (java.util Arrays Collection Iterator StringTokenizer))
  (:import (java.lang StringBuilder))
  (:import (org.apache.commons.lang3 StringUtils))
  )

(defn nsb
  "Returns empty string if obj is null, or obj.toString."
  [obj]
  (if (nil? obj) "" (.toString obj)))

(defn nsn
  "Returns \"(null)\" if obj is null, or obj.toString."
  [obj]
  (if (nil? obj) "(null)" (.toString obj)))

(defn contains?
  "Returns true if this character is inside this string."
  [astr ch]
  (do
    (>= (.indexOf astr (int ch)) 0)))

(defn same
  "Returns true if these 2 strings are the same."
  [ a b ]
  (if (not= (.length a) (.length b))
    false
    (Arrays/equals (.toCharArray a) (.toCharArray b))))

(defn hgl
  "Returns true if this string is not empty."
  [s]
  (if (nil? s) false (> (.length s) 0)))

(defn strim
  "Safely trim this string - handles null."
  [s]
  (if (nil? s) "" (.trim s)))

(defn addAndDelim
  "Append to a string-builder, optionally inserting a delimiter if the buffer is not empty."
  [buf delim  item]
  (let []
    (when (not (nil? item))
      (when (and (> (.length buf) 0) (not (nil? delim)))
        (.append buf delim))
      (.append buf item))
    buf))

(defn splitIntoChunks
  "Split a large string into chucks, each chunk having a specific length."
  [largeString chunkLength]
  (if (nil? largeString)
    []
    (loop [ ret [] src largeString ]
      (if (<= (.length src) chunkLength)
        (if (> (.length src) 0) (conj ret src) ret)
        (recur (conj ret (.substring src 0 chunkLength)) (.substring src chunkLength)) ))))

(defn hasWithin
  "Returns true if src contains one of these substrings."
  [src substrs]
  (if (nil? src)
    false
    (some #(>= (.indexOf src %) 0) substrs)))

(defn startsWith
  "Tests startWith(), looping through the list of possible prefixes."
  [src pfxs]
  (if (nil? src)
    false
    (some #(.startsWith src %) pfxs)))

(defn equalsOneOfIC
  "Tests String.equals() against a list of possible args. (ignore-case)."
  [src strs]
  (if (nil? src)
    false
    (some #(.equalsIgnoreCase src %) strs)))

(defn equalsOneOf
  "Tests String.equals() against a list of possible args."
  [src strs]
  (if (nil? src)
    false
    (some #(.equals src %) strs)))

(defn- lcs [s] (.toLowerCase s))
(defn hasWithinIC
  "Tests String.indexOf() against a list of possible args. (ignoring case)."
  [src substrs]
  (if (nil? src)
    false
    (some #(>= (.indexOf (lcs src) (lcs %)) 0) substrs)))

(defn startsWithIC
  "Tests startsWith (ignore-case)."
  [src pfxs]
  (if (nil? src)
    false
    (some #(.startsWith (lcs src) (lcs %)) pfxs)))

(defn mkString
  ""
  [ch times]
  (let [ buf (StringBuilder.) ]
    (dotimes [ n times ]
      (.append buf ch))
    (.toString buf)))

(defn right
  "Gets the rightmost len characters of a String."
  [src len]
  (if (nil? src)
    ""
    (StringUtils/right src len)) )

(defn left
  "Gets the leftmost len characters of a String."
  [src len]
  (if (nil? src)
    ""
    (StringUtils/left src len)) )














(def ^:private strutils-eof nil)
