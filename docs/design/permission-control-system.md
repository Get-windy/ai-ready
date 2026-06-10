# 权限控制系统设计方案

## 1. 概述

本方案基于对来肯云商（ql361.com）权限控制系统的逆向分析和 AI-Ready 现有权限系统的代码审查，提出前后端一体化的权限控制体系设计方案。

### 1.1 目标

- 构建兼容 ql361 成熟模式的 RBAC 权限体系
- 实现菜单权限、按钮权限、数据权限三层控制
- 支持多租户隔离和单据类型级权限
- 前后端统一校验，杜绝越权访问
- 权限变更实时生效，缓存一致性保证

---

## 2. ql361 权限控制系统分析

### 2.1 认证流程

```
用户登录 → POST /account/Auth → 返回 token + sign + 子域名
       → 访问 {subdomain}.ql361.com/desktop.html?token_=xxx&verify_=yyy
       → GetLoginStatusForDesktop → 返回菜单树 + 用户信息
       → 后续 API 调用在 URL 携带 &s=sign&tok=token
```

关键特征：
- **子域名隔离**：每个租户使用独立子域名（如 `22stable.ql361.com`）
- **双令牌**：token（身份）+ sign（签名），均在 URL 参数中传递
- **菜单树即权限**：登录后返回的菜单树直接决定了用户可访问的功能
- **会话持久化**：30 分钟无操作后自动重新登录

### 2.2 权限模型推断

| 维度 | ql361 实现方式 |
|------|---------------|
| **认证** | token + sign 双参数，HTTPS URL 传递 |
| **租户隔离** | 子域名隔离 + 独立数据库 |
| **菜单权限** | GetLoginStatusForDesktop 返回菜单树 |
| **单据权限** | billbllmanager.getdatalist 根据 billtype 参数过滤 |
| **按钮权限** | 前端根据菜单树中的权限标识控制显隐 |
| **数据权限** | 后端 SQL 自动附加用户/部门过滤条件 |
| **角色体系** | RBAC，支持角色继承和权限模板 |

### 2.3 ql361 前端权限模式

从 API 响应结构和前端请求模式推断：
1. **菜单驱动路由**：后端返回的菜单树直接映射为前端路由
2. **权限标识继承**：父菜单权限自动下钻到子菜单和按钮
3. **权限码前缀**：`menu:`（菜单）、`btn:`（按钮）、`data:`（数据）、`bill:`（单据）
4. **用户类型**：0=系统管理员、1=企业管理员、2=普通用户

---

## 3. AI-Ready 现有系统分析

### 3.1 当前架构

```
[前端]
  Pinia userStore (permissions/roles/menus)
    → router/guard.ts (路由守卫)
    → directives/permission.ts (v-permission/v-role)
    → composables/usePermission.ts (组合式 API)

[后端]
  Sa-Token (认证)
    → StpInterfaceImpl (5min Redis 缓存)
    → PermissionAspect (@RequirePermission 切面)
    → DataPermissionInterceptor (MyBatis-Plus SQL 注入)
    → PermissionCacheManager (30min Redis 缓存)
```

### 3.2 数据库结构

```
核心 RBAC 表（MyBatis-Plus）:
  sys_user          → 用户
  sys_role           → 角色 (dataScope: 0全部/1本部门/2本部门及下级/3仅本人)
  sys_permission     → 权限/菜单 (permissionType: 1目录/2菜单/3按钮)
  sys_menu           → 菜单（与 sys_permission 重叠）
  sys_user_role      → 用户-角色关联
  sys_role_permission → 角色-权限关联
  sys_role_menu      → 角色-菜单关联
  sys_dept           → 部门
  sys_tenant         → 租户
  sys_user_tenant    → 用户-租户关联
  sys_role_inheritance → 角色继承
  sys_data_permission  → 数据权限规则
  sys_permission_template → 权限模板

用户模块表（JPA）:
  users, roles, permissions, user_roles, role_permissions
  → 与 sys_* 表功能重叠，需合并
```

