# 移动端用户认证API文档

## 概述
本文档描述了AI-Ready系统移动端的用户认证相关API接口，包括用户登录、注册、登出、Token管理等功能。

## 基础信息
- **基础路径**: `/api/v1/mobile/auth`
- **认证方式**: JWT Token (Bearer Token)
- **响应格式**: JSON
- **认证要求**: 除登录、注册接口外，其他接口都需要Bearer Token

## API接口列表

### 1. 用户登录
**POST** `/api/v1/mobile/auth/login`

**描述**: 使用用户名密码登录系统，获取访问令牌。

**请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**请求参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userInfo": {
      "userId": 1,
      "username": "admin",
      "realName": "管理员",
      "email": "admin@example.com",
      "phone": "13800138000",
      "avatar": "https://example.com/avatar.jpg",
      "roles": ["admin", "user"],
      "permissions": ["user:add", "user:edit", "user:delete"]
    }
  }
}
```

**响应字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| accessToken | string | 访问令牌，有效期为24小时 |
| refreshToken | string | 刷新令牌，有效期为7天 |
| tokenType | string | 令牌类型，固定为"Bearer" |
| expiresIn | integer | 访问令牌过期时间（秒） |
| userInfo | object | 用户基本信息 |

**错误码**:
- 200: 登录成功
- 400: 用户名或密码错误
- 401: 账号已禁用
- 500: 服务器内部错误

### 2. 用户注册
**POST** `/api/v1/mobile/auth/register`

**描述**: 注册新用户账号。

**请求体**:
```json
{
  "username": "newuser",
  "password": "newpassword123",
  "realName": "新用户",
  "email": "newuser@example.com",
  "phone": "13800138001",
  "gender": 1
}
```

**请求参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名（4-20字符） |
| password | string | 是 | 密码（8-20字符，包含字母和数字） |
| realName | string | 否 | 真实姓名 |
| email | string | 否 | 邮箱地址 |
| phone | string | 否 | 手机号码 |
| gender | integer | 否 | 性别（0-女，1-男） |

**响应**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

**错误码**:
- 200: 注册成功
- 400: 请求参数错误
- 409: 用户名已存在
- 500: 服务器内部错误

### 3. 退出登录
**POST** `/api/v1/mobile/auth/logout`

**描述**: 用户退出登录，清除Token。

**请求头**:
```
Authorization: Bearer {accessToken}
```

**请求参数**: 无

**响应**:
```json
{
  "code": 200,
  "message": "退出成功",
  "data": null
}
```

**错误码**:
- 200: 退出成功
- 401: Token无效或已过期
- 500: 服务器内部错误

### 4. 刷新Token
**POST** `/api/v1/mobile/auth/refresh`

**描述**: 使用刷新令牌获取新的访问令牌。

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refreshToken | string | 是 | 刷新令牌 |

**响应**:
```json
{
  "code": 200,
  "message": "Token刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

**错误码**:
- 200: Token刷新成功
- 401: 刷新令牌无效或已过期
- 500: 服务器内部错误

## 移动端专用优化

### 1. 离线登录支持
移动端支持离线登录，可以在无网络情况下使用本地缓存Token进行认证。

### 2. Token自动刷新
移动端SDK内置Token自动刷新机制，在Token即将过期时自动刷新。

### 3. 登录状态持久化
登录状态会持久化到本地存储，应用重启后自动恢复登录状态。

### 4. 多设备登录管理
支持同一账号在多个设备上同时登录，支持设备管理功能。

## 安全规范

### 1. Token安全
- Token存储在移动端安全存储区域
- Token传输使用HTTPS加密
- Token定期自动刷新

### 2. 密码安全
- 密码传输使用HTTPS加密
- 密码存储使用bcrypt哈希算法
- 支持密码强度检查

### 3. 防暴力破解
- 登录失败次数限制
- IP地址频率限制
- 验证码机制（连续失败后触发）

## 测试用例

### 测试1：正常登录
```bash
curl -X POST "http://test-ai-ready.example.com/api/v1/mobile/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 测试2：Token刷新
```bash
curl -X POST "http://test-ai-ready.example.com/api/v1/mobile/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}'
```

### 测试3：退出登录
```bash
curl -X POST "http://test-ai-ready.example.com/api/v1/mobile/auth/logout" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 错误处理

### 常见错误
1. **Token过期**: 返回401状态码，提示"Token已过期，请重新登录或刷新Token"
2. **权限不足**: 返回403状态码，提示"权限不足，无法访问该资源"
3. **参数错误**: 返回400状态码，提示具体的参数错误信息

### 错误响应格式
```json
{
  "code": 401,
  "message": "Token已过期",
  "data": null,
  "timestamp": "2026-04-27T16:40:00Z"
}
```

## 移动端SDK集成

### Android集成
```kotlin
// 初始化SDK
val authClient = AuthClient.Builder()
    .baseUrl("http://test-ai-ready.example.com")
    .tokenStorage(TokenStorageImpl(context))
    .build()

// 用户登录
authClient.login(username, password) { result ->
    when (result) {
        is Success -> {
            val token = result.data.accessToken
            // 保存Token并跳转到主页面
        }
        is Failure -> {
            // 处理登录失败
        }
    }
}
```

### iOS集成
```swift
// 初始化SDK
let authClient = AuthClient(
    baseURL: URL(string: "http://test-ai-ready.example.com")!,
    tokenStorage: KeychainTokenStorage()
)

// 用户登录
authClient.login(username: username, password: password) { result in
    switch result {
    case .success(let response):
        let token = response.accessToken
        // 保存Token并跳转到主页面
    case .failure(let error):
        // 处理登录失败
    }
}
```

## 注意事项
1. 移动端应用必须使用HTTPS连接生产环境
2. Token必须存储在安全区域（Keychain/KeyStore）
3. 登录状态需要定期同步服务器
4. 支持单点登录功能
5. 提供用户主动退出所有设备的功能

---
*文档最后更新：2026-04-27*
*API版本：v1.0.0*
*适用于：Sprint 27+1 测试环境移动端开发*