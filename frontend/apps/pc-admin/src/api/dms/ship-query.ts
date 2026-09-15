/**
 * 发货查询（配发收 → 发货业务 → 发货查询，菜单 70156）专用接口
 *
 * 页面的单据数据来自销售出库单（`outboundApi`）；本文件只承载**页面固定项**
 * 「配送状态 / 配送线路」的跨域解析：这两项是配送任务（dms_task）的执行属性，
 * 由 DMS 侧按来源单据号反查出库单号，再交给销售出库单分页接口过滤。
 */
import request from '@/utils/request'

export interface DeliveryOutboundFilter {
  /** 命中的来源单据号（销售出库单号） */
  sourceBillNos: string[]
  /** 命中条数 */
  matched: number
  /** 是否被上限截断（true=需收窄条件） */
  truncated: boolean
}

export const shipQueryApi = {
  /** 按配送状态/配送线路反查命中的出库单号 */
  outboundFilter(params: { deliveryStatus?: string; routeId?: number | string; limit?: number }) {
    return request.get('/dms/task/outbound-filter', { params }) as Promise<DeliveryOutboundFilter>
  },
}
