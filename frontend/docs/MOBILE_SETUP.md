# AI-Ready 移动端开发环境配置指南

## 概述

本文为 AI-Ready 项目的移动端开发环境配置指南，包含：
- Node.js + Vue 3 开发环境
- Vant 4 移动端组件库配置
- 调试工具和模拟器配置
- 离线缓存本地存储方案

## 环境配置

### 1. Node.js 环境

已检测到的环境：
- Node.js: v24.11.1 ✅
- npm: 11.8.0 ✅

### 2. 项目结构

移动端项目位于 `I:\AI-Ready\erp-mobile`

```
erp-mobile/
├── src/
│   ├── views/          # 页面组件
│   │   ├── Home.vue    # 首页
│   │   ├── Login.vue   # 登录页
│   │   ├── Orders.vue  # 订单管理
│   │   ├── Delivery.vue # 配送管理
│   │   ├── Scan.vue    # 扫码功能
│   │   └── Profile.vue # 个人中心
│   ├── components/     # 公共组件
│   ├── router/         # 路由配置
│   ├── stores/         # Pinia 状态管理
│   ├── utils/          # 工具函数
│   │   ├── request.ts  # HTTP 请求封装
│   │   └── storage.ts  # 本地存储封装
│   └── styles/         # 全局样式
├── vite.config.ts      # Vite 配置
└── package.json        # 项目依赖
```

### 3. 已配置功能

#### ✅ Vant 4 组件库

已注册组件（在 `main.ts` 中）：
- 基础组件: Button, Cell, Icon, Image
- 导航组件: NavBar, Tabbar, Tabs, Grid
- 表单组件: Field, Form, Checkbox, Radio
- 反馈组件: Toast, Dialog, Loading, PullRefresh
- 展示组件: Card, Tag, Badge, Empty, List

#### ✅ 路由配置

已配置路由（`src/router/index.ts`）：
| 路径 | 组件 | 说明 |
|------|------|------|
| `/` | Home | 首页 |
| `/login` | Login | 登录页 |
| `/orders` | Orders | 订单管理 |
| `/delivery` | Delivery | 配送管理 |
| `/scan` | Scan | 扫码功能 |
| `/profile` | Profile | 个人中心 |
| `/inventory` | Inventory | 库存管理 |

#### ✅ 状态管理 (Pinia)

- **User Store**: 管理用户登录状态
- **App Store**: 管理应用级状态

#### ✅ HTTP 请求封装

文件: `src/utils/request.ts`
- 自动添加 token
- 统一错误处理
- 封装 get/post/put/del 方法

#### ✅ 本地存储封装

文件: `src/utils/storage.ts`
- localStorage 封装
- sessionStorage 封装
- 离线缓存 API 响应
- 支持过期时间

#### ✅ 移动端适配

文件: `src/styles/mobile-adapter.css`
- 响应式字体大小
- 安全区域适配（iPhone X+）
- 触摸反馈优化
- 文本溢出处理

## 快速启动

### 安装依赖

```bash
cd I:\AI-Ready\erp-mobile
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 调试配置

### 1. 浏览器调试

推荐使用 Chrome DevTools 的移动端模拟器：
1. 按 F12 打开 DevTools
2. 点击 Toggle device toolbar (Ctrl+Shift+M)
3. 选择设备尺寸（如 iPhone 12 Pro）

### 2. 真机调试

#### Android
1. 手机开启 USB 调试
2. Chrome 访问 `chrome://inspect`
3. 连接手机后调试

#### iOS
1. 使用 Safari 开发者工具
2. iPhone 开启 Web 检查器
3. Mac Safari 连接调试

### 3. 微信开发者工具

下载地址: https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html

## 离线缓存方案

### 实现方式

1. **Service Worker** (可选)
   - 缓存静态资源
   - 离线访问支持

2. **本地存储** (已实现)
   - 用户登录状态
   - API 响应缓存
   - 扫描历史记录

3. **IndexedDB** (可选)
   - 大量数据存储
   - 离线表单数据

### 使用示例

```typescript
import { storage, offlineCache } from '@/utils/storage'

// 存储数据
storage.set('user', { name: '张三' })

// 读取数据
const user = storage.get('user')

// 带过期时间的存储
storage.set('token', 'xxx', { expires: 3600000 }) // 1小时

// 缓存 API 响应
offlineCache.cacheApi('/api/orders', responseData)

// 读取缓存
const cached = offlineCache.getCachedApi('/api/orders')
```

## 与后端集成

### API 代理配置

`vite.config.ts`:
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost: