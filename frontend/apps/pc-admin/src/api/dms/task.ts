/**
 * DMS 配送任务 API 模块
 * 后端: TaskController (/api/dms/task)
 */
import request from '@/utils/request'

// ── 配送任务 ──────────────────────────────────────────
/** 与后端 DmsTask 实体一致 */
export interface DmsTask {
  id: number
  tenantId?: number
  /** 任务编号 */
  taskNo: string
  orderId?: number
  /** 关联订单号 */
  orderNo: string
  /** 订单类型: 1-销售配送 2-调拨 3-退货 */
  orderType: number
  channelId?: number
  riderId?: number
  /** 调度方式: 1-自动 2-手动 3-抢单 4-竞价 */
  dispatchType?: number
  sourceWarehouseId?: number
  sourceAddress?: string
  sourceLat?: number
  sourceLng?: number
  customerId?: number
  customerName?: string
  customerPhone?: string
  customerAddress?: string
  customerLat?: number
  customerLng?: number
  totalItems?: number
  totalQuantity?: number
  totalWeight?: number
  totalVolume?: number
  goodsAmount?: number
  deliveryFee?: number
  collectOnDelivery?: number
  estimatedDistance?: number
  /** 优先级: 1-普通 2-紧急 3-加急 */
  priority?: number
  /** 状态: 0-待分配 1-已分配 2-已接单 3-取货中 4-配送中 5-已签收 6-已完成 7-已取消 8-异常 */
  status: number
  urgeCount?: number
  urgeTime?: string
  loadTime?: string
  dispatchTime?: string
  pickupTime?: string
  deliveryTime?: string
  completedTime?: string
  deadlineTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DmsTaskPageQuery {
  /** 页码（后端参数名为 current） */
  current?: number
  /** 每页条数 */
  size?: number
  tenantId?: number
  status?: number
  riderId?: number
  orderNo?: string
}

export const taskApi = {
  /** 分页查询配送任务 */
  page(params: DmsTaskPageQuery) { return request.get('/dms/task/page', params) },

  /** 任务详情 */
  getById(id: number) { return request.get(`/dms/task/${id}`) },

  /** 创建配送任务（后端初始化为待分配） */
  create(data: Partial<DmsTask>) { return request.post('/dms/task', data) },

  /** 更新配送任务 */
  update(id: number, data: Partial<DmsTask>) { return request.put(`/dms/task/${id}`, data) },

  /** 状态流转（后端校验 fromStatus -> toStatus 合法性） */
  updateStatus(id: number, fromStatus: number, toStatus: number) {
    return request.put(`/dms/task/${id}/status`, { fromStatus, toStatus })
  },

  /** 取消任务 */
  cancel(id: number) { return request.post(`/dms/task/${id}/cancel`) },

  /** 标记任务异常 */
  markException(id: number) { return request.post(`/dms/task/${id}/exception`) },
}
