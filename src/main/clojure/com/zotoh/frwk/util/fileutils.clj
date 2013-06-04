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

(ns com.zotoh.frwk.util.fileutils
  (:import (java.io File FileInputStream FileOutputStream InputStream OutputStream ))
  (:import (java.util ArrayList))
  (:import (org.apache.commons.io FileUtils))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.io IOUtils))
  (:import (java.util.zip ZipFile ZipEntry))
  (:require [ com.zotoh.frwk.util.coreutils :as CU ] )
  )

;;
;; General file related utilities.
;;
;; @author kenl
;;
;;

(defn- jiggleZipEntryName [ en ]
  (let []
    (println en)
    (.replaceAll (.getName en) "^[\\/]+","")))

(defn isFileWR?
  "Returns true if file is readable & writable."
  [fp]
  (let []
    (if (and (not (nil? fp)) (.exists fp) (.isFile fp) (.canRead fp) (.canWrite fp))
      true
      false)))

(defn isFileR?
  "Returns true if file is readable."
  [fp]
  (let []
    (if (and (not (nil? fp)) (.exists fp) (.isFile fp) (.canRead fp))
      true
      false)))

(defn isDirWR?
  "Returns true if directory is readable and writable."
  [dir]
  (let []
    (if (and (not (nil? dir)) (.exists dir) (.isDirectory dir) (.canRead dir) (.canWrite dir) )
      true
      false)))

(defn isDirR?
  "Returns true if directory is readable."
  [dir]
  (let []
    (if (and (not (nil? dir)) (.exists dir) (.isDirectory dir) (.canRead dir) )
      true
      false)))

(defn canExec?
  "Returns true if file or directory is executable."
  [fp]
  (let []
    (if (and (not (nil? fp)) (.exists fp) (.canExecute fp))
      true
      false)))

(defn getCWD
  "Get the current directory."
  []
  (let [fp (File. (CU/sysQuirk "user.dir")) ]
    fp))

(defn parentPath
  "Get the path to the parent directory."
  [path]
  (if (StringUtils/isEmpty path) 
    path
    (.getParent (File. path))))

(defn explodeZipFile
  "Unzip contents of zip file to a target folder."  
  [ ^File src ^File des ]
  (let [ ents (.entries (ZipFile. src)) 
         dummy (.mkdirs des) ]
    (doseq [ en (list ents) ]
      (let [ f (File. des (jiggleZipEntryName en) ) ]
        (if (.isDirectory en)
          (.mkdirs f)
          (do
            (.mkdirs (.getParentFile f))
            (with-open [ inp (.getInputStream src en) ]
              (with-open [ os (FileOutputStream. f) ]
                (IOUtils/copy inp os)))))))))










(def ^:private fileutils-eof nil)

