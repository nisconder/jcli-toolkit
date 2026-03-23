# JCLI Toolkit 命令参考

本文档提供完整命令参数说明。

## 1. file 命令组

### 1.1 file find

用途：递归查找文件，支持扩展名、名称、体积、修改时间过滤。

参数：

- -d, --dir <path>：搜索目录，默认 .
- -e, --ext <ext>：按扩展名过滤（如 java）
- -n, --name <pattern>：文件名模式，支持 * 和 ?
- -s, --size-gt <size>：大于指定大小，支持 k/m/g 后缀
- -z, --size-lt <size>：小于指定大小，支持 k/m/g 后缀
- -t, --newer <expr>：仅返回较新文件，支持时间戳或 7d 形式
- -o, --output <list|json|csv>：输出格式，默认 list
- -p, --depth <n>：最大递归深度
- -v, --verbose：详细日志

示例：

```bash
jcli file find --dir ./src --ext java --name "*Command*" --output list
jcli file find --dir ./logs --size-gt 10m --newer 7d --output csv
```

### 1.2 file stat

用途：统计目录下文件数、总大小、扩展名分布。

参数：

- -d, --dir <path>：目标目录，默认 .
- -o, --output <table|json>：输出格式，默认 table
- -r, --recursive：递归统计子目录

示例：

```bash
jcli file stat --dir ./src --recursive
jcli file stat --dir ./src --output json
```

### 1.3 file grep

用途：按正则搜索文件内容，支持上下文和扩展名过滤。

参数：

- -p, --pattern <regex>：必填，匹配正则
- -d, --dir <path>：搜索目录，默认 .
- -e, --ext <ext>：扩展名过滤
- -i, --ignore-case：忽略大小写
- -c, --context <n>：输出前后上下文行数，默认 0
- -x, --exclude <regex>：排除路径正则
- -v, --verbose：详细日志

示例：

```bash
jcli file grep --dir ./src --ext java --pattern "TODO|FIXME"
jcli file grep --dir ./src --pattern "class\\s+Main" --context 2 --ignore-case
```

### 1.4 file rename

用途：批量重命名文件，支持替换、前后缀、序号、大小写转换。

参数：

- -d, --dir <path>：目标目录，默认 .
- -p, --pattern <regex>：必填，匹配正则
- -r, --replace <text>：替换文本，默认空
- -x, --prefix <text>：添加前缀
- -s, --suffix <text>：添加后缀（保留扩展名位置）
- -q, --seq：追加序号
- -n, --seq-start <int>：序号起始值，默认 1
- -f, --format <fmt>：序号格式，默认 %d，可用 %02d
- -u, --uppercase：转大写
- -l, --lowercase：转小写
- -y, --dry-run：仅预览，不执行
- -c, --confirm：执行前确认
- -v, --verbose：详细日志

安全保护：

- 危险目标名拦截（防路径注入）
- 目标冲突拦截
- 默认拒绝覆盖非本批次目标文件

示例：

```bash
jcli file rename --dir ./logs --pattern ".*\\.bak" --replace ".log" --dry-run
jcli file rename --dir ./images --pattern "(.*)" --replace "$1" --prefix "img_" --seq --format "%03d" --confirm
```

### 1.5 file sync

用途：把 source 目录同步到 target 目录。

参数：

- -s, --source <path>：源目录（必填）
- -t, --target <path>：目标目录（必填）
- -x, --exclude <regex>：排除路径正则
- -d, --delete：删除目标目录中多余文件
- -y, --dry-run：仅预览，不执行
- -c, --confirm：执行前确认
- -v, --verbose：详细日志

示例：

```bash
jcli file sync --source ./src --target ./backup --dry-run
jcli file sync --source ./src --target ./backup --delete --confirm
```

### 1.6 file diff

用途：比较两个目录中的文件差异。

参数：

- --dir1 <path>：目录 1（必填）
- --dir2 <path>：目录 2（必填）
- -x, --exclude <regex>：排除路径正则

示例：

```bash
jcli file diff --dir1 ./src --dir2 ./backup
jcli file diff --dir1 ./v1 --dir2 ./v2 --exclude ".*\\.class"
```

## 2. gen 命令组

### 2.1 gen class

用途：生成 Java 类文件（pojo/service/repository/controller/builder/singleton）。

参数：

- -n, --name <ClassName>：类名（必填）
- -p, --package <pkg>：包名，默认 com.example
- -t, --template <type>：模板，默认 pojo
- -o, --out <dir>：输出目录，默认当前目录
- -f, --fields <defs>：字段列表，格式 name:type,name2:type2
- -a, --author <name>：作者名注释
- -v, --verbose：详细日志

示例：

```bash
jcli gen class --name User --package com.demo --template pojo --fields "id:Long,name:String"
jcli gen class --name UserService --template service --out ./generated
```

### 2.2 gen project

用途：生成项目目录结构。

模板：plain-java、maven-library、gradle-library、cli-app

参数：

- -n, --name <project>：项目名（必填）
- -t, --template <type>：模板，默认 plain-java
- -g, --group <groupId>：默认 com.example
- -v, --version <version>：默认 1.0.0
- -o, --out <dir>：输出目录，默认当前目录
- --desc <text>：项目描述
- --verbose：详细日志

示例：

```bash
jcli gen project --name demo-app --template maven-library --group com.demo --version 1.0.0
jcli gen project --name quick-cli --template cli-app --out ./sandbox --desc "CLI starter project"
```

### 2.3 gen snippet

用途：生成常见代码片段。

支持类型：getter-setter、equals-hashcode、builder、logger、try-with-resources

参数：

- -t, --type <type>：片段类型（必填）
- -f, --fields <defs>：字段列表（部分类型可选）
- -c, --clipboard：尝试复制到剪贴板

示例：

```bash
jcli gen snippet --type getter-setter --fields "id:Long,name:String"
jcli gen snippet --type try-with-resources
```

## 3. template 命令

用途：模板列表与管理。

子命令：

- list
- add <path>
- remove <name>

示例：

```bash
jcli template list
jcli template add ./my-template
jcli template remove my-template
```
