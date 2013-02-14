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
package model

import com.zotoh.dbio.core.DBPojoBase
import com.zotoh.dbio.meta.Column
import java.util.{Date=>JDate}



/**
 * @author kenl
 */
abstract class AbstractModel extends DBPojoBase {

  @Column(id="LAST_CHANGED",optional=false, system=true)
  def getLastModified() = null
  def setLastModified(t:JDate) {}

  @Column(id="CREATED_ON", optional=false, system=true, dft="current_timestamp")
  def getCreated() = null
  def setCreated(t:JDate) {}

  @Column(id="CREATED_BY")
  def getCreator() = null
  def setCreator(s:String) {}

}
