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

import scala.collection.mutable
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.db.DBUtils
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.db.TableMetaHolder


object MetaCache {
  private val _log=LoggerFactory.getLogger(classOf[MetaCache])
}

/**
 *  This cache holds database table information, and annotated class
 *  information.
 *
 * @author kenl
 *
 */
sealed class MetaCache(models:Schema) extends CoreImplicits {

  private val _classes = mutable.HashMap[ Class[_], ClassMetaHolder]()
  private val _meta= mutable.HashMap[ String, TableMetaHolder]()
  private val _assocs= mutable.HashMap[ String,AssocMetaHolder]()
  private val _mms= mutable.HashMap[ String,Class[_] ]()
  def getMMS() = _mms.toMap
  def putMMS(key:String, z:Class[_]) {
    _mms += key -> z
  }
  def getAssocMetas() = _assocs.toMap
  def putAssocMeta(table:String, meta: AssocMetaHolder) {
    _assocs += table -> meta
  }

  def tlog() = MetaCache._log

//  loadClassMeta(classOf[M2MTable])
  models.getModels.foreach( loadClassMeta( _ ) )

  def findJoined(lhs:Class[_], rhs:Class[_]): Class[_] = {
    val rn= throwNoTable( rhs).table().uc
    val ln= throwNoTable( lhs).table().uc
    val jn= sortAndJoin(ln,rn)
    getMMS().get(jn) match {
      case Some(z) => z
      case _ => null
    }
  }
  def findJoined(lhs:DBPojo, rhs:DBPojo): Class[_] = {
    findJoined(lhs.getClass, rhs.getClass)
  }

  def getTableMeta( con:Connection,table:String ): Option[TableMetaHolder]  = {
    var m= getTableMeta(table)
    if (m.isEmpty && con != null) {
      m= loadTableMeta(con,table)
    }
    m
  }

  def getTableMeta( table:String ): Option[TableMetaHolder] = {
    if (STU.isEmpty(table)) None else _meta.get(table.lc)
  }

  def getClassMeta( target:Class[_]) = {
    var rc= _classes.get(target)
    if (rc.isEmpty) {
      rc= Option(loadClassMeta(target))
    }
    rc
  }

  def getClassMetas() = _classes.toMap

  private def loadClassMeta( z:Class[_]) = synchronized {
    try {
      val rc= new ClassMetaHolder(this).scan(z)
      _classes.put(z, rc)
      rc
    } catch {
      case e:Throwable =>
        tlog.warn("Failed to parse Class annotations: " + z.getName, e)
        throw e
    }
  }

  private def loadTableMeta(con:Connection, table:String) = synchronized {
    DBUtils.loadTableMeta(con,table)
  }

  private def throwNoTable(z:Class[_]) = {
    val t= Utils.getTable(z)
    if (t==null) { throw new Exception("" + z + " has no Table annotation." ) }
    t
  }

}
