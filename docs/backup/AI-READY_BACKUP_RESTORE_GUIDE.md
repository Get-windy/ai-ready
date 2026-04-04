# AI-Ready 备份恢复方案文档

**版本**: v2.0.0  
**日期**: 2026-04-04  
**作者**: devops-engineer  
**项目**: AI-Ready

---

## 一、备份策略

### 1.1 备份类型

| 类型 | 频率 | 保留期 | 数据量 | 存储位置 |
|------|------|--------|--------|----------|
| 全量备份 | 每日02:00 | 30天 | ~5GB | 本地+远程+云 |
| 增量备份 | 每6小时 | 7天 | ~500MB | 本地 |
| WAL归档 | 实时 | 7天 | ~2GB | 本地+远程 |

### 1.2 备份内容

| 组件 | 备份方式 | 备份文件 |
|------|----------|----------|
| PostgreSQL | pg_dump -Fc | *.dump |
| Redis | RDB快照 | *.rdb.gz |
| 文件存储 | tar压缩 | *.tar.gz |
| 配置文件 | tar压缩 | config_*.tar.gz |

### 1.3 存储策略

```
┌─────────────────────────────────────────────────────────────┐
│                       备份存储架构                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   本地存储 (热)          远程服务器 (温)        云存储 (冷) │
│   /backup/ai-ready  →   backup-server    →   OSS/S3       │
│   30天                  60天                  365天        │
│                                                             │
│   ┌─────────┐         ┌─────────┐         ┌─────────┐     │
│   │database │ ─rsync→ │database │ ─sync→  │database │     │
│   │redis    │ ─rsync→ │redis    │ ─sync→  │redis    │     │
│   │files    │ ─rsync→ │files    │ ─sync→  │files    │     │
│   └─────────┘         └─────────┘         └─────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、自动备份配置

### 2.1 Kubernetes CronJob

**配置文件**: `k8s/base/backup-cronjob.yml`

```bash
# 应用配置
kubectl apply -f k8s/base/backup-cronjob.yml

# 手动触发备份
kubectl create job --from=cronjob/ai-ready-backup manual-backup-$(date +%s) -n ai-ready
```

### 2.2 备份脚本

| 脚本 | 功能 | 执行频率 |
|------|------|----------|
| full-backup.sh | 全量备份 | 每日02:00 |
| pg-backup.sh | 数据库备份 | 每6小时 |
| restore.sh | 数据恢复 | 手动执行 |

### 2.3 备份验证

```bash
# 验证备份完整性
sha256sum -c /backup/ai-ready/database/2026-04-04/*.sha256

# 验证PostgreSQL备份
pg_verifybackup /backup/ai-ready/database/2026-04-04/

# 测试恢复(不覆盖)
pg_restore --list /backup/ai-ready/database/2026-04-04/*.dump
```

---

## 三、恢复流程

### 3.1 PostgreSQL恢复

```bash
# 1. 列出可用备份
./scripts/backup/restore.sh list

# 2. 恢复指定备份
./scripts/backup/restore.sh pg /backup/ai-ready/database/2026-04-04/ai_ready_prod_full_20260404_020000.dump

# 3. 验证恢复结果
psql -h localhost -U postgres -d ai_ready_prod -c "SELECT count(*) FROM users;"
```

### 3.2 Redis恢复

```bash
# 恢复Redis
./scripts/backup/restore.sh redis /backup/ai-ready/redis/2026-04-04/redis_20260404_020000.rdb.gz

# 验证
redis-cli INFO keyspace
```

### 3.3 完整恢复

```bash
# 完整恢复到指定日期
./scripts/backup/restore.sh full 2026-04-04
```

### 3.4 时间点恢复 (PITR)

```bash
# 1. 停止应用服务
kubectl scale deployment ai-ready-api --replicas=0 -n ai-ready-prod

# 2. 恢复基础备份
pg_restore -d ai_ready_prod base_backup.dump

# 3. 应用WAL日志
recover_target_time = '2026-04-04 10:00:00'

# 4. 重启服务
kubectl scale deployment ai-ready-api --replicas=3 -n ai-ready-prod
```

---

## 四、演练计划

### 4.1 演练频率

| 演练类型 | 频率 | 参与人员 |
|----------|------|----------|
| 备份验证 | 每周 | 运维 |
| 单表恢复 | 每月 | 运维+开发 |
| 完整恢复 | 每季度 | 运维+开发+DBA |
| 灾难恢复 | 每年 | 全员 |

### 4.2 演练记录

```markdown
## 备份恢复演练记录

**演练日期**: YYYY-MM-DD
**演练类型**: 完整恢复/单表恢复/PITR
**参与人员**: 
**演练环境**: 开发/测试/预发布

### 演练步骤
1. 
2. 
3. 

### 演练结果
- 成功/失败
- 恢复耗时: 
- 数据完整性验证: 

### 问题与改进
- 
- 
```

---

## 五、应急联系

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 运维负责人 | - | - | ops@ai-ready.cn |
| DBA | - | - | dba@ai-ready.cn |
| 安全负责人 | - | - | security@ai-ready.cn |

---

**文档版本**: v2.0.0  
**最后更新**: 2026-04-04  
**维护人**: devops-engineer
