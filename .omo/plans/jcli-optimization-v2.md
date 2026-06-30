# jcli-optimization-v2 - Work Plan

## TL;DR (For humans)

**What you'll get:** A production-ready Java CLI toolkit with enterprise-grade code quality. Your custom CLI parser gets replaced by Picocli (auto-help, validation, GraalVM-friendly). Gson handles safe JSON output. Maven is removed in favor of a single Gradle build with a committed Wrapper. Every module gets JUnit 5 tests with ≥70% line coverage. The plugin system is fixed and works. A GraalVM native-image config lets users compile to a native binary. CI runs Checkstyle and SpotBugs quality gates.

**Why this approach:** Picocli and Gson are the de-facto standard for modern Java CLIs — they eliminate ~240 lines of hand-rolled parser code and fix JSON escaping bugs for free. Gradle-only reduces build-system maintenance to one config. "Code quality first" ensures a clean foundation before writing tests, avoiding test-churn from mid-stream refactoring. Native-image follows the existing CHANGELOG planned item.

**What it will NOT do:** Add any new commands or features. Change the CLI's command structure or argument semantics (except help text will use Picocli's auto-generated format). Modify any Java version (stays at 17). Modify the project's documentation structure or CHANGELOG. Introduce Spring/Guice or other DI frameworks. Change the plugin registration interface (CliCommand is preserved).

**Effort:** Large (6 phases, ~12 files modified/created, ~1100 lines of tests)
**Risk:** Medium — Picocli migration rewrites all 10 command files and Main.java. Help output format changes (intentional, not a bug).
**Decisions to sanity-check:** (1) Gradle-only: all pom.xml removed, build.gradle gets shadow plugin for fat JAR. (2) Picocli auto-help replaces hand-rolled help. (3) CliCommand interface preserved as bridge for ServiceLoader plugins. (4) GenProjectCommand templates still generate pom.xml for maven-library/cli-app (generated projects may use Maven).

---

> TL;DR (machine): Large effort, Medium risk. 6 phases: Logger fix → Gradle-only + Wrapper + shadow → Picocli+Gson migration (all 10 commands + Main) → plugin/template fix → JUnit5+JaCoCo≥70% → Native Image → CI quality gates.

## Scope
### Must have
- Logger output path normalization (info/warn/error/verbose → System.err, json() → System.out remains)
- Remove all Maven pom.xml files; Gradle-only build; committed Gradle Wrapper
- Gradle shadow plugin for fat executable JAR (replaces maven-shade-plugin)
- Picocli 4.7.x replacing custom CommandLineParser (175 lines) + CommandLine (66 lines)
- Gson 2.11.x for safe JSON output in FileFindCommand and FileStatCommand
- All 10 command classes rewritten with @Command/@Option/@Parameters annotations
- Main.java refactored to use Picocli CommandLine dispatch; remove System.exit()
- CliCommand interface preserved as bridge; each command implements both CliCommand + Runnable
- Empty catch block in Main.loadPlugins() fixed with proper warning
- META-INF/services/com.jcli.core.CliCommand created for ServiceLoader
- TemplateCommand.add/remove implemented (copy to ~/.jcli/templates/, delete from there)
- GenProjectCommand.maven-library/cli-app templates: maintain pom.xml generation (external projects, not this project)
- JUnit 5 tests for all 4 core modules, ≥70% line coverage (JaCoCo)
- GraalVM native-image support: native-gradle-plugin + reflect-config.json + resource-config.json
- CI: Gradle-only pipeline, actions v4, Checkstyle + SpotBugs quality gates

### Must NOT have (guardrails, anti-slop, scope boundaries)
- No new commands or features added
- No changes to CLI command structure or argument semantics (command names, subcommand hierarchy, argument positions stay identical)
- No Java version change (stays 17)
- No external DI framework (Spring, Guice, Dagger)
- No changes to CHANGELOG.md, CONTRIBUTING.md, or docs/ structure
- No changes to project .gitignore (except removing Maven-specific entries after Maven removal)
- No rewriting of command business logic (file walking, grep, sync, diff, codegen templates) — only the parsing/output scaffolding changes
- No changes to the ServiceLoader plugin interface (CliCommand preserved)
- No Windows-specific limitations — all changes must work on Windows, macOS, and Linux

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after (Phase 3)
- Framework: JUnit 5 (Jupiter) + JaCoCo 0.8.12 + Gradle jacoco plugin
- Evidence: .omo/evidence/task-<N>-jcli-optimization-v2.md

## Execution strategy
### Parallel execution waves
Wave 0a: Logger fix (1 todo)
Wave 0b: Build system migration (3 todos: Gradle enhancement + Wrapper + CI + docs)
Wave 1: Picocli + Gson migration (3 todos: deps + core rewrite + command rewrites)
Wave 2: Plugin + template system (1 todo)
Wave 3: Tests (4 todos: one per module)
Wave 4: Native Image (1 todo)
Wave 5: CI polish (1 todo)

