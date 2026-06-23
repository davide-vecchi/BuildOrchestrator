/**
 * Created by Davide on 2026-01-28 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.application.AAppContext;
import jakarta.validation.constraints.NotNull;

import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.string.TextUtilities.NL;


// @formatter:off


public class AppContext extends AAppContext {


  private AppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog
                                        , @NotNull Log userLog, @NotNull Log devLog) {

    super(userIO, screenLog, userLog, devLog);

    this.currentVerbosity = MAX_VERBOSITY;
  }


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
