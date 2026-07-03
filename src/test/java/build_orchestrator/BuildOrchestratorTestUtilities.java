/**
 * Created by Davide on 2026-07-03 .
 */
package build_orchestrator;

import application.AAppContext;
import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import duser_input_output.impl.consoleUserIO.ConsoleUserIO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static build_orchestrator.AppContext.newAppContext;
import static dtest.TestUtilities.newUserIOForTests;
import static dutil.list.text.TextListUtilities.assertNoneBlank;
import static dutil.object.ObjectUtilities.B;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.string.TextUtilities.NL;


// @formatter:off


/**
 * Utilities for {@link BuildOrchestrator} tests.
 */
public abstract class BuildOrchestratorTestUtilities {
  
  
  /**
   * @return A new {@link AppContext} to be used from tests.<br>Its {@link AAppContext#userIO userIO} is an instance of
   *         type of the given {@code userIOClass} {@link Class} and has {@link AUserInputOutput#muted muted} {@code
   *         true}.
   */
  public static AAppContext newAppContextForTests(@NotBlank String appName, @NotBlank String  appDescr
                                               , @NotNull  Class<? extends AUserInputOutput> userIOClass) {
    
    assertNoneBlank(appName, appDescr);
    
    final AAppContext appContext = newAppContext(newUserIOForTests(userIOClass)
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
  
  /**
   * @return A new {@link AppContext} to be used from tests.<br>Its {@link AAppContext#userIO userIO} is an instance of {@link
  ConsoleUserIO} and has {@link AUserInputOutput#muted muted} = {@code true}.
   */
  public static AAppContext newAppContextForTests(@NotBlank String appName, @NotBlank String appDescr) {
    
    return newAppContextForTests(appName, appDescr, ConsoleUserIO.class);
  }
 
}