### Dependency matrix (todo numbers)
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1. Logger fix | — | — | — |
| 2. Gradle enhancement | — | 3, 4, 5 | 1 |
| 3. Gen wrapper | 2 | 4 | — |
| 4. CI + docs | 2, 3 | — | 5 |
| 5. Picocli+Gson deps | 2 | 6, 7 | 4 |
| 6. Core+Main Picocli rewrite | 5 | 7, 8, 9 | — |
| 7. Command Picocli rewrite | 5, 6 | 8, 9 | — |
| 8. Plugin+template fix | 6 | 9 | 7 |
| 9. Core tests | 6, 7, 8 | 13 | 10, 11, 12 |
| 10. file-ops tests | 7, 8 | 13 | 9, 11, 12 |
| 11. codegen tests | 7, 8 | 13 | 9, 10, 12 |
| 12. cli-entry tests | 6, 7, 8 | 13 | 9, 10, 11 |
| 13. JaCoCo verify | 9, 10, 11, 12 | — | — |
| 14. Native Image | 7, 8 | — | 15 |
| 15. CI polish | 4, 13 | — | 14 |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->

### Wave 0a — Safe pre-migration fix

- [x] 1. Fix Logger output paths
  What to do / Must NOT do:
    - Change `Logger.info()`, `Logger.warn()`, `Logger.error()`, `Logger.verbose()` to use `System.err` instead of `System.out`
    - `Logger.json()` must REMAIN on `System.out` — this is data output for piping (`jcli file find --output json | jq '.'`)
    - `Logger.printProgress()` already uses `System.err` — leave unchanged
    - Do NOT change any command's direct System.out calls (those are command output, not logs)
  Parallelization: Wave 0a | Blocked by: — | Blocks: —
  References:
    - `core/src/main/java/com/jcli/core/Logger.java:73-81` — `print()` method uses `System.out.println`
    - `core/src/main/java/com/jcli/core/Logger.java:69-71` — `json()` method uses `System.out.println` (MUST KEEP)
    - `.omo/drafts/jcli-optimization-v2.md` — Finding 9 (keep json on stdout for piping)
  Acceptance criteria (agent-executable):
    - `grep -c "System.err.println" core/src/main/java/com/jcli/core/Logger.java` returns ≥4 (info, warn, error, verbose)
    - `grep "System.out.println" core/src/main/java/com/jcli/core/Logger.java` matches only the `json()` method
  QA scenarios:
    - Happy: `Logger.info("test")` produces output on stderr → redirect stdout to file, verify stderr has "test"
    - Happy: `Logger.json("{\"a\":1}")` produces output on stdout → verify stdout has the JSON
    - Failure: `Logger.error("fail")` produces output on stderr even when stdout is redirected
  Commit: Y | `fix(core): redirect logger info/warn/error/verbose to System.err, keep json() on System.out`

### Wave 0b — Build system migration (Gradle-only)

- [x] 2. Enhance build.gradle with all module configs, shadow plugin, and test dependencies
  What to do / Must NOT do:
    - Add `com.github.johnrengelman.shadow` plugin for fat JAR (replaces maven-shade-plugin)
    - Add `application` plugin for `gradle run`
    - Configure `shadowJar` task with `Main-Class = com.jcli.cli.Main`
    - Add per-module project configurations (core, file-ops, codegen, cli-entry)
    - Add `testImplementation project(':core')` etc. for cross-module test visibility
    - Add `jacoco` plugin for coverage
    - Set sourceCompatibility/targetCompatibility to 17 with `java { toolchain { languageVersion = JavaVersion.VERSION_17 } }`
    - Do NOT remove pom.xml files yet (done in todo 4 after CI is ready)
    - Do NOT introduce the `checkstyle` or `spotbugs` plugins yet (done in Phase 5)
  Parallelization: Wave 0b | Blocked by: — | Blocks: 3, 4, 5
  References:
    - `build.gradle` (current — 54 lines, no shadow/application plugin)
    - `cli-entry/pom.xml:47-76` — current shade plugin config (model for shadow)
    - `settings.gradle` — already has 4 modules
    - Metis Finding 5, F12
  Acceptance criteria (agent-executable):
    - `grep -c "shadow" build.gradle` ≥ 1
    - `grep -c "Main-Class" build.gradle` = 2 (one for jar, one for shadowJar)
    - `grep "testImplementation.*project(" build.gradle` matches file-ops and codegen
  QA scenarios:
    - Happy: `gradle clean build` exits 0
    - Happy: `gradle shadowJar` produces `cli-entry/build/libs/cli-entry-1.0.0-all.jar`
    - Happy: `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar --help` shows usage
    - Failure: `gradle build` without Java 17 fails (toolchain enforcement)
  Commit: Y | `build(gradle): add shadow plugin, jacoco, and cross-module test dependencies`

