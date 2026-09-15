/**
 * DMS 路线规划 / 电子围栏 / 配送路线单 API 模块
 * 后端:
 *   - dms RouteController    (/api/dms/route)        地理能力：规划/重规划/编码/逆编码/距离/围栏校验/坐标转换/配置
 *   - dms GeoFenceController (/api/dms/route/fence)  电子围栏档案 CRUD
 *   - erp RouteController    (/api/delivery/route)   配送路线单（执行单，规划结果一键生成）
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
  /** 途经点（后端同时兼容 destinations 字段名） */
  waypoints?: DmsCoordinate[]
  destinations?: DmsCoordinate[]
  destination?: DmsCoordinate
  strategy?: number
  /** DRIVING(默认) / CYCLING / WALKING */
  direction?: string
  /** 是否按最近邻重排（默认 true） */
  optimizeOrder?: boolean
  /** 入参坐标体系 WGS84 / GCJ02(默认) / BD09 */
  coordSystem?: string
  /** 重规划时的已访问点数（点位序号续编） */
  visitedCount?: number
  vehicleType?: number
  plateNo?: string
}

export interface DmsRouteStop {
  index: number
  /** start / waypoint / destination */
  type: string
  address?: string
  lat: number
  lng: number
  distanceFromPrev?: number
  durationFromPrev?: number
  sourceIndex?: number
}

export interface DmsRoutePlanResult {
  success: boolean
  message?: string
  /** amap / tencent / baidu / local */
  provider?: string
  /** true = 未配置地图 Key，按直线距离降级估算 */
  degraded?: boolean
  travelMode?: string
  coordSystem?: string
  totalDistance?: number
  totalDuration?: number
  totalToll?: number
  routePoints?: Array<{ lat: number; lng: number; address?: string }>
  steps?: Array<{ fromIndex: number; toIndex: number; instruction?: string; distance?: number; duration?: number; roadName?: string }>
  optimizedOrder?: number[]
  stops?: DmsRouteStop[]
}

export interface DmsGeocodeCandidate {
  formattedAddress?: string
  lat: number
  lng: number
  province?: string
  city?: string
  district?: string
  adcode?: string
  level?: string
}

export interface DmsGeocodeResult {
  success: boolean
  message?: string
  provider?: string
  degraded?: boolean
  cached?: boolean
  lat?: number
  lng?: number
  formattedAddress?: string
  province?: string
  city?: string
  district?: string
  candidates?: DmsGeocodeCandidate[]
}

export interface DmsReverseGeocodeResult {
  success: boolean
  message?: string
  provider?: string
  degraded?: boolean
  lat?: number
  lng?: number
  formattedAddress?: string
  province?: string
  city?: string
  district?: string
  street?: string
  streetNumber?: string
}

