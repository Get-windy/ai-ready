# 供应商协同门户模块 (erp-supplier-portal)

## 📋 模块概述

供应商协同门户模块是AI-Ready ERP系统的核心组件之一，专注于供应商管理、绩效评估、询价报价协同和通知推送功能。该模块为中小商贸企业提供完整的供应商管理解决方案，支持多租户架构，满足企业级应用需求。

## 🎯 核心功能

### 1. 供应商管理
- 供应商信息全生命周期管理（注册、认证、维护、归档）
- 多维度供应商分类与标签管理
- 供应商门户账户管理（激活、禁用、同步）
- 供应商资质认证管理

### 2. 绩效管理
- 多维度绩效评估体系（质量、交付、价格、服务、响应速度）
- 周期性评估（月度、季度、年度、专项）
- 综合评分计算与等级评定（A/B/C/D）
- 绩效历史追踪与分析

### 3. 协同工作
- 询价报价全流程管理
- 订单协同处理
- 发货协同追踪
- 对账协同处理

### 4. 通知推送
- 多渠道通知（站内信、邮件、短信、微信、钉钉、企业微信）
- 智能通知模板管理
- 通知状态追踪与回执管理
- 历史通知查询

## 🏗️ 技术架构

### 技术栈
- **后端框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0+ (支持PostgreSQL)
- **ORM框架**: MyBatis-Plus 3.5+
- **API文档**: Knife4j (OpenAPI 3.0)
- **缓存**: Redis 6.0+
- **消息队列**: RabbitMQ 3.12+
- **安全框架**: Spring Security + JWT

### 架构特性
- ✅ 多租户数据隔离
- ✅ 微服务架构兼容
- ✅ 分布式事务支持
- ✅ 高性能缓存设计
- ✅ 异步消息处理
- ✅ 完善的监控指标

## 📁 项目结构

```
erp-supplier-portal/
├── src/main/java/cn/aiedge/erp/supplier/
│   ├── controller/          # RESTful API控制器
│   ├── service/            # 业务服务层
│   │   ├── impl/          # 服务实现
│   ├── repository/         # 数据访问层
│   ├── model/             # 数据模型
│   │   ├── entity/       # 数据库实体
│   ├── dto/               # 数据传输对象
│   └── config/            # 配置类
├── src/main/resources/
│   ├── db/migration/      # 数据库迁移脚本
│   ├── application.yml    # 应用配置
│   └── META-INF/          # 元数据
└── src/test/              # 测试代码
```

## 🚀 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+ 或 PostgreSQL 14+
- Redis 6.0+
- RabbitMQ 3.12+

### 2. 数据库初始化
执行数据库迁移脚本：
```sql
-- 在目标数据库中执行
source src/main/resources/db/migration/V1.0.0__create_supplier_portal_tables.sql
```

### 3. 配置修改
编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/erp_supplier?useUnicode=true&characterEncoding=utf8&useSSL=false
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 0
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

supplier:
  portal:
    # 绩效评分权重配置
    performance-weights:
      quality: 0.3      # 质量权重
      delivery: 0.25    # 交付权重
      price: 0.15       # 价格权重
      service: 0.2      # 服务权重
      response: 0.1     # 响应权重
    
    # 等级评定阈值
    level-thresholds:
      a-level: 90       # A级阈值
      b-level: 80       # B级阈值
      c-level: 70       # C级阈值
      d-level: 0        # D级阈值
```

### 4. 编译运行
```bash
# 编译项目
mvn clean compile

# 打包
mvn clean package -DskipTests

# 运行
java -jar target/erp-supplier-portal-1.0.0-SNAPSHOT.jar
```

### 5. 访问API文档
启动后访问：http://localhost:8080/doc.html

## 📖 API接口文档

### 供应商管理API

#### 1. 创建供应商
```http
POST /api/supplier
Content-Type: application/json

{
  "supplierCode": "SUP20240001",
  "supplierName": "北京科技有限公司",
  "supplierType": 1,
  "contactPerson": "张三",
  "contactPhone": "13800138000",
  "contactEmail": "zhangsan@example.com",
  "companyAddress": "北京市朝阳区建国门外大街1号"
}
```

#### 2. 分页查询供应商
```http
POST /api/supplier/page
Content-Type: application/json

{
  "pageNum": 1,
  "pageSize": 20,
  "supplierName": "科技",
  "cooperationStatuses": [1, 2],
  "orderBy": "createTime",
  "orderDirection": "desc"
}
```

#### 3. 获取供应商详情
```http
GET /api/supplier/{id}
```

#### 4. 更新供应商
```http
PUT /api/supplier
Content-Type: application/json

