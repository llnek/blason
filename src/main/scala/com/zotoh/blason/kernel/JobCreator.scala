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
package kernel


import com.zotoh.blason.io.AbstractEvent
import com.zotoh.blason.wflow.OrphanFlow
import com.zotoh.blason.wflow.Pipeline
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.util.SeqNumGen
import com.zotoh.blason.util.{Observable,Observer}
import com.zotoh.blason.core.Composable
import com.zotoh.blason.core.ComponentRegistry


/**
 * @author kenl
 */
class JobCreator extends Observer with Composable {
  private var _par:Container=null

  def compose(r:ComponentRegistry, arg:Any*) = {
    _par = arg(0).asInstanceOf[Container]
    Some(this)
  }

  def update(src:Observable, arg:Any*) {
    val ev=arg(0).asInstanceOf[AbstractEvent]
    val cz= if (ev.hasRouter) ev.routerClass else arg(1).asInstanceOf[String]
    val job= new Job(SeqNumGen.next, _par,ev)    
    var p= maybeMkPipe(cz, job)
    if (p==null) {
      p=new OrphanFlow(job)
    }
    p.start
  }

  private def maybeMkPipe(cz:String,job:Job) = {
    try {
      loadClass(cz).getConstructor(classOf[Job]).newInstance(job) match {
        case p:Pipeline => p
        case _ => null
      }
    } catch {
      case e:Throwable => null
    }
  }

}




