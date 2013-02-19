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
package util

import java.util.{Date=>JDate,Calendar}
import java.sql.{Timestamp=>JTStamp}
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.SimpleTimeZone
import java.util.{TimeZone=>JTZone}

import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.lang3.{StringUtils=>STU}


/**
 * @author kenl
 *
 */
object DateUtils extends Constants with CoreImplicits {

  def hasTZPart(ds:String) = {
    val tkns= if (ds.has(':')) {
      ds.split(TS_REGEX)
    } else {
      ds.split(DT_REGEX)
    }
    tkns.exists { (s) =>
      hasWithin(nsb(s), Array("+","-")) || nsb(s).matches("\\s*[A-Z]+\\s*")
    }
  }

  /**
   * Convert string into a valid Timestamp object.
   *
   * @param t conforming to the format "yyyy-mm-dd hh:mm:ss.[fff...]"
   * @return null if bad data.
   */
  def parseTimestamp(t:String): Option[JTStamp] =
    try { Some(JTStamp.valueOf(t)) } catch { case e:Throwable => None }

  /**
   * Parses datetime in ISO8601 format.
   *
   * @param t string content adhering to ISO8601.
   * @return None if bad data.
   */
  def parseDate(t:String): Option[JDate] = {
    if ( STU.isEmpty(t)) None else {
      val fmt = if (t.has(':') ) {
        val s= if (t.has('.')) DT_FMT_MICRO else DT_FMT
        if (hasTZPart(t)) {
          s+"Z"
        } else { s }
      } else {
        DATE_FMT
      }
      parseDate(t, fmt)
    }
  }

  /**
   * Convert string into a Date object.
   *
   * @param t
   * @param fmt the expected format.
   * @return null if bad data.
   */
  def parseDate(t:String, fmt:String): Option[JDate] = {
    if (STU.isEmpty(t) || STU.isEmpty(fmt)) None else {
      val d= new SimpleDateFormat(fmt).parse(t, new ParsePosition(0))
      if (d==null) None else Some(d)
    }
  }

  /**
   * Convert Timestamp into a string value.
   *
   * @param t
   * @return
   */
  def fmtTimestamp(t:JTStamp) = if( t==null ) "" else t.toString

  /**
   * Convert Date object into a string - GMT timezone.
   *
   * @param t
   * @return
   */
  def fmtDateGMT(t:JDate) =
    fmtDate(t, DT_FMT_MICRO, Some(new SimpleTimeZone(0, "GMT")) )


  /**
   * Convert Date into string value.
   *
   * @param t
   * @param fmt expected format.
   * @return
   */
  def fmtDate(t:JDate, fmt:String): String =  fmtDate(t, fmt, None)

  /**
   * Convert Date into string value,
   * using the built-in format "yyyy-MM-dd'T'HH:mm:ss.SSS".
   *
   * @param t
   * @return
   */
  def fmtDate(t:JDate): String = fmtDate(t, DT_FMT_MICRO, None)


  /**
   * Convert Date into its string value.
   *
   * @param dt
   * @param pattern expected format.
   * @param tz timezone used.
   * @return
   */
  def fmtDate(dt:JDate, pattern:String, tz:Option[JTZone]): String = {
    if (dt==null || STU.isEmpty(pattern)) "" else {
      val fmt = new SimpleDateFormat(pattern)
      tz match { case Some(x) => fmt.setTimeZone(x); case _ => }
      fmt.format(dt)
    }
  }


  /**
   * @param d
   * @param yrs
   * @return
   */
  def addYears(d:JDate, yrs:Int) = add(d, Calendar.YEAR, yrs)

  /**
   * @param d
   * @param mts
   * @return
   */
  def addMonths(d:JDate, mts:Int) = add(d, Calendar.MONTH, mts)

  /**
   * @param d
   * @param days
   * @return
   */
  def addDays(d:JDate, days:Int) = add(d, Calendar.DAY_OF_YEAR, days)

  def dbgCal(cal:Calendar) = {
    import Calendar._
    "{" + cal.getTimeZone().getDisplayName() + "} " + 
    "{" + cal.getTimeZone().getID() + "} " + 
    "[" + cal.getTimeInMillis() + "] " +
    cal.get(YEAR) + "/" + 
   ( cal.get(MONTH) +1 ) + "/" + 
    cal.get(DAY_OF_MONTH) + " " + 
    cal.get(HOUR_OF_DAY) + ":" +
//    cal.get(HOUR) + ":" +
    cal.get(MINUTE) + ":" +
    cal.get(SECOND)
  }
  
  private def add(d:JDate, calendarField:Int, amount:Int) = {
    if (d!= null) {
      val c = Calendar.getInstance
      c.setTime(d)
      c.add(calendarField, amount)
      c.getTime
    } else {
      d
    }
  }

}

