# Sprint 27+1 测试环境消息队列集群配置文档

## 1. 概述

### 1.1 文档目的
本文档提供Sprint 27+1测试环境消息队列集群的详细配置说明，包括RabbitMQ集群配置、高可用方案、监控告警、消息持久化等。

### 1.2 消息队列架构
```
┌─────────────────────────────────────────────────────────┐
│                   RabbitMQ集群架构                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │  节点1       │────│  节点2       │────│  节点3       │ │
│  │ (主节点)     │    │ (镜像节点)   │    │ (镜像节点)   │ │
│  └─────────────┘    └─────────────┘    └─────────────┘ │
│        │                   │                   │        │
│  ┌─────▼──────┐    ┌──────▼─────┐    ┌──────▼─────┐    │
│  │ 磁盘队列    │    │ 磁盘队列    │    │ 磁盘队列    │    │
│  │ (持久化)   │    │ (持久化)   │    │ (持久化)   │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
│        │                   │                   │        │
│  ┌─────▼───────────────────────────────────────▼─────┐    │
│  │               HAProxy负载均衡器                    │    │
│  │            (端口5672/15672代理)                    │    │
│  └───────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## 2. 集群配置

### 2.1 RabbitMQ节点配置

#### 2.1.1 节点1配置（主节点）
```ini
# rabbitmq-node1.config
[
  {rabbit, [
    {cluster_nodes, {['rabbit@node1', 'rabbit@node2', 'rabbit@node3'], disc}},
    {cluster_partition_handling, autoheal},
    {default_user, <<"admin">>},
    {default_pass, <<"admin_password">>},
    {default_vhost, <<"/">>},
    {default_permissions, [<<".*">>, <<".*">>, <<".*">>]},
    {tcp_listeners, [{"0.0.0.0", 5672}]},
    {ssl_listeners, []},
    {heartbeat, 60},
    {vm_memory_high_watermark, 0.6},
    {disk_free_limit, {mem_relative, 1.0}},
    {log_levels, [{connection, info}, {channel, info}]},
    {channel_max, 2047},
    {frame_max, 131072},
    {collect_statistics_interval, 5000}
  ]},
  {rabbitmq_management, [
    {listener, [{port, 15672}]},
    {load_definitions, "/etc/rabbitmq/definitions.json"}
  ]},
  {rabbitmq_prometheus, [
    {path, "/metrics"},
    {prometheus, [{collectors, [default]}]}
  ]}
].
```

#### 2.1.2 节点2配置（镜像节点）
```ini
# rabbitmq-node2.config
[
  {rabbit, [
    {cluster_nodes, {['rabbit@node1', 'rabbit@node2', 'rabbit@node3'], disc}},
    {cluster_partition_handling, autoheal},
    {default_user, <<"admin">>},
    {default_pass, <<"admin_password">>},
    {default_vhost, <<"/">>},
    {default_permissions, [<<".*">>, <<".*">>, <<".*">>]},
    {tcp_listeners, [{"0.0.0.0", 5672}]},
    {ssl_listeners, []},
    {heartbeat, 60},
    {vm_memory_high_watermark, 0.6},
    {disk_free_limit, {mem_relative, 1.0}},
    {log_levels, [{connection, info}, {channel, info}]},
    {channel_max, 2047},
    {frame_max, 131072},
    {collect_statistics_interval, 5000}
  ]},
  {rabbitmq_management, [
    {listener, [{port, 15672}]}
  ]},
  {rabbitmq_prometheus, [
    {path, "/metrics"},
    {prometheus, [{collectors, [default]}]}
  ]}
].
```

#### 2.1.3 节点3配置（镜像节点）
```ini
# rabbitmq-node3.config
[
  {rabbit, [
    {cluster_nodes, {['rabbit@node1', 'rabbit@node2', 'rabbit@node3'], disc}},
    {cluster_partition_handling, autoheal},
    {default_user, <<"admin">>},
    {default_pass, <<"admin_password">>},
    {default_vhost, <<"/">>},
    {default_permissions, [<<".*">>, <<".*">>, <<".*">>]},
    {tcp_listeners, [{"0.0.0.0", 5672}]},
    {ssl_listeners, []},
    {heartbeat, 60},
    {vm_memory_high_watermark, 0.6},
    {disk_free_limit, {mem_relative, 1.0}},
    {log_levels, [{connection, info}, {channel, info}]},
    {channel_max, 2047},
    {frame_max, 131072},
    {collect_statistics_interval, 5000}
  ]},
  {rabbitmq_management, [
    {listener, [{port, 15672}]}
  ]},
  {rabbitmq_prometheus, [
    {path, "/metrics"},
    {prometheus, [{collectors, [default]}]}
  ]}
].
```

### 2.2 集群策略配置

#### 2.2.1 镜像队列策略
```json
{
  "name": "ha-all",
  "pattern": ".*",
  "definition": {
    "ha-mode": "all",
    "ha-sync-mode": "automatic",
    "ha-sync-batch-size": 1,
    "ha-promote-on-shutdown": "always",
    "ha-promote-on-failure": "always"
  },
  "apply-to": "queues",
  "priority": 0
}
```

#### 2.2.2 联邦交换策略
```json
{
  "name": "federation-upstream",
  "definition": {
    "uri": "amqp://admin:admin_password@node1:5672",
    "ack-mode": "on-confirm",
    "reconnect-delay": 5,
    "max-hops": 1,
    "expires": 3600000
  },
  "apply-to": "exchanges"
}
```

## 3. 部署脚本

### 3.1 RabbitMQ集群部署脚本
```bash
#!/bin/bash
# deploy-rabbitmq-cluster.sh

