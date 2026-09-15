/**
 * DMS 配送单（配送任务）API 模块
 * 后端: TaskController (/api/dms/task)
 * 页面: 配送 → 配送业务 → 配送单[历史]（双入口：/form 表单页、/index 列表页）
 */
import request from '@/utils/request'

// ── 配送单（配送任务） ──────────────────────────────────
/** 与后端 DmsTask 实体一致 */
export interface DmsTask {
  id?: number | string
  tenantId?: number
  /** 任务编号（PSD-YYYYMMDD-序号） */
  taskNo: string
  orderId?: number
  /** 关联订单号 */
  orderNo: string
  /** 来源单据编号 */
  sourceBillNo?: string
  /** 订单类型: 1-销售配送 2-调拨 3-退货 */
  orderType: number
  channelId?: number
  /** 配送司机ID */
  riderId?: number
  /** 配送司机名称（快照） */
  riderName?: string
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
  /** 商品行数 */
  totalItems?: number
  /** 发货数量 */
  totalQuantity?: number
  totalWeight?: number
  totalVolume?: number
  /** 发货金额 */
  goodsAmount?: number
  deliveryFee?: number
  collectOnDelivery?: number
  estimatedDistance?: number
  /** 指定配送日期 */
  deliveryDate?: string
  /** 配送车辆ID */
  vehicleId?: number
  /** 配送车辆（车牌号快照） */
  vehicleName?: string
  /** 送货员ID */
  deliverymanId?: number
  /** 送货员名称（快照） */
  deliverymanName?: string
  /** 配送线路ID（线路主数据 erp_route） */
  routeId?: number
  /** 配送区域 */
  routeArea?: string
  /** 配送单量 */
  orderCount?: number
  /** 订金金额 */
  depositAmount?: number
  /** 退货单量 */
  returnOrderCount?: number
  /** 退货数量 */
  returnQuantity?: number
  /** 退货金额 */
  returnAmount?: number
  /** 装箱数量 */
  boxQuantity?: number
  /** 优先级: 1-普通 2-紧急 3-加急 */
  priority?: number
  /** 状态: 0-待分配 1-已分配 2-已接单 3-取货中 4-配送中 5-已签收 6-已完成 7-已取消 8-异常 */
  status?: number
  urgeCount?: number
  urgeTime?: string
  loadTime?: string
  dispatchTime?: string
  /** 配送开始时间 */
  pickupTime?: string
  /** 配送结束时间 */
  deliveryTime?: string
  completedTime?: string
  /** 要求送达时间 */
  deadlineTime?: string
  remark?: string
  /** 打印次数 */
  printCount?: number
  /** 制单人名称（快照） */
  creatorName?: string
  createTime?: string
  updateTime?: string
  // ── 调度工作台展示字段（后端 /dms/task/page 联查/聚合填充，非持久化列） ──
  /** 配送员电话（联查 dms_rider） */
  riderPhone?: string
  /** 配送员当前在途单数（负载） */
  activeTaskCount?: number
  /** 距目的地直线距离(km) */
  distanceKm?: number
  /** 是否超时（在途且已过要求送达时间） */
  overdue?: boolean
}

/** 配送单商品明细行 */
export interface DmsTaskItem {
  id?: number | string
  taskId?: number
  lineNo?: number
  productId?: number
  productCode?: string
  productName?: string
  barcode?: string
  spec?: string
  unit?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  weight?: number
  volume?: number
  remark?: string
}

export interface DmsTaskDetail extends DmsTask {
  items?: DmsTaskItem[]
}

/** 配送查询-查询条件下拉项 */
export interface TaskFilterOption {
  id?: number | string
  name: string
  extra?: string
}

/** 配送查询-查询条件下拉（司机 / 车辆 / 送货员 / 制单人） */
export interface TaskFilterOptions {
  drivers?: TaskFilterOption[]
  deliverymen?: TaskFilterOption[]
  vehicles?: TaskFilterOption[]
  creators?: string[]
}

/** 批量操作结果（后端 BatchResultVO：逐单成功/失败反馈） */
export interface BatchResultVO {
  total: number
  success: number
  failed: number
  items?: Array<{ taskId?: number; taskNo?: string; reason?: string }>
}

/** 调度审计日志（后端 dms_task_log） */
export interface DmsTaskLogVO {
  id?: number | string
  taskId?: number
  taskNo?: string
  action?: string
  actionText?: string
  fromRiderId?: number
  fromRiderName?: string
  toRiderId?: number
  toRiderName?: string
  reason?: string
  operatorId?: number
  operatorName?: string
  createTime?: string
}

