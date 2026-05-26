# 智企连·AI-Ready 前端架构文档

**版本**: v1.0.0  
**最后更新**: 2026-03-28  
**维护者**: team-member

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| TypeScript | 5.3.x | 类型安全 |
| Vite | 5.1.x | 构建工具 |
| Vue Router | 4.3.x | 路由管理 |
| Pinia | 2.1.x | 状态管理 |
| Ant Design Vue | 4.1.x | UI组件库 |
| Axios | 1.6.x | HTTP请求 |

---

## 目录结构

```
smart-admin-web/
├── src/
│   ├── api/              # API接口封装
│   │   ├── user.ts       # 用户相关API
│   │   └── purchase.ts   # 采购订单API
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   │   └── BasicLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # 状态管理
│   │   └── user.ts
│   ├── styles/           # 全局样式
│   │   └── index.css
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios封装
│   ├── views/            # 页面视图
│   │   ├── login/        # 登录页
│   │   ├── dashboard/    # 工作台
│   │   ├── erp/          # ERP模块
│   │   ├── crm/          # CRM模块
│   │   ├── system/       # 系统设置
│   │   └── error/        # 错误页面
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 核心模块

### 1. 路由配置

采用 Vue Router 4.x，支持：
- 路由懒加载
- 路由守卫（权限校验）
- 嵌套路由

### 2. 状态管理

采用 Pinia，主要 Store：
- **userStore**: 用户信息、Token、权限管理

### 3. HTTP请求

基于 Axios 封装：
- 请求/响应拦截器
- Token自动注入
- 统一错误处理
- TypeScript类型支持

### 4. 权限控制

- 路由级别：路由守卫
- 按钮级别：v-if + hasPermission
- 接口级别：后端Token校验

---

## API接口规范

### 统一响应格式

```typescript
interface ApiResponse<T> {
  code: number      // 状态码：200成功
  message: string   // 消息
  data: T          // 数据
  timestamp: number // 时间戳
}
```

### 分页响应格式

```typescript
interface PageResponse<T> {
  records: T[]     // 数据列表
  total: number    // 总数
  current: number  // 当前页
  size: number     // 每页大小
  pages: number    // 总页数
}
```

---

## 开发指南

### 启动项目

```bash
cd smart-admin-web
npm install
npm run dev
```

### 构建项目

```bash
npm run build
```

### 代码规范

- 使用 TypeScript
- 组件使用 `<script setup>` 语法
- 样式使用 scoped

---

**文档维护**: team-member  
**最后更新**: 2026-03-28