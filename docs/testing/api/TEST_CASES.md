# 测试环境API测试用例

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [测试环境准备](#测试环境准备)
3. [认证授权测试用例](#认证授权测试用例)
4. [用户管理测试用例](#用户管理测试用例)
5. [订单管理测试用例](#订单管理测试用例)
6. [库存管理测试用例](#库存管理测试用例)
7. [支付管理测试用例](#支付管理测试用例)
8. [性能测试用例](#性能测试用例)
9. [安全测试用例](#安全测试用例)
10. [自动化测试脚本](#自动化测试脚本)
11. [测试报告模板](#测试报告模板)

---

## 概述

### 测试目标

本文档提供测试环境API的完整测试用例，旨在：

1. **功能验证**: 确保API功能符合设计规范
2. **接口稳定性**: 验证API接口的稳定性和可靠性
3. **性能基准**: 建立API性能基准和响应时间标准
4. **安全合规**: 验证API安全控制和权限管理
5. **兼容性测试**: 确保API与不同客户端的兼容性

### 测试覆盖范围

| 测试类型 | 测试重点 | 测试工具 | 执行频率 |
|----------|----------|----------|----------|
| **功能测试** | API业务逻辑、参数验证 | Postman, curl | 每次部署 |
| **集成测试** | 服务间调用、数据一致性 | Postman, 脚本 | 每次部署 |
| **性能测试** | 响应时间、吞吐量、并发 | JMeter, k6 | 每周一次 |
| **安全测试** | 认证、授权、输入验证 | OWASP ZAP, Burp | 每月一次 |
| **兼容性测试** | 不同客户端、版本 | 多版本客户端 | 每季度一次 |
| **回归测试** | 已有功能验证 | 自动化脚本 | 每次代码变更 |

### 测试数据管理

#### 测试数据分类
```yaml
test_data:
  # 基础测试数据
  basic:
    - 预置用户账户
    - 预置产品数据
    - 预置库存数据
    - 预置订单模板
    
  # 边界测试数据
  boundary:
    - 最大/最小值
    - 空值/Null值
    - 特殊字符
    - 超长字符串
    
  # 异常测试数据
  abnormal:
    - 错误格式数据
    - 缺失必需字段
    - 数据类型错误
    - 业务逻辑异常
    
  # 性能测试数据
  performance:
    - 大批量数据
    - 高并发数据
    - 长时间运行数据
```

#### 测试数据维护
```bash
# 初始化测试数据
./scripts/init-test-data.sh --env test --clean

# 备份测试数据
./scripts/backup-test-data.sh --backup-dir /backup/test-data

# 恢复测试数据
./scripts/restore-test-data.sh --backup-file test-data-2026-04-27.tar.gz

# 清理测试数据
./scripts/clean-test-data.sh --older-than 7d
```

## 测试环境准备

### 环境检查清单

#### 1. 基础环境检查
```bash
# 检查网络连通性
ping test-env.ai-ready.local
telnet test-env.ai-ready.local 8080

# 检查服务状态
curl -X GET "http://test-env.ai-ready.local:8080/actuator/health"

# 检查数据库连接
docker exec test-env-postgres pg_isready -U postgres

# 检查Redis连接
docker exec test-env-redis redis-cli ping
```

#### 2. 环境变量配置
```bash
# 测试环境变量
export TEST_ENV_HOST="test-env.ai-ready.local"
export TEST_ENV_PORT="8080"
export TEST_BASE_URL="http://${TEST_ENV_HOST}:${TEST_ENV_PORT}"
export TEST_API_VERSION="v1"

# 测试用户凭证
export TEST_ADMIN_USERNAME="admin@ai-ready.local"
export TEST_ADMIN_PASSWORD="Admin@2024"
export TEST_USER_USERNAME="tester@ai-ready.local"
export TEST_USER_PASSWORD="Tester@2024"

# 测试数据路径
export TEST_DATA_DIR="/opt/ai-ready/test-data"
export TEST_REPORT_DIR="/opt/ai-ready/test-reports"
```

#### 3. 测试工具准备
```bash
# 安装必要工具
sudo apt-get install -y curl jq postgresql-client redis-tools

# 安装测试框架
pip install pytest requests pytest-html allure-pytest

# 安装性能测试工具
sudo apt-get install -y jmeter

# 安装API测试工具
curl -o- https://get.postman.com/install.sh | bash
```

### 测试执行前检查

#### 健康检查脚本
```bash
#!/bin/bash
# health-check.sh

echo "=== 测试环境健康检查 ==="
echo "检查时间: $(date)"
echo ""

# 1. 检查网络连通性
echo "1. 网络连通性检查"
ping -c 3 test-env.ai-ready.local > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ 网络连通性正常"
else
    echo "✗ 网络连通性异常"
    exit 1
fi

# 2. 检查API服务
echo "2. API服务检查"
API_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://test-env.ai-ready.local:8080/actuator/health)
if [ "$API_HEALTH" = "200" ]; then
    echo "✓ API服务正常"
else
    echo "✗ API服务异常 (状态码: $API_HEALTH)"
    exit 1
fi

# 3. 检查数据库
echo "3. 数据库服务检查"
DB_STATUS=$(docker exec test-env-postgres pg_isready -U postgres 2>/dev/null | grep -c "accepting connections")
if [ "$DB_STATUS" -eq 1 ]; then
    echo "✓ 数据库服务正常"
else
    echo "✗ 数据库服务异常"
    exit 1
fi

# 4. 检查Redis
echo "4. Redis服务检查"
REDIS_STATUS=$(docker exec test-env-redis redis-cli ping 2>/dev/null | grep -c "PONG")
if [ "$REDIS_STATUS" -eq 1 ]; then
    echo "✓ Redis服务正常"
else
    echo "✗ Redis服务异常"
    exit 1
fi

# 5. 检查磁盘空间
echo "5. 磁盘空间检查"
DISK_USAGE=$(df -h / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -lt 80 ]; then
    echo "✓ 磁盘空间充足 ($DISK_USAGE%)"
else
    echo "✗ 磁盘空间不足 ($DISK_USAGE%)"
    exit 1
fi

echo ""
echo "=== 所有检查通过，测试环境就绪 ==="
```

#### 环境重置脚本
```bash
#!/bin/bash
# reset-test-environment.sh

echo "正在重置测试环境..."

# 1. 停止所有服务
echo "停止服务..."
docker-compose -f docker-compose.test.yml down

# 2. 清理数据
echo "清理数据..."
sudo rm -rf /opt/ai-ready/data/test/*
sudo rm -rf /opt/ai-ready/logs/test/*

# 3. 启动服务
echo "启动服务..."
docker-compose -f docker-compose.test.yml up -d

# 4. 等待服务就绪
echo "等待服务就绪..."
sleep 30

# 5. 初始化测试数据
echo "初始化测试数据..."
./scripts/init-test-data.sh --env test

# 6. 验证环境
echo "验证环境..."
./scripts/health-check.sh

echo "测试环境重置完成！"
```

## 认证授权测试用例

### 登录认证测试

#### TC-AUTH-001: 用户正常登录
```yaml
test_case: TC-AUTH-001
title: 用户正常登录
priority: High
precondition: 测试用户已存在
test_steps:
  1. 发送登录请求
  2. 验证响应状态码为200
  3. 验证响应包含有效token
  4. 验证用户信息正确
expected_result: 登录成功，返回有效token和用户信息
test_data:
  username: tester@ai-ready.local
  password: Tester@2024
```

```bash
# 测试脚本
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tester@ai-ready.local",
    "password": "Tester@2024"
  }' | jq '.'
```

#### TC-AUTH-002: 密码错误登录
```yaml
test_case: TC-AUTH-002
title: 密码错误登录
priority: High
precondition: 测试用户已存在
test_steps:
  1. 使用错误密码发送登录请求
  2. 验证响应状态码为401
  3. 验证错误信息正确
expected_result: 登录失败，返回认证错误
test_data:
  username: tester@ai-ready.local
  password: WrongPassword123
```

#### TC-AUTH-003: 用户不存在登录
```yaml
test_case: TC-AUTH-003
title: 用户不存在登录
priority: Medium
test_steps:
  1. 使用不存在用户发送登录请求
  2. 验证响应状态码为401
  3. 验证错误信息正确
expected_result: 登录失败，用户不存在错误
test_data:
  username: not_exist_user@test.com
  password: AnyPassword123
```

#### TC-AUTH-004: 空用户名密码登录
```yaml
test_case: TC-AUTH-004
title: 空用户名密码登录
priority: Medium
test_steps:
  1. 发送空用户名和密码的登录请求
  2. 验证响应状态码为400
  3. 验证参数验证错误信息
expected_result: 登录失败，参数验证错误
test_data:
  username: ""
  password: ""
```

### Token验证测试

#### TC-AUTH-005: 有效Token访问受保护资源
```yaml
test_case: TC-AUTH-005
title: 有效Token访问受保护资源
priority: High
precondition: 已获取有效token
test_steps:
  1. 使用有效token访问用户信息接口
  2. 验证响应状态码为200
  3. 验证返回用户信息正确
expected_result: 成功访问受保护资源
```

```bash
# 测试脚本
# 1. 获取token
TOKEN=$(curl -s -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "tester@ai-ready.local", "password": "Tester@2024"}' \
  | jq -r '.data.token')

# 2. 使用token访问
curl -X GET "http://test-env.ai-ready.local:8080/api/v1/users/me" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

#### TC-AUTH-006: 无效Token访问受保护资源
```yaml
test_case: TC-AUTH-006
title: 无效Token访问受保护资源
priority: High
test_steps:
  1. 使用无效token访问用户信息接口
  2. 验证响应状态码为401
  3. 验证错误信息正确
expected_result: 访问失败，认证错误
test_data:
  token: invalid_token_string
```

#### TC-AUTH-007: 过期Token访问受保护资源
```yaml
test_case: TC-AUTH-007
title: 过期Token访问受保护资源
priority: Medium
precondition: 有过期token（可通过修改时间或等待过期）
test_steps:
  1. 使用过期token访问用户信息接口
  2. 验证响应状态码为401
  3. 验证错误信息为token过期
expected_result: 访问失败，token过期错误
```

#### TC-AUTH-008: 无Token访问受保护资源
```yaml
test_case: TC-AUTH-008
title: 无Token访问受保护资源
priority: High
test_steps:
  1. 不提供token访问用户信息接口
  2. 验证响应状态码为401
  3. 验证错误信息正确
expected_result: 访问失败，需要认证
```

### 权限验证测试

#### TC-AUTH-009: 普通用户访问管理员接口
```yaml
test_case: TC-AUTH-009
title: 普通用户访问管理员接口
priority: High
precondition: 普通用户token已获取
test_steps:
  1. 使用普通用户token访问管理员接口
  2. 验证响应状态码为403
  3. 验证错误信息为权限不足
expected_result: 访问失败，权限不足
test_data:
  admin_endpoint: /api/v1/admin/users
```

#### TC-AUTH-010: 管理员用户访问管理员接口
```yaml
test_case: TC-AUTH-010
title: 管理员用户访问管理员接口
priority: High
precondition: 管理员token已获取
test_steps:
  1. 使用管理员token访问管理员接口
  2. 验证响应状态码为200
  3. 验证返回数据正确
expected_result: 成功访问管理员接口
```

#### TC-AUTH-011: 角色权限验证
```yaml
test_case: TC-AUTH-011
title: 角色权限验证
priority: Medium
precondition: 不同角色用户token已获取
test_steps:
  1. 使用不同角色用户token访问相同接口
  2. 验证权限控制正确
  3. 验证返回数据符合角色权限
expected_result: 权限控制正确，不同角色返回不同数据
test_data:
  roles: [ROLE_ADMIN, ROLE_TESTER, ROLE_USER]
  endpoints: 
    - /api/v1/users
    - /api/v1/admin/users
    - /api/v1/orders
```

### Token刷新测试

#### TC-AUTH-012: 有效Refresh Token刷新
```yaml
test_case: TC-AUTH-012
title: 有效Refresh Token刷新
priority: High
precondition: 已获取有效refresh token
test_steps:
  1. 使用有效refresh token请求刷新
  2. 验证响应状态码为200
  3. 验证返回新的access token和refresh token
  4. 验证新token可正常使用
expected_result: 成功刷新token，新token有效
```

```bash
# 测试脚本
# 1. 获取初始token和refresh token
LOGIN_RESPONSE=$(curl -s -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "tester@ai-ready.local", "password": "Tester@2024"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.refreshToken')

# 2. 使用refresh token刷新
REFRESH_RESPONSE=$(curl -s -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.data.token')
```

#### TC-AUTH-013: 无效Refresh Token刷新
```yaml
test_case: TC-AUTH-013
title: 无效Refresh Token刷新
priority: Medium
test_steps:
  1. 使用无效refresh token请求刷新
  2. 验证响应状态码为401
  3. 验证错误信息正确
expected_result: 刷新失败，无效refresh token
```

#### TC-AUTH-014: 过期Refresh Token刷新
```yaml
test_case: TC-AUTH-014
title: 过期Refresh Token刷新
priority: Medium
precondition: 有过期refresh token
test_steps:
  1. 使用过期refresh token请求刷新
  2. 验证响应状态码为401
  3. 验证错误信息为refresh token过期
expected_result: 刷新失败，refresh token过期
```

### 注销测试

#### TC-AUTH-015: 正常注销
```yaml
test_case: TC-AUTH-015
title: 正常注销
priority: High
precondition: 已获取有效token
test_steps:
  1. 使用有效token请求注销
  2. 验证响应状态码为200
  3. 验证注销后token无法使用
  4. 验证refresh token同时失效
expected_result: 成功注销，token失效
```

#### TC-AUTH-016: 注销后再次登录
```yaml
test_case: TC-AUTH-016
title: 注销后再次登录
priority: Medium
precondition: 用户已注销
test_steps:
  1. 注销后使用相同凭证再次登录
  2. 验证登录成功
  3. 验证获取新token
expected_result: 注销后可重新登录获取新token
```

## 用户管理测试用例

### 用户注册测试

#### TC-USER-001: 新用户正常注册
```yaml
test_case: TC-USER-001
title: 新用户正常注册
priority: High
precondition: 测试环境就绪
test_steps:
  1. 发送新用户注册请求
  2. 验证响应状态码为201
  3. 验证返回用户信息正确
  4. 验证用户已创建并可登录
expected_result: 用户注册成功，信息正确
test_data:
  username: new_user_$(date +%s)@test.com
  password: Password123!
  fullName: 测试用户
  email: new_user_$(date +%s)@test.com
```

```bash
# 测试脚本
TIMESTAMP=$(date +%s)
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"test_user_${TIMESTAMP}@test.com\",
    \"password\": \"Password123!\",
    \"email\": \"test_user_${TIMESTAMP}@test.com\",
    \"fullName\": \"测试用户\"
  }" | jq '.'
```

#### TC-USER-002: 用户名已存在注册
```yaml
test_case: TC-USER-002
title: 用户名已存在注册
priority: High
precondition: 测试用户已存在
test_steps:
  1. 使用已存在用户名发送注册请求
  2. 验证响应状态码为409
  3. 验证错误信息为用户已存在
expected_result: 注册失败，用户已存在
test_data:
  username: tester@ai-ready.local  # 已存在用户
  password: Password123!
  email: new_email@test.com
```

#### TC-USER-003: 邮箱已存在注册
```yaml
test_case: TC-USER-003
title: 邮箱已存在注册
priority: High
precondition: 测试邮箱已存在
test_steps:
  1. 使用已存在邮箱发送注册请求
  2. 验证响应状态码为409
  3. 验证错误信息为邮箱已存在
expected_result: 注册失败，邮箱已存在
test_data:
  username: new_user_$(date +%s)@test.com
  password: Password123!
  email: tester@ai-ready.local  # 已存在邮箱
```

#### TC-USER-004: 密码强度不足注册
```yaml
test_case: TC-USER-004
title: 密码强度不足注册
priority: Medium
test_steps:
  1. 使用弱密码发送注册请求
  2. 验证响应状态码为400
  3. 验证错误信息为密码强度不足
expected_result: 注册失败，密码强度不足
test_data:
  weak_passwords:
    - "123456"
    - "password"
    - "abc123"
    - "Test123"  # 缺少特殊字符
```

#### TC-USER-005: 邮箱格式错误注册
```yaml
test_case: TC-USER-005
title: 邮箱格式错误注册
priority: Medium
test_steps:
  1. 使用错误格式邮箱发送注册请求
  2. 验证响应状态码为400
  3. 验证错误信息为邮箱格式错误
expected_result: 注册失败，邮箱格式错误
test_data:
  invalid_emails:
    - "invalid_email"
    - "user@"
    - "@domain.com"
    - "user@domain"
```

### 用户信息查询测试

#### TC-USER-006: 查询当前用户信息
```yaml
test_case: TC-USER-006
title: 查询当前用户信息
priority: High
precondition: 已登录用户
test_steps:
  1. 发送查询当前用户信息请求
  2. 验证响应状态码为200
  3. 验证返回用户信息正确完整
  4. 验证敏感信息已脱敏
expected_result: 返回完整用户信息，敏感信息脱敏
```

#### TC-USER-007: 查询其他用户信息（有权限）
```yaml
test_case: TC-USER-007
title: 查询其他用户信息（有权限）
priority: High
precondition: 管理员用户已登录
test_steps:
  1. 管理员查询其他用户信息
  2. 验证响应状态码为200
  3. 验证返回用户信息正确
expected_result: 成功查询其他用户信息
```

#### TC-USER-008: 查询其他用户信息（无权限）
```yaml
test_case: TC-USER-008
title: 查询其他用户信息（无权限）
priority: High
precondition: 普通用户已登录
test_steps:
  1. 普通用户查询其他用户信息
  2. 验证响应状态码为403
  3. 验证错误信息为权限不足
expected_result: 查询失败，权限不足
```

#### TC-USER-009: 查询不存在用户信息
```yaml
test_case: TC-USER-009
title: 查询不存在用户信息
priority: Medium
precondition: 管理员用户已登录
test_steps:
  1. 查询不存在用户ID
  2. 验证响应状态码为404
  3. 验证错误信息为用户不存在
expected_result: 查询失败，用户不存在
test_data:
  user_id: non_exist_user_123
```

### 用户信息更新测试

#### TC-USER-010: 更新当前用户信息
```yaml
test_case: TC-USER-010
title: 更新当前用户信息
priority: High
precondition: 已登录用户
test_steps:
  1. 发送更新用户信息请求
  2. 验证响应状态码为200
  3. 验证用户信息已更新
  4. 验证返回更新后信息正确
expected_result: 用户信息更新成功
test_data:
  update_fields:
    fullName: "更新后的姓名"
    phone: "13800138000"
    avatar: "https://example.com/new-avatar.jpg"
```

```bash
# 测试脚本
curl -X PUT "http://test-env.ai-ready.local:8080/api/v1/users/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "更新后的姓名",
    "phone": "13800138000",
    "avatar": "https://example.com/new-avatar.jpg"
  }' | jq '.'
```

#### TC-USER-011: 更新其他用户信息（有权限）
```yaml
test_case: TC-USER-011
title: 更新其他用户信息（有权限）
priority: High
precondition: 管理员用户已登录
test_steps:
  1. 管理员更新其他用户信息
  2. 验证响应状态码为200
  3. 验证用户信息已更新
  4. 验证更新记录正确
expected_result: 成功更新其他用户信息
```

#### TC-USER-012: 更新其他用户信息（无权限）
```yaml
test_case: TC-USER-012
title: 更新其他用户信息（无权限）
priority: High
precondition: 普通用户已登录
test_steps:
  1. 普通用户更新其他用户信息
  2. 验证响应状态码为403
  3. 验证错误信息为权限不足
expected_result: 更新失败，权限不足
```

#### TC-USER-013: 更新不存在用户信息
```yaml
test_case: TC-USER-013
title: 更新不存在用户信息
priority: Medium
precondition: 管理员用户已登录
test_steps:
  1. 更新不存在用户ID
  2. 验证响应状态码为404
  3. 验证错误信息为用户不存在
expected_result: 更新失败，用户不存在
```

### 用户状态管理测试

#### TC-USER-014: 禁用用户账户
```yaml
test_case: TC-USER-014
title: 禁用用户账户
priority: High
precondition: 管理员用户已登录，目标用户存在
test_steps:
  1. 管理员禁用用户账户
  2. 验证响应状态码为200
  3. 验证用户状态为DISABLED
  4. 验证禁用后用户无法登录
expected_result: 用户账户禁用成功
```

#### TC-USER-015: 启用用户账户
```yaml
test_case: TC-USER-015
title: 启用用户账户
priority: High
precondition: 管理员用户已登录，目标用户已禁用
test_steps:
  1. 管理员启用用户账户
  2. 验证响应状态码为200
  3. 验证用户状态为ACTIVE
  4. 验证启用后用户可以登录
expected_result: 用户账户启用成功
```

#### TC-USER-016: 锁定用户账户（密码错误多次）
```yaml
test_case: TC-USER-016
title: 锁定用户账户（密码错误多次）
priority: Medium
precondition: 测试用户存在
test_steps:
  1. 连续多次使用错误密码登录
  2. 验证账户被锁定
  3. 验证锁定后正确密码也无法登录
  4. 验证锁定信息正确
expected_result: 账户锁定机制正常工作
```

### 批量用户操作测试

#### TC-USER-017: 批量查询用户
```yaml
test_case: TC-USER-017
title: 批量查询用户
priority: Medium
precondition: 管理员用户已登录，存在多个用户
test_steps:
  1. 发送批量查询用户请求
  2. 验证响应状态码为200
  3. 验证分页信息正确
  4. 验证返回用户列表正确
  5. 测试不同分页参数
expected_result: 批量查询成功，分页功能正常
test_data:
  page_sizes: [10, 20, 50, 100]
  sort_fields: [created_at, username, email]
```

#### TC-USER-018: 批量导出用户
```yaml
test_case: TC-USER-018
title: 批量导出用户
priority: Low
precondition: 管理员用户已登录
test_steps:
  1. 发送批量导出用户请求
  2. 验证响应状态码为200
  3. 验证导出文件格式正确
  4. 验证导出数据完整准确
expected_result: 批量导出成功，数据完整
test_data:
  export_formats: [csv, excel, json]
```

#### TC-USER-019: 批量删除测试用户
```yaml
test_case: TC-USER-019
title: 批量删除测试用户
priority: Medium
precondition: 管理员用户已登录，存在测试用户
test_steps:
  1. 批量删除测试用户
  2. 验证响应状态码为200
  3. 验证用户已删除
  4. 验证删除后无法查询到
expected_result: 批量删除成功
```

## 订单管理测试用例

### 订单创建测试

#### TC-ORDER-001: 创建正常订单
```yaml
test_case: TC-ORDER-001
title: 创建正常订单
priority: High
precondition: 用户已登录，产品库存充足
test_steps:
  1. 发送创建订单请求
  2. 验证响应状态码为201
  3. 验证订单信息正确
  4. 验证库存已扣减
  5. 验证订单状态为PENDING
expected_result: 订单创建成功，信息正确
test_data:
  products:
    - {id: prod_001, quantity: 2}
    - {id: prod_002, quantity: 1}
  shipping_address: 标准测试地址
  payment_method: ALIPAY
```

```bash
# 测试脚本
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "prod_001",
        "quantity": 2
      },
      {
        "productId": "prod_002",
        "quantity": 1
      }
    ],
    "shippingAddress": {
      "recipient": "测试用户",
      "phone": "13800138000",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detail": "测试地址123号"
    },
    "paymentMethod": "ALIPAY"
  }' | jq '.'
```

#### TC-ORDER-002: 创建订单库存不足
```yaml
test_case: TC-ORDER-002
title: 创建订单库存不足
priority: High
precondition: 用户已登录，产品库存不足
test_steps:
  1. 发送创建订单请求（数量超过库存）
  2. 验证响应状态码为400
  3. 验证错误信息为库存不足
  4. 验证库存未扣减
  5. 验证订单未创建
expected_result: 订单创建失败，库存不足
test_data:
  product_id: prod_001
  available_stock: 10
  order_quantity: 15
```

#### TC-ORDER-003: 创建订单产品不存在
```yaml
test_case: TC-ORDER-003
title: 创建订单产品不存在
priority: High
precondition: 用户已登录
test_steps:
  1. 发送创建订单请求（产品不存在）
  2. 验证响应状态码为404
  3. 验证错误信息为产品不存在
  4. 验证订单未创建
expected_result: 订单创建失败，产品不存在
test_data:
  product_id: non_exist_prod_999
  quantity: 1
```

#### TC-ORDER-004: 创建订单数量为零或负数
```yaml
test_case: TC-ORDER-004
title: 创建订单数量为零或负数
priority: Medium
precondition: 用户已登录，产品存在
test_steps:
  1. 发送创建订单请求（数量为0或负数）
  2. 验证响应状态码为400
  3. 验证错误信息为数量无效
  4. 验证订单未创建
expected_result: 订单创建失败，数量无效
test_data:
  invalid_quantities: [0, -1, -10]
```

#### TC-ORDER-005: 创建订单缺失必填字段
```yaml
test_case: TC-ORDER-005
title: 创建订单缺失必填字段
priority: Medium
precondition: 用户已登录
test_steps:
  1. 发送创建订单请求（缺失必填字段）
  2. 验证响应状态码为400
  3. 验证错误信息为字段缺失
  4. 验证订单未创建
expected_result: 订单创建失败，必填字段缺失
test_data:
  missing_fields:
    - items
    - shippingAddress
    - paymentMethod
```

### 订单查询测试

#### TC-ORDER-006: 查询当前用户订单
```yaml
test_case: TC-ORDER-006
title: 查询当前用户订单
priority: High
precondition: 用户已登录，有订单记录
test_steps:
  1. 发送查询用户订单请求
  2. 验证响应状态码为200
  3. 验证返回订单列表正确
  4. 验证分页信息正确
  5. 验证只返回当前用户订单
expected_result: 成功查询用户订单，数据正确
```

#### TC-ORDER-007: 查询订单详情
```yaml
test_case: TC-ORDER-007
title: 查询订单详情
priority: High
precondition: 用户已登录，有订单记录
test_steps:
  1. 发送查询订单详情请求
  2. 验证响应状态码为200
  3. 验证返回订单详情完整
  4. 验证包含订单项和状态信息
expected_result: 成功查询订单详情，信息完整
```

#### TC-ORDER-008: 查询不存在订单
```yaml
test_case: TC-ORDER-008
title: 查询不存在订单
priority: Medium
precondition: 用户已登录
test_steps:
  1. 发送查询不存在订单请求
  2. 验证响应状态码为404
  3. 验证错误信息为订单不存在
expected_result: 查询失败，订单不存在
test_data:
  order_id: non_exist_order_999
```

#### TC-ORDER-009: 查询其他用户订单（无权限）
```yaml
test_case: TC-ORDER-009
title: 查询其他用户订单（无权限）
priority: High
precondition: 普通用户已登录，有其他用户订单
test_steps:
  1. 普通用户查询其他用户订单
  2. 验证响应状态码为403
  3. 验证错误信息为权限不足
expected_result: 查询失败，权限不足
```

#### TC-ORDER-010: 查询订单带过滤条件
```yaml
test_case: TC-ORDER-010
title: 查询订单带过滤条件
priority: Medium
precondition: 用户已登录，有不同状态订单
test_steps:
  1. 发送带状态过滤的订单查询
  2. 验证响应状态码为200
  3. 验证返回订单符合过滤条件
  4. 测试不同状态过滤
  5. 测试时间范围过滤
expected_result: 过滤查询成功，结果正确
test_data:
  filter_conditions:
    - status=PENDING
    - status=COMPLETED
    - created_at[gte]=2026-04-01
    - amount[gt]=100
```

### 订单状态更新测试

#### TC-ORDER-011: 正常更新订单状态
```yaml
test_case: TC-ORDER-011
title: 正常更新订单状态
priority: High
precondition: 用户或管理员已登录，订单存在
test_steps:
  1. 发送更新订单状态请求
  2. 验证响应状态码为200
  3. 验证订单状态已更新
  4. 验证状态流转符合业务规则
  5. 验证状态更新记录正确
expected_result: 订单状态更新成功
test_data:
  status_flow:
    - PENDING -> PROCESSING
    - PROCESSING -> SHIPPED
    - SHIPPED -> DELIVER