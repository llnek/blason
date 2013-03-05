/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSimport static com.zotoh.core.util.LoggerFactory.getLogger;

E,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WAimport com.zotoh.core.util.Logger;
import com.zotoh.maedr.core.DeviceFactory;
RRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
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
package wflow

import com.zotoh.blason.wflow.Reifier._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.SeqNumGen
import java.util.{Properties=>JPS}
import com.zotoh.blason.kernel.Job
import org.slf4j._
import com.zotoh.frwk.util.MetaUtils._


object Pipeline {

  def pipeline(cz:String , j:Job ) = {
    val rc= try {
      loadClass(cz).getConstructor( classOf[Job] ).newInstance(j) match {
        case p:Pipeline => p
        case _ => null
      }
    } catch {
      case e:Throwable =>
        tlog().warn("",e)
        null
    }
    if (rc ==null) no_flow(j) else rc
  }

  private def no_flow(j:Job) = {
    new Pipeline(j) { def onStart() = new Nihil() }
  }

}

/**
 * @author kenl
 *
 */
abstract class Pipeline protected[wflow](private val _theJob:Job) {

  private val _log:Logger= LoggerFactory.getLogger(classOf[Pipeline] )
  def tlog() = _log

  private val _pid= nextPID()
  private var _active=false

  tlog().debug("{}: {} => pid : {}" , "Pipeline", getClass().getName() , asJObj(_pid))
  tstObjArg("job", _theJob)

  def container() = job().container()
  def isActive() = _active

  protected[wflow] def nextAID() = SeqNumGen.next()

  def core() = container().scheduler()
  def getPID() = _pid

  protected def onEnd() {}

  protected[wflow] def onError(e:Throwable) : Activity = {
    tlog().error("", e)
    null
  }

  protected def onStart():Activity

  def job() = _theJob

  protected def preStart() {}

  def start() {

    tlog().debug("{}: {} => pid : {} => starting" , "Pipeline", getClass().getName() , asJObj(_pid))

    val s1= reifyZero( this)
    preStart()

    val s= onStart() match {
      case x:Nihil => reifyZero(this)
      case null => reifyZero(this)
      case a1 => a1.reify(s1)
    }

    try {
      core().run( s)
    } finally {
      _active=true
    }

  }

  def stop() {
    try {
      onEnd()
    } catch {
      case e:Throwable => tlog().error("",e)
    }

    tlog().debug("{}: {} => pid : {} => end" , "Pipeline",getClass().getName() , asJObj(_pid))
  }

  private def nextPID() = SeqNumGen.next()

  override def toString() = {
    getClass().getSimpleName() + "(" + _pid + ")"
  }

//  override def finalize() {
//    super.finalize()
//    println("=========================> Pipeline: " + getClass.getName + " finz'ed")
//  }
  
}
