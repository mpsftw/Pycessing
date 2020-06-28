package org.pycessing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jep.Jep;
import jep.JepException;
import jep.SharedInterpreter;
import processing.core.PApplet;
import processing.core.PConstants;

public class ManagedInterpreter implements Runnable {
  
  // There can be only one!
  private SharedInterpreter interpreter;
  private Boolean debug=false;

  private Thread thread = null;
  private boolean running=false;
  private boolean interactive=false;
  private boolean mainset=false;
  private CommunicationContainer communication = null;
  private boolean queuedToInterpret=false;
  private final Object interpretLock = new Object();
  private final CountDownLatch startLatch = new CountDownLatch(1);
  private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private PrintStream capturedOut =  new PrintStream(outContent);
  private PrintStream capturedErr = new PrintStream(errContent);
  
  public ManagedInterpreter() {
    interpreter=null;
  }
  
  public void setDebug(Boolean d) {
    debug=d;
  }
  
  public PrintStream getCapturedOut() {
    return capturedOut;
  }
  
  public PrintStream getCapturedErr() {
    return capturedErr;
  }
  
  public SharedInterpreter getInterpreter() {
    return interpreter;
  }
  
  public void setInterpreter(SharedInterpreter i) {
    if (!debug) {
      throw new RuntimeException("Setting the interpreter will break things. Only use this to debug the pycessing interpreter.");
    }
    interpreter=i;
  }
  
  public Thread getThread() {
    return thread;
  }
  
  public void setThread(Thread t) {
    thread = t;
  }
  
  public void captureOutout() throws JepException {
    interpreter.exec("import sys\nfrom io import StringIO\n");
    String outputCapture = 
        "class SplitIO(StringIO):\n" + 
        "   def __init__(self, otherOut):\n" + 
        "     self.otherOut = otherOut\n" +
        "     StringIO.__init__(self)\n" + 
        "   def  write(self, string):\n" + 
        "     self.otherOut.write(string)\n" + 
        "     StringIO.write(self, string)\n" + 
        "   def flush(self):\n" + 
        "     sys.__stdout__.flush()\n" + 
        "     StringIO.flush(self)\n" +
        "   def getvalue(self):\n" +
        "     value = StringIO.getvalue(self)\n" +
        "     StringIO.truncate(self, 0)\n" +
        "     StringIO.seek(self, 0)\n" +
        "     return value\n" +
        "__jepout__ = SplitIO(sys.stdout)\n" +
        "__jeperr__ = SplitIO(sys.stderr)\n" +
        "sys.stdout = __jepout__\n" +
        "sys.stderr = __jeperr__\n";
    
    interpreter.exec(outputCapture);
  }
  
  public String getOutput() {
    return getValue("__jepout__.getvalue()", String.class);
  }
  
  public String getErr() {
    return getValue("__jeperr__.getvalue()", String.class);
  }
  
  public void startInterpreter() {
    Util.log("startInterpreter: starting with running=" + running);
    if (running) {
      Util.log("startInterpreter: looks like we're already running. Returning without doing anything.");
      return;
    }
    try {
      startInterpreter(new Thread(this));
    } catch (NullPointerException e) {
      e.printStackTrace();
      return;
    }
  }

  public void startInterpreter(Thread t) {
    if (running) {
      return;
    }
    running=true;
    thread = t;
    
    // Wait until everything is set up.
    Util.log("startInterpreter: locking runningLock");
    thread.start();
    Util.log("startInterpreter: starting while loop");
    Util.log("startInterpreter: waiting startLatch");
    try {
      startLatch.await();
    } catch (InterruptedException e) {
      running=false;
      return;
    }
    Util.log("startInterpreter: out of wait");
    Util.log("startInterpreter: Finished. Thread is alive: " + thread.isAlive());
  }
  
  public synchronized void close() throws JepException, InterruptedException {
    running = false;
    if (thread == null) {
      return;
    }
    if (thread.isAlive()) {
      thread.interrupt();
    }
    thread.join(5000L);
  }
  
