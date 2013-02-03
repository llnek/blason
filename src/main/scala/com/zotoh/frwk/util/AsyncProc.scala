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

/**
 * @author kenl
 */
class AsyncProc extends Runnable {

  private var _FC: () => Unit = null
  private var _cl:ClassLoader = null
  private var _daemon=false

  def withClassLoader(cl:ClassLoader): this.type = {
    _cl=cl
    this
  }

  def setDaemon(b:Boolean): this.type = {
    _daemon=b
    this
  }

  def mkThread() = {
    val t=new Thread(this)
    if (_cl != null) { t.setContextClassLoader(_cl) }
    t.setDaemon(_daemon)
    t
  }

  def run() {
    _FC()
  }

  def fork( action: () => Unit ) {
    _FC=action
    mkThread.start()
  }

}
