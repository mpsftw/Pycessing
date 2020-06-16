package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pycessing.PAppletConnector;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PSurface;

public class PAppletConnectorTest {
  private static ByteArrayOutputStream outContent;
  private static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private static PrintStream capturedOut;
  private static PrintStream capturedErr;
  
  private ArrayList<String> spiedArgs;
  private PAppletConnector testApplet;
  private ManagedInterpreter mockedInterpreter;
  
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
  public static void setUpBeforeClass() throws Exception {}

  @AfterAll
  public static void tearDownAfterClass() throws Exception {}

  @BeforeEach
  public void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    spiedArgs = newSpiedArgs(testFile.getAbsolutePath());
    mockedInterpreter = Mockito.mock(ManagedInterpreter.class);
    
    testApplet = new PAppletConnector();
    captureOutput();
  }

  @AfterEach
  public void tearDown() throws Exception {
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
  public void testLoadFileFileExists(@TempDir Path fileDir) throws FileNotFoundException {
    Path filePath = fileDir.resolve("testfile.txt");
    File file = filePath.toFile();
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
      failWithMessage("testLoadFileFileExists failed to create test file:\n" + errContent.toString());
    }
    
    testApplet.loadFile(filePath.toString());
    
    Path returnedFile = testApplet.getSourceFile();
    
    assertEquals(filePath, returnedFile);
  }
  
  @Test
  public void testLoadFileFileDoesNotExist(@TempDir Path fileDir) {
    Path filePath = fileDir.resolve("testfile.txt");
    String pathToFile = filePath.toString();
    String errorMsg = pathToFile + " not found";
    
    FileNotFoundException thrown = assertThrows(FileNotFoundException.class, () -> testApplet.loadFile(pathToFile));
    
    assertTrue(thrown.getLocalizedMessage().matches(errorMsg), "\nExpected: " + errorMsg + "\nRecieved: " + thrown.getLocalizedMessage() + "\n");
  }
  
  @Test
  public void testStart() throws InterruptedException {
    //Trust that Thread.class will run PAppletConnector.run()
    Thread mockedThread = Mockito.mock(Thread.class);
    
    testApplet.startThread(spiedArgs, mockedInterpreter, mockedThread);
    
    Mockito.verify(mockedThread).start();
  }
  
  @Test
  public void testIsAlive() throws InterruptedException {
    PAppletConnector spiedConnector = Mockito.spy(testApplet);
    Thread spiedThread = Mockito.spy(new Thread(spiedConnector));
    
    Mockito.doAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          failWithMessage("testIsAlive sleep caught an interruption: \n" + e.getLocalizedMessage());
        }
        return null;
      }
    }).when(spiedConnector).run();
    
    testApplet.startThread(spiedArgs, mockedInterpreter, spiedThread);
    
    assertTrue(spiedThread.isAlive(), "testIsAlive failed to start a thread to test");
    assertTrue(testApplet.isAlive(), "testIsAlive should return true");
    spiedThread.join();
    
  }
  
  @Test
  public void testIsAliveDeadThread() {
    assertFalse(testApplet.isAlive());
  }
  
  @Test
  public void testNormalStop() throws InterruptedException {
    PAppletConnector spiedConnector = Mockito.spy(testApplet);
    Thread spiedThread = Mockito.spy(new Thread(spiedConnector));
    
    // Run would normally set a PSurface, so we have to mock one
    PSurface mockedSurface = Mockito.mock(PSurface.class);
    spiedConnector.setSurface(mockedSurface);
    
    Mockito.doAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          // This is expected
        }
        return null;
      }
    }).when(spiedConnector).run();
    
    spiedConnector.startThread(spiedArgs, mockedInterpreter, spiedThread);
    
    assertTrue(spiedThread.isAlive(), "testStop failed to start a thread for it to stop");
    
    // I'm getting a null pointer and I need a stack trace
    try {
      spiedConnector.halt();
    } catch (NullPointerException e) {
      e.printStackTrace();
      failWithMessage("testNormalStop: testApplet.halt threw a NullPointerException!\n" + errContent.toString());
    }
    
    assertFalse(spiedThread.isAlive(), "testStop failed to stop thread");
    
  }
  
  @Test
  public void testTimeoutStop() throws InterruptedException {
    // Halt should timeout and return control after 5000 millis
    // Make sure that's happening
    PAppletConnector spiedConnector = Mockito.spy(testApplet);
    Thread spiedThread = Mockito.spy(new Thread(spiedConnector));
    
    // Run would normally set a PSurface, so we have to mock one
    PSurface mockedSurface = Mockito.mock(PSurface.class);
    spiedConnector.setSurface(mockedSurface);
    
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
    
    Mockito.doAnswer(timeoutAnswer).when(spiedConnector).run();
    
    spiedConnector.startThread(spiedArgs, mockedInterpreter, spiedThread);
    
    assertTrue(spiedThread.isAlive(), "testTimeoutStop failed to start a thread for it to stop");
    spiedConnector.halt();
    assertTrue(spiedThread.isAlive(), "testTimeoutStop failed to continue running past timeout");
    
    // Make sure the thread actually goes away
    try {
      spiedThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      failWithMessage("testTimeoutStop: Unable to stop thread - Something is very wrong");
    }
    
    assertFalse(spiedThread.isAlive(), "testTimeoutStop failed to stop thread");
    Mockito.verify(spiedThread).interrupt();
    
  }
  
  @Test
  public void testSetSizeFromSetup() throws FileNotFoundException {
    Path script = getPythonTestScript("SimpleTest.py");
    testApplet.setSizeFromSetup(script);
    
    
    assertEquals(200, testApplet.width);
    assertEquals(200, testApplet.height);
  }
  
  @Test
  public void testSetRenderer() {
    testApplet.setRenderer("JAVA2D");
    assertEquals(testApplet.getRenderer(), PConstants.JAVA2D);
    testApplet.setRenderer("P2D");
    assertEquals(testApplet.getRenderer(), PConstants.P2D);
    testApplet.setRenderer("P3D");
    assertEquals(testApplet.getRenderer(), PConstants.P3D);
    testApplet.setRenderer("FX2D");
    assertEquals(testApplet.getRenderer(), PConstants.FX2D);
    testApplet.setRenderer("SVG");
    assertEquals(testApplet.getRenderer(), PConstants.SVG);
    testApplet.setRenderer("PDF");
    assertEquals(testApplet.getRenderer(), PConstants.PDF);
  }
  
  @Test
  public void testRun() throws FileNotFoundException {
    // this might be better to run with a file creating script?
    // I'm not sure how else to ensure the underlying PApplet has
    // run correctly.
    testApplet.loadFile(getPythonTestScript("SimpleTest.py").toString());
    
    try {
      //testApplet.run();
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("Caught an unexpected error");
    }
    
    
  }

}
