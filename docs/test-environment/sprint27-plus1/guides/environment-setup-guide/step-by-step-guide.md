# Sprint 27+1 测试环境搭建指南 - 步骤化详细指南

## 📋 指南概述
**Sprint版本**: 27+1 (测试环境配置专项)  
**创建时间**: 2026-05-05  
**文档状态**: ✅ 详细步骤指南制定完成  
**适用环境**: Ubuntu 22.04 LTS / CentOS 8  
**预估时间**: 2-3小时完成完整环境搭建  
**难度级别**: 中等 (需要基本的Linux操作和Docker知识)

## 🎯 搭建目标
通过本指南，您将完成以下环境的搭建：
- ✅ **基础环境**: Docker, Java 17, Node.js 18, MySQL 8, Redis 7
- ✅ **核心服务**: ERP订单服务、库存服务、采购管理、销售管理
- ✅ **AI服务**: AI资源服务、AI模型服务、NLP处理服务
- ✅ **监控系统**: Prometheus, Grafana, ELK Stack
- ✅ **网络配置**: 服务发现、负载均衡、安全配置

## 📂 目录结构准备

### 步骤1: 创建项目根目录
```bash
# 创建项目根目录
sudo mkdir -p /opt/ai-ready-test-env
sudo chown -R $USER:$USER /opt/ai-ready-test-env
cd /opt/ai-ready-test-env

# 创建标准目录结构
mkdir -p {configs,scripts,logs,data,backups,docs}
mkdir -p {docker-compose,monitoring,databases,services,network}
mkdir -p {scripts/init,scripts/backup,scripts/health-check}
mkdir -p {logs/app,logs/db,logs/monitoring}
mkdir -p {data/mysql,data/redis,data/mongodb,data/prometheus}
mkdir -p {backups/daily,backups/weekly,backups/monthly}

# 验证目录结构
tree -L 2 /opt/ai-ready-test-env/
```

### 步骤2: 创建环境变量配置文件
```bash
# 创建主环境变量文件
cat > /opt/ai-ready-test-env/.env << 'EOF'
# ============================================
# Sprint 27+1 测试环境配置
# ============================================

# 环境标识
ENV_NAME=sprint27-plus1-test
ENV_TYPE=testing
ENV_VERSION=v1.0.0

# 网络配置
NETWORK_NAME=ai-ready-network
NETWORK_SUBNET=172.20.0.0/16
NETWORK_GATEWAY=172.20.0.1

# Docker配置
DOCKER_REGISTRY=registry.aiedge.cn
DOCKER_NAMESPACE=ai-ready
DOCKER_TAG=latest

# 服务端口映射
HTTP_PORT=80
HTTPS_PORT=443
API_GATEWAY_PORT=8080
ERP_ORDER_PORT=8081
ERP_STOCK_PORT=8082
ERP_PURCHASE_PORT=8083
ERP_SALE_PORT=8084
AI_SERVICE_PORT=8085

# 数据库端口
MYSQL_PORT=3306
REDIS_PORT=6379
MONGODB_PORT=27017

# 监控端口
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
ELASTICSEARCH_PORT=9200
KIBANA_PORT=5601

# 数据库配置
MYSQL_ROOT_PASSWORD=AiReady@2026Sprint27+1
MYSQL_DATABASE=ai_ready_test
MYSQL_USER=ai_ready_user
MYSQL_PASSWORD=AiReadyUser@2026

REDIS_PASSWORD=Redis@2026Sprint27+1

# 应用配置
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"
NODE_ENV=production
SPRING_PROFILES_ACTIVE=test

# 时区配置
TZ=Asia/Shanghai
LANG=zh_CN.UTF-8

# 日志配置
LOG_LEVEL=INFO
LOG_PATH=/opt/ai-ready-test-env/logs

# 监控配置
METRICS_ENABLED=true
TRACING_ENABLED=true
HEALTH_CHECK_ENABLED=true

# 安全配置
JWT_SECRET=AiReadyTestJWTSecret2026Sprint27+1
ENCRYPTION_KEY=32ByteEncryptionKeyForTestEnv2026
EOF

# 创建敏感信息加密文件（仅用于测试环境）
cat > /opt/ai-ready-test-env/.env.secret << 'EOF'
# 敏感信息配置（实际生产环境应使用密钥管理服务）
MYSQL_ROOT_PASSWORD_ENCRYPTED=ENC[AES256_GCM,data:xxxx,iv:yyyy,tag:zzzz]
REDIS_PASSWORD_ENCRYPTED=ENC[AES256_GCM,data:xxxx,iv:yyyy,tag:zzzz]
JWT_SECRET_ENCRYPTED=ENC[AES256_GCM,data:xxxx,iv:yyyy,tag:zzzz]

# API密钥（测试环境使用）
ALIYUN_ACCESS_KEY=test_access_key
ALIYUN_SECRET_KEY=test_secret_key
TENCENT_CLOUD_SECRET_ID=test_secret_id
TENCENT_CLOUD_SECRET_KEY=test_secret_key

# 第三方服务配置
WECHAT_APP_ID=test_app_id
WECHAT_APP_SECRET=test_app_secret
ALIPAY_APP_ID=test_alipay_app_id
ALIPAY_PRIVATE_KEY=test_private_key
EOF
```