set -e

echo "开始部署RabbitMQ集群..."

# 1. 安装Docker
echo "安装Docker..."
sudo apt-get update
sudo apt-get install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker

# 2. 创建数据目录
echo "创建数据目录..."
sudo mkdir -p /data/rabbitmq/{node1,node2,node3}
sudo chown -R 1000:1000 /data/rabbitmq

# 3. 创建Docker Compose文件
echo "创建Docker Compose文件..."
cat > docker-compose-rabbitmq.yml << 'EOF'
version: '3.8'

services:
  rabbitmq-node1:
    image: rabbitmq:3.9-management-alpine
    container_name: rabbitmq-node1
    hostname: rabbitmq-node1
    environment:
      RABBITMQ_ERLANG_COOKIE: "secret_cookie_123"
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin_password
      RABBITMQ_NODENAME: rabbit@rabbitmq-node1
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - /data/rabbitmq/node1:/var/lib/rabbitmq
      - ./rabbitmq-node1.config:/etc/rabbitmq/rabbitmq.conf
      - ./definitions.json:/etc/rabbitmq/definitions.json
    networks:
      - rabbitmq-network
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  rabbitmq-node2:
    image: rabbitmq:3.9-management-alpine
    container_name: rabbitmq-node2
    hostname: rabbitmq-node2
    environment:
      RABBITMQ_ERLANG_COOKIE: "secret_cookie_123"
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin_password
      RABBITMQ_NODENAME: rabbit@rabbitmq-node2
    ports:
      - "5673:5672"
      - "15673:15672"
    volumes:
      - /data/rabbitmq/node2:/var/lib/rabbitmq
      - ./rabbitmq-node2.config:/etc/rabbitmq/rabbitmq.conf
    networks:
      - rabbitmq-network
    depends_on:
      - rabbitmq-node1
    command: >
      bash -c "
        sleep 10
        rabbitmqctl stop_app
        rabbitmqctl reset
        rabbitmqctl join_cluster rabbit@rabbitmq-node1
        rabbitmqctl start_app
      "
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  rabbitmq-node3:
    image: rabbitmq:3.9-management-alpine
    container_name: rabbitmq-node3
    hostname: rabbitmq-node3
    environment:
      RABBITMQ_ERLANG_COOKIE: "secret_cookie_123"
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin_password
      RABBITMQ_NODENAME: rabbit@rabbitmq-node3
    ports:
      - "5674:5672"
      - "15674:15672"
    volumes:
      - /data/rabbitmq/node3:/var/lib/rabbitmq
      - ./rabbitmq-node3.config:/etc/rabbitmq/rabbitmq.conf
    networks:
      - rabbitmq-network
    depends_on:
      - rabbitmq-node1
      - rabbitmq-node2
    command: >
      bash -c "
        sleep 15
        rabbitmqctl stop_app
        rabbitmqctl reset
        rabbitmqctl join_cluster rabbit@rabbitmq-node1
        rabbitmqctl start_app
      "
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  haproxy:
    image: haproxy:2.4-alpine
    container_name: rabbitmq-haproxy
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - ./haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg
    networks:
      - rabbitmq-network
    depends_on:
      - rabbitmq-node1
      - rabbitmq-node2
      - rabbitmq-node3
    healthcheck:
      test: ["CMD", "haproxy", "-c", "-f", "/usr/local/etc/haproxy/haproxy.cfg"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  rabbitmq-network:
    driver: bridge
EOF

# 4. 创建HAProxy配置
echo "创建HAProxy配置..."
cat > haproxy.cfg << 'EOF'
global
    log stdout format raw local0
    maxconn 4096
    user haproxy
    group haproxy

defaults
    log global
    mode tcp
    timeout connect 5s
    timeout client 50s
    timeout server 50s
    retries 3

# RabbitMQ AMQP负载均衡
listen rabbitmq-amqp
    bind *:5672
    mode tcp
    balance roundrobin
    option tcp-check
    tcp-check connect port 5672
    tcp-check send "PING\r\n"
    tcp-check expect string "PONG"
    server rabbitmq-node1 rabbitmq-node1:5672 check inter 5s rise 2 fall 3
    server rabbitmq-node2 rabbitmq-node2:5672 check inter 5s rise 2 fall 3
    server rabbitmq-node3 rabbitmq-node3:5672 check inter 5s rise 2 fall 3

# RabbitMQ管理界面负载均衡
listen rabbitmq-management
    bind *:15672
    mode http
    balance roundrobin
    option httpchk GET /api/health/checks/alarms HTTP/1.1\r\nHost:\ localhost
    server rabbitmq-node1 rabbitmq-node1:15672 check inter 10s rise 2 fall 3
    server rabbitmq-node2 rabbitmq-node2:15672 check inter 10s rise 2 fall 3
    server rabbitmq-node3 rabbitmq-node3:15672 check inter 10s rise 2 fall 3
EOF

# 5. 创建RabbitMQ定义文件
echo "创建RabbitMQ定义文件..."
cat > definitions.json << 'EOF'
{
  "users": [
    {
      "name": "admin",
      "password_hash": "pZB2i7Q7F3C8L7K7g3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": "administrator"
    },
    {
      "name": "app_user",
      "password_hash": "pZB2i7Q7F3C8L7K7g3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": "management"
    },
    {
      "name": "monitor_user",
      "password_hash": "pZB2i7Q7F3C8L7K7g3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j3K7j",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": "monitoring"
    }
  ],
  "vhosts": [
    {
      "name": "/"
    },
    {
      "name": "ai_ready"
    }
  ],
  "permissions": [
    {
      "user": "admin",
      "vhost": "/",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    },
    {
      "user": "app_user",
      "vhost": "ai_ready",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    },
    {
      "user": "monitor_user",
      "vhost": "/",
      "configure": "",
      "write": "",
      "read": ".*"
    }
  ],
  "queues": [
    {
      "name": "order_queue",
      "vhost": "ai_ready",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-queue-type": "quorum"
      }
    },
    {
      "name": "notification_queue",
      "vhost": "ai_ready",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-queue-type": "quorum"
      }
    },
    {
      "name": "audit_queue",
      "vhost": "ai_ready",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-queue-type": "quorum"
      }
    }
  ],
  "exchanges": [
    {
      "name": "order_exchange",
      "vhost": "ai_ready",
      "type": "direct",
      "durable": true,
      "auto_delete": false,
      "internal": false,
      "arguments": {}
    },
    {
      "name": "notification_exchange",
      "vhost": "ai_ready",
      "type": "fanout",
      "durable": true,
      "auto_delete": false,
      "internal": false,
      "arguments": {}
    },
    {
      "name": "dead_letter_exchange",
      "vhost": "ai_ready",
      "type": "direct",
      "durable": true,
      "auto_delete": false,
      "internal": false,
      "arguments": {}
    }
  ],
  "bindings": [
    {
      "source": "order_exchange",
      "vhost": "ai_ready",
      "destination": "order_queue",
      "destination_type": "queue",
      "routing_key": "order.create",
      "arguments": {}
    },
    {
      "source": "notification_exchange",
      "vhost": "ai_ready",
      "destination": "notification_queue",
      "destination_type": "queue",
      "routing_key": "",
      "arguments": {}
    }
  ],
  "policies": [
    {
      "name": "ha-all",
      "vhost": "ai_ready",
      "pattern": ".*",
      "definition": {
        "ha-mode": "all",
        "ha-sync-mode": "automatic",
        "ha-sync-batch-size": 1
      },
      "apply-to": "queues",
      "priority": 0
    },
    {
      "name": "dead-letter-policy",
      "vhost": "ai_ready",
      "pattern": ".*",
      "definition": {
        "dead-letter-exchange": "dead_letter_exchange",
        "dead-letter-routing-key": "dead_letter",
        "message-ttl": 86400000
      },
      "apply-to": "queues",
      "priority": 1
    }
  ]
}
EOF

