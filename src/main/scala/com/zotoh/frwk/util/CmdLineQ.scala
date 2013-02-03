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

package com.zotoh.frwk
package util

import org.apache.commons.lang3.{StringUtils=>STU}
import java.util.{Properties=>JPS}
import com.zotoh.frwk.util.StrUtils._


/**
 * @author kenl
 *
 */
abstract class CmdLineQ( private var _id:String, private var _question:String,
    private var _choices:String="", private var _dftAnswer:String="" ) {

  protected var _mandatory=false
  private var _answer=""
  private var _props:JPS= null

  _dftAnswer=nsb(_dftAnswer)
  _question=nsb(_question)
  _id=nsb(_id)
  _choices=nsb(_choices)

  /**
   * @param b
   */
  def setMust(b:Boolean): this.type = { _mandatory=b ; this }

  /**
   * @return
   */
  def isMust() = _mandatory


  /**
   * @return
   */
  def label()  = _id

  /**
   * @return
   */
  def question()  = _question

  /**
   * @return
   */
  def choices()  = _choices

  /**
   * @return
   */
  def answer()  = _answer

  /**
   * @param a
   */
  def setDftAnswer(a:String): this.type = { _dftAnswer=nsb(a) ; this }

  /**
   * @return
   */
  def dftAnswer()  = _dftAnswer

  /**
   * @param c
   */
  def setChoices(c:String): this.type = { _choices=nsb(c) ; this }

  /**
   * @param props
   */
  def setOutput(props:JPS): this.type = {    _props=props ; this  }

  /**
   * @param a
   * @return
   */
  def setAnswer(a:String) = {
    _answer= nsb( if (STU.isEmpty(a)) _dftAnswer else a)
    onRespSetOut(_answer, _props)
  }

  /**
   * @param answer
   * @param props
   * @return
   */
  def onRespSetOut(answer:String, props:JPS): String

}



