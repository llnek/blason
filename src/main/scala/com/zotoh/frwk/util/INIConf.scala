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

import scala.collection.mutable
import java.io.{IOException,FileReader,LineNumberReader,PrintStream}
import org.slf4j._
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import org.apache.commons.lang3.{StringUtils=>STU}
import java.io.File


object INIConf {
  private val _log= LoggerFactory.getLogger(classOf[INIConf])
}

/**
 * Config class that can parse a MS-Windows style .INI file.
 *
 * @author kenl
 *
 */
@SerialVersionUID(-873895734543L)
class INIConf(private val _iniFile:String) extends CoreImplicits with Serializable {

  private var _secs:mutable.LinkedHashMap[String, Section] = null
  def tlog() = INIConf._log

  parse(_iniFile)

  def this(fp:File) {
    this( niceFPath(fp))
  }

  /**
   * @param section
   * @return
   */
  def section(sn:String): Option[ Map[String,String] ] = {
    if (sn==null) None else ncFind(_secs,sn ) match {
      case Some(x) =>Some(x.toMap)
      case _ => None
    }
  }

  /**
   * @return
   */
  def sections() = _secs.keySet.toSeq

  /**
   * @param section
   * @param key
   * @return
   */
  def getStr(sn:String, key:String) = {
    section(sn) match {
      case Some(m) => m.get(key)
      case _ => None
    }
  }

  /**
   * @param section
   * @param key
   * @return
   */
  def getInt(section:String, key:String) = {
    getStr(section, key) match {
      case Some(s) => Some( s.toInt )
      case _ => None
    }
  }

  /**
   * @param ps
   */
  def dbgShow(ps:PrintStream) {
    _secs.foreach { (t) =>
      ps.println("[" + t._1 + "]")
      t._2.foreach { (a) => ps.println(a._1 + "=" + a._2) }
    }
  }

  /**
   * @param iniFilePath
   */
  protected def parse(iniFilePath:String) {
    val rdr= new LineNumberReader(new FileReader(iniFilePath))
    val secs= mutable.LinkedHashMap[String,Section]()
    var kvs:Section= null
    val ex= () =>
      throw new IOException("Bad INI line: " + rdr.getLineNumber)
    var line=""
    var s=""
    do {
      line = rdr.readLine
      if (line != null) {
        line.trim match {
          case ln if STU.isEmpty(ln) || ln.startsWith("#") =>
          case ln if ln.matches("^\\[.*\\]$") =>
            s = STU.trim( STU.strip(ln, "[]"))
            if ( STU.isEmpty(s)) { ex }
            ncFind(secs, s) match {
              case Some(x) => kvs=x
              case None =>
                kvs= new Section()
                secs += Tuple2(s,kvs)
            }
          case ln if (kvs != null) =>
            var pos=ln.indexOf('=')
            if (pos>0) {
              s= ln.substring(0, pos).trim
              if ( STU.isEmpty(s)) { ex}
              kvs += Tuple2(s, ln.substring(pos + 1).trim )
            } else {
              ex
            }
        }
      }
    } while (line != null)
    _secs = secs
  }

  private def ncFind(m:mutable.LinkedHashMap[String,Section], key:String) = {
    m.find((t) => key.eqic(t._1)) match {
      case Some(t) => Some(t._2)
      case _ => None
    }
  }

}

sealed class Section() extends mutable.LinkedHashMap[String,String] {}