### 3.3 前端权限体系

| 机制 | 文件 | 功能 |
|------|------|------|
| **路由守卫** | `router/guard.ts` | 登录检查、Token过期验证、动态路由加载、权限检查 |
| **权限指令** | `directives/permission.ts` | `v-permission`（隐藏/禁用）、`v-role`（角色判断） |
| **权限组合式** | `composables/usePermission.ts` | `usePermission()`、`useMenuPermission()`、`useButtonPermission()`、`useDataPermission()` |
| **工具函数** | `utils/permission.ts` | `hasPermission()`、`hasRole()`、装饰器等 |
| **用户存储** | `stores/user.ts` | Pinia store，保存 permissions/roles/menus |

### 3.4 当前存在的问题

#### 严重问题

| # | 问题 | 位置 | 风险级别 |
|---|------|------|----------|
| P0 | **SecurityUtils 全部硬编码** | `SecurityUtils.java` | 严重 — 所有 getCurrentUserId() 返回 1，hasPermission() 返回 true |
| P0 | **PermissionControllerExt 缺少 @RequirePermission** | `PermissionControllerExt.java` | 严重 — 权限管理 API 完全开放 |
| P1 | **DataPermissionInterceptor SQL 字符串拼接** | `DataPermissionInterceptor.java:121-135` | 高 — SQL 注入风险 |
| P1 | **双实体系统并行** | MyBatis-Plus sys_* + JPA users/roles/* | 高 — 数据不一致风险 |
| P2 | **JWT 模式未启用** | `SaTokenConfig.java` | 中 — UUID token 无法客户端验证过期 |
| P2 | **4 层缓存无一致策略** | 5min/30min/2hr + Sa-Token session | 中 — 权限变更后缓存残留 |

#### 中等问题

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| P3 | **路由权限未填充** | `dynamicRoutes.ts` | 后端返回的菜单权限为空或未与路由关联 |
| P3 | **无权限变更审计日志** | — | 谁在什么时候修改了哪个角色的权限 |
| P3 | **不支持单据类型级权限** | — | 所有 bill_type 对所有用户可见 |
| P3 | **@RequirePermission 未覆盖全部 Controller** | 各 Controller | 部分接口无权限注解保护 |
| P4 | **Sa-Token 配置不支持 JWT** | `application.yml` | 需配置 sa-token.jwt-secret |
| P4 | **SecurityContext 实现不完整** | `SecurityContext.java` | 需要统一上下文获取方式 |

---

## 4. 总体设计方案

### 4.1 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────────┐ │
│  │ Router   │  │ Directives │  │ Composables              │ │
│  │ Guard    │  │ v-perm     │  │ usePermission            │ │
│  │ ①登录检查│  │ v-role     │  │ useMenuPermission       │ │
│  │ ②路由权限│  │ v-bill     │  │ useButtonPermission     │ │
│  │ ③菜单过滤│  │            │  │ useDataPermission       │ │
│  └────┬─────┘  └─────┬──────┘  └───────────┬──────────────┘ │
│       └──────────────┴─────────────────────┘                │
│                              │ HTTP Authorization: Bearer    │
└──────────────────────────────┼──────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────┐
│                        后端 (Spring Boot)                    │
│  ┌───────────────────────────┴───────────────────────────┐  │
│  │              Sa-Token (JWT 模式)                       │  │
│  │  ① StpUtil.checkLogin() → ② JWT 验签 → ③ 会话上下文  │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              │                               │
│  ┌───────────────────────────┴───────────────────────────┐  │
│  │             权限校验层                                   │  │
│  │  ┌─────────────────┐  ┌────────────────────────────┐   │  │
│  │  │ PermissionAspect│  │ DataPermissionInterceptor  │   │  │
│  │  │ @RequirePerm    │  │ @DataPermission            │   │  │
│  │  │ @RequireRole    │  │ 自动注入 WHERE 条件         │   │  │
│  │  └────────┬────────┘  └─────────────┬──────────────┘   │  │
│  │           │                         │                    │  │
│  │  ┌────────┴─────────────────────────┴──────────────┐  │  │
│  │  │            PermissionService                     │  │  │
│  │  │  ① 权限查询 ② 角色管理 ③ 数据权限 ④ 缓存管理     │  │  │
│  │  └─────────────────────┬───────────────────────────┘  │  │
│  └────────────────────────┼──────────────────────────────┘  │
│                           │                                   │
│              ┌────────────┴────────────┐                     │
│              │         Redis           │                     │
│              │  ① 权限缓存 (2min)      │                     │
│              │  ② 黑名单 Token         │                     │
│              │  ③ 会话管理             │                     │
│              └────────────┬────────────┘                     │
│                           │                                   │
│              ┌────────────┴────────────┐                     │
│              │     PostgreSQL          │                     │
│              │  sys_* 权限表           │                     │
│              │  erp_* 业务表           │                     │
│              │  sync_* 同步表          │                     │
│              └─────────────────────────┘                     │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 权限模型数据结构

```sql
-- ============================================================
-- 核心 RBAC 表（使用 MyBatis-Plus，废弃 JPA users 模块）
-- ============================================================

-- 1. 用户表（扩展现有 sys_user）
ALTER TABLE sys_user ADD COLUMN user_type TINYINT NOT NULL DEFAULT 2
  COMMENT '用户类型: 0=超级管理员 1=企业管理员 2=普通用户';
ALTER TABLE sys_user ADD COLUMN status TINYINT NOT NULL DEFAULT 1;
ALTER TABLE sys_user ADD COLUMN dept_id BIGINT;
ALTER TABLE sys_user ADD COLUMN post VARCHAR(50);

-- 2. 角色表（sys_role 已有，增加字段）
ALTER TABLE sys_role ADD COLUMN role_type TINYINT NOT NULL DEFAULT 2
  COMMENT '角色类型: 1=系统内置 2=自定义';
ALTER TABLE sys_role ADD COLUMN data_scope TINYINT NOT NULL DEFAULT 0
  COMMENT '数据权限: 0=全部 1=本部门 2=本部门及下级 3=仅本人';
ALTER TABLE sys_role ADD COLUMN bill_types TEXT
  COMMENT '可访问的单据类型: ["601","604","504","801"]';

-- 3. 权限/菜单表（合并 sys_permission + sys_menu）
ALTER TABLE sys_permission ADD COLUMN permission_type TINYINT NOT NULL DEFAULT 2
  COMMENT '类型: 1=目录 2=菜单 3=按钮 4=单据类型';
ALTER TABLE sys_permission ADD COLUMN permission_code VARCHAR(100) NOT NULL UNIQUE
  COMMENT '权限编码: menu:dashboard / btn:user:add / bill:601';
ALTER TABLE sys_permission ADD COLUMN parent_id BIGINT DEFAULT 0;
ALTER TABLE sys_permission ADD COLUMN sort_order INT DEFAULT 0;
ALTER TABLE sys_permission ADD COLUMN icon VARCHAR(100);
ALTER TABLE sys_permission ADD COLUMN route VARCHAR(200)
  COMMENT '前端路由路径';
ALTER TABLE sys_permission ADD COLUMN api_path VARCHAR(200)
  COMMENT '后端 API 路径';
ALTER TABLE sys_permission ADD COLUMN method VARCHAR(10)
  COMMENT 'HTTP 方法: GET/POST/PUT/DELETE';

-- 4. 用户-角色关联（sys_user_role 已有）
-- 5. 角色-权限关联（sys_role_permission 已有）
-- 6. 部门表（sys_dept 已有）

-- 7. 角色继承表（sys_role_inheritance 已有）
-- 8. 数据权限规则表（sys_data_permission 已有）
-- 9. 权限模板表（sys_permission_template 已有）
```

### 4.3 权限编码规范

```
权限编码格式: {scope}:{module}:{action}

示例:
  menu:dashboard              → 仪表盘菜单
  menu:finance:receipt        → 收款管理菜单
  btn:user:add                → 新增用户按钮
  btn:user:edit               → 编辑用户按钮
  btn:user:delete             → 删除用户按钮
  btn:user:export             → 导出用户按钮
  bill:601                    → 销售出库单访问权限
  bill:604                    → 销售订单访问权限
  bill:504                    → 采购订单访问权限
  bill:801                    → 收款单访问权限
  data:all                    → 全部数据权限
  data:dept                   → 本部门数据权限
  data:self                   → 仅本人数据权限
```

---

## 5. 后端实现方案

### 5.1 Phase 1 — 修复安全基础设施

#### 5.1.1 启用 Sa-Token JWT 模式

```yaml
# application.yml
sa-token:
  token-name: Authorization
  jwt-secret: ${SA_TOKEN_JWT_SECRET:ai-ready-jwt-secret-key}
  is-concurrent: true
  is-share: false
  token-style: jwt
  is-login-after-write: true
  is-read-body: false
  is-read-head: true
  token-prefix: "Bearer"
```

#### 5.1.2 修复 SecurityUtils

```java
public class SecurityUtils {
    public static Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCurrentUsername() {
        try {
            return StpUtil.getSession().getString("username");
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasPermission(String permission) {
        try {
            return StpUtil.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasRole(String role) {
        try {
            return StpUtil.hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 5.1.3 统一 SecurityContext

```java
@Component
public class SecurityContext {
    public Long getCurrentUserId() { return SecurityUtils.getCurrentUserId(); }
    public Long getCurrentTenantId() {
        // 从 JWT 中提取 tenantId claim
        return StpUtil.getLoginIdAsLong();  // 需要改造 LoginId 为复合对象
    }
    public Long getCurrentUserDeptId() {
        return userService.getById(getCurrentUserId()).getDeptId();
    }
    public Integer getCurrentUserDataScope() {
        return roleService.getUserMaxDataScope(getCurrentUserId());
    }
}
```

### 5.2 Phase 2 — 完善权限注解覆盖

#### 5.2.1 @RequirePermission 覆盖所有 Controller

```java
// PermissionControllerExt - 添加权限注解
@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionControllerExt {

    @Operation(summary = "分页查询权限")
    @GetMapping("/page")
    @RequirePermission("sys:permission:list")
    public ApiResponse<PageResult<PermissionDetailVO>> pageList(...) { ... }

    @Operation(summary = "创建权限")
    @PostMapping
    @RequirePermission("sys:permission:add")
    public ApiResponse<Long> create(...) { ... }

    @Operation(summary = "更新权限")
    @PutMapping
    @RequirePermission("sys:permission:edit")
    public ApiResponse<Void> update(...) { ... }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @RequirePermission("sys:permission:delete")
    public ApiResponse<Void> delete(...) { ... }
}
```

#### 5.2.2 所有业务 Controller 添加权限注解

```java
// 示例：收款单 Controller
@RestController
@RequestMapping("/api/finance/receipt")
public class ReceiptController {

    @GetMapping("/page")
    @RequirePermission("bill:801")
    @DataPermission(scope = DataScopeType.AUTO)
    public ApiResponse<PageResult<ReceiptVO>> pageList(...) { ... }

    @PostMapping
    @RequirePermission(value = {"bill:801", "btn:receipt:add"}, logical = Logical.AND)
    public ApiResponse<Void> create(...) { ... }

    @PutMapping("/{id}/approve")
    @RequirePermission("btn:receipt:approve")
    public ApiResponse<Void> approve(...) { ... }
}
```

### 5.3 Phase 3 — 单据类型级权限

#### 5.3.1 数据库扩展

```sql
-- 角色关联可访问的单据类型
CREATE TABLE sys_role_bill_type (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    bill_type VARCHAR(10) NOT NULL COMMENT '单据类型代码: 601/604/504/801',
    permission_level TINYINT DEFAULT 1 COMMENT '1=查看 2=编辑 3=审核',
    UNIQUE (role_id, bill_type)
);
```

#### 5.3.2 后端校验

```java
public class BillTypePermissionService {
    public void validateBillTypeAccess(Long userId, String billType, int requiredLevel) {
        // 超级管理员跳过
        if (isSuperAdmin(userId)) return;

        // 查询用户角色关联的单据权限
        List<RoleBillType> permissions = getRoleBillTypes(userId, billType);
        boolean hasAccess = permissions.stream()
            .anyMatch(p -> p.getPermissionLevel() >= requiredLevel);

        if (!hasAccess) {
            throw new PermissionDeniedException("无权访问单据类型: " + billType);
        }
    }
}

// 在 DataPermissionInterceptor 中添加 bill_type 过滤
```

### 5.4 Phase 4 — 修复数据权限拦截器

#### 5.4.1 当前问题

```java
// DataPermissionInterceptor.java:119-135
// ⚠️ SQL 字符串拼接 —— 注入风险
private String buildWhereClause(String field, DataScopeType scopeType, Long userId) {
    return switch (scopeType) {
        case SELF -> field + " = " + userId;          // 拼接
        case DEPT -> "dept_id = " + deptId;           // 拼接
        case DEPT_AND_CHILD ->
            "dept_id IN (" + String.join(",", ...) + ")"; // 拼接
    };
}
```

#### 5.4.2 修复方案

```java
private Expression buildWhereExpression(String field, DataScopeType scopeType, Long userId) {
    return switch (scopeType) {
        case SELF -> {
            EqualsTo eq = new EqualsTo();
            eq.setLeftExpression(new Column(field));
            eq.setRightExpression(new LongValue(userId));
            yield eq;
        }
        case DEPT -> {
            Long deptId = permissionService.getCurrentUserDeptId();
            if (deptId == null) yield null;
            EqualsTo eq = new EqualsTo();
            eq.setLeftExpression(new Column("dept_id"));
            eq.setRightExpression(new LongValue(deptId));
            yield eq;
        }
        case DEPT_AND_CHILD -> {
            Set<Long> deptIds = permissionService.getCurrentUserDeptAndChildIds();
            if (deptIds == null || deptIds.isEmpty()) yield null;
            InExpression in = new InExpression();
            in.setLeftExpression(new Column("dept_id"));
            in.setRightItemsList(
                new ExpressionList(deptIds.stream()
                    .map(LongValue::new)
                    .collect(Collectors.toList()))
            );
            yield in;
        }
        case ALL, AUTO -> null;
    };
}
```

### 5.5 Phase 5 — 缓存层统一

#### 5.5.1 统一缓存策略

```
统一为 2 级缓存:
  L1: Caffeine (本地缓存, 30秒, 用于高频读取)
  L2: Redis (集中缓存, 5分钟 TTL)

权限变更时:
  1. 更新数据库
  2. 删除 Redis 缓存
  3. 发送 Redis Pub/Sub 消息通知其他实例
  4. 其它实例收到消息后删除本地 Caffeine 缓存
```

#### 5.5.2 缓存管理接口

```java
@Service
public class UnifiedPermissionCacheService {
    private final Cache<Long, List<String>> localCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build();

    public List<String> getPermissions(Long userId) {
        // L1 本地缓存
        List<String> perms = localCache.getIfPresent(userId);
        if (perms != null) return perms;

        // L2 Redis 缓存
        String key = "up:perms:" + userId;
        perms = redisTemplate.opsForValue().get(key);
        if (perms != null) {
            localCache.put(userId, perms);
            return perms;
        }

        // 数据库查询
        perms = userService.getUserPermissionCodes(userId);
        redisTemplate.opsForValue().set(key, perms, 5, TimeUnit.MINUTES);
        localCache.put(userId, perms);
        return perms;
    }

    public void invalidate(Long userId) {
        localCache.invalidate(userId);
        redisTemplate.delete("up:perms:" + userId);
        // 通知其他实例
        redisTemplate.convertAndSend("permission:change", userId.toString());
    }
}
```

#### 5.5.3 废弃旧缓存类

- 废弃 `StpInterfaceImpl` 中的 5min 缓存 → 由 `UnifiedPermissionCacheService` 统一管理
- 废弃 `PermissionCacheManager` 30min 缓存 → 由 `UnifiedPermissionCacheService` 统一管理
- 废弃 `PermissionServiceImpl` 中的 2hr 缓存 → 由 `UnifiedPermissionCacheService` 统一管理

### 5.6 Phase 6 — 合并双实体系统

```java
// 1. 保留 MyBatis-Plus 的 sys_* 系列作为唯一权限模型
// 2. 将 JPA users 模块的字段合并到 sys_user
// 3. JPA entities 标记 @Deprecated
// 4. 提供数据迁移脚本

-- 数据迁移：JPA users → sys_user
INSERT INTO sys_user (id, username, password, real_name, email, phone, status, create_time)
SELECT id, username, password, display_name, email, phone, status, created_at FROM users
ON CONFLICT (username) DO NOTHING;
```

---

## 6. 前端实现方案

### 6.1 路由权限动态加载

借鉴 ql361 的"菜单树即路由"模式：

```typescript
// router/dynamicRoutes.ts
// 后端返回的菜单树直接转换为 Vue Router 配置

interface MenuNode {
  id: number
  name: string
  permissionCode: string  // menu:dashboard / bill:601
  path: string
  component: string
  icon: string
  sortOrder: number
  children: MenuNode[]
  permissions: string[]    // 按钮权限列表
}

export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  const menuTree = await menuApi.getCurrentUserMenus()
  return transformMenuTree(menuTree)
}

function transformMenuTree(menus: MenuNode[]): RouteRecordRaw[] {
  return menus
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(menu => ({
      path: menu.path,
      name: menu.permissionCode,
      component: componentMap[menu.component],
      meta: {
        title: menu.name,
        permission: menu.permissionCode,
        permissions: menu.permissions,  // 子按钮权限
        icon: menu.icon,
      },
      children: menu.children?.length
        ? transformMenuTree(menu.children)
        : undefined,
    }))
}
```

### 6.2 新增单据权限指令

```typescript
// directives/billPermission.ts
// 借鉴 v-permission 模式，增加单据类型级权限控制

import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

export const billPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    // value 格式: "601:edit" 或 ["601:view", "604:view"]
    if (!value) return

    const userPermissions = userStore.permissions
    const billCodes = Array.isArray(value) ? value : [value]

    const hasAccess = billCodes.some(code =>
      userPermissions.includes(`bill:${code}`)
    )

    if (!hasAccess) {
      el.style.display = 'none'
    }
  },
}
```

### 6.3 完善路由守卫权限检查

```typescript
// router/guard.ts — 增强权限检查

function checkRouteAccess(to: RouteLocationNormalized): boolean {
  const userStore = useUserStore()

  // 白名单放行
  if (isInWhiteList(to.path)) return true

  // 超级管理员放行
  if (userStore.userType === 0) return true

  // 权限检查
  const requiredPermission = to.meta?.permission as string
  if (!requiredPermission) return true

  return userStore.hasPermission(requiredPermission)
}
```

### 6.4 前端权限组件封装

```vue
<!-- 权限包装组件 -->
<template>
  <slot v-if="hasAccess" />
  <slot v-else name="noPermission">
    <a-tooltip title="暂无权限">
      <span class="permission-disabled">
        <slot name="disabled" />
      </span>
    </a-tooltip>
  </slot>
</template>

<script setup lang="ts">
const props = defineProps<{
  permission: string | string[]
  mode?: 'hide' | 'disabled'  // 隐藏或禁用态
}>()

const userStore = useUserStore()
const hasAccess = computed(() => {
  const perms = Array.isArray(props.permission) ? props.permission : [props.permission]
  return perms.some(p => userStore.hasPermission(p))
})
</script>

<!-- 使用示例 -->
<Permission :permission="'btn:receipt:add'" mode="disabled">
  <a-button @click="handleAdd">新增收款单</a-button>
</Permission>

<Permission :permission="['bill:601', 'bill:604']">
  <BillSelector />
</Permission>
```

---

## 7. 实施路线图

### 7.1 阶段划分

| 阶段 | 内容 | 优先级 | 预估工时 |
|------|------|--------|----------|
| **P0** | 修复 SecurityUtils + 启用 JWT | 紧急 | 2天 |
| **P0** | 废弃 JPA users 模块，数据迁移 | 紧急 | 3天 |
| **P0** | @RequirePermission 覆盖所有 Controller | 紧急 | 2天 |
| **P1** | 修复 DataPermissionInterceptor SQL 注入 | 高 | 1天 |
| **P1** | 统一缓存层 | 高 | 2天 |
| **P1** | 单据类型级权限（数据库 + API） | 高 | 3天 |
| **P2** | 前端路由权限联动 | 中 | 2天 |
| **P2** | 权限变更审计日志 | 中 | 1天 |
| **P2** | 角色继承完善 | 中 | 2天 |
| **P3** | 权限模板、前端权限组件库 | 低 | 2天 |
| **P3** | 数据权限面向前端开放 | 低 | 1天 |

### 7.2 各阶段依赖关系

```
P0 (SecurityUtils + JWT)
  └── P0 (废弃 JPA)
       └── P0 (@RequirePermission 覆盖)
            ├── P1 (SQL 注入修复)
            ├── P1 (缓存统一)
            └── P1 (单据类型权限)
                 └── P2 (前端路由联动)
                      └── P2 (审计日志) + P2 (角色继承)
                           └── P3 (权限模板 + 组件库)
```

### 7.3 关键里程碑

| 里程碑 | 时间 | 验收标准 |
|--------|------|----------|
| M1 | P0 完成 | SecurityUtils 真实取值、JWT 生效、所有 API 有注解保护 |
| M2 | P1 完成 | 数据权限安全、缓存一致、单据级权限可用 |
| M3 | P2 完成 | 前端路由配合权限、审计可追溯 |
| M4 | P3 完成 | 权限模板、组件库完善 |

---

## 8. 风险与应对

| 风险 | 可能性 | 影响 | 应对措施 |
|------|--------|------|----------|
| JWT 启用后现有 token 失效 | 高 | 所有用户需重新登录 | 灰度发布，先通知再切换 |
| 权限注解遗漏导致接口 403 | 中 | 部分功能不可用 | 自动化测试覆盖，全量扫描 @RestController |
| 双实体合并数据丢失 | 低 | 用户数据丢失 | 先备份再迁移，可回滚脚本 |
| 缓存清理不及时 | 低 | 权限变更未生效 | Redis Pub/Sub 通知 + 最大 5min 自动过期兜底 |
| 新增 bill_type 权限配置影响已有角色 | 中 | 已有角色用户可能看不到单据 | 新增角色时默认不赋予 bill_type 权限，需显式配置 |

---

## 9. ql361 模式借鉴总结

| ql361 模式 | AI-Ready 当前情况 | 对齐方案 |
|------------|-------------------|----------|
| token + sign 双参数 | Sa-Token JWT | 启用 JWT，在 token 中嵌入 userId + tenantId |
| 子域名隔离租户 | sys_tenant 隔离 | 保持现有方案，增加 tenantId 在 JWT claim 中 |
| 菜单树即权限 | 前后端权限分离 | 后端返回菜单树，前端直接生成路由 |
| 单据类型级权限 | 不支持 | 新增 sys_role_bill_type + 前端指令 |
| 角色继承 | sys_role_inheritance 已存在 | 完善继承链计算逻辑 |
| 权限模板 | sys_permission_template 已存在 | 对接角色创建流程 |
| 按钮权限码前缀 | 现有 `btn:` 前缀 | 统一编码规范文档 |
| GetLoginStatus 返回用户信息 | `/api/auth/userinfo` | 扩展返回字段：menus + billTypes + dataScope |
