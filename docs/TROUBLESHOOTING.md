# JCLI Toolkit 故障排查

## 1. VS Code 里 import 全红，但命令行编译通过

通常是 Java 语言服务缓存导致的假红。

按顺序执行：

1. Java: Clean Java Language Server Workspace
2. Java: Reload Projects
3. Developer: Reload Window

## 2. 找不到 gradlew

当前仓库未提交 Gradle Wrapper。你可以：

- 直接使用 Maven：mvn clean package
- 或在本机安装 Gradle 后执行 gradle build

## 3. Windows 正则转义问题

建议用双引号并转义反斜杠，例如：

```text
".*\\.bak"
```

## 4. file rename 执行被拒绝

出现以下情况会被拒绝：

- 目标文件名不安全（包含路径分隔符等）
- 多个源文件映射到同一个目标文件名
- 目标文件已存在且不属于本次重命名源文件集合

建议先使用 --dry-run 预览。

## 5. sync --delete 风险

使用 --delete 会删除目标目录多余文件，建议流程：

1. 先执行一次 --dry-run
2. 人工确认操作清单
3. 再加 --confirm 进行实际执行
