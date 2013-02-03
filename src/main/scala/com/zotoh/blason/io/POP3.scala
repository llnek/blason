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

package com.zotoh.blason
package io

import scala.collection.JavaConversions._
import scala.collection.mutable
import java.util.{Properties=>JPS,ResourceBundle}
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.blason.util.Observer
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.io.IOUtils._
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Provider
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.MimeMessage
import java.io.IOException
import com.zotoh.frwk.security.PwdFactory
import com.zotoh.blason.core.Configuration


object POP3 {
  val ST_POP3S=  "com.sun.mail.pop3.POP3SSLStore"
  val ST_POP3=  "com.sun.mail.pop3.POP3Store"
  val MOCK_ID="demopop3s"
  val POP3S="pop3s"
  val POP3C="pop3"
}

class POP3(evtHdlr:Observer, nm:String) extends ThreadedTimer(evtHdlr,nm) {
import POP3._
//    private static final String ST_IMAP= "com.sun.mail.imap.IMAPStore" ;
//    private static final String ST_IMAPS=  "com.sun.mail.imap.IMAPSSLStore"

  private var _host=""
  private var _user=""
  private var _pwd=""

  private var _delete=false
  private var _ssl=true
  private var _port=0

  private var _pop:Store = null
  private var _fd:Folder =null

  def this() {
    this (null,"")
  }

  override def configure(cfg:Configuration) {
    super.configure(cfg)

      //safePutProp( "provider", "com.zotoh.maedr.mock.mail.MockPop3Store");
      //String impl = trim(deviceProperties.optString("provider") )

    _delete= cfg.getBool("deletemsg", false)
    _ssl= cfg.getBool("ssl", true)

    _host= cfg.getString("host","").trim()
    _port= cfg.getLong("port",995L).toInt
    tstPosIntArg("pop3s-port", _port)

    _user = cfg.getString("username", "").trim()
    _pwd= cfg.getString("passwd","").trim()

    if ( ! STU.isEmpty(_pwd)) {
      _pwd= PwdFactory.mk(_pwd).text()
    }
  }

  override def preLoop() {
    _pop=null
    _fd= null
  }

  override def endLoop() {
    closePOP()
  }

  override def onOneLoop() {
    if ( conn ) {
      try {
        scanPOP()
      } catch {
        case e:Throwable => tlog().warn("",e)
      } finally {
        closeFolder()
      }
    }
  }

  private def scanPOP() {
    openFolder()
    getMsgs()
  }

  private def getMsgs() {

    val cnt= _fd.getMessageCount()

    tlog().debug("POP3: count of new messages: {}" , cnt)
    if (cnt > 0) {
      getMsgs( _fd.getMessages )
    }
  }

  private def getMsgs( msgs:Seq[Message] ) {

    msgs.foreach { (m) =>
      val mm= m.asInstanceOf[MimeMessage]
      //TODO
      //_fd.getUID(mm)
      // read all the header lines
      /*
      val sb=mm.getAllHeaderLines().foldLeft(new StringBuilder()) { (b,a) =>
          b.append(a).append("\r\n")
      }
      val data = readBytes( mm.getRawInputStream )
      * */      
      try {
        mm.getAllHeaders()
        mm.getContent()
        dispatch(new EMailEvent(this, mm))
      } finally {
        if (_delete) { mm.setFlag(Flags.Flag.DELETED, true) }
      }
    }

  }

  private def conn() = {
    if (_pop ==null || !_pop.isConnected ) try {
      var uid= if ( STU.isEmpty(_user)) null else _user
      var pwd= if (STU.isEmpty(_pwd)) null else _pwd
      val (key, sn) = if (_ssl) {
        (ST_POP3S, POP3S )
      } else {
          (ST_POP3, POP3C )
      }
      val props = new JPS().add("mail.store.protocol", sn)
      val session = Session.getInstance(props, null)
      val ps= session.getProviders()
      var proto=sn
      
      closePOP()

      var sun:Provider = ps.find { (p) => key == p.getClassName } match {
        case Some(p:Provider) => p
        case _ => throw new IOException("Failed to find pop3 store")
      }

      System.getProperty("blason.demo.pop3","") match {
        case s:String if s.length > 0 =>
          sun= new Provider(Provider.Type.STORE, MOCK_ID, s, "test", "1.0.0")
          tlog.debug("POP3: using demo pop3 store !!!!")
          proto=MOCK_ID
        case _ =>
      }

      session.setProvider(sun)

      _fd = session.getStore(proto) match {
        case st:Store =>
          _pop=st
          st.connect(_host, _port, uid , pwd)
          st.getDefaultFolder()
        case _ =>
          null
      }

      if (_fd != null) {
        _fd= _fd.getFolder("INBOX")
      }

      if (_fd==null || !_fd.exists()) {
        throw new IOException("POP3: Cannot find inbox")
      } else {
                  _fd.open(Folder.READ_WRITE)
      }
    } catch {
      case e:Throwable =>
        tlog().warn("",e)
        closePOP()
    }

    _pop != null && _pop.isConnected()
  }

  private def closePOP() {

    closeFolder()
    try {
      if (_pop != null) { _pop.close() }
    } catch {
      case e:Throwable => tlog().warn("", e)
    }
    _pop=null
    _fd=null
  }

  private def closeFolder() {
    try {
      if (_fd != null && _fd.isOpen()) { _fd.close(true) }
    } catch {
      case e:Throwable => tlog().warn("", e)
    }
  }

  private def openFolder() {
    if ( _fd != null && !_fd.isOpen()) {
      _fd.open(Folder.READ_WRITE)
    }
  }

}
