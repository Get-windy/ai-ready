/**
 * 批次/序列号管理 API 模块
 * 后端使用 BatchApiResponse 格式，需特殊处理
 */
import axios from 'axios'
import { getToken } from '@/utils/tokenRefresher'

// ── 类型定义 ──

export interface BatchNumber {
  id: number
  batchNo: string
  productId: number
  productCode: string
  productName: string
  specification?: string
  unit?: string
  productionDate?: string
  expirationDate?: string
  batchStatus: string
  totalQuantity: number
  availableQuantity: number
  reservedQuantity: number
  sourceType?: string
  sourceRefNo?: string
  warehouseId?: number
  warehouseName?: string
  locationId?: number
  qualityStatus?: string
  qualityInspectorName?: string
  qualityInspectionDate?: string
  createdByName?: string
  createdAt?: string
}

export interface SerialNumber {
  id: number
  serialNo: string
  productId: number
  productCode: string
  productName: string
  specification?: string
  batchId?: number
  batchNo?: string
  snStatus: string
  snStage: string
  manufacturer?: string
  manufacturingDate?: string
  warrantyPeriod?: number
  warrantyStartDate?: string
  warrantyEndDate?: string
  warehouseId?: number
  warehouseName?: string
  customerId?: number
  customerName?: string
  saleOrderNo?: string
  createdAt?: string
}

/** BatchApiResponse 分页信息 */
interface BatchPagination {
  page: number
  size: number
  total: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
}

/** BatchApiResponse 格式 */
interface BatchApiResponse<T> {
  status: number
  code: string
  message: string
  data: T
  pagination?: BatchPagination
  timestamp: string
}

// ── API 基础 URL ──
const BASE_URL = '/api/erp/batch-sn'

/**
 * 创建带有 Token 的请求头
 */
function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/**
 * 通用 GET 请求
 */
async function get<T>(url: string, params?: Record<string, any>): Promise<T> {
  const response = await axios.get(`${BASE_URL}${url}`, {
    headers: authHeaders(),
    params
  })
  const body = response.data as BatchApiResponse<T>
  if (body.code === 'SUCCESS' || body.status === 200) {
    return body.data as T
  }
  throw new Error(body.message || '请求失败')
}

/**
 * 通用 GET 请求（带分页响应）
 */
async function getPage<T>(url: string, params?: Record<string, any>): Promise<{ records: T[]; total: number }> {
  const response = await axios.get(`${BASE_URL}${url}`, {
    headers: authHeaders(),
    params
  })
  const body = response.data as BatchApiResponse<T[]>
  if (body.code === 'SUCCESS' || body.status === 200) {
    return {
      records: body.data as T[],
      total: body.pagination?.total ?? (body.data as T[])?.length ?? 0
    }
  }
  throw new Error(body.message || '请求失败')
}

/**
 * 通用 POST 请求
 */
async function post<T>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  const response = await axios.post(`${BASE_URL}${url}`, data, {
    headers: authHeaders(),
    params
  })
  const body = response.data as BatchApiResponse<T>
  if (body.code === 'SUCCESS' || body.code === 'CREATED' || body.status === 200 || body.status === 201) {
    return body.data as T
  }
  throw new Error(body.message || '请求失败')
}

/**
 * 通用 PUT 请求
 */
async function put<T>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  const response = await axios.put(`${BASE_URL}${url}`, data, {
    headers: authHeaders(),
    params
  })
  const body = response.data as BatchApiResponse<T>
  if (body.code === 'SUCCESS' || body.status === 200) {
    return body.data as T
  }
  throw new Error(body.message || '请求失败')
}

/**
 * 通用 DELETE 请求（暂无用到，为完整保留）
 */
async function del<T = void>(url: string, params?: Record<string, any>): Promise<T> {
  const response = await axios.delete(`${BASE_URL}${url}`, {
    headers: authHeaders(),
    params
  })
  const body = response.data as BatchApiResponse<T>
  if (body.code === 'SUCCESS' || body.status === 200) {
    return body.data as T
  }
  throw new Error(body.message || '请求失败')
}

// ── 公开 API 对象 ──

