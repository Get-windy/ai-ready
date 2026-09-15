/**
 * 配送路线单（执行单）司机端 API
 *
 * 与 PC 端《配送 → 配送路线 → 配送路线单》同一后端（`/api/delivery/route/*`）：
 * 司机侧只做「看我的路线 + 逐点到达/签收/失败 + 触发规划与 ETA」，不重复实现业务规则。
 */
import { request } from '@/utils/request'

export interface RoutePoint {
  pointId: number
  pointOrder: number
  orderId?: string
  orderNo?: string
  customerName?: string
  customerPhone?: string
  address: string
  latitude?: string
  longitude?: string
  /** PENDING-待配送 IN_ROUTE-在途中 ARRIVED-已到达 DELIVERED-已送达 FAILED-配送失败 SKIPPED-已跳过 */
  status?: string
  arriveTime?: string
  leaveTime?: string
  signee?: string
  signTime?: string
  failReason?: string
  etaTime?: string
  expedited?: number
  remark?: string
  sourceType?: string
}

export interface DriverRoute {
  id: number
  routeCode: string
  fenceName?: string
  deliveryPersonId?: string
  deliveryPersonName?: string
  vehicleNo?: string
  planDate?: string
  totalPoints?: number
  completedPoints?: number
  failedPoints?: number
  progress?: string
  startPoint?: string
  endPoint?: string
  totalDistance?: number
  totalDuration?: number
  actualDuration?: number
  status: string
  statusText?: string
  startTime?: string
  completeTime?: string
  remark?: string
  points?: RoutePoint[]
}

/** 司机端路线接口（司机身份用 riderId / 员工 userId 均可，按后端存的 delivery_person_id 匹配） */
export const deliveryRouteApi = {
  /** 我当前进行中的路线（无则返回 null） */
  active(deliveryPersonId: string | number) {
    return request.get<DriverRoute>(`/delivery/route/active/${deliveryPersonId}`)
  },

  /** 路线详情（含点位） */
  detail(routeId: number) {
    return request.get<DriverRoute>(`/delivery/route/${routeId}`)
  },

  /** 开始配送 */
  start(routeId: number) {
    return request.post(`/delivery/route/${routeId}/start`)
  },

  /** 完成配送 */
  complete(routeId: number) {
    return request.post(`/delivery/route/${routeId}/complete`)
  },

  /** 逐点签收：ARRIVED 到达 / DELIVERED 送达（必填签收人）/ FAILED 失败（必填原因） */
  signPoint(routeId: number, pointId: number, data: { status: string; signee?: string; failReason?: string; remark?: string }) {
    return request.post(`/delivery/route/${routeId}/point/${pointId}/sign`, data)
  },

  /** 按地图能力重新规划（未配置地图 Key 时后端自动降级，仍可用） */
  planOrder(routeId: number) {
    return request.post(`/delivery/route/${routeId}/plan-order`)
  },

  /** ETA 预估（返回各剩余点位预计到达时间） */
  eta(routeId: number) {
    return request.get<{ remainingPoints: number; degraded: boolean; points: Array<{ pointId: number; pointOrder: number; etaTime: string }> }>(
      `/delivery/route/${routeId}/eta`,
    )
  },
}

export default deliveryRouteApi
