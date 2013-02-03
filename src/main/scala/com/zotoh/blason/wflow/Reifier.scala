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


/**
 * @author kenl
 *
 */
object Reifier {

  /**
   * @return a Nihil Step which does nothing but indicates end of flow.
   */
  def reifyZero(f:Pipeline ) = new NihilStep(f)

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyAsyncWait(cur:FlowStep , a:AsyncWait ) = {
    post_reify[AsyncWaitStep]( new AsyncWaitStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyDelay(cur:FlowStep , a:Delay ) = {
    post_reify[DelayStep]( new DelayStep(cur,a))
  }

  /**
   *
   * @param cur
   * @param a
   * @return
   */
  def reifyPTask(cur:FlowStep , a:PTask ) = {
    post_reify[PTaskStep]( new PTaskStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifySwitch(cur:FlowStep , a:Switch ) = {
    post_reify[SwitchStep]( new SwitchStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyIf(cur:FlowStep , a:If ) = {
    post_reify[IfStep]( new IfStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyGroup(cur:FlowStep , a:Group) = {
    post_reify[GroupStep]( new GroupStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifySplit(cur:FlowStep , a:Split ) = {
    post_reify[SplitStep]( new SplitStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyOrJoin(cur:FlowStep , a:Or ) = {
    post_reify[OrStep]( new OrStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyNullJoin(cur:FlowStep , a:NullJoin ) = {
    post_reify[NullJoinStep]( new NullJoinStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyAndJoin(cur:FlowStep , a:And ) = {
    post_reify[AndStep]( new AndStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyWhile(cur:FlowStep , a:While ) = {
    post_reify[WhileStep](new WhileStep(cur,a))
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyFor(cur:FlowStep , a:For ) = {
    post_reify[ForStep]( new ForStep(cur,a))
  }


  private def post_reify[T <: FlowStep](s:T) = {
    s.realize()
    s
  }

}
