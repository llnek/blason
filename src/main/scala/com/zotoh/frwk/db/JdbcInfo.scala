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

import com.zotoh.frwk.util.StrUtils._
import org.slf4j._
import java.sql.Connection

object JdbcInfo {
  private val _log = LoggerFactory.getLogger(classOf[JdbcInfo])
}

/**
 * @author kenl
 *
 */
@SerialVersionUID(6871654777100857463L)
class JdbcInfo(
  private var _user:String, private var _pwd:String,
  private var _url:String,
  private var _driver:String="")  extends Serializable {

  private var _isolation= Connection.TRANSACTION_READ_COMMITTED
  import JdbcInfo._
  
  def tlog() = _log

  _driver=nsb(_driver)
  _user=nsb(_user)
  _url=nsb(_url)
  _pwd=nsb(_pwd)

  def this() {
    this("","","","")
  }

  /**
   *
   * @return
   */
  def isolation = _isolation

  /**
   * @param n
   */
  def isolation_=(n:Int) { _isolation= n }

  /**
   * @param driver
   */
  def driver_=(d:String ) { _driver= nsb(d) }

  /**
   * @param url
   */
  def url_=( u:String) { _url= nsb(u) }

  /**
   * @param user
   */
  def user_=( u:String) { _user= nsb(u) }

  /**
   * @param pwd
   */
  def pwd_=( p:String) { _pwd= nsb(p) }

  /**
   * @return
   */
  def driver = _driver

  /**
   * @return
   */
  def url = _url

  /**
   * @return
   */
  def user = _user

  /**
   * @return
   */
  def pwd = _pwd

  override def toString() = {
    "Driver: " + driver + ", Url: " + url + ", User: " + user + "Pwd: ****"
  }

}

