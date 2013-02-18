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

import org.apache.commons.lang3.{StringUtils=>STU}

import java.lang.reflect.Method
import com.zotoh.dbio.meta.Column

object FldMetaHolder {
  val DUMBO= new FldMetaHolder("",null)
}

/**
 * @author kenl
 *
 */
class FldMetaHolder(private val _name:String, private val _col:Column) {

  private val _mtds = new Array[Method](2)

  def getGetter() = _mtds(0)
  def getSetter() = _mtds(1)

  def setGetter(m:Method ) {
    _mtds(0)=m
  }

  def setSetter(m:Method ) { _mtds(1)=m }

  def isIndex() = if (_col == null) false else {
    !STU.isEmpty(_col.index() )
  }

  def getIndexName() = if (_col == null) "" else _col.index()

  def isAutoGen() = if (_col == null) false else _col.autogen()

  def getId() = _name.toUpperCase()

  def isNullable() = if (_col==null) true else _col.optional()

  def getSize() = if (_col==null) 0 else _col.size()

  def getColType() = if (_mtds(0)==null) null else _mtds(0).getReturnType()

  def getDftValue() = if (_col==null) "" else _col.dftValue()
  def getDft() = if (_col==null) false else _col.dft()
  
  def isPK() = false

  def isInternal() = if (_col==null) false else _col.system()
  def isUpdatable() = if (_col==null) false else _col.updatable()
  
}
