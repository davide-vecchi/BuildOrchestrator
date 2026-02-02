/**
 * Created by Davide on 2026-01-29 .
 *
 * @formatter:off
 */
package build_orchestrator;


import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.NonUniqueExternalValueException;
import dutil.value_holder.TwoObjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dfile.file.FileUtilities.assertExistingPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.list.text.TextListUtilities.listToString;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.B;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.TAB;
import static dutil.string.TextUtilities.TAB2;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.surround;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Represents a Build List, described in {@code DOC/Build List syntax.TXT}.
 */
class BuildList {
  
  
  /**
   * The string which, if it is the first occurrence of non-space chars in a line, indicates that the whole line must be
   * ignored (it's a comment).<br>Can be any length, doesn't need to be one char.
   */
  private static final String COMMENT_STARTER = "#";
  
  /**
   * The name of the Initialization Section in the Build List file (case-insensitive).
   */
  private static final String INIT_SECTION_NAME = "Initialization";
  
  /**
   * The name of the Options Section in the Build List file (case-insensitive).
   */
  private static final String OPTIONS_SECTION_NAME = "Options";
  
  /**
   * The name of the Modules Section in the Build List file (case-insensitive).
   */
  private static final String MODULES_SECTION_NAME = "Modules";
  
  /**
   * The string that starts any Section name.
   */
  private static final String SECTION_NAME_START = "[";
  
  /**
   * The string that ends any Section name.
   */
  private static final String SECTION_NAME_END = "]";
  
  /**
   * If the Build List file contains this line (case-insensitive) in the "Options" Section, {@link #doPause} will be {@link Boolean#FALSE
   * FALSE}, otherwise it will be {@link Boolean#TRUE TRUE}.
   */
  private static final String NO_PAUSE = "NoPause";
  
  /**
   * If the Build List file contains this line (case-insensitive) in the "Options" Section, {@link #doTests} will be {@link Boolean#FALSE
   * FALSE}, otherwise it will be {@link Boolean#TRUE TRUE}.
   */
  private static final String NO_TESTS = "NoTests";
  
  /**
   * The lines in the <i>Initialization</i> Section. {@code null} means that that Section has not been encountered yet.
   * If that Section exists and is empty, this list will be empty.
   */
  @Getter
  private List<String> initCommands;
  
  /**
   * Represents the Build List line (found in the <i>Options</i> Section) that determines whether after the build of
   * each module there will be a pause waiting for a key to be pressed. {@code null} means that that Section has not
   * been encountered yet. If the Section doesn't contain this value, it means
   */
  @Getter
  private Boolean doPause;
  
  /**
   * Represents the Build List line (found in the <i>Options</i> Section) that determines whether Maven will run the
   * tests for each module. {@code null} means that that line has not been encountered yet.
   */
  @Getter
  private Boolean doTests;
  
