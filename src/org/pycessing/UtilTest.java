package test.java.org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.java.org.pycessing.Pycessing;
import main.java.org.pycessing.Util;

class UtilTest {
  private  static ByteArrayOutputStream outContent;
  private  static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private  static PrintStream capturedOut;
  private  static PrintStream capturedErr;
  
  private static void failWithMessage(String msg) {
    releaseOutput();
    fail(msg);
    captureOutput();
  }
  
  private static void captureOutput() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    capturedOut =  new PrintStream(outContent);
    capturedErr = new PrintStream(errContent);
    System.setOut(capturedOut);
    System.setErr(capturedErr);
  }
  
  private static void releaseOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }
  
  private static void reset() {
    Pycessing.INTERACTIVE=false;
    Pycessing.VERBOSE=false;
    Pycessing.PAppletArgs.clear();
  }

  @BeforeAll
  static void setUpBeforeClass() throws Exception {}

  @AfterAll
  static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  void setUp() throws Exception {
    captureOutput();
  }

  @AfterEach
  void tearDown() throws Exception {
    releaseOutput();
  }

  @Test
  void testLogVerboseOn() {
    Pycessing.VERBOSE=true;
    String outputString = "This is a test";
    Util.log(outputString);
    
    assertEquals(outputString + "\n", outContent.toString());
  }
  
  @Test
  void testLogVerboseOff() {
    Pycessing.VERBOSE=false;
    String outputString = "This is a test";
    Util.log(outputString);
    
    assertEquals("", outContent.toString());
  }

}
