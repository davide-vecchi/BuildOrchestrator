/**
 * Created by Davide on 2026-01-29 .
 */
package build_orchestrator;

import dlog.log.Log;
import dparam.AParams;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import dutil.io.ConditionallyCloseablePrintStream;
import dutil.jar.JARUtilities;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static build_orchestrator.AppContext.newAppContext;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dfile.file.FileUtilities.newValidatedFile;
import static dlog.log.Log.writeLogsHeaders;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.date.DateTimeUtilities.waitMillis;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.jar.JARUtilities.calcJARPath;
import static dutil.map.MapUtilities.assertNonEmpty;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.TEN_i;
import static dutil.number.NumberUtilities.TWO_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.properties.PropertiesUtilities.readProperties;
import static dutil.properties.PropertiesUtilities.toMap;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.dq;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


// @formatter:off


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
  static final String CANCEL_CHARS = "Cc/";
	
	
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
      
      final ConditionallyCloseablePrintStream out = new ConditionallyCloseablePrintStream(
                                                              System.out, true, CHARSET_UTF_8, false);
      
      final ConditionallyCloseablePrintStream err = new ConditionallyCloseablePrintStream(
                                                 System.err, true, CHARSET_UTF_8, false);
      
      final AppContext ac = newAppContext(
                         ColorConsoleUserIO.newInstance1(System.in,         out,                 err
                                                             , CYAN,   BLACK,   RED
                                                             , BLACK, YELLOW, BLACK)
                                                                        , screenLog,          userLog,            devLog))
		{
			assertTrue(ac.screenLog.logBare, "The Screen Log must have logBare true. It can be set here instead of asserting, but why is it not true already ?");
			
			try {
				
				writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
				
				showStartupMessages(ac);
        
        if (isNotEmpty(args) && args.length > ONE_i) {
          
          throw new IllegalArgumentException("The program must be started with either 0 or 1 arguments (if started with 1 argument, that argument must be the path to the Build List to use). Instead, the program has been started with " + args.length + " arguments, which are the following:" + NL + Arrays.toString(args));
        }
        orchestrator = newBuildOrchestrator(isNotEmpty(args) ? args[ZERO_i] : null
                                       , null, ac);
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
   * @param buildListPath The path to the Build List to process, if that was given to the program, e.g. as a startup arg.<br>
   *                      Otherwise {@code null}, meaning that the {@link BuildOrchestratorParams#buildListFilepath
   *                      BuildListFile parameter} specifying the Build list to process is expected to be in the
   *                      configuration file.<br>
   *
   * @param testCfgFileSuffix {@code null} if the instance to return is not meant to be used from tests.<br>Otherwise,
   *                          suffix to append to the regular name of the configuration file to use, just before the
   *                          extension.<br>E.g., if the regular name of the configuration file is "{@code
   *                          BuildOrchestrator-Config.TXT}" and this param is given as "{@code _TestBuildList01}", the
   *                          configuration file name will be "{@code BuildOrchestrator-Config_TestBuildList01.TXT}".
   *
   * @return A new {@link BuildOrchestrator} instance constructed from the given {@code args} and ready to {@link BuildOrchestrator#run()
	 *         run}.
	 *
	 * @throws UserRequestedTermination If the user requested to terminate the program, e.g. by answering so to a question.
   */
	static @NotNull BuildOrchestrator newBuildOrchestrator(String buildListPath, String testCfgFileSuffix, @NotNull AppContext ac) throws UserRequestedTermination, InterruptedException {
		
		assertNonNull(ac);
		
		// Determine the configuration file :
		
		final File configurationFile = calcConfigurationFile("-Config" + defaultString(testCfgFileSuffix)
                                                                          + EXTENSION_SEPARATOR + "TXT"
                                                , asList("src"
                                                                             , testCfgFileSuffix != null ? "test"
                                                                                                         : "main"
                                                                             , "resources")
                                                                  , ac);
    
		// Setup all the configuration params from the configuration file :
		
		final BuildOrchestratorParams params = newBuildOrchestratorParams(readConfigurationMap(configurationFile
			                                                                       , ac)
																											 , "Configuration file "+  dq(
																							                 getCanonicalPath(configurationFile))
																									       , ac);
		
		// Initialize the instance of the BuildOrchestrator application :
    
    final String buildListFilepath = assertNonBlankNorTrimmable(
      
      buildListPath                       != null ? buildListPath :                       // : The Build List was given as arg.
      
              params.getBuildListFilepath().value != null ? params.getBuildListFilepath().value : // : The Build List was specified in the configuration file.
              
              askBuildListFilepath("Build List file name not specified, enter it :", CANCEL_CHARS, ac));
    
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
                          + "), or " + calcCancelCharsPrompt(cancelChars);
    String result;
    
    boolean done;
    
    do {
      
      result = ac.userIO.in(prompt , EMPTY, cancelChars);
      
      if (result == null) {
        
        // : The user requested to abort :
        
        ac.warnUser(NL + "Terminating as requested by the user.");
        
        throw new UserRequestedTermination();
      }
      done = new File(result).exists();
      
      if (! done) {
        
        ac.userIO.warnChars("The specified file does not exist.");
        
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
   * @param filenameSuffix The text to append to the filepath of the {@link JARUtilities#calcJARPath(Class) running JAR}
   *                       (without its extension) to obtain the filepath of the configuration file.<br>E.g. if the
   *                       running JAR is<br><br>{@code C:\The\filepath\of\theJAR.jar}<br><br>and this param is "{@code
   *                       -Config.TXT}" then the calculated filepath of the configuration file will be<br><br>{@code
   *                       C:\The\filepath\of\theJAR-Config.TXT}.<br>
   *
   * @param resourcesFolderFromIDE Each element is a single component of the path to the {@code resources} folder from
   *                               which the resources (e.g. configuration file, {@link BuildList} files etc.) must be
   *                               read <b>when running from the IDE</b>. Will be ignored if not running from the IDE.
   *                               May be {@code null}, but if running from the IDE this will throw {@link
   *                               MissingValueException}.<br><br>Typical contents are:<ul>
   *                                 <li>{@code "src"}, {@code "main"}, {@code "resources"}</li>for when running as Java
   *                                     application from the IDE.<br><br>
   *                                 <li>{@code "src"}, {@code "test"}, {@code "resources"}</li>for when running as unit
   *                                     test from the IDE.</li></ul>
   *
   * @return The existing {@link File} corresponding to the calculated filepath.
   *
   * @throws InvalidExternalValueException <ul>
   *                                       <li>If the calculated filepath does not {@link File#exists() exist}.</li>
   *                                       <li>If the calculated filepath exists but {@link File#isFile() is} not a file.</li>
   *                                       <li>If the calculated filepath exists as a non-empty file but is too small to
   *                                           be a configuration file.</li></ul>
   *
   * @throws MissingValueException If running from the IDE and {@code resourcesFolderFromIDE} is {@code null}.
   */
	private static File calcConfigurationFile(         String     filenameSuffix, List<String> resourcesFolderFromIDE
                                          , @NotNull AppContext ac) {
    String jarPath, cfgFilepath;
    
    try {
      
      jarPath = calcJARPath(BuildOrchestratorMain.class);
      
      // Hack for when the program is executed within the IDE :
      
      final String suffixFromIDE = File.separator + "target" + File.separator + "classes";
      
      if (Strings.CI.endsWith(jarPath, suffixFromIDE)) {
        
        // E.g. "C:\whatever\BuildOrchestrator\target\classes".
        
        // : The program is executed within the IDE. TODO @@@ ... or executed from a test during the build process.
        
        assertNonNull(resourcesFolderFromIDE, "It is detected that the program is being executed from within the IDE, so the parameter 'resourcesFolderFromIDE' must be given. Instead, it's null.");
        
        ac.warnUser(NL + "Detected that the program is being executed from within the IDE.");
        
        ac.outUserLog(NLT + "Original path of executable : " + dq(jarPath) + ".");
        
        final List<String> pathElements = new ArrayList<>(TWO_i + resourcesFolderFromIDE.size());
        
        pathElements.add(Strings.CI.removeEnd(jarPath, suffixFromIDE));
        
        pathElements.addAll(resourcesFolderFromIDE);
        
        pathElements.add(BuildOrchestrator.class.getSimpleName() + EXTENSION_SEPARATOR + "jar");
        
        jarPath = calcPath(pathElements.toArray(new String[ZERO_i]));
        
        ac.outUserLog(NL2T + "Adjusted path of executable : " + dq(jarPath) + ".");
      }
      else {
        
        ac.outUserLog("Detected that the program is being executed from a JAR file." + NL2T + "Path of executable : " + dq(jarPath) + ".");
      }
      cfgFilepath = removeExtension(jarPath) + filenameSuffix;
    }
    catch (Exception e) {
      
      final String msg = "Error " + e.getClass().getSimpleName() + " trying to determine the path of the running JAR to retrieve the configuration file.";
      
      ac.outUserLog(msg + NL2T + getFullDescriptionWithRootCause(e));
      
      cfgFilepath = calcPath(getCurrentFolder()
                                       , BuildOrchestrator.class.getSimpleName() + EXTENSION_SEPARATOR + filenameSuffix);
      
      ac.warnUser(msg + NL2 + "Falling back to configuration file location " + dq(cfgFilepath) + ".");
    }
		return newValidatedFile(cfgFilepath, true, 3);
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
		ac.outUser("Avvio " + dq(APP_DESCR) + " il " + new Date() + NL);
		
    ac.showLogInfo();
	}
	
}
