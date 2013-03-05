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
class SplitStep(s:FlowStep , a:Split )  extends FlowStep(s,a) {

  private var _branches:IterWrapper = null
  private var _fallThru=false

  def eval(j:Job ) = {
    val c= popClosureArg()
    var rc:FlowStep =null

    while ( !_branches.isEmpty ) {
      rc = _branches.next()
      rc.attachClosureArg(c)
      flow().core().run(rc)
    }

    realize()

    // should we also pass the closure to the next step ? not for now
    if (_fallThru) nextStep() else null
  }

  def withBranches(w:IterWrapper): this.type = {
    _branches=w
    this
  }

  def fallThrough(): this.type = {
    _fallThru=true
    this
  }

}
