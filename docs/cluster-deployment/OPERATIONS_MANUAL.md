# Sprint 27+1 测试环境集群运维手册

## 1. 概述

### 1.1 文档目的
本文档提供Sprint 27+1测试环境集群的运维操作指南，包括集群启动/停止、服务扩缩容、故障恢复、性能调优等日常运维操作。

### 1.2 适用范围
- 系统管理员
- DevOps工程师
- 运维团队
- 技术支持人员

## 2. 集群启动与停止

### 2.1 完整集群启动流程

#### 2.1.1 启动前检查
```bash
#!/bin/bash
# pre-startup-check.sh

echo "开始集群启动前检查..."

# 1. 检查系统资源
echo "检查系统资源..."
free -h
df -h
top -bn1 | head -20

# 2. 检查网络连接
echo "检查网络连接..."
ping -c 3 8.8.8.8
nc -zv node1 22
nc -zv node2 22
nc -zv node3 22

# 3. 检查依赖服务
echo "检查依赖服务..."
systemctl is-active docker
systemctl is-active ntp
systemctl is-active ssh

# 4. 检查配置完整性
echo "检查配置完整性..."
ls -la /etc/ai-ready/
ls -la /data/ai-ready/

# 5. 检查端口占用
echo "检查端口占用..."
netstat -tlnp | grep -E '(5432|6379|5672|8080|15672|3000|9090)'

echo "✅ 启动前检查完成！"
```

#### 2.1.2 顺序启动集群
```bash
#!/bin/bash
# startup-cluster.sh

set -e

echo "开始顺序启动集群..."

# 1. 启动基础服务
echo "启动基础服务..."
cd /opt/ai-ready

# 启动数据库集群
echo "启动数据库集群..."
docker-compose -f docker-compose-database.yml up -d
sleep 30

# 启动Redis集群
echo "启动Redis集群..."
docker-compose -f docker-compose-redis-cluster.yml up -d
sleep 20

# 启动RabbitMQ集群
echo "启动RabbitMQ集群..."
docker-compose -f docker-compose-rabbitmq-cluster.yml up -d
sleep 30

# 2. 验证基础服务
echo "验证基础服务..."
./scripts/validate-basic-services.sh

# 3. 启动应用服务
echo "启动应用服务..."
docker-compose -f docker-compose-application.yml up -d --scale app=3
sleep 60

# 4. 启动负载均衡
echo "启动负载均衡..."
docker-compose -f docker-compose-nginx.yml up -d
sleep 10

# 5. 启动监控系统
echo "启动监控系统..."
docker-compose -f docker-compose-monitoring.yml up -d
sleep 30

# 6. 验证完整集群
echo "验证完整集群..."
./scripts/validate-full-cluster.sh

echo "✅ 集群启动完成！"
```

### 2.2 完整集群停止流程

#### 2.2.1 优雅停止集群
```bash
#!/bin/bash
# shutdown-cluster.sh

set -e

echo "开始优雅停止集群..."

# 1. 停止应用服务
echo "停止应用服务..."
docker-compose -f docker-compose-application.yml down
sleep 10

# 2. 停止负载均衡
echo "停止负载均衡..."
docker-compose -f docker-compose-nginx.yml down
sleep 5

# 3. 停止监控系统
echo "停止监控系统..."
docker-compose -f docker-compose-monitoring.yml down
sleep 5

# 4. 停止消息队列
echo "停止消息队列..."
docker-compose -f docker-compose-rabbitmq-cluster.yml down
sleep 10

# 5. 停止缓存服务
echo "停止缓存服务..."
docker-compose -f docker-compose-redis-cluster.yml down
sleep 10

# 6. 停止数据库服务
echo "停止数据库服务..."
docker-compose -f docker-compose-database.yml down
sleep 15

echo "✅ 集群优雅停止完成！"
```

