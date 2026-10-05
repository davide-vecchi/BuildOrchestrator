# Module BuildOrchestrator

# Changelog

## [2.3.1-SNAPSHOT]


### Changed


### Added


### Removed


### Fixed


### Internal changes

- Make `private` constructors `protected`.
  For the factory method pattern there is no need that all constructors are `private`, which would signal non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors can be `protected` instead.
