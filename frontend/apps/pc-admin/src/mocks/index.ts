/**
 * pc-admin Mock 数据基础设施 — 统一导出入口
 *
 * 使用方式：
 *   // 在 main.ts 最顶部引入
 *   import { initMockServer } from '@/mocks'
 *   initMockServer()
 *
 * 环境变量控制：
 *   VITE_USE_MOCKS=true         # 优先
 *   VITE_FEATURE_MOCK_API=true  # 备选
 */

// 浏览器端拦截器
export { initMockServer, stopMockServer, isMockActive } from './browser'

// 处理程序（可用于单元测试或自定义扩展）
export { handlers, resetUserStore, mockError } from './handlers'
export type { HandlerContext, MockHandlerDefinition } from './handlers'

// Mock 数据（可直接在组件/测试中引用）
export {
  trendChartData, todos, stockAlerts, quickEntries, dashboardStats
} from './data/dashboard'
export type { TrendChartData, TrendSeries, TodoItem, StockAlertItem, QuickEntry, DashboardStats } from './data/dashboard'

export { mockUsers, filterUsers } from './data/users'
export type { MockUser } from './data/users'