## 🐳 Docker环境配置

### 步骤3: Docker安装与配置
```bash
# 卸载旧版本Docker
sudo apt-get remove docker docker-engine docker.io containerd runc

# 安装依赖包
sudo apt-get update
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# 添加Docker官方GPG密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 设置稳定版仓库
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装Docker引擎
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 验证安装
docker --version
docker-compose --version

# 配置Docker镜像加速（阿里云）
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://registry.docker-cn.com",
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "live-restore": true,
  "default-ulimits": {
    "nofile": {
      "name": "nofile",
      "soft": 65535,
      "hard": 65535
    }
  }
}
EOF

# 启动Docker服务
sudo systemctl enable docker
sudo systemctl start docker
sudo systemctl status docker

# 将当前用户加入docker组
sudo usermod -aG docker $USER
newgrp docker

# 验证Docker运行
docker run hello-world
```

### 步骤4: Docker Compose配置
```bash
# 创建主Docker Compose文件
cat > /opt/ai-ready-test-env/docker-compose.yml << 'EOF'
version: '3.8'

# 网络配置
networks:
  ai-ready-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1

# 卷配置
volumes:
  mysql-data:
    driver: local
  redis-data:
    driver: local
  mongodb-data:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
  elasticsearch-data:
    driver: local

# 服务配置
services:
  # ============ 数据库服务 ============
  mysql:
    image: mysql:8.0
    container_name: ai-ready-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: ${TZ}
    ports:
      - "${MYSQL_PORT}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./configs/mysql/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./scripts/init/mysql:/docker-entrypoint-initdb.d:ro
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  redis:
    image: redis:7-alpine
    container_name: ai-ready-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    ports:
      - "${REDIS_PORT}:6379"
    volumes:
      - redis-data:/data
      - ./configs/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
    networks:
      - ai-ready-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  mongodb:
    image: mongo:6
    container_name: ai-ready-mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    ports:
      - "${MONGODB_PORT}:27017"
    volumes:
      - mongodb-data:/data/db
    networks:
      - ai-ready-network

  # ============ 监控服务 ============
  prometheus:
    image: prom/prometheus:latest
    container_name: ai-ready-prometheus
    restart: unless-stopped
    ports:
      - "${PROMETHEUS_PORT}:9090"
    volumes:
      - prometheus-data:/prometheus
      - ./configs/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./configs/prometheus/rules.yml:/etc/prometheus/rules.yml:ro
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
      - '--web.enable-lifecycle'
    networks:
      - ai-ready-network

  grafana:
    image: grafana/grafana:latest
    container_name: ai-ready-grafana
    restart: unless-stopped
    ports:
      - "${GRAFANA_PORT}:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      GF_INSTALL_PLUGINS: grafana-piechart-panel,grafana-clock-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./configs/grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./configs/grafana/datasources:/etc/grafana/provisioning/datasources:ro
    networks:
      - ai-ready-network
    depends_on:
      - prometheus

  # ============ 日志服务 ============
  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: ai-ready-elasticsearch
    restart: unless-stopped
    environment:
      discovery.type: single-node
      xpack.security.enabled: false
      ES_JAVA_OPTS: "-Xms1g -Xmx1g"
    ports:
      - "${ELASTICSEARCH_PORT}:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - ai-ready-network

  kibana:
    image: kibana:8.11.0
    container_name: ai-ready-kibana
    restart: unless-stopped
    ports:
      - "${KIBANA_PORT}:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    networks:
      - ai-ready-network
    depends_on:
      - elasticsearch

  # ============ 业务服务（占位符） ============
  api-gateway:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/api-gateway:${DOCKER_TAG}
    container_name: ai-ready-api-gateway
    restart: unless-stopped
    ports:
      - "${API_GATEWAY_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
    volumes:
      - ./logs/app/api-gateway:/app/logs
    networks:
      - ai-ready-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  # 更多业务服务配置将根据实际镜像进行扩展
EOF

# 创建服务特定的Docker Compose文件
cat > /opt/ai-ready-test-env/docker-compose.erp.yml << 'EOF'
version: '3.8'

services:
  erp-order:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/erp-order:${DOCKER_TAG}
    container_name: ai-ready-erp-order
    restart: unless-stopped
    ports:
      - "${ERP_ORDER_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${MYSQL_DATABASE}
      DB_USER: ${MYSQL_USER}
      DB_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ./logs/app/erp-order:/app/logs
    networks:
      - ai-ready-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  erp-stock:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/erp-stock:${DOCKER_TAG}
    container_name: ai-ready-erp-stock
    restart: unless-stopped
    ports:
      - "${ERP_STOCK_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
    volumes:
      - ./logs/app/erp-stock:/app/logs
    networks:
      - ai-ready-network
    depends_on:
      - erp-order

  erp-purchase:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/erp-purchase:${DOCKER_TAG}
    container_name: ai-ready-erp-purchase
    restart: unless-stopped
    ports:
      - "${ERP_PURCHASE_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
    volumes:
      - ./logs/app/erp-purchase:/app/logs
    networks:
      - ai-ready-network
    depends_on:
      - erp-order

  erp-sale:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/erp-sale:${DOCKER_TAG}
    container_name: ai-ready-erp-sale
    restart: unless-stopped
    ports:
      - "${ERP_SALE_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
    volumes:
      - ./logs/app/erp-sale:/app/logs
    networks:
      - ai-ready-network
    depends_on:
      - erp-order
EOF

# 创建AI服务Docker Compose文件
cat > /opt/ai-ready-test-env/docker-compose.ai.yml << 'EOF'
version: '3.8'

services:
  ai-service:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/ai-service:${DOCKER_TAG}
    container_name: ai-ready-ai-service
    restart: unless-stopped
    ports:
      - "${AI_SERVICE_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
      AI_MODEL_PATH: /app/models
      AI_CACHE_SIZE: 1024
    volumes:
      - ./logs/app/ai-service:/app/logs
      - ./data/models:/app/models
    networks:
      - ai-ready-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  nlp-service:
    image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/nlp-service:${DOCKER_TAG}
    container_name: ai-ready-nlp-service
    restart: unless-stopped
    ports:
      - "8086:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JAVA_OPTS: ${JAVA_OPTS}
      TZ: ${TZ}
      NLP_MODEL_PATH: /app/models/nlp
    volumes:
      - ./logs/app/nlp-service:/app/logs
      - ./data/models/nlp:/app/models/nlp
    networks:
      - ai-ready-network
    depends_on:
      - ai-service
EOF
```