#### 2.2.2 紧急停止集群
```bash
#!/bin/bash
# emergency-shutdown.sh

set -e

echo "开始紧急停止集群..."

# 1. 停止所有容器
echo "停止所有容器..."
docker stop $(docker ps -q) 2>/dev/null || true

# 2. 清理网络
echo "清理网络..."
docker network prune -f

# 3. 记录停止时间
echo "记录停止时间..."
date > /tmp/cluster-emergency-shutdown-$(date +%Y%m%d_%H%M%S).log

echo "✅ 集群紧急停止完成！"
```

## 3. 服务扩缩容

### 3.1 应用服务扩缩容

#### 3.1.1 水平扩容
```bash
#!/bin/bash
# scale-application.sh

set -e

DESIRED_COUNT=$1

if [ -z "$DESIRED_COUNT" ]; then
  echo "用法: $0 <实例数量>"
  exit 1
fi

echo "开始水平扩容应用服务到 $DESIRED_COUNT 个实例..."

# 1. 检查当前实例数
CURRENT_COUNT=$(docker-compose -f docker-compose-application.yml ps app | grep -c "Up")
echo "当前实例数: $CURRENT_COUNT"

# 2. 执行扩缩容
if [ "$DESIRED_COUNT" -gt "$CURRENT_COUNT" ]; then
  echo "正在扩容..."
  docker-compose -f docker-compose-application.yml up -d --scale app=$DESIRED_COUNT --no-recreate
elif [ "$DESIRED_COUNT" -lt "$CURRENT_COUNT" ]; then
  echo "正在缩容..."
  docker-compose -f docker-compose-application.yml up -d --scale app=$DESIRED_COUNT
else
  echo "实例数已为 $DESIRED_COUNT，无需调整"
  exit 0
fi

# 3. 等待服务稳定
echo "等待服务稳定..."
sleep 30

# 4. 验证服务健康
echo "验证服务健康..."
./scripts/validate-application.sh

# 5. 更新负载均衡配置
echo "更新负载均衡配置..."
./scripts/update-loadbalancer-config.sh

echo "✅ 应用服务扩缩容完成！当前实例数: $DESIRED_COUNT"
```

#### 3.1.2 自动扩缩容策略
```yaml
# autoscale-policy.yml
autoscale:
  enabled: true
  min_instances: 2
  max_instances: 10
  metrics:
    - name: cpu
      threshold: 70
      period: 60
      evaluation_periods: 2
      scale_out_cooldown: 300
      scale_in_cooldown: 600
    - name: memory
      threshold: 80
      period: 60
      evaluation_periods: 2
      scale_out_cooldown: 300
      scale_in_cooldown: 600
    - name: request_rate
      threshold: 1000
      period: 60
      evaluation_periods: 2
      scale_out_cooldown: 180
      scale_in_cooldown: 300
```

### 3.2 数据库扩缩容

#### 3.2.1 数据库读副本扩容
```bash
#!/bin/bash
# scale-database-read-replicas.sh

set -e

REPLICA_COUNT=$1

if [ -z "$REPLICA_COUNT" ]; then
  echo "用法: $0 <读副本数量>"
  exit 1
fi

echo "开始扩容数据库读副本到 $REPLICA_COUNT 个..."

# 1. 检查当前副本数
CURRENT_REPLICAS=$(docker-compose -f docker-compose-database.yml ps postgres-replica | grep -c "Up")
echo "当前读副本数: $CURRENT_REPLICAS"

# 2. 创建新的读副本
for i in $(seq $((CURRENT_REPLICAS + 1)) $REPLICA_COUNT); do
  echo "创建读副本 $i..."
  
  cat > docker-compose-database-replica$i.yml << EOF
version: '3.8'
services:
  postgres-replica$i:
    image: postgres:14
    container_name: postgres-replica$i
    restart: always
    environment:
      POSTGRES_DB: ai_ready
      POSTGRES_USER: ai_ready
      POSTGRES_PASSWORD: \${POSTGRES_PASSWORD}
    ports:
      - "543$((3 + i)):5432"
    volumes:
      - postgres-replica$i-data:/var/lib/postgresql/data
    networks:
      - database-network
    command: >
      bash -c "
        echo '等待主数据库准备就绪...'
        until pg_isready -h postgres-master -p 5432 -U ai_ready; do
          sleep 1
        done
        pg_basebackup -h postgres-master -U ai_ready -D /var/lib/postgresql/data -P -R
        postgres
      "
EOF
  
  docker-compose -f docker-compose-database-replica$i.yml up -d
  sleep 20
done

# 3. 更新应用配置
echo "更新应用配置..."
sed -i "s/^spring.datasource.read.url=.*/spring.datasource.read.url=jdbc:postgresql:\/\/postgres-replica1:5432,postgres-replica2:5432,postgres-replica3:5432\/ai_ready/" application.yml

# 4. 重启应用服务
echo "重启应用服务..."
docker-compose -f docker-compose-application.yml restart app

echo "✅ 数据库读副本扩容完成！当前读副本数: $REPLICA_COUNT"
```

