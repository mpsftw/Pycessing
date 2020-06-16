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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import jep.JepException;
import jep.SharedInterpreter;

class ManagedInterpreterTest {
  private ManagedInterpreter testInterp;
  private Thread mockedThread;
  private PAppletConnector mockedApplet;
  
  private static ByteArrayOutputStream outContent;
  private static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private static PrintStream capturedOut;
  private static PrintStream capturedErr;
  
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
  
  private void createTestInterpreter() {
    mockedApplet = Mockito.mock(PAppletConnector.class);
    testInterp = new ManagedInterpreter(mockedApplet);
    mockedThread = Mockito.mock(Thread.class);
  }

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    
    createTestInterpreter();

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
  }

  @Test
  void testStartStop() throws JepException, InterruptedException {
    testInterp.startThread(mockedThread);
    Mockito.verify(mockedThread).start();
    assertTrue(testInterp.isRunning(), "testStartStop: testInterp failed to set running to true");
    
    testInterp.halt();
    
    Mockito.verify(mockedThread).join(5000L);
    assertFalse(testInterp.isRunning(), "testStartStop: testInterp failed to set running to false");
  }
  
  @Test
  void testStartStopTimeoutInterrupt() throws JepException, InterruptedException {
    ManagedInterpreter spiedInterp = Mockito.spy(testInterp);
    Answer<Void> timeoutAnswer = new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        try {
          Thread.sleep(6000);
        } catch (InterruptedException e) {
          try {
            // Make sure the thread doesn't actually die
            Thread.sleep(6000);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
            failWithMessage("Second sleep is still getting interrupted");
          }
        }
        return null;
      }
    };

    Mockito.doAnswer(timeoutAnswer).when(spiedInterp).run();
    Thread spiedThread = Mockito.spy(new Thread(spiedInterp));
    
    testInterp.startThread(spiedThread);
    
    assertTrue(spiedThread.isAlive(), "testStartStopTimeoutInterrupt: testInterp failed to start a thread for it to stop");
    testInterp.halt();
    Mockito.verify(spiedThread, Mockito.atLeast(1)).interrupt();
    // Halt should return before the thread has finished. 
    assertTrue(spiedThread.isAlive(), "testStartStopTimeoutInterrupt: failed to continue running past timeout");
    
    // Make sure thread actually goes away
    spiedThread.join();
    assertFalse(spiedThread.isAlive(), "testStartStopTimeoutInterrupt: testInterp failed to stop thread");
  }
  
  @Test
  public void testRun() throws JepException, InterruptedException {
    // Tell it we're testing
    ManagedInterpreter.setDebug(true);
    
    // Make sure running is set to false
    assertFalse(testInterp.isRunning(), "Can't test run while testInterp.isRunning is true");
    SharedInterpreter originalInterpreter = ManagedInterpreter.getInterpreter();
    SharedInterpreter mockedInterpreter = Mockito.mock(SharedInterpreter.class);
    ManagedInterpreter.setInterpreter(mockedInterpreter);
    
    testInterp.run();
    
    Mockito.verify(mockedInterpreter).eval("exit()");
    Mockito.verify(mockedInterpreter).close();
    ManagedInterpreter.setInterpreter(originalInterpreter);
    ManagedInterpreter.setDebug(false);
  }
 

}
