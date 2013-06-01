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

(ns com.zotoh.frwk.util.byteutils 
  (:import (java.nio ByteBuffer CharBuffer) )
  (:import (java.nio.charset.Charset) )
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream DataOutputStream DataInputStream) )
  )

;;
;; Utililties for handling byte[] conversions to/from numbers.
;;
;; @author kenl
;;
;;

(defn ^{
    :doc "Convert char[] to byte[]."
  }
  toByteArray [ chArray ^Charset encoding ]
    (.array (.encode encoding (CharBuffer/wrap chArray)) ) )

(defn ^{
    :doc "Convert byte[] to char[]."
  }
  toCharArray [ byteArray ^Charset encoding ]
    (.array (.decode encoding (ByteBuffer/wrap byteArray)) ) )

(defn ^{
    :doc "Return a long by scanning the byte[]."
  }
  readLong [ byteArray ]
    (.readLong (DataInputStream. (ByteArrayInputStream. byteArray)) ))

(defn ^{
    :doc "Return an int by scanning the byte[]."
  }
  readInt [ byteArray ]
    (.readInt (DataInputStream. (ByteArrayInputStream. byteArray)) ))

(defmulti readBytes class)

(defmethod ^{
    :doc "Convert the long into byte[]."
  }
  readBytes Long [ num ]
    (with-open [ baos (ByteArrayOutputStream. 4096) ]
      (let [ ds (DataOutputStream. baos ) ] 
        (.writeLong ds num)
        (.flush ds )
        (.toByteArray baos ) )))

(defmethod ^{
    :doc "Convert the int into byte[]."
  }
  readBytes Integer [ num ]
    (with-open [ baos (ByteArrayOutputStream. 4096) ]
      (let [ ds (DataOutputStream. baos ) ] 
        (.writeInt ds num)
        (.flush ds )
        (.toByteArray baos ) )))


(def ^:private byteutils-eof nil)

