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
class WhileStep(s:FlowStep , a:While ) extends ConditionalStep(s,a) {

  private var _body:FlowStep =null

  def eval(j:Job ) = {
    var rc:FlowStep = this
    val c= popClosureArg()

    if ( ! test(j)) {
      //tlog().debug("WhileStep: test-condition == false")
      rc= nextStep()
      if (rc != null) { rc.attachClosureArg(c) }
      realize()
    } else {
      //tlog().debug("WhileStep: looping - eval body")
      _body.attachClosureArg(c)
      val f= _body.eval(j)
      f match {
        case x:AsyncWaitStep =>
          x.forceNext(rc)
          rc=x
        case x:DelayStep => 
          x.forceNext(rc)
          rc=x
        case s:FlowStep if ! (s eq this) => _body = s
        case _ => /* never */
      }
      
    }
    rc
  }

  def withBody(body:FlowStep ) = {
    _body=body
    this
  }

}
