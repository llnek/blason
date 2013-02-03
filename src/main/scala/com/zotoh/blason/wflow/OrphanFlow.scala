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

import com.zotoh.blason.io.HTTPResult
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.blason.kernel.Job
import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.blason.io.AbstractEvent

/**
 * Deal with jobs which are not handled by any processor.
 * (Internal use only).
 *
 *
 * @author kenl
 */
sealed class OrphanFlow(j:Job) extends Pipeline(j) {

  override protected def onStart() = {

    new PTask withWork new Work {
      def eval(j:Job , arg:Any*) {
        j.event() match {
          case ev:HTTPEvent => handle(ev)
          case e:AbstractEvent => throw new FlowError("Unhandled event-type \"" + e.getClass + "\".")
        }
      }
    }
  }

  private def handle(ev:HTTPEvent ) {
    val res= new HTTPResult()
    res.setStatus(HTTPStatus.NOT_IMPLEMENTED)
//        res.setErrorMsg("Service not implemented");
    ev.setResult(res)
  }


}

