/**
 * 订货商城管理后台 API 模块（支持企业客户 + 个人会员）
 */
import request, { type ApiResponse } from '@/utils/request'
import type { PageQuery } from '@/api/erp'

// ── 通用分页结果 ──
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages?: number
}

// ── 商城配置 ──
export interface ShopConfig {
  id?: number
  tenantId?: number
  shopName: string
  shopLogo?: string
  shopDesc?: string
  themeColor?: string
  bannerIds?: string
  templateId?: number
  paymentMethods?: string | string[]
  enableRegister?: number
  enableAutoAudit?: number
  minOrderAmount?: number
  freeShippingAmount?: number
  freightAmount?: number
  status?: number
}

export const shopConfigApi = {
  /** 获取商城配置 */
  get(): Promise<ApiResponse<ShopConfig>> {
    return request.get('/erp/mall/admin/config')
  },
  /** 更新商城配置 */
  update(data: ShopConfig): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/config', data)
  }
}

// ── 商城用户 ──
export interface ShopUser {
  id: number
  tenantId?: number
  username: string
  phone?: string
  email?: string
  companyName?: string
  nickname?: string
  avatar?: string
  source?: string
  auditStatus: number
  auditTime?: string
  auditBy?: number
  rejectReason?: string
  erpCustomerId?: number
  erpPartnerId?: number
  /** 用户身份类型：ENTERPRISE 企业客户 / MEMBER 个人会员 */
  userType?: string
  /** 统一身份标识 → biz_party.id */
  partyId?: number
  lastLoginTime?: string
  status: number
  createTime?: string
}

export const shopUserApi = {
  /** 分页查询用户 */
  page(params: PageQuery & { keyword?: string; auditStatus?: number; status?: number }): Promise<ApiResponse<PageResult<ShopUser>>> {
    return request.get('/erp/mall/admin/user/page', params)
  },
  /** 审核通过 */
  approve(id: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/approve`)
  },
  /** 审核驳回 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/reject`, null, { params: { reason } })
  },
  /** 启用/禁用 */
  toggleStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/status`, null, { params: { status } })
  }
}

// ── 轮播图 ──
export interface ShopBanner {
  id?: number
  tenantId?: number
  title?: string
  imageUrl: string
  linkUrl?: string
  linkType?: string
  linkValue?: string
  sortOrder?: number
  status?: number
}

export const shopBannerApi = {
  /** 获取轮播图列表 */
  list(): Promise<ApiResponse<ShopBanner[]>> {
    return request.get('/erp/mall/admin/banner')
  },
  /** 创建轮播图 */
  create(data: ShopBanner): Promise<ApiResponse<void>> {
    return request.post('/erp/mall/admin/banner', data)
  },
  /** 更新轮播图 */
  update(id: number, data: ShopBanner): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/banner/${id}`, data)
  },
  /** 删除轮播图 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/mall/admin/banner/${id}`)
  }
}

// ── 页面模板 ──
export interface ShopTemplate {
  id: number
  templateName: string
  templateCode: string
  thumbnail?: string
  description?: string
  configJson?: string
  isDefault?: number
  status?: number
}

export const shopTemplateApi = {
  /** 获取启用的模板列表 */
  list(): Promise<ApiResponse<ShopTemplate[]>> {
    return request.get('/erp/mall/admin/template/list')
  }
}

// ── 商城商品 ──
export interface MallProduct {
  id?: number
  productId: string
  /** 商品编码（后端 MallProduct.productCode） */
  productCode?: string
  productName: string
  imageUrl: string
  salePrice: number
  marketPrice: number
  /** 分类ID（后端 MallProduct.categoryId） */
  categoryId?: string
  categoryName: string
  /** 上架状态：ON_SHELF 上架 / OFF_SHELF 下架 */
  status: string
  description?: string
  stockQuantity?: number
  salesCount?: number
}

// ── 商城商品管理 ──
export const mallProductApi = {
  /** 分页查询 */
  page(params: PageQuery & { keyword?: string; categoryId?: string; status?: string }): Promise<ApiResponse<PageResult<MallProduct>>> {
    return request.get('/erp/mall/admin/product/page', params)
  },
  /** 创建 */
  create(data: MallProduct): Promise<ApiResponse<void>> {
    return request.post('/erp/mall/admin/product', data)
  },
  /** 更新 */
  update(id: number, data: MallProduct): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/product/${id}`, data)
  },
  /** 删除 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/mall/admin/product/${id}`)
  }
}

