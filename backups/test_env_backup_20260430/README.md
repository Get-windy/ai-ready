# Sprint 27+1 测试环境服务集群部署

## 概述

本目录包含Sprint 27+1测试环境的完整微服务集群部署配置，支持多服务并发测试和集成测试。

## 目录结构

```
test_env/
├── README.md                    # 本文档
├── requirements.txt             # Python依赖
├── test_config.yaml            # 基础测试配置
├── run_all_tests.py            # 测试执行脚本
├── config/                     # 配置目录
│   ├── service_cluster_config.yaml      # 服务集群配置
│   ├── database_cluster_config.yaml     # 数据库集群配置
│   ├── message_queue_cluster_config.yaml # 消息队列配置
│   └── integration_test_config.yaml     # 集成测试配置
├── scripts/                    # 部署脚本
│   ├── deploy_cluster.sh      # 集群部署脚本
│   ├── build-services.sh      # 服务构建脚本
│   ├── check-services-health.sh # 服务健康检查
│   ├── validate-services.sh   # 服务验证脚本
│   ├── run-integration-tests.sh # 集成测试脚本
│   └── run-performance-tests.sh # 性能测试脚本
├── data/                      # 测试数据
│   ├── synthetic/             # 合成测试数据
│   ├── fixtures/              # 测试夹具
│   └── backups/               # 数据备份
├── logs/                      # 日志文件
│   ├── application/           # 应用日志
│   ├── database/              # 数据库日志
│   └── test/                  # 测试日志
└── reports/                   # 测试报告
    ├── html/                  # HTML报告
    ├── json/                  # JSON报告
    └── junit/                 # JUnit报告
```

## 配置说明

### 1. 服务集群配置 (`config/service_cluster_config.yaml`)

配置微服务集群的各项参数：

- **服务注册与发现**: 使用Nacos作为服务注册中心
- **微服务实例**: 用户服务、订单服务、库存服务、监控服务
- **负载均衡**: 支持轮询、随机、加权、最少连接数策略
- **服务间调用**: 超时、重试、熔断器、降级策略
- **监控与指标**: Prometheus指标采集、Jaeger分布式追踪
- **安全配置**: JWT认证、RBAC授权、SSL加密

### 2. 数据库集群配置 (`config/database_cluster_config.yaml`)

配置数据库主从复制和读写分离：

- **主数据库**: PostgreSQL主节点，处理写操作
- **从数据库**: 多个读副本，处理读操作
- **读写分离**: 自动路由读写请求
- **连接池**: 连接管理和监控
- **备份恢复**: 持续备份和点恢复
- **缓存层**: Redis集群缓存

### 3. 消息队列配置 (`config/message_queue_cluster_config.yaml`)

配置Kafka消息队列集群：

- **Kafka集群**: 3节点集群，支持高可用
- **Topic配置**: 用户事件、订单事件、库存事件等
- **消费者组**: 用户服务组、订单服务组、监控服务组
- **生产者配置**: 事务性生产、消息压缩、重试策略
- **监控追踪**: 性能指标、消息追踪、告警

### 4. 集成测试配置 (`config/integration_test_config.yaml`)

配置多服务集成测试：

- **测试场景**: 用户注册、订单创建、库存预警等端到端测试
- **服务依赖**: 定义服务间的依赖关系和调用链
- **数据一致性**: 验证跨服务数据一致性
- **性能测试**: 负载测试、压力测试、耐力测试
- **测试数据**: 合成数据生成和管理

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- Java 11+
- Maven 3.6+
- Python 3.8+
- Git

### 部署步骤

1. **克隆项目** (如果尚未克隆)
   ```bash
   git clone <repository-url>
   cd AI-Ready/test_env
   ```

2. **安装依赖**
   ```bash
   pip install -r requirements.txt
   ```

3. **完整部署** (推荐)
   ```bash
   ./scripts/deploy_cluster.sh --all --validate
   ```

4. **部分部署** (按需)
   ```bash
   # 仅部署基础设施
   ./scripts/deploy_cluster.sh --infra-only
   
   # 仅部署数据库
   ./scripts/deploy_cluster.sh --database-only
   
   # 仅部署微服务
   ./scripts/deploy_cluster.sh --services-only
   ```

5. **清理环境** (重新部署前)
   ```bash
   ./scripts/deploy_cluster.sh --clean --all
   ```