# 6. 启动集群
echo "启动RabbitMQ集群..."
docker-compose -f docker-compose-rabbitmq.yml up -d

# 7. 等待集群就绪
echo "等待集群就绪..."
sleep 30

# 8. 验证集群状态
echo "验证集群状态..."
docker exec rabbitmq-node1 rabbitmqctl cluster_status

echo "✅ RabbitMQ集群部署完成！"
```

## 4. 监控配置

### 4.1 Prometheus监控配置
```yaml
# prometheus-rabbitmq.yml
scrape_configs:
  - job_name: 'rabbitmq'
    static_configs:
      - targets:
        - 'rabbitmq-node1:15692'  # RabbitMQ Prometheus插件端口
        - 'rabbitmq-node2:15692'
        - 'rabbitmq-node3:15692'
    metrics_path: /metrics
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '(.*):.*'
        replacement: '${1}'
      
  - job_name: 'rabbitmq-ha-proxy'
    static_configs:
      - targets:
        - 'haproxy:8404'  # HAProxy统计端口
    metrics_path: /metrics
```

### 4.2 RabbitMQ监控脚本
```bash
#!/bin/bash
# monitor-rabbitmq.sh

set -e

echo "开始监控RabbitMQ集群..."

# 1. 检查集群状态
echo "检查集群状态..."
docker exec rabbitmq-node1 rabbitmq-diagnostics cluster_status

