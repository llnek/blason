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
package io

import scala.collection.JavaConversions._
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file._

/**
 * @author kenl
 */
class DirWatcher(private val _targetDir:File) {

  private val _watcher= FileSystems.getDefault().newWatchService()
  private val _targetPath:Path = null// _targetDir.toPath()
  
  _targetPath.register(
                _watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE)

  def start() {
    
    while (true)  {
      val key = _watcher.take() 
      // poll all the events queued for the key
      key.pollEvents().foreach { (ev) =>
        val kind = ev.kind()
        kind.name match {
          case "ENTRY_CREATE" =>
          case "ENTRY_MODIFY" =>
          case "ENTRY_DELETE" =>
        }
        //reset is invoked to put the key back to ready state
        if (! key.reset() ) {
          // something has gone wrong!, what to do ???
        }
      }
    }
    
  }
    
}
