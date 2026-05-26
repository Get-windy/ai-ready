# Sprint 27+1 测试环境服务部署配置说明

## 1. 概述

### 1.1 文档目的
本文档提供Sprint 27+1测试环境服务集群的详细部署配置说明，包括服务配置、部署流程、环境变量、健康检查等。

### 1.2 适用范围
- 测试环境服务部署团队
- DevOps工程师
- 系统管理员

## 2. 部署环境要求

### 2.1 硬件要求
| 组件 | 最小配置 | 推荐配置 | 生产配置 |
|------|----------|----------|----------|
| 应用服务器 | 2核4GB | 4核8GB | 8核16GB |
| 数据库服务器 | 4核8GB | 8核16GB | 16核32GB |
| 缓存服务器 | 2核4GB | 4核8GB | 8核16GB |
| 负载均衡器 | 2核4GB | 4核8GB | 8核16GB |

### 2.2 软件要求
| 软件 | 版本 | 备注 |
|------|------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 容器编排 |
| Java | 17 | 应用运行时 |
| Node.js | 18 | 前端运行时 |
| PostgreSQL | 14 | 数据库 |
| Redis | 6.2 | 缓存 |
| Nginx | 1.21 | 负载均衡 |

## 3. 服务配置说明

### 3.1 应用服务配置

#### 3.1.1 Spring Boot应用配置
```yaml
# application-cluster.yml
spring:
  application:
    name: ai-ready-service
  profiles:
    active: test
  datasource:
    url: jdbc:postgresql://postgres-master:5432/ai_ready
    username: ${DB_USERNAME:ai_ready}
    password: ${DB_PASSWORD:secure_password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  redis:
    cluster:
      nodes:
        - redis-node1:6379
        - redis-node2:6379
        - redis-node3:6379
    password: ${REDIS_PASSWORD:redis_password}
  rabbitmq:
    host: rabbitmq-node1
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: ${RABBITMQ_PASSWORD:admin}
    
server:
  port: 8080
  servlet:
    context-path: /api
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 3.1.2 微服务注册配置
```yaml
# consul配置
spring:
  cloud:
    consul:
      host: consul-server
      port: 8500
      discovery:
        instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
        service-name: ${spring.application.name}
        health-check-path: /actuator/health
        health-check-interval: 10s
        tags:
          - version=${app.version}
          - environment=test
```

### 3.2 数据库配置

#### 3.2.1 PostgreSQL集群配置
```yaml
# docker-compose-database.yml
version: '3.8'

services:
  postgres-master:
    image: postgres:14
    container_name: postgres-master
    restart: always
    environment:
      POSTGRES_DB: ai_ready
      POSTGRES_USER: ai_ready
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_INITDB_ARGS: '--encoding=UTF-8 --locale=C'
    ports:
      - "5432:5432"
    volumes:
      - postgres-master-data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - database-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_ready"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-replica:
    image: postgres:14
    container_name: postgres-replica
    restart: always
    environment:
      POSTGRES_DB: ai_ready
      POSTGRES_USER: ai_ready
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5433:5432"
    volumes:
      - postgres-replica-data:/var/lib/postgresql/data
    networks:
      - database-network
    depends_on:
      - postgres-master
    command: >
      bash -c "
        echo 'Waiting for master to be ready...'
        until pg_isready -h postgres-master -p 5432 -U ai_ready; do
          sleep 1
        done
        pg_basebackup -h postgres-master -U ai_ready -D /var/lib/postgresql/data -P -R
        postgres
      "
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_ready"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-master-data:
  postgres-replica-data:

networks:
  database-network:
    driver: bridge
```

#### 3.2.2 数据库初始化脚本
```sql
-- init-scripts/01-init-database.sql
CREATE DATABASE ai_ready;
\c ai_ready;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 创建表空间（可选）
CREATE TABLESPACE ai_ready_ts LOCATION '/var/lib/postgresql/data/tablespaces';

-- 创建用户和权限
CREATE ROLE app_user WITH LOGIN PASSWORD 'app_password';
GRANT CONNECT ON DATABASE ai_ready TO app_user;

-- 创建监控用户
CREATE ROLE monitor_user WITH LOGIN PASSWORD 'monitor_password';
GRANT pg_monitor TO monitor_user;
```

### 3.3 缓存配置

#### 3.3.1 Redis集群配置
```yaml
# docker-compose-redis-cluster.yml
version: '3.8'

