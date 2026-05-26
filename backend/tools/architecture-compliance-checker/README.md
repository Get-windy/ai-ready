# 架构合规性自动化检查工具

## 项目概述

### 项目背景
根据ERP系统架构现状评估报告（2026-05-04）和紧急制动教训，缺乏自动化检查工具导致架构规范无法有效执行。手工检查效率低、易出错，需要开发自动化工具来确保架构治理落地。

### 项目目标
开发一个自动化检查工具，实现以下架构规范的自动化合规性检查：
- 目录结构合规性
- 包命名规范
- API路径规范
- 代码质量规范

### 核心价值
1. **提高效率**：自动化检查替代手工检查
2. **减少错误**：避免人为遗漏和错误
3. **统一标准**：确保团队遵守统一架构规范
4. **快速反馈**：开发过程中及时发现和修复问题

## 功能特性

### 核心检查功能
- ✅ **目录结构检查**：验证模块目录位置、命名、层次结构
- ✅ **包命名检查**：检查Java包名格式、冲突、层次结构
- ✅ **API路径检查**：验证REST API路径规范、版本管理
- ✅ **代码规范检查**：集成现有代码检查工具，自定义规则

### 集成能力
- ✅ **命令行接口**：独立运行，快速检查
- ✅ **构建工具集成**：Maven/Gradle插件
- ✅ **IDE集成**：IntelliJ/VS Code插件，实时检查
- ✅ **CI/CD集成**：Jenkins/GitHub Actions流水线集成
- ✅ **版本控制集成**：Git pre-commit hook

### 报告输出
- ✅ **多格式报告**：JSON、HTML、Markdown、PDF
- ✅ **详细诊断**：问题定位、原因分析、修复建议
- ✅ **趋势分析**：历史数据对比，改进趋势分析
- ✅ **团队报告**：团队级汇总报告，问题分布分析

## 快速开始

### 安装方式

#### 方式一：命令行安装
```bash
# 下载最新版本
curl -L https://github.com/aiedge/architecture-checker/releases/latest/download/architecture-checker.jar -o architecture-checker.jar

# 运行检查
java -jar architecture-checker.jar check --project-path ./my-project
```

#### 方式二：Maven插件
```xml
<plugin>
    <groupId>cn.aiedge.architecture</groupId>
    <artifactId>architecture-checker-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 方式三：Docker容器
```bash
docker run -v $(pwd):/project aiedge/architecture-checker:latest check /project
```

### 基本使用

#### 简单检查
```bash
# 检查整个项目
architecture-checker check --project ./my-project

# 检查特定目录
architecture-checker check --directory ./my-project/src/main/java

# 检查特定类型
architecture-checker check --type directory,package --project ./my-project
```

#### 生成报告
```bash
# HTML报告
architecture-checker report --format html --output compliance-report.html

# JSON报告（机器可读）
architecture-checker report --format json --output compliance-report.json

# Markdown报告
architecture-checker report --format markdown --output README-compliance.md
```

#### 自动修复
```bash
# 查看修复建议
architecture-checker fix --dry-run --project ./my-project

# 自动修复（部分）
architecture-checker fix --auto-fix --project ./my-project
```

## 配置说明

### 配置文件示例
创建 `.architecture-checker.yaml` 文件：

```yaml
# 基础配置
project:
  name: "my-erp-project"
  language: "java"
  version: "1.0.0"

# 检查规则配置
rules:
  # 目录结构规则
  directory:
    enabled: true
    strict-mode: true
    allowed-modules:
      - core
      - erp
      - ai
      - tests
    disallowed-locations:
      - root-directory
  
  # 包命名规则
  package:
    enabled: true
    base-package: "cn.aiedge"
    module-pattern: "[a-z-]+"
    submodule-pattern: "[a-z-]+"
  
  # API路径规则
  api:
    enabled: true
    base-path: "/api"
    version-pattern: "v[1-9][0-9]*"
    resource-pattern: "[a-z-]+"
    action-pattern: "[a-z-]+"

