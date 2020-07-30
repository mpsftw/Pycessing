package org.pycessing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentLinkedQueue;

import jep.JepException;
import jep.SharedInterpreter;
import jep.SubInterpreter;
import processing.core.PApplet;
import processing.core.PConstants;

public class ManagedInterpreter extends SubInterpreter {
  
  // There can be only one!
  private String capturedOutput = "";
  private String capturedError = "";
  private final Object waitLock = new Object();
  
  private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
  
  public ManagedInterpreter(PAppletConnector p) throws JepException {
    super();
    captureOutput();
    initializeInterpreter();
    setPAppletMain(p);
  }
  
  private void captureOutput() throws JepException {
    super.exec("import sys\n");
    super.exec("from io import StringIO\n");
    super.set("__captured_output__", (Object) capturedOutput);
    super.set("__captured_error__", (Object) capturedError);
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
    
    super.exec(outputCapture);
  }
  
  public String getCapturedOutput() {
    String rv = capturedOutput;
    capturedOutput = "";
    return rv;
  }
  
  public String getCapturedError() {
    String rv = capturedError;
    capturedError = "";
    return rv;
  }
  
  private void moveOutputToString() throws JepException {
    try {
      capturedOutput += getValue("__jepout__.getvalue()", String.class);
    } catch (JepException e) {
      if (! e.getLocalizedMessage().contains("'__jepout__' is not defined")) {
        throw e;
      }
    }
  }
  
  public void moveErrorToString() throws JepException {
    try {
      capturedError += getValue("__jeperr__.getvalue()", String.class);
    } catch (JepException e) {
      if (! e.getLocalizedMessage().contains("'__jeperr__' is not defined")) {
        throw e;
      }
    }
  }
  
  @Override
  public boolean eval(String e) throws JepException {
    boolean rv = super.eval(e);
    moveOutputToString();
    moveErrorToString();
    return rv;
  }
  
  @Override
  public void exec(String e) throws JepException {
    super.exec(e);
    moveOutputToString();
    moveErrorToString();
  }
  
  public void runScript(String path) throws JepException {
    super.runScript(path);
    moveOutputToString();
    moveErrorToString();
  }
  
  public void close() throws JepException {
    synchronized (waitLock) {
      waitLock.notifyAll();
    }
    super.close();
  }

  private void initializeInterpreter() throws JepException {
    super.set("interpreter", this);
    super.set("managedinterpreter", this);
  }
  
  public void queueToInterpretAndWait(String s) {
    synchronized (waitLock) {
      queue.add(s);
      try {
          waitLock.wait();
      } catch (InterruptedException e) {
        return;
      }
    }
  }
  
  public void processQueue() throws JepException {
    synchronized(waitLock) {
      while (!queue.isEmpty()) {
        String toEval = queue.poll();
        exec(toEval);
      }
      waitLock.notifyAll();
    }
  }

  public synchronized void startREPL() {
    // TODO Auto-generated method stub
    
  }

  private void setPAppletMain(PAppletConnector pAppletConnector) throws JepException {
    Util.log("ManagedInterpreter.setPAppletMain");
    Util.log("ManagedInterpreter.setPAppletMain startInterpreter()");
    super.set("PAppletMain", pAppletConnector);
    Util.log("ManagedInterpreter.setPAppletMain get methods");
    Method[] pAppletConnectorMethods = PApplet.class.getDeclaredMethods();
    for (Method method : pAppletConnectorMethods) {
      if (Modifier.isPublic(method.getModifiers())) {
        String name = method.getName();
        if ((name == "draw") || (name == "setup") ||(name == "settings") || (name == "print")) {
          continue;
        }
        
        // exit() has meaning in both PApplet and python
        //if (name == "exit") {
        //  super.exec("def exit():\n" + 
        //      "  PAppletMain.dispose()\n\n"
        //      );
        //}
        
        String pythonEval = name + " = PAppletMain." + name;
        Util.log("ManagedInterpreter.setPAppletMain pythonEval = '" + pythonEval + "'");
        super.eval(pythonEval);
      }
    }
    super.eval("frameCount = 0");
    
    Field[] pConstants = PConstants.class.getDeclaredFields();
    for (Field field : pConstants) {
      if (Modifier.isPublic(field.getModifiers())) {
        String name = field.getName();
        switch (name) {
          case "BACKSPACE": continue;
          case "TAB": continue;
          case "ENTER": continue;
          case "RETURN": continue;
          case "ESC": continue;
          case "DELETE": continue;
        }
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
            case "java.lang.String": pythonEval += "\"\"\"" + (String) field.get(field) + "\"\"\"";
              break;
            case "java.lang.String[]": super.set(field.getName(), (String[])field.get(field));
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
        super.eval(pythonEval);
      }
    }
    
//    exec("for name in dir(PAppletMain):\n"
//        + "  if name.startswith('__') and name.endswith('__'):\n"
//        + "    continue\n"
//        + "  if (name == 'setup') or (name == 'draw') or (name == 'settings'):\n"
//        + "    continue\n"
//        + "  exec('global ' + name + '\n' + name + ' = PAppletMain.' + name)\n\n\n");
    //exec("ellipse = PAppletMain.ellipse");
    Util.log("ManagedInterpreter.setPAppletMain returning");
  }

}
