# AI-Ready 数据库设计文档

## 概述

AI-Ready 系统采用多租户架构，支持 PostgreSQL 和 MySQL 双数据库。本文档描述核心数据模型设计。

- **数据库类型**: PostgreSQL 14+ / MySQL 8.0+
- **字符集**: UTF-8 (utf8mb4)
- **引擎**: InnoDB (MySQL)
- **设计原则**: 多租户隔离、软删除、审计字段

---

## 表结构设计

### 1. 租户管理模块

#### sys_tenant - 租户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，租户ID |
| tenant_name | VARCHAR(100) | 租户名称 |
| tenant_code | VARCHAR(50) | 租户编码（唯一） |
| logo | VARCHAR(255) | Logo URL |
| domain | VARCHAR(255) | 绑定域名 |
| status | TINYINT | 状态(0-正常 1-禁用) |
| expire_time | DATETIME | 过期时间 |
| max_users | INT | 最大用户数 |
| max_depts | INT | 最大部门数 |
| contact_name | VARCHAR(50) | 联系人 |
| contact_phone | VARCHAR(20) | 联系电话 |
| contact_email | VARCHAR(100) | 联系邮箱 |
| address | VARCHAR(255) | 地址 |
| deleted | TINYINT | 删除标记 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

### 2. 用户权限模块

#### sys_user - 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，用户ID |
| tenant_id | BIGINT | 租户ID |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码(BCrypt加密) |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| gender | TINYINT | 性别(0-未知 1-男 2-女) |
| user_type | TINYINT | 用户类型(0-系统 1-企业 2-代理) |
| status | TINYINT | 状态(0-正常 1-禁用 2-锁定) |
| dept_id | BIGINT | 部门ID |
| post_id | BIGINT | 岗位ID |
| last_login_time | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录IP |
| login_count | INT | 登录次数 |
| ext_info | JSON | 扩展信息 |
| deleted | TINYINT | 删除标记 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | BIGINT | 创建人 |
| update_by | BIGINT | 更新人 |

**索引**:
- PRIMARY KEY (id)
- UNIQUE KEY uk_tenant_username (tenant_id, username)
- INDEX idx_user_tenant (tenant_id)
- INDEX idx_user_dept (dept_id)
- INDEX idx_user_status (status)

#### sys_role - 角色表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，角色ID |
| tenant_id | BIGINT | 租户ID |
| role_name | VARCHAR(50) | 角色名称 |
| role_code | VARCHAR(50) | 角色编码 |
| role_type | TINYINT | 角色类型(0-系统 1-自定义) |
| data_scope | TINYINT | 数据权限范围 |
| sort | INT | 排序 |
| status | TINYINT | 状态 |
| remark | VARCHAR(255) | 备注 |
| deleted | TINYINT | 删除标记 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### sys_menu - 菜单表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，菜单ID |
| tenant_id | BIGINT | 租户ID |
| parent_id | BIGINT | 父级ID |
| menu_name | VARCHAR(50) | 菜单名称 |
| menu_code | VARCHAR(50) | 菜单编码 |
| menu_type | TINYINT | 类型(0-目录 1-菜单 2-按钮) |
| path | VARCHAR(255) | 路由路径 |
| component | VARCHAR(255) | 组件路径 |
| route_name | VARCHAR(100) | 路由名称 |
| redirect | VARCHAR(255) | 重定向 |
| icon | VARCHAR(100) | 图标 |
| sort | INT | 排序 |
| is_external | TINYINT | 是否外链 |
| is_cache | TINYINT | 是否缓存 |
| visible | TINYINT | 是否可见 |
| status | TINYINT | 状态 |
| remark | VARCHAR(255) | 备注 |
| deleted | TINYINT | 删除标记 |

#### sys_permission - 权限表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，权限ID |
| tenant_id | BIGINT | 租户ID |
| parent_id | BIGINT | 父级ID |
| permission_name | VARCHAR(50) | 权限名称 |
| permission_code | VARCHAR(100) | 权限编码 |
| permission_type | TINYINT | 类型(1-菜单 2-按钮 3-API) |
| path | VARCHAR(255) | 菜单路径 |
| component | VARCHAR(255) | 组件路径 |
| icon | VARCHAR(100) | 图标 |
| api_path | VARCHAR(255) | API路径 |
| method | VARCHAR(10) | HTTP方法 |
| sort | INT | 排序 |
| visible | TINYINT | 是否可见 |
| status | TINYINT | 状态 |
| deleted | TINYINT | 删除标记 |

#### sys_user_role - 用户角色关联表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| role_id | BIGINT | 角色ID |
| tenant_id | BIGINT | 租户ID |
| create_time | DATETIME | 创建时间 |

#### sys_role_menu - 角色菜单关联表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| role_id | BIGINT | 角色ID |
| menu_id | BIGINT | 菜单ID |
| tenant_id | BIGINT | 租户ID |
| create_time | DATETIME | 创建时间 |

