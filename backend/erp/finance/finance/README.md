# 财务管理模块

## 概述
财务管理模块是AI-Ready系统的核心模块之一，负责处理企业的财务相关业务，包括会计核算、预算管理、支付处理、税务计算和财务报表等。

## 模块结构
```
finance/
├── src/main/java/cn/aiedge/finance/
│   ├── accounting/       # 会计核算
│   ├── budget/          # 预算管理
│   ├── config/          # 配置类
│   ├── controller/      # 控制器层
│   ├── dto/            # 数据传输对象
│   ├── entity/         # 实体类
│   ├── exception/      # 异常处理
│   ├── payment/        # 支付处理
│   ├── report/         # 财务报表
│   ├── repository/     # 数据访问层
│   ├── service/        # 业务逻辑层
│   ├── tax/           # 税务计算
│   └── util/          # 工具类
├── src/main/resources/
│   └── application.yml # 配置文件
└── src/test/           # 测试代码
```

## 快速开始

### 1. 环境要求
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

### 2. 数据库配置
创建数据库：
```sql
CREATE DATABASE ai_ready_finance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'finance_user'@'%' IDENTIFIED BY 'finance_password';
GRANT ALL PRIVILEGES ON ai_ready_finance.* TO 'finance_user'@'%';
FLUSH PRIVILEGES;
```

### 3. 配置文件
配置文件位于 `src/main/resources/application.yml`，主要配置项：
- 数据库连接
- Redis缓存
- 消息队列
- 安全配置
- 财务管理相关参数

### 4. 启动应用
```bash
cd I:\AI-Ready\backend\finance
mvn spring-boot:run
```

应用将在 http://localhost:8085 启动。

## API文档
启动后访问：
- Swagger UI: http://localhost:8085/swagger-ui.html
- API文档: http://localhost:8085/api-docs

## 功能特性

### 1. 会计核算
- 凭证录入与审核
- 科目余额计算
- 期间结转处理
- 凭证反冲与调整

### 2. 预算管理
- 预算编制与审批
- 预算执行监控
- 预算调整与变更
- 预算执行分析

### 3. 支付处理
- 多种支付方式支持（支付宝、微信、银行转账）
- 支付状态跟踪
- 支付失败重试
- 支付凭证生成

### 4. 税务计算
- 增值税计算
- 所得税计算
- 税费自动计算
- 税务报表生成

### 5. 财务报表
- 资产负债表
- 利润表
- 现金流量表
- 预算执行差异表

## 测试
运行单元测试：
```bash
mvn test
```

运行集成测试：
```bash
mvn verify -P integration-test
```

## 部署
### 开发环境
```bash
mvn clean package -P dev
```

### 测试环境
```bash
mvn clean package -P test
```

### 生产环境
```bash
mvn clean package -P prod
```

## 监控
应用提供以下监控端点：
- 健康检查: http://localhost:8085/actuator/health
- 指标信息: http://localhost:8085/actuator/metrics
- Prometheus: http://localhost:8085/actuator/prometheus

## 注意事项
1. 生产环境务必修改默认密码和密钥
2. 建议启用HTTPS
3. 定期备份数据库
4. 监控系统资源使用情况

## 故障排查
常见问题及解决方案参见 [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)