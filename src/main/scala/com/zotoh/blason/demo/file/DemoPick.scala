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

import java.io.File
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.util.CoreUtils._

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.io.{FileUtils=>FUT}



/**
 * @author kenl
 *
 */
class Demo(job:Job) extends Pipeline(job) {

  override def onStart() = new PTask withWork new Work {
      def eval(job:Job,arg:Any*) {
          val ev= job.event().asInstanceOf[FILEEvent]
          val f0= ev.origFilePath()
          val f=ev.file()
          ev.action match {
            case FILEAction.FP_DELETED =>
            case _ =>
              println("Picked up new file: " + f0)
              println("Read new file: " + niceFPath(f))
              println("Content: " + readText(f, "utf-8"))
              FUT.deleteQuietly(f)
          }
      }
  }


}


