# @ai-ready/role-management

ERP系统用户角色权限管理前端模块。提供完整的角色管理、权限分配、用户授权等功能的前端实现。

## 功能特性

### 🎯 角色管理
- 角色列表展示与搜索过滤
- 角色创建、编辑、删除
- 角色状态管理（启用/禁用）
- 角色级别和类型管理
- 默认角色设置

### 🔐 权限管理
- 权限树形结构展示
- 多层级权限组织
- 权限类型支持（菜单、按钮、API、数据）
- 权限搜索和过滤
- 批量权限操作

### 👥 用户授权
- 用户角色分配界面
- 批量用户授权
- 权限预览功能
- 授权历史记录
- 权限冲突检测

### 📊 可视化与优化
- 权限可视化展示（图表、卡片、标签）
- 响应式设计（桌面端、平板、手机）
- 操作引导和最佳实践提示
- 操作日志和审计追溯

## 技术栈

- **前端框架**: Vue 3.4 + Composition API + TypeScript 5.5
- **UI组件库**: Element Plus 2.4 + Arco Design Vue
- **状态管理**: Pinia 2.1 + Vue Router 4.3
- **图表组件**: ECharts 5.5 + AntV G6
- **表格组件**: VxeTable 4.12 + ProTable
- **构建工具**: Vite 5.3 + UnoCSS
- **测试框架**: Vitest 1.5 + Playwright 1.42

## 安装

```bash
npm install @ai-ready/role-management
# 或
yarn add @ai-ready/role-management
# 或
pnpm add @ai-ready/role-management
```

## 快速开始

### 1. 安装依赖

```bash
pnpm add vue@^3.4.0 pinia@^2.1.0 element-plus@^2.4.0
```

### 2. 在Vue应用中引入

```typescript
import { createApp } from 'vue'
import { createRoleManagementApp, RoleListView } from '@ai-ready/role-management'
import 'element-plus/dist/index.css'

// 方式1：创建独立应用
const app = createRoleManagementApp(RoleListView, '#app')

// 方式2：在现有应用中作为组件使用
import RoleManagement from './RoleManagement.vue'

// RoleManagement.vue
<script setup lang="ts">
import { RoleList, useRoleStore } from '@ai-ready/role-management'

const roleStore = useRoleStore()

onMounted(() => {
  roleStore.fetchRoles()
})
</script>

<template>
  <div class="role-management-container">
    <h2>用户角色权限管理</h2>
    <RoleList />
  </div>
</template>
```

### 3. 配置API端点

在环境变量中配置API基础URL：

```env
VITE_API_BASE_URL=/api
VITE_ROLE_MANAGEMENT_API=/api/role-management
```

或直接在代码中配置：

```typescript
import { roleManagementApi } from '@ai-ready/role-management'

// 修改API基础URL
roleManagementApi.baseUrl = import.meta.env.VITE_ROLE_MANAGEMENT_API || '/api/role-management'
```

## 组件使用

### RoleList 角色列表组件

```vue
<template>
  <RoleList 
    :show-actions="true"
    :enable-export="true"
    @role-selected="handleRoleSelected"
    @role-created="handleRoleCreated"
  />
</template>

<script setup lang="ts">
import { RoleList } from '@ai-ready/role-management'

const handleRoleSelected = (role: Role) => {
  console.log('选中角色:', role)
}

const handleRoleCreated = () => {
  console.log('角色创建成功')
}
</script>
```

### PermissionTree 权限树组件

```vue
<template>
  <PermissionTree
    :tree-data="permissionTree"
    :selected-keys="selectedPermissionIds"
    :check-strictly="false"
    @change="handlePermissionChange"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { PermissionTree } from '@ai-ready/role-management'
import type { PermissionTreeNode } from '@ai-ready/role-management'

const permissionTree = ref<PermissionTreeNode[]>([])
const selectedPermissionIds = ref<string[]>([])

const handlePermissionChange = (keys: string[]) => {
  selectedPermissionIds.value = keys
  console.log('已选权限:', keys)
}
</script>
```

### UserRoleAssign 用户角色分配组件

```vue
<template>
  <UserRoleAssign
    :user-id="currentUserId"
    :available-roles="availableRoles"
    @assign-complete="handleAssignComplete"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UserRoleAssign } from '@ai-ready/role-management'
import type { Role } from '@ai-ready/role-management'

const currentUserId = ref('user-123')
const availableRoles = ref<Role[]>([])

const handleAssignComplete = (result: { userId: string; roleIds: string[] }) => {
  console.log('分配完成:', result)
}
</script>
```

## Store使用

### 角色Store

