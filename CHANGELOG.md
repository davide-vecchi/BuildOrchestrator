# Module BuildOrchestrator

# Changelog

## [2.3.1-SNAPSHOT]


### Changed


### Added


### Removed


### Fixed

- Translate a message into English in the bash version of the launch script.


### Internal changes

- Extract the file-name literals into constants in the packaging scripts.

- Include the `README.md` and the `LICENSE` in the distributable ZIP.

- Make `private` constructors `protected`.
  For the factory method pattern there is no need that all constructors are `private`, which would signal non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors can be `protected` instead.
