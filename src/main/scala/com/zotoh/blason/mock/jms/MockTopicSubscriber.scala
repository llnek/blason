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
package mock.jms

import javax.jms.JMSException
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Topic
import javax.jms.TopicSubscriber


/**
 * @author kenl
 *
 */
class MockTopicSubscriber(private var _topic:Topic, private val _name:String) extends TopicSubscriber {

  private var _sub:MessageListener = null

  def close() {
    _topic=null
    _sub=null
  }

  def getMessageListener() = _sub

  def getMessageSelector() = null

  def receive() = null

  def receive(arg:Long) = null

  def receiveNoWait() = null

  def setMessageListener(ml:MessageListener ) {
    _sub= ml
  }

  def getNoLocal() = false

  def getTopic() = _topic

}

