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

import scala.collection.mutable
import java.lang.reflect.Method
import com.zotoh.dbio.meta._




object ClassMetaHolder {
  private val _assocs= mutable.HashMap[String,AssocMetaHolder]()
}

/**
 * Holds annotated information associated with this class.
 *
 * @author kenl
 *
 */
class ClassMetaHolder(z:Class[_]) {

  private val _info= new FMap()
  private var _table=""

  iniz(z)

  /**
   *
   */
  def this() { this(null) }

  def scan(z:Class[_] ): this.type = {
    iniz(z)
    this
  }

  def getTable() = _table

  def getGetter( col:String ) = {
    _info.getViaCol(col).getGetter() 
  }

  def getSetter( col:String ) = {
    _info.getViaCol(col).getSetter()
  }

  def isKey( col:String ) = {
    _info.getViaCol(col).isUniqueKey()
  }

  def getUniques() = {
    val rc= mutable.HashSet[String]()
    _info.foreach { (en) =>
      val ii= en._2
      val m = if (ii.isUniqueKey()) ii.getGetter() else null
      val c= if (m==null) null else m.getAnnotation(classOf[Column])
      if (c != null) {
        rc += c.id().uc
      }
    }
    rc.toSeq
  }

  /**
   * Get the set of methods that are annotated to return
   * database column values.
   *
   * @return
   */
  public Map<String,Method> getGetters() {
      Map<String,Method> rc= MP()
      for (Map.Entry<String, FldMetaHolder> en : _info.entrySet()) {
          Method m= en.getValue().getGetter()
      Column c= m==null ? null : m.getAnnotation(Column.class) 
        if (c != null) {
              rc.put( c.id().toUpperCase() , m)
        }
      }
      return rc
  }

  /**
   * @return
   */
  public static Map<String,AssocMetaHolder> getAssocMetas() { return _assocs; }

  /**
   * @return
   */
  public Map<String,FldMetaHolder> getFldMetas() { return _info; }


  private void iniz( Class<?> z) throws Exception {

  if ( ! StratumObj.class.isAssignableFrom(z)) {
    throw new IllegalArgumentException("Class " + z + " is not a StratumObj")
  }

      Table t= z.getAnnotation(Table.class) 
      if (t==null) { return; }  // skip, not a db table
      _table= t.table()

      Map<String,Method> getters= MP()
      Map<String,Method> mtds= MP()
      Method[] ms= z.getMethods()
      Method m
      for (int i=0; i < ms.length; ++i) {
        m= ms[i]
        mtds.put(m.getName(), m)
      }

      pass1(ms, getters)
      pass2(ms, getters)
      pass3(z, ms, mtds)
      pass4(ms)

    injectSysCols()
  }


  private void pass1(Method[] ms, Map<String,Method> outGetters) throws Exception {
      // scan for "getter(s)", all column defs are bound to getters
      for (int i=0; i < ms.length; ++i) {
          Method m=  ms[i]
          Column c= m.getAnnotation(Column.class) 
          if (c==null) { continue; }
          String cn= c.id().toUpperCase()
          String mn= m.getName()
          if ( !mn.startsWith("get")) {
              throw new Exception("Can only annotate getter(s) :  found : " + mn) 
          }
          FldMetaHolder mi= _info.get(cn)
          if (mi==null) {
              _info.put(cn, (mi=new FldMetaHolder())) 
          }
          else if (mi.getGetter() != null) {
              throw new Exception("Can only annotate column once :  existing getter : "
                      + mi.getGetter().getName() + " , found another : " + mn ) 
          }

          Class<?> rt= m.getReturnType()
          mi.setGetter(m)
          outGetters.put(mn, m)

          if (c.autogen()) {
              if ( ! (Integer.class.equals(rt) ||
                      Long.class.equals(rt) ||
                      int.class.equals(rt) ||
                      long.class.equals(rt)) ) {
                  throw new Exception("Using auto-gen, only int or long are allowed") 
              }

          }
      }
  }

