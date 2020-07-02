package org.pycessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import jep.JepException;
import jep.python.PyCallable;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PSurface;

public class PAppletConnector extends PApplet implements Runnable{
  
  private String renderer=PConstants.JAVA2D;
  private Path sourceFile=null;
  private ArrayList<String> args;
  private ManagedInterpreter interp;
  private boolean debug=false;
  private final Object finishedLock = new Object();
  public Integer frameCount=new Integer(0);
  
  public PAppletConnector() {
    super();
    args = new ArrayList<String>();
    interp = new ManagedInterpreter();
  }
  
  public PAppletConnector(ArrayList<String> args) {
    super();
    this.args=args;
    interp = new ManagedInterpreter();
  }
  
  protected void setDebug(boolean b) {
    debug=b;
  }
  
  public ArrayList<String> getArgs() {
    return args;
  }
  
  public void setArgs(ArrayList<String> args) {
    this.args = args;
  }
  
  public ManagedInterpreter getInterpreter() {
    return interp;
  }
  
  public void setInterpreter(ManagedInterpreter interp) {
    if (!debug) {
      throw new RuntimeException("You can only set the interpreter in debug mode.");
    }
    this.interp = interp;
  }
  
  public void setSurface(PSurface s) {
    surface=s;
  }

  public static void main(String[] args) {
    // This should probably never run. This exists to match PApplet
    // It may be safe to remove this later.
    
    PApplet.main("pycessing.PAppletConnector", args);

  }

  public synchronized void loadFile(String fileFromCLI) throws FileNotFoundException {
    Util.log("PAppletConnector loadFile: " + fileFromCLI);
    sourceFile = Paths.get(fileFromCLI);
    Util.log("PAppletConnector sourceFile set to: " + sourceFile.toString());
    if (!Files.exists(sourceFile)) {
      throw new FileNotFoundException(fileFromCLI + " not found");
    }
    Util.log("PAppletConnector running setSizeFromSetup");
    setSizeFromSetup(sourceFile);
  }

  public Path getSourceFile() {
    return sourceFile;
  }
  
  public void startREPL() {
    this.interp.startREPL();
  }
  
  @Override
  public void size(int width, int height) {
    // For simplicity. size() can only be called from settings() which sends it to super
    return;
  }
  
  @Override
  public void size(int width, int height, String renderer) {
    // For simplicity. size() can only be called from settings() which sends it to super
    return;
  }
  
  @Override
  public void size(int width, int height, String renderer, String path) {
    // For simplicity. size() can only be called from settings() which sends it to super
    return;
  }
  
  @Override 
  public void start() {
    Util.log("PAppletConnector start");
    Util.log("PAppletConnector start: starting interpreter");
    interp.startInterpreter();
    interp.setPAppletMain(this);
    super.start();
    Util.log("PAppletConnector exit start");
  }
  
  @Override 
  public void handleDraw() {
    Util.log("PAppletConnector handleDraw");
    super.handleDraw();
    Util.log("PAppletConnector exit handleDraw");
  }
  
  @Override
  public synchronized void draw() {
    Util.log("PAppletConnector draw");
    this.interp.exec("draw()");
    Util.log("PAppletConnector draw finished. Incrementing frameCount.");
    this.interp.eval("frameCount+=1");
    String stdout = interp.getOutput();
    String stderr = interp.getErr();
    Util.log("Draw stdout/err: \n" + stdout + "\n" + stderr);
  }
  
  @Override
  public synchronized void setup() {
    Util.log("PAppletConnector setup");
    //this.interp.exec("if 'setup' in dir():\n"
    //    + "  setup()\n\n");
    this.interp.exec("setup()");
    String stdout = interp.getOutput();
    String stderr = interp.getErr();
    Util.log("Setup stdout/err: \n" + stdout + "\n" + stderr);
  }
  
