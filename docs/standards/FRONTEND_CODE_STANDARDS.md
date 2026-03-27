# 智企连·AI-Ready 前端代码规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. 项目结构规范

### 1.1 目录结构

```
smart-admin-web/
├── src/
│   ├── api/                 # API 接口封装
│   │   ├── modules/         # 按模块划分
│   │   │   ├── user.ts      # 用户相关 API
│   │   │   └── order.ts     # 订单相关 API
│   │   └── index.ts         # 统一导出
│   ├── assets/              # 静态资源
│   │   ├── images/          # 图片
│   │   ├── styles/          # 全局样式
│   │   └── fonts/           # 字体
│   ├── components/          # 公共组件
│   │   ├── common/          # 通用组件
│   │   ├── business/        # 业务组件
│   │   └── layout/          # 布局组件
│   ├── composables/         # 组合式函数 (Hooks)
│   │   ├── useUser.ts
│   │   └── useTable.ts
│   ├── directives/          # 自定义指令
│   ├── enums/               # 枚举定义
│   ├── router/              # 路由配置
│   │   ├── modules/         # 路由模块
│   │   └── index.ts
│   ├── store/               # 状态管理 (Pinia)
│   │   ├── modules/
│   │   │   ├── user.ts
│   │   │   └── app.ts
│   │   └── index.ts
│   ├── types/               # TypeScript 类型定义
│   │   ├── api.d.ts         # API 响应类型
│   │   └── global.d.ts      # 全局类型
│   ├── utils/               # 工具函数
│   │   ├── request.ts       # Axios 封装
│   │   ├── storage.ts       # 存储工具
│   │   └── format.ts        # 格式化工具
│   ├── views/               # 页面视图
│   │   ├── dashboard/
│   │   ├── system/
│   │   └── error/
│   ├── App.vue              # 根组件
│   └── main.ts              # 入口文件
├── public/                  # 公共资源
├── .env.development         # 开发环境变量
├── .env.production          # 生产环境变量
├── vite.config.ts           # Vite 配置
├── tsconfig.json            # TypeScript 配置
└── package.json
```

---

## 2. 命名规范

### 2.1 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 组件文件 | 大驼峰 | `UserList.vue`, `SearchForm.vue` |
| 组合式函数 | 小驼峰 + use 前缀 | `useTable.ts`, `useUser.ts` |
| 工具函数 | 小驼峰 | `formatDate.ts`, `request.ts` |
| 类型文件 | 小驼峰 + .d.ts | `user.d.ts`, `api.d.ts` |
| 样式文件 | 小驼峰 | `variables.scss`, `common.scss` |

### 2.2 组件命名

```vue
<!-- ✅ 多词组件名，避免与 HTML 元素冲突 -->
<template>
  <UserList />
  <SearchForm />
  <DatePicker />
</template>

<!-- ❌ 单词组件名 -->
<template>
  <User />
  <Form />
  <Picker />
</template>
```

### 2.3 变量命名

```typescript
// ✅ 小驼峰命名
const userName = 'admin'
const isLoading = false
const userList = []

// ✅ 布尔值使用 is/has/can 前缀
const isActive = true
const hasPermission = false
const canEdit = true

// ✅ 常量全大写下划线
const API_BASE_URL = '/api/v1'
const MAX_PAGE_SIZE = 100

// ✅ 私有变量下划线前缀
const _privateVar = 'secret'
```

### 2.4 函数命名

```typescript
// ✅ 动词开头
function getUserInfo() {}
function handleSubmit() {}
function validateForm() {}

// ✅ 事件处理使用 handle 前缀
const handleClick = () => {}
const handleInputChange = () => {}

// ✅ 异步函数可加 async 后缀
async function fetchUserListAsync() {}
```

---

## 3. TypeScript 规范

### 3.1 类型定义

```typescript
// ✅ 使用 interface 定义对象类型
interface User {
  id: number
  username: string
  email: string
  status: 'active' | 'inactive'
  createdAt: string
}

// ✅ 使用 type 定义联合类型、交叉类型
type Status = 'active' | 'inactive' | 'locked'
type UserWithRole = User & { role: string }

// ✅ 使用泛型
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ✅ 使用 readonly
interface Config {
  readonly apiUrl: string
  readonly timeout: number
}
```

### 3.2 枚举定义

