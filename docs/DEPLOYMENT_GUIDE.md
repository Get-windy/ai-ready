# 智企连·AI-Ready 部署方案

**版本**: v1.0  
**日期**: 2026-03-27  
**作者**: devops-engineer  
**项目**: 智企连·AI-Ready

---

## 执行摘要

本文档定义智企连·AI-Ready 项目的完整部署方案，涵盖生产环境、开发环境和测试环境的部署架构、部署流程、回滚方案和监控方案。

---

## 1. 部署架构设计

### 1.1 生产环境部署架构

#### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        智企连生产环境部署架构                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         ┌─────────────────┐                                  │
│                         │   CDN/WAF       │                                  │
│                         │  (阿里云DDoS)   │                                  │
│                         └────────┬────────┘                                  │
│                                  │                                           │
│                       ┌──────────▼──────────┐                                │
│                       │     负载均衡层        │                                │
│                       │  ┌─────────────┐    │                                │
│                       │  │  NGINX  A   │    │                                │
│                       │  │ (主/Keepalived)│   │                                │
│                       │  └─────────────┘    │                                │
│                       │     10.0.1.100 VIP    │                                │
│                       └──────────┬──────────┘                                │
│                                  │                                           │
│                      ┌───────────▼───────────┐                              │
│                      │      应用层集群         │                              │
│                      │  ┌───────┐ ┌───────┐ │                              │
│                      │  │ App 1 │ │ App 2 │ │                              │
│                      │  └───┬───┘ └───┬───┘ │                              │
│                      │    │          │     │                              │
│                      │    └──────────┴─────┘                              │
│                      │         Spring Boot                │                              │
│                      └─────────────────────────────────────┘                              │
│                                  │                                           │
│              ┌───────────────────┼───────────────────┐                    │
│              │                   │                   │                    │
│        ┌─────▼─────┐     ┌───────▼────────┐     ┌─────▼─────┐           │
│        │ Redis     │     │ PostgreSQL     │     │ Log Center│           │
│        │ Cluster   │     │Primary + Replica│    │ ELK       │           │
│        │ 3节点     │     │ (Patroni HA)   │    │           │           │
│        └───────────┘     └────────────────┘    └───────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 1.1.1 生产环境服务器规划

| 角色 | 服务器数量 | 配置 | IP范围 | 用途 |
|------|-----------|------|--------|------|
| NGINX 负载均衡 | 2 | 4C/8G/200G | 10.0.1.101-102 | 前端流量入口 |
| 应用服务器 | 3 | 8C/16G/500G | 10.0.1.201-203 | Spring Boot 应用 |
| Redis 集群 | 3 | 4C/8G/100G | 10.0.1.301-303 | Redis Cluster |
| PostgreSQL 主节点 | 1 | 16C/64G/1T | 10.0.1.401 | 数据库主节点 |
| PostgreSQL 从节点 | 2 | 16C/64G/1T | 10.0.1.402-403 | 数据库从节点 |
| ELK 日志中心 | 3 | 8C/16G/2T | 10.0.1.501-503 | 日志收集分析 |

#### 1.1.2 生产环境网络规划

```
互联网
  │
  ▼
┌───────────────────────────────────────────────────────────┐
│                    边缘层 (DMZ)                            │
│  - CDN:静态资源加速                                        │
│  - WAF:Web应用防火墙                                       │
│  - DDOS防护:防DDoS攻击                                     │
└───────────────────────────────────────────────────────────┘
  │
  ▼
┌───────────────────────────────────────────────────────────┐
│                    核心层 (内网)                           │
│  - 负载均衡:NGINX Keepalived HA                           │
│  - 应用集群:Spring Boot 3节点                             │
└───────────────────────────────────────────────────────────┘
  │
  ▼
┌───────────────────────────────────────────────────────────┐
│                    数据层 (专属网络)                       │
│  - Redis Cluster:3节点HA                                  │
│  - PostgreSQL HA:主从复制                                 │
│  - ELK:日志中心                                           │
└───────────────────────────────────────────────────────────┘
```

### 1.2 开发环境部署架构

#### 开发环境配置

