# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of JCLI Toolkit
- Core module with command line parser, logger, and plugin interface
- File operations module (find, stat, rename, grep, sync, diff)
- Code generation module (class, project, snippet)
- Template management system
- CLI entry point with command routing
- Maven and Gradle build support
- Comprehensive documentation

### Planned
- [ ] GraalVM native image support
- [ ] More code templates
- [ ] Complete unit test coverage
- [ ] Performance optimizations
- [ ] Additional file operation commands

## [1.0.0] - 2024-01-XX

### Added
- Initial public release
- File find command with filters (extension, name, size, time)
- File stat command for directory statistics
- File rename command with pattern matching
- File grep command for content search
- File sync and diff commands
- Gen class command with multiple templates (pojo, service, repository, controller, builder, singleton)
- Gen project command with project scaffolding (plain-java, maven-library, gradle-library, cli-app)
- Gen snippet command for common code patterns
- Template management (list, add, remove)
- Color-coded terminal output
- JSON/CSV output formats
- Progress bar support
- ServiceLoader plugin system
- Comprehensive CLI help system

### Security
- Dry-run mode for dangerous operations
- Confirmation prompts
- Proper error handling and exit codes

### Documentation
- README with quick start guide
- Contributing guidelines
- API documentation
- Command reference

## [0.1.0] - 2024-01-XX

### Added
- Core module foundation
- Command line parser
- Logger implementation
- CliCommand interface
- Basic file find command
- Basic file stat command
- Gen class command (pojo template only)
- CLI entry point

### Known Issues
- Limited template support
- No plugin loading
- Basic error handling

---

[Unreleased]: https://github.com/yourusername/jcli-toolkit/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yourusername/jcli-toolkit/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/yourusername/jcli-toolkit/releases/tag/v0.1.0
