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
package kernel

import com.zotoh.frwk.util.CoreUtils._
import org.slf4j._
import com.zotoh.blason.io.AbstractEvent

/**
 * When an event is spawned, a job is created.  The runtime will decide on what pipeline
 * should handle this job from the emitter configuration.
 *
 * @author kenl
 */
class Job( private val _jobID:Long, private val _parent:Container, private val _data:JobData ) {

  def this( jobID:Long, par:Container, ev:AbstractEvent ) {
    this(jobID, par, new JobData(ev))
  }

  def container() = _parent

  def setData(key:Any, value:Any) {
    _data.setData(key, value)
  }

  def getData(key:Any) = _data.getData(key)

  def event() = _data.getEvent

  def jobData() = _data

  def getID() = _jobID

  override def finalize() {
    super.finalize()
    println("=========================> Job: " + _jobID + " finz'ed")
  }
  
}
