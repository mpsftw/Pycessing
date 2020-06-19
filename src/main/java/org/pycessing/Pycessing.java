package org.pycessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.cli.*;

import jep.JepException;
import jep.SharedInterpreter;
import processing.core.PApplet;

public class Pycessing {
  
  public static final PAppletConnector PyApplet = new PAppletConnector();
  public static final ArrayList<String> PAppletArgs = new ArrayList<String>();
  public static Boolean INTERACTIVE=false;
  public static Boolean VERBOSE=false;
  public static String fileFromCLI=null;

  public static void main(String[] args) {
    
    Options options = getOptions();
    try {
      getArgs(options,args);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      showHelp("", false);
      return;
    }
    
    if (fileFromCLI != null) {
      try {
        PyApplet.loadFile(fileFromCLI);
      } catch (FileNotFoundException e) {
        System.err.println(e.getLocalizedMessage());
      }
    }
    
    PyApplet.startThread(PAppletArgs);
    
    
    if (INTERACTIVE) {
      PyApplet.startREPL();
    }

  }
  
  private static Boolean checkForColor(String color) {
    String pattern = "^#[a-fA-F0-9]{6}$";
    return color.matches(pattern);
  }
  
  public static Options getOptions() {
    /*
     * Copied from PApplet 
     *
     * Used by the PDE to suggest a display (set in prefs, passed on Run)
     * static public final String ARGS_DISPLAY = "--display";
     *
     * static public final String ARGS_WINDOW_COLOR = "--window-color";
     *
     * static public final String ARGS_PRESENT = "--present";
     *
     * static public final String ARGS_STOP_COLOR = "--stop-color";
     * 
     * static public final String ARGS_HIDE_STOP = "--hide-stop";
     * 
     * Allows the user or PdeEditor to set a specific sketch folder path.
     * <p>
     * Used by PdeEditor to pass in the location where saveFrame()
     * and all that stuff should write things.
     * 
     * static public final String ARGS_SKETCH_FOLDER = "--sketch-path";
     * 
     */
    
    final Option verboseOption = Option.builder("v")
        .required(false)
        .hasArg(false)
        .longOpt("verbose")
        .desc("Print debugging information")
        .build();
    final Option interactiveOption = Option.builder("i")
        .required(false)
        .hasArg(false)
        .longOpt("interactive")
        .desc("Start an interactive session")
        .build();
    final Option displayOption = Option.builder("d")
        .required(false)
        .hasArg(true)
        .type(Integer.class)
        .longOpt("display")
        .desc("Suggest a display to use")
        .build();
    final Option windowColorOption = Option.builder("w")
        .required(false)
        .hasArg(true)
        .longOpt("window-color")
        .desc("Set the color of the display window. <arg> should be in the form of a hexadecimal like #FFFFFF")
        .build();
    final Option presentOption = Option.builder("p")
        .required(false)
        .hasArg(false)
        .longOpt("present")
        .desc("Run the sketch in present (fullscreen) mode")
        .build();
    final Option stopColorOption = Option.builder("c")
        .required(false)
        .hasArg(true)
        .longOpt("stop-color")
        .desc("Set the color of the stop button. <arg> should be in the form of a hexadecimal like #FFFFFF")
        .build();
    final Option hideStopOption = Option.builder("hs")
        .required(false)
        .hasArg(false)
        .longOpt("hide-stop")
        .desc("-hs or --hide-stop: Hide the stop button")
        .build();
    final Option sketchPathOption = Option.builder("s")
        .required(false)
        .hasArg(true)
        .longOpt("sketch-path")
        .desc("Set the sketch folder. This folder will be used as the default location for commands like \"saveFrame()\"")
        .build();
    final Option helpOption = Option.builder("h")
        .required(false)
        .hasArg(false)
        .longOpt("help")
        .desc("Print this help message")
        .build();
        
    
    Options options = new Options();
    options.addOption(verboseOption);
    options.addOption(interactiveOption);
    options.addOption(displayOption);
    options.addOption(windowColorOption);
    options.addOption(presentOption);
    options.addOption(stopColorOption);
    options.addOption(hideStopOption);
    options.addOption(sketchPathOption);
    options.addOption(helpOption);
    
    return options;
    
  }
  
  public static void showHelp(String msg, Boolean stdout) {
    HelpFormatter formatter = new HelpFormatter();
    PrintWriter pw = stdout ? new PrintWriter(System.out) : new PrintWriter(System.err);
    formatter.printHelp(pw, 80, "pycessing [OPTIONS] [FILE]", "Options:", getOptions(), 4, 4, msg);
    pw.flush();
    pw.close();
  }
  
  public static void getArgs(Options options, String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    
    try {
      cmd = parser.parse( options, args);
    } catch (MissingArgumentException e1) {
      Option o = e1.getOption();
      showHelp("-" + o.getOpt() + " requires " + o.getArgs() + " argument(s)", false);
      return;
    }
    
    final String[] remaining = cmd.getArgs();
    
    if (cmd.hasOption("h") || cmd.hasOption("help")) {
      showHelp("", true);
      return;
    }
    
    if (remaining.length > 1) {
      showHelp("Too many options", false);
      return;
    }
    
    if (remaining.length == 0) {
      INTERACTIVE=true;
    } else {
      fileFromCLI = remaining[0];
    }
    
    if (cmd.hasOption("v")) {
      VERBOSE=true;
    }
    
    if (cmd.hasOption("i")) {
      INTERACTIVE=true;
    }
    
    if (cmd.hasOption("d")) {
      String d = cmd.getOptionValue("d");
      try {
        Integer.parseInt(d);
      } catch (NumberFormatException e) {
        showHelp("Display argument must be a number. Recieved: " + d, false);
        return;
      }
      String a=PApplet.ARGS_DISPLAY + "=" + d;
      PAppletArgs.add(a);
    }
    
    if (cmd.hasOption("w")) {
      String color = cmd.getOptionValue("w");
      if (!checkForColor(color)) {
        showHelp(color + " was not in the form of a hexadecimal like #FFFFFF", false);
        return;
      }
      String a=PApplet.ARGS_WINDOW_COLOR + "=" + color;
      PAppletArgs.add(a);
    }
    
    if (cmd.hasOption("p")) {
      String a=PApplet.ARGS_PRESENT;
      PAppletArgs.add(a);
    }
    
    if (cmd.hasOption("p")) {
      PAppletArgs.add(PApplet.ARGS_PRESENT);
    }
    
    if (cmd.hasOption("c")) {
      String color = cmd.getOptionValue("c");
      if (!checkForColor(color)) {
        showHelp(color + " was not in the form of a hexadecimal like #FFFFFF", false);
        return;
      }
      String a = PApplet.ARGS_STOP_COLOR + "=" + color;
      PAppletArgs.add(a);
    }
    
    if (cmd.hasOption("hs")) {
      PAppletArgs.add(PApplet.ARGS_HIDE_STOP);
    }
    
    if (cmd.hasOption("s")) {
      String path = cmd.getOptionValue("s");
      File directory = new File(path);
      try {
        if (!directory.exists()) {
          directory.mkdirs();
        } else {
          if (!directory.isDirectory()) {
            showHelp("A file already exists at " + path, false);
            return;
          }
        }
      } catch (SecurityException e) {
        showHelp("Security error reading or writing to path " + path + "\n" + e.getLocalizedMessage(), false);
        throw e;
      }
      String a = PApplet.ARGS_SKETCH_FOLDER + "=" + path;
      PAppletArgs.add(a);
    }
  }

}
