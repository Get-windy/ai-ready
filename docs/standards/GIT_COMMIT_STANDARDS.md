# 智企连·AI-Ready Git 提交规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. 分支管理策略

### 1.1 分支类型

| 分支类型 | 命名规则 | 说明 | 生命周期 |
|----------|----------|------|----------|
| `main` | `main` | 生产环境代码 | 永久 |
| `develop` | `develop` | 开发主分支 | 永久 |
| `feature` | `feature/xxx` | 功能开发分支 | 临时 |
| `bugfix` | `bugfix/xxx` | Bug 修复分支 | 临时 |
| `release` | `release/vX.Y.Z` | 发布分支 | 临时 |
| `hotfix` | `hotfix/xxx` | 紧急修复分支 | 临时 |

### 1.2 分支流程图

```
main ────────●─────────●─────────●────────→
             ↑         ↑         ↑
release ────●─────────●─────────●──────→
             ↑         ↑         ↑
develop ────●──●──●──●──●──●──●──●──●──→
             ↑     ↑     ↑
feature ────●─────●─────●─────────────→
                  ↑
bugfix ──────────●──────────────────→
```

### 1.3 分支命名规范

```
feature/模块-功能描述
feature/user-login
feature/order-export

bugfix/模块-问题描述
bugfix/user-password-validation
bugfix/order-calculation

release/v版本号
release/v1.0.0
release/v1.1.0

hotfix/问题描述
hotfix/security-patch
hotfix/payment-error
```

---

## 2. Commit Message 规范

### 2.1 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 2.2 Type 类型

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(user): 添加用户注册功能` |
| `fix` | Bug 修复 | `fix(order): 修复订单金额计算错误` |
| `docs` | 文档更新 | `docs: 更新 README 安装说明` |
| `style` | 代码格式（不影响功能） | `style: 格式化代码缩进` |
| `refactor` | 重构（不是新功能或 Bug 修复） | `refactor(user): 重构用户登录逻辑` |
| `perf` | 性能优化 | `perf(order): 优化订单查询性能` |
| `test` | 添加/修改测试 | `test(user): 添加用户服务单元测试` |
| `build` | 构建系统或依赖更新 | `build: 升级 Spring Boot 到 3.2.5` |
| `ci` | CI 配置更新 | `ci: 添加 GitHub Actions 配置` |
| `chore` | 其他杂项 | `chore: 更新 .gitignore` |
| `revert` | 回滚提交 | `revert: 回滚用户注册功能` |

### 2.3 Scope 范围

按模块划分：

| Scope | 说明 |
|-------|------|
| `core` | 核心模块 |
| `user` | 用户模块 |
| `order` | 订单模块 |
| `erp` | ERP 模块 |
| `crm` | CRM 模块 |
| `api` | API 接口 |
| `ui` | 前端界面 |
| `db` | 数据库相关 |

### 2.4 Subject 规则

- 使用中文描述
- 以动词开头（添加、修复、更新、删除、优化等）
- 不超过 50 个字符
- 不以句号结尾

### 2.5 Body 规则

- 详细说明本次提交的内容
- 可以分多行
- 说明改动的原因和影响

### 2.6 Footer 规则

- **Breaking Changes**: 以 `BREAKING CHANGE:` 开头
- **关闭 Issue**: `Closes #123` 或 `Fixes #456`

### 2.7 完整示例

```
feat(user): 添加用户邮箱验证功能

- 新增邮箱验证码发送接口
- 新增邮箱验证逻辑
- 用户注册时验证邮箱格式
- 发送验证码邮件使用异步任务

Closes #123
```

```
fix(order): 修复订单金额计算错误

问题：当订单包含多个商品时，总金额计算错误
原因：循环累加时未考虑商品折扣
解决：修正折扣计算逻辑

Fixes #456
```

```
refactor(auth)!: 重构认证模块

BREAKING CHANGE: 认证接口返回格式变更，需要前端同步更新
```

---

## 3. Commit 最佳实践

### 3.1 提交粒度

