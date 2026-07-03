/**
 * Created by Davide on 2026-01-28 .
 */
package build_orchestrator;

import application.AAppContext;
import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import jakarta.validation.constraints.NotNull;

import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.string.TextUtilities.NL;


// @formatter:off


/**
 * Implementation of {@link AAppContext} for {@link BuildOrchestrator}.
 */
public class AppContext extends AAppContext {
  
  
  /**
   * Private constructor. Sets {@link #currentVerbosity} to {@link AAppContext#MAX_VERBOSITY max}.
   *
   * @param userIO    {@link AAppContext#userIO userIO}.
   * @param screenLog {@link AAppContext#userIO screenLog}.
   * @param userLog   {@link AAppContext#userIO userLog}.
   * @param devLog    {@link AAppContext#userIO devLog}.
   */
  private AppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog
                                                     , @NotNull Log userLog, @NotNull Log devLog) {
    super(userIO, screenLog, userLog, devLog);
    
    this.currentVerbosity = MAX_VERBOSITY;
  }
  
  /**
   * Factory method. Sets {@link #currentVerbosity} to {@link AAppContext#MAX_VERBOSITY max}.
   *
   * @param userIO    {@link AAppContext#userIO userIO}.
   * @param screenLog {@link AAppContext#userIO screenLog}.
   * @param userLog   {@link AAppContext#userIO userLog}.
   * @param devLog    {@link AAppContext#userIO devLog}.
   */
  public static AppContext newAppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog
                                                                         , @NotNull Log userLog, @NotNull Log devLog) {
    return new AppContext(userIO, screenLog, userLog, devLog);
  }


  @Override
  public void showLogInfo(int verbosity) {
    
    outUser(verbosity, NL + "Il file di log di questa esecuzione per lo SCHERMO è :       "
                                                              + getCanonicalPathAsDescr(this.screenLog.logFile) + ".");
    
    outUser(verbosity,     "Il file di log di questa esecuzione per l'UTENTE è :         "
                                                              + getCanonicalPathAsDescr(this.userLog  .logFile) + ".");
    
    outUser(verbosity,     "Il file di log di questa esecuzione per il PROGRAMMATORE è : "
                                                              + getCanonicalPathAsDescr(this.devLog   .logFile) + ".");
  }

}
