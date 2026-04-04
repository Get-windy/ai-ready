# AI-Ready CI/CD 流水线配置文档

**版本**: v2.0.0  
**日期**: 2026-04-04  
**作者**: devops-engineer  
**项目**: AI-Ready

---

## 目录

- [一、CI/CD架构设计](#一cicd架构设计)
- [二、流水线配置](#二流水线配置)
- [三、构建流水线](#三构建流水线)
- [四、部署流水线](#四部署流水线)
- [五、自动化测试集成](#五自动化测试集成)
- [六、环境管理](#六环境管理)
- [七、监控与告警](#七监控与告警)
- [八、最佳实践](#八最佳实践)

---

## 一、CI/CD架构设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AI-Ready CI/CD 架构                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   代码提交                                                           │
│      │                                                              │
│      ▼                                                              │
│   ┌──────────────────────────────────────────────────────────┐     │
│   │                    CI 阶段                                 │     │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐      │     │
│   │  │代码检查 │→ │单元测试 │→ │集成测试 │→ │质量分析 │      │     │
│   │  │ Lint   │  │ Unit    │  │Integra- │  │ SonarQube│      │     │
│   │  │        │  │         │  │ tion    │  │         │      │     │
│   │  └─────────┘  └─────────┘  └─────────┘  └─────────┘      │     │
│   └──────────────────────────────────────────────────────────┘     │
│                         │                                          │
│                         ▼                                          │
│   ┌──────────────────────────────────────────────────────────┐     │
│   │                    构建阶段                                │     │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐                   │     │
│   │  │ Maven   │→ │ Docker  │→ │ 安全扫描 │                   │     │
│   │  │ Build   │  │ Build   │  │ Trivy   │                   │     │
│   │  └─────────┘  └─────────┘  └─────────┘                   │     │
│   └──────────────────────────────────────────────────────────┘     │
│                         │                                          │
│                         ▼                                          │
│   ┌──────────────────────────────────────────────────────────┐     │
│   │                    CD 阶段                                 │     │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐                   │     │
│   │  │ Deploy  │→ │ Deploy  │→ │ Deploy  │                   │     │
│   │  │  Dev    │  │ Staging │  │  Prod   │                   │     │
│   │  └─────────┘  └─────────┘  └─────────┘                   │     │
│   └──────────────────────────────────────────────────────────┘     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 流水线类型

| 类型 | 触发条件 | 执行内容 |
|------|----------|----------|
| PR流水线 | Pull Request | 代码检查、单元测试 |
| CI流水线 | Push to main/develop | CI + 构建 + 安全扫描 |
| Release流水线 | Tag v* | CI + 构建 + 部署Prod |
| 手动流水线 | Workflow Dispatch | 可选环境部署 |

### 1.3 环境策略

| 环境 | 分支 | 触发 | 审批 |
|------|------|------|------|
| Development | develop | 自动 | 无 |
| Staging | main | 自动 | 无 |
| Production | v* tags | 手动 | 需要 |

---

## 二、流水线配置

### 2.1 GitHub Actions配置

**主配置文件**: `.github/workflows/ci-cd-full.yml`

#### 触发条件

```yaml
on:
  push:
    branches: [main, develop, 'release/*']
    tags: ['v*']
  pull_request:
    branches: [main, develop]
  workflow_dispatch:
    inputs:
      environment:
        type: choice
        options: [development, staging, production]
```

#### Job依赖关系

```
lint ──────┬──> unit-test ──┬──> build ──> docker-build ──> security-scan ──> deploy-*
           │                │
           └──> integration-test
           │                │
           └──> code-quality
```

### 2.2 GitLab CI配置

**配置文件**: `.gitlab-ci.yml`

#### Stage定义

```yaml
stages:
  - build      # 编译
  - test       # 测试
  - package    # 打包
  - security   # 安全扫描
  - deploy     # 部署
```

---

## 三、构建流水线

### 3.1 编译配置

```bash
# Maven编译命令
./mvnw clean package \
  -DskipTests \
  -B \
  -T 2C           # 并行构建

# 依赖缓存
~/.m2/repository/
```

### 3.2 Docker镜像构建

```yaml
# 镜像标签策略
tags:
  - type=ref,event=branch        # 分支名
  - type=semver,pattern={{version}}  # 版本号
  - type=sha,prefix=sha-         # Git SHA
  - type=raw,value=latest        # latest标签

# 多平台支持
platforms:
  - linux/amd64
  - linux/arm64
```

### 3.3 构建优化

| 优化项 | 方法 | 效果 |
|--------|------|------|
| 依赖缓存 | actions/cache | 减少50%时间 |
| Maven并行 | -T 2C | 减少30%时间 |
| Docker缓存 | buildx cache | 减少60%时间 |
| 分层构建 | Spring Boot分层 | 加速部署 |

---

## 四、部署流水线

### 4.1 部署策略

#### 开发环境 (Development)

```yaml
deploy-dev:
  trigger: push to develop
  approval: none
  strategy: rolling update
  timeout: 300s
```

#### 预发布环境 (Staging)

```yaml
deploy-staging:
  trigger: push to main
  approval: none
  strategy: blue-green
  timeout: 300s
  tests: E2E tests
```

#### 生产环境 (Production)

```yaml
deploy-prod:
  trigger: tag v*
  approval: required
  strategy: canary
  timeout: 600s
  rollback: automatic
```

### 4.2 部署命令

```bash
# 设置镜像
kubectl set image deployment/ai-ready-api \
  ai-ready-api=ghcr.io/ai-ready/ai-ready-api:v1.0.0 \
  -n ai-ready-prod

# 滚动更新状态
kubectl rollout status deployment/ai-ready-api \
  -n ai-ready-prod \
  --timeout=600s

# 回滚
kubectl rollout undo deployment/ai-ready-api -n ai-ready-prod

# 查看历史
kubectl rollout history deployment/ai-ready-api -n ai-ready-prod
```

### 4.3 部署验证

```bash
# 健康检查
curl -f https://prod.ai-ready.cn/actuator/health

# Pod状态
kubectl get pods -n ai-ready-prod -l app=ai-ready-api

# 日志检查
kubectl logs -n ai-ready-prod -l app=ai-ready-api --tail=100
```

---

## 五、自动化测试集成

### 5.1 测试类型

| 测试类型 | 工具 | 执行时机 | 覆盖率要求 |
|----------|------|----------|------------|
| 单元测试 | JUnit 5 | 每次提交 | >80% |
| 集成测试 | Spring Boot Test | PR/合并 | 关键路径 |
| E2E测试 | Playwright | 部署后 | 核心流程 |
| 性能测试 | JMeter | Release | 基准要求 |

### 5.2 测试配置

**配置文件**: `.github/workflows/automated-tests.yml`

#### 单元测试

```bash
./mvnw test \
  -Dtest.groups=unit \
  -Djacoco.skip=false
```

#### 集成测试

```bash
./mvnw verify \
  -Dtest.groups=integration \
  -Dspring.profiles.active=integration-test
```

#### E2E测试

```bash
npx playwright test --reporter=html
```

### 5.3 测试报告

| 报告类型 | 存储位置 | 保留时间 |
|----------|----------|----------|
| 单元测试 | surefire-reports | 30天 |
| 集成测试 | failsafe-reports | 30天 |
| E2E测试 | playwright-report | 30天 |
| 覆盖率 | Codecov | 永久 |

---

## 六、环境管理

### 6.1 环境配置

| 环境 | 命名空间 | 副本数 | URL |
|------|----------|--------|-----|
| Development | ai-ready-dev | 1 | dev.ai-ready.cn |
| Staging | ai-ready-staging | 2 | staging.ai-ready.cn |
| Production | ai-ready-prod | 3+ | prod.ai-ready.cn |

### 6.2 环境变量管理

```yaml
# GitHub Secrets
- KUBE_CONFIG_DEV      # 开发环境kubeconfig
- KUBE_CONFIG_STAGING  # 预发布环境kubeconfig
- KUBE_CONFIG_PROD     # 生产环境kubeconfig
- SONAR_TOKEN          # SonarCloud令牌
- CODECOV_TOKEN        # Codecov令牌
- DINGTALK_TOKEN       # 钉钉机器人令牌
```

### 6.3 配置分离

```yaml
# 开发环境
SPRING_PROFILES_ACTIVE: dev
LOGGING_LEVEL_ROOT: DEBUG

# 预发布环境
SPRING_PROFILES_ACTIVE: staging
LOGGING_LEVEL_ROOT: INFO

# 生产环境
SPRING_PROFILES_ACTIVE: prod
LOGGING_LEVEL_ROOT: WARN
```

---

## 七、监控与告警

### 7.1 流水线监控

| 指标 | 阈值 | 告警 |
|------|------|------|
| 构建时间 | >15分钟 | 警告 |
| 测试失败率 | >5% | 警告 |
| 部署失败 | 任何失败 | 严重 |
| 安全漏洞 | Critical>0 | 严重 |

### 7.2 通知配置

```yaml
# 钉钉通知
- name: 发送钉钉通知
  uses: zcong1993/actions-ding@master
  with:
    dingToken: ${{ secrets.DINGTALK_TOKEN }}
    body: |
      {
        "msgtype": "markdown",
        "markdown": {
          "title": "AI-Ready CI/CD 通知",
          "text": "部署状态: ${{ job.status }}"
        }
      }
```

### 7.3 Dashboard

- GitHub Actions: 流水线执行历史
- Codecov: 覆盖率趋势
- SonarCloud: 代码质量趋势

---

## 八、最佳实践

### 8.1 分支策略

```
main ────────→ Staging (自动)
  │
  ├── develop → Development (自动)
  │
  └── v* tag → Production (手动审批)
```

### 8.2 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
test: 测试相关
chore: 构建/工具相关
refactor: 重构
```

### 8.3 Code Review

- 所有PR需要至少1个审批
- CI必须通过才能合并
- 禁止直接推送到main

### 8.4 回滚策略

```bash
# 查看部署历史
kubectl rollout history deployment/ai-ready-api -n ai-ready-prod

# 回滚到上一版本
kubectl rollout undo deployment/ai-ready-api -n ai-ready-prod

# 回滚到指定版本
kubectl rollout undo deployment/ai-ready-api --to-revision=3 -n ai-ready-prod
```

---

## 附录

### A. 配置文件清单

```
I:\AI-Ready\.github\workflows\
├── ci-cd-full.yml          # 完整CI/CD流水线
├── automated-tests.yml     # 自动化测试
├── container-build.yml     # 镜像构建
├── ci.yml                  # 基础CI
└── ci-optimized.yml        # 优化版CI

I:\AI-Ready\
└── .gitlab-ci.yml          # GitLab CI配置
```

### B. 常用命令

```bash
# 手动触发流水线
gh workflow run ci-cd-full.yml -f environment=staging

# 查看流水线状态
gh run list --workflow=ci-cd-full.yml

# 查看运行详情
gh run view <run-id>

# 取消运行
gh run cancel <run-id>
```

### C. 故障排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 构建超时 | 依赖下载慢 | 使用缓存 |
| 测试失败 | 代码问题 | 查看报告 |
| 部署失败 | 配置问题 | 检查日志 |
| 镜像拉取失败 | 权限问题 | 检查凭证 |

---

**文档版本**: v2.0.0  
**最后更新**: 2026-04-04  
**维护人**: devops-engineer  
**项目**: AI-Ready
