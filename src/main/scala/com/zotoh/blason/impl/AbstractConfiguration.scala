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
package impl

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import java.net.URL
import com.zotoh.blason.core.Disposable
import com.zotoh.blason.core.Initializable
import com.zotoh.blason.core.Configuration

/**
 * @author kenl
 */
abstract class AbstractConfiguration(private var _par:Configuration) extends Configuration with Initializable with Disposable {

  def getParent() = if (_par==null) None else Some(_par)

  protected def setParent(p:Configuration) {
    _par=p
  }

}
