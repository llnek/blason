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

/**
 * @author kenl
 *
 */
class PTask(private var _work:Work) extends Activity {

  def this() {
    this(null)
  }

  def reify(cur:FlowStep ) = reifyPTask(cur, this)

  def realize(cur:FlowStep ) {
    cur match {
      case s:PTaskStep => s.withWork(_work)
      case _ => /* never */
    }
  }

  def work() = _work

  def withWork(w:Work) = {
    _work=w
    this
  }

}
