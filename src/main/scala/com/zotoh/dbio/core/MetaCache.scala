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
package meta

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException
import org.slf4j._
import com.zotoh.frwk.utils.CoreImplicits
import com.zotoh.frwk.db.DBUtils
import org.apache.commons.lang3.{StringUtils=>STU}


object MetaCache {
  private val _log=LoggerFactory.getLogger(classOf[MetaCache])
  /*
  val COL_ROWID= "II_ROWID"
  val COL_VERID= "II_VERID"
  val COL_RHS= "II_RHS"
  val COL_LHS= "II_LHS"
  val COL_RHSOID= "II_RHSOID"
  val COL_LHSOID= "II_LHSOID"
  */
}

/**
 *  This cache holds database table information, and annotated class
 *  information.
 *
 * @author kenl
 *
 */
sealed class MetaCache extends CoreImplicits {

  private val _classes = mutable.HashMap[Class[_], ClassMetaHolder]()
  private val _meta= mutable.HashMap[String, TableMetaHolder]()

  def tlog() = MetaCache._log
  import MetaCache._

  loadClassMeta(classOf[M2MTable])

  def getTableMeta( con:Connection,table:String ) = {
    var m= getTableMeta(table)
    if (m.isEmpty && con != null) {
      m= loadTableMeta(con,table)
    }
    m
  }

  def getTableMeta( table:String ) = {
    if (STU.isEmpty(table)) None else _meta.get(table.lc)
  }

  def getClassMeta( target:Class[_]) = {
    var rc= _classes.get(target)
    if (rc.isEmpty)
      rc= loadClassMeta(target)
    }
    rc
  }

  private def loadClassMeta( z:Class[_]) = synchronized {
    try {
      val rc= new ClassMetaHolder().scan(z)
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

}
