# 用户管理模块API文档

## 概述
本文档描述了AI-Ready系统用户管理模块的RESTful API接口。该模块提供用户注册、登录、登出、用户信息管理、权限管理和角色管理等功能。

## 基础信息
- **基础URL**: `/api`
- **认证方式**: JWT Token (Bearer Token)
- **API版本**: v1
- **响应格式**: JSON

## 认证管理 (Authentication)

### 1. 用户登录
**POST** `/api/auth/login`

**请求体**:
```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
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

### 2. 用户登出
**POST** `/api/auth/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

### 3. 获取当前用户信息
**GET** `/api/auth/user-info`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000",
    "avatar": "https://example.com/avatar.jpg",
    "roles": ["admin", "user"],
    "permissions": ["user:add", "user:edit", "user:delete"]
  }
}
```

### 4. 检查用户名是否可用
**GET** `/api/auth/check-username?username={username}`

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

## 用户管理 (User Management)

### 1. 分页查询用户
**GET** `/api/user/page`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页大小，默认10 |
| username | string | 否 | 用户名模糊查询 |
| realName | string | 否 | 真实姓名模糊查询 |
| phone | string | 否 | 手机号精确查询 |
| email | string | 否 | 邮箱精确查询 |
| status | integer | 否 | 状态（0-禁用，1-启用） |
| deptId | integer | 否 | 部门ID |
| gender | integer | 否 | 性别（0-女，1-男） |

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "realName": "管理员",
        "email": "admin@example.com",
        "phone": "13800138000",
        "status": 1,
        "gender": 1,
        "createTime": "2026-04-27 00:00:00",
        "updateTime": "2026-04-27 00:00:00",
        "roles": [
          {
            "id": 1,
            "name": "管理员",
            "code": "admin",
            "description": "系统管理员"
          }
        ]
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 2. 获取用户详情
**GET** `/api/user/{id}`

**路径参数**:
- `id`: 用户ID

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000",
    "status": 1,
    "gender": 1,
    "deptId": 1,
    "createTime": "2026-04-27 00:00:00",
    "updateTime": "2026-04-27 00:00:00",
    "roles": [
      {
        "id": 1,
        "name": "管理员",
        "code": "admin",
        "description": "系统管理员"
      }
    ],
    "roleIds": [1]
  }
}
```

### 3. 创建用户
**POST** `/api/user`

**请求体**:
```json
{
  "username": "testuser",
  "password": "password123",
  "realName": "测试用户",
  "email": "test@example.com",
  "phone": "13800138001",
  "gender": 1,
  "deptId": 1,
  "status": 1,
  "roleIds": [2, 3]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": 2
}
```

### 4. 更新用户
**PUT** `/api/user`

**请求体**:
```json
{
  "id": 2,
  "realName": "测试用户更新",
  "email": "test2@example.com",
  "phone": "13800138002",
  "gender": 1,
  "deptId": 1,
  "status": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

### 5. 删除用户
**DELETE** `/api/user/{id}`

**路径参数**:
- `id`: 用户ID

**响应**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 6. 批量删除用户
**DELETE** `/api/user/batch`

**请求体**:
```json
[1, 2, 3]
```

**响应**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": null
}
```

### 7. 修改密码
**PUT** `/api/user/{id}/password`

**路径参数**:
- `id`: 用户ID

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码 |

**响应**:
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

### 8. 重置密码
**PUT** `/api/user/{id}/password/reset`

**路径参数**:
- `id`: 用户ID

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newPassword | string | 是 | 新密码 |

**响应**:
```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": null
}
```

### 9. 启用/禁用用户
**PUT** `/api/user/{id}/status`

**路径参数**:
- `id`: 用户ID

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 状态（0-禁用，1-启用） |

**响应**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null
}
```

### 10. 分配角色
**POST** `/api/user/{id}/roles`

**路径参数**:
- `id`: 用户ID

**请求体**:
```json
[1, 2, 3]
```

**响应**:
```json
{
  "code": 200,
  "message": "角色分配成功",
  "data": null
}
```

## 角色管理 (Role Management)

### 1. 分页查询角色
**GET** `/api/role/page`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页大小，默认10 |
| name | string | 否 | 角色名称模糊查询 |
| code | string | 否 | 角色编码模糊查询 |
| status | integer | 否 | 状态（0-禁用，1-启用） |

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "管理员",
        "code": "admin",
        "description": "系统管理员角色",
        "status": 1,
        "createTime": "2026-04-27 00:00:00",
        "updateTime": "2026-04-27 00:00:00",
        "permissionIds": [1, 2, 3, 4, 5]
      }
    ],
    "total": 10,
    "current": 1,
    "size": 10,
    "pages": 1
  }
}
```

### 2. 获取角色详情
**GET** `/api/role/{id}`

