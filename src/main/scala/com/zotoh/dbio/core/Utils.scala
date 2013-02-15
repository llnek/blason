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
import com.zotoh.dbio.meta.Column
import com.zotoh.dbio.meta.Table

/**
 * @author kenl
 */
object Utils {

  def getColumn(m:Method) = if (m==null) null else m.getAnnotation(classOf[Column])  
  def hasColumn(m:Method) = getColumn(m) != null
  
  def getTable(z:Class[_]) = if(z==null) null else z.getAnnotation(classOf[Table])  
  def hasTable(z:Class[_]) = getTable(z) != null
  
  
}


