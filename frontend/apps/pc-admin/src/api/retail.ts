/**
 * 零售 POS 收银 API
 * 对接后端 erp-sales：
 *   RetailController      /api/sales/retail
 *   RetailShiftController /api/sales/retail/shift
 *
 * 注意：该模块端点均为裸返回（实体 / List / Page / void，无统一 wrapper）。
 * - List / Page 走标准 request（响应拦截器对数组与 records+total 原样透传）
 * - 实体 / void 端点走原生 axios 自行解包（参照 @/api/crm.ts 的 getRaw/postRaw 既定模式），
 *   否则会被标准拦截器误判为业务失败
 */
import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'

const BASE = '/api/sales/retail'

// ── 类型定义 ─────────────────────────────────────────────

/** 商品快查结果（/products/quick 返回的 Map） */
export interface QuickProduct {
  id: number
  productName: string
  productCode?: string
  barcode?: string
  specification?: string
  model?: string
  unit?: string
  unitPrice?: number
  retailPrice?: number
  wholesalePrice?: number
  availableStock?: number
  imageUrl?: string
  [key: string]: any
}

/** 零售单明细行（提交） */
export interface RetailItemPayload {
  productId: number
  productCode?: string
  productName?: string
  barcode?: string
  specification?: string
  unit?: string
  quantity: number
  unitPrice: number
  remark?: string
}

/** 零售单头（提交，后端 calculateAmounts 会重算 amount/totalQuantity，
 *  payableAmount > 0 时保留前端传入值） */
export interface RetailOrderPayload {
  warehouseId: number
  warehouseName?: string
  cashierId: number
  cashierName?: string
  customerId?: number
  customerName?: string
  memberCardNo?: string
  memberName?: string
  saleType?: string
  status?: number
  generationMethod?: string
  posMode?: boolean
  directDiscount?: number
  payableAmount?: number
  changeAmount?: number
  orderDate?: string
  remark?: string
}

/** 支付行（结算） */
export interface RetailPayment {
  paymentMethod: string // CASH/WECHAT/ALIPAY/CARD/TRANSFER
  paymentAmount: number
  paymentAccount?: string
  transactionNo?: string
  remark?: string
}

/** 零售单（返回） */
export interface RetailOrder {
  id: number
  retailNo: string
  status: number // 0草稿 1已完成 2挂单 3已作废
  warehouseId?: number
  warehouseName?: string
  cashierId?: number
  cashierName?: string
  customerId?: number
  customerName?: string
  memberCardNo?: string
  memberName?: string
  totalQuantity?: number
  amount?: number
  directDiscount?: number
  payableAmount?: number
  totalReceived?: number
  changeAmount?: number
  cashAmount?: number
  wechatAmount?: number
  alipayAmount?: number
  cardAmount?: number
  transferAmount?: number
  remark?: string
  createTime?: string
  completedTime?: string
  [key: string]: any
}

/** 零售单明细行（返回） */
export interface RetailOrderItem {
  id?: number
  orderId?: number
  productId?: number
  productCode?: string
  productName?: string
  barcode?: string
  specification?: string
  unit?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  [key: string]: any
}

/** 零售单详情 / 打印数据 */
export interface RetailOrderDetailVO {
  order: RetailOrder
  items: RetailOrderItem[]
  payments?: RetailPayment[]
}

/** 收银班次 */
export interface RetailShift {
  id: number
  shiftNo: string
  cashierId: number
  cashierName?: string
  warehouseId?: number
  openTime?: string
  openingCash?: number
  closeTime?: string
  closingCash?: number
  expectedCash?: number
  difference?: number
  orderCount?: number
  totalAmount?: number
  cashAmount?: number
  qrAmount?: number
  otherAmount?: number
  status: number // 1营业中 2已交班
  remark?: string
}

/** 班次详情（含该班次已结算零售单） */
export interface ShiftDetailVO {
  shift: RetailShift
  orders: RetailOrder[]
}

// ── 裸返回解包（参照 @/api/crm.ts 既定模式） ──────────────

function authHeaders(): Record<string, string> {
  const token = getToken()
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    tenantId: localStorage.getItem('tenantId') || '1'
  }
}

