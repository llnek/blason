
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


object MVCSession {
  private val _log= LoggerFactory.getLogger(classOf[MVCSession])
}

/**
 * @author kenl
 */
class MVCSession { //extends HttpSession {

  private val _attrs= new mutable.HashMap[String, String] with mutable.SynchronizedMap[String, String]{}
  private var _createTS= System.currentTimeMillis()
  private var _lastTS= _createTS
  private var _valid=false
  private var _id=""
  private var _maxIdleMillis= 60*60*24 * 1000L  // one day
  private var _newOne=true
  
  
  
  def getAttribute(name:String): Option[Any] = {
    if (STU.isEmpty(name)) None else _attrs.get(name)
  } 
  
  def getAttributeNames() = _attrs.keySet.toSeq  
  
  def getCreationTime() = _createTS
  
  def getId() = _id
    
  def getLastAccessedTime()  = _lastTS
  
  def getMaxInactiveInterval() = _maxIdleMillis.toInt
  
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
    val n = if (idleSecs < 0) {
     -1
    } else {
      1000L * idleSecs
    }
    tstArg(n > Int.MaxValue, "Value too large for Int type.")
    _maxIdleMillis = n
  }
  
  def contains(name:String): Boolean = {
    if (STU.isEmpty(name)) false else { _attrs.containsKey(name) }
  }
  
  def clear() {    _attrs.clear  }
  
  def save() {
  }

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
