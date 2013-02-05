package @@APPDOMAIN@@

import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.frwk.io.XData

import com.zotoh.blason.core.Constants
import com.zotoh.blason.kernel.Job
import com.zotoh.blason.mvc.RouteInfo
import com.zotoh.blason.wflow._
import com.zotoh.blason.io.HTTPResult
import com.zotoh.blason.io.HTTPEvent

class Handler(job:Job) extends Pipeline(job) with Constants {

  override def onStart() = new PTask withWork new Work {

    def eval(job:Job,arg:Any*) {
      val evt= job.event.asInstanceOf[HTTPEvent]
      val src=evt.emitter
      val c= src.container
      val res=new HTTPResult()
      val rc= evt.attr(PF_ROUTE_INFO) match {
        case Some(ri:RouteInfo) =>
          c.processTemplate(ri, c.getAppCfg.asJHM)
        case _ =>
          ( new XData("<html><h1>Hello World!</h1></html>"), "text/html")
      }
      res.setHeader("content-type", rc._2)
      res.setStatus(HTTPStatus.OK)
      res.setData(rc._1)
      evt.setResult(res)
      //println("***********************************************")
      //println("               Handled one job.")
      //println("***********************************************")
    }

  }

}


