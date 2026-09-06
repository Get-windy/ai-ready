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
  /** 往来单位编号 */
  partnerCode?: string
  warehouseId: number
  warehouseName: string
  /** 借出/借进日期（LocalDate，YYYY-MM-DD） */
  borrowDate: string
  /** 预计归还日期（LocalDate，YYYY-MM-DD） */
  expectedReturnDate?: string
  /** 状态 0-草稿 1-待审批 2-已审批 3-部分归还 4-已归还 5-已取消 */
  status: number
  totalQuantity: number
  returnedQuantity: number
  remark: string
  /** 经手人 */
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  creatorName?: string
  bookkeeperId?: number
  bookkeeperName?: string
  bookkeepingTime?: string
  summary?: string
  attachment?: string
  printCount?: number
  borrowAmount?: number
  borrowQuantity?: number
  nonProcessedQuantity?: number
  nonProcessedAmount?: number
  convertPurchaseQuantity?: number
  convertPurchaseAmount?: number
  totalWeight?: number
  totalVolume?: number
  redFlag?: number
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
  barcode?: string
  model?: string
  origin?: string
  region?: string
  location?: string
  taste?: string
  productionDate?: string
  shelfLife?: string
  expiryDate?: string
  batchCode?: string
  conversionRelation?: string
  conversionResult?: number
  pieceQuantity?: number
  bigPack?: number
  midPack?: number
  smallPack?: number
  smallUnit?: string
  smallUnitQuantity?: number
  smallUnitPrice?: number
  retailPrice?: number
  wholesalePrice?: number
  minPrice?: number
  availableStock?: number
  bookStock?: number
  weight?: number
  volume?: number
  processedReturnQuantity?: number
  processedPurchaseQuantity?: number
  nonProcessedQuantity?: number
  nonProcessedAmount?: number
  priceLevel1?: number
  priceLevel2?: number
  priceLevel3?: number
  priceLevel4?: number
  priceLevel5?: number
  priceLevel6?: number
  priceLevel7?: number
  priceLevel8?: number
  itemExtNum1?: number
  itemExtNum2?: number
  itemExtNum3?: number
  itemExtText1?: string
  itemExtText2?: string
}

// ── 借进借出单（含明细，对齐 WmsBorrowOrderVO） ───────
export interface WmsBorrowOrderVO extends WmsBorrowOrder {
  items?: WmsBorrowOrderItem[]
}

// ── 按明细列表行（对齐 BorrowOrderItemVO） ────────────
export interface BorrowOrderItemVO extends WmsBorrowOrderItem {
  orderNo: string
  status: number
  warehouseName: string
  partnerId?: number
  partnerCode?: string
  partnerName?: string
  handlerName?: string
  deptName?: string
  docRemark?: string
  summary?: string
  attachment?: string
  bookkeeperName?: string
  creatorName?: string
  bookkeepingTime?: string
  createTime?: string
  printCount?: number
  expectedReturnDate?: string
  totalWeight?: number
  totalVolume?: number
  customerLevel?: string
  contact?: string
  address?: string
  defaultHandler?: string
  oneBill?: string
  customerRemark?: string
}

// ── 列表查询条件（对齐 BorrowOrderQuery） ────────────
export interface BorrowOrderQuery {
  pageNum?: number
  pageSize?: number
  direction?: number
  dateStart?: string
  dateEnd?: string
  returnDateStart?: string
  returnDateEnd?: string
  orderNo?: string
  partnerName?: string
  partnerCode?: string
  handlerName?: string
  deptName?: string
  creatorName?: string
  bookkeeperName?: string
  warehouseId?: number
  warehouseName?: string
  status?: number
  remark?: string
  itemRemark?: string
  productName?: string
  showRed?: boolean
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

// ── 借转采购请求（对齐 ConvertPurchaseRequest） ────────
export interface ConvertPurchaseRequest {
  orderId: number
  items: { orderItemId: number; quantity: number }[]
}

export const borrowApi = {
  /** 分页查询：GET /wms/borrow/page?current&size&direction&status&partnerId&partnerName&orderNo&warehouseId */
  page(params: any) { return request.get('/wms/borrow/page', { params }) },
  /** 多条件分页查询(按单据)：GET /wms/borrow/doc-query */
  docQuery(params: BorrowOrderQuery) { return request.get('/wms/borrow/doc-query', { params }) },
  /** 分页查询借进借出明细(按明细)：GET /wms/borrow/page-detail */
  pageDetail(params: BorrowOrderQuery) { return request.get('/wms/borrow/page-detail', { params }) },
  /** 借进借出商品台账聚合查询（按 商品×往来单位 分组）：GET /wms/borrow/aggregate */
  aggregate(params: {
    direction?: number
    partnerName?: string
    productName?: string
    dateStart?: string
    dateEnd?: string
    categoryId?: number | string
  }) { return request.get('/wms/borrow/aggregate', { params }) },
  /** 生成下一借进单号：GET /wms/borrow/next-no */
  nextNo() { return request.get('/wms/borrow/next-no') },
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
  /** 记账（入库/出库）：POST /wms/borrow/post?id&operatorId&operatorName */
  post(id: number, operatorId?: number, operatorName?: string) {
    return request.post('/wms/borrow/post', null, { params: { id, operatorId, operatorName } })
  },
  /** 借转采购登记：POST /wms/borrow/convert-purchase */
  convertPurchase(data: ConvertPurchaseRequest) { return request.post('/wms/borrow/convert-purchase', data) },
  /** 借转销售登记（借出方向）：POST /wms/borrow/convert-sale */
  convertSale(data: ConvertPurchaseRequest) { return request.post('/wms/borrow/convert-sale', data) },
  /** 查询某商品×仓库可用库存：GET /wms/inventory/query */
  inventoryQuery(productId: number, warehouseId: number) {
    return request.get('/wms/inventory/query', { params: { productId, warehouseId } })
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