  @Override
  public void exit() {
    super.exit();
    try {
      interp.close();
    } catch (JepException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    finishedLock.notifyAll();
  }
  
  public void setSizeFromSetup(Path path) throws FileNotFoundException {
    Util.log("PAppletConnector setSizeFromSetup with: " + path.toString());
    Scanner fileScanner = new Scanner(path.toFile());
    Pattern setupPattern = Pattern.compile("^def\\s*setup\\s*\\(\\s*\\)\\s*:.*$");
    Matcher matcher = null;
    
    while (fileScanner.hasNextLine()) {
      String line = fileScanner.nextLine();
      Util.log("PAppletConnector setSizeFromSetup line: " + line);
      matcher = setupPattern.matcher(line);
      if (matcher.find()) {
        Util.log("PAppletConnector setSizeFromSetup found setup on line: " + line);
        lookThroughSetupFromFile(fileScanner);
      }
    }
  }

  private void lookThroughSetupFromFile(Scanner fileScanner) {
    Util.log("PAppletConnector lookThroughSetupFromFile");
    Pattern sizePattern = Pattern.compile("^\\s+size\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,?\\s*([A-Z,0-9]+)?\\s*\\).*$");
    Pattern outOfSetup = Pattern.compile("^\\S+.*$");
    String line;
    Matcher matcher;
    
    while (fileScanner.hasNextLine()) {
      line = fileScanner.nextLine();
      Util.log("PAppletConnector lookThroughSetupFromFile line: " + line);
      matcher = outOfSetup.matcher(line);
      if (matcher.find()) {
        Util.log("PAppletConnector lookThroughSetupFromFile outOfSetup matched line: " + line + " \nReturning.");
        return;
      }
      matcher = sizePattern.matcher(line);
      if (matcher.find()) {
        String w = matcher.group(1);
        String h = matcher.group(2);
        String r = matcher.group(3);
        Util.log("Found usable size: " + line + " " + w + " " + h + " " + r);
        width = Integer.parseInt(w);
        height = Integer.parseInt(h);
        setRenderer(r);
        return;
      }
    }
  }

  void setRenderer(String r) {
    if (r == null) {
      return;
    }
    if (r.equals("JAVA2D")) {
      renderer = JAVA2D;
    } else if (r.equals("P2D")) {
      renderer = P2D;
    } else if (r.equals("P3D")) {
      renderer = P3D;
    } else if (r.equals("OPENGL")) {
      renderer = P3D;
    } else if (r.equals("FX2D")) {
      renderer = FX2D;
    } else if (r.equals("PDF")) {
      renderer = PDF;
    } else if (r.equals("SVG")) {
      renderer = SVG;
    } else if (r.equals("DXF")) {
      renderer = DXF;
    }
  }
  
  String getRenderer() {
    return renderer;
  }

  @Override 
  public synchronized void settings() {
    Util.log("PAppletConnector settings interp=" + interp + "and is running: " + interp.isRunning());
    Util.log("PAppletConnector settings running super.size(" + width + "," + height + "," + renderer);
    super.size(width,height,renderer);
    this.interp.set("width", width);
    this.interp.set("height", height);
    this.interp.exec("if 'settings' in dir():\n"
        + "  settings()\n\n");
  }

  @Override
  public void run() {
    runSketch();
  }
  
  @Override
  public void runSketch() {
    
    String[] argsArray = new String[args.size()+1];
    if (sourceFile != null) {
      Util.log("PAppletConnector runSketch: running interp.runScript");
      interp.runScript(sourceFile.toString());
    }

    Util.log("PAppletConnector runSketch: adding args");
    args.add(0, "org.pycessing.PAppletConnector");
    argsArray = args.toArray(argsArray);
    if (argsArray == null) {
      argsArray = new String[] {};
    }

    if (Pycessing.VERBOSE) {
      System.out.print("PAppletConnector runSketch: running super.runsketch([");
      for (int i=0; i<argsArray.length; i++) {
        System.out.print(argsArray[i] + ", ");
      }
      System.out.print("]" + ", null)\n");
    }
    super.runSketch(argsArray);
    Util.log("PAppletConnector runSketch: complete. Exiting");
  }
  
  @Override
  public void dispose() {
    super.dispose();
    synchronized(finishedLock) {
      finishedLock.notifyAll();
    }
  }
  
  public void waitForFinish() throws InterruptedException {
    synchronized(finishedLock) {
      while (!finished) {
        finishedLock.wait(100);
      }
    }
  }
  

}