# 2. 检查队列状态
echo "检查队列状态..."
docker exec rabbitmq-node1 rabbitmqctl list_queues --vhost ai_ready name messages_ready messages_unacknowledged consumers

# 3. 检查连接状态
echo "检查连接状态..."
docker exec rabbitmq-node1 rabbitmqctl list_connections name peer_host peer_port state

# 4. 检查通道状态
echo "检查通道状态..."
docker exec rabbitmq-node1 rabbitmqctl list_channels name consumer_count messages_unacknowledged

# 5. 检查节点状态
echo "检查节点状态..."
docker exec rabbitmq-node1 rabbitmq-diagnostics node_health_check

# 6. 检查磁盘使用情况
echo "检查磁盘使用情况..."
docker exec rabbitmq-node1 rabbitmq-diagnostics disk_free

# 7. 检查内存使用情况
echo "检查内存使用情况..."
docker exec rabbitmq-node1 rabbitmq-diagnostics memory_breakdown

# 8. 生成监控报告
echo "生成监控报告..."
cat > /tmp/rabbitmq-monitor-report-$(date +%Y%m%d_%H%M%S).txt << EOF
RabbitMQ集群监控报告
生成时间: $(date)
========================================

集群状态:
$(docker exec rabbitmq-node1 rabbitmq-diagnostics cluster_status)

