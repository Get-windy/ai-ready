# 前端应用模块配置说明

## 概述

本文档详细说明了AI-Ready项目前端应用模块的配置，包括目录结构、环境配置、构建配置、部署配置和开发规范。

## 目录结构

```
frontend/
├── apps/                          # 应用目录（Monorepo）
│   ├── pc-admin/                  # PC管理端
│   ├── mobile-admin/              # 移动管理端
│   ├── order-shop/                # 订单商城
│   ├── picking-delivery/          # 拣货配送
│   └── sales-crm/                 # 销售CRM
├── packages/                      # 公共包
│   ├── components/               # 组件库
│   ├── utils/                    # 工具函数
│   ├── styles/                   # 样式系统
│   ├── themes/                   # 主题系统
│   ├── finance-management/       # 财务管理模块
│   └── monitoring-alert/         # 监控告警模块
├── src/                          # 源代码
│   ├── components/              # 公共组件
│   │   ├── common/             # 通用组件
│   │   ├── layout/             # 布局组件
│   │   ├── business/           # 业务组件
│   │   └── @ai-ready/          # 发布包组件
│   ├── views/                  # 页面视图
│   ├── router/                 # 路由配置
│   ├── store/                  # 状态管理
│   ├── api/                    # API接口
│   ├── utils/                  # 工具函数
│   ├── types/                  # TypeScript类型
│   ├── hooks/                  # Vue Hooks
│   ├── assets/                 # 静态资源
│   └── styles/                 # 全局样式
├── tests/                       # 测试文件
├── infra/k8s/frontend/         # Kubernetes部署配置
├── .env.development            # 开发环境配置
├── .env.test                   # 测试环境配置
├── vite.config.ts              # Vite构建配置
├── Dockerfile                  # Docker构建文件
├── docker-compose.test.yml     # 测试环境Docker编排
├── nginx.conf                  # Nginx配置
└── package.json                # 项目配置
```

## 环境配置

### 开发环境 (.env.development)

```env
# 基本配置
NODE_ENV=development
VITE_APP_ENV=development
VITE_APP_TITLE=AI-Ready 开发环境

# API配置
VITE_API_BASE_URL=/api/v1
VITE_API_TIMEOUT=60000
VITE_API_ADMIN_URL=/api/admin
VITE_API_MOBILE_URL=/api/mobile

# 代理配置
VITE_PROXY_TARGET=http://localhost:8080
VITE_PROXY_CHANGE_ORIGIN=true

# 开发服务器
VITE_DEV_SERVER_HOST=localhost
VITE_DEV_SERVER_PORT=5173
VITE_DEV_SERVER_HTTPS=false
```

### 测试环境 (.env.test)

```env
# 基本配置
NODE_ENV=test
VITE_APP_ENV=test
VITE_APP_TITLE=AI-Ready 测试环境

# API配置
VITE_API_BASE_URL=/api/v1
VITE_API_TIMEOUT=30000
VITE_API_ADMIN_URL=/api/admin
VITE_API_MOBILE_URL=/api/mobile

# 代理配置
VITE_PROXY_TARGET=http://backend-test:8080
VITE_PROXY_CHANGE_ORIGIN=true

# 功能开关
VITE_FEATURE_DEBUG=true
VITE_FEATURE_MOCK_API=true
```

## 构建配置

### Vite配置 (vite.config.ts)

主要特性：
- TypeScript支持
- Vue 3 Composition API
- 路径别名配置
- 自动导入组件
- API代理配置
- 构建优化

### 构建命令

```bash
# 开发模式
pnpm run dev

# 测试环境构建
pnpm run build:test

# 生产环境构建
pnpm run build

# 预览构建结果
pnpm run preview

# 类型检查
pnpm run type-check

# 代码检查
pnpm run lint

# 单元测试
pnpm run test:unit
```

## API配置

### 基础配置

```typescript
// src/api/config.ts
export const apiConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: parseInt(import.meta.env.VITE_API_TIMEOUT),
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
  }
}
```

### API实例

```typescript
// src/api/index.ts
export const api = createAxiosInstance()                 // 通用API
export const adminApi = createAxiosInstance({...})       // 管理端API
export const mobileApi = createAxiosInstance({...})      // 移动端API
export const uploadApi = createAxiosInstance({...})      // 文件上传API
```

### 业务模块API端点

```typescript
// 用户管理
apiEndpoints.users.list      // GET /api/v1/users
apiEndpoints.users.detail(id) // GET /api/v1/users/{id}

// 订单管理
apiEndpoints.orders.list     // GET /api/v1/orders
apiEndpoints.orders.create   // POST /api/v1/orders

// 财务管理
apiEndpoints.finance.invoices.list // GET /api/v1/finance/invoices

// 采购管理
apiEndpoints.purchases.orders.list // GET /api/v1/purchases/orders

// 监控系统
apiEndpoints.monitoring.alerts.list // GET /api/v1/monitoring/alerts
```

## 部署配置

### Docker部署

