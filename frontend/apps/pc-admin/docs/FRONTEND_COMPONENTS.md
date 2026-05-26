# AI-Ready 前端组件说明文档

## 📖 项目概述

AI-Ready 管理系统前端是一个基于 Vue 3 + TypeScript + Ant Design Vue 的现代化企业级管理系统，提供完整的用户管理、客户管理、订单管理等功能模块。

## 🛠 技术栈

### 核心框架
- **Vue 3.4+**: 采用 Composition API 和 `<script setup>` 语法
- **TypeScript 5.3+**: 提供完整的类型支持
- **Vite 5.1+**: 快速的构建工具

### UI 框架
- **Ant Design Vue 4.1+**: 企业级 UI 组件库
- **@ant-design/icons-vue 7.0+**: 图标库

### 状态管理
- **Pinia 2.1+**: Vue 官方推荐的状态管理库

### 路由
- **Vue Router 4.3+**: 官方路由管理器

### 工具库
- **Axios 1.6+**: HTTP 请求库
- **Day.js 1.11+**: 日期处理库
- **Lodash-es 4.17+**: 工具函数库

### 开发工具
- **ESLint 8.56+**: 代码规范检查
- **Vitest 1.2+**: 单元测试框架

## 📁 目录结构

```
src/
├── api/                    # API 接口定义
│   ├── user.ts            # 用户相关 API
│   ├── customer.ts        # 客户相关 API
│   ├── order.ts           # 订单相关 API
│   ├── role.ts            # 角色相关 API
│   ├── permission.ts      # 权限相关 API
│   └── purchase.ts        # 采购相关 API
├── components/            # 公共组件
│   ├── Charts/            # 图表组件
│   ├── ConfirmDialog/     # 确认对话框
│   ├── EmptyState/        # 空状态组件
│   ├── ErrorBoundary/     # 错误边界
│   ├── Form/              # 表单组件
│   ├── FormBuilder/       # 表单构建器
│   ├── Loading/           # 加载组件
│   ├── LoadingButton/     # 加载按钮
│   ├── LocaleSwitch/      # 语言切换
│   ├── SearchBar/         # 搜索栏
│   └── TableList/         # 表格列表
├── views/                 # 页面组件
│   ├── dashboard/         # 工作台
│   ├── login/             # 登录页面
│   ├── error/             # 错误页面
│   ├── system/            # 系统管理
│   │   ├── user/          # 用户管理
│   │   ├── role/          # 角色管理
│   │   ├── permission/    # 权限管理
│   │   └── menu/          # 菜单管理
│   ├── crm/               # 客户关系管理
│   │   ├── customer/      # 客户管理
│   │   └── lead/          # 线索管理
│   └── erp/               # 企业资源计划
│       ├── purchase/      # 采购管理
│       ├── sale/          # 销售管理
│       └── stock/         # 库存管理
├── router/                # 路由配置
│   ├── index.ts           # 路由主文件
│   ├── guard.ts           # 路由守卫
│   ├── dynamic.ts         # 动态路由
│   └── dynamicRoutes.ts   # 动态路由加载
├── stores/                # Pinia 状态管理
├── utils/                 # 工具函数
├── styles/                # 全局样式
├── layouts/               # 布局组件
├── composables/           # 组合式函数
├── directives/            # 自定义指令
└── locales/               # 国际化配置
```

## 🧩 核心组件说明

### 1. 用户管理组件

**位置**: `src/views/system/user/index.vue`

**功能特性**:
- ✅ 用户列表展示（分页、搜索、筛选）
- ✅ 用户增删改查（CRUD）
- ✅ 批量删除用户
- ✅ 用户状态切换（启用/停用）
- ✅ 用户角色分配
- ✅ 密码重置
- ✅ 用户类型管理（超级管理员/管理员/普通用户）

**主要功能**:
- 搜索：支持按用户名、手机号、状态筛选
- 新增：创建新用户并设置密码
- 编辑：修改用户信息（不包括密码）
- 删除：单个删除和批量删除
- 角色分配：使用 Transfer 组件分配角色
- 状态管理：一键切换用户状态

**API 接口**:
```typescript
import { userApi } from '@/api/user'

// 获取用户列表
userApi.getPage({ tenantId: 1, pageNum: 1, pageSize: 10 })

// 创建用户
userApi.create({ username: 'test', password: '123456', ... })

// 更新用户
userApi.update(userId, { nickname: '新昵称' })

// 删除用户
userApi.delete(userId)

// 批量删除
userApi.batchDelete([id1, id2, id3])

// 重置密码
userApi.resetPassword(userId, 'newPassword')

// 分配角色
userApi.assignRoles(userId, [roleId1, roleId2])

// 更新状态
userApi.updateStatus(userId, 1)
```

### 2. 客户管理组件

**位置**: `src/views/crm/customer/index.vue`

**功能特性**:
- ✅ 客户列表展示（分页、搜索、筛选）
- ✅ 客户增删改查
- ✅ 客户等级管理（VIP/重要/普通/潜在）
- ✅ 行业分类
- ✅ 客户状态管理
- ✅ 客户跟进记录
- ✅ 导出功能

**主要功能**:
- 搜索：支持按客户名称、等级、状态筛选
- 新增：创建新客户信息
- 编辑：修改客户信息
- 删除：删除客户
- 跟进：添加客户跟进记录
- 查看跟进记录：查看客户历史跟进
- 查看订单记录：查看客户相关订单
- 导出：导出客户数据

