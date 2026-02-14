/**
 * Created by Davide on 2026-02-11 .
 *
 * @formatter:off
 */
package build_orchestrator;

import build_orchestrator.BuildOrchestrator.OrchestratorCommandOutcome;
import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static dfile.file.FileUtilities.assertExistingPath;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.assertNonBlank;

/**
 * Class to record the performed operations (e.g. {@link BuildOrchestrator#execModulesBuild() builds}, {@link BuildOrchestrator#deployBuiltModule
 * deployments}.<br>A new Journal instance is created for each {@link BuildOrchestrator#run() execution}.
 */
public class Journal {
  
  
  /**
   * The {@link File} containing the {@link BuildList} containing the info from which the builds and possible
   * deployments represented by all the {@link #entries} of this Journal have been performed.
   */
  @Getter @Setter
  private File buildListFile;
  
  /**
   * Each entry represents an {@link BuildOrchestrator#issueInitCommands() issued Initialization Command} and its
   * outcome.
   * <ul><li>{@link TwoObjects#o1 o1} is the full text of the issued command.</li>
   *     <li>{@link TwoObjects#o2 o2} is a {@link TwoObjects} where:
   *     <ul><li>{@link TwoObjects#o1 o1} is the exit code of the OS process executed for the command (0 means success).</li>
   *         <li>{@link TwoObjects#o2 o2} is {@code null} if the command either succeeded or failed but not with an
   *                                      exception. Otherwise it's that exception.</li>
   * </ul>
   */
  @Getter
  private final List<@NotNull TwoObjects<@NotBlank String, @NotNull OrchestratorCommandOutcome>> issuedInitCommands;
  
  /**
   * The {@link JournalEntry entries} of this Journal.
   */
  @Getter
  private final @NotNull List<JournalEntry> entries;
  
  
  /**
   * Factory method (see {@link Journal#Journal} for params).
   */
  public static Journal newInstance() {
    
    return new Journal();
  }
  
  /**
   * Constructor.
   *
   * @param buildListFile {@link #buildListFile}.
   */
  private Journal() {
    
    this.issuedInitCommands = new ArrayList<>();
    
    this.entries = new ArrayList<>();
  }
  
  
  /**
   * Adds to this Journal's {@link #issuedInitCommands} list one entry representing an issued Initialization Command and
   * its outcome.
   *
   * @param initCommand
   * @param cmdResult
   */
  void addIssuedInitCommand(@NotBlank String initCommand, @NotNull OrchestratorCommandOutcome cmdResult) {
    
    this.issuedInitCommands.add(new TwoObjects<>(assertNonBlank(initCommand), cmdResult));
  }
  
  /**
   * Appends the given {@code entry} to this Journal's {@link #entries}.
   *
   * @param entry
   *
   * @return The given {@code entry}.
   */
  public JournalEntry addEntry(@NotNull JournalEntry entry) {
    
    this.entries.add(assertNonNull(entry));
    
    return entry;
  }
  
  /**
   * One entry of the {@link Journal}. It represents <i>one</i> {@link BuildOrchestrator#execModulesBuild() build} with
   * its possible {@link BuildOrchestrator#deployBuiltModule deployment}.
   */
  public static class JournalEntry {
    
    
    /**
     * Description of the operation this entry is about. E.g. the OS command that was issued to start a build.
     */
    final @NotBlank String operationDescr;
    
    /**
     * The folder from which the {@link #operationDescr} was executed.
     */
    final File sourceFolder;
    
    /**
     * Milliseconds taken by the {@link #operationDescr operation} to either succeed or fail.<br>If {@code null} it
     * means that for any reason the {@link #operationDescr operation} has not been performed.
     */
    final Long durationMs;
    
    /**
     * The time at which this {@link JournalEntry} has been created.
     */
    final @NotNull Instant createdAt;
    
    /**
     * @param operationDescr {@link #operationDescr}.
     *
     * @return A new {@link JournalEntry} created {@link JournalEntry#operationDescr with} the given {@code
     *         operationDescr}. All its other fields will be {@code null}.
     */
    public static JournalEntry newInstance(@NotBlank String operationDescr) {
    
      return new JournalEntry(operationDescr, null, null);
    }
    
    /**
     * @param buildCommand {@link #operationDescr}.<br>
     * @param sourceFolder {@link #sourceFolder}.<br>
     * @param durationMs   {@link #durationMs}.
     *
     * @return A new {@link JournalEntry} created with the given params.
     */
    public static JournalEntry newInstance(@NotBlank String operationDescr, @NotNull File sourceFolder, Long durationMs) {
      
      return new JournalEntry(operationDescr, sourceFolder, durationMs);
    }
    
    /**
     * Constructor.
     *
     * @param operationDescr {@link #operationDescr}.<br>
     * @param sourceFolder   {@link #sourceFolder}.<br>
     * @param durationMs     {@link #durationMs}.<br>
     */
    JournalEntry(@NotBlank String operationDescr, File sourceFolder, Long durationMs) {
      
      if (sourceFolder != null) {
      
        assertExistingPath(sourceFolder.getAbsolutePath(), true);
      }
      this.operationDescr = assertNonBlank(operationDescr);
      
      this.sourceFolder =                          sourceFolder;
      
      this.durationMs =                            durationMs;
      
      this.createdAt =                             Instant.now();
    }

  }
  
}
