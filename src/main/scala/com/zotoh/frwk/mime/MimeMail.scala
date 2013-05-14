/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

package com.zotoh.frwk
package mime

import scala.collection.JavaConversions._
import scala.collection.mutable

import java.util.{Properties=>JPS}
import java.io.{File}
import javax.activation.{DataHandler, FileDataSource}
import javax.mail.internet.{MimeBodyPart, MimeMultipart, MimeMessage,InternetAddress}
import javax.mail.{Transport, Part, Message, Session}
import com.zotoh.frwk.util.StrUtils._

/**
 * @author kenl
 *
 */
class MimeMail(private val _sender:String,
  private val _subj:String,private val _smtpHost:String="") extends Email {

  private val _atts= mutable.HashSet[FileDataSource]()
  private val _from= new InternetAddress(_sender)

  val ps = new JPS()
  if (hgl(_smtpHost)) {
    ps.put("mail.smtp.host", _smtpHost)
  }
  val session = Session.getInstance(ps, null)
  private val _msg = new MimeMessage(session)

  def attach(fp:File ) {
    _atts += new FileDataSource(fp)
  }

  def addRecipient(kind:Message.RecipientType,addrs:String ) {
    _msg.addRecipients(kind,addrs)
  }

  def send(body:String ) = send(body, "text", "html" )

  def send(body:String , cType:String, subType:String ) {

    _msg.setHeader("Content-Type", cType+"/"+subType + "; charset=UTF-8")
    _msg.setSubject(_subj, "UTF-8")
    _msg.setFrom(_from)

    if( _atts.isEmpty) { _msg.setText(body, "UTF-8", subType) } else {
      val mp = new MimeMultipart()
      val bp = new MimeBodyPart()

      bp.setDisposition(Part.INLINE)
      bp.setText(body, "UTF-8", subType)
      mp.addBodyPart(bp)

      _atts.foreach { (a) =>
        val bp = new MimeBodyPart()
        bp.setDisposition(Part.ATTACHMENT)
        bp.setFileName(a.getName)
        bp.setDataHandler(new DataHandler(a))
        mp.addBodyPart(bp)
      }

      _msg.setContent(mp)
    }

    Transport.send(_msg)

  }

}



