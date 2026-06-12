/**
 * WMS 拣货/波次 API 模块
 */
import request from '@/utils/request'

// ── 波次 ──────────────────────────────────────────────
export interface WmsPickWave {
  id: number
  waveNo: string
  warehouseId: number
  warehouseName: string
  waveType: number
  totalQty: number
  pickedQty: number
  status: number
  operatorName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 拣货任务 ──────────────────────────────────────────
export interface WmsPickTask {
  id: number
  taskNo: string
  waveId: number
  waveNo: string
  warehouseId: number
  warehouseName: string
  sourceType: number
  sourceNo: string
  sourceId: number
  assigneeName: string
  totalQty: number
  pickedQty: number
  status: number
  remark: string
  createTime: string
  updateTime: string
}

// ── 拣货明细 ──────────────────────────────────────────
export interface WmsPickDetail {
  id: number
  taskId: number
  waveId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQty: number
  pickedQty: number
  locationId: number
  locationCode: string
  batchNo: string
  status: number
  remark: string
}

export const pickApi = {
  // 波次
  waveSave(data: Partial<WmsPickWave>) { return request.post('/wms/pick/wave/save', data) },
  waveUpdate(data: Partial<WmsPickWave>) { return request.post('/wms/pick/wave/update', data) },
  waveGetById(id: number) { return request.get(`/wms/pick/wave/${id}`) },
  wavePage(params: any) { return request.get('/wms/pick/wave/page', { params }) },
  waveRemove(id: number) { return request.delete(`/wms/pick/wave/${id}`) },
  waveCreate(data: Partial<WmsPickWave>) { return request.post('/wms/pick/wave/create', data) },

  // 拣货任务
  taskSave(data: Partial<WmsPickTask>) { return request.post('/wms/pick/task/save', data) },
  taskUpdate(data: Partial<WmsPickTask>) { return request.post('/wms/pick/task/update', data) },
  taskGetById(id: number) { return request.get(`/wms/pick/task/${id}`) },
  taskPage(params: any) { return request.get('/wms/pick/task/page', { params }) },
  taskRemove(id: number) { return request.delete(`/wms/pick/task/${id}`) },
  taskStart(id: number) { return request.post(`/wms/pick/task/${id}/start`) },
  taskComplete(id: number) { return request.post(`/wms/pick/task/${id}/complete`) },
  taskListByWave(waveId: number) { return request.get(`/wms/pick/task/list-by-wave/${waveId}`) },

  // 拣货明细
  detailConfirm(id: number, pickedQty: number) { return request.post(`/wms/pick/detail/${id}/confirm`, null, { params: { pickedQty } }) },
  detailShortage(id: number, shortageQty: number) { return request.post(`/wms/pick/detail/${id}/shortage`, null, { params: { shortageQty } }) },
  detailList(taskId: number) { return request.get(`/wms/pick/detail/list/${taskId}`) },
}
