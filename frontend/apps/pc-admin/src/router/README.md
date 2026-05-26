# AI-Ready 前端路由配置优化说明

## 优化内容

### 1. 路由懒加载优化

**优化前**：
```typescript
component: () => import('@/views/dashboard/index.vue')
```

**优化后**：
- 添加 loading 组件显示加载状态
- 添加错误处理组件
- 配置延迟和超时时间
- 使用 webpackChunkName 实现代码分块

### 2. 路由守卫优化

**新增功能**：
- ✅ NProgress 进度条显示
- ✅ 页面标题自动设置
- ✅ Token 过期自动跳转登录
- ✅ 权限检查（基于 permissions）
- ✅ 登录页已登录自动跳转首页
- ✅ 路由错误处理

### 3. 动态路由配置

**新增文件**：
- `src/router/dynamic.ts` - 动态路由工具函数

**功能**：
- 根据用户权限过滤路由
- 动态添加/移除路由
- 菜单数据转路由配置

### 4. 路由缓存策略

**新增组件**：
- `src/layouts/components/RouterCache.vue` - 路由缓存包装器
- `src/stores/tagsView.ts` - 标签页状态管理

**缓存规则**：
- 需要在路由 meta 中设置 `keepAlive: true`
- 支持设置最大缓存数量（默认20）
- 支持动态添加/移除缓存

### 5. 路由元信息扩展

```typescript
interface RouteMeta {
  title?: string         // 页面标题
  icon?: string          // 菜单图标
  requiresAuth?: boolean // 是否需要认证
  permissions?: string[] // 所需权限
  roles?: string[]       // 所需角色
  keepAlive?: boolean    // 是否缓存
  hidden?: boolean       // 是否隐藏菜单
  breadcrumb?: boolean   // 是否显示面包屑
  affix?: boolean        // 是否固定在标签栏
}
```

## 文件清单

| 文件 | 说明 |
|------|------|
| `src/router/index.ts` | 主路由配置（优化后） |
| `src/router/dynamic.ts` | 动态路由工具函数 |
| `src/stores/tagsView.ts` | 标签页状态管理 |
| `src/layouts/components/RouterCache.vue` | 路由缓存包装器 |
| `src/layouts/components/Breadcrumb.vue` | 面包屑组件 |
| `src/components/Loading/RouteLoading.vue` | 路由加载中组件 |
| `src/components/Loading/RouteError.vue` | 路由加载错误组件 |

## 使用示例

### 路由配置示例

```typescript
{
  path: 'customer',
  name: 'Customer',
  component: () => import('@/views/crm/customer/index.vue'),
  meta: { 
    title: '客户管理', 
    icon: 'TeamOutlined',
    permissions: ['crm:customer:view'],
    keepAlive: true
  }
}
```

### 权限检查

```typescript
// 在路由守卫中自动检查
// 用户必须有 'crm:customer:view' 权限才能访问

// 手动检查
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
if (userStore.permissions.includes('crm:customer:view')) {
  // 有权限
}
```

### 路由缓存

```typescript
// 在路由配置中启用缓存
meta: { keepAlive: true }

// 或在组件中使用
import { useTagsViewStore } from '@/stores/tagsView'

const tagsViewStore = useTagsViewStore()
tagsViewStore.addCachedView('Customer')
```

## 性能优化效果

1. **首屏加载时间减少**：路由懒加载 + 代码分块
2. **页面切换体验提升**：loading状态 + 进度条
3. **内存占用优化**：路由缓存限制最大数量
4. **权限控制细化**：精确到按钮级别

---
**更新时间**: 2026-03-30
**负责人**: team-member