```
✅ 好的提交（功能单一）
feat(user): 添加用户注册接口
feat(user): 添加邮箱验证功能
test(user): 添加用户注册测试用例

❌ 不好的提交（功能混杂）
feat(user): 添加用户注册接口、邮箱验证功能，修复登录Bug，更新样式
```

### 3.2 提交时机

- 一个逻辑变更 = 一个提交
- 提交前确保编译通过
- 提交前运行测试
- 不提交未完成的代码

### 3.3 提交检查清单

- [ ] 代码已格式化
- [ ] 无编译错误
- [ ] 无 Lint 警告
- [ ] 单元测试通过
- [ ] Commit Message 符合规范

---

## 4. Commitlint 配置

### 4.1 安装依赖

```bash
npm install -D @commitlint/cli @commitlint/config-conventional
```

### 4.2 配置文件

```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'docs', 'style', 'refactor', 'perf', 'test', 'build', 'ci', 'chore', 'revert']
    ],
    'subject-full-stop': [2, 'never', '.'],
    'subject-case': [0],
    'header-max-length': [2, 'always', 100]
  }
}
```

### 4.3 Git Hooks 集成

```bash
# 安装 husky
npm install -D husky

# 配置 pre-commit
npx husky add .husky/commit-msg 'npx --no -- commitlint --edit "$1"'
```

---

## 5. 合并请求规范

### 5.1 PR 标题格式

与 Commit Message 格式一致：

```
feat(user): 添加用户邮箱验证功能
```

### 5.2 PR 描述模板

```markdown
## 变更类型
- [ ] 新功能 (feat)
- [ ] Bug 修复 (fix)
- [ ] 文档更新 (docs)
- [ ] 代码重构 (refactor)
- [ ] 性能优化 (perf)

## 变更说明
<!-- 详细描述本次变更的内容 -->

## 关联 Issue
Closes #

## 测试情况
- [ ] 单元测试已通过
- [ ] 集成测试已通过
- [ ] 手动测试已完成

## 影响范围
<!-- 说明本次变更影响的模块或功能 -->

## 检查清单
- [ ] 代码符合规范
- [ ] 无新增 Lint 警告
- [ ] 文档已更新
- [ ] 测试覆盖充分
```

### 5.3 Code Review 要求

- 至少 1 人审核通过
- 无未解决的讨论
- CI 构建通过
- 代码覆盖率不降低

---

## 6. 版本发布规范

### 6.1 版本号规则

遵循语义化版本 (SemVer)：

```
MAJOR.MINOR.PATCH

MAJOR: 不兼容的 API 变更
MINOR: 向后兼容的功能新增
PATCH: 向后兼容的问题修复
```

### 6.2 版本标签

```
v1.0.0        # 正式版本
v1.0.0-beta.1 # 测试版本
v1.0.0-rc.1   # 候选版本
```

### 6.3 CHANGELOG

每次发布更新 `CHANGELOG.md`：

```markdown
## [1.1.0] - 2026-03-27

### Added
- 用户邮箱验证功能
- 订单导出 Excel 功能

### Fixed
- 订单金额计算错误 (#456)
- 用户登录超时问题 (#457)

### Changed
- 优化用户查询性能

### Breaking Changes
- 认证接口返回格式变更
```

---

## 7. 常见问题

### Q1: 如何撤销最近的提交？

```bash
# 保留变更，撤销提交
git reset --soft HEAD~1

# 丢弃变更，撤销提交
git reset --hard HEAD~1
```

### Q2: 如何修改最近的提交信息？

```bash
# 修改最后一次提交
git commit --amend

# 修改历史提交（交互式变基）
git rebase -i HEAD~3
```

### Q3: 如何处理合并冲突？

```bash
# 查看冲突文件
git status

# 手动解决冲突后
git add .
git commit -m "chore: 解决合并冲突"
```

### Q4: 如何同步远程分支？

```bash
# 获取远程更新
git fetch origin

# 变基到最新
git rebase origin/develop
```

---

## 8. 参考资料

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [语义化版本](https://semver.org/lang/zh-CN/)

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer