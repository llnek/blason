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

package demo.timer

import java.util.concurrent.atomic.AtomicInteger
import java.util.{Date=>JDate}

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._

/**
 * @author kenl
 *
 */
class DemoMain {
  def start() {    
    println("Demo timer functions..." )
  }
  def stop() {    
  }
  def dispose() {    
  }
}



object Demo {
  private val _count= new AtomicInteger(0)
  def count() = _count.incrementAndGet()
}

class Demo(job:Job) extends Pipeline(job) {
import Demo._
  override def onStart() = new PTask withWork  new Work {
      def eval(job:Job,arg:Any*) {
        val ev= job.event.asInstanceOf[TimerEvent]
        if ( ev.isRepeating ) {
          println("-----> (" + count +  ") repeating-update: " + new JDate())
        } else {
          println("-----> once-only!!: " + new JDate())
        }
      }
    }

}




