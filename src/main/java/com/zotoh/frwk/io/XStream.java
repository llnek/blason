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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Wrapper on top of a File input stream such that it can
 * delete itself from the file system when necessary.
 *
 * @author kenl
 *
 */
public class XStream extends InputStream {

  protected boolean _deleteFile=false;
  protected File _fn;

  private transient InputStream _inp = null;
  protected boolean _closed = true;
  private long pos = 0L;

  public XStream(File f, boolean delFile) {
    _deleteFile=delFile;
    _fn=f;
  }
  
  public XStream(File f) {
    this(f,false);
  }

  public int available() throws IOException {
    pre();
    return _inp.available();
  }

  public int read() throws IOException {
    pre();
    int r = _inp.read();
    pos += 1;
    return r;
  }

  public int read(byte[] b) throws IOException {
    if (b==null) { return -1; } else { return read(b, 0, b.length); }
  }

  public int read(byte[] b, int offset, int len) throws IOException {
    if (b==null) { return -1; } else {
      pre();
      int r = _inp.read(b, offset, len);
      if (r== -1 ) { pos = -1; } else { pos = pos + r; }
      return r;
    }
  }

  /**
   * @param ch
   * @return
   * @throws IOException 
   */
  public char[] readChars(Charset cs, int len) throws IOException {
    char[] rc= new char[0];
    if (len > 0) {
      byte[] b = new byte[len];
      int c = read(b, 0, len);
      if (c>0) {  
        rc= cs.decode( ByteBuffer.wrap(b)).array();
      }
    }
    return rc;
  }

  public long skip(long n) throws IOException {
    if (n < 0L) { return -1L; } else {
      pre();
      long r= _inp.skip(n);
      if (r > 0L) { pos +=  r; }
      return r;
    }
  }

  public void close() {
    IOUtils.closeQuietly(_inp);
    _inp= null;
    _closed= true;
  }

  public void mark(int readLimit) {
    if (_inp != null) {
      _inp.mark(readLimit);
    }
  }

  public void reset() throws FileNotFoundException {
    close();
    _inp= new FileInputStream(_fn);
    _closed=false;
    pos=0;
  }

  public boolean markSupported() { return true; }

  public XStream setDelete(boolean dfile) { 
    _deleteFile = dfile ;
    return this; 
  }

  public void delete() {
    close();
    if (_deleteFile && _fn != null) {
      FileUtils.deleteQuietly(_fn);
    }
  }

  public String filename() throws IOException {
    if (_fn == null) { return ""; } else {
      return _fn.getCanonicalPath();
    }
  }

  public String toString() { 
    try {
      return filename();
    } catch (IOException e) {
      return "";
    } 
  }

  public long getPosition() {  return pos; }

  public void finalize() {
    delete();
  }

  private void pre() throws FileNotFoundException {
    if (_closed) { ready(); }
  }

  private void ready() throws FileNotFoundException {
    reset();
  }

}

