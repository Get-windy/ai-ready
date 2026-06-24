import request from '@/utils/request'
import type { PageResult, Result } from '@/api/common'

// 检验类型枚举
export const INSPECTION_TYPE_MAP: Record<string, { name: string; color: string }> = {
  INBOUND: { name: '入库检验', color: 'blue' },
  OUTBOUND: { name: '出库检验', color: 'green' },
  PROCESS: { name: '过程检验', color: 'orange' }
}

// 检验结果枚举
export const INSPECTION_RESULT_MAP: Record<string, { text: string; color: string }> = {
  PASS: { text: '合格', color: 'success' },
  FAIL: { text: '不合格', color: 'error' },
  PENDING: { text: '待检', color: 'warning' }
}

// 缺陷类型枚举
export const DEFECT_TYPE_MAP: Record<string, { name: string }> = {
  QUALITY: { name: '质量缺陷' },
  PACKAGING: { name: '包装缺陷' },
  LABELING: { name: '标签缺陷' }
}

// 处理方式枚举
export const HANDLE_TYPE_MAP: Record<string, { name: string; color: string }> = {
  RETURN: { name: '退货', color: 'red' },
  REWORK: { name: '返工', color: 'orange' },
  SCRAP: { name: '报废', color: 'error' },
  SPECIAL_RELEASE: { name: '特采', color: 'green' }
}

// 质检标准类型
export interface QualityStandard {
  id: number
  tenantId: number
  standardCode: string
  standardName: string
  inspectionType: string
  inspectionItems: string
  sampleRate: number
  passThreshold: number
  description: string
  status: number
  createTime: string
}

// 检验记录类型
export interface QualityInspection {
  id: number
  tenantId: number
  bizId: number
  bizType: string
  bizNo: string
  productId: number
  productName: string
  batchNo: string
  quantity: number
  sampleQuantity: number
  inspectionResult: string
  passQuantity: number
  failQuantity: number
  inspectionItems: string
  inspectorId: number
  inspectorName: string
  inspectionTime: string
  remark: string
  createTime: string
}

// 不合格处理类型
export interface QualityDefectHandle {
  id: number
  tenantId: number
  inspectionId: number
  defectType: string
  defectDesc: string
  handleType: string
  handleQuantity: number
  handlerId: number
  handlerName: string
  handleTime: string
  handleResult: string
  status: number
  createTime: string
}

// 质检标准 API
export const qualityStandardApi = {
  create: (standard: QualityStandard) =>
    request.post<Result<QualityStandard>>('/api/quality/standard', standard),

  update: (id: number, standard: QualityStandard) =>
    request.put<Result<QualityStandard>>(`/api/quality/standard/${id}`, standard),

  delete: (id: number) =>
    request.delete<Result<void>>(`/api/quality/standard/${id}`),

  page: (params: { pageNum: number; pageSize: number; inspectionType?: string; status?: number }) =>
    request.get<Result<PageResult<QualityStandard>>>('/api/quality/standard/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityStandard>>(`/api/quality/standard/${id}`),

  listByType: (inspectionType: string) =>
    request.get<Result<QualityStandard[]>>('/api/quality/standard/list-by-type', { params: { inspectionType } })
}

// 检验记录 API
export const qualityInspectionApi = {
  create: (inspection: QualityInspection) =>
    request.post<Result<QualityInspection>>('/api/quality/inspection', inspection),

  complete: (id: number, params: { result: string; passQuantity: number; failQuantity: number }) =>
    request.post<Result<void>>(`/api/quality/inspection/${id}/complete`, params),

  page: (params: { pageNum: number; pageSize: number; bizType?: string; result?: string }) =>
    request.get<Result<PageResult<QualityInspection>>>('/api/quality/inspection/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityInspection>>(`/api/quality/inspection/${id}`),

  listPending: (bizType?: string) =>
    request.get<Result<QualityInspection[]>>('/api/quality/inspection/pending', { params: { bizType } })
}

// 不合格处理 API
export const qualityDefectHandleApi = {
  create: (params: { inspectionId: number; defectType: string; defectDesc: string; defectQuantity: number }) =>
    request.post<Result<QualityDefectHandle>>('/api/quality/defect', params),

  handle: (id: number, params: { handleType: string; handleQuantity: number; handleResult: string }) =>
    request.post<Result<void>>(`/api/quality/defect/${id}/handle`, params),

  page: (params: { pageNum: number; pageSize: number; status?: number }) =>
    request.get<Result<PageResult<QualityDefectHandle>>>('/api/quality/defect/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityDefectHandle>>(`/api/quality/defect/${id}`),

  listPending: () =>
    request.get<Result<QualityDefectHandle[]>>('/api/quality/defect/pending')
}