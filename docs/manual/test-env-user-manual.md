# Sprint 27+1 测试环境用户手册

> **文档版本**: v1.0
> **创建日期**: 2026-04-27
> **适用对象**: 测试人员、开发人员、产品经理
> **Sprint**: Sprint 27+1
> **预计阅读时间**: 60分钟

## 📋 目录

- [1. 环境访问指南](#1-环境访问指南)
  - [1.1 环境地址和端口说明](#11-环境地址和端口说明)
  - [1.2 测试环境访问方式](#12-测试环境访问方式)
  - [1.3 账号申请和权限管理流程](#13-账号申请和权限管理流程)
  - [1.4 网络访问限制说明](#14-网络访问限制说明)
- [2. 功能使用手册](#2-功能使用手册)
  - [2.1 用户管理功能](#21-用户管理功能)
  - [2.2 订单管理功能](#22-订单管理功能)
  - [2.3 库存管理功能](#23-库存管理功能)
  - [2.4 采购管理功能](#24-采购管理功能)
  - [2.5 财务管理功能](#25-财务管理功能)
  - [2.6 供应商门户功能](#26-供应商门户功能)
- [3. API接口调用示例](#3-api接口调用示例)
  - [3.1 认证接口](#31-认证接口)
  - [3.2 用户管理接口](#32-用户管理接口)
  - [3.3 订单管理接口](#33-订单管理接口)
  - [3.4 库存管理接口](#34-库存管理接口)
  - [3.5 数据查询接口](#35-数据查询接口)
  - [3.6 文件上传接口](#36-文件上传接口)
- [4. 数据管理和查询操作指南](#4-数据管理和查询操作指南)
  - [4.1 测试数据准备流程](#41-测试数据准备流程)
  - [4.2 数据库查询操作](#42-数据库查询操作)
  - [4.3 缓存数据管理](#43-缓存数据管理)
  - [4.4 数据导入导出操作](#44-数据导入导出操作)
  - [4.5 数据备份恢复操作](#45-数据备份恢复操作)
- [5. 监控和日志查看方法](#5-监控和日志查看方法)
  - [5.1 系统监控指标查看](#51-系统监控指标查看)
  - [5.2 应用性能监控](#52-应用性能监控)
  - [5.3 日志查看和分析](#53-日志查看和分析)
  - [5.4 告警规则和通知](#54-告警规则和通知)
- [6. 操作流程指南](#6-操作流程指南)
  - [6.1 测试数据准备流程](#61-测试数据准备流程)
  - [6.2 测试执行操作步骤](#62-测试执行操作步骤)
  - [6.3 测试结果验证方法](#63-测试结果验证方法)
  - [6.4 问题上报流程](#64-问题上报流程)
- [7. 常见问题处理](#7-常见问题处理)
  - [7.1 登录问题](#71-登录问题)
  - [7.2 权限问题](#72-权限问题)
  - [7.3 数据问题](#73-数据问题)
  - [7.4 性能问题](#74-性能问题)
  - [7.5 网络问题](#75-网络问题)
- [8. 最佳实践指南](#8-最佳实践指南)
  - [8.1 测试环境使用最佳实践](#81-测试环境使用最佳实践)
  - [8.2 性能优化建议](#82-性能优化建议)
  - [8.3 安全使用规范](#83-安全使用规范)
  - [8.4 资源优化建议](#84-资源优化建议)
- [9. 附录](#9-附录)
  - [9.1 快捷键列表](#91-快捷键列表)
  - [9.2 错误代码对照表](#92-错误代码对照表)
  - [9.3 联系信息](#93-联系信息)
  - [9.4 更新记录](#94-更新记录)

---

## 1. 环境访问指南

### 1.1 环境地址和端口说明

#### 1.1.1 环境基本信息

| 环境名称 | 访问地址 | 说明 | 负责人 | 状态 |
|---------|---------|------|--------|------|
| **Sprint 27+1 主测试环境** | http://test-env.ai-ready.com | 主测试环境，用于功能测试 | 张三 | ✅ 运行中 |
| **Sprint 27+1 性能测试环境** | http://perf-test.ai-ready.com | 性能测试专用环境 | 李四 | ✅ 运行中 |
| **Sprint 27+1 安全测试环境** | http://sec-test.ai-ready.com | 安全测试专用环境 | 王五 | ✅ 运行中 |

#### 1.1.2 服务端口分配表

| 服务名称 | 内部端口 | 外部端口 | 访问地址 | 用途 | 状态 |
|---------|----------|----------|---------|------|------|
| **前端Web服务** | 80 | 3000 | http://test-env.ai-ready.com:3000 | Web用户界面 | ✅ 正常 |
| **API网关服务** | 8080 | 8080 | http://test-env.ai-ready.com:8080 | RESTful API接口 | ✅ 正常 |
| **用户管理服务** | 8081 | 8081 | http://test-env.ai-ready.com:8081 | 用户认证和授权 | ✅ 正常 |
| **订单管理服务** | 8082 | 8082 | http://test-env.ai-ready.com:8082 | 订单处理功能 | ✅ 正常 |
| **库存管理服务** | 8083 | 8083 | http://test-env.ai-ready.com:8083 | 库存管理功能 | ✅ 正常 |
| **采购管理服务** | 8084 | 8084 | http://test-env.ai-ready.com:8084 | 采购管理功能 | ✅ 正常 |
| **财务管理服务** | 8085 | 8085 | http://test-env.ai-ready.com:8085 | 财务管理功能 | ✅ 正常 |
| **供应商门户** | 8086 | 8086 | http://test-env.ai-ready.com:8086 | 供应商协作功能 | ✅ 正常 |
| **PostgreSQL数据库** | 5432 | 5432 | 仅内网访问 | 主数据库 | ✅ 正常 |
| **Redis缓存** | 6379 | 6379 | 仅内网访问 | 缓存服务 | ✅ 正常 |
| **RabbitMQ消息队列** | 5672 | 5672 | 仅内网访问 | 消息队列服务 | ✅ 正常 |
| **Prometheus监控** | 9090 | 9090 | http://test-env.ai-ready.com:9090 | 监控数据收集 | ✅ 正常 |
| **Grafana仪表板** | 3000 | 3001 | http://test-env.ai-ready.com:3001 | 监控可视化 | ✅ 正常 |
| **AlertManager告警** | 9093 | 9093 | http://test-env.ai-ready.com:9093 | 告警管理 | ✅ 正常 |

### 1.2 测试环境访问方式

#### 1.2.1 浏览器访问

**方式一：直接访问**
```bash
# 前端界面（推荐）
http://test-env.ai-ready.com:3000

# API文档（Swagger UI）
http://test-env.ai-ready.com:8080/swagger-ui.html

# 监控仪表板
http://test-env.ai-ready.com:3001
用户名: admin
密码: admin
```

**方式二：通过VPN访问（需要内网权限）**
```bash
# 连接公司VPN后访问
# VPN配置信息请咨询IT部门

# 内部地址访问
http://192.168.1.100:3000  # 前端界面
http://192.168.1.100:8080  # API服务
```

#### 1.2.2 命令行访问

**使用curl测试连接:**
```bash
# 测试API服务健康状态
curl -f http://test-env.ai-ready.com:8080/actuator/health

# 测试前端服务
curl -I http://test-env.ai-ready.com:3000

# 测试数据库连接（需要VPN）
curl -X POST http://test-env.ai-ready.com:8080/api/v1/db/test-connection
```

**使用Postman测试API:**
1. 下载并安装Postman: https://www.postman.com/downloads/
2. 导入API文档: 使用Swagger文档链接
3. 配置环境变量:
   ```json
   {
     "baseUrl": "http://test-env.ai-ready.com:8080",
     "token": "Bearer <your_token>"
   }
   ```

#### 1.2.3 移动端访问

**Android应用:**
- 应用名称: AI-Ready测试客户端
- 下载地址: http://test-env.ai-ready.com:3000/download/android
- 版本要求: Android 8.0+

**iOS应用:**
- 应用名称: AI-Ready测试版
- 下载地址: http://test-env.ai-ready.com:3000/download/ios
- 版本要求: iOS 12.0+

### 1.3 账号申请和权限管理流程

#### 1.3.1 账号申请流程

**步骤1：准备申请材料**
```bash
# 需要准备的信息
- 姓名: [您的姓名]
- 工号: [员工工号]
- 部门: [所属部门]
- 职位: [职位名称]
- 邮箱: [公司邮箱]
- 手机: [联系电话]
- 申请理由: [具体测试需求]
- 预计使用时间: [开始日期]至[结束日期]
```

**步骤2：提交申请**
```bash
# 方式一：通过系统申请（推荐）
访问: http://test-env.ai-ready.com:3000/account/apply

# 方式二：邮件申请
收件人: test-env-admin@ai-ready.com
主题: 测试环境账号申请 - [姓名] - [部门]
正文模板:
---
申请人信息:
- 姓名: 张三
- 工号: 12345
- 部门: 测试部
- 职位: 测试工程师
- 邮箱: zhangsan@ai-ready.com
- 手机: 13800138000

测试需求:
- 测试模块: 订单管理、库存管理
- 测试类型: 功能测试、接口测试
- 预计使用时间: 2026-04-27 至 2026-05-27
- 需要权限: 前端访问、API访问、数据库只读

申请理由:
Sprint 27+1功能测试，需要对订单和库存模块进行全面测试
---
```

**步骤3：审批流程**
```mermaid
graph TD
    A[提交申请] --> B[部门经理审批]
    B --> C{审批结果}
    C -->|通过| D[环境管理员审批]
    C -->|拒绝| E[申请驳回]
    D --> F{审批结果}
    F -->|通过| G[账号创建]
    F -->|拒绝| E
    G --> H[权限分配]
    H --> I[通知申请人]
    E --> J[通知申请人]
```

**审批时间:**
- 部门经理审批: 1个工作日内
- 环境管理员审批: 1个工作日内
- 账号创建: 审批通过后4小时内

#### 1.3.2 权限管理

**权限级别说明:**
| 权限级别 | 说明 | 可访问资源 | 适用角色 |
|---------|------|-----------|---------|
| **Level 1: 只读权限** | 只能查看，不能修改 | 前端界面、API查询接口、监控系统 | 产品经理、观察员 |
| **Level 2: 基础操作权限** | 可以执行基本操作 | 前端功能操作、API增删改查 | 测试人员、开发人员 |
| **Level 3: 高级操作权限** | 可以执行高级操作 | 数据库查询、配置修改、数据导入导出 | 高级测试人员、开发组长 |
| **Level 4: 管理员权限** | 完全控制权限 | 所有资源、用户管理、权限分配 | 环境管理员、运维人员 |

**权限申请和调整:**
```bash
# 查看当前权限
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/users/me/permissions

# 申请权限提升
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/permissions/request \
  -d '{
    "permission": "DATABASE_READ",
    "reason": "需要进行数据库查询测试",
    "duration": "7d",
    "approver": "manager@ai-ready.com"
  }'

# 审批权限申请（管理员操作）
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/admin/permissions/approve \
  -d '{
    "requestId": "perm_req_123456",
    "action": "APPROVE",
    "comment": "同意测试需求"
  }'
```

### 1.4 网络访问限制说明

#### 1.4.1 访问时间限制

| 时间段 | 访问权限 | 说明 | 异常处理 |
|--------|---------|------|---------|
| **工作日 08:00-20:00** | ✅ 完全访问 | 正常工作时间 | - |
| **工作日 20:00-08:00** | ⚠️ 限制访问 | 仅开发人员和测试人员可访问 | 需要VPN连接 |
| **周末和节假日** | ❌ 禁止访问 | 系统维护时间 | 如需访问需特殊申请 |
| **系统维护时间** | ❌ 禁止访问 | 每周日凌晨2:00-4:00 | 提前通知 |

**特殊访问申请:**
```bash
# 申请特殊时间访问
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/access/special-request \
  -d '{
    "reason": "紧急问题修复",
    "startTime": "2026-04-27T22:00:00Z",
    "endTime": "2026-04-28T02:00:00Z",
    "approver": "ops-manager@ai-ready.com"
  }'
```

#### 1.4.2 IP地址限制

**允许访问的IP段:**
```bash
# 公司内部网络
192.168.1.0/24    # 办公区
10.0.0.0/16       # 数据中心
172.16.0.0/12     # 开发环境

# VPN网络
10.8.0.0/24       # OpenVPN
10.9.0.0/24       # IPSec VPN

# 合作伙伴网络（需申请）
203.0.113.0/24    # 合作伙伴A
198.51.100.0/24   # 合作伙伴B
```

**IP白名单申请:**
```bash
# 申请IP白名单
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/access/ip-whitelist \
  -d '{
    "ipAddress": "203.0.113.100",
    "reason": "合作伙伴测试需求",
    "duration": "30d",
    "contact": "partner@example.com"
  }'
```

#### 1.4.3 流量限制

| 资源类型 | 限制 | 说明 | 超出处理 |
|---------|------|------|---------|
| **API请求频率** | 100请求/分钟 | 单个用户每分钟最大请求数 | 返回429状态码 |
| **文件上传大小** | 100MB/文件 | 单个文件最大上传大小 | 返回413状态码 |
| **数据导出大小** | 10,000条/次 | 单次数据导出最大记录数 | 分批导出 |
| **并发连接数** | 50连接/用户 | 单个用户最大并发连接数 | 拒绝新连接 |

**查看当前限制:**
```bash
# 查看API限制状态
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/rate-limit/status

# 返回示例:
{
  "remaining": 85,
  "limit": 100,
  "resetTime": "2026-04-27T15:30:00Z",
  "strategy": "token-bucket"
}
```

## 2. 功能使用手册

### 2.1 用户管理功能

#### 2.1.1 用户注册

**Web界面操作:**
1. 访问注册页面: http://test-env.ai-ready.com:3000/register
2. 填写注册信息:
   - 用户名: 3-20个字符，支持字母、数字、下划线
   - 邮箱: 有效的公司邮箱地址
   - 密码: 8-20位，必须包含大小写字母、数字、特殊字符
   - 确认密码: 与密码一致
   - 手机号: 11位手机号码（可选）
3. 阅读并同意用户协议
4. 点击"注册"按钮
5. 查收验证邮件，点击验证链接完成注册

**API调用示例:**
```bash
# 用户注册
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user_001",
    "email": "test.user@ai-ready.com",
    "password": "Test123!@#",
    "confirmPassword": "Test123!@#",
    "phone": "13800138000",
    "agreedToTerms": true
  }'

# 返回示例:
{
  "userId": "user_1234567890",
  "username": "test_user_001",
  "email": "test.user@ai-ready.com",
  "status": "PENDING_VERIFICATION",
  "verificationToken": "verify_abc123def456",
  "message": "注册成功，请查收验证邮件"
}

# 邮箱验证
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "verify_abc123def456"
  }'
```

#### 2.1.2 用户登录

**Web界面操作:**
1. 访问登录页面: http://test-env.ai-ready.com:3000/login
2. 输入用户名和密码
3. 选择"记住我"选项（保持登录状态7天）
4. 点击"登录"按钮
5. 登录成功后跳转到首页

**API调用示例:**
```bash
# 用户登录
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user_001",
    "password": "Test123!@#",
    "rememberMe": true
  }'

# 返回示例:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "user_1234567890",
    "username": "test_user_001",
    "email": "test.user@ai-ready.com",
    "roles": ["USER"],
    "permissions": ["FRONTEND_ACCESS", "API_ACCESS"]
  }
}

# 使用令牌访问受保护资源
curl -X GET \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://test-env.ai-ready.com:8080/api/v1/users/me
```

#### 2.1.3 用户信息管理

**查看个人信息:**
```bash
# 获取当前用户信息
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/users/me

# 返回示例:
{
  "id": "user_1234567890",
  "username": "test_user_001",
  "email": "test.user@ai-ready.com",
  "nickname": "测试用户",
  "avatar": "https://test-env.ai-ready.com/avatars/default.png",
  "phone": "13800138000",
  "department": "测试部",
  "position": "测试工程师",
  "status": "ACTIVE",
  "createdAt": "2026-04-27T10:00:00Z",
  "lastLoginAt": "2026-04-27T14:30:00Z"
}
```

**更新个人信息:**
```bash
# 更新用户信息
curl -X PUT \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/users/me \
  -d '{
    "nickname": "新昵称",
    "avatar": "https://test-env.ai-ready.com/avatars/custom.png",
    "phone": "13900000000",
    "department": "质量保证部",
    "position": "高级测试工程师"
  }'
```

**修改密码:**
```bash
# 修改密码
curl -X PUT \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/auth/password \
  -d '{
    "oldPassword": "Test123!@#",
    "newPassword": "NewPass456$%^"
  }'
```

#### 2.1.4 用户权限管理

**查看权限列表:**
```bash
# 获取当前用户权限
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/users/me/permissions

# 返回示例:
{
  "userId": "user_1234567890",
  "username": "test_user_001",
  "roles": ["TESTER"],
  "permissions": [
    {
      "code": "FRONTEND_ACCESS",
      "name": "前端访问权限",
      "description": "允许访问前端界面",
      "level": "READ"
    },
    {
      "code": "API_ACCESS",
      "name": "API访问权限",
      "description": "允许调用API接口",
      "level": "READ_WRITE"
    },
    {
      "code": "ORDER_MANAGE",
      "name": "订单管理权限",
      "description": "允许管理订单",
      "level": "FULL"
    }
  ],
  "expiresAt": "2026-05-27T23:59:59Z"
}
```

**申请权限提升:**
```bash
# 申请新的权限
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/permissions/request \
  -d '{
    "permission": "INVENTORY_MANAGE",
    "reason": "需要进行库存管理测试",
    "duration": "14d",
    "approver": "test-manager@ai-ready.com",
    "urgency": "HIGH",
    "testPlan": "Sprint 27+1库存模块功能测试"
  }'

# 查看申请状态
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/permissions/requests
```

### 2.2 订单管理功能

#### 2.2.1 创建订单

**Web界面操作:**
1. 登录系统，进入"订单管理"模块
2. 点击"新建订单"按钮
3. 选择产品:
   - 在产品列表中搜索或选择产品
   - 输入购买数量
   - 查看产品详情和库存信息
4. 填写收货信息:
   - 选择收货地址或输入新地址
   - 填写联系人信息
   - 选择配送方式
5. 选择支付方式:
   - 在线支付（支付宝、微信支付）
   - 货到付款
   - 公司转账
6. 填写备注信息（可选）
7. 点击"提交订单"按钮
8. 查看订单确认页面

**API调用示例:**
```bash
# 创建订单
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/orders \
  -d '{
    "items": [
      {
        "productId": "prod_123456",
        "sku": "TEST-A-001",
        "name": "测试产品A",
        "quantity": 2,
        "unitPrice": 99.99,
        "totalPrice": 199.98
      },
      {
        "productId": "prod_789012",
        "sku": "TEST-B-002",
        "name": "测试产品B",
        "quantity": 1,
        "unitPrice": 199.99,
        "totalPrice": 199.99
      }
    ],
    "shippingAddress": {
      "recipient": "张三",
      "phone": "13800138000",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detail": "建国路88号SOHO现代城A座",
      "postalCode": "100022"
    },
    "paymentMethod": "ALIPAY",
    "shippingMethod": "EXPRESS",
    "remarks": "测试订单，请优先处理",
    "invoiceRequired": true,
    "invoiceTitle": "AI-Ready科技有限公司"
  }'

# 返回示例:
{
  "orderId": "ORD202404270001",
  "orderNumber": "TEST-ORDER-20240427-001",
  "status": "PENDING_PAYMENT",
  "totalAmount": 399.97,
  "items": [
    {
      "productId": "prod_123456",
      "name": "测试产品A",
      "quantity": 2,
      "unitPrice": 99.99,
      "totalPrice": 199.98
    }
  ],
  "createdAt": "2026-04-27T15:00:00Z",
  "paymentUrl": "http://test-env.ai-ready.com:3000/payment/ORD202404270001"
}
```

#### 2.2.2 查询订单

**按订单号查询:**
```bash
# 查询单个订单
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/orders/ORD202404270001

# 返回示例:
{
  "orderId": "ORD202404270001",
  "orderNumber": "TEST-ORDER-20240427-001",
  "userId": "user_1234567890",
  "username": "test_user_001",
  "status": "PENDING_PAYMENT",
  "totalAmount": 399.97,
  "items": [...],
  "shippingAddress": {...},
  "paymentMethod": "ALIPAY",
  "paymentStatus": "UNPAID",
  "shippingStatus": "PENDING",
  "createdAt": "2026-04-27T15:00:00Z",
  "updatedAt": "2026-04-27T15:00:00Z",
  "timeline": [
    {
      "event": "ORDER_CREATED",
      "timestamp": "2026-04-27T15:00:00Z",
      "description": "订单创建成功"
    }
  ]
}
```

**按条件查询订单列表:**
```bash
# 查询订单列表
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  "http://test-env.ai-ready.com:8080/api/v1/orders?status=PENDING&startDate=2026-04-01&endDate=2026-04-27&page=1&size=20&sort=createTime,desc"

# 返回示例:
{
  "total": 45,
  "page": 1,
  "size": 20,
  "pages": 3,
  "items": [
    {
      "orderId": "ORD202404270001",
      "orderNumber": "TEST-ORDER-20240427-001",
      "status": "PENDING_PAYMENT",
      "totalAmount": 399.97,
      "itemCount": 2,
      "createdAt": "2026-04-27T15:00:00Z"
    },
    ...
  ]
}
```

#### 2.2.3 订单状态管理

**取消订单:**
```bash
# 取消订单
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/orders/ORD202404270001/cancel \
  -d '{
    "reason": "测试取消订单",
    "reasonCode": "USER_CANCEL",
    "refundRequired": true
  }'
```

**更新订单状态:**
```bash
# 更新订单状态（管理员操作）
curl -X PUT \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/admin/orders/ORD202404270001/status \
  -d '{
    "status": "SHIPPED",
    "trackingNumber": "SF1234567890",
    "carrier": "顺丰速运",
    "shippedAt": "2026-04-27T16:00:00Z",
    "estimatedDelivery": "2026-04-29T18:00:00Z"
  }'
```

### 2.3 库存管理功能

#### 2.3.1 库存查询

**查询产品库存:**
```bash
# 查询单个产品库存
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/products/prod_123456

# 返回示例:
{
  "productId": "prod_123456",
  "sku": "TEST-A-001",
  "name": "测试产品A",
  "category": "电子产品",
  "currentStock": 950,
  "availableStock": 900,
  "reservedStock": 50,
  "safetyStock": 100,
  "reorderPoint": 200,
  "unit": "个",
  "locations": [
    {
      "warehouseId": "wh_001",
      "warehouseName": "北京仓库",
      "quantity": 600,
      "available": 550
    },
    {
      "warehouseId": "wh_002",
      "warehouseName": "上海仓库",
      "quantity": 350,
      "available": 350
    }
  ],
  "lastUpdated": "2026-04-27T14:30:00Z"
}
```

**批量查询库存:**
```bash
# 批量查询库存
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/batch \
  -d '{
    "productIds": ["prod_123456", "prod_789012", "prod_345678"],
    "includeReserved": true,
    "includeLocations": true
  }'
```

#### 2.3.2 库存调整

**增加库存（入库）:**
```bash
# 库存入库
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/adjust \
  -d '{
    "type": "IN",
    "productId": "prod_123456",
    "quantity": 100,
    "warehouseId": "wh_001",
    "reason": "采购入库",
    "referenceNo": "PO20240427001",
    "batchNo": "BATCH20240427001",
    "expiryDate": "2027-04-27",
    "unitCost": 50.00,
    "operator": "test_user_001",
    "remarks": "测试采购入库"
  }'
```

**减少库存（出库）:**
```bash
# 库存出库
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/adjust \
  -d '{
    "type": "OUT",
    "productId": "prod_123456",
    "quantity": 10,
    "warehouseId": "wh_001",
    "reason": "销售出库",
    "referenceNo": "SO20240427001",
    "orderId": "ORD202404270001",
    "operator": "test_user_001",
    "remarks": "测试销售出库"
  }'
```

### 2.4 采购管理功能

#### 2.4.1 创建采购单

```bash
# 创建采购单
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/purchase-orders \
  -d '{
    "supplierId": "supp_456789",
    "supplierName": "测试供应商A",
    "items": [
      {
        "productId": "prod_123456",
        "sku": "TEST-A-001",
        "name": "测试产品A",
        "quantity": 100,
        "unitPrice": 25.50,
        "totalPrice": 2550.00,
        "deliveryDate": "2026-04-30",
        "specifications": "标准规格"
      }
    ],
    "totalAmount": 2550.00,
    "taxRate": 0.13,
    "taxAmount": 331.50,
    "grandTotal": 2881.50,
    "paymentTerms": "NET30",
    "deliveryTerms": "FOB",
    "deliveryAddress": "上海市浦东新区张江高科技园区",
    "contactPerson": "李四",
    "contactPhone": "13900000000",
    "remarks": "测试采购单，用于Sprint 27+1测试",
    "attachments