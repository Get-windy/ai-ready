# AI-Ready 前端组件命名规范

## 目录结构规范

```
@ai-ready/
├── user-management/           # 用户管理模块
│   ├── components/           # 业务组件
│   ├── views/               # 页面视图
│   ├── hooks/               # 自定义Hooks
│   └── api/                 # API接口
├── order-management/         # 订单管理模块
├── inventory-management/     # 库存管理模块
├── purchase-management/      # 采购管理模块
├── finance-management/       # 财务管理模块
├── ai-features/             # AI智能功能模块
└── common/                 # 通用组件
    ├── components/
    ├── hooks/
    └── utils/
```

## 组件命名规范

### 1. 文件命名
- **业务组件**: PascalCase，如 `UserList.vue`, `OrderForm.vue`
- **页面视图**: PascalCase，如 `UserManage.vue`, `OrderDetail.vue`
- **自定义Hooks**: camelCase + `use` 前缀，如 `useUserList.ts`, `useOrderForm.ts`
- **API接口**: camelCase，如 `userApi.ts`, `orderApi.ts`

### 2. 组件内部命名
- **组件名**: 与文件名一致，PascalCase
- **Props**: camelCase，如 `userData`, `orderId`
- **Events**: kebab-case，如 `@user-created`, `@order-updated`
- **Methods**: camelCase，如 `fetchUsers`, `submitOrder`

### 3. 组件组织
- 每个模块独立管理自己的组件、视图、Hooks和API
- 通用组件放在 `common/components/` 目录
- 复杂的组件可以创建子组件目录

### 4. 导出规范
```typescript
// 组件导出
export { default as UserList } from './UserList.vue'

// Hooks导出
export { useUserList } from './hooks/useUserList'
```

## 组件开发规范

### 1. Vue 3.4 最佳实践
- 使用 `<script setup>` 语法糖
- 使用 Composition API
- 定义组件名称用于调试
- 使用 TypeScript 提供类型安全

### 2. Element Plus 集成
- 按需引入组件
- 统一使用 Element Plus 的主题变量
- 保持设计一致性

### 3. 响应式设计
- 使用 Element Plus 的栅格系统
- 支持移动端和桌面端适配
- 考虑 UniApp 跨平台兼容性

## 示例组件模板

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { User } from './types'

// 组件名称
defineOptions({
  name: 'UserList'
})

// Props定义
interface Props {
  users?: User[]
  loading?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  users: () => [],
  loading: false
})

// Events定义
interface Emits {
  (e: 'user-click', user: User): void
  (e: 'user-delete', userId: number): void
}
const emit = defineEmits<Emits>()

// 响应式数据
const searchQuery = ref('')

// 计算属性
const filteredUsers = computed(() => {
  return props.users.filter(user =>
    user.name.includes(searchQuery.value)
  )
})

// 方法
const handleUserClick = (user: User) => {
  emit('user-click', user)
}

// 生命周期
onMounted(() => {
  console.log('UserList mounted')
})
</script>

<template>
  <div class="user-list">
    <!-- 组件内容 -->
  </div>
</template>

<style scoped lang="scss">
.user-list {
  /* 样式 */
}
</style>
```

## 注意事项
1. 保持组件单一职责
2. 避免组件过于庞大，适当拆分
3. 合理使用 Slots 提高组件复用性
4. 编写清晰的注释和文档