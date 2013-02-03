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

package com.zotoh.jmx.support;

import javax.management.MBeanOperationInfo;

/**
 * 
 * @author kenl
 *
 */
public enum JMXBeanAction {

  READ_WRITE(MBeanOperationInfo.ACTION_INFO),
  WRITE(MBeanOperationInfo.ACTION),
  READ(MBeanOperationInfo.INFO),
  UNKNOWN(MBeanOperationInfo.UNKNOWN)

  ;

  public static JMXBeanAction fromValue(int v) {
    for (JMXBeanAction a : values()) {
      if (a._value == v) {
        return a;
      }
    }
    return UNKNOWN;
  }

  public int infoValue() {
    return _value;
  }

  private JMXBeanAction(int v) {
    _value=v;
  }

  private final int _value;
}

