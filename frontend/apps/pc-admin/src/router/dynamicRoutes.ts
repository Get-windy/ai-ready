import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const CLIENT_TYPE = 'pc-admin'

interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuCode: string
  menuType: number
  path?: string
  component?: string
  routeName?: string
  redirect?: string
  icon?: string
  sort: number
  isExternal?: number
  isCache?: number
  visible: number
  status: number
  clientType?: string
  billType?: string
  children?: MenuItem[]
}

const componentMap: Record<string, () => Promise<any>> = {
  'erp/product/index': () => import('@/views/erp/product/index.vue'),
  'erp/product/detail': () => import('@/views/erp/product/detail.vue'),
  'erp/product/price-batch': () => import('@/views/erp/product/price-batch.vue'),
  'erp/product/inventory-mode': () => import('@/views/erp/product/inventory-mode.vue'),
'partner/index': () => import('@/views/erp/partner/index.vue'),
'partner/detail': () => import('@/views/erp/partner/detail.vue'),
'pricing/index': () => import('@/views/erp/pricing/index.vue'),
  'charts/index': () => import('@/views/charts/index.vue'),
  'crm/contract/index': () => import('@/views/crm/contract/index.vue'),
  'crm/customer/detail/CustomerDetail': () => import('@/views/crm/customer/detail/CustomerDetail.vue'),
  'crm/customer/index': () => import('@/views/crm/customer/index.vue'),
  'crm/invoice/index': () => import('@/views/crm/invoice/index.vue'),
  'crm/lead/index': () => import('@/views/crm/lead/index.vue'),
  'crm/opportunity/index': () => import('@/views/crm/opportunity/index.vue'),
  'crm/quotation/index': () => import('@/views/crm/quotation/index.vue'),
  'crm/supplier/index': () => import('@/views/crm/supplier/index.vue'),
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'finance/accounts-payable/index': () => import('@/views/finance/accounts-payable/index.vue'),
  'finance/accounts-payable/payment-approval': () => import('@/views/finance/accounts-payable/payment-approval.vue'),
  'finance/accounts-receivable/aging-analysis': () => import('@/views/finance/accounts-receivable/aging-analysis.vue'),
  'finance/accounts-receivable/collection-reminder': () => import('@/views/finance/accounts-receivable/collection-reminder.vue'),
  'finance/accounts-receivable/index': () => import('@/views/finance/accounts-receivable/index.vue'),
  'finance/accounts-receivable/payment-record': () => import('@/views/finance/accounts-receivable/payment-record.vue'),
  'finance/index': () => import('@/views/finance/index.vue'),
  'finance/reconciliation/index': () => import('@/views/finance/reconciliation/index.vue'),
  'finance/reports/index': () => import('@/views/finance/reports/index.vue'),
  'finance/voucher/index': () => import('@/views/finance/voucher/index.vue'),
  'finance/voucher/VoucherDetail': () => import('@/views/finance/voucher/VoucherDetail.vue'),
  'finance/subject/index': () => import('@/views/finance/subject/index.vue'),
  'finance/receivable/index': () => import('@/views/finance/receivable/index.vue'),
  'finance/payable/index': () => import('@/views/finance/payable/index.vue'),
  'finance/pre-receipt/index': () => import('@/views/finance/pre-receipt/index.vue'),
  'finance/pre-payment/index': () => import('@/views/finance/pre-payment/index.vue'),
  'finance/deposit/index': () => import('@/views/finance/deposit/index.vue'),
  'finance/write-off/index': () => import('@/views/finance/write-off/index.vue'),
  'finance/offset/index': () => import('@/views/finance/offset/index.vue'),
  'finance/capital-flow/index': () => import('@/views/finance/capital-flow/index.vue'),
  'finance/receipt/index': () => import('@/views/finance/receipt/index.vue'),
  'finance/payment/index': () => import('@/views/finance/payment/index.vue'),
  'finance/report/index': () => import('@/views/finance/report/index.vue'),
  'notification/index': () => import('@/views/notification/index.vue'),
  'order-center/index': () => import('@/views/order-center/index.vue'),
  'profile/index': () => import('@/views/profile/index.vue'),
  'purchase/detail/inbound/InboundDetail': () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
  'purchase/detail/inquiry/InquiryDetail': () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
  'purchase/detail/OrderDetail': () => import('@/views/purchase/detail/OrderDetail.vue'),
  'purchase/index': () => import('@/views/purchase/index.vue'),
  'sale/detail/OrderDetail': () => import('@/views/sale/detail/OrderDetail.vue'),
  'sale/index': () => import('@/views/sale/index.vue'),
  'stock/detail/StockDetail': () => import('@/views/stock/detail/StockDetail.vue'),
  'stock/index': () => import('@/views/stock/index.vue'),
  'supplier/create': () => import('@/views/supplier/create.vue'),
  'supplier/detail': () => import('@/views/supplier/detail.vue'),
  'supplier/edit': () => import('@/views/supplier/edit.vue'),
  'supplier/index': () => import('@/views/supplier/index.vue'),
  'supplier/inquiry/index': () => import('@/views/supplier/inquiry/index.vue'),
  'supplier/performance/index': () => import('@/views/supplier/performance/index.vue'),
  'system/config/index': () => import('@/views/system/config/index.vue'),
  'system/department/index': () => import('@/views/system/department/index.vue'),
  'system/dict/index': () => import('@/views/system/dict/index.vue'),
  'system/log/index': () => import('@/views/system/log/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
  'system/permission/index': () => import('@/views/system/permission/index.vue'),
  'admin/sys/permissions/index': () => import('@/views/admin/sys/permissions/index.vue'),
  'admin/tenant/permissions/index': () => import('@/views/admin/tenant/permissions/index.vue'),
  'system/position/index': () => import('@/views/system/position/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/user/index': () => import('@/views/system/user/index.vue'),
  'system/tenant/index': () => import('@/views/system/tenant/index.vue'),
  'system/tenant-approval/index': () => import('@/views/system/tenant-approval/index.vue'),
  'system/data-import/index': () => import('@/views/system/data-import/index.vue'),
  'workflow/instance-monitor': () => import('@/views/workflow/instance-monitor.vue'),
  'workflow/process-analysis': () => import('@/views/workflow/process-analysis.vue'),
  'workflow/task-management': () => import('@/views/workflow/task-management.vue'),

  // 打印模块
  'printing/template/index': () => import('@/views/printing/template/index.vue'),
  'printing/chain/index': () => import('@/views/printing/chain/index.vue'),
  'printing/client/index': () => import('@/views/printing/client/index.vue'),
  'printing/task/index': () => import('@/views/printing/task/index.vue'),
  'printing/designer/index': () => import('@/views/printing/designer/index.vue'),

  // ERP 模块
  'erp/sale/index': () => import('@/views/erp/sale/index.vue'),
  'erp/stock/index': () => import('@/views/erp/stock/index.vue'),
  'erp/sales-analysis/index': () => import('@/views/erp/sales-analysis/index.vue'),
  'erp/sales-report/index': () => import('@/views/erp/sales-report/index.vue'),
  'erp/purchase-exchange/index': () => import('@/views/erp/purchase-exchange/index.vue'),
  'erp/stock-in/index': () => import('@/views/erp/stock-in/index.vue'),
  'erp/stock-in/detail': () => import('@/views/erp/stock-in/detail.vue'),
  'erp/stocktake/index': () => import('@/views/erp/stocktake/index.vue'),
  'erp/stocktake/detail': () => import('@/views/erp/stocktake/detail.vue'),
  'erp/return/index': () => import('@/views/erp/return/index.vue'),
  'erp/return/detail': () => import('@/views/erp/return/detail.vue'),
  'erp/shipment/index': () => import('@/views/erp/shipment/index.vue'),
  'erp/shipment/detail': () => import('@/views/erp/shipment/detail.vue'),
  'erp/pricing/approval/index': () => import('@/views/erp/pricing/approval/index.vue'),
  'erp/pricing/tiers/index': () => import('@/views/erp/pricing/tiers/index.vue'),
  'erp/dashboard/index': () => import('@/views/erp/dashboard/index.vue'),
  'erp/purchase/index': () => import('@/views/erp/purchase/index.vue'),
  'erp/batch/index': () => import('@/views/erp/batch/index.vue'),
  'erp/serial/index': () => import('@/views/erp/serial/index.vue'),
  'erp/serial/detail': () => import('@/views/erp/serial/detail.vue'),

  // ── 库存扩展模块 ──
  'erp/stock-cost-adjust/index': () => import('@/views/erp/stock-cost-adjust/index.vue'),
  'erp/stock-overflow/index': () => import('@/views/erp/stock-overflow/index.vue'),
  'erp/stock-damage/index': () => import('@/views/erp/stock-damage/index.vue'),
  'erp/stock-transfer/index': () => import('@/views/erp/stock-transfer/index.vue'),
  'erp/stock-replenishment/index': () => import('@/views/erp/stock/replenishment/index.vue'),
  'erp/stock-alert-config/index': () => import('@/views/erp/stock-alert-config/index.vue'),
  'erp/stock-bom/index': () => import('@/views/erp/stock-bom/index.vue'),
  'erp/stock-assemble/index': () => import('@/views/erp/stock-assemble/index.vue'),
  'erp/stock-split/index': () => import('@/views/erp/stock-split/index.vue'),

  // ── 商城管理模块 ──
  'erp/mall/config/index': () => import('@/views/erp/mall/config/index.vue'),
  'erp/mall/user-audit/index': () => import('@/views/erp/mall/user-audit/index.vue'),
  'erp/mall/banner/index': () => import('@/views/erp/mall/banner/index.vue'),
  'erp/mall/order/index': () => import('@/views/erp/mall/order/index.vue'),
  'erp/mall/product/index': () => import('@/views/erp/mall/product/index.vue'),

  'fixed-asset/index': () => import('@/views/fixed-asset/index.vue'),
  'fixed-asset/asset/index': () => import('@/views/fixed-asset/asset/index.vue'),
  'fixed-asset/asset/detail': () => import('@/views/fixed-asset/asset/AssetDetail.vue'),
  'fixed-asset/category/index': () => import('@/views/fixed-asset/category/index.vue'),
  'fixed-asset/depreciation/index': () => import('@/views/fixed-asset/depreciation/index.vue'),
  'fixed-asset/transfer/index': () => import('@/views/fixed-asset/transfer/index.vue'),
  'fixed-asset/disposal/index': () => import('@/views/fixed-asset/disposal/index.vue'),
  'fixed-asset/inventory/index': () => import('@/views/fixed-asset/inventory/index.vue'),
  'fixed-asset/report/index': () => import('@/views/fixed-asset/report/index.vue'),
  'fixed-asset/purchase/index': () => import('@/views/fixed-asset/purchase/index.vue'),

  // ── 费用管理模块 ──
  'erp/expense/application/index': () => import('@/views/erp/expense/application/index.vue'),
  'erp/expense/application/detail': () => import('@/views/erp/expense/application/detail.vue'),
  'erp/expense/reimbursement/index': () => import('@/views/erp/expense/reimbursement/index.vue'),
  'erp/expense/reimbursement/detail': () => import('@/views/erp/expense/reimbursement/detail.vue'),
  'erp/expense/approval/index': () => import('@/views/erp/expense/approval/index.vue'),
  'erp/expense/payment/index': () => import('@/views/erp/expense/payment/index.vue'),
  'erp/expense/statistics/index': () => import('@/views/erp/expense/statistics/index.vue'),

  'budget/index': () => import('@/views/budget/index.vue'),
  'budget/template/index': () => import('@/views/budget/template/index.vue'),
  'budget/annual/index': () => import('@/views/budget/annual/index.vue'),
  'budget/annual/detail': () => import('@/views/budget/annual/BudgetDetail.vue'),
  'budget/adjustment/index': () => import('@/views/budget/adjustment/index.vue'),
  'budget/report/index': () => import('@/views/budget/report/index.vue'),
}

/**
 * 路由路径 → 单据类型代码映射表
 * 用于自动为动态路由分配 billType 元数据，配合 checkRouteAccess 实现单据类型级路由访问控制
 *
 * 单据类型代码说明：
 *   504 = 采购订单, 601 = 销售出库, 604 = 销售订单, 801 = 收款单, 802 = 付款单
 */
const routeBillTypeMap: Record<string, string> = {
  // ── 销售订单 (604) ──
  'sale': '604',
  'sale/index': '604',
  'sale/order': '604',
  'erp/sale': '604',
  'erp/sales-analysis': '604',
  'erp/sales-report': '604',

  // ── 销售出库 (601) ──
  'stock': '601',
  'stock/index': '601',
  'stock/detail': '601',
  'erp/stock': '601',
  'erp/stock-in': '601',
  'erp/shipment': '601',
  'erp/stocktake': '601',
  'erp/stocktake': '601',
  'erp/stock-cost-adjust': '601',
  'erp/stock-overflow': '601',
  'erp/stock-damage': '601',
  'erp/stock-transfer': '601',
  'erp/stock-bom': '601',
  'erp/stock-assemble': '601',
  'erp/stock-split': '601',
  'erp/stock-alert-config': '601',
  'erp/stock-replenishment': '601',
  'erp/purchase-exchange': '601',
  'erp/return': '601',

  // ── 采购订单 (504) ──
  'purchase': '504',
  'purchase/index': '504',
  'purchase/order': '504',
  'purchase/inquiry': '504',
  'purchase/inbound': '504',

  // ── 收款单 (801) ──
  'finance/receivable': '801',
  'finance/receivable/index': '801',
  'finance/accounts-receivable': '801',
  'finance/accounts-receivable/index': '801',
  'finance/accounts-receivable/payment-record': '801',
  'finance/accounts-receivable/collection-reminder': '801',
  'finance/accounts-receivable/aging-analysis': '801',
  'finance/pre-receipt': '801',
  'finance/pre-receipt/index': '801',
  'finance/deposit': '801',
  'finance/deposit/index': '801',
  'erp/finance/receivable': '801',
  'finance/receipt': '801',
  'finance/receipt/index': '801',

  // ── 付款单 (802) ──
  'finance/payable': '802',
  'finance/payable/index': '802',
  'finance/accounts-payable': '802',
  'finance/accounts-payable/index': '802',
  'finance/accounts-payable/payment-approval': '802',
  'finance/pre-payment': '802',
  'finance/pre-payment/index': '802',
  'erp/finance/payable': '802',
  'finance/payment': '802',
  'finance/payment/index': '802',
}

/**
 * 根据路由路径获取对应的单据类型代码
 * 移除动态路径参数（如 :id）后进行前缀匹配
 */
function getBillTypeForRoute(routePath: string): string | undefined {
  const normalized = routePath.replace(/^\/+|\/+$/g, '').replace(/\/:\w+\??/g, '').replace(/\/:\w+/g, '')

  // 精确匹配
  if (routeBillTypeMap[normalized]) return routeBillTypeMap[normalized]

  // 逐级前缀匹配（处理嵌套路由）
  const parts = normalized.split('/').filter(Boolean)
  for (let i = parts.length - 1; i >= 1; i--) {
    const prefix = parts.slice(0, i).join('/')
    if (routeBillTypeMap[prefix]) return routeBillTypeMap[prefix]
  }

  return undefined
}

function getComponent(componentPath: string) {
  const normalizedPath = componentPath.replace(/^views\//, '').replace(/\.vue$/, '')
  if (componentMap[normalizedPath]) {
    return componentMap[normalizedPath]
  }
  console.warn('[动态路由] 未找到组件映射:', normalizedPath, '尝试直接导入')
  return () => import(`../views/${normalizedPath}.vue`)
}

function transformMenuToRoute(menu: MenuItem, parentPath: string = ''): RouteRecordRaw {
  let routePath = menu.path || ''
  
  if (parentPath && menu.path && menu.path.startsWith(parentPath + '/')) {
    routePath = menu.path.substring(parentPath.length + 1)
  } else if (menu.path && menu.path.startsWith('/')) {
    routePath = menu.path.substring(1)
  }
  
  // 自动根据路径分配 billType（后端菜单返回时可覆盖此自动推断）
  const billType = menu.billType || getBillTypeForRoute(routePath)

  const route: RouteRecordRaw = {
    path: routePath,
    name: menu.routeName || menu.menuCode,
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      keepAlive: menu.isCache === 1,
      hidden: menu.visible === 0,
      requiresAuth: true,
      ...(billType ? { billType } : {})
    }
  }

  if (menu.menuType === 1 && menu.component) {
    const componentPath = menu.component.replace(/^views\//, '').replace(/\.vue$/, '')
    route.component = getComponent(componentPath)
  }

  if (menu.children && menu.children.length > 0) {
    route.children = menu.children.map(child => transformMenuToRoute(child, menu.path || parentPath))
    if (menu.menuType === 0 && !menu.redirect) {
      route.redirect = menu.path + '/' + (route.children[0].path || '')
    }
  }

  if (menu.redirect) {
    route.redirect = menu.redirect
  }

  return route
}

function buildMenuTree(menus: MenuItem[], parentId: number = 0): MenuItem[] {
  return menus
    .filter(menu => menu.parentId === parentId)
    .sort((a, b) => a.sort - b.sort)
    .map(menu => ({
      ...menu,
      children: buildMenuTree(menus, menu.id)
    }))
}

export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  const userStore = useUserStore()
  const userId = userStore.userId
  const tenantId = userStore.tenantId || 1

  // 先检查 Token 是否有效，避免因后端 Sa-Token 会话过期导致菜单接口异常
  try {
    const checkRes = await request.get('/auth/check', { _skipAuthRefresh: true })
    if (!checkRes?.data?.valid) {
      console.warn('[动态路由] Token 已失效，准备跳转登录页')
      const err: any = new Error('Token 已失效')
      err.status = 401
      throw err
    }
  } catch (checkErr: any) {
    if (checkErr?.status === 401 || checkErr?.response?.status === 401) {
      throw checkErr
    }
    // 其他错误（如网络错误）忽略，继续尝试加载菜单
    console.warn('[动态路由] Token 检查失败，继续尝试加载菜单:', checkErr?.message)
  }

  try {
    const response = await request.get(`/menu/user/client/${CLIENT_TYPE}`, { userId, tenantId })

    if (response.data && response.data.length > 0) {
      const menuTree = response.data
      userStore.menus = menuTree as any
      
      // 注入必须始终存在的路由（不依赖后端菜单树的隐藏详情页等）
      const requiredRoutes = getRequiredRoutes()

      const layoutRoute: RouteRecordRaw = {
        path: '/',
        name: 'Layout',
        component: () => import('@/layouts/BasicLayout.vue'),
        meta: { requiresAuth: true },
        children: [
          ...menuTree.map(menu => transformMenuToRoute(menu)),
          ...requiredRoutes,
          {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('@/views/error/404.vue'),
            meta: { title: '页面不存在', requiresAuth: false }
          }
        ]
      }

      return [layoutRoute]
    }
  } catch (error: any) {
    console.error('[动态路由] 加载失败:', error)
    
    // 检查是否是401错误，如果是则抛出错误让路由守卫处理
    if (error?.response?.status === 401 || error?.status === 401) {
      throw error
    }

    // 菜单接口返回 500 时，二次验证 Token 是否已失效
    if (error?.response?.status === 500) {
      try {
        const checkRes = await request.get('/auth/check', { _skipAuthRefresh: true })
        if (!checkRes?.data?.valid) {
          console.warn('[动态路由] Token 已失效（菜单接口500确认）')
          const err: any = new Error('Token 已失效')
          err.status = 401
          throw err
        }
      } catch (secondaryErr: any) {
        if (secondaryErr?.status === 401 || secondaryErr?.response?.status === 401) {
          throw secondaryErr
        }
      }
    }

    // 其他错误，返回fallback路由
    return getFallbackRoutes()
  }

  return getFallbackRoutes()
}

/**
 * 必须始终存在的隐藏路由（详情页、编辑页等，不依赖后端菜单树）
 * 无论后端菜单 API 返回什么数据，这些路由都会注入到 Layout.children 中
 */
function getRequiredRoutes(): RouteRecordRaw[] {
  return [
    {
      path: 'erp/product/:id',
      name: 'ErpProductDetail',
      component: () => import('@/views/erp/product/detail.vue'),
      meta: { title: '产品详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/create',
      name: 'ErpProductCreate',
      component: () => import('@/views/erp/product/detail.vue'),
      meta: { title: '新增产品', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/price-batch',
      name: 'ErpProductPriceBatch',
      component: () => import('@/views/erp/product/price-batch.vue'),
      meta: { title: '批量价格管理', icon: 'DollarOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/inventory-mode',
      name: 'ErpProductInventoryMode',
      component: () => import('@/views/erp/product/inventory-mode.vue'),
      meta: { title: '库存管理模式', icon: 'SettingOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'purchase/order/:id',
      name: 'PurchaseOrderDetail',
      component: () => import('@/views/purchase/detail/OrderDetail.vue'),
      meta: { title: '采购订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/inquiry/:id',
      name: 'PurchaseInquiryDetail',
      component: () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
      meta: { title: '询价详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/inbound/:id',
      name: 'PurchaseInboundDetail',
      component: () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
      meta: { title: '入库详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'sale/order/:id',
      name: 'SaleOrderDetail',
      component: () => import('@/views/sale/detail/OrderDetail.vue'),
      meta: { title: '销售订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    {
      path: 'crm/customer/:id',
      name: 'CrmCustomerDetail',
      component: () => import('@/views/crm/customer/detail/CustomerDetail.vue'),
      meta: { title: '客户详情', icon: 'UserOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'stock/detail/:id',
      name: 'StockDetail',
      component: () => import('@/views/stock/detail/StockDetail.vue'),
      meta: { title: '库存详情', icon: 'ContainerOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'erp/partner',
      name: 'ErpPartner',
      component: () => import('@/views/erp/partner/index.vue'),
      meta: { title: '往来单位管理', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/partner/:id',
      name: 'ErpPartnerDetail',
      component: () => import('@/views/erp/partner/detail.vue'),
      meta: { title: '单位详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/partner/create',
      name: 'ErpPartnerCreate',
      component: () => import('@/views/erp/partner/detail.vue'),
      meta: { title: '新增单位', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/stock-in/:id',
      name: 'ErpStockInDetail',
      component: () => import('@/views/erp/stock-in/detail.vue'),
      meta: { title: '入库单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/shipment/:id',
      name: 'ErpShipmentDetail',
      component: () => import('@/views/erp/shipment/detail.vue'),
      meta: { title: '出库单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/stocktake/:id',
      name: 'ErpStocktakeDetail',
      component: () => import('@/views/erp/stocktake/detail.vue'),
      meta: { title: '盘点单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/return/:id',
      name: 'ErpReturnDetail',
      component: () => import('@/views/erp/return/detail.vue'),
      meta: { title: '退货单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/batch',
      name: 'ErpBatch',
      component: () => import('@/views/erp/batch/index.vue'),
      meta: { title: '批次管理', icon: 'BarcodeOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/serial',
      name: 'ErpSerial',
      component: () => import('@/views/erp/serial/index.vue'),
      meta: { title: '序列号管理', icon: 'NumberOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/serial/:id',
      name: 'ErpSerialDetail',
      component: () => import('@/views/erp/serial/detail.vue'),
      meta: { title: '序列号追溯', icon: 'SearchOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/detail/:id',
      name: 'SupplierDetail',
      component: () => import('@/views/supplier/detail.vue'),
      meta: { title: '供应商详情', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/create',
      name: 'SupplierCreate',
      component: () => import('@/views/supplier/create.vue'),
      meta: { title: '新增供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/edit/:id',
      name: 'SupplierEdit',
      component: () => import('@/views/supplier/edit.vue'),
      meta: { title: '编辑供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/inquiry/:id?',
      name: 'SupplierInquiry',
      component: () => import('@/views/supplier/inquiry/index.vue'),
      meta: { title: '供应商询价', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/performance/:id?',
      name: 'SupplierPerformance',
      component: () => import('@/views/supplier/performance/index.vue'),
      meta: { title: '供应商绩效', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'workflow/instance-monitor',
      name: 'WorkflowInstanceMonitor',
      component: () => import('@/views/workflow/instance-monitor.vue'),
      meta: { title: '流程监控', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'workflow/task-management',
      name: 'WorkflowTaskManagement',
      component: () => import('@/views/workflow/task-management.vue'),
      meta: { title: '任务管理', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'workflow/process-analysis',
      name: 'WorkflowProcessAnalysis',
      component: () => import('@/views/workflow/process-analysis.vue'),
      meta: { title: '流程分析', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'notification/index',
      name: 'Notification',
      component: () => import('@/views/notification/index.vue'),
      meta: { title: '通知公告', icon: 'BellOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'profile/index',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { title: '个人中心', icon: 'UserOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'charts/index',
      name: 'Charts',
      component: () => import('@/views/charts/index.vue'),
      meta: { title: '图表', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'system/data-import',
      name: 'DataImport',
      component: () => import('@/views/system/data-import/index.vue'),
      meta: { title: '数据导入', icon: 'ImportOutlined', keepAlive: true, requiresAuth: true }
    },

    // ── 商城管理模块 ──
    {
      path: 'erp/mall/config',
      name: 'ErpMallConfig',
      component: () => import('@/views/erp/mall/config/index.vue'),
      meta: { title: '商城配置', icon: 'SettingOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/mall/user-audit',
      name: 'ErpMallUserAudit',
      component: () => import('@/views/erp/mall/user-audit/index.vue'),
      meta: { title: '用户审核', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/mall/banner',
      name: 'ErpMallBanner',
      component: () => import('@/views/erp/mall/banner/index.vue'),
      meta: { title: '轮播图管理', icon: 'PictureOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/mall/order',
      name: 'ErpMallOrder',
      component: () => import('@/views/erp/mall/order/index.vue'),
      meta: { title: '订单管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/mall/product',
      name: 'ErpMallProduct',
      component: () => import('@/views/erp/mall/product/index.vue'),
      meta: { title: '商品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
  ]
}

function getFallbackRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/BasicLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '工作台', icon: 'DashboardOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'purchase',
          name: 'Purchase',
          component: () => import('@/views/purchase/index.vue'),
          meta: { title: '采购管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, billType: '504' }
        },
        {
          path: 'purchase/order/:id',
          name: 'PurchaseOrderDetail',
          component: () => import('@/views/purchase/detail/OrderDetail.vue'),
          meta: { title: '采购订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
        },
        {
          path: 'sale',
          name: 'Sale',
          component: () => import('@/views/sale/index.vue'),
          meta: { title: '销售管理', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
        },
        {
          path: 'stock',
          name: 'Stock',
          component: () => import('@/views/stock/index.vue'),
          meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
        },
        {
          path: 'finance',
          name: 'Finance',
          component: () => import('@/views/finance/index.vue'),
          meta: { title: '财务管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'crm/customer',
          name: 'CrmCustomer',
          component: () => import('@/views/crm/customer/index.vue'),
          meta: { title: '客户管理', icon: 'TeamOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'crm/customer/:id',
          name: 'CrmCustomerDetail',
          component: () => import('@/views/crm/customer/detail/CustomerDetail.vue'),
          meta: { title: '客户详情', icon: 'UserOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'sale/order/:id',
          name: 'SaleOrderDetail',
          component: () => import('@/views/sale/detail/OrderDetail.vue'),
          meta: { title: '销售订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
        },
        {
          path: 'purchase/inquiry/:id',
          name: 'PurchaseInquiryDetail',
          component: () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
          meta: { title: '询价详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
        },
        {
          path: 'purchase/inbound/:id',
          name: 'PurchaseInboundDetail',
          component: () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
          meta: { title: '入库详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
        },
        {
          path: 'stock/detail/:id',
          name: 'StockDetail',
          component: () => import('@/views/stock/detail/StockDetail.vue'),
          meta: { title: '库存详情', icon: 'ContainerOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
        },
        {
          path: 'supplier/detail/:id',
          name: 'SupplierDetail',
          component: () => import('@/views/supplier/detail.vue'),
          meta: { title: '供应商详情', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'supplier/create',
          name: 'SupplierCreate',
          component: () => import('@/views/supplier/create.vue'),
          meta: { title: '新增供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'supplier/edit/:id',
          name: 'SupplierEdit',
          component: () => import('@/views/supplier/edit.vue'),
          meta: { title: '编辑供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'supplier/inquiry/:id?',
          name: 'SupplierInquiry',
          component: () => import('@/views/supplier/inquiry/index.vue'),
          meta: { title: '供应商询价', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'supplier/performance/:id?',
          name: 'SupplierPerformance',
          component: () => import('@/views/supplier/performance/index.vue'),
          meta: { title: '供应商绩效', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'system/tenant',
          name: 'SystemTenant',
          component: () => import('@/views/system/tenant/index.vue'),
          meta: { title: '租户管理', icon: 'ApartmentOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'system/tenant-approval',
          name: 'SystemTenantApproval',
          component: () => import('@/views/system/tenant-approval/index.vue'),
          meta: { title: '租户审批', icon: 'SafetyOutlined', keepAlive: true, requiresAuth: true, permissions: ['system:tenant:approve'] }
        },
        {
          path: 'system/data-import',
          name: 'DataImport',
          component: () => import('@/views/system/data-import/index.vue'),
          meta: { title: '数据导入', icon: 'ImportOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'system/position',
          name: 'SystemPosition',
          component: () => import('@/views/system/position/index.vue'),
          meta: { title: '岗位管理', icon: 'IdcardOutlined', keepAlive: true, requiresAuth: true }
        },
        // ERP 统一模块（含所有业务模块）
        {
          path: 'erp',
          name: 'ErpLayout',
          component: () => import('@/layouts/BasicLayout.vue'),
          redirect: '/purchase',
          meta: { title: 'ERP管理', icon: 'AppstoreOutlined', requiresAuth: true },
          children: [
            {
              path: 'dashboard',
              name: 'ErpDashboard',
              component: () => import('@/views/erp/dashboard/index.vue'),
              meta: { title: 'ERP仪表盘', icon: 'DashboardOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'sale',
              name: 'ErpSale',
              component: () => import('@/views/erp/sale/index.vue'),
              meta: { title: '销售管理', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
            },
            {
              path: 'product',
              name: 'ErpProduct',
              component: () => import('@/views/erp/product/index.vue'),
              meta: { title: '产品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'product/price-batch',
              name: 'ErpProductPriceBatch',
              component: () => import('@/views/erp/product/price-batch.vue'),
              meta: { title: '批量价格管理', icon: 'DollarOutlined', keepAlive: false, requiresAuth: true, hidden: true }
            },
            {
              path: 'product/inventory-mode',
              name: 'ErpProductInventoryMode',
              component: () => import('@/views/erp/product/inventory-mode.vue'),
              meta: { title: '库存管理模式', icon: 'SettingOutlined', keepAlive: false, requiresAuth: true, hidden: true }
            },
            {
              path: 'partner',
              name: 'ErpPartner',
              component: () => import('@/views/erp/partner/index.vue'),
              meta: { title: '往来单位管理', icon: 'TeamOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'batch',
              name: 'ErpBatch',
              component: () => import('@/views/erp/batch/index.vue'),
              meta: { title: '批次管理', icon: 'BarcodeOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'serial',
              name: 'ErpSerial',
              component: () => import('@/views/erp/serial/index.vue'),
              meta: { title: '序列号管理', icon: 'NumberOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'serial/:id',
              name: 'ErpSerialDetail',
              component: () => import('@/views/erp/serial/detail.vue'),
              meta: { title: '序列号追溯', icon: 'SearchOutlined', keepAlive: false, requiresAuth: true, hidden: true }
            },
            {
              path: 'stock',
              name: 'ErpStock',
              component: () => import('@/views/erp/stock/index.vue'),
              meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'purchase',
              name: 'ErpPurchase',
              component: () => import('@/views/erp/purchase/index.vue'),
              meta: { title: '采购管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, billType: '504' }
            },
            {
              path: 'purchase-exchange',
              name: 'ErpPurchaseExchange',
              component: () => import('@/views/erp/purchase-exchange/index.vue'),
              meta: { title: '采购换货', icon: 'SwapOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'stock-in',
              name: 'ErpStockIn',
              component: () => import('@/views/erp/stock-in/index.vue'),
              meta: { title: '入库管理', icon: 'InboxOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'stocktake',
              name: 'ErpStocktake',
              component: () => import('@/views/erp/stocktake/index.vue'),
              meta: { title: '库存盘点', icon: 'CheckSquareOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'return',
              name: 'ErpReturn',
              component: () => import('@/views/erp/return/index.vue'),
              meta: { title: '退货管理', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'shipment',
              name: 'ErpShipment',
              component: () => import('@/views/erp/shipment/index.vue'),
              meta: { title: '发货管理', icon: 'SendOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
            },
            {
              path: 'sales-analysis',
              name: 'ErpSalesAnalysis',
              component: () => import('@/views/erp/sales-analysis/index.vue'),
              meta: { title: '销售分析', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
            },
            {
              path: 'sales-report',
              name: 'ErpSalesReport',
              component: () => import('@/views/erp/sales-report/index.vue'),
              meta: { title: '销售报表', icon: 'LineChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
            },
            {
              path: 'pricing/customer-grade',
              name: 'ErpPricingCustomerGrade',
              component: () => import('@/views/erp/pricing/index.vue'),
              meta: { title: '客户等级定价', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'pricing/approval',
              name: 'ErpPricingApproval',
              component: () => import('@/views/erp/pricing/approval/index.vue'),
              meta: { title: '定价审批', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'pricing/tiers',
              name: 'ErpPricingTiers',
              component: () => import('@/views/erp/pricing/tiers/index.vue'),
              meta: { title: '价格层级', icon: 'PullRequestOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'fixed-asset',
              name: 'ErpFixedAsset',
              component: () => import('@/views/fixed-asset/index.vue'),
              meta: { title: '固定资产', icon: 'BankOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'budget',
              name: 'ErpBudget',
              component: () => import('@/views/budget/index.vue'),
              meta: { title: '预算管理', icon: 'FundOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'mall',
              name: 'ErpMall',
              component: () => import('@/layouts/BasicLayout.vue'),
              redirect: 'mall/config',
              meta: { title: '商城管理', icon: 'ShopOutlined', keepAlive: true, requiresAuth: true },
              children: [
                {
                  path: 'config',
                  name: 'ErpMallConfig',
                  component: () => import('@/views/erp/mall/config/index.vue'),
                  meta: { title: '商城配置', icon: 'SettingOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'user-audit',
                  name: 'ErpMallUserAudit',
                  component: () => import('@/views/erp/mall/user-audit/index.vue'),
                  meta: { title: '用户审核', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'banner',
                  name: 'ErpMallBanner',
                  component: () => import('@/views/erp/mall/banner/index.vue'),
                  meta: { title: '轮播图管理', icon: 'PictureOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'order',
                  name: 'ErpMallOrder',
                  component: () => import('@/views/erp/mall/order/index.vue'),
                  meta: { title: '订单管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'product',
                  name: 'ErpMallProduct',
                  component: () => import('@/views/erp/mall/product/index.vue'),
                  meta: { title: '商品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true }
                }
              ]
            },
            {
              path: 'printing',
              name: 'ErpPrinting',
              component: () => import('@/layouts/BasicLayout.vue'),
              meta: { title: '打印管理', icon: 'PrinterOutlined', keepAlive: true, requiresAuth: true },
              children: [
                {
                  path: 'template',
                  name: 'PrintTemplate',
                  component: () => import('@/views/printing/template/index.vue'),
                  meta: { title: '打印模板', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'chain',
                  name: 'PrintChain',
                  component: () => import('@/views/printing/chain/index.vue'),
                  meta: { title: '打印链路', icon: 'LinkOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'client',
                  name: 'PrintClient',
                  component: () => import('@/views/printing/client/index.vue'),
                  meta: { title: '打印客户端', icon: 'LaptopOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'task',
                  name: 'PrintTask',
                  component: () => import('@/views/printing/task/index.vue'),
                  meta: { title: '打印任务', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'designer/:id?',
                  name: 'PrintDesigner',
                  component: () => import('@/views/printing/designer/index.vue'),
                  meta: { title: '模板设计器', icon: 'EditOutlined', keepAlive: false, requiresAuth: true, hidden: true }
                }
              ]
            },
            {
              path: 'finance',
              name: 'ErpFinance',
              component: () => import('@/views/finance/index.vue'),
              meta: { title: '财务管理', icon: 'AccountBookOutlined', keepAlive: true, requiresAuth: true },
              children: [
                {
                  path: 'subject',
                  name: 'ErpFinanceSubject',
                  component: () => import('@/views/finance/subject/index.vue'),
                  meta: { title: '科目管理', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'voucher',
                  name: 'ErpFinanceVoucher',
                  component: () => import('@/views/finance/voucher/index.vue'),
                  meta: { title: '凭证管理', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'voucher/:id',
                  name: 'ErpFinanceVoucherDetail',
                  component: () => import('@/views/finance/voucher/VoucherDetail.vue'),
                  meta: { title: '凭证详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
                },
                {
                  path: 'receivable',
                  name: 'ErpFinanceReceivable',
                  component: () => import('@/views/finance/receivable/index.vue'),
                  meta: { title: '应收账款', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '801' }
                },
                {
                  path: 'payable',
                  name: 'ErpFinancePayable',
                  component: () => import('@/views/finance/payable/index.vue'),
                  meta: { title: '应付账款', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '802' }
                },
                {
                  path: 'report',
                  name: 'ErpFinanceReport',
                  component: () => import('@/views/finance/report/index.vue'),
                  meta: { title: '财务报表', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'receipt',
                  name: 'ErpFinanceReceipt',
                  component: () => import('@/views/finance/receipt/index.vue'),
                  meta: { title: '收款单管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '801' }
                },
                {
                  path: 'pre-receipt',
                  name: 'ErpFinancePreReceipt',
                  component: () => import('@/views/finance/pre-receipt/index.vue'),
                  meta: { title: '预收款管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '801' }
                },
                {
                  path: 'pre-payment',
                  name: 'ErpFinancePrePayment',
                  component: () => import('@/views/finance/pre-payment/index.vue'),
                  meta: { title: '预付款管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '802' }
                },
                {
                  path: 'payment',
                  name: 'ErpFinancePayment',
                  component: () => import('@/views/finance/payment/index.vue'),
                  meta: { title: '付款单管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '802' }
                },
                {
                  path: 'deposit',
                  name: 'ErpFinanceDeposit',
                  component: () => import('@/views/finance/deposit/index.vue'),
                  meta: { title: '定金押金管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true, billType: '801' }
                },
                {
                  path: 'write-off',
                  name: 'ErpFinanceWriteOff',
                  component: () => import('@/views/finance/write-off/index.vue'),
                  meta: { title: '收付款核销', icon: 'CheckCircleOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'offset',
                  name: 'ErpFinanceOffset',
                  component: () => import('@/views/finance/offset/index.vue'),
                  meta: { title: '往来对冲', icon: 'SwapOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'capital-flow',
                  name: 'ErpFinanceCapitalFlow',
                  component: () => import('@/views/finance/capital-flow/index.vue'),
                  meta: { title: '资金流水台账', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}

const MODULE_ROUTE_MAP: Record<string, string[]> = {
  'sale': ['sale', 'erp/sale', 'erp/sales-analysis', 'erp/sales-report'],
  'purchase': ['purchase', 'erp/purchase', 'erp/purchase-exchange'],
  'warehouse': ['stock', 'erp/stock', 'erp/stock-in', 'erp/stocktake', 'erp/return', 'erp/shipment', 'erp/batch', 'erp/serial'],
  'finance': ['finance', 'erp/finance'],
  'customer': ['crm/customer', 'crm/contract', 'crm/lead', 'crm/opportunity', 'crm/quotation', 'crm/invoice'],
  'supplier': ['supplier'],
  'report': ['charts', 'erp/dashboard'],
  'system': ['system'],
  'printing': ['printing'],
  'notification': ['notification'],
}

/**
 * 根据路由路径推断所属模块编码
 */
function getRouteModule(routePath: string): string | undefined {
  const normalized = routePath.replace(/^\/+|\/+$/g, '').replace(/\/:\w+\??/g, '')
  for (const [moduleCode, paths] of Object.entries(MODULE_ROUTE_MAP)) {
    for (const p of paths) {
      if (normalized === p || normalized.startsWith(p + '/')) {
        return moduleCode
      }
    }
  }
  // 取路径首段作为模块
  const firstSegment = normalized.split('/')[0]
  return firstSegment || undefined
}

export function checkRouteAccess(route: any): boolean {
  const userStore = useUserStore()
  const permissions = route.meta?.permissions as string[] | undefined
  const billType = route.meta?.billType as string | undefined

  const userPermissions = userStore.permissions || []
  if (userPermissions.includes('*')) {
    return true
  }

  // 检查权限码
  if (permissions && permissions.length > 0) {
    if (!permissions.some(permission => userPermissions.includes(permission))) {
      return false
    }
  }

  // 检查单据类型权限
  if (billType) {
    const userBillTypes = new Set(userStore.billTypes || [])
    if (!userBillTypes.has(billType)) {
      return false
    }
  }

  // 租户模块有效性校验（双重检查：角色权限 + 模块有效性）
  const routePath = route.path?.replace(/^\//, '') || ''
  if (routePath && route.meta?.requiresAuth !== false) {
    const routeModule = getRouteModule(routePath)
    if (routeModule && !userStore.hasValidModule(routeModule)) {
      return false
    }
  }

  return true
}

export function filterRoutesByPermission(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  return routes.filter(route => {
    if (!checkRouteAccess(route)) {
      return false
    }
    
    if (route.children) {
      route.children = filterRoutesByPermission(route.children)
    }
    
    return true
  })
}