# 输出配置
output:
  format: "html"
  directory: "./reports"
  filename: "architecture-compliance-report"
  include-summary: true
  include-details: true
  include-suggestions: true
```

### 规则自定义

#### 自定义目录结构规则
```yaml
custom-rules:
  - id: custom-directory-001
    name: "业务模块目录规则"
    description: "ERP业务模块必须在erp/目录下"
    type: "directory-location"
    pattern:
      must-match: "backend/erp/.*"
      must-not-match: "backend/(?!erp/).*"
    severity: "ERROR"
    message: "ERP业务模块必须在erp/目录下"
    suggestion: "将模块移动到backend/erp/目录"
```

#### 自定义包命名规则
```yaml
custom-rules:
  - id: custom-package-001
    name: "服务接口命名规则"
    description: "服务接口必须以I开头"
    type: "java-class"
    pattern:
      class-name: "I[A-Z][a-zA-Z0-9]*"
      package-suffix: "service"
    severity: "WARNING"
    message: "服务接口必须以I开头"
    suggestion: "将类名改为I开头，如IUserService"
```

## 集成指南

### IDE集成

#### IntelliJ IDEA插件
1. 安装插件：Preferences → Plugins → Marketplace
2. 搜索 "Architecture Compliance Checker"
3. 安装并重启IDE
4. 配置检查规则：Preferences → Tools → Architecture Checker

#### VS Code扩展
```json
{
  "extensions": [
    {
      "identifier": "aiedge.architecture-checker",
      "version": "1.0.0"
    }
  ],
  "settings": {
    "architectureChecker.enable": true,
    "architectureChecker.rulesFile": ".architecture-checker.yaml"
  }
}
```

### CI/CD集成

#### GitHub Actions
```yaml
name: Architecture Compliance Check
on: [push, pull_request]

jobs:
  compliance-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Architecture Check
        uses: aiedge/architecture-checker-action@v1
        with:
          project-path: ./
          output-format: html
          fail-on-error: true
```

#### Jenkins Pipeline
```groovy
pipeline {
    agent any
    stages {
        stage('Architecture Check') {
            steps {
                sh 'architecture-checker check --project . --fail-on-error'
                archiveArtifacts 'architecture-compliance-report.html'
            }
        }
    }
}
```

### 构建工具集成

#### Maven插件配置
```xml
<build>
    <plugins>
        <plugin>
            <groupId>cn.aiedge.architecture</groupId>
            <artifactId>architecture-checker-maven-plugin</artifactId>
            <version>1.0.0</version>
            <configuration>
                <projectPath>${project.basedir}</projectPath>
                <outputFormat>html</outputFormat>
                <failOnError>true</failOnError>
                <rulesFile>.architecture-checker.yaml</rulesFile>
            </configuration>
            <executions>
                <execution>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### Gradle插件配置
```groovy
plugins {
    id 'cn.aiedge.architecture.checker' version '1.0.0'
}

architectureChecker {
    projectPath = projectDir
    outputFormat = 'html'
    failOnError = true
    rulesFile = '.architecture-checker.yaml'
}
```

## 检查规则参考

### 内置规则

#### 目录结构规则
| 规则ID | 规则名称 | 描述 | 严重级别 |
|--------|----------|------|----------|
| DIR-001 | 模块目录位置 | 检查模块目录是否在正确位置 | ERROR |
| DIR-002 | 目录命名规范 | 检查目录命名是否符合规范 | WARNING |
| DIR-003 | 目录层次结构 | 检查目录层次结构是否正确 | ERROR |
| DIR-004 | 重复目录检测 | 检测重复或冲突的目录 | ERROR |

