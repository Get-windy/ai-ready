# 采购管理模块配置说明文档

## 概述
本文档详细说明了ERP采购管理模块的配置和部署过程。采购管理模块负责供应商管理、采购询价、采购订单、入库管理等核心业务流程。

## 目录结构

### 后端目录结构
```
I:\AI-Ready\backend\erp\erp-purchase\
├── src/
│   ├── main/
│   │   ├── java/cn/aiedge/erp/purchase/
│   │   │   ├── controller/     # 控制器层
│   │   │   ├── service/        # 服务层
│   │   │   ├── entity/         # 实体类
│   │   │   ├── mapper/         # 数据访问层
│   │   │   ├── enums/          # 枚举类
│   │   │   └── dto/            # 数据传输对象
│   │   └── resources/
│   │       ├── application.yml          # 主配置文件
│   │       ├── application-dev.yml      # 开发环境配置
│   │       ├── application-test.yml     # 测试环境配置
│   │       ├── application-prod.yml     # 生产环境配置
│   │       └── mapper/                  # MyBatis映射文件
│   └── test/                    # 测试代码
├── Dockerfile                  # Docker构建文件
├── docker-compose.yml         # Docker Compose配置
└── pom.xml                    # Maven配置
```

### 前端目录结构
```
I:\AI-Ready\frontend\src\components\purchase-management\
├── src/
│   ├── components/            # Vue组件
│   ├── composables/           # 组合式函数
│   ├── stores/                # Pinia状态管理
│   ├── utils/                 # 工具函数
│   ├── api/                   # API接口定义
│   └── types/                 # TypeScript类型定义
├── package.json              # 依赖配置
├── tsconfig.json             # TypeScript配置
└── vite.config.ts            # Vite构建配置
```

### 基础设施目录结构
```
I:\AI-Ready\infra\k8s\purchase-management\
├── deployment.yaml           # Kubernetes部署配置
├── service.yaml             # Kubernetes服务配置
├── configmap.yaml           # 配置映射
├── hpa.yaml                 # 自动缩放配置
└── ingress.yaml             # 入口路由配置
```

## 配置文件说明

### 1. 应用配置文件（application.yml）
```yaml
# 主要配置项：
server:
  port: 8081                  # 服务端口
  servlet:
    context-path: /purchase   # 上下文路径

spring:
  datasource:                # 数据库配置
  redis:                     # Redis缓存配置
  rabbitmq:                  # RabbitMQ消息队列配置

erp:
  purchase:
    api:
      prefix: /api/v1/purchase     # API路径前缀
      admin-prefix: /api/admin/purchase  # 管理端API
      mobile-prefix: /api/mobile/purchase # 移动端API
    business:                # 业务配置
    integration:             # 集成配置
    security:                # 安全配置
```

### 2. 环境配置
- **开发环境** (`application-dev.yml`): 本地开发使用，启用详细日志
- **测试环境** (`application-test.yml`): CI/CD测试环境，使用测试数据库
- **生产环境** (`application-prod.yml`): 生产环境，优化性能和安全性

## 数据库配置

### 数据库表结构
```sql
-- 采购订单表
CREATE TABLE purchase_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  supplier_id BIGINT NOT NULL,
  total_amount DECIMAL(15,2) NOT NULL,
  tax_amount DECIMAL(15,2),
  status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  created_time DATETIME NOT NULL,
  updated_by BIGINT,
  updated_time DATETIME,
  deleted TINYINT DEFAULT 0
);

-- 采购订单明细表
CREATE TABLE purchase_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(10,2) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  total_price DECIMAL(15,2) NOT NULL,
  FOREIGN KEY (order_id) REFERENCES purchase_order(id)
);

-- 供应商表
CREATE TABLE supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  contact_person VARCHAR(100),
  phone VARCHAR(50),
  email VARCHAR(100),
  address VARCHAR(500),
  status VARCHAR(20) DEFAULT 'ACTIVE'
);
```

