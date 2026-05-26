# 架构合规性自动化检查工具设计概述

## 1. 项目背景

### 1.1 问题现状
根据ERP系统架构现状评估报告（2026-05-04），发现以下问题：

1. **架构治理缺失**：缺乏自动化工具，手工检查效率低、易出错
2. **紧急制动教训**：重复模块、目录结构混乱等问题未能及时发现
3. **技术债务积累**：31项技术债务，115人天解决工作量

### 1.2 项目目标
开发自动化检查工具，实现架构规范的自动化合规性检查，确保架构治理能够有效落地执行。

## 2. 需求分析

### 2.1 检查范围

#### 2.1.1 目录结构检查
- **检查项**：
  - 模块目录位置是否符合规范
  - 检测重复或冲突的目录
  - 验证目录命名符合约定
  - 检查目录层次结构正确性
- **依据**：`AGENTS.md`中的目录规范

#### 2.1.2 包命名检查
- **检查项**：
  - Java包名是否符合规范格式
  - 检测包名冲突和命名违规
  - 验证包名层次结构正确性
  - 检查包名与模块对应关系
- **依据**：项目约定的包命名规范

#### 2.1.3 API路径检查
- **检查项**：
  - REST API路径是否符合规范
  - 验证API版本管理符合约定
  - 检测资源命名符合RESTful规范
  - 检查API路径一致性
- **依据**：API设计规范文档

#### 2.1.4 代码规范检查
- **检查项**：
  - 集成现有代码规范检查工具
  - 自定义项目特定代码规范检查
  - 代码复杂度、重复度等质量检查
  - 测试覆盖率检查
- **依据**：代码规范文档

### 2.2 用户场景

#### 2.2.1 本地开发检查
- **场景**：开发人员在提交代码前运行检查
- **需求**：快速反馈，提供修复建议
- **集成**：Git pre-commit hook

#### 2.2.2 CI/CD流水线检查
- **场景**：代码提交后自动运行检查
- **需求**：生成详细报告，阻断违规提交
- **集成**：Jenkins/GitHub Actions

#### 2.2.3 架构评审检查
- **场景**：架构师定期检查项目合规性
- **需求**：生成整体报告，识别风险点
- **集成**：定期扫描，邮件通知

## 3. 架构设计

### 3.1 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                  架构合规性检查工具                          │
├─────────────────────────────────────────────────────────────┤
│                      API网关层                              │
│                ┌─────────┴─────────┐                      │
│          CLI接口            Web接口                       │
├─────────────────────────────────────────────────────────────┤
│                      服务层                                 │
│      ┌─────────┬─────────┬─────────┬─────────┐            │
│   目录检查   包检查   API检查   代码检查  结果处理         │
├─────────────────────────────────────────────────────────────┤
│                      引擎层                                 │
│      ┌─────────┬─────────┬─────────┬─────────┐            │
│  文件解析   规则引擎  插件管理  缓存管理  报告生成         │
├─────────────────────────────────────────────────────────────┤
│                      数据层                                 │
│      ┌─────────┬─────────┬─────────┬─────────┐            │
│   规则配置   结果存储  历史记录  模板库   日志库           │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件设计

#### 3.2.1 规则引擎
- **功能**：管理检查规则，执行规则检查
- **特性**：
  - 支持规则优先级
  - 支持规则组合
  - 支持条件规则
  - 支持自定义规则

#### 3.2.2 插件系统
- **功能**：支持扩展检查能力
- **接口**：
  - 规则插件：新增检查规则
  - 解析插件：支持新语言解析
  - 输出插件：支持新报告格式
  - 集成插件：与第三方工具集成

#### 3.2.3 缓存管理
- **功能**：提升检查性能
- **策略**：
  - 文件内容哈希缓存
  - 解析结果缓存
  - 检查结果缓存
  - 缓存失效机制

### 3.3 技术选型方案

#### 方案A：Java技术栈
- **优势**：
  - 与现有ERP技术栈一致
  - 团队熟悉，开发效率高
  - 现有Java生态工具丰富
- **技术栈**：
  - 框架：Spring Boot
  - 解析：JavaParser、ANTLR
  - 构建：Maven/Gradle
  - 测试：JUnit、Mockito

#### 方案B：Python技术栈
- **优势**：
  - 开发速度快
  - 静态分析库丰富（AST、pylint等）
  - 脚本化，易于集成
- **技术栈**：
  - 框架：FastAPI（Web）、Click（CLI）
  - 解析：ast、libcst
  - 构建：poetry/pipenv
  - 测试：pytest

#### 方案C：混合技术栈
- **优势**：
  - 核心引擎用Java，保证性能
  - 插件系统用Python，灵活扩展
  - 兼顾性能和灵活性
- **实现**：
  - Jython集成
  - gRPC通信
  - 微服务架构

### 3.4 接口设计

