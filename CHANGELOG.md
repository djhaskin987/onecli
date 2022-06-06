# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
## [Unreleased]
### Added

### Changed

## [0.8.0] - 2022-06-05
### Added
- Test proving that JSON slurped in using a YAML parser is still the same
  as if it were a JSON parser, even in the presence of multiple keys.

### Fixed
- Stacktraces in YAML look nice, but still are the same in JSON to fix for
  backwards compatibility

## [0.7.0] - 2022-06-05
### Added
- Output format option `set-output-format` given an alias of `-o`.
- Output format option added to enable YAML output
- Stacktraces in YAML look nice
- Config files in YAML format are looked for if their JSON counterparts don't exist
- CLI and ENV are yaml-aware
### Changed
- All JSON is slurped in using a YAML parser, so that's something.

## [0.1.1] - 2019-11-26
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2019-11-26
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[Unreleased]: https://github.com/your-name/onecli/compare/0.8.0...HEAD
[0.8.0]: https://github.com/your-name/onecli/compare/0.7.0...0.80
[0.7.0]: https://github.com/your-name/onecli/compare/0.1.1...0.7.0
[0.1.1]: https://github.com/your-name/onecli/compare/0.1.0...0.1.1
