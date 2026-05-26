#!/bin/bash
# PostgreSQL 主从复制部署脚本
# 一键部署完整的 PostgreSQL 主从复制环境
# 创建日期: 2026-04-24

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../../docker-compose-test.yml"
REPLICATION_FILE="${SCRIPT_DIR}/../../postgresql-replication.yml"

echo "=== PostgreSQL 主从复制部署 ==="
echo "主配置文件: ${COMPOSE_FILE}"
echo "复制配置文件: ${REPLICATION_FILE}"
echo "时间: $(date)"
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
  echo "❌ 错误: Docker 未运行"
  exit 1
fi

# 检查 Docker Compose 是否可用
if ! command -v docker-compose &> /dev/null; then
  echo "❌ 错误: Docker Compose 未安装"
  exit 1
fi

# 检查配置文件
if [ ! -f "${COMPOSE_FILE}" ]; then
  echo "❌ 错误: 主配置文件不存在: ${COMPOSE_FILE}"
  exit 1
fi

if [ ! -f "${REPLICATION_FILE}" ]; then
  echo "❌ 错误: 复制配置文件不存在: ${REPLICATION_FILE}"
  exit 1
fi

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 数据库列表
declare -A DATABASES=(
  ["main"]="gateway_user:gateway_pass_2026:gateway_db:5432:repuser_gateway"
  ["finance"]="finance_user:finance_pass_2026:finance_db:5433:repuser_finance"
  ["inventory"]="inventory_user:inventory_pass_2026:inventory_db:5434:repuser_inventory"
  ["ai"]="ai_user:ai_pass_2026:ai_db:5435:repuser_ai"
  ["data"]="data_user:data_pass_2026:data_db:5436:repuser_data"
)

echo -e "${BLUE}🔧 步骤 1: 部署 PostgreSQL 主节点...${NC}"
echo ""

# 部署主节点
docker-compose -f ${COMPOSE_FILE} up -d \
  postgres-main \
  postgres-finance \
  postgres-inventory \
  postgres-ai \
  postgres-data

echo -e "${GREEN}✅ 主节点部署完成${NC}"
echo "等待主节点就绪..."

# 等待主节点启动
sleep_count=0
max_wait=180
while [ ${sleep_count} -lt ${max_wait} ]; do
  ALL_READY=true
  for db in "${!DATABASES[@]}"; do
    IFS=':' read -r USER PASS DB_NAME PORT REP_USER <<< "${DATABASES[$db]}"
    if ! docker exec ai-ready-postgres-${db}-test pg_isready -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
      ALL_READY=false
      break
    fi
  done
  
  if [ "${ALL_READY}" = true ]; then
    echo -e "${GREEN}✅ 所有主节点已就绪${NC}"
    break
  fi
  
  sleep_count=$((sleep_count + 5))
  if [ ${sleep_count} -ge ${max_wait} ]; then
    echo -e "${RED}❌ 主节点启动超时${NC}"
    exit 1
  fi
  echo "等待主节点启动... (${sleep_count}/${max_wait}秒)"
  sleep 5
done

echo ""
echo -e "${BLUE}🔧 步骤 2: 配置主节点复制参数...${NC}"

# 配置主节点复制参数
for db in "${!DATABASES[@]}"; do
  IFS=':' read -r USER PASS DB_NAME PORT REP_USER <<< "${DATABASES[$db]}"
  
  echo "配置 ${db} 数据库主节点..."
  
  # 创建主节点初始化脚本
  cat > /tmp/init-master-${db}.sql << EOF
-- 配置 ${db} 数据库主节点复制参数
ALTER SYSTEM SET wal_level = replica;
ALTER SYSTEM SET max_wal_senders = 10;
ALTER SYSTEM SET max_replication_slots = 10;
ALTER SYSTEM SET hot_standby = on;
ALTER SYSTEM SET hot_standby_feedback = on;
ALTER SYSTEM SET synchronous_commit = on;
ALTER SYSTEM SET synchronous_standby_names = '*';

-- 创建复制用户
CREATE USER ${REP_USER} WITH REPLICATION PASSWORD 'rep_pass_2026';
GRANT CONNECT ON DATABASE ${DB_NAME} TO ${REP_USER};

-- 创建复制槽
SELECT pg_create_physical_replication_slot('${db}_replica_slot_1');
SELECT pg_create_physical_replication_slot('${db}_replica_slot_2');

-- 重新加载配置
SELECT pg_reload_conf();
EOF
  
  # 执行初始化脚本
  docker cp /tmp/init-master-${db}.sql ai-ready-postgres-${db}-test:/tmp/init-master.sql
  docker exec ai-ready-postgres-${db}-test psql -U ${USER} -d ${DB_NAME} -f /tmp/init-master.sql
  
  echo -e "${GREEN}✅ ${db} 主节点配置完成${NC}"
done

echo ""
echo -e "${BLUE}🔧 步骤 3: 部署 PostgreSQL 从节点...${NC}"

# 部署从节点
docker-compose -f ${REPLICATION_FILE} up -d

echo -e "${GREEN}✅ 从节点部署完成${NC}"
echo "等待从节点同步..."