## 🔧 配置文件创建

### 步骤5: 创建配置文件目录和文件
```bash
# 创建MySQL配置文件
mkdir -p /opt/ai-ready-test-env/configs/mysql
cat > /opt/ai-ready-test-env/configs/mysql/my.cnf << 'EOF'
[mysqld]
# 基础配置
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+08:00'
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION

# 性能优化
innodb_buffer_pool_size=512M
innodb_log_file_size=128M
innodb_flush_log_at_trx_commit=2
innodb_flush_method=O_DIRECT
max_connections=200
thread_cache_size=10
table_open_cache=2000

# 日志配置
slow_query_log=1
slow_query_log_file=/var/lib/mysql/slow.log
long_query_time=2
log_queries_not_using_indexes=1

# 二进制日志
server-id=1
log-bin=mysql-bin
binlog_format=ROW
expire_logs_days=7
max_binlog_size=100M

[client]
default-character-set=utf8mb4

[mysql]
default-character-set=utf8mb4
EOF

# 创建Redis配置文件
mkdir -p /opt/ai-ready-test-env/configs/redis
cat > /opt/ai-ready-test-env/configs/redis/redis.conf << 'EOF'
# Redis配置文件 - 测试环境

# 基础配置
bind 0.0.0.0
protected-mode no
port 6379
timeout 0
tcp-keepalive 300

# 安全配置
requirepass ${REDIS_PASSWORD}
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command CONFIG ""

# 内存配置
maxmemory 1gb
maxmemory-policy allkeys-lru

# 持久化配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 性能配置
tcp-backlog 511
databases 16
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir ./

# 日志配置
loglevel notice
logfile ""

# 慢查询日志
slowlog-log-slower-than 10000
slowlog-max-len 128

# 客户端配置
maxclients 10000
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit replica 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60

# 集群配置（单机模式）
cluster-enabled no
EOF

# 创建Prometheus配置文件
mkdir -p /opt/ai-ready-test-env/configs/prometheus
cat > /opt/ai-ready-test-env/configs/prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'ai-ready-test-env'
    environment: 'sprint27-plus1'

# Alertmanager配置
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093

# 规则文件
rule_files:
  - "rules.yml"

# 抓取配置
scrape_configs:
  # Prometheus自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node Exporter（需要单独部署）
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']

  # Docker监控
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']

  # MySQL监控
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']

  # Redis监控
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # 业务应用监控
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'api-gateway:8080'
        - 'erp-order:8080'
        - 'erp-stock:8080'
        - 'erp-purchase:8080'
        - 'erp-sale:8080'
        - 'ai-service:8080'
        - 'nlp-service:8080'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '([^:]+)(?::\d+)?'
        replacement: '${1}'
EOF

# 创建Prometheus告警规则
cat > /opt/ai-ready-test-env/configs/prometheus/rules.yml << 'EOF'
groups:
  - name: ai-ready-test-alerts
    rules:
      # 系统级告警
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

      - alert: HighDiskUsage
        expr: 100 - (node_filesystem_avail_bytes{fstype!="tmpfs"} / node_filesystem_size_bytes{fstype!="tmpfs"} * 100) > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高磁盘使用率"
          description: "实例 {{ $labels.instance }} 的磁盘使用率超过90% (当前值: {{ $value }}%)"

      # 服务级告警
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务下线"
          description: "服务 {{ $labels.job }} 实例 {{ $labels.instance }} 已下线"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100 > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "高错误率"
          description: "服务 {{ $labels.job }} 实例 {{ $labels.instance }} 的错误率超过5%"

      # 数据库告警
      - alert: MySQLDown
        expr: mysql_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "MySQL服务下线"
          description: "MySQL实例已下线"

      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis服务下线"
          description: "Redis实例已下线"
EOF

# 创建Grafana配置
mkdir -p /opt/ai-ready-test-env/configs/grafana/{dashboards,datasources}
cat > /opt/ai-ready-test-env/configs/grafana/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

cat > /opt/ai-ready-test-env/configs/grafana/dashboards/dashboard.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF
```