### 数据初始化脚本
位置：`backend/erp/erp-purchase/src/main/resources/sql/init.sql`

## 部署配置

### 1. Docker部署
```bash
# 构建镜像
docker build -t erp-purchase-service:latest .

# 运行容器
docker-compose up -d

# 查看日志
docker-compose logs -f erp-purchase-service
```

### 2. Kubernetes部署
```bash
# 应用配置
kubectl apply -f infra/k8s/purchase-management/

# 查看部署状态
kubectl get deployment -n erp erp-purchase-service
kubectl get pods -n erp -l app=erp-purchase-service

# 查看服务
kubectl get service -n erp erp-purchase-service
```

### 3. 环境变量配置
| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| DB_HOST | 数据库主机 | localhost |
| DB_PORT | 数据库端口 | 3306 |
| DB_NAME | 数据库名称 | erp_purchase_dev |
| DB_USER | 数据库用户 | purchase_dev |
| DB_PASSWORD | 数据库密码 | DevPassword123! |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PASSWORD | Redis密码 | - |
| RABBITMQ_HOST | RabbitMQ主机 | localhost |
| RABBITMQ_USER | RabbitMQ用户 | guest |
| RABBITMQ_PASSWORD | RabbitMQ密码 | guest |
| SPRING_PROFILES_ACTIVE | 激活的Profile | dev |

## API接口规范

### API路径规范
- 公共API: `/api/v1/purchase/*`
- 管理端API: `/api/admin/purchase/*`
- 移动端API: `/api/mobile/purchase/*`
- 监控API: `/actuator/*`

### 接口示例
```http
# 获取采购订单列表
GET /api/v1/purchase/orders
Authorization: Bearer {token}

# 创建采购订单
POST /api/v1/purchase/orders
Content-Type: application/json
Authorization: Bearer {token}

{
  "supplierId": 1,
  "items": [
    {
      "productId": 1001,
      "quantity": 10,
      "unitPrice": 100.00
    }
  ]
}

# 获取供应商详情
GET /api/v1/purchase/suppliers/1
Authorization: Bearer {token}
```

## 集成配置

### 1. 财务模块集成
- **API端点**: `http://finance-service:8080/finance/api/v1/payables`
- **集成功能**: 应付账款处理、付款申请、发票管理
- **超时配置**: 10秒

### 2. 库存模块集成
- **API端点**: `http://stock-service:8080/stock/api/v1/inventory`
- **集成功能**: 入库单创建、库存更新、物料接收
- **超时配置**: 10秒

### 3. 供应商门户集成
- **API端点**: `http://supplier-service:8080/supplier/api/v1/portal`
- **集成功能**: 供应商信息同步、询价单推送、订单通知
- **超时配置**: 8秒

## 监控配置

### 1. Prometheus指标
- `http_server_requests_seconds_count`: HTTP请求计数
- `http_server_requests_seconds_sum`: HTTP请求总耗时
- `jvm_memory_used_bytes`: JVM内存使用
- `system_cpu_usage`: 系统CPU使用率
- `hikaricp_connections_active`: 数据库连接池活跃连接数

### 2. 健康检查端点
- `GET /actuator/health`: 应用健康状态
- `GET /actuator/health/liveness`: 存活探针
- `GET /actuator/health/readiness`: 就绪探针
- `GET /actuator/info`: 应用信息
- `GET /actuator/metrics`: 指标数据
- `GET /actuator/prometheus`: Prometheus格式指标

### 3. 告警规则
```yaml
# 高错误率告警
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status!~"2.."}[5m]) > 0.1
  for: 5m

# 高响应时间告警
- alert: HighResponseTime
  expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
  for: 5m
```

## 安全配置

### 1. 认证授权
- **认证方式**: JWT Token (Sa-Token)
- **Token有效期**: 30分钟
- **刷新机制**: 支持Token刷新
- **权限控制**: 基于角色的访问控制 (RBAC)

