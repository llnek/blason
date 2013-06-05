(ns com.zotoh.frwk.util.cmdlineseq
  )


(defn- readData
  ""
  [console]
  (let [ b (StringBuilder.) ]
    (loop [ c (.read console) ]
      (cond
        (or (= c -1)(= c 4)) { :cancel true }
        (= c (int \n)) {}
        (or (= c (int \r) (= c (int \b) (= c 27)) {}


      (if (c== -1 || c==4) { esc=true; loop=false }
      (if (c== '\n') { loop=false }
      (if (c=='\r' || c== '\b'|| c==27 /*esc*/) { /* continue */ }

    var b=new StringBuilder()
    var loop=true
    var esc=false
    var c=0

    // windows has '\r\n'
    // linux has '\n'
    while(loop) {
      c=  _in.read()
      if (c== -1 || c==4) { esc=true; loop=false }
      if (c== '\n') { loop=false }
      if (c=='\r' || c== '\b'|| c==27 /*esc*/) { /* continue */ }
      else if (loop) { b.append(c.toChar) }
    }

    if (esc) {
      _canceled=true ; b.setLength(0)
    }

    STU.trim( b.toString )
  }

(start 
    (let [ kp (if (CU/isWindows) "<Ctrl-C>" "<Ctrl-D>" ]
      (if (nil? parSeq)
        (println  (str ">>> Press " kp "<Enter> to cancel..."))
        (if (.isCanceled (.start parSeq props))
          (do 
            (finito)


  def start(props:JPS) : this.type = {
    val kp= if (isWindows()) "<Ctrl-C>" else "<Ctrl-D>"
    var stop= false
    if (_par == null) {
//        println( ">>> Press <Ctrl-D><Enter> to cancel...")
        println(  ">>> Press "+kp+"<Enter> to cancel...")
    } else if ( _par.start(props).isCanceled) {
        _canceled=true
        end()
        stop=true
    }
    if (!stop) {
      _Qs.get(onStart) match {
        case Some(c) => cycle(c, props)
        case _ => end()
      }
    }
    this
  }


