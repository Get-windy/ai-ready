# AI-Ready 数据库备份恢复操作文档

**版本**: v1.0  
**日期**: 2026-04-01  
**作者**: devops-engineer

---

## 一、备份策略

### 1.1 备份类型

| 备份类型 | 频率 | 保留时间 | 存储位置 |
|----------|------|----------|----------|
| 全量备份 | 每日凌晨2:00 | 30天 | /backup/ai-ready/db/full/ |
| 增量备份 | 每小时 | 7天 | /backup/ai-ready/db/wal/ |
| 周备份 | 每周日 | 90天 | /backup/ai-ready/db/full/ |
| 月备份 | 每月1日 | 365天 | /backup/ai-ready/db/full/ |

### 1.2 备份内容

- 数据库结构 (DDL)
- 数据内容 (DML)
- 索引定义
- 约束条件
- 序列值

---

## 二、备份脚本使用

### 2.1 全量备份

```bash
# 执行全量备份
./db-backup.sh

# 指定参数
DB_HOST=192.168.1.100 DB_NAME=ai_ready ./db-backup.sh
```

### 2.2 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_HOST | localhost | 数据库主机 |
| DB_PORT | 5432 | 数据库端口 |
| DB_NAME | ai_ready | 数据库名称 |
| DB_USER | postgres | 数据库用户 |
| BACKUP_DIR | /backup/ai-ready/db | 备份目录 |
| RETENTION_DAYS | 30 | 保留天数 |

---

## 三、恢复操作

### 3.1 列出可用备份

```bash
./db-restore.sh list
```

### 3.2 恢复最新备份

```bash
./db-restore.sh restore
```

### 3.3 恢复指定备份

```bash
./db-restore.sh restore /backup/ai-ready/db/full/ai_ready_full_20260401_020000.sql.gz
```

### 3.4 验证恢复

```bash
./db-restore.sh verify
```

---

## 四、定时任务配置

### 4.1 Crontab配置

```bash
# 编辑crontab
crontab -e

# 添加定时任务
# 每日凌晨2点全量备份
0 2 * * * /opt/ai-ready/scripts/backup/db-backup.sh >> /var/log/ai-ready/backup.log 2>&1

# 每小时增量备份
0 * * * * /opt/ai-ready/scripts/backup/db-backup.sh incremental >> /var/log/ai-ready/backup.log 2>&1
```

---

## 五、监控告警

### 5.1 备份监控

| 监控项 | 阈值 | 告警级别 |
|--------|------|----------|
| 备份失败 | 1次 | P1 |
| 备份延迟 | >2小时 | P2 |
| 备份文件损坏 | 1次 | P0 |
| 存储空间不足 | <10GB | P2 |

### 5.2 验证脚本

```bash
# 检查最近备份时间
find /backup/ai-ready/db/full -name "*.sql.gz" -mtime -1 | wc -l

# 检查备份大小
du -sh /backup/ai-ready/db/full/
```

---

## 六、应急恢复流程

### 6.1 数据丢失恢复

1. **停止应用服务**
   ```bash
   systemctl stop ai-ready-api
   ```

2. **确认备份文件**
   ```bash
   ./db-restore.sh list
   ```

3. **执行恢复**
   ```bash
   ./db-restore.sh restore /backup/ai-ready/db/full/最新备份文件.sql.gz
   ```

4. **验证数据**
   ```bash
   ./db-restore.sh verify
   ```

5. **重启服务**
   ```bash
   systemctl start ai-ready-api
   ```

---

**文档完成时间**: 2026-04-01  
**版本**: v1.0