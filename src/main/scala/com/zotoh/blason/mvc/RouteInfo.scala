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

package com.zotoh.blason
package mvc

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.lang3.{StringUtils=>STU}
import org.slf4j._
import com.zotoh.frwk.util.INIConf
import java.io.File
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import jregex.{Matcher,Pattern}
import java.util.StringTokenizer

/**
 * @author kenl
 */
object RouteInfo {

  private val _log= LoggerFactory.getLogger(classOf[RouteInfo])
  def tlog() = _log

  def loadRoutes(file:File) = {
    val rc= mutable.ArrayBuffer[RouteInfo]()
    val stat= file.getName().startsWith("static-")
    val cf=new INIConf(file)
    cf.sections.foreach { (s) =>
      val r= cf.section(s) match {
        case Some(m) => mkRoute(stat, s, m)
        case _ =>null
      }
      if (r != null) { rc += r }
    }
    rc.toSeq
  }

  private def mkRoute(stat:Boolean, key:String, flds:Map[String,String]) = {
    val tpl=flds.getOrElse("template","")
    val verb=flds.getOrElse("verb","")
    val pipe=flds.getOrElse("pipe","")
    val rc=new RouteInfo(key, verb, pipe)
    if (stat) {
      rc.mountPoint = flds.getOrElse("mount","")
      rc.setStatic(true)
      tstEStrArg("static-route mount point", rc.mountPoint)
    } else {
      tstEStrArg("http method for route", verb)
      tstEStrArg("pipeline for route", pipe)
    }
    if (!STU.isEmpty(tpl)) {
      rc.template = tpl
    }
    rc.initialize()
    rc
  }

  private val DELIM="/"
}

/**
 * @author kenl
 */
class RouteInfo(private var _path:String, private val _verb:String,
private val _pipe:String) extends CoreImplicits {
  import RouteInfo._
  private val _placeholders= mutable.ArrayBuffer[ (Int,String) ]()
  private var _regex:Pattern= null
  private var _staticFile = false
  private var _mountPt=""
  private var _tpl=""
  private val _verbArr= STU.split(_verb, ",;|").map { (s) => s.trim().uc }

  private def initialize() {
    val tknz = new StringTokenizer(_path, DELIM, true)
    val buff= new StringBuilder(512)
    var t=""
    var gn= ""
    var cg=0
    while (tknz.hasMoreTokens ) {
      t=tknz.nextToken()
      if (t == DELIM) { buff.append(DELIM) } else {
        if (t.startsWith(":")) {
          cg += 1
          gn= t.substring(1)
          _placeholders.add( ( cg , gn ) )
          t = "({" + gn + "}[^/]+)"
        } else {
          val c= STU.countMatches(t, "(")
          if (c > 0) {
            cg += c
          }
        }
        buff.append(t)
      }
    }
    tlog.debug("Route added: {}\ncanonicalized to: {}{}", _path, buff,"")
    _path=buff.toString
    _regex= new Pattern(_path)
  }

  def setStatic(b:Boolean): this.type = {
    _staticFile=b
    this
  }
  def isStatic() = _staticFile

  def resemble(mtd:String, path:String): Option[Matcher] = {
    val m=_regex.matcher(path)
    if (m.matches() &&
      _verbArr.find { (s) =>s=="*" || s == mtd.uc }.isDefined ) {
      Some(m)
    } else {
      None
    }
  }

  def mountPoint_=(s:String) {   _mountPt = nsb(s) }
  def mountPoint = _mountPt

  def template_=(s:String) {   _tpl = nsb(s) }
  def template = _tpl

  def pattern() = _regex
  def pipeline() = _pipe
  def path() = _path
  def verb() = _verb

  def resolveMatched(mc:Matcher) = {
    val rc= mutable.HashMap[String,String]()
    val gc = mc.groupCount()
    _placeholders.foreach { (t) =>
      rc.put( t._2, nsb ( mc.group(t._2) ) )
//      if (t._1 <= gc) {
//        rc.put( t._2, mc.group(t._1) )
//      }
    }
    rc.toMap
  }

  /*
   *  path can be /host.com/abc/:id1/gg/:id2
   */
}




