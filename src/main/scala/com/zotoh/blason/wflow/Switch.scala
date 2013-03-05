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

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.blason.wflow.Reifier._


/**
 * @author kenl
 *
 */
class Switch(private var _expr:SwitchChoiceExpr ) extends Activity {

  private val _choices= mutable.HashMap[Any,Activity]()
  private var _def:Activity =null

  def this() {
    this(null)
  }

  def withExpr(e:SwitchChoiceExpr ): this.type = {
    _expr=e
    this
  }

  def withChoice(matcher:Any, body:Activity ): this.type = {
    _choices.put(matcher, body)
    this
  }

  def withDef(a:Activity ): this.type = {
    _def=a
    this
  }

  def reify(cur:FlowStep ) = reifySwitch(cur, this)

  def realize(cur:FlowStep ) {

    val t= mutable.HashMap[Any,FlowStep]()
    val nxt= cur.nextStep()

    _choices.foreach { (en) =>
      t.put(en._1,
              en._2.reify(nxt))
    }

    cur match {
      case s:SwitchStep =>
        s.withChoices(t.toMap)
        if (_def != null) {
          s.withDef( _def.reify(nxt))
        }
        s.withExpr(_expr)
      case _ => /* never */
    }

  }








}