/** 配送单查询条件（对齐《配送单开发文档》§2.1 + 《调度任务开发文档》§3.3） */
export interface DmsTaskPageQuery {
  current?: number
  size?: number
  pageNum?: number
  pageSize?: number
  taskNo?: string
  sourceBillNo?: string
  orderNo?: string
  customerName?: string
  /** 客户ID（往来单位选择器，雪花ID 以字符串回传避免精度丢失） */
  customerId?: number | string
  receiverKeyword?: string
  statusList?: number[]
  /** 优先级：1-普通 2-紧急 3-加急 */
  priority?: number
  /** 订单类型：1-销售配送 2-调拨 3-退货 */
  orderType?: number
  /** 仅看未分配（配送员为空） */
  unassigned?: boolean
  /** 仅看异常任务 */
  abnormal?: boolean
  /** 仅看超时在途任务 */
  overdue?: boolean
  riderId?: number | string
  vehicleId?: number | string
  deliverymanId?: number | string
  routeId?: number | string
  routeArea?: string
  creatorName?: string
  remark?: string
  deliveryDateStart?: string
  deliveryDateEnd?: string
  createTimeStart?: string
  createTimeEnd?: string
  showRed?: boolean
  keyword?: string
  sortField?: string
  sortOrder?: string
}

export const taskApi = {
  // ── 配送单金标准接口 ──────────────────────────────
  /** 生成下一配送单号（PSD-YYYYMMDD-序号） */
  nextNo() { return request.get('/dms/task/next-no') },

  /** 分页查询配送单（按单据视图） */
  page(params: DmsTaskPageQuery) { return request.get('/dms/task/page', { params }) },

  /** 分页查询配送单（按明细视图） */
  pageDetail(params: DmsTaskPageQuery) { return request.get('/dms/task/page-detail', { params }) },

  /** 配送查询-查询条件下拉（配送司机 / 配送车辆 / 送货员 / 制单人） */
  filterOptions() { return request.get('/dms/task/filter-options') },

  /** 配送单详情（含商品明细） */
  detail(id: number | string) { return request.get(`/dms/task/${id}`) },

  /** 保存配送单（头 + 商品明细；新建/修改） */
  save(data: Partial<DmsTaskDetail>) { return request.post('/dms/task/save', data) },

  /** 删除配送单 */
  remove(id: number | string) { return request.delete(`/dms/task/${id}`) },

  /** 审核（待分配 → 已分配） */
  audit(id: number | string) { return request.post(`/dms/task/${id}/audit`) },

  /** 反审核（已分配 → 待分配） */
  unaudit(id: number | string) { return request.post(`/dms/task/${id}/unaudit`) },

  /** 记录打印次数 */
  print(id: number | string) { return request.post(`/dms/task/${id}/print`) },

  // ── 调度任务（配送 → 调度管理 → 调度任务）专属接口 ──────────────
  /** 指派配送员（调度视角，写调度审计） */
  assign(id: number | string, riderId: number | string, reason?: string) {
    return request.post(`/dms/task/${id}/assign`, { riderId, reason })
  },

  /** 改派配送员（调度视角；fromRiderId 缺省取任务当前配送员） */
  reassign(id: number | string, toRiderId: number | string, fromRiderId?: number | string, reason?: string) {
    return request.post(`/dms/task/${id}/reassign`, { fromRiderId, toRiderId, reason })
  },

  /** 批量指派（逐单结果反馈） */
  batchAssign(taskIds: Array<number | string>, riderId: number | string, reason?: string): Promise<BatchResultVO> {
    return request.post('/dms/task/batch-assign', { taskIds, riderId, reason })
  },

  /** 批量取消（逐单结果反馈） */
  batchCancel(taskIds: Array<number | string>, reason?: string): Promise<BatchResultVO> {
    return request.post('/dms/task/batch-cancel', { taskIds, reason })
  },

  /** 批量记录打印次数（批量打印后回写） */
  batchPrint(taskIds: Array<number | string>): Promise<number> {
    return request.post('/dms/task/batch-print', { taskIds })
  },

  /** 任务调度审计时间线 */
  logs(id: number | string): Promise<DmsTaskLogVO[]> {
    return request.get(`/dms/task/${id}/logs`)
  },

  /** 导出配送单列表（真实 xlsx，返回 Blob） */
  exportExcel(params: DmsTaskPageQuery) {
    return request.get('/dms/task/export', { params, responseType: 'blob' })
  },

  // ── 兼容旧入口（司机端 / 调度侧） ──────────────────
  /** 任务详情（兼容旧字段名） */
  getById(id: number | string) { return request.get(`/dms/task/${id}`) },

  /** 创建配送任务（后端初始化为待分配） */
  create(data: Partial<DmsTask>) { return request.post('/dms/task', data) },

  /** 更新配送任务 */
  update(id: number | string, data: Partial<DmsTask>) { return request.put(`/dms/task/${id}`, data) },

  /** 状态流转（后端校验 fromStatus -> toStatus 合法性） */
  updateStatus(id: number | string, fromStatus: number, toStatus: number) {
    return request.put(`/dms/task/${id}/status`, { fromStatus, toStatus })
  },

  /** 取消任务（可带原因，写调度审计） */
  cancel(id: number | string, reason?: string) { return request.post(`/dms/task/${id}/cancel`, { reason }) },

  /** 标记任务异常（可带原因，写调度审计） */
  markException(id: number | string, reason?: string) {
    return request.post(`/dms/task/${id}/exception`, { reason })
  },
}
