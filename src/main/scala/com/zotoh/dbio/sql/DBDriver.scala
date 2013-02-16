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

import org.apache.commons.lang3.{StringUtils=>STU}
import java.sql.SQLException
import java.util.{TreeSet=>JTreeSet,Date=>JDate}
import com.zotoh.frwk.util.MetaUtils._
import com.zotoh.frwk.db.DBVendor._
import com.zotoh.dbio.meta._
import com.zotoh.frwk.db.DBVendor
import java.util.Arrays
import com.zotoh.dbio.core.ClassMetaHolder
import com.zotoh.dbio.core.FldMetaHolder
import com.zotoh.dbio.core.MetaCache
import com.zotoh.dbio.core.AssocMetaHolder
import com.zotoh.dbio.core.M2MTable
import com.zotoh.dbio.core.Schema
import com.zotoh.dbio.core.DBPojo




object DBDriver {
  private val S_DDLSEP= "-- :"

  def newDriver(v:DBVendor ) = {

    v match {
      case POSTGRESQL => new PostgreSQLImpl()
      case H2 => new H2Impl()
      case MYSQL => new MySQLImpl()
      case SQLSERVER => new SQLSvrImpl()
      case ORACLE => new OracleImpl()
      case DERBY => new DerbyImpl()
      case DB2 => new DB2Impl()
      case _ => throw new SQLException("Unsupported database: " + v)
    }

  }

}


/**
 * @author kenl
 *
 */
abstract class DBDriver protected() {

  import DBDriver._

  protected var _meta= new MetaCache( new Schema { def getModels = Nil } )
  protected var _useSep = true

  def withSchema(s:Schema): this.type = {
    _meta=new MetaCache(s)
    this
  }
  
  def getDDL() = {

//    val arr= List[Class[_]]( classOf[M2MTable] ) ++ classes
    val body= new StringBuilder(1024)
    val drops= new StringBuilder(512)
    
    _meta.getClassMetas.foreach { (c) =>
        drops.append( genDrop(c._2.getTable() ))
        body.append( genOneClass(c._2))
    }

    "" + drops + body + genEndSQL()
  }

  protected def genOneClass(zm:ClassMetaHolder ) = {
    val n= zm.getTable()
    if (STU.isEmpty(n)) "" else {
      genOneTable(n, zm.getFldMetas, MetaCache.getAssocMetas )
    }
  }

  protected def genOneTable(table:String, cols:Map[String,FldMetaHolder], 
      assocs:Map[String,AssocMetaHolder] )  = {
    
    val ddl= new StringBuilder(10000)
    val inx= new StringBuilder(256)
    //ddl.append( genDrop(table))
    ddl.append( genBegin(table))
    ddl.append(genBody(table, cols, assocs, inx))
    ddl.append(genEnd)
    if (inx.length() > 0) {
      ddl.append(inx.toString)
    }
    ddl.append( genGrant(table))
    ddl.toString()
  }

  protected def genDrop( tbl:String ) = {
    new StringBuilder(256).append("DROP TABLE ").append(tbl).append(genExec).append("\n\n").toString
  }

  protected def genBegin( tbl:String )  = {
    new StringBuilder(256).append("CREATE TABLE ").append(tbl).append("\n(\n").toString
  }

