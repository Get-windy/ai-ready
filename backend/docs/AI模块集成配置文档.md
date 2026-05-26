# AI模块集成配置文档

## 文档信息
- **项目**: AI-Ready（企智连系统）
- **模块**: AI模块
- **Sprint**: 27+1
- **环境**: 测试环境
- **创建时间**: 2026-04-27
- **负责人**: test-agent-1

## 1. 概述

本文档描述AI模块在测试环境中的集成配置方案，包括部署环境配置、服务集成、监控设置和验证流程。

## 2. AI模块架构

### 2.1 模块组成
```
AI模块系统架构：
├── AI推理服务 (ai-service)
│   ├── 文本嵌入服务 (embeddings)
│   ├── 聊天补全服务 (chat)
│   ├── 向量检索服务 (vector-search)
│   └── 健康检查服务 (health)
├── AI智能分析模块 (ai_intelligent_analysis)
│   ├── 数据预处理
│   ├── 模型推理
│   └── 结果可视化
└── AI模块管理服务 (ai-modules)
    ├── 基础数据管理
    ├── 知识库管理
    ├── 通知服务
    └── 生产管理
```

### 2.2 技术栈
- **后端框架**: FastAPI (Python), Spring Boot (Java)
- **AI框架**: TensorFlow 2.21.0, PyTorch 2.11.0
- **数据库**: PostgreSQL (业务数据), ChromaDB (向量数据)
- **缓存**: Redis
- **监控**: Prometheus, Grafana
- **容器化**: Docker, Docker Compose

## 3. 测试环境配置

### 3.1 环境变量配置 (.env.test)

```bash
# AI Service Configuration
AI_SERVICE_HOST=0.0.0.0
AI_SERVICE_PORT=8000
AI_SERVICE_ENV=test

# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=ai_ready_test
POSTGRES_USER=ai_ready_user
POSTGRES_PASSWORD=test_password_123

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Vector Database Configuration
CHROMA_PERSIST_DIRECTORY=./chroma_db_test

# Model Configuration
AI_MODEL_PATH=./models/test_models
AI_MODEL_VERSION=v1.0.0-test

# Monitoring Configuration
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090
LOGGING_LEVEL=INFO
```

### 3.2 Docker Compose配置 (docker-compose.test.yml)

```yaml
version: '3.8'

services:
  # AI推理服务
  ai-service:
    build: ./backend/ai/service/ai-service
    container_name: ai-service-test
    ports:
      - "8000:8000"
    environment:
      - AI_SERVICE_ENV=test
      - AI_SERVICE_PORT=8000
    volumes:
      - ./chroma_db_test:/app/chroma_db_test
      - ./models/test_models:/app/models
    depends_on:
      - postgres
      - redis
    networks:
      - ai-test-network

  # PostgreSQL数据库
  postgres:
    image: postgres:15-alpine
    container_name: postgres-test
    environment:
      POSTGRES_DB: ai_ready_test
      POSTGRES_USER: ai_ready_user
      POSTGRES_PASSWORD: test_password_123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data-test:/var/lib/postgresql/data
    networks:
      - ai-test-network

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: redis-test
    ports:
      - "6379:6379"
    networks:
      - ai-test-network

  # Prometheus监控
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: prometheus-test
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.test.yml:/etc/prometheus/prometheus.yml
      - prometheus-data-test:/prometheus
    networks:
      - ai-test-network

networks:
  ai-test-network:
    driver: bridge

volumes:
  postgres-data-test:
  prometheus-data-test:
```

## 4. 服务集成配置

### 4.1 Spring Boot配置 (application-test.yml)

