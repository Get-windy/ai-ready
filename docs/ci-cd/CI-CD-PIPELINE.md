# AI-Ready CI/CD 流水线完整文档

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-29  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [流水线架构](#流水线架构)
3. [环境配置](#环境配置)
4. [GitHub Actions 配置](#github-actions-配置)
5. [GitLab CI 配置](#gitlab-ci-配置)
6. [Jenkins Pipeline 配置](#jenkins-pipeline-配置)
7. [质量检查集成](#质量检查集成)
8. [部署策略](#部署策略)
9. [回滚机制](#回滚机制)
10. [监控与告警](#监控与告警)
11. [最佳实践](#最佳实践)

---

## 概述

### 流水线目标

- **自动化构建**: 代码提交自动触发构建和测试
- **质量保证**: 集成代码分析、测试覆盖率、安全扫描
- **安全部署**: 分环境部署，生产环境需人工审批
- **快速回滚**: 支持一键回滚到历史版本
- **持续监控**: 部署后自动健康检查和监控

### 技术栈

| 工具 | 用途 | 版本 |
|------|------|------|
| GitHub Actions | 主要 CI/CD 平台 | - |
| GitLab CI | 备选 CI/CD 平台 | 15.x+ |
| Jenkins | 企业级 CI/CD | 2.400+ |
| Maven | Java 构建工具 | 3.9.x |
| Docker | 容器化 | 24.x |
| Kubernetes | 容器编排 | 1.28+ |
| Trivy | 安全扫描 | latest |
| SonarQube | 代码质量分析 | 10.x |

---

## 流水线架构

### 流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      AI-Ready CI/CD Pipeline                        │
└─────────────────────────────────────────────────────────────────────┘

  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
  │  代码提交 │───▶│  代码检查 │───▶│  单元测试 │───▶│   构建   │
  │   Push   │    │   Lint   │    │   Test   │    │  Build   │
  └──────────┘    └──────────┘    └──────────┘    └──────────┘
       │               │               │               │
       ▼               ▼               ▼               ▼
  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
  │ PR 触发  │    │Checkstyle│    │ Jacoco   │    │ Maven    │
  │ 分支触发 │    │ SpotBugs │    │ Coverage │    │ Package  │
  │ 手动触发 │    │ SonarQube│    │ 报告生成 │    │ JAR 文件 │
  └──────────┘    └──────────┘    └──────────┘    └──────────┘

       │
       ▼
  ┌──────────────────────────────────────────────────────────┐
  │                    Docker 构建阶段                        │
  ├──────────────────────────────────────────────────────────┤
  │  ┌──────────┐    ┌──────────┐    ┌──────────┐           │
  │  │ 构建镜像 │───▶│ 安全扫描 │───▶│ 推送仓库 │           │
  │  │  Docker  │    │  Trivy   │    │ Registry │           │
  │  └──────────┘    └──────────┘    └──────────┘           │
  └──────────────────────────────────────────────────────────┘
       │
       ▼
  ┌──────────────────────────────────────────────────────────┐
  │                    多环境部署                             │
  ├──────────────────────────────────────────────────────────┤
  │                                                           │
  │  ┌──────────┐    ┌──────────┐    ┌──────────┐           │
  │  │   Dev    │───▶│ Staging  │───▶│   Prod   │           │
  │  │ 自动部署 │    │ 手动触发 │    │人工审批后│           │
  │  │ develop  │    │   main   │    │ release/*│           │
  │  └──────────┘    └──────────┘    └──────────┘           │
  │       │              │              │                    │
  │       ▼              ▼              ▼                    │
  │  ┌──────────┐    ┌──────────┐    ┌──────────┐           │
  │  │健康检查 │    │ 集成测试 │    │ 监控增强 │           │
  │  │通知团队 │    │ 性能测试 │    │ 日志记录 │           │
  │  └──────────┘    └──────────┘    └──────────┘           │
  └──────────────────────────────────────────────────────────┘
```

### 流水线阶段

| 阶段 | 描述 | 耗时估算 | 失败处理 |
|------|------|----------|----------|
| lint | 代码质量分析 | 3-5分钟 | 允许失败，记录问题 |
| test | 单元测试 | 5-10分钟 | 阻止后续阶段 |
| build | 构建 JAR 包 | 3-5分钟 | 阻止后续阶段 |
| docker | Docker 镜像构建 | 5-10分钟 | 阻止后续阶段 |
| deploy-dev | 开发环境部署 | 2-5分钟 | 自动重试 |
| deploy-staging | 预发布部署 | 5-10分钟 | 手动介入 |
| deploy-prod | 生产部署 | 10-20分钟 | 回滚机制 |

---

## 环境配置

### 环境定义

| 环境 | 分支 | 命名空间 | URL | 部署策略 |
|------|------|----------|-----|----------|
| dev | develop | ai-ready-dev | https://dev.ai-ready.cn | 自动部署 |
| staging | main | ai-ready-staging | https://staging.ai-ready.cn | 手动触发 |
| prod | release/* | ai-ready-prod | https://prod.ai-ready.cn | 人工审批 |

### Secrets 配置

#### GitHub Secrets

```yaml
# 必需的 Secrets
KUBE_CONFIG_DEV: "开发环境 kubeconfig"
KUBE_CONFIG_STAGING: "预发布环境 kubeconfig"
KUBE_CONFIG_PROD: "生产环境 kubeconfig"
GITHUB_TOKEN: "GitHub 访问令牌"

# 可选 Secrets
SONAR_URL: "SonarQube 服务地址"
SONAR_TOKEN: "SonarQube 访问令牌"
WEBHOOK_URL: "通知 Webhook 地址"
```

#### GitLab Variables

```yaml
# CI/CD Variables
KUBE_API_SERVER_DEV: "开发环境 API Server"
KUBE_API_SERVER_STAGING: "预发布环境 API Server"
KUBE_API_SERVER_PROD: "生产环境 API Server"
KUBE_TOKEN_DEV: "开发环境 Token"
KUBE_TOKEN_STAGING: "预发布环境 Token"
KUBE_TOKEN_PROD: "生产环境 Token"
SONAR_URL: "SonarQube 服务地址"
SONAR_TOKEN: "SonarQube 访问令牌"
WEBHOOK_URL: "通知 Webhook 地址"
```

#### Jenkins Credentials

```groovy
// 需要配置的 Credentials
docker-credentials: Docker Registry 登录凭据
kube-config-dev: 开发环境 kubeconfig
kube-config-staging: 预发布环境 kubeconfig
kube-config-prod: 生产环境 kubeconfig
sonar-url: SonarQube URL
sonar-token: SonarQube Token
```

---

## GitHub Actions 配置

### 配置文件位置

```
I:\AI-Ready\.github\workflows\ci.yml
```

### 触发条件

```yaml
on:
  push:
    branches: [main, develop, feature/*]
  pull_request:
    branches: [main, develop]
  workflow_dispatch:  # 手动触发
    inputs:
      environment: [dev, staging, prod]
      tag: Docker 镜像标签
```

### 主要 Jobs

#### 1. 代码检查 (lint)

```yaml
lint:
  name: Code Analysis
  runs-on: ubuntu-latest
  steps:
    - mvn clean verify -B
      -Dpmd.skip=false
      -Dcheckstyle.skip=false
      -Dspotbugs.skip=false
      -Djacoco.skip=false
```

#### 2. 单元测试 (test)

```yaml
test:
  name: Unit Tests
  needs: lint
  steps:
    - mvn test -B
    - Upload coverage report
```

#### 3. 构建 (build)

```yaml
build:
  name: Build Application
  needs: test
  outputs:
    version: ${{ steps.version.outputs.version }}
  steps:
    - mvn clean package -B -DskipTests
    - Set version from pom.xml
```

#### 4. Docker 构建

```yaml
docker-build:
  name: Build Docker Image
  needs: build
  steps:
    - docker buildx build
    - docker push to ghcr.io
    - Cache with GHA
```

#### 5. 部署

```yaml
deploy-dev:
  needs: docker-build
  if: github.ref == 'refs/heads/develop'
  environment: dev
  steps:
    - kubectl apply -f k8s/dev/
    - kubectl rollout status

deploy-staging:
  needs: [docker-build, deploy-dev]
  if: github.ref == 'refs/heads/main'
  environment: staging

deploy-prod:
  needs: [docker-build, deploy-staging]
  if: github.event.inputs.environment == 'prod'
  environment: prod
```

### 手动触发部署

1. 进入 GitHub Actions 页面
2. 选择 "AI-Ready CI/CD Pipeline"
3. 点击 "Run workflow"
4. 选择环境和标签
5. 确认执行

---

## GitLab CI 配置

### 配置文件位置

```
I:\AI-Ready\.github\workflows\gitlab-ci.yml
```

复制到 GitLab 项目根目录为 `.gitlab-ci.yml`

### 阶段定义

```yaml
stages:
  - lint          # 代码检查
  - test          # 测试
  - build         # 构建
  - docker        # Docker 构建
  - deploy-dev    # 开发部署
  - deploy-staging # 预发布部署
  - deploy-prod   # 生产部署
  - rollback      # 回滚
```

### 关键特性

1. **并行检查**: lint 阶段并行执行 checkstyle、spotbugs、sonarqube
2. **服务容器**: 集成测试自动启动 PostgreSQL 和 Redis
3. **手动触发**: staging 和 prod 部署需要手动触发
4. **回滚支持**: 专门的 rollback 阶段支持一键回滚

---

## Jenkins Pipeline 配置

### 配置文件位置

```
I:\AI-Ready\ci-cd\jenkins\Jenkinsfile
```

### Jenkins 要求

- Jenkins 2.400+ 版本
- Kubernetes Plugin
- Pipeline Plugin
- Docker Plugin
- SonarQube Plugin

### Pipeline 特性

#### Kubernetes Agent

```groovy
agent {
    kubernetes {
        yaml '''
        Pod with containers:
        - maven (build)
        - docker (image)
        - kubectl (deploy)
        '''
    }
}
```

#### 并行代码检查

```groovy
stage('Code Quality') {
    parallel {
        stage('Checkstyle') { ... }
        stage('SpotBugs') { ... }
        stage('SonarQube') { ... }
    }
}
```

#### 生产审批门

```groovy
stage('Deploy Production') {
    input {
        message "Deploy to Production?"
        submitter "prod-deployers"
        parameters {
            booleanParam(name: 'PERFORM_BACKUP')
            booleanParam(name: 'ENABLE_MONITORING')
        }
    }
}
```

---

## 质量检查集成

### 检查工具

| 工具 | 检查内容 | 阈值 | 处理 |
|------|----------|------|------|
| Checkstyle | 代码风格 | 100 violations | 允许失败 |
| SpotBugs | 静态分析 | 10 HIGH issues | 阻止部署 |
| PMD | 代码规范 | 50 violations | 允许失败 |
| Jacoco | 测试覆盖率 | 80% | 建议达标 |
| Trivy | 安全扫描 | HIGH/CRITICAL | 阻止部署 |
| OWASP Dependency Check | 依赖安全 | CVE 检查 | 建议修复 |

### 质量检查脚本

```bash
# 运行完整质量检查
I:\AI-Ready\ci-cd\quality\quality-checks.sh

# 跳过测试（紧急情况）
I:\AI-Ready\ci-cd\quality\quality-checks.sh skip-tests
```

### 报告输出

```
I:\AI-Ready\quality-reports/
├── 20260329_233000/
│   ├── checkstyle.log
│   ├── spotbugs.log
│   ├── pmd.log
│   ├── unit-tests.log
│   ├── integration-tests.log
│   ├── dependency-check.log
│   ├── license-check.log
│   ├── trivy-scan.log
│   └── summary.md          # 综合报告
```

---

## 部署策略

### 部署流程

#### 开发环境 (Dev)

```
1. 代码合并到 develop 分支
2. 自动触发 CI/CD
3. 通过所有检查后自动部署
4. 健康检查验证
5. 团队通知
```

#### 预发布环境 (Staging)

```
1. 代码合并到 main 分支
2. 手动触发部署
3. 执行完整测试套件
4. 性能基准测试
5. 人工验收
```

#### 生产环境 (Prod)

```
1. 创建 release/* 分支
2. 手动触发部署
3. 人工审批确认
4. 可选备份
5. 执行部署
6. 监控增强
7. 日志记录
```

### 部署命令

```bash
# Kubernetes 部署命令

# 开发环境
kubectl apply -f k8s/base/ -n ai-ready-dev
kubectl apply -f k8s/dev/ -n ai-ready-dev
kubectl set image deployment/ai-ready-api ai-ready-api=<image> -n ai-ready-dev
kubectl rollout status deployment/ai-ready-api -n ai-ready-dev --timeout=300s

# 预发布环境
kubectl apply -f k8s/base/ -n ai-ready-staging
kubectl apply -f k8s/staging/ -n ai-ready-staging
kubectl set image deployment/ai-ready-api ai-ready-api=<image> -n ai-ready-staging
kubectl rollout status deployment/ai-ready-api -n ai-ready-staging --timeout=300s

# 生产环境（含健康检查）
kubectl apply -f k8s/base/ -n ai-ready-prod
kubectl apply -f k8s/prod/ -n ai-ready-prod
kubectl set image deployment/ai-ready-api ai-ready-api=<image> -n ai-ready-prod
kubectl rollout status deployment/ai-ready-api -n ai-ready-prod --timeout=600s
```

---

## 回滚机制

### 回滚脚本

```bash
# 回滚到上一版本
I:\AI-Ready\ci-cd\scripts\rollback.sh staging

# 回滚到指定版本
I:\AI-Ready\ci-cd\scripts\rollback.sh prod 3

# 查看部署历史
kubectl rollout history deployment/ai-ready-api -n ai-ready-staging
```

### 回滚流程

```
1. 确认回滚环境和版本
2. 检查 Kubernetes 连接
3. 显示当前部署状态
4. 备份当前部署配置
5. 执行回滚命令
6. 等待 rollout 完成
7. 健康检查验证
8. 发送通知
```

### 回滚检查清单

```markdown
## 回滚前检查

- [ ] 确认回滚原因已记录
- [ ] 确认回滚版本号
- [ ] 检查当前系统状态
- [ ] 备份当前配置
- [ ] 获取相关方确认（生产环境）

## 回滚后验证

- [ ] Pods 状态正常
- [ ] 健康检查通过
- [ ] 核心功能可用
- [ ] 监控指标正常
- [ ] 通知团队
```

---

## 监控与告警

### 部署监控指标

| 指标 | 描述 | 告警阈值 |
|------|------|----------|
| deployment_status | 部署状态 | failed |
| pod_ready_ratio | Pod 就绪比例 | < 80% |
| rollout_duration | 部署耗时 | > 10分钟 |
| health_check_status | 健康检查 | 非UP状态 |

### Grafana 仪表盘

部署仪表盘包含：
- 当前部署版本
- 部署历史时间线
- Pod 状态分布
- 健康检查结果
- 部署耗时趋势

### 告警规则

```yaml
# Prometheus 告警规则
groups:
  - name: deployment-alerts
    rules:
      - alert: DeploymentFailed
        expr: deployment_status{status="failed"} == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Deployment failed for {{ $labels.namespace }}"
          
      - alert: PodNotReady
        expr: pod_ready_ratio < 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pods not ready in {{ $labels.namespace }}"
```

---

## 最佳实践

### 分支策略

```
main (稳定版本)
  │
  ├── develop (开发分支)
  │     │
  │     ├── feature/* (功能分支)
  │     ├── bugfix/* (修复分支)
  │
  ├── release/* (发布分支)
  │
  └── hotfix/* (紧急修复)
```

### 代码提交规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试相关
chore: 构建/工具

示例:
feat(customer): 添加客户批量导入功能
fix(api): 修复登录接口超时问题
docs(ci): 更新 CI/CD 文档
```

### 部署检查清单

```markdown
## 部署前检查

### 代码质量
- [ ] 代码已通过审查
- [ ] 所有检查通过
- [ ] 测试覆盖率达标
- [ ] 无已知高危漏洞

### 环境准备
- [ ] 目标环境可用
- [ ] 配置文件已更新
- [ ] Secrets 已配置
- [ ] 备份已完成（生产）

### 审批确认
- [ ] 技术负责人确认
- [ ] 产品负责人确认（生产）

## 部署后验证

### 功能验证
- [ ] 健康检查通过
- [ ] 核心功能可用
- [ ] 性能指标正常

### 监控确认
- [ ] Grafana 指标正常
- [ ] 无错误日志堆积
- [ ] 告警规则生效
```

### 故障排查流程

```
1. 检查 Pod 状态
   kubectl get pods -n <namespace> -l app=ai-ready-api

2. 查看 Pod 日志
   kubectl logs -n <namespace> <pod-name> --tail=100

3. 检查部署状态
   kubectl rollout status deployment/ai-ready-api -n <namespace>

4. 查看部署历史
   kubectl rollout history deployment/ai-ready-api -n <namespace>

5. 执行回滚（必要时）
   ./rollback.sh <environment>
```

---

## 附录

### 配置文件清单

| 文件 | 位置 | 用途 |
|------|------|------|
| ci.yml | .github/workflows/ | GitHub Actions |
| gitlab-ci.yml | .github/workflows/ | GitLab CI 模板 |
| Jenkinsfile | ci-cd/jenkins/ | Jenkins Pipeline |
| rollback.sh | ci-cd/scripts/ | 回滚脚本 |
| quality-checks.sh | ci-cd/quality/ | 质量检查脚本 |

### 常用命令速查

```bash
# 本地构建
mvn clean package -DskipTests

# 本地测试
mvn test

# Docker 构建
docker build -t ai-ready:local .

# 查看部署状态
kubectl get deployment -n ai-ready-<env>

# 查看日志
kubectl logs -f deployment/ai-ready-api -n ai-ready-<env>

# 手动回滚
kubectl rollout undo deployment/ai-ready-api -n ai-ready-<env>
```

### 支持与反馈

如遇到 CI/CD 问题，请联系：
- **devops-engineer**: 运维工程师
- **项目群组**: group:group_1774582947832_uxqojz

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-29*