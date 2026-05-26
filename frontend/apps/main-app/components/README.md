# AI-Ready 前端组件库

## 目录结构

```
src/components/
├── common/              # 通用基础组件
│   ├── Button/
│   ├── Input/
│   ├── Table/
│   ├── Modal/
│   ├── Loading/
│   └── ...
├── layout/             # 布局组件
│   ├── Layout/
│   ├── Header/
│   ├── Sidebar/
│   ├── Footer/
│   └── ...
├── business/           # 业务组件（按模块划分）
│   ├── user-management/
│   ├── order-management/
│   ├── finance-management/
│   ├── purchase-management/
│   └── ...
├── @ai-ready/          # 发布包组件
│   ├── utils/          # 工具函数包
│   ├── themes/         # 主题系统包
│   ├── styles/         # 样式工具包
│   └── components/     # 公共组件包
└── hooks/             # 公共Hooks
```

## 组件开发规范

### 1. 命名规范
- **组件文件**: 大驼峰命名，如 `UserList.vue`
- **目录命名**: 小写短横线命名，如 `user-management`
- **CSS类名**: BEM命名法，如 `.user-list__item--active`

### 2. 组件结构
每个组件应包含：
```vue
<template>
  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// TypeScript定义
interface Props {
  // 属性定义
}

const props = defineProps<Props>();
</script>

<style scoped lang="scss">
/* 组件样式 */
</style>
```

### 3. 类型定义
- 使用TypeScript严格模式
- 接口命名以 `I` 开头，如 `IUserData`
- 类型定义放在单独的类型文件中

### 4. 样式规范
- 使用CSS Modules或Scoped Styles
- 优先使用CSS变量
- 支持主题切换

### 5. 组件文档
每个组件目录应包含：
- `README.md`: 组件说明文档
- `demo.vue`: 使用示例
- `test.spec.ts`: 单元测试

## 业务模块组件前缀

| 业务模块 | 组件前缀 | 示例 |
|---------|---------|------|
| 用户管理 | user- | UserList, UserForm |
| 订单管理 | order- | OrderTable, OrderDetail |
| 财务管理 | finance- | FinanceReport, InvoiceList |
| 采购管理 | purchase- | PurchaseOrder, SupplierList |
| 监控系统 | monitor- | MonitorChart, AlertList |

## 发布包管理

### @ai-ready/components
通用UI组件库，支持按需导入：
```javascript
import { Button, Table } from '@ai-ready/components';
```

### @ai-ready/utils
工具函数库：
```javascript
import { formatDate, debounce } from '@ai-ready/utils';
```

### @ai-ready/themes
主题系统：
```javascript
import { getCurrentTheme } from '@ai-ready/themes';
```

## 开发流程

1. **创建组件**:
   ```bash
   cd src/components/business/user-management
   touch UserList.vue
   ```

2. **编写文档**:
   ```bash
   touch README.md
   touch demo.vue
   ```

3. **编写测试**:
   ```bash
   touch UserList.spec.ts
   ```

4. **构建发布**:
   ```bash
   cd packages/components
   npm run build
   ```

## 质量要求

- ✅ 单元测试覆盖率 >= 80%
- ✅ 类型检查通过
- ✅ ESLint检查通过
- ✅ 代码复杂度不超过10
- ✅ 组件文档完整