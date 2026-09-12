---
name: "mvn-target-unlock"
description: "检测并终止占用 Maven target 目录 jar 的 Java 进程（如 java -jar 启动的 core-api dev 服务）。当 mvn clean 报 Failed to delete target/*.jar，或执行 mvn clean 前需要排查文件锁时调用。"
---

# Maven target 占用解锁（mvn clean 前置处理）

## 触发场景

- 用户执行 `mvn clean` 失败，日志出现 `Failed to delete ...\target\xxx.jar` 或 `Failed to clean project`
- 用户要求执行 `mvn clean` 前排查占用
- 用户提到 jar 被占用、target 无法删除

## 根因

用 `java -jar <模块>/target/xxx-exec.jar` 启动的 dev 服务仍在运行，Windows 锁定该 jar，maven-clean-plugin 无法删除 target 目录。本项目（AI-Ready backend，根目录 `I:\AI-Ready\backend`）该问题反复出现。

## 处理流程

### 第 1 步：检测占用进程

**必须把 PowerShell 写入 `.ps1` 文件再用 `-File` 执行**。不要用 `powershell -Command "..."` 内联带 `$` 变量的脚本——沙箱会转义失败（变量名被吞、报 Missing variable name）。

脚本模板：

```powershell
# 1) 引用项目 target 路径的 java 进程（候选占用者）
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*/target/*' } |
  Select-Object ProcessId, CreationDate, CommandLine |
  Format-List

# 2) 正在运行的 Maven 构建（不能打断）
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*classworlds.launcher.Launcher*' } |
  Select-Object ProcessId, CreationDate, CommandLine |
  Format-List
```

执行：`powershell -ExecutionPolicy Bypass -File <脚本绝对路径>`

### 第 2 步：精确分类（关键，避免误杀）

| 进程特征 | 处理方式 |
|---------|---------|
| 命令行含 `<模块>/target/` 的 `-jar` 进程 | 锁定 target，**需要终止** |
| jar 在 Temp 等非 target 目录（如 `C:/Users/.../Temp/core-api-xxx.jar`、相对路径不带 target 的 `core-api-xxx-run.jar`） | **不终止**，不影响 clean |
| 命令行含 `classworlds.launcher.Launcher`（正在进行的 mvn install/package） | **不终止**，等待其结束后再 clean |
| redhat.java / jdt.ls 语言服务器（命令行含 `jdt.ls`、`equinox.launcher`） | **绝不终止** |

注意：每次 `java -jar` 启动通常产生**成对的两个进程**（Oracle javapath 一个、JDK 17 一个，启动时间相同），需成对终止。

### 第 3 步：征得用户同意

用 AskUserQuestion 列出：PID、启动时间、jar 名称、`--server.port` 端口、profile。提供选项：

1. 终止并自动执行 `mvn clean`
2. 仅终止，用户手动重试（常见偏好）
3. 不终止

若有 Maven 构建正在运行，优先提供"等待构建完成再处理"选项；等待用后台轮询脚本（`Get-Process -Id <pid>`，每 10 秒一次，设 8 分钟超时）。

### 第 4 步：终止并验证

```powershell
Stop-Process -Id <pid列表> -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 800
# 复查：再次执行第 1 步的查询脚本，无残留即成功
# target 目录若已不存在，同样说明锁已释放
```

### 第 5 步：执行 mvn clean（仅当用户选择自动执行）

- 用 Shell 工具的 `cwd` 参数指定 `I:\AI-Ready\backend`（多模块项目必须在父 pom 根目录）
- 命令只写 `mvn clean`，**不要用 `&&` 链式写法**（PowerShell 不兼容）
- 拿到 BUILD SUCCESS 后立即结束，不要重复执行

## 常用路径

- 高频出问题模块：`core/api/core-api/target/core-api-<版本>-exec.jar`
- 临时脚本目录：`C:\Users\Administrator\AppData\Local\Temp\*.ps1`