### 验证部署

1. **检查服务状态**
   ```bash
   docker-compose ps
   ```

2. **验证服务健康**
   ```bash
   ./scripts/check-services-health.sh
   ```

3. **运行基础测试**
   ```bash
   ./scripts/run-basic-tests.sh
   ```

4. **访问管理界面**
   - Nacos控制台: http://localhost:8848/nacos (nacos/nacos)
   - Grafana: http://localhost:3000 (admin/admin)
   - Prometheus: http://localhost:9090
   - Jaeger: http://localhost:16686

## 测试执行

### 集成测试

```bash
# 运行所有集成测试
./scripts/run-integration-tests.sh --all

# 运行特定场景测试
./scripts/run-integration-tests.sh --scenario user-registration
./scripts/run-integration-tests.sh --scenario order-creation
./scripts/run-integration-tests.sh --scenario inventory-alert

# 生成测试报告
./scripts/run-integration-tests.sh --report html
```

### 性能测试

```bash
# 运行负载测试
./scripts/run-performance-tests.sh --load --users 100 --duration 300

# 运行压力测试
./scripts/run-performance-tests.sh --stress --users 500 --duration 600

# 运行耐力测试
./scripts/run-performance-tests.sh --endurance --duration 3600

# 生成性能报告
./scripts/run-performance-tests.sh --report grafana
```

### 数据一致性测试

```bash
# 验证订单-库存一致性
./scripts/run-consistency-tests.sh --type order-inventory

# 验证用户会话一致性
./scripts/run-consistency-tests.sh --type user-session

# 验证跨服务数据一致性
./scripts/run-consistency-tests.sh --type cross-service
```

## 监控与告警

### 监控指标

1. **服务级别指标**
   - 请求率 (QPS)
   - 响应时间 (P50, P95, P99)
   - 错误率
   - 服务可用性

2. **系统级别指标**
   - CPU使用率
   - 内存使用率
   - 磁盘IO
   - 网络带宽

3. **业务级别指标**
   - 用户注册数
   - 订单创建数
   - 库存变化
   - 支付成功率

### 告警配置

告警规则配置在Prometheus中，可通过Grafana查看和管理：

1. **关键告警**
   - 服务不可用超过5分钟
   - 错误率超过1%
   - 响应时间P95超过5秒
   - 磁盘使用率超过90%

2. **业务告警**
   - 订单失败率超过0.5%
   - 库存低于安全阈值
   - 用户注册异常增长

3. **通知渠道**
   - Email: 发送到团队邮箱
   - Slack: 发送到#alerts频道
   - Webhook: 集成到内部系统

## 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 查看服务日志
   docker-compose logs <service-name>
   
   # 检查端口冲突
   netstat -tulpn | grep :<port>
   
   # 重启服务
   docker-compose restart <service-name>
   ```

2. **数据库连接问题**
   ```bash
   # 检查数据库状态
   docker-compose exec postgres psql -U postgres -c "SELECT version();"
   
   # 检查连接数
   docker-compose exec postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
   
   # 重启数据库
   docker-compose restart postgres
   ```

3. **Kafka问题**
   ```bash
   # 检查Kafka状态
   docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
   
   # 检查消息积压
   docker-compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
   
   # 重启Kafka
   docker-compose restart kafka
   ```

4. **Nacos配置问题**
   ```bash
   # 检查Nacos健康
   curl http://localhost:8848/nacos/v1/ns/operator/health
   
   # 重新导入配置
   ./scripts/import-nacos-config.sh
   
   # 重启Nacos
   docker-compose restart nacos
   ```

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f <service-name>

# 查看最近100行日志
docker-compose logs --tail 100 <service-name>

# 查看错误日志
docker-compose logs <service-name> | grep -i error
```

### 性能调优

1. **服务调优**
   ```bash
   # 调整JVM参数
   JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
   
   # 调整连接池
   DB_POOL_SIZE=20
   REDIS_POOL_SIZE=10
   
   # 调整线程池
   THREAD_POOL_SIZE=50
   ```

2. **数据库调优**
   ```bash
   # PostgreSQL配置优化
   shared_buffers = 256MB
   work_mem = 16MB
   maintenance_work_mem = 64MB
   max_connections = 100
   ```