  /**
   * Represents the Build list Section <i>Modules</i>. The elements of this list will be the {@link ModuleBlock}s
   * containing the info on each module to build. {@code null} means that that Section has not been encountered yet.
   */
  @Getter
  private List<ModuleBlock> moduleBlocks;
  
  
  /**
   * Private constructor.
   */
  private BuildList() {}
  
  
  /**
   * @param buildListFile The file containing the Build List.
   *
   * @return A new Build List (described in {@code DOC/Build List syntax.TXT}) populated by reading the given {@code
   *         buildListFile}.
   */
  static @NotNull BuildList newBuildList(@NotNull File buildListFile, @NotNull AppContext ac) {
    
    ac.outUser(NL2 + Instant.now().toString() + TAB + "Starting   creation of Build List from " + getCanonicalPathAsDescr(buildListFile) + " ..." + NL);
    
    final BuildList result = new BuildList();
    
    // Read the file content with line number tracking :
    
    final List<LineWithNumber> allLinesWithNumbers = readAllLinesWithNumbers(buildListFile);
    
    // Phase 1: Split file into sections with line number tracking :
    
    final TwoObjects<Map<String, List<LineWithNumber>>
                   , List<LineWithNumber>> lines = extractSectionLines(buildListFile, allLinesWithNumbers);
    
    final Map<String, List<LineWithNumber>> sectionLines =   lines.o1;
    
    final             List<LineWithNumber>  noSectionLines = lines.o2;
    
    // : Phase 1 complete - file split into sections :
    
    final String initSectionKey =    INIT_SECTION_NAME   .toUpperCase();
    
    final String optionsSectionKey = OPTIONS_SECTION_NAME.toUpperCase();
    
    final String modulesSectionKey = MODULES_SECTION_NAME.toUpperCase();
    
    // Validate we have all required sections :
    
    validateSections(buildListFile, sectionLines, initSectionKey, optionsSectionKey, modulesSectionKey);
    
    // Validate no content outside sections (except comments which were already filtered) :
    
    validateOutsideSections(buildListFile, noSectionLines);
    
    // Phase 2: Parse each section :
    
    // Parse Initialization section :
    
    result.initCommands = extractInitCommandsSection(sectionLines, initSectionKey);
    
    // Parse Options section :
    
    final TwoObjects<Boolean, Boolean> options = extractOptionsSection(buildListFile, sectionLines, optionsSectionKey);
    
    result.doPause = assertNonNull(options.o1);
    
    result.doTests = assertNonNull(options.o2);
    
    // Parse Modules section :
    
    result.moduleBlocks = extractModulesSection(buildListFile, sectionLines, modulesSectionKey, ac);
    
    // Validate at least one module exists :
    
    validateModulesPresent(buildListFile, result.moduleBlocks);
    
    // : Phase 2 complete - all sections parsed and validated.
    
    ac.outUser(NL + Instant.now().toString() + TAB + "Terminated creation of Build List from " + getCanonicalPathAsDescr(buildListFile) + ".");
    ac.outUser(NL + "The Build List is:" + NL2 + result);
    
    return result;
  }
  
  
  /**
   * Validate at least one module exists.
   *
   * @param buildListFile
   * @param moduleBlocks
   */
  private static void validateModulesPresent(@NotNull File buildListFile, @NotNull List<ModuleBlock> moduleBlocks) {
    
    if (moduleBlocks.isEmpty()) {
      
      throw new MissingExternalValueException("No modules defined in [" + MODULES_SECTION_NAME + "] section of file "
                                              + getCanonicalPathAsDescr(buildListFile) + ". At least one module must be specified.");
    }
  }
  
