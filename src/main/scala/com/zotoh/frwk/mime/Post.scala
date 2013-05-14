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

package com.zotoh.frwk
package mime

import javax.mail._
import javax.mail.internet._
import javax.xml.transform._
import javax.xml.transform.stream._
import javax.activation._
import java.io.{StringWriter, InputStream, File}

import com.zotoh.frwk.util.CoreUtils._


object Post {

  def create(from:String , subj:String, smtpHost:String ) = new MimeMail(from, subj,smtpHost)
  def create(from:String , subj:String ) = new MimeMail(from, subj )

  def transform(xslt:InputStream , xml:InputStream ) = using(new StringWriter) { (w) =>
    val tr = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt))
    tr.transform(new StreamSource(xml), new StreamResult(w))
    w.toString()
  }

}
