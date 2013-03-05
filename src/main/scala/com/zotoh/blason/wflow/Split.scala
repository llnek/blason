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
class Split(private var _join:Join) extends Composite {

  protected var _theJoin:Join= null

  def this() {
    this(null)
  }

  def addSplit(a:Activity ): this.type = {
    add(a)
    this
  }

  def reify(cur:FlowStep ) = reifySplit(cur, this)

  def withJoin(a:Join ): this.type = {
    _join=a
    this
  }

  def realize(cur:FlowStep ) {

    if ( _join != null) {
      _join.withBranches( size )
      _theJoin = _join
    } else {
      _theJoin= new NullJoin()
    }

    val s = _theJoin.reify(cur.nextStep )
    cur match {
      case ss:SplitStep =>
        _theJoin match {
          case n:NullJoin => ss.fallThrough()
          case _ => /* no op */
        }
        ss.withBranches( reifyInnerSteps(s) )
      case _ => /* never */
    }

  }


}

/**
 * @author kenl
 *
 */
class NullJoin extends Join {

  def reify(cur:FlowStep ) = reifyNullJoin(cur, this)

  def realize(cur:FlowStep ) {}

}

/**
 * @author kenl
 *
 */
class NullJoinStep(s:FlowStep, a:Join ) extends JoinStep(s,a) {

  def eval(j:Job ) = null

}
