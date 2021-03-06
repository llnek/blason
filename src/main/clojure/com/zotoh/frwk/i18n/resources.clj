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

(ns com.zotoh.frwk.i18n.resources 
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (java.util ResourceBundle Locale))
  (:require [ com.zotoh.frwk.util.metautils :as MU])
  (:require [ com.zotoh.frwk.util.strutils :as SU])
  )


(defn getString
  "Return the string value for this key, pms may contain values for positional substitutions."
  [ bundle pkey pms ]
  (let [ kv (SU/nsb (.getString bundle pkey)) ]
    (if (empty? pms)
      kv
      (loop [ src kv pos 0 ]
        (if (>= pos (.size pms))
          src
          (recur (StringUtils/replace src "{}" (SU/nsb (nth pms pos)) 1) (inc pos)))))))


(defn getResourceBundle
  "Return a resource bundle."
  [ baseName locale cl ]
  (let []
    (if (or (nil? baseName)(nil? locale))
      nil
      (ResourceBundle/getBundle baseName locale (MU/getCZldr cl))) ))


(def ^:private resources-eof nil)

