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
package core

import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import org.slf4j._
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zotoh.dbio.meta._
import com.zotoh.frwk.util.CoreUtils._
import com.google.common.util.concurrent.ExecutionError
import java.util.concurrent.ExecutionException
import java.net.URL
import com.zotoh.frwk.util.INIConf
import java.io.File


sealed class CacheElementBox(private val _em:Any) extends Object {
  def get() = _em
}

/**
 * @author kenl
 */
object Utils {

  private val _log= LoggerFactory.getLogger(classOf[Utils])
  def tlog() = _log
  
  private var _dbcache:Cache[String,CacheElementBox] = null
    
  // this doesn't work in the way that I want, doesn't expire stuff ????
  def setupCache(cfg:File) {
    val f= new INIConf( cfg)
    val timeToLiveSecs= f.getInt("dbio-memcached", "time_to_live_secs").getOrElse(120)
    val maxItems= f.getInt("dbio-memcached", "max_items").getOrElse(10000)
    val a = CacheBuilder.newBuilder().maximumSize(maxItems).expireAfterAccess(timeToLiveSecs, TimeUnit.SECONDS ).build(
        new CacheLoader[String,CacheElementBox] {
            def load(k:String) = {   throw new ExecutionException() {} }
        }
        )
    _dbcache=a    
    tlog.info("Created cache#dbio-memcached.  maxItems= {}, timeToLiveSecs= {}", maxItems, timeToLiveSecs)
  }
  
  private def safeCacheClear(key:String) {
    try {
      _dbcache.invalidate(key)
    } catch {
      case e:Throwable =>
    }
  }
  
  private def safeCachePut(key:String, v:Any) {
    try {
      _dbcache.put(key, new CacheElementBox(v))
      safeCacheGet(key) // force an access ?
    } catch {
      case e:Throwable => tlog.error("",e)
    }
  }
  
  private def safeCacheGet(key:String) = {
    val rc = try {
      _dbcache.get(key, new Callable[CacheElementBox]() {
        def call() = { throw new ExecutionException() {} }      
      })      
    } catch {
      case e:Throwable => tlog.error("",e); null
    }
    if (rc==null) null else rc.get()
  }
  
  def XputToCache(key:String, v:Option[Any]) {
    if (v.isDefined) {
      tlog.debug("PutCache: key= {}, v = {}", key, v.get.getClass.getName(), "" )            
    } else {
      tlog.debug("PutCache: key= {}, v = None", key)      
    }
    v match {
      case Some(x) => safeCachePut(key, x)
      case _ => safeCacheClear(key)
    }      
  }
  
  def XgetFromCache(key:String): Option[Any] = {
    safeCacheGet(key) match {
      case x:Any => Option(x)
      case null => None
    }
  }  
  
  def XgetCacheSize(): Long =  _dbcache.size()  
  
  def ensureAssoc(m:Method) = {
    val mn=m.getName
    if ( mn.startsWith("dbio_") && mn.endsWith("_fkey") && mn.length > 10 ) {} else {
      throw new Exception("Invalid assoc-fkey marker:  found : " + mn)
    }
    mn
  }
  
  def ensureMarker(m:Method) = {
    val mn=m.getName
    if ( mn.startsWith("dbio_") && mn.endsWith("_column") && mn.length > 12 ) {} else {
      throw new Exception("Invalid marker:  found : " + mn)
    }
    mn
  }


  def fmtMarkerKey(mn:String) =    "dbio_" + mn + "_column"
  def fmtAssocKey(mn:String) =  "dbio_" + mn + "_fkey"

  def splitMarkerKey(mn:String) = mn.substring(5, mn.length - 7)
  def splitAssocKey(mn:String) = mn.substring(5,mn.length - 5)


  def getField(m:Method) = if (m==null) null else m.getAnnotation(classOf[Field])
  def hasField(m:Method) = getField(m) != null

  def getAssocDef(m:Method) = if (m==null) null else m.getAnnotation(classOf[Assoc])
  def hasAssocDef(m:Method) = getAssocDef(m) != null
  
  def getColumn(m:Method) = if (m==null) null else m.getAnnotation(classOf[Column])
  def hasColumn(m:Method) = getColumn(m) != null

  def getTable(z:Class[_]) = {
    if(z==null) null else {
      var t=z.getAnnotation(classOf[Table])
      if (t==null) t = z.getInterfaces().find(  _.getAnnotation(classOf[Table]) != null  ) match {
        case Some(x) => x.getAnnotation(classOf[Table])
        case _ => null
      }
      t
    }
  }
  def hasTable(z:Class[_]) = getTable(z) != null

  def getManifest(z:Class[_]) = if (z==null) null else  z.getAnnotation(classOf[CodeGenManifest])
  def hasManifest(z:Class[_]) = getManifest(z) != null
  
  def getM2M(m:Method) = if (m==null) null else m.getAnnotation(classOf[Many2Many])
  def getO2M(m:Method) = if (m==null) null else m.getAnnotation(classOf[One2Many])
  def getO2O(m:Method) = if (m==null) null else m.getAnnotation(classOf[One2One])

  def hasAssoc(m:Method) = hasO2O(m) || hasO2M(m) || hasM2M(m)
  def hasM2M(m:Method) = getM2M(m) != null
  def hasO2O(m:Method) = getO2O(m) != null
  def hasO2M(m:Method) = getO2M(m) != null

  def throwNoTable(z:Class[_]) = {
    getTable(z) match {
      case x:Table => x
      case _ => throw new Exception("No table annotation for class: " + z)
    }
  }

  def throwNoColumn(m:Method) = {
    getColumn(m) match {
      case x:Column => x
      case _ => throw new Exception("No column annotation for class: " + m.getName)
    }
  }

}

sealed class Utils {}

