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
 * 
 * @author kenl
 *
 */
public class Reaper extends Coroutine {

  private volatile boolean _active=true;
  private Crop _crop;
  private int _delayMillis;

  public Reaper(Crop c, int delayMillis) {
    _crop=c;
    _delayMillis= delayMillis;
  }

  public Reaper(Crop c) {
    this(c, 0);
  }

  public void stop() {
    _active = false;
  }

  public void run() {

    while (_active) {
      try { 
        Thread.sleep(_delayMillis);
        _crop.reap();
      } catch (Throwable t)
      {}
      if (_delayMillis == 0) {
        stop();
      }
    }

  }

}
