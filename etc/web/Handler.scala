package @@APPDOMAIN@@

import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.frwk.io.XData

import com.zotoh.blason.kernel.Job
import com.zotoh.blason.wflow._
import com.zotoh.blason.io.HTTPResult
import com.zotoh.blason.io.HTTPEvent

class Handler(job:Job) extends Pipeline(job) {

  override def onStart() = new PTask withWork new Work {
    def eval(job:Job,arg:Any*) {
      job.event match {
        case ev:HTTPEvent =>
          val res=new HTTPResult()
          res.setData(new XData("<html><h1>Hello World!</h1></html>"))
          res.setStatus(HTTPStatus.OK)
          ev.setResult(res)
        case _ =>
      }
      println("***********************************************")
      println("               Handled one job.")
      println("***********************************************")
    }
  }

}