```
┌────────────────────────────────────────────────────────────┐
│                      开发环境部署架构                       │
└────────────────────────────────────────────────────────────┘

开发服务器 (10.0.2.10)
├── NGINX (80/443)
├── Spring Boot (8080)
│   ├── application-dev.yml
│   └── application-local.yml
├── PostgreSQL (5432) - 单机版
├── Redis (6379) - 单机版
└── Filebeat (5044) - 日志采集
```

#### 开发环境配置文件

```yaml
# application-dev.yml (开发环境)

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/devdb?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: devuser
    password: Dev@2026#Local
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
  
  redis:
    host: localhost
    port: 6379
    password: 
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImp

server:
  port: 8080
  servlet:
    context-path: /
  
  # 开发环境调试配置
  jetty:
    accesslog:
      enabled: true
```

### 1.3 测试环境部署架构

#### 测试环境配置

```
┌────────────────────────────────────────────────────────────┐
│                      测试环境部署架构                       │
└────────────────────────────────────────────────────────────┘

测试服务器 (10.0.3.10-12)
├── NGINX (负载均衡)
│   ├── server 1: 10.0.3.11:80
│   └── server 2: 10.0.3.12:80
├── Spring Boot (2节点)
│   ├── App 1: 10.0.3.21:8080
│   └── App 2: 10.0.3.22:8080
├── PostgreSQL (主从)
│   ├── Primary: 10.0.3.31:5432
│   └── Replica: 10.0.3.32:5432
├── Redis Cluster (3节点)
│   ├── Node 1: 10.0.3.41:6379
│   ├── Node 2: 10.0.3.42:6379
│   └── Node 3: 10.0.3.43:6379
└── ELK (日志中心)
    ├── Elasticsearch: 10.0.3.51:9200
    ├── Kibana: 10.0.3.52:5601
    └── Logstash: 10.0.3.53:5044
```

#### 测试环境配置文件

```yaml
# application-test.yml (测试环境)

spring:
  datasource:
    url: jdbc:postgresql://10.0.3.31:5432/testdb?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: testuser
    password: Test@2026#Local
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
  
  redis:
    cluster:
      nodes:
        - 10.0.3.41:6379
        - 10.0.3.42:6379
        - 10.0.3.43:6379
    password: 
    lettuce:
      pool:
        max-active: 16

server:
  port: 8080
  servlet:
    context-path: /api/v1

# 日志配置
logging:
  level:
    root: INFO
    com.aiedge: DEBUG
  file:
    name: /var/log/ai-ready/app.log
```

---

## 2. 部署流程设计

### 2.1 部署准备

#### 2.1.1 前置条件检查

```bash
#!/bin/bash
# pre-deploy-check.sh - 部署前置条件检查

echo "=========================================="
echo "智企连部署前置条件检查"
echo "=========================================="

# 检查系统要求
echo "1. 检查系统要求..."

# 检查Java版本
if [ -z "$(command -v java)" ]; then
    echo "❌ Java 未安装"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | grep 'version' | awk -F'"' '{print $2}')
echo "✅ Java 版本: $JAVA_VERSION"

# 检查Maven
if [ -z "$(command -v mvn)" ]; then
    echo "❌ Maven 未安装"
    exit 1
fi
MAVEN_VERSION=$(mvn -version 2>&1 | head -1)
echo "✅ Maven 版本: $MAVEN_VERSION"

# 检查Docker
if [ -z "$(command -v docker)" ]; then
    echo "⚠️ Docker 未安装 (可选)"
else
    DOCKER_VERSION=$(docker --version)
    echo "✅ Docker 版本: $DOCKER_VERSION"
fi

# 检查网络
echo "2. 检查网络连接..."
if ! ping -c 1 api.ai-ready.cn > /dev/null 2>&1; then
    echo "⚠️ 外网连接异常"
fi

# 检查数据库连接
echo "3. 检查数据库连接..."
if ! psql -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "SELECT 1;" > /dev/null 2>&1; then
    echo "❌ PostgreSQL 连接失败"
    exit 1
fi
echo "✅ PostgreSQL 连接成功"

# 检查 Redis 连接
echo "4. 检查 Redis 连接..."
if ! redis-cli ping > /dev/null 2>&1; then
    echo "❌ Redis 连接失败"
    exit 1
fi
echo "✅ Redis 连接成功"

echo "=========================================="
echo "✅ 所有前置条件检查通过"
echo "=========================================="
```

