#!/bin/bash
# PostgreSQL 从节点提升脚本
# 将从节点提升为主节点

set -e

echo "=== PostgreSQL 从节点提升 ==="
echo "当前节点: ${HOSTNAME}"
echo "数据库: ${POSTGRES_DB}"
echo ""

# 检查当前是否为从节点
echo "检查当前节点状态..."
STATUS=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -t -c "SELECT pg_is_in_recovery();" | tr -d '[:space:]')

if [ "${STATUS}" = "t" ]; then
  echo "当前节点为从节点，可以提升"
else
  echo "当前节点已为主节点，无需提升"
  exit 0
fi

# 询问确认
read -p "确定要将此从节点提升为主节点吗？(y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "取消提升操作"
  exit 0
fi

# 停止复制进程
echo "停止复制进程..."
psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "SELECT pg_wal_replay_pause();"

# 创建提升触发器文件
echo "创建提升触发器文件..."
touch /tmp/promote_trigger

# 等待 PostgreSQL 检测到触发器并提升
echo "等待 PostgreSQL 提升..."
sleep 10

# 验证提升结果
echo "验证提升结果..."
NEW_STATUS=$(psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -t -c "SELECT pg_is_in_recovery();" | tr -d '[:space:]')

if [ "${NEW_STATUS}" = "f" ]; then
  echo "✅ 从节点提升成功！"
  echo "当前节点已为主节点"
  
  # 清理复制配置
  echo "清理复制配置..."
  rm -f ${PGDATA}/postgresql.auto.conf
  
  # 重启 PostgreSQL 以应用新配置
  echo "重启 PostgreSQL 服务..."
  pg_ctl restart
  
  # 创建新的复制槽
  echo "创建新的复制槽..."
  psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} <<EOF
ALTER SYSTEM SET wal_level = replica;
ALTER SYSTEM SET max_wal_senders = 10;
ALTER SYSTEM SET max_replication_slots = 10;
ALTER SYSTEM SET hot_standby = on;
SELECT pg_reload_conf();
EOF
  
  for i in {1..2}; do
    psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
      -c "SELECT pg_create_physical_replication_slot('replica_slot_${i}_new');"
  done
  
  echo ""
  echo "=== 提升完成 ==="
  echo "1. 当前节点已成功提升为主节点"
  echo "2. 已创建新的复制槽: replica_slot_1_new, replica_slot_2_new"
  echo "3. PostgreSQL 已重启应用新配置"
  echo ""
  echo "下一步:"
  echo "1. 更新应用配置连接到新主节点"
  echo "2. 重新配置其他从节点连接到新主节点"
  echo "3. 更新监控配置"
  
else
  echo "❌ 从节点提升失败"
  echo "当前节点状态: ${NEW_STATUS}"
  exit 1
fi