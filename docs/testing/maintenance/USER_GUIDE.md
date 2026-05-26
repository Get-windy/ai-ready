# 测试环境使用手册

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [环境访问](#环境访问)
3. [服务使用指南](#服务使用指南)
4. [测试数据管理](#测试数据管理)
5. [环境配置管理](#环境配置管理)
6. [常见操作流程](#常见操作流程)
7. [最佳实践](#最佳实践)

---

## 概述

### 手册目标

本手册旨在为测试团队、开发团队和运维团队提供完整的测试环境使用指南，确保所有用户能够高效、安全地使用测试环境。

### 适用对象

- **测试工程师**: 进行功能测试、性能测试、回归测试
- **开发工程师**: 验证新功能、调试问题、集成测试
- **运维工程师**: 环境维护、配置管理、监控告警
- **产品经理**: 功能验收、用户体验验证

### 环境特点

- **隔离性**: 与生产环境完全隔离，不影响线上业务
- **可重复性**: 支持一键部署和重置
- **可扩展性**: 支持按需扩展服务实例
- **监控完善**: 集成完整的监控告警系统

## 环境访问

### 访问方式

#### Web界面访问

| 服务 | 访问地址 | 默认端口 | 说明 |
|------|---------|---------|------|
| **用户管理界面** | http://test-env.ai-ready.local:8080 | 8080 | 用户管理Web界面 |
| **API网关** | http://test-env.ai-ready.local:8000 | 8000 | API网关和文档 |
| **监控面板** | http://test-env.ai-ready.local:3000 | 3000 | Grafana监控面板 |
| **API文档** | http://test-env.ai-ready.local:8000/docs | 8000 | Swagger API文档 |

#### API访问

```bash
# 基础API访问
curl -X GET "http://test-env.ai-ready.local:8000/api/v1/health"

# 带认证的API访问
curl -X GET "http://test-env.ai-ready.local:8000/api/v1/users" \
  -H "Authorization: Bearer <token>"
```

#### 数据库访问

| 数据库 | 访问地址 | 端口 | 默认用户 | 说明 |
|--------|---------|------|---------|------|
| **PostgreSQL** | test-env.ai-ready.local | 5432 | postgres | 主业务数据库 |
| **Redis** | test-env.ai-ready.local | 6379 | - | 缓存数据库 |
| **MongoDB** | test-env.ai-ready.local | 27017 | - | 文档数据库 |

### 访问凭证

#### 默认账户

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| **管理员** | admin@ai-ready.local | Admin@2024 | 所有权限 |
| **测试用户** | tester@ai-ready.local | Tester@2024 | 测试相关权限 |
| **开发用户** | developer@ai-ready.local | Developer@2024 | 开发相关权限 |

#### 认证方式

1. **JWT Token认证**
   ```bash
   # 获取Token
   curl -X POST "http://test-env.ai-ready.local:8000/api/v1/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin@ai-ready.local", "password": "Admin@2024"}'
   ```

2. **API Key认证**
   ```bash
   # 使用API Key
   curl -X GET "http://test-env.ai-ready.local:8000/api/v1/data" \
     -H "X-API-Key: <your-api-key>"
   ```

## 服务使用指南

### 用户管理服务

#### 功能概述
- 用户注册、登录、认证
- 角色和权限管理
- 用户信息管理
- 会话管理

#### 常用操作

```bash
# 1. 用户注册
curl -X POST "http://test-env.ai-ready.local:8000/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser@test.com",
    "password": "Password123!",
    "email": "newuser@test.com",
    "fullName": "测试用户"
  }'

# 2. 用户登录
curl -X POST "http://test-env.ai-ready.local:8000/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser@test.com",
    "password": "Password123!"
  }'

# 3. 获取用户信息
curl -X GET "http://test-env.ai-ready.local:8000/api/v1/users/me" \
  -H "Authorization: Bearer <token>"
```

### 订单管理服务

#### 功能概述
- 订单创建、查询、更新
- 订单状态管理
- 订单支付处理
- 订单统计报表

#### 常用操作

```bash
# 1. 创建订单
curl -X POST "http://test-env.ai-ready.local:8000/api/v1/orders" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod_001",
    "quantity": 2,
    "shippingAddress": "测试地址"
  }'

# 2. 查询订单
curl -X GET "http://test-env.ai-ready.local:8000/api/v1/orders" \
  -H "Authorization: Bearer <token>"

# 3. 更新订单状态
curl -X PATCH "http://test-env.ai-ready.local:8000/api/v1/orders/order_123/status" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

### 库存管理服务

#### 功能概述
- 库存查询和更新
- 库存预警和告警
- 库存调拨和盘点
- 库存历史记录

#### 常用操作

```bash
# 1. 查询库存
curl -X GET "http://test-env.ai-ready.local:8000/api/v1/inventory/products/prod_001" \
  -H "Authorization: Bearer <token>"

# 2. 更新库存
curl -X POST "http://test-env.ai-ready.local:8000/api/v1/inventory/adjust" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod_001",
    "quantity": -10,
    "reason": "销售出库"
  }'

# 3. 设置库存预警
curl -X POST "http://test-env.ai-ready.local:8000/api/v1/inventory/alerts" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod_001",
    "threshold": 50,
    "alertType": "LOW_STOCK"
  }'
```

## 测试数据管理

### 数据准备

#### 预置数据

测试环境已预置以下数据：

1. **用户数据**
   - 10个测试用户（不同角色）
   - 5个管理员用户
   - 20个普通用户

2. **产品数据**
   - 50个测试产品
   - 10个产品类别
   - 完整的库存数据

3. **订单数据**
   - 100个测试订单
   - 不同状态的订单样本
   - 完整的订单历史

#### 数据生成脚本

```bash
# 生成测试数据
cd /opt/ai-ready/scripts
./generate-test-data.sh --users 20 --products 100 --orders 200

# 重置测试数据
./reset-test-data.sh --preserve-users
```

### 数据管理

#### 数据备份

```bash
# 备份数据库
docker exec test-env-postgres pg_dump -U postgres ai_ready > backup_$(date +%Y%m%d).sql

# 备份Redis数据
docker exec test-env-redis redis-cli SAVE
docker cp test-env-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

#### 数据恢复

```bash
# 恢复数据库
cat backup_20240427.sql | docker exec -i test-env-postgres psql -U postgres ai_ready

# 恢复Redis数据
docker cp ./redis_backup_20240427.rdb test-env-redis:/data/dump.rdb
docker exec test-env-redis redis-cli SHUTDOWN
docker start test-env-redis
```

### 数据隔离

#### 测试数据隔离策略

1. **按测试套件隔离**
   ```bash
   # 为每个测试套件创建独立的数据空间
   export TEST_SUITE_ID="regression_test_001"
   export DB_NAME="ai_ready_${TEST_SUITE_ID}"
   ```

2. **按用户隔离**
   ```bash
   # 使用不同的用户前缀
   export TEST_USER_PREFIX="test_user_${TEST_RUN_ID}_"
   ```

3. **按时间戳隔离**
   ```bash
   # 使用时间戳作为数据标识
   export TEST_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
   ```

## 环境配置管理

### 配置查看

#### 查看当前配置

```bash
# 查看所有环境变量
docker exec test-env-api env

# 查看特定服务配置
docker exec test-env-api cat /app/config/application.yml

# 查看数据库配置
docker exec test-env-postgres psql -U postgres -c "SHOW ALL;"
```

#### 配置验证

```bash
# 验证配置正确性
cd /opt/ai-ready
./validate-config.sh

# 检查配置依赖
./check-config-dependencies.sh
```

### 配置更新

#### 临时配置更新

```bash
# 临时修改环境变量
docker-compose -f docker-compose.test.yml up -d --env-file .env.test

# 临时修改服务配置
docker exec test-env-api sh -c "echo 'new.config=value' >> /app/config/override.properties"
```

#### 永久配置更新

```bash
# 更新配置文件
vim /opt/ai-ready/config/application-test.yml

# 重新部署服务
docker-compose -f docker-compose.test.yml down
docker-compose -f docker-compose.test.yml up -d
```

### 配置备份和恢复

```bash
# 备份配置
tar czf config_backup_$(date +%Y%m%d).tar.gz /opt/ai-ready/config/

# 恢复配置
tar xzf config_backup_20240427.tar.gz -C /opt/ai-ready/
```

## 常见操作流程

### 新功能测试流程

1. **环境准备**
   ```bash
   # 1. 确保测试环境正常运行
   cd /opt/ai-ready
   docker-compose -f docker-compose.test.yml ps
   
   # 2. 重置测试数据
   ./scripts/reset-test-data.sh --clean
   
   # 3. 部署新版本
   ./scripts/deploy-test-version.sh --version v1.2.3
   ```

2. **功能测试**
   ```bash
   # 1. 运行功能测试
   ./scripts/run-functional-tests.sh --suite new-feature
   
   # 2. 运行集成测试
   ./scripts/run-integration-tests.sh --services user,order,inventory
   
   # 3. 运行性能测试
   ./scripts/run-performance-tests.sh --scenario normal-load
   ```

3. **问题排查**
   ```bash
   # 1. 查看日志
   docker-compose -f docker-compose.test.yml logs --tail=100 api-service
   
   # 2. 检查监控指标
   open http://test-env.ai-ready.local:3000
   
   # 3. 调试API
   curl -v -X POST "http://test-env.ai-ready.local:8000/api/v1/debug" \
     -H "Authorization: Bearer <token>"
   ```

### 回归测试流程

1. **环境检查**
   ```bash
   # 1. 检查服务状态
   ./scripts/check-service-health.sh
   
   # 2. 检查数据完整性
   ./scripts/check-data-integrity.sh
   
   # 3. 检查配置一致性
   ./scripts/check-config-consistency.sh
   ```

2. **执行回归测试**
   ```bash
   # 1. 运行核心功能测试
   ./scripts/run-regression-tests.sh --core
   
   # 2. 运行边界测试
   ./scripts/run-regression-tests.sh --boundary
   
   # 3. 运行异常测试
   ./scripts/run-regression-tests.sh --exception
   ```

3. **生成测试报告**
   ```bash
   # 1. 收集测试结果
   ./scripts/collect-test-results.sh
   
   # 2. 生成测试报告
   ./scripts/generate-test-report.sh --format html
   
   # 3. 发送测试报告
   ./scripts/send-test-report.sh --recipients team@ai-ready.local
   ```

### 性能测试流程

1. **测试准备**
   ```bash
   # 1. 清理缓存
   ./scripts/clear-cache.sh
   
   # 2. 准备测试数据
   ./scripts/prepare-performance-data.sh --size large
   
   # 3. 设置监控基线
   ./scripts/set-monitoring-baseline.sh
   ```

2. **执行性能测试**
   ```bash
   # 1. 负载测试
   ./scripts/run-load-test.sh --users 100 --duration 10m
   
   # 2. 压力测试
   ./scripts/run-stress-test.sh --users 500 --duration 5m
   
   # 3. 稳定性测试
   ./scripts/run-stability-test.sh --duration 24h
   ```

3. **性能分析**
   ```bash
   # 1. 收集性能数据
   ./scripts/collect-performance-metrics.sh
   
   # 2. 生成性能报告
   ./scripts/generate-performance-report.sh
   
   # 3. 性能优化建议
   ./scripts/analyze-performance-bottlenecks.sh
   ```

## 最佳实践

### 测试环境使用最佳实践

1. **环境隔离**
   - 为不同的测试目的使用不同的环境实例
   - 避免在同一个环境中同时进行多个不相关的测试
   - 使用数据隔离策略防止测试数据污染

2. **资源管理**
   - 定期清理不再使用的测试数据
   - 监控环境资源使用情况
   - 及时释放不再需要的测试资源

3. **协作规范**
   - 在使用环境前检查是否有其他团队正在使用
   - 在测试完成后及时通知其他团队
   - 记录环境使用情况和发现的问题

### 测试数据管理最佳实践

1. **数据准备**
   - 使用脚本自动化生成测试数据
   - 确保测试数据的代表性和多样性
   - 为不同的测试场景准备不同的数据集

2. **数据清理**
   - 测试完成后及时清理测试数据
   - 保留必要的测试数据用于问题复现
   - 定期归档历史测试数据

3. **数据安全**
   - 不要在测试环境中使用真实生产数据
   - 对测试数据进行脱敏处理
   - 定期检查测试数据的安全性

### 问题排查最佳实践

1. **日志分析**
   - 熟悉各服务的日志格式和位置
   - 使用日志聚合工具进行日志分析
   - 为关键操作添加详细的日志记录

2. **监控告警**
   - 配置关键指标的监控告警
   - 定期检查监控面板
   - 建立监控告警的响应流程

3. **调试技巧**
   - 使用API调试工具进行接口调试
   - 利用数据库工具进行数据查询和分析
   - 掌握基本的网络诊断工具

### 性能优化最佳实践

1. **性能监控**
   - 建立性能基准线
   - 监控关键性能指标
   - 定期进行性能回归测试

2. **资源优化**
   - 根据实际需求调整资源配置
   - 优化数据库查询和索引
   - 合理使用缓存技术

3. **容量规划**
   - 根据业务增长预测容量需求
   - 定期进行容量评估
   - 建立容量预警机制

## 附录

### A. 常用命令速查表

| 操作 | 命令 | 说明 |
|------|------|------|
| **环境状态检查** | `docker-compose -f docker-compose.test.yml ps` | 检查服务状态 |
| **查看日志** | `docker-compose -f docker-compose.test.yml logs -f api` | 查看API服务日志 |
| **重启服务** | `docker-compose -f docker-compose.test.yml restart api` | 重启API服务 |
| **数据备份** | `./scripts/backup-test-data.sh` | 备份测试数据 |
| **环境重置** | `./scripts/reset-test-environment.sh` | 重置测试环境 |

### B. 环境访问信息汇总

| 项目 | 信息 | 备注 |
|------|------|------|
| **环境地址** | test-env.ai-ready.local | 内部域名 |
| **API网关** | http://test-env.ai-ready.local:8000 | 所有API入口 |
| **监控面板** | http://test-env.ai-ready.local:3000 | Grafana |
| **数据库** | test-env.ai-ready.local:5432 | PostgreSQL |
| **默认管理员** | admin@ai-ready.local / Admin@2024 | 全权限账户 |

### C. 联系方式

| 角色 | 联系人 | 联系方式 | 职责 |
|------|--------|---------|------|
| **环境管理员** | devops-engineer | devops@ai-ready.local | 环境维护、配置管理 |
| **测试负责人** | qa-engineer | qa@ai-ready.local | 测试协调、问题反馈 |
| **技术支持** | team-member | support@ai-ready.local | 技术问题支持 |
| **文档维护** | doc-writer | docs@ai-ready.local | 文档更新维护 |

---

**文档版本**: 1.0.0  
**最后更新**: 2026-04-27  
**维护责任人**: doc-writer  
**审核状态**: ✅ 已审核

> **注意**: 本手册内容会随测试环境的更新而更新，请定期查看最新版本。