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

package demo.async

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._


/**
 * @author kenl
 *
 */
class DemoMain(c:Container) {
  def start() {    
          println("Demo calling an async java-api & resuming.")
  }
  def stop() {    
  }
  def dispose() {    
  }
}

class Demo(job:Job) extends Pipeline(job) {

  val task1= new Work {
      def eval(job:Job,arg:Any*) {
          val t= new AsyncResumeToken( curStep )

          println("/* Calling a mock-webservice which takes a long time (10secs),")
          println("- since the call is *async*, event loop is not blocked.")
          println("- When we get a *call-back*, the normal processing will continue */")

          DemoAsyncWS.doLongAsyncCall(new AsyncCallback() {
              override def onSuccess(result:Option[Any]) {
                  println("CB: Got WS callback: onSuccess")
                  println("CB: Tell the scheduler to re-schedule the original process")
                  // use the token to tell framework to restart the idled process
                  t.resume(result)
              }
              override def onError(e:Exception) {
                  t.resume(Some(e))
              }
              override def onTimeout() {
                  onError( new Exception("time out"))
              }
          })

          println("\n\n")
          println("+ Just called the webservice, the process will be *idle* until")
          println("+ the websevice is done.")
          println("\n\n")

          setResult( new AsyncWait )
      }
  }

  val task2= new Work {
      def eval(j:Job, arg:Any*)  {
          println("-> The result from WS is: " + arg(0))
      }
      def eval(j:Job) {}
  }

  override def onStart() = new PTask(task1) chain new PTask(task2)


}

