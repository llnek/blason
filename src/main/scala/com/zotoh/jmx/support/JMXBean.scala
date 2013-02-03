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

package com.zotoh.jmx
package support

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.language.reflectiveCalls


import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Arrays

import javax.management.Attribute
import javax.management.AttributeList
import javax.management.AttributeNotFoundException
import javax.management.DynamicMBean
import javax.management.MBeanAttributeInfo
import javax.management.MBeanException
import javax.management.MBeanInfo
import javax.management.MBeanOperationInfo
import javax.management.MBeanParameterInfo
import javax.management.ReflectionException

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.util.MetaUtils._


/**
 * @author kenl
 */
class JMXBean(private val _obj:Any) extends DynamicMBean {

  private val _jmxPropMap = mutable.HashMap[String, BPropInfo]()
  private val _jmxMtdMap = mutable.HashMap[NameParams, Method]()
  private val _jmxFldMap = mutable.HashMap[String, BFieldInfo]()
  private val _beanInfo = toBeanInfo(_obj.getClass )

  def getMBeanInfo() = _beanInfo

  // get actual field value but try getter first.
  def getAttribute(attrName:String): Object = {
    val prop = _jmxPropMap.getOrElse(attrName,null)
    if (prop == null) {
      val fld = _jmxFldMap.getOrElse(attrName,null)
      if (fld == null || !fld.isGetter ) {
        throw unknownError(attrName)
      }
      fld.field.get(_obj)
    }
    else if (prop.getter == null) {
      throw unknownError(attrName)
    }
    else {
      prop.getter.invoke(_obj)
    }
  }

  def getAttributes(attrNames:Array[String]): AttributeList = {
    val rcl = new AttributeList()
    attrNames.foreach { (name) =>
      try {
        rcl.add(new Attribute(name, getAttribute(name)))
      } catch {
        case e:Throwable =>
        tlog.error("",e)
        rcl.add(new Attribute(name, e.getMessage))
      }
    }
    rcl
  }

  def setAttribute(attr:Attribute) {
    val v= attr.getValue
    val an=attr.getName
    val prop = _jmxPropMap.getOrElse(an, null)
    if (prop == null) {
      val fld = _jmxFldMap.getOrElse(an, null)
      if (fld == null || !fld.isSetter) {
        throw unknownError(an)
      }
      fld.field.set(_obj, v)
    }
    else if (prop.setter == null) {
      throw unknownError(an)
    }
    else {
      prop.setter.invoke(_obj, v)
    }
  }

  def setAttributes(attrs:AttributeList ): AttributeList = {
    val rcl = new AttributeList(attrs.size)
    attrs.asList.foreach { (a) =>
      val name = a.getName
      try {
        setAttribute(a)
        rcl.add(new Attribute(name, getAttribute(name)))
      } catch {
        case e:Throwable =>
        tlog.error("",e)
        rcl.add(new Attribute(name, e.getMessage))
      }
    }
    rcl
  }

  // invoke a real method.
  def invoke(opName:String, params:Array[Object], sig:Array[String]) = {

    val mtd = _jmxMtdMap.getOrElse(new NameParams(opName, sig),null)
    if (mtd == null) {
      throw beanError("Unknown operation \"" + opName + "\"")
    }
    if (params==null || params.length == 0) mtd.invoke(_obj) else {
      mtd.invoke(_obj, params:_*)
    }

  }

  private def arrToMap[T <: { def name(): String } ] (arr:Array[T]): Map[String,T] = {
    if (arr != null) {
      val m = mutable.HashMap[String, T]()
      arr.foreach { (a) =>
        m.put(a.name(), a)
      }
      m.toMap
    } else { null }
  }

  private def toBeanInfo(cz:Class[_]) = {

    val rc = cz.getAnnotation(classOf[JMXResource] )
    val methods = cz.getMethods()
    val desc = if (rc == null || STU.isEmpty(rc.desc) ) {
      "Information about " + cz
    } else {
      rc.desc
    }

    new MBeanInfo(cz.getName, desc,
        handleProps(methods) ++ handleFlds(), null,
        handleMtds(methods), null )
  }

  // scan for getters & setters.
  private def handleProps(mtds:Array[Method]): Array[MBeanAttributeInfo] = {
    mtds.foreach { (mtd) =>
      mtd.getAnnotation(classOf[JMXProperty]) match {
        case j:JMXProperty =>
          val mn=mtd.getName
          val propInfo = new JMXPropertyInfo(mn,j)
          val ptypes= mtd.getParameterTypes()
          val rtype= mtd.getReturnType()
          val pname = maybeGetPropName(mtd)
          var methodInfo = _jmxPropMap.getOrElse(pname,null)
          mn match {
            case s if startsWith(s,Array("is","get")) =>
              assertArgs(mn, ptypes, 0)
              if (methodInfo == null) {
                _jmxPropMap.put(pname,
                  new BPropInfo(pname, propInfo.desc, (mtd,null) ))
              } else {
                methodInfo.setGetter(mtd)
              }
            case s if s.startsWith("set") =>
              assertArgs(mn, ptypes, 1)
              if (methodInfo == null) {
                _jmxPropMap.put(pname,
                  new BPropInfo(pname, propInfo.desc,(null,mtd) ))
              } else {
                methodInfo.setSetter( mtd)
              }
            case _ => tlog.warn("\"{}\" should match [sg]etXXX.", mtd)
          }
        case _ =>
      }
    }

    _jmxPropMap.values.map { (t) =>
      new MBeanAttributeInfo(t.name, t.getType.getName,
          t.desc, (t.getter != null), (t.setter != null),
          t.isQuery )
    }.toArray

  }

