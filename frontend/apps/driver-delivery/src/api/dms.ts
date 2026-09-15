import request from '@/utils/request'

/**
 * 签收类型（《签收管理开发文档》§3.6.8 双端同一套枚举）——与后端 `SignTypeEnum` 对齐，禁止两端各自定义。
 */
export const SIGN_TYPE = {
  /** 正常签收 */
  NORMAL: 1,
  /** 部分签收（须填实际签收数量） */
  PARTIAL: 2,
  /** 拒收（须填拒收原因 + 照片） */
  REJECT: 3,
} as const

export const SIGN_TYPE_OPTIONS = [
  { text: '正常签收', value: SIGN_TYPE.NORMAL },
  { text: '部分签收', value: SIGN_TYPE.PARTIAL },
  { text: '拒收', value: SIGN_TYPE.REJECT },
]

/** 司机端提交签收的载荷（后端 `SignSubmitDTO`，JSON body） */
export interface DriverSignPayload {
  taskId: number | string
  /** 1-正常签收 2-部分签收 3-拒收 */
  signType: number
  /** 照片 URL 列表（JSON 数组串，如 `["/api/file/view/xx.jpg"]`） */
  photoUrls?: string
  /** 手写签名图片 URL */
  signatureUrl?: string
  /** 签收定位（司机端 GPS） */
  signLat?: number
  signLng?: number
  /** 实际签收数量（部分签收必填） */
  actualQuantity?: number
  /** 签收备注（拒收时即拒收原因，必填） */
  remark?: string
}

/** 收款提交载荷（后端 `PaymentController#confirmPayment`，@RequestParam → 走 query） */
export interface DriverPaymentPayload {
  taskId: number | string
  amount: number
  /** 1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他 */
  payChannel?: number
  /** 1-代收货款 2-配送费（不传由后端按任务代收货款推断） */
  paymentType?: number
  externalOrderNo?: string
}

/**
 * 司机端 ↔ DMS 后端（core-api `/api/dms/**`）
 *
 * ⚠️ 2026-09-13 契约修复（原实现整层不可用）：
 *  1. `utils/request` 的 baseURL 已是 `/api`，原写法又带 `/api/dms/...` → 实际请求 `/api/api/dms/...` 恒 404；
 *  2. payload 用 snake_case（`vehicle_id`/`bind_reason`/`inspection_id`），后端 DTO 是 camelCase
 *     → Jackson 忽略后触发必填校验 400；
 *  3. 交车路径少了 `binding/` 一层（`/verification/{id}/handover`）→ 404；
 *  4. 用「登录用户ID」当 riderId 查询活跃绑定 → 永远查不到。
 */
export const dmsApi = {
  // ── 当前登录司机 ──
  /** 当前登录人对应的配送员档案（服务端按 userId 反查，前端不传 riderId） */
  getMyRider: () => request.get('/dms/verification/me/rider'),
  updateLocation: (data: { riderId: number | string; lat: number; lng: number }) =>
    request.post('/dms/rider/location', data),
  updateStatus: (id: number | string, status: number) => request.post(`/dms/rider/${id}/status`, { status }),

  // ── 车辆 / 人车绑定 ──
  /** 车辆选择器数据源（排除已报废） */
  getVehicleOptions: () => request.get('/dms/vehicle/options'),
  /** 配送员当前活跃绑定 */
  getActiveBinding: (riderId: number | string) =>
    request.get(`/dms/verification/binding/active/rider/${riderId}`),

  // ── 巡检（出车前 / 收车后） ──
  /** 创建巡检记录（inspectionType：1-出车前 2-收车后 3-随机抽检 4-定期检查） */
  createInspection: (data: any) => request.post('/dms/verification/inspection', data),
  /** 补能登记（加油/充电/加气/换电；车/人二选一，与 PC 同一套契约） */
  createEnergyLog: (data: {
    vehicleId?: number | string | null
    riderId?: number | string | null
    energyType: number
    amountYuan: number
    quantity?: number
    odometer?: number
    cardNo?: string
    station?: string
    occurredAt?: string
    voucherUrl?: string
    remark?: string
  }) => request.post('/dms/vehicle/energy', data),

  /** 巡检记录详情（取回后端判定结果） */
  getInspection: (id: number | string) => request.get(`/dms/verification/inspection/${id}`),

  /** 出车登记：出车前检查通过后绑定（inspectionId 必传，后端强制门控） */
  bindVehicle: (data: {
    riderId: number | string
    vehicleId: number | string
    inspectionId: number | string
    bindReason?: string
  }) => request.post('/dms/verification/bind', data),

  /** 交车：收车登记 + 收车后检查（inspection 必传，后端强制门控） */
  handover: (bindingId: number | string, data: {
    handoverMileage?: number
    handoverLocation?: string
    handoverLat?: number
    handoverLng?: number
    remark?: string
    inspection?: Record<string, any>
  }) => request.post(`/dms/verification/binding/${bindingId}/handover`, data),

  // ── 任务 ──
  getMyTasks: (riderId: number | string, params: any) =>
    request.get('/dms/task/page', { params: { ...params, riderId } }),
  /**
   * 任务状态流转（后端 `PUT /dms/task/{id}/status` 收 JSON body `{fromStatus,toStatus}`，
   * 并按 `TaskStatusEnum` 校验 1→2→3→4 的合法迁移；原文案传 query 参数会被拒）
   */
  updateTaskStatus: (id: number | string, fromStatus: number, toStatus: number) =>
    request.put(`/dms/task/${id}/status`, { fromStatus, toStatus }),

  /** 任务详情（含明细与来源单据） */
  getTaskDetail: (id: number | string) => request.get(`/dms/task/${id}`),

  // ── 签收 / 收款 / 轨迹 ──
  /** 提交签收（服务端算偏差、幂等、联动任务状态） */
  submitSign: (data: DriverSignPayload) => request.post('/dms/sign/submit', data),
  /** 按任务查最新一条签收 */
  getTaskSign: (taskId: number | string) => request.get(`/dms/sign/${taskId}`),
  /** 收款字典（支付方式 / 收款类型） */
  getPaymentDict: () => request.get('/dms/payment/dict'),
  /** 线下收款确认（现金/POS/银行转账） */
  confirmPayment: (data: DriverPaymentPayload) =>
    request.post('/dms/payment/confirm', null, { params: data as any }),
  reportLocation: (data: { taskId: number | string; riderId: number | string; lat: number; lng: number; timestamp?: string }) =>
    request.post('/dms/tracking/report', data),
}

/**
 * 上传文件到通用文件服务，返回可直接引用的 `url`（`/api/file/view/...`）。
 * 供签收照片 / 手写签名使用。
 */
export async function uploadFile(file: File | Blob, fileName?: string): Promise<string> {
  const fd = new FormData()
  fd.append('file', file, fileName || 'file.png')
  const res: any = await request.post('/file/upload', fd)
  const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
  if (!url || typeof url !== 'string') {
    throw new Error('文件上传失败')
  }
  return url
}

/** dataURL(base64) → Blob，供签名图片上传 */
export function dataUrlToBlob(dataUrl: string): Blob {
  const [head, body] = dataUrl.split(',')
  const mime = /:(.*?);/.exec(head)?.[1] || 'image/png'
  const bin = atob(body)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i)
  }
  return new Blob([bytes], { type: mime })
}
