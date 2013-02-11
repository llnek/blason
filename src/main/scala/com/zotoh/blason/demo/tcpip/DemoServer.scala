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

package demo.tcpip

import java.io.BufferedInputStream
import java.io.InputStream

import com.zotoh.frwk.net.NetUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.ByteUtils._

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._


/**
 * @author kenl
 *
 */
class DemoMain(c:Container) {
  def start() {    
    println("Demo sending & receiving messages via sockets..." )    
  }
  def stop() {    
  }
  def dispose() {    
  }
}

class Demo(job:Job) extends Pipeline(job) {
  private var _clientMsg=""

  val task1= new Work {
      def eval(job:Job,arg:Any*) {
          val ev= job.event.asInstanceOf[SocketEvent]
          val sockBin = { (ev:SocketEvent)  =>
            val bf= new BufferedInputStream( ev.getSockIn )
            var buf= new Array[Byte](4)
            var clen=0
            bf.read(buf)
            clen= readAsInt(buf)
            buf= new Array[Byte](clen)
            bf.read(buf)
            _clientMsg=new String(buf,"utf-8")
          }
          sockBin(ev)
          // add a delay into the workflow before next step
          setResult( new Delay(1500))
      }
  }

  val task2= new Work {
      def eval(job:Job,arg:Any*) {
        println("Socket Server Received: " + _clientMsg )
      }
  }

  override def onStart() = new PTask(task1) chain new PTask(task2)

}

