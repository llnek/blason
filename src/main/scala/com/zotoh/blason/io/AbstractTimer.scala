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
package io

import scala.math._
import java.util.{Date=>JDate,Timer=>JTimer}
import com.zotoh.frwk.util.DateUtils._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.util.Observer

object AbstractTimer {
  val PSTR_INTVSECS= "interval-secs"
  val PSTR_DELAYSECS= "delay-secs"
  val PSTR_WHEN= "delay-when"
}

/**
 * @author kenl
 */
abstract class AbstractTimer(evtHdlr:Observer, nm:String) extends EventEmitter(evtHdlr,nm)  with Loopable {

  private var _when:Option[JDate] = None
  private var _delayMillis:Long =0L
  var _timer:JTimer=null

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    val delay= max( cfg.getLong(AbstractTimer.PSTR_DELAYSECS, 1L ), 1L)
    val when= cfg.getDate(AbstractTimer.PSTR_WHEN).getOrElse(null)
    if (when != null) {
      setWhen(when)
    } else {
      setDelayMillis(1000L * delay )
    }
  }

  protected def delayMillis() = _delayMillis
  protected def when() = _when

  protected def setDelayMillis(d:Long) = {
    tstNonNegLongArg("delay-millis", d)
    _delayMillis=d
    this
  }

  protected def setWhen(d:JDate) = {
    tstObjArg("when",d)
    _when=Some(d)
    this
  }


}

