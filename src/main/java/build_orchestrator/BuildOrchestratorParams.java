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
import static dutil.date.DateTimeUtilities.MS_IN_HOUR;
import static dutil.number.NumberUtilities.L;
import static dutil.number.NumberUtilities.ZERO_l;
import static java.util.Arrays.asList;
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
	private static final long serialVersionUID = -4628955895467891834L;
	
	/**
	 * Default for optional param {@link #commandTimeoutMs}, 1 h (3,600,000 ms).
	 */
	private static final long DEFAULT_COMMAND_TIMEOUT_MS = MS_IN_HOUR;
	
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
	 * Optional : The filesystem path to the Maven installation folder.
	 * Default  : The value of environment variable MAVEN_HOME .
	 */
	@Getter
	@NotNull ParamMono<Object, String> mavenFolder;
	
	/**
	 * Optional: The timeout of issuing a build command.
	 */
	@Getter
	ParamMono<Object, Long> commandTimeoutMs;
	
	
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
		
		// Param CommandTimeoutMs :
		
		this.commandTimeoutMs  = new ParamMono<>(this, "CommandTimeoutMs");
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
		// Param CommandTimeoutMs :
		
		this.commandTimeoutMs.setValueParser(new LongStringParser()).loadOptionalValue(
																																 L(DEFAULT_COMMAND_TIMEOUT_MS));
	}
	
	@Override
	public void validate() {
		
		// Param BuildListFilePath :
		
		validateExistingFilePathParam(this.buildListFilePath);
		
		// Param MavenFolder :
		
		validateExistingFolderPathParam(this.mavenFolder);
		
		// Param CommandTimeoutMs :
		
		final Long timeout = this.commandTimeoutMs.value;
		
		handleParamValidationResult(this.commandTimeoutMs.name, timeout
										 , timeout == null || timeout.longValue() >= ZERO_l
									, "A non-negative number of milliseconds", this.sourceDescr);
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