  @Override
  public void run() {
    
    //Setup - this takes some time, but it MUST happen in the interpreter thread
    try {
      interpreter = new SharedInterpreter();
    } catch (JepException e3) {
      // TODO Auto-generated catch block
      e3.printStackTrace();
    }
    try {
      initializeInterpreter(interpreter);
    } catch (JepException e2) {
      //PythonException.raiseJepException(e2);
      e2.printStackTrace();
      return;
    }
    Util.log("Interpreter initialized");

    Util.log("run: counting down latch inRunLoop");
    startLatch.countDown();
    
    try {
      runLoop();
    } catch (InterruptedException e1) {
      Util.log("run: Caught interruption! Returning.");
      running=false;
    }

    // Try to be nice
    try {
      // sending exit() crashes the interpreter. That could be a problem.
      // interpreter.eval("exit()");
      Util.log("run: closing interpreter");
      interpreter.close();
      interpreter = null;
    } catch (JepException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Util.log("run: Returning.");
  }

  private void runLoop() throws InterruptedException {
    // Let everyone know we're here and looping
    Util.log("runLoop: entering while loop queuedToInterpret: " + queuedToInterpret);
    while (running) {
      Util.log("runLoop:Top of loop quedToInterpret: " + queuedToInterpret);
      CommunicationContainer c = null;

      Util.log("runLoop: locking interpretLock queuedToInterpret: " + queuedToInterpret);
      synchronized (interpretLock) {
        while (!queuedToInterpret) {
          Util.log("runLoop: waiting queuedToInterpret: " + queuedToInterpret);
          interpretLock.wait();
        }
        Util.log("runLoop: interpreting queuedToInterpret: " + queuedToInterpret);
        interpret(communication);
        queuedToInterpret=false;
        Util.log("runLoop: End of loop queuedToInterpret: " + queuedToInterpret);
        interpretLock.notifyAll();
      }
      Util.log("runLoop:Bottom of loop quedToInterpret: " + queuedToInterpret);
    }
  }

  @SuppressWarnings("unchecked")
  private CommunicationContainer interpret(CommunicationContainer comm) {
    try {
      switch (comm.getCmd()) {
        case EVAL: 
          Util.log("ManagedInterpreter.interpret eval");
          try {
            comm.setObj(interpreter.eval(comm.getStatement()));
          } catch (JepException e) {
            comm.setException(e);
          }
          break;
        case EXEC: 
          Util.log("ManagedInterpreter.interpret exec");
          interpreter.eval(comm.getStatement());
          break;
        case GETVALUE: 
          Util.log("ManagedInterpreter.interpret getValue");
          comm.setObj(interpreter.getValue(comm.getStatement()));
          break;
        case GETVALUEWITHTYPE:  
          Util.log("ManagedInterpreter.interpret GETVALUEWITHTYPE");
          comm.setObj(interpreter.getValue(comm.getStatement(), comm.getType()));
          break;
        case INVOKEKWARGS:   
          Util.log("ManagedInterpreter.interpret INVOKEKWARGS");
          comm.setObj(interpreter.invoke(comm.getStatement(), comm.getKwargs()));
          break;
        case INVOKEARGS:   
          Util.log("ManagedInterpreter.interpret INVOKEARGS");
          comm.setObj(interpreter.invoke(comm.getStatement(), comm.getArgs()));
          break;
        case INVOKEARGSKWARGS:    
          Util.log("ManagedInterpreter.interpret INVOKEARGSKWARGS");
          comm.setObj(interpreter.invoke(comm.getStatement(), comm.getArgs(), comm.getKwargs()));
          break;
        case RUNSCRIPT:     
          Util.log("ManagedInterpreter.interpret RUNSCRIPT");
          interpreter.runScript(comm.getStatement());
          break;
        case SET:      
          Util.log("ManagedInterpreter.interpret SET");
          interpreter.set(comm.getStatement(), comm.getValue());
          break;
      }
    } catch (JepException e) {
      comm.setException(e);
    }
    Util.log("ManagedInterpreter.interpret returning comm");
    return comm;
  }

  public void initializeInterpreter(SharedInterpreter interp) throws JepException {
    interp.set("interpreter", interp);
    interp.set("managedinterpreter", this);
    if (!Pycessing.INTERACTIVE) {
      captureOutout();
    }
  }
  
  public synchronized Boolean isRunning() {
    return running;
  }

  public synchronized void startREPL() {
    // TODO Auto-generated method stub
    
  }
  
  @SuppressWarnings("unchecked")
  protected synchronized <T> CommunicationContainer<T> send(CommunicationContainer<T> c) throws InterruptedException {
    Util.log("In send with command: " + c.getCmd());
    Util.log("send: locking interpretLock queuedToInterpret: " + queuedToInterpret);
    synchronized(interpretLock) {
      Util.log("send: setting communication queuedToInterpret: " + queuedToInterpret);
      communication=c;
      queuedToInterpret=true;
      Util.log("send: notifying interpretLock queuedToInterpret: " + queuedToInterpret);
      interpretLock.notifyAll();
    }

    Util.log("send: locking interpretLock queuedToInterpret: " + queuedToInterpret);
    synchronized(interpretLock) {
      while (queuedToInterpret) {
        Util.log("send: waiting interpretLock queuedToInterpret: " + queuedToInterpret);
        interpretLock.wait();
      }
      c=communication;
    }
    Util.log("send: leaving command " + c.getCmd() + " with return value: " + c.getValue() + " and exception " + c.exception + " queuedToInterpret: " + queuedToInterpret);
    
    return c;
  }
  
  
  public synchronized Boolean eval(String statement) {
    Util.log("eval(statements) with: " + statement);
    Boolean returnValue=null;
    
    CommunicationContainer<Boolean> comm = new CommunicationContainer<Boolean>(CMDS.EVAL);
    comm.setStatement(statement);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return returnValue;
    }
    
    returnValue=(Boolean) comm.getValue();
    
    return returnValue;
  }
  
