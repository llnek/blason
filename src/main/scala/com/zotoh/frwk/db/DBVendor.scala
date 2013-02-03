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

import org.apache.commons.lang3.{StringUtils=>STU}

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreImplicits


/**
 * Constants for list of common db vendors.
 *
 * @author kenl
 *
 */

object DBVendor extends Constants with CoreImplicits {

  val POSTGRESQL=DBVendor(S_POSTGRESQL)
  val SQLSERVER=DBVendor( S_MSSQL) // .setTestSQL("select count(*) from sysusers")
  val ORACLE=DBVendor(S_ORACLE).setTestSQL("select 1 from DUAL")//"select count(*) from user_tables")
  val MYSQL=DBVendor(S_MYSQL) //.setTestSQL("select version()")
  val H2=DBVendor(S_H2)
  val HSQLDB=DBVendor(S_HSQLDB)
  val DB2=DBVendor(S_DB2).setTestSQL("select count(*) from sysibm.systables")
  val DERBY=DBVendor(S_DERBY)
  val NOIDEA=DBVendor("?")

  /**
   * @param s
   * @return
   */
  def fromString(s:String) = {
    nsb(s) match {
      case s if ( s.eqic(S_POSTGRESQL)) => POSTGRESQL
      case s if ( s.eqic(S_MSSQL)) => SQLSERVER
      case s if ( s.eqic(S_H2)) => H2
      case s if ( s.eqic(S_HSQLDB)) => HSQLDB
      case s if ( s.eqic(S_MYSQL)) => MYSQL
      case s if ( s.eqic(S_ORACLE)) => ORACLE
      case s if ( s.eqic(S_DB2)) => DB2
      case s if ( s.eqic(S_DERBY)) => DERBY
      case _ => NOIDEA
    }

  }

}

sealed case class DBVendor protected(private var _prod:String) extends Constants with CoreImplicits {

  private var _test:String = "select 1"
  private var _ver:String = "?"
  private var _id=_prod
  private var _upper=true
  private var _lower=false
  private var _mixed=false
  import DBVendor._

  /**
   * @param n
   */
  def productName_=(n:String) { _prod = n }

  /**
   * @return
   */
  def productName = _prod

  /**
   * @param v
   */
  def productVer_=(v:String) { _ver= v }

  /**
   * @return
   */
  def productVer = _ver

  override def toString() = _id

  /**
   * @param sql
   * @return
   */
  def tweakSQL(sql:String) = {
    nsb(sql).lc match {
      //case s if (s.has("insert") && s.has("into"))  =>
      case s if (s.has("update") && s.has("set")) => tweakUPDATE(sql)
      case s if (s.has("delete") )  => tweakDELETE(sql)
      case s if (s.has("select") && s.has("from")) => tweakSELECT(sql)
      case _ => sql
    }
  }

  /**
   * @param sql
   * @return
   */
  def tweakSELECT(sql:String) = {
    if ( SQLSERVER eq this) tweakMSSQL(sql, "where", NOLOCK) else sql
  }

  /**
   * @param sql
   * @return
   */
  def tweakUPDATE(sql:String) = {
    if ( SQLSERVER eq this) tweakMSSQL(sql, "set", ROWLOCK) else sql
  }

  /**
   * @param sql
   * @return
   */
  def tweakDELETE(sql:String) = {
    if ( SQLSERVER eq this ) tweakMSSQL(sql, "where", ROWLOCK) else sql
  }

  /**
   * @param upper
   * @param lower
   * @param mixed
   */
  def setCase(upper:Boolean, lower:Boolean, mixed:Boolean) {
    _upper=upper; _lower=lower; _mixed=mixed
  }

  /**
   * @param table
   * @return
   */
  def assureTableCase(table:String) = {
    if (_upper) { table.uc } else if (_lower) { table.lc } else { table }
  }

  /**
   * @param col
   * @return
   */
  def assureColCase(col:String) = {
    if (_upper) { col.uc } else if (_lower) { col.lc } else { col }
  }

  /**
   * @return
   */
  def isUpperCase() = _upper

  /**
   * @return
   */
  def isLowerCase() = _lower

  /**
   * @return
   */
  def isMixedCase() = _mixed

  def setTestSQL(s:String) = { _test = s; this }

  def getTestSQL() = _test

  private def tweakMSSQL(sql:String, token:String, cmd:String) = {

    if ( ! STU.isEmpty(sql)) {
      val pos = sql.lc.indexOf(token)
      var head=sql
      var tail=""
      if (pos >= 0) {
        head = sql.substring(0, pos)
        tail = sql.substring(pos)
      }
      head + " WITH (" + cmd + ") " + tail
    } else {
      sql
    }
  }

}

