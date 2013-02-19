/*??
 * COPYRIGHT (C) 2012-13 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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
package sql

import com.zotoh.dbio.core.FldMetaHolder



/**
 * @author kenl
 *
 */
class H2Impl extends DBDriver {

  override def getBlobKeyword() = "BLOB"

  override def getDoubleKeyword() = "DOUBLE"

  override def getFloatKeyword() = "FLOAT"

  override def genDrop(tbl:String ) = {
    new StringBuilder(256).append("DROP TABLE ").
      append(tbl).
      append(" IF EXISTS CASCADE").
      append(genExec).append("\n\n").toString
  }

  override def genBegin(tbl:String ) = {
    new StringBuilder(256).append("CREATE CACHED TABLE ").
      append(tbl).
      append("\n(\n").toString
  }

  override def genAutoInteger( table:String , fld:FldMetaHolder ) = {
    val t= getIntKeyword
    val col= fld.getId
    new StringBuilder(256).
    append(getPad).append(col ).append(" ").
    append( t).
    append( if (fld.isPK) " IDENTITY( 1 ) " else " AUTO_INCREMENT( 1 ) " ).toString
  }

  override def genAutoLong( table:String , fld:FldMetaHolder ) = {
    val t= getLongKeyword
    val col= fld.getId
    new StringBuilder(256).
    append(getPad()).append(col ).append(" ").
    append( t).
    append( if (fld.isPK) " IDENTITY( 1 ) " else " AUTO_INCREMENT( 1 ) " ).toString
  }

  override protected def getDateKeyword() = "TIMESTAMP"

}