  public synchronized void exec(String statements) {
    Util.log("exec(statements) with: " + statements);
    Util.log("Running: " + thread.isAlive());
    CommunicationContainer<Boolean> comm = new CommunicationContainer<Boolean>(CMDS.EXEC);
    comm.setStatement(statements);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return;
    }
  }
  
  public synchronized Object getValue(String symbol) {
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.GETVALUE);
    comm.setStatement(symbol);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return comm.getValue();
    }
    return comm.getValue();
  }
  
  public synchronized <T> T getValue(String symbol, Class<T> clazz) {
    // I know the temptation is to be lazy and cast the result from getValue
    // Really, I do. I tried that too.
    // Jep automatically casts some things, so if you don't do this the right way
    // you won't be able to get Integers (because they'll be cast to Long)
    CommunicationContainer<T> comm = new CommunicationContainer<T>(CMDS.GETVALUEWITHTYPE);
    comm.setStatement(symbol);
    comm.setType(clazz);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return clazz.cast(comm.getValue());
    }
    return clazz.cast(comm.getValue());
  }
  
  public synchronized Object invoke(String symbol, Map<String, Object> kwargs) {
    Util.log("invoke(symbol, kwargs) with: " + symbol + " " + kwargs);
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.INVOKEKWARGS);
    comm.setStatement(symbol);
    comm.setKwargs(kwargs);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return comm.getValue();
    }
    return comm.getValue();
  }
  
  public synchronized Object invoke(String symbol, Object... args) {
    Util.log("invoke(symbol, args) with: " + symbol + " " + args);
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.INVOKEARGS);
    comm.setStatement(symbol);
    comm.setArgs(args);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return comm.getValue();
    }
    return comm.getValue();
  }
  
  public synchronized Object invoke(String symbol, Object[] args, Map<String, Object> kwargs) {
    Util.log("invoke(symbol, args, kwargs) with: " + symbol + " " + args + " " + kwargs);
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.INVOKEARGSKWARGS);
    comm.setStatement(symbol);
    comm.setArgs(args);
    comm.setKwargs(kwargs);

    Util.log("invoke sending");
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      Util.log("invoke interrupted! returning.");
      running=false;
      return comm.getValue();
    }
    Util.log("invoke returning");
    return comm.getValue();
  }
  
  public synchronized void runScript(String script) {
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.RUNSCRIPT);
    comm.setStatement(script);
    
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return; //(String) comm.getValue();
    }
    return; //(String) comm.getValue();
  }
  
  public synchronized void set(String symbol, Object obj) {
    CommunicationContainer<Object> comm = new CommunicationContainer<Object>(CMDS.SET);
    comm.setStatement(symbol);
    comm.setObj(obj);
    try {
      comm=send(comm);
    } catch (InterruptedException e) {
      running=false;
      return;
    }
    return;
  }
  
  protected class CommunicationContainer<T> {
    private String statement=null;
    private Object value=null;
    private Class<T> type;
    public JepException exception=null;
    private Object[] args;
    private Map<String, Object> kwargs;
    private final CMDS cmd;
    
    public CommunicationContainer(CMDS c) {
      cmd=c;
    }
    
    public void setType(Class<T> c) {
      type=c;
    }
    
    public Class<T> getType() {
      return type;
    }
    
    public final CMDS getCmd() {
      return cmd;
    }
    
    void setStatement(String s) {
      statement = s;
    }
    
    String getStatement() {
      return statement;
    }
    
    void setObj(Object o) {
      value=o;
    }
    
    Object getValue() {
      return value;
    }
    
    void setArgs(Object[] a) {
      args=a;
    }
    
    Object[] getArgs() {
      return args;
    }
    
    void setKwargs(Map<String, Object> m) {
      kwargs=m;
    }
    
    Map<String, Object> getKwargs() {
      return kwargs;
    }
    
    void setException(JepException e) {
      exception=e;
    }
    
    synchronized void throwException() throws JepException {
      if (exception != null) {
        throw exception;
      }
    }
  }
  
  protected enum CMDS {
    EVAL,
    EXEC,
    GETVALUE,
    GETVALUEWITHTYPE,
    INVOKEKWARGS,
    INVOKEARGS,
    INVOKEARGSKWARGS,
    RUNSCRIPT,
    SET
  }

  public void setPAppletMain(PAppletConnector pAppletConnector) {
    Util.log("ManagedInterpreter.setPAppletMain");
    if (mainset) {
      Util.log("ManagedInterpreter.setPAppletMain has already been run. Leaving.");
      return;
    }
    Util.log("ManagedInterpreter.setPAppletMain startInterpreter()");
    startInterpreter();
    set("PAppletMain", pAppletConnector);
    Util.log("ManagedInterpreter.setPAppletMain get methods");
    Method[] pAppletConnectorMethods = PApplet.class.getDeclaredMethods();
    for (Method method : pAppletConnectorMethods) {
      if (Modifier.isPublic(method.getModifiers())) {
        String name = method.getName();
        if ((name == "draw") || (name == "setup") ||(name == "settings")) {
          continue;
        }
        String pythonEval = name + " = PAppletMain." + name;
        Util.log("ManagedInterpreter.setPAppletMain pythonEval = '" + pythonEval + "'");
        eval(pythonEval);
      }
    }
    eval("frameCount = 0");
    
    Field[] pConstants = PConstants.class.getDeclaredFields();
    for (Field field : pConstants) {
      if (Modifier.isPublic(field.getModifiers())) {
        String name = field.getName();
        // There's got to be a better way to do this
        String typeString = field.getAnnotatedType().getType().getTypeName();
        String pythonEval = name + " = ";
        try {
          switch(typeString) {
            case "int": pythonEval += field.getInt(field);
              break;
            case "char": pythonEval += field.getChar(field);
              break;
            case "float": pythonEval += field.getFloat(field);
              break;
            case "java.lang.String": pythonEval += (String) field.get(field);
              break;
            case "java.lang.String[]": set(field.getName(), (String[])field.get(field));
              pythonEval = "";
              break;
          }
        } catch (IllegalArgumentException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        Util.log("ManagedInterpreter.setPAppletMain pythonEval = '" + pythonEval + "'");
        eval(pythonEval);
      }
    }
    
//    exec("for name in dir(PAppletMain):\n"
//        + "  if name.startswith('__') and name.endswith('__'):\n"
//        + "    continue\n"
//        + "  if (name == 'setup') or (name == 'draw') or (name == 'settings'):\n"
//        + "    continue\n"
//        + "  exec('global ' + name + '\n' + name + ' = PAppletMain.' + name)\n\n\n");
    //exec("ellipse = PAppletMain.ellipse");
    mainset=true;
    Util.log("ManagedInterpreter.setPAppletMain returning");
  }

}
