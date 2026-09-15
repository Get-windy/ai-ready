/**
 * DMS 配送结算 API 模块
 * 后端: SettlementController (/api/dms/settlement)
 *
 * 金标准口径（2026-09-13）：
 *   · 计费规则**配置化**（`dms_config` 的 `dms.settlement.*`，改价不改代码）
 *   · 结算单闭环：生成（草稿）→ 确认（锁定金额）→ 推送 ERP（幂等）
 *   · 报表口径 = 已签收/已完成（status 5/6）
 */
import request from '@/utils/request'

/** 计费规则（配置化费率） */
export interface SettlementRule {
  /** 起步价（元） */
  baseFee?: number
  /** 免费里程（公里） */
  freeDistanceKm?: number
  /** 每公里单价（元/公里） */
  perKmRate?: number
  /** 夜间时段附加系数（按起步价倍数） */
  timeSurchargeRate?: number
  /** 加急附加费（元） */
  urgentSurcharge?: number
}

/** 结算单 */
export interface DmsSettlement {
  id?: number
  settlementNo?: string
  /** 1-配送员 2-渠道 */
  targetType?: number
  targetId?: number
  targetName?: string
  periodStart?: string
  periodEnd?: string
  taskCount?: number
  totalAmount?: number
  /** 0-草稿 1-已确认 2-已推送 */
  status?: number
  pushTime?: string
  pushCount?: number
  pushTraceId?: string
  ruleSnapshot?: string
  remark?: string
  createTime?: string
}

/** 结算单明细（费用构成） */
export interface DmsSettlementItem {
  id?: number
  settlementId?: number
  taskId?: number
  taskNo?: string
  signTime?: string
  distanceKm?: number
  baseFee?: number
  mileageFee?: number
  timeSurcharge?: number
  urgentSurcharge?: number
  totalFee?: number
}

/** 按任务计算配送费返回结构 */
export interface SettlementFee {
  taskId?: number
  taskNo?: string
  baseFee?: number
  distanceKm?: number
  freeDistanceKm?: number
  mileageFee?: number
  timeSurcharge?: number
  urgentSurcharge?: number
  totalFee?: number
  rule?: SettlementRule
  [k: string]: any
}

/** 周期结算报表 */
export interface SettlementReport {
  totalTasks?: number
  totalFee?: number
  byRider?: Array<Record<string, any>>
  byDay?: Array<Record<string, any>>
  rule?: SettlementRule
  [k: string]: any
}

/** 计费规则（按 结算对象 × 渠道/线路 × 生效期 差异化计价） */
export interface DmsSettlementRule {
  id?: number
  ruleCode?: string
  ruleName?: string
  /** 1-配送员 2-渠道 */
  targetType?: number
  channelId?: number | null
  routeId?: number | null
  /** 1-按单 2-按距离 3-按重量 4-组合 */
  billingType?: number
  baseFee?: number
  freeDistanceKm?: number
  perKmRate?: number
  perKgRate?: number
  timeSurchargeRate?: number
  urgentSurcharge?: number
  /** 1-日结 2-周结 3-月结 */
  settleCycle?: number
  effectiveStart?: string
  effectiveEnd?: string
  priority?: number
  /** 0-停用 1-启用 */
  status?: number
  remark?: string
}

/** 对账行（结算单 vs 财务） */
export interface SettlementReconcileRow {
  id?: number
  settlementNo?: string
  targetType?: number
  targetName?: string
  periodStart?: string
  periodEnd?: string
  taskCount?: number
  totalAmount?: number
  status?: number
  erpVoucherNo?: string
  voucherAmount?: number
  accounted?: boolean
  erpPayableNo?: string
  payableStatus?: string
  settledAmount?: number
  diff?: number
  matched?: boolean
}

export interface SettlementReconcile {
  startDate?: string
  endDate?: string
  summary?: {
    settlementCount?: number
    totalAmount?: number
    settledAmount?: number
    diffAmount?: number
    diffCount?: number
    unaccountedCount?: number
  }
  rows?: SettlementReconcileRow[]
}

export const settlementApi = {
  /** 当前计费规则（配置化费率） */
  rule() { return request.get('/dms/settlement/rule') },

  /** 计算某任务的配送费 */
  fee(taskId: number) { return request.get(`/dms/settlement/fee/${taskId}`) },

  /** 周期结算报表（口径：已签收/已完成） */
  report(params: { tenantId?: number; startDate?: string; endDate?: string }) {
    return request.get('/dms/settlement/report', params)
  },

  /** 结算单分页 */
  page(params: Record<string, any>) { return request.get('/dms/settlement/page', params) },

  /** 生成结算单（按周期+结算对象聚合已签收任务） */
  generate(params: { periodStart: string; periodEnd: string; targetType?: number; targetId?: number; remark?: string }) {
    return request.post('/dms/settlement/generate', null, { params })
  },

  /** 结算单详情 */
  detail(id: number) { return request.get(`/dms/settlement/${id}`) },

  /** 结算单明细（费用构成） */
  items(id: number) { return request.get(`/dms/settlement/${id}/items`) },

  /** 确认（锁定金额） */
  confirm(id: number, remark?: string) {
    return request.post(`/dms/settlement/${id}/confirm`, null, { params: { remark } })
  },

  /** 推送 ERP（幂等） */
  pushErp(id: number) { return request.post(`/dms/settlement/${id}/push-erp`) },

  /** 删除（仅草稿） */
  remove(id: number) { return request.delete(`/dms/settlement/${id}`) },

  /** 推送结算数据到 ERP（兼容旧入口） */
  pushToErp(data: Record<string, any>) { return request.post('/dms/settlement/push-erp', data) },

  // ── 计费规则（§3.1 规则模型：按渠道/线路/生效期/计价方式） ──

  /** 计费规则分页 */
  rulePage(params: Record<string, any>) { return request.get('/dms/settlement/rule/page', params) },

  /** 启用中的计费规则（生成结算单预览） */
  ruleEnabled(targetType?: number) {
    return request.get('/dms/settlement/rule/enabled', { params: { targetType } })
  },

  /** 新增计费规则 */
  ruleCreate(data: Partial<DmsSettlementRule>) { return request.post('/dms/settlement/rule', data) },

  /** 修改计费规则 */
  ruleUpdate(id: number, data: Partial<DmsSettlementRule>) {
    return request.put(`/dms/settlement/rule/${id}`, data)
  },

  /** 启停计费规则 */
  ruleStatus(id: number, status?: number) {
    return request.post(`/dms/settlement/rule/${id}/status`, null, { params: { status } })
  },

  /** 删除计费规则 */
  ruleRemove(id: number) { return request.delete(`/dms/settlement/rule/${id}`) },

  // ── 对账 ──

  /** 结算对账（凭证金额 / 应付核销 / 配送费收款，输出差异清单） */
  reconcile(params: { startDate?: string; endDate?: string; targetType?: number; settlementNo?: string; onlyDiff?: boolean }) {
    return request.get('/dms/settlement/reconcile', params)
  },
}