#### 3.4.1 REST API接口
```java
@RestController
@RequestMapping("/api/v1/check")
public class ComplianceCheckController {
    
    @PostMapping("/directory")
    public CheckResult checkDirectory(@RequestBody CheckRequest request);
    
    @PostMapping("/package")
    public CheckResult checkPackage(@RequestBody CheckRequest request);
    
    @PostMapping("/api-path")
    public CheckResult checkApiPath(@RequestBody CheckRequest request);
    
    @PostMapping("/code")
    public CheckResult checkCode(@RequestBody CheckRequest request);
}
```

#### 3.4.2 命令行接口
```bash
# 运行所有检查
architecture-checker check --project-path /path/to/project

# 运行特定检查
architecture-checker check --type directory --project-path /path/to/project

# 生成报告
architecture-checker report --format html --output report.html

# 修复建议
architecture-checker fix --auto-fix
```

## 4. 检查规则设计

### 4.1 目录结构规则
```yaml
rules:
  - id: directory-structure-001
    name: 模块目录位置检查
    description: 检查模块目录是否在正确位置
    severity: ERROR
    pattern: 
      type: directory-location
      allowed-locations:
        - backend/core/*
        - backend/erp/*
        - backend/ai/*
      disallowed-locations:
        - root-directory
        - backend/*/tests
```

### 4.2 包命名规则
```yaml
rules:
  - id: package-naming-001
    name: Java包名格式检查
    description: 检查Java包名是否符合规范
    severity: ERROR
    pattern:
      type: java-package
      format: "cn.aiedge.{module}.{submodule}.*"
      reserved-prefixes:
        - com.sun
        - java
        - javax
        - sun
```

### 4.3 API路径规则
```yaml
rules:
  - id: api-path-001
    name: REST API路径规范检查
    description: 检查REST API路径是否符合规范
    severity: ERROR
    pattern:
      type: api-path
      format: "/api/{version}/{resource}/{id}?"
      version-pattern: "v[1-9][0-9]*"
      resource-pattern: "[a-z-]+"
```

## 5. 集成方案

### 5.1 IDE集成
- **IntelliJ IDEA插件**：实时检查，快速修复
- **VS Code扩展**：轻量级检查，代码提示
- **Eclipse插件**：传统IDE支持

### 5.2 构建工具集成
- **Maven插件**：mvn architecture:check
- **Gradle插件**：gradle architectureCheck
- **Ant任务**：传统构建支持

### 5.3 CI/CD集成
- **Jenkins插件**：流水线集成
- **GitHub Actions**：自动检查
- **GitLab CI**：流水线集成
- **TeamCity**：构建集成

### 5.4 版本控制集成
- **Git pre-commit hook**：提交前检查
- **Git pre-receive hook**：服务器端检查
- **SVN pre-commit hook**：传统版本控制

## 6. 部署方案

### 6.1 本地部署
- **独立运行**：命令行工具
- **Docker容器**：容器化部署
- **桌面应用**：GUI界面

### 6.2 服务器部署
- **微服务架构**：独立服务
- **容器化部署**：Kubernetes
- **云服务**：SaaS服务

### 6.3 混合部署
- **本地轻量级检查**：快速反馈
- **服务器深度检查**：全面分析
- **云服务增强检查**：AI分析

## 7. 测试策略

### 7.1 单元测试
- **规则测试**：验证单个规则正确性
- **引擎测试**：验证检查引擎功能
- **插件测试**：验证插件接口

### 7.2 集成测试
- **端到端测试**：完整流程测试
- **性能测试**：大规模代码库测试
- **兼容性测试**：不同环境测试

### 7.3 验收测试
- **用户场景测试**：真实用户场景验证
- **回归测试**：确保现有功能正常
- **负载测试**：高并发场景测试

## 8. 项目计划

### 8.1 第一阶段：MVP（2周）
- **目标**：核心检查功能
- **交付**：
  - 目录结构检查
  - 包命名检查
  - 命令行接口
  - 基础报告

### 8.2 第二阶段：增强（2周）
- **目标**：完善功能和集成
- **交付**：
  - API路径检查
  - 代码质量检查
  - CI/CD集成
  - 详细报告

### 8.3 第三阶段：优化（2周）
- **目标**：性能优化和高级功能
- **交付**：
  - 缓存优化
  - 插件系统
  - IDE集成
  - AI增强检查

## 9. 风险评估与应对

### 9.1 技术风险
- **风险**：解析复杂代码结构困难
- **应对**：分阶段实现，先支持基础结构

### 9.2 性能风险
- **风险**：大规模代码库检查慢
- **应对**：实现增量检查，优化缓存

### 9.3 扩展风险
- **风险**：需求变化快，扩展困难
- **应对**：插件化架构，模块化设计

## 10. 成功标准

### 10.1 技术标准
- ✅ 支持至少4种核心检查类型
- ✅ 检查准确率 > 95%
- ✅ 性能：100万行代码 < 30秒
- ✅ 支持插件扩展

### 10.2 业务标准
- ✅ 减少架构问题发现时间 > 80%
- ✅ 架构规范遵守率提升 > 50%
- ✅ 团队满意度 > 80%

---

**设计版本**: v0.1  
**设计时间**: 2026年5月5日  
**设计人员**: 架构团队  
**状态**: 草案，待评审