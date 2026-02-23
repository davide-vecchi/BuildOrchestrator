/**
 * Created by Davide on 2026-01-29 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dlog.log.Log;
import dparam.AParams;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static build_orchestrator.AppContext.newAppContext;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dfile.file.FileUtilities.newValidatedFile;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.date.DateTimeUtilities.waitMillis;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.map.MapUtilities.assertNonEmpty;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.TEN_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.properties.PropertiesUtilities.readProperties;
import static dutil.properties.PropertiesUtilities.toMap;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.dq;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


/**
 * The startup class of the {@code BuildOrchestrator} application. That is a Maven build orchestrator, to replace Batch /
 * Bash Maven build scripts.
 */
@SuppressWarnings("PublicConstructor")
public class BuildOrchestratorMain {
	
	
	/**
	 * The name of this program. Short name, no description (see #APP_DESCR).
	 */
	public static final String APP_NAME = "Build Orchestrator";
	
	/**
	 * The description of this program. Description, not a Short name (see #APP_NAME).
	 */
	public static final String APP_DESCR = APP_NAME + " - Build orchestrator for building Java projects with Maven.";
  
  /**
   * Each of these characters, if entered alone by the user as the answer to a question, means that the user is
   * requesting to terminate the program.
   */
  static final String CANCEL_CHARS = "Aa/";
	
	
	/**
	 * Entry point of the BuildOrchestrator program.
	 *
	 * @param args The command line args.
	 */
	public static void main(String[] args) throws Exception {
		
		final BuildOrchestrator orchestrator;
		
		try (
      
      final Log screenLog = new Log(APP_DESCR + " - screen log",    APP_NAME + "_screen-log.LOG", true);
      
      final Log userLog =   new Log(APP_DESCR + " - user log",      APP_NAME + "_user-log.LOG",   true);
      
      final Log devLog =    new Log(APP_DESCR + " - developer log", APP_NAME + "_dev-log.LOG",    true);
    
			final AppContext ac = newAppContext(
				ColorConsoleUserIO.newInstance1(System.in,         System.out,         System.err
                                            , CYAN,   BLACK,   RED
                                            , BLACK, YELLOW, BLACK)
																						           , screenLog,          userLog,            devLog))
		{
			assertTrue(ac.screenLog.logBare, "The Screen Log must have logBare true. It can be set here instead of asserting, but why is it not true already ?");
			
			try {
				
				writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
				
				showStartupMessages(ac);
				
				orchestrator = newBuildOrchestrator(args, ac);
				
				orchestrator.run();
			}
			catch (InterruptedException ie) { // Compliant; the interrupted state is restored
				
				ac.outDevLog( NLT + "Interrupted !!!!!" + NL + getFullDescriptionWithRootCause(ie));
				
				// Clean up whatever needs to be handled before interrupting :
				
				Thread.currentThread().interrupt();
			}
			catch (UserRequestedTermination t) {
				
				ac.warnUser(NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
			}
			catch (Exception e) {
				
				ac.errUser(NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage().trim() + NL2);
				
				ac.outUserLog(getFullDescriptionWithRootCause(e));
			}
			finally {
				
				ac.showLogInfo();
			}
		}
	}
	
	/**
   * @param args Array of 1 element, which is the path to the configuration file.
   *
   * @return A new {@link BuildOrchestrator} instance constructed from the given {@code args} and ready to {@link BuildOrchestrator#run()
	 *         run}.
	 *
	 * @throws UserRequestedTermination If the user requested to terminate the program, e.g. by answering so to a question.
   */
	static @NotNull BuildOrchestrator newBuildOrchestrator(@NotNull String[] args, @NotNull AppContext ac) throws UserRequestedTermination, InterruptedException {
		
		if (args.length != ONE_i) {
			
			throw new IllegalArgumentException("The program must be started with exactly 1 arguments (the path to the configuration file). Instead, the program has been started with the following " + args.length + " arguments:" + NL + Arrays.toString(args));
		}
		assertNonNull(ac);
		
		// Process first and only arg (the configuration file) :
		
		final File configurationFile = processArg_ConfigurationFile(args[ZERO_i].trim());
		
		// Setup all the configuration params :
		
		final BuildOrchestratorParams params = newBuildOrchestratorParams(readConfigurationMap(configurationFile
			                                                                       , ac)
																											 , "Configuration file "+  dq(
																							                 getCanonicalPath(configurationFile))
																									       , ac);
		
		// Initialize the instance of the BuildOrchestrator application, using the configuration params :
    
    final String buildListFilepath = assertNonBlankNorTrimmable(
                        params.getBuildListFilepath().value == null ?
                                  askBuildListFilepath("Build List file name not found in configuration "
                                                                  + getCanonicalPathAsDescr(configurationFile) + "."
                                                    , CANCEL_CHARS, ac)
                                : params.getBuildListFilepath().value);
    
		final File buildListFile = newValidatedFile(assertNonBlank(buildListFilepath)
																						 , true,       TEN_i);
		
		return BuildOrchestrator.newInstance(params, buildListFile);
	}
  
  /**
   * @param prePrompt Text to show right before the prompt that this method shows (which starts with "Enter the Build
   *                  List file name" etc.). It will not be re-shown if the entered value is invalid and causes the
   *                  prompt to be re-shown.<br>May be {@code null}.<br>
   *
   * @param cancelChars If the entered value is 1-char long and contained in this string, returns {@code null}.<br>Pass
   *                    an {@link StringUtils#EMPTY empty string} to prevent the user from canceling.
   *
   * @return The text entered by the user.
   *
   * @throws UserRequestedTermination If the user responds to the question with one of the {@code cancelChars}.
   */
  private static String askBuildListFilepath(String prePrompt, @NotNull String cancelChars, @NotNull AppContext ac) throws UserRequestedTermination, InterruptedException {
    
    ac.warnUser(NL + prePrompt);
    
    final String prompt = "Enter the Build List file name, with or without path (the current folder is " + dq(getCurrentFolder())
                          + "), or " + (cancelChars.length() == ONE_i ? cancelChars : "one of the " + dq(cancelChars) + " characters")
                          + " and then Enter to Abort : ";
    String result;
    
    boolean done;
    
    do {
      
      result = ac.userIO.in(prompt , EMPTY, cancelChars);
      
      if (result == null) {
        
        // : The user requested to abort :
        
        ac.warnUser(NL + "Terminating as requested by the user." + NL);
        
        throw new UserRequestedTermination();
      }
      done = new File(result).exists();
      
      if (! done) {
        
        ac.userIO.warnChars("The specified file does not exist." + NL);
        
        final InterruptedException ie = waitMillis(500);
        
        if (ie != null) {
          
          throw ie;
        }
      }
    }
    while (! done);
    
    return result;
  }
  
  
  /**
	 * @param configurationMap {@link AParams#configurationMap configurationMap}.<br>
	 *
	 * @param sourceDescr      {@link AParams#sourceDescr sourceDescr}.
	 *
	 * @return A new instance, with the fields for all the existing params instantiated, {@link AParams#populate()
	 *         populated}, {@link AParams#validate() validated} and fully usable.
	 */
	static BuildOrchestratorParams newBuildOrchestratorParams(@NotNull  Map<String, String> configurationMap
																													, @NotBlank String              sourceDescr
																													, @NotNull  AppContext          appContext) {
		
		final BuildOrchestratorParams allParams =  new BuildOrchestratorParams(
																													assertNonEmpty(configurationMap), sourceDescr
																																												, appContext);
		
		// Create all the existing params, as empty :
		
		allParams.addAllParams();
		
		// Populate and validate all the existing params :
		
		allParams.populate();
		
		allParams.validate();
		
		allParams.createValueChangeMarkers();
		
		return allParams;
	}
	
	/**
	 * @param argCfgFilePath The argument of the command line that represents the path to the configuration file.
	 *
	 * @return The {@link DFile file} instantiated from the given path string.
	 *
	 * @throws InvalidExternalValueException <ul><li>If {@code arg} does not {@link File#exists() exist} as a file path.</li>
	 *                                           <li>If {@code arg} is a file path that exists but {@link File#isFile() is}
	 *                                               not a file.</li>
	 *                                           <li>If {@code arg} exists as a non-empty file but is too short to be a
	 *                                               configuration file.</li></ul>
	 */
	private static File processArg_ConfigurationFile(String argCfgFilePath) {
		
		return newValidatedFile(argCfgFilePath, true, 3);
	}
	
	/**
	 * Returns a new {@link Map} populated by reading the configuration params from the given {@code configurationFile}.<br>
	 * It is guaranteed that each parameter appeared only once in the whole configuration file.<br>Assumes that {@link
	 * #initLogger(String)} and {@link #initUserIO()} have already been called, and uses their output objects to write
	 * status messages about this operation.
	 *
	 * @param configurationFile The existing file from which to read to fill the configuration map to return.
	 *
	 * @throws NonUniqueExternalValueException If the given configuration file contains duplicate keys.
	 */
	private static Map<String, String> readConfigurationMap(@NotNull File       configurationFile
																												, @NotNull AppContext appContext) {
		
		final String cfgFileCanonicalName = getCanonicalPath(configurationFile);
		
		appContext.outUser(NL + "Reading configuration file " + dq(cfgFileCanonicalName) + "...");
		
		final Map<String, String> configurationMap = toMap(readProperties(cfgFileCanonicalName));
		
		appContext.outUser("The following " + configurationMap.size() + " parameters were read from the configuration file :" + NL2 + configurationMap);
  
		return configurationMap;
	}
	
	/**
	 * Shows the startup messages.
	 *
	 * @throws UncheckedIOException If there is an I/O error.
	 */
	private static void showStartupMessages(@NotNull AppContext ac) {
		
		ac.outUser();
		ac.outUser("Avvio "    + dq(APP_DESCR) + " il " + new Date() + NL);
		
		ac.outUser("Screen log: " + getCanonicalPathAsDescr(ac.screenLog.logFile));
		
		ac.outUser("  User log: " + getCanonicalPathAsDescr(ac.userLog.logFile));
		
		ac.outUser("   Dev log: " + getCanonicalPathAsDescr(ac.devLog.logFile));
	}
	
}
