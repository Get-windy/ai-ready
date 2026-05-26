# AI-Ready 前端性能优化

## 概述

本文档记录了 AI-Ready 项目的前端性能优化措施和工具。

## 优化内容

### 1. 构建优化 (vite.config.ts)

#### 已实施的优化：
- **代码分割**: 将第三方库和业务模块按策略分割成多个 chunk
- **压缩优化**: 同时使用 Gzip 和 Brotli 压缩
- **图片优化**: 使用 vite-plugin-imagemin 自动优化图片
- **Tree-shaking**: 移除未使用的代码
- **Terser 压缩**: 移除 console 和 debugger
- **CSS 模块化**: 更好的 CSS tree-shaking
- **依赖预构建**: 优化常用依赖的预构建

#### 代码分割策略：
```typescript
- antd: Ant Design Vue 组件
- vue-vendor: Vue 核心库（vue, vue-router, pinia）
- utils-vendor: 工具库（lodash, dayjs, axios）
- charts-vendor: 图表库
- vendor: 其他第三方库
- erp-module: ERP 业务模块
- crm-module: CRM 业务模块
- system-module: 系统设置模块
- dashboard-module: 工作台模块
- api-module: API 模块
- common-components: 公共组件
- common-utils: 工具函数
```

### 2. 组件优化 (main.ts)

#### 已实施的优化：
- **按需引入**: Ant Design Vue 改为按需引入，减少包体积
- **异步初始化**: i18n 异步加载，不阻塞应用启动

### 3. 性能工具

#### 3.1 组件懒加载 (src/utils/asyncComponent.ts)

提供组件懒加载功能，支持：
- 加载状态显示
- 错误处理和重试
- 预加载功能

**使用示例：**
```typescript
import { createAsyncComponent } from '@/utils/asyncComponent'

const LazyComponent = createAsyncComponent(
  () => import('./MyComponent.vue'),
  {
    delay: 200,
    timeout: 10000,
    retryable: true,
    maxRetries: 3
  }
)
```

#### 3.2 图片懒加载 (src/components/LazyImage.tsx)

提供图片懒加载组件，支持：
- Intersection Observer API
- 占位图和加载状态
- 错误重试
- 图片预览

**使用示例：**
```vue
<template>
  <LazyImage
    src="https://example.com/image.jpg"
    width="200"
    height="200"
    alt="示例图片"
    :preview="true"
  />
</template>
```

#### 3.3 虚拟滚动 (src/components/VirtualScroll.tsx)

提供虚拟滚动功能，优化大数据列表渲染：
- 仅渲染可见区域的数据
- 支持动态高度
- 缓冲区优化

**使用示例：**
```vue
<template>
  <VirtualList
    :data="items"
    :item-size="50"
    :height="500"
  >
    <template #default="{ item, index }">
      <div>{{ item.name }}</div>
    </template>
  </VirtualList>
</template>
```

#### 3.4 性能监控 (src/utils/performance.ts)

提供性能指标收集和监控功能：
- Core Web Vitals (FCP, LCP, FID, CLS)
- 自定义指标记录
- 性能计时器
- 性能装饰器

**使用示例：**
```typescript
import performanceMonitor from '@/utils/performance'
import { measurePerformance, measureAsyncPerformance } from '@/utils/performance'

// 获取性能报告
const report = performanceMonitor.getReport()

// 记录自定义指标
performanceMonitor.recordMetric('custom-metric', value)

// 使用装饰器测量函数性能
class MyClass {
  @measurePerformance
  myMethod() {
    // 方法实现
  }
}

// 测量异步函数
await measureAsyncPerformance(async () => {
  // 异步操作
}, 'async-operation')
```

#### 3.5 Web Worker (src/utils/worker.ts)

提供 Web Worker 管理，将计算密集型任务移到后台线程：
- Worker 任务管理
- 数据处理工具（filter, map, reduce, sort, groupBy, chunk, search）
- 错误处理和重试

**使用示例：**
```typescript
import { dataProcess } from '@/utils/worker'

// 在 Worker 中过滤大量数据
const filtered = await dataProcess.filter(largeArray, item => item.age > 18)

// 在 Worker 中搜索
const results = await dataProcess.search(items, 'keyword')
```

### 4. 路由优化 (src/router/index.ts)

#### 已实施的优化：
- 所有路由组件使用懒加载
- 按模块分割路由代码

## 使用建议

### 1. 图片优化
- 使用 LazyImage 组件替代原生 img 标签
- 尽量使用 WebP 格式
- 控制图片尺寸，避免过大图片

### 2. 列表渲染
- 对于大量数据（> 100 条），使用虚拟滚动
- 避免在模板中使用复杂表达式
- 使用 key 属性优化 diff 算法

### 3. 组件加载
- 非关键组件使用懒加载
- 首屏组件保持轻量
- 预加载即将使用的组件

### 4. 数据处理
- 大数据量处理使用 Web Worker
- 避免在主线程执行复杂计算
- 使用防抖和节流优化高频事件

### 5. 性能监控
- 定期查看性能报告
- 关注 Core Web Vitals 指标
- 使用性能工具定位瓶颈

## 性能指标

### 目标指标
- **FCP (首次内容绘制)**: < 1.8s
- **LCP (最大内容绘制)**: < 2.5s
- **FID (首次输入延迟)**: < 100ms
- **CLS (累积布局偏移)**: < 0.1
- **TTI (可交互时间)**: < 3.5s

### 查看性能报告

构建后查看 dist/stats.html 文件：
```bash
npm run build
# 然后在浏览器中打开 dist/stats.html
```

## 后续优化方向

1. **CDN 加速**: 将第三方库改为 CDN 引入
2. **Service Worker**: 实现离线缓存和更新策略
3. **SSR/SSG**: 服务端渲染或静态生成
4. **HTTP/2**: 利用 HTTP/2 多路复用
5. **资源预加载**: 关键资源预加载策略
6. **代码拆分**: 进一步细化代码拆分粒度

## 注意事项

1. **开发环境**: 部分优化仅在生产环境生效
2. **浏览器兼容性**: 确保优化措施在目标浏览器中可用
3. **测试验证**: 优化后需进行充分测试
4. **监控迭代**: 持续监控性能指标并迭代优化

## 参考资源

- [Vite 性能优化指南](https://vitejs.dev/guide/performance.html)
- [Web 性能优化](https://web.dev/performance/)
- [Core Web Vitals](https://web.dev/vitals/)