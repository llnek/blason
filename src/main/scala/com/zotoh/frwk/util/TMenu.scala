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
import org.slf4j._
import java.io.{BufferedReader,Console,InputStreamReader}
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._


/**
 * @author kenl
 *
 */
class TMenu( protected var _title:String) {

  protected val _choices= mutable.ArrayBuffer[TMenuItem]()
  protected val _ids= mutable.HashSet[String]()
  protected var _prev:TMenu=null

  private val _log= LoggerFactory.getLogger(classOf[TMenu])
  def tlog() = _log

  _title= strim(_title)

  /**
   *
   */
  def pop(): this.type = {
    if (_prev != null) { _prev.display }
    this
  }

  /**
   * @param upper
   */
  def show(upper:TMenu): this.type = {
    if (upper != null) { _prev=upper }
    display()
    this
  }

  /**
   *
   */
  protected def display() {
    val c= System.console()
    //  clsConsole()
    dispTitle(c)
    (1 /: _choices) { (cnt, ch) => dispMItem(c, cnt, ch.desc()); cnt+1 }
    dispMItem(c, 99, if (_prev==null) "Quit" else "^Back")

    asInt( STU.trim( getInput(c)), 0) match {
      case 99 => pop()
      case n:Int if (n >= 1 && n <= _choices.length) => _choices(n-1).onSelect()
      case _ => display()
    }
  }

  /**
   * @param i
   */
  def add(i:TMenuItem) {
    if (_ids.contains(i.id)) {
      errBadArg("Item(id) already exists.")
    }
    i.setParent(this)
    _ids += i.id
    _choices += i
  }

  /**
   * @param i
   */
  def remove(i:TMenuItem): this.type = {
    if (i != null) {
      _ids -= i.id
      _choices -= i
    }
    this
  }

  /**
   * @return
   */
  def getItems() = _choices.toSeq

  private def dispTitle(c:Console) {
    if (c == null) {
      print("****************************************\n")
      println("%s %s".format("Menu:",_title) )
      print("****************************************\n")
    }
    else {
      c.printf("****************************************\n")
      c.printf("%s %s", "Menu:",_title)
      c.printf("****************************************\n")
    }
  }

  private def dispMItem(c:Console, pos:Int, desc:String) {
    val i= asJObj(pos)
    if (c == null) {
      println("%2d)  %s".format(i, desc) )
    } else {
      c.printf("%2d)  %s\n", i, desc)
    }
  }

  private def getInput(c:Console) = {
    if (c != null) { c.readLine } else {
      new BufferedReader(new InputStreamReader(System.in)).readLine
    }
  }

}
