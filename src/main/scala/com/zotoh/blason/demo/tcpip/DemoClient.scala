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

import java.io.OutputStream
import java.net.Socket
import java.util.{Date=>JDate}
import com.zotoh.frwk.net.NetUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.ByteUtils._

import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._
import org.apache.commons.lang3.{StringUtils=>STU}

/**
 * @author kenl
 *
 */
class DemoClient(job:Job) extends Pipeline(job) {
  private val _textMsg= "Hello World, time is ${TS} !"

  override def onStart() = {
    val me=this
    new Delay(2000) chain( new PTask withWork new Work {
          // opens a socket and write something back to parent process
      def eval(job:Job,arg:Any*) {
          me.container().getService("default-sample") match {
            case Some(tcp:SocketIO) =>
              val s= STU.replace(_textMsg,"${TS}", new JDate().toString )
              println("TCP Client: about to send message" + s )
              val bits= asBytes(s)
              val port= tcp.port()
              val host=tcp.host()
              using( new Socket( hostByName(host), port)) { (soc) =>
                val os= soc.getOutputStream()
                os.write( readAsBytes(bits.length))
                os.write(bits)
                os.flush()
              }
            case _ =>
          }
      }} )

  }

}

