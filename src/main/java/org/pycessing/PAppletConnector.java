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
import jep.SharedInterpreter;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PSurface;

public class PAppletConnector extends PApplet implements Runnable{
  
  private String renderer=PConstants.JAVA2D;
  private Path sourceFile=null;
  private ArrayList<String> args;
  private Thread myThread;
  private Boolean running=false;
  private ManagedInterpreter interp;
  
  public PAppletConnector() {
    super();
    args = new ArrayList<String>();
  }
  
  public ManagedInterpreter getInterpreter() {
    return interp;
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

  public synchronized void startThread(ArrayList<String> args, ManagedInterpreter i) {
    startThread(args, i, new Thread(this));
  }
  
  public synchronized void startThread(ArrayList<String> args, ManagedInterpreter i, Thread t) {
    this.args=args;
    this.interp = i;
    myThread = t;
    myThread.start();
  }
  
  // Stop causes problems. Override it and have it do nothing
  @Override 
  public void stop() {
    return;
  }
  
  public synchronized void halt() throws InterruptedException {
    running=false;
    dispose();
    myThread.interrupt();
    myThread.join(5000L);
  }
  
  public synchronized Boolean isAlive() {
    if (myThread == null) {
      return false;
    }
    return myThread.isAlive();
  }
  
  @Override
  public void draw() {
    if (!running) {
     // super.exit();
    }
    interp.send("draw()");
  }
  
  @Override
  public void setup() {
    interp.send("setup()");
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
    size(width,height,renderer);
    interp.send("settings()");
  }

  @Override
  public void run() {
    runSketch();
  }
  
  @Override
  public void runSketch() {
    String[] argsArray = new String[args.size()+1];
    if (sourceFile != null) {
      interp.exec(sourceFile);
    }
    args.add(0, "org.pycessing.PAppletConnector");
    argsArray = args.toArray(argsArray);
    runSketch(argsArray, null);
  }

}
