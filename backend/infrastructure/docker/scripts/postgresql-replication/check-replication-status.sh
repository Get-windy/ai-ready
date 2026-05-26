#!/bin/bash
# PostgreSQL 复制状态检查脚本
# 检查主从复制状态和健康度

set -e

echo "=== PostgreSQL 复制状态检查 ==="
echo "时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 数据库列表
declare -A DATABASES=(
  ["main"]="gateway_user:gateway_pass_2026:gateway_db:5432"
  ["finance"]="finance_user:finance_pass_2026:finance_db:5433"
  ["inventory"]="inventory_user:inventory_pass_2026:inventory_db:5434"
  ["ai"]="ai_user:ai_pass_2026:ai_db:5435"
  ["data"]="data_user:data_pass_2026:data_db:5436"
)

# 检查主节点状态
echo "🔍 检查主节点状态..."
echo ""

for db in "${!DATABASES[@]}"; do
  IFS=':' read -r USER PASS DB_NAME PORT <<< "${DATABASES[$db]}"
  
  echo "📊 数据库: ${db} (${DB_NAME})"
  echo "----------------------------------------"
  
  # 检查主节点连接
  if pg_isready -h localhost -p ${PORT} -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
    echo -e "✅ 主节点连接正常 (localhost:${PORT})"
    
    # 检查主节点复制状态
    CONNECTION_STRING="postgresql://${USER}:${PASS}@localhost:${PORT}/${DB_NAME}"
    
    # 检查是否有活动的复制连接
    REPLICATION_COUNT=$(psql ${CONNECTION_STRING} -t -c "SELECT count(*) FROM pg_stat_replication;" | tr -d '[:space:]')
    
    if [ "${REPLICATION_COUNT}" -gt 0 ]; then
      echo -e "✅ 活动复制连接: ${REPLICATION_COUNT}"
      
      # 显示详细的复制状态
      echo "复制连接详情:"
      psql ${CONNECTION_STRING} <<EOF
SELECT 
  application_name,
  client_addr::text,
  state,
  sync_state,
  pg_wal_lsn_diff(sent_lsn, write_lsn) AS write_lag_bytes,
  pg_wal_lsn_diff(sent_lsn, flush_lsn) AS flush_lag_bytes,
  pg_wal_lsn_diff(sent_lsn, replay_lsn) AS replay_lag_bytes,
  to_char(pg_wal_lsn_diff(sent_lsn, replay_lsn) / 1024.0 / 1024.0, '999.99') AS replay_lag_mb,
  to_char(EXTRACT(EPOCH FROM (now() - replay_lag_time)), '999.99') AS replay_lag_seconds,
  to_char(backend_start, 'YYYY-MM-DD HH24:MI:SS') AS connected_since
FROM pg_stat_replication
ORDER BY application_name;
EOF
      
    else
      echo -e "${YELLOW}⚠️  无活动复制连接${NC}"
    fi
    
    # 检查复制槽状态
    echo "复制槽状态:"
    psql ${CONNECTION_STRING} <<EOF
SELECT 
  slot_name,
  slot_type,
  active,
  pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) AS restart_lag_bytes,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS restart_lag_pretty,
  pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn) AS confirmed_lag_bytes,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) AS confirmed_lag_pretty
FROM pg_replication_slots
ORDER BY slot_name;
EOF
    
  else
    echo -e "${RED}❌ 主节点连接失败 (localhost:${PORT})${NC}"
  fi
  
  echo ""
done

# 检查从节点状态
echo "🔍 检查从节点状态..."
echo ""

# 从节点端口映射
declare -A REPLICA_PORTS=(
  ["main"]="5437 5438"
  ["finance"]="5443 5444"
  ["inventory"]="5447 5448"
  ["ai"]="5453 5454"
  ["data"]="5457 5458"
)

for db in "${!REPLICA_PORTS[@]}"; do
  IFS=':' read -r USER PASS DB_NAME <<< "${DATABASES[$db]}"
  
  echo "📊 数据库: ${db} 从节点"
  echo "----------------------------------------"
  
  PORT_INDEX=1
  for PORT in ${REPLICA_PORTS[$db]}; do
    # 检查从节点连接
    if pg_isready -h localhost -p ${PORT} -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
      echo -e "✅ 从节点 ${PORT_INDEX} 连接正常 (localhost:${PORT})"
      
      CONNECTION_STRING="postgresql://${USER}:${PASS}@localhost:${PORT}/${DB_NAME}"
      
      # 检查从节点恢复状态
      psql ${CONNECTION_STRING} <<EOF
SELECT 
  pg_is_in_recovery() AS is_in_recovery,
  pg_last_wal_receive_lsn() AS last_receive_lsn,
  pg_last_wal_replay_lsn() AS last_replay_lsn,
  pg_last_xact_replay_timestamp() AS last_replay_time,
  CASE 
    WHEN pg_last_wal_receive_lsn() = pg_last_wal_replay_lsn() THEN '0 bytes'
    ELSE pg_size_pretty(pg_wal_lsn_diff(pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn()))
  END AS replay_lag,
  pg_is_wal_replay_paused() AS is_replay_paused
FROM pg_is_in_recovery()
WHERE pg_is_in_recovery();
EOF
      
    else
      echo -e "${RED}❌ 从节点 ${PORT_INDEX} 连接失败 (localhost:${PORT})${NC}"
    fi
    
    PORT_INDEX=$((PORT_INDEX + 1))
  done
  
  echo ""
done

# 总结报告
echo "📋 复制状态总结"
echo "========================================"

ALL_HEALTHY=true
for db in "${!DATABASES[@]}"; do
  IFS=':' read -r USER PASS DB_NAME PORT <<< "${DATABASES[$db]}"
  
  # 检查主节点
  if ! pg_isready -h localhost -p ${PORT} -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
    echo -e "${RED}❌ ${db} 主节点不可用${NC}"
    ALL_HEALTHY=false
    continue
  fi
  
  CONNECTION_STRING="postgresql://${USER}:${PASS}@localhost:${PORT}/${DB_NAME}"
  REPLICATION_COUNT=$(psql ${CONNECTION_STRING} -t -c "SELECT count(*) FROM pg_stat_replication;" | tr -d '[:space:]')
  
  EXPECTED_REPLICAS=2
  if [ "${REPLICATION_COUNT}" -lt ${EXPECTED_REPLICAS} ]; then
    echo -e "${YELLOW}⚠️  ${db}: 只有 ${REPLICATION_COUNT}/${EXPECTED_REPLICAS} 个从节点连接${NC}"
    ALL_HEALTHY=false
  else
    echo -e "${GREEN}✅ ${db}: 主节点正常，${REPLICATION_COUNT} 个从节点连接${NC}"
  fi
done

echo ""
if [ "${ALL_HEALTHY}" = true ]; then
  echo -e "${GREEN}🎉 所有 PostgreSQL 复制状态正常${NC}"
else
  echo -e "${YELLOW}⚠️  发现一些问题，请检查以上警告和错误${NC}"
fi

echo ""
echo "=== 检查完成 ==="
echo "检查时间: $(date)"
echo "运行命令: $0"