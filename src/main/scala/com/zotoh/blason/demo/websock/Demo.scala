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
  

package demo.websock

import com.zotoh.blason.wflow.PTask
import com.zotoh.blason.io.WebSockEvent
import com.zotoh.frwk.util.CoreUtils
import com.zotoh.blason.io.WebSockResult
import com.zotoh.blason.wflow.Work
import com.zotoh.blason.wflow.Pipeline
import com.zotoh.blason.kernel.Job
import com.zotoh.blason.kernel.Container

/**
 * @author kenl
 *
 */
class DemoMain(c:Container) {
  def start() {
    println("Demo Websockets.")
  }
  def stop() {
  }
  def dispose() {
  }
}


class Demo(job:Job) extends Pipeline(job) {

    val task1= new Work() {
        override def eval(job:Job, arg:Any*) {
            val ev= job.event.asInstanceOf[WebSockEvent]
            var msg= ev.data.stringify()
            var i= CoreUtils.asInt(msg, 0)
            val rc = 1L * i*i
            msg= rc.toString
            val res= new WebSockResult()
            res.setData(msg)
            ev.setResult(res)
        }
    }

    def onStart() = new PTask(task1 )
}

