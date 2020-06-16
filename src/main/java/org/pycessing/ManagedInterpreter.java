package org.pycessing;

import java.nio.file.Path;

import jep.JepException;
import jep.SharedInterpreter;

public class ManagedInterpreter implements Runnable {
  
  // There can be only one!
  private static SharedInterpreter interpreter;
  private static Boolean debug=false;

  private Thread thread;
  private Boolean running=false;
  private Boolean interactive=false;
  private PAppletConnector applet;
  
  public static void setDebug(Boolean d) {
    debug=d;
  }
  
  public static SharedInterpreter getInterpreter() {
    return interpreter;
  }
  
  public static void setInterpreter(SharedInterpreter i) {
    if (!debug) {
      throw new RuntimeException("Setting the interpreter will break things. Only use this to debug the pycessing interpreter.");
    }
    interpreter=i;
  }
  
  public ManagedInterpreter(PAppletConnector pa) {
    applet=pa;
  }
  
  public Thread getThread() {
    return thread;
  }
  
  public void setThread(Thread t) {
    thread = t;
  }
  
  @Override
  public void run() {
    try {
      if (interpreter == null) {
        interpreter = new SharedInterpreter();
        initializeInterpreter(interpreter);
      }
    } catch (JepException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    runLoop();
    

    // Try to be nice
    try {
      interpreter.eval("exit()");
      interpreter.close();
    } catch (JepException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void runLoop() {
    while (running) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  public void initializeInterpreter(SharedInterpreter i) {
    // TODO Auto-generated method stub
    
  }
  
  public void startThread() {
    startThread(new Thread(this));
  }

  public void startThread(Thread t) {
    thread = t;
    running = true;
    thread.start();
  }
  
  public synchronized void halt() throws JepException, InterruptedException {
    running = false;
    thread.interrupt();
    thread.join(5000L);
  }
  
  public Boolean isRunning() {
    return running;
  }
  
  public Object send(String cmd) {
    return new Object();
  }

  public void startREPL() {
    // TODO Auto-generated method stub
    
  }

  public void exec(Path sourceFile) {
    // TODO Auto-generated method stub
    
  }

}
