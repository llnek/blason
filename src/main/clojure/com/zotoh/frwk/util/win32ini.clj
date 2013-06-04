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

(ns com.zotoh.frwk.util.win32)

(defprotocol IWin32Conf)

(defn- throwBadIni [rdr]
  (throw (IOException.  (str "Bad ini line: " (.getLineNumber rdr)))))

(defn- ciFind [ m k ]
  (keys m)

(defn- pSection [rdr line]
  (let [ s (StringUtils/trim (StringUtils/strip line "[]")) ]
    (when (StringUtils/isEmpty s) (throwBadIni rdr))


            ncFind(secs, s) match {
              case Some(x) => kvs=x
              case None =>
                kvs= new Section()
                secs += Tuple2(s,kvs)
            }
(defn- f1 [line]
  (let [ ln (.trim line) ]
    (cond

      (or (StringUtils/isEmpty ln) (.startsWith ln "#")) 
      nil

      (.matches ln "^\\[.*\\]$") (pSection ln)
      (do

     )

      () ""

      :else "" )

(defn- ctor
  "Parses the ini file."
  [iniFilePath]
  (let [ rdr (LineNumberReader. (FileReader. iniFilePath)) ]
    (loop [ line (.readLine rdr) ]
      (if (nil? line)
        (break)
        (let [ ln (.trim line) ]
          (cond
            () ""
            () ""
            :else "" )


    secs mutable.LinkedHashMap[String,Section]()
    kvs:Section= null

        ]
    val ex= () =>
      throw new IOException("Bad INI line: " + rdr.getLineNumber)
    var line=""
    var s=""
    do {
      line = rdr.readLine
      if (line != null) {
        line.trim match {
          case ln if STU.isEmpty(ln) || ln.startsWith("#") =>
          case ln if ln.matches("^\\[.*\\]$") =>
            s = STU.trim( STU.strip(ln, "[]"))
            if ( STU.isEmpty(s)) { ex }
            ncFind(secs, s) match {
              case Some(x) => kvs=x
              case None =>
                kvs= new Section()
                secs += Tuple2(s,kvs)
            }
          case ln if (kvs != null) =>
            var pos=ln.indexOf('=')
            if (pos>0) {
              s= ln.substring(0, pos).trim
              if ( STU.isEmpty(s)) { ex}
              kvs += Tuple2(s, ln.substring(pos + 1).trim )
            } else {
              ex
            }
        }
      }
    } while (line != null)
    _secs = secs

(defrecord Win32Conf []
  IWin32Conf
  ()


)
  

  


