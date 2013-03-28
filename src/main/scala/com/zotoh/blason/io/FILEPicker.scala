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

package com.zotoh.blason
package io

import org.apache.commons.lang3.{StringUtils=>STU}
import scala.collection.mutable
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.FileUtils._
import java.io.{File,FilenameFilter,IOException}
import java.util.{Properties=>JPS,ResourceBundle}
import org.apache.commons.io.filefilter._
import org.apache.commons.io.{FileUtils=>FUT}
import com.zotoh.blason.core.Configuration
import com.zotoh.blason.util.Observer
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import java.io.FileFilter

object FILEAction extends Enumeration {
  type FILEAction = Value
  val FP_CREATED = Value(0,"created")
  val FP_CHANGED = Value(1,"changed")
  val FP_DELETED = Value(2,"deleted")
}


/**
 * @author kenl
 */
class FILEPicker(evtHdlr:Observer, nm:String) extends RepeatTimer(evtHdlr,nm) {
  import FILEAction._
  private var _monitor:FileAlterationMonitor= null
  //private val _dirs= mutable.HashSet[File]()
  private var _mask:FileFilter= null
  private var _destMove:File=null
  private var _srcDir:File=null

  def this() { this (null,"") }

  def destDir() = _destMove
  def srcDir() = _srcDir

  override def configure(cfg:Configuration) {
    super.configure(cfg)
    val root= filterVars( cfg.getString("target-folder","") )
    val dest= filterVars( cfg.getString("recv-folder","") )
    val mask= cfg.getString("fmask","")

    tstEStrArg("file-root-folder", root)
    _srcDir = new File(root)
    _srcDir.mkdirs()
    //_dirs += _srcDir
    _mask = mask match {
      case s:String if s.startsWith("*.") => new SuffixFileFilter(s.substring(1))
      case s:String if s.endsWith("*") => new PrefixFileFilter(s.substring(0,s.length()-1))
      case s:String if s.length() > 0 => new RegexFileFilter(mask)//new WildcardFileFilter(mask)
      case _ => FileFileFilter.FILE
    }

    if (! STU.isEmpty(dest)) {
      _destMove=new File(dest)
      _destMove.mkdirs()
    }

    tlog.debug("FILEPicker: monitoring folder: {}", _srcDir)
    tlog.debug("FILEPicker: recv folder: {}", nsn(_destMove))
  }

  override def onInit() {
    val observer = new FileAlterationObserver(_srcDir,_mask)
    val mon= new FileAlterationMonitor(intervalMillis() )
    val lnr = new FileAlterationListenerAdaptor() {
      override def onFileCreate(file:File) {
        postPoll(file, FP_CREATED )
      }
      override def onFileChange(file:File) {
        postPoll(file, FP_CHANGED)
      }
      override def onFileDelete(file:File) {
        postPoll(file, FP_DELETED)
      }
    }
    observer.addListener(lnr)
    mon.addObserver(observer)
    _monitor=mon
  }

  override def schedule() {
    when() match {
      case Some(w) => scheduleTriggerWhen(w)
      case _ => scheduleTrigger( delayMillis() )
    }
  }

  override def wakeup() {
    _monitor.start()
  }

  private def testDir(dir:File) = {
    if ( ! isDirWR(dir)) {
      throw new Exception("Folder: " +
          dir.getCanonicalPath() + " must be a valid directory with RW access")
    }
    dir
  }

  private def postPoll(f:File, action:FILEAction ) {
    tlog().debug("{} : {} was {}" , "FilePicker", f, action)

    val fn= niceFPath(f)
    var cf=f

    if ( FP_DELETED != action && _destMove != null) try {
      FUT.moveFileToDirectory(f, _destMove, false)
      cf=new File(_destMove, f.getName())
    } catch {
      case e:Throwable => tlog.warn("",e)
    }

    dispatch(new FILEEvent(this, fn, cf, action))
  }

}