```bash
# 构建镜像
docker build -t ai-ready-frontend:test .

# 运行容器
docker run -p 8081:80 ai-ready-frontend:test

# Docker Compose（测试环境）
docker-compose -f docker-compose.test.yml up -d
```

### Kubernetes部署

部署文件：`infra/k8s/frontend/deployment.yaml`

部署步骤：
```bash
# 应用部署配置
kubectl apply -f infra/k8s/frontend/deployment.yaml

# 查看部署状态
kubectl get deployments -n ai-ready-test

# 查看Pod状态
kubectl get pods -n ai-ready-test -l app=ai-ready-frontend

# 查看服务
kubectl get services -n ai-ready-test

# 查看Ingress
kubectl get ingress -n ai-ready-test
```

### 访问地址

- 前端应用: http://frontend.test.ai-ready.com
- 管理端: http://admin.test.ai-ready.com
- 移动端: http://mobile.test.ai-ready.com
- 健康检查: http://frontend.test.ai-ready.com/health
- 版本信息: http://frontend.test.ai-ready.com/version

## 组件开发规范

### 命名规范

1. **组件文件**: 大驼峰命名，如 `UserList.vue`
2. **目录命名**: 小写短横线命名，如 `user-management`
3. **CSS类名**: BEM命名法，如 `.user-list__item--active`
4. **TypeScript接口**: 以 `I` 开头，如 `IUserData`

### 组件结构

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

### 业务模块组件前缀

| 业务模块 | 组件前缀 | 示例 |
|---------|---------|------|
| 用户管理 | user- | UserList, UserForm |
| 订单管理 | order- | OrderTable, OrderDetail |
| 财务管理 | finance- | FinanceReport, InvoiceList |
| 采购管理 | purchase- | PurchaseOrder, SupplierList |
| 监控系统 | monitor- | MonitorChart, AlertList |

## 样式系统

### CSS变量系统

```scss
// src/styles/variables.scss
:root {
  // 主色调
  --color-primary: #409eff;
  --color-primary-light-3: #79bbff;
  
  // 文本颜色
  --color-text-primary: #303133;
  --color-text-regular: #606266;
  
  // 边框颜色
  --border-color-base: #dcdfe6;
  
  // 背景颜色
  --background-color-base: #f5f7fa;
}

// 暗色主题
html.dark {
  --color-text-primary: #e5eaf3;
  --background-color-base: #141414;
}
```

### SCSS混合器

```scss
// src/styles/mixins.scss
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@mixin responsive($breakpoint) {
  @media (max-width: $breakpoint) {
    @content;
  }
}
```

## 测试环境监控

### 监控系统

1. **Prometheus**: 指标收集 (http://localhost:9090)
2. **Grafana**: 数据可视化 (http://localhost:3000)
3. **Loki**: 日志收集 (http://localhost:3100)
4. **Promtail**: 日志代理

### 监控指标

- 应用性能指标
- API响应时间
- 错误率
- 资源使用率
- 用户访问统计

## 质量门禁

### 1. 配置完整性门禁

- [ ] 目录结构符合规范
- [ ] 配置文件完整
- [ ] 构建配置正确
- [ ] 部署配置完整

### 2. 功能可用性门禁

- [ ] 应用可构建
- [ ] 应用可启动
- [ ] API代理正常
- [ ] 路由功能正常

### 3. 性能门禁

- [ ] 构建时间 < 3分钟
- [ ] 首屏加载时间 < 3秒
- [ ] Lighthouse性能评分 > 80
- [ ] 包体积符合标准

### 4. 兼容性门禁

- [ ] 支持Chrome 90+
- [ ] 支持Firefox 88+
- [ ] 支持Safari 14+
- [ ] 支持Edge 90+

## 故障排除

### 常见问题

1. **应用无法启动**
   - 检查环境变量配置
   - 检查端口是否被占用
   - 检查依赖是否安装完整

2. **API请求失败**
   - 检查代理配置
   - 检查后端服务状态
   - 检查跨域配置

3. **样式不生效**
   - 检查CSS引入顺序
   - 检查样式作用域
   - 检查浏览器兼容性

4. **构建失败**
   - 检查TypeScript错误
   - 检查依赖版本冲突
   - 检查构建配置

### 调试工具

```javascript
// 开启调试模式
VITE_FEATURE_DEBUG=true

// 查看网络请求
console.log('[API Request]', config)

// 查看状态变化
import { useDevtools } from 'vue-devtools'
```

## 更新记录

| 版本 | 日期 | 更新内容 | 负责人 |
|------|------|----------|--------|
| 1.0.0 | 2026-04-30 | 初始版本创建 | 前端开发工程师 |
| 1.0.1 | 2026-04-30 | 完善API配置 | 前端开发工程师 |
| 1.0.2 | 2026-04-30 | 添加部署配置 | 前端开发工程师 |

## 联系方式

- 项目负责人: 前端开发工程师
- 问题反馈: 项目群组频道
- 文档维护: 前端开发团队