#### 2.1.2 环境变量配置

```bash
#!/bin/bash
# env-config.sh - 环境变量配置

# ==================== 基础配置 ====================
export APP_NAME="ai-ready"
export APP_VERSION="1.0.0"
export APP_ENV="production"

# ==================== 数据库配置 ====================
export POSTGRES_HOST="prod-db.ai-ready.cn"
export POSTGRES_PORT="5432"
export POSTGRES_USER="produser"
export POSTGRES_PASSWORD="StrongPassword2026!"
export POSTGRES_DB="proddb"

# ==================== Redis配置 ====================
export REDIS_HOST="prod-redis.ai-ready.cn"
export REDIS_PORT="6379"
export REDIS_PASSWORD="StrongRedisPassword2026!"

# ==================== 应用配置 ====================
export SERVER_PORT="8080"
export SERVER_CONTEXT_PATH="/"
export JAVA_OPTS="-Xms4g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# ==================== 监控配置 ====================
export PROMETHEUS_HOST="prometheus.ai-ready.cn"
export PROMETHEUS_PORT="9090"
export GRAFANA_URL="https://grafana.ai-ready.cn"

# ==================== 日志配置 ====================
export LOG_LEVEL="INFO"
export LOG_PATH="/var/log/ai-ready"

# ==================== 安全配置 ====================
export JWT_SECRET="YourSuperSecretJWTKey2026!"
export ENCRYPTION_KEY="EncryptionKey2026!"
```

### 2.2 部署步骤设计

#### 2.2.1 生产环境部署步骤

```bash
#!/bin/bash
# deploy-production.sh - 生产环境部署脚本

set -e

echo "=========================================="
echo "智企连生产环境部署"
echo "=========================================="

# 1. 加载环境变量
echo "1. 加载环境变量..."
source /etc/profile.d/ai-ready.sh

# 2. 代码拉取
echo "2. 拉取最新代码..."
cd /opt/ai-ready
git pull origin main

# 3. 版本构建
echo "3. 构建应用..."
mvn clean package -DskipTests -P production

# 4. 备份旧版本
echo "4. 备份旧版本..."
BACKUP_DIR="/opt/ai-ready/backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp -r /opt/ai-ready/app-*.jar $BACKUP_DIR/
cp -r /opt/ai-ready/application-*.yml $BACKUP_DIR/

# 5. 健康检查
echo "5. 执行部署前健康检查..."
curl -f http://localhost:8080/actuator/health || true

# 6. 停止旧服务
echo "6. 停止旧服务..."
systemctl stop ai-ready || true

# 7. 启动新服务
echo "7. 启动新服务..."
nohup java $JAVA_OPTS -jar /opt/ai-ready/app-*.jar --spring.profiles.active=production \
    > /var/log/ai-ready/app.log 2>&1 &

# 8. 等待服务启动
echo "8. 等待服务启动..."
sleep 30

# 9. 健康验证
echo "9. 执行健康验证..."
for i in {1..10}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
    if [ "$HTTP_CODE" == "200" ]; then
        echo "✅ 服务启动成功"
        break
    fi
    echo "等待中... ($i/10)"
    sleep 10
done

# 10. 上线验证
echo "10. 执行上线验证..."
curl -f http://localhost:8080/actuator/health/readiness || {
    echo "❌ 健康检查失败，执行回滚"
    ./rollback.sh
    exit 1
}

# 11. 日志验证
echo "11. 检查应用日志..."
grep -i "started" /var/log/ai-ready/app.log | tail -5

# 12. 通知
echo "12. 发送部署通知..."
curl -X POST "${SLACK_WEBHOOK}" \
    -H 'Content-type: application/json' \
    -d "{
        \"text\": \"✅ 智企连 生产环境部署成功\\n版本: $APP_VERSION\\n时间: $(date '+%Y-%m-%d %H:%M:%S')\",
        \"blocks\": [
            {
                \"type\": \"section\",
                \"text\": {\"type\": \"mrkdwn\", \"text\": \"*✅ 智企连 生产环境部署成功*\"}
            },
            {
                \"type\": \"section\",
                \"fields\": [
                    {\"type\": \"mrkdwn\", \"text\": \"*版本*: $APP_VERSION\"},
                    {\"type\": \"mrkdwn\", \"text\": \"*时间*: $(date '+%Y-%m-%d %H:%M:%S')\"},
                    {\"type\": \"mrkdwn\", \"text\": \"*环境*: production\"}
                ]
            }
        ]
    }"

echo "=========================================="
echo "✅ 生产环境部署完成"
echo "=========================================="
```