### 3.3 缓存集群扩缩容

#### 3.3.1 Redis集群扩容
```bash
#!/bin/bash
# scale-redis-cluster.sh

set -e

NEW_NODE_COUNT=$1

if [ -z "$NEW_NODE_COUNT" ]; then
  echo "用法: $0 <新节点数量>"
  exit 1
fi

echo "开始扩容Redis集群..."

# 1. 检查当前集群状态
echo "检查当前集群状态..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster check localhost:6379

# 2. 添加新节点
CURRENT_NODES=$(docker exec redis-node1 redis-cli -a $REDIS_PASSWORD cluster nodes | wc -l)
START_PORT=6382

for i in $(seq 1 $NEW_NODE_COUNT); do
  PORT=$((START_PORT + i - 1))
  NODE_NAME="redis-node$((CURRENT_NODES + i))"
  
  echo "添加新节点: $NODE_NAME (端口: $PORT)..."
  
  cat > docker-compose-redis-node$((CURRENT_NODES + i)).yml << EOF
version: '3.8'
services:
  $NODE_NAME:
    image: redis:6.2-alpine
    container_name: $NODE_NAME
    command: redis-server --port 6379 --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes --requirepass \${REDIS_PASSWORD}
    ports:
      - "$PORT:6379"
    volumes:
      - $NODE_NAME-data:/data
    networks:
      - redis-network
EOF
  
  docker-compose -f docker-compose-redis-node$((CURRENT_NODES + i)).yml up -d
  sleep 10
  
  # 将新节点加入集群
  docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster add-node $NODE_NAME:6379 redis-node1:6379
  sleep 5
done

# 3. 重新平衡槽位
echo "重新平衡槽位..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster rebalance --cluster-use-empty-masters

# 4. 验证集群状态
echo "验证集群状态..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster check localhost:6379

echo "✅ Redis集群扩容完成！"
```

## 4. 故障恢复

### 4.1 数据库故障恢复

#### 4.1.1 主数据库故障恢复
```bash
#!/bin/bash
# recover-master-database.sh

set -e

echo "开始主数据库故障恢复..."

# 1. 检查故障状态
echo "检查故障状态..."
if docker-compose -f docker-compose-database.yml ps postgres-master | grep -q "Exit"; then
  echo "主数据库已停止"
else
  echo "主数据库正在运行，无需恢复"
  exit 0
fi

# 2. 提升从数据库为主
echo "提升从数据库为主..."
docker exec postgres-replica1 touch /var/lib/postgresql/data/trigger_file

# 3. 等待提升完成
echo "等待提升完成..."
sleep 30

# 4. 验证新主数据库
echo "验证新主数据库..."
docker exec postgres-replica1 pg_isready -U ai_ready

# 5. 重新配置复制
echo "重新配置复制..."
# 在新的主数据库上创建复制槽
docker exec postgres-replica1 psql -U ai_ready -d ai_ready -c "SELECT * FROM pg_create_physical_replication_slot('replica1_slot');"

# 6. 更新应用配置
echo "更新应用配置..."
sed -i 's/postgres-master/postgres-replica1/g' application.yml

# 7. 重启应用服务
echo "重启应用服务..."
docker-compose -f docker-compose-application.yml restart app

echo "✅ 主数据库故障恢复完成！"
```

