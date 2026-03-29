# AI-Ready 后端 API 文档

## 概述

AI-Ready 系统提供了一套完整的 RESTful API，用于用户管理、角色管理、权限管理和菜单管理。

- **基础路径**: `http://localhost:8080/api`
- **认证方式**: Sa-Token (JWT)
- **响应格式**: JSON

---

## 统一响应格式

### Result<T>

所有 API 返回统一的响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

---

## 1. 用户管理 API (/api/user)

### 1.1 用户登录

- **POST** `/api/user/login`
- **权限**: 无需认证
- **请求体**:
```json
{
  "username": "admin",
  "password": "password123",
  "tenantId": 1
}
```
- **响应**: 返回登录 Token

### 1.2 用户登出

- **POST** `/api/user/logout`
- **权限**: 需要登录
- **响应**: 成功消息

### 1.3 创建用户

- **POST** `/api/user`
- **权限**: `user:create`
- **请求体**:
```json
{
  "tenantId": 1,
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "user@example.com",
  "phone": "13800138000",
  "gender": 1,
  "userType": 1,
  "deptId": 10,
  "postId": 20
}
```
- **响应**: 返回用户 ID

### 1.4 更新用户

- **PUT** `/api/user/{id}`
- **权限**: `user:update`
- **请求体**: 同创建用户（不含密码）

### 1.5 删除用户

- **DELETE** `/api/user/{id}`
- **权限**: `user:delete`

### 1.6 批量删除用户

- **DELETE** `/api/user/batch`
- **权限**: `user:delete`
- **请求体**: `[1, 2, 3]` (用户 ID 数组)

### 1.7 分页查询用户

- **GET** `/api/user/page`
- **权限**: `user:list`
- **参数**:
  - `tenantId`: 租户 ID
  - `username`: 用户名（可选）
  - `status`: 状态（可选）
  - `deptId`: 部门 ID（可选）
  - `pageNum`: 页码
  - `pageSize`: 每页数量

### 1.8 获取用户详情

- **GET** `/api/user/{id}`
- **权限**: `user:detail`

### 1.9 重置密码

- **PUT** `/api/user/{id}/password/reset`
- **权限**: `user:reset-password`
- **参数**: `newPassword`

### 1.10 修改密码

- **PUT** `/api/user/{id}/password/change`
- **权限**: 需要登录
- **参数**: `oldPassword`, `newPassword`

### 1.11 分配角色

- **POST** `/api/user/{id}/roles`
- **权限**: `user:assign-role`
- **请求体**: `[1, 2, 3]` (角色 ID 数组)

### 1.12 更新用户状态

- **PUT** `/api/user/{id}/status`
- **权限**: `user:update-status`
- **参数**: `status` (0-正常, 1-禁用, 2-锁定)

---

## 2. 角色管理 API (/api/role)

### 2.1 创建角色

- **POST** `/api/role`
- **权限**: `role:create`
- **请求体**:
```json
{
  "tenantId": 1,
  "roleName": "管理员",
  "roleCode": "admin",
  "sort": 1,
  "remark": "系统管理员"
}
```

### 2.2 更新角色

- **PUT** `/api/role/{id}`
- **权限**: `role:update`

### 2.3 删除角色

- **DELETE** `/api/role/{id}`
- **权限**: `role:delete`

### 2.4 分页查询角色

- **GET** `/api/role/page`
- **权限**: `role:list`
- **参数**: `tenantId`, `roleName`, `status`, `current`, `size`

### 2.5 分配权限

- **POST** `/api/role/{id}/permissions`
- **权限**: `role:assign-permission`
- **请求体**: `[1, 2, 3]` (权限 ID 数组)

### 2.6 分配菜单

- **POST** `/api/role/{id}/menus`
- **权限**: `role:assign-menu`
- **请求体**: `[1, 2, 3]` (菜单 ID 数组)

### 2.7 获取角色权限

- **GET** `/api/role/{id}/permissions`
- **权限**: `role:detail`

### 2.8 更新角色状态

