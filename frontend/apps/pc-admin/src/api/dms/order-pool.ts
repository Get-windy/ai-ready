/**
 * DMS 订单池 API 模块
 * 后端: OrderPoolController (/api/dms/order-pool)
 * 页面: 配送 → 调度管理 → 订单池（80860）
 */
import request from '@/utils/request'

// ── 订单池 ──────────────────────────────────────────
/** 订单池台账行（后端 OrderPoolRowVO：池 + 任务主数据联查，页面不显示裸 taskId） */
export interface DmsOrderPool {
  id: number
  taskId: number
  taskNo: string
  orderNo: string
  /** 订单类型 1-销售配送 2-调拨 3-退货 */
  orderType: number
  orderTypeText: string
  customerName: string
  sourceAddress: string
  customerAddress: string
  /** 配送线路ID（任务侧线路档案引用，名称由页面按线路下拉映射） */
  routeId: number
  /** 配送区域（线路档案区域快照） */
  routeArea: string
  estimatedDistance: number
  goodsAmount: number
  deliveryFee: number
  /** 竞价模式 0-关闭 1-开启 */
  bidEnabled: number
  bidStartPrice: number
  bidCurrentPrice: number
  bidCount: number
  /** 池状态 0-待抢单 1-竞价中 2-已接单 3-已过期 4-已下架 */
  poolStatus: number
  poolStatusText: string
  riderId: number
  riderName: string
  publishedTime: string
  bidStartTime: string
  expireTime: string
  remainSeconds: number
  /** 下架原因/时间（人工下架留痕） */
  offlineReason: string
  offlineTime: string
  remark: string
  createTime: string
}

/** 订单池查询条件 */
export interface OrderPoolQuery {
  current?: number
  size?: number
  taskNo?: string
  orderNo?: string
  customerName?: string
  orderType?: number
  poolStatus?: number
  bidEnabled?: number
  /** 配送线路ID（erp_route） */
  routeId?: number
  riderId?: number
  publishTimeStart?: string
  publishTimeEnd?: string
  feeMin?: number
  feeMax?: number
  keyword?: string
}

export interface DmsBid {
  id: number
  poolId: number
  taskId: number
  riderId: number
  riderName: string
  bidPrice: number
  /** 0-未中标 1-中标 */
  isWin: number
  bidTime: string
}

/** 池状态字典（与后端 PoolStatusEnum 一致） */
export const POOL_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待抢单', color: 'orange' },
  1: { label: '竞价中', color: 'processing' },
  2: { label: '已接单', color: 'green' },
  3: { label: '已过期', color: 'default' },
  4: { label: '已下架', color: 'red' },
}

export const orderPoolApi = {
  /** 台账分页（多条件） */
  page(params: OrderPoolQuery) { return request.get('/dms/order-pool/page', { params }) },

  getById(id: number) { return request.get(`/dms/order-pool/${id}`) },

  /** 发布任务到池（批量；可同时开启竞价） */
  publish(data: { taskIds: number[]; deliveryFee?: number; bidStartPrice?: number; durationMinutes?: number }) {
    return request.post('/dms/order-pool/publish', data)
  },

  /** 下架 */
  offline(id: number, reason?: string) {
    return request.post(`/dms/order-pool/${id}/offline`, null, { params: { reason } })
  },

  /** 开启竞价（待抢单 → 竞价中） */
  enableBid(id: number, startPrice: number, durationMinutes: number) {
    return request.post(`/dms/order-pool/${id}/enable-bid`, null, { params: { startPrice, durationMinutes } })
  },

  /** 关闭竞价（竞价中 → 待抢单；作废本次全部报价），返回作废条数 */
  disableBid(id: number) { return request.post(`/dms/order-pool/${id}/disable-bid`) },

  /** 结算竞价（价低优先，同事务指派任务） */
  settle(id: number) { return request.post(`/dms/order-pool/${id}/settle`) },

  /** 强制分配（定向指派） */
  forceAssign(id: number, data: { riderId: number; riderName?: string }) {
    return request.post(`/dms/order-pool/${id}/force-assign`, data)
  },

  /** 过期扫描 */
  expireScan() { return request.post('/dms/order-pool/expire-scan') },

  /** 抢单 */
  grab(id: number, data: { riderId: number; riderName?: string }) {
    return request.post(`/dms/order-pool/${id}/grab`, data)
  },

  /** 出价 */
  bid(id: number, data: { riderId: number; riderName?: string; price: number }) {
    return request.post(`/dms/order-pool/${id}/bid`, data)
  },

  /** 竞价记录（价低优先） */
  bidList(id: number): Promise<DmsBid[]> { return request.get(`/dms/order-pool/${id}/bid-list`) },

  /** 取消出价 */
  cancelBid(id: number, data: { bidId: number; riderId: number }) {
    return request.post(`/dms/order-pool/${id}/cancel-bid`, data)
  },

  /** 导出台账（真实 xlsx） */
  exportExcel(params: OrderPoolQuery) {
    return request.get('/dms/order-pool/export', { params, responseType: 'blob' })
  },
}