#### 4.1.2 数据损坏恢复
```bash
#!/bin/bash
# recover-corrupted-data.sh

set -e

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
  BACKUP_FILE=$(ls -t /data/backups/postgresql/ai_ready_backup_*.sql.gz | head -1)
fi

echo "开始数据损坏恢复..."
echo "使用备份文件: $BACKUP_FILE"

# 1. 停止应用服务
echo "停止应用服务..."
docker-compose -f docker-compose-application.yml down

# 2. 停止数据库服务
echo "停止数据库服务..."
docker-compose -f docker-compose-database.yml down

# 3. 清理数据目录
echo "清理数据目录..."
rm -rf /data/postgresql/master/*
rm -rf /data/postgresql/replica/*

# 4. 恢复数据库
echo "恢复数据库..."
gunzip -c $BACKUP_FILE | docker run -i --rm -v /data/postgresql/master:/var/lib/postgresql/data postgres:14 psql -U postgres -d postgres

# 5. 重新启动数据库
echo "重新启动数据库..."
docker-compose -f docker-compose-database.yml up -d

# 6. 重新配置复制
echo "重新配置复制..."
sleep 30
docker exec postgres-replica1 bash -c "
  rm -rf /var/lib/postgresql/data/*
  pg_basebackup -h postgres-master -U ai_ready -D /var/lib/postgresql/data -P -R
"

# 7. 重新启动应用服务
echo "重新启动应用服务..."
docker-compose -f docker-compose-application.yml up -d

echo "✅ 数据损坏恢复完成！"
```

### 4.2 缓存故障恢复

#### 4.2.1 Redis节点故障恢复
```bash
#!/bin/bash
# recover-redis-node.sh

set -e

FAILED_NODE=$1

if [ -z "$FAILED_NODE" ]; then
  echo "用法: $0 <故障节点名称>"
  exit 1
fi

echo "开始恢复Redis节点: $FAILED_NODE..."

# 1. 检查集群状态
echo "检查集群状态..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster check localhost:6379

# 2. 故障节点处理
if docker ps | grep -q "$FAILED_NODE"; then
  echo "节点 $FAILED_NODE 正在运行"
else
  echo "节点 $FAILED_NODE 已停止，正在重启..."
  
  # 重启故障节点
  docker-compose -f docker-compose-redis-cluster.yml restart $FAILED_NODE
  sleep 10
  
  # 重新加入集群
  NODE_ID=$(docker exec redis-node1 redis-cli -a $REDIS_PASSWORD cluster nodes | grep "$FAILED_NODE" | cut -d' ' -f1)
  docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster meet $FAILED_NODE 6379
  docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster replicate $NODE_ID
fi

# 3. 修复槽位分配
echo "修复槽位分配..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster fix

# 4. 验证集群状态
echo "验证集群状态..."
docker exec redis-node1 redis-cli -a $REDIS_PASSWORD --cluster check localhost:6379

echo "✅ Redis节点故障恢复完成！"
```

### 4.3 消息队列故障恢复

