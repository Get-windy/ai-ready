/**
 * DMS 路线规划 / 配送路线 API 模块
 * 后端:
 *   - dms RouteController (/api/dms/route) 高德地图服务（规划/地理编码/围栏）
 *   - erp-delivery-route RouteController (/api/delivery/route) 配送路线单管理
 */
import request from '@/utils/request'

// ── 路线规划（地图服务） ───────────────────────────────
export interface DmsCoordinate {
  lat: number
  lng: number
  address?: string
  stayDuration?: number
}

export interface DmsRoutePlanRequest {
  origin: DmsCoordinate
  waypoints?: DmsCoordinate[]
  destination?: DmsCoordinate
  strategy?: number
  vehicleType?: number
  plateNo?: string
}

export interface DmsGeocodeResult {
  lng: number
  lat: number
  address: string
}

export const routeApi = {
  /** 配送路线规划（多点最优） */
  planRoute(data: DmsRoutePlanRequest) { return request.post('/dms/route/plan', data) },

  /** 地理编码（地址 -> 坐标） */
  geocode(address: string): Promise<DmsGeocodeResult> {
    return request.get('/dms/route/geocode', { params: { address } })
  },

  /** 逆地理编码（坐标 -> 地址） */
  reverseGeocode(params: { lng: number; lat: number }) {
    return request.get('/dms/route/reverse-geocode', { params })
  },

  /** 批量距离计算（POST 请求体） */
  distance(data: { origin: DmsCoordinate; destinations: DmsCoordinate[]; type?: number }) {
    return request.post('/dms/route/distance', data)
  },

  /** 电子围栏校验（圆形围栏，GET 参数） */
  fenceCheck(params: { lat: number; lng: number; centerLat: number; centerLng: number; radiusMeters: number }): Promise<boolean> {
    return request.get('/dms/route/fence-check', { params })
  },
}

// ── 配送路线单（erp-delivery-route） ───────────────────
/** 与后端 DeliveryRoute 实体一致 */
export interface DeliveryRoute {
  id: number
  routeCode: string
  deliveryPersonId: string
  deliveryPersonName: string
  totalPoints: number
  completedPoints: number
  startPoint?: string
  endPoint?: string
  totalDistance?: number
  totalDuration?: number
  /** 状态: PLANNING-规划中 READY-待出发 IN_PROGRESS-配送中 COMPLETED-已完成 CANCELLED-已取消 */
  status: string
  startTime?: string
  completeTime?: string
  remark?: string
  createTime?: string
}

export const deliveryRouteApi = {
  /** 路线列表分页查询（返回 { list, total }） */
  list(params: { page?: number; size?: number; deliveryPersonId?: string; status?: string }) {
    return request.get('/delivery/route/list', params)
  },

  /** 路线详情 */
  getById(routeId: number) { return request.get(`/delivery/route/${routeId}`) },

  /** 开始配送 */
  start(routeId: number) { return request.post(`/delivery/route/${routeId}/start`) },

  /** 完成配送 */
  complete(routeId: number) { return request.post(`/delivery/route/${routeId}/complete`) },

  /** 取消路线 */
  cancel(routeId: number) { return request.post(`/delivery/route/${routeId}/cancel`) },
}
