/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.io.XData
import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.frwk.io.IOUtils._

import javax.servlet.http.{HttpServletResponse=>HSResponse}
import javax.servlet.http.{HttpServletRequest=>HSRequest}

import org.eclipse.jetty.continuation.ContinuationSupport
import org.eclipse.jetty.continuation.Continuation


/**
 * @author kenl
 */
class AsyncServletTrigger( private var _req:HSRequest, private var _rsp:HSResponse,
  src:EventEmitter) extends AsyncTrigger(src) with CoreImplicits {

  override def resumeWithResult(result:AbstractResult) {
    val res= result.asInstanceOf[HTTPResult]
    var cl= 0L
    val hdrs= res.headers()
    try {
      hdrs.foreach { (t) =>
        if ( "content-length".eqic(t._1)) {} else {
          _rsp.setHeader(t._1, t._2)
        }
      }
      if (res.hasError ) {
        _rsp.sendError(res.statusCode , res.errorMsg )
        cl= -1L
      } else {
        _rsp.setStatus(res.statusCode )
      }
      if (cl == 0L ) res.data match {
        case Some(d) =>
          if (d.hasContent ) {
            cl=d.size
            copy(d.stream, _rsp.getOutputStream, cl )
            _rsp.setContentLength( cl.toInt)
          } else {
            _rsp.setContentLength(0)
          }
        case _ => /* noop */
      }
    } catch {
      case e:Throwable => tlog().error("",e)
    } finally {
      getCont().complete()
    }

  }

  override def resumeWithError() {
    val s= HTTPStatus.INTERNAL_SERVER_ERROR
    try {
      _rsp.sendError(s.code, s.reasonPhrase )
    } catch {
      case e:Throwable => tlog().error("",e)
    } finally {
      getCont().complete()
    }
  }

  private def getCont() = {
    ContinuationSupport.getContinuation(_req)
  }

}