#### 4.3.1 RabbitMQ节点故障恢复
```bash
#!/bin/bash
# recover-rabbitmq-node.sh

set -e

FAILED_NODE=$1

if [ -z "$FAILED_NODE" ]; then
  echo "用法: $0 <故障节点名称>"
  exit 1
fi

echo "开始恢复RabbitMQ节点: $FAILED_NODE..."

# 1. 检查集群状态
echo "检查集群状态..."
docker exec rabbitmq-node1 rabbitmqctl cluster_status

# 2. 故障节点处理
if docker ps | grep -q "$FAILED_NODE"; then
  echo "节点 $FAILED_NODE 正在运行"
  
  # 检查节点状态
  NODE_STATUS=$(docker exec $FAILED_NODE rabbitmq-diagnostics node_health_check)
  if echo "$NODE_STATUS" | grep -q "FAILED"; then
    echo "节点 $FAILED_NODE 健康检查失败，正在修复..."
    
    # 重置节点
    docker exec $FAILED_NODE rabbitmqctl stop_app
    docker exec $FAILED_NODE rabbitmqctl reset
    docker exec $FAILED_NODE rabbitmqctl join_cluster rabbit@rabbitmq-node1
    docker exec $FAILED_NODE rabbitmqctl start_app
  fi
else
  echo "节点 $FAILED_NODE 已停止，正在重启..."
  
  # 重启故障节点
  docker-compose -f docker-compose-rabbitmq-cluster.yml restart $FAILED_NODE
  sleep 30
fi

# 3. 同步队列
echo "同步队列..."
docker exec rabbitmq-node1 rabbitmqctl sync_queue --all

# 4. 验证集群状态
echo "验证集群状态..."
docker exec rabbitmq-node1 rabbitmqctl cluster_status

echo "✅ RabbitMQ节点故障恢复完成！"
```

## 5. 性能调优

### 5.1 系统性能调优

#### 5.1.1 操作系统调优
```bash
#!/bin/bash
# tune-os-parameters.sh

set -e

echo "开始操作系统性能调优..."

# 1. 内核参数调优
echo "调优内核参数..."
cat > /etc/sysctl.d/99-ai-ready.conf << EOF
# 网络参数
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_tw_recycle = 0
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_time = 1200
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 3
net.ipv4.ip_local_port_range = 1024 65535

# 内存参数
vm.swappiness = 10
vm.dirty_ratio = 60
vm.dirty_background_ratio = 5
vm.overcommit_memory = 1

# 文件系统参数
fs.file-max = 2097152
fs.nr_open = 2097152
EOF

sysctl -p /etc/sysctl.d/99-ai-ready.conf

# 2. 文件描述符限制
echo "设置文件描述符限制..."
cat > /etc/security/limits.d/99-ai-ready.conf << EOF
* soft nofile 65535
* hard nofile 65535
* soft nproc 65535
* hard nproc 65535
root soft nofile 65535
root hard nofile 65535
EOF

# 3. 磁盘调度器优化
echo "优化磁盘调度器..."
for disk in /sys/block/sd*; do
  echo "deadline" > $disk/queue/scheduler 2>/dev/null || true
done

echo "✅ 操作系统性能调优完成！"
```

#### 5.1.2 Docker性能调优
```bash
#!/bin/bash
# tune-docker-parameters.sh

set -e

echo "开始Docker性能调优..."

# 1. 创建Docker配置文件
cat > /etc/docker/daemon.json << EOF
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true"
  ],
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65535,
      "Soft": 65535
    }
  },
  "live-restore": true,
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 10,
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
EOF

# 2. 重启Docker服务
systemctl daemon-reload
systemctl restart docker

# 3. 创建Docker网络优化
docker network create --driver bridge \
  --opt com.docker.network.bridge.name=ai-ready-bridge \
  --opt com.docker.network.bridge.enable_ip_masquerade=true \
  --opt com.docker.network.bridge.enable_icc=true \
  --opt com.docker.network.driver.mtu=1500 \
  ai-ready-network

echo "✅ Docker性能调优完成！"
```

### 5.2 应用性能调优

#### 5.2.1 JVM调优
```bash
#!/bin/bash
# tune-jvm-parameters.sh

set -e

echo "开始JVM性能调优..."

# 创建JVM调优配置文件
cat > /opt/ai-ready/config/jvm-options.conf << EOF
# 堆内存设置
-Xms2g
-Xmx4g
-XX:MaxMetaspaceSize=512m
-XX:MetaspaceSize=256m

# GC设置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=8m
-XX:InitiatingHeapOccupancyPercent=45
-XX:ConcGCThreads=4

# 性能优化
-XX:+AlwaysPreTouch
-XX:+UseStringDeduplication
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-XX:+TieredCompilation
-XX:ReservedCodeCacheSize=256m
-XX:InitialCodeCacheSize=64m

# 日志和监控
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/opt/ai-ready/logs/heapdump.hprof
-XX:ErrorFile=/opt/ai-ready/logs/hs_err_pid%p.log
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintTenuringDistribution
-Xloggc:/opt/ai-ready/logs/gc.log

# 安全设置
-XX:+UseContainerSupport
-XX:InitialRAMPercentage=50.0
-XX:MaxRAMPercentage=75.0
-XX:MinRAMPercentage=25.0
EOF

echo "✅ JVM性能调优配置已生成！"
```

