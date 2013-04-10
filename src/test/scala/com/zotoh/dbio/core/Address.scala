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

import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.StrUtils._




@Table(table="TBL_ADDRESS")
class Address extends AbstractModel {

  def dbio_getAddr1_column = "ADDR1"
  @Column(data=classOf[String])
  def getAddr1() = {
    readData(dbio_getAddr1_column ) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setAddr1(s:String) {
    writeData(dbio_getAddr1_column, Option(s))
  }

  def dbio_getAddr2_column= "ADDR2"
  @Column(data=classOf[String])
  def getAddr2()= {
    readData(dbio_getAddr2_column ) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setAddr2(s:String ) {
    writeData(dbio_getAddr2_column, Option(s))
  }

  def dbio_getCity_column= "CITY"
  @Column(data=classOf[String],size=128)
  def getCity() = {
    readData(dbio_getCity_column) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setCity(s:String ) {
    writeData(dbio_getCity_column, Option(s))
  }

  def dbio_getState_column= "STATE"
  @Column(data=classOf[String],size=128)
  def getState() = {
    readData( dbio_getState_column) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setState(s:String ) {
    writeData( dbio_getState_column, Option(s))
  }


  def dbio_getZip_column= "ZIP"
  @Column(data=classOf[String],size=64)
  def getZip() = {
    readData( dbio_getZip_column) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setZip(s:String ) {
    writeData( dbio_getZip_column, Option(s))
  }


  def dbio_getCountry_column = "COUNTRY"
  @Column(data=classOf[String],size=128)
  def getCountry() = {
    readData( dbio_getCountry_column ) match {
    case Some(v) => nsb(v)
    case _ => ""
    }
  }
  def setCountry(s:String ) {
    writeData( dbio_getCountry_column , Option(s))
  }


}
