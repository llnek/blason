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

package com.zotoh.frwk.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;


/**
 * Wrapper structure to abstract a piece of data which can be a file
 * or a memory byte[].  If the data is byte[], it will also be
 * compressed if a certain threshold is exceeded.
 *
 * @author kenl
 *
 */
public class XData implements java.io.Serializable {

  private static final long serialVersionUID = 3109739389414555705L;
  private int CMPZ_THREADHOLD=1024*1024*4; // 4meg
  private boolean _cmpz=false;
  private boolean _cls=true;

  private String _encoding="UTF-8";
  private String _fp="";

  private long _binSize= 0L;
  private byte[] _bin;

  public byte[] javaBytes() {
    byte[] rc= bytes();
    if (rc == null) { return new byte[0]; } else { return rc; }
  }

  public XData( Object p) throws IOException {
    resetContent(p);
  }
  
  public XData() {
  }

  public void setEncoding(String enc) { _encoding=enc; }

  public String getEncoding() { return _encoding; }

  public boolean isZiped() { return  _cmpz; }

  /**
   * Control the internal file.
   *
   * @param del true to delete, false ignore.
   */
  public XData setDeleteFile(boolean del) {
    _cls= del;
    return this; 
  }

  /**
   * Tests if the file is to be deleted.
   *
   * @return
   */
  public boolean isDeleteFile() { return _cls; }

  /**
   * Clean up.
   */
  public void destroy() {
    if (! StringUtils.isEmpty(_fp) && isDeleteFile() ) {
      FileUtils.deleteQuietly( new File(_fp));
    }
    reset();
  }

  /**
   * Tests if the internal data is a file.
   *
   * @return
   */
  public boolean isDiskFile() { 
    return ! StringUtils.isEmpty( _fp);
  }

  public XData resetContent(Object obj) throws IOException { 
    return resetContent(obj, true);
  }

  public XData resetContent(Object obj, boolean delIfFile) throws IOException {
    destroy();
    if (obj instanceof File[]) {
      File[] fa= (File[]) obj;
      if (fa.length > 0) {
        _fp= fa[0].getCanonicalPath();
      }
    }
    else
    if (obj instanceof ByteArrayOutputStream) {
      ByteArrayOutputStream baos = (ByteArrayOutputStream ) obj;
      maybeCmpz( baos.toByteArray());
    }
    else
    if (obj instanceof byte[]) {
      maybeCmpz( (byte[]) obj);
    }
    else
    if (obj instanceof File) {
      File f= (File) obj;
      _fp= f.getCanonicalPath();
    }
    else
    if (obj instanceof String) {
      String s= (String) obj;
      maybeCmpz( s.getBytes(_encoding));
    }
    else if (obj != null) {
      throw new IOException("Unsupported data type: " + obj.getClass());
    }
    setDeleteFile(delIfFile);
    return this;
  }

  /**
   * Get the internal data.
   *
   * @return
   */
  public Object content() {
    if (isDiskFile() )  {
      return new File( _fp);
    }
    if ( _bin==null) {
      return null;
    }
    if (! _cmpz) {
      return _bin;
    } else {
      try { return IOUtils.gunzip( _bin); } catch (Throwable t) { return null; }
    }
  }

  public boolean hasContent() {
    if (isDiskFile()) { return true; } 
    else 
    if ( _bin !=null) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get the data as bytes.  If it's a file-ref, the entire file content will be read
   * in as byte[].
   *
   * @return
   * @throws IOException
   */
  public byte[] bytes() {
    if (isDiskFile()) {
      try { return FileUtils.readFileToByteArray(new File(_fp)); } catch (Throwable t) { return null; }
    } else {
      return binData();
    }
  }

  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or null.
   */
  public File fileRef() {
    if ( StringUtils.isEmpty(_fp)) { return null; } else { return new File( _fp); }
  }

  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or "".
   */
  public String filePath() {
    if ( StringUtils.isEmpty(_fp)) { return ""; } else { return _fp; }
  }


  /**
   * Get the internal data if it is in memory byte[].
   *
   * @return
   */
  public byte[] binData(){
    if (_bin==null) { return null; } else {
      if (!_cmpz) { return _bin; } else { try { return IOUtils.gunzip( _bin); } catch (Throwable t) { return null; } }
    }
  }

  public long size() {
    if ( isDiskFile() ) { return new File(_fp).length(); } else { return _binSize; }
  }

  public void finalize() {
    destroy();
  }

  public String stringify() {
    try { return new String( javaBytes(), _encoding ); } catch (Throwable t) { return null; }
  }

  public InputStream stream() throws IOException {
    if (isDiskFile()) { return new XStream(new File( _fp)); }
    else if ( _bin==null) { return null; }
    else {
      byte[] bits= _bin;
      if (_cmpz) { bits= IOUtils.gunzip(_bin); }
      return new ByteArrayInputStream( bits);
    }
  }

  private void maybeCmpz(byte[] bits) throws IOException {
    if (bits==null) { _binSize= 0L; } else { _binSize= bits.length; }
    _cmpz=false;
    _bin=bits;
    if (_binSize > CMPZ_THREADHOLD) {
      _cmpz=true;
      _bin= IOUtils.gzip(bits);
    }
  }

  private void reset() {
    _cmpz=false;
    _cls=true;
    _bin=null;
    _fp=null;
    _binSize=0L;
  }

}

