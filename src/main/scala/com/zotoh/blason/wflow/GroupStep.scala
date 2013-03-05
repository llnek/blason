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
package wflow

import com.zotoh.blason.kernel.Job


/**
 * @author kenl
 *
 */
class GroupStep protected[wflow](s:FlowStep, a:Group) extends FlowStep(s,a) {

  protected var _steps:IterWrapper= null

  def eval(j:Job ) = {
    val c= popClosureArg()   // data pass back from previous async call?
    var rc:FlowStep =null

    if ( ! _steps.isEmpty()) {
      //tlog.debug("GroupStep: {} element(s.)",  _steps.size )
      val n=_steps.next()
      n.attachClosureArg(c)
      rc = n.eval(j)
    } else {
      //tlog.debug("GroupStep: no more elements.")
      rc=nextStep()
      if (rc != null) {  rc.attachClosureArg(c) }
      realize()
    }

    rc
  }

  def withSteps(wrap:IterWrapper ): this.type = {
    _steps=wrap
    this
  }

}
