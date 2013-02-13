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


/**
 * @author kenl
 *
 */
@Table(table="DBIO_MMTABLE")
class M2MTable extends DBPojo {

  private long _rhsObj, _lhsObj;
  private String _lhs, _rhs;

  @Column(id="XX_LHS", unique=true)
  def getLHS() = lhs
  def setLHS(s:String ) {
    _lhs=s
  }

  @Column(id="XX_RHS", unique=true)
  def getRHS() = _rhs
  def setRHS(s:String ) {
    _rhs=s
  }

  @Column(id="XX_LHSOID", unique=true)
  def getLhsObjID() = _lhsObj
  def setLhsObjID(id:Long) {
    _lhsObj=id
  }

  @Column(id="XX_RHSOID", unique=true)
  def getRhsObjID() = _rhsObj
  def setRhsObjID(id:Long ) {
    _rhsObj=id
  }


}