3. **Kafka调优**
   ```bash
   # Kafka配置优化
   num.io.threads = 8
   num.network.threads = 3
   socket.send.buffer.bytes = 102400
   socket.receive.buffer.bytes = 102400
   ```

## 维护指南

### 日常维护

1. **每日检查**
   ```bash
   # 检查服务状态
   ./scripts/check-services-health.sh
   
   # 检查资源使用
   docker stats --no-stream
   
   # 检查日志错误
   ./scripts/check-error-logs.sh
   
   # 备份关键数据
   ./scripts/backup-critical-data.sh
   ```

2. **每周维护**
   ```bash
   # 清理旧日志
   ./scripts/cleanup-old-logs.sh --days 7
   
   # 清理旧报告
   ./scripts/cleanup-old-reports.sh --days 30
   
   # 更新依赖
   ./scripts/update-dependencies.sh
   
   # 运行全面测试
   ./scripts/run-comprehensive-tests.sh
   ```

3. **每月维护**
   ```bash
   # 安全更新
   ./scripts/apply-security-updates.sh
   
   # 性能基准测试
   ./scripts/run-performance-benchmark.sh
   
   # 容量规划
   ./scripts/analyze-capacity-usage.sh
   
   # 文档更新
   ./scripts/update-documentation.sh
   ```

### 备份与恢复

1. **数据备份**
   ```bash
   # 备份数据库
   ./scripts/backup-database.sh --full
   
   # 备份配置
   ./scripts/backup-configuration.sh
   
   # 备份测试数据
   ./scripts/backup-test-data.sh
   ```

2. **数据恢复**
   ```bash
   # 恢复数据库
   ./scripts/restore-database.sh --backup <backup-file>
   
   # 恢复配置
   ./scripts/restore-configuration.sh --backup <backup-file>
   
   # 恢复测试环境
   ./scripts/restore-test-environment.sh
   ```

## 扩展与定制

### 添加新服务

1. **创建服务配置**
   ```yaml
   # 在service_cluster_config.yaml中添加
   new-service:
     enabled: true
     service_name: "new-service"
     version: "1.0.0"
     instances:
       - id: "new-service-1"
         host: "localhost"
         port: 8089
         weight: 1
     endpoints:
       - path: "/api/new/**"
         method: ["GET", "POST"]
     health_check:
       path: "/actuator/health"
   ```

2. **创建Docker配置**
   ```dockerfile
   # 创建Dockerfile
   FROM openjdk:11-jre-slim
   COPY target/new-service.jar app.jar
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

3. **更新部署脚本**
   ```bash
   # 在deploy_cluster.sh中添加
   deploy_new_service() {
     log_info "部署新服务..."
     # 部署逻辑
   }
   ```

### 自定义测试场景

1. **创建测试场景**
   ```yaml
   # 在integration_test_config.yaml中添加
   - id: "scenario-new"
     name: "新业务流程测试"
     description: "测试新的业务功能"
     services_involved: ["user-service", "new-service"]
     test_steps: [...]
   ```

2. **创建测试数据**
   ```json
   # 创建测试数据文件
   {
     "test_scenario": "new-business",
     "data": {...}
   }
   ```

3. **创建测试脚本**
   ```python
   # 创建测试脚本
   def test_new_scenario():
       # 测试逻辑
       pass
   ```

## 支持与联系

### 问题反馈

1. **报告问题**
   - GitHub Issues: [项目Issues页面]
   - Email: [团队邮箱]
   - Slack: [#test-environment频道]

2. **紧急支持**
   - 值班工程师: [联系人]
   - 联系电话: [电话号码]
   - 响应时间: 7x24小时

### 文档更新

1. **文档贡献**
   - 提交PR到文档仓库
   - 更新配置示例
   - 添加故障排除案例

2. **最佳实践**
   - 分享使用经验
   - 提供优化建议
   - 贡献测试用例

## 版本历史

### v1.0.0 (2026-04-27)
- 初始版本发布
- 完整的微服务集群配置
- 数据库主从复制配置
- Kafka消息队列配置
- 集成测试框架
- 自动化部署脚本
- 监控与告警系统

### 未来计划
- 支持Kubernetes部署
- 添加混沌测试
- 集成CI/CD流水线
- 扩展监控功能
- 优化性能测试

---

**最后更新**: 2026-04-27  
**维护团队**: AI-Ready测试团队  
**文档状态**: ✅ 已完成