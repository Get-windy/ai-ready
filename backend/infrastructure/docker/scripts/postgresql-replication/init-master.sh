#!/bin/bash
# PostgreSQL 主节点初始化脚本
# 配置主节点支持流复制

set -e

echo "=== PostgreSQL 主节点初始化 ==="
echo "数据库: ${POSTGRES_DB}"
echo "复制用户: ${REPLICATION_USER}"
echo ""

# 等待 PostgreSQL 启动
echo "等待 PostgreSQL 启动..."
until pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}; do
  echo "PostgreSQL 未就绪，等待 5 秒..."
  sleep 5
done

echo "PostgreSQL 已就绪，开始配置主节点..."

# 配置 PostgreSQL 复制参数
echo "配置复制参数..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
-- 设置 WAL 级别为 replica
ALTER SYSTEM SET wal_level = replica;
-- 设置最大 WAL 发送进程数
ALTER SYSTEM SET max_wal_senders = 10;
-- 设置最大复制槽数
ALTER SYSTEM SET max_replication_slots = 10;
-- 启用热备
ALTER SYSTEM SET hot_standby = on;
-- 启用热备反馈
ALTER SYSTEM SET hot_standby_feedback = on;
-- 设置同步提交
ALTER SYSTEM SET synchronous_commit = on;
-- 设置同步备机名称（所有备机）
ALTER SYSTEM SET synchronous_standby_names = '*';
EOF

# 创建复制用户
echo "创建复制用户 ${REPLICATION_USER}..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
CREATE USER ${REPLICATION_USER} WITH REPLICATION PASSWORD '${REPLICATION_PASSWORD}';
GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${REPLICATION_USER};
EOF

# 创建复制槽（为每个从节点创建一个）
echo "创建复制槽..."
for i in {1..2}; do
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
    -c "SELECT pg_create_physical_replication_slot('replica_slot_${i}');"
done

# 重新加载配置
echo "重新加载 PostgreSQL 配置..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "SELECT pg_reload_conf();"

# 验证配置
echo "验证配置..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
SELECT name, setting FROM pg_settings 
WHERE name IN ('wal_level', 'max_wal_senders', 'max_replication_slots', 'hot_standby', 'synchronous_commit');
EOF

echo "主节点初始化完成"
echo "复制用户: ${REPLICATION_USER}"
echo "复制密码: ${REPLICATION_PASSWORD}"
echo "复制槽: replica_slot_1, replica_slot_2"