services:
  redis-node1:
    image: redis:6.2-alpine
    container_name: redis-node1
    command: redis-server --port 6379 --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    volumes:
      - redis-node1-data:/data
    networks:
      - redis-network

  redis-node2:
    image: redis:6.2-alpine
    container_name: redis-node2
    command: redis-server --port 6379 --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes --requirepass ${REDIS_PASSWORD}
    ports:
      - "6380:6379"
    volumes:
      - redis-node2-data:/data
    networks:
      - redis-network

  redis-node3:
    image: redis:6.2-alpine
    container_name: redis-node3
    command: redis-server --port 6379 --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes --requirepass ${REDIS_PASSWORD}
    ports:
      - "6381:6379"
    volumes:
      - redis-node3-data:/data
    networks:
      - redis-network

  redis-init:
    image: redis:6.2-alpine
    container_name: redis-init
    depends_on:
      - redis-node1
      - redis-node2
      - redis-node3
    command: >
      bash -c "
        sleep 10
        redis-cli -a ${REDIS_PASSWORD} --cluster create redis-node1:6379 redis-node2:6379 redis-node3:6379 --cluster-replicas 0
      "
    networks:
      - redis-network

volumes:
  redis-node1-data:
  redis-node2-data:
  redis-node3-data:

networks:
  redis-network:
    driver: bridge
```

### 3.4 消息队列配置

#### 3.4.1 RabbitMQ集群配置
```yaml
# docker-compose-rabbitmq-cluster.yml
version: '3.8'

services:
  rabbitmq-node1:
    image: rabbitmq:3.9-management-alpine
    container_name: rabbitmq-node1
    hostname: rabbitmq-node1
    environment:
      RABBITMQ_ERLANG_COOKIE: "secret_cookie"
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
      RABBITMQ_NODENAME: rabbit@rabbitmq-node1
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-node1-data:/var/lib/rabbitmq
    networks:
      - rabbitmq-network

  rabbitmq-node2:
    image: rabbitmq:3.9-management-alpine
    container_name: rabbitmq-node2
    hostname: rabbitmq-node2
    environment:
      RABBITMQ_ERLANG_COOKIE: "secret_cookie"
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
      RABBITMQ_NODENAME: rabbit@rabbitmq-node2
    ports:
      - "5673:5672"
      - "15673:15672"
    volumes:
      - rabbitmq-node2-data:/var/lib/rabbitmq
    networks:
      - rabbitmq-network
    command: >
      bash -c "
        sleep 10
        rabbitmqctl stop_app
        rabbitmqctl reset
        rabbitmqctl join_cluster rabbit@rabbitmq-node1
        rabbitmqctl start_app
      "

volumes:
  rabbitmq-node1-data:
  rabbitmq-node2-data:

networks:
  rabbitmq-network:
    driver: bridge
```

## 4. 环境变量配置

### 4.1 环境变量文件
创建 `.env` 文件：
```bash
# 数据库配置
POSTGRES_PASSWORD=secure_password_123
DB_USERNAME=ai_ready
DB_PASSWORD=ai_ready_password

# Redis配置
REDIS_PASSWORD=redis_secure_password

# RabbitMQ配置
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=admin_secure_password

# 应用配置
APP_VERSION=1.0.0
ENVIRONMENT=test
LOG_LEVEL=INFO

# 监控配置
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=true
ALERTMANAGER_ENABLED=true
```

### 4.2 环境变量验证脚本
```bash
#!/bin/bash
# validate-env.sh

# 检查必需的环境变量
required_vars=(
  "POSTGRES_PASSWORD"
  "DB_USERNAME"
  "DB_PASSWORD"
  "REDIS_PASSWORD"
  "RABBITMQ_USER"
  "RABBITMQ_PASSWORD"
)

echo "正在验证环境变量..."

for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "❌ 错误: 环境变量 $var 未设置"
    exit 1
  else
    echo "✅ $var 已设置"
  fi
done

echo "✅ 所有必需环境变量验证通过"
```

## 5. 部署流程

### 5.1 部署前准备
```bash
# 1. 克隆代码
git clone https://github.com/ai-ready/ai-ready.git
cd ai-ready

# 2. 设置环境变量
cp .env.example .env
# 编辑 .env 文件，设置实际值

# 3. 验证环境
./scripts/validate-environment.sh
```

### 5.2 分步部署

#### 5.2.1 部署基础服务
```bash
# 1. 启动数据库集群
docker-compose -f docker-compose-database.yml up -d

# 2. 启动Redis集群
docker-compose -f docker-compose-redis-cluster.yml up -d

# 3. 启动RabbitMQ集群
docker-compose -f docker-compose-rabbitmq-cluster.yml up -d

