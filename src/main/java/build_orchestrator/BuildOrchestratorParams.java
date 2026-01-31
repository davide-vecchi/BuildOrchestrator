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
import dutil.string.value_parser.NeutralStringParser;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.util.Map;

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
	 * The {@link AppContext application context}. This is not a param.
	 */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
	@Getter
	protected final @NotNull AppContext appContext;
	
	/**
	 * Mandatory: The filesystem path to the <i>Build List file</i>.
	 */
	@Getter
	@NotNull ParamMono<Object, String> buildListFilePath;
	

	/**
	 * Non-public constructor.
	 *
	 * @param configurationMap {@link #configurationMap}.
	 *
	 * @param sourceDescr {@link #sourceDescr}.
	 */
	BuildOrchestratorParams(Map<String, String> configurationMap, String sourceDescr, @NotNull AppContext appContext) {
		
		super(configurationMap, null, sourceDescr, appContext.userLog);
		
		this.appContext = appContext;
	}
	
	@Override
	public void populate() {
		
		// Param buildListFilePath :
		
		this.buildListFilePath.setValueParser(new NeutralStringParser()).loadMandatoryValue();
	}
	
	@Override
	public void validate() {
		
		// Param buildListFilePath :
		
		validateExistingFilePathParam(this.buildListFilePath);
  }
	
	/**
	 * Returns an {@link EmptyValueChangeTextReader} because {@link BuildOrchestrator} doesn't use the PVDC functionality
	 * (<b>PVDC</b> = <i>Parameter Value Dynamic Change</i>).
	 * 
	 * @see AParams#getDefaultValueChangeReader()
	 */
	@Override
	protected AValueChangeTextReader<Object, ?> getDefaultValueChangeReader() {
		
		return new EmptyValueChangeTextReader<>(this.log);
	}

}
