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

package demo.file

import java.util.{Date=>JDate}
import java.io.File
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author kenl
 */
class DemoMain(c:Container) {
  def start() {    
      println("Demo file directory monitoring - picking up new files")
  }
  def stop() {    
  }
  def dispose() {    
  }
}

object DemoGen {
  private val _count= new AtomicInteger(0)
  def count() = _count.incrementAndGet()
}

/**
 * @author kenl
 * Create a new file every n secs
 *
 */
class DemoGen(job:Job) extends Pipeline(job) {
  import DemoGen._
  override def onStart() = {
    val me=this
    new PTask withWork  new Work {
      def eval(job:Job,arg:Any*) {
          val s= "Current time is " + fmtDate(new JDate )
          me.container().getService("default-sample") match {
            case Some(p:FILEPicker) =>
              writeFile( new File(p.srcDir, "ts-"+ count +".txt"), s, "utf-8")
            case _ =>
          }
      }
  }}

}

