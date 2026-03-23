# Contributing to JCLI Toolkit

感谢您对 JCLI Toolkit 的关注！我们欢迎各种形式的贡献。

## 开发环境设置

### 前置要求

- Java 17+
- Maven 3.6+ 或 Gradle 8+
- Git

### 克隆仓库

```bash
git clone https://github.com/nisconder/jcli-toolkit.git
cd jcli-toolkit
```

### 构建

```bash
# Maven
mvn clean install

# Gradle
./gradlew clean build
```

## 代码风格

- 使用 4 空格缩进
- 遵循 Google Java Style Guide
- 添加适当的 Javadoc 注释
- 保持方法简短和单一职责

## 添加新功能

### 1. 创建新命令

在相应的模块中实现 `CliCommand` 接口：

```java
package com.jcli.example;

import com.jcli.core.CliCommand;
import com.jcli.core.CommandLine;
import com.jcli.core.CommandLineParser;
import com.jcli.core.Logger;

public class ExampleCommand implements CliCommand {
    @Override
    public String name() {
        return "example";
    }

    @Override
    public String description() {
        return "Example command";
    }

    @Override
    public int execute(String[] args) throws Exception {
        CommandLineParser parser = new CommandLineParser("jcli example")
                .description("Example command description")
                .addOption(CommandLineParser.Option.of("v", "verbose", "Verbose output"));

        CommandLine cmdLine = parser.parse(args);

        if (cmdLine.shouldShowHelp()) {
            parser.printHelp();
            return 0;
        }

        // 命令逻辑
        return 0;
    }
}
```

### 2. 注册命令

在 `cli-entry/src/main/java/com/jcli/cli/Main.java` 中注册：

```java
static {
    // 现有命令...

    registerCommand("example", new ExampleCommand(), "Example command");
}
```

### 3. 添加测试

在相应模块的 `src/test/java` 目录下创建测试：

```java
package com.jcli.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleCommandTest {
    @Test
    void testExecute() {
        ExampleCommand cmd = new ExampleCommand();
        int result = cmd.execute(new String[]{});
        assertEquals(0, result);
    }
}
```

## 开发插件

### 插件结构

1. 创建独立的 Maven/Gradle 项目
2. 实现 `CliCommand` 接口
3. 创建服务注册文件

### 服务注册

在 `src/main/resources/META-INF/services/com.jcli.core.CliCommand` 文件中：

```
com.example.myplugin.MyCommand
```

### 插件构建和安装

```bash
# 构建插件
mvn clean package

# 复制到插件目录
cp target/my-plugin-1.0.0.jar ~/.jcli/plugins/
```

## 提交 Pull Request

### 分支策略

1. Fork 仓库
2. 创建功能分支：`git checkout -b feature/my-feature`
3. 提交更改：`git commit -m "Add my feature"`
4. 推送到分支：`git push origin feature/my-feature`
5. 创建 Pull Request

### 提交信息格式

遵循 Conventional Commits：

```
feat: add file grep command
fix: resolve directory traversal issue
docs: update README
test: add unit tests for file operations
refactor: improve command parsing logic
```

### PR 检查清单

- [ ] 代码通过所有测试
- [ ] 添加了必要的测试
- [ ] 更新了文档
- [ ] 遵循代码风格指南
- [ ] 提交信息清晰明确

## 测试

### 运行测试

```bash
# Maven
mvn test

# Gradle
./gradlew test
```

### 测试覆盖率

```bash
# Maven
mvn jacoco:report

# Gradle
./gradlew jacocoTestReport
```

## 文档

- 在 `docs/` 目录下添加文档
- 更新 README.md 如果涉及用户可见的更改
- 添加 Javadoc 注释到公共 API

## 问题报告

在提交 issue 时，请提供：

- 清晰的标题和描述
- 重现步骤
- 预期行为
- 实际行为
- 环境信息（操作系统、Java 版本等）

## 行为准则

- 尊重所有贡献者
- 接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

## 获取帮助

- 查看 [文档](docs/)
- 在 [GitHub Issues](https://github.com/nisconder/jcli-toolkit/issues) 中提问
- 加入我们的讨论

## 许可证

通过贡献代码，您同意您的贡献将在 MIT 许可证下授权。

---

再次感谢您的贡献！
