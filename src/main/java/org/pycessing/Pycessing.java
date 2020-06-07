package main.java.org.pycessing;

import java.util.ArrayList;

import org.apache.commons.cli.*;

import processing.core.PApplet;

public class Pycessing {
  
  public static final PAppletConnector PYAPPLET = new PAppletConnector();
  public static final ArrayList<String> PAPPLETARGS = new ArrayList<String>();
  public static Boolean INTERACTIVE=false;
  public static Boolean VERBOSE=false;

  public static void main(String[] args) {
    Options options = getOptions();
    try {
      getArgs(options,args);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      showHelp();
      return;
    }
    

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
        .desc("Print extra debug information")
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
        .longOpt("display")
        .desc("Suggest a display")
        .build();
    final Option windowColorOption = Option.builder("w")
        .required(false)
        .hasArg(true)
        .longOpt("window-color")
        .desc("Set the color of the display window")
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
        .desc("Set the color of the stop button")
        .build();
    final Option hideStopOption = Option.builder("h")
        .required(false)
        .hasArg(false)
        .longOpt("hide-stop")
        .desc("Hide the stop button")
        .build();
    final Option sketchPathOption = Option.builder("s")
        .required(false)
        .hasArg(true)
        .longOpt("sketch-path")
        .desc("Set the sketch folder. This folder will be used as the default location for commands like \"saveFrame()\"")
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
    
    return options;
    
  }
  
  public static void showHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("pycessing", getOptions());
  }
  
  public static void getArgs(Options options, String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse( options, args);
    
    final String[] remaining = cmd.getArgs();
    String file;
    
    if (remaining.length > 1) {
      throw new ParseException("Invalid command");
    } else if (remaining.length == 0) {
      INTERACTIVE=true;
    } else {
      file = remaining[0];
    }
    
    if (cmd.hasOption("v")) {
      VERBOSE=true;
    }
    if (cmd.hasOption("i")) {
      INTERACTIVE=true;
    }
    if (cmd.hasOption("d")) {
      String a=PApplet.ARGS_DISPLAY + "=" + cmd.getOptionValue("d");
      PAPPLETARGS.add(a);
    }
    if (cmd.hasOption("w")) {
      String a=PApplet.ARGS_WINDOW_COLOR + "=" + cmd.getOptionValue("w");
      PAPPLETARGS.add(a);
    }
    if (cmd.hasOption("p")) {
      PAPPLETARGS.add(PApplet.ARGS_PRESENT);
    }
    if (cmd.hasOption("c")) {
      String a = PApplet.ARGS_STOP_COLOR + "=" + cmd.getOptionValue("c");
      PAPPLETARGS.add(a);
    }
    if (cmd.hasOption("h")) {
      PAPPLETARGS.add(PApplet.ARGS_HIDE_STOP);
    }
    if (cmd.hasOption("s")) {
      String a = PApplet.ARGS_SKETCH_FOLDER + "=" + cmd.getOptionValue("s");
      PAPPLETARGS.add("a");
    }
    
    
  }

}