  /**
   * Parse Modules section.
   *
   * @param buildListFile
   * @param sectionLines
   * @param modulesSectionKey
   * @param ac
   *
   * @return TODO @@@@ COMMENT
   */
  private static List<ModuleBlock> extractModulesSection(@NotNull  File                             buildListFile
                                                       , @NotEmpty Map<String,List<LineWithNumber>> sectionLines
                                                       , @NotBlank String                           modulesSectionKey
                                                       , @NotNull  AppContext                       ac) {
    
    final List<ModuleBlock> moduleBlocks = new ArrayList<>();
    
    final List<LineWithNumber> rawModuleLines = sectionLines.get(modulesSectionKey);
    
    // Track seen module paths for duplicate detection with case normalization :
    
    final Set<String> seenModulePaths = new HashSet<>();
    
    int listIndex = ZERO_i;
    
    while (listIndex < rawModuleLines.size()) {
      
      final LineWithNumber pathLineWithNumber = rawModuleLines.get(listIndex);
      
      final String pathLine = pathLineWithNumber.line;
      
      final String trimmedPath = pathLine.trim();
      
      final int pathLineNumber = pathLineWithNumber.number;
      
      // Skip empty lines between blocks :
      
      if (trimmedPath.isEmpty()) {
        
        listIndex++;
      }
      else {
        
        // : Start of a module block.
        
        // First line of block is module path :
        
        final String modulePath = pathLine.stripTrailing();
        
        // Validate module path is not empty :
        
        if (modulePath.trim().isEmpty()) {
          
          throw new MissingExternalValueException("Empty module path at line " + pathLineNumber
                                                + " in [" + MODULES_SECTION_NAME + "] section of file "
                                                + getCanonicalPathAsDescr(buildListFile));
        }
        // Normalize path for case‑insensitive duplicate check on Windows :
        
        final String normalizedPath = normalizePathForComparison(modulePath);
        
        // Check for duplicates :
        
        if (seenModulePaths.contains(normalizedPath)) {
          
          throw new NonUniqueExternalValueException("Duplicate module path '" + modulePath + "' at line " + pathLineNumber
                                                  + " in [" + MODULES_SECTION_NAME + "] section of file "
                                                  + getCanonicalPathAsDescr(buildListFile) + ". Each module must have a unique path.");
        }
        seenModulePaths.add(normalizedPath);
        
        // Look for second line (Maven command) :
        
        listIndex++;
        
        if (listIndex >= rawModuleLines.size()) {
          
          throw new MissingExternalValueException("Incomplete module block in file " + getCanonicalPathAsDescr(buildListFile)
                                                + ". Module path at line " + pathLineNumber + " has no corresponding Maven command.");
        }
        final LineWithNumber commandLineWithNumber = rawModuleLines.get(listIndex);
        
        final String commandLine = commandLineWithNumber.line;
        
        final int commandLineNumber = commandLineWithNumber.number;
        
        listIndex++;
        
        final String mavenCommand = commandLine.trim();
        
        // Validate it's not empty :
        
        if (mavenCommand.isEmpty()) {
          
          throw new MissingExternalValueException("Empty Maven command at line " + commandLineNumber
                                                + " for module path at line " + pathLineNumber
                                                + " in file " + getCanonicalPathAsDescr(buildListFile)
                                                + ". Module blocks must be exactly 2 consecutive non‑empty lines.");
        }
        // Basic Maven command validation :
        
        final String mavenCommandUpper = mavenCommand.toUpperCase();
        
        if (! mavenCommandUpper.startsWith("MVN ")) {
          
          ac.outDevLog("Warning: Maven command at line " + commandLineNumber + " for module '" + modulePath.trim()
                             + " in file " + getCanonicalPathAsDescr(buildListFile)
                             + "' doesn't start with 'mvn ' (case‑insensitive). Command: " + mavenCommand);
        }
        // Create module block (will validate path exists via assertExistingPath) :
        
        moduleBlocks.add(new ModuleBlock(modulePath, mavenCommand));
        
        // Note: Next iteration will handle any blank lines between blocks.
      }
    }
    return moduleBlocks;
  }
  
  /**
   * Parse Options section.
   *
   * @param buildListFile
   * @param sectionLines
   * @param optionsSectionKey
   *
   * @return
   */
  private static TwoObjects<@NotNull Boolean, @NotNull Boolean> extractOptionsSection(
                                                                   @NotNull  File                      buildListFile
                                                                  ,@NotEmpty Map<String
                                                                               , List<LineWithNumber>> sectionLines
                                                                 , @NotBlank String                    optionsSectionKey) {
    
    boolean doPause = true, doTests = true; // : Default values if their option is missing in the file.
    
    final List<LineWithNumber> rawOptionsLines = sectionLines.get(optionsSectionKey);
    
    for (final LineWithNumber lineWithNumber : rawOptionsLines) {
      
      final String trimmed = assertNonBlank(lineWithNumber.line).trim();
      
      if (trimmed.equalsIgnoreCase(NO_PAUSE)) {
        
        doPause = false;
      }
      else if (trimmed.equalsIgnoreCase(NO_TESTS)) {
        
        doTests = false;
      }
      else {
        
        throw new InvalidExternalValueException("Invalid option '" + trimmed + "' at line " + lineWithNumber.number
                                              + " in section [" + OPTIONS_SECTION_NAME + "] of file "
                                              + getCanonicalPathAsDescr(buildListFile)
                                              + ". Valid options are: " + NO_PAUSE + ", " + NO_TESTS);
      }
    }
    return new TwoObjects<>(B(doPause), B(doTests));
  }
  
  /**
   * Parse Initialization section.
   *
   * @param sectionLines
   * @param initSectionKey
   *
   * @return TODO @@@@ COMMENT
   */
  private static @NotNull List<String> extractInitCommandsSection(
                                                              @NotEmpty Map<String, List<LineWithNumber>> sectionLines
                                                            , @NotEmpty String                            initSectionKey) {
    final List<String> result = new ArrayList<>();
    
    final List<LineWithNumber> rawInitLines = sectionLines.get(initSectionKey);
    
    for (final LineWithNumber lineWithNumber : rawInitLines) {
      
      result.add(assertNonBlank(lineWithNumber.line).stripTrailing());
    }
    return result;
  }
  
