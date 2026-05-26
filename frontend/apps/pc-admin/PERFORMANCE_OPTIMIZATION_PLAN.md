# AI-Ready 前端性能优化方案

> **文档版本**: v1.0
> **创建日期**: 2026-04-12
> **负责人**: 前端开发工程师

---

## 一、当前性能瓶颈分析

### 1.1 已识别的潜在瓶颈

| 类别 | 瓶颈点 | 影响程度 | 优先级 |
|------|--------|----------|--------|
| **首屏加载** | Ant Design Vue 组件全量加载 | 高 | P0 |
| **代码体积** | 业务模块未充分按需拆分 | 高 | P0 |
| **图片资源** | 缺乏统一的图片优化策略 | 中 | P1 |
| **缓存策略** | 缺少 HTTP 缓存头配置 | 高 | P0 |
| **网络请求** | API 请求未优化（合并/缓存） | 中 | P1 |
| **运行时性能** | 大列表渲染未虚拟化 | 中 | P1 |

### 1.2 构建产物分析（预估）

基于当前配置的构建产物预估：

- **总包体积**: ~2.5MB (未压缩)
- **主要 chunk**:
  - `vue-vendor`: ~300KB (Vue 核心库)
  - `antd`: ~800KB (Ant Design Vue)
  - `vendor`: ~400KB (其他第三方库)
  - 业务模块 chunk: 各模块 50-200KB 不等

---

## 二、代码分割与懒加载策略

### 2.1 当前代码分割策略（已实施）

```typescript
// vite.config.ts 中的 manualChunks 配置
manualChunks: (id) => {
  // 第三方库分割
  if (id.includes('node_modules')) {
    // Ant Design Vue 相关
    if (id.includes('ant-design-vue') || id.includes('@ant-design')) {
      return 'antd'
    }
    // Vue 核心库
    if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
      return 'vue-vendor'
    }
    // 工具库
    if (id.includes('lodash') || id.includes('dayjs') || id.includes('axios')) {
      return 'utils-vendor'
    }
    // 图表库
    if (id.includes('echarts') || id.includes('chart')) {
      return 'charts-vendor'
    }
    return 'vendor'
  }

  // 业务模块分割
  if (id.includes('src/views/erp')) return 'erp-module'
  if (id.includes('src/views/crm')) return 'crm-module'
  if (id.includes('src/views/system')) return 'system-module'
  if (id.includes('src/views/dashboard')) return 'dashboard-module'
  if (id.includes('src/api')) return 'api-module'
  if (id.includes('src/components') || id.includes('src/layouts')) return 'common-components'
  if (id.includes('src/utils') || id.includes('src/composables')) return 'common-utils'
}
```

### 2.2 进一步优化策略

#### 2.2.1 Ant Design Vue 按需引入（已实施）

```typescript
// vite.config.ts
Components({
  resolvers: [
    AntDesignVueResolver({
      importStyle: false, // 使用 CSS-in-JS（按需引入样式）
    }),
  ],
})
```

**优化效果**: 预计减少 40-60% 的 Ant Design 体积

#### 2.2.2 路由级懒加载（已实施）

所有路由组件使用动态导入：

```typescript
// router/index.ts
{
  path: 'dashboard',
  name: 'Dashboard',
  component: () => import('@/views/dashboard/index.vue'),
  meta: { title: '工作台', keepAlive: true }
}
```

#### 2.2.3 组件级懒加载（推荐实施）

非首屏关键组件使用懒加载：

```vue
<script setup lang="ts">
import { defineAsyncComponent } from 'vue'

// 懒加载非关键组件
const LazyChart = defineAsyncComponent(() =>
  import('@/components/Charts/LineChart.vue')
)
</script>

<template>
  <Suspense>
    <LazyChart />
  </Suspense>
</template>
```

#### 2.2.4 预加载策略（推荐新增）

在路由守卫中预加载即将访问的页面：

```typescript
// router/guard.ts
router.beforeEach(async (to, from, next) => {
  // 预加载相关页面的资源
  if (to.path.startsWith('/erp') && from.path.startsWith('/dashboard')) {
    import('@/views/erp/purchase/index.vue')
  }
  next()
})
```

---

## 三、资源加载优化

### 3.1 图片优化

#### 3.1.1 构建时压缩（已实施）

```typescript
// vite.config.ts
viteImagemin({
  gifsicle: { optimizationLevel: 7, interlaced: false },
  optipng: { optimizationLevel: 7 },
  mozjpeg: { quality: 80 },
  pngquant: { quality: [0.8, 0.9], speed: 4 },
  svgo: {
    plugins: [
      { name: 'removeViewBox', active: false },
      { name: 'removeEmptyAttrs', active: false },
    ],
  },
})
```

