/**
 * Created by Davide on 2026-02-01 .
 */
package build_orchestrator;

import application.AAppContext;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InternalErrorException;
import dutil.exception.exceptions.InvalidExternalValueException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.time.LocalDateTime;

import static build_orchestrator.BuildOrchestratorMain.newBuildOrchestrator;
import static build_orchestrator.BuildOrchestratorTestUtilities.newAppContextForTests;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.newValidatedFile;
import static dfile.file.FileUtilities.write;
import static dlog.log.Log.writeLogsHeaders;
import static dtest.TestUtilities.assertFilesEqual;
import static dutil.exception.ExceptionUtilities.calcUnchecked;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.TEN_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.DASH80;
import static dutil.string.TextUtilities.FMT_DT2;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.setSystemEncodingUTF8;


// @formatter:off


public class BuildOrchestratorTest {
  
  
  private static final String APP_NAME =  BuildOrchestratorTest.class.getSimpleName();
  
  private static final String APP_DESCR = BuildOrchestratorTest.class.getName();
  
  private AAppContext appContext;
  
  
  @BeforeClass
  public void beforeClass() {
    
    setSystemEncodingUTF8();
    
    assertNull(this.appContext);
    
    this.appContext = newAppContextForTests(APP_NAME, APP_DESCR);
  }
  
  @AfterClass
  public void afterClass() {
    
    assertNonNull(this.appContext);
    
    try {
      
      this.appContext.close();
    }
    catch (Exception e) {
      
      throw calcUnchecked(e);
    }
    this.appContext = null;
  }
  
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "01".
   */
  @Test
  public void buildListTest01() throws Exception {
    
    testBuildList("01", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "02".
   */
  @Test
  public void buildListTest02() throws Exception {
    
    testBuildList("02", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "03".
   */
  @Test
  public void buildListTest03() throws Exception {
    
    testBuildList("03", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "04".
   */
  @Test
  public void buildListTest04() throws Exception {
    
    testBuildList("04", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "05a".
   */
  @Test()
  public void buildListTest05a() throws Exception {
    
    try {

      testBuildList("05a", this.appContext);
      
      Assert.fail("An " + InvalidExternalValueException.class.getSimpleName() + " was expected, instead nothing was thrown.");
    }
    catch (InvalidExternalValueException expected) {
      
      final String expectedStart = "Invalid section name 'InvalidSectionName_MustBeDetectedAsSuch'";
      
      Assert.assertTrue(expected.getMessage().startsWith(expectedStart)
                        , "Exception " + expected.getClass().getSimpleName() + " was thrown as expected, but its message is:"
                                 + NL2 + expected.getMessage() + NL2 + "while it was expected to start with:" + NL2 + expectedStart + NL);
    }
  }
  
  /**
   * Calls {@link #testBuildList(String, AAppContext) testBuildList(*)} passing to it the test ID "05b".
   */
  @Test()
  public void buildListTest05b() throws Exception {
    
    try {
      
      testBuildList("05b", this.appContext);
      
      Assert.fail("An " + InvalidExternalValueException.class.getSimpleName() + " was expected, instead nothing was thrown.");
    }
    catch (InvalidExternalValueException expected) {
      
      final String expectedStart = "Content found outside of sections";
      
      Assert.assertTrue(expected.getMessage().startsWith(expectedStart)
                        , "Exception " + expected.getClass().getSimpleName() + " was thrown as expected, but its message is:"
                                 + NL2 + expected.getMessage() + NL2 + "while it was expected to start with:" + NL2 + expectedStart + NL);
    }
  }
  
  /**
   * {@link BuildOrchestratorMain#newBuildOrchestrator Creates} and {@link BuildOrchestrator#run() run}s a {@link
   *        BuildOrchestrator} instance loading a Build List identified through the given {@code testID}, saves it to
   *        file, and if that file is different from the corresponding "OK" file fails the test.
   *
   * @param testID Identifies the set of data used by a specific test ran by this method. E.g. "{@code 01}".<br>Used to:<ul>
   *               <li>{@link AAppContext#outUser show} it in the console to indicate which test method is running.</li>
   *               <li>Choose the BuildOrchestrator config file to use to run the test.</li></ul>
   */
  private static void testBuildList(@NotBlank String testID, @NotNull AAppContext ac) throws Exception {
    
    ac.outUser(NL2 + DASH80 + NL + "Method testBuildList with ID " + dq(assertNonBlankNorTrimmable(testID)) + " :" + NL);
    
    ac.showLogInfo();
    
    try {
      
      writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
      
      final String testDataPath = calcPath("src", "test", "resources");
      
      ac.outUser(NL + "testDataPath calculated as " + dq(testDataPath) + "." + NL);
      
      final BuildOrchestrator orchestrator = newBuildOrchestrator(null
                                                            , "_TestBuildList" + testID, ac);
      
      // 'orchestrator.run()' not called, because this test doesn't test the whole build process but just the loading of
      // the Build List.
      
      // Create a Build List, save it to file and compare the file with the expected one :
      
      final BuildList buildList = BuildList.newBuildList(orchestrator.getBuildListFile(), ac);
      
      final File fileTest = newValidatedFile(calcPath(testDataPath, "TEST-BuildList-" + testID + ".DUMP")
                                       , false, MINUS1_i);
      
      write(fileTest, buildList.toString(), null);
      
      final File fileOK = newValidatedFile(calcPath(testDataPath, fileTest.getName() + "-{OK}")
                                     , true, TEN_i);
      
      assertFilesEqual(fileTest, fileOK, L(10));
      
      ac.outUser("Build List successfully compared with expected (" + fileOK.length() + " bytes).");
      
      ac.outUser(NL + LocalDateTime.now().format(FMT_DT2));
    }
    catch (UserRequestedTermination e) {
      
      throw new InternalErrorException(e);
    }
    finally {
      
      ac.showLogInfo();
    }
  }
  
}
