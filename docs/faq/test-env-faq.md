# Sprint 27+1 测试环境常见问题（FAQ）

> **文档版本**: v1.0
> **创建日期**: 2026-04-27
> **适用对象**: 测试人员、开发人员、运维人员
> **Sprint**: Sprint 27+1
> **最后更新**: 2026-04-27

## 📋 目录

- [1. 环境访问问题](#1-环境访问问题)
  - [1.1 无法访问测试环境](#11-无法访问测试环境)
  - [1.2 登录问题](#12-登录问题)
  - [1.3 权限问题](#13-权限问题)
  - [1.4 网络连接问题](#14-网络连接问题)
- [2. 功能使用问题](#2-功能使用问题)
  - [2.1 用户管理功能问题](#21-用户管理功能问题)
  - [2.2 订单管理功能问题](#22-订单管理功能问题)
  - [2.3 库存管理功能问题](#23-库存管理功能问题)
  - [2.4 采购管理功能问题](#24-采购管理功能问题)
  - [2.5 财务管理功能问题](#25-财务管理功能问题)
- [3. API接口问题](#3-api接口问题)
  - [3.1 API调用失败](#31-api调用失败)
  - [3.2 认证授权问题](#32-认证授权问题)
  - [3.3 参数验证问题](#33-参数验证问题)
  - [3.4 响应格式问题](#34-响应格式问题)
- [4. 数据问题](#4-数据问题)
  - [4.1 数据查询失败](#41-数据查询失败)
  - [4.2 数据不一致](#42-数据不一致)
  - [4.3 数据导入导出问题](#43-数据导入导出问题)
  - [4.4 数据备份恢复问题](#44-数据备份恢复问题)
- [5. 性能问题](#5-性能问题)
  - [5.1 响应时间慢](#51-响应时间慢)
  - [5.2 高并发问题](#52-高并发问题)
  - [5.3 内存泄漏问题](#53-内存泄漏问题)
  - [5.4 数据库性能问题](#54-数据库性能问题)
- [6. 部署和配置问题](#6-部署和配置问题)
  - [6.1 服务启动失败](#61-服务启动失败)
  - [6.2 配置错误](#62-配置错误)
  - [6.3 端口冲突](#63-端口冲突)
  - [6.4 依赖服务问题](#64-依赖服务问题)
- [7. 监控和日志问题](#7-监控和日志问题)
  - [7.1 监控指标缺失](#71-监控指标缺失)
  - [7.2 日志不显示](#72-日志不显示)
  - [7.3 告警不触发](#73-告警不触发)
  - [7.4 仪表板问题](#74-仪表板问题)
- [8. 安全和合规问题](#8-安全和合规问题)
  - [8.1 安全漏洞](#81-安全漏洞)
  - [8.2 合规性问题](#82-合规性问题)
  - [8.3 数据加密问题](#83-数据加密问题)
  - [8.4 访问控制问题](#84-访问控制问题)
- [9. 紧急问题处理](#9-紧急问题处理)
  - [9.1 服务宕机](#91-服务宕机)
  - [9.2 数据丢失](#92-数据丢失)
  - [9.3 安全事件](#93-安全事件)
  - [9.4 网络攻击](#94-网络攻击)
- [10. 其他问题](#10-其他问题)
  - [10.1 浏览器兼容性问题](#101-浏览器兼容性问题)
  - [10.2 移动端问题](#102-移动端问题)
  - [10.3 第三方集成问题](#103-第三方集成问题)
  - [10.4 版本兼容性问题](#104-版本兼容性问题)

---

## 1. 环境访问问题

### 1.1 无法访问测试环境

#### 问题: 无法通过浏览器访问测试环境
**错误现象:**
```
无法访问此网站
test-env.ai-ready.com 的响应时间过长。
```

**可能原因:**
1. 网络连接问题
2. DNS解析失败
3. 防火墙阻挡
4. 服务未启动

**解决方案:**

**步骤1: 检查网络连接**
```bash
# 测试网络连通性
ping -c 4 test-env.ai-ready.com

# 如果ping不通，检查本地网络
ping -c 4 8.8.8.8
```

**步骤2: 检查DNS解析**
```bash
# 使用nslookup检查DNS解析
nslookup test-env.ai-ready.com

# 使用dig检查DNS解析
dig test-env.ai-ready.com

# 如果DNS解析失败，尝试使用IP地址访问
# 测试环境IP地址: 192.168.1.100
```

**步骤3: 检查防火墙**
```bash
# Windows系统检查防火墙
netsh advfirewall show allprofiles

# Linux系统检查防火墙
sudo ufw status

# 临时禁用防火墙测试
sudo ufw disable
# 测试访问
curl http://test-env.ai-ready.com:3000
# 重新启用防火墙
sudo ufw enable
```

**步骤4: 检查服务状态**
```bash
# 检查前端服务状态
curl -I http://test-env.ai-ready.com:3000

# 检查API服务状态
curl -I http://test-env.ai-ready.com:8080

# 如果服务未启动，联系运维人员
```

**步骤5: 使用VPN访问**
```bash
# 如果从外部网络访问，需要连接VPN
# VPN配置请联系IT部门
```

#### 问题: 访问时出现证书错误
**错误现象:**
```
您的连接不是私密连接
攻击者可能会试图从 test-env.ai-ready.com 窃取您的信息
```

**解决方案:**
```bash
# 方案1: 接受风险继续访问
# 在浏览器中点击"高级" -> "继续前往"

# 方案2: 导入测试证书
# 下载测试证书: http://test-env.ai-ready.com:8080/certificate
# 导入到浏览器信任存储

# 方案3: 使用HTTP访问
http://test-env.ai-ready.com:3000
```

### 1.2 登录问题

#### 问题: 用户名或密码错误
**错误现象:**
```
401 Unauthorized
Invalid username or password
```

**解决方案:**

**步骤1: 检查用户名和密码**
```bash
# 确认用户名正确
# 测试环境默认账号:
# 用户名: test_user
# 密码: Test123!@#

# 如果忘记密码，使用密码重置功能
```

**步骤2: 检查账号状态**
```bash
# 使用管理员账号检查用户状态
# 需要管理员权限
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/users/test_user/status
```

**步骤3: 解锁账号**
```bash
# 如果账号被锁定（密码错误超过5次）
# 等待30分钟自动解锁，或联系管理员解锁

# 管理员解锁账号
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/users/test_user/unlock
```

**步骤4: 重置密码**
```bash
# 申请密码重置
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/password/reset/request \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "email": "test@ai-ready.com"}'

# 查收重置邮件，按照指引重置密码
```

#### 问题: 登录后立即退出
**错误现象:**
登录成功，但几秒钟后自动退出登录状态。

**解决方案:**

**步骤1: 检查会话超时配置**
```bash
# 查看会话配置
curl http://test-env.ai-ready.com:8080/actuator/configprops | grep -A5 -B5 session

# 默认配置:
# 普通会话: 30分钟超时
# 记住我会话: 7天超时
```

**步骤2: 检查浏览器设置**
```javascript
// 检查浏览器Cookie设置
// Chrome: 设置 → 隐私和安全 → Cookie和其他网站数据
// Firefox: 选项 → 隐私与安全 → Cookie和网站数据

// 确保允许Cookie
// 确保没有启用"阻止第三方Cookie"
```

**步骤3: 清除浏览器缓存**
```bash
# 清除浏览器缓存和Cookie
# Chrome: Ctrl+Shift+Delete
# Firefox: Ctrl+Shift+Delete

# 然后重新登录
```

**步骤4: 使用无痕模式测试**
```bash
# 使用浏览器的无痕/隐私模式测试
# 排除浏览器插件干扰
```

### 1.3 权限问题

#### 问题: 无权限访问功能
**错误现象:**
```
403 Forbidden
Access denied. You don't have permission to access this resource.
```

**解决方案:**

**步骤1: 检查当前权限**
```bash
# 查看当前用户权限
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/users/me/permissions
```

**步骤2: 检查功能所需权限**
```bash
# 查看API端点权限要求
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/endpoints/permissions
```

**步骤3: 申请权限**
```bash
# 申请所需权限
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/permissions/request \
  -d '{
    "permission": "ORDER_MANAGE",
    "reason": "需要进行订单管理测试",
    "duration": "14d",
    "approver": "test-manager@ai-ready.com"
  }'
```

**步骤4: 联系管理员**
```bash
# 如果急需权限，联系环境管理员
# 管理员邮箱: test-env-admin@ai-ready.com
# 紧急电话: +86-xxx-xxxx-xxxx
```

#### 问题: 权限突然失效
**错误现象:**
之前可以正常访问的功能，突然提示无权限。

**解决方案:**

**步骤1: 检查权限有效期**
```bash
# 查看权限过期时间
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/users/me/permissions/expiry
```

**步骤2: 检查权限变更记录**
```bash
# 查看权限变更历史（需要管理员权限）
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  "http://test-env.ai-ready.com:8080/api/v1/admin/audit/permissions?userId=your_user_id"
```

**步骤3: 重新申请权限**
```bash
# 如果权限已过期，重新申请
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/permissions/request \
  -d '{
    "permission": "需要重新申请的权限",
    "reason": "权限已过期，需要续期",
    "duration": "30d"
  }'
```

### 1.4 网络连接问题

#### 问题: 容器间网络不通
**错误现象:**
```
java.net.UnknownHostException: postgres
或
Connection refused: postgres:5432
```

**解决方案:**

**步骤1: 检查网络配置**
```bash
# 查看Docker网络
docker network ls

# 查看网络详情
docker network inspect ai-ready-test-env_default
```

**步骤2: 测试容器间连通性**
```bash
# 从API服务测试连接数据库
docker-compose exec api-service ping -c 4 postgres

# 测试端口连通性
docker-compose exec api-service nc -zv postgres 5432
```

**步骤3: 检查DNS解析**
```bash
# 测试DNS解析
docker-compose exec api-service nslookup postgres

# 查看DNS配置
docker-compose exec api-service cat /etc/resolv.conf
```

**步骤4: 重启网络**
```bash
# 重建网络
docker-compose down
docker network prune
docker-compose up -d
```

#### 问题: 外部访问失败
**错误现象:**
可以从容器内部访问服务，但无法从外部访问。

**解决方案:**

**步骤1: 检查端口映射**
```bash
# 查看端口映射
docker-compose ps
docker-compose port api-service 8080

# 检查容器端口监听
docker-compose exec api-service netstat -tulpn | grep 8080
```

**步骤2: 检查防火墙规则**
```bash
# 查看防火墙状态
sudo ufw status

# 临时禁用防火墙测试
sudo ufw disable
# 测试访问
curl http://localhost:8080/health
# 重新启用防火墙
sudo ufw enable

# 添加防火墙规则
sudo ufw allow 8080/tcp
```

**步骤3: 检查路由配置**
```bash
# 查看路由表
route -n

# 测试路由
traceroute test-env.ai-ready.com
```

## 2. 功能使用问题

### 2.1 用户管理功能问题

#### 问题: 用户注册失败
**错误现象:**
注册时提示"用户名已存在"或"邮箱已注册"。

**解决方案:**

**步骤1: 检查用户名是否唯一**
```bash
# 检查用户名是否已存在
curl -X GET \
  "http://test-env.ai-ready.com:8080/api/v1/users/check-username?username=test_user"
```

**步骤2: 检查邮箱是否已注册**
```bash
# 检查邮箱是否已注册
curl -X GET \
  "http://test-env.ai-ready.com:8080/api/v1/users/check-email?email=test@ai-ready.com"
```

**步骤3: 使用其他用户名或邮箱**
```bash
# 建议使用带时间戳的用户名
test_user_$(date +%s)
# 例如: test_user_1714215600

# 建议使用测试专用邮箱
test+$(date +%s)@ai-ready.com
# 例如: test+1714215600@ai-ready.com
```

**步骤4: 联系管理员清理测试数据**
```bash
# 如果确实需要重复测试，联系管理员清理测试数据
# 管理员邮箱: test-env-admin@ai-ready.com
```

#### 问题: 用户信息无法更新
**错误现象:**
更新用户信息时提示"更新失败"或没有错误但信息未更新。

**解决方案:**

**步骤1: 检查请求参数**
```bash
# 确认请求参数格式正确
curl -X PUT \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/users/me \
  -d '{
    "nickname": "新昵称",
    "phone": "13900000000"
  }'
```

**步骤2: 检查字段验证规则**
```bash
# 查看用户信息验证规则
# 常见验证规则:
# - 昵称: 1-20个字符
# - 手机号: 11位数字
# - 邮箱: 必须符合邮箱格式
```

**步骤3: 查看错误详情**
```bash
# 启用详细日志查看具体错误
curl -v -X PUT \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/users/me \
  -d '{"nickname": "新昵称"}'
```

**步骤4: 检查数据库连接**
```bash
# 检查数据库服务状态
curl http://test-env.ai-ready.com:8080/actuator/health | jq .components.db
```

### 2.2 订单管理功能问题

#### 问题: 创建订单失败
**错误现象:**
创建订单时提示"库存不足"或"产品不存在"。

**解决方案:**

**步骤1: 检查产品库存**
```bash
# 查询产品库存
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/products/prod_123456
```

**步骤2: 检查产品状态**
```bash
# 查询产品信息
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/products/prod_123456
```

**步骤3: 使用测试专用产品**
```bash
# 使用专为测试设计的产品
# 测试产品SKU以TEST-开头
# 例如: TEST-A-001, TEST-B-002, TEST-C-003

# 这些产品有充足的库存，可以随意测试
```

**步骤4: 补充测试库存**
```bash
# 如果测试库存不足，可以补充库存
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/admin/inventory/adjust \
  -d '{
    "type": "IN",
    "productId": "prod_123456",
    "quantity": 1000,
    "reason": "补充测试库存"
  }'
```

#### 问题: 订单状态无法更新
**错误现象:**
订单状态卡在某个阶段，无法推进到下一步。

**解决方案:**

**步骤1: 检查订单当前状态**
```bash
# 查询订单详情
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/orders/ORD202404270001
```

**步骤2: 检查状态流转规则**
```bash
# 订单状态流转规则:
# PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED → COMPLETED
#                ↘ CANCELLED

# 每个状态都有前置条件:
# - PAID: 需要支付成功
# - PROCESSING: 需要库存充足
# - SHIPPED: 需要物流信息
# - DELIVERED: 需要确认收货
```

**步骤3: 完成前置条件**
```bash
# 如果订单卡在PENDING_PAYMENT，需要模拟支付
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/payments/simulate/ORD202404270001

# 如果订单卡在PROCESSING，检查库存
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/check/ORD202404270001
```

**步骤4: 手动更新状态（管理员）**
```bash
# 管理员可以手动更新订单状态
curl -X PUT \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/admin/orders/ORD202404270001/status \
  -d '{"status": "SHIPPED"}'
```

### 2.3 库存管理功能问题

#### 问题: 库存查询结果不一致
**错误现象:**
不同时间查询同一产品的库存，结果不一致。

**解决方案:**

**步骤1: 检查缓存一致性**
```bash
# 清除缓存重新查询
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/cache/clear/inventory/prod_123456

# 重新查询
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/products/prod_123456
```

**步骤2: 检查事务隔离级别**
```bash
# 查看数据库事务日志
# 检查是否有未提交的事务影响查询结果

# 使用数据库管理工具查看
docker-compose exec postgres psql -U ai_ready_user -d ai_ready_test \
  -c "SELECT * FROM pg_stat_activity WHERE state = 'idle in transaction';"
```

**步骤3: 检查数据同步延迟**
```bash
# 检查数据同步状态
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/data-sync/status
```

**步骤4: 强制刷新数据**
```bash
# 强制刷新库存数据
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/inventory/refresh/prod_123456
```

#### 问题: 库存调整失败
**错误现象:**
库存调整时提示"调整失败"或"库存不足"。

**解决方案:**

**步骤1: 检查当前库存**
```bash
# 查询当前库存详情
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/detail/prod_123456
```

**步骤2: 检查调整参数**
```bash
# 确认调整参数正确
# type: IN(入库)或OUT(出库)
# quantity: 调整数量（正数）
# warehouseId: 仓库ID
# reason: 调整原因

curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/adjust \
  -d '{
    "type": "IN",
    "productId": "prod_123456",
    "quantity": 100,
    "warehouseId": "wh_001",
    "reason": "测试入库"
  }'
```

**步骤3: 检查并发冲突**
```bash
# 查看是否有其他操作正在修改同一库存
docker-compose exec postgres psql -U ai_ready_user -d ai_ready_test \
  -c "SELECT locked_pid, locked_relation FROM pg_locks WHERE relation = 'inventory'::regclass;"
```

**步骤4: 使用乐观锁重试**
```bash
# 获取当前版本号
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/version/prod_123456

# 使用版本号进行更新
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/inventory/adjust \
  -d '{
    "type": "IN",
    "productId": "prod_123456",
    "quantity": 100,
    "version": 123,
    "reason": "测试入库"
  }'
```

### 2.4 采购管理功能问题

#### 问题: 采购单创建失败
**错误现象:**
创建采购单时提示"供应商不存在"或"产品信息错误"。

**解决方案:**

**步骤1: 检查供应商信息**
```bash
# 查询供应商列表
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  "http://test-env.ai-ready.com:8080/api/v1/suppliers?status=ACTIVE"
```

**步骤2: 使用测试供应商**
```bash
# 使用专为测试设计的供应商
# 测试供应商ID以supp_test_开头
# 例如: supp_test_001, supp_test_002

# 这些供应商有完整的配置，可以用于测试
```

**步骤3: 检查产品信息**
```bash
# 查询产品信息
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/products/prod_123456
```

**步骤4: 创建测试采购单模板**
```bash
# 使用标准的测试采购单模板
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/purchase-orders/template \
  -d '{
    "template": "TEST_PURCHASE_ORDER",
    "params": {
      "supplierId": "supp_test_001",
      "productId": "prod_123456",
      "quantity": 100
    }
  }'
```

#### 问题: 采购单审批流程卡住
**错误现象:**
采购单提交审批后，长时间没有进展。

**解决方案:**

**步骤1: 检查审批流程状态**
```bash
# 查询采购单审批状态
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/purchase-orders/PO20240427001/approval-status
```

**步骤2: 检查审批人配置**
```bash
# 查看审批流程配置
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/approval/workflows/purchase-order
```

**步骤3: 模拟审批操作**
```bash
# 使用测试账号模拟审批
# 测试审批人账号: approver_test
# 密码: Test123!@#

# 登录审批人账号
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "approver_test", "password": "Test123!@#"}'

# 审批采购单
curl -X POST \
  -H "Authorization: Bearer <approver_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/approvals/purchase-orders/PO20240427001/approve \
  -d '{"comment": "测试审批通过"}'
```

**步骤4: 跳过审批流程（测试环境）**
```bash
# 在测试环境，可以配置跳过审批
# 修改测试环境配置:
# approval.workflow.purchase-order.enabled=false

# 或者使用管理员权限直接通过
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/purchase-orders/PO20240427001/force-approve
```

### 2.5 财务管理功能问题

#### 问题: 发票创建失败
**错误现象:**
创建发票时提示"订单信息错误"或"金额不匹配"。

**解决方案:**

**步骤1: 检查订单状态**
```bash
# 查询订单详情
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/orders/ORD202404270001
```

**步骤2: 检查订单金额**
```bash
# 确认订单金额与发票金额匹配
# 订单总金额: 399.97
# 发票金额必须与订单金额一致
```

**步骤3: 使用测试发票模板**
```bash
# 使用测试发票模板
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/invoices/template \
  -d '{
    "template": "TEST_INVOICE",
    "orderId": "ORD202404270001",
    "invoiceType": "SALES"
  }'
```

**步骤4: 检查税务配置**
```bash
# 查看税务配置
curl -X GET \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/tax/config

# 测试环境默认税率: 13%
```

#### 问题: 财务报表生成失败
**错误现象:**
生成财务报表时提示"数据不足"或"时间范围错误"。

**解决方案:**

**步骤1: 检查时间范围**
```bash
# 确认时间范围合理
# 开始时间不能晚于结束时间
# 时间范围不能超过365天

curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/finance/reports \
  -d '{
    "reportType": "INCOME_STATEMENT",
    "startDate": "2026-04-01",
    "endDate": "2026-04-27",
    "currency": "CNY"
  }'
```

**步骤2: 生成测试数据**
```bash
# 如果数据不足，生成测试数据
curl -X POST \
  -H "Authorization: Bearer <admin_token>" \
  http://test-env.ai-ready.com:8080/api/v1/admin/finance/generate-test-data \
  -d '{
    "startDate": "2026-04-01",
    "endDate": "2026-04-27",
    "transactionCount": 100
  }'
```

**步骤3: 使用预先生成的报告**
```bash
# 使用预先生成的测试报告
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  "http://test-env.ai-ready.com:8080/api/v1/finance/reports/sample?type=INCOME_STATEMENT"
```

**步骤4: 调整报告参数**
```bash
# 调整报告参数，减少数据量
curl -X POST \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  http://test-env.ai-ready.com:8080/api/v1/finance/reports \
  -d '{
    "reportType": "INCOME_STATEMENT",
    "startDate": "2026-04-26",
    "endDate": "2026-04-27",
    "currency": "CNY",
    "includeDetails": false
  }'
```

## 3. API接口问题

### 3.1 API调用失败

#### 问题: API返回500错误
**错误现象:**
```
500 Internal Server Error
```

**解决方案:**

**步骤1: 查看错误详情**
```bash
# 启用详细输出查看错误
curl -v -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/orders

# 或者查看响应头中的错误信息
```

**步骤2: 检查服务日志**
```bash
# 查看API服务日志
docker-compose logs api-service --tail=50

# 搜索错误信息
docker-compose logs api-service | grep -i "error\|exception"
```

**步骤3: 检查依赖服务**
```bash
# 检查数据库服务
curl http://test-env.ai-ready.com:8080/actuator/health | jq .components.db

# 检查Redis服务
curl http://test-env.ai-ready.com:8080/actuator/health | jq .components.redis
```

**步骤4: 简化请求测试**
```bash
# 使用最简单的请求测试
curl -X GET \
  http://test-env.ai-ready.com:8080/actuator/health

# 如果健康检查通过，说明基础服务正常
# 问题可能出在具体业务逻辑
```

#### 问题: API返回404错误
**错误现象:**
```
404 Not Found
```

**解决方案:**

**步骤1: 检查API路径**
```bash
# 确认API路径正确
# 正确的API路径示例:
http://test-env.ai-ready.com:8080/api/v1/orders
http://test-env.ai-ready.com:8080/api/v1/users

# 查看API文档确认路径
http://test-env.ai-ready.com:8080/swagger-ui.html
```

**步骤2: 检查API版本**
```bash
# 确认使用正确的API版本
# 当前版本: v1

# 错误的路径:
http://test-env.ai-ready.com:8080/api/orders  # 缺少版本号
http://test-env.ai-ready.com:8080/api/v2/orders  # 错误的版本号
```

**步骤3: 检查资源是否存在**
```bash
# 确认请求的资源存在
# 例如，查询不存在的订单
curl -X GET \
  -H "Authorization: Bearer <your_token>" \
  http://test-env.ai-ready.com:8080/api/v1/orders/NOT_EXIST

# 应该返回404，这是正常行为
```

**步骤4: 查看API路由表**
```bash
# 查看所有可用的API端点
curl -X GET \
  http://test-env.ai-ready.com:8080/actuator/mappings | jq '.contexts."application".mappings.dispatcherServlets.dispatcherServlet[]'
```

### 3.2 认证授权问题

#### 问题: 令牌无效或过期
**错误现象:**
```
401 Unauthorized
Invalid or expired token
```

**解决方案:**

**步骤1: 检查令牌格式**
```bash
# 确认令牌格式正确
# 正确的格式: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 错误的格式示例:
Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  # 缺少Bearer前缀
Authorization: Bearer   # 令牌为空
```

**步骤2: 检查令牌有效期**
```bash
# 验证令牌是否过期
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/token/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}'

# 如果过期，重新获取令牌
```

**步骤3: 重新获取访问令牌**
```bash
# 使用刷新令牌获取新的访问令牌
curl -X POST \
  http://test-env.ai-ready.com:8080/api/v1/auth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9