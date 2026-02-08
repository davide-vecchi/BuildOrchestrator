/**
 * Created by Davide on 2026-01-28 .
 *
 * @formatter:off
 */

package build_orchestrator;

import dparam.AParams;
import dparam.ParamMono;
import dparam.pvdc.AValueChangeInfo;
import dparam.pvdc.change_loader.AValueChangeTextReader;
import dparam.pvdc.change_loader.EmptyValueChangeTextReader;
import dutil.string.value_parser.LongStringParser;
import dutil.string.value_parser.NeutralStringParser;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import static dfile.file.FileUtilities.SEPARATOR_CHAR;
import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.checkIsValidFileName;
import static dutil.date.DateTimeUtilities.MS_IN_HOUR;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.ZERO_l;
import static dutil.string.TextUtilities.isBlankOrTrimmable;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Class containing the fields that represent the user-controlled parameters (AKA "inputs") of a {@link
 * BuildOrchestrator} executable instance.<br><br>
 * The {@link AValueChangeInfo#marker Value Change Marker}s are of unspecified type because {@link BuildOrchestrator}
 * doesn't use the PVDC functionality (<b>PVDC</b> = <i>Parameter Value Dynamic Change</i>).
 */
@ToString(callSuper = true)
public class BuildOrchestratorParams extends AParams<Object> {
	
	
	@Serial
	private static final long serialVersionUID = -4623955895467891834L;
	
	/**
	 * Default for optional param {@link #commandTimeoutMs}, 1 h (3,600,000 ms).
	 */
	private static final long DEFAULT_COMMAND_TIMEOUT_MS = MS_IN_HOUR;
	
	/**
	 * Default for optional param {@link #artifactName}.
	 */
	private static final String DEFAULT_ARTIFACT_NAME = "jar-with-dependencies.jar";
	
	/**
	 * The possible names of the environment variable representing the Maven installation folder.
	 */
	private static final List<String> MAVEN_HOME_ENV_VAR_NAMES = asList("MAVEN_HOME", "M2_HOME");
	
	/**
	 * The {@link AppContext application context}. This is not a param.
	 */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
	@Getter
	protected final @NotNull AppContext appContext;
	
	/**
	 * Mandatory : The filesystem path to the <i>Build List file</i>.
	 */
	@Getter
	@NotNull ParamMono<Object, String> buildListFilePath;
	
	/**
	 * Optional : The filesystem path to the Maven installation folder (not the Maven repository folder).<br><br>
	 *
	 * Default  : The value of environment variable {@code MAVEN_HOME}.
	 */
	@Getter
	@NotNull ParamMono<Object, String> mavenFolder;
  
  /**
   * Optional : The filesystem path to the Maven repository folder (not the Maven installation folder).<br><br>
   *
   * Default  : [user home]\.m2\repository
   *            E.g. {@code C:\Users\Davide\.m2\repository}.
   */
  @Getter
  @NotNull ParamMono<Object, String> mavenRepoFolder;
	
	/**
	 * Optional: The timeout of issuing a build command.
	 */
	@Getter
	ParamMono<Object, Long> commandTimeoutMs;
	
	/**
	 * Optional: The name (without path) of the artifacts that get built. So that means all the modules are assumed to get
	 *           built into artifacts all having the same name.<br>
	 *           Typically this name is defined in the module's {@code pom.xml} under {@code <build>  <plugins>  <plugin>  <executions>  <execution> <configuration> <descriptorRefs>  <descriptorRef>}.<br><br>
	 *
	 * Default  : {@code jar-with-dependencies.jar}.
	 */
	@Getter
	ParamMono<Object, String> artifactName;
	
	
	/**
	 * Non-public constructor.
	 *
	 * @param configurationMap {@link #configurationMap}.
	 * @param sourceDescr      {@link #sourceDescr}.
	 */
	BuildOrchestratorParams(Map<String, String> configurationMap, String sourceDescr, @NotNull AppContext appContext) {
		
		super(configurationMap, null, sourceDescr, appContext.userLog);
		
		this.appContext = appContext;
	}
	
	
	/**
	 * Sets new instances of all the params into this {@link BuildOrchestratorParams}.
	 */
	public void addAllParams() {
		
		// Param BuildListFilePath :
		
		this.buildListFilePath = new ParamMono<>(this, "BuildListFile");
    
    // Param MavenFolder :
    
    this.mavenFolder = new ParamMono<>(this, "MavenFolder");
    
    // Param MavenRepoFolder :
    
    this.mavenRepoFolder = new ParamMono<>(this, "MavenRepoFolder");
		
		// Param CommandTimeoutMs :
		
		this.commandTimeoutMs  = new ParamMono<>(this, "CommandTimeoutMs");
		
		// Param ArtifactName :
		
		this.artifactName = new ParamMono<>(this, "ArtifactName");
	}
	
	@Override
	public void populate() {
		
		// Param BuildListFilePath :
		
		this.buildListFilePath.setValueParser(new NeutralStringParser()).loadMandatoryValue();
		
		// Param MavenFolder :
		
		final String mvnHomeEnvVarName = MAVEN_HOME_ENV_VAR_NAMES.stream()
	                                                  .filter(n -> isNotBlank(System.getenv(n)))
		                                                .findFirst().orElse("Maven");
		
		this.mavenFolder.setValueParser(new NeutralStringParser()).loadOptionalValue(
														defaultIfBlank(System.getenv(mvnHomeEnvVarName)
																										, SEPARATOR_CHAR + "Maven"));
    
    // Param MavenRepoFolder :
    
    this.mavenRepoFolder.setValueParser(new NeutralStringParser()).loadOptionalValue(calcPath(
                                defaultIfBlank(System.getProperty("user.home"), EMPTY)
                                          , ".m2", "repository"));
		// Param CommandTimeoutMs :
		
		this.commandTimeoutMs.setValueParser(new LongStringParser()).loadOptionalValue(
																																 L(DEFAULT_COMMAND_TIMEOUT_MS));
		// Param ArtifactName :
		
		this.artifactName.setValueParser(new NeutralStringParser()).loadOptionalValue(DEFAULT_ARTIFACT_NAME);
	}
	
	@Override
	public void validate() {
		
		// Param BuildListFilePath :
		
		validateExistingFilePathParam(this.buildListFilePath);
    
    // Param MavenFolder :
    
    validateExistingFolderPathParam(this.mavenFolder);
    
    // Param MavenRepoFolder :
    
    validateExistingFolderPathParam(this.mavenRepoFolder);
		
		// Param CommandTimeoutMs :
		
		final Long timeout = this.commandTimeoutMs.value;
		
		handleParamValidationResult(this.commandTimeoutMs.name, timeout
										 , timeout == null || timeout.longValue() >= ZERO_l
									, "A non-negative number of milliseconds", this.sourceDescr);
		
		// Param ArtifactName :
		
		final String invalid = checkIsValidFileName(this.artifactName.value);
		
		handleParamValidationResult(this.artifactName.name,       this.artifactName.value
										 , (! isBlankOrTrimmable(this.artifactName.value)) && invalid == null
									, "A non-empty, non-trimmable text that can be a file name", invalid
								, this.sourceDescr);
	}
	
	/**
	 * Returns a new {@link EmptyValueChangeTextReader} because {@link BuildOrchestrator} doesn't use the PVDC
	 * functionality (<b>PVDC</b> = <i>Parameter Value Dynamic Change</i>).
	 * 
	 * @see AParams#getDefaultValueChangeReader()
	 */
	@Override
	protected AValueChangeTextReader<Object, ?> getDefaultValueChangeReader() {
		
		return new EmptyValueChangeTextReader<>(this.log);
	}

}
