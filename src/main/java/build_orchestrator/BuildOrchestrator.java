/**
 * Created by Davide on 2026-01-27 .
 *
 * @formatter:off
 */
package build_orchestrator;

import build_orchestrator.BuildList.ModuleBlock;
import build_orchestrator.Journal.JournalEntry;
import dfile.file.FileUtilities;
import dlog.log.Log;
import dmaven.DeploymentInfo;
import dutil.system.OSUtilities;
import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
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
import static dfile.file.FileUtilities.isExistingFolder;
import static dmaven.MavenUtilities.calcMavenDeploymentInfo;
import static dmaven.MavenUtilities.calcNonRunnableJarPath;
import static dutil.exception.ExceptionUtilities.getShortDescriptionWithRootCause;
import static dutil.exception.ExceptionUtilities.getUnchecked;
import static dutil.list.text.TextListUtilities.assertNoneBlankNorTrimmable;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.DASH;
import static dutil.string.TextUtilities.DQChar;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.SPACEChar;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.dq;
import static dutil.string.TextUtilities.parseNotWithinDelimiters;
import static dutil.string.TextUtilities.removeEnd;
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
  @Getter
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
   * The {@link Journal} for this {@link #run() execution}.
   */
  @Getter
  private Journal journal;
	
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
		
    this.journal = Journal.newInstance();
    
		this.buildList = newBuildList(this.buildListFile, this.appContext);
		
    this.journal.setBuildListFile(this.buildListFile);
    
		// Issue the initialization commands (one by one, so they won't share shell state with each other;
		// this is the meaning of the note, found around in the code and in text files of this module,
		// warning that issuing initialization commands is not implemented yet; it actually kind of is,
		// but with this limitation) :
		
		issueInitCommands();
		
		// Loop over the entries in the Modules section of the Build List, and for each one execute its Maven command :
		
		buildAndDeployModules();
		
		this.appContext.outUserLog(NL2 + "Execution journal:" + NL2 + this.journal);
		
		// @@@ q @@@@@@@@@@@@@@@
		
	}
	
	/**
	 * Loops over the entries in the {@link BuildList#getModuleBlocks() Modules section} of the {@link #buildList Build
	 * List}, and for each one executes its {@link ModuleBlock#mvnCommand Maven command}.
	 */
	private void buildAndDeployModules() throws InterruptedException {
		
		for (final ModuleBlock moduleBlock : this.buildList.getModuleBlocks()) {
			
			final File pomFolder = new File(moduleBlock.modulePath());
			
			this.appContext.outUser(NL2 + "Building module in folder " + dq(getCanonicalPath(pomFolder) + " ..."));
			
			final List<String> args = parseNotWithinDelimiters(moduleBlock.mvnCommand(), SPACEChar
																											                                     , DQChar);
      final TwoObjects<String, String> mvnCmds = calcMvnCmd(args);
      
      final String mvnExecPath = mvnCmds.o2;
      
      final String mvnCmd = mvnCmds.o1;
      
			final String mvnCmdWithPath = calcPath(assertNonBlank(mvnExecPath)
                                                       , assertNonBlank(mvnCmd));
			
			final OrchestratorCommandOutcome cmdResult = runOrchestratorCommand(
																															 pomFolder, mvnCmdWithPath
																										, args.subList(ONE_i, args.size()).toArray(new String[0]));
      if (cmdResult.exitCode == ZERO_i) {
				
				// : The build command succeeded.
				
				assertNull(cmdResult.exception);
        
        this.appContext.outUser(  NL + "Build successful.");
        
        this.appContext.outDevLog(NL + "The exit code of command :" + NL2T + mvnCmd + NL2 + "was " + cmdResult.exitCode + " .");
        
        // If the module has an artifact destination path specified, move the built artifact there :
				
				if (moduleBlock.artifactDestPath() != null) {
          
          final String msg = NL + "Deployment from " + dq(moduleBlock.modulePath()) + " : ";
          
          // Perform the deployment :
          
          final BuiltArtifactDeploymentResult deploymentResult = deployBuiltModule(moduleBlock);
          
          if (deploymentResult.failure() == null) {
            
            // : The deployment succeeded.
            
            this.appContext.outUser(msg + "successful. Deployment info :" + NL2T + deploymentResult);
          }
          else {
            
            // : The deployment failed.
            
            if (deploymentResult.failure().o2 != null) {
              
              throw getUnchecked(deploymentResult.failure().o2);
            }
            throw new UncheckedIOException(new IOException(msg + "failed :" + NL2T + deploymentResult.failure()));
          }
          // Update the last entry of the journal, which was created when running the Orchestrator Command, adding to it
          // the outcome of the deployment :
          
        }
			}
			else {
				
        // : The build command failed.
        
				final String errMsg = "Build command " + dq(mvnCmdWithPath) + " failed: " + cmdResult.exception + " (exit code " + cmdResult.exitCode + ").";
				
				this.appContext.errUser(errMsg);
				
				throw getUnchecked(cmdResult.exception);
			}
		}
	}
  
  /**
   * Moves the artifact that was built for the given {@code moduleBlock} to the artifact destination folder specified in
   * the {@link #buildList} for that module.
   *
   * @param moduleBlock The {@link ModuleBlock} specifying the deployment info for the given {@code moduleBlock}.
   *
   * @return A {@link BuiltArtifactDeploymentResult} describing whether and how the deployment succeeded or failed.
   */
  @NotNull BuiltArtifactDeploymentResult deployBuiltModule(@NotNull ModuleBlock moduleBlock) {
    
    final String mvnRepoFolder = assertExistingPath( this.params.mavenRepoFolder.value, true);
    
    this.appContext.outUser(  NL + "Starting deployment to folder " + dq(getCanonicalPath(moduleBlock.artifactDestPath())) + " ...");
    
    final String pomFilepath = assertExistingPath(calcPath(moduleBlock.modulePath(), "pom.xml")
                                    , false);
    
    final DeploymentInfo mvnInfoForDeployment = calcMavenDeploymentInfo(pomFilepath
                                                    , getCanonicalPathAsDescr(this.buildListFile)
                                                                      , moduleBlock.artifactDestPath()
                                                                      , this.appContext.devLog);
    
    final BuiltArtifactDeploymentResult deploymentResult = moveBuiltArtifact(mvnInfoForDeployment
                                                           , mvnRepoFolder, this.appContext.devLog);
    
    this.journal.addEntry(deploymentResult.journalEntry);
    
    if (deploymentResult.failure() == null) {
      
      // : The deployment succeeded.
      
      this.appContext.outUser(NL + "Deployment successful. The built artifact " + dq(mvnInfoForDeployment.builtArtifactNameElement())
                                         + " has been moved from folder "               + dq(deploymentResult.jarFilepaths().o1)
                                         + " to folder "                                + dq(deploymentResult.jarFilepaths().o2));
    }
    else {
      
      // : The deployment failed.
      
      final String errDescr = deploymentResult.failure().o1;
      
      final Exception exception = deploymentResult.failure().o2;
      
      this.appContext.errUser(NL + "Deployment FAILED. Reason :" + NLT + errDescr);
      
      if (exception != null) {
        
        throw getUnchecked(exception);
      }
    }
    return deploymentResult;
  }
  
  /**
   * <ul>
   *   <li>
   *     Deletes the non-runnable jar that the build creates (if it exists).<br><br>That corresponds to the following
   *     command from the old BAT build scripts :<br><br>
   *     {@code DEL %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT.jar}.<br>
   *   </li>
   *   <li>
   *     Move the runnable jar that the build creates (it must exist).<br><br>That corresponds to the following command
   *     from the old BAT build scripts :<br><br>
   *     {@code MOVE %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT-jar-with-dependencies.jar
   *                 C:\IPSG\IPSG-Core.jar}.
   *   </li>
   * </ul>
   *
   * @param mvnInfo
   * @param mavenRepoFolder
   * @param log
   *
   * @return {@code null} if the deployment succeeds, otherwise in {@link TwoObjects#o1 o1} description of what failed
   *         and, if the failure was due to an exception, in {@link TwoObjects#o2 o2} that exception, otherwise {@code
   *         o2} is {@code null}.
   */
  private static @NotNull BuiltArtifactDeploymentResult moveBuiltArtifact(@NotNull  DeploymentInfo mvnInfo
                                                                        , @NotBlank String         mavenRepoFolder
                                                                        , @NotNull  Log            log) {
    
    assertNoneBlankNorTrimmable(mvnInfo.mvnGroupId(), mvnInfo.mvnArtifactId(), mvnInfo.mvnVersion(), mvnInfo.builtArtifactNameElement(), mvnInfo.builtArtifactName(), mavenRepoFolder);
    
    assertNonEmpty(mvnInfo.pomFile());
    
    assertExistingPath(mavenRepoFolder, true);
    
    assertNonNull(log);
    
    final String deploymentInfoDescr = "Deployment info:" + NLT + mvnInfo + NLT + "Maven repo folder: " + dq(mavenRepoFolder) + ".";
    
    BuiltArtifactDeploymentResult result;
    
    final File nonRunnableJar = new File(calcNonRunnableJarPath(mavenRepoFolder, mvnInfo));
    
    final String builtArtifactExtension = FileUtilities.getExtension(nonRunnableJar.getName());
    
    final String runnableJarFilepath = removeEnd(getCanonicalPath(nonRunnableJar)
                                             , builtArtifactExtension)  // %MavenRepoFolder%\DAccessori\BuildOrchestrator\1.0-SNAPSHOT\BuildOrchestrator-1.0-SNAPSHOT
                                         + DASH + mvnInfo.builtArtifactName()     // -jar-with-dependencies
                                         + builtArtifactExtension;                // .jar
    
    final File runnableJar = new File(runnableJarFilepath);
    
    final String targetPath = getCanonicalPath(nonRunnableJar.getParentFile());
    
    try {
      
      // Delete the non-runnable jar that the build creates (if it exists) :
      // Corresponds to the following command from the old BAT build scripts :
      //
      // DEL %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT.jar
      
      if (isExistingFolder(targetPath)) {
        
        // : The folder where the jar(s) had to be created exists.
        
        if (nonRunnableJar.exists()) {
          
          log.log("Deleting existing non-runnable jar " + getCanonicalPathAsDescr(nonRunnableJar) + " ...");
          
          FileUtils.delete(nonRunnableJar);
        }
        else {
          
          log.log("Not needed to delete non-existing non-runnable jar " + getCanonicalPathAsDescr(nonRunnableJar) + ".");
        }
        // Move the runnable jar that the build creates (it must exist) :
        // Corresponds to the following command from the old BAT build scripts :
        //
        //       MOVE %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT-jar-with-dependencies.jar ^
        //            C:\IPSG\IPSG-Core.jar
        
        final String msg = checkIsExistingFile(runnableJar);
        
        if (msg == null) {
          
          // : The runnable jar to move exists. Move it :
          
          FileUtils.moveFileToDirectory(runnableJar, new File(targetPath)
                                 , false);
          
          result = new BuiltArtifactDeploymentResult(
                           new TwoObjects<>(getCanonicalPath(runnableJar), targetPath)
                             , null
                         , JournalEntry.newInstance(
                           "The built artifact" + NL + dq(runnableJar.getName())
                                          + " has been moved to folder" + NL + dq(targetPath)
                                          + "." + NL2 + deploymentInfoDescr
                           , runnableJar.getParentFile(), null));
        }
        else {
          
          // : The runnable jar to move doesn't exist.
          
          result = new BuiltArtifactDeploymentResult(null
                                                 , new TwoObjects<>("Cannot find the built executable artifact file :" + NLT + msg
                                                                           , null)
                                             , JournalEntry.newInstance(
                                                "The artifact " + runnableJar.getName()
                                                               + " that should have been built has not been moved to folder " + dq(targetPath)
                                                               + " because it was not found." + NL2 + deploymentInfoDescr
                                                , runnableJar.getParentFile(), null));
        }
      }
      else {
        
        // : The destination path doesn't exist.
        
        result = new BuiltArtifactDeploymentResult(null
                                               , new TwoObjects<>("The folder where the jar(s) had to be created does not exist :" + NLT + dq(targetPath) + "."
                                                                         , null)
                                           , JournalEntry.newInstance(
                                                            "The destination folder " + dq(targetPath)
                                                                             + " where the built artifact should have been moved does not exist, so no file was moved."
                                                                             + NL2 + deploymentInfoDescr
                                                            , runnableJar.getParentFile(), null));
      }
    }
    catch (Exception e) {
      
      log.log(ONE_i, e, ONE_i);
      
      result = new BuiltArtifactDeploymentResult(
                                           null
                                             , new TwoObjects<>(e.getClass().getSimpleName() + " : " + e.getLocalizedMessage(), e)
                                         , JournalEntry.newInstance(
                                                          getShortDescriptionWithRootCause(e)
                                                                         + " (see logs for details) occurred during the requested deployment of artifact "
                                                                         + getCanonicalPathAsDescr(runnableJar) + "." + NL2 + deploymentInfoDescr
                                                          , runnableJar.getParentFile(), null));
    }
    return result;
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
		
		for (final String initCommand : this.buildList.getInitCommands()) {
      
      final OrchestratorCommandOutcome cmdResult = runOrchestratorCommand(null, initCommand);
			
			if (cmdResult.exitCode != ZERO_i) {
				
				final String errMsg = "Initialization command " + dq(initCommand) + " failed: " + cmdResult.exception + " (exit code " + cmdResult.exitCode + ").";
				
				this.appContext.errUser(errMsg);
				
				if (this.breakOnInitCommandFailure) {
					
					break;
				}
			}
			else {
				
				assertNull(cmdResult.exception);
			}
      this.journal.addIssuedInitCommand(initCommand, cmdResult);
		}
	}
	
	/**
	 * {@link OSUtilities#runCommand( File, String, long, String...) Runs} the given shell command as per the given params.<br>
	 * When the command returns, {@link AppContext#outUser shows} an <i>OK</i> message if the command succeded, otherwise
	 * a <i>KO</i> {@link AppContext#errUser message} with the command's {@link Process#exitValue() error code}.<br><br>
	 *
	 * The params of this method are the same as the corresponding ones of {@link OSUtilities#runCommand( File, String, long, String...)}.<br><br>
   *
   * When this method returns, no matter the command's outcome, a new {@link JournalEntry} has been {@link Journal#addEntry
   * added} to the {@link #journal} (and it can be further updated if needed).
	 *
	 * @return TODO @@@ FIX THIS COMMENT @@@ The OS process' exit code. Besides its {@link Process#exitValue() normal values}, the following custom
	 *         values can be returned by this method:<ul>
	 *           <li>-101 ({@link IOException})</li>
	 *           <li>-102 ({@link TimeoutException})</li></ul>
	 */
	@NotNull private OrchestratorCommandOutcome runOrchestratorCommand(          File       folder
                                                                   , @NotBlank String     command
                                                                             , String ... args) throws InterruptedException {
    final JournalEntry resultJournalEntry;
    int                resultExitValue;
    Exception          resultException;
		
		this.appContext.outUser(NL + "Command: " + dq(command) + "; args: " + asList(args) + NL);
    
    String msg;
    
    if (this.dontBuild) {
			
			// : Don't run the build commands. This was set to true for example by a test.
      
      //noinspection ConstantValue
      msg = "Not executing build command" + NL2T + command + NL2 + "because the 'dontBuild' flag is " + this.dontBuild + " .";
      
			this.appContext.warnUser(NL + msg);
      
      resultExitValue = ZERO_i;
      
      resultException = null;
      
      resultJournalEntry = this.journal.addEntry(JournalEntry.newInstance(command));
		}
		else {
			
			// : Run the build command :
			
			long timeMs = System.currentTimeMillis();
      
      TwoObjects<@NotBlank String, Integer> cmdOutcome = null;
      
			try {
        
        cmdOutcome = OSUtilities.runCommand(folder, command, this.params.commandTimeoutMs.value.longValue()
                                 , args);
        
				resultExitValue = cmdOutcome.o2.intValue();
        
        resultException = null;
			}
			catch (IOException e) {
        
        resultExitValue = -101;
				
				resultException = e;
			}
			catch (TimeoutException e) {
        
        resultExitValue = -102;
        
        resultException = e;
			}
			finally {
				
				timeMs = System.currentTimeMillis() - timeMs;
			}
			if (resultExitValue == ZERO_i) {
    
				this.appContext.outUser(NL  + "The command"  + NL2T + assertNonNull(cmdOutcome).o1
                                      + NL2 + "executed successfully in " + timeMs + " ms from folder " + dq(getCanonicalPath(folder)) + ".");
			}
			else {
				
				this.appContext.errUser(NL  + "The command" + NL2T + assertNonBlank(command)
                                      + NL2 + "executed from folder " + dq(getCanonicalPath(folder))
				                              + NL  + "resulted in an error " + resultExitValue
                                            + " ("  + getShortDescriptionWithRootCause(resultException)
                                            + ") in " + timeMs + " ms.");
			}
      resultJournalEntry = this.journal.addEntry(JournalEntry.newInstance(command, folder
                                                                                   , L(timeMs)));
		}
		return new OrchestratorCommandOutcome(assertNonNull(resultJournalEntry), resultExitValue
                                                     , resultException);
	}
  
  /**
   * Represents the outcome of {@link #runOrchestratorCommand running an Orchestrator command}.
   *
   * @param journalEntry
   * @param exitCode
   * @param exception
   */
  record OrchestratorCommandOutcome(@NotNull JournalEntry journalEntry, int exitCode, Exception exception) {}
  
  /**
   * The result of the operation of deploying a module.
   *
   * @param jarFilepaths {@code null} if the deployment failed. Otherwise in {@link TwoObjects#o1 o1} the filepath where
   *                     the built jar was initially created and in {@link TwoObjects#o2 o2} the folder path where the
   *                     deployment operation moved that jar.<br>
   *
   * @param failure      {@code null} if the deployment succeeded. Otherwise in {@link TwoObjects#o1 o1} description of
   *                     what failed and, if the failure was due to an exception, in {@link TwoObjects#o2 o2} that
   *                     exception, otherwise {@code o2} is {@code null}.<br>
   *
   * @param journalEntry A new {@link JournalEntry} describing the result of the operation.
   */
  record BuiltArtifactDeploymentResult(            TwoObjects<String, String>    jarFilepaths
                                                 , TwoObjects<String, Exception> failure
                                        , @NotNull JournalEntry                  journalEntry) {}

}
