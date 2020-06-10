package test.java.org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.java.org.pycessing.Pycessing;
import processing.core.PApplet;

class PycessingMainTest {
  
  private static Options testOptions;
  
  private  static ByteArrayOutputStream outContent;
  private  static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private  static PrintStream capturedOut;
  private  static PrintStream capturedErr;
  
  private final static String standardUsageString = "usage: pycessing [OPTIONS] [FILE]\n" + 
      "Options:\n" + 
      "    -c,--stop-color <arg>      Set the color of the stop button. <arg> should be\n" + 
      "                               in the form of a hexadecimal like #FFFFFF\n" + 
      "    -d,--display <arg>         Suggest a display to use\n" + 
      "    -h,--help                  Print this help message\n" + 
      "    -hs,--hide-stop            -hs or --hide-stop: Hide the stop button\n" + 
      "    -i,--interactive           Start an interactive session\n" + 
      "    -p,--present               Run the sketch in present (fullscreen) mode\n" + 
      "    -s,--sketch-path <arg>     Set the sketch folder. This folder will be used\n" + 
      "                               as the default location for commands like\n" + 
      "                               \"saveFrame()\"\n" + 
      "    -v,--verbose               Print debugging information\n" + 
      "    -w,--window-color <arg>    Set the color of the display window. <arg> should\n" + 
      "                               be in the form of a hexadecimal like #FFFFFF\n";
  
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
    assertEquals(standardUsageString, outContent.toString(), "testShowHelp failed test 1");
    
    handleOutput();
    Pycessing.showHelp("This is a test", true);
    assertEquals(standardUsageString + "This is a test\n", outContent.toString(), "testShowHelp failed test 2");
    
