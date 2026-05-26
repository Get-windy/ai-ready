# ERP Expense Management Module

## Overview
费用单管理模块，提供完整的费用申请、审批、报销、预算控制等功能。

## Features
- 费用单申请和审批流程
- 费用类型管理
- 预算控制和预警
- 多级审批工作流
- 费用统计和报表
- 发票附件管理

## API Endpoints

### Expense APIs (9个)
1. `POST /api/erp/expense/apply` - 申请费用单
2. `GET /api/erp/expense/{id}` - 获取费用单详情
3. `PUT /api/erp/expense/{id}` - 更新费用单
4. `DELETE /api/erp/expense/{id}` - 删除费用单
5. `POST /api/erp/expense/{id}/submit` - 提交审批
6. `POST /api/erp/expense/{id}/approve` - 审批通过
7. `POST /api/erp/expense/{id}/reject` - 审批拒绝
8. `GET /api/erp/expense/list` - 费用单列表
9. `GET /api/erp/expense/statistics` - 费用统计

### Expense Type APIs (1个)
1. `GET /api/erp/expense/type/list` - 费用类型列表

## Database Schema
- `expense_application` - 费用申请主表
- `expense_item` - 费用明细项
- `expense_type` - 费用类型
- `expense_approval` - 审批记录
- `expense_attachment` - 附件表

## Workflow Integration
集成Activiti工作流引擎，支持以下流程：
1. 费用申请流程
2. 多级审批流程
3. 报销支付流程
4. 预算超支审批流程

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Spring Boot 3.1+

### Installation
```bash
mvn clean install
mvn spring-boot:run
```

### Configuration
Create `application.yml` in `src/main/resources`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erp_expense
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8081

# Workflow Configuration
activiti:
  database-schema-update: true
  history-level: audit
  check-process-definitions: true
```

## Testing
Run unit tests:
```bash
mvn test
```

Code coverage target: ≥70%

## Integration with Frontend
- Frontend code location: `I:/AI-Ready/frontend/src/views/expense/`
- Router config: `I:/AI-Ready/frontend/src/router/modules/expense.js`

## License
Proprietary - AIEdge ERP System