- **PUT** `/api/role/{id}/status`
- **权限**: `role:update-status`

---

## 3. 权限管理 API (/api/permission)

### 3.1 创建权限

- **POST** `/api/permission`
- **权限**: `permission:create`
- **请求体**:
```json
{
  "tenantId": 1,
  "parentId": 0,
  "permissionName": "用户管理",
  "permissionCode": "user:manage",
  "permissionType": 1,
  "path": "/user",
  "component": "system/user/index",
  "icon": "user",
  "apiPath": "/api/user",
  "method": "GET",
  "sort": 1,
  "visible": 1
}
```

### 3.2 更新权限

- **PUT** `/api/permission/{id}`
- **权限**: `permission:update`

### 3.3 删除权限

- **DELETE** `/api/permission/{id}`
- **权限**: `permission:delete`

### 3.4 批量删除权限

- **DELETE** `/api/permission/batch`
- **权限**: `permission:delete`

### 3.5 分页查询权限

- **GET** `/api/permission/page`
- **权限**: `permission:list`

### 3.6 获取权限树

- **GET** `/api/permission/tree`
- **权限**: `permission:list`
- **参数**: `tenantId`

### 3.7 获取子权限列表

- **GET** `/api/permission/children/{parentId}`
- **权限**: `permission:list`

### 3.8 更新权限状态

- **PUT** `/api/permission/{id}/status`
- **权限**: `permission:update-status`

### 3.9 检查权限编码

- **GET** `/api/permission/check-code`
- **参数**: `permissionCode`, `tenantId`, `excludeId`

---

## 4. 菜单管理 API (/api/menu)

### 4.1 创建菜单

- **POST** `/api/menu`
- **权限**: `menu:create`
- **请求体**:
```json
{
  "tenantId": 1,
  "parentId": 0,
  "menuName": "系统管理",
  "menuCode": "system",
  "menuType": 0,
  "path": "/system",
  "component": "Layout",
  "routeName": "System",
  "icon": "setting",
  "sort": 1,
  "isExternal": 0,
  "isCache": 0,
  "visible": 1
}
```

### 4.2 更新菜单

- **PUT** `/api/menu/{id}`
- **权限**: `menu:update`

### 4.3 删除菜单

- **DELETE** `/api/menu/{id}`
- **权限**: `menu:delete`

### 4.4 获取菜单树

- **GET** `/api/menu/tree`
- **权限**: `menu:list`
- **参数**: `tenantId`

### 4.5 获取用户菜单树

- **GET** `/api/menu/user/tree`
- **权限**: 需要登录
- **参数**: `userId`

### 4.6 获取子菜单列表

- **GET** `/api/menu/children/{parentId}`
- **权限**: `menu:list`

### 4.7 更新菜单排序

- **PUT** `/api/menu/{id}/sort`
- **权限**: `menu:update`

### 4.8 更新菜单状态

- **PUT** `/api/menu/{id}/status`
- **权限**: `menu:update-status`

---

## 权限编码列表

| 权限编码 | 描述 |
|---------|------|
| user:create | 创建用户 |
| user:update | 更新用户 |
| user:delete | 删除用户 |
| user:list | 查询用户列表 |
| user:detail | 查看用户详情 |
| user:reset-password | 重置密码 |
| user:assign-role | 分配角色 |
| user:update-status | 更新用户状态 |
| role:create | 创建角色 |
| role:update | 更新角色 |
| role:delete | 删除角色 |
| role:list | 查询角色列表 |
| role:assign-permission | 分配权限 |
| role:assign-menu | 分配菜单 |
| permission:create | 创建权限 |
| permission:update | 更新权限 |
| permission:delete | 删除权限 |
| permission:list | 查询权限列表 |
| menu:create | 创建菜单 |
| menu:update | 更新菜单 |
| menu:delete | 删除菜单 |
| menu:list | 查询菜单列表 |

---

## 错误码说明

| 错误码 | 描述 |
|-------|------|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无权限 |
| 500 | 服务器错误 |

---

*文档生成时间: 2026-03-29*
*版本: 1.0.0*