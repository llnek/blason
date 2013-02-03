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

import org.apache.commons.lang3.{StringUtils=>STU}
import scala.collection.JavaConversions._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import java.util.{Hashtable=>JHT,Properties=>JPS,ResourceBundle}
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Destination
import javax.jms.Message
import javax.jms.MessageConsumer
import javax.jms.MessageListener
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueReceiver
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.Topic
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory
import javax.jms.TopicSession
import javax.jms.TopicSubscriber
import javax.naming.Context
import javax.naming.InitialContext
import com.zotoh.frwk.util.CoreImplicits
import java.io.IOException
import com.zotoh.blason.util.Observer
import com.zotoh.blason.core.Configuration


object JMSIO {

val PSTR_CTXTFAC= "contextfactory"
val PSTR_CONNFAC= "connfactory"
val PSTR_JNDIUSER= "jndiuser"
val PSTR_JNDIPWD= "jndipwd"
val PSTR_JMSUSER= "jmsuser"
val PSTR_JMSPWD= "jmspwd"
val PSTR_DURABLE= "durable"
val PSTR_PROVIDER= "providerurl"
val PSTR_DESTINATION= "destination"

}

/**
 * A JMS client receiver.  The message is not confirmed by default unless an error occurs.  Therefore, the application is
 * responsible for the confirmation to messages.
 *
 * The set of properties:
 *
 * <b>contextfactory</b>
 * The class name of the context factory to be used as part of InitContext().
 * <b>connfactory</b>
 * The name of the connection factory.
 * <b>jndiuser</b>
 * The JNDI username, if any.
 * <b>jndipwd</b>
 * The JNDI user password, if any.
 * <b>jmsuser</b>
 * The username needed for your JMS server.
 * <b>jmspwd</b>
 * The password for your JMS server.
 * <b>durable</b>
 * Set to boolean true if message is persistent.
 * <b>providerurl</b>
 * The provider URL.
 * <b>destination</b>
 * The name of the destination.
 *
 * @author kenl
 *
 */
class JMS(evtHdlr:Observer, nm:String) extends EventEmitter(evtHdlr,nm) with CoreImplicits {
  import JMSIO._

  private var _conn:Connection = null
  private var _durable=false

  private var _JNDIPwd=""
  private var _JNDIUser=""
  private var _connFac=""
  private var _ctxFac=""
  private var _url=""
  private var _dest=""
  private var _jmsUser=""
  private var _jmsPwd=""

  def this() {
    this(null,"")
  }
  
  private def onMessage(original:Message) {
    var msg=original
    try {
      dispatch( new JMSEvent(this,msg) )
      msg=null
    } catch {
      case e:Throwable => tlog().error("", e)
    } finally {
      if (msg!=null) block { () => msg.acknowledge() }
    }
  }

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    _ctxFac= cfg.getString(PSTR_CTXTFAC,"")
    _connFac= cfg.getString(PSTR_CONNFAC,"")
    _JNDIUser= cfg.getString(PSTR_JNDIUSER,"")
    _JNDIPwd= cfg.getString(PSTR_JNDIPWD,"")
    _jmsUser= cfg.getString(PSTR_JMSUSER,"")
    _jmsPwd= cfg.getString(PSTR_JMSPWD,"")
    _durable= cfg.getBool(PSTR_DURABLE,false)
    _url= cfg.getString(PSTR_PROVIDER,"")
    _dest= cfg.getString(PSTR_DESTINATION,"")
  }

  def onStart() { inizConn() }

  def onStop() {
    if (_conn != null) try {
      _conn.close()
    } catch {
      case _:Throwable =>
    }
    _conn=null
  }

  private def inizConn() {

    val vars= new JHT[String,String]()

    if (! STU.isEmpty(_ctxFac))
    {  vars.put(Context.INITIAL_CONTEXT_FACTORY, _ctxFac) }

    if (! STU.isEmpty(_url))
    {  vars.put(Context.PROVIDER_URL, _url) }

    if (! STU.isEmpty(_JNDIPwd))
    {  vars.put("jndi.password", _JNDIPwd) }

    if (! STU.isEmpty(_JNDIUser))
    {  vars.put("jndi.user", _JNDIUser) }

    val ctx= new InitialContext(vars)
    ctx.lookup(_connFac) match {
      case obj:QueueConnectionFactory => inizQueue(ctx, obj)
      case obj:TopicConnectionFactory => inizTopic(ctx, obj)
      case obj:ConnectionFactory => inizFac(ctx, obj)
      case _ =>
        throw new IOException("JMS: unsupported JMS Connection Factory")
    }
    if (_conn != null) {
      _conn.start()
    }
  }

  private def inizFac(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[ConnectionFactory]
    val c= ctx.lookup(_dest)
    val me=this

    _conn= if ( ! STU.isEmpty(_jmsUser)) {
      f.createConnection( _jmsUser, _jmsPwd)
    } else {
      f.createConnection()
    }

    c match {
      case x:Destination =>
        //TODO ? ack always ?
        _conn.createSession(false, Session.CLIENT_ACKNOWLEDGE).
        createConsumer(x).
        setMessageListener( new MessageListener {
          def onMessage(m:Message) { me.onMessage(m) }
        })
      case _ =>
        throw new IOException("JMS: Object not of Destination type")
    }
  }

  private def inizTopic(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[TopicConnectionFactory]
    val me=this    
    val c = if ( ! STU.isEmpty(_jmsUser)) {
      f.createTopicConnection(_jmsUser, _jmsPwd)
    } else {
      f.createTopicConnection()
    }
    _conn=c
    val s= c.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE)
    val t= ctx.lookup(_dest).asInstanceOf[Topic]
    val b = if (_durable) {
      s.createDurableSubscriber(t, uid )
    } else {
      s.createSubscriber(t)
    }
    b.setMessageListener( new MessageListener {
      def onMessage(m:Message) { me.onMessage(m) }
    })
    
  }

  private def inizQueue(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[QueueConnectionFactory]
    val q= ctx.lookup(_dest).asInstanceOf[Queue]

    val c = if ( ! STU.isEmpty(_jmsUser)) {
      f.createQueueConnection(_jmsUser, _jmsPwd)
    } else {
      f.createQueueConnection()
    }
    _conn=c
    val s= c.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)
    val r= s.createReceiver(q)
    val me=this

    r.setMessageListener( new MessageListener {
      def onMessage(m:Message) { me.onMessage(m) }
    })
    
  }

}

