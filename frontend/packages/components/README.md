# @ai-ready/ui-components

AI-Ready ERP 系统统一前端组件库

## 🚀 特性

- **现代化技术栈**: Vue 3.4 + TypeScript + Vite
- **完整类型支持**: 100% TypeScript 覆盖
- **响应式设计**: 移动端优先，支持多端适配
- **主题系统**: 支持亮色/深色主题切换
- **无障碍访问**: 符合 WCAG 2.1 标准
- **模块化设计**: 支持按需引入和 Tree Shaking

## 📦 安装

```bash
# 使用 npm
npm install @ai-ready/ui-components

# 使用 yarn
yarn add @ai-ready/ui-components

# 使用 pnpm
pnpm add @ai-ready/ui-components
```

## 🔧 快速开始

### 完整引入

```typescript
import { createApp } from 'vue'
import AIReadyUI from '@ai-ready/ui-components'
import '@ai-ready/ui-components/dist/style.css'

const app = createApp(App)
app.use(AIReadyUI)
app.mount('#app')
```

### 按需引入

```typescript
import { ARButton, ARInput } from '@ai-ready/ui-components'

// 在组件中使用
export default {
  components: {
    ARButton,
    ARInput
  }
}
```

### Vite 配置自动导入（推荐）

```typescript
// vite.config.ts
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { AIReadyUIResolver } from '@ai-ready/ui-components/resolver'

export default defineConfig({
  plugins: [
    AutoImport({
      imports: ['vue'],
      resolvers: [AIReadyUIResolver()]
    }),
    Components({
      resolvers: [AIReadyUIResolver()]
    })
  ]
})
```

## 📚 组件列表

### 基础组件

| 组件 | 描述 | 状态 |
|------|------|------|
| `ARButton` | 增强型按钮组件 | ✅ |
| `ARInput` | 增强型输入框组件 | ✅ |
| `ARTable` | 增强型表格组件 | 🚧 |
| `ARDialog` | 增强型对话框组件 | 🚧 |
| `ARLayout` | 布局组件 | 🚧 |

### ERP 业务组件

| 组件 | 描述 | 状态 |
|------|------|------|
| `SupplierTable` | 供应商信息表格 | 🚧 |
| `PurchaseOrderForm` | 采购订单表单 | 🚧 |
| `InventoryDashboard` | 库存仪表板 | 🚧 |
| `FinancialChart` | 财务图表 | 🚧 |

## 🎨 主题定制

### CSS 变量

组件库使用 CSS 自定义属性进行样式控制：

```css
:root {
  --ar-color-primary: #409eff;
  --ar-color-success: #67c23a;
  --ar-color-warning: #e6a23c;
  --ar-color-danger: #f56c6c;
  --ar-color-info: #909399;
  
  --ar-font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  --ar-font-size-base: 14px;
  --ar-border-radius-base: 4px;
}
```

### 深色主题

```css
[data-theme="dark"] {
  --ar-bg-color: #141414;
  --ar-text-color-primary: #e5e5e5;
  --ar-border-color-base: #434343;
}
```

### 动态切换主题

```typescript
// 切换到深色主题
document.documentElement.setAttribute('data-theme', 'dark')

// 切换回亮色主题
document.documentElement.removeAttribute('data-theme')
```

## 📝 使用示例

### ARButton 组件

```vue
<template>
  <ARButton type="primary" size="large" :loading="loading" @click="handleClick">
    主要按钮
  </ARButton>
  
  <ARButton type="success" icon="ar-icon-check" round>
    成功按钮
  </ARButton>
  
  <ARButton type="warning" disabled>
    禁用按钮
  </ARButton>
</template>

<script setup>
const loading = ref(false)

const handleClick = () => {
  loading.value = true
  // 执行异步操作
  setTimeout(() => {
    loading.value = false
  }, 1000)
}
</script>
```

### ARInput 组件

```vue
<template>
  <ARInput
    v-model="username"
    placeholder="请输入用户名"
    prefix-icon="ar-icon-user"
    clearable
  />
  
  <ARInput
    v-model="password"
    type="password"
    placeholder="请输入密码"
    show-password
    suffix-icon="ar-icon-lock"
  />
  
  <ARInput
    v-model="email"
    placeholder="请输入邮箱"
    :error="emailError"
    @blur="validateEmail"
  />
</template>

<script setup>
const username = ref('')
const password = ref('')
const email = ref('')
const emailError = ref('')

const validateEmail = () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.value)) {
    emailError.value = '邮箱格式不正确'
  } else {
    emailError.value = ''
  }
}
</script>
```

## 🛠️ 工具函数

组件库提供丰富的工具函数：

```typescript
import {
  formatDate,
  formatCurrency,
  debounce,
  throttle,
  deepClone,
  isValidEmail,
  isValidPhone
} from '@ai-ready/ui-components'

// 格式化日期
const dateStr = formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss')

// 格式化金额
const amount = formatCurrency(1234567.89) // ¥1,234,567.89

// 防抖函数
const search = debounce((keyword) => {
  // 搜索逻辑
}, 300)

// 验证邮箱
const isValid = isValidEmail('user@example.com') // true
```

## 📊 类型定义

完整的 TypeScript 类型支持：

```typescript
import type {
  ButtonProps,
  InputProps,
  TableColumn,
  Supplier,
  PurchaseOrder
} from '@ai-ready/ui-components'

// 使用类型
const buttonProps: ButtonProps = {
  type: 'primary',
  size: 'large',
  loading: false
}

const supplier: Supplier = {
  id: '1',
  name: '示例供应商',
  contact: '张三',
  phone: '13800138000',
  status: 'active'
}
```

## 🎯 开发指南

### 项目结构

```
src/
├── base/              # 基础组件
│   ├── button/
│   ├── input/
│   ├── table/
│   └── ...
├── business/          # ERP 业务组件
│   ├── erp-purchase/
│   ├── erp-order/
│   ├── erp-inventory/
│   └── erp-finance/
├── styles/            # 样式系统
│   ├── variables.scss
│   └── index.scss
├── utils/             # 工具函数
├── types/             # 类型定义
└── index.ts           # 主入口文件
```

### 开发新组件

1. 创建组件目录和文件
2. 实现组件逻辑和样式
3. 编写类型定义
4. 添加导出文件
5. 更新主入口文件

### 代码规范

- 使用 Composition API 编写 Vue 组件
- 所有组件必须有完整的 TypeScript 类型定义
- 使用 CSS 自定义属性进行样式控制
- 组件必须有完整的 Props 和 Events 文档
- 遵循无障碍访问标准

## 🔍 浏览器支持

- Chrome ≥ 88
- Firefox ≥ 78
- Safari ≥ 14
- Edge ≥ 88

## 📄 许可证

MIT License © 2026 AI-Ready Team

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

## 📞 支持

- 文档: [docs.ai-ready.com](https://docs.ai-ready.com)
- Issues: [GitHub Issues](https://github.com/ai-ready/ai-ready-ui/issues)
- 讨论: [GitHub Discussions](https://github.com/ai-ready/ai-ready-ui/discussions)