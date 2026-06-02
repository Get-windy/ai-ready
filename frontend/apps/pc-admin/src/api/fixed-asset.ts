import request from '@/utils/request'

export const fixedAssetApi = {
  // 资产
  getPage: (params: any) => request.get('/erp/fixed-asset/asset/page', { params }),
  getById: (id: number) => request.get(`/erp/fixed-asset/asset/${id}`),
  create: (data: any) => request.post('/erp/fixed-asset/asset', data),
  update: (id: number, data: any) => request.put(`/erp/fixed-asset/asset/${id}`, data),
  delete: (id: number) => request.delete(`/erp/fixed-asset/asset/${id}`),
  getStatistics: () => request.get('/erp/fixed-asset/asset/statistics'),
  depreciate: (id: number) => request.post(`/erp/fixed-asset/asset/${id}/depreciate`),
  exportData: (params: any) => request.get('/erp/fixed-asset/asset/export', { params, responseType: 'blob' })
}

export const fixedAssetCategoryApi = {
  getList: () => request.get('/erp/fixed-asset/category/list'),
  getTree: () => request.get('/erp/fixed-asset/category/tree'),
  getById: (id: number) => request.get(`/erp/fixed-asset/category/${id}`),
  create: (data: any) => request.post('/erp/fixed-asset/category', data),
  update: (id: number, data: any) => request.put(`/erp/fixed-asset/category/${id}`, data),
  delete: (id: number) => request.delete(`/erp/fixed-asset/category/${id}`)
}

export const depreciationApi = {
  getPage: (params: any) => request.get('/erp/fixed-asset/depreciation/page', { params }),
  getById: (id: number) => request.get(`/erp/fixed-asset/depreciation/${id}`),
  batchCalculate: (data: any) => request.post('/erp/fixed-asset/depreciation/batch-calculate', data)
}

export const transferApi = {
  getPage: (params: any) => request.get('/erp/fixed-asset/transfer/page', { params }),
  getById: (id: number) => request.get(`/erp/fixed-asset/transfer/${id}`),
  create: (data: any) => request.post('/erp/fixed-asset/transfer', data),
  update: (id: number, data: any) => request.put(`/erp/fixed-asset/transfer/${id}`, data),
  delete: (id: number) => request.delete(`/erp/fixed-asset/transfer/${id}`),
  approve: (id: number, comment?: string) => request.post(`/erp/fixed-asset/transfer/${id}/approve`, null, { params: { comment } }),
  reject: (id: number, comment?: string) => request.post(`/erp/fixed-asset/transfer/${id}/reject`, null, { params: { comment } })
}

export const disposalApi = {
  getPage: (params: any) => request.get('/erp/fixed-asset/disposal/page', { params }),
  getById: (id: number) => request.get(`/erp/fixed-asset/disposal/${id}`),
  create: (data: any) => request.post('/erp/fixed-asset/disposal', data),
  update: (id: number, data: any) => request.put(`/erp/fixed-asset/disposal/${id}`, data),
  delete: (id: number) => request.delete(`/erp/fixed-asset/disposal/${id}`),
  approve: (id: number, comment?: string) => request.post(`/erp/fixed-asset/disposal/${id}/approve`, null, { params: { comment } }),
  reject: (id: number, comment?: string) => request.post(`/erp/fixed-asset/disposal/${id}/reject`, null, { params: { comment } })
}

export const inventoryApi = {
  getPage: (params: any) => request.get('/erp/fixed-asset/inventory/page', { params }),
  getById: (id: number) => request.get(`/erp/fixed-asset/inventory/${id}`),
  create: (data: any) => request.post('/erp/fixed-asset/inventory', data),
  update: (id: number, data: any) => request.put(`/erp/fixed-asset/inventory/${id}`, data)
}

export const reportApi = {
  getDepreciationSummary: (params?: any) => request.get('/erp/fixed-asset/report/depreciation-summary', { params }),
  getAssetLedger: (params?: any) => request.get('/erp/fixed-asset/report/asset-ledger', { params }),
  getAgeAnalysis: () => request.get('/erp/fixed-asset/report/age-analysis'),
  getCategorySummary: () => request.get('/erp/fixed-asset/report/category-summary')
}
