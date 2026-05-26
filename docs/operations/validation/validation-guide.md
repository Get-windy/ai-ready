# 测试环境部署验证操作手册

## 目录

1. [操作手册概述](#1-操作手册概述)
2. [验证前准备](#2-验证前准备)
3. [标准验证流程](#3-标准验证流程)
4. [验证脚本详细操作](#4-验证脚本详细操作)
5. [验证报告操作](#5-验证报告操作)
6. [常见问题处理](#6-常见问题处理)
7. [验证任务分工](#7-验证任务分工)
8. [附录](#8-附录)

---

## 1. 操作手册概述

### 1.1 手册目的

本手册为测试环境部署验证提供标准化操作流程，确保验证工作的一致性和可重复性。

### 1.2 适用角色

| 角色 | 职责 | 所需权限 |
|------|------|----------|
| 测试工程师 | 执行验证、记录结果 | 环境访问权限 |
| DevOps工程师 | 环境部署、问题修复 | 服务器管理权限 |
| 开发工程师 | API验证、问题排查 | 代码仓库权限 |
| 项目经理 | 验收确认 | 报告查看权限 |

### 1.3 验证时机

- **部署完成后**：每次部署完成后必须执行验证
- **环境变更后**：配置修改、版本更新后需重新验证
- **每日巡检**：测试环境每日健康检查
- **问题修复后**：故障修复后验证修复效果

---

## 2. 验证前准备

### 2.1 环境确认

**步骤1：确认部署已完成**
```bash
# Docker Compose环境
docker-compose ps

# 应看到以下服务均为Up状态：
# - postgres
# - redis
# - rabbitmq
# - app
# - nginx
# - prometheus
# - grafana
```

**步骤2：确认网络可达**
```bash
# 测试本机访问
curl http://localhost:8080
ping localhost

# 测试外部访问（如适用）
curl http://test-env.company.com
```

**步骤3：确认工具就绪**
```bash
# 检查必需工具
docker --version
curl --version
nc -h 2>/dev/null || echo "nc未安装，将使用备用方法"
bc --version 2>/dev/null || echo "bc未安装，资源检查功能受限"
```

### 2.2 脚本准备

**步骤1：进入验证脚本目录**
```bash
cd I:\AI-Ready\scripts\validation
# Linux/mac:
# cd /path/to/ai-ready/scripts/validation
```

**步骤2：检查脚本权限**
```bash
# Linux/mac:
chmod +x *.sh

# Windows (Git Bash):
# 确保脚本有执行权限
```

**步骤3：创建报告目录**
```bash
mkdir -p reports
```

### 2.3 验证计划确认

在开始验证前，确认以下信息：

- [ ] 本次验证范围（全部/部分服务）
- [ ] 验证模式（docker/manual）
- [ ] 验证人员
- [ ] 预计完成时间
- [ ] 报告接收人

---

## 3. 标准验证流程

### 3.1 快速验证（5分钟）

适用场景：每日巡检、快速确认环境状态

```bash
# 1. 进入脚本目录
cd scripts/validation

# 2. 执行快速健康检查
./health-check.sh all

# 3. 验证结果判断
# 全部通过 → 环境正常
# 有失败项 → 进入详细验证
```

### 3.2 标准验证（15分钟）

适用场景：部署完成后常规验证

```bash
# 1. 前置条件检查
./validate-deployment.sh -q

# 2. 容器状态验证（Docker模式）
./validate-deployment.sh -s all

# 3. 健康检查
./health-check.sh all

# 4. API验证
./api-validation.sh

# 5. 生成报告
./validate-deployment.sh -o reports/validation-$(date +%Y%m%d).md
```

### 3.3 完整验证（30分钟）

适用场景：首次部署、重大版本更新、环境重建

```bash
# 1. 执行一键完整验证
./quick-validate.sh reports/full-validation-$(date +%Y%m%d).md

# 2. 查看报告
cat reports/full-validation-$(date +%Y%m%d).md

# 3. 手动补充验证（如需）
# - 业务功能验证
# - 性能基准测试
# - 安全扫描
```

### 3.4 验证流程图

```
开始验证
    │
    ▼
确认部署状态
    │
    ├── 未部署 → 终止，通知部署人员
    │
    └── 已部署 → 继续
                │
                ▼
        选择验证级别
                │
                ├── 快速验证(5min)
                │   └── health-check.sh
                │
                ├── 标准验证(15min)
                │   └── validate-deployment.sh + api-validation.sh
                │
                └── 完整验证(30min)
                    └── quick-validate.sh
                │
                ▼
        分析验证结果
                │
                ├── 全部通过 → 生成报告 → 完成
                │
                └── 存在失败 → 问题分类
                            │
                            ├── 环境问题 → 通知DevOps
                            ├── 应用问题 → 通知开发
                            └── 配置问题 → 现场修复
                            │
                            ▼
                    修复后重新验证
                            │
                            └── 循环直到通过
```

---

## 4. 验证脚本详细操作

### 4.1 validate-deployment.sh 详细操作

**场景1：验证Docker Compose部署的所有服务**
```bash
./validate-deployment.sh
```
预期输出：
```
[10:00:00] ========================================
[10:00:00] Sprint 27+1 测试环境部署验证
[10:00:00] 模式: docker
[10:00:00] 服务: all
[10:00:00] ========================================
[10:00:00] 检查前置条件...
[✓] Docker 环境正常
[✓] curl 已安装
[10:00:01] 验证容器运行状态...
[✓] 容器 postgres 运行中
[✓] 容器 redis 运行中
...
========================================
通过: 21
失败: 0
警告: 1
========================================
```

**场景2：验证手动部署环境**
```bash
./validate-deployment.sh -m manual
```
注意：手动模式下不检查容器状态，只检查端口和服务响应。

**场景3：仅验证指定服务**
```bash
# 仅验证PostgreSQL
./validate-deployment.sh -s postgres

# 仅验证应用服务
./validate-deployment.sh -s app
```

**场景4：静默模式（用于CI/CD流水线）**
```bash
./validate-deployment.sh -q -o report.md
if [ $? -ne 0 ]; then
    echo "验证失败，查看报告: report.md"
    exit 1
fi
```

### 4.2 health-check.sh 详细操作

**场景1：检查所有服务**
```bash
./health-check.sh all
```

**场景2：检查数据库**
```bash
./health-check.sh postgres
```
输出示例：
```
[10:05:00] 检查 PostgreSQL...
[✓] PostgreSQL 服务正常
[✓] 数据库数量: 3
```

**场景3：在监控脚本中使用**
```bash
#!/bin/bash
# 每小时执行一次健康检查
while true; do
    ./health-check.sh all > /var/log/health-$(date +%H).log 2>&1
    sleep 3600
done
```

### 4.3 api-validation.sh 详细操作

**场景1：默认基础URL验证**
```bash
./api-validation.sh
```

**场景2：指定测试环境**
```bash
API_BASE_URL=http://test-server:8080 ./api-validation.sh
```

**场景3：集成到CI/CD**
```bash
# .github/workflows/validate.yml
- name: API Validation
  run: |
    API_BASE_URL=http://localhost:8080 ./scripts/validation/api-validation.sh
  continue-on-error: false
```

### 4.4 quick-validate.sh 详细操作

**场景1：完整验证并生成报告**
```bash
./quick-validate.sh
```
输出：
```
========================================
Sprint 27+1 一键部署验证
开始时间: 2026-04-27 10:00:00
========================================

[1/4] 执行部署验证...
[2/4] 执行健康检查...
[3/4] 执行API验证...
[4/4] 生成综合报告...

========================================
验证完成
报告位置: scripts/validation/reports/validation_report_20260427_100030.md
========================================
```

**场景2：指定报告输出路径**
```bash
./quick-validate.sh /shared/reports/sprint27-validation.md
```

---

## 5. 验证报告操作

### 5.1 报告生成

**自动生成**：
```bash
# 生成Markdown报告
./validate-deployment.sh -o validation-report.md

# 生成完整报告
./quick-validate.sh full-report.md
```

**手动生成**：
1. 复制模板：`cp docs/templates/validation-report.md my-report.md`
2. 使用文本编辑器填写各字段
3. 保存并提交

### 5.2 报告提交

**提交位置**：
- 项目文档库：`docs/testing/reports/`
- 共享存储：`//shared/validation-reports/`
- 项目管理工具：Jira/Confluence

**提交规范**：
```
文件名格式：validation_{环境}_{日期}_{版本}.md
示例：validation_test_20260427_s27.md
```

### 5.3 报告审核

**审核流程**：
1. 验证人员填写报告
2. 测试负责人审核
3. DevOps确认环境状态
4. 项目经理验收签字

**审核检查清单**：
- [ ] 验证时间、人员、环境信息完整
- [ ] 所有验证项均有结果记录
- [ ] 失败项有详细说明和修复建议
- [ ] 验证结论明确
- [ ] 相关人员签字确认

---

## 6. 常见问题处理

### 6.1 脚本执行问题

**问题：脚本无执行权限**
```bash
# 现象：-bash: ./validate-deployment.sh: Permission denied
# 解决：
chmod +x scripts/validation/*.sh
```

**问题：Windows下脚本无法运行**
```bash
# 使用Git Bash或WSL
# 或在PowerShell中使用等效命令
# 推荐：在WSL2 Ubuntu环境中运行
```

**问题：依赖工具缺失**
```bash
# 安装curl
sudo apt-get install curl    # Debian/Ubuntu
sudo yum install curl        # CentOS/RHEL

# 安装nc
sudo apt-get install netcat  # Debian/Ubuntu
sudo yum install nc          # CentOS/RHEL

# 安装bc
sudo apt-get install bc      # Debian/Ubuntu
sudo yum install bc          # CentOS/RHEL
```

### 6.2 验证失败处理

**情况1：部分服务验证失败**

处理流程：
1. 记录失败服务名称和错误信息
2. 查看该服务日志：`docker logs <服务名>`
3. 判断问题类型：
   - 配置问题 → 修改配置后重启
   - 依赖问题 → 确认依赖服务正常后重启
   - 资源问题 → 释放资源或扩容
4. 修复后重新验证该服务

**情况2：全部验证失败**

处理流程：
1. 检查Docker守护进程：`docker info`
2. 检查系统资源：`df -h`, `free -h`
3. 检查网络状态：`ip addr`, `iptables -L`
4. 如无法快速修复，执行紧急恢复：
   ```bash
   docker-compose down
   docker-compose up -d
   sleep 60
   ./quick-validate.sh
   ```

**情况3：验证通过但业务功能异常**

处理流程：
1. 确认基础服务正常（已通过验证）
2. 检查应用日志中的业务错误
3. 执行业务功能测试
4. 如有问题，提交Bug给开发团队

### 6.3 性能问题处理

**内存使用率过高**：
```bash
# 查看内存使用详情
docker stats --no-stream

# 限制容器内存（如需要）
docker update --memory=1g --memory-swap=1g <container>
```

**CPU使用率过高**：
```bash
# 查看CPU使用详情
top
htop

# 限制容器CPU（如需要）
docker update --cpus=1.0 <container>
```

---

## 7. 验证任务分工

### 7.1 验证任务矩阵

| 验证项 | 测试工程师 | DevOps | 开发工程师 | 自动化 |
|--------|-----------|--------|-----------|--------|
| 容器状态验证 | 执行 | 协助 | - | ✅ |
| 端口连通性 | 执行 | 协助 | - | ✅ |
| 健康检查端点 | 执行 | - | 协助 | ✅ |
| 数据库连接 | 执行 | 协助 | - | ✅ |
| 服务间通信 | 执行 | 协助 | - | ✅ |
| 日志检查 | 执行 | - | 分析 | ✅ |
| 资源使用 | 监控 | 处理 | - | ✅ |
| API接口验证 | 执行 | - | 协助 | ✅ |
| 业务功能验证 | 执行 | - | 修复 | - |
| 性能测试 | 执行 | 协助 | 优化 | - |
| 安全扫描 | 执行 | 协助 | 修复 | - |
| 报告编写 | 负责 | 审核 | - | - |

### 7.2 验证排期建议

**日常验证**：
- 时间：每日上午9:00
- 执行人：测试工程师
- 内容：health-check.sh all
- 耗时：5分钟

**部署后验证**：
- 时间：每次部署完成后30分钟内
- 执行人：部署执行人
- 内容：quick-validate.sh
- 耗时：30分钟

**版本发布前验证**：
- 时间：发布前1天
- 执行人：测试团队
- 内容：完整验证 + 业务测试
- 耗时：2-4小时

---

## 8. 附录

### 8.1 快捷命令参考

```bash
# 快速健康检查
./health-check.sh all

# 快速部署验证
./validate-deployment.sh -q

# 验证指定服务
./validate-deployment.sh -s postgres

# 生成验证报告
./validate-deployment.sh -o report.md

# 完整一键验证
./quick-validate.sh

# 查看容器状态
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 查看容器日志
docker logs --tail 50 -f <container>

# 查看资源使用
docker stats --no-stream
```

### 8.2 相关文档

| 文档 | 路径 |
|------|------|
| 部署验证指南 | `docs/testing/deploy/DEPLOYMENT_VALIDATION.md` |
| Docker Compose部署指南 | `docs/testing/deploy/DOCKER_COMPOSE_DEPLOY.md` |
| 手动部署手册 | `docs/testing/deploy/MANUAL_DEPLOY.md` |
| 健康检查指南 | `docs/testing/deploy/HEALTH_CHECK.md` |
| 验证报告模板 | `docs/templates/validation-report.md` |

### 8.3 版本历史

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| 1.0.0 | 2026-04-27 | 初始版本 | doc-writer |

---

*本手册由 doc-writer 维护，如有问题请联系项目团队。*
