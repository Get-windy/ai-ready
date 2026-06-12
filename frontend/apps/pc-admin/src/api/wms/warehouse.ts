/**
 * WMS 仓库/货位 API 模块
 */
import request from '@/utils/request'

// ── 仓库 ──────────────────────────────────────────────
export interface WmsWarehouse {
  id: number
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  warehouseType: number
  zoneCount: number
  locationCount: number
  totalCapacity: number
  usedCapacity: number
  isWmsEnabled: number
  remark: string
}

export const warehouseApi = {
  save(data: Partial<WmsWarehouse>) { return request.post('/wms/warehouse/save', data) },
  update(data: Partial<WmsWarehouse>) { return request.post('/wms/warehouse/update', data) },
  getById(id: number) { return request.get(`/wms/warehouse/${id}`) },
  page(params: any) { return request.get('/wms/warehouse/page', { params }) },
  remove(id: number) { return request.delete(`/wms/warehouse/${id}`) },
  listAll() { return request.get('/wms/warehouse/list-all') },
}

// ── 货位 ──────────────────────────────────────────────
export interface WmsLocation {
  id: number
  warehouseId: number
  locationCode: string
  locationName: string
  locationType: number
  locationLevel: number
  parentId: number
  path: string
  maxCapacity: number
  usedCapacity: number
  maxWeight: number
  status: number
  isPickable: number
  isReceivable: number
  sortOrder: number
  remark: string
}

export const locationApi = {
  save(data: Partial<WmsLocation>) { return request.post('/wms/location/save', data) },
  update(data: Partial<WmsLocation>) { return request.post('/wms/location/update', data) },
  getById(id: number) { return request.get(`/wms/location/${id}`) },
  page(params: any) { return request.get('/wms/location/page', { params }) },
  remove(id: number) { return request.delete(`/wms/location/${id}`) },
  listByWarehouse(warehouseId: number) { return request.get(`/wms/location/list-by-warehouse/${warehouseId}`) },
  recommend(params: any) { return request.get('/wms/location/recommend', { params }) },
}
