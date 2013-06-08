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

(ns com.zotoh.frwk.util.metautils 
  (:import (java.lang.reflect Field Method Modifier))
  (:require [ com.zotoh.frwk.util.coreutils :as CU ] )
  (:require [ com.zotoh.frwk.util.strutils :as SU ] )
  )

;;
;; Utility functions for class related or reflection related operations.
;;
;; @author kenl
;;

(defn isChildCZ
  "Returns true if clazz is subclass of this base class."
  [basz cz]
  (if (or (nil? basz) (nil? cz)) false (.isAssignableFrom basz cz)))

(defn isChildObj
  "Returns true if object is subclass of this base class."
  [basz obj]
  (if (or (nil? basz) (nil? obj)) false (isChildCZ basz (.getClass obj))))

(defn bytesClass
  "Return the java class for byte[]."
  []
  (Class/forName "[B"))

(defn charsClass
  "Return the java class for char[]."
  []
  (Class/forName "[C"))

(defn isBoolean
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) ["boolean" "Boolean" "java.lang.Boolean"] ) true false))

(defn isChar
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "char" "Char" "java.lang.Char" ] ) true false))

(defn isInt
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "int" "Int" "java.lang.Integer" ] ) true false))

(defn isLong
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "long" "Long" "java.lang.Long" ] ) true false))

(defn isFloat
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "float" "Float" "java.lang.Float" ]) true false ))

(defn isDouble
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "double" "Double" "java.lang.Double" ]) true false))

(defn isByte
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "byte" "Byte" "java.lang.Byte" ]) true false ))

(defn isShort
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "short" "Short" "java.lang.Short" ]) true false))

(defn isString
  ""
  [cz]
  (if (SU/equalsOneOf (.getName cz) [ "String" "java.lang.String" ]) true false))

(defn isBytes
  ""
  [cz]
  (if (= cz (.getClass (byte-array 0))) true false))

(defn forName
  ""
  ( [z] (forName z nil))
  ( [z cl]
    (if (nil? cl) (java.lang.Class/forName z) (java.lang.Class/forName z true cl))))

(defn getCZldr
  ""
  ([] (getCZldr nil))
  ([cl] (if (nil? cl) (.getContextClassLoader (Thread/currentThread)) cl)))

(defn setCZldr
  ""
  [cl]
  (let []
    (CU/tstObjArg "class-loader" cl)
    (.setContextClassLoader (Thread/currentThread) cl)))

(defn loadCZ
  ""
  ( [clazzName] (loadCZ clazzName nil))
  ( [clazzName cl]
    (if (not (SU/hgl clazzName)) nil (.loadClass (getCZldr cl) clazzName))))

(defn ctorObj
  ""
  [cz]
  (let []
    (CU/tstObjArg "java-class" cz)
    (.newInstance (.getDeclaredConstructor cz (make-array Class 0))  (make-array Object 0)  )))

(defn mkObj
  ""
  ([clazzName] (mkObj clazzName nil))
  ([clazzName cl]
   (if (not (SU/hgl clazzName)) nil (ctorObj (loadCZ clazzName cl)))))

(defn listParents
  ""
  [cz]
  (let [ rc (loop [ bin [] par cz ]
              (if (nil? par)
                bin
                (recur (conj bin par) (.getSuperclass par))))  ]
    ;; since we always add the original class, we need to ignore it on return
    (if (> (.size rc) 1) (rest rc) [] )))


(defn- iterXXX
  ""
  [ cz level getDeclXXX bin ]
  (let [ props (getDeclXXX cz) ]
    (reduce (fn [sum m]
              (let [ x (.getModifiers m) ]
                (if (and (> level 0)
                         (or (Modifier/isStatic x) (Modifier/isPrivate x)) )
                  sum
                  (assoc sum (.getName m) m)))) bin props) ))

(defn- listMtds [ cz level ]
  (let [ par (.getSuperclass cz) ]
    (iterXXX cz level #(.getDeclaredMethods %) (if (nil? par) {} (listMtds par (inc level))))))

(defn- listFlds [ cz level ]
  (let [ par (.getSuperclass cz) ]
    (iterXXX cz level #(.getDeclaredFields %) (if (nil? par) {} (listFlds par (inc level))))))

(defn listMethods
  "List all methods belonging to this class, including inherited ones."
  [cz]
  (vals (if (nil? cz) {} (listMtds cz 0 ))))

(defn listFields
  "List all fields belonging to this class, including inherited ones."
  [cz]
  (vals (if (nil? cz) {} (listFlds cz 0 ))))























(def ^:private metautils-eof nil)

