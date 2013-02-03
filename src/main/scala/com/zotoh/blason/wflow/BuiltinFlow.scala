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
package wflow

import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.StrArr
import com.zotoh.blason.kernel.Job
import com.zotoh.blason.core.Constants

import com.zotoh.blason.io.{HTTPResult,HTTPEvent}
import com.zotoh.frwk.net.HTTPStatus

/**
 * Handles internal system events.
 * (Internal use only)
 *
 * @author kenl
 */
class BuiltinFlow(j:Job) extends Pipeline(j) with Constants {

  override def onStart() = {
    val me=this
    val t3= new PTask withWork new Work {
      def eval(j:Job , arg:Any*) {
        me.eval_shutdown(j)
      }
    }
    val t2= new Delay(3000L)
    val t1= new PTask withWork new Work {
      def eval(j:Job , arg:Any*) {
        me.do_shutdown(j)
      }
    }

    val t= new BoolExpr(){
      def eval(j:Job ) = {
        SHUTDOWN_DEVID == j.event().emitter().name()
    }}

    new If(t, t3.chain(t2).chain(t1))
  }

  private def do_shutdown(j:Job) {
    j.container().dispose()
  }

  private def eval_shutdown(j:Job ) {
    val ev= j.event().asInstanceOf[HTTPEvent]
    var a=ev.param("pwd").getOrElse(null)
    val res= new HTTPResult()
    val ignore=false
    var w= ""

    if (a==null) {
      a= ev.param("password").getOrElse(null)
    }

    if (a != null) { w= nsb(a.first()) }

    //TODO: check shutdown
    //if ( ! j.container().scheduler().verifyShutdown(ev.getUri(), w)) { ignore=true }

    if (ignore) {
      tlog().warn("ShutdownTask: wrong password or uri, ignoring shutdown request.")
      res.setStatus(HTTPStatus.FORBIDDEN)
    } else {
      res.setStatus(HTTPStatus.OK)
    }
    ev.setResult(res)

    if (!ignore) {
      // do something ?
    }
  }

}
