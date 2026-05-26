# CI/CD流水线架构设计

## 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Git Repository (ERP)                     │
└───────────────────────┬─────────────────────────────────────┘
                        │
                Code Push/Pull Request
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              CI/CD Orchestrator (GitHub Actions)            │
├─────────────────────────────────────────────────────────────┤
│  Stage 1: Code Commit        Stage 2: Build & Test         │
│  ┌──────────────────┐        ┌───────────────────────────┐ │
│  │ • Code Format    │        │ • Dependency Install      │ │
│  │ • Lint Check     │        │ • Compile/Package         │ │
│  │ • Commit Msg     │        │ • Unit Tests             │ │
│  └──────────────────┘        │ • Integration Tests      │ │
│                              └───────────────────────────┘ │
│                                                           │
│  Stage 3: Quality Gates      Stage 4: Security Scan      │
│  ┌──────────────────┐        ┌───────────────────────────┐ │
│  │ • Static Analysis│        │ • Dependency Scan         │ │
│  │ • Code Coverage  │        │ • Container Scan         │ │
│  │ • Complexity     │        │ • SAST Scan              │ │
│  └──────────────────┘        └───────────────────────────┘ │
│                                                           │
│  Stage 5: Build Artifacts   Stage 6: Deployment          │
│  ┌──────────────────┐        ┌───────────────────────────┐ │
│  │ • Docker Image   │        │ • Dev Env Deploy         │ │
│  │ • Package        │        │ • Test Env Deploy        │ │
│  │ • Documentation  │        │ • Staging Env Deploy     │ │
│  └──────────────────┘        │ • Prod Env Deploy        │ │
│                              └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                        │
                Quality Gates & Approvals
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│               Monitoring & Feedback System                  │
├─────────────────────────────────────────────────────────────┤
│  • Pipeline Metrics    • Quality Metrics    • Alerting     │
│  • Performance Dashboards • Trend Analysis • Reports       │
└─────────────────────────────────────────────────────────────┘
```

## 流水线阶段详细设计

### Stage 1: 代码提交阶段 (Code Commit)

**触发条件**: 代码推送(push)或拉取请求(PR)

**工作流程**:
1. **代码格式化检查**
   - 工具: Prettier (前端), Google Java Format (后端)
   - 规则: 自动格式化不符合规范的代码
   - 失败条件: 格式化失败或大量格式问题

2. **代码规范检查**
   - 工具: ESLint (前端), Checkstyle (后端)
   - 规则: 项目代码规范配置
   - 失败条件: 严重级别问题存在

3. **提交信息规范检查**
   - 工具: commitlint
   - 规则: Conventional Commits规范
   - 失败条件: 提交信息不符合规范

**输出**: 代码检查报告

### Stage 2: 构建与测试阶段 (Build & Test)

**触发条件**: Stage 1通过

**工作流程**:
1. **依赖安装与缓存**
   ```yaml
   - name: Cache dependencies
     uses: actions/cache@v3
     with:
       path: ~/.m2  # Maven缓存
       key: maven-${{ hashFiles('**/pom.xml') }}
   ```

2. **编译/打包**
   - 后端: Maven/Gradle构建
   - 前端: npm/yarn构建
   - 输出: 可部署的包文件

3. **单元测试**
   - 后端: JUnit/TestNG
   - 前端: Jest/Vitest
   - 覆盖率要求: > 80%

4. **集成测试**
   - 使用Testcontainers进行数据库集成测试
   - API接口测试
   - 微服务间通信测试

**输出**: 构建产物、测试报告、覆盖率报告

### Stage 3: 代码质量门禁阶段 (Quality Gates)

**触发条件**: Stage 2通过

**工作流程**:
1. **静态代码分析**
   ```yaml
   - name: SonarQube Scan
     uses: SonarSource/sonarqube-scan-action@master
     env:
       SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
   ```

2. **代码覆盖率检查**
   - 工具: JaCoCo (Java), Istanbul (JavaScript)
   - 门禁: 单元测试覆盖率 > 80%
   - 报告: 生成覆盖率报告

3. **代码复杂度检查**
   - 圈复杂度: < 10 per method
   - 认知复杂度: < 15 per method
   - 重复率: < 5%

4. **技术债务分析**
   - 债务比率: < 5%
   - 修复优先级: 高优先级问题必须修复

**输出**: 质量报告、技术债务分析

### Stage 4: 安全扫描阶段 (Security Scan)

**触发条件**: Stage 3通过

**工作流程**:
1. **依赖漏洞扫描**
   ```yaml
   - name: Scan dependencies
     uses: aquasecurity/trivy-action@master
     with:
       scan-type: 'fs'
       scan-ref: '.'
   ```

2. **容器安全扫描**
   ```yaml
   - name: Scan Docker image
     run: trivy image ${{ steps.build.outputs.image }}
   ```

3. **静态应用安全测试(SAST)**
   - 工具: Semgrep, Bandit
   - 检查: SQL注入、XSS、敏感信息泄露等

4. **配置安全检查**
   - 工具: Checkov
   - 检查: 基础设施配置安全

**输出**: 安全扫描报告、漏洞列表

### Stage 5: 构建产物阶段 (Build Artifacts)

**触发条件**: Stage 4通过

**工作流程**:
1. **Docker镜像构建**
   ```dockerfile
   # 多阶段构建
   FROM maven:3.8-openjdk-17 AS builder
   COPY . /app
   RUN mvn clean package -DskipTests
   
   FROM openjdk:17-jre-slim
   COPY --from=builder /app/target/*.jar /app.jar
   ```

2. **软件包发布**
   - 后端: Maven Central / Nexus
   - 前端: npm Registry
   - 容器: Docker Registry

3. **文档生成**
   - API文档: Swagger/OpenAPI
   - 架构文档: Mermaid图表
   - 用户文档: MkDocs

**输出**: Docker镜像、软件包、文档

### Stage 6: 部署阶段 (Deployment)

**触发条件**: Stage 5通过 + 人工审批(生产环境)

**工作流程**:
1. **开发环境部署**
   - 自动部署每个提交
   - 用于开发测试
   - 快速反馈

2. **测试环境部署**
   - 每日自动部署
   - 用于集成测试
   - 完整质量门禁

3. **预发环境部署**
   - 手动触发
   - 生产环境配置
   - 用户验收测试

4. **生产环境部署**
   - 手动审批
   - 蓝绿部署/金丝雀发布
   - 回滚机制

**部署策略**:
```yaml
deployment:
  strategy:
    type: rolling-update
    maxSurge: 25%
    maxUnavailable: 0
  healthCheck:
    path: /actuator/health
    initialDelay: 30
    period: 10
```

**输出**: 部署状态、服务URL、监控链接

## 多环境配置

### 环境变量管理

```yaml
# .github/workflows/environments.yml
environments:
  development:
    name: Development
    url: https://dev.erp.example.com
    secrets:
      - DATABASE_URL
      - API_KEY
      
  testing:
    name: Testing
    url: https://test.erp.example.com
    deployment-branch: main
    
  staging:
    name: Staging
    url: https://staging.erp.example.com
    required-reviewers:
      - qa-lead
      
  production:
    name: Production
    url: https://erp.example.com
    required-reviewers:
      - devops-engineer
      - product-owner
```

### 环境特定配置

```properties
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/erp_dev
logging.level.root=DEBUG

# application-test.properties  
spring.datasource.url=jdbc:postgresql://test-db:5432/erp_test
logging.level.root=INFO

# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-db:5432/erp_prod
logging.level.root=WARN
```

## 流水线优化策略

### 1. 缓存优化

```yaml
cache-strategy:
  dependencies:
    key: ${{ runner.os }}-deps-${{ hashFiles('**/package-lock.json', '**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-deps-
  
  build-outputs:
    key: ${{ runner.os }}-build-${{ github.sha }}
    paths:
      - target/
      - dist/
