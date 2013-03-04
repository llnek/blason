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
package kernel

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.zotoh.blason.io.AbstractEvent
import com.zotoh.frwk.util.CoreUtils._

/**
 * JobData is a transient collection of data belonging to a Job.  By default, it has a reference to the original event
 * which spawned the job.
 * If a Processor needs to persist some job data, those data should be encapsulate in a ProcessState object.
 *
 * @author kenl
 */
sealed class JobData protected[kernel](private var _evt:AbstractEvent) {

  private val _data= mutable.HashMap[Any,Any]()

  def setEvent(ev:AbstractEvent ) {
    _evt=ev
  }

  def getEvent() = _evt

  def setData( key:Any, value:Any) {
    if ( key != null) {
      tstObjArg("job-data value",value)
      _data.put(key, value)
    }
  }

  def getData(key:Any) = {
    if (key==null) None else _data.get(key)
  }

  def removeData(key:Any) = {
    if (key==null) None else _data.remove(key)
  }

  def clearAll() { _data.clear  }

}
