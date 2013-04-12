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

import java.io.File
import java.io.StringWriter
import java.lang.reflect.Method
import java.sql.{Time=>JST}
import java.sql.{Timestamp=>JSTSP}
import java.util.{Calendar=>JCal}
import java.util.{Date => JDate}
import java.util.Date
import java.util.{HashMap => JHM}
import java.util.{Map => JMap}
import org.apache.commons.lang3.{StringUtils => STU}
import com.zotoh.dbio.core.Utils
import com.zotoh.dbio.meta.Many2Many
import com.zotoh.dbio.meta.One2Many
import com.zotoh.dbio.meta.One2One
import com.zotoh.frwk.io.IOUtils.writeFile
import com.zotoh.frwk.util.CoreUtils.genTmpDir
import com.zotoh.frwk.util.CoreUtils.rc2Str
import com.zotoh.frwk.util.CoreUtils.tstArg
import com.zotoh.frwk.util.DateUtils
import com.zotoh.frwk.util.MetaUtils
import com.zotoh.frwk.util.StrUtils._
import freemarker.template.{Configuration => FTLCfg}
import freemarker.template.DefaultObjectWrapper
import com.zotoh.dbio.core.DBIOManifest
import com.zotoh.dbio.meta.CodeGenManifest
import com.zotoh.dbio.meta.Assoc



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
    if (args.length < 2) usage() else {
      MetaUtils.mkRef(args(0)) match {
        case x:DBIOManifest => genFiles( x, new File( args(1))) 
        case _ => usage()
      }
    }
  }
  
  private def usage() {
    println("CodeGen: manifest-class output-dir")
    println("")
  }
  
  private def genFiles(mf:DBIOManifest, desDir:File) {
    val cz= mf.getClass()
    val mmf= Utils.getManifest(cz)
    tstArg(mmf != null, "Invalid Manifest, missing annotation - CodeGenManifest")
    desDir.mkdirs()
    cz.getClasses().foreach { (z) =>
      writeOneFile( mmf, z, new CodeGen(_ftlDir).genSourceFile(mf, z), desDir)                
    }
    println("Done.")      
  }
    
  private def writeOneFile(mmf:CodeGenManifest, cz:Class[_], body:String, desDir:File) {
    val p=STU.replaceChars(mmf.pkg, '.','/')
    val n= cz.getSimpleName() + ".scala"
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
  
  def genSourceFile(mf:DBIOManifest, cz:Class[_]) = {
    val b=new StringBuilder()
    val mtds= cz.getMethods()
    val (model, obj) = prepareDef(mf,cz)
    mtds.filter ( Utils.hasField(_) ).foldLeft(b) { (b, m) =>
      resetScope(model)
      b.append( genOneCol(mf, model, obj, m.getName, m))
      b
    }
    mtds.filter ( Utils.hasAssocDef(_) ).foldLeft(b) { (b, m) =>
      resetScope(model)
      b.append( genOneRef(mf, model, obj, m.getName, m))
      b
    }
    pimpScope(model, b.toString)
    val out= finz(model)
    out
  }

  def genOneRef(mf:DBIOManifest, model:JMap[_,_], obj:Any, mn:String, mtd:Method) = {    
    tstArg( mtd.getReturnType() == classOf[String], "Method must return a string  - column name.")    
    tstArg( mn.startsWith("get"), "Method must be a getter.")    
    val pkg= Utils.getManifest(mf.getClass() ).pkg()
    val annon=mtd.getAnnotation(classOf[Assoc])
    val kind= annon.kind()
    var rhs= annon.rhs()
    val cn= Utils.fmtAssocKey(mn)
    val scope= getScope(model)
    val sn=  mn.substring(3)
    var ts=""
    var b=""
    var sum=0
    //if (rhs.indexOf('.') < 0) { rhs = pkg + "." + rhs } 
    scope.put("getter", mn)
    scope.put("setter", "set"+sn)
    scope.put("adder", "link" + sn)
    scope.put("delone", "unlink"+ sn)
    scope.put("delall", "purge"+sn)
    scope.put("encacher", "encache"+sn)
    scope.put("decacher", "decache"+sn)
    scope.put("colname", cn)
    scope.put("columnid", nsb( mtd.invoke(obj)) )
    scope.put("col",false)
    scope.put("o2o",false)
    scope.put("o2m",false)
    scope.put("m2m",false)
    scope.put("singly", false )
    kind match {
      case "o2o" =>
        scope.put("rhstype", rhs )
        scope.put("o2o",true)
        b = "@One2One( rhs=classOf["+rhs+"] )"
        sum += 1
      case "o2m" =>
        scope.put("rhstype", rhs )
        scope.put("o2m",true)
        b = "@One2Many( rhs=classOf["+rhs+"] )"
        sum += 1
      case "o2o+" =>
        scope.put("rhstype", rhs )
        scope.put("o2m",true)
        scope.put("singly", true )
        b = "@One2Many( rhs=classOf["+rhs+"], singly=true )"
        sum += 1
      case s if s.startsWith("m2m:") =>
        ts= s.substring(4)
        //if (ts.indexOf('.') < 0) { ts = pkg + "." + ts}
        scope.put("joinedtype", ts )
        scope.put("rhstype", rhs )
        scope.put("m2m",true)
        b = "@Many2Many( rhs=classOf["+rhs+"], joined=classOf[" + ts + "])"
        sum += 1
      case _ =>
    }
    if (sum != 1) {
      throw new Exception("Expected 1 Assoc annotation only for " + mn)
    }
    scope.put("refdetails", b)
    process(model)  
  }
  
  def genOneCol(mmf:DBIOManifest, model:JMap[_,_], obj:Any, mn:String, mtd:Method) = {
    tstArg( mtd.getReturnType() == classOf[String], "Method must return a string  - column name.")        
    val sn=  mn.substring(3)
    val c= Utils.getField(mtd)
    val (pt,rdr)= getParamType( c.data )
    val scope= getScope(model)
    var b=" data=classOf[" + pt + "], "
    scope.put("col",true)
    scope.put("o2o",false)
    scope.put("o2m",false)
    scope.put("m2m",false)
    scope.put("getter", mn)
    scope.put("setter", "set"+sn)
    scope.put("colname", Utils.fmtMarkerKey(mn))
    scope.put("columnid", nsb( mtd.invoke(obj)) )    
    scope.put("reader", rdr)
    scope.put("param_type", pt)    
    if ( c.autogen()) { b += "autogen=true," }
    if ( hgl(c.desc())) {  b+= "desc=\"" + c.desc + "\","}
    if ( hgl(c.index())) {  b+= "index=\"" + c.index + "\","}
    if ( !c.optional  ) { b += "optional=false," }
    if ( c.readonly  ) { b += "readonly=true," }
    if ( c.size() != 255 ) { b += "size=" + c.size + "," }
    b=strim(b)
    if (b.endsWith(",")) { b=b.substring(0, b.length-1) }
    scope.put("coldetails", b)
    process(model)  
  }  
  
  def getParamType( cz:String) = {
    cz.toLowerCase() match {
      case "string" => ("String","readString")
      case "boolean" => ("Boolean", "readBool")
      case "long" =>  ("Long","readLong") 
      case "int" => ("Int","readInt")
      case "double" => ("Double","readDouble")
      case "float" => ("Float","readFloat")
      case "date" | "java.util.date" =>  ("java.util.Date","readDate") 
      case "calendar" => ("Calendar","readCalendar")
      case "timestamp" | "java.sql.timestamp" =>  ("java.sql.Timestamp","readTimestamp")
      case "time" | "java.sql.time" =>  ("java.sql.Time","readTime")
      case _ =>
      throw new Exception("Bad DataType: " + cz)      
    }
  }

  private def prepareDef(mf:DBIOManifest, cz:Class[_]) = {
    val pn= cz.getPackage().getName()
    val model= new JHM[String, Any]()
    val scope= new JHM[String, Any]()
    val cn= cz.getSimpleName()
    val t= Utils.getTable(cz)
    var b= ""
    if (t != null) {
      
      b= "@Table( table = \"" + t.table() + "\", indexes=Array(" + 
      join( t.indexes().map( "\"" + _ + "\"" ) , ",") + "), uniques=Array(" + 
      join( t.uniques().map( "\"" + _ + "\"" ), ",") + ") )"
    }
    model.put("gendate", DateUtils.fmtDate(new JDate))
    model.put("pkg", pn)
    model.put("classname", cn)
    model.put("trait", cn)
    model.put("scope", scope)
    model.put("tbldetails", b)
    
    ( model, cz.getConstructor(mf.getClass()).newInstance(mf) )
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



