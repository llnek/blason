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

import com.zotoh.blason.kernel.Job
import com.zotoh.blason.wflow.PTask
import com.zotoh.blason.wflow.Pipeline
import com.zotoh.blason.wflow.Work
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.blason.io.WebSocketClientFactory
import java.net.URI
import com.zotoh.blason.io.WebSocketClientCB
import com.zotoh.blason.io.WebSocketClient
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.zotoh.blason.wflow.While
import com.zotoh.blason.wflow.BoolExpr
import com.zotoh.blason.wflow.Group
import com.zotoh.blason.wflow.Delay
import com.zotoh.frwk.util.ProcessUtils._
import com.zotoh.blason.wflow.AsyncResumeToken
import com.zotoh.blason.wflow.AsyncWait
import com.zotoh.blason.io.NettyIO

/**
 * @author kenl
 *
 */
class DemoClient(job:Job) extends Pipeline(job) {
  private var _wc:WebSocketClient= null
  private var _ctr= this.container
  
  val task0 = new Work() {
    def eval(j:Job,arg:Any*) = {
      val t= new AsyncResumeToken( curStep )
      val url= _ctr.getService( "default-sample") match {
        case Some(x:NettyIO) =>"ws://" + x.host() + ":" + x.port() + "/squarenum"
        case _ =>""
      }
      _wc= WebSocketClientFactory.newClient( new URI( url ), 
            new WebSocketClientCB() {
              override def onFrame(c:WebSocketClient, frame:WebSocketFrame) {
                  println( "Client got Message: result: " + frame.asInstanceOf[TextWebSocketFrame].getText )
              }
              override def onError(c:WebSocketClient, t:Throwable) {
                tlog.error("",t)
              }
              override def onDisconnect(c:WebSocketClient ) { }
              override def onConnect(c:WebSocketClient ) { 
                t.resume(Some("OK"))
              }
          })
          _wc.start()       
          setResult( new AsyncWait() )
    }
  }
    val task1= new Work() {
        override def eval(job:Job, arg:Any*) {
          val  n = newRandom.nextInt( 1000 )
          println( "Client send Message: square this number: " + n )
          _wc.send(new TextWebSocketFrame( n.toString ))
        }
    }
    
    // forever while-loop sending req to server
    
    def onStart() =  new PTask(task0) chain new While().withBody(    
        new PTask(task1)  chain new Delay(3000) ).withExpr(new BoolExpr(){
      def eval(j:Job)=true
    })
    
    
}
