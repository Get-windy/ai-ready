# AI-Ready 自动化运维方案

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-04-08  
> **维护者**: devops-engineer

---

## 一、运维自动化需求分析

### 1.1 核心需求

| 需求类别 | 描述 | 优先级 |
|----------|------|--------|
| 自动化部署 | 一键部署/回滚，支持多环境 | P0 |
| 自动化监控 | 实时监控指标采集，自动告警 | P0 |
| 自动化备份 | 定时全量/增量备份，自动验证 | P0 |
| 自动化日志 | 统一日志采集、分析、告警 | P1 |
| 自动化巡检 | 定时健康检查，问题自动诊断 | P1 |
| 自动化扩缩容 | 基于负载自动调整资源 | P2 |

### 1.2 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-Ready 自动化运维架构                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │   计划任务   │   │   事件触发   │   │   手动触发   │          │
│  │ Cron/Job    │   │  Webhook    │   │  CLI/API    │          │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘          │
│         │                 │                 │                   │
│         ▼                 ▼                 ▼                   │
│  ┌─────────────────────────────────────────────────────┐       │
│  │                  运维自动化引擎                       │       │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │       │
│  │  │ 部署自动化│ │ 监控自动化│ │ 备份自动化│ │ 巡检自动化│  │       │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘  │       │
│  └───────┼───────────┼───────────┼───────────┼───────┘       │
│          │           │           │           │                │
│          ▼           ▼           ▼           ▼                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │
│  │ Kubernetes  │ │ Prometheus  │ │  备份存储    │             │
│  │   Cluster   │ │  + AlertMgr │ │  S3/OSS     │             │
│  └─────────────┘ └─────────────┘ └─────────────┘             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、自动化运维流程

### 2.1 部署自动化流程

```
代码提交 → CI构建 → 镜像推送 → 自动测试 → Staging部署 → 验证 → 生产部署
                                                              ↓
                                                    [失败] → 自动回滚
```

| 阶段 | 自动化动作 | 触发条件 |
|------|------------|----------|
| CI构建 | Maven构建 + 单元测试 | Git Push |
| 镜像构建 | Docker Build + Trivy扫描 | CI通过 |
| Staging部署 | 自动部署到测试环境 | 镜像构建完成 |
| 生产部署 | 审批后部署 | 手动触发 |
| 自动回滚 | 恢复到上一版本 | 健康检查失败 |

### 2.2 监控自动化流程

```
指标采集 → 规则评估 → 告警生成 → 通知路由 → 升级处理 → 问题关闭
    ↓
  数据存储 (Prometheus)
    ↓
  可视化 (Grafana)
```

| 动作 | 描述 | 频率 |
|------|------|------|
| 指标采集 | Node/PostgreSQL/Redis/RocketMQ Exporter | 15s |
| 规则评估 | Prometheus Rules | 30s |
| 告警通知 | AlertManager发送通知 | 即时 |
| 告警升级 | 未响应自动升级 | 5/15/30min |

### 2.3 备份自动化流程

```
定时任务 → 数据备份 → 完整性校验 → 加密传输 → 存储归档 → 恢复演练
    ↓                                                       ↓
  备份失败 → 告警通知                                    演练失败 → 告警
```

| 备份类型 | 执行时间 | 保留期 | 存储位置 |
|----------|----------|--------|----------|
| 全量备份 | 每日02:00 | 30天 | 本地+远程 |
| 增量备份 | 每6小时 | 7天 | 本地 |
| WAL归档 | 实时 | 7天 | 本地+远程 |

---

## 三、运维脚本体系

### 3.1 脚本目录结构

```
scripts/
├── deploy/                    # 部署脚本
│   ├── deploy.sh             # 主部署脚本
│   ├── rollback.sh           # 回滚脚本
│   └── health-check.sh       # 健康检查
├── backup/                   # 备份脚本
│   ├── full-backup.sh        # 全量备份
│   ├── incremental-backup.sh # 增量备份
│   ├── restore.sh            # 恢复脚本
│   └── verify-backup.sh      # 备份验证
├── monitor/                  # 监控脚本
│   ├── collect-metrics.sh    # 指标采集
│   ├── check-services.sh     # 服务检查
│   └── alerts-handler.sh     # 告警处理
├──巡检/                    # 巡检脚本
│   ├── daily-check.sh        # 日巡检
│   ├── weekly-check.sh       # 周巡检
│   └── monthly-check.sh      # 月巡检
└── maintenance/              # 维护脚本
    ├── clear-logs.sh         # 日志清理
    ├── clear-cache.sh        # 缓存清理
    └── optimize-db.sh        # 数据库优化
```

### 3.2 核心脚本示例

#### 3.2.1 一键部署脚本 (deploy.sh)