export interface DmsGeoFence {
  /** 雪花/自增 ID（后端 Long 序列化为字符串以避免精度丢失） */
  id?: number | string
  fenceCode?: string
  fenceName: string
  /** CIRCLE / POLYGON */
  fenceType: string
  centerLat?: number | null
  centerLng?: number | null
  radiusMeters?: number | null
  polygonPoints?: string | null
  /** ROUTE / CHANNEL / WAREHOUSE / OTHER */
  bizType?: string
  bizId?: string
  bizName?: string
  /** ENABLED / DISABLED */
  status?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DmsFenceCheckResult {
  success: boolean
  message?: string
  inside?: boolean
  distanceMeters?: number
  fenceId?: number | string
  fenceName?: string
  fenceType?: string
  results?: Array<{ lat: number; lng: number; address?: string; inside: boolean; distanceMeters: number }>
}

export interface DmsRouteProviderStatus {
  provider: string
  configured: boolean
  active: boolean
  /** ENV-环境变量 SPRING-Spring配置 CONFIG-配置中心 NONE-未配置 */
  source: string
  envVarName: string
  configKey: string
}

export interface DmsRouteConfig {
  provider: string
  defaultProvider: string
  configured: boolean
  degraded: boolean
  /** Key 来源：ENV / SPRING / CONFIG / NONE */
  keySource: string
  /** 该服务商对应的环境变量名，如 AMAP_API_KEY */
  envVarName: string
  /** 该服务商在《配送参数》中的配置键，如 map.amap.api-key */
  configKey: string
  /** 按当前来源生成的配置指引（下一步去哪配） */
  hint: string
  providers: DmsRouteProviderStatus[]
  coordSystem: string
  travelModes: string[]
  geocodeCacheSize: number
}

export interface DmsRouteVerifyResult {
  ok: boolean
  provider: string
  keySource: string
  envVarName: string
  configKey: string
  degraded: boolean
  latencyMs?: number
  sampleAddress: string
  lat?: number
  lng?: number
  formattedAddress?: string
  message: string
}

export const routeApi = {
  /** 路线规划（起点 + 多途经点，最近邻排序） */
  plan(data: DmsRoutePlanRequest): Promise<DmsRoutePlanResult> {
    return request.post('/dms/route/plan', data)
  },

  /** 重新规划（当前位置 → 剩余点位重排序） */
  reoptimize(data: DmsRoutePlanRequest): Promise<DmsRoutePlanResult> {
    return request.post('/dms/route/reoptimize', data)
  },

  /** 地理编码（地址 -> 坐标，返回候选列表） */
  geocode(address: string, city?: string): Promise<DmsGeocodeResult> {
    return request.get('/dms/route/geocode', { params: { address, city } })
  },

  /** 逆地理编码（坐标 -> 结构化地址） */
  reverseGeocode(params: { lat: number; lng: number; from?: string }): Promise<DmsReverseGeocodeResult> {
    return request.get('/dms/route/reverse-geocode', { params })
  },

  /** 批量距离计算（POST 请求体） */
  distance(data: { origin: DmsCoordinate; destinations: DmsCoordinate[]; type?: number }) {
    return request.post('/dms/route/distance', data)
  },

  /** 电子围栏校验（围栏档案 / 内联圆形 / 内联多边形；支持批量点位） */
  fenceCheck(data: {
    lat?: number
    lng?: number
    points?: Array<{ lat: number; lng: number; address?: string }>
    fenceId?: number
    fenceType?: string
    centerLat?: number
    centerLng?: number
    radiusMeters?: number
    polygon?: string
  }): Promise<DmsFenceCheckResult> {
    return request.post('/dms/route/fence-check', data)
  },

  /** 坐标体系转换（WGS84 / GCJ02 / BD09 互转） */
  convert(data: { lat?: number; lng?: number; points?: Array<{ lat: number; lng: number }>; from?: string; to?: string }) {
    return request.post('/dms/route/convert', data)
  },

  /** 地理能力配置状态（服务商 / 是否降级 / 坐标体系） */
  config(): Promise<DmsRouteConfig> {
    return request.get('/dms/route/config')
  },

  /** 连通性自检：用当前 Key 真实调用一次地理编码（绕过缓存） */
  verify(): Promise<DmsRouteVerifyResult> {
    return request.post('/dms/route/verify')
  },
}

// ── 电子围栏档案 ──────────────────────────────────────
export const geoFenceApi = {
  page(params: { pageNum?: number; pageSize?: number; keyword?: string; fenceType?: string; status?: string; bizType?: string }) {
    return request.get('/dms/route/fence/page', { params })
  },

  detail(id: number | string) { return request.get(`/dms/route/fence/${id}`) },

  nextCode(): Promise<string> { return request.get('/dms/route/fence/next-code') },

  /** 启用围栏下拉（供线路/渠道绑定） */
  options(): Promise<DmsGeoFence[]> { return request.get('/dms/route/fence/options') },

  create(data: DmsGeoFence) { return request.post('/dms/route/fence', data) },

  update(id: number | string, data: DmsGeoFence) { return request.put(`/dms/route/fence/${id}`, data) },

  remove(id: number | string) { return request.delete(`/dms/route/fence/${id}`) },

  updateStatus(id: number | string, status: string) {
    return request.put(`/dms/route/fence/${id}/status`, null, { params: { status } })
  },
}

// ── 配送路线单（erp-delivery-route，执行单） ───────────
/**
 * 配送路线单 = **执行单**（谁跑、跑到哪、开始/完成/取消）。
 * ⚠️ 与线路档案主数据（资料 → 配送管理 → 线路，/api/erp/md/route）严格区分，二者不重复。
 */
export interface DeliveryRoutePoint {
  pointId?: number
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
  remark?: string
}

export interface DeliveryRoute {
  id: number
  routeCode: string
  routeId?: number
  routeName?: string
  routeType?: string
  routeTypeText?: string
  deliveryPersonId: string
  deliveryPersonName: string
  vehicleId?: number
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
  /** 状态: PLANNING-规划中 READY-待出发 IN_PROGRESS-配送中 COMPLETED-已完成 CANCELLED-已取消 */
  status: string
  statusText?: string
  startTime?: string
  completeTime?: string
  cancelTime?: string
  cancelReason?: string
  remark?: string
  createByName?: string
  createTime?: string
  updateTime?: string
  points?: DeliveryRoutePoint[]
}

export interface DeliveryRouteQuery {
  keyword?: string
  routeId?: number
  deliveryPersonId?: string
  vehicleId?: number
  /** 状态多选，逗号分隔 */
  status?: string
  planDateStart?: string
  planDateEnd?: string
  createTimeStart?: string
  createTimeEnd?: string
  showCancelled?: number
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
}

export const deliveryRouteApi = {
  /** 多条件分页（返回 Page：{ records, total }） */
  page(params: DeliveryRouteQuery) { return request.get('/delivery/route/page', { params }) },

  /** 生成下一个路线编号（PSXL-YYYYMMDD-序号） */
  nextNo() { return request.get('/delivery/route/next-no') },

  /** 路线详情（含点位明细） */
  detail(routeId: number) { return request.get(`/delivery/route/${routeId}`) },

  /** 手工建单 */
  create(data: Partial<DeliveryRoute>) { return request.post('/delivery/route', data) },

  /** 修改（仅规划中/待出发；点位整体替换） */
  update(routeId: number, data: Partial<DeliveryRoute>) { return request.put(`/delivery/route/${routeId}`, data) },

  /** 开始配送 */
  start(routeId: number) { return request.post(`/delivery/route/${routeId}/start`) },

  /** 完成配送 */
  complete(routeId: number, reason?: string) { return request.post(`/delivery/route/${routeId}/complete`, { reason }) },

  /** 取消路线 */
  cancel(routeId: number, reason?: string) { return request.post(`/delivery/route/${routeId}/cancel`, { reason }) },

  /** 批量状态流转（action: start / complete / cancel） */
  batchStatus(data: { ids: Array<number | string>; action: string; reason?: string }) {
    return request.post('/delivery/route/batch-status', data)
  },

  /** 点位签收（逐点独立） */
  signPoint(routeId: number, pointId: number, data: { status: string; signee?: string; failReason?: string; remark?: string }) {
    return request.post(`/delivery/route/${routeId}/point/${pointId}/sign`, data)
  },

  /** 导出真实 xlsx */
  export(params: DeliveryRouteQuery): Promise<Blob> {
    return request.get('/delivery/route/export', { params, responseType: 'blob' })
  },

  // ── 配送需求归集（围栏自动 + 手动添加） ──
  /** 可入线的配送需求（销售出库单/销售订单） */
  demands(params: { source?: string; keyword?: string; limit?: number }) {
    return request.get('/delivery/route/demands', { params })
  },

  /** 围栏自动归集预览（干跑，不落库） */
  autoCollectPreview(data: RouteAutoCollectQuery) {
    return request.post('/delivery/route/auto-collect/preview', data)
  },

  /** 围栏自动归集（命中围栏的配送需求入线） */
  autoCollect(data: RouteAutoCollectQuery) {
    return request.post('/delivery/route/auto-collect', data)
  },

  /** 手动添加配送点位（不受围栏限制） */
  addPoints(routeId: number, data: { replan?: boolean; items: Array<{ sourceType: string; sourceId: number }> }) {
    return request.post(`/delivery/route/${routeId}/add-points`, data)
  },

  /** 客户配送坐标清单 */
  customerGeo(params?: { limit?: number }) {
    return request.get('/delivery/route/customer-geo', { params })
  },

  /** 补录客户配送坐标 */
  saveCustomerGeo(data: { customerId: number; latitude: number | string; longitude: number | string }) {
    return request.put('/delivery/route/customer-geo', data)
  },

  // ── 路线规划 / 催单 / ETA ──
  /** 按地图能力规划路线顺序（未配地图 Key 自动降级） */
  planOrder(routeId: number) {
    return request.post(`/delivery/route/${routeId}/plan-order`)
  },

  /** 催单：把指定点位移到目标序号 */
  expeditePoint(routeId: number, pointId: number, data: { targetSeq?: number; replanRest?: boolean; reason?: string }) {
    return request.post(`/delivery/route/${routeId}/point/${pointId}/expedite`, data)
  },

  /** ETA 预估 */
  eta(routeId: number) {
    return request.get(`/delivery/route/${routeId}/eta`)
  },

  /** 生成 ETA 客户通知（落库待发送，通道未接入） */
  notifyEta(routeId: number, data: { pointIds?: number[]; channel?: string }) {
    return request.post(`/delivery/route/${routeId}/notify-eta`, data)
  },

  // ── ETA 通知台账（通道未接入下的运营闭环） ──
  /** 通知台账分页 */
  etaNotifyPage(params: { routeId?: number; status?: string; keyword?: string; pageNum?: number; pageSize?: number }) {
    return request.get('/delivery/route/eta-notify/page', { params })
  },

  /** 通知状态回写（SENT / FAILED / CANCELLED） */
  updateEtaNotifyStatus(data: { ids: Array<number | string>; status: string; errorMsg?: string }) {
    return request.post('/delivery/route/eta-notify/status', data)
  },

  /** 尝试发送（通道未接入时明确返回未配置，不篡改状态） */
  sendEtaNotify(data: { ids: Array<number | string> }) {
    return request.post('/delivery/route/eta-notify/send', data)
  },

  /** 补投递到消息底座（幂等：把待发送且未投递的通知投递出去） */
  dispatchEtaNotify(data: { routeId?: number }) {
    return request.post('/delivery/route/eta-notify/dispatch', data)
  },
}

/** 围栏归集入参 */
export interface RouteAutoCollectQuery {
  /** 指定路线单（可空：扫描所有开启自动归集的待出发路线单） */
  routeId?: number
  /** BOTH-出库单+订单（默认）/ OUT-仅出库单 / SO-仅订单 */
  source?: string
  keyword?: string
  limit?: number
}

/** 配送需求（可入线的来源单据） */
export interface DeliveryDemand {
  sourceType: string
  sourceId: number
  billNo: string
  customerId?: number
  customerName?: string
  address?: string
  receiverName?: string
  receiverPhone?: string
  latitude?: number | string
  longitude?: number | string
  billTime?: string
  insideFence?: boolean
  distanceMeters?: number
}

/** 围栏归集结果 */
export interface RouteAutoCollectResult {
  scanned: number
  matched: number
  added: number
  alreadyOnRoute: number
  missingCoordinate: number
  outsideFence: number
  dryRun: boolean
  messages: string[]
  items: Array<{
    routeId?: number
    routeCode?: string
    fenceName?: string
    sourceType: string
    sourceId: number
    billNo: string
    customerName?: string
    address?: string
    /** ADDED / PREVIEW / ALREADY_ON_ROUTE / NO_COORD / OUTSIDE */
    result: string
    reason?: string
  }>
}