  protected def genBody(table:String, cols:Map[String,FldMetaHolder],
    assocs:Map[String,AssocMetaHolder], inx:StringBuilder) = {

    val pkeys= new JTreeSet[String]()
    val keys= new JTreeSet[String]()
    val bf= new StringBuilder(512)

    cols.foreach { (en) =>
      val fld= en._2
      val cn= en._1
      var col=""
      val dt= fld.getColType

      if (fld.isPK) { pkeys.add(cn) }
      else
      if (fld.isUniqueKey) { keys.add(cn) }

      if ( isBoolean(dt)) { col= genBool(fld) }
      else if ( classOf[java.sql.Timestamp] == dt ) { col= genTimestamp(fld) }
      else if ( classOf[JDate] == dt) { col= genDate(fld) }
      else if ( isInt(dt)) {
        col= if (fld.isAutoGen) genAutoInteger(table, fld) else genInteger(fld)
      }
      else if ( isLong(dt)) {
        col= if (fld.isAutoGen) genAutoLong(table, fld) else genLong(fld)
      }
      else if ( isDouble(dt)) { col= genDouble(fld) }
      else if ( isFloat(dt)) { col= genFloat(fld) }
      else if ( isString(dt)) { col= genString(fld) }
      else if ( isBytes(dt)) { col= genBytes(fld) }

      if (! STU.isEmpty(col)) {
        if (bf.length() > 0) { bf.append(",\n") }
        bf.append(col)
      }
    }
    
    val asoc= assocs.get(table)
    inx.setLength(0)
    var iix=1
    if (asoc.isDefined) asoc.get.getInfo.foreach { (t) =>
      val cn = t._3.toUpperCase()
      val col = genColDef(cn, getLongKeyword() , true)
      if (bf.length() > 0) { bf.append(",\n") }
      bf.append(col)
      inx.append( "CREATE INDEX " + table + "_IDX_" + iix + " ON " + table + 
        " ( "  + cn + " )" + genExec + "\n\n" )
      iix += 1
    }

    if (bf.length() > 0) {
      var s= if(pkeys.size ==0 ) "" else genPrimaryKey(pkeys.toArray )
      if ( !STU.isEmpty(s)) {
        bf.append(",\n").append(s)
      }
      s= if (keys.size == 0) "" else genUniques(keys.toArray)
      if ( !STU.isEmpty(s)) {
        bf.append(",\n").append(s)
      }
    }

    bf.toString()
  }

  protected def genEnd() = {
    new StringBuilder(256).append("\n)").append(genExec).append("\n\n").toString
  }

  protected def genGrant(tbl:String ) = ""

  protected def genEndSQL() = ""

  protected def genPrimaryKey(keys:Array[Object]) = {
    Arrays.sort(keys)
    val b=keys.foldLeft(new StringBuilder) { (b,k) =>
      if (b.length() > 0) { b.append( ",") }
      b.append(k)
    }
    getPad() + "PRIMARY KEY(" + b + ")"
  }

  protected def genUniques(keys:Array[Object]) = {
    Arrays.sort(keys)
    val b=keys.foldLeft(new StringBuilder) { (b,k) =>
      if (b.length() > 0) { b.append(",") }
      b.append(k)
    }
    getPad() + "UNIQUE(" + b + ")"
  }

  protected def genColDef(col:String , ty:String , optional:Boolean) = {
    new StringBuilder(256).
      append(getPad).append(col ).append(" ").
      append( ty).
      append(" ").
      append(nullClause( optional )).toString
  }

  protected def genBytes(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getBlobKeyword, fld.isNullable )
  }

  protected def genString(fld:FldMetaHolder ) = {
    genColDef(fld.getId,
      getStringKeyword + "(" + fld.getSize().toString + ")",
      fld.isNullable )
  }

  protected def genInteger(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getIntKeyword, fld.isNullable)
  }

  protected def genAutoInteger(table:String , fld:FldMetaHolder ) = ""

  protected def genDouble(fld:FldMetaHolder )  = {
    genColDef(fld.getId, getDoubleKeyword, fld.isNullable)
  }

  protected def genFloat(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getFloatKeyword, fld.isNullable)
  }

  protected def genLong(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getLongKeyword, fld.isNullable)
  }

  protected def genAutoLong(table:String , fld:FldMetaHolder ) = ""

  protected def genTimestamp(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getTSKeyword, fld.isNullable)
  }

  protected def genDate(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getDateKeyword, fld.isNullable)
  }

  protected def genBool(fld:FldMetaHolder ) = {
    genColDef(fld.getId, getBoolKeyword, fld.isNullable)
  }

  protected def getTSDefault() = "DEFAULT CURRENT_TIMESTAMP"

  protected def getPad() = "    "

  protected def getFloatKeyword() = "FLOAT"

  protected def getIntKeyword() = "INTEGER"

  protected def getTSKeyword() = "TIMESTAMP"

  protected def getDateKeyword() = "DATE"

  protected def getBoolKeyword() = "INTEGER"

  protected def getLongKeyword() = "BIGINT"

  protected def getDoubleKeyword() = "DOUBLE PRECISION"

  protected def getStringKeyword() = "VARCHAR"

  protected def getBlobKeyword() = "BLOB"

  protected def nullClause(opt:Boolean ) = {
    if (opt) getNull else getNotNull
  }

  protected def getNotNull() = "NOT NULL"

  protected def getNull() = "NULL"

  protected def genExec() = ";\n" + genSep()

  protected def genSep() = {
    if (_useSep) S_DDLSEP else ""
  }

}
