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
 * A logical group - sequence of connected activities.
 * 
 * @author kenl
 *
 */
class Group extends Composite {

  def this(a:Activity,  more:Activity*) {
    this()
    add(a)
    more.foreach { (m) => add(m) }
  }

  override def chain(a:Activity ): this.type = {
    add(a)
    this
  }

  override def +(a:Activity): this.type = chain(a) 
  
  def reify(cur:FlowStep ) = reifyGroup(cur, this)

  def realize( cur:FlowStep ) {
    cur match {
      case s:GroupStep => s.withSteps( reifyInnerSteps(cur))
      case _ => /* never */
    }

  }

}
