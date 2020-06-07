package test.java.org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.java.org.pycessing.Pycessing;

class PycessingMainTest {
  
  private static Options testOptions;
  
  private  static ByteArrayOutputStream outContent;
  private  static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private  static PrintStream capturedOut;
  private  static PrintStream capturedErr;
  
  private void reset() {
    Pycessing.INTERACTIVE=false;
    Pycessing.VERBOSE=false;
    Pycessing.PAPPLETARGS.clear();
  }

  private void failWithMessage(String msg) {
    System.setOut(originalOut);
    System.setErr(originalErr);
    fail(msg);
    handleOutput();
  }
  
  private static void handleOutput() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    capturedOut =  new PrintStream(outContent);
    capturedErr = new PrintStream(errContent);
    System.setOut(capturedOut);
    System.setErr(capturedErr);
  }

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    testOptions=Pycessing.getOptions();
    handleOutput();
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @BeforeEach
  void setUp() throws Exception {
    handleOutput();
  }

  @AfterEach
  void tearDown() throws Exception {
    reset();
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testDefaults() {
    assertFalse(Pycessing.INTERACTIVE);
    assertFalse(Pycessing.VERBOSE);
    assertNotNull(Pycessing.PYAPPLET);
    assertNotNull(Pycessing.PAPPLETARGS);
  }
  
  @Test
  public void testGetOptions() {
    Options options = Pycessing.getOptions();
    assertNotNull(options);
    assertTrue(options.hasLongOption("verbose"));
    assertTrue(options.hasShortOption("v"));
    assertTrue(options.hasLongOption("interactive"));
    assertTrue(options.hasShortOption("i"));
    assertTrue(options.hasLongOption("display"));
    assertTrue(options.hasShortOption("d"));
    assertTrue(options.hasLongOption("window-color"));
    assertTrue(options.hasShortOption("w"));
    assertTrue(options.hasLongOption("present"));
    assertTrue(options.hasShortOption("p"));
    assertTrue(options.hasLongOption("stop-color"));
    assertTrue(options.hasShortOption("c"));
    assertTrue(options.hasLongOption("hide-stop"));
    assertTrue(options.hasShortOption("h"));
    assertTrue(options.hasLongOption("sketch-path"));
    assertTrue(options.hasShortOption("s"));
  }
  
  @Test
  public void testShowHelp() {
    Pycessing.showHelp("", true);
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n", outContent.toString(), "testShowHelp failed test 1");
    
    handleOutput();
    Pycessing.showHelp("This is a test", true);
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n" + 
        "This is a test\n", outContent.toString(), "testShowHelp failed test 2");
    
    handleOutput();
    Pycessing.showHelp("This is a test", false);
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n" + 
        "This is a test\n", errContent.toString(), "testShowHelp failed test 3");
  }
  
  @Test
  public void testHelp() {
    String[] shortArgs = {"-h"};
    String[] longArgs = {"--help"};
    String[] tooManyArgs = {"-i", "-p", "blah", "1", "2", "3"};
    String[] withOtherArgs = {"-i", "-h", "/path/to/file"};
    String[] withFile = { "-h", "/path/to/file"};
    
    try {
      Pycessing.getArgs(testOptions, shortArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n", outContent.toString(), "testShowHelp failed for short option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, longArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n", outContent.toString(), "testShowHelp failed for long option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, tooManyArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n" +
        "Too many options\n", errContent.toString(), "testShowHelp failed for tooManyArgs option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, withOtherArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n", outContent.toString(), "testShowHelp failed for withOtherArgs option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, withFile);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals("usage: pycessing [OPTIONS] [FILE]\n" + 
        "Options:\n" + 
        "    -c,--stop-color <arg>      Set the color of the stop button\n" + 
        "    -d,--display <arg>         Suggest a display\n" + 
        "    -h,--help                  Print this help message\n" + 
        "    -hs,--hide-stop            Hide the stop button\n" + 
        "    -i,--interactive           Start an interactive session\n" + 
        "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
        "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
        "                               as the default location for commands like\n" + 
        "                               \"saveFrame()\"\n" + 
        "    -v,--verbose               Print extra debug information\n" + 
        "    -w,--window-color <arg>    Set the color of the display window\n", outContent.toString(), "testShowHelp failed for withFile option");
  }
  
  @Test
  public void testInteractiveOptions() {
    String[] argsEmpty = {};
    String[] argsShort = {"-i", "/path/to/test"};
    String[] argsLong = {"--interactive"};
    
    try {
      Pycessing.getArgs(testOptions, argsEmpty);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsEmpty Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE, "Interactive empty test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE, "Interactive short test");
    reset();

    try {
      Pycessing.getArgs(testOptions, argsLong);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE, "Interactive long test");
    reset();
  }

}
