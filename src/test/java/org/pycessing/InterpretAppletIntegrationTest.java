package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import processing.core.PApplet;

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
  static void setUpBeforeClass() throws Exception {
    PApplet.useNativeSelect = true;
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    
    captureOutput();
  }

  @AfterEach
  void tearDown() throws Exception {
    Util.log("Releasing output");
    String output = outContent.toString();
    String error = errContent.toString();
    releaseOutput();

    Util.log("Print stdout/stderr");
    if (!output.isEmpty()) {
      System.out.print("\nout:\n'" + output + "'\n");
    }
    
    if (!error.isEmpty()) {
      System.err.print("\nerr:\n'" + error + "'\n");
    }

    Util.log("Deleting temp files");
    Files.deleteIfExists(testFilePath);
    Util.log("validateMockitoUsage");
    Mockito.validateMockitoUsage();
    Util.log("Done");
    Pycessing.VERBOSE=false;
  }
  
  public void setupEnvironment() {
    spiedArgs = newSpiedArgs(testFile.getAbsolutePath());
    
    applet = new PAppletConnector();
    spiedApplet = Mockito.spy(applet);
    spiedApplet.setDebug(true);
    spiedInterpreter = Mockito.spy(spiedApplet.getInterpreter());
    spiedInterpreter.startInterpreter();
    spiedApplet.setInterpreter(spiedInterpreter);

    spiedInterpreter.setPAppletMain(spiedApplet);
  }
  
  public void setupEnvironmentWithArgs(ArrayList<String> args) {
    spiedArgs = Mockito.spy(args);

    applet = new PAppletConnector(spiedArgs);
    spiedApplet = Mockito.spy(applet);
    spiedApplet.setDebug(true);
    spiedInterpreter = Mockito.spy(spiedApplet.getInterpreter());
    spiedInterpreter.startInterpreter();
    spiedApplet.setInterpreter(spiedInterpreter);

    spiedInterpreter.setPAppletMain(spiedApplet);
  }
  
  @Test
  @Timeout(10)
  public void testDraw() throws InterruptedException {
    setupEnvironment();
    //Pycessing.VERBOSE=true;
    Util.log("spiedApplet: " + spiedApplet );
    //Pycessing.VERBOSE=false;
    //spiedInterpreter.setPAppletMain(spiedApplet);
    //Pycessing.VERBOSE=true;
    try {
      spiedInterpreter.exec("def setup():\n"
          //+ "  print('In python setup\\n')\n"
          + "  background(255)\n"
          + "  ellipseMode(CENTER)\n"
          + "  smooth()\n"
          + "  stroke(255,30,30)\n"
          + "  fill(16, 100, 200)\n\n");
      spiedInterpreter.exec("def draw():\n"
          //+ "  print('In python draw\\n')\n"
          + "  background(255)\n"
          + "  ellipse(50, 50, frameCount, 60)\n"
          + "  if (frameCount > 60):\n"
          //+ "    print('exiting\\n')\n"
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
    spiedApplet.waitForFinish();
  }
  
  @Test
  @Timeout(10)
  public void testRunScript() throws FileNotFoundException, InterruptedException {
    Path BasicPythonScript = getPythonTestScript("BasicPythonScript.py");
    ArrayList<String> args = new ArrayList<String>();
    args.add(BasicPythonScript.toString());
    setupEnvironmentWithArgs(args);
    //Pycessing.VERBOSE=true;
    
    spiedApplet.loadFile(BasicPythonScript.toString());
    spiedApplet.runSketch();
    
  //wait for it to finish
    spiedApplet.waitForFinish();
    System.out.println("testRunScript finished");
  }
  
  @Test
  //@Timeout(10)
  public void testRunScriptP2D() throws FileNotFoundException, InterruptedException {
    Path BasicPythonScript = getPythonTestScript("BasicPythonP2DScript.py");
    ArrayList<String> args = new ArrayList<String>();
    args.add(BasicPythonScript.toString());
    //setupEnvironmentWithArgs(args);
    PAppletConnector myApplet = new PAppletConnector(args);
    myApplet.getInterpreter().startInterpreter();
    
    //Pycessing.VERBOSE=true;
    
    myApplet.loadFile(BasicPythonScript.toString());
    //assertTimeout(Duration.ofSeconds(5), () -> {
    try {
      myApplet.runSketch();
    } catch (NullPointerException e) {
      e.printStackTrace();
      failWithMessage("testRunScriptP2D caught null pointer");
    }
    //});
    
  //wait for it to finish
    assertTimeout(Duration.ofSeconds(5), () -> {
      myApplet.waitForFinish();
    });
  }
  
  //@Test
  public void testREPL() {
    fail("Not implemented");
  }

}
