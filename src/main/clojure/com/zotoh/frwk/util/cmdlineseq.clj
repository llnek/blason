(ns com.zotoh.frwk.util.cmdlineseq
  (:import (java.io BufferedOutputStream InputStreamReader OutputStreamWriter))
  (:import (java.io Reader Writer))
  (:import (java.util Properties))
  (:import (org.apache.commons.lang3 StringUtils))
  (:require [ com.zotoh.frwk.util.coreutils :as CU])
  (:require [ com.zotoh.frwk.util.strutils :as SU])
  )

(defrecord CmdSeqQ [ qid qline choices dft must onok ])

(defn- readData [cout cin]
  (let [ buf (StringBuilder.)
         ms (loop [ c (.read cin) ]
                  ;; windows has '\r\n' linux has '\n'
                  (let [ m (cond
                              (or (= c -1)(= c 4))  #{ :quit :break }
                              (= c (int \newline)) #{ :break }
                              (or (= c (int \return)) (= c (int \backspace)) (= c 27)) #{}
                              :else (do (.append buf (char c)) #{})) ]
                    (if (clojure.core/contains? m :break)
                      m
                      (recur (.read cin))))) ]
    (if (clojure.core/contains? ms :quit) nil (.trim (.toString buf)))))

(defn- popQQ [cout cin cmdQ props]
  (let [ must (:must cmdQ)
         dft (:dft cmdQ)
         onResp (:onok cmdQ)
         q (:qline cmdQ)
         chs (:choices cmdQ) ]
    (.write cout (str q (if must "*" "" ) " ? "))
    (if-not (StringUtils/isEmpty chs)
      (if (SU/contains? chs \n)
        (do (.write cout (str
              (if (.startsWith chs "\n") "[" "[\n")  chs 
              (if (.endsWith chs "\n") "]" "\n]" ) )))
        (do (.write cout (str "[" chs "]")))))
    (if-not (StringUtils/isEmpty dft)
      (.write cout (str "(" dft ")")) )
    (.write cout " ")
    (.flush cout)
    ;; get the input from user
    ;; point to next question, blank ends it
    (let [ rc (readData cout cin)]
      (if (nil? rc)
        (do (.write cout "\n") nil )
        (do (onResp (if (StringUtils/isEmpty rc) dft rc) props))))))

(defn- popQ [cout cin cmdQ props]
  (if (nil? cmdQ)
    ""
    (popQQ cout cin cmdQ props)))


(defn- cycleQ [cout cin cmdQNs start props]
  (let []
    (loop [ rc (popQ cout cin (get cmdQNs start) props) ]
      (cond
        (StringUtils/isEmpty rc) props
        (nil? rc) nil
        :else (recur (popQ cout cin (get cmdQNs rc) props))))))

(defn start [cmdQs q1]
  (let [ kp (if (CU/isWindows) "<Ctrl-C>" "<Ctrl-D>")
         cout (OutputStreamWriter. (BufferedOutputStream. (System/out)))
         cin (InputStreamReader. (System/in))
         props (Properties.) ]
    (.write cout (str ">>> Press " kp "<Enter> to cancel...\n"))
    (cycleQ cout cin cmdQs q1 props)))


(comment
(def q1 (->CmdSeqQ "q1" "hello ken" "q|b|c" "c" true 
           (fn [a ps]
             (do (.put ps "a1" a) "q2")) ) )
(def q2 (->CmdSeqQ "q2" "hello paul" "" "" false 
           (fn [a ps]
             (do (.put ps "a2" a) "q3"))) )
(def q3 (->CmdSeqQ "q3" "hello joe" "z" "" false 
           (fn [a ps]
             (do (.put ps "a3" a) "" ))) )
(def QM { "q1" q1 "q2" q2 "q3" q3 })
)

(def ^:private cmdlineseq-eof nil)