队列状态:
$(docker exec rabbitmq-node1 rabbitmqctl list_queues --vhost ai_ready name messages_ready messages_unacknowledged consumers | head -20)

连接状态:
$(docker exec rabbitmq-node1 rabbitmqctl list_connections name peer_host peer_port state | head -10)

节点健康检查:
$(docker exec rabbitmq-node1 rabbitmq-diagnostics node_health_check)

磁盘和内存:
$(docker exec rabbitmq-node1 rabbitmq-diagnostics disk_free)
$(docker exec rabbitmq-node1 rabbitmq-diagnostics memory_breakdown | head -10)
EOF

echo "监控报告已生成: /tmp/rabbitmq-monitor-report-*.txt"
echo "✅ RabbitMQ监控完成！"
```

## 5. 备份恢复策略

### 5.1 配置备份脚本
```bash
#!/bin/bash
# backup-rabbitmq.sh

set -e

BACKUP_DIR="/data/backups/rabbitmq"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/rabbitmq_backup_${DATE}.json"

echo "开始备份RabbitMQ配置..."

# 创建备份目录
mkdir -p $BACKUP_DIR

# 1. 备份定义文件
echo "备份定义文件..."
docker exec rabbitmq-node1 rabbitmqctl export_definitions $BACKUP_FILE

# 2. 备份配置文件
echo "备份配置文件..."
cp rabbitmq-node1.config ${BACKUP_DIR}/rabbitmq-node1.config.${DATE}
cp rabbitmq-node2.config ${BACKUP_DIR}/rabbitmq-node2.config.${DATE}
cp rabbitmq-node3.config ${BACKUP_DIR}/rabbitmq-node3.config.${DATE}
cp haproxy.cfg ${BACKUP_DIR}/haproxy.cfg.${DATE}

# 3. 备份数据目录（可选）
echo "备份数据目录..."
tar -czf ${BACKUP_DIR}/rabbitmq_data_${DATE}.tar.gz /data/rabbitmq

# 4. 保留最近7天的备份
find $BACKUP_DIR -name "rabbitmq_backup_*.json" -mtime +7 -delete
find $BACKUP_DIR -name "rabbitmq-node*.config.*" -mtime +7 -delete
find $BACKUP_DIR -name "haproxy.cfg.*" -mtime +7 -delete
find $BACKUP_DIR -name "rabbitmq_data_*.tar.gz" -mtime +7 -delete

# 5. 记录备份信息
echo "备份完成: $BACKUP_FILE" >> ${BACKUP_DIR}/backup.log
ls -lh $BACKUP_FILE

echo "✅ RabbitMQ备份完成！"
```

### 5.2 配置恢复脚本
```bash
#!/bin/bash
# restore-rabbitmq.sh

set -e

BACKUP_DIR="/data/backups/rabbitmq"
BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
  # 使用最新的备份文件
  BACKUP_FILE=$(ls -t ${BACKUP_DIR}/rabbitmq_backup_*.json | head -1)
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "❌ 备份文件不存在: $BACKUP_FILE"
  exit 1