**路径参数**:
- `id`: 角色ID

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "管理员",
    "code": "admin",
    "description": "系统管理员角色",
    "status": 1,
    "createTime": "2026-04-27 00:00:00",
    "updateTime": "2026-04-27 00:00:00",
    "permissions": [
      {
        "id": 1,
        "name": "用户管理",
        "code": "user:manage",
        "type": 1,
        "path": "/api/user/**",
        "method": "*",
        "description": "用户管理权限"
      }
    ],
    "permissionIds": [1, 2, 3, 4, 5]
  }
}
```

### 3. 创建角色
**POST** `/api/role`

**请求体**:
```json
{
  "name": "测试角色",
  "code": "test_role",
  "description": "测试角色描述",
  "status": 1,
  "permissionIds": [1, 2, 3]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "角色创建成功",
  "data": 2
}
```

### 4. 更新角色
**PUT** `/api/role`

**请求体**:
```json
{
  "id": 2,
  "name": "测试角色更新",
  "description": "更新后的角色描述",
  "status": 1,
  "permissionIds": [1, 3, 4]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "角色更新成功",
  "data": null
}
```

### 5. 删除角色
**DELETE** `/api/role/{id}`

**路径参数**:
- `id`: 角色ID

**响应**:
```json
{
  "code": 200,
  "message": "角色删除成功",
  "data": null
}
```

### 6. 批量删除角色
**DELETE** `/api/role/batch`

**请求体**:
```json
[1, 2, 3]
```

**响应**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": null
}
```

### 7. 启用/禁用角色
**PUT** `/api/role/{id}/status`

**路径参数**:
- `id`: 角色ID

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 状态（0-禁用，1-启用） |

**响应**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null
}
```

### 8. 分配权限
**POST** `/api/role/{id}/permissions`

**路径参数**:
- `id`: 角色ID

**请求体**:
```json
[1, 2, 3, 4, 5]
```

**响应**:
```json
{
  "code": 200,
  "message": "权限分配成功",
  "data": null
}
```

## 权限管理 (Permission Management)

### 1. 分页查询权限
**GET** `/api/permission/page`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页大小，默认10 |
| name | string | 否 | 权限名称模糊查询 |
| code | string | 否 | 权限编码模糊查询 |
| type | integer | 否 | 权限类型（1-菜单，2-按钮，3-API） |

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "用户管理",
        "code": "user:manage",
        "type": 3,
        "path": "/api/user/**",
        "method": "*",
        "description": "用户管理权限",
        "status": 1,
        "createTime": "2026-04-27 00:00:00",
        "updateTime": "2026-04-27 00:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

### 2. 获取权限详情
**GET** `/api/permission/{id}`

**路径参数**:
- `id`: 权限ID

**响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "用户管理",
    "code": "user:manage",
    "type": 3,
    "path": "/api/user/**",
    "method": "*",
    "description": "用户管理权限",
    "status": 1,
    "createTime": "2026-04-27 00:00:00",
    "updateTime": "2026-04-27 00:00:00"
  }
}
```

### 3. 创建权限
**POST** `/api/permission`

**请求体**:
```json
{
  "name": "测试权限",
  "code": "test:permission",
  "type": 3,
  "path": "/api/test/**",
  "method": "GET,POST",
  "description": "测试权限描述",
  "status": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "权限创建成功",
  "data": 51
}
```

### 4. 更新权限
**PUT** `/api/permission`

**请求体**:
```json
{
  "id": 51,
  "name": "测试权限更新",
  "description": "更新后的权限描述",
  "status": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "权限更新成功",
  "data": null
}
```

### 5. 删除权限
**DELETE** `/api/permission/{id}`

**路径参数**:
- `id`: 权限ID

**响应**:
```json
{
  "code": 200,
  "message": "权限删除成功",
  "data": null
}
```

### 6. 批量删除权限
**DELETE** `/api/permission/batch`

**请求体**:
```json
[51, 52, 53]
```

**响应**:
```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": null
}
```

### 7. 启用/禁用权限
**PUT** `/api/permission/{id}/status`

**路径参数**:
- `id`: 权限ID

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 状态（0-禁用，1-启用） |

**响应**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null
}
```

## 错误码说明

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 200 | 成功 | 操作成功 |
| 400 | 请求参数错误 | 检查请求参数格式和内容 |
| 401 | 未授权 | 用户未登录或token已过期 |
| 403 | 权限不足 | 用户没有执行此操作的权限 |
| 404 | 资源不存在 | 检查请求的资源ID是否正确 |
| 409 | 资源冲突 | 用户名、邮箱或手机号已存在 |
| 500 | 服务器内部错误 | 联系系统管理员 |

## 测试环境信息
- **测试环境URL**: http://test-ai-ready.example.com
- **测试数据库**: PostgreSQL 14.0
- **默认测试账号**:
  - 用户名: admin
  - 密码: admin123
- **默认测试角色**: admin, user, guest
- **默认测试权限**: user:*, role:*, permission:*

## API使用示例

### 1. 登录并获取token
```bash
curl -X POST "http://test-ai-ready.example.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 2. 查询用户列表
```bash
curl -X GET "http://test-ai-ready.example.com/api/user/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"
```

### 3. 创建新用户
```bash
curl -X POST "http://test-ai-ready.example.com/api/user" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","realName":"测试用户","email":"test@example.com","phone":"13800138001","gender":1,"deptId":1,"status":1,"roleIds":[2]}'
```

### 4. 修改用户密码
```bash
curl -X PUT "http://test-ai-ready.example.com/api/user/2/password?oldPassword=old123&newPassword=new456" \
  -H "Authorization: Bearer {token}"
```

## 注意事项
1. 所有敏感操作（如删除、修改密码）需要管理员权限
2. 密码传输需要加密处理
3. 接口调用频率有限制，避免频繁调用
4. 测试环境数据定期清理，请勿使用生产数据
5. 所有API都需要在header中携带Authorization token（登录接口除外）