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
package impl

import com.zotoh.blason.wflow.PTask
import com.zotoh.blason.kernel.Job
import com.zotoh.blason.wflow.Pipeline
import com.zotoh.blason.io.HTTPResult
import com.zotoh.blason.wflow.Work
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.frwk.net.HTTPStatus

/**
 * @author kenl
 */
class ShutdownHandler(job:Job) extends Pipeline(job) {
// no used
  def onStart() = 
    new PTask( new Work() { def eval(j:Job,args:Any*) {
      val evt= j.event.asInstanceOf[HTTPEvent]
      val res= new HTTPResult()
      res.setStatus(HTTPStatus.OK)
      evt.setResult(res)
    }
  })
}
