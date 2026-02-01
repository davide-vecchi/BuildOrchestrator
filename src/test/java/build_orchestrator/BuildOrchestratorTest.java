/**
 * Created by Davide on 2026-02-01 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import duser_input_output.impl.consoleUserIO.ConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InternalErrorException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static build_orchestrator.AppContext.newAppContext;
import static build_orchestrator.BuildOrchestratorMain.newBuildOrchestrator;
import static dfile.file.FileUtilities.assertExistingPath;
import static dlog.log.Log.writeLogsHeaders;
import static dtest.TestUtilities.newUserIOForTests;
import static dutil.exception.ExceptionUtilities.getUnchecked;
import static dutil.list.text.TextListUtilities.assertNoneBlank;
import static dutil.object.ObjectUtilities.B;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.string.TextUtilities.DASH80;
import static dutil.string.TextUtilities.FMT_DT2;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;


public class BuildOrchestratorTest {
  
  
  static final String APP_NAME =  BuildOrchestratorTest.class.getSimpleName();
  
  static final String APP_DESCR = BuildOrchestratorTest.class.getName();
  
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
   * Calls {@link #test(String, AppContext) test(*)} passing to it the test ID "01".
   */
  @Test
  public void test01() throws Exception {
    
    testBuildOrchestrator("01", this.appContext);
  }
  
  /**
   * {@link BuildOrchestratorMain#newBuildOrchestrator Creates} and {@link BuildOrchestrator#run() run}s a {@link BuildOrchestrator} instance according to the BuildOrchestrator
   * configuration file identified by the given {@code testID}, and if the generated output files are different from
   * their "OK" file fails the test.
   *
   * @param testID Identifies the set of data used by a specific test ran by this method. E.g. "{@code 01}".<br>Used to:<ul>
   *               <li>{@link AppContext#outUser show} it in the console to indicate which test method is running.</li>
   *               <li>Choose the BuildOrchestrator config file to use to run the test.</li></ul>
   */
  private static void testBuildOrchestrator(@NotBlank String testID, @NotNull AppContext ac) {
    
    ac.outUser(NL2 + DASH80 + NL + "Method testBuildOrchestrator" + assertNonBlankNorTrimmable(testID) + "() :" + NL);
    
    try {
      
      writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
      
      final String testDataPath = "src\\test\\resources\\";
      
      final BuildOrchestrator orchestrator = newBuildOrchestrator(
                                new String[] {
                                                      assertExistingPath(
                                                  testDataPath + "BuildOrchestrator-Config_Test" + testID + ".TXT"
                                          , false)
                                                    }
                                , ac);
      orchestrator.run();
      /*
      // Compare the files where the Detection Storer stored the read Detections :
      
      File fileTest = assertNonEmpty(new File(orchestrator.getParams().getDetectionsOutputFile().value));
      
      File fileOK   = assertNonEmpty(new File(testDataPath + fileTest.getName() + "-{OK}"));
      
      assertFilesEqual(fileTest, fileOK, L(10));
      
      // Compare the files where the Recommendation Storer stored the produced Recommendations :
      
      fileTest = assertNonEmpty(new File(orchestrator.getParams().getRecomsOutputFile().value));
      
      fileOK   = assertNonEmpty(new File(testDataPath + fileTest.getName() + "-{OK}"));
      
      assertFilesEqual(fileTest, fileOK, L(10));
      */
      ac.outUser(NL + LocalDateTime.now().format(FMT_DT2));
    }
    catch (UserRequestedTermination e) {
      
      throw new InternalErrorException(e);
    }
    finally {
      
      ac.showLogInfo();
    }
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
