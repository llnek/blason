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

import com.zotoh.blason.core.Loggable
import org.slf4j._

/**
 * An Activity is a definition of work - a task to be done.
 * At runtime, it has to be reified - make alive.  This process
 * turns an Activity into a Step in the Workflow.
 *
 * @author kenl
 *
 */
abstract class Activity protected[wflow]() extends Loggable {

  private val _log:Logger= LoggerFactory.getLogger(classOf[Activity])
  def tlog() = _log

  /**
   * Connect up another activity to make up a chain.
   *
   * @param a the unit of work to follow after this one.
   * @return an *ordered* list of work units.
   */
  def chain( a:Activity ) =  new Group(this, a)

  def +(a:Activity) = chain(a) 
  
  /**
   * Instantiate a *live* version of this work unit as it becomes
   * part of the Workflow.
   *
   * @param cur current step.
   * @return a *live* version of this Activity.
   */
  def reify(cur:FlowStep ):FlowStep

  /**
   * Configure the *live* version of this Activity.
   *
   * @param cur
   */
  def realize(cur:FlowStep ):Unit

}
