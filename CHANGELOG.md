# AI-Ready 变更日志

**项目**: 智企连·AI-Ready  
**维护者**: AI-Ready Team  
**格式规范**: [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)  
**版本规范**: [SemVer](https://semver.org/lang/zh-CN/)

---

## 目录

- [未发布](#未发布)
- [0.1.0] - 2026-04-03
- [版本规范说明](#版本规范说明)
- [维护流程](#维护流程)

---

## [未发布]

### 新增 (Added)
- 待添加新功能

### 变更 (Changed)
- 待变更内容

### 修复 (Fixed)
- 待修复问题

### 移除 (Removed)
- 待移除功能

---

## [0.1.0] - 2026-04-03

### 新增 (Added)

#### 核心功能
- **项目基础结构**: 初始化项目结构，添加PostgreSQL数据库配置
- **ERP模块骨架**: 创建ERP模块基础结构和核心功能
- **CRM模块骨架**: 创建CRM模块基础结构
- **客户管理模块**: 新建CRM客户管理模块，实现CustomerController解决404问题

#### 模块架构
- **ai-ready-core**: 核心模块，包含通用工具和基础配置
- **ai-ready-erp**: ERP业务模块
- **ai-ready-crm**: CRM业务模块
- **crm-customer**: CRM客户管理子模块

#### 数据库
- PostgreSQL数据库配置和连接池
- 基础数据表结构

### 文档 (Documentation)

#### 开发规范文档
- Java代码规范 (docs/standards/JAVA_CODE_STANDARDS.md)
- 前端代码规范 (docs/standards/FRONTEND_CODE_STANDARDS.md)
- Git提交规范 (docs/standards/GIT_COMMIT_STANDARDS.md)
- PR审核规范 (docs/standards/PR_REVIEW_STANDARDS.md)

#### 项目文档
- 项目初始化报告 (docs/INIT_REPORT.md)
- 项目方案文档 (docs/AI-Ready项目方案.md)

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 后端开发语言 |
| Spring Boot | 3.x | 应用框架 |
| PostgreSQL | 15+ | 主数据库 |
| MyBatis Plus | 3.5+ | ORM框架 |
| Sa-Token | 1.37+ | 认证授权 |

---

## [0.0.1] - 2026-03-25

### 新增 (Added)
- **项目初始化**: 完成项目骨架创建
- **基础配置**: Maven多模块项目结构
- **方案文档**: 智企连·AI-Ready项目方案原始文档

---

## 版本规范说明

本项目采用 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

### 版本格式

```
主版本号.次版本号.修订号
MAJOR.MINOR.PATCH
```

### 版本递增规则

| 类型 | 说明 | 示例 |
|------|------|------|
| **MAJOR** | 不兼容的API修改 | 1.0.0 → 2.0.0 |
| **MINOR** | 向下兼容的功能性新增 | 1.0.0 → 1.1.0 |
| **PATCH** | 向下兼容的问题修复 | 1.0.0 → 1.0.1 |

### 预发布版本

```
1.0.0-alpha.1  # 内部测试版
1.0.0-beta.1   # 公开测试版
1.0.0-rc.1     # 发布候选版
```

---

## 维护流程

### 1. 变更记录原则

变更日志记录以下类型的变更：

| 类型 | 关键词 | 说明 |
|------|--------|------|
| **新增** | `feat`, `add` | 新功能、新特性 |
| **变更** | `change`, `update` | 现有功能的变更 |
| **修复** | `fix`, `bugfix` | Bug修复 |
| **移除** | `remove`, `deprecate` | 移除的功能 |
| **安全** | `security` | 安全相关修复 |
| **文档** | `docs` | 文档更新 |

### 2. Git提交规范

提交信息格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**:
- `feat`: 新功能
- `fix`: 修复Bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具变动

**示例**:
```
feat(customer): 添加客户管理API

- 创建CustomerController提供基础客户管理API
- 添加crm-customer模块pom.xml
- 在父pom.xml中注册crm-customer模块

Closes #123
```

### 3. 发布流程

```
1. 开发完成 → 更新CHANGELOG.md
2. 代码审查 → 合并到main分支
3. 创建Tag → git tag -a v1.0.0 -m "Release v1.0.0"
4. 推送Tag → git push origin v1.0.0
5. 构建发布 → CI/CD自动构建部署
```

### 4. 变更日志更新时机

| 时机 | 操作 |
|------|------|
| 功能开发完成 | 添加到[未发布]区域 |
| 版本发布 | 创建新版本号区域 |
| Bug修复 | 更新对应版本的修复内容 |
| 文档更新 | 更新对应版本的文档内容 |

---

## 版本历史

| 版本 | 发布日期 | 主要变更 |
|------|----------|----------|
| 0.1.0 | 2026-04-03 | ERP/CRM模块骨架、客户管理模块 |
| 0.0.1 | 2026-03-25 | 项目初始化 |

---

## 贡献指南

### 如何贡献变更日志

1. **开发新功能时**：在[未发布]区域添加`新增`条目
2. **修复Bug时**：在[未发布]区域添加`修复`条目
3. **发布版本时**：
   - 将[未发布]内容移动到新版本号区域
   - 添加发布日期
   - 清空[未发布]区域

### 变更描述规范

- 使用简洁明了的描述
- 说明变更的影响范围
- 关联相关的Issue/PR编号
- 破坏性变更需特别标注

**示例**:
```markdown
### 新增 (Added)
- 添加客户管理API，支持CRUD操作 (#123)

### 修复 (Fixed)
- 修复登录页面在Safari浏览器的兼容性问题 (#124)

### 破坏性变更 (Breaking Changes)
- 重构用户API，移除废弃的`/api/v1/user/info`接口，请使用`/api/v1/users/me`
```

---

## 相关文档

- [项目README](./README.md)
- [开发规范](./docs/standards/)
- [API文档](./docs/api/)
- [部署指南](./docs/deployment/)

---

## 联系方式

- **技术支持**: support@aiedge.cn
- **问题反馈**: https://github.com/your-repo/ai-ready/issues
- **文档维护**: doc-writer

---

**文档维护**: doc-writer  
**创建日期**: 2026-04-03  
**最后更新**: 2026-04-03  
**版本**: v1.0
