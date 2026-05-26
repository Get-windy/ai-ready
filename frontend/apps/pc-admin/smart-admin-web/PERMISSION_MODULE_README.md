# AI-Ready 权限控制模块

## 📋 模块概述

AI-Ready前端权限控制模块提供了完整的权限管理解决方案，包括菜单权限、按钮权限、路由守卫、数据权限等功能。

## 🎯 核心功能

### 1. 权限指令 (v-permission, v-role)

```vue
<!-- 单个权限检查 -->
<a-button v-permission="'system:user:add'">添加用户</a-button>

<!-- 多个权限检查（满足任意一个即可） -->
<a-button v-permission="['system:user:edit', 'system:user:delete']">
  编辑/删除用户
</a-button>

<!-- 角色检查 -->
<a-button v-role="'admin'">管理员操作</a-button>

<!-- 多角色检查 -->
<a-button v-role="['admin', 'manager']">管理操作</a-button>
```

### 2. 组合式函数

#### usePermission - 基础权限控制

```typescript
import { usePermission } from '@/composables/usePermission'

const {
  permissions,      // 用户权限列表
  roles,            // 用户角色列表
  isLoggedIn,       // 是否已登录
  isSuperAdminUser, // 是否为超级管理员
  isAdminUser,      // 是否为管理员
  checkPermission,      // 检查单个权限
  checkAnyPermission,   // 检查任意权限
  checkAllPermissions,  // 检查所有权限
  checkRole,            // 检查单个角色
  checkAnyRole          // 检查任意角色
} = usePermission()
```

#### useButtonPermission - 按钮权限控制

```typescript
import { useButtonPermission } from '@/composables/usePermission'

const {
  canOperate,           // 检查按钮是否可操作
  canOperateAny,        // 检查任意按钮是否可操作
  getButtonDisabled,    // 获取按钮禁用状态
  getButtonsDisabled    // 获取多个按钮的禁用状态
} = useButtonPermission()
```

#### useMenuPermission - 菜单权限控制

```typescript
import { useMenuPermission } from '@/composables/usePermission'

const {
  canAccessMenu,    // 检查菜单是否可访问
  canAccessAnyMenu, // 检查任意菜单是否可访问
  filterMenus       // 过滤菜单列表
} = useMenuPermission()
```

#### useDataPermission - 数据权限控制

```typescript
import { useDataPermission } from '@/composables/usePermission'

const {
  checkDataScope,      // 检查数据范围权限
  getDataScope,        // 获取数据范围
  filterByDataScope    // 根据数据权限过滤数据
} = useDataPermission()
```

### 3. 权限工具函数

```typescript
import { hasPermission, hasRole, isSuperAdmin } from '@/utils/permission'

// 检查权限
if (hasPermission('system:user:add')) {
  // 执行操作
}

// 检查角色
if (hasRole('admin')) {
  // 执行操作
}

// 检查是否为超级管理员
if (isSuperAdmin()) {
  // 执行操作
}
```

### 4. 路由权限守卫

```typescript
import { setupRouterGuard } from '@/router/guard'
import router from '@/router'

// 设置路由守卫
setupRouterGuard(router, {
  beforeEach: async (to, from, next) => {
    // 自定义前置守卫逻辑
    next()
  },
  afterEach: (to, from) => {
    // 自定义后置守卫逻辑
  }
})
```

### 5. 动态路由

```typescript
import { loadDynamicRoutes, filterRoutesByPermission } from '@/router/dynamicRoutes'

// 加载动态路由
const routes = await loadDynamicRoutes()

// 根据权限过滤路由
const accessibleRoutes = filterRoutesByPermission(routes)

// 添加到路由
accessibleRoutes.forEach(route => {
  router.addRoute(route)
})
```

## 📂 文件结构

```
src/
├── directives/
│   └── permission.ts          # 权限指令
├── utils/
│   └── permission.ts          # 权限工具函数
├── router/
│   ├── dynamicRoutes.ts       # 动态路由
│   └── guard.ts              # 路由守卫
├── composables/
│   └── usePermission.ts      # 权限组合式函数
├── stores/
│   └── user.ts               # 用户状态管理
└── views/
    └── system/
        ├── permission-example.vue  # 使用示例
        └── permission/             # 权限管理页面
```

