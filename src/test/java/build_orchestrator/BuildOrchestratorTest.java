/**
 * Created by Davide on 2026-02-01 .
 *
 * @formatter:off
 */
package build_orchestrator;

import build_orchestrator.BuildList.ModuleBlock;
import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import duser_input_output.impl.consoleUserIO.ConsoleUserIO;
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

import static build_orchestrator.AppContext.newAppContext;
import static build_orchestrator.BuildOrchestratorMain.newBuildOrchestrator;
import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.newValidatedFile;
import static dfile.file.FileUtilities.write;
import static dlog.log.Log.writeLogsHeaders;
import static dtest.TestUtilities.assertFilesEqual;
import static dtest.TestUtilities.newUserIOForTests;
import static dutil.exception.ExceptionUtilities.getUnchecked;
import static dutil.list.text.TextListUtilities.assertNoneBlank;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.TEN_i;
import static dutil.object.ObjectUtilities.B;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.string.TextUtilities.DASH80;
import static dutil.string.TextUtilities.FMT_DT2;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.dq;


public class BuildOrchestratorTest {
  
  
  private static final String APP_NAME =  BuildOrchestratorTest.class.getSimpleName();
  
  private static final String APP_DESCR = BuildOrchestratorTest.class.getName();
  
  private AppContext appContext;
  
  
  @BeforeClass
  public void beforeClass() {
    
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
      
      throw getUnchecked(e);
    }
    this.appContext = null;
  }
  
  
  /**
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "01".
   */
  @Test
  public void buildListTest01() throws Exception {
    
    testBuildList("01", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "02".
   */
  @Test
  public void buildListTest02() throws Exception {
    
    testBuildList("02", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "03".
   */
  @Test
  public void buildListTest03() throws Exception {
    
    testBuildList("03", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "04".
   */
  @Test
  public void buildListTest04() throws Exception {
    
    testBuildList("04", this.appContext);
  }
  
  /**
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "05a".
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
   * Calls {@link #testBuildList(String, AppContext) testBuildList(*)} passing to it the test ID "05b".
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
   * Calls {@link #testDeploy(String, AppContext) testDeploy(*)} passing to it the test ID "01".
   */
  @Test
  public void deployTest01() throws Exception {
    
    testDeploy("01", this.appContext);
  }
  
  /**
   * {@link #newBuildOrchestrator Creates} and {@link BuildOrchestrator#run() run}s a {@link BuildOrchestrator} instance
   *        according to the BuildOrchestrator configuration file identified by the given {@code testID}, and if the
   *        generated Build List files are different from their "OK" file fails the test.
   *
   * @param testID Identifies the set of data used by a specific test ran by this method. E.g. "{@code 01}".<br>Used to:<ul>
   *               <li>{@link AppContext#outUser show} it in the console to indicate which test method is running.</li>
   *               <li>Choose the BuildOrchestrator config file to use to run the test.</li></ul>
   */
  private static void testBuildList(@NotBlank String testID, @NotNull AppContext ac) throws Exception {
    
    ac.outUser(NL2 + DASH80 + NL + "Method testBuildList with ID " + dq(assertNonBlankNorTrimmable(testID)) + " :" + NL);
    
    try {
      
      writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
      
      final String testDataPath = calcPath("src", "test", "resources");
      
      final BuildOrchestrator orchestrator = newBuildOrchestrator_DontBuild(
                                new String[] {
                                                      assertExistingPath(
                                                        calcPath(
                                              testDataPath, "BuildOrchestrator-Config_TestBuildList" + testID + ".TXT")
                                          , false)
                                                    }
                                , ac);
      orchestrator.run();
      
      // Get the created Build List, save it to file and compare the file with the expected one :
      
      final BuildList buildList = orchestrator.getBuildList();
      
      final File fileTest = newValidatedFile(calcPath(testDataPath, "TEST-BuildList-" + testID + ".DUMP"), false, MINUS1_i);
      
      write(fileTest, buildList.toString(), null);
      
      final File fileOK = newValidatedFile(calcPath(testDataPath, fileTest.getName() + "-{OK}"), true, TEN_i);
      
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
  
  /**
   * {@link #newBuildOrchestrator Creates} a {@link BuildOrchestrator} instance according to the BuildOrchestrator configuration file identified by the given {@code
   * testID}, does not {@link BuildOrchestrator#run() run} the instance (so no test artifacts get built), calls {@link BuildOrchestrator#deployBuiltModule(ModuleBlock)
   * deployBuiltModule(*)} and tests that the artifact file(s) have been moved or deleted as expected.
   *
   * @param testID Identifies the set of data used by a specific test ran by this method. E.g. "{@code 01}".<br>Used to:<ul>
   *               <li>{@link AppContext#outUser show} it in the console to indicate which test method is running.</li>
   *               <li>Choose the BuildOrchestrator config file to use to run the test.</li></ul>
   */
  private static void testDeploy(@NotBlank String testID, @NotNull AppContext ac) throws Exception {
    
    ac.outUser(NL2 + DASH80 + NL + "Method testDeploy with ID " + dq(assertNonBlankNorTrimmable(testID)) + " :" + NL);
    
    try {
      
      writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
      
      final String testDataPath = calcPath("src", "test", "resources");
      
      final BuildOrchestrator orchestrator = newBuildOrchestrator(
            new String[] {
                                  assertExistingPath(
                                    calcPath(testDataPath, "BuildOrchestrator-Config_TestDeploy" + testID + ".TXT")
                                            , false)
                               }
            , ac);
      
      orchestrator.run();
      
      
      // @@@ q @@
      
      
      /* @@@@ NO - PASTED FROM BUILD LIST TESTS - ADJUST / REMOVE @@@@@@@@@@@@@@@@@@@@@@@@@@@@@
      // Get the created Build List, save it to file and compare the file with the expected one :
      
      final BuildList buildList = orchestrator.getBuildList();
      
      final File fileTest = newValidatedFile(calcPath(testDataPath, "TEST-BuildList-" + testID + ".DUMP"), false, MINUS1_i);
      
      write(fileTest, buildList.toString(), null);
      
      final File fileOK = newValidatedFile(calcPath(testDataPath, fileTest.getName() + "-{OK}"), true, TEN_i);
      
      assertFilesEqual(fileTest, fileOK, L(10));
      
      ac.outUser("Build List successfully compared with expected (" + fileOK.length() + " bytes).");
      
      ac.outUser(NL + LocalDateTime.now().format(FMT_DT2));
      
      */
      
    }
    catch (UserRequestedTermination e) {
      
      throw new InternalErrorException(e);
    }
    finally {
      
      ac.showLogInfo();
    }
  }
  
  /**
   * Same params as {@link BuildOrchestratorMain#newBuildOrchestrator(String[], AppContext)}.
   *
   * @return A {@link BuildOrchestratorMain#newBuildOrchestrator new BuildOrchestrator} with {@link BuildOrchestrator#dontBuild
   *         dontBuild} {@code true}, to be used by the tests that must avoid to actually build.
   */
  private static BuildOrchestrator newBuildOrchestrator_DontBuild(String[] args, @NotNull AppContext ac) throws UserRequestedTermination {
    
    final BuildOrchestrator result = newBuildOrchestrator(args, ac);
    
    result.dontBuild = true;
    
    return result;
  }
  
  
  /** TODO @@@ UNIFY AND MOVE TO DTest .
   *
   * @return A new {@link AppContext} to be used from tests.<br>Its {@link AppContext#userIO userIO} is an instance of
   *         type of the given {@code userIOClass} {@link Class} and has {@link AUserInputOutput#muted muted} {@code
   *         true}.
   */
  public static AppContext newAppContextForTests(@NotBlank String appName, @NotBlank String  appDescr
                                               , @NotNull  Class<? extends AUserInputOutput> userIOClass) {
    
    assertNoneBlank(appName, appDescr);
    
    final AppContext appContext = newAppContext(newUserIOForTests(userIOClass)
                                    , new Log(appDescr + " - screen log",   appName + "_screen-log.LOG"
                                                      , true)
                                      , new Log(appDescr + " - user log",     appName + "_user-log.LOG"
                                                      , true)
                                       , new Log(appDescr + " - developer log",appName + "_dev-log.LOG"
                                                      , true));
    
    assertTrue(appContext.userIO.muted == false, "'muted' is", B(appContext.userIO.muted) + ".", NL, "This consistency check exists only because the default for 'muted' is false, but if this design changes, just update this consistency check.");
    
    assertTrue(appContext.screenLog.logBare, "The Screen Log must have logBare true. It can be set here instead of asserting, but why is it not true already ?");
    
    return appContext;
  }
  
  /** TODO @@@ UNIFY AND MOVE TO DTest .
   * @return A new {@link AppContext} to be used from tests.<br>Its {@link AppContext#userIO userIO} is an instance of {@link
  ConsoleUserIO} and has {@link AUserInputOutput#muted muted} = {@code true}.
   */
  public static AppContext newAppContextForTests(@NotBlank String appName, @NotBlank String appDescr) {
    
    return newAppContextForTests(appName, appDescr, ConsoleUserIO.class);
  }
  
}
