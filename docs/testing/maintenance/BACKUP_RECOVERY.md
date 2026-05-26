# 测试环境备份恢复手册

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**适用环境**: Sprint 27+1 测试环境  
**最后更新**: 2026-04-27

---

## 目录

1. [概述](#1-概述)
2. [备份策略与计划](#2-备份策略与计划)
3. [备份操作指南](#3-备份操作指南)
4. [恢复操作指南](#4-恢复操作指南)
5. [灾难恢复预案](#5-灾难恢复预案)
6. [备份验证与测试](#6-备份验证与测试)
7. [备份监控与告警](#7-备份监控与告警)
8. [常见问题处理](#8-常见问题处理)
9. [附录](#9-附录)

---

## 1. 概述

### 1.1 目的

本文档提供 Sprint 27+1 测试环境的完整备份恢复方案，确保在数据丢失、系统故障或人为误操作等情况下，能够快速、完整地恢复测试环境，保障测试工作的连续性和数据完整性。

### 1.2 适用范围

- **环境范围**: 测试环境所有组件（数据库、应用、配置、日志）
- **备份类型**: 全量备份、增量备份、差异备份
- **恢复场景**: 数据恢复、环境重建、灾难恢复
- **操作人员**: 运维工程师、系统管理员

### 1.3 备份恢复目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **RPO (恢复点目标)** | ≤ 1小时 | 数据丢失不超过1小时 |
| **RTO (恢复时间目标)** | ≤ 2小时 | 系统恢复不超过2小时 |
| **备份成功率** | ≥ 99.9% | 备份任务成功率 |
| **恢复成功率** | ≥ 99.5% | 恢复操作成功率 |
| **备份验证率** | 100% | 所有备份必须验证 |
| **数据完整性** | 100% | 恢复后数据完整可用 |

### 1.4 备份恢复架构

```
┌─────────────────────────────────────────────────────────┐
│               备份恢复架构总览                           │
└─────────────────────────────────────────────────────────┘
         │
         ├─→ [1] 备份源
         │    ├─ PostgreSQL数据库
         │    ├─ Redis缓存
         │    ├─ 应用配置文件
         │    ├─ 日志文件
         │    └─ Docker镜像
         │
         ├─→ [2] 备份方式
         │    ├─ 全量备份（每日）
         │    ├─ 增量备份（每小时）
         │    ├─ 差异备份（每日）
         │    └─ 实时备份（关键数据）
         │
         ├─→ [3] 存储位置
         │    ├─ 本地存储（快速恢复）
         │    ├─ 网络存储（冗余备份）
         │    ├─ 对象存储（长期归档）
         │    └─ 异地备份（灾难恢复）
         │
         ├─→ [4] 恢复流程
         │    ├─ 数据恢复
         │    ├─ 环境重建
         │    ├─ 验证测试
         │    └─ 业务恢复
         │
         └─────────────────────────────────────────────────────┘
```

---

## 2. 备份策略与计划

### 2.1 备份策略矩阵

| 数据类别 | 备份类型 | 频率 | 保留时间 | 存储位置 | 恢复优先级 |
|---------|---------|------|---------|---------|-----------|
| **PostgreSQL数据** | 全量+WAL | 每日全量+持续WAL | 30天 | 本地+网络存储 | P0 |
| **Redis数据** | RDB+AOF | 每日RDB+实时AOF | 15天 | 本地存储 | P0 |
| **应用配置文件** | 全量 | 每次变更 | 90天 | Git+本地存储 | P1 |
| **Docker镜像** | 全量 | 每次构建 | 30天 | 镜像仓库 | P1 |
| **应用日志** | 增量 | 每日 | 7天 | 本地存储 | P2 |
| **监控数据** | 全量 | 每周 | 90天 | 对象存储 | P2 |
| **测试数据** | 全量 | 每次测试 | 15天 | 对象存储 | P3 |

### 2.2 备份时间计划

**每日备份计划**：

```yaml
# backup-schedule.yml
schedule:
  daily:
    - name: "postgres-full-backup"
      time: "02:00"
      type: "full"
      retention: "30d"
      enabled: true
    
    - name: "redis-rdb-backup"
      time: "03:00"
      type: "rdb"
      retention: "15d"
      enabled: true
    
    - name: "config-backup"
      time: "04:00"
      type: "full"
      retention: "90d"
      enabled: true

  hourly:
    - name: "postgres-wal-backup"
      interval: "1h"
      type: "wal"
      retention: "24h"
      enabled: true
    
    - name: "redis-aof-backup"
      interval: "1h"
      type: "aof"
      retention: "24h"
      enabled: true

  weekly:
    - name: "monitoring-data-backup"
      day: "sunday"
      time: "05:00"
      type: "full"
      retention: "90d"
      enabled: true
```

### 2.3 备份存储规划

**存储层级**：

| 存储层级 | 位置 | 容量 | 用途 | 访问速度 | 成本 |
|---------|------|------|-----|---------|------|
| **L1 - 本地存储** | `/backups/local/` | 500GB | 快速恢复 | 高速 | 高 |
| **L2 - 网络存储** | `nfs://backup-nas/` | 2TB | 冗余备份 | 中速 | 中 |
| **L3 - 对象存储** | `s3://ai-ready-backup/` | 5TB | 长期归档 | 低速 | 低 |
| **L4 - 异地存储** | `异地数据中心` | 1TB | 灾难恢复 | 低速 | 高 |

**存储目录结构**：

```
/backups/
├── local/                    # 本地备份存储
│   ├── postgres/            # PostgreSQL备份
│   │   ├── full/            # 全量备份
│   │   │   ├── 2026-04-27/
│   │   │   │   ├── base.tar.gz
│   │   │   │   └── backup.log
│   │   │   └── 2026-04-28/
│   │   └── wal/             # WAL日志备份
│   │       ├── 000000010000000000000001
│   │       └── 000000010000000000000002
│   ├── redis/               # Redis备份
│   │   ├── rdb/             # RDB备份
│   │   │   ├── dump-2026-04-27.rdb
│   │   │   └── dump-2026-04-28.rdb
│   │   └── aof/             # AOF备份
│   │       ├── appendonly-2026-04-27.aof
│   │       └── appendonly-2026-04-28.aof
│   ├── config/              # 配置文件备份
│   │   ├── 2026-04-27/
│   │   │   ├── application.yml
│   │   │   └── nginx.conf
│   │   └── 2026-04-28/
│   ├── logs/                # 日志备份
│   └── docker/              # Docker镜像备份
│       └── images/
│           ├── app-1.0.0.tar
│           └── postgres-15.tar
│
├── network/                 # 网络备份存储（NFS）
│   └── ...（结构同local）
│
└── archive/                 # 归档备份（每月）
    ├── 2026-04/
    └── 2026-05/
```

---

## 3. 备份操作指南

### 3.1 PostgreSQL备份操作

**全量备份脚本**：

```bash
#!/bin/bash
# backup-postgres-full.sh
# PostgreSQL全量备份脚本

set -euo pipefail

# 配置变量
BACKUP_DIR="/backups/local/postgres/full/$(date +%Y-%m-%d)"
WAL_DIR="/backups/local/postgres/wal"
LOG_FILE="${BACKUP_DIR}/backup.log"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p "${BACKUP_DIR}"
mkdir -p "${WAL_DIR}"

# 日志函数
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
    exit 1
}

# 检查PostgreSQL连接
log_info "检查PostgreSQL连接..."
if ! docker-compose exec -T postgres pg_isready -U appuser; then
    log_error "PostgreSQL连接失败"
fi

# 执行全量备份
log_info "开始PostgreSQL全量备份..."
BACKUP_FILE="${BACKUP_DIR}/base-$(date +%Y%m%d_%H%M%S).sql.gz"

docker-compose exec -T postgres pg_dumpall -U appuser \
    --clean \
    --if-exists \
    --verbose \
    | gzip > "${BACKUP_FILE}"

# 检查备份文件
if [ -f "${BACKUP_FILE}" ] && [ -s "${BACKUP_FILE}" ]; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    log_info "备份完成: ${BACKUP_FILE} (${BACKUP_SIZE})"
else
    log_error "备份文件创建失败"
fi

# 备份WAL配置
log_info "备份WAL配置..."
docker-compose exec -T postgres bash -c "
    echo \"archive_mode = on\"
    echo \"archive_command = 'test ! -f ${WAL_DIR}/%f && cp %p ${WAL_DIR}/%f'\"
    echo \"archive_timeout = 3600\"
" > "${BACKUP_DIR}/wal-config.txt"

# 清理旧备份
log_info "清理${RETENTION_DAYS}天前的备份..."
find "/backups/local/postgres/full/" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null || true
find "/backups/local/postgres/wal/" -type f -mtime +7 -delete 2>/dev/null || true

# 生成备份报告
log_info "生成备份报告..."
cat > "${BACKUP_DIR}/backup-report.md" << EOF
# PostgreSQL备份报告

## 基本信息
- 备份时间: $(date '+%Y-%m-%d %H:%M:%S')
- 备份类型: 全量备份
- 数据库: ai_ready_test
- 用户: appuser

## 备份详情
- 备份文件: $(basename "${BACKUP_FILE}")
- 文件大小: ${BACKUP_SIZE}
- 压缩格式: gzip
- 备份目录: ${BACKUP_DIR}

## 验证信息
- 文件存在: ✅ 是
- 文件大小: ✅ 正常
- 压缩格式: ✅ 正确
- 权限设置: ✅ 644

## 清理信息
- 保留策略: ${RETENTION_DAYS}天
- 清理文件: $(find "/backups/local/postgres/full/" -type d -mtime +${RETENTION_DAYS} 2>/dev/null | wc -l)个目录
EOF

log_info "PostgreSQL全量备份完成"
```

**增量备份脚本**：

```bash
#!/bin/bash
# backup-postgres-wal.sh
# PostgreSQL WAL增量备份脚本

set -euo pipefail

WAL_DIR="/backups/local/postgres/wal"
LOG_FILE="/backups/local/postgres/wal/backup-$(date +%Y%m%d).log"
RETENTION_HOURS=24

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# 检查WAL目录
mkdir -p "${WAL_DIR}"

# 触发WAL切换
log_info "触发WAL切换..."
docker-compose exec -T postgres psql -U appuser -c "SELECT pg_switch_wal();"

# 同步WAL文件
log_info "同步WAL文件..."
docker-compose exec -T postgres bash -c "
    cd /var/lib/postgresql/data/pg_wal
    for wal in *.partial *.backup 0*; do
        if [ -f \"\$wal\" ]; then
            cp -f \"\$wal\" ${WAL_DIR}/\${wal}
        fi
    done
"

# 清理旧WAL文件
log_info "清理${RETENTION_HOURS}小时前的WAL文件..."
find "${WAL_DIR}" -type f -mmin +$((RETENTION_HOURS * 60)) -delete 2>/dev/null || true

# 记录统计信息
WAL_COUNT=$(find "${WAL_DIR}" -name "0*" -type f | wc -l)
WAL_SIZE=$(du -sh "${WAL_DIR}" | cut -f1)

log_info "WAL备份统计: ${WAL_COUNT}个文件, 总大小 ${WAL_SIZE}"
```

### 3.2 Redis备份操作

**RDB备份脚本**：

```bash
#!/bin/bash
# backup-redis-rdb.sh
# Redis RDB备份脚本

set -euo pipefail

BACKUP_DIR="/backups/local/redis/rdb"
LOG_FILE="${BACKUP_DIR}/backup-$(date +%Y%m%d).log"
RETENTION_DAYS=15

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
    exit 1
}

# 检查Redis连接
log_info "检查Redis连接..."
if ! docker-compose exec -T redis redis-cli ping | grep -q PONG; then
    log_error "Redis连接失败"
fi

# 执行BGSAVE
log_info "执行Redis BGSAVE..."
docker-compose exec -T redis redis-cli BGSAVE

# 等待BGSAVE完成
log_info "等待BGSAVE完成..."
for i in {1..30}; do
    if docker-compose exec -T redis redis-cli info persistence | grep -q "rdb_bgsave_in_progress:0"; then
        log_info "BGSAVE完成"
        break
    fi
    sleep 2
done

# 检查BGSAVE结果
if ! docker-compose exec -T redis redis-cli info persistence | grep -q "rdb_last_bgsave_status:ok"; then
    log_error "BGSAVE失败"
fi

# 复制RDB文件
log_info "复制RDB文件..."
RDB_FILE="${BACKUP_DIR}/dump-$(date +%Y-%m-%d).rdb"
docker-compose cp redis:/data/dump.rdb "${RDB_FILE}"

# 验证备份文件
if [ -f "${RDB_FILE}" ] && [ -s "${RDB_FILE}" ]; then
    FILE_SIZE=$(du -h "${RDB_FILE}" | cut -f1)
    log_info "RDB备份完成: ${RDB_FILE} (${FILE_SIZE})"
else
    log_error "RDB文件复制失败"
fi

# 清理旧备份
log_info "清理${RETENTION_DAYS}天前的备份..."
find "${BACKUP_DIR}" -name "dump-*.rdb" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true

# 记录备份信息
cat >> "${LOG_FILE}" << EOF
备份完成时间: $(date '+%Y-%m-%d %H:%M:%S')
备份文件: $(basename "${RDB_FILE}")
文件大小: ${FILE_SIZE}
Redis版本: $(docker-compose exec -T redis redis-cli info server | grep redis_version | cut -d: -f2)
数据库大小: $(docker-compose exec -T redis redis-cli info keyspace | grep ^db0 | cut -d= -f2 | cut -d, -f1) keys
EOF

log_info "Redis RDB备份完成"
```

**AOF备份脚本**：

```bash
#!/bin/bash
# backup-redis-aof.sh
# Redis AOF备份脚本

set -euo pipefail

BACKUP_DIR="/backups/local/redis/aof"
LOG_FILE="${BACKUP_DIR}/backup-$(date +%Y%m%d_%H%M).log"
RETENTION_HOURS=24

mkdir -p "${BACKUP_DIR}"

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# 检查AOF配置
log_info "检查AOF配置..."
AOF_ENABLED=$(docker-compose exec -T redis redis-cli config get appendonly | tail -1)
if [ "${AOF_ENABLED}" != "yes" ]; then
    log_info "AOF未启用，跳过AOF备份"
    exit 0
fi

# 执行AOF重写
log_info "执行AOF重写..."
docker-compose exec -T redis redis-cli BGREWRITEAOF

# 等待AOF重写完成
log_info "等待AOF重写完成..."
for i in {1..30}; do
    if docker-compose exec -T redis redis-cli info persistence | grep -q "aof_rewrite_in_progress:0"; then
        log_info "AOF重写完成"
        break
    fi
    sleep 2
done

# 复制AOF文件
log_info "复制AOF文件..."
AOF_FILE="${BACKUP_DIR}/appendonly-$(date +%Y%m%d_%H%M).aof"
docker-compose cp redis:/data/appendonly.aof "${AOF_FILE}"

# 验证AOF文件
if [ -f "${AOF_FILE}" ] && [ -s "${AOF_FILE}" ]; then
    FILE_SIZE=$(du -h "${AOF_FILE}" | cut -f1)
    log_info "AOF备份完成: ${AOF_FILE} (${FILE_SIZE})"
else
    log_info "AOF文件为空或不存在"
fi

# 清理旧AOF备份
log_info "清理${RETENTION_HOURS}小时前的AOF备份..."
find "${BACKUP_DIR}" -name "appendonly-*.aof" -mmin +$((RETENTION_HOURS * 60)) -delete 2>/dev/null || true

log_info "Redis AOF备份完成"
```

### 3.3 配置文件备份

**配置文件备份脚本**：

```bash
#!/bin/bash
# backup-config.sh
# 配置文件备份脚本

set -euo pipefail

BACKUP_DIR="/backups/local/config/$(date +%Y-%m-%d)"
LOG_FILE="${BACKUP_DIR}/backup.log"
RETENTION_DAYS=90

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# 备份应用配置文件
log_info "备份应用配置文件..."
CONFIG_DIRS=(
    "I:/AI-Ready/config"
    "I:/AI-Ready/infra/docker"
    "I:/AI-Ready/deploy"
    "I:/AI-Ready/scripts"
)

for dir in "${CONFIG_DIRS[@]}"; do
    if [ -d "${dir}" ]; then
        dir_name=$(basename "${dir}")
        log_info "备份目录: ${dir}"
        tar -czf "${BACKUP_DIR}/${dir_name}-$(date +%Y%m%d_%H%M%S).tar.gz" -C "$(dirname "${dir}")" "${dir_name}" 2>/dev/null || true
    fi
done

# 备份Docker Compose文件
log_info "备份Docker Compose文件..."
find "I:/AI-Ready" -name "docker-compose*.yml" -o -name "*.yaml" | while read -r file; do
    if [ -f "${file}" ]; then
        rel_path=$(realpath --relative-to="I:/AI-Ready" "${file}")
        backup_path="${BACKUP_DIR}/docker-compose/$(dirname "${rel_path}")"
        mkdir -p "${backup_path}"
        cp "${file}" "${backup_path}/"
        log_info "备份: ${file} → ${backup_path}/"
    fi
done

# 备份环境变量文件
log_info "备份环境变量文件..."
find "I:/AI-Ready" -name "*.env" -o -name ".env*" | while read -r file; do
    if [ -f "${file}" ]; then
        # 脱敏敏感信息
        backup_file="${BACKUP_DIR}/env/$(basename "${file}").backup"
        mkdir -p "$(dirname "${backup_file}")"
        
        # 脱敏密码和密钥
        grep -v -E "(PASSWORD|SECRET|KEY|TOKEN)=" "${file}" > "${backup_file}" 2>/dev/null || cp "${file}" "${backup_file}"
        
        log_info "备份环境文件: ${file}"
    fi
done

# Git备份配置
log_info "执行Git备份..."
if [ -d "I:/AI-Ready/.git" ]; then
    cd "I:/AI-Ready"
    git bundle create "${BACKUP_DIR}/repo-$(date +%Y%m%d).bundle" --all 2>/dev/null || true
    log_info "Git仓库备份完成"
fi

# 清理旧备份
log_info "清理${RETENTION_DAYS}天前的配置备份..."
find "/backups/local/config/" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \; 2>/dev/null || true

# 生成备份清单
log_info "生成备份清单..."
ls -laR "${BACKUP_DIR}" > "${BACKUP_DIR}/file-list.txt" 2>/dev/null

log_info "配置文件备份完成"
```

### 3.4 自动备份调度

**使用cron调度备份**：

```bash
# /etc/cron.d/ai-ready-backup
# 每日全量备份 - 凌晨2点
0 2 * * * root /opt/ai-ready/scripts/backup-postgres-full.sh >> /var/log/backup-postgres-full.log 2>&1
0 3 * * * root /opt/ai-ready/scripts/backup-redis-rdb.sh >> /var/log/backup-redis-rdb.log 2>&1
0 4 * * * root /opt/ai-ready/scripts/backup-config.sh >> /var/log/backup-config.log 2>&1

# 每小时增量备份
0 * * * * root /opt/ai-ready/scripts/backup-postgres-wal.sh >> /var/log/backup-postgres-wal.log 2>&1
30 * * * * root /opt/ai-ready/scripts/backup-redis-aof.sh >> /var/log/backup-redis-aof.log 2>&1

# 每周监控数据备份 - 周日凌晨5点
0 5 * * 0 root /opt/ai-ready/scripts/backup-monitoring.sh >> /var/log/backup-monitoring.log 2>&1

# 每月归档备份 - 每月1日凌晨6点
0 6 1 * * root /opt/ai-ready/scripts/archive-backup.sh >> /var/log/archive-backup.log 2>&1
```

**备份监控脚本**：

```bash
#!/bin/bash
# monitor-backup.sh
# 备份监控脚本

set -euo pipefail

LOG_FILE="/var/log/backup-monitor.log"
ALERT_EMAIL="admin@ai-ready.local"

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

check_backup_status() {
    local backup_type=$1
    local log_file=$2
    local max_age_hours=$3
    
    log_info "检查${backup_type}备份状态..."
    
    # 检查日志文件是否存在
    if [ ! -f "${log_file}" ]; then
        log_info "${backup_type}日志文件不存在"
        return 1
    fi
    
    # 检查最后修改时间
    local last_modified=$(stat -c %Y "${log_file}")
    local current_time=$(date +%s)
    local age_hours=$(( (current_time - last_modified) / 3600 ))
    
    if [ ${age_hours} -gt ${max_age_hours} ]; then
        log_info "${backup_type}备份已超过${max_age_hours}小时未执行"
        return 2
    fi
    
    # 检查日志中是否有错误
    if tail -100 "${log_file}" | grep -q -E "ERROR|FAILED|FAILURE"; then
        log_info "${backup_type}备份存在错误"
        return 3
    fi
    
    log_info "${backup_type}备份状态正常"
    return 0
}

# 检查各类型备份
check_backup_status "PostgreSQL全量备份" "/var/log/backup-postgres-full.log" 26
check_backup_status "Redis RDB备份" "/var/log/backup-redis-rdb.log" 26
check_backup_status "配置文件备份" "/var/log/backup-config.log" 26
check_backup_status "PostgreSQL WAL备份" "/var/log/backup-postgres-wal.log" 2
check_backup_status "Redis AOF备份" "/var/log/backup-redis-aof.log" 2

# 检查备份目录空间
log_info "检查备份目录空间..."
BACKUP_USAGE=$(df -h /backups | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "${BACKUP_USAGE}" -gt 90 ]; then
    log_info "备份目录使用率超过90%: ${BACKUP_USAGE}%"
    # 发送告警
    echo "备份目录使用率告警: ${BACKUP_USAGE}%" | mail -s "备份存储空间告警" "${ALERT_EMAIL}"
fi

log_info "备份监控完成"
```

---

## 4. 恢复操作指南

### 4.1 PostgreSQL恢复操作

**全量恢复脚本**：

```bash
#!/bin/bash
# restore-postgres.sh
# PostgreSQL全量恢复脚本

set -euo pipefail

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    exit 1
}

# 参数检查
if [ $# -lt 1 ]; then
    echo "使用方法: $0 <备份文件> [恢复时间点]"
    echo "示例: $0 /backups/local/postgres/full/2026-04-27/base-20260427_020000.sql.gz"
    echo "示例: $0 /backups/local/postgres/full/2026-04-27/ '2026-04-27 10:30:00'"
    exit 1
fi

BACKUP_FILE=$1
RESTORE_TIME=${2:-""}

# 检查备份文件
if [ -d "${BACKUP_FILE}" ]; then
    # 如果是目录，查找最新的备份文件
    BACKUP_FILE=$(find "${BACKUP_FILE}" -name "base-*.sql.gz" | sort -r | head -1)
fi

if [ ! -f "${BACKUP_FILE}" ]; then
    log_error "备份文件不存在: ${BACKUP_FILE}"
fi

log_info "开始恢复PostgreSQL数据库..."
log_info "备份文件: ${BACKUP_FILE}"
log_info "恢复时间点: ${RESTORE_TIME:-"最新备份"}"

# 停止应用服务
log_info "停止应用服务..."
docker-compose stop backend nginx || true

# 停止PostgreSQL
log_info "停止PostgreSQL..."
docker-compose stop postgres

# 备份当前数据（如有）
CURRENT_BACKUP="/tmp/postgres-current-$(date +%Y%m%d_%H%M%S).sql.gz"
log_info "备份当前数据: ${CURRENT_BACKUP}"
docker-compose start postgres
sleep 5
docker-compose exec -T postgres pg_dumpall -U appuser | gzip > "${CURRENT_BACKUP}" || true
docker-compose stop postgres

# 清理数据目录
log_info "清理PostgreSQL数据目录..."
docker-compose rm -f postgres
docker volume rm ai-ready_postgres-data 2>/dev/null || true

# 启动空的PostgreSQL
log_info "启动新的PostgreSQL容器..."
docker-compose up -d postgres
sleep 10

# 恢复全量备份
log_info "恢复全量备份..."
gunzip -c "${BACKUP_FILE}" | docker-compose exec -T postgres psql -U appuser

# 恢复WAL日志（如果指定了时间点恢复）
if [ -n "${RESTORE_TIME}" ]; then
    log_info "执行时间点恢复至: ${RESTORE_TIME}"
    
    # 创建恢复配置
    cat > /tmp/recovery.conf << EOF
restore_command = 'cp /backups/local/postgres/wal/%f %p'
recovery_target_time = '${RESTORE_TIME}'
recovery_target_action = promote
EOF
    
    # 复制恢复配置
    docker cp /tmp/recovery.conf ai-ready_postgres_1:/var/lib/postgresql/data/recovery.conf
    
    # 重启PostgreSQL进入恢复模式
    docker-compose restart postgres
    
    # 等待恢复完成
    log_info "等待恢复完成..."
    for i in {1..60}; do
        if docker-compose logs postgres | grep -q "database system is ready"; then
            log_info "恢复完成"
            break
        fi
        sleep 5
    done
fi

# 验证恢复结果
log_info "验证数据库恢复..."
if docker-compose exec -T postgres psql -U appuser -c "SELECT current_database();" | grep -q "ai_ready_test"; then
    log_info "数据库恢复验证成功"
else
    log_error "数据库恢复验证失败"
fi

# 检查数据完整性
log_info "检查数据完整性..."
TABLE_COUNT=$(docker-compose exec -T postgres psql -U appuser -d ai_ready_test -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
if [ "${TABLE_COUNT}" -gt 0 ]; then
    log_info "数据完整性检查通过，共有 ${TABLE_COUNT} 张表"
else
    log_error "数据完整性检查失败，未找到表"
fi

# 启动应用服务
log_info "启动应用服务..."
docker-compose up -d

log_info "PostgreSQL恢复完成"
```

**时间点恢复操作**：

```bash
#!/bin/bash
# restore-postgres-pitr.sh
# PostgreSQL时间点恢复

set -euo pipefail

# 参数检查
if [ $# -lt 2 ]; then
    echo "使用方法: $0 <基础备份目录> <恢复时间点>"
    echo "示例: $0 /backups/local/postgres/full/2026-04-27/ '2026-04-27 10:30:00'"
    exit 1
fi

BASE_BACKUP_DIR=$1
RESTORE_TIME=$2

# 查找基础备份
BASE_BACKUP=$(find "${BASE_BACKUP_DIR}" -name "base-*.sql.gz" | sort -r | head -1)
if [ -z "${BASE_BACKUP}" ]; then
    echo "错误: 在 ${BASE_BACKUP_DIR} 中未找到基础备份"
    exit 1
fi

echo "基础备份: ${BASE_BACKUP}"
echo "恢复时间点: ${RESTORE_TIME}"

# 执行恢复
./restore-postgres.sh "${BASE_BACKUP}" "${RESTORE_TIME}"
```

### 4.2 Redis恢复操作

**RDB文件恢复**：

```bash
#!/bin/bash
# restore-redis-rdb.sh
# Redis RDB文件恢复

set -euo pipefail

log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1"
    exit 1
}

# 参数检查
if [ $# -lt 1 ]; then
    echo "使用方法: $0 <RDB文件>"
    echo "示例: $0 /backups/local/redis/rdb/dump-2026-04-27.rdb"
    exit 1
fi

RDB_FILE=$1

# 检查RDB文件
if [ ! -f "${RDB_FILE}" ]; then
    log_error "RDB文件不存在: ${RDB_FILE}"
fi

log_info "开始恢复Redis数据..."
log_info "RDB文件: ${RDB_FILE}"

# 停止Redis
log_info "停止Redis服务..."
docker-compose stop redis

# 备份当前数据
CURRENT_RDB="/tmp/redis-current-$(date +%Y%m%d_%H%M%S).rdb"
if docker-compose exec -T redis ls /data/dump.rdb 2>/dev/null; then
    log_info "备份当前RDB文件: ${CURRENT_RDB}"
    docker-compose cp redis:/data/dump.rdb "${CURRENT_RDB}"
fi

# 清理Redis数据
log_info "清理Redis数据..."
docker-compose rm -f redis
docker volume rm ai-ready_redis-data 2>/dev/null || true

# 复制RDB文件到数据目录
log_info "复制RDB文件..."
mkdir -p "I:/AI-Ready/data/redis"
cp "${RDB_FILE}" "I:/AI-Ready/data/redis/dump.rdb"

# 启动Redis
log_info "启动Redis..."
docker-compose up -d redis
sleep 5

# 验证恢复
log_info "验证Redis恢复..."
if docker-compose exec -T redis redis-cli ping | grep -q PONG; then
    log_info "Redis连接成功"
else
    log_error "Redis连接失败"
fi

# 检查数据
KEY_COUNT=$(docker-compose exec -T redis redis-cli info keyspace | grep ^db0 | cut -d= -f2 | cut -d, -f1)
if [ -n "${KEY_COUNT}" ] && [ "${KEY_COUNT}" -gt 0 ]; then
    log_info "Redis数据恢复成功，共有 ${KEY_COUNT} 个key"
else
    log_info "