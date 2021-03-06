/*??
 * COPYRIGHT (C) 2013 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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

package com.zotoh.xref.util;

/**
 * @author kenl
 */
public class AsyncProc implements Runnable {

  private boolean _daemon=false;
  private Runnable _FC = null;
  private ClassLoader _cl= null;

  public AsyncProc withClassLoader(ClassLoader cl) {
    _cl=cl;
    return this;
  }

  public AsyncProc setDaemon(boolean b) {
    _daemon=b;
    return this;
  }

  public Thread mkThread() {
    Thread t=new Thread(this);
    if (_cl != null) { t.setContextClassLoader(_cl); }
    t.setDaemon(_daemon);
    return t;
  }

  public void run() {
    _FC.run();
  }

  public void fork( Runnable r) {
    _FC=r;
    mkThread().start();
  }

}
