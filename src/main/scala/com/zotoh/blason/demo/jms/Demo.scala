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

package demo.jms

import javax.jms.{TextMessage,Message}
import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author kenl
 */
class DemoMain(c:Container) {
  def start() {    
          println("Demo receiving JMS messages..." )
  }
  def stop() {    
  }
  def dispose() {    
  }
}


/**
 * @author kenl
 *
 */
object Demo {
  private var _count= new AtomicInteger(0)
  def count() = _count.incrementAndGet() 
}

class Demo(job:Job) extends Pipeline(job) {
  import Demo._
  
  override def onStart() = new PTask withWork new Work {
    def eval(job:Job,arg:Any*) {

      val ev= job.event().asInstanceOf[JMSEvent]
      val msg= ev.getMsg()

      println("-> Correlation ID= " + msg.getJMSCorrelationID())
      println("-> Msg ID= " + msg.getJMSMessageID())
      println("-> Type= " + msg.getJMSType())

      msg match {
        case t:TextMessage => println("("+count+") -> Text Message= " + t.getText())
        case _ =>
      }

    }} 

}

