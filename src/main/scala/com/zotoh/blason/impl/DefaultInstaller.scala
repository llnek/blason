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

package com.zotoh.blason
package impl

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.io.{FileUtils=>FUT}
import org.apache.commons.io.{IOUtils=>IOU}
import java.net.URL
import org.apache.commons.io.FilenameUtils
import java.util.zip.ZipFile
import java.io.File
import java.util.zip.ZipEntry
import com.zotoh.frwk.util.CoreImplicits
import com.zotoh.frwk.util.CoreUtils._
import com.zotoh.frwk.util.FileUtils._
import java.io.FileOutputStream


class DefaultInstaller(private val _baseDir:File, private val _workDir:File) extends CoreImplicits {
}

class InstallInfo(private val _srcUrl:URL) {
}

object DefaultInstaller {
}


