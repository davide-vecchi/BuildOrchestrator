/**
 * Created by Davide on 2026-01-27 .
 *
 * @formatter:off
 */
package build_orchestrator;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static build_orchestrator.BuildList.newBuildList;
import static dfile.file.FileUtilities.assertNonEmpty;
import static dutil.number.NumberUtilities.TEN_i;
import static dutil.object.ObjectUtilities.assertNonNull;


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
	public void run() {
		
		this.buildList = newBuildList(this.buildListFile, this.appContext);
		
		// @@@ q @@@@@@@@@@@@@@@
		
	}
	
	/**
	 *
	 * @param projectDir
	 * @param mvnArgs
	 *
	 * @return {@link Process#exitValue() exitValue}.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	private int runMavenCommand(File projectDir, String... mvnArgs) throws IOException, InterruptedException, TimeoutException {
		
		List<String> command = new ArrayList<>();
		command.add("mvn");
		command.addAll(Arrays.asList(mvnArgs));
		
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(projectDir);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		
		Process p = pb.start();
		
		// Wait with timeout
		if (! p.waitFor(TEN_i, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			throw new TimeoutException("Maven build timed out");
		}
		
		return p.exitValue();
	}
	
}