/** 裸实体响应解包：HTTP 200 即成功；body 带数值 code≠200 视为业务错误 */
function unwrapRaw<T>(body: any): T {
  if (
    body && typeof body === 'object' && !Array.isArray(body) &&
    typeof body.code === 'number' && body.code !== 200 && 'message' in body
  ) {
    throw new Error(body.message || `请求失败(${body.code})`)
  }
  return body as T
}

function extractError(e: any): Error {
  return new Error(e?.response?.data?.message || e?.message || '请求失败')
}

async function getRaw<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.get(url, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

async function postRaw<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.post(url, data ?? null, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

async function putRaw<T = any>(url: string, data?: any): Promise<T> {
  try {
    const res = await axios.put(url, data ?? null, { headers: authHeaders() })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

// ── 零售单 ───────────────────────────────────────────────

export const retailApi = {
  /** 商品快速查找（条码/编码/名称，裸 List） */
  quickSearch(keyword: string, warehouseId?: number): Promise<QuickProduct[]> {
    return request.get(`${BASE}/products/quick`, { keyword, warehouseId })
  },
  /** 挂单列表（裸 List） */
  holdList(warehouseId?: number): Promise<RetailOrder[]> {
    return request.get(`${BASE}/hold-list`, { warehouseId })
  },
  /** 详情（含明细行+支付明细，裸 VO） */
  getDetail(id: number | string): Promise<RetailOrderDetailVO> {
    return getRaw<RetailOrderDetailVO>(`${BASE}/${id}`)
  },
  /** 创建零售单（裸实体） */
  create(order: RetailOrderPayload, items: RetailItemPayload[]): Promise<RetailOrder> {
    return postRaw<RetailOrder>(BASE, { order, items })
  },
  /** 更新零售单（草稿/取单后改单，裸实体） */
  update(id: number | string, order: RetailOrderPayload, items: RetailItemPayload[]): Promise<RetailOrder> {
    return putRaw<RetailOrder>(`${BASE}/${id}`, { order, items })
  },
  /** 结算（裸实体；后端写支付记录并扣减库存） */
  settle(id: number | string, payments: RetailPayment[]): Promise<RetailOrder> {
    return postRaw<RetailOrder>(`${BASE}/${id}/settle`, { payments })
  },
  /** 挂单 */
  hold(id: number | string): Promise<void> {
    return postRaw<void>(`${BASE}/${id}/hold`)
  },
  /** 取单 */
  unhold(id: number | string): Promise<void> {
    return postRaw<void>(`${BASE}/${id}/unhold`)
  },
  /** 作废 */
  voidOrder(id: number | string, reason?: string): Promise<void> {
    return postRaw<void>(`${BASE}/${id}/void`, null, { reason })
  },
  /** 打印数据（裸 VO） */
  getPrintData(id: number | string): Promise<RetailOrderDetailVO> {
    return getRaw<RetailOrderDetailVO>(`${BASE}/${id}/print-data`)
  },
  /** 打印后更新计数 */
  afterPrint(id: number | string): Promise<void> {
    return postRaw<void>(`${BASE}/${id}/print`)
  },
}

// ── 收银交班 ─────────────────────────────────────────────

export const retailShiftApi = {
  /** 当前营业中班次（无则后端返回空 body） */
  async current(cashierId: number): Promise<RetailShift | null> {
    const res = await getRaw<RetailShift | null>(`${BASE}/shift/current`, { cashierId })
    return res || null
  },
  /** 开班 */
  open(data: { cashierId: number; cashierName?: string; warehouseId?: number; openingCash: number; remark?: string }): Promise<RetailShift> {
    return postRaw<RetailShift>(`${BASE}/shift/open`, data)
  },
  /** 交班（后端自动汇总班次销售并计算长短款） */
  close(data: { shiftId: number; closingCash: number; remark?: string }): Promise<RetailShift> {
    return postRaw<RetailShift>(`${BASE}/shift/close`, data)
  },
  /** 历史班次分页（裸 Page） */
  page(params: Record<string, any>): Promise<any> {
    return request.get(`${BASE}/shift/page`, params)
  },
  /** 班次详情（含已结算零售单，裸 VO） */
  getDetail(id: number | string): Promise<ShiftDetailVO> {
    return getRaw<ShiftDetailVO>(`${BASE}/shift/${id}`)
  },
}