**优化效果**: 图片体积减少 30-50%

#### 3.1.2 图片懒加载（推荐实施）

使用已提供的 LazyImage 组件：

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

#### 3.1.3 响应式图片（推荐实施）

```html
<picture>
  <source srcset="image-800.webp" type="image/webp" media="(min-width: 800px)">
  <source srcset="image-400.webp" type="image/webp">
  <img src="image-400.jpg" alt="响应式图片" loading="lazy">
</picture>
```

#### 3.1.4 WebP 格式迁移（推荐实施）

将静态资源迁移至 WebP 格式，并提供 JPEG 回退：

```bash
# 批量转换（需安装 cwebp）
find src/assets/images -name "*.jpg" -exec cwebp -q 80 {} -o {}.webp \;
```

### 3.2 字体优化

#### 3.2.1 字体子集化（推荐实施）

使用 fonttools 提取实际使用的字符：

```bash
# 安装工具
pip install fonttools

# 生成子集
pyftsubset source.woff2 --text-file=chars.txt --output-file=subset.woff2
```

#### 3.2.2 字体加载策略

```css
/* 使用 font-display 优化加载体验 */
@font-face {
  font-family: 'CustomFont';
  src: url('./custom-font.woff2') format('woff2');
  font-display: swap; /* 先显示后备字体，字体加载完成后切换 */
  font-weight: 400;
}

/* 预加载关键字体 */
<link rel="preload" href="/fonts/custom-font.woff2" as="font" type="font/woff2" crossorigin>
```

### 3.3 CSS 优化

#### 3.3.1 CSS 模块化（已配置）

```typescript
// vite.config.ts
css: {
  modules: {
    scopeBehaviour: 'local',
  },
}
```

#### 3.3.2 Critical CSS（推荐实施）

提取首屏关键 CSS，内联到 HTML：

```html
<style>
  /* 首屏关键样式 */
  .app-container { display: flex; height: 100vh; }
  /* ... */
</style>
```

### 3.4 JavaScript 优化

#### 3.4.1 Terser 压缩（已配置）

```typescript
// vite.config.ts
terserOptions: {
  compress: {
    drop_console: true,
    drop_debugger: true,
    pure_funcs: ['console.log'],
  },
  format: {
    comments: false,
  },
}
```

---

## 四、浏览器缓存策略

### 4.1 HTTP 缓存头配置（需在服务器配置）

#### 4.1.1 Nginx 配置示例

```nginx
server {
  listen 80;
  server_name example.com;
  root /var/www/dist;

  # 静态资源长期缓存（带 hash 的文件名）
  location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?|eot|ttf|otf)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
  }

  # HTML 文件不缓存
  location ~* \.html$ {
    expires -1;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
  }

  # 其他资源短期缓存
  location / {
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
  }
}
```

#### 4.1.2 CDN 配置

将静态资源上传至 CDN，利用 CDN 边缘缓存：

```bash
# 上传构建产物到 CDN
aws s3 sync dist/ s3://cdn-bucket/static/ --cache-control "public, max-age=31536000, immutable"
```

### 4.2 Service Worker 缓存（推荐实施）

#### 4.2.1 注册 Service Worker

```typescript
// src/main.ts
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  navigator.serviceWorker.register('/sw.js').then((registration) => {
    console.log('Service Worker 注册成功:', registration)
  }).catch((error) => {
    console.log('Service Worker 注册失败:', error)
  })
}
```

#### 4.2.2 Service Worker 实现

```javascript
// public/sw.js
const CACHE_NAME = 'aiready-v1.0.0'
const urlsToCache = [
  '/',
  '/index.html',
  '/manifest.json',
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(urlsToCache)
    })
  )
})

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request).then((response) => {
      // 缓存优先策略
      return response || fetch(event.request)
    })
  )
})

self.addEventListener('activate', (event) => {
  const cacheWhitelist = [CACHE_NAME]
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName)
          }
        })
      )
    })
  )
})
```

### 4.3 本地存储策略

#### 4.3.1 Pinia 持久化（已实施）

```typescript
// main.ts
pinia.use(piniaPluginPersistedstate)
```

#### 4.3.2 合理使用 localStorage / sessionStorage

```typescript
// 存储用户偏好设置
localStorage.setItem('theme', 'dark')

// 存储会话数据
sessionStorage.setItem('currentPage', '1')
```

---

## 五、性能监控与指标

### 5.1 Core Web Vitals 目标