```typescript
import { useRoleStore } from '@ai-ready/role-management'

const roleStore = useRoleStore()

// 获取角色列表
await roleStore.fetchRoles({ page: 1, pageSize: 20 })

// 创建角色
const newRole = await roleStore.createRole({
  code: 'project-manager',
  name: '项目经理',
  type: 'custom',
  level: 3,
  description: '项目管理相关权限'
})

// 更新角色
await roleStore.updateRole('role-123', { 
  name: '高级项目经理',
  level: 2
})

// 获取角色权限
const permissionIds = await roleStore.fetchRolePermissions('role-123')

// 分配权限
await roleStore.assignRolePermissions('role-123', ['perm-1', 'perm-2', 'perm-3'])
```

### 权限Store

```typescript
import { usePermissionStore } from '@ai-ready/role-management'

const permissionStore = usePermissionStore()

// 获取权限列表
await permissionStore.fetchPermissions()

// 获取权限树
const permissionTree = await permissionStore.fetchPermissionTree()

// 搜索权限
const searchResult = await permissionStore.searchPermissions('用户管理')
```

## API接口

模块提供了完整的API接口，可以直接使用：

```typescript
import { roleManagementApi } from '@ai-ready/role-management'

// 获取角色列表
const { data: roles, total } = await roleManagementApi.getRoles({
  page: 1,
  pageSize: 20,
  keyword: '管理'
})

// 创建权限树
const permissionTree = await roleManagementApi.getPermissionTree()

// 用户角色分配
await roleManagementApi.assignUserRoles({
  userId: 'user-123',
  roleIds: ['role-1', 'role-2'],
  assignedBy: 'admin'
})

// 获取授权历史
const { data: history } = await roleManagementApi.getAuthorizationHistory({
  page: 1,
  pageSize: 50,
  startDate: '2024-01-01',
  endDate: '2024-12-31'
})
```

## 主题定制

模块支持Element Plus的主题定制：

```scss
// 自定义主题变量
:root {
  --el-color-primary: #409eff;
  --el-color-success: #67c23a;
  --el-color-warning: #e6a23c;
  --el-color-danger: #f56c6c;
  --el-color-info: #909399;
}

// 自定义组件样式
.role-management-container {
  .el-card {
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  
  .el-table {
    --el-table-header-bg-color: #f5f7fa;
    --el-table-row-hover-bg-color: #f5f7fa;
  }
}
```

## 响应式设计

模块内置响应式支持，自动适配不同设备：

```css
/* 移动端适配 */
@media (max-width: 768px) {
  .role-list-container {
    padding: 8px;
  }
  
  .filter-section .el-form-item {
    width: 100%;
    margin-right: 0;
  }
  
  .el-table {
    font-size: 12px;
  }
}

/* 平板适配 */
@media (min-width: 769px) and (max-width: 1024px) {
  .role-list-container {
    padding: 12px;
  }
}
```

## 测试

### 单元测试

```bash
# 运行所有测试
pnpm test

# 运行测试并生成覆盖率报告
pnpm test:coverage

# 使用UI模式运行测试
pnpm test:ui
```

### 组件测试示例

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import RoleList from '../src/components/RoleList/RoleList.vue'

describe('RoleList', () => {
  it('渲染角色列表', () => {
    const wrapper = mount(RoleList)
    expect(wrapper.find('.role-list-container').exists()).toBe(true)
  })
  
  it('显示搜索框', () => {
    const wrapper = mount(RoleList)
    expect(wrapper.find('.search-input').exists()).toBe(true)
  })
})
```

## 开发指南

### 项目结构

```
role-management/
├── src/
│   ├── components/          # 组件目录
│   │   ├── RoleList/       # 角色列表组件
│   │   ├── RoleForm/       # 角色表单组件
│   │   ├── PermissionTree/ # 权限树组件
│   │   └── UserRoleAssign/ # 用户角色分配组件
│   ├── views/              # 页面视图
│   ├── store/              # Pinia Store
│   ├── types/              # TypeScript类型定义
│   ├── api/                # API接口
│   ├── utils/              # 工具函数
│   └── main.ts             # 主入口文件
├── tests/                  # 测试文件
├── docs/                   # 文档
└── package.json
```

### 开发命令

```bash
# 开发模式
pnpm dev

# 构建生产版本
pnpm build

# 代码检查
pnpm lint

# 代码格式化
pnpm format

# 运行测试
pnpm test
```

## 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启Pull Request

## 许可证

MIT License © 2024 AI-Ready Team

## 支持

- 文档: [https://docs.ai-ready.com/role-management](https://docs.ai-ready.com/role-management)
- 问题: [GitHub Issues](https://github.com/ai-ready/role-management/issues)
- 讨论: [GitHub Discussions](https://github.com/ai-ready/role-management/discussions)