  /**
   * Validate no content outside sections (except comments which were already filtered).
   *
   * @param buildListFile
   * @param noSectionLines
   */
  private static void validateOutsideSections(@NotNull  File                 buildListFile
                                            , @NotEmpty List<LineWithNumber> noSectionLines) {
    if (! noSectionLines.isEmpty()) {
      
      // Filter out empty lines that might be before first section :
      
      final boolean hasNonEmptyContent = noSectionLines
                                           .stream()
                                           .anyMatch(lwn -> ! lwn.line.trim().isEmpty());
      if (hasNonEmptyContent) {
        
        throw new InvalidExternalValueException("Content found outside of sections in file " + getCanonicalPathAsDescr(buildListFile) + ". All content must be inside "
                                              + surround(INIT_SECTION_NAME,    SECTION_NAME_START, SECTION_NAME_END) + ", "
                                              + surround(OPTIONS_SECTION_NAME, SECTION_NAME_START, SECTION_NAME_END) + ", or "
                                              + surround(MODULES_SECTION_NAME, SECTION_NAME_START, SECTION_NAME_END) + " sections.");
      }
    }
  }
  
  /**
   *
   * @param buildListFile
   * @param sectionLines
   * @param initSectionKey
   * @param optionsSectionKey
   * @param modulesSectionKey
   */
  private static void validateSections(@NotNull  File                              buildListFile
                                     , @NotEmpty Map<String, List<LineWithNumber>> sectionLines
                                     , @NotEmpty String                            initSectionKey
                                     , @NotBlank String                            optionsSectionKey
                                     , @NotBlank String                            modulesSectionKey) {
    
    if (! sectionLines.containsKey(initSectionKey)) {
      
      throw new MissingExternalValueException("Missing required section '[" + INIT_SECTION_NAME + "]' in file "
                                              + getCanonicalPathAsDescr(buildListFile));
    }
    if (! sectionLines.containsKey(optionsSectionKey)) {
      
      throw new MissingExternalValueException("Missing required section '[" + OPTIONS_SECTION_NAME + "]' in file "
                                              + getCanonicalPathAsDescr(buildListFile));
    }
    if (! sectionLines.containsKey(modulesSectionKey)) {
      
      throw new MissingExternalValueException("Missing required section '[" + MODULES_SECTION_NAME + "]' in file "
                                              + getCanonicalPathAsDescr(buildListFile));
    }
  }
  
  
  /**
   * Splits file into sections with line number tracking.
   *
   * @param buildListFile
   * @param allLinesWithNumbers
   *
   * @return TODO @@@@@ COMMENT
   */
  private static TwoObjects<Map<String, List<LineWithNumber>>
                          , List<LineWithNumber>> extractSectionLines(@NotNull  File                 buildListFile
                                                                    , @NotEmpty List<LineWithNumber> allLinesWithNumbers) {
    
    final Map<String, List<LineWithNumber>> sectionLines = HashMap.newHashMap(3);
    
    String currentSection = null;
    
    final List<LineWithNumber> noSectionLines = new ArrayList<>();
    
    for (final LineWithNumber lineWithNumber : allLinesWithNumbers) {
      
      final String rawLine = lineWithNumber.line;
      
      final String trimmedLine = rawLine.trim();
      
      final int lineNumber = lineWithNumber.number;
      
      // Skip comments :
      
      if (! trimmedLine.startsWith(COMMENT_STARTER)) {
        
        // : Not a comment line.
        
        // Check for empty line :
        
        if (trimmedLine.isEmpty()) {
          
          // : Empty line - keep if we're in a section (for module block separation) :
          
          if (currentSection != null) {
            
            sectionLines.get(currentSection).add(lineWithNumber);
          }
          else {
            
            noSectionLines.add(lineWithNumber);
          }
        }
        // Check for section header :
        
        else if (trimmedLine.startsWith(SECTION_NAME_START) && trimmedLine.endsWith(SECTION_NAME_END)) {
          
          final String sectionName = trimmedLine.substring(1, trimmedLine.length() - 1).trim();
          
          // Validate section name :
          
          if (sectionName.equalsIgnoreCase(INIT_SECTION_NAME)
              || sectionName.equalsIgnoreCase(OPTIONS_SECTION_NAME)
              || sectionName.equalsIgnoreCase(MODULES_SECTION_NAME)) {
            
            currentSection = sectionName.toUpperCase();
            
            sectionLines.putIfAbsent(currentSection, new ArrayList<>());
          }
          else {
            
            throw new InvalidExternalValueException("Invalid section name '" + sectionName + "' at line " + lineNumber
                                                    + " in file " + getCanonicalPathAsDescr(buildListFile)
                                                    + ". Valid sections are: [" + INIT_SECTION_NAME + "], ["
                                                    + OPTIONS_SECTION_NAME + "], [" + MODULES_SECTION_NAME + "].");
          }
        }
        else {
          
          // : Regular content line :
          
          if (currentSection != null) {
            
            sectionLines.get(currentSection).add(lineWithNumber);
          }
          else {
            
            noSectionLines.add(lineWithNumber);
          }
        }
      }
    }
    return new TwoObjects<>(sectionLines, noSectionLines);
  }
  
  
  /**
   * Reads the file content with line number tracking. Skips {@link StringUtils#isBlank blank} lines.
   *
   * @param buildListFile The file containing the Build List.
   *
   * @return List of the lines read, with their position.
   */
  private static List<LineWithNumber> readAllLinesWithNumbers(@NotNull File buildListFile) {
    
    final List<LineWithNumber> result = new ArrayList<>();
    
    assertExistingPath(buildListFile.getAbsolutePath(), false);
    
    final List<String> rawLines;
    
    try {
      
      rawLines = FileUtils.readLines(buildListFile, Charset.defaultCharset());
    }
    catch (IOException e) {
      
      throw new InvalidExternalValueException("Cannot read Build List file " + getCanonicalPathAsDescr(buildListFile) + " :" + NL + e.getMessage() + " .");
    }
    String rawLine;
    
    for (int i = ZERO_i; i < rawLines.size(); i++) {
      
      rawLine = rawLines.get(i);
      
      if (isNotBlank(rawLine)) {
      
        result.add(new LineWithNumber(rawLine, i + ONE_i));
      }
    }
    return result;
  }
  
