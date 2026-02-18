/**
 * Created by Davide on 2026-01-28 .
 * 
 * @formatter:off
 */

package build_orchestrator;

import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.string.TextUtilities;
import jakarta.validation.constraints.NotNull;

import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.removeEnd;
import static org.apache.commons.lang3.StringUtils.EMPTY;


/** TODO Class duplicated in several places. UNIFY INTO A GENERIC LIBRARY.<br><br>
 *
 * Class that represents the context in which an application runs, {@link #screenLog with} {@link #userLog its} {@link #devLog
 * logger}s and {@link #userIO user input / output}.<br><br>It can be used to share global objects that must be
 * accessible from anywhere in the application, like the loggers and the screen / keyboard user I/O.<br>
 * It is {@link AutoCloseable} so it should be used with {@code try-with-resources}, in which case it must be kept in
 * mind that its {@link #close()} method will also close its {@link #userIO user input / output}, {@link #screenLog},
 * {@link #userLog} and {@link #devLog}.<br>
 *
 * When a class has an instance of this class as a field, consider excluding the field from that class' {@link Object#toString()
 * toString()} and {@link Object#equals(Object) equals()}.
 */
public class AppContext implements AutoCloseable {
  
  
  /**
   * The object to use to communicate with the user. To be set by clients.
   */
  public @NotNull AUserInputOutput userIO;
  
  /**
   * The log containing only the text that is shown {@link #outUser(String) on the screen}. To be set by clients. Its {@link Log#logBare
   * logBare} will be always {@code true}.
   */
  public @NotNull Log screenLog;
  
  /**
   * The log containing info for the user, not only for the developer. To be set by clients.
   */
  public @NotNull Log userLog;
  
  /**
   * The log containing only info for the developer, not for the user. To be set by clients.
   */
  public @NotNull Log devLog;
  
  
  /**
   * Constructor.
   *
   * @param userIO    {@link #userIO}.<br>
   *
   * @param screenLog {@link #screenLog}. Its {@link Log#logBare logBare} will be set to {@code true}.<br>
   *
   * @param userLog   {@link #userLog}.<br>
   *
   * @param devLog    {@link #devLog}.
   */
  private AppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog, @NotNull Log userLog, @NotNull Log devLog) {
    
    this.userIO =    assertNonNull(userIO);
    
    this.screenLog = assertNonNull(screenLog);
    
    this.screenLog.logBare = true;
    
    this.userLog =   assertNonNull(userLog);
    
    this.devLog =    assertNonNull(devLog);
  }
  
  /**
   * Factory method.
   *
   * @param userIO    {@link #userIO}.<br>
   * @param screenLog {@link #screenLog}.<br>
   * @param userLog   {@link #userLog}.<br>
   * @param devLog    {@link #devLog}.
   *
   * @return A new {@link AppContext} instantiated with the given values.
   */
  public static AppContext newAppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog, @NotNull Log userLog
                                                                         , @NotNull Log devLog) {
    return new AppContext(userIO, screenLog, userLog, devLog);
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string}, followed by {@link TextUtilities#NL new
   * line}, to {@link #userIO}, {@link #screenLog}, {@link #userLog} and {@link #devLog}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String outUser(String string) {
    
    return outUser_Chars(string + NL);
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} one empty row to {@link #userIO}, {@link #screenLog}, {@link
     #userLog} and {@link #devLog}.
   *
   * @return The outputted string.
   */
  public String outUser() {
    
    return outUser(EMPTY);
  }
  
  /**
   * {@link AUserInputOutput#errChars(String) Outputs} the given {@code string} as error, to {@link #userIO}, {@link
     #screenLog}, {@link #userLog} and {@link #devLog}, followed by {@link TextUtilities#NL new line}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String errUser(String string) {
    
    return errUser_Chars(string + NL);
  }
  
  /**
   * Like {@link #outUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String outUser_Chars(String string) {
    
    final String outputted = this.userIO.outChars(string);
    
    outScreenLog(outputted);
    
    return outputted;
  }
  
  /**
   * Like {@link #errUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String errUser_Chars(String string) {
    
    final String outputted = this.userIO.errChars(string);
    
    outScreenLog(outputted);
    
    outUserLog(  outputted);
    
    return outputted;
  }
  
  /**
   * {@link AUserInputOutput#errChars(String) Outputs} the given {@code string} to {@link #userIO} (as a warning), {@link
     #screenLog}, {@link #userLog} and {@link #devLog}, followed by {@link TextUtilities#NL new line}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String warnUser(String string) {
    
    return warnUser_Chars(string + NL);
  }
  
  /**
   * Like {@link #warnUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String warnUser_Chars(String string) {
    
    final String outputted = this.userIO.warnChars(string);
    
    outScreenLog(outputted);
    
    outUserLog(  outputted);
    
    return       outputted;
  }
  
  /**
   * Outputs the given {@code string} to {@link #screenLog}, {@link #userLog} and {@link #devLog}.
   *
   * @param string The string to output. If it ends with a {@link TextUtilities#NL new line}, that will be stripped in
   *               the {@link #screenLog}.
   *
   * @return The given string.
   */
  public String outScreenLog(String string) {
    
    outUserLog(string);
    
    this.screenLog.log(removeEnd(string, NL));
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string} to {@link #userLog} and {@link #devLog}.
   *
   * @param string The string to output.
   *
   * @return The given string.
   */
  public String outUserLog(String string) {
    
    outDevLog(string);
    
    this.userLog.log(string);
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} one empty row to {@link #userLog} and {@link #devLog}.
   *
   * @return The outputted string.
   */
  public String outUserLog() {
    
    return outUserLog(EMPTY);
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string} to {@link #devLog}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String outDevLog(String string) {
    
    this.devLog.log(string);
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} one empty row to {@link #devLog}.
   *
   * @return The outputted string.
   */
  public String outDevLog() {
    
    return outDevLog(EMPTY);
  }
  
  /**
   * {@link AppContext#outUser() Outputs} to the user info on the location of the log files.
   */
  public void showLogInfo() {
    
    outUser();
    
    outUser("Il file di log di questa esecuzione per lo SCHERMO è :       " + getCanonicalPathAsDescr(this.screenLog.logFile) + ".");
    
    outUser("Il file di log di questa esecuzione per l'UTENTE è :         " + getCanonicalPathAsDescr(this.userLog  .logFile) + ".");
    
    outUser("Il file di log di questa esecuzione per il PROGRAMMATORE è : " + getCanonicalPathAsDescr(this.devLog   .logFile) + ".");
  }
  
  /**
   * Closes {@link #userIO} and all the logs.
   *
   * @throws Exception
   */
  @Override
  public void close() throws Exception {
    
    if (this.userIO != null) {
      
      this.userIO.close();
    }
    if (this.screenLog != null) {
      
      this.screenLog.close();
    }
    if (this.userLog != null) {
      
      this.userLog.close();
    }
    if (this.devLog != null) {
      
      this.devLog.close();
    }
  }

}
