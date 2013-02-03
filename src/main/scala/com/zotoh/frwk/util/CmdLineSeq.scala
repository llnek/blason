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

import java.io.{BufferedOutputStream,InputStreamReader,OutputStreamWriter}
import java.io.{Reader,Writer}
import java.util.{Properties=>JPS}
import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.CoreUtils._


/**
 * @author kenl
 *
 */
abstract class CmdLineSeq( qs:CmdLineQ* ) {

  private var _out= new OutputStreamWriter( new BufferedOutputStream(System.out))
  private var _in = new InputStreamReader(System.in)
  private val _Qs= mutable.HashMap[String,CmdLineQ]()
  private var _par:CmdLineSeq= null
  private var _canceled=false

  qs.foreach { (q) =>  _Qs +=  q.label() -> q  }

  /**
   * @param props
   */
  def start(props:JPS) : this.type = {
    val kp= if (isWindows()) "<Ctrl-C>" else "<Ctrl-D>"
    var stop= false
    if (_par == null) {
//        println( ">>> Press <Ctrl-D><Enter> to cancel...")
        println(  ">>> Press "+kp+"<Enter> to cancel...")
    } else if ( _par.start(props).isCanceled) {
        _canceled=true
        end()
        stop=true
    }
    if (!stop) {
      _Qs.get(onStart) match {
        case Some(c) => cycle(c, props)
        case _ => end()
      }
    }
    this
  }

  /**
   * @return
   */
  def isCanceled() = _canceled

  /**
   * @param id
   */
  def remove(id:String): this.type = { _Qs -= id ; this }

  private def cycle(original:CmdLineQ, props:JPS) {
    var c:CmdLineQ =original
    var n=""
    while (c != null) {
      c.setOutput(props)
      n=popQuestion(c)
      c = if (n == "") null else {
       if (_Qs.isDefinedAt(n)) _Qs( n) else null
      }
    }
    end()
  }

  /**
   * @param c
   * @return
   */
  protected def popQuestion(c:CmdLineQ) = {
    var d= c.dftAnswer
    var q= c.question
    var ch= c.choices
    var s= ""

    _out.write(q +
        ( if (c.isMust) "*" else "" ) + " ? " )

    if (! STU.isEmpty(ch)) {
      if (ch.indexOf('\n') >= 0) {
        _out.write(
            (if (ch.startsWith("\n")) "[" else "[\n") + ch +
            (if (ch.endsWith("\n")) "]" else "\n]" )
        )
      } else {
        _out.write("[" + ch + "]")
      }
      s= " "
    }
    if (! STU.isEmpty(d)) {
      _out.write("(" + d + ")")
      s= " "
    }

    _out.write(s)
    _out.flush()

    // get the input from user
    s= readData()

    // point to next question, blank ends it
    if (isCanceled) {
      println("")
      ""
    } else {
      c.setAnswer(s)
    }

  }

  private def readData() = {
    var b=new StringBuilder()
    var loop=true
    var esc=false
    var c=0

    // windows has '\r\n'
    // linux has '\n'
    while(loop) {
      c=  _in.read()
      if (c== -1 || c==4) { esc=true; loop=false }
      if (c== '\n') { loop=false }
      if (c=='\r' || c== '\b'|| c==27 /*esc*/) { /* continue */ }
      else if (loop) { b.append(c.toChar) }
    }

    if (esc) {
      _canceled=true ; b.setLength(0)
    }

    STU.trim( b.toString )
  }

  private def end() = onEnd()

  /**
   * @return
   */
  protected def onStart(): String

  /**
   *
   */
  protected def onEnd() {}

  /**
   * @param par
   * @param qs
   */
  def this(par: Option[CmdLineSeq], qs:CmdLineQ*) {
    this(qs: _*)
    _par= par.getOrElse( null )
  }

}