```typescript
// ✅ 使用 const enum 提升性能
const enum UserStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  LOCKED = 'locked'
}

// ✅ 字符串枚举
enum ErrorCode {
  SUCCESS = 'SUCCESS',
  PARAM_ERROR = 'PARAM_ERROR',
  SYSTEM_ERROR = 'SYSTEM_ERROR'
}
```

### 3.3 类型导入

```typescript
// ✅ 使用 import type 导入类型
import type { User, UserQuery } from '@/types/user'

// ✅ 普通导入
import { getUserList, createUser } from '@/api/user'
```

---

## 4. Vue 3 组合式 API 规范

### 4.1 组件结构

```vue
<template>
  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 1. 导入
import { ref, computed, onMounted } from 'vue'
import type { User } from '@/types/user'
import { getUserList } from '@/api/user'

// 2. Props 定义
const props = defineProps<{
  id: number
}>()

// 3. Emits 定义
const emit = defineEmits<{
  (e: 'update', value: User): void
  (e: 'delete', id: number): void
}>()

// 4. 响应式数据
const loading = ref(false)
const userList = ref<User[]>([])

// 5. 计算属性
const activeUsers = computed(() => 
  userList.value.filter(u => u.status === 'active')
)

// 6. 方法
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList()
    userList.value = res.data
  } finally {
    loading.value = false
  }
}

// 7. 生命周期
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
/* 样式 */
</style>
```

### 4.2 组合式函数 (Hooks)

```typescript
// composables/useTable.ts
import { ref, reactive, computed } from 'vue'
import type { TableProps } from 'ant-design-vue'

export interface TableOptions<T> {
  fetchData: (params: any) => Promise<{ list: T[]; total: number }>
  defaultPageSize?: number
}

export function useTable<T>(options: TableOptions<T>) {
  const { fetchData, defaultPageSize = 10 } = options
  
  const loading = ref(false)
  const dataSource = ref<T[]>([])
  const pagination = reactive({
    current: 1,
    pageSize: defaultPageSize,
    total: 0
  })
  
  const loadData = async () => {
    loading.value = true
    try {
      const { list, total } = await fetchData({
        page: pagination.current,
        pageSize: pagination.pageSize
      })
      dataSource.value = list
      pagination.total = total
    } finally {
      loading.value = false
    }
  }
  
  const handleTableChange: TableProps['onChange'] = (pag) => {
    pagination.current = pag.current || 1
    pagination.pageSize = pag.pageSize || defaultPageSize
    loadData()
  }
  
  return {
    loading,
    dataSource,
    pagination,
    loadData,
    handleTableChange
  }
}
```

### 4.3 Props 和 Emits

```typescript
// ✅ 使用 TypeScript 泛型定义
const props = defineProps<{
  modelValue: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

// ✅ 带默认值
const props = withDefaults(defineProps<{
  title: string
  size?: 'small' | 'medium' | 'large'
}>(), {
  size: 'medium'
})
```

---

## 5. 样式规范

### 5.1 使用 Scoped 样式

```vue
<template>
  <div class="user-list">
    <div class="user-item">{{ user.name }}</div>
  </div>
</template>

<style scoped lang="scss">
.user-list {
  padding: 16px;
  
  .user-item {
    padding: 8px;
    border-bottom: 1px solid #eee;
    
    &:hover {
      background: #f5f5f5;
    }
  }
}
</style>
```

### 5.2 CSS 变量

```scss
// assets/styles/variables.scss
:root {
  --primary-color: #1890ff;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
  
  --border-radius: 4px;
  --transition-duration: 0.3s;
}
```

### 5.3 类名命名

```scss
// ✅ BEM 命名规范
.user-list { }
.user-list__item { }
.user-list__item--active { }

// ✅ 功能性命名
.is-active { }
.is-loading { }
.has-error { }
```

---

## 6. API 请求规范

### 6.1 Axios 封装

```typescript
// utils/request.ts
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code === 200) {
      return data.data
    }
    message.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (error) => {
    const { response } = error
    if (response?.status === 401) {
      // 跳转登录
      window.location.href = '/login'
    } else {
      message.error(response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
```

### 6.2 API 模块化

