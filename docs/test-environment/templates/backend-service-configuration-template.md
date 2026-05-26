# [服务名称] 后端服务配置

## 服务概述
本文档描述AI-Ready项目中[服务名称]后端服务的测试环境配置，包括应用配置、依赖配置、监控配置等。

## 1. 服务基本信息

### 1.1 服务标识
| 配置项 | 配置值 | 说明 |
|--------|--------|------|
| **服务名称** | `[服务名]` | 服务的唯一标识 |
| **服务版本** | `[版本号]` | 当前服务版本 |
| **服务类型** | `[类型]` | 微服务/单体应用/API服务 |
| **所属模块** | `[模块名]` | 所属业务模块 |
| **负责人** | `[姓名]` | 服务负责人 |
| **创建时间** | `[时间]` | 服务创建时间 |
| **最后更新** | `[时间]` | 最后更新时间 |

### 1.2 服务部署信息
| 配置项 | 配置值 | 说明 |
|--------|--------|------|
| **部署环境** | `[环境]` | 测试/开发/生产 |
| **部署方式** | `[方式]` | Docker/K8s/物理机 |
| **实例数量** | `[数量]` | 服务实例数量 |
| **部署位置** | `[位置]` | 服务器/IP地址 |
| **健康检查** | `[URL]` | 健康检查端点 |
| **就绪检查** | `[URL]` | 就绪检查端点 |

## 2. 应用配置

### 2.1 基础配置
```yaml
# application-test.yml
spring:
  application:
    name: ${服务名称}
  profiles:
    active: test
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:ai_ready_test}
    username: ${DB_USERNAME:test_user}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: ${DB_MAX_POOL_SIZE:10}
      minimum-idle: ${DB_MIN_IDLE:5}
      connection-timeout: ${DB_CONN_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}
    show-sql: ${JPA_SHOW_SQL:true}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: ${HIBERNATE_FORMAT_SQL:true}
  
  # Redis配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DB:0}
    timeout: ${REDIS_TIMEOUT:5000ms}
    lettuce:
      pool:
        max-active: ${REDIS_MAX_ACTIVE:8}
        max-idle: ${REDIS_MAX_IDLE:8}
        min-idle: ${REDIS_MIN_IDLE:0}
```

### 2.2 服务配置
```yaml
# 服务端口配置
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: ${CONTEXT_PATH:/}
    session:
      timeout: ${SESSION_TIMEOUT:30m}
  compression:
    enabled: ${COMPRESSION_ENABLED:true}
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: ${COMPRESSION_MIN_SIZE:2048}
  
# HTTP配置
  http2:
    enabled: ${HTTP2_ENABLED:false}
  tomcat:
    max-threads: ${TOMCAT_MAX_THREADS:200}
    min-spare-threads: ${TOMCAT_MIN_THREADS:10}
    accept-count: ${TOMCAT_ACCEPT_COUNT:100}
    connection-timeout: ${TOMCAT_CONN_TIMEOUT:20000}
    keep-alive-timeout: ${TOMCAT_KEEPALIVE_TIMEOUT:30000}
```

### 2.3 缓存配置
```yaml
# 缓存配置
spring:
  cache:
    type: ${CACHE_TYPE:redis}
    cache-names: ${CACHE_NAMES:default}
    redis:
      time-to-live: ${CACHE_TTL:600000}
      cache-null-values: ${CACHE_NULL_VALUES:true}
      key-prefix: ${CACHE_KEY_PREFIX:cache:}
      use-key-prefix: ${CACHE_USE_PREFIX:true}
  
# 本地缓存配置
  caffeine:
    spec: maximumSize=${CAFFEINE_MAX_SIZE:1000},expireAfterWrite=${CAFFEINE_EXPIRE:10m}
```

## 3. 依赖服务配置

### 3.1 数据库依赖
| 依赖服务 | 地址 | 端口 | 数据库 | 用户名 | 连接池 | 超时设置 |
|----------|------|------|--------|--------|--------|----------|
| **主数据库** | `${DB_HOST}` | `${DB_PORT}` | `${DB_NAME}` | `${DB_USERNAME}` | `HikariCP` | `30s` |
| **从数据库** | `${DB_SLAVE_HOST}` | `${DB_SLAVE_PORT}` | `${DB_SLAVE_NAME}` | `${DB_SLAVE_USERNAME}` | `HikariCP` | `30s` |
| **Redis缓存** | `${REDIS_HOST}` | `${REDIS_PORT}` | `${REDIS_DB}` | `-` | `Lettuce` | `5s` |

### 3.2 外部服务依赖
| 服务名称 | 服务地址 | 协议 | 超时 | 重试策略 | 熔断配置 | 降级策略 |
|----------|----------|------|------|----------|----------|----------|
| `[服务1]` | `[地址]` | `[协议]` | `[超时]` | `[重试]` | `[熔断]` | `[降级]` |
| `[服务2]` | `[地址]` | `[协议]` | `[超时]` | `[重试]` | `[熔断]` | `[降级]` |