#### 包命名规则
| 规则ID | 规则名称 | 描述 | 严重级别 |
|--------|----------|------|----------|
| PKG-001 | Java包名格式 | 检查Java包名格式 | ERROR |
| PKG-002 | 包名层次结构 | 检查包名层次结构 | WARNING |
| PKG-003 | 包名冲突检测 | 检测包名冲突 | ERROR |
| PKG-004 | 保留包名检查 | 检查是否使用保留包名 | ERROR |

#### API路径规则
| 规则ID | 规则名称 | 描述 | 严重级别 |
|--------|----------|------|----------|
| API-001 | REST路径格式 | 检查REST API路径格式 | ERROR |
| API-002 | 版本管理规范 | 检查API版本管理 | WARNING |
| API-003 | 资源命名规范 | 检查资源命名 | WARNING |
| API-004 | 路径一致性 | 检查API路径一致性 | ERROR |

### 自定义规则开发

#### 规则开发示例
```java
// 自定义目录检查规则
public class CustomDirectoryRule implements CheckRule {
    
    @Override
    public String getId() {
        return "custom-directory-001";
    }
    
    @Override
    public String getName() {
        return "自定义目录规则";
    }
    
    @Override
    public CheckResult check(ProjectContext context) {
        // 实现检查逻辑
        List<Issue> issues = new ArrayList<>();
        
        // 检查目录逻辑...
        
        return CheckResult.builder()
            .ruleId(getId())
            .issues(issues)
            .build();
    }
}
```

#### 规则注册
```java
// SPI注册
@AutoService(CheckRule.class)
public class CustomDirectoryRule implements CheckRule {
    // 实现...
}
```

## 故障排除

### 常见问题

#### Q1: 检查速度慢
**原因**: 代码库过大，检查规则复杂
**解决**:
1. 启用缓存：`--enable-cache`
2. 增量检查：`--incremental`
3. 并行检查：`--parallel 4`
4. 选择性检查：只检查重要规则

#### Q2: 误报太多
**原因**: 规则过于严格
**解决**:
1. 调整规则严重级别
2. 添加例外配置
3. 优化规则逻辑
4. 使用自定义规则覆盖

#### Q3: 集成问题
**原因**: 环境配置问题
**解决**:
1. 检查环境变量
2. 验证配置文件
3. 查看日志输出
4. 更新工具版本

### 调试模式
```bash
# 启用详细日志
architecture-checker check --verbose --debug --project ./my-project

# 生成诊断报告
architecture-checker diagnose --output diagnostic-report.zip
```

## 项目贡献

### 开发环境搭建
```bash
# 克隆项目
git clone https://github.com/aiedge/architecture-checker.git

# 安装依赖
cd architecture-checker
mvn clean install

# 运行测试
mvn test

# 构建项目
mvn package
```

### 代码结构
```
architecture-checker/
├── checker-core/          # 核心检查引擎
├── checker-plugins/       # 插件系统
├── checker-cli/          # 命令行接口
├── checker-api/          # REST API接口
├── checker-integration/  # 集成模块
├── checker-ui/          # Web界面
└── checker-docs/        # 文档
```

### 贡献指南
1. Fork项目仓库
2. 创建特性分支：`git checkout -b feature/new-rule`
3. 提交更改：`git commit -am 'Add new rule'`
4. 推送分支：`git push origin feature/new-rule`
5. 创建Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证。

## 支持与联系

### 文档资源
- [详细文档](https://docs.aiedge.cn/architecture-checker)
- [API参考](https://api.aiedge.cn/architecture-checker)
- [示例项目](https://github.com/aiedge/architecture-checker-examples)

### 社区支持
- [GitHub Issues](https://github.com/aiedge/architecture-checker/issues)
- [Discord频道](https://discord.gg/aiedge)
- [邮件列表](architecture-checker@aiedge.cn)

### 商业支持
如需商业支持或定制开发，请联系：support@aiedge.cn

---

**版本**: v0.1.0  
**最后更新**: 2026年5月5日  
**状态**: 开发中