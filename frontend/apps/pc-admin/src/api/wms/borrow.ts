/**
 * WMS 借进借出 API 模块
 * 后端: BorrowController (/api/wms/borrow)
 */
import request from '@/utils/request'

// ── 借进借出单（对齐 WmsBorrowOrder 实体） ────────────
export interface WmsBorrowOrder {
  id: number
  orderNo: string
  /** 方向 1-借进 2-借出 */
  direction: number
  partnerId: number
  partnerName: string
  warehouseId: number
  warehouseName: string
  /** 借出/借进日期（LocalDate，YYYY-MM-DD） */
  borrowDate: string
  /** 预计归还日期（LocalDate，YYYY-MM-DD） */
  expectedReturnDate: string
  /** 状态 0-草稿 1-待审批 2-已审批 3-部分归还 4-已归还 5-已取消 */
  status: number
  totalQuantity: number
  returnedQuantity: number
  remark: string
  createTime: string
  updateTime: string
}

// ── 借进借出单明细（对齐 WmsBorrowOrderItem 实体） ────
export interface WmsBorrowOrderItem {
  id: number
  orderId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  unit: string
  quantity: number
  returnedQuantity: number
  price: number
  amount: number
  remark: string
}

// ── 借进借出单（含明细，对齐 WmsBorrowOrderVO） ───────
export interface WmsBorrowOrderVO extends WmsBorrowOrder {
  items?: WmsBorrowOrderItem[]
}

// ── 归还记录（对齐 WmsBorrowReturn 实体） ─────────────
export interface WmsBorrowReturn {
  id: number
  orderId: number
  returnDate: string
  operatorId: number
  operatorName: string
  remark: string
  createTime: string
}

// ── 归还记录明细（对齐 WmsBorrowReturnItem 实体） ─────
export interface WmsBorrowReturnItem {
  id?: number
  returnId?: number
  orderItemId: number
  productId?: number
  productCode?: string
  productName?: string
  /** 本次归还数量 */
  quantity: number
}

// ── 归还请求（对齐 BorrowReturnRequest） ──────────────
export interface BorrowReturnRequest {
  orderId: number
  returnDate?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  items: WmsBorrowReturnItem[]
}

export const borrowApi = {
  /** 分页查询：GET /wms/borrow/page?current&size&direction&status&partnerId&partnerName&orderNo&warehouseId */
  page(params: any) { return request.get('/wms/borrow/page', { params }) },
  /** 单据详情（含明细 items）：GET /wms/borrow/{id} */
  getById(id: number) { return request.get(`/wms/borrow/${id}`) },
  /** 新建（含明细，单号后端自动生成，状态=草稿）：POST /wms/borrow/create */
  create(data: Partial<WmsBorrowOrderVO>) { return request.post('/wms/borrow/create', data) },
  /** 更新（仅草稿，明细整体替换）：POST /wms/borrow/update */
  update(data: Partial<WmsBorrowOrderVO>) { return request.post('/wms/borrow/update', data) },
  /** 删除（仅草稿/已取消）：DELETE /wms/borrow/{id} */
  remove(id: number) { return request.delete(`/wms/borrow/${id}`) },
  /** 提交审批：POST /wms/borrow/submit?id */
  submit(id: number) { return request.post('/wms/borrow/submit', null, { params: { id } }) },
  /** 审批通过（借进库存增加/借出库存扣减）：POST /wms/borrow/approve?id&operatorId&operatorName */
  approve(id: number, operatorId?: number, operatorName?: string) {
    return request.post('/wms/borrow/approve', null, { params: { id, operatorId, operatorName } })
  },
  /** 取消单据（仅草稿/待审批）：POST /wms/borrow/cancel?id */
  cancel(id: number) { return request.post('/wms/borrow/cancel', null, { params: { id } }) },
  /** 归还登记（支持部分归还，库存反向回冲）：POST /wms/borrow/return */
  returnOrder(data: BorrowReturnRequest) { return request.post('/wms/borrow/return', data) },
  /** 归还记录分页：GET /wms/borrow/return-page?current&size&orderId */
  returnPage(params: any) { return request.get('/wms/borrow/return-page', { params }) },
  /** 归还记录明细：GET /wms/borrow/return-items/{returnId} */
  returnItems(returnId: number) { return request.get(`/wms/borrow/return-items/${returnId}`) },
}