### 2. 网络安全
- **CORS配置**: 允许特定域名访问
- **CSRF防护**: 启用Token验证
- **请求限制**: 接口防刷限流
- **SQL注入防护**: 参数过滤和验证

### 3. 数据安全
- **敏感数据脱敏**: 身份证、手机号、银行卡号
- **日志安全**: 不记录敏感信息
- **传输加密**: HTTPS强制启用

## 性能配置

### 1. 数据库连接池
```yaml
hikari:
  maximum-pool-size: 20      # 最大连接数
  minimum-idle: 5            # 最小空闲连接
  connection-timeout: 30000  # 连接超时(ms)
  idle-timeout: 600000       # 空闲连接超时(ms)
```

### 2. Redis缓存
```yaml
redis:
  lettuce:
    pool:
      max-active: 20         # 最大连接数
      max-idle: 10           # 最大空闲连接
      min-idle: 5            # 最小空闲连接
```

### 3. JVM参数
```bash
# 开发环境
-Xmx512m -Xms256m -XX:+UseG1GC

# 生产环境
-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

## 测试配置

### 1. 单元测试
```bash
# 运行测试
mvn test

# 生成测试报告
mvn surefire-report:report
```

### 2. 集成测试
```bash
# 使用Testcontainers运行集成测试
mvn verify -Pintegration-test
```

### 3. 性能测试
```bash
# 使用JMeter进行性能测试
jmeter -n -t src/test/jmeter/purchase-load-test.jmx -l results.jtl
```

## 故障排查

### 1. 常见问题
1. **数据库连接失败**: 检查数据库服务状态和连接配置
2. **Redis连接超时**: 检查Redis服务状态和网络连通性
3. **消息队列异常**: 检查RabbitMQ服务状态和队列配置
4. **API响应缓慢**: 检查数据库查询性能和缓存命中率

### 2. 日志查询
```bash
# 查看应用日志
docker-compose logs erp-purchase-service

# 查看Kubernetes日志
kubectl logs -n erp deployment/erp-purchase-service

# 查看特定时间段的日志
kubectl logs -n erp deployment/erp-purchase-service --since=1h
```

### 3. 监控仪表板
- **Grafana地址**: http://localhost:3000
- **Prometheus地址**: http://localhost:9090
- **应用监控**: http://localhost:8080/actuator

## 维护指南

### 1. 日常维护
1. 监控应用健康状态和性能指标
2. 定期检查日志文件中的错误和警告
3. 清理过期的缓存数据和日志文件
4. 备份重要数据和配置文件

### 2. 版本升级
1. 备份当前版本的数据和配置
2. 停止当前运行的服务
3. 部署新版本的应用
4. 运行数据库迁移脚本
5. 验证新版本功能正常
6. 回滚计划准备

### 3. 灾难恢复
1. 定期备份数据库和配置文件
2. 准备备用服务器和环境
3. 制定故障切换流程
4. 定期进行灾难恢复演练

## 附录

### 1. 配置检查清单
- [ ] 数据库连接配置正确
- [ ] Redis缓存配置正确
- [ ] 消息队列配置正确
- [ ] API路径配置正确
- [ ] 安全配置生效
- [ ] 监控配置正常
- [ ] 日志配置正常

### 2. 部署验证清单
- [ ] 服务正常启动
- [ ] 健康检查通过
- [ ] API接口可访问
- [ ] 数据库连接正常
- [ ] 缓存服务正常
- [ ] 消息队列正常
- [ ] 监控数据正常

### 3. 性能验证清单
- [ ] 启动时间 < 30秒
- [ ] API响应P95 < 500ms
- [ ] 内存使用稳定
- [ ] CPU使用正常
- [ ] 数据库连接池正常
- [ ] 缓存命中率 > 90%

---
**文档版本**: 1.0.0  
**最后更新**: 2026-04-30  
**维护团队**: AI-Ready DevOps Team