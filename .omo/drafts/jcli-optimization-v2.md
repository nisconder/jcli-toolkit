---
slug: jcli-optimization-v2
status: approved
intent: clear
pending-action: write .omo/plans/jcli-optimization-v2.md
approach: 6-phase optimization: code-quality fixes → Gradle-only build migration → Picocli+Gson migration → plugin/template completion → test coverage (≥70%) → GraalVM native-image
---

# Draft: jcli-optimization-v2

## Components (topology ledger)
| id | outcome | status | evidence path |
|----|---------|--------|---------------|
| C1 | 代码质量热修复（空catch, System.exit, Logger输出） | active | Main.java:49,56,61,98,102; Logger.java:70,80 |
| C2 | 构建迁移：移除Maven，增强Gradle，提交Wrapper | active | pom.xml ×5, build.gradle, .github/workflows/ci.yml |
| C3 | Picocli + Gson 迁移 | active | Core/*.java, all 10 command files |
| C4 | 插件系统 + 模板功能完成 | active | Main.java:43-51, TemplateCommand.java:71-79 |
| C5 | 测试覆盖（JUnit 5 + JaCoCo ≥70%） | active | 0 test files currently |
| C6 | GraalVM Native Image 配置 | deferred | cli-entry/build.gradle |
| C7 | CI/CD最终打磨（Checkstyle, SpotBugs, actions v4） | deferred | .github/workflows/ci.yml |

## Metis gap analysis findings (integrated 2026-06-30)

### HIGH severity — must restructure plan
| Finding | Action |
|---------|--------|
| F1+F2: Phase 0 System.exit + execute() fixes overwritten by Phase 2 Picocli migration | **Merge Phase 0 into Phase 2**. Only safe pre-Picocli fixes survive. |
| F3: "No visible behavior change" contradicts Picocli auto-help | **Update Scope OUT**: accept Picocli's default help format |
| F4: ServiceLoader has no META-INF/services files | Phase 3 must create service descriptor; document in plan |
| F5: Gradle has no fat JAR → CI artifact broken | Add `com.github.johnrengelman.shadow` plugin in Phase 1 |
| F6: GenProjectCommand generates pom.xml templates | Update to generate both pom.xml and build.gradle, or document intent |
| F8: CliCommand interface lifecycle undefined | **Decision**: Keep CliCommand, commands implement both CliCommand + Runnable |

### MEDIUM severity — include in plan
| Finding | Action |
|---------|--------|
| F7: GraalVM multiple concerns (CI, reflection, Picocli+Gson) | Document requirements: agent run, reflect-config, CI constraints |
| F10: JaCoCo CI uses Maven path → fails after Phase 1 | Switch to Gradle jacoco plugin in Phase 1 |
| F11: Checkstyle/SpotBugs need Gradle plugin config | Specify plugin coordinates in Phase 6 |
| F13: CI cache key references pom.xml | Remove Maven cache step in Phase 1 |

### LOW severity — document in plan
| Finding | Action |
|---------|--------|
| F9: Logger fix must keep System.out for json() | Explicitly state in Logger fix: json() stays on System.out |
| F12: Gradle test-scope cross-module deps missing | Add testImplementation project(':*') in Phase 1 |
| F14: Picocli annotationProcessor in Gradle | Add `annotationProcessor 'info.picocli:picocli-codegen'` in Phase 2 |

## Revised phases (after Metis)

```
Phase 0a — Safe pre-migration fixes (survive Picocli)
  Logger.java output path fix (keep json() on stdout)

Phase 0b — Build migration: Gradle-only + shadow jar
  Remove Maven → enhance build.gradle → Wrapper → CI → docs

Phase 1 — Picocli + Gson migration + code quality (merged)
  Migrate all 10 commands + Main.java + remove old Parser
  CliCommand: keep as interface, commands implement both CliCommand + Runnable
  Accept Picocli's default help output format

Phase 2 — Plugin system + TemplateCommand
  Create META-INF/services, fix ServiceLoader, implement template add/remove

Phase 3 — Test coverage (JUnit 5 + JaCoCo ≥70%)

Phase 4 — GraalVM Native Image

Phase 5 — CI/CD polish (Checkstyle, SpotBugs, actions v4)
```

## Open assumptions (announced defaults)
| assumption | adopted default | rationale | reversible? |
|------------|----------------|-----------|-------------|
| Picocli版本 | picocli 4.7.x | 最新稳定版，Java 17兼容 | Yes |
| Gson版本 | gson 2.11.x | 最新稳定版 | Yes |
| Gradle版本 | 8.10.2 | CI中已有此版本 | Yes |
| JaCoCo版本 | 0.8.12 | 最新版 | Yes |
| GraalVM native plugin | org.graalvm.buildtools:native-gradle-plugin 0.10.x | 标准Gradle native插件 | Yes |
| 测试框架 | JUnit 5 (Jupiter) + TempDir | 已在依赖中 | Yes |
| Checkstyle | Google Java Style | 项目约定遵守Google风格 | Yes |

## Findings (cited - path:lines)
- **0 tests**: no `src/test/` dirs in any module — `glob("**/src/test/**/*.java")` = empty
- **Empty catch**: `Main.java:49` — `} catch (Exception e) {}` silently swallows plugin loading errors
- **System.exit(×4)**: `Main.java:56,61,98,102` — prevents all unit testing
- **Logger uses System.out**: `Logger.java:70,80` — while `printProgress()` correctly uses `System.err` (line 94,96,100)
- **Hand-rolled JSON no escaping**: `FileFindCommand.java:187-202`, `FileStatCommand.java:131-153`
- **Self-built CLI parser**: `CommandLineParser.java` (175 lines) + `CommandLine.java` (66 lines)
- **TemplateCommand add/remove are stubs**: `TemplateCommand.java:71-79`
- **GenProjectCommand option order bug**: `GenProjectCommand.java:34` — `ofValue("description", "desc", ...)` misplaces short/long name
- **CI actions at v3**: `.github/workflows/ci.yml:19,22,28,47,61,64,69`
- **No Gradle Wrapper**: `settings.gradle` exists, `build.gradle` exists, but no `gradlew`/`gradlew.bat`
- **Maven + Gradle dual build**: 5 pom.xml files + 1 build.gradle

