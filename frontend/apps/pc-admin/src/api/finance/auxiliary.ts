import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 辅助核算类型API
 */
export const auxiliaryTypeApi = {
  getList: (params?: any) => request.get('/erp/finance/auxiliary/type/list', params),
  getById: (id: number) => request.get(`/erp/finance/auxiliary/type/${id}`),
  create: (data: any) => request.post('/erp/finance/auxiliary/type', data),
  update: (id: number, data: any) => request.put(`/erp/finance/auxiliary/type/${id}`, data),
  delete: (id: number) => request.delete(`/erp/finance/auxiliary/type/${id}`),
  toggleEnabled: (id: number, enabled?: boolean) =>
    request.put(`/erp/finance/auxiliary/type/${id}/enable`, null, { params: { enabled } })
}

/**
 * 辅助核算项目API
 */
export const auxiliaryItemApi = {
  getList: (params?: any) => request.get('/erp/finance/auxiliary/item/list', params),
  getById: (id: number) => request.get(`/erp/finance/auxiliary/item/${id}`),
  create: (data: any) => request.post('/erp/finance/auxiliary/item', data),
  update: (id: number, data: any) => request.put(`/erp/finance/auxiliary/item/${id}`, data),
  delete: (id: number) => request.delete(`/erp/finance/auxiliary/item/${id}`),
  toggleEnabled: (id: number, enabled?: boolean) =>
    request.put(`/erp/finance/auxiliary/item/${id}/enable`, null, { params: { enabled } })
}