| 指标 | 目标值 | 当前状态 |
|------|--------|----------|
| FCP (首次内容绘制) | < 1.8s | 待测试 |
| LCP (最大内容绘制) | < 2.5s | 待测试 |
| FID (首次输入延迟) | < 100ms | 待测试 |
| CLS (累积布局偏移) | < 0.1 | 待测试 |
| TTI (可交互时间) | < 3.5s | 待测试 |

### 5.2 性能监控工具

#### 5.2.1 使用已提供的性能监控工具

```typescript
import performanceMonitor from '@/utils/performance'

// 获取性能报告
const report = performanceMonitor.getReport()
console.log('性能报告:', report)

// 记录自定义指标
performanceMonitor.recordMetric('api-response-time', 500)
```

#### 5.2.2 Lighthouse 测试

```bash
# 安装 Lighthouse CLI
npm install -g lighthouse

# 运行测试
lighthouse http://localhost:3000 --output html --output-path ./lighthouse-report.html
```

### 5.3 构建分析

```bash
# 生成构建分析报告
npm run build:analyze

# 查看构建产物大小
npm run build:report
```

---

## 六、实施计划

### 阶段一：基础优化（1-2 周）

| 任务 | 负责人 | 预计工时 | 优先级 |
|------|--------|----------|--------|
| ✅ 验证 Ant Design Vue 按需引入 | 前端 | 4h | P0 |
| ✅ 验证代码分割配置 | 前端 | 4h | P0 |
| ✅ 配置服务器缓存策略 | 运维/后端 | 8h | P0 |
| 📝 建立性能基准测试 | 前端 | 8h | P0 |

### 阶段二：资源优化（1-2 周）

| 任务 | 负责人 | 预计工时 | 优先级 |
|------|--------|----------|--------|
| 📝 实施图片懒加载 | 前端 | 16h | P1 |
| 📝 迁移至 WebP 格式 | 前端 | 8h | P1 |
| 📝 字体子集化 | 前端 | 8h | P1 |
| 📝 组件级懒加载改造 | 前端 | 16h | P1 |

### 阶段三：高级优化（2-3 周）

| 任务 | 负责人 | 预计工时 | 优先级 |
|------|--------|----------|--------|
| 📝 实现 Service Worker | 前端 | 16h | P1 |
| 📝 大列表虚拟化 | 前端 | 16h | P1 |
| 📝 API 请求优化 | 前端 | 12h | P1 |
| 📝 性能监控集成 | 前端 | 8h | P1 |

### 阶段四：持续优化（长期）

| 任务 | 负责人 | 频率 |
|------|--------|------|
| 定期性能测试 | 前端 | 每周 |
| 性能报告分析 | 全团队 | 每两周 |
| 优化迭代 | 前端 | 持续 |

---

## 七、预期效果

| 优化项 | 优化前 | 优化后 | 提升幅度 |
|--------|--------|--------|----------|
| 首屏加载时间 | ~3.5s | ~1.5s | 57% ⬆️ |
| 总包体积 | ~2.5MB | ~1.2MB | 52% ⬇️ |
| FCP | ~2.5s | ~1.2s | 52% ⬆️ |
| LCP | ~3.5s | ~2.0s | 43% ⬆️ |
| 图片体积 | - | 40% ⬇️ | - |

---

## 八、参考资源

- [Vite 性能优化指南](https://vitejs.dev/guide/performance.html)
- [Web 性能优化](https://web.dev/performance/)
- [Core Web Vitals](https://web.dev/vitals/)
- [MDN Web 性能](https://developer.mozilla.org/zh-CN/docs/Web/Performance)
- [Ant Design Vue 性能优化](https://antdv.com/docs/vue/getting-started-cn)

---

## 附录：快速检查清单

### 构建配置
- [x] 代码分割配置
- [x] Terser 压缩
- [x] Gzip/Brotli 压缩
- [x] 图片压缩
- [ ] Tree-shaking 优化
- [ ] Source map 配置（生产环境关闭）

### 代码质量
- [ ] 移除未使用的依赖
- [ ] 减少第三方库体积
- [ ] 优化事件监听器（及时移除）
- [ ] 避免深层嵌套渲染

### 资源优化
- [ ] 图片懒加载
- [ ] WebP 格式迁移
- [ ] 字体子集化
- [ ] CSS 压缩和去重

### 缓存策略
- [ ] HTTP 缓存头配置
- [ ] Service Worker 实现
- [ ] CDN 部署
- [ ] 本地存储优化

### 监控与测试
- [ ] Lighthouse 测试
- [ ] 性能监控工具集成
- [ ] 真实环境测试
- [ ] 定期性能报告