# 等待从节点启动和同步
sleep_count=0
max_wait=300
while [ ${sleep_count} -lt ${max_wait} ]; do
  ALL_SYNCED=true
  
  for db in "${!DATABASES[@]}"; do
    IFS=':' read -r USER PASS DB_NAME PORT REP_USER <<< "${DATABASES[$db]}"
    
    # 检查从节点1
    REPLICA_PORT=$((PORT + 5))
    if ! docker exec ai-ready-postgres-${db}-replica1-test pg_isready -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
      ALL_SYNCED=false
      break
    fi
    
    # 检查从节点2
    REPLICA_PORT=$((PORT + 6))
    if ! docker exec ai-ready-postgres-${db}-replica2-test pg_isready -U ${USER} -d ${DB_NAME} > /dev/null 2>&1; then
      ALL_SYNCED=false
      break
    fi
  done
  
  if [ "${ALL_SYNCED}" = true ]; then
    echo -e "${GREEN}✅ 所有从节点已就绪${NC}"
    break
  fi
  
  sleep_count=$((sleep_count + 10))
  if [ ${sleep_count} -ge ${max_wait} ]; then
    echo -e "${YELLOW}⚠️  从节点启动超时，但继续执行验证${NC}"
    break
  fi
  echo "等待从节点同步... (${sleep_count}/${max_wait}秒)"
  sleep 10
done

echo ""
echo -e "${BLUE}🔧 步骤 4: 验证复制状态...${NC}"

# 验证复制状态
ALL_REPLICATION_HEALTHY=true
for db in "${!DATABASES[@]}"; do
  IFS=':' read -r USER PASS DB_NAME PORT REP_USER <<< "${DATABASES[$db]}"
  
  echo "验证 ${db} 数据库复制状态..."
  
  # 检查主节点复制连接
  CONNECTION_STRING="postgresql://${USER}:${PASS}@localhost:${PORT}/${DB_NAME}"
  REPLICATION_COUNT=$(docker exec ai-ready-postgres-${db}-test psql ${CONNECTION_STRING} -t -c "SELECT count(*) FROM pg_stat_replication;" | tr -d '[:space:]')
  
  if [ "${REPLICATION_COUNT}" -ge 2 ]; then
    echo -e "${GREEN}✅ ${db}: 主节点有 ${REPLICATION_COUNT} 个复制连接${NC}"
    
    # 显示复制状态
    docker exec ai-ready-postgres-${db}-test psql ${CONNECTION_STRING} <<EOF
SELECT 
  application_name,
  client_addr::text,
  state,
  sync_state,
  pg_size_pretty(pg_wal_lsn_diff(sent_lsn, replay_lsn)) AS replay_lag,
  to_char(EXTRACT(EPOCH FROM (now() - replay_lag_time)), '999.99') AS replay_lag_seconds
FROM pg_stat_replication
ORDER BY application_name;
EOF
    
  elif [ "${REPLICATION_COUNT}" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  ${db}: 只有 ${REPLICATION_COUNT}/2 个复制连接${NC}"
    ALL_REPLICATION_HEALTHY=false
  else
    echo -e "${RED}❌ ${db}: 无复制连接${NC}"
    ALL_REPLICATION_HEALTHY=false
  fi
  
  # 检查从节点恢复状态
  for replica_num in 1 2; do
    CONTAINER_NAME="ai-ready-postgres-${db}-replica${replica_num}-test"
    REPLICA_STATUS=$(docker exec ${CONTAINER_NAME} psql -U ${USER} -d ${DB_NAME} -t -c "SELECT pg_is_in_recovery();" | tr -d '[:space:]')
    
    if [ "${REPLICA_STATUS}" = "t" ]; then
      echo -e "${GREEN}✅ ${db} 从节点${replica_num}: 处于恢复模式${NC}"
    else
      echo -e "${RED}❌ ${db} 从节点${replica_num}: 不在恢复模式${NC}"
      ALL_REPLICATION_HEALTHY=false
    fi
  done
  
  echo ""
done

echo ""
echo -e "${BLUE}📋 部署总结${NC}"
echo "========================================"

if [ "${ALL_REPLICATION_HEALTHY}" = true ]; then
  echo -e "${GREEN}🎉 PostgreSQL 主从复制部署成功！${NC}"
else
  echo -e "${YELLOW}⚠️  PostgreSQL 主从复制部署完成，但存在一些问题${NC}"
fi

echo ""
echo -e "${BLUE}🌐 服务访问地址${NC}"
echo "主节点:"
echo "  - main:        localhost:5432"
echo "  - finance:     localhost:5433"
echo "  - inventory:   localhost:5434"
echo "  - ai:          localhost:5435"
echo "  - data:        localhost:5436"
echo ""
echo "从节点:"
echo "  - main 从节点: localhost:5437, localhost:5438"
echo "  - finance 从节点: localhost:5443, localhost:5444"
echo "  - inventory 从节点: localhost:5447, localhost:5448"
echo "  - ai 从节点: localhost:5453, localhost:5454"
echo "  - data 从节点: localhost:5457, localhost:5458"
echo ""
echo -e "${BLUE}🔧 管理命令${NC}"
echo "查看复制状态: ./scripts/postgresql-replication/check-replication-status.sh"
echo "停止复制服务: docker-compose -f ${REPLICATION_FILE} down"
echo "清理数据卷: docker-compose -f ${REPLICATION_FILE} down -v"
echo ""
echo -e "${BLUE}📊 监控地址${NC}"
echo "Prometheus: http://localhost:9090"
echo "Grafana:    http://localhost:3000 (admin/admin_test_2026)"
echo ""
echo "部署完成时间: $(date)"