fi

echo "开始恢复RabbitMQ配置..."
echo "使用备份文件: $BACKUP_FILE"

# 1. 停止应用连接
echo "停止应用连接..."
# 这里可以添加停止应用的逻辑

# 2. 恢复定义文件
echo "恢复定义文件..."
docker cp $BACKUP_FILE rabbitmq-node1:/tmp/definitions.json
docker exec rabbitmq-node1 rabbitmqctl import_definitions /tmp/definitions.json

# 3. 重启集群使配置生效
echo "重启RabbitMQ集群..."
docker-compose -f docker-compose-rabbitmq.yml restart

# 4. 等待集群恢复
echo "等待集群恢复..."
sleep 30

# 5. 验证集群状态
echo "验证集群状态..."
docker exec rabbitmq-node1 rabbitmqctl cluster_status

echo "✅ RabbitMQ恢复完成！"
```

## 6. 性能优化

### 6.1 性能监控脚本
```bash
#!/bin/bash
# performance-monitor-rabbitmq.sh

set -e

echo "开始RabbitMQ性能监控..."

# 监控指标收集
while true; do
  TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
  
  # 收集队列指标
  QUEUE_STATS=$(docker exec rabbitmq-node1 rabbitmqctl list_queues --vhost ai_ready name messages_ready messages_unacknowledged memory | grep -v "Listing queues")
  
  # 收集连接指标
  CONNECTION_COUNT=$(docker exec rabbitmq-node1 rabbitmqctl list_connections | grep -c "running")
  
  # 收集通道指标
  CHANNEL_COUNT=$(docker exec rabbitmq-node1 rabbitmqctl list_channels | grep -c "running")
  
  # 收集节点指标
  NODE_STATS=$(docker exec rabbitmq-node1 rabbitmq-diagnostics node_health_check | grep -E "(uptime|fd_used|sockets_used|mem_used|disk_free)")
  
  # 输出监控数据
  cat >> /tmp/rabbitmq-performance.log << EOF
时间: $TIMESTAMP
连接数: $CONNECTION_COUNT
通道数: $CHANNEL_COUNT
节点状态: $NODE_STATS
队列状态:
$QUEUE_STATS
========================================
EOF
  
  # 每5分钟收集一次
  sleep 300
done
```

### 6.2 性能优化配置
```ini
# rabbitmq-performance.config
[
  {rabbit, [
    {tcp_listen_options, [
      {backlog, 128},
      {nodelay, true},
      {linger, {true, 0}},
      {exit_on_close, false}
    ]},
    {collect_statistics_interval, 1000},
    {msg_store_index_module, rabbit_msg_store_ets_index},
    {queue_index_embed_msgs_below, 4096},
    {mnesia_table_loading_retry_timeout, 30000},
    {mnesia_table_loading_retry_limit, 10},
    {log_levels, [{connection, warning}, {channel, warning}]},
    {auth_mechanisms, ['PLAIN', 'AMQPLAIN']},
    {auth_backends, [rabbit_auth_backend_internal]},
    {default_user_tags, [administrator]},
    {loopback_users, []},
    {cluster_partition_handling, pause_minority},
    {mirroring_sync_batch_size, 4096},
    {mirroring_flow_control, true}
  ]},
  {rabbitmq_management, [
    {rates_mode, detailed},
    {sample_retention_policies, [
      {global,   [{60, 5}, {3600, 60}, {86400, 1200}]},
      {basic,    [{60, 5}, {3600, 60}]},
      {detailed, [{10, 5}]}
    ]}
  ]}
].
```

## 7. 故障处理

### 7.1 常见故障处理

#### 7.1.1 节点加入集群失败
```bash
# 重置节点
docker exec rabbitmq-node2 rabbitmqctl stop_app
docker exec rabbitmq-node2 rabbitmqctl reset
docker exec rabbitmq-node2 rabbitmqctl join_cluster rabbit@rabbitmq-node1
docker exec rabbitmq-node2 rabbitmqctl start_app
```

#### 7.1.2 队列同步问题
```bash
# 检查队列同步状态
docker exec rabbitmq-node1 rabbitmqctl list_queues name synchronised_slave_pids

