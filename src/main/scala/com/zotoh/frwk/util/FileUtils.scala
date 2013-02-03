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

import scala.collection.JavaConversions._
import scala.math._
import java.io.{File,FileInputStream,FileOutputStream,InputStream,OutputStream,IOException}
import org.apache.commons.io.{FileUtils=>FUS}
import org.apache.commons.lang3.{StringUtils=>STU}
import org.apache.commons.io.{IOUtils=>IOU}
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.StrUtils._
import com.zotoh.frwk.io.IOUtils._
import java.util.zip.ZipFile
import java.util.zip.ZipEntry

/**
 * @author kenl
 *
 */
object FileUtils {

  /**
   * @param fp
   * @return
   */
  def isFileWR(fp:File) = {
    fp != null && fp.exists() && fp.isFile() && fp.canRead() && fp.canWrite()
  }

  def isFileR(fp:File) = {
    fp != null && fp.exists() && fp.isFile() && fp.canRead()
  }

  /**
   * @param dir
   * @return
   */
  def isDirWR(dir:File) = {
    dir != null  && dir.exists() && dir.isDirectory() && dir.canRead() && dir.canWrite()
  }

  def isDirR(fp:File) = {
    fp != null && fp.exists() && fp.isDirectory() && fp.canRead()
  }

  def canExec(fp:File) = {
    fp != null && fp.exists() && fp.canExecute()
  }

  /**
   * @return Current working directory.
   */
  def getCWD() =  new File(sysQuirk("user.dir"))


  /**
   * @param path
   * @return
   */
  def parentPath(path:String) = {
    if ( ! STU.isEmpty(path)) {
      new File(path).getParent()
    } else {
      path
    }
  }

  def extractAll(src:ZipFile,des:File) {
    des.mkdirs
    src.entries.foreach { (en) =>
      val f=new File(des, jiggleZipEntryName(en))
      if (en.isDirectory) {
        f.mkdirs
      } else {
        f.getParentFile().mkdirs
        using(src.getInputStream(en)) { (inp) =>
        using(new FileOutputStream(f))  { (os) =>
          IOU.copy(inp, os)
        }}
      }
    }
  }

  private def jiggleZipEntryName(en:ZipEntry) ={
    en.getName().replaceAll("^[\\/]+","")
  }

}