```

### 2. 并行执行

```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps: [...]
    
  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps: [...]
    
  e2e-tests:
    runs-on: ubuntu-latest
    needs: integration-tests
    steps: [...]
```

### 3. 增量构建

```yaml
# 仅构建变化的模块
- name: Detect changed modules
  id: changes
  uses: tj-actions/changed-files@v22
  with:
    files: |
      backend/**
      frontend/**
      
- name: Build changed modules only
  if: steps.changes.outputs.any_changed == 'true'
  run: |
    # 只构建有变化的模块
```

### 4. 构建矩阵

```yaml
strategy:
  matrix:
    java: [17, 21]
    node: [18, 20]
    os: [ubuntu-latest, windows-latest]
```

## 监控与告警

### 流水线监控指标

| 指标 | 监控频率 | 告警阈值 | 响应时间 |
|------|----------|----------|----------|
| 流水线成功率 | 实时 | < 95% | 15分钟 |
| 平均构建时间 | 每小时 | 增加20% | 30分钟 |
| 测试失败率 | 每次运行 | > 10% | 立即 |
| 部署成功率 | 每次部署 | < 98% | 立即 |

### 质量趋势监控

```prometheus
# 代码质量指标
code_quality_score{project="erp"} 85
test_coverage{project="erp", type="unit"} 82
technical_debt_ratio{project="erp"} 3.2

# 安全指标  
security_vulnerabilities{project="erp", severity="critical"} 0
security_vulnerabilities{project="erp", severity="high"} 2

# 性能指标
build_duration_seconds{project="erp"} 245
test_execution_seconds{project="erp"} 120
```

### 告警配置

```yaml
alerts:
  - name: Pipeline Failure Rate High
    condition: rate(pipeline_failures_total[5m]) > 0.05
    severity: critical
    notification:
      - slack: devops-alerts
      - email: devops-team@example.com
      
  - name: Build Time Increased
    condition: build_duration_seconds > avg_over_time(build_duration_seconds[7d]) * 1.2
    severity: warning
    notification:
      - slack: devops-warnings
```

## 故障排除与恢复

### 常见问题处理

1. **构建失败**
   - 检查依赖版本
   - 验证构建缓存
   - 查看详细日志

2. **测试失败**
   - 分析测试报告
   - 检查测试数据
   - 验证环境配置

3. **部署失败**
   - 检查资源配额
   - 验证网络连接
   - 查看部署日志

4. **安全扫描失败**
   - 评估漏洞严重性
   - 制定修复计划
   - 申请临时豁免

### 回滚策略

```yaml
rollback-strategy:
  automated: true
  max-attempts: 3
  steps:
    - health-check: verify current deployment
    - scale-down: reduce new version traffic
    - restore: revert to previous version
    - notify: send rollback notification
```

## 性能基准

### 目标指标

| 阶段 | 目标时间 | 实际基准 |
|------|----------|----------|
| 代码检查 | < 1分钟 | 45秒 |
| 构建 | < 3分钟 | 2分30秒 |
| 单元测试 | < 2分钟 | 1分45秒 |
| 质量扫描 | < 5分钟 | 4分20秒 |
| 安全扫描 | < 3分钟 | 2分50秒 |
| 总时间 | < 15分钟 | 12分30秒 |

### 优化建议

1. **并行化**: 将独立任务并行执行
2. **缓存**: 有效利用缓存减少重复工作
3. **增量**: 仅处理变化的文件
4. **资源**: 适当增加计算资源
5. **工具**: 选择性能更好的工具

## 扩展性设计

### 支持多项目

```yaml
# 多项目配置模板
projects:
  - name: erp-backend
    type: java
    build: maven
    tests: [unit, integration]
    
  - name: erp-frontend
    type: typescript
    build: vite
    tests: [unit, e2e]
    
  - name: erp-infrastructure
    type: terraform
    build: terraform
    tests: [validate, plan]
```

### 插件化架构

```python
# 插件接口
class PipelinePlugin:
    def before_build(self, context):
        pass
        
    def after_test(self, context):
        pass
        
    def quality_check(self, context):
        pass
```

### 自定义质量门禁

```yaml
custom-gates:
  - name: business-rule-check
    script: scripts/check-business-rules.sh
    timeout: 300
    required: true
    
  - name: documentation-completeness
    script: scripts/check-documentation.sh
    timeout: 180
    required: false
```

## 后续实施步骤

1. **工具链配置**: 安装和配置所有必要工具
2. **流水线实现**: 编写GitHub Actions工作流
3. **质量门禁集成**: 配置SonarQube、Trivy等工具
4. **环境部署**: 设置开发、测试、预发、生产环境
5. **监控配置**: 部署Prometheus、Grafana监控
6. **文档编写**: 创建用户指南和最佳实践
7. **团队培训**: 培训开发团队使用新流程
8. **持续优化**: 基于监控数据进行优化