// ── 商城订单管理 ──
export interface MallOrderItem {
  id?: number
  productId?: string
  productName?: string
  productImage?: string
  price?: number
  quantity?: number
  subtotal?: number
}

export interface MallOrder {
  id?: number
  orderNo?: string
  customerId?: number
  customerName?: string
  totalAmount?: number
  payAmount?: number
  orderStatus?: string
  paymentMethod?: string
  paymentStatus?: string
  deliveryStatus?: string
  consignee?: string
  phone?: string
  address?: string
  remark?: string
  source?: string
  orderItems?: MallOrderItem[]
  createTime?: string
}

export const mallOrderApi = {
  /** 分页查询 */
  page(params: PageQuery & { keyword?: string; orderStatus?: string }): Promise<ApiResponse<PageResult<MallOrder>>> {
    return request.get('/erp/mall/admin/order/page', params)
  },
  /** 获取详情 */
  getDetail(id: number): Promise<ApiResponse<MallOrder>> {
    return request.get(`/erp/mall/admin/order/${id}`)
  },
  /** 根据ID获取 */
  getById(id: number): Promise<ApiResponse<MallOrder>> {
    return request.get(`/erp/mall/admin/order/${id}`)
  },
  /** 统计 */
  stats(): Promise<any> {
    return request.get('/erp/mall/admin/order/stats')
  },
  /** 审核通过 */
  approve(id: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/order/${id}/approve`)
  },
  /** 审核驳回 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/order/${id}/reject`, null, { params: { reason } })
  },
  /** 付款 */
  pay(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/mall/admin/order/${id}/pay`)
  },
  /** 发货 */
  ship(id: number, data: { logisticsCompany?: string; trackingNo?: string; remark?: string }): Promise<ApiResponse<void>> {
    return request.post(`/erp/mall/admin/order/${id}/ship`, data)
  },
  /** 退款 */
  refund(id: number, data: { amount?: number; reason?: string; remark?: string }): Promise<ApiResponse<void>> {
    return request.post(`/erp/mall/admin/order/${id}/refund`, data)
  },
  /** 批量审核 */
  batchApprove(ids: number[]): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/order/batch-approve', ids)
  },
  /** 批量发货 */
  batchShip(ids: number[]): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/order/batch-ship', ids)
  }
}

// ── 商城订单（管理端视图，字段与后端 ErpSaleOrderMall / erp_sale_order 一致） ──
export interface MallOrderAdmin {
  id: number
  orderNo: string
  customerId?: number
  customerName?: string
  orderDate?: string
  /** ERP订单状态: 0草稿(待付款) 1待审批(已付款) 2已审批 3部分出库(已发货) 4完成 5取消/驳回 */
  status?: number
  /** 订单来源: 2=企业客户商城 3=个人会员商城 */
  orderSource?: number
  totalAmount?: number
  receivedAmount?: number
  paymentMethod?: string
  /** 支付状态: 0待支付 1支付中 2已支付 3部分支付 4已退款 */
  paymentStatus?: number
  /** 发货状态: 0待发货 1部分发货 2已发货 3已签收 */
  deliveryStatus?: number
  consignee?: string
  consigneePhone?: string
  consigneeAddress?: string
  shippingAddress?: string
  orderRemark?: string
  buyerRemark?: string
  remark?: string
  /** 扩展信息JSON，含 originalMallStatus: PENDING_PAYMENT/PAID/APPROVED/SHIPPED/COMPLETED/CANCELLED/REJECTED */
  extInfo?: string
  createTime?: string
}

export const mallAdminOrderApi = {
  /** 分页查询商城订单（管理端） */
  page(params: PageQuery & { keyword?: string; orderStatus?: string }): Promise<PageResult<MallOrderAdmin>> {
    return request.get('/erp/mall/admin/order/page', params)
  },
  /** 获取订单详情 */
  detail(id: number): Promise<MallOrderAdmin> {
    return request.get(`/erp/mall/admin/order/${id}`)
  },
  /** 审核通过（仅 PAID 状态可操作） */
  approve(id: number): Promise<void> {
    return request.put(`/erp/mall/admin/order/${id}/approve`)
  },
  /** 审核驳回（仅 PAID/PENDING_PAYMENT 状态可操作） */
  reject(id: number, reason: string): Promise<void> {
    return request.put(`/erp/mall/admin/order/${id}/reject`, null, { params: { reason } })
  }
}

// ── 商城交易分析（/erp/mall/admin/trade-analysis，字段与 TradeAnalysisDTO 一致） ──
export interface MallTradeSummary {
  totalOrderCount?: number
  totalGmv?: number
  avgOrderAmount?: number
  refundOrderCount?: number
  refundRate?: number
}

export interface MallTradeDaily {
  day: string
  orderCount?: number
  gmv?: number
  avgOrderAmount?: number
}

export interface MallPaymentStatusItem {
  paymentStatus?: number
  paymentStatusName?: string
  count?: number
}

export interface MallTradeAnalysis {
  summary?: MallTradeSummary
  daily?: MallTradeDaily[]
  paymentStatusDistribution?: MallPaymentStatusItem[]
}

export const mallTradeApi = {
  /** 交易分析（按日GMV/客单价/退款率/支付状态分布） */
  analysis(params?: { startDate?: string; endDate?: string }): Promise<MallTradeAnalysis> {
    return request.get('/erp/mall/admin/trade-analysis', params)
  }
}

// ── 商品组合/套装（erp-sales /erp/product-kit，字段与 ProductKit/ProductKitItem 一致） ──
export interface ProductKitItem {
  id?: number
  kitId?: number
  lineNo?: number
  componentProductId?: number
  componentProductCode?: string
  componentProductName?: string
  componentProductSpec?: string
  componentProductUnit?: string
  quantity?: number
  unitCost?: number
  lineCost?: number
  optional?: boolean
  substitutable?: boolean
  remark?: string
}

export interface ProductKit {
  id?: number
  kitCode?: string
  kitName: string
  productId?: number
  productCode?: string
  productName?: string
  productSpec?: string
  productUnit?: string
  /** 套装类型: 1组合套装 2捆绑套装 3礼品套装 4组装产品 5拆分产品 */
  kitType?: number
  status?: number
  kitPrice?: number
  kitCost?: number
  profitRate?: number
  /** 是否启用（activate/deactivate 切换此字段） */
  active?: boolean
  allowSplit?: boolean
  allowPartial?: boolean
  minQuantity?: number
  maxQuantity?: number
  description?: string
  remark?: string
  items?: ProductKitItem[]
  createTime?: string
}

export const productKitApi = {
  /** 分页查询套装 */
  page(params: PageQuery & { keyword?: string; kitType?: number; status?: number }): Promise<PageResult<ProductKit>> {
    return request.get('/erp/product-kit/page', params)
  },
  /** 套装详情（含组件） */
  detail(id: number): Promise<ProductKit> {
    return request.get(`/erp/product-kit/${id}`)
  },
  /** 套装组件列表 */
  items(id: number): Promise<ProductKitItem[]> {
    return request.get(`/erp/product-kit/${id}/items`)
  },
  /** 创建套装（含组件） */
  create(data: Partial<ProductKit>): Promise<ProductKit> {
    return request.post('/erp/product-kit', data)
  },
  /** 更新套装（含组件） */
  update(id: number, data: Partial<ProductKit>): Promise<ProductKit> {
    return request.put(`/erp/product-kit/${id}`, data)
  },
  /** 复制套装 */
  copy(id: number): Promise<ProductKit> {
    return request.post(`/erp/product-kit/${id}/copy`)
  },
  /** 启用套装 */
  activate(id: number): Promise<void> {
    return request.post(`/erp/product-kit/${id}/activate`)
  },
  /** 停用套装 */
  deactivate(id: number): Promise<void> {
    return request.post(`/erp/product-kit/${id}/deactivate`)
  }
}

// ── 商城公告（/erp/mall/admin/notice，字段与后端 MallNotice 一致） ──
export interface MallNotice {
  id: number
  /** 公告标题 */
  title?: string
  /** 公告内容 */
  content?: string
  /** 公告类型: 1公告 2活动 3系统 */
  noticeType?: number
  /** 状态: 0草稿 1已发布 2已下线 */
  status?: number
  /** 发布时间 */
  publishTime?: string
  /** 排序 */
  sort?: number
  createTime?: string
}

export const mallNoticeApi = {
  /** 分页查询公告 */
  page(params: PageQuery & { title?: string; noticeType?: number; status?: number }): Promise<PageResult<MallNotice>> {
    return request.get('/erp/mall/admin/notice/page', params)
  },
  /** 公告详情 */
  detail(id: number): Promise<MallNotice> {
    return request.get(`/erp/mall/admin/notice/${id}`)
  },
  /** 创建公告（后端默认置为草稿） */
  create(data: Partial<MallNotice>): Promise<void> {
    return request.post('/erp/mall/admin/notice', data)
  },
  /** 更新公告 */
  update(id: number, data: Partial<MallNotice>): Promise<void> {
    return request.put(`/erp/mall/admin/notice/${id}`, data)
  },
  /** 删除公告 */
  delete(id: number): Promise<void> {
    return request.delete(`/erp/mall/admin/notice/${id}`)
  },
  /** 发布公告 */
  publish(id: number): Promise<void> {
    return request.put(`/erp/mall/admin/notice/${id}/publish`)
  },
  /** 下线公告 */
  offline(id: number): Promise<void> {
    return request.put(`/erp/mall/admin/notice/${id}/offline`)
  }
}

// ── 商城搜索关键词（/erp/mall/admin/keyword，字段与后端 MallKeyword 一致） ──
export interface MallKeyword {
  id: number
  /** 关键词 */
  keyword?: string
  /** 关键词类型: 1热门 2置顶 3屏蔽 */
  keywordType?: number
  /** 排序 */
  sort?: number
  /** 状态: 1启用 0禁用 */
  status?: number
  createTime?: string
}

export const mallKeywordApi = {
  /** 分页查询关键词 */
  page(params: PageQuery & { keyword?: string; keywordType?: number; status?: number }): Promise<PageResult<MallKeyword>> {
    return request.get('/erp/mall/admin/keyword/page', params)
  },
  /** 新增关键词（后端默认置为启用） */
  create(data: Partial<MallKeyword>): Promise<void> {
    return request.post('/erp/mall/admin/keyword', data)
  },
  /** 更新关键词 */
  update(id: number, data: Partial<MallKeyword>): Promise<void> {
    return request.put(`/erp/mall/admin/keyword/${id}`, data)
  },
  /** 删除关键词 */
  delete(id: number): Promise<void> {
    return request.delete(`/erp/mall/admin/keyword/${id}`)
  },
  /** 启用/禁用关键词 */
  toggleStatus(id: number, status: number): Promise<void> {
    return request.put(`/erp/mall/admin/keyword/${id}/status`, null, { params: { status } })
  }
}

// ── 商城退货/售后 ──
export interface MallReturn {
  id: number
  returnNo: string
  orderId: number
  orderNo: string
  customerName: string
  productName: string
  quantity: number
  amount: number
  reason: string
  status: number
  remark?: string
  createTime: string
}

export const mallReturnApi = {
  page(params: PageQuery & { keyword?: string; status?: number }): Promise<PageResult<MallReturn>> {
    return request.get('/erp/mall/admin/return/page', params)
  },
  stats(): Promise<any> {
    return request.get('/erp/mall/admin/return/stats')
  },
  getById(id: number): Promise<ApiResponse<MallReturn>> {
    return request.get(`/erp/mall/admin/return/${id}`)
  },
  approve(id: number): Promise<void> {
    return request.put(`/erp/mall/admin/return/${id}/approve`)
  },
  reject(id: number): Promise<void> {
    return request.put(`/erp/mall/admin/return/${id}/reject`)
  },
  batchApprove(ids: number[]): Promise<void> {
    return request.put('/erp/mall/admin/return/batch-approve', ids)
  },
  batchReject(ids: number[]): Promise<void> {
    return request.put('/erp/mall/admin/return/batch-reject', ids)
  }
}
