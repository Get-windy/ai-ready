import request from '@/utils/request'

export const accountSubjectApi = {
  getTree: (params?: any) => request.get('/erp/finance/subject/tree', { params }),
  getList: (params?: any) => request.get('/erp/finance/subject/list', { params }),
  getById: (id: number) => request.get(`/erp/finance/subject/${id}`),
  getByType: (type: number) => request.get(`/erp/finance/subject/type/${type}`),
  create: (data: any) => request.post('/erp/finance/subject', data),
  update: (id: number, data: any) => request.put(`/erp/finance/subject/${id}`, data),
  delete: (id: number) => request.delete(`/erp/finance/subject/${id}`),
  toggleEnabled: (id: number) => request.put(`/erp/finance/subject/${id}/enable`)
}

export const voucherApi = {
  getPage: (params: any) => request.get('/erp/finance/voucher/list', { params }),
  getById: (id: number) => request.get(`/erp/finance/voucher/${id}`),
  getByVoucherNo: (no: string) => request.get(`/erp/finance/voucher/no/${no}`),
  create: (data: any) => request.post('/erp/finance/voucher', data),
  audit: (id: number) => request.put(`/erp/finance/voucher/${id}/audit`),
  post: (id: number) => request.put(`/erp/finance/voucher/${id}/post`),
  reverse: (id: number, reason: string) => request.post(`/erp/finance/voucher/${id}/reverse`, { reason })
}

export const receivableApi = {
  getPage: (params: any) => request.get('/erp/finance/receivable/list', { params }),
  getById: (id: number) => request.get(`/erp/finance/receivable/${id}`),
  getAging: () => request.get('/erp/finance/receivable/aging'),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/receivable/${id}/write-off`, { amount }),
  markBadDebt: (id: number) => request.put(`/erp/finance/receivable/${id}/bad-debt`)
}

export const payableApi = {
  getPage: (params: any) => request.get('/erp/finance/payable/list', { params }),
  getById: (id: number) => request.get(`/erp/finance/payable/${id}`),
  getAging: () => request.get('/erp/finance/payable/aging'),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/payable/${id}/write-off`, { amount })
}

export const reportApi = {
  getTrialBalance: (params: any) => request.get('/erp/finance/report/v2/trial-balance', { params }),
  getBalanceSheet: (params: any) => request.get('/erp/finance/report/v2/balance-sheet', { params }),
  getIncomeStatement: (params: any) => request.get('/erp/finance/report/v2/income-statement', { params }),
  getDashboard: () => request.get('/erp/finance/report/v2/dashboard')
}
