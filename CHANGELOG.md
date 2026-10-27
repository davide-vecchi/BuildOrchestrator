# Module BuildOrchestrator

# Changelog

## [2.3.1-SNAPSHOT]


### Changed

- Update the `duserinputoutput` dependency to its latest version.

- Update the `dtest` dependency to its latest version.

- Update the `dapplication` dependency to its latest version.

- Update the `dutil` dependency to its latest version.

- Update the `dmaven` dependency to its latest version.

- Update the `dfile` dependency to its latest version.


### Added


### Removed


### Fixed

- Translate a message into English in the bash version of the launch script.

- Convert a groupId's dots to path separators for the Maven-repo path.


### Internal changes

- Enable `addDefaultImplementationEntries` in the assembly plugin, so the executable JAR's manifest carries the module's `Implementation-Version`.

- Extract the file-name literals into constants in the packaging scripts.

- Include the `README.md` and the `LICENSE` in the distributable ZIP.

- Make `private` constructors `protected`.
  For the factory method pattern there is no need that all constructors are `private`, which would signal non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors can be `protected` instead.
