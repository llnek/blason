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

import java.util.{Date=>JDate}
import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.db.JDBCUtils._
import java.util.Calendar
import java.util.TimeZone


@Table(table="TBL_PERSON")
class Person extends AbstractModel {
  
  def dbio_getFirst_column="first_name"
  @Column(optional=false)
  def getFirst() = {
    readData( dbio_getFirst_column) match {
      case Some(v) => nsb(v)
      case _ => ""
    }
  }
  def setFirst(n:String ) {
    writeData( dbio_getFirst_column, Option(n))
  }
  
  def dbio_getLast_column="last_name"
  @Column(optional=false)
  def getLast() = {
    readData( dbio_getLast_column) match {
      case Some(v) => nsb(v)
      case _ => ""
    }
  }
  def setLast(n:String ) {
    writeData( dbio_getLast_column, Option(n))
  }

  def dbio_getIQ_column = "iQ"
  @Column()
  def getIQ() = {
    readData( dbio_getIQ_column ) match {
      case Some(v) => javaToSQLInt(v)
      case _ => 0
    }
  }
  def setIQ(n:Int) {
    writeData( dbio_getIQ_column , Option(n))
  }

  def dbio_getBDay_column = "bday"
  @Column(optional=false)
  def getBDay() = {
    readData( dbio_getBDay_column ) match {
      case Some(v) =>
        val tz = nsb ( readData(dbio_getBDay_column + "_tz").getOrElse("GMT") )
        javaToCalendar(v, TimeZone.getTimeZone(tz) )
      case _ => null
    }
  }
  def setBDay(d:Calendar) {
    writeData( dbio_getBDay_column+"_tz" , Option(d.getTimeZone().getID() ) )
    writeData( dbio_getBDay_column , Option(d))
  }

  def dbio_getSex_column= "sex"
  @Column(optional=false,size=8)
  def getSex() = {
    readData( dbio_getSex_column) match {
      case Some(v) => nsb(v)
      case _ => ""
    }
  }
  def setSex(x:String ) {
    writeData( dbio_getSex_column, Option(x))
  }


  def dbio_getSpouse_fkey = "FK_SPOUSE"
  @One2One(rhs=classOf[Person] )
  def getSpouse[T <: Person](db:SQLProc, rhs:Class[T] ): Option[T] = {
    db.getO2O(this, rhs, dbio_getSpouse_fkey  )
  }
  def setSpouse[T <: Person](db:SQLProc, p:T ) {
    db.setO2O(this, p, dbio_getSpouse_fkey )
  }



}
