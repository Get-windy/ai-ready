# AI-Ready 前端错误边界处理文档

## 概述

本项目实现了完整的 Vue 3 错误边界处理系统，包括：
- 组件级错误捕获
- 全局错误处理
- 错误降级 UI
- 错误日志上报

## 核心文件

```
src/
├── components/ErrorBoundary/
│   ├── ErrorBoundary.vue          # 错误边界组件
│   ├── GlobalErrorFallback.vue    # 全局错误降级 UI
│   ├── errorBoundaryUtils.ts      # 工具函数和 HOC
│   └── index.ts                   # 导出索引
├── utils/
│   └── errorReporter.ts           # 错误上报服务
├── views/error/
│   └── ErrorPage.vue              # 错误页面路由
├── main.ts                        # 集成全局错误处理
└── App.vue                        # 应用级错误边界包装
```

## 使用方法

### 1. 基本使用 - 包装组件

```vue
<template>
  <ErrorBoundary @error="handleError">
    <YourComponent />
  </ErrorBoundary>
</template>

<script setup>
import { ErrorBoundary } from '@/components/ErrorBoundary'

const handleError = (error, instance, info) => {
  console.error('Component error:', error)
}
</script>
```

### 2. 自定义降级 UI

```vue
<template>
  <ErrorBoundary>
    <template #default>
      <YourComponent />
    </template>
    
    <!-- 自定义降级内容由 ErrorBoundary 内部处理 -->
  </ErrorBoundary>
</template>
```

### 3. 高阶组件包装

```typescript
import { withErrorBoundary } from '@/components/ErrorBoundary'

// 包装单个组件
const SafeComponent = withErrorBoundary(YourComponent, {
  onError: (error) => {
    console.error('Wrapped component error:', error)
  }
})
```

### 4. 异步组件错误处理

```typescript
import { createAsyncComponentWithErrorBoundary } from '@/components/ErrorBoundary'

const SafeAsyncComponent = createAsyncComponentWithErrorBoundary(
  () => import('./HeavyComponent.vue'),
  {
    loadingComponent: LoadingSpinner,
    errorComponent: CustomErrorFallback,
    timeout: 10000
  }
)
```

### 5. 创建动态错误边界

```typescript
import { createErrorBoundary } from '@/components/ErrorBoundary'

const DynamicBoundary = createErrorBoundary(CustomFallback)
```

## 错误类型分类

`errorReporter.ts` 自动分类错误类型：

| 类型 | 触发条件 | 建议处理 |
|------|----------|----------|
| network | 包含 'network'/'fetch' | 检查网络连接 |
| timeout | 包含 'timeout' | 建议用户稍后重试 |
| auth | 包含 'auth'/'unauthorized' | 重新登录 |
| type | 包含 'undefined'/'null' | 数据校验 |
| syntax | 包含 'syntax'/'parse' | 代码修复 |
| unknown | 其他 | 显示通用错误 |

## 错误上报配置

在 `main.ts` 中配置：

```typescript
initErrorReporter(app, {
  enabled: true,                // 是否启用
  endpoint: '/api/log/error',   // 上报接口
  batchSize: 5,                 // 批量上报数量
  flushInterval: 3000           // 定时上报间隔(ms)
})
```

## 错误上报数据结构

```typescript
interface ErrorReport {
  type: 'component' | 'global' | 'promise' | 'resource' | 'ajax'
  error: Error | string
  componentInfo?: string
  timestamp: string
  componentInstance?: string
  url?: string
  userAgent?: string
  stack?: string
  extra?: Record<string, unknown>
}
```

## Props 说明

### ErrorBoundary.vue

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| fallbackTitle | string | '页面出现异常' | 错误标题 |
| fallbackSubtitle | string | '抱歉...' | 错误副标题 |
| retryable | boolean | true | 是否允许重试 |
| onError | function | - | 错误回调 |

### Events

| Event | 参数 | 说明 |
|-------|------|------|
| error | (error, instance, info) | 捕获错误时触发 |
| retry | - | 重试时触发 |

## 最佳实践

1. **关键组件包装**: 对容易出错的关键组件使用 ErrorBoundary 包装
2. **异步操作**: 异步加载的组件使用 createAsyncComponentWithErrorBoundary
3. **错误上报**: 生产环境启用错误上报，便于问题追踪
4. **用户友好**: 根据错误类型显示用户友好的提示信息
5. **降级策略**: 为关键功能提供降级方案，不完全中断用户操作

## 示例代码

### 完整页面错误处理

```vue
<template>
  <ErrorBoundary
    :fallback-title="数据处理异常"
    :fallback-subtitle="数据加载失败，请刷新页面重试"
    @error="logError"
    @retry="onRetry"
  >
    <DataList :data="listData" />
  </ErrorBoundary>
</template>

<script setup>
import { ErrorBoundary } from '@/components/ErrorBoundary'
import { reportError } from '@/utils/errorReporter'
import { ref } from 'vue'

const listData = ref([])

const logError = (error) => {
  reportError({
    type: 'component',
    error,
    timestamp: new Date().toISOString()
  })
}

const onRetry = () => {
  // 重新加载数据
  loadListData()
}

const loadListData = async () => {
  // ...
}
</script>
```

## 注意事项

1. Vue 3 没有像 React 那样的内置错误边界，使用 `onErrorCaptured` 钩子实现
2. `onErrorCaptured` 只能捕获子组件的渲染错误，无法捕获：
   - 事件处理器中的错误
   - 异步操作中的错误
   - setup 函数中的错误（除非在生命周期钩子中）
3. 全局错误处理器 (`app.config.errorHandler`) 作为后备方案
4. Promise 错误需要通过 `unhandledrejection` 事件捕获
5. 资源加载错误需要通过 `window.error` 事件捕获

---

_错误边界实现完成。如有问题请查看组件源码或联系开发团队。_