```yaml
# Spring Boot AI模块配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_ready_test
    username: ai_ready_user
    password: test_password_123
    driver-class-name: org.postgresql.Driver
  
  redis:
    host: localhost
    port: 6379
    timeout: 5000ms

# AI服务配置
ai:
  service:
    base-url: http://localhost:8000
    timeout: 10000
    retry-count: 3
    health-check-path: /health
    embeddings-path: /embeddings
    chat-path: /chat
    
  model:
    default-model: gpt-3.5-turbo
    temperature: 0.7
    max-tokens: 1000
    embedding-model: text-embedding-ada-002

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 4.2 模型版本管理配置

```yaml
# model-version-config.yml
model_versions:
  current_version: v1.0.0-test
  supported_versions:
    - v1.0.0-test
    - v0.9.0-beta
  
  model_configs:
    v1.0.0-test:
      name: "gpt-3.5-turbo-test"
      description: "GPT-3.5 Turbo模型测试版本"
      framework: "openai"
      parameters: 1750000000
      memory_requirements: "4GB"
      gpu_required: false
      
    v0.9.0-beta:
      name: "bert-base-chinese-test"
      description: "BERT中文模型测试版本"
      framework: "transformers"
      parameters: 110000000
      memory_requirements: "2GB"
      gpu_required: false
```

## 5. 监控与日志配置

### 5.1 Prometheus配置 (prometheus.test.yml)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-service-test'
    static_configs:
      - targets: ['ai-service-test:8000']
        labels:
          environment: 'test'
          service: 'ai-service'

  - job_name: 'spring-boot-test'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          environment: 'test'
          service: 'backend-api'

alerting:
  alertmanagers:
    - static_configs:
        - targets: []
```

### 5.2 日志配置 (logback-test.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="./logs/test"/>
    <property name="LOG_FILE" value="ai-module-test"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="AI_METRICS" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/ai-metrics-test.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/ai-metrics-test-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="ai.service" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="AI_METRICS"/>
    </logger>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## 6. 验证流程

### 6.1 环境验证检查清单

```markdown
## AI模块测试环境验证检查清单

### 环境准备验证
- [ ] 数据库连接正常 (PostgreSQL)
- [ ] 缓存服务正常 (Redis)
- [ ] 网络配置正确
- [ ] 存储卷配置正确

### 服务启动验证
- [ ] AI推理服务启动正常 (端口8000)
- [ ] Spring Boot集成正常
- [ ] 健康检查接口可用
- [ ] API文档可访问

### 功能验证
- [ ] 文本嵌入功能正常
- [ ] 聊天补全功能正常
- [ ] 向量检索功能正常
- [ ] 模型版本管理正常

### 性能验证
- [ ] 单次推理响应时间 < 2秒
- [ ] 并发请求处理能力 > 10 QPS
- [ ] 内存使用正常 < 4GB
- [ ] 服务稳定性 > 99%

### 监控验证
- [ ] Prometheus数据采集正常
- [ ] 业务指标监控正常
- [ ] 日志收集正常
- [ ] 告警配置正确
```

### 6.2 API测试用例

```yaml
# api-test-cases.yml
test_cases:
  health_check:
    endpoint: GET /health
    expected_status: 200
    expected_response:
      status: "healthy"
      timestamp: "string"
      version: "v1.0.0-test"
      
  embeddings:
    endpoint: POST /embeddings
    request_body:
      texts: ["测试文本1", "测试文本2"]
      model: "text-embedding-ada-002"
    expected_status: 200
    expected_response_fields:
      - embeddings
      - model
      - usage
      
  chat_completion:
    endpoint: POST /chat
    request_body:
      messages:
        - role: "user"
          content: "你好，请介绍一下AI模块"
      model: "gpt-3.5-turbo"
    expected_status: 200
    expected_response_fields:
      - choices
      - model
      - usage
```

## 7. 故障恢复计划

### 7.1 常见问题处理

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 服务启动失败 | 端口冲突 | 检查端口占用情况，修改配置 |
| 数据库连接失败 | 连接配置错误 | 验证数据库配置和网络连接 |
| 模型加载失败 | 模型文件缺失 | 检查模型文件路径和权限 |
| 内存溢出 | 资源不足 | 增加内存限制，优化模型配置 |

### 7.2 应急恢复流程

1. **服务不可用**: 重启AI服务容器
2. **数据库故障**: 切换到备份数据库
3. **模型故障**: 回退到旧版本模型
4. **监控告警**: 检查监控指标，排查问题

## 8. 交付物清单

1. ✅ AI模块集成配置文档（本文档）
2. □ 功能验证测试报告
3. □ 性能测试分析报告
4. □ 集成测试验证报告
5. □ 部署脚本和配置文件

---
*文档版本: v1.0*
*最后更新: 2026-04-27*