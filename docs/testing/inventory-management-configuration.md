# 库存管理模块配置说明文档

## 模块概述
库存管理模块是AI-Ready项目的核心模块之一，负责库存数据的维护、查询、统计和预警功能。

## 目录结构
```
I:\AI-Ready\backend\inventory\
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/
│   │   │       └── aiedge/
│   │   │           └── inventory/
│   │   │               ├── controller/       # 控制器层
│   │   │               ├── service/          # 服务层
│   │   │               ├── entity/           # 实体类
│   │   │               └── repository/       # 数据访问层
│   │   └── resources/
│   │       ├── application.yml              # 主配置文件
│   │       ├── application-dev.yml          # 开发环境配置
│   │       └── application-test.yml         # 测试环境配置
│   └── test/
│       └── java/
│           └── cn/
│               └── aiedge/
│                   └── inventory/           # 测试代码
└── pom.xml                                 # Maven配置文件
```

## 配置文件详解

### 1. 数据源配置
```yaml
datasource:
  primary:
    url: jdbc:postgresql://localhost:5432/ai_ready_inventory
    username: postgres
    password: postgres
```

**配置说明**：
- 主数据库：用于读写操作
- 从数据库：用于只读查询（负载均衡）
- 连接池：使用Druid连接池，支持监控和统计

### 2. 缓存配置
```yaml
redis:
  host: localhost
  port: 6379
  database: 1
```

**配置说明**：
- 数据库1：库存数据缓存
- 超时时间：3000ms
- 连接池：最大8个连接

### 3. 消息队列配置
```yaml
rabbitmq:
  host: localhost
  port: 5672
  virtual-host: /
```

**支持的消息类型**：
- 库存变更通知
- 低库存告警
- 库存统计计算
- 数据同步消息

### 4. 安全配置
```yaml
security:
  jwt:
    secret: inventory-management-secret-key
    expiration: 86400000
```

**权限要求**：
- 库存查询：所有用户
- 库存修改：库存管理员
- 库存删除：超级管理员

## 部署配置

### Docker部署配置
```yaml
# I:\AI-Ready\infra\k8s\inventory-management\deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-management
  labels:
    app: inventory-management
spec:
  replicas: 3
  selector:
    matchLabels:
      app: inventory-management
  template:
    metadata:
      labels:
        app: inventory-management
    spec:
      containers:
      - name: inventory-management
        image: ai-ready/inventory-management:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

### Kubernetes服务配置
```yaml
# I:\AI-Ready\infra\k8s\inventory-management\service.yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-management
spec:
  selector:
    app: inventory-management
  ports:
  - port: 80
    targetPort: 8081
    protocol: TCP
  type: ClusterIP
```

## 监控配置

### Prometheus监控指标
库存管理模块暴露以下监控指标：
1. **业务指标**
   - `inventory_stock_total` - 库存总量
   - `inventory_low_stock_count` - 低库存数量
   - `inventory_movement_count` - 库存变动次数

2. **性能指标**
   - `inventory_api_duration_seconds` - API响应时间
   - `inventory_db_query_duration_seconds` - 数据库查询时间
   - `inventory_cache_hit_rate` - 缓存命中率

### Grafana仪表板配置
创建库存管理监控仪表板，包含：
- 库存总量趋势图
- 库存变动频率图
- API响应时间监控
- 数据库连接池状态

## 测试环境配置

### 测试数据库初始化
```sql
-- I:\AI-Ready\tests\data\inventory-test-data.sql
CREATE DATABASE ai_ready_inventory_test;
\c ai_ready_inventory_test;

-- 创建库存表
CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    quantity INTEGER DEFAULT 0,
    unit VARCHAR(20),
    safe_stock INTEGER DEFAULT 10,
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建库存流水表
CREATE TABLE inventory_flow (
    id SERIAL PRIMARY KEY,
    inventory_id INTEGER NOT NULL,
    flow_type INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    before_quantity INTEGER,
    after_quantity INTEGER,
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operator_id INTEGER,
    remark VARCHAR(500)
);

-- 插入测试数据
INSERT INTO inventory (product_id, warehouse_id, quantity, unit) VALUES
(1, 1, 100, '个'),
(2, 1, 50, '个'),
(3, 2, 200, '件'),
(4, 3, 30, '套');
```

### 测试环境配置文件
```yaml
# I:\AI-Ready\backend\inventory\src\main\resources\application-test.yml
test:
  environment: test
  database:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
  cache:
    enabled: false
  queue:
    enabled: false
```

## 质量门禁要求

### 1. 配置完整性门禁
- [x] 目录结构符合规范
- [x] 配置文件齐全
- [x] API路由配置正确

### 2. 功能可用性门禁
- [ ] 服务可正常启动（健康检查通过）
- [ ] API接口可调用（响应状态码200）
- [ ] 数据库连接正常（连接池无异常）
- [ ] 缓存服务正常（Redis连接正常）

### 3. 性能门禁
- [ ] 启动时间 < 30秒
- [ ] API响应P95 < 500ms
- [ ] 数据库查询P95 < 100ms
- [ ] 缓存命中率 > 80%

## 启动与验证

### 启动命令
```bash
# 开发环境
mvn spring-boot:run -Dspring.profiles.active=dev

# 测试环境  
mvn spring-boot:run -Dspring.profiles.active=test

# 生产环境
java -jar inventory-management.jar --spring.profiles.active=prod
```

### 健康检查
```bash
# 应用健康检查
curl http://localhost:8081/api/v1/inventory/actuator/health

# 数据库健康检查
curl http://localhost:8081/api/v1/inventory/actuator/health/db

# 缓存健康检查
curl http://localhost:8081/api/v1/inventory/actuator/health/redis
```

### API接口测试
```bash
# 查询库存列表
curl http://localhost:8081/api/v1/inventory/inventories

# 查询特定库存
curl http://localhost:8081/api/v1/inventory/inventories/1

# 创建库存
curl -X POST http://localhost:8081/api/v1/inventory/inventories \
  -H "Content-Type: application/json" \
  -d '{"productId": 5, "warehouseId": 1, "quantity": 100}'
```

## 故障排查

### 常见问题
1. **数据库连接失败**
   - 检查PostgreSQL服务是否运行
   - 验证数据库连接字符串
   - 检查用户名密码

2. **Redis连接失败**
   - 检查Redis服务是否运行
   - 验证Redis配置参数
   - 检查防火墙设置

3. **API响应缓慢**
   - 检查数据库索引
   - 优化SQL查询
   - 调整连接池参数

4. **内存泄漏**
   - 监控JVM内存使用
   - 检查缓存策略
   - 优化大对象处理

### 日志查看
```bash
# 查看应用日志
tail -f logs/inventory-management.log

# 查看错误日志
grep -i "error" logs/inventory-management.log

# 查看慢查询日志
grep -i "slow" logs/inventory-management.log
```

## 版本历史

| 版本 | 日期 | 说明 | 负责人 |
|------|------|------|--------|
| v1.0.0 | 2026-04-30 | 初始版本，基础配置完成 | team-member |
| v1.0.1 | 2026-04-30 | 添加监控配置 | team-member |
| v1.0.2 | 2026-04-30 | 完善测试环境配置 | team-member |

---

**文档状态**: 已完成基础配置  
**最后更新**: 2026-04-30  
**负责人**: team-member  
**项目**: AI-Ready  
**模块**: 库存管理