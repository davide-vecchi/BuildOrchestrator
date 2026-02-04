/**
 * Created by Davide on 2026-01-27 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dutil.system.OSUtilities;
import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static build_orchestrator.BuildList.newBuildList;
import static build_orchestrator.BuildList.validateMavenCommand;
import static dfile.file.FileUtilities.assertNonEmpty;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.checkIsExistingFile;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.exception.ExceptionUtilities.getShortDescriptionWithRootCause;
import static dutil.exception.ExceptionUtilities.getUnchecked;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_I;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.DQChar;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.SPACEChar;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.dq;
import static dutil.string.TextUtilities.parseNotWithinDelimiters;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;


/**
 * Build orchestrator for building Java projects with Maven.<br>An instance of this class constitutes the {@link #run()
 * runnable} {@code BuildOrchestrator} app.
 */
@ToString
public final class BuildOrchestrator {
	
	
	/**
	 * The configuration parameters of the {@link BuildOrchestrator} application.
	 */
	@Getter
	private final @NotNull BuildOrchestratorParams params;
	
	/**
	 * The file containing the Build List.
	 */
	private final @NotNull File buildListFile;
	
	/**
	 * The {@link BuildList} resulting from parsing the {@link #buildListFile}. Will be {@code null} before it's
	 * calculated.
	 */
	@Getter
	private BuildList buildList;
	
	/**
	 * Whether to terminate after an {@link #issueInitCommands() Initialization Command} returned an error result.
	 * TODO @@@ MAKE THIS A {@link BuildOrchestratorParams param}.
	 */
	private final boolean breakOnInitCommandFailure = true;
	
	
	/**
	 * The {@link AppContext application context}.
	 */
	@Getter
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private final @NotNull AppContext appContext;
  
  
  /**
   * Factory method (see {@link BuildOrchestrator#BuildOrchestrator} for params).
   */
  public static BuildOrchestrator newInstance(@NotNull BuildOrchestratorParams params
                                            , @NotNull File                    buildListFile) {
    
    return new BuildOrchestrator(params, buildListFile);
  }
  
	/**
	 * Constructor.
	 *
	 * @param params     {@link #params}.<br>
	 * @param sourceFile {@link #buildListFile}.
	 */
	private BuildOrchestrator(@NotNull BuildOrchestratorParams params, @NotNull File buildListFile) {
		
		this.appContext =    assertNonNull(params.getAppContext());
		
		this.params =        assertNonNull(params);
		
		this.buildListFile = assertNonEmpty(buildListFile);
	}
	
	
	/**
	 * The method that starts the processing.
	 */
	public void run() throws IOException, InterruptedException, TimeoutException {
		
		this.buildList = newBuildList(this.buildListFile, this.appContext);
		
		// Issue the initialization commands (one by one, so they won't share shell state with each other;
		// this is the meaning of the note, found around in the code and in text files of this module,
		// warning that issuing initialization commands is not implemented yet; it actually kind of is,
		// but with this limitation) :
		
		issueInitCommands();
		
		// Loop over the entries in the Modules section of the Build List, and for each one execute its Maven command :
		
		execModulesBuild();
		
		
		
		// @@@ q @@@@@@@@@@@@@@@
		
	}
	