## 🚀 使用示例

### 在组件中使用

```vue
<template>
  <div>
    <!-- 使用指令 -->
    <a-button v-permission="'system:user:add'" @click="handleAdd">
      添加用户
    </a-button>

    <!-- 使用组合式函数 -->
    <a-button 
      v-if="canOperate('system:user:edit')"
      @click="handleEdit"
    >
      编辑用户
    </a-button>

    <!-- 禁用按钮 -->
    <a-button
      :disabled="getButtonDisabled('system:user:delete')"
      @click="handleDelete"
    >
      删除用户
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { useButtonPermission } from '@/composables/usePermission'

const { canOperate, getButtonDisabled } = useButtonPermission()

const handleAdd = () => {
  // 添加用户逻辑
}

const handleEdit = () => {
  // 编辑用户逻辑
}

const handleDelete = () => {
  // 删除用户逻辑
}
</script>
```

### 在路由配置中使用

```typescript
// src/router/index.ts
import { setupRouterGuard } from '@/router/guard'

const routes = [
  {
    path: '/dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: {
      title: '仪表盘',
      requiresAuth: true,
      permissions: ['system:dashboard:view'],
      roles: ['admin', 'user']
    }
  }
]

// 设置路由守卫
setupRouterGuard(router)
```

### 在API请求中使用

```typescript
import axios from 'axios'
import { hasPermission } from '@/utils/permission'

// 拦截器中检查权限
axios.interceptors.request.use(config => {
  const requiredPermission = config.meta?.permission
  
  if (requiredPermission && !hasPermission(requiredPermission)) {
    throw new Error('没有权限执行此操作')
  }
  
  return config
})
```

## 🔐 权限设计

### 权限类型

- **目录权限** (`menu:*`) - 控制菜单访问权限
- **按钮权限** (`system:*:*`) - 控制按钮操作权限
- **API权限** (`api:*:*`) - 控制接口访问权限
- **数据权限** (`data:*`) - 控制数据访问范围

### 角色类型

- **超级管理员** - 拥有所有权限
- **管理员** - 拥有管理权限
- **普通用户** - 拥有基础权限

### 数据权限范围

- **全部数据** - 可访问所有数据
- **本部门数据** - 可访问本部门数据
- **本部门及下级数据** - 可访问本部门及子部门数据
- **本人数据** - 只能访问自己的数据

## 📝 权限配置

### 在路由中配置权限

```typescript
{
  path: '/system/user',
  component: () => import('@/views/system/user/index.vue'),
  meta: {
    title: '用户管理',
    permissions: ['system:user:view'],
    roles: ['admin', 'manager'],
    requiresAuth: true
  }
}
```

### 在组件中配置权限

```typescript
// 组件元数据
const componentPermissions = {
  add: 'system:user:add',
  edit: 'system:user:edit',
  delete: 'system:user:delete'
}
```

## ⚠️ 注意事项

1. **超级管理员权限** - 超级管理员拥有所有权限，不受权限限制
2. **权限缓存** - 权限信息缓存在store中，登录时获取
3. **动态加载** - 路由和权限支持动态加载，提升性能
4. **权限更新** - 权限变更后需要重新登录生效
5. **安全性** - 前端权限控制只是辅助，后端必须进行权限验证

## 🔧 常见问题

### Q: 如何自定义权限指令？

A: 在 `src/directives/permission.ts` 中修改或添加新的指令。

### Q: 如何动态添加路由？

A: 使用 `loadDynamicRoutes` 和 `router.addRoute` 方法。

### Q: 如何处理权限变更？

A: 监听权限变更事件，刷新页面或重新加载权限。

### Q: 如何测试权限？

A: 使用 `src/views/system/permission-example.vue` 中的示例页面进行测试。

## 📚 参考文档

- [Vue Router 守卫](https://router.vuejs.org/zh/guide/advanced/navigation-guards.html)
- [Vue 自定义指令](https://cn.vuejs.org/guide/reusability/custom-directives.html)
- [Ant Design Vue](https://antdv.com/)

## 🤝 贡献

欢迎提交问题和改进建议。

## 📄 许可证

MIT License