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
class OracleImpl extends DBDriver {

//private Map<String,FldMetaHolder> _ids= MP();

  override def getStringKeyword() = "VARCHAR2"

  override def genDrop(tbl:String ) = {
    new StringBuilder(256).append("DROP TABLE ").
      append(tbl).
      append(" CASCADE CONSTRAINTS PURGE").
      append(genExec).append("\n\n").toString
  }

  override def getTSDefault() = "DEFAULT SYSTIMESTAMP"

  override def getLongKeyword() = "NUMBER(38)"

  override def getDoubleKeyword() = "BINARY_DOUBLE"

  override def getFloatKeyword() = "BINARY_FLOAT"

  override def genAutoInteger(table:String , fld:FldMetaHolder ) = {
    _ids.put(table, fld)
    genInteger(fld)
  }

  override def genAutoLong(table:String , fld:FldMetaHolder ) = {
    _ids.put(table, fld) 
    genLong(fld)
  }

  override  def genEndSQL() = {
    val sql = _ids.foldLeft(new StringBuilder ) { (b, t) =>
      b.append( create_sequence(t._1))
      b.append( create_sequence_trigger(t._1, t._2.getId))
      b
    }
    sql.toString
  }

  override def create_sequence(table:String ) = {
    "CREATE SEQUENCE SEQ_" + table +  
          " START WITH 1 INCREMENT BY 1" + 
          genExec + "\n\n"
  }

  private def create_sequence_trigger(table:String , key:String ) = {
    "CREATE OR REPLACE TRIGGER TRIG_" + table + "\n" +
      "BEFORE INSERT ON " + table + "\n" + 
      "REFERENCING NEW AS NEW\n" + 
      "FOR EACH ROW\n" + 
      "BEGIN\n" + 
      "SELECT SEQ_" + table + ".NEXTVAL INTO :NEW." + key + " FROM DUAL;\n" +
      "END" + genExec() + "\n\n"
  }

}
