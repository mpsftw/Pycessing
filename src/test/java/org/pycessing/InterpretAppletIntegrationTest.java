package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.time.Duration;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mockito;

import jep.JepException;
import processing.core.PApplet;

class InterpretAppletIntegrationTest {private static ByteArrayOutputStream outContent;
private static ByteArrayOutputStream errContent;
private final static PrintStream originalOut = System.out;
private final static PrintStream originalErr = System.err;
private static PrintStream capturedOut;
private static PrintStream capturedErr;

public ArrayList<String> spiedArgs;
public PAppletConnector applet;
public PAppletConnector spiedApplet;
public ManagedInterpreter spiedInterpreter;

private final static SecurityManager originalSecurityManager = System.getSecurityManager();
private final static SecurityManager disallowExitManager = new DisallowExitSecurityManager(originalSecurityManager);
/*private final static SecurityManager disallowExitManager = new SecurityManager() {
  @Override
  public void checkPermission(Permission perm) 
  {
    //super.checkPermission(perm);
  }
  @Override
  public void checkPermission(Permission perm, Object context) {
    
  }
  @Override
  public void checkExit(int status) {
    //super.checkExit(status);
    throw new SecurityException("System.exit called with status: " + status);
  }
};*/

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
    System.setSecurityManager(disallowExitManager);
  }

  @AfterEach
  void tearDown() throws Exception {
    Util.log("Enabling System.exit");
    System.setSecurityManager(originalSecurityManager);
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
  }
  
  public void setupEnvironment() throws JepException {
    spiedArgs = newSpiedArgs(testFile.getAbsolutePath());
    
    applet = new PAppletConnector();
    spiedApplet = Mockito.spy(applet);
    spiedApplet.setDebug(true);

  }
  
  public void setupEnvironmentWithArgs(ArrayList<String> args) throws JepException {
    spiedArgs = Mockito.spy(args);

    applet = new PAppletConnector(spiedArgs);
    spiedApplet = Mockito.spy(applet);
    spiedApplet.setDebug(true);

  }
  
  private void testScript(String script, String test) throws FileNotFoundException, JepException, InterruptedException {
    Path BasicPythonScript = getPythonTestScript(script);
    ArrayList<String> args = new ArrayList<String>();
    args.add(BasicPythonScript.toString());
    setupEnvironmentWithArgs(args);
    //Pycessing.VERBOSE=true;
    
    applet.loadFile(BasicPythonScript.toString());
    try {
      applet.runSketch();
    } catch (NullPointerException e) { 
      e.printStackTrace();
      failWithMessage(test + " caught a NullPointerException when trying to runSkecth()");
    }
  //wait for it to finish
    applet.waitForFinish();
    System.out.println(test + " finished");
  }
  
  @Test
  @Timeout(10)
  public void testRunScript() throws FileNotFoundException, InterruptedException, JepException, SecurityException {
    //Pycessing.VERBOSE=true;
    testScript("BasicPythonScript.py", "testRunScript");
  }
  
  @Test
  @Timeout(10)
  public void testRunScriptP2D() throws FileNotFoundException, InterruptedException, JepException {
  //Pycessing.VERBOSE=true;
    testScript("BasicPythonP2DScript.py", "testRunScript");
  }
  
  @Test
  @Timeout(10)
  public void testRunScriptP3D() throws FileNotFoundException, InterruptedException, JepException {
  //Pycessing.VERBOSE=true;
    testScript("BasicPythonP3DScript.py", "testRunScript");
  }
  
  @Test
  @Timeout(10)
  public void testRunScriptFX2D() throws FileNotFoundException, InterruptedException, JepException {
  //Pycessing.VERBOSE=true;
    testScript("BasicPythonFX2DScript.py", "testRunScript");
  }
  
  @Test
  public void testREPL() {
    fail("Not implemented");
  }

}
