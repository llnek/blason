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

import scala.collection.mutable

import java.io.IOException
import java.lang.management.ManagementFactory
import java.net.MalformedURLException
import java.rmi.NoSuchObjectException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject

import javax.management.JMException
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.remote.JMXConnectorServer
import javax.management.remote.JMXConnectorServerFactory
import javax.management.remote.JMXServiceURL

import com.zotoh.frwk.util.CoreUtils._

/**
 * @author kenl
 */
class JMXServer(private var _host:String="localhost") {

  import JMXUtils._

  private val _objNames= mutable.HashSet[ObjectName]()
  private var _conn:JMXConnectorServer = null
  private var _rmi:Registry = null
  private var _beanSvr:MBeanServer = null

  private var _registryPort:Int = 7777
  private var _serverPort:Int = 0

  def clear(): Unit = synchronized {
    _objNames.foreach { (n) =>
      block { () => _beanSvr.unregisterMBean(n) }
    }
    _objNames.clear()
  }

  def start(): Unit = synchronized {
    startRMI()
    startJMX()
  }

  def stop(): Unit = synchronized {
    block { () =>
      
      clear()
      
      if (_conn != null) try {
        _conn.stop()
      } catch {
        case e:Throwable => tlog.error("",e)
      }
      _conn=null

      if (_rmi != null) try {
        UnicastRemoteObject.unexportObject(_rmi, true)
      } catch {
        case e:Throwable => tlog.error("",e)
      }
      _rmi=null

    }
  }

  def register(obj:Any): Unit = {
    register(obj,"","")
  }

  def register(obj:Any, domain:String, name:String): Unit = {
    register(obj,domain,name,Array())
  }

  def register(obj:Any, domain:String, name:String, paths:Array[String]): Unit = synchronized {
    try {
      doReg( inferObjectNameEx(obj, domain,name,paths), new JMXBean(obj) )
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to register object: " + obj, e)
    }
  }

  def unregister(obj:Object ) {
    block { () =>
      unreg(obj)
    }
  }

  def unregister(objName:ObjectName ) {
    block { () =>
      unregName(objName)
    }
  }

  def unreg(obj:Any ): Unit = synchronized {
    _beanSvr.unregisterMBean( inferObjectName(obj) )
  }

  def unregName(objName:ObjectName ): Unit = synchronized {
    _beanSvr.unregisterMBean(objName)
  }

  def setRegistryPort(port:Int) = {
    _registryPort = port  // jconsole port
    this
  }

  def setServerPort(port:Int) = {
    _serverPort = port
    this
  }

  private def doReg(objName:ObjectName , bean:JMXBean) {
    try {
      _beanSvr.registerMBean(bean, objName)
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to register bean: " + objName, e)
    }
    _objNames.add(objName)
    tlog.info("Registered JMXBean: {}", objName.toString )
  }

  private def startRMI() {
    if (_rmi == null) try {
      _rmi = LocateRegistry.createRegistry(_registryPort)
    } catch {
      case e:Throwable =>
      mkJMXrror("Failed to create RMI registry: " + _registryPort, e)
    }
  }

  private def startJMX() {
    if (_conn == null) {
      if (_serverPort <= 0) { _serverPort = _registryPort + 1 }

      var endpt = "service:jmx:rmi://"+_host+":" + _serverPort + "/jndi/rmi://:" + _registryPort + "/jmxrmi"
      val url = try {
        new JMXServiceURL(endpt)
      } catch {
        case e:Throwable =>
        mkJMXrror("Malformed url: " + endpt, e)
      }

      _conn = try {
        JMXConnectorServerFactory.newJMXConnectorServer(url, null, ManagementFactory.getPlatformMBeanServer )
      } catch {
        case e:Throwable =>
        mkJMXrror("Failed to connect JMX", e)
      }

      try {
        _conn.start()
      } catch {
        case e:Throwable =>
        _conn = null
        mkJMXrror("Failed to start JMX",e)
      }

      _beanSvr = _conn.getMBeanServer()
    }

  }

  private def mkJMXrror(msg:String , e:Throwable )  = {
    val err = new JMException(msg)
    err.initCause(e)
    throw err
  }

}