- [x] 3. Generate and commit Gradle Wrapper
  What to do / Must NOT do:
    - Run `gradle wrapper --gradle-version 8.10.2` from project root
    - Commit `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
    - Verify `gradlew build` works clean (no `gradle` required)
    - Do NOT modify any existing source files
  Parallelization: Wave 0b | Blocked by: 2 | Blocks: 4
  References:
    - `TROUBLESHOOTING.md:13-18` — notes "当前仓库未提交 Gradle Wrapper"
    - `README.md:44-46` — mentions Gradle Wrapper not committed
    - Metis Finding 5 (shadow jar)
  Acceptance criteria (agent-executable):
    - `Test-Path gradlew` returns True
    - `Test-Path gradlew.bat` returns True
    - `Test-Path gradle/wrapper/gradle-wrapper.properties` returns True
    - `.\gradlew build --no-daemon` exits 0
  QA scenarios:
    - Happy: `.\gradlew clean shadowJar` produces runnable JAR
    - Happy: `.\gradlew test --no-daemon` runs and passes (zero tests expected initially)
    - Failure: Running on a machine without Gradle installed — `gradlew` wrapper downloads correct version
  Commit: Y | `build(gradle): add Gradle Wrapper 8.10.2`

- [x] 4. Remove Maven files, update CI pipeline, update docs
  What to do / Must NOT do:
    - Delete all 5 `pom.xml` files (root + 4 modules)
    - Remove Eclipse IDE metadata (`.classpath`, `.project`, `.settings/`) from git tracking if present — these are Maven-generated
    - Update `.github/workflows/ci.yml`:
      - Remove Maven `build` job (lines 10-49)
      - Keep Gradle `build-gradle` job, rename to `build`
      - Remove `package` job (Gradle shadowJar replaces it)
      - Add `upload-artifact` step to `build` job for shadow JAR
      - Upgrade actions to v4: `actions/checkout@v4`, `actions/setup-java@v4`, `actions/cache@v4`, `gradle/actions/setup-gradle@v4`
      - Add JaCoCo report upload: `build/reports/jacoco/test/jacoco.xml`
      - Remove `lint` job (replaced by Phase 5 Checkstyle+SpotBugs)
    - Update `README.md`:
      - Remove Maven build instructions, use `gradlew` instead
      - Remove "Gradle 8+（可选）" note — make Gradle required
    - Update `CONTRIBUTING.md`: change all `mvn` commands to `gradlew`
    - Update `.gitignore`: remove `bin/` (Maven-generated), keep `build/`, `target/`, `.gradle/`
    - Remove `.classpath`, `.project`, `.settings/` entries from `.gitignore`
    - Do NOT modify `docs/COMMAND-REFERENCE.md` or `docs/USER-GUIDE.md` (they reference commands, not build)
    - Do NOT modify `CHANGELOG.md`
  Parallelization: Wave 0b | Blocked by: 2, 3 | Blocks: —
  References:
    - `.github/workflows/ci.yml` — full CI file
    - `README.md:13-17,42-46` — build instructions
    - `CONTRIBUTING.md:22-28` — build commands
    - `.gitignore:3-19` — IDE and build entries
    - Metis Findings 5, 10, 13
  Acceptance criteria (agent-executable):
    - `Get-ChildItem -Recurse -Filter pom.xml` returns 0 files
    - `grep "mvn" README.md` returns 0 matches
    - `grep "maven" .github/workflows/ci.yml` returns 0 matches (except maybe in comments)
    - `grep "actions/.*@v4" .github/workflows/ci.yml` returns ≥5 (checkout, setup-java, cache, etc.)
  QA scenarios:
    - Happy: `.\gradlew clean build --no-daemon` exits 0
    - Happy: `.\gradlew shadowJar --no-daemon` creates fat JAR
    - Happy: CI `build` job produces artifact `jcli-toolkit-jar`
    - Failure: Running `mvn clean package` fails with "command not found" (expected — Maven removed)
  Commit: Y | `build: remove Maven, migrate to Gradle-only build with shadow JAR; update CI and docs`

### Wave 1 — Picocli + Gson migration (code quality merged)

- [x] 5. Add Picocli and Gson dependencies; create CliCommand→Picocli bridge
  What to do / Must NOT do:
    - In `build.gradle`: add dependencies
      - `implementation 'info.picocli:picocli:4.7.6'`
      - `annotationProcessor 'info.picocli:picocli-codegen:4.7.6'`
      - `implementation 'com.google.code.gson:gson:2.11.0'`
    - Preserve `CliCommand` interface (`core/src/main/java/com/jcli/core/CliCommand.java`)
    - Before making any code changes, run `gradle dependencies` to verify dependencies resolve
    - Do NOT change any Java source files yet (done in tasks 6, 7)
  Parallelization: Wave 1 | Blocked by: 2 | Blocks: 6, 7
  References:
    - `build.gradle:22-24` — current dependencies section
    - Metis Findings 8 (CliCommand decision), 14 (annotationProcessor)
  Acceptance criteria (agent-executable):
    - `grep "picocli" build.gradle` matches 2 lines (implementation + annotationProcessor)
    - `grep "gson" build.gradle` matches 1 line
    - `gradle dependencies --no-daemon` exits 0 and shows picocli+gson in compileClasspath
  QA scenarios:
    - Happy: `gradle build --no-daemon` exits 0 after deps added
    - Failure: wrong version → build fails (CI catches it)
  Commit: N (part of tasks 6/7 — batched commit with todo 7)

- [x] 6. Rewrite core module (CliCommand→Picocli bridge) and Main.java
  What to do / Must NOT do:
    - `Main.java`:
      - Remove static `commands`/`subcommands` maps and all registration
      - Use Picocli `@Command` at class level with subcommands
      - Replace custom parse logic with `new CommandLine(new JcliCli()).execute(args)`
      - Remove all System.exit() calls — use Picocli exit code handling
      - `loadPlugins()` still uses ServiceLoader but gets proper logging
    - `CommandLineParser.java`: DELETE (replaced by Picocli)
    - `CommandLine.java`: DELETE (replaced by Picocli `CommandLine`)
    - `LogLevel.java`: KEEP (still used by Logger)
    - Create `JcliCli.java` as root Picocli command:
      ```java
      @Command(name = "jcli", version = "JCLI Toolkit v1.0.0",
               description = "Java CLI Toolkit for Developers",
               subcommands = {FileFindCommand.class, ...},
               mixinStandardHelpOptions = true)
      ```
    - `CliCommand.java`: KEEP — commands implement both CliCommand + Runnable
      - Add default method: `default Integer call() { return 0; }` for Picocli compatibility
    - Fix `loadPlugins()`: change empty catch to `Logger.warn()`
    - Do NOT change any file-ops or codegen command implementation (done in todo 7)
  Parallelization: Wave 1 | Blocked by: 5 | Blocks: 7, 8, 9
  References:
    - `cli-entry/src/main/java/com/jcli/cli/Main.java` — full rewrite (159 lines)
    - `core/src/main/java/com/jcli/core/CommandLineParser.java` — to delete (175 lines)
    - `core/src/main/java/com/jcli/core/CommandLine.java` — to delete (66 lines)
    - `core/src/main/java/com/jcli/core/CliCommand.java` — to keep (9 lines)
    - `.omo/drafts/jcli-optimization-v2.md` — Metis Finding 1, 2, 3 (help format change)
  Acceptance criteria (agent-executable):
    - `Test-Path core/src/main/java/com/jcli/core/CommandLineParser.java` returns False
    - `Test-Path core/src/main/java/com/jcli/core/CommandLine.java` returns False
    - `Test-Path cli-entry/src/main/java/com/jcli/cli/JcliCli.java` returns True
    - `grep "System.exit" cli-entry/src/main/java/com/jcli/cli/Main.java` returns 0 matches
    - `grep "catch.*Exception.*{}" cli-entry/src/main/java/com/jcli/cli/Main.java` returns 0 matches
    - `.\gradlew build --no-daemon` exits 0
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar --help` shows usage
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar --version` shows version
  QA scenarios:
    - Happy: `-h` and `--help` show usage
    - Happy: `-V` and `--version` show "JCLI Toolkit v1.0.0"
    - Happy: unknown command `jcli foo` shows error + help
    - Happy: `jcli file --help` shows file subcommands
    - Failure: `jcli` with no args shows help (not crash)
  Commit: N (batched with todo 7 — atomic with command rewrites)

- [x] 7. Rewrite all 10 commands with Picocli annotations + Gson JSON
  What to do / Must NOT do:
    - Each command becomes `@Command(name=...) implements CliCommand, Runnable`
    - All options replaced with `@Option(names = {"-d", "--dir"})` fields
    - Required parameters marked with `required = true`
    - Each command's business logic moves to `run()` method
    - `execute(String[] args)` delegates to Picocli: `return new CommandLine(this).execute(args);`
    - **FileFindCommand**: replace hand-rolled JSON (lines 187-202) with Gson
    - **FileStatCommand**: replace hand-rolled JSON (lines 131-153) with Gson
    - **GenProjectCommand**: fix option order bug (line 34 `ofValue("description", "desc", ...)`)
    - All `CliCommand` imports updated to use preserved interface
    - COMMANDS TO REWRITE:
      - `file-ops/FileFindCommand.java` (216 lines) — rewrite parsing layer, keep business logic
      - `file-ops/FileGrepCommand.java` (152 lines) — rewrite parsing
      - `file-ops/FileStatCommand.java` (167 lines) — rewrite parsing + JSON
      - `file-ops/FileRenameCommand.java` (251 lines) — rewrite parsing
      - `file-ops/FileSyncCommand.java` (237 lines) — rewrite parsing
      - `file-ops/FileDiffCommand.java` (143 lines) — rewrite parsing
      - `codegen/GenClassCommand.java` (238 lines) — rewrite parsing
      - `codegen/GenProjectCommand.java` (243 lines) — rewrite parsing + fix option bug
      - `codegen/GenSnippetCommand.java` (162 lines) — rewrite parsing
      - `codegen/TemplateCommand.java` (80 lines) — rewrite parsing (but NOT implement add/remove yet — Phase 2)
    - Do NOT change business logic (file walking, regex matching, diffing, codegen templates)
    - Do NOT implement template add/remove yet (Phase 2)
    - All must compile and pass `gradle build`
  Parallelization: Wave 1 | Blocked by: 5, 6 | Blocks: 8, 9
  References:
    - ALL 10 files listed above with line ranges
    - `core/src/main/java/com/jcli/core/CliCommand.java` — preserved interface
    - `.omo/drafts/jcli-optimization-v2.md` — Finding 6 (GenProjectCommand templates maintain pom.xml)
  Acceptance criteria (agent-executable):
    - `.\gradlew clean build --no-daemon` exits 0 with no warnings
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar file find --help` shows picocli-formatted help
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar file grep --help` shows picocli-formatted help
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar gen class --help` shows picocli-formatted help
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar template list` outputs template list
    - `grep "Gson\|gson" FileFindCommand.java` ≥ 1
    - `grep "Gson\|gson" FileStatCommand.java` ≥ 1
  QA scenarios:
    - Happy: `jcli file find --dir . --ext java` works and outputs files
    - Happy: `jcli file stat --dir . --output json` outputs valid JSON (parseable by `jq .`)
    - Happy: `jcli file grep --pattern "TODO" --dir .` works
    - Happy: `jcli file rename --dir . --pattern "test" --dry-run` works
    - Happy: `jcli gen class --name Foo --package com.test --template pojo` generates file
    - Failure: `jcli file grep` without `--pattern` shows required-error message
    - Failure: `jcli file find --dir /nonexistent` returns exit code 1
  Commit: Y | `refactor: migrate all commands to Picocli annotations and Gson JSON output`

### Wave 2 — Plugin system + TemplateCommand

- [ ] 8. Fix plugin loading and implement TemplateCommand add/remove
  What to do / Must NOT do:
    - Create `META-INF/services/com.jcli.core.CliCommand` file (empty, for ServiceLoader discovery)
    - `Main.java` (now JcliCli.java) `loadPlugins()`:
      - Add `~/.jcli/plugins/` directory scanning
      - Use `URLClassLoader` to load JARs from that directory
      - Log warning if plugin loading fails (not silent)
    - `TemplateCommand.java`:
      - **add**: copy file/directory from `args[1]` to `~/.jcli/templates/<name>/`
      - **remove**: recursively delete `~/.jcli/templates/<name>/`
      - Update `Subcommand` to continue working with Picocli annotations
    - `~/.jcli/` directory: create if not exists
    - Do NOT change the Picocli command structure
    - Do NOT add unit tests yet (Phase 3)
  Parallelization: Wave 2 | Blocked by: 6 | Blocks: 9
  References:
    - NOTE: After todo 6, Main.java is rewritten into `JcliCli.java`. The `loadPlugins()` method now lives in `JcliCli.java`.
    - `codegen/src/main/java/com/jcli/codegen/TemplateCommand.java:71-79` — stub add/remove
    - `CONTRIBUTING.md:113-137` — plugin documentation (describes META-INF/services approach)
    - Metis Finding 4 (no META-INF/services files exist)
  Acceptance criteria (agent-executable):
    - `Test-Path "core/src/main/resources/META-INF/services/com.jcli.core.CliCommand"` returns True
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar template list` outputs built-in + user templates
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar template add .` copies dir to `~/.jcli/templates/`
    - `java -jar cli-entry/build/libs/cli-entry-1.0.0-all.jar template remove <name>` deletes from `~/.jcli/templates/`
    - `grep "catch.*Exception.*{}" Main.java` OR `grep "catch.*Exception.*{}" JcliCli.java` returns 0 matches
  QA scenarios:
    - Happy: `template list` shows all built-in templates
    - Happy: `template add ./my-tpl` works, then `template list` shows it
    - Happy: `template remove my-tpl` works
    - Failure: `template add /nonexistent` returns exit code 1 + error message
    - Failure: `template remove nonexistent` returns exit code 1 + error message
  Commit: Y | `feat: fix plugin loading with proper logging and implement template add/remove`

### Wave 3 — Test coverage (JUnit 5 + JaCoCo ≥70%)
- [ ] 9. Write core module tests

  What to do / Must NOT do:
    - `core/src/test/java/com/jcli/core/LoggerTest.java`:
      - Test setLevel filtering
      - Test setVerbose/setQuiet
      - Test colorEnabled=false strips ANSI
      - Test json() goes to System.out vs other methods go to System.err
    - `core/src/test/java/com/jcli/core/LogLevelTest.java`:
      - Test enum ordering (INFO < WARN < ERROR)
    - All tests use `ByteArrayOutputStream` + `System.setOut()`/`System.setErr()`
    - Do NOT write tests for deleted classes (CommandLineParser, CommandLine)
  Parallelization: Wave 3 | Blocked by: 7, 8 | Blocks: 13
  References:
    - `core/src/main/java/com/jcli/core/Logger.java` — full file (104 lines)
    - `core/src/main/java/com/jcli/core/LogLevel.java` — full file (17 lines)
  Acceptance criteria (agent-executable):
    - `.\gradlew :core:test --no-daemon` passes
    - JaCoCo report for core shows ≥70% line coverage
  QA scenarios:
    - Happy: Logger.info() writes to System.err
    - Happy: Logger.json() writes to System.out
    - Happy: LogLevel.INFO.getLevel() = 0, WARN = 1, ERROR = 2
  Commit: N (batched with todo 13)

- [ ] 10. Write file-ops module tests
  What to do / Must NOT do:
    - Test files in `file-ops/src/test/java/com/jcli/fileops/`:
    - `FileFindCommandTest.java`:
      - Create temp dir with known files; test --ext, --name, --size-gt, --depth
      - Test --output json produces valid JSON
      - Test --output csv produces valid CSV
    - `FileGrepCommandTest.java`:
      - Create temp files with known content; test --pattern matching
      - Test --context, --ignore-case, --exclude
    - `FileStatCommandTest.java`:
      - Test --recursive vs non-recursive
      - Test --output json
    - `FileRenameCommandTest.java`:
      - Test --dry-run doesn't rename
      - Test --confirm with mocked stdin
      - Test prefix/suffix/seq
      - Test collision detection returns 1
    - `FileSyncCommandTest.java`:
      - Test copy-on-newer
      - Test --delete removes extra files
      - Test --dry-run
    - `FileDiffCommandTest.java`:
      - Test identical dirs → "Directories are identical"
      - Test dirs with differences → shows only-in-X / modified
    - All tests use `@TempDir` for temporary directories
    - Use `@Mock` or manual stdin for confirm prompts
  Parallelization: Wave 3 | Blocked by: 7, 8 | Blocks: 13
  References:
    - All 6 file-ops command files (rewritten in todo 7)
  Acceptance criteria (agent-executable):
    - `.\gradlew :file-ops:test --no-daemon` passes
    - JaCoCo report for file-ops shows ≥70% line coverage
  QA scenarios: (see individual test descriptions above)
  Commit: N (batched with todo 13)

- [ ] 11. Write codegen module tests
  What to do / Must NOT do:
    - Test files in `codegen/src/test/java/com/jcli/codegen/`:
    - `GenClassCommandTest.java`:
      - Test POJO template generates valid Java class with fields
      - Test service/repository/controller/builder/singleton templates
      - Test missing --name returns 1
    - `GenProjectCommandTest.java`:
      - Test all 4 project templates create correct directory structure
      - Test missing --name returns 1
      - Test existing output directory returns 1
    - `GenSnippetCommandTest.java`:
      - Test all 5 snippet types
    - `TemplateCommandTest.java`:
      - Test `list` outputs templates
      - Test `add` copies to ~/.jcli/templates/
      - Test `remove` deletes from ~/.jcli/templates/
  Parallelization: Wave 3 | Blocked by: 7, 8 | Blocks: 13
  References:
    - All 4 codegen command files (rewritten in todo 7, updated in todo 8)
  Acceptance criteria (agent-executable):
    - `.\gradlew :codegen:test --no-daemon` passes
    - JaCoCo report for codegen shows ≥70% line coverage
    - Generated Java files are syntactically valid (can be parsed)
  Commit: N (batched with todo 13)

- [ ] 12. Write cli-entry module tests
  What to do / Must NOT do:
    - `cli-entry/src/test/java/com/jcli/cli/MainTest.java` — test against JcliCli root command:
    - Test `--help` and `--version` output
    - Test `file find` dispatches correctly
    - Test `file grep --pattern foo` dispatches correctly
    - Test unknown command returns 1
    - Test no args shows help
    - Do NOT call System.exit() — test through `CommandLine.execute()` or `JcliCli.run()`
  Parallelization: Wave 3 | Blocked by: 6, 7, 8 | Blocks: 13
  References:
    - `cli-entry/src/main/java/com/jcli/cli/Main.java` + `JcliCli.java` (from todo 6)
  Acceptance criteria (agent-executable):
    - `.\gradlew :cli-entry:test --no-daemon` passes
    - JaCoCo report for cli-entry ≥70%
    - No tests call System.exit() (grep for System.exit in test files = 0)
  Commit: N (batched with todo 13)

- [ ] 13. Configure JaCoCo in Gradle and verify ≥70% coverage
  What to do / Must NOT do:
    - In `build.gradle`: add `id 'jacoco'` plugin at root
    - Configure JaCoCo version to 0.8.12
    - Add coverage rule: line coverage ≥0.70 for each module
    - Add `jacocoTestReport` task that generates XML + HTML reports
    - Run `gradle test jacocoTestReport` and verify coverage meets threshold **locally**
    - Do NOT add JaCoCo coverage verification to CI yet (deferred to todo 15)
  Parallelization: Wave 3 | Blocked by: 9, 10, 11, 12 | Blocks: —
  References:
    - `build.gradle` — add jacoco config
    - `.github/workflows/ci.yml` — JaCoCo step (will be updated in Phase 5)
    - Metis Finding 10
  Acceptance criteria (agent-executable):
    - `grep "jacoco" build.gradle` ≥ 2 lines (plugin + version + rule)
    - `grep "jacocoTestReport" build.gradle` ≥ 1
    - `.\gradlew test jacocoTestReport --no-daemon` exits 0
    - `Test-Path build/reports/jacoco/test/jacoco.xml` returns True
    - `.\gradlew test --no-daemon` succeeds (all 15+ test classes pass)
    - Coverage check fails if coverage <70% (test by temporarily reducing coverage)
  QA scenarios:
    - Happy: all tests pass, coverage ≥70%
    - Failure: delete a test, coverage drops below 70%, `gradle test` fails
  Commit: Y | `test: add JUnit 5 tests for all modules with JaCoCo 70% coverage gate`

### Wave 4 — GraalVM Native Image

- [ ] 14. Configure GraalVM Native Image support
  What to do / Must NOT do:
    - Add `org.graalvm.buildtools:native-gradle-plugin:0.10.3` to `build.gradle`
    - Add `id 'org.graalvm.buildtools.native'` plugin
    - Run the application with `native-image-agent` to generate reflection config:
      1. Run with `-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/`
      2. Exercise all commands: `file find`, `file stat --output json`, `file grep`, `file rename`, `gen class`, etc.
    - Commit generated `reflect-config.json`, `resource-config.json`, `jni-config.json`, `proxy-config.json`, `serialization-config.json`
    - Add `nativeBuild` job to CI (Ubuntu only — macOS/Windows native-image is unreliable for GraalVM CE)
    - Document in README: `gradle nativeCompile` → `./build/native/nativeCompile/jcli`
    - Do NOT make native-image a required CI check (allowed to fail)
    - Do NOT modify existing command code for GraalVM compatibility (reflection config handles it)
  Parallelization: Wave 4 | Blocked by: 7, 8 | Blocks: —
  References:
    - `cli-entry/build.gradle` — add native plugin
    - `.github/workflows/ci.yml` — add optional native build job
    - `CHANGELOG.md:22` — "GraalVM native image support" listed as Planned
    - Metis Finding 7 (reflection config for Picocli + Gson + ServiceLoader)
    - Picocli docs: picocli uses reflection for @Command/@Option — needs `reflect-config.json`
    - Gson docs: field access via reflection — needs `reflect-config.json`
  Acceptance criteria (agent-executable):
    - `grep "native" build.gradle` ≥ 2 lines
    - `Test-Path "src/main/resources/META-INF/native-image/reflect-config.json"` returns True
    - `Test-Path "src/main/resources/META-INF/native-image/resource-config.json"` returns True
    - `gradle nativeCompile` exits 0 (requires GraalVM JDK)
  QA scenarios:
    - Happy: `gradle nativeCompile` creates native binary
    - Happy: `./build/native/nativeCompile/jcli --help` shows usage (fast startup)
    - Happy: `./build/native/nativeCompile/jcli file find --dir .` works
    - Failure: Running on non-GraalVM JDK → `nativeCompile` is skipped, not crash
    - Failure: Missing reflection config → command fails at runtime with ReflectionException
  Commit: Y | `feat: add GraalVM native-image support with reflection config`

### Wave 5 — CI/CD polish (Checkstyle, SpotBugs, actions)

- [ ] 15. Add Checkstyle and SpotBugs quality gates; finalize CI
  What to do / Must NOT do:
    - In `build.gradle`:
      - Add `id 'checkstyle'` plugin
      - Add `id 'com.github.spotbugs'` version '6.0.0' plugin
      - Configure checkstyle with Google Java Style:
        ```gradle
        checkstyle {
            toolVersion = '10.17.0'
            configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
        }
        ```
    - Create `config/checkstyle/checkstyle.xml`:
      - Based on Google Java Style (4-space indent, Javadoc required, naming conventions)
      - Relax rules for generated code in codegen module (snippet command generates System.out code)
    - Add SpotBugs excludes file: `config/spotbugs/exclude.xml`
    - Update `.github/workflows/ci.yml`:
      - Add `lint` job: `gradle checkstyleMain spotbugsMain`
      - Keep `build` job as main validation
      - Ensure JaCoCo coverage check runs in CI (`gradle test` includes it)
      - Actions already at v4 (from todo 4)
    - Remove `continue-on-error: true` from lint steps (quality gate is mandatory)
    - Add `codecov/codecov-action@v4` with correct Gradle path: `build/reports/jacoco/test/jacoco.xml`
    - Do NOT add SpotBugs findsecbugs plugin (out of scope)
    - Do NOT make `lint` job block the `build` job — they run in parallel
  Parallelization: Wave 5 | Blocked by: 4, 13 | Blocks: —
  References:
    - `build.gradle` — add plugins
    - `.github/workflows/ci.yml:89-104` — current `lint` job (uses `mvn checkstyle:check`)
    - `CONTRIBUTING.md:30-36` — mentions "4 空格缩进" and Google Java Style
    - Metis Finding 11 (Checkstyle/SpotBugs Gradle plugin config)
  Acceptance criteria (agent-executable):
    - `grep "checkstyle" build.gradle` ≥ 2 lines
    - `grep "spotbugs" build.gradle` ≥ 1 line
    - `Test-Path config/checkstyle/checkstyle.xml` returns True
    - `.\gradlew checkstyleMain --no-daemon` exits 0 (no style violations)
    - `.\gradlew spotbugsMain --no-daemon` exits 0 (no bugs found)
    - `.github/workflows/ci.yml` has `gradle checkstyleMain spotbugsMain` in lint job
    - `grep "continue-on-error.*true" .github/workflows/ci.yml` returns 0 matches
  QA scenarios:
    - Happy: `gradle check build` passes all quality gates
    - Happy: CI `lint` job passes
    - Failure: Add intentional style violation → `checkstyleMain` fails
    - Failure: Introduce null dereference → `spotbugsMain` fails
  Commit: Y | `ci: add Checkstyle and SpotBugs quality gates; finalize Gradle CI pipeline`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit — every todo committed, no scope creep
- [ ] F2. Code quality review — `gradle check build` passes, no Checkstyle/SpotBugs violations
- [ ] F3. Real manual QA — `java -jar cli-entry/build/libs/cli-entry-*-all.jar` each command: file find, file stat --output json, file grep, file rename --dry-run, file sync --dry-run, file diff, gen class, gen project, gen snippet, template list
- [ ] F4. Scope fidelity — no new commands, no Java version change, no DI framework added

## Commit strategy
Separate commits per todo, with the following convention:
```
type(scope): message
```
Types: `fix`, `refactor`, `build`, `test`, `feat`, `ci`
Scopes: `core`, `file-ops`, `codegen`, `cli-entry`, `build`, `ci`, `gradle`

Commits:
1. `fix(core): redirect logger info/warn/error/verbose to System.err, keep json() on System.out`
2. `build(gradle): add shadow plugin, jacoco, and cross-module test dependencies`
3. `build(gradle): add Gradle Wrapper 8.10.2`
4. `build: remove Maven, migrate to Gradle-only build with shadow JAR; update CI and docs`
5. `refactor: migrate all commands to Picocli annotations and Gson JSON output`
6. `feat: fix plugin loading with proper logging and implement template add/remove`
7. `test: add JUnit 5 tests for all modules with JaCoCo 70% coverage gate`
8. `feat: add GraalVM native-image support with reflection config`
9. `ci: add Checkstyle and SpotBugs quality gates; finalize Gradle CI pipeline`

## Success criteria
1. `gradle clean build` exits 0
2. `gradle test` passes all tests (>15 test classes)
3. JaCoCo line coverage ≥70% for each module
4. `java -jar cli-entry/build/libs/cli-entry-*-all.jar` is runnable with all commands
5. `gradle nativeCompile` produces native binary (on GraalVM JDK)
6. CI has three passing jobs: `build`, `lint`, and optional `native`
7. All System.exit() calls removed from Main.java
8. No empty catch blocks remain
9. Logger output: info/warn/error → System.err, json → System.out
10. Gson generates valid JSON output for `file find --output json` and `file stat --output json`
11. TemplateCommand add/remove work with `~/.jcli/templates/`
12. `gradlew build` works on all three OS (Windows, macOS, Linux)
