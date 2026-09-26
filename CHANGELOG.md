# Module BuildOrchestrator

# Changelog

## [2.3.0-SNAPSHOT]


### Changed

- Use DMaven 2.3.0 for its new findMavenHome() method, that makes attempts at discovering the Maven installation folder.
  Will be used if the Maven installation folder is not given as a config param.

- Update all dependencies (DLibs and 3rd party) to their latest versions.


### Added


### Removed


### Fixed

- Use DFile 2.4.0 for its new FilesystemPathParser class, to convert paths to have the name separator ("\" vs. "/") of
  the current OS.


### Internal changes

- Make private constructor protected.
  For the factory method pattern there is no need that all constructors are private, which would signal
  non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors
  can be protected instead.

- Add and remove some @NotNull annotations.

- Remove DTestNG DLib, use TestNG directly.
