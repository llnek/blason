package com.zotoh.frwk.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author kenl
 *
 */
public enum IOUtils {

  INSTANCE;

  private static volatile File _workDir= new File( System.getProperty("java.io.tmpdir"));
  private static volatile int READ_STREAM_LIMIT = 1024 * 1024 * 8;
  
  public static void setWorkDir(File dir) {
    _workDir=dir;
  }

  public static File getWorkDir() {
    return _workDir;
  }

  public static void setStreamLimit(int limit) {
    READ_STREAM_LIMIT=limit;
  }

  public static int getStreamLimit() {
    return READ_STREAM_LIMIT;
  }

  public static byte[] gzip(byte[] bits) throws IOException {
    if (bits==null) { return null;} else {
      ByteArrayOutputStream baos= new ByteArrayOutputStream(4096);
      GZIPOutputStream g=new GZIPOutputStream(baos);
      g.write(bits, 0, bits.length);
      return baos.toByteArray();
    }
  }

  public static byte[] gunzip(byte[] bits) throws IOException {
    if (bits==null) { return null;} else {
      GZIPInputStream g= new GZIPInputStream( new ByteArrayInputStream(bits));
      return org.apache.commons.io.IOUtils.toByteArray(g);
    }
  }

  public static File mkTempFile(File tmpDir, String pfx, String sux) throws IOException {
    return File.createTempFile(
      StringUtils.isEmpty(pfx) ? "temp-" : pfx,
      StringUtils.isEmpty(sux) ? ".dat" : sux,
      tmpDir);
  }

  public static Object[] newTempFile(File tmpDir) throws IOException {
    return newTempFile(tmpDir, false);
  }

  public static Object[] newTempFile(File tmpDir, boolean open) throws IOException {
    File f= mkTempFile(tmpDir, "", "");
    Object[] rc= new Object[2];
    rc[0]=f;
    rc[1]=null;
    if (open) {
      rc[1]= new FileOutputStream(f);
    }
    return rc;
  }


  public static XData readBytes(InputStream inp) throws IOException {
    return readBytes(inp, false);
  }

  @SuppressWarnings("resource")
  public static XData readBytes(InputStream inp, boolean useFile) throws IOException {
    ByteArrayOutputStream baos= new ByteArrayOutputStream(10000);
    int lmt= READ_STREAM_LIMIT;
    if (useFile) { lmt = 1; }
    byte[] bits= new byte[4096];
    OutputStream os= baos;
    XData rc= new XData();
    int cnt=0;
    boolean loop=true;

    try {
      while (loop) {
        int c= inp.read(bits);
        if (c < 0) {
          loop=false;
        } else {
          if (c > 0) {
            os.write(bits, 0, c);
            cnt += c;
            if ( lmt > 0 && cnt > lmt) {
              os=swap(baos, rc);
              lmt= -1;
            }
          }
        }
      }
      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetContent(baos);
      }
    } finally {
      os.close();
    }

    return rc;
  }

  public static XData readChars(Reader rdr) throws IOException {
    return readChars(rdr, false);
  }

  @SuppressWarnings("resource")
  public static XData readChars(Reader rdr, boolean useFile) throws IOException {
    CharArrayWriter wtr= new CharArrayWriter(10000);
    int lmt = READ_STREAM_LIMIT;
    if (useFile) { lmt = 1; }
    char[] bits= new char[4096];
    Writer w=wtr;
    XData rc= new XData();
    int cnt=0;
    boolean loop=true;

    try {
      while (loop) {
        int c = rdr.read(bits);
        if (c < 0) {
          loop=false;
        } else {
          if (c > 0) {
            w.write(bits, 0, c);
            cnt += c;
            if ( lmt > 0 && cnt > lmt) {
              w=swap(wtr, rc);
              lmt= -1;
            }
          }
        }
      }

      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetContent(wtr.toString());
      }

    } finally {
      w.close();
    }

    return rc;
  }

  private static OutputStream swap( ByteArrayOutputStream baos, XData data) throws IOException {
    Object[] t= newTempFile(_workDir, true);
    byte[] bits=baos.toByteArray();
    OutputStream os= (OutputStream) t[1];
    File f= (File) t[0];
    if ( bits != null && bits.length > 0) {
      os.write(bits);
      os.flush();
    }
    baos.close();
    data.resetContent(f);
    return os;
  }

  private static Writer swap(CharArrayWriter wtr, XData data) throws IOException {
    Object[] t= newTempFile(_workDir, true);
    OutputStream os= (OutputStream) t[1];
    File f= (File) t[0];
    Writer w= new OutputStreamWriter(os);
    data.resetContent(f);
    w.write( wtr.toCharArray() );
    w.flush();
    return w;
  }

}
