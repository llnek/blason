
package com.zotoh.blason
package mvc

import scala.collection.JavaConversions._
import scala.collection.mutable
import javax.servlet.http.HttpSession
import org.slf4j._
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.tools.ant.taskdefs.condition.Http
import java.io.IOException
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.security.Crypto._
import com.zotoh.blason.io.HTTPEvent
import com.zotoh.frwk.util.StrUtils._
import java.net.URLEncoder._
import java.net.URLDecoder._
import com.zotoh.blason.io.HTTPResult
import org.jboss.netty.handler.codec.http.DefaultCookie
import org.jboss.netty.handler.codec.http.Cookie
import com.zotoh.frwk.util.UUID

/**
 * @author kenl
 */
object MVCSession {
  
  private val SESSION_COOKIE= "blason_ssref"
  private val SSID_FLAG= "f_01ecf22fdb6c"
  private val TS_FLAG= "f_684f11a0c385"
  private val NV_SEP="\u0000"
    
  def resurrect(evt:HTTPEvent) = {
    val cookie= evt.getCookie(SESSION_COOKIE) match {
      case Some(c) =>nsb ( STU.trim( c.getValue ) )
      case _ =>""
    }
    val netty=evt.emitter.asInstanceOf[NettyMVC]
    val idleSecs=netty.getCacheMaxAgeSecs
    val pos= cookie.indexOf("-")
    var ss= new MVCSession()
    
    val rc = if (pos < 0) ("","") else {
      ( cookie.substring(0,pos),  cookie.substring(pos+1) )
    }
    
    if (STU.isNotEmpty( rc._1) && STU.isNotEmpty( rc._2 )) {
      val k= evt.emitter.container.getAppKey.getBytes("utf-8")
      if ( same( genMAC(k, rc._2) , rc._1) ) {
        STU.split( decode( rc._2, "utf-8") , NV_SEP).foreach { (x) =>
          STU.split(x, ":") match {
            case Array(n,v) =>ss.setAttribute(n, v)
            case _ =>
          }
        }
      }
    }
    
    val expired= ss.getAttribute(TS_FLAG) match {
      case Some(s:String) =>s.toLong < System.currentTimeMillis
      case _ =>if (idleSecs > 0) true else false
    }
    if (expired && idleSecs > 0) {
      ss.setAttribute(TS_FLAG, System.currentTimeMillis  + idleSecs*1000L )
    }    
    
    evt.bindSession(ss)
    evt
  }
  
  private val _log= LoggerFactory.getLogger(classOf[MVCSession])
}

/**
 * @author kenl
 */
class MVCSession { //extends HttpSession {
  import MVCSession._
  private val _attrs= new mutable.HashMap[String, String] with mutable.SynchronizedMap[String, String]{}
  private var _createTS= System.currentTimeMillis()
  private var _lastTS= _createTS
  private var _valid=false
  private var _maxIdleSecs= 60*60 // 1 hour
  private var _newOne=true
  
  setAttribute( SSID_FLAG, UUID.newUUID() )
  
  def mkCookie(key:Array[Byte], ssl:Boolean): Cookie = {
    val b = _attrs.keySet.foldLeft( new StringBuilder ){ (b,k) =>
      if (b.length > 0) { b.append(NV_SEP) }
      b.append(k).append(":").append(_attrs.get(k).get)
      b
    }
    val data = encode(b.toString, "utf-8") 
    val mac=genMAC( key, data)
    val c=new DefaultCookie(SESSION_COOKIE, mac+"-"+data)
    c.setHttpOnly(true)
    c.setSecure(ssl)
    c.setPath("/")
    if (_maxIdleSecs > 0) {
      c.setMaxAge( _maxIdleSecs)      
    }
    c
  }
  
  def getAttribute(name:String): Option[Any] = {
    if (STU.isEmpty(name)) None else _attrs.get(name)
  } 
  
  def getAttributeNames() = _attrs.keySet.toSeq  
  
  def getCreationTime() = _createTS
  
  def getId() = getAttribute(SSID_FLAG).getOrElse("")
    
  def getLastAccessedTime()  = _lastTS
  
  def getMaxInactiveInterval() = _maxIdleSecs.toInt * 1000
  
  def invalidate():Unit  = synchronized {
      _createTS=0L
      _valid=false
      _attrs.clear
      _newOne=true
  }

  def isNew() = _newOne
  
  def removeAttribute(name:String) {
    if ( ! STU.isEmpty(name)) { _attrs.remove(name) }
  }
  
  def setAttribute(name:String, value:Any ) {
    if ( ! STU.isEmpty(name)) {
      if (value == null) {
        _attrs.remove(name)
      } else {
        _attrs.put(name, value)        
      }
    }
  }
  
  def setMaxInactiveInterval( idleSecs:Int) {
    val n = if (idleSecs < 0) -1L else 1000L * idleSecs
    tstArg(n > Int.MaxValue, "Value too large for Int type.")
    _maxIdleSecs = if ( n < 0L ) -1 else idleSecs 
  }
  
  def contains(name:String): Boolean = {
    if (STU.isEmpty(name)) false else { _attrs.containsKey(name) }
  }
  
  def clear() {    _attrs.clear  }

  def isEmpty() = _attrs.size == 0
  
  protected def access() {
      _lastTS = System.currentTimeMillis()
      _newOne = false
  }
  
  // not used
//  def getValueNames(): Array[String] = Array()
//  def getServletContext() = null
//  def getSessionContext() = null
//  def getValue(name:String): Object = null
//  def putValue(name:String, value:Object ) {}
//  def removeValue(name:String) {}

  
  def tlog() = MVCSession._log
}
