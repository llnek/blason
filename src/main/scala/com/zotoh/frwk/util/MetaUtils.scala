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

import scala.language.existentials
import scala.collection.mutable

import java.lang.reflect.{Field,InvocationTargetException,Method,Modifier}
import com.zotoh.frwk.util.CoreUtils._

import org.apache.commons.lang3.{StringUtils=>STU}

/**
 * Utility functions for class related or reflection related operations.
 *
 * @author kenl
 *
 */
object MetaUtils {

  type ListOfCzs= List[Class[_]]


  def isChild(base:Class[_], c:Class[_]): Boolean = {
    if (c==null) false else base.isAssignableFrom(c)
  }

  def isChild(base:Class[_], obj:Any): Boolean = {
    if (obj==null) false else isChild(base, obj.getClass)
  }

  def isBoolean(z:Class[_]) = {
    z.getName match {
      case "boolean" | "Boolean" | "java.lang.Boolean" => true
      case _ => false
    }
  }

  def isChar(z:Class[_]) = {
    z.getName match {
      case "char" | "Char" | "java.lang.Char" => true
      case _ => false
    }
  }

  def isInt(z:Class[_]) = {
    z.getName match {
      case "int" | "Int" | "java.lang.Integer" => true
      case _ => false
    }
  }

  def isLong(z:Class[_]) = {
    z.getName match {
      case "long" | "Long" | "java.lang.Long" => true
      case _ => false
    }
  }

  def isFloat(z:Class[_]) = {
    z.getName match {
      case "float" | "Float" | "java.lang.Float" => true
      case _ => false
    }
  }

  def isDouble(z:Class[_]) = {
    z.getName match {
      case "double" | "Double" | "java.lang.Double" => true
      case _ => false
    }
  }

  def isByte(z:Class[_]) = {
    z.getName match {
      case "byte" | "Byte" | "java.lang.Byte" => true
      case _ => false
    }
  }

  def isShort(z:Class[_]) = {
    z.getName match {
      case "short" | "Short" | "java.lang.Short" => true
      case _ => false
    }
  }

  def isString(z:Class[_]) = {
    z.getName match {
      case "String" | "java.lang.String" => true
      case _ => false
    }
  }

  def isBytes(z:Class[_]) = {
    if (z == classOf[Array[Byte]]) true else false
  }

  /**
   * @param z
   * @return
   */
  def forName(z:String,cl:ClassLoader=null) = {
    if (cl ==null) Class.forName(z) else Class.forName(z,true,cl)
  }

  /**
   * Get the classloader used by this object.
   *
   * @return
   */
  def getCZldr(cl:ClassLoader = null) = {
    if (cl==null) Thread.currentThread().getContextClassLoader else cl
  }

  def setCZldr(cl:ClassLoader) {
    tstObjArg("class-loader",cl)
    Thread.currentThread().setContextClassLoader( cl)
  }

  /**
   *
   * @param clazz
   * @param ld
   * @return
   */
  def loadClass(clazz:String, cl:ClassLoader=null) = {
    if (STU.isEmpty(clazz)) null else getCZldr(cl).loadClass(clazz)
  }

  /**
   * Create an object of this class, calling the default constructor.
   *
   * @param clazz
   * @param ldr optional.
   * @return
   */
  def mkRef(clazz:String, cl:ClassLoader=null): Any = {
    if (STU.isEmpty(clazz)) null else mkRef( loadClass(clazz, cl))
  }

  /**
   * Create an object of this class, calling the default constructor.
   *
   * @param c
   * @return
   */
  def mkRef(c:Class[_]): Any = {
    c.getDeclaredConstructor().newInstance()
  }

  /**
   * @param c
   * @return
   */
  def listParents(c:Class[_]) = {
    // since we always add the original class
    val a= collPars(c, Nil) match {
      case x :: tail if tail.length > 0 => tail
      case lst => lst
    }
    a.toSeq
  }

  /**
   * @param c
   * @return
   */
  def listMethods(c:Class[_]) = {
    collMtds(c, 0, Map[String,Method]()).values.toSeq
  }

  /**
   * @param c
   * @return
   */
  def listFields(c:Class[_]) = {
    collFlds(c, 0, Map[String,Field]() ).values.toSeq
  }

  private def collPars(c:Class[_], bin:ListOfCzs): ListOfCzs = {
    val par = c.getSuperclass
    var rc= if (par != null) {
      collPars(par, bin)
    } else {
      bin
    }
    c :: rc
  }

  private def collFlds(c:Class[_], level:Int,
          bin:Map[String,Field]):Map[String,Field] = {

    val flds= c.getDeclaredFields
    val par = c.getSuperclass
    var m= if (par != null) { collFlds(par, level +1, bin) } else { bin }

    // we only want the inherited fields from parents
    (m /: flds) { (rc, f) =>
      val x= f.getModifiers
      if (level > 0 &&
         (Modifier.isStatic(x) || Modifier.isPrivate(x)) ) rc  else {
         rc + (f.getName -> f)
      }
    }

  }

  private def collMtds(c:Class[_], level:Int, bin:Map[String,Method]): Map[String,Method] = {
    val mtds= c.getDeclaredMethods
    val par = c.getSuperclass
    var mp = if (par != null) { collMtds(par, level +1, bin) } else { bin }

    // we only want the inherited methods from parents
    (mp /: mtds) { (rc,m) =>
      val x= m.getModifiers
      if (level > 0 &&
        ( Modifier.isStatic(x) || Modifier.isPrivate(x))) rc else {
        rc + (m.getName -> m)
      }
    }
  }

}

