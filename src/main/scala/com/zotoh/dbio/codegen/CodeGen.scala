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
package codegen

import org.apache.commons.lang3.{StringUtils=>STU}
import java.lang.reflect.Method
import com.zotoh.dbio.meta.Column
import com.zotoh.dbio.meta.Many2Many
import com.zotoh.dbio.meta.One2Many
import com.zotoh.dbio.meta.One2One
import com.zotoh.frwk.util.CoreUtils.tstArg
import com.zotoh.dbio.core.Utils
import java.sql.{Timestamp=>JSTSP, Time=>JST}
import java.util.{Date=>JDate,Calendar=>JCal,Map=>JMap,HashMap=>JHM}
import freemarker.template.{Configuration=>FTLCfg}
import freemarker.template.DefaultObjectWrapper
import freemarker.template.Template
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import java.io.File
import com.zotoh.frwk.io.IOUtils._
import java.io.StringWriter
import com.zotoh.frwk.util.DateUtils
import java.util.Calendar
import com.zotoh.frwk.util.MetaUtils


/**
 * @author kenl
 */
object CodeGen {
  
  private val _ftlDir= genTmpDir()
  writeFile(new File(_ftlDir,"classdef.ftl"), 
        rc2Str("com/zotoh/dbio/codegen/classdef.ftl", "utf-8"),
        "utf-8")
  writeFile(new File(_ftlDir,"model.ftl"), 
        rc2Str("com/zotoh/dbio/codegen/model.ftl", "utf-8"),
        "utf-8")
  
  def main(args:Array[String]) {
    val rc=List("IAncillaryInfo","ICarrier","ICarrierConnection","IFeedback","IRetailer","IStdAddress","ITrackingDetail","ITrackingInfo","IWatchList").map { (s) =>
      "com.narvar.model." + s
    }
    genFiles(rc, new File("/tmp/abc"))
  }
  
  private def genFiles(cz:Seq[String], desDir:File) {
    desDir.mkdirs()
    cz.foreach { (c) =>
      val z=MetaUtils.loadClass(c)
      writeOneFile( z, new CodeGen(_ftlDir).genOneFile(z), desDir)          
    }
  }
  
  private def writeOneFile(cz:Class[_], body:String, desDir:File) {
    val pn=cz.getPackage().getName()
    val p=STU.replaceChars(pn, '.','/')
    val n= cz.getSimpleName() + "DAO.scala"
    val d=new File(desDir,p)
    d.mkdirs()
    val f=new File( d, n)
    writeFile(f, body, "utf-8")
  }
  
}

/**
 * @author kenl
 */
sealed class CodeGen(ftlDir:File) {  

  private val _ftlCfg = new FTLCfg()
    
  _ftlCfg.setObjectWrapper(new DefaultObjectWrapper())
  _ftlCfg.setDirectoryForTemplateLoading( ftlDir)

  def genOneFile(cz:Class[_]) = {
    tstArg(cz.isInterface(), "" + cz.getName() + " - must be a trait or interface.")
//    tstArg( cz.getSimpleName().startsWith("I") , "" + cz.getName() + " - name must start with I.")
    val b=new StringBuilder()
    val mtds= cz.getMethods()
    val model= prepareDef(cz)
    mtds.filter ( Utils.hasColumn(_) ).foldLeft(b) { (b, m) =>
      resetScope(model)
      b.append( genOneMtd(model, m.getName, m))
      b
    }
    mtds.filter ( Utils.hasAssoc(_) ).foldLeft(b) { (b, m) =>
      resetScope(model)
      b.append( genOneMtd(model, m.getName, m))
      b
    }
    pimpScope(model, b.toString)
    val out= finz(model)
    out
  }

  def genOneMtd(model:JMap[_,_], mn:String, mtd:Method) = {
    val b1= mn.startsWith("dbio_") && mn.endsWith("_column") && Utils.hasColumn(mtd)
    val b2= mn.startsWith("dbio_") && mn.endsWith("_fkey") && Utils.hasAssoc(mtd) 
    if (b1 || b2) tstArg( mtd.getReturnType() == classOf[String], "Method must return a string  - column name.")       
    if (b1) {
        genOneCol(model,mn, mtd)      
    }
    else if (b2) {
        genOneRef(model,mn, mtd)      
    } else {
      ""
    }
    
  }
  
