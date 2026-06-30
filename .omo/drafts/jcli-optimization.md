# Draft: jcli-optimization

## Decisions (from interview, 2026-06-30)

| # | 维度 | 决策 | 说明 |
|---|------|------|------|
| D1 | 优化范围 | 全部六项 | 测试 + 代码质量 + 构建CI + 插件系统 + CLI增强 + Native Image |
| D2 | 测试策略 | 测试后补 | 先修代码质量再补测试，目标≥70% |
| D3 | 构建系统 | 仅保留 Gradle | 移除所有 pom.xml，增强 build.gradle，提交 Gradle Wrapper |
| D4 | 外部依赖 | 引入 Gson + Picocli | 替换自建 CLI 解析器 + JSON 输出 |
| D5 | Native Image | 纳入计划 | 添加 GraalVM native-image 配置 |
| D6 | 执行优先级 | 代码质量优先 | 代码修复先于测试编写 |

## Key findings from codebase audit (2026-06-30)

- **16 Java sources**, 4 modules, ~2000 LOC
- **0 test files** despite JUnit 5 declared in all pom.xml
- **Empty catch block**: `Main.java:49` — `} catch (Exception e) {}`
- **4× System.exit()**: `Main.java:56,61,98,102` — prevents all unit testing
- **Self-built CLI parser**: `CommandLineParser.java` (175 lines) + `CommandLine.java` (66 lines) — functional but limited
- **Logger uses System.out** for all output (standard: System.err for logs, System.out for data)
- **JSON output is hand-rolled** without escaping (`FileFindCommand.java:187-202`, `FileStatCommand.java:131-153`)
- **TemplateCommand add/remove** are stubs (`TemplateCommand.java:71-79`)
- **GenProjectCommand option registration** has argument order bug (`GenProjectCommand.java:34-35`)
- **CI actions** at v3 (should be v4)
- **Maven + Gradle dual build** with no Gradle Wrapper committed
- **No checkstyle/spotbugs** configuration
- **GraalVM native-image** planned in CHANGELOG but not started

## Restructured plan phases (after Picocli + Gradle-only decisions)

```
Phase 0 — Pre-migration code quality fixes
  Fix empty catch, System.exit(), Logger output

Phase 1 — Build system: Gradle-only migration
  Remove Maven, enhance build.gradle, generate wrapper, update CI/docs

Phase 2 — Picocli + Gson migration (replaces Parser + JSON output)
  Add dependencies, rewrite commands with annotations, remove old parser

Phase 3 — Plugin system + TemplateCommand completion
  Fix plugin loading, implement template add/remove

Phase 4 — Test infrastructure (测试后补)
  JUnit 5 tests for all 4 modules, JaCoCo 70%

Phase 5 — GraalVM Native Image
  Gradle native-image plugin + reflect-config

Phase 6 — CI/CD polish
  Checkstyle, SpotBugs, actions v4
```

## Status

- [x] Exploration complete
- [x] Interview complete (all 6 forks resolved)
- [ ] Approval gate: awaiting approval
- [ ] Plan generation
- [ ] Metis gap analysis
- [ ] Momus high-accuracy review (CLEAR path: optional, offered at delivery)
