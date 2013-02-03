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
package db

import scala.collection.mutable

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreImplicits


/**
 * Holds database table information.
 *
 * @author kenl
 *
 */
class TableMetaHolder(private val _table:String) extends CoreImplicits {

  private val _keys= mutable.HashMap[String, ColMetaHolder]()
  private val _cols= mutable.HashMap[String, ColMetaHolder]()
  private var _supportsGetGeneratedKeys = false
  private var _supportsTransactions =false

  /**
   * @return
   */
  def canGetGeneratedKeys() = _supportsGetGeneratedKeys

  /**
   * @return
   */
  def canTransact() = _supportsTransactions

  /**
   * @return
   */
  def getName() = _table

  /**
   * @param b
   */
  def setGetGeneratedKeys(b:Boolean) {
    _supportsGetGeneratedKeys=b
  }

  /**
   * @param b
   */
  def setTransact(b:Boolean) {
    _supportsTransactions=b
  }

  /**
   * @param c
   * @param isKey
   */
  def addCol(c:ColMetaHolder, isKey:Boolean) {
    val cn= c.getName().uc
    _cols.put(cn, c)
    if (isKey) {
        _keys.put(cn, c)
    }
  }

  /**
   * @param c
   */
  def addCol(c:ColMetaHolder ) {
    addCol(c, false)
  }

  /**
   * @param n
   * @return
   */
  def getColMeta(n:String):Option[ColMetaHolder] = {
    if (STU.isEmpty(n)) None else _cols.get(n.uc)
  }

  /**
   * @param cn
   * @return
   */
  def hasCol(cn:String) = {
    if (STU.isEmpty(cn)) false else _cols.isDefinedAt(cn.uc)
  }

  /**
   * @return
   */
  def getColMetas() = _cols.toMap

  /**
   * @return
   */
  def getKeys() = _keys.toMap

  /**
   * @return
   */
  def getKeysAsArray() = _keys.values.toArray


}
