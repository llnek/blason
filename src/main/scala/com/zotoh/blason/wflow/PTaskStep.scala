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

import com.zotoh.blason.wflow.Reifier._
import com.zotoh.blason.kernel.Job

/**
 * @author kenl
 *
 */
class PTaskStep(s:FlowStep, a:PTask ) extends FlowStep(s,a) {

  private var _work:Work = null

  def withWork(w:Work) = {
    _work=w
    this
  }

  def eval(j:Job ) = {
    //tlog.debug("PTask: {} about to perform work.", this.id )
    val a= _work.perform(this, j)
    var rc= nextStep()

    if (a != null) {
      if (a.isInstanceOf[Nihil] ) { rc = reifyZero( flow ) }
      else {
        rc= a.reify(rc)
      }
    }

    rc
  }


}
