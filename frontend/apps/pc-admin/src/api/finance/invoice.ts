import request, { type ApiResponse } from '@/utils/request'

/**
 * 发票 API（财务模块）。
 *
 * 归属说明：发票**属于财务模块**（后端实现在 `erp-finance` 的 `InvoiceController`，
 * 前缀 `/api/erp/invoice`，权限码 `invoice:*`）。2026-09-26 之前这套页面挂在 CRM 菜单下、
 * 按钮用的是 `crm:invoice:*` 码 —— 而那 7 个码后端零消费、与守卫用的 `invoice:*` 零交集，
 * 且模块权益门按 `invoice:` → `finance` 判定，只买 CRM 的租户必然 403。现已整体搬回财务。
 *
 * 注：该后端**没有统一响应包装**（裸实体 / 裸 List / 裸 Page），
 * 查询类走标准 request 由拦截器原样透传。
 */

// ── 类型 ──────────────────────────────────────────

export interface InvoiceItem {
  id: number
  invoiceNo?: string
  invoiceType?: string
  customerId?: number
  customerName: string
  invoiceDate: string
  amount: number
  taxAmount?: number
  totalAmount?: number
  status?: string
  issuer?: string
  remark?: string
  createTime: string
}

export interface InvoiceQuery {
  keyword?: string
  customerId?: number
  status?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

// ── API ──────────────────────────────────────────

export const invoiceApi = {
  /** 分页查询（`/page`，Spring Data 分页模型：content/totalElements） */
  page(params: InvoiceQuery): Promise<any> {
    return request.get('/erp/invoice/page', params)
  },
  /**
   * 条件查询（`/query`）。
   *
   * ⚠️ 这是**列表页实际使用的端点**（`/page` 只被本文件的历史封装引用过）。
   * 两者后端都存在，但查询参数键不同：本端点走 `pageNum/pageSize`（见后端 `@RequestParam`）。
   */
  query(params?: any): Promise<any> {
    return request.get('/erp/invoice/query', params)
  },
  getById(id: number | string): Promise<ApiResponse<InvoiceItem>> {
    return request.get(`/erp/invoice/${id}`)
  },
  /** 新建：后端只有「由发票申请生成」这一个入口（要求 applicationId） */
  create(data: Partial<InvoiceItem>): Promise<ApiResponse<InvoiceItem>> {
    return request.post('/erp/invoice/create-from-application', data)
  },
  update(id: number | string, data: Partial<InvoiceItem>): Promise<ApiResponse<InvoiceItem>> {
    return request.put(`/erp/invoice/${id}`, data)
  },
  /** 状态变更：newStatus 取后端 InvoiceStatus 枚举 name */
  updateStatus(id: number | string, newStatus: string, notes?: string): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/invoice/${id}/status`, null, { params: { newStatus, notes } })
  },
  voidInvoice(id: number | string, reason: string, voidedBy?: any): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/invoice/${id}/void`, null, { params: { reason, voidedBy } })
  },
  sendInvoice(id: number | string, sendMethod: string, sentBy?: any): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/invoice/${id}/send`, null, { params: { sendMethod, sentBy } })
  },
  getStatistics(startDate?: string, endDate?: string): Promise<ApiResponse<any>> {
    return request.get('/erp/invoice/statistics', { startDate, endDate })
  }
}
