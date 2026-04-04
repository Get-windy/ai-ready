# AI-Ready 自动化备份配置文档

**项目**: 智企连·AI-Ready  
**版本**: v2.0  
**日期**: 2026-04-03  
**负责人**: devops-engineer

---

## 一、备份策略概览

### 1.1 备份类型

| 备份类型 | 频率 | 保留周期 | 执行时间 |
|----------|------|----------|----------|
| **全量备份** | 每日 | 30天 | 02:00 |
| **增量备份** | 每6小时 | 7天 | 06:00/12:00/18:00/22:00 |
| **WAL归档** | 持续 | 7天 | 实时 |

### 1.2 备份内容

| 内容 | 备份方式 | 存储位置 |
|------|----------|----------|
| PostgreSQL数据库 | pg_dump -Fc | 本地 + 远程 |
| Redis数据 | RDB快照 | 本地 |
| 文件存储 | tar.gz | 本地 + 远程 |

---

## 二、部署配置

### 2.1 前置条件

- Kubernetes集群
- PostgreSQL 15+
- Redis 7+
- 存储类支持PVC

### 2.2 部署步骤

```bash
# 1. 创建命名空间
kubectl create namespace ai-ready

# 2. 创建Secret
kubectl create secret generic ai-ready-secrets \
  --from-literal=DB_HOST=prod-db \
  --from-literal=DB_USERNAME=ai_ready \
  --from-literal=DB_PASSWORD=xxx \
  -n ai-ready

# 3. 创建PVC
kubectl apply -f k8s/base/backup-cronjob.yml

# 4. 验证部署
kubectl get cronjobs -n ai-ready
kubectl get pods -n ai-ready -l app=ai-ready-backup
```

### 2.3 手动执行备份

```bash
# 手动触发全量备份
kubectl create job --from=cronjob/ai-ready-backup manual-backup-$(date +%Y%m%d) -n ai-ready

# 查看备份日志
kubectl logs -n ai-ready -l job-name=manual-backup-$(date +%Y%m%d)
```

---

## 三、备份验证

### 3.1 自动验证

```bash
# 执行恢复验证
./scripts/backup/verify-restore.sh
```

### 3.2 验证报告

验证报告生成在 `/backup/ai-ready/logs/verification-report-YYYYMMDD.json`

### 3.3 定期演练

建议每月执行一次完整恢复演练。

---

## 四、恢复操作

### 4.1 PostgreSQL恢复

```bash
# 1. 停止应用服务
kubectl scale deployment ai-ready-api --replicas=0 -n ai-ready

# 2. 恢复数据库
PGPASSWORD="${DB_PASSWORD}" pg_restore \
  -h prod-db \
  -U postgres \
  -d ai_ready_prod \
  --clean \
  --if-exists \
  /backup/database/2026-04-03/ai_ready_prod_full_20260403_020000.dump

# 3. 启动应用服务
kubectl scale deployment ai-ready-api --replicas=3 -n ai-ready
```

### 4.2 Redis恢复

```bash
# 1. 停止Redis
redis-cli -h redis-service SHUTDOWN NOSAVE

# 2. 恢复RDB文件
gunzip -c /backup/redis/2026-04-03/redis_20260403_020000.rdb.gz > /var/lib/redis/dump.rdb
chown redis:redis /var/lib/redis/dump.rdb

# 3. 启动Redis
systemctl start redis
```

### 4.3 时间点恢复 (PITR)

```bash
# 使用pgBackRest恢复到指定时间
pgbackrest --stanza=main \
  --delta \
  --type=time \
  --target="2026-04-03 10:30:00" \
  restore
```

---

## 五、监控告警

### 5.1 备份监控

```yaml
# Prometheus告警规则
- alert: BackupJobFailed
  expr: kube_job_status_failed{job_name=~"ai-ready-backup.*"} > 0
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "AI-Ready备份任务失败"

- alert: BackupNotRun
  expr: time() - kube_cronjob_last_schedule_time{cronjob="ai-ready-backup"} > 86400
  for: 1h
  labels:
    severity: warning
  annotations:
    summary: "AI-Ready备份超过24小时未执行"
```

### 5.2 通知配置

备份结果会自动发送到钉钉群组。

---

## 六、文件清单

| 文件路径 | 说明 |
|----------|------|
| `scripts/backup/full-backup.sh` | 全量备份脚本 |
| `scripts/backup/pg-backup.sh` | PostgreSQL备份脚本 |
| `scripts/backup/verify-restore.sh` | 恢复验证脚本 |
| `k8s/base/backup-cronjob.yml` | Kubernetes CronJob配置 |
| `docs/automated-backup-guide.md` | 本文档 |

---

**文档生成**: devops-engineer  
**版本**: v1.0
