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

import scala.collection.JavaConversions._
import scala.collection.mutable

import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.io.XData

import java.io.{IOException,InputStream,OutputStream,ByteArrayOutputStream=>ByteArrayOS}
import javax.servlet.http.{HttpServletRequest=>HSRequest}

/**
 *
 * @author kenl
 */
object HTTPHplr extends CoreImplicits {

  def extract( src:BaseHttpIO, req:HSRequest ): HTTPEvent = {

    val clen= req.getContentLength()
    val thold= src.threshold()
    val ev= new HTTPEvent(src)

    ev.setContentType(req.getContentType )
    ev.setContentLength( clen)
    ev.setContextPath(req.getContextPath )
    ev.setMethod( req.getMethod  )
    ev.setServletPath( req.getServletPath )
    ev.setScheme(req.getScheme )
    ev.setUrl( nsb( req.getRequestURL ) )
    ev.setUri( req.getRequestURI  )
    ev.setQueryString( req.getQueryString  )
    ev.setProtocol(req.getProtocol )

    req.getParameterNames().foreach { (n) =>
      ev.addParam(n, req.getParameterValues(n))
    }

    req.getHeaderNames().foreach { (n) =>
      req.getHeaders(n).foreach { (v) =>
        ev.addHeader( n, v )
      }
    }

    req.getAttributeNames().foreach { (n) =>
      ev.addAttr(n, req.getAttribute(n))
    }

    if (clen > 0L) {
      grabPayload(ev, req.getInputStream, clen, thold)
    }

    ev
  }

  private def grabPayload(ev:HTTPEvent, inp:InputStream, clen:Long, thold:Long) {
    val t = if (clen > thold) { newTempFile(true) } else {
      (null, new ByteArrayOS(4096))
    }
    using(t._2) { (os) =>
      copy(inp, os, clen)
      ev.setData( new XData( if(t._1==null) os else t._1))
    }
  }

}


