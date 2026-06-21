/**
 * 工作台 KPI 指标定义
 * 根据角色显示不同指标集，支持用户自定义
 */
import type { Component } from 'vue'
import {
  TeamOutlined, UserAddOutlined, FileTextOutlined,
  ShoppingOutlined, RollbackOutlined, WalletOutlined,
  ShopOutlined, CarOutlined, ShoppingCartOutlined,
  AlertOutlined, DollarOutlined, BarChartOutlined,
  CloudServerOutlined, ApiOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'

/** 用户角色类型 */
export type UserRole = 'system_admin' | 'system_staff' | 'tenant_admin' | 'tenant_staff'

/** KPI 指标定义 */
export interface KpiIndicator {
  id: string
  label: string
  icon: Component
  color: string
  apiField: string
  suffix?: string
  unit?: string
  prefix?: string
}

/** 所有可用指标 */
export const ALL_INDICATORS: KpiIndicator[] = [
  // ── 业务指标 ──
  { id: 'visit_count', label: '拜访客户', icon: TeamOutlined, color: '#ff4d4f', apiField: 'visitCount', suffix: '家', unit: '今日0家' },
  { id: 'new_customer', label: '拓客数量', icon: UserAddOutlined, color: '#1890ff', apiField: 'newCustomer', suffix: '', unit: '今日0家' },
  { id: 'order_amount', label: '实订金额', icon: FileTextOutlined, color: '#722ed1', apiField: 'orderAmount', prefix: '¥' },
  { id: 'sales_amount', label: '实销金额', icon: ShoppingOutlined, color: '#13c2c2', apiField: 'salesAmount', prefix: '¥' },
  { id: 'return_amount', label: '退货金额', icon: RollbackOutlined, color: '#fa8c16', apiField: 'returnAmount', prefix: '¥' },
  { id: 'payment_amount', label: '回款金额', icon: WalletOutlined, color: '#52c41a', apiField: 'paymentAmount', prefix: '¥' },

  // ── 运营指标 ──
  { id: 'customer_count', label: '客户总数', icon: ShopOutlined, color: '#1890ff', apiField: 'customerCount', unit: '' },
  { id: 'pending_ship', label: '待发货', icon: CarOutlined, color: '#faad14', apiField: 'pendingShip', unit: '单' },
  { id: 'pending_receive', label: '待收货', icon: ShoppingCartOutlined, color: '#1890ff', apiField: 'pendingReceive', unit: '单' },
  { id: 'stock_alert', label: '库存预警', icon: AlertOutlined, color: '#ff4d4f', apiField: 'stockAlert', unit: '项' },
  { id: 'pending_approval', label: '待审批', icon: FileTextOutlined, color: '#faad14', apiField: 'pendingApproval', unit: '单' },

  // ── 财务指标 ──
  { id: 'monthly_income', label: '本月收入', icon: DollarOutlined, color: '#52c41a', apiField: 'monthlyIncome', prefix: '¥' },
  { id: 'monthly_expense', label: '本月支出', icon: DollarOutlined, color: '#ff4d4f', apiField: 'monthlyExpense', prefix: '¥' },
  { id: 'monthly_profit', label: '本月利润', icon: BarChartOutlined, color: '#1890ff', apiField: 'monthlyProfit', prefix: '¥' },

  // ── 系统指标 ──
  { id: 'tenant_count', label: '租户数量', icon: CloudServerOutlined, color: '#1890ff', apiField: 'tenantCount' },
  { id: 'user_count', label: '用户数量', icon: TeamOutlined, color: '#722ed1', apiField: 'userCount' },
  { id: 'api_calls', label: 'API调用', icon: ApiOutlined, color: '#13c2c2', apiField: 'apiCalls' },
  { id: 'system_health', label: '系统健康', icon: CheckCircleOutlined, color: '#52c41a', apiField: 'systemHealth', suffix: '%' }
]

/** 各角色默认显示的指标 ID 列表 */
export const ROLE_DEFAULT_INDICATORS: Record<UserRole, string[]> = {
  system_admin: ['tenant_count', 'user_count', 'api_calls', 'system_health', 'monthly_income', 'monthly_expense'],
  system_staff: ['api_calls', 'system_health', 'pending_approval'],
  tenant_admin: ['monthly_income', 'monthly_expense', 'monthly_profit', 'customer_count', 'sales_amount', 'order_amount', 'stock_alert', 'pending_approval'],
  tenant_staff: ['visit_count', 'new_customer', 'order_amount', 'sales_amount', 'return_amount', 'payment_amount']
}

/** 根据 userType 数字映射到 UserRole */
export function mapUserTypeToRole(userType: number): UserRole {
  if (userType === 0) return 'system_admin'
  if (userType === 1) return 'tenant_admin'
  return 'tenant_staff'
}

/** 获取角色默认指标 */
export function getRoleDefaultIndicators(role: UserRole): KpiIndicator[] {
  const ids = ROLE_DEFAULT_INDICATORS[role]
  return ids.map(id => ALL_INDICATORS.find(i => i.id === id)!).filter(Boolean)
}

/** localStorage 键名 */
const STORAGE_KEY = 'dashboard_custom_indicators'
const STORAGE_ROLE_KEY = 'dashboard_indicator_role'

/** 获取用户自定义指标（返回 null 表示未自定义，使用角色默认） */
export function getCustomIndicators(): string[] | null {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (!saved) return null
  try {
    return JSON.parse(saved)
  } catch {
    return null
  }
}

/** 保存用户自定义指标 */
export function saveCustomIndicators(indicatorIds: string[], role: UserRole): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(indicatorIds))
  localStorage.setItem(STORAGE_ROLE_KEY, role)
}

/** 清除自定义（恢复角色默认） */
export function clearCustomIndicators(): void {
  localStorage.removeItem(STORAGE_KEY)
  localStorage.removeItem(STORAGE_ROLE_KEY)
}

/** 获取当前应显示的指标列表 */
export function getCurrentIndicators(role: UserRole): KpiIndicator[] {
  const custom = getCustomIndicators()
  if (custom) {
    return custom.map(id => ALL_INDICATORS.find(i => i.id === id)!).filter(Boolean)
  }
  return getRoleDefaultIndicators(role)
}

/** 判断是否有自定义设置 */
export function hasCustomIndicators(): boolean {
  return localStorage.getItem(STORAGE_KEY) !== null
}