# 4. 验证基础服务
./scripts/validate-basic-services.sh
```

#### 5.2.2 部署应用服务
```bash
# 1. 构建应用镜像
./mvnw clean package -DskipTests
docker build -t ai-ready-service:latest .

# 2. 部署应用服务
docker-compose -f docker-compose-application.yml up -d --scale app=3

# 3. 验证应用服务
./scripts/validate-application.sh
```

#### 5.2.3 部署负载均衡
```bash
# 1. 配置Nginx
cp nginx/nginx.conf.template nginx/nginx.conf
# 根据实际环境修改配置

# 2. 启动Nginx
docker-compose -f docker-compose-nginx.yml up -d

# 3. 验证负载均衡
./scripts/validate-loadbalancer.sh
```

#### 5.2.4 部署监控系统
```bash
# 1. 启动监控服务
docker-compose -f docker-compose-monitoring.yml up -d

# 2. 导入监控面板
./scripts/import-grafana-dashboards.sh

# 3. 验证监控系统
./scripts/validate-monitoring.sh
```

### 5.3 部署后验证
```bash
# 运行完整验证脚本
./scripts/full-deployment-validation.sh

# 生成验证报告
./scripts/generate-validation-report.sh
```

## 6. 健康检查配置

### 6.1 应用健康检查
```yaml
# Docker健康检查配置
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### 6.2 服务端点健康检查
```bash
#!/bin/bash
# health-check-all.sh

services=(
  "http://localhost:8080/actuator/health"
  "http://localhost:5432/health"
  "http://localhost:6379/ping"
  "http://localhost:15672/api/health/checks/alarms"
  "http://localhost:9090/-/healthy"
  "http://localhost:3000/api/health"
)

for service in "${services[@]}"; do
  echo "检查服务: $service"
  response=$(curl -s -o /dev/null -w "%{http_code}" $service)
  
  if [ "$response" = "200" ]; then
    echo "✅ 服务健康"
  else
    echo "❌ 服务异常，HTTP状态码: $response"
    exit 1
  fi
done

echo "✅ 所有服务健康检查通过"
```

## 7. 故障排查

### 7.1 常见问题

#### 7.1.1 数据库连接失败
```bash
# 检查数据库服务
docker ps | grep postgres

# 检查数据库日志
docker logs postgres-master

# 测试数据库连接
pg_isready -h localhost -p 5432 -U ai_ready
```

#### 7.1.2 Redis集群问题
```bash
# 检查Redis集群状态
redis-cli -a $REDIS_PASSWORD --cluster check localhost:6379

# 查看集群节点
redis-cli -a $REDIS_PASSWORD cluster nodes
```

#### 7.1.3 RabbitMQ集群问题
```bash
# 检查RabbitMQ集群状态
docker exec rabbitmq-node1 rabbitmqctl cluster_status

# 查看队列状态
curl -u admin:admin http://localhost:15672/api/queues
```

### 7.2 日志收集
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker logs -f ai-ready-service

# 导出日志文件
docker-compose logs --tail=1000 > deployment.log
```

## 8. 性能优化建议

### 8.1 数据库优化
```sql
-- 创建索引
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_order_created_at ON orders(created_at);

-- 分析查询性能
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

### 8.2 缓存优化
```java
// Spring Boot缓存配置
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 9. 附录

### 9.1 部署检查清单
- [ ] 环境变量配置完成
- [ ] 数据库集群正常运行
- [ ] Redis集群正常运行
- [ ] RabbitMQ集群正常运行
- [ ] 应用服务部署完成
- [ ] 负载均衡配置正确
- [ ] 监控系统正常运行
- [ ] 健康检查全部通过
- [ ] 性能测试通过
- [ ] 安全扫描通过

### 9.2 相关文档链接
- [集群架构设计文档](./CLUSTER_ARCHITECTURE_DESIGN.md)
- [数据库集群配置文档](./DATABASE_CLUSTER_CONFIGURATION.md)
- [消息队列集群配置文档](./MESSAGE_QUEUE_CLUSTER_CONFIGURATION.md)
- [运维手册](./OPERATIONS_MANUAL.md)

### 9.3 联系信息
- **运维团队**: devops@ai-ready.test
- **技术支持**: support@ai-ready.test
- **紧急联系人**: +86 13800138000

---

**文档版本**: v1.0  
**创建时间**: 2026-04-27  
**更新记录**:
- v1.0 (2026-04-27): 初始版本，创建服务部署配置说明

**负责人**: doc-writer  
**审核人**: devops-engineer  
**批准人**: main