# JCLI Toolkit

轻量、易上手的 Java 命令行工具。

你可以用它快速完成：

- 文件操作：查找、统计、搜索、重命名、同步、对比
- 代码生成：类、项目脚手架、代码片段、模板管理

## 30 秒上手

```bash
git clone https://github.com/nisconder/jcli-toolkit.git
cd jcli-toolkit
mvn clean package
java -jar cli-entry/target/cli-entry-1.0.0.jar --help
```

## 常用命令

```bash
# 查找 Java 文件
jcli file find --dir ./src --ext java

# 代码内容搜索
jcli file grep --dir ./src --pattern "TODO|FIXME"

# 批量重命名（先预览）
jcli file rename --dir ./logs --pattern ".*\\.bak" --replace ".log" --dry-run

# 生成类
jcli gen class --name User --package com.demo --template pojo
```

## 文档导航

- 用户指南（安装、运行、场景）：[docs/USER-GUIDE.md](docs/USER-GUIDE.md)
- 命令参考（全部参数与示例）：[docs/COMMAND-REFERENCE.md](docs/COMMAND-REFERENCE.md)
- 故障排查（IDE 假红、转义、常见错误）：[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

## 环境要求

- JDK 17+
- Maven 3.8+（推荐）
- Gradle 8+（可选）

说明：当前仓库未提交 Gradle Wrapper（gradlew/gradlew.bat），若无 Gradle 请直接使用 Maven。

## 贡献与许可证

- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)
- 许可证：[MIT License](LICENSE)