#### 2.2.2 Docker 部署

```yaml
# docker-compose.yml (生产环境)

version: '3.8'

services:
  # 应用服务
  app:
    image: ai-ready/app:1.0.0
    container_name: ai-ready-app
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
      - SPRING_REDIS_HOST=${SPRING_REDIS_HOST}
      - JAVA_OPTS=${JAVA_OPTS}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - ai-ready-network
    restart: unless-stopped

  # NGINX 负载均衡
  nginx:
    image: nginx:alpine
    container_name: ai-ready-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - app
    networks:
      - ai-ready-network
    restart: unless-stopped

networks:
  ai-ready-network:
    driver: bridge
```

### 2.3 回滚方案设计

#### 2.3.1 回滚场景

| 场景 | 描述 | 回滚方式 |
|------|------|----------|
| 服务启动失败 | 应用启动后健康检查失败 | 自动回滚 |
| 严重 Bug | 生产环境发现严重 Bug | 紧急回滚 |
| 性能下降 | 新版本性能明显下降 | 手动回滚 |
| 数据库异常 | 数据库兼容性问题 | 紧急回滚 |

#### 2.3.2 回滚脚本

```bash
#!/bin/bash
# rollback.sh - 回滚脚本

set -e

echo "=========================================="
echo "智企连版本回滚"
echo "=========================================="

# 1. 加载环境变量
source /etc/profile.d/ai-ready.sh

# 2. 确认回滚
read -p "确认回滚到上一个版本? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "回滚取消"
    exit 0
fi

# 3. 查找备份
BACKUP_DIR=$(ls -td /opt/ai-ready/backup/*/ | head -1)
if [ -z "$BACKUP_DIR" ]; then
    echo "❌ 未找到备份文件"
    exit 1
fi

echo "找到备份目录: $BACKUP_DIR"

# 4. 停止当前服务
echo "4. 停止当前服务..."
systemctl stop ai-ready || true

# 5. 恢复备份
echo "5. 恢复备份..."
rm -f /opt/ai-ready/app-*.jar
cp -r $BACKUP_DIR/*.jar /opt/ai-ready/

# 6. 启动服务
echo "6. 启动服务..."
nohup java $JAVA_OPTS -jar /opt/ai-ready/app-*.jar \
    --spring.profiles.active=production \
    > /var/log/ai-ready/app.log 2>&1 &

# 7. 等待启动
echo "7. 等待服务启动..."
sleep 30

# 8. 验证
echo "8. 验证..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ 回滚成功"
    curl -X POST "${SLACK_WEBHOOK}" \
        -H 'Content-type: application/json' \
        -d "{\"text\": \"⚠️ 智企连 已回滚到上一版本\\n时间: $(date '+%Y-%m-%d %H:%M:%S')\"}"
else
    echo "❌ 回滚失败，请手动处理"
    exit 1
fi

echo "=========================================="
echo "✅ 回滚完成"
echo "=========================================="
```

### 2.4 监控方案设计

#### 2.4.1 监控指标

```yaml
# 监控指标配置
monitoring:
  # 应用指标
  application:
    - name: http_requests_total
      type: counter
      description: HTTP请求总数
      labels: [method, path, status]
      
    - name: http_request_duration_seconds
      type: histogram
      description: HTTP请求延迟
      labels: [method, path]
      
    - name: jvm_memory_used_bytes
      type: gauge
      description: JVM内存使用
      labels: [area, category]
      
    - name: jvm_gc_pause_seconds
      type: histogram
      description: GC暂停时间

  # 业务指标
  business:
    - name: active_users
      type: gauge
      description: 活跃用户数
      labels: []
      
    - name: orders_created_total
      type: counter
      description: 订单创建总数
      labels: [status]

  # 基础设施指标
  infrastructure:
    - name: cpu_usage_percent
      type: gauge
      description: CPU使用率
      labels: []
      
    - name: memory_usage_percent
      type: gauge
      description: 内存使用率
      labels: []
      
    - name: disk_usage_percent
      type: gauge
      description: 磁盘使用率
      labels: [mountpoint]
```

