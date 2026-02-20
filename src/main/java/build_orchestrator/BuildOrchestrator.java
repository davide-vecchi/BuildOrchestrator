/**
 * Created by Davide on 2026-01-27 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dfile.file.FileUtilities;
import dlog.log.Log;
import dmaven.MavenArtifactInfo;
import dutil.exception.exceptions.ExternalValueException;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.system.OSUtilities;
import dutil.value_holder.ObjectAndDescr;
import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static build_orchestrator.BuildList.newBuildList;
import static dfile.file.FileUtilities.assertExistingFile;
import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.assertNonEmpty;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.checkIsExistingFile;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dfile.file.FileUtilities.isExistingFolder;
import static dmaven.MavenUtilities.calcMavenArtifactInfo;
import static dmaven.MavenUtilities.calcNonRunnableJarPath;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.exception.ExceptionUtilities.getShortDescriptionWithRootCause;
import static dutil.exception.ExceptionUtilities.getUnchecked;
import static dutil.list.text.TextListUtilities.assertNoneBlankNorTrimmable;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.DASH;
import static dutil.string.TextUtilities.DASH80;
import static dutil.string.TextUtilities.DQChar;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.SPACEChar;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.assertNonBlankNorTrimmable;
import static dutil.string.TextUtilities.dq;
import static dutil.string.TextUtilities.parseNotWithinDelimiters;
import static dutil.string.TextUtilities.removeEnd;
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
		
		this.appContext.outUserLog(NL2 + DASH80 + NL2 + "Execution journal:" + NL2 + this.journal + NL2 + DASH80);
		
		// @@@ q @@@@@@@@@@@@@@@
		
	}
	
	/**
	 * Loops over the entries in the {@link BuildList#getModuleBlocks() Modules section} of the {@link #buildList Build
	 * List}, and for each one executes its {@link BuildList.ModuleBlock#mvnCommand Maven command}.
	 */
	private void buildAndDeployModules() throws InterruptedException {
		
		for (final BuildList.ModuleBlock moduleBlock : this.buildList.getModuleBlocks()) {
			
			final File pomFolder = new File(moduleBlock.modulePath());
			
			this.appContext.outUser(NL + DASH80 + NL2 + "Building module in folder " + dq(getCanonicalPath(pomFolder) + " ..."));
			
			final List<String> args = parseNotWithinDelimiters(moduleBlock.mvnCommand(), SPACEChar
																											                                     , DQChar);
      final TwoObjects<String, String> mvnCmds = calcMvnCmd(args);
      
      final String mvnExecPath = mvnCmds.o2;
      
      final String mvnCmd =      mvnCmds.o1;
      
			final String mvnCmdWithPath = calcPath(assertNonBlank(mvnExecPath)
                                                       , assertNonBlank(mvnCmd));
      
      // Calculate the artifact's Maven info (id, group id etc.) :
      
      final String pomFilepath = assertExistingPath(calcPath(moduleBlock.modulePath(), "pom.xml")
                                                           , false);
      
      final MavenArtifactInfo mvnArtifactInfo = calcMavenArtifactInfo(pomFilepath, this.appContext.devLog);
      
      // Empty the folder in the local Maven repo where the build will create the jar (e.g.
      // ".m2\repository\DJavaLibraries\DTestNG\") :
      
      final ObjectAndDescr<File> mvnRepoArtifactFolder = calcMvnRepoArtifactFolder(mvnArtifactInfo.mvnGroupId()
                                                                                 , mvnArtifactInfo.mvnArtifactId()
                                                                 , true);
      if (mvnRepoArtifactFolder.description != null) {
      
        this.appContext.warnUser(NL + mvnRepoArtifactFolder.description);
      }
      emptyFolder(getCanonicalPath(mvnRepoArtifactFolder.object), NL + "Emptying artifact's Maven repo folder ");
      
      // Run the command to build :
      
			final OrchestratorCommandOutcome cmdResult = runOrchestratorCommand(
																															 pomFolder, mvnCmdWithPath
																										, args.subList(ONE_i, args.size()).toArray(new String[0]));
      if (cmdResult.exitCode == ZERO_i) {
				
				// : The build command succeeded.
				
				assertNull(cmdResult.exception);
        
        this.appContext.outUser(  NL + "Build successful.");
        
        this.appContext.outDevLog(NL + "The exit code of command :" + NL2T + mvnCmdWithPath + NL2 + "was " + cmdResult.exitCode + " .");
				
				if (moduleBlock.executableDestPath() != null) {
          
          // : The module has an executable artifact destination path specified, so move the artifact there) :
            
          final String msg = NL + "Deployment from " + dq(moduleBlock.modulePath()) + " : ";
          
          // Perform the deployment :
          
          final BuiltArtifactDeploymentResult deploymentResult = deployBuiltModule(moduleBlock, mvnArtifactInfo);
          
          if (deploymentResult.failure() == null) {
            
            // : The deployment succeeded.
            
            this.appContext.outUser(msg + "successful." + NL2 + "Deployment info :" + NL2T + deploymentResult);
          }
          else {
            
            // : The deployment failed.
            
            if (deploymentResult.failure().o2 != null) {
              
              throw getUnchecked(deploymentResult.failure().o2);
            }
            throw new UncheckedIOException(new IOException(msg + "failed :" + NL2T + deploymentResult.failure()));
          }
        }
        else {
          
          // : The module does not have an executable artifact destination path specified.
          
          this.appContext.outUser("No deployment attempted for the module because it does not have an executable artifact destination path specified in the Build List.");
        }
			}
			else {
				
        // : The build command failed.
        
				final String errMsg = "Build command " + dq(mvnCmdWithPath) + " failed: " + cmdResult.exception + " (exit code " + cmdResult.exitCode + ").";
				
				this.appContext.errUser(errMsg);
				
        if (cmdResult.exception != null) {
				
          throw getUnchecked(cmdResult.exception);
        }
        throw new ExternalValueException("Error " + cmdResult.exitCode + " returned from command :" + NL2T + cmdResult.commandDescr + NL2 + "executed from folder " + dq(moduleBlock.modulePath()) + ". Error code " + cmdResult.exitCode + " instead of " + ZERO_i + " .");
			}
		}
	}
  
  /**
   * Moves the artifact that was built for the given {@code moduleBlock} to the artifact destination folder specified in
   * the {@link #buildList} for that module.
   *
   * @param moduleBlock The {@link BuildList.ModuleBlock} specifying the build info for the given {@code moduleBlock}.<br>
   *
   * @param mvnArtifactInfo {@link MavenArtifactInfo Info} on the artifact to deploy (artifact id, group id etc.).
   *
   * @return A {@link BuiltArtifactDeploymentResult} describing whether and how the deployment succeeded or failed.
   */
  @NotNull BuiltArtifactDeploymentResult deployBuiltModule(@NotNull BuildList.ModuleBlock moduleBlock
                                                         , @NotNull MavenArtifactInfo     mvnArtifactInfo) {
    
    this.appContext.outUser(  NL + "Starting deployment to folder " + dq(getCanonicalPath(moduleBlock.executableDestPath())) + " ...");
    
    final BuiltArtifactDeploymentResult deploymentResult = moveBuiltArtifact(mvnArtifactInfo, moduleBlock);
    
    this.journal.addEntry(deploymentResult.journalEntry);
    
    if (deploymentResult.failure() == null) {
      
      // : The deployment succeeded.
      
      this.appContext.outUser(NL + "Deployment successful.");
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
   * @param mvnGroupId {@code <groupId>} of the artifact in the POM file.<br>
   *
   * @param mvnArtifactId {@code <artifactId>} of the artifact in the POM file.<br>
   *
   * @param createIfMissing If the path to return doesn't exist under the {@link BuildOrchestratorParams#mavenRepoFolder
   *                        local Maven repo} (which must exist):<ul><li>If this is {@code true}, the path will be created.</li><li>
   *                        If this is {@code false}, an exception will be thrown.</li></ul>
   *
   * @return In {@link ObjectAndDescr#object object} the path where the artifact must be built, under the {@link BuildOrchestratorParams#mavenRepoFolder
   *         local Maven repo}.<br>This returned path is guaranteed to exist and to be a folder.<br><br>
   *         In {@link ObjectAndDescr#description description} {@code null} if the {@link ObjectAndDescr#object path}
   *         already existed, otherwise textual info that the path has been created.
   *
   * @throws MissingExternalValueException <ul><li>If the {@link BuildOrchestratorParams#mavenRepoFolder local Maven
   *                                               repo} does not exist.</li>
   *
   *                                           <li>If the path to return does not exist and {@code createIfMissing} is {@code
   *                                               false}.</li></ul>
   *
   * @throws InvalidExternalValueException <ul><li>If the {@link BuildOrchestratorParams#mavenRepoFolder local Maven
   *                                               repo} path corresponds to a file instead of a folder.</li>
   *
   *                                           <li>If the path to return corresponds to a file instead of a folder.</li></ul>
   */
  private @NotNull ObjectAndDescr<@NotNull File> calcMvnRepoArtifactFolder(@NotBlank String  mvnGroupId
                                                                         , @NotBlank String  mvnArtifactId
                                                                                   , boolean createIfMissing) {
    final ObjectAndDescr<File> result = new ObjectAndDescr<>();
    
    final String mvnRepoFolder = assertExistingPath( this.params.mavenRepoFolder.value, true);
    
    final String mvnRepoArtifactFolder = calcPath(mvnRepoFolder
                                                            , assertNonBlankNorTrimmable(mvnGroupId)
                                                            , assertNonBlankNorTrimmable(mvnArtifactId));
    
    result.object = new File(mvnRepoArtifactFolder);
    
    if (result.object.exists()) {
      
      // : The path to return already exists.
      
      assertExistingFile(result.object, true);
    }
    else {
      
      // : The path to return does not exist. If requested, create it :
      
      if (! createIfMissing) {
      
        throw new MissingExternalValueException("The path" + NL2T + getCanonicalPath(result.object) + NL2 + " does not exist.");
      }
      try {
        
        FileUtils.forceMkdir(result.object);
        
        result.description = "Created non-existing path " + dq(getCanonicalPath(result.object));
      }
      catch (IOException e) {
  
        throw getUnchecked(e);
      }
    }
    return result;
  }
  
  /**
   *  TODO @@@@ FIX COMMENT
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
   * @return {@code null} if the deployment succeeds, otherwise in {@link TwoObjects#o1 o1} description of what failed
   *         and, if the failure was due to an exception, in {@link TwoObjects#o2 o2} that exception, otherwise {@code
   *         o2} is {@code null}.
   */
  private @NotNull BuiltArtifactDeploymentResult moveBuiltArtifact(@NotNull MavenArtifactInfo     mvnInfo
                                                                 , @NotNull BuildList.ModuleBlock moduleBlock) {
    
    final String mvnRepoFolder = assertExistingPath( this.params.mavenRepoFolder.value, true);
    
    assertNoneBlankNorTrimmable(mvnInfo.mvnGroupId(), mvnInfo.mvnArtifactId(), mvnInfo.mvnVersion(), mvnInfo.executableArtifactNameElement(), mvnInfo.executableArtifactName(), mvnRepoFolder);
    
    assertNonEmpty(mvnInfo.pomFile());
    
    final String deploymentInfoDescr = "Deployment info:" + NLT + mvnInfo + NLT + "Maven repo folder: " + dq(mvnRepoFolder) + ".";
    
    BuiltArtifactDeploymentResult result;
    
    final File nonRunnableJar = new File(calcNonRunnableJarPath(mvnRepoFolder, mvnInfo));
    
    final String builtArtifactExtension = FileUtilities.getExtension(nonRunnableJar.getName());
    
    final String runnableJarFilepath = removeEnd(getCanonicalPath(nonRunnableJar)
                                             , builtArtifactExtension)  // %MavenRepoFolder%\DAccessori\BuildOrchestrator\1.0-SNAPSHOT\BuildOrchestrator-1.0-SNAPSHOT
                                       + DASH + mvnInfo.executableArtifactName()  // -jar-with-dependencies
                                       + builtArtifactExtension;                  // .jar
    
    final File runnableJar = new File(runnableJarFilepath);
    
    final String jarCreationPath = getCanonicalPath(nonRunnableJar.getParentFile());
    
    try {
      
      // Delete the non-runnable jar that the build creates (if it exists) :
      // Corresponds to the following command from the old BAT build scripts :
      //
      // DEL %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT.jar
      
      if (isExistingFolder(jarCreationPath)) {
        
        // : The folder where the jar(s) had to be created exists.
        
        if (nonRunnableJar.exists()) {
          
          this.appContext.outUserLog("Deleting existing non-runnable jar " + getCanonicalPathAsDescr(nonRunnableJar) + " ...");
          
          FileUtils.delete(nonRunnableJar);
        }
        else {
          
          this.appContext.outUserLog("Not needed to delete non-existing non-runnable jar " + getCanonicalPathAsDescr(nonRunnableJar) + ".");
        }
        // Move the runnable jar that the build creates (it must exist) :
        // Corresponds to the following command from the old BAT build scripts :
        //
        //       MOVE %MavenRepoFolder%\IPSG\IPSG-Core\1.0-SNAPSHOT\IPSG-Core-1.0-SNAPSHOT-jar-with-dependencies.jar ^
        //            C:\IPSG\IPSG-Core.jar
        
        final String msg = checkIsExistingFile(runnableJar);
        
        if (msg == null) {
          
          // : The runnable jar to move exists. Move it, first deleting the old one if it's there :
          
          final File oldRunnableJar = new File(calcPath(moduleBlock.executableDestPath(),
                                                                              runnableJar.getName()));
          if (oldRunnableJar.exists()) {
          
            this.appContext.outUser(NL + "Overwriting old non-renamed runnable jar " + getCanonicalPathAsDescr(oldRunnableJar) + " .");
            
            FileUtils.delete(oldRunnableJar);
          }
          FileUtils.moveFileToDirectory(runnableJar, new File(moduleBlock.executableDestPath())
                                 , false);
          
          // Rename the moved runnable jar to its final name, first deleting the old one if it's there :
          
          final String destArtifactFileName = mvnInfo.mvnArtifactId() + EXTENSION_SEPARATOR
                                                                      + getExtension(runnableJar.getName());
          
          final File destArtifactFile = new File(calcPath(moduleBlock.executableDestPath()
                                                                              , destArtifactFileName));
          if (destArtifactFile.exists()) {
            
            this.appContext.outUser(NL + "Overwriting old renamed runnable jar " + getCanonicalPathAsDescr(destArtifactFile) + " .");
            
            FileUtils.delete(destArtifactFile);
          }
          FileUtils.moveFile(oldRunnableJar, destArtifactFile); // : This is a renaming.
          
          result = new BuiltArtifactDeploymentResult(
                      new TwoObjects<>(getCanonicalPath(runnableJar)
                                                  , moduleBlock.executableDestPath())
                        , null
                    , Journal.Entry.newInstance(
                      "The built executable artifact" + NL  + dq(runnableJar.getName())
                                     + " has been moved to folder"   + NL  + dq(moduleBlock.executableDestPath())
                                     + " and renamed to " +                  dq(destArtifactFile.getName())
                                     + "."                           + NL2 +           deploymentInfoDescr
                    , runnableJar.getParentFile(), null));
          
          this.appContext.outUserLog(result.journalEntry.operationDescr);
        }
        else {
          
          // : The runnable jar to move doesn't exist.
          
          result = new BuiltArtifactDeploymentResult(null
                                                 , new TwoObjects<>("Cannot find the built executable artifact file :" + NLT + msg
                                                                           , null)
                                             , Journal.Entry.newInstance(
                                                "The artifact " + runnableJar.getName()
                                                               + " that should have been built has not been moved to folder " + dq(jarCreationPath)
                                                               + " because it was not found." + NL2 + deploymentInfoDescr
                                                , runnableJar.getParentFile(), null));
        }
      }
      else {
        
        // : The destination path doesn't exist.
        
        result = new BuiltArtifactDeploymentResult(null
                                               , new TwoObjects<>("The folder where the jar(s) had to be created does not exist :" + NLT + dq(jarCreationPath) + "."
                                                                         , null)
                                           , Journal.Entry.newInstance(
                                                            "The destination folder " + dq(jarCreationPath)
                                                                             + " where the built artifact should have been moved does not exist, so no file was moved."
                                                                             + NL2 + deploymentInfoDescr
                                                            , runnableJar.getParentFile(), null));
      }
    }
    catch (Exception e) {
      
      this.appContext.outUserLog(getFullDescriptionWithRootCause(e));
      
      result = new BuiltArtifactDeploymentResult(
                                           null
                                             , new TwoObjects<>(e.getClass().getSimpleName() + " : " + e.getLocalizedMessage(), e)
                                         , Journal.Entry.newInstance(
                                                          getShortDescriptionWithRootCause(e)
                                                                         + " (see logs for details) occurred during the requested deployment of artifact "
                                                                         + getCanonicalPathAsDescr(runnableJar) + "." + NL2 + deploymentInfoDescr
                                                          , runnableJar.getParentFile(), null));
    }
    return result;
  }
  
  /**
   * @param args List where the first element is the OS command (e.g. {@code mvn}) and the subsequent elements are the
   *             args to the OS command (e.g. {@code clean} {@code install} {@code -D skipTests}).
   *
   * @return In {@link TwoObjects#o1 o1} the complete Maven command to issue (including all its args), in the form
   *         required by the current OS.<br>
   *         In {@link TwoObjects#o2 o2} the path to the folder where the Maven executable is. This path is already
   *         included in {@link TwoObjects#o1 o1}.
   */
  private @NotNull TwoObjects<String, String> calcMvnCmd(@NotEmpty List<String> args) {
    
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
	 * {@link OSUtilities#runCommand(File, String, long, Log, String...) Runs} the given shell command as per the given
   * params.<br>
	 * When the command returns, {@link AppContext#outUser shows} an <i>OK</i> message if the command succeded, otherwise
	 * a <i>KO</i> {@link AppContext#errUser message} with the command's {@link Process#exitValue() error code}.<br><br>
	 *
	 * The params of this method are the same as the corresponding ones of {@link OSUtilities#runCommand(File, String, long, Log, String...)}.<br><br>
   *
   * When this method returns, no matter the command's outcome, a new {@link Journal.Entry} has been {@link Journal#addEntry
   * added} to the {@link #journal}).
	 *
	 * @return TODO @@@ FIX THIS COMMENT @@@ The OS process' exit code. Besides its {@link Process#exitValue() normal values}, the following custom
	 *         values can be returned by this method:<ul>
	 *           <li>-101 ({@link IOException})</li>
	 *           <li>-102 ({@link TimeoutException})</li></ul>
	 */
	@NotNull private OrchestratorCommandOutcome runOrchestratorCommand(          File       folder
                                                                   , @NotBlank String     command
                                                                             , String ... args) throws InterruptedException {
    final Journal.Entry resultJournalEntry;
    int                        resultExitValue;
    Exception                  resultException;
		
		this.appContext.outUser(NL + "Command: " + dq(command) + "; args: " + asList(args) + NL);
    
    final String commandDescr;
			
    // : Run the build command :
    
    long timeMs = System.currentTimeMillis();
    
    TwoObjects<@NotBlank String, Integer> cmdOutcome = null;
    
    try {
      
      cmdOutcome = OSUtilities.runCommand(folder,                                          command
                               ,this.params.commandTimeoutMs.value.longValue(), this.appContext.devLog
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
      
      this.appContext.outUser(NL  + "The command" + NL2T + assertNonNull(cmdOutcome).o1
                                    + NL2 + "executed successfully in " + timeMs + " ms from folder " + dq(getCanonicalPath(folder)) + ".");
    }
    else {
      
      this.appContext.errUser(NL  + "The command" + NL2T + assertNonBlank(command)
                                    + NL2 + "executed from folder " + dq(getCanonicalPath(folder))
                                    + NL  + "resulted in an error " + resultExitValue
                                    + (resultException != null ? " ("  + getShortDescriptionWithRootCause(resultException) + ")" : EMPTY)
                                    + " in " + timeMs + " ms.");
    }
    commandDescr = cmdOutcome != null ? cmdOutcome.o1 : command;
    
    resultJournalEntry = this.journal.addEntry(Journal.Entry.newInstance(commandDescr, folder
                                                                                  , L(timeMs)));
		
		return new OrchestratorCommandOutcome(commandDescr,assertNonNull(resultJournalEntry)
                              , resultExitValue,           resultException);
	}
  
  /**
   * @param path The path of the folder to empty.<br>
   *
   * @param msg  Text to {@link AppContext#outUser show} to the user before the path. May be {@link StringUtils#isEmpty
   *             empty}.
   */
  private void emptyFolder(@NotBlank String path, @NotNull String msg) {
    
    this.appContext.outUser(assertNonNull(msg) + dq(assertNonBlankNorTrimmable(path)) + " .");
    
    try {
      
      final File folder = new File(assertExistingPath(path, true));
      
      FileUtils.cleanDirectory(folder);
      
      if (! FileUtils.isEmptyDirectory(folder)) {
        
        throw new UncheckedIOException(new IOException("Could not empty folder: " + dq(path)));
      }
    }
    catch (IOException e) {
      
      throw getUnchecked(e);
    }
  }
  
  /**
   * Represents the outcome of {@link #runOrchestratorCommand running an Orchestrator command}.
   *
   * @param commandDescr
   * @param journalEntry
   * @param exitCode
   * @param exception
   */
  record OrchestratorCommandOutcome(@NotBlank String    commandDescr, @NotNull Journal.Entry journalEntry, int exitCode
                                            , Exception exception) {}
  
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
  record BuiltArtifactDeploymentResult(         TwoObjects<String, String>    jarFilepaths
                                              , TwoObjects<String, Exception> failure
                                     , @NotNull Journal.Entry                 journalEntry) {}

}
