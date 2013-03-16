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

package com.zotoh.blason
package etc

import java.io.File
import java.util.{Date => JDate}
import java.util.{Properties => JPS}

import org.apache.commons.lang3.time.DateUtils.addMonths

import com.zotoh.frwk.i18n.Resources
import com.zotoh.frwk.io.IOUtils._
import com.zotoh.frwk.security._
import com.zotoh.frwk.security.Crypto
import com.zotoh.frwk.security.PwdFactory
import com.zotoh.frwk.util.CmdLineQ
import com.zotoh.frwk.util.CmdLineSeq
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdCrypto(home:File,cwd:File,rc:Resources) extends CmdLine(home,cwd,rc)  with CoreImplicits {

  def eval(args:Array[String]) {}
  def getCmds() = Set()

  def testjce() {
    try {
      Crypto.testJCEPolicy()
      System.out.format("%s\n", _rcb.getString( "cmd.jce.ok"))
    } catch {
      case e:Throwable =>
        System.out.format("%s\n%s\n%s\n%s\n",
          _rcb.getString( "cmd.jce.error1"), _rcb.getString( "cmd.jce.error2"),
          _rcb.getString( "cmd.jce.error3"), _rcb.getString( "cmd.jce.error4"))
    }
  }

  def generatePassword() {
    println("\n" + PwdFactory.mkRandomText(16))
  }

  def encrypt(pwd:String, txt:String) {
    println( "\n" + PwdFactory.encrypt(pwd,txt) )
  }
  
  def decrypt(pwd:String, blob:String) {
    println( "\n" + PwdFactory.decrypt(pwd, blob) )
  }

  def keyfile() {
    val s=keyFileInput
    val ps=new JPS()
    if ( ! s.start(ps).isCanceled) {
      val out= new File( ps.gets("fn"))
      val start= new JDate()
      val end= addMonths(start, asInt(ps.gets("months"),12))

      Crypto.mkSSV1PKCS12(uid(), start, end,
              "CN="+ps.gets("cn")+", OU="+ps.gets("ou")+", O="+ps.gets("o")+", L="+ps.gets("l")+", ST="+
              ps.gets("st")+", C="+ ps.gets("c"),
              ps.gets("pwd"), asInt(ps.gets("size"),1024), out)
    }
  }

  def csrfile() {
    val ps=new JPS()
    val s= csrInput
    if (!s.start(ps).isCanceled) {
      val t=Crypto.mkCSR( asInt(ps.gets("size"),1024),
              "CN="+ps.gets("cn")+", OU="+ps.gets("ou")+", O="+ps.gets("o")+
            ", L="+ps.gets("l")+", ST="+ps.gets("st")+", C="+ps.gets("c"), PEM)
      val fn= ps.gets("fn")
      var out= new File( fn)
      writeFile(out, t._1)
      out= new File(fn + ".key")
      writeFile(out, t._2)
    }
  }

  // create the set of questions to prompt during the creation of server key
  private def keyFileInput() = {
    val q10= new CmdLineQ("fname", _rcb.getString("cmd.save.file"), "", "test.p12") {
          override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("fn", a)
              ""
      }}
    val q9= new CmdLineQ("pwd", _rcb.getString("cmd.key.pwd")) {
          override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("pwd", a)
              "fname"
        }}
    val q8= new CmdLineQ("duration", _rcb.getString("cmd.key.duration"), "", "12") {
          override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("months", a)
              "pwd"
        }}
    val p= csrInput
    p.remove("fname")
    new CmdLineSeq(Some(p), q8,q9,q10){
      override def onStart() = q8.label
    }
  }

  // create the set of questions to prompt during the creation of CSR
  private def csrInput() = {
    val q8= new CmdLineQ("fname", _rcb.getString( "cmd.save.file"), "", "test.csr") {
        override def onRespSetOut(a:String , ps:JPS) = {
            ps.put("fn", a)
            ""
      }}
    val q7= new CmdLineQ("size", _rcb.getString( "cmd.key.size"), "", "1024") {
        override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("size", a)
              "fname"
      }}
    val q6= new CmdLineQ("c", _rcb.getString( "cmd.dn.c"), "", "US") {
        override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("c", a)
              "size"
      }}
    val q5= new CmdLineQ("st", _rcb.getString("cmd.dn.st")) {
        override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("st", a)
              "c"
      }}
    val q4= new CmdLineQ("loc", _rcb.getString("cmd.dn.loc")) {
        override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("l", a)
              "st"
      }}
    val q3= new CmdLineQ("o", _rcb.getString("cmd.dn.org"), "", "") {
        override def onRespSetOut(a:String , ps:JPS) = {
            ps.put("o", a)
            "loc"
      }}
    val q2= new CmdLineQ("ou", _rcb.getString( "cmd.dn.ou")) {
          override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("ou", a)
              "o"
      }}
    val q1= new CmdLineQ("cn", _rcb.getString( "cmd.dn.cn")) {
          override def onRespSetOut(a:String , ps:JPS) = {
              ps.put("cn", a)
              "ou"
      }}
    new CmdLineSeq(q1,q2,q3,q4,q5,q6,q7,q8) {
        override def onStart() = "cn"
    }
  }

}