	/**
	 * Loops over the entries in the {@link BuildList#getModuleBlocks() Modules section} of the {@link #buildList Build
	 * List}, and for each one executes its {@link BuildList.ModuleBlock#mvnCommand Maven command}.
	 */
	private void execModulesBuild() throws InterruptedException {
		
		for (final BuildList.ModuleBlock moduleBlock : this.buildList.getModuleBlocks()) {
			
			final File folder = new File(moduleBlock.modulePath());
			
			this.appContext.outUser(NL + "Building module in folder " + dq(getCanonicalPath(folder) + " ..."));
			
			final List<String> args = parseNotWithinDelimiters(moduleBlock.mvnCommand(), SPACEChar
																											, DQChar);
			String mvnCmd = args.getFirst();
			
			validateMavenCommand(mvnCmd, null);
			
			final String mvnExecPath = calcPath(this.params.mavenFolder.value, "bin");
			
			if (SystemUtils.IS_OS_WINDOWS && isEmpty(getExtension(mvnCmd))) {
				
				// We are on Windows and in the Module declaration 'mvn' appears without extension, so try to add one :
				
				for (final String ext : asList("cmd", "exe")) {
					
					if (checkIsExistingFile(calcPath(mvnExecPath, mvnCmd + EXTENSION_SEPARATOR + ext)) ==
					                                                                                                          null) {
						// : The Maven executable exists with this extension.
						
						mvnCmd += EXTENSION_SEPARATOR + ext;
						
						break;
					}
				}
			}
			final String mvnCmdWithPath = calcPath(mvnExecPath, assertNonBlank(mvnCmd));
			// @@@@@@ q @
			
			final TwoObjects<@NotNull Integer, Exception> cmdResult = runOrchestratorCommand(
																															 folder, mvnCmdWithPath
																										, args.subList(ONE_i, args.size()).toArray(new String[0]));
			if (cmdResult.o1.intValue() != ZERO_i) {
				
				final String errMsg = "Build command " + dq(mvnCmdWithPath) + " failed: " + cmdResult.o2 + " (exit code " + cmdResult.o1.intValue() + ").";
				
				this.appContext.errUser(errMsg);
				
				throw getUnchecked(cmdResult.o2);
			}
			else {
				
				assertNull(cmdResult.o2);
			}
		}
	}
	
	/**
	 * {@link #runOrchestratorCommand Issues} the {@link BuildList#getInitCommands() Initialization Commands}.
	 */
	private void issueInitCommands() throws InterruptedException {
		
		this.appContext.outUser();
		
		TwoObjects<@NotNull Integer, Exception> cmdResult;
		
		for (final String initCommand : this.buildList.getInitCommands()) {
			
			cmdResult = runOrchestratorCommand(null, initCommand);
			
			if (cmdResult.o1.intValue() != ZERO_i) {
				
				final String errMsg = "Build command " + dq(initCommand) + " failed: " + cmdResult.o2 + " (exit code " + cmdResult.o1.intValue() + ").";
				
				this.appContext.errUser(errMsg);
				
				if (this.breakOnInitCommandFailure) {
					
					break;
				}
			}
			else {
				
				assertNull(cmdResult.o2);
			}
		}
	}
	
	/**
	 * {@link OSUtilities#runCommand( File, String, long, String...) Runs} the given shell command as per the given params.<br>
	 * When the command returns, {@link AppContext#outUser shows} an <i>OK</i> message if the command succeded, otherwise
	 * a <i>KO</i> {@link AppContext#errUser message} with the command's {@link Process#exitValue() error code}.<br><br>
	 *
	 * The params of this method are the same as the corresponding ones of {@link OSUtilities#runCommand( File, String, long, String...)}.
	 *
	 * @return The OS process' exit code. Besides its {@link Process#exitValue() normal values}, the following custom
	 *         values can be returned by this method:<ul><li>-101 {@link IOException}</li><li>-102 {@link TimeoutException}</li></ul>
	 */
	@NotNull TwoObjects<@NotNull Integer, Exception> runOrchestratorCommand(File folder, @NotBlank String command
																																				, String ... args) throws InterruptedException {
		
		final TwoObjects<@NotNull Integer, Exception> result = new TwoObjects<>();
		
		this.appContext.outUser_Chars("Command: " + command + " ... ");
		
		long timeMs = System.currentTimeMillis();
		
		try {

			result.o1 = I(OSUtilities.runCommand(folder, command, this.params.commandTimeoutMs.value.longValue()
															, args));
		}
		catch (IOException e) {
			
			result.o1 = I(-101);
			
			result.o2 = e;
		}
		catch (TimeoutException e) {
  
			result.o1 = I(-102);
			
			result.o2 = e;
		}
		finally{
			
			timeMs = System.currentTimeMillis() - timeMs;
		}
		this.appContext.outUser(NL2 + "The command:" + NL2T + command + NL);
		
		if (result.o1.equals(ZERO_I)) {
			
			this.appContext.outUser("executed successfully in " + timeMs + " ms.");
		}
		else {
			
			this.appContext.errUser(NLT + "resulted in an error " + result.o1 + " in " + timeMs + " ms ."
			                                    + (result.o2 != null ? " (" + getShortDescriptionWithRootCause(result.o2) + ")"
			                                                         : EMPTY));
		}
		
		return result;
	}
	
}
