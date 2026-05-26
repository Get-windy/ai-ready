#!/bin/bash
# PostgreSQL 从节点初始化脚本
# 自动连接到主节点并启动流复制

set -e

echo "=== PostgreSQL 从节点初始化 ==="
echo "主节点: ${PRIMARY_HOST}:${PRIMARY_PORT}"
echo "数据库: ${POSTGRES_DB}"
echo "复制用户: ${REPLICATION_USER}"
echo "复制模式: ${REPLICATION_MODE}"
echo ""

# 等待主节点启动
echo "等待主节点启动..."
until pg_isready -h ${PRIMARY_HOST} -p ${PRIMARY_PORT} -U ${POSTGRES_USER} -d ${POSTGRES_DB}; do
  echo "主节点未就绪，等待 5 秒..."
  sleep 5
done

echo "主节点已就绪，开始配置从节点..."

# 如果 PGDATA 目录已存在数据，可能是之前启动的，跳过基础备份
if [ -f "${PGDATA}/PG_VERSION" ]; then
  echo "检测到已有 PostgreSQL 数据，跳过基础备份..."
else
  # 清空数据目录（第一次启动）
  echo "清空数据目录..."
  rm -rf ${PGDATA}/*

  # 从主节点执行基础备份
  echo "执行基础备份..."
  PGPASSWORD=${REPLICATION_PASSWORD} pg_basebackup \
    -h ${PRIMARY_HOST} \
    -p ${PRIMARY_PORT} \
    -U ${REPLICATION_USER} \
    -D ${PGDATA} \
    -Fp \
    -Xs \
    -R \
    -P

  echo "基础备份完成"
fi

# 配置 postgresql.auto.conf
echo "配置复制参数..."
cat > ${PGDATA}/postgresql.auto.conf << EOF
# PostgreSQL 从节点复制配置
# 自动生成，请勿手动修改

# 主节点连接信息
primary_conninfo = 'host=${PRIMARY_HOST} port=${PRIMARY_PORT} user=${REPLICATION_USER} password=${REPLICATION_PASSWORD} application_name=${HOSTNAME}'

# 复制槽名称（根据容器名分配）
primary_slot_name = 'replica_slot_$(echo ${HOSTNAME} | grep -o '[0-9]' | tail -n1)'

# 提升触发器文件
promote_trigger_file = '/tmp/promote_trigger'

# 热备配置
hot_standby = on
hot_standby_feedback = on

# 复制模式
wal_receiver_status_interval = 10s
hot_standby_feedback = on

# 性能优化
max_standby_archive_delay = 30s
max_standby_streaming_delay = 30s
EOF

# 根据复制模式配置同步级别
if [ "${REPLICATION_MODE}" = "sync" ]; then
  echo "配置为同步复制模式..."
  echo "synchronous_commit = on" >> ${PGDATA}/postgresql.auto.conf
else
  echo "配置为异步复制模式..."
  echo "synchronous_commit = off" >> ${PGDATA}/postgresql.auto.conf
fi

# 设置正确的权限
chmod 600 ${PGDATA}/postgresql.auto.conf
chown postgres:postgres ${PGDATA}/postgresql.auto.conf

echo "从节点初始化完成"
echo "启动 PostgreSQL 从节点服务..."

# 启动 PostgreSQL 服务
exec postgres "$@"