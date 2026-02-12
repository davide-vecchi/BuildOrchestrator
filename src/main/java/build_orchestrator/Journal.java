/**
 * Created by Davide on 2026-02-11 .
 *
 * @formatter:off
 */
package build_orchestrator;

import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static dfile.file.FileUtilities.assertExistingPath;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.assertNonBlank;

/**
 * Class to record the performed {@link BuildOrchestrator#execModulesBuild() builds}, each with its possible {@link BuildOrchestrator#deployBuiltModule
 * deployment}.<br>A new Journal instance is created for each {@link BuildOrchestrator#run() execution}.
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
  private List<@NotNull TwoObjects<@NotBlank String
             , @NotNull TwoObjects<@NotNull  Integer, Exception>>> issuedInitCommands;
  
  /**
   * The {@link JournalEntry entries} of this Journal.
   */
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
  public void addIssuedInitCommand(@NotBlank String                                  initCommand
                                 , @NotNull  TwoObjects<@NotNull Integer, Exception> cmdResult) {
    
    assertNonNull(cmdResult.o1, "Its o2 was :", cmdResult.o2);
    
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
     * The OS command that was issued to start the build. Typically {@code mvn clean install ...}.
     */
    final @NotBlank String buildCommand;
    
    /**
     * The folder from which the {@link #buildCommand} was executed.
     */
    final File sourceFolder;
    
    /**
     * Milliseconds taken by the {@link #buildCommand OS command} to either succeed or fail.<br>If {@code null} it means
     * that for any reason the {@link #buildCommand command} has not been executed.
     */
    final Long durationMs;
    
    /**
     * @param command {@link #buildCommand}.
     *
     * @return A new {@link JournalEntry} created {@link JournalEntry#buildCommand with} the given {@code command}. All its
     *         other fields will be {@code null}.
     */
    public static JournalEntry newInstance(@NotBlank String command) {
    
      return new JournalEntry(command, null, null);
    }
    
    /**
     * @param buildCommand {@link #buildCommand}.<br>
     * @param sourceFolder {@link #sourceFolder}.<br>
     * @param durationMs   {@link #durationMs}.<br>
     *
     * @return A new {@link JournalEntry} created with the given params.
     */
    public static JournalEntry newInstance(@NotBlank String command, @NotNull File sourceFolder, Long durationMs) {
      
      return new JournalEntry(command, sourceFolder, durationMs);
    }
    
    /**
     * Constructor.
     *
     * @param buildCommand {@link #buildCommand}.<br>
     * @param sourceFolder {@link #sourceFolder}.<br>
     * @param durationMs   {@link #durationMs}.<br>
     */
    private JournalEntry(@NotBlank String buildCommand, File sourceFolder, Long durationMs) {
      
      if (sourceFolder != null) {
      
        assertExistingPath(sourceFolder.getAbsolutePath(), true);
      }
      this.buildCommand = assertNonBlank(buildCommand);
      
      this.sourceFolder =                        sourceFolder;
      
      this.durationMs =                          durationMs;
      
      
      
      
    }


  }


}