  private def handleFlds(): Array[MBeanAttributeInfo] = {
    _obj.getClass().getDeclaredFields().flatMap { (field) =>
      field.getAnnotation(classOf[JMXField] ) match {
        case f:JMXField =>
          val fn=field.getName
          val fldInfo = new JMXFieldInfo(fn, f)
          if (!field.isAccessible) { field.setAccessible(true) }
          _jmxFldMap.put(fn, new BFieldInfo(field, fldInfo.isReadable,
              fldInfo.isWritable))
          var desc= fldInfo.desc match {
            case s if (! STU.isEmpty(s)) => s
            case _ => fn + " attribute"
          }
          Some( new MBeanAttributeInfo(fn, field.getType.getName, desc,
            fldInfo.isReadable, fldInfo.isWritable,
            fn.startsWith("is") && isBoolean(field.getType) ) )
        case _ =>
          None
      }
    }
  }

  private def handleMtds(mtds:Array[Method]): Array[MBeanOperationInfo] = {
    mtds.flatMap { (m) =>
      m.getAnnotation(classOf[JMXMethod] ) match {
        case j:JMXMethod =>
          val mn=m.getName
          if (startsWith(mn,Array("is","get","set"))) {
            throw badArg("\""+ mn + "\""+" has incorrect annotation.")
          }
          val mtdInfo = new JMXMethodInfo(mn, j)
          val pnames= m.getParameterTypes().map { (t) => t.getName }
          val nameParams = new NameParams(mn, pnames)
          val parameterInfos = mkParameterInfo(m, mtdInfo)
          var desc= mtdInfo.desc match {
            case s if (!STU.isEmpty(s)) => s
            case _ => mn + " attribute"
          }
          _jmxMtdMap.put(nameParams, m)
          Some(new MBeanOperationInfo(mn, desc, parameterInfos,
            m.getReturnType.getName, mtdInfo.info.infoValue ) )
        case _ =>
          None
      }
    }
  }

  private def mkParameterInfo(mtd:Method, info:JMXMethodInfo): Array[MBeanParameterInfo] = {

    val ptypes = mtd.getParameterTypes()
    val pms= info.params()
    val keys=pms.keySet.toArray
    val vals=pms.values.toArray
    var i=0
    ptypes.map { (t) =>
      val n=if (i >= keys.size) "p"+(i+1) else keys(i)
      val d=if (i >= vals.size) "" else vals(i)
      i+=1
      new MBeanParameterInfo(n, t.getName, d)
    }
  }

  private def maybeGetPropName(mtd:Method) = {
    val name=mtd.getName
    val pos= name match {
      case s if startsWith(s,Array("get","set")) => 3
      case s if s.startsWith("is") => 2
      case _ =>
        throw badArg("\"" + name + "\" is incorrectly named.")
    }
    Character.toLowerCase(name.charAt(pos)) + name.substring(pos+1)
  }

  private def unknownError(attr:String ) = {
    new AttributeNotFoundException("Unknown property " + attr)
  }

  private def beanError(msg:String) = new MBeanException(new Exception(msg))    
  private def badArg(msg:String) = new IllegalArgumentException(msg)
  private def assertArgs(mtd:String, ptypes:Array[_], n:Int) {
    if (ptypes.length != n) {
      throw badArg("\"" + mtd + "\" needs "+ n + "args.")
    }
  }

}

class NameParams(private val _name:String, private val _pms:Array[String]) {

  override def hashCode() = {
    var hash= 31 * (31 + _name.hashCode)
    if (_pms != null) {
      hash += Arrays.hashCode(_pms.toArray[Object] )
    }
    hash
  }

  override def equals(obj:Any) = {
    if (obj == null || getClass() != obj.getClass) false else {
      val other = obj.asInstanceOf[NameParams]
      if ( _name != other._name) false else {
        Arrays.equals(_pms.toArray[Object], other._pms.toArray[Object])
      }
    }
  }

}

/*
*/
class BPropInfo(private val _prop:String, private var _desc:String,
  gs:(Method,Method) ) {

  private var _type:Class[_] = null
  private var _getr = gs._1
  private var _setr = gs._2

  iniz()

  def getType() = _type
  def desc() = _desc

  def getter() = _getr
  def name() = _prop
  def setter() = _setr

  def setSetter(m:Method) { _setr= m }
  def setGetter(m:Method) { _getr= m }

  def isQuery() = {
    if (_getr == null) false else {
      _getr.getName.startsWith("is") && isBoolean(_type)
    }
  }

  private def iniz() {
    _type= if (_getr != null)  _getr.getReturnType else {
      if (_setr == null) null else _setr.getParameterTypes()(0)
    }
    if (STU.isEmpty(_desc)) {
      _desc = _prop + " property"
    }
  }
}

/*
*/
class BFieldInfo(private val _field:Field,
private val _getr:Boolean,
private val _setr:Boolean) {

  def isGetter() = _getr
  def isSetter() = _setr
  def field() = _field

}

