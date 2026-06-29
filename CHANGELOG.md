# Changelog

## [2.1.0-SNAPSHOT] - Unreleased


### Changed


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

- Make test resources paths relative in test configuration files, were absolute.
