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

package com.zotoh.jmx
package support

import scala.collection.JavaConversions._

import javax.management.ObjectName
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


/**
 *
 * @author kenl
 */
object JMXUtils {

  def mkObjectName(rc:JMXResource,domain:String,beanName:String,
      paths:Array[String]): ObjectName = {
    
    val sb = new StringBuilder(512)
    var comma=false
    var num = 0
    if ( !STU.isEmpty(domain)) sb.append(domain) else sb.append(rc.domain)
    sb.append(":")
    val arr= if (paths.size > 0) paths else rc.paths
    arr.foreach { (fns) =>
      if (comma) { sb.append(',') }
      if (fns.indexOf('=') < 0) {
        sb.append(String.format("%1$02d", asJObj(num))).append('=')
        num += 1
      }
      sb.append(fns)
      comma=true
    }

    if (comma) { sb.append(',') }
    sb.append("name=").append(
        if (!STU.isEmpty(beanName)) beanName else rc.beanName 
    )

    new ObjectName( sb.toString )
  }
  
  def mkObjectName(rc:JMXResource): ObjectName = {
      mkObjectName(rc,"","",Array())
  }

  def mkObjectName(domain:String, bean:String ): ObjectName = {
    tstEStrArg("jmx domain name",domain)
    tstEStrArg("jmx bean name",bean)
    new ObjectName(domain + ":name=" + bean )
  }

  def inferObjectNameEx(obj:Any,domain:String,name:String,paths:Array[String]): ObjectName = {
    val rc = obj.getClass().getAnnotation(classOf[JMXResource])
    tstObjArg("jmx resource annotation",rc)
    mkObjectName(rc,domain,name,paths)
  }

  def inferObjectName(obj:Any): ObjectName = {
    val rc = obj.getClass().getAnnotation(classOf[JMXResource])
    tstObjArg("jmx resource annotation",rc)
    mkObjectName(rc)
  }
  
}