  def genOneRef(model:JMap[_,_], mn:String, mtd:Method) = {    
    val gn= mn.substring(5, mn.length() - 5)
    val sn=  gn.substring(3)
    val scope= getScope(model)
    var sum=0
    scope.put("getter", gn)
    scope.put("setter", "set"+sn)
    scope.put("adder", "link" + sn)
    scope.put("delone", "unlink"+ sn)
    scope.put("delall", "purge"+sn)
    scope.put("encacher", "encache"+sn)
    scope.put("decacher", "decache"+sn)
    scope.put("colname", mn)
    scope.put("col",false)
    scope.put("o2o",false)
    scope.put("o2m",false)
    scope.put("m2m",false)
    Utils.getO2M(mtd) match {
      case x:One2Many =>
        scope.put("rhstype", x.rhs().getName() )
        scope.put("o2m",true)
        scope.put("singly", x.singly() )
        sum += 1
      case _ =>
    }
    Utils.getO2O(mtd) match {
      case x:One2One =>
        scope.put("rhstype", x.rhs().getName() )
        scope.put("o2o",true)
        sum += 1
      case _ =>
    }
    Utils.getM2M(mtd) match {
      case x:Many2Many =>
        scope.put("rhstype", x.rhs().getName() )
        scope.put("joinedtype", x.joined().getName() )
        scope.put("m2m",true)
        sum += 1
      case _ =>
    }
    if (sum != 1) {
      throw new Exception("Expected 1 Assoc annotation only for " + mn)
    }
    process(model)  
  }
  
  def genOneCol(model:JMap[_,_], mn:String, mtd:Method) = {
    val gn= mn.substring(5, mn.length() - 7)
    val sn=  gn.substring(3)
    val c= Utils.getColumn(mtd)
    val (pt,rdr)= getParamType( c.data )
    val scope= getScope(model)
    var b=""
    scope.put("col",true)
    scope.put("o2o",false)
    scope.put("o2m",false)
    scope.put("m2m",false)
    scope.put("getter", gn)
    scope.put("setter", "set"+sn)
    scope.put("colname", mn)
    scope.put("reader", rdr)
    scope.put("param_type", pt)    
    if ( c.autogen()) { b += "autogen=true," }
    if ( hgl(c.desc())) {  b+= "desc=\"" + c.desc + "\","}
    if ( hgl(c.index())) {  b+= "index=\"" + c.index + "\","}
    if ( !c.optional  ) { b += "optional=false," }
    if ( c.readonly  ) { b += "readonly=true," }
    if ( c.size() != 255 ) { b += "size=" + c.size + "," }
    if (b.endsWith(",")) { b=b.substring(0, b.length-1) }
    scope.put("coldetails", b)
    process(model)  
  }  
  
  def getParamType( cz:Class[_]) = {
    if (cz == classOf[Boolean]) ("Boolean", "readBool") else
    if (cz == classOf[Double]) ("Double","readDouble") else
    if (cz == classOf[Float]) ("Float","readFloat") else
    if (cz == classOf[Long]) ("Long","readLong") else
    if (cz == classOf[Int]) ("Int","readInt") else
    if (cz == classOf[String]) ("String","readString") else
    if (cz == classOf[JSTSP]) ("java.sql.Timestamp","readTimestamp") else
    if (cz == classOf[JST]) ("java.sql.Time","readTime") else
    if (cz == classOf[JDate]) ("java.util.Date","readDate") else
    if (cz == classOf[JCal]) ("Calendar","readCalendar") else
    throw new Exception("Bad DataType: " + cz)      
  }

  private def prepareDef(cz:Class[_]) = {
    val pn= cz.getPackage().getName()
    val model= new JHM[String, Any]()
    val scope= new JHM[String, Any]()
    val cn= cz.getSimpleName()
    model.put("gendate", DateUtils.fmtDate(new JDate))
    model.put("pkg", pn)
    model.put("classname", cn + "DAO")
    model.put("trait", cn)
    model.put("scope", scope)
    model
  }
  private def resetScope(m:JMap[_,_]) = {
    val scope= m.get("scope").asInstanceOf[JMap[String,Any]]
    scope.clear()
  }
  private def pimpScope(m:JMap[String,Any], body:String) = {
    val scope= m.get("scope").asInstanceOf[JMap[String,Any]]    
    scope.clear()
    m.put("classbody", body)
  }
  
  private def getCZDef() =    _ftlCfg.getTemplate( "/classdef.ftl")
  private def getTpl() =    _ftlCfg.getTemplate( "/model.ftl")
  private def getScope(m:JMap[_,_]) = m.get("scope").asInstanceOf[JMap[String,Any]]
  private def process(m:JMap[_,_]) = {
    val out = new StringWriter()
    val t= getTpl
    t.process( m, out)
    out.flush()
    out.toString()    
  }
  private def finz(m:JMap[_,_]) = {
    val out = new StringWriter()
    val t= getCZDef
    t.process( m, out)
    out.flush()
    out.toString()    
  }
}



