/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package com.zotoh.blason
package io


import scala.collection.mutable
import java.io.IOException
import org.slf4j._
import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.continuation.Continuation
import org.eclipse.jetty.continuation.ContinuationSupport
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.blason.core.Loggable

object WEBServlet {
  val WEBSERVLET_DEVID= "_#jetty.emitter#_"
}

/**
 * @author kenl
 *
 */
@SerialVersionUID(-3862652820921092885L) class WEBServlet(private var _src:BaseHttpIO ) extends HttpServlet
with CoreImplicits with Serializable with Loggable {

  private var _log = LoggerFactory.getLogger(classOf[WEBServlet])
  private var _jettyAsync=false
  def tlog() = _log

  def this() {
    this(null)
  }
  
  override def destroy() {
    tlog().debug("WEBServlet: destroy()")
  }

  override def service(request:ServletRequest, response:ServletResponse) {
    val rsp= response.asInstanceOf[HttpServletResponse]
    val req= request.asInstanceOf[HttpServletRequest]

    tlog().debug("{}\n{}\n{}",
    "********************************************************************",
      req.getRequestURL(),
      "********************************************************************")

    val evt= HTTPHplr.extract( _src, req)
    if (_jettyAsync) {
      doASyncSvc(evt, req, rsp)
    } else {
      doSyncSvc(evt, req,rsp)
    }
  }

  private def doASyncSvc(evt:HTTPEvent, req:HttpServletRequest, rsp:HttpServletResponse) {
    val c = ContinuationSupport.getContinuation(req)
    if (c.isInitial ) try {
      dispREQ(c, evt, req,rsp)
    } catch {
      case e:Throwable => tlog().error("",e)
    }
  }

  private def doSyncSvc(evt:HTTPEvent, req:HttpServletRequest, rsp:HttpServletResponse) {
    val w= new SyncWaitEvent( evt )
    val ev = w.inner()

    _src.hold(w)
    _src.dispatch(ev)

    try {
      w.timeoutMillis(  _src.waitMillis )
    } finally {
      _src.release(w)
    }

    w.inner.result match {
      case Some(res) =>
        replyService( res.asInstanceOf[HTTPResult] , rsp)
      case _ =>
        replyService( new HTTPResult(HTTPStatus.REQUEST_TIMEOUT), rsp)
    }

  }

  protected def replyService(res:HTTPResult, rsp:HttpServletResponse) {
    val sc= res.statusCode()
    val hdrs= res.headers()
    val data  = res.data()
    var clen=0L

    try {
      hdrs.foreach { (t) =>
        if ( !"content-length".eqic(t._1)) {
          rsp.setHeader(t._1,t._2)
        }
      }
      if (res.hasError ) {
        rsp.sendError(sc, res.errorMsg )
      } else {
        rsp.setStatus(sc)
      }
      data match {
        case Some(d) if (d.hasContent ) =>
          clen=d.size()
          copy( d.stream, rsp.getOutputStream, clen)
        case _ => /* noop */
      }

      rsp.setContentLength( clen.toInt)

    } catch {
      case e:Throwable => tlog().warn("",e)
    }
  }

  private def dispREQ(ct:Continuation, evt:HTTPEvent, req:HttpServletRequest, rsp:HttpServletResponse) {

    ct.setTimeout(_src.waitMillis )
    ct.suspend(rsp)

    val w= new AsyncWaitEvent(evt, new AsyncServletTrigger( req, rsp, _src) )
    val ev = w.inner()

    w.timeoutMillis(_src.waitMillis )
    _src.hold(w)
    _src.dispatch(ev)

  }

  override def init(config:ServletConfig) {
    super.init(config)

    val ctx= config.getServletContext()
    ctx.getAttribute( WEBServlet.WEBSERVLET_DEVID) match {
      case x:BaseHttpIO => _src =x
      case _ =>
    }

    block { () =>
      val z= loadClass("org.eclipse.jetty.continuation.ContinuationSupport")
      if (z != null) { _jettyAsync= true }
    }

    block { () =>
      tlog().debug("{}\n{}{}\n{}\n{}{}",
        "********************************************************************",
        "Servlet Container: ",
        ctx.getServerInfo(),
        "********************************************************************",
        "Servlet:iniz() - servlet:" ,
        getServletName())
    }

  }

}
