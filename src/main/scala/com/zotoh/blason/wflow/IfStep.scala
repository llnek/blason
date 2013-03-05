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
class IfStep(s:FlowStep , a:If ) extends ConditionalStep(s,a) {

  private var _then:FlowStep = null
  private var _else:FlowStep = null

  def withElse(s:FlowStep ): this.type = {
    _else=s
    this
  }

  def withThen(s:FlowStep ): this.type = {
    _then=s
    this
  }

  def eval(j:Job ) = {
    val c= popClosureArg()   // data pass back from previous async call?
    val b = test(j)
    tlog().debug("If: test {}", ( if(b) "OK" else "FALSE"))
    val rc = if (b) _then else _else
    if (rc != null) {
      rc.attachClosureArg(c)
    }
    realize()
    rc
  }

}