{
  "id": 1,
  "supplierName": "北京科技有限公司(更新)",
  "contactPerson": "李四",
  "contactPhone": "13900139000"
}
```

#### 5. 删除供应商
```http
DELETE /api/supplier/{id}
```

### 绩效管理API

#### 1. 绩效评估
```http
POST /api/supplier/performance/evaluate
Content-Type: application/json

{
  "supplierId": 1,
  "evaluationPeriod": "2024-01",
  "evaluationType": 1,
  "qualityScore": 95.0,
  "deliveryScore": 90.0,
  "priceScore": 85.0,
  "serviceScore": 92.0,
  "responseScore": 88.0,
  "strengths": "产品质量稳定，交付及时",
  "improvementSuggestions": "价格方面可以进一步优化"
}
```

#### 2. 获取绩效历史
```http
GET /api/supplier/{supplierId}/performance/history?periodType=1&limit=12
```

### 门户管理API

#### 1. 激活门户账户
```http
POST /api/supplier/{id}/activate-portal?portalAccountId=user123456
```

#### 2. 禁用门户账户
```http
POST /api/supplier/{id}/disable-portal?reason=长期未登录
```

## 🔧 配置说明

### 多租户配置
```yaml
tenant:
  # 租户数据隔离策略
  isolation:
    strategy: DATABASE  # DATABASE | SCHEMA | COLUMN
    column-name: tenant_id
  
  # 默认租户
  default-tenant: default
  
  # 租户白名单
  white-list:
    - default
    - tenant_001
    - tenant_002
```

### 缓存配置
```yaml
cache:
  # Redis缓存配置
  redis:
    # 供应商信息缓存
    supplier:
      key-prefix: "supplier:"
      ttl: 3600  # 1小时
    
    # 绩效数据缓存
    performance:
      key-prefix: "performance:"
      ttl: 1800  # 30分钟
    
    # 通知缓存
    notification:
      key-prefix: "notification:"
      ttl: 300   # 5分钟
```

### 通知渠道配置
```yaml
notification:
  channels:
    # 邮件配置
    email:
      enabled: true
      smtp-host: smtp.example.com
      smtp-port: 587
      username: noreply@example.com
      password: your_password
      from-address: noreply@example.com
    
    # 短信配置
    sms:
      enabled: true
      provider: aliyun
      access-key: your_access_key
      access-secret: your_access_secret
      sign-name: AIERP
      template-codes:
        verification: SMS_123456
        notification: SMS_234567
    
    # 微信配置
    wechat:
      enabled: false
      app-id: your_app_id
      app-secret: your_app_secret
```

## 🧪 测试

### 单元测试
```bash
# 运行所有单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=SupplierServiceTest

# 生成测试报告
mvn surefire-report:report
```

### 集成测试
```bash
# 运行集成测试（需要TestContainers）
mvn verify -Pintegration-test
```

### API测试
使用Postman或curl测试API接口：
```bash
# 测试创建供应商
curl -X POST http://localhost:8080/api/supplier \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: default" \
  -H "Authorization: Bearer {jwt_token}" \
  -d '{
    "supplierCode": "TEST001",
    "supplierName": "测试供应商",
    "supplierType": 1,
    "contactPerson": "测试联系人",
    "contactPhone": "13800138000"
  }'
```

## 📊 监控指标

模块提供以下监控指标：

### 业务指标
- 供应商总数及增长趋势
- 供应商等级分布（A/B/C/D）
- 绩效评估完成率
- 门户账户激活率
- 通知发送成功率

### 性能指标
- API响应时间（P50/P95/P99）
- 数据库查询性能
- 缓存命中率
- 消息队列处理延迟

### 健康检查
```bash
# 健康检查端点
GET /actuator/health

# 指标端点
GET /actuator/metrics

# 自定义业务指标
GET /actuator/supplier-metrics
```

## 🔄 部署

### Docker部署
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/erp-supplier-portal-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: erp-supplier-portal
spec:
  replicas: 3
  selector:
    matchLabels:
      app: erp-supplier-portal
  template:
    metadata:
      labels:
        app: erp-supplier-portal
    spec:
      containers:
      - name: supplier-portal
        image: erp-supplier-portal:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: erp-config
              key: db.host
```

## 📈 性能优化建议

### 数据库优化
1. 定期清理历史数据
2. 建立合适的索引
3. 使用读写分离
4. 实施分库分表（数据量大时）

### 缓存策略
1. 热点数据缓存
2. 缓存预热机制
3. 缓存穿透防护
4. 缓存雪崩防护

### 异步处理
1. 耗时长操作异步化
2. 批量处理优化
3. 消息队列解耦

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如有问题或建议，请通过以下方式联系：
- 提交 [Issue](https://github.com/your-org/erp-supplier-portal/issues)
- 发送邮件至 support@aiedge.cn
- 加入技术交流群

---

**最后更新**: 2026-04-29  
**版本**: 1.0.0-SNAPSHOT  
**状态**: ✅ 生产就绪