## 🚀 初始化脚本创建

### 步骤6: 创建初始化脚本
```bash
# 创建MySQL初始化脚本
mkdir -p /opt/ai-ready-test-env/scripts/init
cat > /opt/ai-ready-test-env/scripts/init/mysql/01-create-databases.sql << 'EOF'
-- Sprint 27+1 测试环境数据库初始化脚本
-- 创建时间: 2026-05-05

-- 创建ERP业务数据库
CREATE DATABASE IF NOT EXISTS erp_order_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS erp_stock_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS erp_purchase_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS erp_sale_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS erp_finance_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建AI服务数据库
CREATE DATABASE IF NOT EXISTS ai_service_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nlp_service_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ml_service_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户服务数据库
CREATE DATABASE IF NOT EXISTS user_service_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建监控和日志数据库
CREATE DATABASE IF NOT EXISTS monitoring_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS audit_log_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建测试用户并授权
CREATE USER IF NOT EXISTS 'erp_test_user'@'%' IDENTIFIED BY 'ErpTestUser@2026';
CREATE USER IF NOT EXISTS 'ai_test_user'@'%' IDENTIFIED BY 'AiTestUser@2026';
CREATE USER IF NOT EXISTS 'monitoring_user'@'%' IDENTIFIED BY 'MonitoringUser@2026';

-- 授权ERP用户
GRANT ALL PRIVILEGES ON erp_order_test.* TO 'erp_test_user'@'%';
GRANT ALL PRIVILEGES ON erp_stock_test.* TO 'erp_test_user'@'%';
GRANT ALL PRIVILEGES ON erp_purchase_test.* TO 'erp_test_user'@'%';
GRANT ALL PRIVILEGES ON erp_sale_test.* TO 'erp_test_user'@'%';
GRANT ALL PRIVILEGES ON erp_finance_test.* TO 'erp_test_user'@'%';

-- 授权AI用户
GRANT ALL PRIVILEGES ON ai_service_test.* TO 'ai_test_user'@'%';
GRANT ALL PRIVILEGES ON nlp_service_test.* TO 'ai_test_user'@'%';
GRANT ALL PRIVILEGES ON ml_service_test.* TO 'ai_test_user'@'%';

-- 授权监控用户
GRANT SELECT, INSERT, UPDATE, DELETE ON monitoring_test.* TO 'monitoring_user'@'%';
GRANT SELECT, INSERT ON audit_log_test.* TO 'monitoring_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 记录初始化完成
INSERT INTO ai_ready_test.environment_log (event_type, event_message, created_by) 
VALUES ('DATABASE_INIT', 'Sprint 27+1测试环境数据库初始化完成', 'system');
EOF

# 创建环境检查脚本
cat > /opt/ai-ready-test-env/scripts/health-check.sh << 'EOF'
#!/bin/bash
# 测试环境健康检查脚本
# 创建时间: 2026-05-05

set -e

echo "========================================"
echo "Sprint 27+1 测试环境健康检查"
echo "检查时间: $(date)"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_docker() {
    echo "检查Docker服务..."
    if docker ps &> /dev/null; then
        echo -e "${GREEN}✓ Docker服务正常${NC}"
    else
        echo -e "${RED}✗ Docker服务异常${NC}"
        return 1
    fi
}

check_mysql() {
    echo "检查MySQL服务..."
    if docker exec ai-ready-mysql mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD} &> /dev/null; then
        echo -e "${GREEN}✓ MySQL服务正常${NC}"
    else
        echo -e "${RED}✗ MySQL服务异常${NC}"
        return 1
    fi
}

check_redis() {
    echo "检查Redis服务..."
    if docker exec ai-ready-redis redis-cli -a ${REDIS_PASSWORD} ping | grep -q PONG; then
        echo -e "${GREEN}✓ Redis服务正常${NC}"
    else
        echo -e "${RED}✗ Redis服务异常${NC}"
        return 1
    fi
}

check_prometheus() {
    echo "检查Prometheus服务..."
    if curl -s http://localhost:${PROMETHEUS_PORT}/-/healthy &> /dev/null; then
        echo -e "${GREEN}✓ Prometheus服务正常${NC}"
    else
        echo -e "${RED}✗ Prometheus服务异常${NC}"
        return 1
    fi
}

check_grafana() {
    echo "检查Grafana服务..."
    if curl -s http://localhost:${GRAFANA_PORT}/api/health &> /dev/null; then
        echo -e "${GREEN}✓ Grafana服务正常${NC}"
    else
        echo -e "${RED}✗ Grafana服务异常${NC}"
        return 1
    fi
}

check_ports() {
    echo "检查服务端口..."
    local ports=("80" "443" "8080" "8081" "8082" "8083" "8084" "8085" "3306" "6379" "9090" "3000")
    local all_ok=true
    
    for port in "${ports[@]}"; do
        if ss -tln | grep -q ":$port "; then
            echo -e "  ${GREEN}✓ 端口 $port 监听正常${NC}"
        else
            echo -e "  ${RED}✗ 端口 $port 未监听${NC}"
            all_ok=false
        fi
    done
    
    if $all_ok; then
        return 0
    else
        return 1
    fi
}

check_resources() {
    echo "检查系统资源..."
    
    # 检查CPU负载
    local load=$(uptime | awk -F