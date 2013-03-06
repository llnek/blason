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
package sql

import com.zotoh.dbio.core.FldMetaHolder



/**
 * @author kenl
 *
 */
class PostgreSQLImpl extends DBDriver {

  override def getTSKeyword() = "TIMESTAMP WITH TIME ZONE"

  override def getBlobKeyword() = "BYTEA"

  override def getDoubleKeyword() = "DOUBLE PRECISION"

  override def getFloatKeyword() = "REAL" //"FLOAT(23)"

  override def genDrop(tbl:String ) = {
    new StringBuilder(256).append("DROP TABLE IF EXISTS ").
      append(tbl).
      append(" CASCADE").
      append(genExec()).append("\n\n").toString
  }

  override def genAutoInteger(table:String , fld:FldMetaHolder ) = {
    new StringBuilder(256).
      append(getPad).append(fld.getId ).append(" ").
      append( "SERIAL" ).toString
  }

  override def genAutoLong(table:String , fld:FldMetaHolder ) = {
    new StringBuilder(256).
      append(getPad).append(fld.getId ).append(" ").
      append( "BIGSERIAL" ).toString
  }


}
