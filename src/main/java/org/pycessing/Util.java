package org.pycessing;


public class Util {

  public static void log(final String msg) {
    if (Pycessing.VERBOSE) {
      System.out.println(msg);
    }
  }
  
  public static void errLog(final String msg) {
    if (Pycessing.VERBOSE) {
      System.err.println(msg);
    }
  }

}
