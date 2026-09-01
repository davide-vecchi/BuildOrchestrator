# Module BuildOrchestrator

# Changelog

## [@@@@@@@@@@@@@@@@@@@@ 2.3.0 @@@@@@@@@@@@@@@@@@@@]


### Changed

@ Improve discovery of Maven installation folder if config param not given.


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
