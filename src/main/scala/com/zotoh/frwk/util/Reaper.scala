
package com.zotoh.frwk
package util

import CoreUtils._

trait Crop {
  def reap(): Unit
}

class Reaper(private var _crop:Crop, private var _delayMillis:Int) extends Coroutine {

  @volatile private var _active=true

  def this(c:Crop ) {
    this(c, 0)
  }

  def stop() {
    _active = false
  }

  def run() {

    while (_active) {
      block {  () =>
        ProcessUtils.safeWait(_delayMillis)
        _crop.reap()
      }
      if(_delayMillis == 0) {
        stop()
      }
    }

  }

}