  private void pass2(Method[] ms, Map<String,Method> curGetters) throws Exception {
      // scan for corresponding "setter(s)"
      for (int i=0; i < ms.length; ++i) {
          Method m=  ms[i]
          String mn= m.getName()
          if ( !mn.startsWith("set")) { continue; }
          Method g= curGetters.get( "get" + mn.substring(3) )
          if (g==null) { continue; }
          // ok, got a matching setter
          String col= g.getAnnotation(Column.class).id()
          FldMetaHolder h = _info.getViaCol(col) 
          if (h == null) {
            throw new Exception("Setter found but no field meta: table = " + _table + ", col = " + col) 
          }
          if ( h.getSetter() != null) {
              throw new Exception("Can only have one setter  :  existing setter : "
                              + h.getSetter().getName() + " , found another : " + mn ) 
          }
          h.setSetter(m)
      }
  }

  private void pass3(Class<?> z, Method[] ms, Map<String, Method> allMtds) throws Exception {

    // scan for "assoc(s)" ...

      for (int i=0; i < ms.length; ++i) {
          Method m=  ms[i]
          String mn= m.getName()
          if ( !mn.startsWith("get")) { continue; }

          Many2Many m2m= m.getAnnotation(Many2Many.class) 
          One2One o2o= m.getAnnotation(One2One.class) 
          One2Many o2m= m.getAnnotation(One2Many.class) 
          int count=0
          Class<?> rhs=null
          if (m2m != null) { ++count; rhs=m2m.rhs();  }
          if (o2o != null) { ++count; rhs= o2o.rhs(); }
          if (o2m != null) { ++count; rhs= o2m.rhs(); }
          if (count == 0) { continue; }
          if (count > 1 ) {
            throw new Exception("Cannot have multiple assoc types bound to same getter")
          }
          Method m2= allMtds.get(mn + "FKey")
          if (m2 ==null) {
            throw new Exception("Missing foreign key column getter for assoc : mtd = " + mn) 
          }
          String fkey = (String) m2.invoke( m2.getDeclaringClass().newInstance() )
          Table rt
          if (rhs.isAssignableFrom(z)) {
            // assoc target is a parent of this class, switch to be this class instead
              rt= z.getAnnotation(Table.class)
          } else {
              rt= rhs.getAnnotation(Table.class)
          }

          if (rt == null) {
            throw new Exception("RHS of assoc must have Table annotated")
          }
          String rtb= rt.table().toUpperCase()
        AssocMetaHolder am= _assocs.get(rtb) 
        if (am==null) {
          _assocs.put(rtb, am=new AssocMetaHolder() )
        }
        am.add(m2m != null, z, rhs, fkey) 
      }

  }

  private void pass4(Method[] ms) throws Exception {
  }

  // add in internal cols
  private void injectSysCols() throws Exception {
    FldMetaHolder h = new FldMetaHolder() {
      public boolean isAutoGen() { return true;  }
      public String getId() { return COL_ROWID; }
      public boolean isNullable() { return false; }
      public boolean isPK() { return true; }
      public Class<?> getColType() { return Long.class; }
    }
    _info.put(COL_ROWID, h)

    h = new FldMetaHolder() {
      public boolean isAutoGen() { return false;  }
      public String getId() { return COL_VERID; }
      public boolean isNullable() { return false; }
      public boolean isPK() { return false; }
      public Class<?> getColType() { return Long.class; }
    }
    _info.put(COL_VERID, h)
  }
}


/**
 * @author kenl
 *
 */
class FMap extends HashMap<String, FldMetaHolder> {

  private static final long serialVersionUID = 1L
  public FMap() {}
  public FldMetaHolder getViaCol(String col) {
  FldMetaHolder h= col == null ? null : get( col.toUpperCase() ) 
  return h==null ? FldMetaHolder.DUMBO : h
  }

}

