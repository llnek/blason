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

package com.zotoh.dbio.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Use it to provide extra information relating to a database column.
 *  The annotation MUST ONLY be associated with the column's
 *  getter method.
 *
 * @author kenl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Column {

  public String index() default "";

  /**
   * Flag to reflect column's nullability.
   *
   * @return
   */
  public boolean optional() default true;

  /**
   * For varchar, this will be the size of the string.
   *
   * @return
   */
  public int size() default 255;

  /**
   * @return
   */
  public boolean autogen() default false;

  /**
   * For UI
   * @return
   */
  public String desc() default "";

  public boolean readonly() default false;
  public boolean viewable() default true;

  public boolean dft() default false;
  public String dftValue() default "";

  public boolean updatable() default true;
  public boolean system() default false;

}
