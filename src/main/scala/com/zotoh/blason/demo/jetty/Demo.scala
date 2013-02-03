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

package demo.jetty

import java.text.SimpleDateFormat
import java.util.{Date=>JDate}
import com.zotoh.frwk.net.HTTPStatus
import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io.HTTPResult
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.frwk.io.XData

/**
 * @author kenl
 *
 */
class DemoMain {
  def start() {
    println("Point your browser to http://localhost:8081/test/helloworld")
  }
  def stop() {
  }
  def dispose() {
  }
}

class Demo(job:Job) extends Pipeline(job) {
  private def fmtHtml() = """            
      <html><head> 
      <title>BLASON: Test Jetty Servlet</title>
      <link rel="shortcut icon" href="public/images/favicon.ico"/>
      <link type="text/css" rel="stylesheet" href="public/styles/main.css"/>
      <script type="text/javascript" src="public/scripts/test.js"></script>
      </head>
      <body><h1>Bonjour!</h1><br/>
      <button type="button" onclick="pop();">Click Me!</button>
      </body></html>
  """

  val task1= new Work {
    def eval(job:Job, arg:Any*) {
        val ev= job.event.asInstanceOf[HTTPEvent]
        val res= new HTTPResult()
        /*
        val text= <html>
        <h1>The current date-time is:</h1>
        <p>
          { new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss.SSSZ").format( new JDate() ) }
        </p>
        </html>.buildString(false)
*/
        // construct a simple html page back to caller
        // by wrapping it into a stream data object
        res.setData(new XData( fmtHtml.getBytes("utf-8") ) )
        res.setStatus(HTTPStatus.OK)

        // associate this result with the orignal event
        // this will trigger the http response
        ev.setResult(res)
    }
  }

  override def onStart() = new PTask( task1)
}

