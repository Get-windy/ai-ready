/**
 * 仓库规划 API（资料 → 仓库管理 → 仓库规划）
 *
 * - 仓库主数据：erp_warehouse（全系统唯一口径，销售/采购/仓储/财务统一引用）
 * - 仓库分类树：erp_warehouse_category
 * - 货位：wms_location（沿用 WMS 货位表，不另建）
 */
import request from '@/utils/request'

// ═══ 仓库主数据 ═══════════════════════════════════════════
export interface ErpWarehouse {
  id: string
  warehouseCode: string
  warehouseName: string
  contactPerson?: string
  contactPhone?: string
  address?: string
  easyCode?: string
  zipCode?: string
  parentId?: string
  categoryId?: string
  sortOrder?: number
  /** 1-启用 0-停用 */
  status?: number
  remark?: string
}

export const warehousePlanApi = {
  /** 分页查询（keyword / categoryId / showDisabled / showHierarchy） */
  page(params: Record<string, any>): Promise<any> {
    return request.get('/erp/warehouse/page', { params })
  },
  listAll(): Promise<ErpWarehouse[]> {
    return request.get('/erp/warehouse/list')
  },
  getById(id: string | number): Promise<ErpWarehouse> {
    return request.get(`/erp/warehouse/${id}`)
  },
  save(data: Partial<ErpWarehouse>): Promise<ErpWarehouse> {
    return request.post('/erp/warehouse/save', data)
  },
  update(data: Partial<ErpWarehouse>): Promise<boolean> {
    return request.post('/erp/warehouse/update', data)
  },
  /** 启用/停用 */
  updateStatus(id: string | number, status: number): Promise<boolean> {
    return request.post(`/erp/warehouse/${id}/status`, null, { params: { status } })
  },
  remove(id: string | number): Promise<boolean> {
    return request.delete(`/erp/warehouse/${id}`)
  },
  /** 下一个仓库编号（对标：新增时默认带出 ck005） */
  nextCode(): Promise<string> {
    return request.get('/erp/warehouse/next-code')
  },
  /** 导出真实 xlsx（与列表同口径） */
  export(params: Record<string, any>): Promise<Blob> {
    return request.get('/erp/warehouse/export', { params, responseType: 'blob' })
  },
}

// ═══ 仓库分类树 ═══════════════════════════════════════════
export interface WarehouseCategory {
  id: string
  categoryCode?: string
  categoryName: string
  parentId?: string
  categoryLevel?: number
  sortOrder?: number
  status?: number
  remark?: string
  warehouseCount?: number
  children?: WarehouseCategory[]
}

export const warehouseCategoryApi = {
  tree(): Promise<WarehouseCategory[]> {
    return request.get('/erp/warehouse-category/tree')
  },
  getById(id: string | number): Promise<WarehouseCategory> {
    return request.get(`/erp/warehouse-category/${id}`)
  },
  create(data: Partial<WarehouseCategory>): Promise<WarehouseCategory> {
    return request.post('/erp/warehouse-category', data)
  },
  update(id: string | number, data: Partial<WarehouseCategory>): Promise<boolean> {
    return request.put(`/erp/warehouse-category/${id}`, data)
  },
  remove(id: string | number): Promise<boolean> {
    return request.delete(`/erp/warehouse-category/${id}`)
  },
}

// ═══ 货位（wms_location） ═════════════════════════════════
export interface WarehouseLocation {
  id: string
  warehouseId?: string
  warehouseName?: string
  locationCode?: string
  locationName?: string
  locationType?: number
  locationLevel?: number
  status?: number
  isEnabled?: number
  isBuiltin?: number
  remark?: string
}

/** 货位批量生成参数（对标：编号 = 通道+货架-层+列，如 A1-101） */
export interface LocationGeneratePayload {
  warehouseId: string | number
  warehouseName?: string
  channelNo?: string
  channelCount?: number
  shelfNo?: string
  shelfCount?: number
  layerCount?: number
  columnNo?: string
  columnCount?: number
  remark?: string
}

export const warehouseLocationApi = {
  page(params: Record<string, any>): Promise<any> {
    return request.get('/wms/location/plan-page', { params })
  },
  generate(data: LocationGeneratePayload): Promise<number> {
    return request.post('/wms/location/generate', data)
  },
  update(data: Partial<WarehouseLocation>): Promise<boolean> {
    return request.post('/wms/location/update', data)
  },
  getById(id: string | number): Promise<WarehouseLocation> {
    return request.get(`/wms/location/${id}`)
  },
  setEnabled(id: string | number, isEnabled: number): Promise<boolean> {
    return request.post(`/wms/location/${id}/enabled`, null, { params: { isEnabled } })
  },
  remove(id: string | number): Promise<boolean> {
    return request.delete(`/wms/location/${id}`)
  },
  batchDelete(ids: Array<string | number>): Promise<number> {
    return request.post('/wms/location/batch-delete', ids)
  },
  export(params: Record<string, any>): Promise<Blob> {
    return request.get('/wms/location/export', { params, responseType: 'blob' })
  },
}
