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
package util

import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author kenl
 */
trait Observable {

  val _changed = new AtomicBoolean(false)
  var _obs:Observer

  def setObserver(obj:Observer): Unit = synchronized {
    _obs=obj
  }

  def notifyObservers(arg:Any*) {
    if (_changed.get()) {
      clearChanged()
    }
    _obs.update(this,arg:_*)
  }

  protected def setChanged(): Unit = synchronized {
    _changed.set(true)
  }

  protected def clearChanged(): Unit = synchronized {
    _changed.set(false)
  }

  def hasChanged(): Boolean = synchronized {
    _changed.get
  }

}