```typescript
// api/modules/user.ts
import request from '@/utils/request'
import type { User, UserQuery, UserForm } from '@/types/user'

export const userApi = {
  // 获取用户列表
  getList(params: UserQuery) {
    return request.get<{ list: User[]; total: number }>('/users', { params })
  },
  
  // 获取用户详情
  getDetail(id: number) {
    return request.get<User>(`/users/${id}`)
  },
  
  // 创建用户
  create(data: UserForm) {
    return request.post<User>('/users', data)
  },
  
  // 更新用户
  update(id: number, data: Partial<UserForm>) {
    return request.put<User>(`/users/${id}`, data)
  },
  
  // 删除用户
  delete(id: number) {
    return request.delete(`/users/${id}`)
  }
}
```

---

## 7. 状态管理规范

### 7.1 Pinia Store

```typescript
// store/modules/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import { userApi } from '@/api/modules/user'

export const useUserStore = defineStore('user', () => {
  // State
  const userInfo = ref<User | null>(null)
  const token = ref<string>('')
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userName = computed(() => userInfo.value?.username || '')
  
  // Actions
  async function login(username: string, password: string) {
    const res = await userApi.login({ username, password })
    token.value = res.token
    userInfo.value = res.user
  }
  
  function logout() {
    token.value = ''
    userInfo.value = null
  }
  
  return {
    userInfo,
    token,
    isLoggedIn,
    userName,
    login,
    logout
  }
})
```

---

## 8. 路由规范

### 8.1 路由配置

```typescript
// router/modules/system.ts
import type { RouteRecordRaw } from 'vue-router'

const systemRoutes: RouteRecordRaw[] = [
  {
    path: '/system',
    component: () => import('@/layouts/BasicLayout.vue'),
    meta: { title: '系统管理', icon: 'setting' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', keepAlive: true }
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' }
      }
    ]
  }
]

export default systemRoutes
```

### 8.2 路由守卫

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

const router = createRouter({
  history: createWebHistory(),
  routes: []
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
```

---

## 9. 组件规范

### 9.1 通用组件

```vue
<!-- components/common/Modal.vue -->
<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    :width="width"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <slot></slot>
    <template #footer>
      <slot name="footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleOk">
          确定
        </a-button>
      </slot>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  open: boolean
  title?: string
  width?: number
  loading?: boolean
}>(), {
  title: '提示',
  width: 520,
  loading: false
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'ok'): void
  (e: 'cancel'): void
}>()

const visible = ref(props.open)

watch(() => props.open, (val) => {
  visible.value = val
})

const handleOk = () => {
  emit('ok')
}

const handleCancel = () => {
  visible.value = false
  emit('update:open', false)
  emit('cancel')
}
</script>
```

### 9.2 组件文档

```vue
<!--
@component UserSelect
@description 用户选择器组件

@example
<UserSelect
  v-model="selectedUserId"
  :multiple="true"
  placeholder="请选择用户"
  @change="handleUserChange"
/>
-->
```

---

## 10. 性能优化

### 10.1 懒加载

```typescript
// ✅ 路由懒加载
const UserList = () => import('@/views/system/user/index.vue')

// ✅ 组件懒加载
const AsyncComponent = defineAsyncComponent(() =>
  import('@/components/HeavyComponent.vue')
)
```

### 10.2 虚拟列表

```vue
<template>
  <a-list
    :data-source="largeList"
    :virtual="true"
    :height="400"
  >
    <template #renderItem="{ item }">
      <a-list-item>{{ item.name }}</a-list-item>
    </template>
  </a-list>
</template>
```

### 10.3 防抖节流

```typescript
import { debounce, throttle } from 'lodash-es'

// 搜索防抖
const handleSearch = debounce((keyword: string) => {
  fetchSearchResults(keyword)
}, 300)

// 滚动节流
const handleScroll = throttle(() => {
  checkScrollPosition()
}, 200)
```

---

## 11. 代码质量

### 11.1 ESLint 配置

```json
// .eslintrc.json
{
  "extends": [
    "eslint:recommended",
    "plugin:vue/vue3-recommended",
    "@vue/eslint-config-typescript",
    "@vue/eslint-config-prettier"
  ],
  "rules": {
    "vue/multi-word-component-names": "error",
    "vue/no-unused-vars": "error",
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/explicit-function-return-type": "warn"
  }
}
```

### 11.2 Git Hooks

```json
// package.json
{
  "lint-staged": {
    "*.{js,ts,vue}": ["eslint --fix", "prettier --write"],
    "*.{css,scss}": ["prettier --write"]
  }
}
```

---

## 12. 参考资料

- [Vue 3 官方文档](https://vuejs.org/)
- [Ant Design Vue](https://antdv.com/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Vue Style Guide](https://vuejs.org/style-guide/)

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer