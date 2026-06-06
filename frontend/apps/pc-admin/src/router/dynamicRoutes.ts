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
  children?: MenuItem[]
}

const componentMap: Record<string, () => Promise<any>> = {
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
  'system/position/index': () => import('@/views/system/position/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/user/index': () => import('@/views/system/user/index.vue'),
  'system/tenant/index': () => import('@/views/system/tenant/index.vue'),
  'workflow/instance-monitor': () => import('@/views/workflow/instance-monitor.vue'),
  'workflow/process-analysis': () => import('@/views/workflow/process-analysis.vue'),
  'workflow/task-management': () => import('@/views/workflow/task-management.vue'),

  // ERP 模块
  'erp/sale/index': () => import('@/views/erp/sale/index.vue'),
  'erp/stock/index': () => import('@/views/erp/stock/index.vue'),
  'erp/sales-analysis/index': () => import('@/views/erp/sales-analysis/index.vue'),
  'erp/sales-report/index': () => import('@/views/erp/sales-report/index.vue'),
  'erp/purchase-exchange/index': () => import('@/views/erp/purchase-exchange/index.vue'),
  'erp/stock-in/index': () => import('@/views/erp/stock-in/index.vue'),
  'erp/stocktake/index': () => import('@/views/erp/stocktake/index.vue'),
  'erp/return/index': () => import('@/views/erp/return/index.vue'),
  'erp/shipment/index': () => import('@/views/erp/shipment/index.vue'),
  'erp/pricing/approval/index': () => import('@/views/erp/pricing/approval/index.vue'),
  'erp/pricing/tiers/index': () => import('@/views/erp/pricing/tiers/index.vue'),
  'fixed-asset/index': () => import('@/views/fixed-asset/index.vue'),
  'fixed-asset/asset/index': () => import('@/views/fixed-asset/asset/index.vue'),
  'fixed-asset/asset/detail': () => import('@/views/fixed-asset/asset/AssetDetail.vue'),
  'fixed-asset/category/index': () => import('@/views/fixed-asset/category/index.vue'),
  'fixed-asset/depreciation/index': () => import('@/views/fixed-asset/depreciation/index.vue'),
  'fixed-asset/transfer/index': () => import('@/views/fixed-asset/transfer/index.vue'),
  'fixed-asset/disposal/index': () => import('@/views/fixed-asset/disposal/index.vue'),
  'fixed-asset/inventory/index': () => import('@/views/fixed-asset/inventory/index.vue'),
  'fixed-asset/report/index': () => import('@/views/fixed-asset/report/index.vue'),
  'budget/index': () => import('@/views/budget/index.vue'),
  'budget/template/index': () => import('@/views/budget/template/index.vue'),
  'budget/annual/index': () => import('@/views/budget/annual/index.vue'),
  'budget/annual/detail': () => import('@/views/budget/annual/BudgetDetail.vue'),
  'budget/adjustment/index': () => import('@/views/budget/adjustment/index.vue'),
  'budget/report/index': () => import('@/views/budget/report/index.vue'),
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
  
  const route: RouteRecordRaw = {
    path: routePath,
    name: menu.routeName || menu.menuCode,
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      keepAlive: menu.isCache === 1,
      hidden: menu.visible === 0,
      requiresAuth: true
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
      
      const layoutRoute: RouteRecordRaw = {
        path: '/',
        name: 'Layout',
        component: () => import('@/layouts/BasicLayout.vue'),
        meta: { requiresAuth: true },
        children: [
          ...menuTree.map(menu => transformMenuToRoute(menu)),
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
          meta: { title: '采购管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'purchase/order/:id',
          name: 'PurchaseOrderDetail',
          component: () => import('@/views/purchase/detail/OrderDetail.vue'),
          meta: { title: '采购订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'sale',
          name: 'Sale',
          component: () => import('@/views/sale/index.vue'),
          meta: { title: '销售管理', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true }
        },
        {
          path: 'stock',
          name: 'Stock',
          component: () => import('@/views/stock/index.vue'),
          meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true }
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
          meta: { title: '销售订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'purchase/inquiry/:id',
          name: 'PurchaseInquiryDetail',
          component: () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
          meta: { title: '询价详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'purchase/inbound/:id',
          name: 'PurchaseInboundDetail',
          component: () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
          meta: { title: '入库详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
        },
        {
          path: 'stock/detail/:id',
          name: 'StockDetail',
          component: () => import('@/views/stock/detail/StockDetail.vue'),
          meta: { title: '库存详情', icon: 'ContainerOutlined', keepAlive: false, requiresAuth: true, hidden: true }
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
          path: 'system/tenant',
          name: 'SystemTenant',
          component: () => import('@/views/system/tenant/index.vue'),
          meta: { title: '租户管理', icon: 'ApartmentOutlined', keepAlive: true, requiresAuth: true }
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
              path: 'sale',
              name: 'ErpSale',
              component: () => import('@/views/erp/sale/index.vue'),
              meta: { title: '销售管理', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'stock',
              name: 'ErpStock',
              component: () => import('@/views/erp/stock/index.vue'),
              meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'purchase-exchange',
              name: 'ErpPurchaseExchange',
              component: () => import('@/views/erp/purchase-exchange/index.vue'),
              meta: { title: '采购换货', icon: 'SwapOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'stock-in',
              name: 'ErpStockIn',
              component: () => import('@/views/erp/stock-in/index.vue'),
              meta: { title: '入库管理', icon: 'InboxOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'stocktake',
              name: 'ErpStocktake',
              component: () => import('@/views/erp/stocktake/index.vue'),
              meta: { title: '库存盘点', icon: 'CheckSquareOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'return',
              name: 'ErpReturn',
              component: () => import('@/views/erp/return/index.vue'),
              meta: { title: '退货管理', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'shipment',
              name: 'ErpShipment',
              component: () => import('@/views/erp/shipment/index.vue'),
              meta: { title: '发货管理', icon: 'SendOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'sales-analysis',
              name: 'ErpSalesAnalysis',
              component: () => import('@/views/erp/sales-analysis/index.vue'),
              meta: { title: '销售分析', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true }
            },
            {
              path: 'sales-report',
              name: 'ErpSalesReport',
              component: () => import('@/views/erp/sales-report/index.vue'),
              meta: { title: '销售报表', icon: 'LineChartOutlined', keepAlive: true, requiresAuth: true }
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
                  meta: { title: '应收账款', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'payable',
                  name: 'ErpFinancePayable',
                  component: () => import('@/views/finance/payable/index.vue'),
                  meta: { title: '应付账款', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
                },
                {
                  path: 'report',
                  name: 'ErpFinanceReport',
                  component: () => import('@/views/finance/report/index.vue'),
                  meta: { title: '财务报表', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}

export function checkRouteAccess(route: any): boolean {
  const userStore = useUserStore()
  const permissions = route.meta?.permissions as string[] | undefined
  
  if (!permissions || permissions.length === 0) {
    return true
  }
  
  const userPermissions = userStore.permissions || []
  if (userPermissions.includes('*')) {
    return true
  }
  
  return permissions.some(permission => userPermissions.includes(permission))
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