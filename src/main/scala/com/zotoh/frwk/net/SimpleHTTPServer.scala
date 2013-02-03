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

package com.zotoh.frwk
package net

import java.net.URL



object SimpleHTTPServer {
  /**
   * @param args
   */
  def main(args:Array[String])  {
    MemXXXServer.xxx_main(false, "com.zotoh.frwk.net.SimpleHTTPServer", args)
  }
}

/**
 * @author kenl
 *
 */
class SimpleHTTPServer(vdir:String, host:String, port:Int) extends MemHTTPServer(vdir,host,port)  {

  /**
   * @param vdir
   * @param key
   * @param pwd
   * @param host
   * @param port
   */
  def this(vdir:String, key:URL, pwd:String, host:String, port:Int) {
    this(vdir, host,port)
    setKeyAuth(key,pwd)
  }


}
