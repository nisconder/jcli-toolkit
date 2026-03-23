# JCLI Toolkit 用户指南

本指南面向普通使用者，帮助你从安装到实际使用快速上手。

## 1. 环境要求

- JDK 17 或更高版本
- Maven 3.8+（推荐）
- Gradle 8+（可选）

说明：当前仓库未提交 gradlew/gradlew.bat 包装器，若本机无 Gradle，请优先使用 Maven。

## 2. 安装与运行

### 2.1 获取代码

```bash
git clone https://github.com/nisconder/jcli-toolkit.git
cd jcli-toolkit
```

### 2.2 Maven 构建并运行

```bash
mvn clean package
java -jar cli-entry/target/cli-entry-1.0.0.jar --help
```

如果本地产物名略有差异，请查看 `cli-entry/target` 目录，选择带主类清单的 JAR。

### 2.3 Gradle 构建（可选）

```bash
gradle clean build
```

## 3. 全局用法

基础格式：

```text
jcli <command> [subcommand] [options]
```

全局参数：

- -h, --help：显示帮助
- -V, --version：显示版本
- -v, --verbose：开启详细日志
- -q, --quiet：静默模式

## 4. 常用场景

### 4.1 查找并统计 Java 文件

```bash
jcli file find --dir ./src --ext java
jcli file stat --dir ./src --recursive
```

### 4.2 批量重命名（先预览）

```bash
jcli file rename --dir ./logs --pattern ".*\\.bak" --replace ".log" --dry-run
jcli file rename --dir ./logs --pattern ".*\\.bak" --replace ".log" --confirm
```

### 4.3 代码内容搜索

```bash
jcli file grep --dir ./src --ext java --pattern "TODO|FIXME"
```

### 4.4 生成类和项目

```bash
jcli gen class --name User --package com.demo --template pojo --fields "id:Long,name:String"
jcli gen project --name demo-app --template maven-library --group com.demo
```

## 5. 返回码说明

- 0：成功（含 help、dry-run、无匹配等可接受结果）
- 1：参数错误、路径不存在、业务检查失败、部分操作失败
- 2：运行时异常（入口 Main 捕获异常后返回）

## 6. 安全建议

- 文件写操作建议先加 --dry-run
- 大规模改名或同步前先在测试目录验证
- 使用 file sync --delete 前先 dry-run 并人工确认
- 生产目录操作前建议先备份

## 7. 下一步

- 完整命令参数与示例请查看 [命令参考](COMMAND-REFERENCE.md)
- 运行异常与 IDE 假红请查看 [故障排查](TROUBLESHOOTING.md)
