#!/bin/bash
# ============================================================
# 自动回滚脚本 - Sprint 27+1
# 最后更新: 2026-04-27
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$PROJECT_DIR/backups/test"
DEPLOY_ENV="test"

usage() {
    echo "使用方法: $0 [backup_timestamp]"
    echo ""
    echo "示例:"
    echo "  $0                      # 回滚到最新备份"
    echo "  $0 20240427_120000      # 回滚到指定时间戳"
    exit 1
}

# 获取时间戳
if [ -n "$1" ]; then
    BACKUP_TIME="$1"
else
    # 获取最新备份
    BACKUP_TIME=$(ls -t "$BACKUP_DIR" | head -1)
fi

if [ -z "$BACKUP_TIME" ]; then
    echo "[ERROR] 未找到可回滚的备份"
    exit 1
fi

echo "[INFO] 开始回滚到备份: $BACKUP_TIME"
BACKUP_PATH="$BACKUP_DIR/$BACKUP_TIME"

# 检查备份是否存在
if [ ! -d "$BACKUP_PATH" ]; then
    echo "[ERROR] 备份目录不存在: $BACKUP_PATH"
    exit 1
fi

# 恢复PostgreSQL
if [ -f "$BACKUP_PATH/postgres_$BACKUP_TIME.sql" ]; then
    echo "[INFO] 恢复PostgreSQL数据..."
    docker-compose -f "infra/docker/docker-compose.optimized.yml" exec -T postgres psql -U appuser -d qizhilian < "$BACKUP_PATH/postgres_$BACKUP_TIME.sql" 2>/dev/null || echo "[WARN] PostgreSQL恢复失败"
else
    echo "[INFO] PostgreSQL备份文件不存在，跳过"
fi

# 恢复Redis
if [ -f "$BACKUP_PATH/redis_dump.rdb" ]; then
    echo "[INFO] 恢复Redis数据..."
    docker-compose -f "infra/docker/docker-compose.optimized.yml" cp "$BACKUP_PATH/redis_dump.rdb" redis:/data/dump.rdb
else
    echo "[INFO] Redis备份文件不存在，跳过"
fi

# 重启服务
echo "[INFO] 重启服务..."
docker-compose -f "infra/docker/docker-compose.optimized.yml" restart

echo "[INFO] 回滚完成!"