#### 2.4.2 告警规则

```yaml
# 告警规则配置
alerting:
  groups:
    - name: ai-ready-alerts
      rules:
        # 服务可用性告警
        - alert: ServiceDown
          expr: up{job="ai-ready"} == 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "服务宕机"
            description: "实例 {{ $labels.instance }} 已宕机超过1分钟"
            
        # 性能告警
        - alert: HighResponseTime
          expr: histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 2
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "响应时间过高"
            description: "P99响应时间 {{ $value | humanize }}秒"
            
        # 错误率告警
        - alert: HighErrorRate
          expr: sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 0.05
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "错误率过高"
            description: "5xx错误率 {{ $value | humanizePercentage }}"
            
        # JVM告警
        - alert: HighMemoryUsage
          expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) * 100 > 85
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "JVM内存使用率过高"
            description: "内存使用率 {{ $value | humanize }}%"
```

---

## 3. 部署文档

### 3.1 部署检查清单

#### 部署前检查清单

```markdown
# 部署前检查清单

## 1. 环境准备
- [ ] 数据库连接配置正确
- [ ] Redis 连接配置正确
- [ ] 环境变量配置完成
- [ ] 日志目录创建完成

## 2. 代码检查
- [ ] 代码已通过 CI/CD 测试
- [ ] 代码已合并到 release 分支
- [ ] 版本号已更新
- [ ] Git Tag 已创建

## 3. 备份检查
- [ ] 数据库已备份
- [ ] 配置文件已备份
- [ ] 应用包已备份

## 4. 监控检查
- [ ] 监控系统已启动
- [ ] 告警规则已配置
- [ ] Dashboard 已更新

## 5. 人员准备
- [ ] 运维人员就位
- [ ] 开发人员电话待命
- [ ] 故障处理流程已明确

## 6. 验收标准
- [ ] 服务启动成功
- [ ] 健康检查通过
- [ ] 核心功能验证通过
- [ ] 性能指标达标
```

#### 部署后检查清单

```markdown
# 部署后检查清单

## 1. 服务状态检查
- [ ] 服务进程运行正常
- [ ] 健康检查全部通过
- [ ] 端口监听正常
- [ ] 日志无异常

## 2. 功能验证
- [ ] 主页访问正常
- [ ] 登录功能正常
- [ ] 核心业务流程正常
- [ ] API 响应正常

## 3. 监控验证
- [ ] 监控数据正常上报
- [ ] 告警规则正常
- [ ] Dashboard 数据正常

## 4. 性能验证
- [ ] 响应时间在预期范围内
- [ ] 错误率正常
- [ ] CPU/内存使用正常

## 5. 文档更新
- [ ] 部署日志已记录
- [ ] 变更文档已更新
- [ ] 知识库已更新
```

### 3.2 部署步骤文档

#### 生产环境部署步骤

| 步骤 | 操作 | 持续时间 | 风险等级 |
|------|------|----------|----------|
| 1 | 备份旧版本 | 5分钟 | 低 |
| 2 | 停止旧服务 | 1分钟 | 低 |
| 3 | 启动新服务 | 30秒 | 中 |
| 4 | 健康检查 | 2分钟 | 低 |
| 5 | 功能验证 | 10分钟 | 高 |
| 6 | 性能测试 | 5分钟 | 中 |
| 7 | 监控验证 | 2分钟 | 低 |

**总部署时间**: 约 25 分钟

#### 回滚步骤

| 步骤 | 操作 | 持续时间 | 风险等级 |
|------|------|----------|----------|
| 1 | 确认回滚 | 1分钟 | 低 |
| 2 | 停止当前服务 | 1分钟 | 低 |
| 3 | 恢复备份 | 30秒 | 低 |
| 4 | 启动服务 | 30秒 | 低 |
| 5 | 验证 | 2分钟 | 低 |

**总回滚时间**: 约 5 分钟

---

## 4. 运维手册