#### 5.2.2 数据库连接池调优
```yaml
# application-datasource.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: AIReadyHikariPool
      connection-test-query: SELECT 1
      validation-timeout: 5000
      initialization-fail-timeout: 1
      
    tomcat:
      max-active: 20
      max-idle: 10
      min-idle: 5
      initial-size: 5
      max-wait: 10000
      test-on-borrow: true
      test-while-idle: true
      validation-query: SELECT 1
      time-between-eviction-runs-millis: 5000
      min-evictable-idle-time-millis: 60000
```

### 5.3 监控与告警

#### 5.3.1 性能监控脚本
```bash
#!/bin/bash
# performance-monitoring.sh

set -e

echo "开始性能监控..."

# 监控输出文件
OUTPUT_FILE="/opt/ai-ready/logs/performance-$(date +%Y%m%d_%H%M%S).log"

# 监控函数
monitor_system() {
  echo "=== 系统监控 $(date) ===" >> $OUTPUT_FILE
  top -bn1 | head -20 >> $OUTPUT_FILE
  free -h >> $OUTPUT_FILE
  df -h >> $OUTPUT_FILE
}

monitor_docker() {
  echo "=== Docker监控 $(date) ===" >> $OUTPUT_FILE
  docker stats --no-stream >> $OUTPUT_FILE
  docker-compose -f docker-compose-application.yml ps >> $OUTPUT_FILE
}

monitor_application() {
  echo "=== 应用监控 $(date) ===" >> $OUTPUT_FILE
  curl -s http://localhost:8080/actuator/metrics >> $OUTPUT_FILE
  curl -s http://localhost:8080/actuator/health >> $OUTPUT_FILE
}

monitor_database() {
  echo "=== 数据库监控 $(date) ===" >> $OUTPUT_FILE
  PGPASSWORD=ai_ready_password psql -h localhost -U ai_ready -d ai_ready << EOF >> $OUTPUT_FILE
SELECT * FROM pg_stat_activity WHERE state = 'active';
SELECT * FROM pg_stat_database WHERE datname = 'ai_ready';
EOF
}

# 执行监控
monitor_system
monitor_docker
monitor_application
monitor_database

echo "性能监控完成！输出文件: $OUTPUT_FILE"
```

#### 5.3.2 告警配置
```yaml
# alert-rules.yml
groups:
  - name: ai-ready-alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高CPU使用率"
          description: "实例 {{ $labels.instance }} 的CPU使用率超过80% (当前值: {{ $value }}%)"
          
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高内存使用率"
          description: "实例 {{ $labels.instance }} 的内存使用率超过85% (当前值: {{ $value }}%)"
          
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务宕机"
          description: "服务 {{ $labels.job }} 在实例 {{ $labels.instance }} 上宕机"
          
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高延迟"
          description: "HTTP请求的95分位数延迟超过1秒 (当前值: {{ $value }}s)"
```

## 6. 日常运维任务

