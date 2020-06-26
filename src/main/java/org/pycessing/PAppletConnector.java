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
  private Boolean running=false;
  private ManagedInterpreter interp;
  private boolean debug;
  
  public PAppletConnector() {
    super();
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
    
    System.err.println("PAppletConnector should not be run from main()");
    return;
    //PApplet.main("pycessing.PAppletConnector", args);

  }

  public synchronized void loadFile(String fileFromCLI) throws FileNotFoundException {
    sourceFile = Paths.get(fileFromCLI);
    if (!Files.exists(sourceFile)) {
      throw new FileNotFoundException(fileFromCLI + " not found");
    }
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
    Util.log("PAppletConnector size(" + width + "," + height + ")");
    super.size(width, height);
  }
  
  @Override 
  public void start() {
    Util.log("PAppletConnector start");
    super.start();
  }
  
  @Override 
  public void handleDraw() {
    Util.log("PAppletConnector handleDraw");
    super.handleDraw();
  }
  
  @Override
  public void draw() {
    Util.log("PAppletConnector draw");
    this.interp.exec("if 'draw' in dir():\n"
        + "  draw()\n\n");
  }
  
  @Override
  public void setup() {
    Util.log("PAppletConnector setup");
    this.interp.exec("if 'setup' in dir():\n"
        + "  setup()\n\n");
  }
  
  public void setSizeFromSetup(Path path) throws FileNotFoundException {
    // If size() is in setup(), it must be the first line
    Scanner fileScanner = new Scanner(path.toFile());
    Pattern setupPattern = Pattern.compile("def\\s*setup\\s*\\(\\s*\\)\\s*:.*");
    Pattern sizePattern = Pattern.compile("\\s+size\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,?\\s*([A-Z,0-9]+)?\\s*\\).*");
    Matcher matcher = null;
    
    while (fileScanner.hasNextLine()) {
      String line = fileScanner.nextLine();
      matcher = setupPattern.matcher(line);
      if (matcher.find()) {
        if (fileScanner.hasNextLine()) {
          line = fileScanner.nextLine();
          matcher = sizePattern.matcher(line);
          if (matcher.find()) {
            String w = matcher.group(1);
            String h = matcher.group(2);
            String r = matcher.group(3);
            Util.log("Found usable size: " + line + " " + w + " " + h + " " + r);
            width = Integer.parseInt(w);
            height = Integer.parseInt(h);
            setRenderer(r);
          }
        }
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
    this.interp.exec("if 'settings' in dir()\n"
        + "  settings()\n\n");
  }

  @Override
  public void run() {
    runSketch();
  }
  
  @Override
  public void runSketch() {
    Util.log("PAppletConnector runScript: starting interpreter");
    interp.startInterpreter();
    interp.setPAppletMain(this);
    
    
    String[] argsArray = new String[args.size()+1];
    if (sourceFile != null) {
      Util.log("PAppletConnector runScript: running interp.runScript");
      interp.runScript(sourceFile.toString());
    }

    Util.log("PAppletConnector runScript: adding args");
    args.add(0, "org.pycessing.PAppletConnector");
    argsArray = args.toArray(argsArray);
    if (argsArray == null) {
      argsArray = new String[] {};
    }

    if (Pycessing.VERBOSE) {
      System.out.print("PAppletConnector runScript: running super.runsketch(" + this.getClass().toString() + ", [");
      for (int i=0; i<argsArray.length; i++) {
        System.out.print(argsArray[i] + ", ");
      }
      System.out.print("]" + ", null)\n");
    }
    super.runSketch(argsArray);
    Util.log("PAppletConnector runScript: complete. Exiting");
  }

}
