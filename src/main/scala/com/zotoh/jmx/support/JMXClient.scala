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

import java.lang.reflect.Constructor
import java.net.MalformedURLException
import java.util.Arrays

import javax.management.Attribute
import javax.management.AttributeList
import javax.management.JMException
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.MBeanOperationInfo
import javax.management.MBeanParameterInfo
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

import org.apache.commons.lang3.{StringUtils=>STU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.MetaUtils._

/**
 * @author kenl
 */
class JMXClient {

  import JMXUtils._

  private var _attributes:Array[MBeanAttributeInfo] = null
  private var _operations:Array[MBeanOperationInfo] = null

  private var _beanConn:MBeanServerConnection = null
  private var _conn:JMXConnector = null
  private var _serviceUrl:JMXServiceURL = null

  /**
   * service:jmx:rmi:///jndi/rmi://hostName:portNumber/jmxrmi
   */

  def this(url:String, user:String, pwd:String ) {
    this()
    iniz(url,user,pwd)
  }

  def this(url:String) {
    this()
    iniz(url, "","")
  }

  private def iniz(url:String, user:String, pwd:String ) {

    tstEStrArg("JMX Url",url)

    val props= if (STU.isEmpty(pwd)) null else {
      Map("jmx.remote.credentials" -> Array(user,pwd))
    }

    try {
      _serviceUrl = new JMXServiceURL(url)
    } catch {
      case e:Throwable =>
      mkJMXrror("Malformed JMX Url: " + url, e)
    }

    try {
      _conn = JMXConnectorFactory.connect( _serviceUrl, props)
      _beanConn = _conn.getMBeanServerConnection
    } catch {
      case e:Throwable =>
        if (_conn != null) block { () =>
          _conn.close()
        }
        _beanConn=null
        _conn=null
        mkJMXrror("Failed while connecting to server", e)
    }
  }

  def this(port:Int) {
    this()
    iniz(fmtSvcUrl("", port),"","")
  }

  def this(host:String, port:Int) {
    this()
    iniz(fmtSvcUrl(host, port),"","")
  }

  private def fmtSvcUrl(host:String , port:Int) = {
    "service:jmx:rmi:///jndi/rmi://" + host+ ":" + port + "/jmxrmi"
  }

  def close(): Unit = synchronized {
    block { () =>

      if (_conn != null) {
        _conn.close()
      }
      _beanConn = null
      _conn = null

    }
  }


  def getBeanDomains() = {

    try {
      _beanConn.getDomains()
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to get domains.", e)
    }
  }

  def getBeanNames() = {

    try {
      _beanConn.queryNames(null, null)
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to get bean names",e)
    }

  }

  def getAttributesInfo(domain:String, bean:String ): Array[MBeanAttributeInfo] = {
    getAttributesInfo(mkObjectName(domain, bean))
  }

  def getAttributesInfo(objName:ObjectName ): Array[MBeanAttributeInfo]  = {

    try {
      _beanConn.getMBeanInfo(objName).getAttributes()
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to get bean info: " + objName, e)
    }
  }

  def getAttributeInfo(objName:ObjectName , attr:String ): MBeanAttributeInfo = {
    getAttrInfo(objName, attr)
  }

  def getOperationsInfo(domain:String , bean:String ):Array[MBeanOperationInfo] = {
    getOperationsInfo(mkObjectName(domain, bean))
  }

  def getOperationsInfo(objName:ObjectName ):Array[MBeanOperationInfo] = {

    try {
      _beanConn.getMBeanInfo(objName).getOperations()
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to get bean information: " + objName, e)
    }
  }

  def getOperationInfo(objName:ObjectName , mtd:String ): Option[MBeanOperationInfo] = {

    val bean = try {
      _beanConn.getMBeanInfo(objName)
    } catch {
      case e:Throwable=>
      mkJMXrror("Failed to get bean information: " + objName, e)
    }
    bean.getOperations().foreach { (op) =>
      if (mtd == op.getName) {
        return Some(op)
      }
    }

    None
  }

  def getAttribute(domain:String , bean:String , attr:String ):Any = {
    getAttribute(mkObjectName(domain, bean), attr)
  }

  def getAttribute(objName:ObjectName , attr:String ): Any = {
    _beanConn.getAttribute(objName, attr)
  }

  def getAttributeString(domain:String, bean:String, attr:String ): String = {
    getAttributeString(mkObjectName(domain, bean), attr)
  }

  def getAttributeString(objName:ObjectName , attr:String ): String = {

    getAttribute(objName, attr) match {
      case b if (b != null) => b.toString()
      case _ => ""
    }
  }

  def getAttributes(objName:ObjectName , attrs:Array[String]):Seq[Attribute] = {
    _beanConn.getAttributes(objName, attrs).asList().toSeq
  }

  def getAttributes(domain:String, bean:String , attrs:Array[String]):Seq[Attribute] = {
    getAttributes(mkObjectName(domain, bean), attrs)
  }

  /*
  def setAttribute(domain:String , bean:String , attr:String , value:String ) {
    setAttribute(mkObjectName(domain, bean), attr, value)
  }
  */
  def setAttribute(domain:String, bean:String , attr:String , value:Any) {
    setAttribute(mkObjectName(domain, bean), attr, value)
  }

  def setAttributeString(objName:ObjectName , attr:String , value:String ) {
    val info = getAttrInfo(objName, attr)
    setAttribute(objName, attr, stringToObject(value, info.getType()))
  }

  def setAttribute(objName:ObjectName , attr:String , value:Any ) {
    val attribute = new Attribute(attr, value)
    _beanConn.setAttribute(objName, attribute)
  }

  def setAttributes(objName:ObjectName , attrs:Seq[Attribute] ) {
    _beanConn.setAttributes(objName, new AttributeList(attrs.toList ))
  }

  def setAttributes(domain:String , bean:String , attrs:Seq[Attribute] ) {
    setAttributes(mkObjectName(domain, bean), attrs)
  }

  def invokeOperationWithString(domain:String , bean:String , mtd:String , pms:String*): Any = {
    invokeOperation(mkObjectName(domain, bean), mtd, pms:_*)
  }

  def invokeOperationWithParams(objName:ObjectName , mtd:String , pms:String*): Any = {
    val paramTypes = lookupParamTypes(objName, mtd, pms:_*)
    val paramObjs= mutable.ArrayBuffer[Any]()
    var i=0
    pms.foreach { (s) =>
      paramObjs += stringToObject(s, paramTypes(i))
      i += 1
    }
    invokeOperation(objName, mtd, paramTypes, paramObjs.toArray)
  }

  def invokeOperationToString(objName:ObjectName , mtd:String , pms:String*) = {
    invokeOperation(objName, mtd, pms:_*).toString
  }

  def invokeOperation(domain:String, bean:String , mtd:String , pms:Any*): Any = {
    invokeOperation(mkObjectName(domain, bean), mtd, pms:_*)
  }

  def invokeOperation(objName:ObjectName , mtd:String , pms:Any*): Any = {
    val paramTypes = lookupParamTypes(objName, mtd, pms:_*)
    invokeOperation(objName, mtd, paramTypes, pms:_*)
  }

  private def invokeOperation(objName:ObjectName , mtd:String , paramTypes:Array[String], params:Any*) = {

    val ps= if (params != null && params.length == 0) {
       null
    } else {
      params.map { (p) => p.asInstanceOf[Object] }            
    }
    _beanConn.invoke(objName, mtd, ps.toArray, paramTypes)
  }

  private def lookupParamTypes(objName:ObjectName , mtd:String , pms:Any*): Array[String] = {

    if (_operations == null) try {
      _operations = _beanConn.getMBeanInfo(objName).getOperations()
    } catch {
      case e:Throwable=>
      mkJMXrror("Failed to get attribute info: " + objName, e)
    }

    val paramTypes = pms.map { (obj) => obj.getClass().toString }
    var nameC = 0
    var first:Array[String] = null

    _operations.foreach { (info) =>

      if (info.getName() == mtd) {
        val sig = info.getSignature()
        if ( sig.length == pms.length ) {
          val sigTypes = sig.map { (s) => s.getType }
          if (Arrays.equals(paramTypes.toArray[Object],
            sigTypes.toArray[Object])) {
            return sigTypes
          }
          first = sigTypes
          nameC += 1
        }
      }
    }

    if (first == null) {
      throw new IllegalArgumentException("Cannot find operation named '" + mtd + "'")
    }

    if (nameC > 1) {
      throw new IllegalArgumentException("Cannot find operation named '" +mtd +
          "' with matching argument types")
    }

    first
  }

  private def getAttrInfo(objName:ObjectName , attr:String ): MBeanAttributeInfo = {
    if (_attributes == null) try {
      _attributes = _beanConn.getMBeanInfo(objName).getAttributes()
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to get attribute info: " + objName, e)
    }

    _attributes.find { (info) => info.getName() == attr } match {
      case Some(x) => x
      case _ => null
    }
  }

  private def stringToObject(str:String, ptype:String ) = {
    ptype match {
      case "boolean" | "java.lang.Boolean" => str.toBoolean
      case "char" | "java.lang.Character" =>
        if (str.length == 0) null else str.toCharArray()(0)
      case "byte" | "java.lang.Byte" => str.toByte
      case "short" | "java.lang.Short" => str.toShort
      case "int" | "java.lang.Integer" => Integer.parseInt(str)
      case "long" | "java.lang.Long" => str.toLong
      case "float" | "java.lang.Float" => str.toFloat
      case "double" | "java.lang.Double" => str.toDouble
      case "java.lang.String" => str
      case _ =>
        getCtor(ptype).newInstance(str)
    }
  }

  private def getCtor(ptype:String ) = {
    loadClass(ptype).getConstructor(classOf[String] )
  }

  private def mkJMXrror(msg:String , e:Throwable) = {
    val err = new JMException(msg)
    err.initCause(e)
    throw err
  }

}
