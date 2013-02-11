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

package demo.pop3

import java.util.concurrent.atomic.AtomicInteger
import com.zotoh.blason.kernel._
import com.zotoh.blason.wflow._
import com.zotoh.blason.io._
import javax.mail.Provider
import javax.mail.Message
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import javax.mail.Multipart

/**
 * @author kenl
 *
 */
class DemoMain(c:Container) {
  private val _PS= "com.zotoh.blason.mock.mail.MockPop3Store"
  //private val _PV=new Provider(Provider.Type.STORE, "pop3s", _PS, "test", "1.0.0")
  
  System.setProperty("blason.demo.pop3", _PS)
    
  def start() {    
      println("Demo receiving POP3 emails..." )
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

  override def onStart() = new PTask withWork new Work {
    def eval(job:Job,arg:Any*) {
      val msg= job.event().asInstanceOf[EMailEvent].getMsg
      println("######################## (" + count + ")" )
      print( msg.getSubject() + "\r\n")
      print( msg.getFrom()(0).toString() + "\r\n")
      print(msg.getRecipients( Message.RecipientType.TO )(0).toString + "\r\n")
      print("\r\n")
      msg.getContent() match {
        case p:Multipart =>
          println ( new String( bytes( p.getBodyPart(0).getInputStream()), "utf-8") )
        case _ =>
      }
    }

  }

}