    handleOutput();
    Pycessing.showHelp("This is a test", false);
    assertEquals(standardUsageString + "This is a test\n", errContent.toString(), "testShowHelp failed test 3");
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
    assertEquals(standardUsageString, outContent.toString(), "testShowHelp failed for short option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, longArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString(), "testShowHelp failed for long option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, tooManyArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "Too many options\n", errContent.toString(), "testShowHelp failed for tooManyArgs option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, withOtherArgs);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString(), "testShowHelp failed for withOtherArgs option");
    
    handleOutput();
    try {
      Pycessing.getArgs(testOptions, withFile);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString(), "testShowHelp failed for withFile option");
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
  
  public void testVerboseOptions() {
    String[] argsShort = {"-v", "/path/to/test"};
    String[] argsLong = {"--verbose", "/path/to/test"};
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testVerboseOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.VERBOSE, "Verbose short test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testVerboseOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.VERBOSE, "Verbose long test");
    reset();
  }
  
  @Test
  public void testDisplayOptions() {
    String[] argsShort = {"-d", "0", "/path/to/file"};
    String[] argsLong = {"--display", "0", "/path/to/file"};
    String[] argsMissingWithFile = {"-d", "/path/to/file"};
    String[] argsMissingWithoutFile = {"-i", "-d"};
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_DISPLAY + "=0", "Display long test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_DISPLAY + "=0", "Display short test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithFile);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(0, Pycessing.PAPPLETARGS.size(), "Display argsMissingWithFile test had non-zero PAPPLETARGS length");
    assertEquals(standardUsageString + "Display argument must be a number. Recieved: /path/to/file\n", errContent.toString(), "Display argsMissingWithFile test failed to produce correct error");
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithoutFile);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsMissingWithoutFile Caught Exception: " + errContent.toString());
    }
    assertEquals(0, Pycessing.PAPPLETARGS.size(), "Display argsMissingWithoutFile test had non-zero PAPPLETARGS length");
    assertEquals(standardUsageString + "-d requires 1 argument(s)\n", errContent.toString(), "Display argsMissingWithoutFile test failed to produce correct error");
    reset();
    handleOutput();
  }
  
  @Test
  public void testWindowColorOptions() {
    String[] argsShort = {"-w", "#FFFFFF", "/path/to/file"};
    String[] argsLong = {"--window-color", "#FFFFFF", "/path/to/file"};
    String[] argsNotColor = { "-w", "notacolor", "/path/to/file"};
    String[] argsMissingWithFile = { "-w", "/path/to/file"};
    String[] argsMissingWithoutFile = { "-w" };
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
      assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_WINDOW_COLOR + "=#FFFFFF", "Window color short test");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testWindowColorOptions argsShort Caught Exception: " + errContent.toString());
    }
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
      assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_WINDOW_COLOR + "=#FFFFFF", "Window color long test");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testWindowColorOptions argsLong Caught Exception: " + errContent.toString());
    }
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsNotColor);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Window color argsNotColor test had non-zero PAPPLETARGS length");
      failWithMessage("testDisplayOptions argsNotColor Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "notacolor was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString(), "Display argsNotColor test failed to produce correct error");
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithFile);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Window color argsMissingWithFile test had non-zero PAPPLETARGS length");
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "/path/to/file was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString(), "Display argsMissingWithFile test failed to produce correct error");
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithoutFile);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Window color argsMissingWithoutFile test had non-zero PAPPLETARGS length");
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "-w requires 1 argument(s)\n", errContent.toString(), "Display argsMissingWithoutFile test failed to produce correct error");
    reset();
    handleOutput();
  }
  
  @Test
  public void testPresentOptions() {
    String[] argsShort = {"-p", "/path/to/test"};
    String[] argsLong = {"--present", "/path/to/test"};
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testPresentOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_PRESENT, Pycessing.PAPPLETARGS.get(0), "Present short test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testPresentOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_PRESENT, Pycessing.PAPPLETARGS.get(0), "Present long test");
    reset();
  }
  
  @Test
  public void testStopColorOptions() {
    String[] argsShort = {"-c", "#FFFFFF", "/path/to/file"};
    String[] argsLong = {"--stop-color", "#FFFFFF", "/path/to/file"};
    String[] argsNotColor = { "-c", "notacolor", "/path/to/file"};
    String[] argsMissingWithFile = { "-c", "/path/to/file"};
    String[] argsMissingWithoutFile = { "-c" };
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
      assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_STOP_COLOR + "=#FFFFFF", "Window color short test");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testStopColorOptions argsShort Caught Exception: " + errContent.toString());
    }
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
      assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_STOP_COLOR + "=#FFFFFF", "Window color long test");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testStopColorOptions argsLong Caught Exception: " + errContent.toString());
    }
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsNotColor);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Stop color argsNotColor test had non-zero PAPPLETARGS length");
      failWithMessage("testStopColorOptions argsNotColor Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "notacolor was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString(), "Display argsNotColor test failed to produce correct error");
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithFile);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Stop color argsMissingWithFile test had non-zero PAPPLETARGS length");
      failWithMessage("testStopColorOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "/path/to/file was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString(), "Display argsMissingWithFile test failed to produce correct error");
    reset();
    handleOutput();
    
    try {
      Pycessing.getArgs(testOptions, argsMissingWithoutFile);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAPPLETARGS.size(), "Stop color argsMissingWithoutFile test had non-zero PAPPLETARGS length");
      failWithMessage("testStopColorOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "-c requires 1 argument(s)\n", errContent.toString(), "Display argsMissingWithoutFile test failed to produce correct error");
    reset();
    handleOutput();
  }
  
  @Test
  public void testHideStopOptions() {
    String[] argsShort = {"-hs", "/path/to/test"};
    String[] argsLong = {"--hide-stop", "/path/to/test"};
    
    try {
      Pycessing.getArgs(testOptions, argsShort);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHideStopOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_HIDE_STOP, Pycessing.PAPPLETARGS.get(0), "Hide stop short test");
    reset();
    
    try {
      Pycessing.getArgs(testOptions, argsLong);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHideStopOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_HIDE_STOP, Pycessing.PAPPLETARGS.get(0), "Hide stop long test");
    reset();
  }
  
  @Test
  public void testSketchPathOptions() {
    FileAttribute<Set<PosixFilePermission>> functioningPermissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"));
    FileAttribute<Set<PosixFilePermission>> noWritePermission = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr-xr-x"));

    Path writableParentDir = null;
    Path writableTmpDir = null;
    Path writableTmpFile = null;
    
    try {
      writableParentDir = Files.createTempDirectory("writableParentDir", functioningPermissions);
      writableTmpDir = Paths.get(writableParentDir.toString(), "writableTmpDir");
      writableTmpFile = Paths.get(writableTmpDir.toString(), "writableTmpFile");
    } catch (IOException e) {
      e.printStackTrace();
      failWithMessage("Failed to create temp dirs: \n" + errContent.toString());
    }
    
    String[] argsShort = {"-s", writableTmpDir.toString(), "/path/to/file"};
    String[] argsLong = {"--sketch-path", writableTmpDir.toString(), "/path/to/file"};
    // Using writableTmpDir for cleanup
    String[] argsMissingWithFile = {"-s", writableTmpDir.toString()};
    String[] argsMissingWithoutFile = {"-s"};
    
    try {
      // Checking for args being set ignoring filesystem
      try {
        Pycessing.getArgs(testOptions, argsShort);
        assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_SKETCH_FOLDER + "=" + writableTmpDir.toString(), "Sketch folder short test");
      } catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions argsShort Caught Exception: " + errContent.toString());
      }
      reset();
      handleOutput();
      
      try {
        Pycessing.getArgs(testOptions, argsLong);
        assertEquals(Pycessing.PAPPLETARGS.get(0), PApplet.ARGS_SKETCH_FOLDER + "=" + writableTmpDir.toString(), "Sketch folder long test");
      } catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions argsLong Caught Exception: " + errContent.toString());
      }
      reset();
      handleOutput();
      
      // This should actually be accepted, assume the arg is the path to the sketch file, and set INTERACTIVE to true
      try {
        Pycessing.getArgs(testOptions, argsMissingWithFile);
        assertEquals(PApplet.ARGS_SKETCH_FOLDER + "=" + writableTmpDir, Pycessing.PAPPLETARGS.get(0), "Sketch folder argsMissingWithFile test");
        assertTrue(Pycessing.INTERACTIVE, "Sketch folder argsMissingWithFile failed to set INTERACTIVE to true");
      } catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions argsMissingWithFile Caught Exception: " + errContent.toString());
      }
      reset();
      handleOutput();
      
      // Should fail with missing arg exception
      try {
        Pycessing.getArgs(testOptions, argsMissingWithoutFile);
      } catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions argsMissingWithoutFile Caught Exception: " + errContent.toString());
      }
      assertEquals(standardUsageString + "-s requires 1 argument(s)\n", errContent.toString(), "Display argsMissingWithoutFile test failed to produce correct error");
      reset();
      handleOutput();
      
      // Check actual file operations
      // Cleanup in case something is left over
      Files.deleteIfExists(writableTmpFile);
      Files.deleteIfExists(writableTmpDir);
      Files.deleteIfExists(writableParentDir);
      
      // For simplicity
      String content;
      Pattern p;
      Matcher m;
      
      
      // Should create directory
      try {
        Pycessing.getArgs(testOptions, argsShort);
      }catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions directory creation test caught Exception: " + errContent.toString());
      }
      if (!Files.isDirectory(writableTmpDir)) {
        failWithMessage("testSketchPathOptions directory creation test failed to create directory " + writableTmpDir + "\nOutput:\n" +
            outContent.toString() + "\nError:\n" +
            errContent.toString());
      }
      reset();
      handleOutput();
      Files.deleteIfExists(writableTmpFile);
      Files.deleteIfExists(writableTmpDir);
      
      // Should fail because there's a file where the directory should be
      p = Pattern.compile("^usage:.*A file already exists at.*writableTmpDir$", Pattern.DOTALL);
      Files.createFile(writableTmpDir, functioningPermissions);
      try {
        Pycessing.getArgs(testOptions, argsShort);
      }catch (Exception e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions directory creation test caught Exception: " + errContent.toString());
      }
      content = errContent.toString();
      m = p.matcher(content);
      assertTrue(m.find(), "testSketchPathOptions file exists failure test got result: \n" + content + "\nFrom match: " + m.find());
      reset();
      handleOutput();
      Files.deleteIfExists(writableTmpDir);
      Files.deleteIfExists(writableTmpFile);
      Files.deleteIfExists(writableTmpDir);
      
    } catch (Exception e1) {
      e1.printStackTrace();
      failWithMessage("testSketchPathOptions failed with uncaught exception: \n" + errContent.toString());
    } finally {
      // Clean up temp files
      try {
        
        if (Files.exists(writableTmpDir)) {
          if (Files.isDirectory(writableTmpDir)) {
            Files.deleteIfExists(writableTmpFile);
            Files.deleteIfExists(writableTmpDir);
          } else {
            Files.deleteIfExists(writableTmpDir);
          }
        }
        Files.deleteIfExists(writableParentDir);
      } catch (IOException e) {
        e.printStackTrace();
        failWithMessage("testSketchPathOptions failed to delete temp files: \n" + errContent.toString());
      }
    }
  }

}
