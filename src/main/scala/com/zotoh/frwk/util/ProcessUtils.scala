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

import java.lang.management.ManagementFactory
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils._


/**
 * @author kenl
 *
 */
object ProcessUtils {

  private val _log= LoggerFactory.getLogger(classOf[ProcessUtils])
  def tlog() = _log

  /**
   * @param r
   */
  def asyncExec(r:Runnable) {
    if ( r != null) {
      val t=new Thread(r)
      t.setContextClassLoader( getCZldr())
      t.setDaemon(true)
      t.start
    }
  }

  /**
   * @param millisecs
   */
  def safeWait(millisecs:Long) {
    block { () =>
      if ( millisecs > 0L) { Thread.sleep(millisecs) }
    }
  }

  /**
   * Block and wait on the object.
   *
   */
  def blockAndWait(lock:AnyRef, waitMillis:Long) {
    lock.synchronized {
      block { () =>
        if (waitMillis > 0L) { lock.wait(waitMillis) } else { lock.wait() }
      }
    }
  }

  def pid() = {
    val ss = nsb( ManagementFactory.getRuntimeMXBean().getName()).split("@")
    if ( ! isNilSeq(ss)) ss(0) else ""
  }

  /**
   *
   */
  def blockForever() {
    while (true) block { () =>
      Thread.sleep(5000)
    }
  }

}

sealed class ProcessUtils {}