# 强制同步队列
docker exec rabbitmq-node1 rabbitmqctl sync_queue order_queue
```

#### 7.1.3 磁盘空间不足
```bash
# 检查磁盘使用情况
docker exec rabbitmq-node1 rabbitmq-diagnostics disk_free

# 清理过期数据
docker exec rabbitmq-node1 rabbitmqctl purge_queue order_queue
```

### 7.2 紧急恢复流程
```bash
#!/bin/bash
# emergency-recovery-rabbitmq.sh

set -e

echo "开始RabbitMQ紧急恢复流程..."

# 1. 停止应用服务
echo "停止应用服务..."
docker-compose -f docker-compose-application.yml down

# 2. 备份当前状态
echo "备份当前状态..."
./backup-rabbitmq.sh

# 3. 停止RabbitMQ集群
echo "停止RabbitMQ集群..."
docker-compose -f docker-compose-rabbitmq.yml down

# 4. 清理数据目录
echo "清理数据目录..."
rm -rf /data/rabbitmq/*
mkdir -p /data/rabbitmq/{node1,node2,node3}
chown -R 1000:1000 /data/rabbitmq

# 5. 重新启动集群
echo "重新启动集群..."
docker-compose -f docker-compose-rabbitmq.yml up -d

# 6. 等待集群就绪
echo "等待集群就绪..."
sleep 60

# 7. 恢复配置
echo "恢复配置..."
LATEST_BACKUP=$(ls -t /data/backups/rabbitmq/rabbitmq_backup_*.json | head -1)
docker cp $LATEST_BACKUP rabbitmq-node1:/tmp/definitions.json
docker exec rabbitmq-node1 rabbitmqctl import_definitions /tmp/definitions.json

# 8. 启动应用服务
echo "启动应用服务..."
docker-compose -f docker-compose-application.yml up -d

# 9. 验证服务
echo "验证服务..."
./scripts/validate-application.sh

echo "✅ RabbitMQ紧急恢复完成！"
```

## 8. 附录

### 8.1 配置检查清单
- [ ] 集群节点配置正确
- [ ] Erlang Cookie一致
- [ ] 镜像队列策略配置正确
- [ ] HAProxy负载均衡配置正确
- [ ] 监控插件启用
- [ ] 备份策略配置正确
- [ ] 防火墙规则配置正确
- [ ] 磁盘空间充足

### 8.2 性能基准
| 指标 | 目标值 | 当前值 | 状态 |
|------|--------|--------|------|
| 消息吞吐量 | > 10,000 msg/s | - | - |
| 连接响应时间 | < 100ms | - | - |
| 队列深度 | < 10,000 | - | - |
| 内存使用率 | < 60% | - | - |
| 磁盘I/O使用率 | < 70% | - | - |
| 网络延迟 | < 10ms | - | - |

### 8.3 相关命令参考
```bash
# 管理命令
rabbitmqctl status
rabbitmqctl cluster_status
rabbitmqctl list_queues
rabbitmqctl list_exchanges
rabbitmqctl list_bindings

# 诊断命令
rabbitmq-diagnostics node_health_check
rabbitmq-diagnostics memory_breakdown
rabbitmq-diagnostics disk_free

# 监控命令
rabbitmq-top
rabbitmq-queues rebalance
```

---

**文档版本**: v1.0  
**创建时间**: 2026-04-27  
**更新记录**:
- v1.0 (2026-04-27): 初始版本，创建消息队列集群配置文档

**负责人**: doc-writer  
**审核人**: devops-engineer  
**批准人**: main