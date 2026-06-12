/**
 * DMS 路线规划 API 模块
 */
import request from '@/utils/request'

// ── 路线规划 ──────────────────────────────────────────
export interface DmsRoutePlanRequest {
  originLng: number
  originLat: number
  destLng: number
  destLat: number
  waypoints?: string
}

export interface DmsRouteResult {
  distance: number
  duration: number
  polyline: string
  steps: Array<{
    instruction: string
    distance: number
    duration: number
    polyline: string
  }>
}

export interface DmsGeocodeResult {
  lng: number
  lat: number
  address: string
}

export interface DmsFenceCheckRequest {
  lng: number
  lat: number
  fenceId?: number
  riderId?: number
}

export const routeApi = {
  /** 路线规划 */
  planRoute(data: DmsRoutePlanRequest) { return request.post('/api/dms/route/plan', data) },

  /** 地理编码（地址 -> 坐标） */
  geocode(params: { address: string; city?: string }) { return request.get('/api/dms/route/geocode', { params }) },

  /** 逆地理编码（坐标 -> 地址） */
  reverseGeocode(params: { lng: number; lat: number }) { return request.get('/api/dms/route/reverse-geocode', { params }) },

  /** 计算距离 */
  distance(params: { originLng: number; originLat: number; destLng: number; destLat: number }) { return request.get('/api/dms/route/distance', { params }) },

  /** 电子围栏检测 */
  fenceCheck(data: DmsFenceCheckRequest) { return request.post('/api/dms/route/fence/check', data) },
}
