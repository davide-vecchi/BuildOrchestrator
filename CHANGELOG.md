# Module BuildOrchestrator

# Changelog

## [2.2.0-SNAPSHOT]


### Changed

- Bump version to 2.2.0-SNAPSHOT .<br><br>

- Update DApplication dependency to 2.2.0-SNAPSHOT .<br><br>

- Update DUtil dependency to 2.2.0-SNAPSHOT .<br><br>

- Update DLog dependency to 2.2.0-SNAPSHOT .<br><br>

- Update DUserInputOutput dependency to 2.2.0-SNAPSHOT .<br><br>


### Added

- Always show whether the program is being executed from within the IDE / build test vs. JAR.

- Set system property "file.encoding" to UTF-8.

- **Breaking:**
  Add a new abstract class AAppContext from new module DApplication to be used
  by consumers to extend their own AppContext concrete class, which must now
  extend AAppContext.

- Add CHANGELOG.md .

- Add in pom a <repositories> section defining the modules of all the DLibs dependencies on GHP.


### Removed


### Fixed


### Internal changes