export const batchApi = {
  /** 分页查询批次 */
  page(params: {
    pageNum?: number
    pageSize?: number
    batchNo?: string
    productCode?: string
    status?: string
    sourceType?: string
    warehouseId?: number
  }): Promise<{ records: BatchNumber[]; total: number }> {
    return getPage<BatchNumber>('/batches/page', {
      page: params.pageNum || 1,
      size: params.pageSize || 20,
      batchNo: params.batchNo,
      productCode: params.productCode,
      status: params.status,
      sourceType: params.sourceType,
      warehouseId: params.warehouseId
    })
  },

  /** 查询批次详情 */
  getById(id: number): Promise<BatchNumber> {
    return get<BatchNumber>(`/batches/${id}`)
  },

  /** 创建批次 */
  create(data: Partial<BatchNumber>): Promise<BatchNumber> {
    return post<BatchNumber>('/batches', data)
  },

  /** 更新批次 */
  update(id: number, data: Partial<BatchNumber>): Promise<BatchNumber> {
    return put<BatchNumber>(`/batches/${id}`, data)
  },

  /** 批次入库 */
  inbound(
    data: Partial<BatchNumber>,
    warehouseId: number,
    warehouseName: string,
    locationId?: number
  ): Promise<BatchNumber> {
    return post<BatchNumber>('/batches/inbound', data, { warehouseId, warehouseName, locationId })
  },

  /** 批次出库 */
  outbound(
    batchId: number,
    quantity: number,
    warehouseId: number,
    locationId?: number
  ): Promise<BatchNumber> {
    return post<BatchNumber>('/batches/outbound', null, { batchId, quantity, warehouseId, locationId })
  },

  /** 批次转移 */
  transfer(data: {
    batchId: number
    fromWarehouseId: number
    toWarehouseId: number
    fromLocationId?: number
    toLocationId?: number
    quantity: number
    remark?: string
  }): Promise<BatchNumber> {
    return post<BatchNumber>('/batches/transfer', data)
  },

  /** 批次盘点 */
  inventory(data: {
    batchId: number
    physicalQuantity: number
    adjustmentQuantity: number
    adjustmentReason?: string
    operatorId?: string
    operatorName?: string
  }): Promise<BatchNumber> {
    return post<BatchNumber>('/batches/inventory', data)
  },

  /** 批次质检 */
  qualityInspection(
    id: number,
    status: string,
    inspectorId: string,
    inspectorName: string
  ): Promise<BatchNumber> {
    return post<BatchNumber>(`/batches/${id}/quality-inspection`, null, {
      status,
      inspectorId,
      inspectorName
    })
  },

  /** 更新批次状态 */
  updateStatus(batchIds: number[], newStatus: string): Promise<number> {
    return axios.patch(`${BASE_URL}/batches/status`, batchIds, {
      headers: authHeaders(),
      params: { newStatus }
    }).then(res => {
      const body = res.data as BatchApiResponse<number>
      if (body.code === 'SUCCESS' || body.status === 200) return body.data as number
      throw new Error(body.message || '请求失败')
    })
  },

  /** 临期批次预警 */
  getExpiringBatches(warningDays?: number): Promise<BatchNumber[]> {
    return get<BatchNumber[]>('/batches/expiring-warning', { warningDays: warningDays || 30 })
  },

  /** 批次库存汇总 */
  getStockSummary(): Promise<BatchNumber[]> {
    return get<BatchNumber[]>('/batches/stock-summary')
  },

  /** 验证批次号唯一性 */
  validateBatchNo(batchNo: string): Promise<boolean> {
    return get<boolean>('/batches/validate-batch-no', { batchNo })
  },

  /** 高级搜索 */
  advancedSearch(params: Record<string, any>): Promise<BatchNumber[]> {
    return post<BatchNumber[]>('/batches/search', params)
  },

  /** 导出 */
  export(params: Record<string, any>): Promise<BatchNumber[]> {
    return get<BatchNumber[]>('/batches/export', params)
  }
}

export const serialApi = {
  /** 分页查询序列号 */
  page(params: {
    pageNum?: number
    pageSize?: number
    serialNo?: string
    productCode?: string
    status?: string
    batchNo?: string
  }): Promise<SerialNumber[]> {
    return get<SerialNumber[]>('/serials', {
      serialNo: params.serialNo,
      productCode: params.productCode,
      status: params.status,
      batchNo: params.batchNo,
      page: params.pageNum || 1,
      size: params.pageSize || 20
    })
  },

  /** 查询序列号详情 */
  getById(id: number): Promise<SerialNumber> {
    return get<SerialNumber>(`/serials/${id}`)
  },

  /** 创建序列号 */
  create(data: Partial<SerialNumber>): Promise<SerialNumber> {
    return post<SerialNumber>('/serials', data)
  },

  /** 更新序列号 */
  update(id: number, data: Partial<SerialNumber>): Promise<SerialNumber> {
    return put<SerialNumber>(`/serials/${id}`, data)
  },

  /** 序列号入库 */
  inbound(
    data: Partial<SerialNumber>,
    warehouseId: number,
    warehouseName: string,
    locationId: number
  ): Promise<SerialNumber> {
    return post<SerialNumber>('/serials/inbound', data, { warehouseId, warehouseName, locationId })
  },

  /** 序列号出库 */
  outbound(
    serialId: number,
    saleOrderId: number,
    saleOrderNo: string,
    warehouseId: number,
    locationId: number
  ): Promise<SerialNumber> {
    return post<SerialNumber>('/serials/outbound', null, {
      serialId, saleOrderId, saleOrderNo, warehouseId, locationId
    })
  },

  /** 更新序列号状态 */
  updateStatus(id: number, status: string, stage: string): Promise<SerialNumber> {
    return axios.patch(`${BASE_URL}/serials/${id}/status`, null, {
      headers: authHeaders(),
      params: { status, stage }
    }).then(res => {
      const body = res.data as BatchApiResponse<SerialNumber>
      if (body.code === 'SUCCESS' || body.status === 200) return body.data as SerialNumber
      throw new Error(body.message || '请求失败')
    })
  },

  /** 质保预警 */
  getWarrantyExpiring(warningDays?: number): Promise<SerialNumber[]> {
    return get<SerialNumber[]>('/serials/warranty-warning', { warningDays: warningDays || 30 })
  },

  /** 完整流转历史 */
  getFullHistory(id: number): Promise<SerialNumber[]> {
    return get<SerialNumber[]>(`/serials/${id}/full-history`)
  },

  /** 验证序列号 */
  validateSerialNo(serialNo: string): Promise<boolean> {
    return get<boolean>('/serials/validate-serial-no', { serialNo })
  }
}