### 4.1 日常运维

#### 日常巡检

```bash
#!/bin/bash
# daily-inspection.sh - 日常巡检脚本

echo "=========================================="
echo "智企连日常巡检"
echo "=========================================="

# 1. 检查服务状态
echo "1. 检查服务状态..."
systemctl status ai-ready | grep -E "Active:|Main PID:" || true

# 2. 检查系统资源
echo "2. 检查系统资源..."
top -bn1 | head -20

# 3. 检查磁盘空间
echo "3. 检查磁盘空间..."
df -h

# 4. 检查日志
echo "4. 检查错误日志..."
grep -i "error\|exception\|failed" /var/log/ai-ready/app.log | tail -20 || echo "无错误"

# 5. 检查数据库连接
echo "5. 检查数据库连接..."
pg_isready -h localhost -p 5432 -U produser || true

# 6. 检查Redis连接
echo "6. 检查Redis连接..."
redis-cli ping || true

# 7. 检查磁盘使用率
echo "7. 检查磁盘使用率..."
df -h | grep -E "Filesystem|ai-ready"

echo "=========================================="
echo "巡检完成"
echo "=========================================="
```

#### 日志分类

| 日志类型 | 路径 | 保留时间 | 说明 |
|----------|------|----------|------|
| 应用日志 | /var/log/ai-ready/app.log | 30天 | 应用运行日志 |
| 访问日志 | /var/log/ai-ready/access.log | 7天 | HTTP访问日志 |
| 错误日志 | /var/log/ai-ready/error.log | 90天 | 错误日志 |
| gc日志 | /var/log/ai-ready/gc.log | 7天 | GC日志 |
| 慢查询日志 | /var/log/ai-ready/slow.log | 14天 | 慢查询日志 |

### 4.2 故障处理

#### 常见故障处理

| 故障现象 | 可能原因 | 处理方法 |
|----------|----------|----------|
| 服务无法启动 | 端口占用 | `lsof -i:8080` 查找进程并停止 |
| 服务启动失败 | 配置错误 | 检查 application.yml 配置 |
| 数据库连接失败 | 网络问题 | 检查防火墙和数据库状态 |
| Redis 连接失败 | 密码错误 | 检查 Redis 密码配置 |
| 响应时间慢 | 内存不足 | 检查 JVM 内存配置 |
| 错误率高 | 代码异常 | 查看错误日志定位问题 |

#### 故障处理流程

```
故障发现
   │
   ├─→ 监控告警
   ├─→ 用户反馈
   └─→ 巡检发现

故障定位
   │
   ├─→ 查看服务状态
   ├─→ 检查日志
   └─→ 验证依赖服务

故障处理
   │
   ├─→ 临时处理 (重启/回滚)
   └─→ 根本解决 (代码/配置)

故障复盘
   │
   ├─→ 故障分析
   ├─→ 改进措施
   └─→ 文档更新
```

---

## 5. 附录

### 5.1 配置文件模板

#### application.yml (生产环境)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}?useSSL=true&serverTimezone=UTC&characterEncoding=utf8
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
  
  redis:
    cluster:
      nodes: ${REDIS_NODES}
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 16
  
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true

server:
  port: 8080
  servlet:
    context-path: /
  
  # SSL配置
  ssl:
    enabled: true
    key-store: /etc/ssl/ai-ready/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD}

# Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.aiedge: DEBUG
  file:
    name: /var/log/ai-ready/app.log
```

### 5.2 端口规划

| 服务 | 端口 | 协议 | 说明 |
|------|------|------|------|
| NGINX | 80 | HTTP | Web流量入口 |
| NGINX | 443 | HTTPS | SSL流量入口 |
| Spring Boot | 8080 | HTTP | 应用服务 |
| PostgreSQL | 5432 | TCP | 数据库服务 |
| Redis | 6379 | TCP | 缓存服务 |
| ELK | 9200 | HTTP | Elasticsearch |
| ELK | 5601 | HTTP | Kibana |
| Prometheus | 9090 | HTTP | 监控服务 |
| Grafana | 3000 | HTTP | 可视化服务 |

---

**文档完成时间**: 2026-03-27  
**作者**: devops-engineer  
**下一次评审**: 2026-04-27