```bash
#!/bin/bash
set -e

ENV=${1:-dev}
VERSION=${2:-latest}
NAMESPACE=ai-ready

echo "=== AI-Ready 部署开始 ==="
echo "环境: $ENV"
echo "版本: $VERSION"

# 1. 拉取最新镜像
docker pull aiedge/ai-ready-api:${VERSION}

# 2. 更新Kubernetes部署
kubectl set image deployment/ai-ready-api \
  api=aiedge/ai-ready-api:${VERSION} \
  -n ${NAMESPACE}

# 3. 等待滚动更新
kubectl rollout status deployment/ai-ready-api -n ${NAMESPACE} --timeout=300s

# 4. 健康检查
./scripts/deploy/health-check.sh ${ENV}

echo "=== 部署完成 ==="
```

#### 3.2.2 自动回滚脚本 (rollback.sh)

```bash
#!/bin/bash
set -e

NAMESPACE=ai-ready
REVISION=${1:-1}

echo "=== 开始回滚 ==="
echo "回滚到版本: ${REVISION}"

# 回滚到指定版本
kubectl rollout undo deployment/ai-ready-api \
  -n ${NAMESPACE} \
  --to-revision=${REVISION}

# 等待回滚完成
kubectl rollout status deployment/ai-ready-api -n ${NAMESPACE} --timeout=300s

# 验证
./scripts/deploy/health-check.sh production

echo "=== 回滚完成 ==="
```

#### 3.2.3 健康检查脚本 (health-check.sh)

```bash
#!/bin/bash

ENV=${1:-dev}
HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "=== 健康检查开始 ==="

for i in $(seq 1 $MAX_RETRIES); do
  RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null ${HEALTH_ENDPOINT})
  
  if [ "$RESPONSE" = "200" ]; then
    echo "健康检查通过 (尝试 $i/$MAX_RETRIES)"
    exit 0
  fi
  
  echo "等待服务启动... ($i/$MAX_RETRIES)"
  sleep $RETRY_INTERVAL
done

echo "健康检查失败"
exit 1
```

#### 3.2.4 备份验证脚本 (verify-backup.sh)

```bash
#!/bin/bash

BACKUP_DIR=${1:-/backup/ai-ready}
DATE=$(date +%Y-%m-%d)

echo "=== 备份验证开始 ==="

# 验证文件存在
for f in database files config; do
  BACKUP_FILE=$(ls -t ${BACKUP_DIR}/${f}* 2>/dev/null | head -1)
  if [ -z "$BACKUP_FILE" ]; then
    echo "❌ 备份文件不存在: ${f}"
    exit 1
  fi
  echo "✓ 找到备份: $BACKUP_FILE"
done

# 验证PostgreSQL备份
PG_BACKUP=$(ls -t ${BACKUP_DIR}/database/*.dump 2>/dev/null | head -1)
if pg_verifybackup -l $PG_BACKUP >/dev/null 2>&1; then
  echo "✓ PostgreSQL备份完整性验证通过"
else
  echo "⚠ PostgreSQL备份验证失败，尝试列出内容"
  pg_restore --list $PG_BACKUP >/dev/null 2>&1 || exit 1
fi

echo "=== 备份验证完成 ==="
```

---

## 四、自动化运维配置

### 4.1 Kubernetes CronJob配置

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ai-ready-daily-backup
  namespace: ai-ready
spec:
  schedule: "0 2 * * *"  # 每日02:00
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: aiedge/ai-ready-backup:latest
            command: ["/scripts/backup/full-backup.sh"]
            env:
            - name: BACKUP_DIR
              value: "/backup"
            - name: S3_BUCKET
              value: "ai-ready-backup"
          restartPolicy: OnFailure
```

### 4.2 自动化巡检任务

| 任务 | 频率 | 检查内容 |
|------|------|----------|
| 服务存活检查 | 每5分钟 | API服务、数据库、缓存 |
| 磁盘空间检查 | 每小时 | 各挂载点使用率 |
| 证书过期检查 | 每天 | SSL证书剩余天数 |
| 日志异常检查 | 每天 | ERROR/WARN日志统计 |
| 性能基线检查 | 每周 | 响应时间、吞吐量趋势 |
| 备份恢复演练 | 每月 | 恢复测试验证 |

---

## 五、实施计划

### 5.1 实施阶段

| 阶段 | 时间 | 内容 |
|------|------|------|
| 第一阶段 | 第1-2周 | 部署/回滚/健康检查脚本 |
| 第二阶段 | 第3-4周 | 备份恢复脚本 + 自动验证 |
| 第三阶段 | 第5-6周 | 监控告警自动化配置 |
| 第四阶段 | 第7-8周 | 巡检自动化 + 维护脚本 |

### 5.2 验收标准

- [ ] 部署脚本支持一键部署，回滚时间<5分钟
- [ ] 备份成功率100%，恢复演练通过率100%
- [ ] 监控告警覆盖所有核心组件，告警响应时间<1分钟
- [ ] 巡检报告自动生成，问题发现率提升50%

---

## 附录：相关文档

- [CI/CD流水线配置](../ci-cd/AI-READY_CICD_GUIDE.md)
- [监控告警系统](./MONITORING-ALERTING.md)
- [备份恢复方案](../backup/AI-READY_BACKUP_RESTORE_GUIDE.md)
- [容器部署指南](../deployment/AI-READY_CONTAINER_DEPLOYMENT_GUIDE.md)