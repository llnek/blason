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


package com.zotoh.dbio
package core

import java.lang.reflect.Method
import com.zotoh.dbio.meta._

/**
 * @author kenl
 */
object Utils {

  def ensureAssoc(m:Method) = {
    val mn=m.getName
    if ( mn.startsWith("dbio_") && mn.endsWith("_fkey") && mn.length > 10 ) {} else {
      throw new Exception("Invalid assoc-fkey marker:  found : " + mn)
    }
    mn
  }
  
  def ensureMarker(m:Method) = {
    val mn=m.getName
    if ( mn.startsWith("dbio_") && mn.endsWith("_column") && mn.length > 12 ) {} else {
      throw new Exception("Invalid marker:  found : " + mn)
    }
    mn
  }


  def fmtMarkerKey(mn:String) =    "dbio_" + mn + "_column"
  def fmtAssocKey(mn:String) =  "dbio_" + mn + "_fkey"

  def splitMarkerKey(mn:String) = mn.substring(5, mn.length - 7)
  def splitAssocKey(mn:String) = mn.substring(5,mn.length - 5)


  
  def getColumn(m:Method) = if (m==null) null else m.getAnnotation(classOf[Column])
  def hasColumn(m:Method) = getColumn(m) != null

  def getTable(z:Class[_]) = {
    if(z==null) null else {
      var t=z.getAnnotation(classOf[Table])
      if (t==null) t = z.getInterfaces().find(  _.getAnnotation(classOf[Table]) != null  ) match {
        case Some(x) => x.getAnnotation(classOf[Table])
        case _ => null
      }
      t
    }
  }
  def hasTable(z:Class[_]) = getTable(z) != null

  def getM2M(m:Method) = if (m==null) null else m.getAnnotation(classOf[Many2Many])
  def getO2M(m:Method) = if (m==null) null else m.getAnnotation(classOf[One2Many])
  def getO2O(m:Method) = if (m==null) null else m.getAnnotation(classOf[One2One])

  def hasAssoc(m:Method) = hasO2O(m) || hasO2M(m) || hasM2M(m)
  def hasM2M(m:Method) = getM2M(m) != null
  def hasO2O(m:Method) = getO2O(m) != null
  def hasO2M(m:Method) = getO2M(m) != null

  def throwNoTable(z:Class[_]) = {
    getTable(z) match {
      case x:Table => x
      case _ => throw new Exception("No table annotation for class: " + z)
    }
  }

  def throwNoColumn(m:Method) = {
    getColumn(m) match {
      case x:Column => x
      case _ => throw new Exception("No column annotation for class: " + m.getName)
    }
  }

}


