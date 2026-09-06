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
  PENDING: { text: '待检', color: 'warning' },
  CONCESSION: { text: '让步接收', color: 'processing' }
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
  qualityNo: string
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
  bizNo: string
  defectType: string
  defectDesc: string
  defectLevel: string
  defectQuantity: number
  handleType: string
  handleQuantity: number
  handlerId: number
  handlerName: string
  handleTime: string
  handleResult: string
  correctiveAction: string
  preventiveAction: string
  returnNo: string
  damageNo: string
  status: number
  createTime: string
}

// 缺陷处理历史
export interface QualityDefectHandleHistory {
  id: number
  defectId: number
  handleType: string
  handleQuantity: number
  handleResult: string
  handlerName: string
  handleTime: string
  returnNo: string
  damageNo: string
  correctiveAction: string
  preventiveAction: string
}

// 质检标准 API
export const qualityStandardApi = {
  create: (standard: QualityStandard) =>
    request.post<Result<QualityStandard>>('/api/quality/standard', standard),

  update: (id: number, standard: Partial<QualityStandard>) =>
    request.put<Result<QualityStandard>>(`/api/quality/standard/${id}`, standard),

  updateStatus: (id: number, status: number) =>
    request.put<Result<QualityStandard>>(`/api/quality/standard/${id}`, { status }),

  delete: (id: number) =>
    request.delete<Result<void>>(`/api/quality/standard/${id}`),

  page: (params: { pageNum: number; pageSize: number; standardCode?: string; standardName?: string; inspectionType?: string; status?: number }) =>
    request.get<Result<PageResult<QualityStandard>>>('/api/quality/standard/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityStandard>>(`/api/quality/standard/${id}`),

  listByType: (inspectionType: string) =>
    request.get<Result<QualityStandard[]>>('/api/quality/standard/list-by-type', { params: { inspectionType } }),

  nextNo: () =>
    request.get<Result<string>>('/api/quality/standard/next-no')
}

// 检验记录 API
export const qualityInspectionApi = {
  create: (inspection: QualityInspection) =>
    request.post<Result<QualityInspection>>('/api/quality/inspection', inspection),

  update: (id: number, inspection: Partial<QualityInspection>) =>
    request.put<Result<QualityInspection>>(`/api/quality/inspection/${id}`, inspection),

  complete: (id: number, params: { result: string; passQuantity: number; failQuantity: number; remark?: string }) =>
    request.post<Result<void>>(`/api/quality/inspection/${id}/complete`, params),

  cancel: (id: number) =>
    request.post<Result<QualityInspection>>(`/api/quality/inspection/${id}/cancel`),

  delete: (id: number) =>
    request.delete<Result<void>>(`/api/quality/inspection/${id}`),

  batchDelete: (ids: number[]) =>
    request.delete<Result<void>>('/api/quality/inspection/batch', { data: ids }),

  nextNo: () =>
    request.get<Result<string>>('/api/quality/inspection/next-no'),

  page: (params: { pageNum: number; pageSize: number; bizType?: string; result?: string; excludeResult?: string; bizNo?: string; qualityNo?: string; productName?: string; inspectorName?: string; inspectionType?: string; status?: number; dateStart?: string; dateEnd?: string; keyword?: string }) =>
    request.get<Result<PageResult<QualityInspection>>>('/api/quality/inspection/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityInspection>>(`/api/quality/inspection/${id}`),

  listPending: (bizType?: string) =>
    request.get<Result<QualityInspection[]>>('/api/quality/inspection/pending', { params: { bizType } })
}

// 质量证书类型
export interface QualityCertificate {
  id: number
  tenantId: number
  certificateNo: string
  certificateType: string
  productName: string
  productCode: string
  batchNo: string
  supplierName: string
  inspectionDate: string
  issueDate: string
  expiryDate: string
  result: string
  inspectorId: number
  inspectorName: string
  certificateUrl: string
  remark: string
  status: number
  createTime: string
}

// 检验结论枚举
export const CERTIFICATE_RESULT_MAP: Record<string, { text: string; color: string }> = {
  QUALIFIED: { text: '合格', color: 'success' },
  UNQUALIFIED: { text: '不合格', color: 'error' },
  CONDITIONAL: { text: '有条件放行', color: 'warning' }
}

// 证书类型枚举
export const CERTIFICATE_TYPE_MAP: Record<string, { name: string; color: string }> = {
  COA: { name: 'COA 分析证书', color: 'blue' },
  COC: { name: 'COC 合格证书', color: 'green' },
  ISO: { name: 'ISO 认证', color: 'purple' },
  OTHER: { name: '其他', color: 'default' }
}

// 不合格处理 API
export const qualityDefectHandleApi = {
  create: (params: { inspectionId: number; defectType: string; defectDesc: string; defectQuantity: number; defectLevel?: string }) =>
    request.post<Result<QualityDefectHandle>>('/api/quality/defect', params),

  handle: (id: number, params: { handleType: string; handleQuantity: number; handleResult: string; correctiveAction?: string; preventiveAction?: string }) =>
    request.post<Result<void>>(`/api/quality/defect/${id}/handle`, params),

  page: (params: { pageNum: number; pageSize: number; inspectionId?: string | number; defectType?: string; defectLevel?: string; status?: string | number; bizNo?: string; handlerName?: string; createTimeStart?: string; createTimeEnd?: string }) =>
    request.get<Result<PageResult<QualityDefectHandle>>>('/api/quality/defect/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityDefectHandle>>(`/api/quality/defect/${id}`),

  listPending: () =>
    request.get<Result<QualityDefectHandle[]>>('/api/quality/defect/pending'),

  history: (id: number) =>
    request.get<Result<QualityDefectHandleHistory[]>>(`/api/quality/defect/${id}/history`)
}

// 质量证书 API
export const qualityCertificateApi = {
  create: (certificate: QualityCertificate) =>
    request.post<Result<QualityCertificate>>('/api/quality/certificate', certificate),

  update: (id: number, certificate: QualityCertificate) =>
    request.put<Result<QualityCertificate>>(`/api/quality/certificate/${id}`, certificate),

  delete: (id: number) =>
    request.delete<Result<void>>(`/api/quality/certificate/${id}`),

  page: (params: { pageNum: number; pageSize: number; productName?: string; batchNo?: string; result?: string; startDate?: string; endDate?: string }) =>
    request.get<Result<PageResult<QualityCertificate>>>('/api/quality/certificate/page', { params }),

  get: (id: number) =>
    request.get<Result<QualityCertificate>>(`/api/quality/certificate/${id}`)
}