## Decisions (with rationale)
| # | Decision | Rationale | Source |
|---|----------|-----------|--------|
| D1 | 全部六项维度 | 全面优化 | User interview |
| D2 | 测试后补 | 先修复代码问题再补测试 | User interview |
| D3 | 仅保留 Gradle | 减少构建系统维护成本 | User interview |
| D4 | 引入 Picocli + Gson | 用成熟库替代自建解析器和手拼JSON | User interview |
| D5 | 纳入 Native Image | CHANGELOG已planned | User interview |
| D6 | 代码质量优先 | Phase顺序调整 | User interview |

## Scope IN
- 修复现有代码中的所有bug和代码异味
- 用 Picocli 替换自建 CommandLineParser
- 用 Gson 替换手削JSON输出
- 移除所有 Maven POM，增强 Gradle 构建
- 为全部4个模块编写 JUnit 5 测试，JaCoCo ≥70%
- 完善插件加载和模板管理功能
- GraalVM native-image 配置
- CI升级和代码质量门禁

## Scope OUT (Must NOT have)
- 不添加新命令或新功能（只优化现有功能）
- 不改变 CLI 的用户可见行为（参数语义、命令结构不变）
- 不引入 Picocli 以外的框架级依赖（如 Spring/Guice）
- 不改动 CHANGELOG.md 外的文档结构
- 不改动 Java 版本（保持 17）
- 不修改项目 .gitignore（仅移除 Maven 产物条目）

## Open questions
All resolved by interview.

## Approval gate
status: approved
<!-- Approved by user on 2026-06-30. Metis gap analysis pending, then plan generation. -->
