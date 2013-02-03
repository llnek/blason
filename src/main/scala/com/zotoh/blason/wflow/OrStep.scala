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
class OrStep(s:FlowStep, a:Or)  extends JoinStep(s,a) {

  def eval(j:Job ) = eval_0(j)

  private def eval_0(j:Job ) = synchronized {
    val c= popClosureArg()
    var rc:FlowStep = this

    _cntr += 1

    if (size == 0) {
      rc= nextStep()
      realize()
    }
    else if (_cntr==1) {
      rc= if (_body== null) nextStep() else _body
    }
    else if ( _cntr==size ) {
      rc=null
      realize()
    }

    if (rc != null) { rc.attachClosureArg(c) }
    rc
  }

}