### 3.3 消息队列配置
```yaml
# RabbitMQ配置
spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USERNAME:guest}
    password: ${RABBIT_PASSWORD:guest}
    virtual-host: ${RABBIT_VHOST:/}
    connection-timeout: ${RABBIT_CONN_TIMEOUT:30000}
    
    # 生产者配置
    template:
      retry:
        enabled: ${RABBIT_RETRY_ENABLED:true}
        max-attempts: ${RABBIT_RETRY_ATTEMPTS:3}
        initial-interval: ${RABBIT_RETRY_INTERVAL:1000}
        max-interval: ${RABBIT_RETRY_MAX_INTERVAL:10000}
        multiplier: ${RABBIT_RETRY_MULTIPLIER:2}
    
    # 消费者配置
    listener:
      simple:
        concurrency: ${RABBIT_CONCURRENCY:1}
        max-concurrency: ${RABBIT_MAX_CONCURRENCY:10}
        prefetch: ${RABBIT_PREFETCH:10}
        auto-startup: ${RABBIT_AUTO_START:true}
        default-requeue-rejected: ${RABBIT_REQUEUE:false}
```

## 4. 安全配置

### 4.1 认证配置
```yaml
# Spring Security配置
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${ISSUER_URI:http://localhost:8080/auth/realms/ai-ready}
          jwk-set-uri: ${JWK_SET_URI:http://localhost:8080/auth/realms/ai-ready/protocol/openid-connect/certs}
    
# JWT配置
  jwt:
    secret: ${JWT_SECRET:test-secret-key}
    expiration: ${JWT_EXPIRATION:3600000}
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:86400000}
```

### 4.2 权限配置
| 接口路径 | 请求方法 | 所需权限 | 角色要求 | 访问控制 | 审计日志 |
|----------|----------|----------|----------|----------|----------|
| `/api/v1/public/**` | `GET` | `无` | `无` | `公开` | `是` |
| `/api/v1/user/**` | `*` | `USER_READ` | `USER` | `认证用户` | `是` |
| `/api/v1/admin/**` | `*` | `ADMIN_ACCESS` | `ADMIN` | `管理员` | `是` |

### 4.3 加密配置
| 加密类型 | 算法 | 密钥长度 | 密钥存储 | 密钥轮换 | 证书路径 |
|----------|------|----------|----------|----------|----------|
| **JWT签名** | `HS256` | `256位` | `环境变量` | `每月` | `-` |
| **数据加密** | `AES-GCM` | `256位` | `密钥管理服务` | `每季度` | `-` |
| **TLS证书** | `RSA` | `2048位` | `证书文件` | `每年` | `/etc/ssl/certs/` |

## 5. 监控配置

### 5.1 监控端点
```yaml
# Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,env
      base-path: /actuator
      path-mapping:
        health: healthcheck
    
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
    
  # 健康检查配置
  health:
    db:
      enabled: true
    redis:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB
    mail:
      enabled: false
```

### 5.2 自定义指标
| 指标名称 | 指标类型 | 标签 | 描述 | 告警阈值 |
|----------|----------|------|------|----------|
| `http_requests_total` | Counter | `method,uri,status` | HTTP请求总数 | `-` |
| `http_request_duration_seconds` | Histogram | `method,uri` | HTTP请求耗时 | `>2s` |
| `jvm_memory_used_bytes` | Gauge | `area` | JVM内存使用量 | `>80%` |
| `database_query_duration_seconds` | Histogram | `query_type` | 数据库查询耗时 | `>1s` |
| `cache_hit_ratio` | Gauge | `cache_name` | 缓存命中率 | `<80%` |

### 5.3 日志配置
```yaml
# 日志配置
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    cn.aiedge: ${LOG_LEVEL_APP:DEBUG}
    org.springframework: ${LOG_LEVEL_SPRING:INFO}
    org.hibernate: ${LOG_LEVEL_HIBERNATE:WARN}
    org.apache.kafka: ${LOG_LEVEL_KAFKA:WARN}
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %-5level [%thread] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: ${LOG_FILE_PATH:logs/${spring.application.name}.log}
    max-size: ${LOG_FILE_MAX_SIZE:10MB}
    max-history: ${LOG_FILE_MAX_HISTORY:30}
    total-size-cap: ${LOG_FILE_TOTAL_SIZE:1GB}
  
  logback:
    rollingpolicy:
      max-file-size: ${LOG_ROLLING_SIZE:10MB}
      clean-history-on-start: ${LOG_CLEAN_ON_START:false}
```

## 6. 性能配置

### 6.1 线程池配置
| 线程池类型 | 核心线程数 | 最大线程数 | 队列容量 | 拒绝策略 | 线程名称 |
|------------|------------|------------|----------|----------|----------|
| **业务线程池** | `10` | `50` | `100` | `CallerRunsPolicy` | `business-thread` |
| **IO线程池** | `5` | `20` | `50` | `AbortPolicy` | `io-thread` |
| **定时任务池** | `3` | `10` | `20` | `DiscardOldestPolicy` | `scheduled-thread` |

