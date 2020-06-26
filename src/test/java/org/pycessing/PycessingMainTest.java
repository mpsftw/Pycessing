package org.pycessing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.pycessing.Pycessing;

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

import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;
import processing.core.PApplet;


public class PycessingMainTest {
  
  
  private static Options testOptions;
  
  private  static ByteArrayOutputStream outContent;
  private  static ByteArrayOutputStream errContent;
  private final static PrintStream originalOut = System.out;
  private final static PrintStream originalErr = System.err;
  private  static PrintStream capturedOut;
  private  static PrintStream capturedErr;
  private static final FileAttribute<Set<PosixFilePermission>> writePermissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"));
  private static final FileAttribute<Set<PosixFilePermission>> noWritePermission = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr-xr-x"));
  
  @TempDir
  public File testDir;
  private File testFile;
  private Path testFilePath;
  
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
  
  private static void reset() {
    Pycessing.INTERACTIVE=false;
    Pycessing.VERBOSE=false;
    Pycessing.PAppletArgs.clear();
  }

  private static void failWithMessage(String msg) {
    releaseOutput();
    fail(msg);
    captureOutput();
  }
  
  private static void captureOutput() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    capturedOut =  new PrintStream(outContent);
    capturedErr = new PrintStream(errContent);
    System.setOut(capturedOut);
    System.setErr(capturedErr);
  }
  
  private static void releaseOutput() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    MockitoAnnotations.initMocks(PycessingMainTest.class);
    testOptions=Pycessing.getOptions();
    captureOutput();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    releaseOutput();
    reset();
  }

  @BeforeEach
  public void setUp() throws Exception {
    testFilePath = Paths.get(testDir.getAbsolutePath()).resolve("testfile.txt");
    testFile = testFilePath.toFile();
    
    captureOutput();
  }

  @AfterEach
  public void tearDown() throws Exception {
    releaseOutput();
    
    
    Files.deleteIfExists(testFilePath);
    Mockito.validateMockitoUsage();
    reset();
  }

  @Test
  @Timeout(5)
  public void testDefaults() {
    assertFalse(Pycessing.INTERACTIVE);
    assertFalse(Pycessing.VERBOSE);
    assertNotNull(Pycessing.PyApplet);
    assertNotNull(Pycessing.PAppletArgs);
  }
  
  @Test
  @Timeout(5)
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
  @Timeout(5)
  public void testShowHelpNoMessageStdout() {
    Pycessing.showHelp("", true);
    assertEquals(standardUsageString, outContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testShowHelpWithMessageStdout() {
    Pycessing.showHelp("This is a test", true);
    assertEquals(standardUsageString + "This is a test\n", outContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testShowHelpWithMessageStderr() {
    Pycessing.showHelp("This is a test", false);
    assertEquals(standardUsageString + "This is a test\n", errContent.toString());
  }
  
  @Test 
  @Timeout(5)
  public void testGetFilenameMultipleOptions() {
    String filename = testFile.getAbsolutePath();
    String[] args = {"-i", "-p", filename};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testGetFilenameMultipleOptions caught an exception: " + errContent.toString());
    }
    assertEquals(filename, Pycessing.fileFromCLI);
  }
  
  @Test 
  @Timeout(5)
  public void testGetFilenameNoOptions() {
    String filename = testFile.getAbsolutePath();
    String[] args = {filename};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testGetFilenameNoOptions caught an exception: " + errContent.toString());
    }
    assertEquals(filename, Pycessing.fileFromCLI);
  }
  
  @Test
  @Timeout(5)
  public void testHelpShort() {
    String[] args = {"-h"};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp shortArgs caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testHelpLong() {
    String[] args = {"--help"};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp long caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testHelpTooMany() {

    String[] args = {"-i", "-p", "blah", "1", "2", "3"};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp too many args caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "Too many options\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testHelpWithOtherArgs() {
    String[] args = {"-i", "-h", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp with other args caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testHelpWithFile() {
    String[] args = { "-h", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHelp with file caught an exception: " + errContent.toString());
    }
    assertEquals(standardUsageString, outContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testInteractiveOptionsEmptyArgs() {
    String[] args = {};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsEmpty Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE);
  }
  
  @Test
  @Timeout(5)
  public void testInteractiveShortArgs() {
    String[] args = {"-i", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE);
  }

  @Test
  @Timeout(5)
  public void testInteractiveLongArgs() {
    String[] args = {"--interactive"};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testInteractiveOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.INTERACTIVE);
  }
  
  @Test
  @Timeout(5)
  public void testVerboseOptionsShortArgs() {
    String[] args = {"-v", testFile.getAbsolutePath()};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testVerboseOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.VERBOSE);
  }
    
  @Test 
  @Timeout(5)
  public void testVerboseOptionsLongArgs() {
    String[] args = {"--verbose", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testVerboseOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertTrue(Pycessing.VERBOSE);
  }
  
  @Test
  @Timeout(5)
  public void testDisplayOptionsShortArgs() {
    String[] args = {"-d", "0", testFile.getAbsolutePath()};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions short args Caught Exception: " + errContent.toString());
    }
    assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_DISPLAY + "=0");
    reset();
  }
   
  @Test
  @Timeout(5)
  public void testDisplayOptionsLongArgs() {
    String[] args = {"--display", "0", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions long args Caught Exception: " + errContent.toString());
    }
    assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_DISPLAY + "=0");
  }
    
  @Test
  @Timeout(5)
  public void testDisplayOptionsMissingArgsWithFile() {
    String[] args = {"-d", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(0, Pycessing.PAppletArgs.size());

    Pattern regex = Pattern.compile("(.*)Display argument must be a number. Recieved:(.*)", Pattern.DOTALL);
    String content = errContent.toString();
    Matcher m = regex.matcher(content);
    assertTrue(m.find(), "testDisplayOptionsMissingArgsWithFile test failed to produce correct error\n" +
        "Expected:\n" + regex.toString() +
        "\nRecieved:\n" + content + "\n");
    //assertEquals(standardUsageString + "Display argument must be a number. Recieved: " + testFile.getAbsolutePath() + "\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testDisplayOptionsMissingArgsWithoutFile() {
    String[] args = {"-i", "-d"};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testDisplayOptions argsMissingWithoutFile Caught Exception: " + errContent.toString());
    }
    assertEquals(0, Pycessing.PAppletArgs.size());
    assertEquals(standardUsageString + "-d requires 1 argument(s)\n", errContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testWindowColorOptionsShort() {
    String[] args = {"-w", "#FFFFFF", testFile.getAbsolutePath()};
    
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_WINDOW_COLOR + "=#FFFFFF");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testWindowColorOptions argsShort Caught Exception: " + errContent.toString());
    }
  }
   
  @Test
  @Timeout(5)
  public void testWindowColorOptionsLong() {
    String[] args = {"--window-color", "#FFFFFF", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_WINDOW_COLOR + "=#FFFFFF");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testWindowColorOptions argsLong Caught Exception: " + errContent.toString());
    }
  }
   
  @Test
  @Timeout(5)
  public void testWindowColorOptionsArgNotAColor() {
    String[] args = { "-w", "notacolor", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testDisplayOptions argsNotColor Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "notacolor was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testWindowColorOptionsArgsMissingWithFile() {
    String[] args = { "-w", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    
    Pattern regex = Pattern.compile("(.*)was not in the form of a hexadecimal like #FFFFFF(.*)", Pattern.DOTALL);
    String content = errContent.toString();
    Matcher m = regex.matcher(content);
    assertTrue(m.find(), "testWindowColorOptionsArgsMissingWithFile test failed to produce correct error\n" +
        "Expected:\n" + regex.toString() +
        "\nRecieved:\n" + content + "\n");
    //assertEquals(standardUsageString + "/path/to/file was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testWindowColorOptionsArgsMissingWithoutFile() {
    String[] args = { "-w" };
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testDisplayOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "-w requires 1 argument(s)\n", errContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testPresentOptionsShortArgs() {
    String[] args = {"-p", testFile.getAbsolutePath()};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testPresentOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_PRESENT, Pycessing.PAppletArgs.get(0));
  }
    
  @Test
  @Timeout(5)
  public void testPresentOptionsLongArgs() {
    String[] args = {"--present", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testPresentOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_PRESENT, Pycessing.PAppletArgs.get(0));
    reset();
  }
  
  @Test
  @Timeout(5)
  public void testStopColorOptionsShort() {
    String[] args = {"-c", "#FFFFFF", testFile.getAbsolutePath()};
    
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_STOP_COLOR + "=#FFFFFF");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testStopColorOptions argsShort Caught Exception: " + errContent.toString());
    }
  }
    
  @Test
  @Timeout(5)
  public void testStopColorOptionsLong() {
    String[] args = {"--stop-color", "#FFFFFF", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_STOP_COLOR + "=#FFFFFF");
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testStopColorOptions argsLong Caught Exception: " + errContent.toString());
    }
  }
    
  @Test
  @Timeout(5)
  public void testStopColorOptionsArgsNotAColor() {
    String[] args = { "-c", "notacolor", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testStopColorOptions argsNotColor Caught Exception: " + errContent.toString());
    }
    
    assertEquals(standardUsageString + "notacolor was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testStopColorOptionsArgsMissingWithFile() {
    String[] args = { "-c", testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testStopColorOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }

    Pattern regex = Pattern.compile("(.*)was not in the form of a hexadecimal like #FFFFFF(.*)", Pattern.DOTALL);
    String content = errContent.toString();
    Matcher m = regex.matcher(content);
    assertTrue(m.find(), "testStopColorOptionsArgsMissingWithFile test failed to produce correct error\n" +
        "Expected:\n" + regex.toString() +
        "\nRecieved:\n" + content + "\n");
    //assertEquals(standardUsageString + "/path/to/file was not in the form of a hexadecimal like #FFFFFF\n", errContent.toString());
  }
    
  @Test
  @Timeout(5)
  public void testStopColorOptionsArgsMissingWithoutFile() {
    String[] args = { "-c" };
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(0, Pycessing.PAppletArgs.size());
      failWithMessage("testStopColorOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "-c requires 1 argument(s)\n", errContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testHideStopOptionsShort() {
    String[] args = {"-hs", "/path/to/test"};
    
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHideStopOptions argsShort Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_HIDE_STOP, Pycessing.PAppletArgs.get(0));
  }
    
  @Test
  @Timeout(5)
  public void testHideStopOptionsLong() {
    String[] args = {"--hide-stop", "/path/to/test"};
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (ParseException e) {
      e.printStackTrace();
      failWithMessage("testHideStopOptions argsLong Caught Exception: " + errContent.toString());
    }
    assertEquals(PApplet.ARGS_HIDE_STOP, Pycessing.PAppletArgs.get(0));
  }
  
  @Test
  @Timeout(5)
  public void testSketchPathOptionsShort(@TempDir Path sketchDir) {
    String[] args = {"-s", sketchDir.toString(), testFile.getAbsolutePath()};
    // Using writableTmpDir for cleanup
    
    // Checking for args being set ignoring filesystem
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_SKETCH_FOLDER + "=" + sketchDir.toString());
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions argsShort Caught Exception: " + errContent.toString());
    }
  }
     
  @Test
  @Timeout(5)
  public void testSketchPathOptionsLong(@TempDir Path sketchDir) {
    String[] args = {"--sketch-path", sketchDir.toString(), testFile.getAbsolutePath()};
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(Pycessing.PAppletArgs.get(0), PApplet.ARGS_SKETCH_FOLDER + "=" + sketchDir.toString());
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions argsLong Caught Exception: " + errContent.toString());
    }
  }
      
  @Test
  @Timeout(5)
  public void testSketchPathOptionsArgsMissingWithFile(@TempDir Path sketchDir) {
    String[] args = {"-s", sketchDir.toString()};
    // This should actually be accepted, assume the arg is the path to the sketch file, and set INTERACTIVE to true
    try {
      Pycessing.getArgs(testOptions, args);
      assertEquals(PApplet.ARGS_SKETCH_FOLDER + "=" + sketchDir, Pycessing.PAppletArgs.get(0));
      assertTrue(Pycessing.INTERACTIVE);
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions argsMissingWithFile Caught Exception: " + errContent.toString());
    }
  }
   
  @Test
  @Timeout(5)
  public void testSketchPathOptionsArgsMissingWithoutFile() {
    String[] args = {"-s"};
    // Should fail with missing arg exception
    try {
      Pycessing.getArgs(testOptions, args);
    } catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions argsMissingWithoutFile Caught Exception: " + errContent.toString());
    }
    assertEquals(standardUsageString + "-s requires 1 argument(s)\n", errContent.toString());
  }
  
  @Test
  @Timeout(5)
  public void testSketchPathOptionsDirectoryCreation(@TempDir Path sketchDir) {
    Path sketch = sketchDir.resolve("dir");
    String[] args = {"-s", sketch.toString(), testFile.getAbsolutePath()};
    // Should create directory
    try {
      Pycessing.getArgs(testOptions, args);
    }catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions directory creation test caught Exception: " + errContent.toString());
    }
    
    if (!Files.isDirectory(sketch)) {
      failWithMessage("testSketchPathOptions directory creation test failed to create directory " + sketch + "\nOutput:\n" +
          outContent.toString() + "\nError:\n" +
          errContent.toString());
    }
  }
  
  @Test
  @Timeout(5)
  public void testSketchPathOptionsDirectoryIsAFile(@TempDir Path sketchDir) {
    Path sketchFile = sketchDir.resolve("file");
    File f = sketchFile.toFile();
    try {
      f.createNewFile();
    } catch (IOException e1) {
      e1.printStackTrace();
      failWithMessage("Failed to create " + f.getAbsolutePath());
    }
    
    if (!f.exists()) {
      failWithMessage("Failed to create " + f.getAbsolutePath());
    }
    
    // Expect line breaks in the file name, so use pattern matching instead
    Pattern regex = Pattern.compile("(.*)A file already exists at(.*)", Pattern.DOTALL);
    
    String[] args = {"-s", f.getAbsolutePath(), testFile.getAbsolutePath()};
    
    // Should create directory
    try {
      Pycessing.getArgs(testOptions, args);
    }catch (Exception e) {
      e.printStackTrace();
      failWithMessage("testSketchPathOptions directory creation test caught Exception: " + errContent.toString());
    }
    String content = errContent.toString();
    Matcher m = regex.matcher(content);
    assertTrue(m.find(), "Display testSketchPathOptionsDirectoryIsAFile test failed to produce correct error\n" +
        "Expected:\n" + regex.toString() +
        "\nRecieved:\n" + content + "\n");
  }

}
