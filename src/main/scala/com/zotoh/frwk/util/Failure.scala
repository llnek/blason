/*??
 * COPYRIGHT (C) 2012-2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

/**
 * @author kenl
 */
class RootFailure(t:Throwable) {
  var tt= t
  while (tt.getCause() != null) {
    tt = tt.getCause()
  }
  private val _root = tt

  def root() = _root
}


/**
 * @author kenl
 */
class Failure(msg:String,t:Throwable) extends Exception(msg,t) {

  def this(m:String ) {
    this(m,null)
  }

  def this(e:Throwable) {
    this("",e)
  }

  def this(h:RootFailure ) {
    this(h.root )
  }

}

