package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class InterpretAppletIntegrationTest {private static ByteArrayOutputStream outContent;
private static ByteArrayOutputStream errContent;
private final static PrintStream originalOut = System.out;
private final static PrintStream originalErr = System.err;
private static PrintStream capturedOut;
private static PrintStream capturedErr;

private ArrayList<String> spiedArgs;
private PAppletConnector applet;
private PAppletConnector spiedApplet;
private ManagedInterpreter spiedInterpreter;

@TempDir
public File testDir;
private File testFile;
private Path testFilePath;

private Path getPythonTestScript(String name) throws FileNotFoundException {
  //String path =  getClass().getClassLoader().getResource("").getFile();
  Path path = Paths.get("src", "test", "resources", "PythonTests", name);
  
  if (!Files.exists(path)) {
   throw new FileNotFoundException("File not found: " + path.toFile().getAbsolutePath());
  }
  
  return path;
}

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

private ArrayList<String> newSpiedArgs(String file) {
  ArrayList<String> args = Mockito.spy(new ArrayList<String>());
  args.add("-v");
  args.add(file);
  return args;
}

  @BeforeAll
  static void setUpBeforeClass() throws Exception {}

  @AfterAll
  static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    spiedArgs = newSpiedArgs(testFile.getAbsolutePath());
    
    applet = new PAppletConnector();
    spiedApplet = Mockito.spy(applet);
    spiedApplet.setDebug(true);
    spiedInterpreter = Mockito.spy(spiedApplet.getInterpreter());
    spiedInterpreter.startInterpreter();
    spiedApplet.setInterpreter(spiedInterpreter);
    
    
    captureOutput();
  }

  @AfterEach
  void tearDown() throws Exception {
    String output = outContent.toString();
    String error = errContent.toString();
    releaseOutput();
    
    if (!output.isEmpty()) {
      System.out.print("\nout:\n'" + output + "'\n");
    }
    
    if (!error.isEmpty()) {
      System.err.print("\nerr:\n'" + error + "'\n");
    }
    
    Files.deleteIfExists(testFilePath);
    Mockito.validateMockitoUsage();
    Pycessing.VERBOSE=false;
  }
  
  @Test
  @Timeout(10)
  public void testDraw() {
    //Pycessing.VERBOSE=true;
    Util.log("spiedApplet: " + spiedApplet );
    //Pycessing.VERBOSE=false;
    spiedInterpreter.setPAppletMain(spiedApplet);
    //Pycessing.VERBOSE=true;
    try {
      spiedInterpreter.exec("def setup():\n"
          + "  background(255)\n"
          + "  ellipseMode(CENTER)\n"
          + "  smooth()\n"
          + "  stroke(255,30,30)\n"
          + "  fill(16, 100, 200)\n"
          + "  print('In python setup')\n\n");
      spiedInterpreter.exec("def draw():\n"
          + "  print('In python draw')\n"
          + "  background(255)\n"
          + "  ellipse(50, 50, frameCount, 60)\n"
          + "  if (frameCount > 60):\n"
          + "    exit()\n\n\n");
    } catch (NullPointerException e) {
      e.printStackTrace();
      failWithMessage("testDraw caught a null pointer trying to define draw()");
    }
    try {
      spiedApplet.runSketch();
    } catch (NullPointerException e) {
      e.printStackTrace();
      failWithMessage("testDraw caught a null pointer trying runSketch()");
    }
    
    //wait for it to finish
    int i=0;
    while (!spiedApplet.finished) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      i++;
      if (i > 1000) {
        break;
      }
    }
  }
  
  @Test
  public void testRunScript() {
    fail("Not implemented");
  }
  
  @Test
  public void testREPL() {
    fail("Not implemented");
  }

}
