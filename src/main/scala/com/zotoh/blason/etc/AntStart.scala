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
package etc

import java.util.{Properties=>JPS}
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter


/**
 * (Internal use only).
 *
 * @author kenl
 */
object AntStart {

  def main(args:Array[String]) {
    
    import freemarker.template._
    val cfg = new Configuration()
    cfg.setDirectoryForTemplateLoading(
        new File("/tmp"))
    cfg.setObjectWrapper(new DefaultObjectWrapper())  
    val temp = cfg.getTemplate("test.ftl")
    
    val out = new OutputStreamWriter(System.out)
    val root= new java.util.HashMap[Object,Object]()
    var m= new java.util.HashMap[Object,Object]()
    root.put("user", "kenl")
    root.put("app", m)
    m.put("encoding", "latin-1")
    m.put("author", "kenken")
    temp.process(root, out)
    out.flush()  
    
  }
  
  
}    
