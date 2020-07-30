package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import jep.JepException;
import jep.SharedInterpreter;

class ManagedInterpreterTest {
  
  private static ByteArrayOutputStream outContent;
  private static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private static PrintStream capturedOut;
  private static PrintStream capturedErr;
  
  private static ManagedInterpreter interpreter;
  private static PAppletConnector mockedConnector;
  @TempDir
  public File testDir;
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
    Util.log("captureOutput: outContent");
    outContent = new ByteArrayOutputStream();
    Util.log("captureOutput: errContent");
    errContent = new ByteArrayOutputStream();
    Util.log("captureOutput: capturedOut");
    capturedOut =  new PrintStream(outContent);
    Util.log("captureOutput: capturedErr");
    capturedErr = new PrintStream(errContent);
    Util.log("captureOutput: setOut");
    System.setOut(capturedOut);
    Util.log("captureOutput: setErr");
    System.setErr(capturedErr);
  }
  
  private static void releaseOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }
  
  @BeforeAll
  @Timeout(5)
  static void setUpBeforeClass() throws Exception {
    mockedConnector = Mockito.mock(PAppletConnector.class);
    //Pycessing.VERBOSE=true;
    try {
      interpreter = new ManagedInterpreter(mockedConnector);
    } catch (JepException e) {
      e.printStackTrace();
    }
    //Pycessing.VERBOSE=false;
  }

  @AfterAll
  @Timeout(5)
  static void tearDownAfterClass() throws Exception {
    interpreter.close();
  }

  @BeforeEach
  @Timeout(5)
  void setUp() throws Exception {
    //Pycessing.VERBOSE=true;
    Util.log("setUp: testFilePath");
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    Util.log("setUp: testFile");
    Util.log("setUp: mockedThread");

    Util.log("setUp: captureOutput");
    captureOutput();
  }

  @AfterEach
  @Timeout(10)
  void tearDown() throws Exception {
    Util.log("tearDown: delete file");
    
    Files.deleteIfExists(testFilePath);
    Util.log("tearDown: validateMockitoUsage");
    Mockito.validateMockitoUsage();
    Util.log("tearDown: spiedInterp.close");
    Util.log("tearDown: capture content");
    Pycessing.VERBOSE=false;
    String output = outContent.toString();
    String error = errContent.toString();
    releaseOutput();
    
    if (!output.isEmpty()) {
      System.out.print("\nout:\n-----\n" + output + "\n-----\n");
    }
    
    if (!error.isEmpty()) {
      System.err.print("\nerr:\n-----\n" + error + "\n-----\n");
    }
  }
  
  @Test 
  @Timeout(5)
  public void testOutputCapture() throws JepException {
    //Pycessing.VERBOSE=true;
    interpreter.eval("print('hello world')");
    //interpreter.eval("a = 'hello world'");

    String output = interpreter.getCapturedOutput();
    Util.log("output: '" + output + "'\n'");
    assertEquals("hello world\n", output);
    //assertEquals("hello world\n", interpreter.getValue("a"));
      
    interpreter.eval("print('another hello world')");
    interpreter.eval("print('and then a third hello world')");
    assertEquals("another hello world\nand then a third hello world\n", interpreter.getCapturedOutput());
    // Once more to make sure the buffer is getting flushed
    interpreter.eval("print('test')");
    assertEquals("test\n", interpreter.getCapturedOutput());
  }
  
  @Test
  @Timeout(5) 
  public void testErrorCapture() throws JepException {

    Pycessing.VERBOSE=true;
    
    interpreter.eval("print(\"hello world\", file=sys.stderr)");

    assertEquals("hello world\n", interpreter.getCapturedError());
      
    interpreter.eval("print('another hello world', file=sys.stderr)");
    assertEquals("another hello world\n", interpreter.getCapturedError());
    
  }
  
  @Test
  @Timeout(5)
  public void testQueueToInterpretAndWait() throws JepException, InterruptedException {
    // We have to start our interpreter in a different thread
    class MIThread extends Thread {
      public  ManagedInterpreter i = null;
      public boolean running = false;
      public void run() {
        running=true;
        try {
          i = new ManagedInterpreter(mockedConnector);
        } catch (JepException e) {
          e.printStackTrace();
          failWithMessage("testQueueToInterpretAndWait: MIThread failed to create an interpreter");
        }
        while (running) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
            try {
              i.close();
            } catch (JepException e1) {
              e1.printStackTrace();
              failWithMessage("testQueueToInterpretAndWait: MIThread failed to close an interpreter");
            }
            failWithMessage("testQueueToInterpretAndWait: MIThread was interrupted");
          }
          try {
            i.processQueue();
          } catch (JepException e) {
            e.printStackTrace();
            try {
              i.close();
            } catch (JepException e1) {
              e1.printStackTrace();
              failWithMessage("testQueueToInterpretAndWait: MIThread failed to close an interpreter");
            }
            failWithMessage("testQueueToInterpretAndWait: MIThread failed to process stack");
          }
        }
        try {
          i.close();
        } catch (JepException e) {
          e.printStackTrace();
          failWithMessage("testQueueToInterpretAndWait: MIThread failed to close an interpreter");
        }
      }
    };
    
    MIThread t = new MIThread();
    t.start();
    
    while (t.i == null) {
      Thread.sleep(50);
    }
    
    Pycessing.VERBOSE=true;
    try {
      t.i.queueToInterpretAndWait("print('testQueueToInterpretAndWait test')");
    } catch (NullPointerException e) {
      e.printStackTrace();
      failWithMessage("testQueueToInterpretAndWait: queueToInterpretAndWait caught a NullPointerException");
    }

    String output = t.i.getCapturedOutput();
    assertEquals("testQueueToInterpretAndWait test\n", output);

    t.running=false;
    t.join();
  }
  
  @Test
  @Timeout(5)
  public void testRunScriptNoMain() throws FileNotFoundException, JepException {
    Path testScript = getPythonTestScript("SimplePlainPythonNoMain.py");
      
    interpreter.runScript(testScript.toString());
      
    // simpleTestFunc returns n + 1
    String testFunc = "simpleTestFunc(2)";
    Integer result = interpreter.getValue(testFunc, Integer.class);
    assertEquals(3, result, testFunc + " should return n + 1");
  }
  
  @Test
  @Timeout(5)
  public void testRunScriptWithMain() throws FileNotFoundException, JepException {
    //Pycessing.VERBOSE=true;
      
    Path testScript = getPythonTestScript("SimplePlainPythonWithMain.py");
      
    interpreter.runScript(testScript.toString());
    String output = interpreter.getCapturedOutput();
    Util.log("output: '" + output + "'\n'");
      
    // The script runs simpleTestFunc(2) (same as testRunScriptNoMain) and prints to stdout:
    // "Function returned: 3"

    assertEquals("Function returned: 3\n", output);
    
    
  }

}
