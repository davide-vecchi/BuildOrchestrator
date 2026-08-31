# Module BuildOrchestrator

# Changelog

## [@@@@@@@@@@@@@@@@@@@@ AFTER 2.2.1 @@@@@@@@@@@@@@@@@@@@]


### Changed


### Added


### Removed


### Fixed


### Internal changes

- Private constructor made protected.
  For the factory method pattern there is no need that all constructors are private, which would signal
  non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors
  can be protected instead.

- Added and removed some @NotNull annotations.
