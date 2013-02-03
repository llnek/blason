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
import com.zotoh.blason.kernel.Job

/**
 * @author kenl
 *
 */
class SwitchStep(s:FlowStep , a:Activity ) extends FlowStep(s,a) {

  private var _expr:SwitchChoiceExpr =null
  private var _cs:Map[Any,FlowStep]=null
  private var _def:FlowStep =null

  def withChoices(cs:Map[Any,FlowStep] ) = {
    _cs= cs.toMap
    this
  }

  def withDef(s:FlowStep ) = {
    _def=s
    this
  }
  
  def withExpr(e:SwitchChoiceExpr ) = {
    _expr=e
    this
  }

  def choices() = _cs

  def defn() = _def

  def eval(j:Job ) = {
    val c= popClosureArg()
    var a:FlowStep = _expr.eval(j) match {
      case m if m != null => _cs.get(m).getOrElse(null)
      case _ => null
    }

    // if no match, try default?
    if (a == null) {
      a=_def
    }
    if (a != null) {
      a.attachClosureArg(c) 
    }

    realize()
    a
  }

}
