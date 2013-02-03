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

import java.util.concurrent.atomic.{AtomicLong,AtomicInteger}


/**
 * @author kenl
 *
 */
object SeqNumGen {

  private val _numInt= new AtomicInteger(1)
  private val _num= new AtomicLong(1L)

  /**
   * @return
   */
  def nextInt() = {
    val n= _numInt.getAndIncrement
    if (n==Int.MaxValue) {
      _numInt.set(1)
    }
    n
  }

  /**
   * @return
   */
  def next() = {
    val ln= _num.getAndIncrement
    if ( ln== Long.MaxValue) {
      _num.set(1L )
    }
    ln
  }

}

