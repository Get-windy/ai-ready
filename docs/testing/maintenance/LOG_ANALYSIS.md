# 日志收集和分析指南

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [日志架构设计](#日志架构设计)
3. [日志格式规范](#日志格式规范)
4. [日志收集配置](#日志收集配置)
5. [日志存储方案](#日志存储方案)
6. [日志分析工具](#日志分析工具)
7. [日志查询技巧](#日志查询技巧)
8. [日志告警配置](#日志告警配置)
9. [日志安全合规](#日志安全合规)
10. [日志最佳实践](#日志最佳实践)

---

## 概述

### 日志管理目标

测试环境日志管理系统旨在实现：

1. **集中收集**: 统一收集所有服务的日志
2. **标准化存储**: 按照标准格式存储日志
3. **快速检索**: 支持实时和历史的日志查询
4. **智能分析**: 自动分析日志模式和异常
5. **可视化展示**: 通过图表展示日志趋势
6. **告警通知**: 基于日志事件自动告警
7. **安全审计**: 满足安全和合规要求

### 日志分类

| 日志类别 | 日志级别 | 保留时间 | 用途 |
|----------|----------|----------|------|
| **应用日志** | INFO/ERROR | 30天 | 业务逻辑追踪、错误排查 |
| **访问日志** | INFO | 90天 | 用户行为分析、安全审计 |
| **审计日志** | INFO | 1年 | 合规审计、操作追溯 |
| **系统日志** | WARN/ERROR | 30天 | 系统监控、故障排查 |
| **性能日志** | INFO | 7天 | 性能分析、优化参考 |
| **调试日志** | DEBUG | 1天 | 开发调试、问题复现 |

### 日志级别定义

| 级别 | 描述 | 使用场景 |
|------|------|----------|
| **ERROR** | 错误 | 系统错误、业务异常、需要立即处理的问题 |
| **WARN** | 警告 | 潜在问题、性能警告、配置问题 |
| **INFO** | 信息 | 业务操作、系统状态、重要事件 |
| **DEBUG** | 调试 | 详细调试信息、变量值、流程追踪 |
| **TRACE** | 追踪 | 最详细的日志、方法调用栈、数据流转 |

## 日志架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                   日志源 (Log Sources)                       │
├──────────────┬──────────────┬──────────────┬───────────────┤
│ 应用服务     │ 中间件       │ 数据库       │ 基础设施      │
│ (Spring Boot)│ (Nginx)      │ (PostgreSQL) │ (Docker)      │
└──────────────┴──────────────┴──────────────┴───────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   日志收集层 (Log Collection)                │
├─────────────────────────────────────────────────────────────┤
│                   Filebeat/Fluentd                          │
│  • 实时收集日志                                             │
│  • 日志解析和增强                                           │
│  • 日志过滤和路由                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   日志缓冲层 (Log Buffer)                    │
├─────────────────────────────────────────────────────────────┤
│                   Kafka/RabbitMQ                            │
│  • 解耦收集和处理                                           │
│  • 流量削峰                                                 │
│  • 保证日志不丢失                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   日志处理层 (Log Processing)                │
├─────────────────────────────────────────────────────────────┤
│                   Logstash/Flink                            │
│  • 日志解析和清洗                                           │
│  • 字段提取和转换                                           │
│  • 日志丰富和聚合                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│  日志存储层 (Storage)    │   │  日志分析层 (Analysis)  │
├─────────────────────────┤   ├─────────────────────────┤
│ Elasticsearch           │   │ Kibana                  │
│ • 索引和存储            │   │ • 日志查询              │
│ • 全文搜索              │   │ • 可视化分析            │
│ • 数据聚合              │   │ • 报表生成              │
└─────────────────────────┘   └─────────────────────────┘
                │                           │
                ▼                           ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│  日志归档层 (Archive)    │   │  日志告警层 (Alerting)  │
├─────────────────────────┤   ├─────────────────────────┤
│ S3/MinIO                │   │ ElastAlert              │
│ • 长期存储              │   │ • 异常检测              │
│ • 成本优化              │   │ • 告警通知              │
│ • 合规要求              │   │ • 事件响应              │
└─────────────────────────┘   └─────────────────────────┘
```

### 组件说明

#### 1. Filebeat
- **版本**: 8.11.0
- **角色**: 轻量级日志收集器
- **特点**: 低资源消耗、支持多种输入输出
- **配置**: YAML格式，支持动态重载

#### 2. Kafka
- **版本**: 3.5.0
- **角色**: 消息队列，作为日志缓冲
- **特点**: 高吞吐、持久化、分区复制
- **配置**: 3节点集群，复制因子2

#### 3. Logstash
- **版本**: 8.11.0
- **角色**: 日志处理管道
- **特点**: 强大的过滤和转换能力
- **配置**: 多阶段管道处理

#### 4. Elasticsearch
- **版本**: 8.11.0
- **角色**: 日志存储和搜索
- **特点**: 分布式、实时搜索、RESTful API
- **配置**: 3节点集群，分片数5，副本数1

#### 5. Kibana
- **版本**: 8.11.0
- **角色**: 日志可视化和分析
- **特点**: 丰富的可视化组件、仪表盘
- **配置**: 与Elasticsearch集成

#### 6. ElastAlert
- **版本**: 0.2.4
- **角色**: 日志告警引擎
- **特点**: 基于规则的告警、多种告警方式
- **配置**: YAML规则文件

### 部署架构

#### Docker Compose配置
```yaml
version: '3.8'
services:
  # 日志收集
  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    container_name: test-env-filebeat
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - ./logs:/var/log/app:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      - kafka
    restart: unless-stopped

  # 消息队列
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: test-env-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    restart: unless-stopped

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: test-env-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    restart: unless-stopped

  # 日志处理
  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    container_name: test-env-logstash
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline:ro
      - ./logstash/config:/usr/share/logstash/config:ro
    ports:
      - "5000:5000"
    environment:
      LS_JAVA_OPTS: "-Xmx1g -Xms1g"
    depends_on:
      - kafka
      - elasticsearch
    restart: unless-stopped

  # 日志存储
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: test-env-elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
      - xpack.security.enabled=false
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    restart: unless-stopped

  # 日志可视化
  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: test-env-kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    restart: unless-stopped

  # 日志告警
  elastalert:
    image: jertel/elastalert2:2.12.0
    container_name: test-env-elastalert
    volumes:
      - ./elastalert/config:/opt/elastalert/config:ro
      - ./elastalert/rules:/opt/elastalert/rules:ro
    depends_on:
      - elasticsearch
    restart: unless-stopped

volumes:
  elasticsearch_data:
```

## 日志格式规范

### 通用日志格式

#### JSON格式（推荐）
```json
{
  "@timestamp": "2026-04-27T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.aiready.api.UserController",
  "thread": "http-nio-8080-exec-1",
  "message": "用户登录成功",
  "service": "user-service",
  "environment": "test",
  "trace_id": "abc123def456",
  "span_id": "xyz789",
  "user_id": "user_001",
  "session_id": "session_abc123",
  "request_id": "req_123456",
  "http_method": "POST",
  "http_path": "/api/v1/auth/login",
  "http_status": 200,
  "response_time_ms": 150,
  "client_ip": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "metadata": {
    "business_type": "user_auth",
    "operation": "login",
    "success": true
  }
}
```

#### 文本格式（兼容性）
```text
2026-04-27 10:30:45.123 INFO  [http-nio-8080-exec-1] c.a.a.UserController - 用户登录成功 
[service=user-service, environment=test, trace_id=abc123def456, user_id=user_001, 
http_method=POST, http_path=/api/v1/auth/login, http_status=200, response_time_ms=150]
```

### 字段定义规范

#### 必需字段
| 字段名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| `@timestamp` | ISO8601 | 日志时间戳 | "2026-04-27T10:30:45.123Z" |
| `level` | String | 日志级别 | "INFO", "ERROR", "WARN" |
| `message` | String | 日志消息 | "用户登录成功" |
| `service` | String | 服务名称 | "user-service" |
| `environment` | String | 环境标识 | "test", "staging", "prod" |

#### 上下文字段
| 字段名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| `trace_id` | String | 请求追踪ID | "abc123def456" |
| `span_id` | String | 调用链跨度ID | "xyz789" |
| `request_id` | String | 请求唯一ID | "req_123456" |
| `session_id` | String | 会话ID | "session_abc123" |
| `user_id` | String | 用户ID | "user_001" |
| `client_ip` | String | 客户端IP | "192.168.1.100" |
| `user_agent` | String | 用户代理 | "Mozilla/5.0..." |

#### 业务字段
| 字段名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| `business_type` | String | 业务类型 | "order", "payment", "inventory" |
| `operation` | String | 操作类型 | "create", "update", "delete" |
| `resource_id` | String | 资源ID | "order_123", "product_456" |
| `amount` | Number | 金额/数量 | 100.50, 5 |
| `currency` | String | 货币类型 | "CNY", "USD" |

#### 性能字段
| 字段名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| `response_time_ms` | Number | 响应时间(毫秒) | 150 |
| `db_query_time_ms` | Number | 数据库查询时间 | 50 |
| `cache_hit` | Boolean | 缓存是否命中 | true |
| `memory_used_mb` | Number | 内存使用(MB) | 512 |
| `cpu_usage_percent` | Number | CPU使用率 | 45.5 |

### 日志级别使用规范

#### ERROR级别
```json
{
  "level": "ERROR",
  "message": "数据库连接失败",
  "error_code": "DB_CONNECTION_FAILED",
  "error_message": "Connection refused to database server",
  "stack_trace": "java.sql.SQLException: Connection refused...",
  "retry_count": 3,
  "max_retries": 5
}
```

#### WARN级别
```json
{
  "level": "WARN", 
  "message": "缓存命中率下降",
  "cache_hit_rate": 75.5,
  "threshold": 80.0,
  "suggestion": "检查缓存策略和热点数据"
}
```

#### INFO级别
```json
{
  "level": "INFO",
  "message": "订单创建成功",
  "order_id": "order_123456",
  "amount": 199.99,
  "currency": "CNY",
  "user_id": "user_001",
  "payment_method": "alipay"
}
```

#### DEBUG级别
```json
{
  "level": "DEBUG",
  "message": "处理用户请求",
  "request_body": "{\"username\":\"test\",\"password\":\"***\"}",
  "parsed_data": {"username": "test"},
  "validation_result": {"valid": true, "errors": []}
}
```

### 日志模板配置

#### Logback配置（Spring Boot）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- 定义属性 -->
    <property name="LOG_PATH" value="/var/log/app" />
    <property name="APP_NAME" value="ai-ready-api" />
    
    <!-- JSON日志格式 -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${APP_NAME}","environment":"test"}</customFields>
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeCallerData>true</includeCallerData>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <level>level</level>
                <thread>thread</thread>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
    </appender>
    
    <!-- 控制台输出（开发环境） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 日志级别配置 -->
    <root level="INFO">
        <appender-ref ref="JSON_FILE" />
        <appender-ref ref="CONSOLE" />
    </root>
    
    <logger name="com.aiready" level="DEBUG" />
    <logger name="org.springframework" level="WARN" />
    <logger name="org.hibernate" level="WARN" />
    
</configuration>
```

#### Log4j2配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    
    <Properties>
        <Property name="LOG_PATH">/var/log/app</Property>
        <Property name="APP_NAME">ai-ready-api</Property>
        <Property name="LOG_PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %c{1.} - %msg%n</Property>
    </Properties>
    
    <Appenders>
        <!-- JSON文件输出 -->
        <File name="JsonFile" fileName="${LOG_PATH}/${APP_NAME}.json" append="true">
            <JsonLayout complete="true" compact="false" eventEol="true">
                <KeyValuePair key="service" value="${APP_NAME}" />
                <KeyValuePair key="environment" value="test" />
            </JsonLayout>
        </File>
        
        <!-- 控制台输出 -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN}" />
        </Console>
        
        <!-- 错误日志单独文件 -->
        <RollingFile name="ErrorFile" fileName="${LOG_PATH}/error.log"
                     filePattern="${LOG_PATH}/error.%d{yyyy-MM-dd}.log">
            <PatternLayout pattern="${LOG_PATTERN}" />
            <ThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" />
            </Policies>
            <DefaultRolloverStrategy max="30" />
        </RollingFile>
    </Appenders>
    
    <Loggers>
        <Root level="info">
            <AppenderRef ref="JsonFile" />
            <AppenderRef ref="Console" />
            <AppenderRef ref="ErrorFile" />
        </Root>
        
        <!-- 特定包日志级别 -->
        <Logger name="com.aiready" level="debug" additivity="false">
            <AppenderRef ref="JsonFile" />
            <AppenderRef ref="Console" />
        </Logger>
    </Loggers>
    
</Configuration>
```

## 日志收集配置

### Filebeat配置

#### 主配置文件
```yaml
# filebeat.yml
filebeat.inputs:
  # 应用日志输入
  - type: filestream
    id: app-logs
    enabled: true
    paths:
      - /var/log/app/*.json
      - /var/log/app/*.log
    parsers:
      - ndjson:
          target: ""
          overwrite_keys: true
          add_error_key: true
    fields:
      log_type: "application"
      environment: "test"
    fields_under_root: true
    
  # 容器日志输入
  - type: container
    id: container-logs
    enabled: true
    paths:
      - /var/lib/docker/containers/*/*.log
    stream: all
    processors:
      - add_docker_metadata: ~
    fields:
      log_type: "container"
      environment: "test"
    fields_under_root: true
    
  # 系统日志输入
  - type: log
    id: system-logs
    enabled: true
    paths:
      - /var/log/syslog
      - /var/log/auth.log
    fields:
      log_type: "system"
      environment: "test"
    fields_under_root: true

# 处理器配置
processors:
  - add_host_metadata:
      netinfo.enabled: true
  - add_cloud_metadata: ~
  - add_kubernetes_metadata: ~
  - decode_json_fields:
      fields: ["message"]
      target: ""
      overwrite_keys: true
  - drop_fields:
      fields: ["agent.ephemeral_id", "agent.id", "agent.name"]
      
# 输出到Kafka
output.kafka:
  enabled: true
  hosts: ["kafka:9092"]
  topic: "logs-%{[log_type]}"
  partition.hash:
    reachable_only: true
  required_acks: 1
  compression: gzip
  max_message_bytes: 1000000
  ssl.enabled: false
  
# 监控配置
monitoring:
  enabled: true
  elasticsearch:
    hosts: ["elasticsearch:9200"]
    
# 日志配置
logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644
```

#### 处理器配置
```yaml
# processors.yml
processors:
  # 1. 时间戳处理
  - timestamp:
      field: "timestamp"
      layouts:
        - "2006-01-02 15:04:05.000"
        - "RFC3339"
      test:
        - "2026-01-02T15:04:05.000Z"
      ignore_failure: true
      ignore_missing: true
      
  # 2. 日志解析
  - dissect:
      tokenizer: "%{timestamp} %{level} [%{thread}] %{logger} - %{message}"
      field: "message"
      target_prefix: ""
      ignore_failure: true
      
  # 3. Grok模式匹配
  - grok:
      field: "message"
      patterns:
        - '%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \[%{DATA:thread}\] %{DATA:logger} - %{GREEDYDATA:message}'
        - 'User ID: %{WORD:user_id}, Action: %{WORD:action}, Result: %{WORD:result}'
      ignore_missing: true
      
  # 4. 字段重命名
  - rename:
      fields:
        - from: "msg"
          to: "message"
        - from: "lvl"
          to: "level"
        - from: "ts"
          to: "@timestamp"
      ignore_missing: true
      
  # 5. 字段添加
  - add_fields:
      target: ""
      fields:
        environment: "test"
        log_source: "filebeat"
        collected_at: "{{.timestamp}}"
        
  # 6. 条件处理
  - if:
      equals:
        log_type: "error"
    then:
      - add_fields:
          target: "metadata"
          fields:
            requires_attention: true
            severity: "high"
    else:
      - add_fields:
          target: "metadata"
          fields:
            requires_attention: false
            severity: "normal"
```

### Logstash配置

#### 管道配置
```ruby
# pipelines.yml
- pipeline.id: log-processing
  path.config: "/usr/share/logstash/pipeline/log-processing.conf"
  pipeline.workers: 2
  pipeline.batch.size: 125
  
- pipeline.id: error-processing  
  path.config: "/usr/share/logstash/pipeline/error-processing.conf"
  pipeline.workers: 1
  pipeline.batch.size: 50
```

#### 主处理管道
```ruby
# log-processing.conf
input {
  kafka {
    bootstrap_servers => "kafka:9092"
    topics => ["logs-application", "logs-container", "logs-system"]
    group_id => "logstash"
    auto_offset_reset => "latest"
    codec => json
    consumer_threads => 3
    decorate_events => true
  }
}

filter {
  # 1. 时间戳处理
  date {
    match => [ "timestamp", "ISO8601", "yyyy-MM-dd HH:mm:ss.SSS" ]
    target => "@timestamp"
    remove_field => ["timestamp"]
  }
  
  # 2. GeoIP处理
  if [client_ip] {
    geoip {
      source => "client_ip"
      target => "geoip"
      database => "/usr/share/logstash/GeoLite2-City.mmdb"
    }
  }
  
  # 3. User Agent解析
  if [user_agent] {
    useragent {
      source => "user_agent"
      target => "user_agent"
    }
  }
  
  # 4. 字段清理
  mutate {
    # 移除无用字段
    remove_field => [
      "[@metadata][kafka]", "[@metadata][version]",
      "host", "version", "ecs"
    ]
    
    # 字段类型转换
    convert => {
      "response_time_ms" => "integer"
      "http_status" => "integer"
      "amount" => "float"
    }
    
    # 字段重命名
    rename => {
      "log.level" => "level"
      "log.logger" => "logger"
      "log.origin.file.name" => "source_file"
    }
  }
  
  # 5. 条件路由
  if [level] == "ERROR" {
    mutate {
      add_tag => ["error", "requires_attention"]
    }
  }
  
  if "_grokparsefailure" in [tags] {
    mutate {
      add_tag => ["parse_failure"]
    }
  }
}

output {
  # 主输出到Elasticsearch
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{[environment]}-%{+YYYY.MM.dd}"
    document_type => "_doc"
    template => "/usr/share/logstash/template/logs-template.json"
    template_name => "logs"
    template_overwrite => true
    
    # 重试策略
    retry_initial_interval => 1
    retry_max_interval => 60
    retry_on_conflict => 3
  }
  
  # 错误日志单独输出
  if "error" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "error-logs-%{[environment]}-%{+YYYY.MM.dd}"
      document_type => "_doc"
    }
  }
  
  # 调试输出到控制台
  if [environment] == "dev" {
    stdout {
      codec => rubydebug
    }
  }
}
```

#### 错误处理管道
```ruby
# error-processing.conf
input {
  kafka {
    bootstrap_servers => "kafka:9092"
    topics => ["logs-error"]
    group_id => "logstash-error"
    auto_offset_reset => "earliest"
    codec => json
    consumer_threads => 1
  }
}

filter {
  # 错误日志增强
  grok {
    match => { "message" => "%{GREEDYDATA:error_message}(?:\n%{GREEDYDATA:stack_trace})?" }
  }
  
  # 提取错误类型
  if [stack_trace] {
    grok {
      match => { "stack_trace" => "^(%{JAVACLASS:exception_class}): %{GREEDYDATA:exception_message}" }
    }
  }
  
  # 错误分类
  if [exception_class] {
    ruby {
      code => '
        exception_class = event.get("exception_class")
        error_category = "unknown"
        
        if exception_class.include?("SQL") || exception_class.include?("Database")
          error_category = "database"
        elsif exception_class.include?("Network") || exception_class.include?("Connection")
          error_category = "network"
        elsif exception_class.include?("Timeout") || exception_class.include?("TimeoutException")
          error_category = "timeout"
        elsif exception_class.include?("NullPointer") || exception_class.include?("IllegalArgument")
          error_category = "code_error"
        elsif exception_class.include?("OutOfMemory") || exception_class.include?("Memory")
          error_category = "resource"
        end
        
        event.set("error_category", error_category)
      '
    }
  }
  
  # 添加错误上下文
  mutate {
    add_field => {
      "error_handled" => false
      "error_resolution" => "pending"
      "error_priority" => "medium"
    }
  }
  
  # 设置错误优先级
  if [level] == "ERROR" and [service] == "payment-service" {
    mutate {
      replace => { "error_priority" => "high" }
      add_tag => ["critical_business_error"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "enhanced-error-logs-%{[environment]}-%{+YYYY.MM.dd}"
    document_type => "_doc"
  }
  
  # 发送到告警系统
  http {
    url => "http://alert-manager:9093/api/v1/alerts"
    http_method => "post"
    format => "json"
    mapping => {
      "labels" => {
        "alertname" => "ApplicationError"
        "severity" => "%{[error_priority]}"
        "service" => "%{[service]}"
        "error_category" => "%{[error_category]}"
      }
      "annotations" => {
        "summary" => "应用错误: %{[exception_class] || message}"
        "description" => "服务: %{[service]}, 环境: %{[environment]}, 错误: %{[error_message]}"
      }
    }
    content_type => "application/json"
  }
}
```

## 日志存储方案

### Elasticsearch索引模板

#### 索引模板配置
```json
{
  "index_patterns": ["logs-*", "error-logs-*"],
  "settings": {
    "index": {
      "number_of_shards": 5,
      "number_of_replicas": 1,
      "refresh_interval": "30s",
      "codec": "best_compression",
      "lifecycle": {
        "name": "logs-lifecycle",
        "rollover_alias": "logs"
      }
    },
    "analysis": {
      "analyzer": {
        "log_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "stop", "asciifolding"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "@timestamp": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis"
      },
      "level": {
        "type": "keyword",
        "fields": {
          "text": {
            "type": "text",
            "analyzer": "log_analyzer"
          }
        }
      },
      "message": {
        "type": "text",
        "analyzer": "log_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "service": {
        "type": "keyword"
      },
      "environment": {
        "type": "keyword"
      },
      "trace_id": {
        "type": "keyword"
      },
      "user_id": {
        "type": "keyword"
      },
      "http_method": {
        "type": "keyword"
      },
      "http_path": {
        "type": "keyword",
        "fields": {
          "text": {
            "type": "text",
            "analyzer": "log_analyzer"
          }
        }
      },
      "http_status": {
        "type": "integer"
      },
      "response_time_ms": {
        "type": "integer"
      },
      "client_ip": {
        "type": "ip"
      },
      "geoip": {
        "properties": {
          "city_name": {
            "type": "keyword"
          },
          "country_name": {
            "type": "keyword"
          },
          "location": {
            "type": "geo_point"
          }
        }
      },
      "user_agent": {
        "type": "object",
        "properties": {
          "name": {
            "type": "keyword"
          },
          "os": {
            "type": "keyword"
          },
          "device": {
            "type": "keyword"
          }
        }
      },
      "metadata": {
        "type": "object",
        "dynamic": true
      },
      "tags": {
        "type": "keyword"
      }
    }
  },
  "aliases": {
    "logs": {}
  }
}
```

#### 索引生命周期管理
```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_age": "7d",
            "max_size": "50gb",
            "max_docs": 10000000
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "forcemerge": {
            "max_num_segments": 1
          },
          "shrink": {
            "number_of_shards": 2
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "set_priority": {
            "priority": 0
          },
          "searchable_snapshot": {
            "snapshot_repository": "s3-repository"
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 存储优化策略

#### 1. 数据分层存储
```yaml
storage_tiers:
  hot_tier:
    duration: 7天
    storage: SSD
    replicas