  /**
   * Normalizes a file path for case‑insensitive duplicate detection.
   */
  private static String normalizePathForComparison(@NotBlank String path) {
    
    assertNonBlank(path);
    
    final String result;
    
    if (SystemUtils.IS_OS_WINDOWS) {
      
      // : Windows: case‑insensitive file system :
      
      result = path.toLowerCase();
    }
    else {
      
      // : Unix/Linux/macOS: case‑sensitive file system :
      
      result = path;
    }
    return result;
  }
  
  
  /**
   * Helper record to track original line numbers through parsing phases.
   */
  private record LineWithNumber(String line, int number) {}
  
  
  /**
   * Represents one Build List's block of lines, which describe one module to build.
   *
   * @param modulePath Path of main folder of the source of the module to build (where the pom.xml of that module is).
   *                   With or without the ending [back]slash.
   *
   * @param mvnCommand Whole Maven-invoking command. May include any args; they will be passed to this Maven command as
   *                   they are.
   */
  private record ModuleBlock(String modulePath, String mvnCommand) {
    
    
    /**
     * @param modulePath {@link #modulePath}. Must match an existing folder.<br>
     *
     * @param mvnCommand {@link #mvnCommand}.
     */
    private ModuleBlock(String modulePath, String mvnCommand) {
      
      this.modulePath = assertExistingPath(modulePath, true);
      
      this.mvnCommand = mvnCommand;
    }
  }


  @Override
  public String toString() {
  
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append(" {").append(NL2T);
    
    sb.append("initCommands=").append(listToString(this.initCommands, null, TAB2
                                        , EMPTY,            NL)).append(NL2);
    
    sb.append(TAB).append("doPause=").append(this.doPause).append(NL2);
    
    sb.append(TAB).append("doTests=").append(this.doTests).append(NL2);
    
    sb.append(TAB).append("moduleBlocks=").append(listToString(this.moduleBlocks
                                                          , null, TAB2
                                                     , EMPTY,   NL)).append(NL2).append('}');
    return sb.toString();
  }

}
