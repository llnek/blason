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
package core

import java.util.{Date=>JDate}


/**
 * @author kenl
 */
trait Configuration {

  def getChild(name:String): Option[Configuration]
  def getSequence(name:String): Seq[Any]

  def contains(name:String):Boolean
  def size(): Int

  def getString(name:String, dft:String): String
  def getLong(name:String, dft:Long): Long
  def getDouble(name:String, dft:Double): Double
  def getBool(name:String, dft:Boolean): Boolean
  def getDate(name:String): Option[JDate]

  def getKeys(): Seq[String]

}