### 6.1 每日检查清单
```bash
#!/bin/bash
# daily-checklist.sh

set -e

echo "开始每日运维检查..."

DATE=$(date +%Y-%m-%d)
LOG_FILE="/opt/ai-ready/logs/daily-check-${DATE}.log"

# 1. 系统健康检查
echo "1. 系统健康检查..." | tee -a $LOG_FILE
uptime | tee -a $LOG_FILE
free -h | tee -a $LOG_FILE
df -h | tee -a $LOG_FILE

# 2. 服务状态检查
echo -e "\n2. 服务状态检查..." | tee -a $LOG_FILE
docker-compose -f docker-compose-application.yml ps | tee -a $LOG_FILE
docker-compose -f docker-compose-database.yml ps | tee -a $LOG_FILE
docker-compose -f docker-compose-redis-cluster.yml ps | tee -a $LOG_FILE
docker-compose -f docker-compose-rabbitmq-cluster.yml ps | tee -a $LOG_FILE

# 3. 应用健康检查
echo -e "\n3. 应用健康检查..." | tee -a $LOG_FILE
curl -s http://localhost:8080/actuator/health | jq . | tee -a $LOG_FILE

# 4. 数据库健康检查
echo -e "\n4. 数据库健康检查..." | tee -a $LOG_FILE
PGPASSWORD=ai_ready_password psql -h localhost -U ai_ready -d ai_ready -c "SELECT now(), version();" | tee -a $LOG_FILE

# 5. 日志检查
echo -e "\n5. 日志检查..." | tee -a $LOG_FILE
tail -100 /opt/ai-ready/logs/application.log | grep -E "(ERROR|WARN|Exception)" | head -20 | tee -a $LOG_FILE

# 6. 备份检查
echo -e "\n6. 备份检查..." | tee -a $LOG_FILE
ls -lh /data/backups/postgresql/*.gz | tail -5 | tee -a $LOG_FILE
ls -lh /data/backups/rabbitmq/*.json | tail -5 | tee -a $LOG_FILE

# 7. 监控告警检查
echo -e "\n7. 监控告警检查..." | tee -a $LOG_FILE
curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.state == "firing")' | tee -a $LOG_FILE

echo -e "\n✅ 每日运维检查完成！" | tee -a $LOG_FILE
echo "详细日志: $LOG_FILE"
```

### 6.2 每周维护任务
```bash
#!/bin/bash
# weekly-maintenance.sh

set -e

echo "开始每周维护任务..."

# 1. 清理旧日志
echo "1. 清理旧日志..."
find /opt/ai-ready/logs -name "*.log" -mtime +30 -delete
find /var/log -name "*.log" -mtime +30 -delete

# 2. 清理Docker资源
echo "2. 清理Docker资源..."
docker system prune -f
docker volume prune -f

# 3. 更新系统包
echo "3. 更新系统包..."
apt-get update
apt-get upgrade -y

# 4. 重启服务（滚动重启）
echo "4. 滚动重启服务..."
docker-compose -f docker-compose-application.yml restart --rolling

# 5. 执行完整备份
echo "5. 执行完整备份..."
./scripts/full-backup.sh

# 6. 生成周报
echo "6. 生成运维周报..."
./scripts/generate-weekly-report.sh

echo "✅ 每周维护任务完成！"
```

## 7. 附录

### 7.1 运维检查清单
- [ ] 系统资源监控正常
- [ ] 所有服务运行正常
- [ ] 应用健康检查通过
- [ ] 数据库连接正常
- [ ] 缓存服务正常
- [ ] 消息队列正常
- [ ] 监控告警系统正常
- [ ] 备份任务执行正常
- [ ] 日志收集正常
- [ ] 安全扫描正常

### 7.2 紧急联系方式
- **运维值班**: +86 13800138001
- **技术负责人**: +86 13800138002
- **数据库专家**: +86 13800138003
- **网络专家**: +86 13800138004
- **安全专家**: +86 13800138005

### 7.3 运维文档链接
- [集群架构设计文档](./CLUSTER_ARCHITECTURE_DESIGN.md)
- [服务部署配置说明](./SERVICE_DEPLOYMENT_CONFIGURATION.md)
- [数据库集群配置文档](./DATABASE_CLUSTER_CONFIGURATION.md)
- [消息队列集群配置文档](./MESSAGE_QUEUE_CLUSTER_CONFIGURATION.md)

---

**文档版本**: v1.0  
**创建时间**: 2026-04-27  
**更新记录**:
- v1.0 (2026-04-27): 初始版本，创建运维手册

**负责人**: doc-writer  
**审核人**: devops-engineer  
**批准人**: main