**API 接口**:
```typescript
import { customerApi } from '@/api/customer'

// 获取客户列表
customerApi.getPage({ tenantId: 1, pageNum: 1, pageSize: 10 })

// 创建客户
customerApi.create({ name: '客户名称', code: 'C001', ... })

// 更新客户
customerApi.update(customerId, { name: '新名称' })

// 删除客户
customerApi.delete(customerId)

// 添加跟进记录
customerApi.addFollowRecord(customerId, {
  followType: 1,
  content: '跟进内容',
  result: 3
})

// 获取跟进记录
customerApi.getFollowRecords(customerId, { pageNum: 1, pageSize: 10 })

// 导出客户
customerApi.export({ tenantId: 1 })
```

### 3. 销售订单管理组件

**位置**: `src/views/erp/sale/index.vue`

**功能特性**:
- ✅ 订单列表展示（分页、搜索、筛选）
- ✅ 订单状态管理（草稿/待审批/已审批/部分发货/完成/已取消）
- ✅ 订单流程管理
- ✅ 订单审批功能
- ✅ 订单导出

**主要功能**:
- 搜索：支持按订单号、客户、状态筛选
- 新建：创建销售订单
- 编辑：编辑草稿状态订单
- 提交：提交订单审批
- 审批：审批待审批订单
- 删除：删除草稿状态订单
- 导出：导出订单数据

**API 接口**:
```typescript
import { salesOrderApi } from '@/api/order'

// 获取订单列表
salesOrderApi.getPage({ tenantId: 1, pageNum: 1, pageSize: 10 })

// 创建订单
salesOrderApi.create({ orderNo: 'SO20240409001', customerId: 1, ... })

// 更新订单
salesOrderApi.update(orderId, { remark: '更新备注' })

// 删除订单
salesOrderApi.delete(orderId)

// 提交审批
salesOrderApi.submit(orderId)

// 审批订单
salesOrderApi.approve(orderId, true, '审批通过')

// 取消订单
salesOrderApi.cancel(orderId, '取消原因')

// 导出订单
salesOrderApi.export({ tenantId: 1 })

// 获取统计
salesOrderApi.getStatistics({ startDate: '2024-01-01', endDate: '2024-12-31' })
```

## 🔧 公共组件

### SearchBar 搜索栏组件
提供统一的搜索表单布局，支持多种表单项。

### TableList 表格列表组件
封装了常用的表格功能，包括分页、排序、筛选等。

### FormBuilder 表单构建器
基于配置动态生成表单，简化表单开发。

### LoadingButton 加载按钮
带加载状态的按钮组件。

### EmptyState 空状态组件
展示无数据状态的统一组件。

## 🎨 样式规范

### 颜色系统
```css
/* 主色调 */
--primary-color: #1890ff;
--success-color: #52c41a;
--warning-color: #faad14;
--error-color: #ff4d4f;
--info-color: #1890ff;

/* 中性色 */
--text-primary: #262626;
--text-secondary: #595959;
--text-disabled: #bfbfbf;
--border-color: #d9d9d9;
--background-color: #f0f2f5;
```

### 间距系统
```css
--spacing-xs: 4px;
--spacing-sm: 8px;
--spacing-md: 16px;
--spacing-lg: 24px;
--spacing-xl: 32px;
```

## 🚀 快速开始

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

### 运行测试
```bash
npm run test
```

## 📝 开发规范

### 命名规范
- **组件文件**: 使用 PascalCase，如 `UserManagement.vue`
- **工具函数**: 使用 camelCase，如 `formatDate.ts`
- **常量**: 使用 UPPER_SNAKE_CASE，如 `API_BASE_URL`
- **接口/类型**: 使用 PascalCase，如 `UserInfo`, `CustomerQuery`

### 代码风格
- 使用 `<script setup>` 语法
- 优先使用 Composition API
- 使用 TypeScript 类型注解
- 遵循 ESLint 规则

### 注释规范
```typescript
/**
 * 获取用户列表
 * @param params - 查询参数
 * @returns Promise<ApiResponse<PageResponse<UserInfo>>>
 */
getPage(params: UserQuery): Promise<ApiResponse<PageResponse<UserInfo>>> {
  return request.get('/user/page', { params })
}
```

## 🔐 权限控制

### 路由权限
在路由 meta 中配置权限：
```typescript
{
  path: 'user',
  name: 'SystemUser',
  component: () => import('@/views/system/user/index.vue'),
  meta: { 
    title: '用户管理', 
    icon: 'UserOutlined',
    permissions: ['system:user:view']
  }
}
```

### 按钮权限
使用自定义指令控制按钮显示：
```vue
<a-button v-permission="['system:user:create']">新增</a-button>
```

## 📊 性能优化

### 路由懒加载
```typescript
const UserManagement = () => import('@/views/system/user/index.vue')
```

### 组件懒加载
```vue
<a-button v-if="showDetail">
  <DetailComponent />
</a-button>
```

### 列表虚拟滚动
对于大数据量列表，使用虚拟滚动提升性能。

### 图片懒加载
使用 `loading="lazy"` 属性实现图片懒加载。

## 🧪 测试

### 单元测试示例
```typescript
import { describe, it, expect } from 'vitest'
import { userApi } from '@/api/user'

describe('User API', () => {
  it('should get user list', async () => {
    const result = await userApi.getPage({ pageNum: 1, pageSize: 10 })
    expect(result.data).toBeDefined()
  })
})
```

## 📚 参考资料

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [Ant Design Vue 官方文档](https://antdv.com/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。

---

**维护者**: AI-Ready 开发团队  
**最后更新**: 2026-04-09