### 6.2 连接池配置
| 连接池类型 | 最大连接数 | 最小空闲 | 最大空闲 | 获取超时 | 验证查询 |
|------------|------------|----------|----------|----------|----------|
| **数据库连接池** | `20` | `5` | `10` | `30s` | `SELECT 1` |
| **Redis连接池** | `8` | `2` | `8` | `5s` | `PING` |
| **HTTP连接池** | `20` | `5` | `10` | `10s` | `-` |

### 6.3 缓存配置
| 缓存名称 | 缓存类型 | 最大大小 | 过期时间 | 刷新策略 | 统计启用 |
|----------|----------|----------|----------|----------|----------|
| `user_cache` | `Redis` | `10000` | `30m` | `写后刷新` | `是` |
| `config_cache` | `Caffeine` | `1000` | `10m` | `定时刷新` | `是` |
| `session_cache` | `Redis` | `5000` | `2h` | `访问刷新` | `否` |

## 7. 部署配置

### 7.1 Docker配置
```dockerfile
# Dockerfile
FROM openjdk:11-jre-slim
LABEL maintainer="ai-ready-team@example.com"

# 设置工作目录
WORKDIR /app

# 复制应用JAR文件
COPY target/*.jar app.jar

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 7.2 Kubernetes配置
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${服务名称}
  namespace: test
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ${服务名称}
  template:
    metadata:
      labels:
        app: ${服务名称}
    spec:
      containers:
      - name: ${服务名称}
        image: ${镜像地址}:${版本}
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "test"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: host
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

## 8. 环境变量配置

### 8.1 必需环境变量
| 变量名 | 默认值 | 必需 | 描述 | 示例 |
|--------|--------|------|------|------|
| `DB_HOST` | `localhost` | 是 | 数据库主机 | `mysql-test` |
| `DB_PORT` | `3306` | 是 | 数据库端口 | `3306` |
| `DB_NAME` | `ai_ready_test` | 是 | 数据库名称 | `ai_ready_test` |
| `DB_USERNAME` | `test_user` | 是 | 数据库用户名 | `test_user` |
| `DB_PASSWORD` | - | 是 | 数据库密码 | `password123` |
| `REDIS_HOST` | `localhost` | 是 | Redis主机 | `redis-test` |
| `REDIS_PORT` | `6379` | 是 | Redis端口 | `6379` |
| `SERVER_PORT` | `8080` | 是 | 服务端口 | `8080` |

### 8.2 可选环境变量
| 变量名 | 默认值 | 描述 | 示例 |
|--------|--------|------|------|
| `LOG_LEVEL` | `INFO` | 日志级别 | `DEBUG` |
| `CACHE_ENABLED` | `true` | 缓存启用 | `true` |
| `METRICS_ENABLED` | `true` | 监控指标启用 | `true` |
| `TRACING_ENABLED` | `false` | 链路追踪启用 | `true` |

## 9. 故障排查指南

### 9.1 常见问题
| 问题现象 | 可能原因 | 排查步骤 | 解决方案 |
|----------|----------|----------|----------|
| **服务启动失败** | 端口占用/配置错误 | 1. 检查端口占用<br>2. 检查配置文件<br>3. 查看启动日志 | 1. 更换端口<br>2. 修正配置<br>3. 重启服务 |
| **数据库连接失败** | 网络问题/认证失败 | 1. 测试网络连通性<br>2. 验证数据库凭证<br>3. 检查防火墙规则 | 1. 修复网络<br>2. 更新凭证<br>3. 调整防火墙 |
| **内存溢出** | 内存泄漏/配置不当 | 1. 分析堆转储<br>2. 检查GC日志<br>3. 调整JVM参数 | 1. 修复内存泄漏<br>2. 优化代码<br>3. 增加内存 |

### 9.2 日志分析
| 日志级别 | 关键词 | 含义 | 处理建议 |
|----------|--------|------|----------|
| `ERROR` | `Connection refused` | 连接拒绝 | 检查服务状态和网络 |
| `WARN` | `Slow query detected` | 慢查询 | 优化SQL语句或索引 |
| `INFO` | `Service started` | 服务启动成功 | 正常启动 |
| `DEBUG` | `Cache miss` | 缓存未命中 | 检查缓存策略 |

## 10. 配置验证清单

### 10.1 启动前检查
- [ ] 环境变量配置完整
- [ ] 依赖服务可访问
- [ ] 配置文件语法正确
- [ ] 端口未被占用
- [ ] 磁盘空间充足

### 10.2 启动后验证
- [ ] 服务健康检查通过
- [ ] 数据库连接正常
- [ ] 缓存连接正常
- [ ] API接口可访问
- [ ] 监控指标正常

### 10.3 性能验证
- [ ] 响应时间 < 500ms
- [ ] 内存使用 < 80%
- [ ] CPU使用 < 70%
- [ ] 数据库连接池正常
- [ ] 缓存命中率 > 80%

---

**文档状态**: `[草稿/审核中/已发布]`
**最后更新**: `YYYY-MM-DD HH:mm:ss`
**更新人**: `[姓名]`
**下次评审**: `YYYY-MM-DD`