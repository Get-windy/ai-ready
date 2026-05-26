# CI/CD集成配置

## 概述

本配置提供多种CI/CD系统的集成方案，实现自动化测试的持续执行。

## 支持的CI/CD系统

### Jenkins
- **配置文件**: `Jenkinsfile`
- **特点**: 功能完整，适合企业级部署

### GitLab CI
- **配置文件**: `.gitlab-ci.yml`
- **特点**: 与GitLab深度集成

### GitHub Actions
- **配置文件**: `.github/workflows/test.yml`
- **特点**: 与GitHub深度集成，免费额度充足

## Jenkins集成

### 前提条件
- Jenkins服务器
- Docker插件
- Pipeline插件
- HTML Publisher插件
- Cobertura插件

### 配置步骤

1. 创建新的Pipeline任务
2. 配置SCM（Git仓库地址）
3. 指定Jenkinsfile路径: `tests/docker/Jenkinsfile`
4. 保存并运行

### 特性
- 自动触发（代码提交、定时执行）
- 分阶段执行（冒烟→API→集成→E2E→性能）
- HTML报告发布
- 覆盖率报告
- 失败通知（Slack、邮件）

## GitLab CI集成

### 前提条件
- GitLab Runner（Docker executor）
- 配置环境变量

### 配置步骤

1. 将 `.gitlab-ci.yml` 复制到项目根目录
2. 配置Runner标签
3. 提交并推送

### 特性
- 并行阶段执行
- 制品收集
- JUnit报告集成
- 覆盖率报告
- 钉钉通知

## GitHub Actions集成

### 前提条件
- GitHub仓库
- 配置Secrets

### 配置步骤

1. 创建目录 `.github/workflows/`
2. 复制 `.github-workflows-test.yml` 为 `test.yml`
3. 提交并推送

### 特性
- 多触发方式（push、PR、定时）
- 矩阵构建
- 制品上传
- 缓存支持
- Slack通知

## 环境变量配置

### 必需变量
```bash
TEST_ENV=test                    # 测试环境
DOCKER_COMPOSE_FILE=docker-compose.test-env.yml  # Compose文件
PYTHON_VERSION=3.11             # Python版本
```

### 可选变量
```bash
SLACK_WEBHOOK_URL=             # Slack通知地址
DINGTALK_TOKEN=                # 钉钉机器人Token
EMAIL_RECIPIENTS=              # 邮件通知列表
```

## 测试执行流程

```
1. 检出代码
2. 安装依赖
3. 启动测试环境（Docker容器）
4. 执行冒烟测试
5. 执行API测试
6. 执行集成测试
7. 执行E2E测试
8. 执行性能测试（main分支）
9. 生成报告
10. 清理环境
11. 发送通知
```

## 报告收集

### 测试报告
- HTML报告: `reports/*-report.html`
- JSON报告: `reports/*-report.json`
- JUnit XML: `reports/junit-*.xml`

### 覆盖率报告
- XML格式: `reports/coverage.xml`
- HTML格式: `reports/coverage/`

### 性能报告
- Benchmark JSON: `reports/benchmark.json`

## 通知配置

### Slack通知
1. 创建Slack Incoming Webhook
2. 配置环境变量 `SLACK_WEBHOOK_URL`

### 钉钉通知
1. 创建钉钉机器人
2. 配置环境变量 `DINGTALK_TOKEN`

### 邮件通知
1. 配置Jenkins邮件服务器
2. 设置收件人列表

## 故障排除

### Docker环境启动失败
```bash
# 检查Docker服务
systemctl status docker

# 手动启动测试环境
cd tests/docker
docker-compose -f docker-compose.test-env.yml up -d

# 查看容器日志
docker-compose -f docker-compose.test-env.yml logs
```

### 测试执行失败
```bash
# 本地复现
cd tests/tests
pytest -v --tb=long

# 查看详细报告
open reports/report.html
```

### 报告未生成
```bash
# 检查pytest插件
pip list | grep pytest

# 重新安装依赖
pip install -r tests/docker/requirements-test.txt
```

## 最佳实践

1. **分层执行**: 冒烟→API→集成→E2E，快速失败
2. **并行优化**: API测试使用并行执行
3. **缓存依赖**: 利用CI缓存加速构建
4. **制品保留**: 保留报告便于分析
5. **及时通知**: 失败时立即通知相关人员