#### sys_role_permission - 角色权限关联表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| role_id | BIGINT | 角色ID |
| permission_id | BIGINT | 权限ID |
| tenant_id | BIGINT | 租户ID |
| create_time | DATETIME | 创建时间 |

#### sys_dept - 部门表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，部门ID |
| tenant_id | BIGINT | 租户ID |
| parent_id | BIGINT | 父级ID |
| dept_name | VARCHAR(50) | 部门名称 |
| dept_code | VARCHAR(50) | 部门编码 |
| leader | VARCHAR(50) | 负责人 |
| phone | VARCHAR(20) | 电话 |
| email | VARCHAR(100) | 邮箱 |
| sort | INT | 排序 |
| status | TINYINT | 状态 |
| deleted | TINYINT | 删除标记 |

---

### 3. 系统配置模块

#### sys_project_config - 项目配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，配置ID |
| tenant_id | BIGINT | 租户ID |
| config_key | VARCHAR(100) | 配置键 |
| config_value | TEXT | 配置值 |
| config_type | VARCHAR(50) | 配置类型(string/json/number/boolean) |
| config_group | VARCHAR(50) | 配置分组 |
| description | VARCHAR(255) | 配置描述 |
| status | TINYINT | 状态 |
| deleted | TINYINT | 删除标记 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### sys_oper_log - 操作日志表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，日志ID |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| module | VARCHAR(50) | 模块 |
| action | VARCHAR(100) | 操作 |
| method | VARCHAR(200) | 方法 |
| request_url | VARCHAR(255) | 请求URL |
| request_method | VARCHAR(10) | 请求方法 |
| request_params | TEXT | 请求参数 |
| response_result | TEXT | 响应结果 |
| status | TINYINT | 状态(0-成功 1-失败) |
| error_msg | TEXT | 错误信息 |
| oper_time | DATETIME | 操作时间 |
| cost_time | BIGINT | 耗时(ms) |
| oper_ip | VARCHAR(50) | 操作IP |
| oper_location | VARCHAR(100) | 操作地点 |

---

## ER 图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  sys_tenant │     │   sys_user  │     │   sys_role  │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id          │◄────│ tenant_id   │     │ id          │
│ tenant_name │     │ id          │     │ tenant_id   │
│ tenant_code │     │ username    │     │ role_name   │
│ ...         │     │ password    │     │ role_code   │
└─────────────┘     │ ...         │     │ ...         │
                    └──────┬──────┘     └──────┬──────┘
                           │                   │
                           │    ┌──────────────┘
                           │    │
                    ┌──────▼────┴─────┐
                    │  sys_user_role  │
                    ├─────────────────┤
                    │ user_id         │
                    │ role_id         │
                    └─────────────────┘

┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│  sys_menu   │     │ sys_role_menu   │     │  sys_role   │
├─────────────┤     ├─────────────────┤     ├─────────────┤
│ id          │◄────│ menu_id         │────►│ id          │
│ menu_name   │     │ role_id         │     │ role_name   │
│ menu_code   │     └─────────────────┘     │ ...         │
│ ...         │                             └─────────────┘
└─────────────┘

┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│sys_permission│    │sys_role_perm    │     │  sys_role   │
├─────────────┤     ├─────────────────┤     ├─────────────┤
│ id          │◄────│ permission_id   │────►│ id          │
│ perm_name   │     │ role_id         │     │ role_name   │
│ perm_code   │     └─────────────────┘     │ ...         │
│ ...         │                             └─────────────┘
└─────────────┘
```

---

## 索引策略

### 主要索引

1. **租户索引**: 所有表都有 `tenant_id` 索引，支持多租户查询
2. **外键索引**: 所有外键字段都有索引
3. **唯一索引**: 用户名、角色编码、菜单编码等需要唯一约束
4. **状态索引**: 常用过滤字段如 `status`、`deleted`

### 索引命名规范

- 主键: `PRIMARY KEY`
- 唯一键: `uk_表名_字段名`
- 普通索引: `idx_表名_字段名`

---

## 数据初始化

### 默认数据

1. **默认租户**: ID=1，租户编码 DEFAULT
2. **超级管理员**: admin / admin123
3. **默认角色**: 超级管理员、管理员、普通用户
4. **默认菜单**: 系统管理模块
5. **默认权限**: 用户、角色、菜单相关权限
6. **默认配置**: 系统基础配置

---

## 脚本文件

| 文件 | 说明 |
|------|------|
| init-database.sql | PostgreSQL 初始化脚本 |
| init-database-mysql.sql | MySQL 初始化脚本 |
| init-base-tables.sql | 基础表结构（PostgreSQL） |

---

*文档版本: 1.0.0*
*更新日期: 2026-03-29*