/**
 * Created by Davide on 2026-01-27 .
 *
 * @formatter:off
 */
package build_orchestrator;

import build_orchestrator.BuildList.ModuleBlock;
import dmaven.BuiltArtifactMoveResult;
import dmaven.MavenInfoForBuild;
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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static build_orchestrator.BuildList.newBuildList;
import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.assertNonEmpty;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.checkIsExistingFile;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dmaven.MavenUtilities.calcMavenInfoForDeployment;
import static dmaven.MavenUtilities.moveBuiltArtifact;
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
	@SuppressWarnings("FieldMayBeStatic")
	private final boolean breakOnInitCommandFailure = true;
	
	/**
	 * If {@code true}, it does everything normally except it does not actually invoke the build commands, to make sure no
	 * jar is built, which is used by some tests that need to avoid creating the jars (e.g. not to overwrite existing ones).
	 */
	boolean dontBuild = false;
	
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
	public void run() throws InterruptedException {
		
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
			
			final File pomFolder = new File(moduleBlock.modulePath());
			
			this.appContext.outUser(NL2 + "Building module in folder " + dq(getCanonicalPath(pomFolder) + " ..."));
			
			final List<String> args = parseNotWithinDelimiters(moduleBlock.mvnCommand(), SPACEChar
																											, DQChar);
			
      final TwoObjects<String, String> mvnCmds = calcMvnCmd(args);
      
      final String mvnExecPath = mvnCmds.o2;
      
      final String mvnCmd = mvnCmds.o1;
      
			final String mvnCmdWithPath = calcPath(assertNonBlank(mvnExecPath)
                                                       , assertNonBlank(mvnCmd));
			
			final TwoObjects<@NotNull Integer, Exception> cmdResult = runOrchestratorCommand(
																															 pomFolder, mvnCmdWithPath
																										, args.subList(ONE_i, args.size()).toArray(new String[0]));
			if (cmdResult.o1.intValue() == ZERO_i) {
				
				// : The build command succeeded.
				
				assertNull(cmdResult.o2);
        
        this.appContext.outUser(  NL + "Build successful.");
        
        this.appContext.outDevLog(NL + "The return value of command :" + NL2T + mvnCmd + NL2 + "was " + cmdResult.o1 + " .");
        
        // If the module has an artifact destination path specified, move the built artifact there :
				
				if (moduleBlock.artifactDestPath() != null) {
          
          final String msg = NL + "Deployment from " + dq(moduleBlock.modulePath()) + " : ";
          
          final String deployErr = deployBuiltModule(moduleBlock);
          
          if (deployErr != null) {
            
            // : The deployment failed.
            
            throw new UncheckedIOException(new IOException(msg + "failed : " + deployErr));
          }
          this.appContext.outUser(msg + "successful.");
				}
			}
			else {
				
        // : The build command failed.
        
				final String errMsg = "Build command " + dq(mvnCmdWithPath) + " failed: " + cmdResult.o2 + " (exit code " + cmdResult.o1 + ").";
				
				this.appContext.errUser(errMsg);
				
				throw getUnchecked(cmdResult.o2);
			}
		}
	}
  
  /**
   * Moves the artifact that was built for the given {@code moduleBlock} to the artifact destination folder specified in
   * the {@link #buildList} for that module.
   *
   * @param moduleBlock The {@link ModuleBlock} specifying the deployment info for the given {@code moduleBlock}.
   *
   * @return {@code null} if the deployment succeeds, otherwise failure description.
   */
  String deployBuiltModule(@NotNull ModuleBlock moduleBlock) {
    
    final String error;
    
    final String mvnRepoFolder = assertExistingPath( this.params.mavenRepoFolder.value, true);
    
    this.appContext.outUser(  NL + "Starting deployment to folder " + dq(getCanonicalPath(moduleBlock.artifactDestPath())) + " ...");
    
    final String pomFilepath = assertExistingPath(calcPath(moduleBlock.modulePath(), "pom.xml")
                                    , false);
    
    final MavenInfoForBuild mvnInfoForDeployment = calcMavenInfoForDeployment(pomFilepath
                                                          , getCanonicalPathAsDescr(this.buildListFile)
                                                                            , moduleBlock.artifactDestPath()
                                                                            , this.appContext.devLog);
    
    final BuiltArtifactMoveResult moveResult = moveBuiltArtifact(mvnInfoForDeployment, mvnRepoFolder
                                                               , this.appContext.devLog);
    if (moveResult.failure() == null) {
      
      // : The deployment succeeded.
      
      error = null;
      
      this.appContext.outUser(NL + "Deployment successful. The built artifact " + dq(mvnInfoForDeployment.builtArtifactNameElement())
                                         + " has been moved from folder "               + dq(moveResult.jarFilepaths().o1)
                                         + " to folder "                                + dq(moveResult.jarFilepaths().o2));
    }
    else {
      
      // : The deployment failed.
      
      final String errDescr = moveResult.failure().o1;
      
      final Exception exception = moveResult.failure().o2;
      
      this.appContext.errUser(NL + "Deployment FAILED. Reason :" + NLT + errDescr);
      
      if (exception != null) {
        
        throw getUnchecked(exception);
      }
      error = errDescr;
    }
    return error;
  }
  
  /**
   *  TODO @@@ COMMENT
   * @param args
   * @return
   */
  private @NotNull TwoObjects<String, String> calcMvnCmd(List<String> args) {
    
    String mvnCmd = args.getFirst();
    
    final String mvnExecPath = assertExistingPath(calcPath(this.params.mavenFolder.value, "bin")
                                                         , true);
    
    if (SystemUtils.IS_OS_WINDOWS && isEmpty(getExtension(mvnCmd))) {
      
      // We are on Windows and in the Module declaration 'mvn' appears without extension, so try to add one :
      
      for (final String ext : asList("cmd", "exe")) {
        
        if (checkIsExistingFile(calcPath(mvnExecPath, mvnCmd + EXTENSION_SEPARATOR + ext)) == null) {
          
          // : The Maven executable exists with this extension.
          
          mvnCmd += EXTENSION_SEPARATOR + ext;
          
          break;
        }
      }
    }
    return new TwoObjects<>(mvnCmd, mvnExecPath);
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
	 *         values can be returned by this method:<ul>
	 *           <li>-101 ({@link IOException})</li>
	 *           <li>-102 ({@link TimeoutException})</li>
	 *         </ul>
	 */
	@NotNull private TwoObjects<@NotNull Integer, Exception> runOrchestratorCommand(          File       folder
																																								, @NotBlank String     command
																																													, String ... args) throws InterruptedException {
		final TwoObjects<@NotNull Integer, Exception> result = new TwoObjects<>();
		
		this.appContext.outUser(NL + "Command: " + dq(command) + "; args: " + asList(args) + NL);
		
		if (this.dontBuild) {
			
			// : Don't run the build commands. This was set to true for example by a test.
			
			//noinspection ConstantValue
			this.appContext.warnUser(NL + "Not executing build command" + NL2T + command + NL2 + "because the 'dontBuild' flag is " + this.dontBuild + " .");
			
			result.o1 = ZERO_I;
		}
		else {
			
			// : Run the build command :
			
			long timeMs = System.currentTimeMillis();
      
      TwoObjects<@NotBlank String, Integer> cmdOutcome = null;
      
			try {
        
        cmdOutcome = OSUtilities.runCommand(
                                folder, command, this.params.commandTimeoutMs.value.longValue(), args);
        
				result.o1 = cmdOutcome.o2;
			}
			catch (IOException e) {
				
				result.o1 = I(-101);
				
				result.o2 = e;
			}
			catch (TimeoutException e) {
	  
				result.o1 = I(-102);
				
				result.o2 = e;
			}
			finally {
				
				timeMs = System.currentTimeMillis() - timeMs;
			}
			if (result.o1.equals(ZERO_I)) {
    
				this.appContext.outUser(NL  + "The command"  + NL2T + assertNonNull(cmdOutcome).o1
                                      + NL2 + "executed successfully in " + timeMs + " ms from folder " + dq(getCanonicalPath(folder)) + ".");
			}
			else {
				
				this.appContext.errUser(NL  + "The command" + NL2T + assertNonNull(cmdOutcome).o1
                                      + NL2 + "executed from folder " + dq(getCanonicalPath(folder))
				                              + NL  + "resulted in an error " + result.o1 + (result.o2 != null ? " ("  + getShortDescriptionWithRootCause(result.o2) + ")"
				                                                                                               : " .") + " in " + timeMs + " ms.");
			}
		}
		assertNonNull(result.o1);
		
		return result;
	}
  
}
