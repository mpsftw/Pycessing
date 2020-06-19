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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pycessing.ManagedInterpreter.CMDS;
import org.pycessing.ManagedInterpreter.CommunicationContainer;

import jep.JepException;
import jep.SharedInterpreter;

class ManagedInterpreterTest {
  private Thread mockedThread;
  private ManagedInterpreter spiedInterp;
  
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
  
  private void createTestInterpreter() throws JepException {
    spiedInterp = Mockito.spy(new ManagedInterpreter());
    spiedInterp.setDebug(true);
  }

  @BeforeAll
  @Timeout(5)
  static void setUpBeforeClass() throws Exception {
    
  }

  @AfterAll
  @Timeout(5)
  static void tearDownAfterClass() throws Exception {
  }

  @BeforeEach
  @Timeout(5)
  void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    mockedThread = Mockito.mock(Thread.class);
    
    createTestInterpreter();

    captureOutput();
  }

  @AfterEach
  @Timeout(5)
  void tearDown() throws Exception {
    //Pycessing.VERBOSE=false;
    String output = outContent.toString();
    String error = errContent.toString();
    releaseOutput();
    
    if (!output.isEmpty()) {
      System.out.print("\nout:\n-----\n" + output + "\n-----\n");
    }
    
    if (!error.isEmpty()) {
      System.err.print("\nerr:\n-----\n" + error + "\n-----\n");
    }
    
    Files.deleteIfExists(testFilePath);
    Mockito.validateMockitoUsage();
    spiedInterp = null;
  }
  
  @Test 
  @Timeout(5)
  public void testOutputCapture() throws JepException {

    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    spiedInterp.eval("print('hello world')");

    assertEquals("hello world\n", spiedInterp.getOutput());
    
    spiedInterp.eval("print('another hello world')");
    assertEquals("another hello world\n", spiedInterp.getOutput());
  }
  
  @Test
  @Timeout(5) 
  public void testErrorCapture() throws JepException {

    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    spiedInterp.eval("print('hello world', file=sys.stderr)");

    assertEquals("hello world\n", spiedInterp.getErr());
    
    spiedInterp.eval("print('another hello world', file=sys.stderr)");
    assertEquals("another hello world\n", spiedInterp.getErr());
  }
  
  @Test
  @Timeout(5)
  public void testRun() throws JepException, InterruptedException {

    //Pycessing.VERBOSE=true;
    // Make sure running is set to false
    assertFalse(spiedInterp.isRunning(), "Can't test run while spiedInterp.isRunning is true");
    
    // Everything is self contained. There's not really anything to test other than making sure it doesn't 
    // throw an exception or kill the VM (which happens when the exit() is run in the interpreter)
    // If it does kill the VM, maven will crash
    assertDoesNotThrow(() -> {spiedInterp.run();});
    
  }
  
  @Test
  @Timeout(5)
  void testStartStop() throws JepException, InterruptedException {
    //Pycessing.VERBOSE=true;
    Util.log("testStartStop begin");
    
    Thread spiedThread = Mockito.spy(new Thread(spiedInterp));
    
    // Even with the @Timeout, this tends to hang if you get it wrong
    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {spiedInterp.startInterpreter(spiedThread);});
    
    // Make sure start works
    assertTrue(spiedInterp.isRunning(), "testStartStop: isRunning returned false");
    assertTrue(spiedThread.isAlive(), "testStartStop: Thread.isAlive returned false");
    Mockito.verify(spiedThread).start();
    Mockito.verify(spiedInterp).run();
    
    // If run() hasn't reached the runLoop() yet, this will crash maven
    spiedInterp.close();
    assertFalse(spiedInterp.isRunning(), "testStartStop: isRunning returned true after close");
    Mockito.verify(spiedThread).interrupt();
    Mockito.verify(spiedThread).join(5000L);
    
    // Make sure it's gone
    spiedThread.interrupt();
    spiedThread.join();
    assertFalse(spiedThread.isAlive());
    Util.log("testStartStop end");
  }
  
  @Test
  @Timeout(5)
  public void testRunLoop() throws JepException, InterruptedException {
    
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter mockedInterpreter = Mockito.mock(SharedInterpreter.class);
    spiedInterp.setInterpreter(mockedInterpreter);
    
    // This will crash maven if it's wrong
    // If you see a maven crash, it's probably coming from new SharedInterpreter()
    spiedInterp.startInterpreter();
    
    assertTrue(spiedInterp.isRunning(), "testRunLoop: Failed to start interpreter");
    // Make sure it keeps running until it's closed
    Thread.sleep(100);
    assertTrue(spiedInterp.isRunning(), "testRunLoop: Thread died too soon");
    
    spiedInterp.close();
    assertFalse(spiedInterp.isRunning(), "testRunLoop: Thread didn't die");
    
    spiedInterp.setInterpreter(originalInterpreter);
  }
  
  @Test
  @Timeout(5)
  public void testSend() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    CommunicationContainer send = spiedInterp.new CommunicationContainer(CMDS.EVAL);
    send.setStatement("1+1");
    //Pycessing.VERBOSE=true;
    spiedInterp.send(send);
    
    Mockito.verify(spiedInterpreter).eval("1+1");
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testEval() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Boolean recv = spiedInterp.eval("1+1");
    
    assertTrue(recv);
    Mockito.verify(spiedInterpreter).eval("1+1");
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testExec() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    // I wish I could figure out where Jep was sending stdout
    String pythonString="print(\"Test output from python\")\n" +
        "for i in range(4):\n" +
        "  i+1\n";
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(pythonString);
    
    Mockito.verify(spiedInterpreter, Mockito.description("testExec failed to evaluate string")).eval(pythonString);
    
    spiedInterp.close();
  }
 
  @Test
  @Timeout(5)
  public void testGetValue() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Long n=497L;
    String symbol="a";
    String pythonString=symbol + "=" + n;
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(pythonString);
    //Pycessing.VERBOSE=true;
    
    Object returnValue = spiedInterp.getValue(symbol);
    Long returnLong = (Long) returnValue;
    
    Mockito.verify(spiedInterpreter, Mockito.description("testGetValue failed to evaluate string")).eval(pythonString);
    Mockito.verify(spiedInterpreter, Mockito.description("testGetValue failed to getValue")).getValue("a");
    
    assertEquals(returnLong, n);
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testGetVlueWithType() throws JepException, InterruptedException {
    // Numbers default as Longs, so casting to Integer should prove this
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Integer n=2637;
    String symbol="a";
    String pythonString=symbol + "=" + n;
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(pythonString);
    //Pycessing.VERBOSE=true;
    
    Integer returnValue = spiedInterp.getValue(symbol, Integer.class);
    
    Mockito.verify(spiedInterpreter, Mockito.description("testGetVlueWithType failed to evaluate string")).eval(pythonString);
    Mockito.verify(spiedInterpreter, Mockito.description("testGetVlueWithType failed to getValue")).getValue("a", Integer.class);
    
    assertEquals(returnValue, n);
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testInvokeKwargs() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    String kwargsTestFunc = "def testKwargs(**kwargs):\n" + 
        "   a=\"\"\n" + 
        "   for key, value in kwargs.items():\n" + 
        "     a += \"( %s : %s )\" % (key, value)\n" + 
        "   return a";
    
    String func = "testKwargs";
    String name = "test";
    Object object = "this";
    Map<String, Object> kwargs = new HashMap<String, Object>();
    kwargs.put(name, object);
    
    // All of this put together should run the python function:
    // testKwargs(test="this")
    // Which should return a String object:
    // "( test : this )"
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(kwargsTestFunc);
    //Pycessing.VERBOSE=true;
    
    Object returnValue = spiedInterp.invoke(func, kwargs);
    String result = (String) returnValue;
    
    Mockito.verify(spiedInterpreter, Mockito.description("testInvokeKwargs failed to run invoke")).invoke(func, kwargs);
    
    assertEquals(result, "( test : this )");
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testInvokeArgs() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    String argsTestFunc = "def testArgs(*args):\n" + 
        "   a=\"( \"\n" + 
        "   for i in args:\n" + 
        "     a += str(i) + \" \"\n" + 
        "   a += \")\"\n" + 
        "   return a";
    
    String func = "testArgs";
    String name = "test";
    Object object = "this";
    Map<String, Object> kwargs = new HashMap<String, Object>();
    kwargs.put(name, object);
    
    // All of this put together should run the python function:
    // testArgs(1, 2, 3)
    // Which should return a String object:
    // "( 1 2 3 )"
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(argsTestFunc);
    //Pycessing.VERBOSE=true;
    
    Object returnValue = spiedInterp.invoke(func, 1, 2, 3);
    String result = (String) returnValue;
    
    Mockito.verify(spiedInterpreter, Mockito.description("testInvokeKwargs failed to run invoke")).invoke(func, 1, 2, 3);
    
    assertEquals(result, "( 1 2 3 )");
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testInvokeArgsKwargs() throws JepException, InterruptedException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    String argsTestFunc = "def testArgsKwargs(*args, **kwargs):\n" + 
        "   a=\"( ( \"\n" + 
        "   for i in args:\n" + 
        "     a += str(i) + \" \"\n" + 
        "   a += \") \"\n" + 
        "   for key, value in kwargs.items():\n" + 
        "     a += \"( %s : %s ) \" % (key, value)\n" + 
        "   a += \")\"\n" + 
        "   return a";

    String func = "testArgsKwargs";
    
    Object[] args = {1, 2, 3};
    
    Map<String, Object> kwargs = new HashMap<String, Object>();
    kwargs.put("test2", 5);
    kwargs.put("test1", 4);
    
    // All of this put together should run the python function:
    // testArgsKwargs(1,2,3,test1=4,test2=5)
    // Which should return a String object:
    // '( ( 1 2 3 ) ( test2 : 5 ) ( test1 : 4 ) )'
    
    // I don't think there's a guarantee on the position of the 
    // members of kwargs. Might need to change this test into a regex
    
    //Pycessing.VERBOSE=true;
    spiedInterp.exec(argsTestFunc);
    //Pycessing.VERBOSE=true;
    
    Object returnValue = spiedInterp.invoke(func, args, kwargs);
    String result = (String) returnValue;
    
    Mockito.verify(spiedInterpreter, Mockito.description("testInvokeKwargs failed to run invoke")).invoke(func, args, kwargs);
    
    assertEquals("( ( 1 2 3 ) ( test2 : 5 ) ( test1 : 4 ) )", result);
    
    spiedInterp.close();
  }
  
  @Test
  @Timeout(5)
  public void testRunScriptNoMain() throws FileNotFoundException, JepException {
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Path testScript = getPythonTestScript("SimplePlainPythonNoMain.py");
    
    spiedInterp.runScript(testScript.toString());
    Mockito.verify(spiedInterpreter, Mockito.description("Failed to run script")).runScript(testScript.toString());
    
    // simpleTestFunc returns n + 1
    String testFunc = "simpleTestFunc(2)";
    Integer result = spiedInterp.getValue(testFunc, Integer.class);
    assertEquals(3, result, testFunc + " should return n + 1");
  }
  
  //@Test
  @Timeout(5)
  public void testRunScriptWithMain() throws FileNotFoundException, JepException {
    //Pycessing.VERBOSE=true;
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Path testScript = getPythonTestScript("SimplePlainPythonWithMain.py");
    
    // The script runs simpleTestFunc(2) (same as testRunScriptNoMain) and prints to stdout:
    // "Function returned: 3"
    
    String expectedOutput = "Function returned: 3";
    spiedInterp.runScript(testScript.toString());
    Mockito.verify(spiedInterpreter, Mockito.description("Failed to run script")).runScript(testScript.toString());
    
    assertEquals(expectedOutput, spiedInterp.getOutput());
  }
  
  @Test
  @Timeout(5)
  public void testSet() throws JepException {
    //Pycessing.VERBOSE=true;
    spiedInterp.startInterpreter();
    SharedInterpreter originalInterpreter = spiedInterp.getInterpreter();
    SharedInterpreter spiedInterpreter = Mockito.spy(originalInterpreter);
    spiedInterp.setInterpreter(spiedInterpreter);
    
    Integer[] testValue = {1, 2, 3, 4};
    spiedInterp.set("tv", testValue);
    Mockito.verify(spiedInterpreter, Mockito.description("Failed to run set()")).set("tv", testValue);
    
    assertSame(testValue, spiedInterp.getValue("tv"));
    
    testValue[2]=784;
    assertEquals(testValue, spiedInterp.getValue("tv"), "The new value didn't follow");
  }

}
