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
class If(expr:BoolExpr) extends Conditional(expr) {

  private var _then:Activity =null
  private var _else:Activity =null

  def this(expr:BoolExpr, thenCode:Activity , elseCode:Activity ) {
    this(expr)
    _then= thenCode
    _else= elseCode
  }

  def this(expr:BoolExpr , thenCode:Activity ) {
    this(expr, thenCode, null)
  }

  def this() {
    this(null)
  }

  def reify(cur:FlowStep ) = reifyIf(cur, this)

  def withElse(elseCode:Activity ): this.type  = {
    _else=elseCode
    this
  }

  def withThen(thenCode:Activity ): this.type = {
    _then=thenCode
    this
  }

  def realize(cur:FlowStep ) {
    val next=cur.nextStep() match {
      case f:FlowStep => f
      case _ => null
    }
    cur match {
      case s:IfStep =>
        s.withElse( if(_else ==null) next else _else.reify(next) )
        s.withThen( _then.reify(next))
        s.withTest( expr() )
      case _ => /* never */
    }

  }

}
