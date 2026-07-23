/**
 * 营销域 API 封装
 *
 * 端点逐一核对自 backend 控制器（禁止对猜测路径接线）：
 *  - erp-marketing: LoyaltyCouponController   /api/erp/marketing/coupon
 *                   LoyaltyCardController     /api/erp/marketing/card
 *                   LoyaltyProgramController  /api/erp/marketing/program
 *                   MarketingRuleController   /api/erp/marketing/rules
 *                   FlashSaleController       /api/erp/marketing/flash-sale
 *  - erp-sales:     PromotionController       /api/sale/promotion
 *                   ProductKitController      /api/erp/product-kit
 *  - erp-stock:     GroupBuyController        /api/erp/marketing/group-buy
 *  - erp-pricing:   ProductGradePriceController /api/erp/product-grade-price
 *  - crm:           MarketingCampaignController /api/crm/marketing
 *  - platform:      SmsConfigController       /api/sms
 *  - search:        SearchController          /api/search/hot
 *
 * 响应解包说明：
 *  - Result.ok(data)（code=200）经 request 拦截器直接返回 data
 *  - MyBatis-Plus Page（{records,total} 无 wrapper）拦截器原样透传
 *  - CRM 活动/套装的单对象与 void 响应、短信保存/测试、热搜端点均无标准
 *    wrapper（无 code 字段），会被拦截器误判为失败，故参照 ./erp/batch 的
 *    既定兼容方式改用原生 axios 自行解包
 */
import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'

// ── 通用类型 ────────────────────────────────────────────

/** MyBatis-Plus 分页结果（拦截器解包/透传后的形状） */
export interface PageResult<T = any> {
  records: T[]
  total: number
  current?: number
  size?: number
  pages?: number
}

// ── 裸请求辅助（非标准 wrapper 端点专用） ────────────────

function rawHeaders(): Record<string, string> {
  const token = getToken()
  const headers: Record<string, string> = { tenantId: localStorage.getItem('tenantId') || '1' }
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

/** HTTP 2xx 即视为成功，返回响应体本身 */
async function rawGet<T>(url: string, params?: Record<string, any>): Promise<T> {
  const res = await axios.get(url, { headers: rawHeaders(), params })
  return res.data as T
}

async function rawPost<T>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  const res = await axios.post(url, data, { headers: rawHeaders(), params })
  return res.data as T
}

// ═══ 优惠券（/api/erp/marketing/coupon） ═══

/** 优惠券状态：UNUSED-未使用 USED-已使用 EXPIRED-已过期 CANCELLED-已取消 */
export interface LoyaltyCoupon {
  id: number
  programId?: number
  partnerId?: number
  code?: string
  status?: string
  usedTime?: string
  usedOrderId?: number
  faceValue?: number
  balance?: number
  expirationDate?: string
  remark?: string
  createTime?: string
}

export const couponApi = {
  page(params: { partnerId?: number; status?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<LoyaltyCoupon>> {
    return request.get('/erp/marketing/coupon/page', params)
  },
  create(data: Partial<LoyaltyCoupon>): Promise<boolean> {
    return request.post('/erp/marketing/coupon', data)
  },
  update(id: number, data: Partial<LoyaltyCoupon>): Promise<boolean> {
    return request.put(`/erp/marketing/coupon/${id}`, data)
  },
  listAvailable(partnerId: number): Promise<LoyaltyCoupon[]> {
    return request.get(`/erp/marketing/coupon/available/${partnerId}`)
  },
  use(id: number): Promise<boolean> {
    return request.post(`/erp/marketing/coupon/${id}/use`)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/coupon/${id}`)
  }
}

// ═══ 会员卡/积分账户（/api/erp/marketing/card） ═══

export interface LoyaltyCard {
  id: number
  programId?: number
  partnerId?: number
  cardCode?: string
  points?: number
  totalEarned?: number
  totalRedeemed?: number
  expirationDate?: string
  isActive?: number
  remark?: string
  createTime?: string
}

export const loyaltyCardApi = {
  page(params: { memberId?: number; pageNum?: number; pageSize?: number }): Promise<PageResult<LoyaltyCard>> {
    return request.get('/erp/marketing/card/page', params)
  },
  getById(id: number): Promise<LoyaltyCard> {
    return request.get(`/erp/marketing/card/${id}`)
  },
  create(data: Partial<LoyaltyCard>): Promise<boolean> {
    return request.post('/erp/marketing/card', data)
  },
  update(id: number, data: Partial<LoyaltyCard>): Promise<boolean> {
    return request.put(`/erp/marketing/card/${id}`, data)
  },
  addPoints(id: number, points: number): Promise<boolean> {
    return request.post(`/erp/marketing/card/${id}/points/add`, null, { params: { points } })
  },
  deductPoints(id: number, points: number): Promise<boolean> {
    return request.post(`/erp/marketing/card/${id}/points/deduct`, null, { params: { points } })
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/card/${id}`)
  }
}

// ═══ 促销与忠诚程序（/api/erp/marketing/program） ═══

/** 程序类型：PROMOTION-满减 COUPON-优惠券 DISCOUNT_CODE-优惠码 LOYALTY-会员积分卡 GIFT_CARD-礼品卡 EWALLET-电子钱包 NEXT_ORDER-返券 */
export interface LoyaltyProgram {
  id: number
  programType?: string
  name?: string
  description?: string
  triggerType?: string
  startDate?: string
  endDate?: string
  isActive?: number
  maxUsage?: number
  usageCount?: number
  applyScope?: string
  pricelistId?: number
  sortOrder?: number
  remark?: string
  createTime?: string
}

export const loyaltyProgramApi = {
  page(params: { programType?: string; keyword?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<LoyaltyProgram>> {
    return request.get('/erp/marketing/program/page', params)
  },
  list(programType?: string): Promise<LoyaltyProgram[]> {
    return request.get('/erp/marketing/program/list', programType ? { programType } : {})
  },
  active(): Promise<LoyaltyProgram[]> {
    return request.get('/erp/marketing/program/active')
  },
  getById(id: number): Promise<LoyaltyProgram> {
    return request.get(`/erp/marketing/program/${id}`)
  },
  create(data: Partial<LoyaltyProgram>): Promise<boolean> {
    return request.post('/erp/marketing/program', data)
  },
  update(id: number, data: Partial<LoyaltyProgram>): Promise<boolean> {
    return request.put(`/erp/marketing/program/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/program/${id}`)
  }
}

// ═══ 营销规则（/api/erp/marketing/rules） ═══

export interface MarketingRule {
  id: number
  ruleCode?: string
  ruleName?: string
  ruleType?: string
  ruleSubtype?: string
  priority?: number
  isStackable?: number
  startTime?: string
  endTime?: string
  minOrderAmount?: number
  maxOrderAmount?: number
  minQuantity?: number
  maxQuantity?: number
  usageLimitTotal?: number
  usageLimitPerCustomer?: number
  useCount?: number
  maxDiscountAmount?: number
  status?: string
  remark?: string
  createTime?: string
}

export const marketingRuleApi = {
  page(params: { ruleType?: string; status?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<MarketingRule>> {
    return request.get('/erp/marketing/rules/page', params)
  },
  getById(id: number): Promise<MarketingRule> {
    return request.get(`/erp/marketing/rules/${id}`)
  },
  create(data: Partial<MarketingRule>): Promise<boolean> {
    return request.post('/erp/marketing/rules', data)
  },
  update(id: number, data: Partial<MarketingRule>): Promise<boolean> {
    return request.put(`/erp/marketing/rules/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/rules/${id}`)
  }
}

// ═══ 促销活动（/api/sale/promotion） ═══

/** 促销类型：discount-折扣 full_reduction-满减 gift-赠品 combo-组合 */
export interface PromotionActivity {
  id: number
  name?: string
  description?: string
  type?: string
  startTime?: string
  endTime?: string
  discountRate?: number
  minAmount?: number
  reductionAmount?: number
  giftConfig?: string
  comboConfig?: string
  customerLevels?: string[]
  productIds?: number[]
  regions?: string[]
  maxUsageCount?: number
  usageLimitPerCustomer?: number
  stackable?: boolean
  /** draft-草稿 published-已发布 expired-已过期 cancelled-已取消 */
  status?: string
  createTime?: string
  updateTime?: string
}

export const promotionApi = {
  page(params: { pageNum?: number; pageSize?: number; tenantId?: number; name?: string; status?: string; type?: string }): Promise<PageResult<PromotionActivity>> {
    return request.get('/sale/promotion/page', params)
  },
  getById(id: number): Promise<PromotionActivity> {
    return request.get(`/sale/promotion/${id}`)
  },
  create(data: Partial<PromotionActivity>): Promise<number> {
    return request.post('/sale/promotion', data)
  },
  update(id: number, data: Partial<PromotionActivity>): Promise<void> {
    return request.put(`/sale/promotion/${id}`, data)
  },
  remove(id: number): Promise<void> {
    return request.delete(`/sale/promotion/${id}`)
  },
  publish(id: number): Promise<void> {
    return request.post(`/sale/promotion/${id}/publish`)
  },
  cancel(id: number): Promise<void> {
    return request.post(`/sale/promotion/${id}/cancel`)
  },
  active(tenantId: number): Promise<PromotionActivity[]> {
    return request.get('/sale/promotion/active', { tenantId })
  }
}

// ═══ 拼团活动（/api/erp/marketing/group-buy） ═══

export interface GroupBuyActivity {
  id: number
  activityCode?: string
  activityName?: string
  productId?: number
  originalPrice?: number
  groupPrice?: number
  minGroupSize?: number
  maxGroupSize?: number
  timeLimitMinutes?: number
  quantityLimit?: number
  totalQuantity?: number
  soldQuantity?: number
  startTime?: string
  endTime?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface GroupBuyParticipant {
  id: number
  activityId?: number
  groupId?: string
  customerId?: number
  userName?: string
  quantity?: number
  orderId?: number
  orderStatus?: string
  isCreator?: number
  groupStatus?: string
  joinTime?: string
}

export const groupBuyApi = {
  page(params: { pageNum?: number; pageSize?: number }): Promise<PageResult<GroupBuyActivity>> {
    return request.get('/erp/marketing/group-buy/page', params)
  },
  getById(id: number): Promise<GroupBuyActivity> {
    return request.get(`/erp/marketing/group-buy/${id}`)
  },
  create(data: Partial<GroupBuyActivity>): Promise<boolean> {
    return request.post('/erp/marketing/group-buy', data)
  },
  update(id: number, data: Partial<GroupBuyActivity>): Promise<boolean> {
    return request.put(`/erp/marketing/group-buy/${id}`, data)
  },
  updateStatus(id: number, status: string): Promise<boolean> {
    return request.put(`/erp/marketing/group-buy/${id}/status`, null, { params: { status } })
  },
  participants(id: number): Promise<GroupBuyParticipant[]> {
    return request.get(`/erp/marketing/group-buy/${id}/participants`)
  }
}

// ═══ 秒杀场次（/api/erp/marketing/flash-sale） ═══

/** 场次状态：0草稿 1已发布 2已取消 3已结束 */
export interface FlashSale {
  id: number
  title?: string
  productId?: number
  productCode?: string
  productName?: string
  /** 秒杀价 */
  flashPrice?: number
  /** 原价 */
  originalPrice?: number
  /** 秒杀限量库存 */
  stockLimit?: number
  /** 已售数量（由参与记录累计，后端维护） */
  soldCount?: number
  startTime?: string
  endTime?: string
  /** 状态：0草稿 1已发布 2已取消 3已结束 */
  status?: number
  sort?: number
  remark?: string
  createTime?: string
}

/** 参与状态：0已参与 1已下单 2已取消 */
export interface FlashSaleParticipant {
  id: number
  sessionId?: number
  orderId?: number
  customerId?: number
  customerName?: string
  quantity?: number
  amount?: number
  /** 参与状态：0已参与 1已下单 2已取消 */
  status?: number
  createTime?: string
}

export const flashSaleApi = {
  page(params: { title?: string; status?: number; pageNum?: number; pageSize?: number }): Promise<PageResult<FlashSale>> {
    return request.get('/erp/marketing/flash-sale/page', params)
  },
  getById(id: number): Promise<FlashSale> {
    return request.get(`/erp/marketing/flash-sale/${id}`)
  },
  create(data: Partial<FlashSale>): Promise<boolean> {
    return request.post('/erp/marketing/flash-sale', data)
  },
  update(id: number, data: Partial<FlashSale>): Promise<boolean> {
    return request.put(`/erp/marketing/flash-sale/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/flash-sale/${id}`)
  },
  publish(id: number): Promise<boolean> {
    return request.post(`/erp/marketing/flash-sale/${id}/publish`)
  },
  cancel(id: number): Promise<boolean> {
    return request.post(`/erp/marketing/flash-sale/${id}/cancel`)
  },
  participants(id: number, params?: { pageNum?: number; pageSize?: number }): Promise<PageResult<FlashSaleParticipant>> {
    return request.get(`/erp/marketing/flash-sale/${id}/participants`, params)
  }
}

// ═══ 套装/套餐（/api/erp/product-kit） ═══

export interface ProductKit {
  id: number
  kitCode?: string
  kitName?: string
  productId?: number
  productCode?: string
  productName?: string
  productSpec?: string
  productUnit?: string
  kitType?: number
  kitTypeDesc?: string
  status?: number
  kitPrice?: number
  kitCost?: number
  profitRate?: number
  active?: boolean
  allowSplit?: boolean
  allowPartial?: boolean
  minQuantity?: number
  maxQuantity?: number
  description?: string
  remark?: string
  createTime?: string
  items?: any[]
}

export interface ProductKitItem {
  id?: number
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
}

export const productKitApi = {
  /** 分页（Page 无 wrapper，拦截器透传） */
  page(params: { keyword?: string; kitType?: number; status?: number; pageNum?: number; pageSize?: number }): Promise<PageResult<ProductKit>> {
    return request.get('/erp/product-kit/page', params)
  },
  /** 组件明细（数组响应，拦截器直接返回） */
  items(id: number): Promise<ProductKitItem[]> {
    return request.get(`/erp/product-kit/${id}/items`)
  },
  /** 以下端点返回单对象/void（无标准 wrapper），改用裸请求 */
  getById(id: number): Promise<ProductKit> {
    return rawGet(`/api/erp/product-kit/${id}`)
  },
  create(data: Partial<ProductKit>): Promise<ProductKit> {
    return rawPost('/api/erp/product-kit', data)
  },
  update(id: number, data: Partial<ProductKit>): Promise<ProductKit> {
    return axios.put(`/api/erp/product-kit/${id}`, data, { headers: rawHeaders() }).then(r => r.data as ProductKit)
  },
  activate(id: number): Promise<void> {
    return rawPost(`/api/erp/product-kit/${id}/activate`)
  },
  deactivate(id: number): Promise<void> {
    return rawPost(`/api/erp/product-kit/${id}/deactivate`)
  }
}

// ═══ 商品等级特价（/api/erp/product-grade-price） ═══

export interface ProductGradePrice {
  id: number
  productId?: number
  gradeCode?: string
  gradeName?: string
  priceType?: string
  basePrice?: number
  gradePrice?: number
  discountRate?: number
  discountAmount?: number
  minOrderQty?: number
  maxOrderQty?: number
  effectiveDate?: string
  expireDate?: string
  isActive?: number
  remark?: string
}

export const gradePriceApi = {
  byProduct(productId: number): Promise<ProductGradePrice[]> {
    return request.get(`/erp/product-grade-price/by-product/${productId}`)
  },
  create(data: Partial<ProductGradePrice>): Promise<boolean> {
    return request.post('/erp/product-grade-price', data)
  },
  update(id: number, data: Partial<ProductGradePrice>): Promise<boolean> {
    return request.put(`/erp/product-grade-price/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/product-grade-price/${id}`)
  }
}

// ═══ 营销活动/推广（/api/crm/marketing） ═══

/** 活动状态：0草稿 1待审批 2已审批 3已排期 4进行中 5已暂停 6已完成 7已取消 */
export interface MarketingCampaign {
  id: number
  campaignCode?: string
  campaignName?: string
  campaignType?: number
  campaignTypeDesc?: string
  campaignCategory?: number
  description?: string
  objective?: string
  startDate?: string
  endDate?: string
  status?: number
  statusDesc?: string
  budget?: number
  actualCost?: number
  expectedRevenue?: number
  actualRevenue?: number
  expectedLeads?: number
  actualLeads?: number
  expectedOrders?: number
  actualOrders?: number
  targetCustomerCount?: number
  reachedCustomerCount?: number
  convertedCustomerCount?: number
  targetAudience?: string
  targetRegion?: string
  ownerName?: string
  departmentName?: string
  roiPercentage?: number
  conversionRate?: number
  remark?: string
  createTime?: string
}

export interface CampaignCreatePayload {
  campaignName: string
  campaignType?: number
  campaignCategory?: number
  description?: string
  objective?: string
  startDate?: string
  endDate?: string
  budget?: number
  expectedRevenue?: number
  expectedLeads?: number
  expectedOpportunities?: number
  expectedOrders?: number
  targetAudience?: string
  targetRegion?: string
  remark?: string
}

export const campaignApi = {
  /** 分页（Page 无 wrapper，拦截器透传） */
  page(params: { keyword?: string; campaignType?: number; status?: number; pageNum?: number; pageSize?: number }): Promise<PageResult<MarketingCampaign>> {
    return request.get('/crm/marketing/page', params)
  },
  /** 进行中/已结束列表（数组响应，拦截器直接返回） */
  running(): Promise<MarketingCampaign[]> {
    return request.get('/crm/marketing/running')
  },
  ended(): Promise<MarketingCampaign[]> {
    return request.get('/crm/marketing/ended')
  },
  /** 以下端点返回单对象（无标准 wrapper），改用裸请求 */
  getById(id: number): Promise<MarketingCampaign> {
    return rawGet(`/api/crm/marketing/${id}`)
  },
  create(data: CampaignCreatePayload): Promise<MarketingCampaign> {
    return rawPost('/api/crm/marketing', data)
  },
  update(id: number, data: CampaignCreatePayload): Promise<MarketingCampaign> {
    return axios.put(`/api/crm/marketing/${id}`, data, { headers: rawHeaders() }).then(r => r.data as MarketingCampaign)
  },
  submit(id: number): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/approve`, null, note ? { note } : {})
  },
  reject(id: number, reason: string): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/reject`, null, { reason })
  },
  start(id: number): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/start`)
  },
  pause(id: number): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/pause`)
  },
  resume(id: number): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/resume`)
  },
  complete(id: number): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<MarketingCampaign> {
    return rawPost(`/api/crm/marketing/${id}/cancel`, null, { reason })
  }
}

// ═══ 短信配置（/api/sms，platform） ═══

export interface SmsConfig {
  id?: number
  provider?: string
  accessKey?: string
  accessSecret?: string
  signName?: string
  enabled?: boolean
}

interface SmsConfigWrapper {
  code: number
  data: SmsConfig
  message: string
}

interface SmsActionResult {
  success: boolean
  message?: string
  config?: SmsConfig
}

export const smsApi = {
  /** 获取配置（返回 Map.of(code=200,data=...) ，标准拦截器可解包） */
  async getConfig(): Promise<SmsConfig | null> {
    const res = await rawGet<SmsConfigWrapper>('/api/sms/config')
    return res?.data ?? null
  },
  /** 保存配置（无标准 wrapper，裸请求） */
  async saveConfig(data: SmsConfig): Promise<boolean> {
    const res = await rawPost<SmsActionResult>('/api/sms/config', data)
    return !!res?.success
  },
  /** 测试短信服务连通性 */
  async test(data: SmsConfig): Promise<{ success: boolean; message: string }> {
    const res = await rawPost<SmsActionResult>('/api/sms/test', data)
    return { success: !!res?.success, message: res?.message || (res?.success ? '测试成功' : '测试失败') }
  }
}

// ═══ 热门搜索词（/api/search/hot） ═══

export const searchHotApi = {
  /** 获取热门搜索词（无标准 wrapper，裸请求） */
  async hot(limit = 20): Promise<{ hotSearches: string[]; count: number }> {
    const res = await rawGet<{ hotSearches?: string[]; count?: number }>('/api/search/hot', { limit })
    return { hotSearches: res?.hotSearches || [], count: res?.count || 0 }
  }
}

// ═══════════════════════════════════════════════════════════════════
// 以下为批次 0.3 新增（商城预售/弹窗广告/加价购）
// ═══════════════════════════════════════════════════════════════════

// ── 预售活动（/api/erp/marketing/presale） ──

export interface Presale {
  id: number
  tenantId: number
  activityName: string
  productId: number
  productName?: string
  productCode?: string
  depositAmount: number
  finalAmount: number
  startTime: string
  endTime: string
  depositEndTime?: string
  finalStartTime?: string
  stockLimit: number
  soldCount: number
  status: number
  sort: number
  remark?: string
  createTime: string
  updateTime: string
}

/** 预售状态：0=未开始 1=进行中 2=已结束 3=已取消 */
export const PRESALE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '未开始', color: 'default' },
  1: { text: '进行中', color: 'blue' },
  2: { text: '已结束', color: 'green' },
  3: { text: '已取消', color: 'red' },
}

export const presaleApi = {
  page(params: any) { return request.get('/erp/marketing/presale/page', params) },
  getById(id: number) { return request.get(`/erp/marketing/presale/${id}`) },
  create(data: Partial<Presale>) { return request.post('/erp/marketing/presale', data) },
  update(id: number, data: Partial<Presale>) { return request.put(`/erp/marketing/presale/${id}`, data) },
  remove(id: number) { return request.delete(`/erp/marketing/presale/${id}`) },
  publish(id: number) { return request.post(`/erp/marketing/presale/${id}/publish`) },
  cancel(id: number) { return request.post(`/erp/marketing/presale/${id}/cancel`) },
}

// ── 弹窗广告（/api/erp/mall/admin/popup-ad） ──

export interface PopupAd {
  id: number
  tenantId: number
  title: string
  imageUrl?: string
  linkUrl?: string
  showType: string
  targetUser: string
  startTime?: string
  endTime?: string
  sort: number
  status: number
  remark?: string
  createTime: string
  updateTime: string
}

/** 弹窗广告状态：0=草稿 1=投放中 2=已结束 3=已下架 */
export const POPUP_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '投放中', color: 'green' },
  2: { text: '已结束', color: 'orange' },
  3: { text: '已下架', color: 'red' },
}

export const SHOW_TYPE_MAP: Record<string, string> = {
  once: '仅首次',
  everyday: '每日',
  once_per_session: '每次会话',
}

export const TARGET_USER_MAP: Record<string, string> = {
  all: '全部用户',
  member: '会员',
  new: '新用户',
}

export const popupAdApi = {
  page(params: any) { return request.get('/erp/mall/admin/popup-ad/page', params) },
  getById(id: number) { return request.get(`/erp/mall/admin/popup-ad/${id}`) },
  create(data: Partial<PopupAd>) { return request.post('/erp/mall/admin/popup-ad', data) },
  update(id: number, data: Partial<PopupAd>) { return request.put(`/erp/mall/admin/popup-ad/${id}`, data) },
  remove(id: number) { return request.delete(`/erp/mall/admin/popup-ad/${id}`) },
  publish(id: number) { return request.post(`/erp/mall/admin/popup-ad/${id}/publish`) },
  offline(id: number) { return request.post(`/erp/mall/admin/popup-ad/${id}/offline`) },
}

// ── 加价购规则（/api/erp/marketing/addon-rule） ──

export interface AddonRule {
  id: number
  tenantId: number
  ruleName: string
  mainProductId: number
  mainProductName?: string
  addonProductId: number
  addonProductName?: string
  addonPrice: number
  maxPerOrder: number
  startTime: string
  endTime: string
  status: number
  sort: number
  remark?: string
  createTime: string
  updateTime: string
}

export const ADDON_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '停用', color: 'default' },
  1: { text: '启用', color: 'green' },
}

export const addonRuleApi = {
  page(params: any) { return request.get('/erp/marketing/addon-rule/page', params) },
  getById(id: number) { return request.get(`/erp/marketing/addon-rule/${id}`) },
  create(data: Partial<AddonRule>) { return request.post('/erp/marketing/addon-rule', data) },
  update(id: number, data: Partial<AddonRule>) { return request.put(`/erp/marketing/addon-rule/${id}`, data) },
  remove(id: number) { return request.delete(`/erp/marketing/addon-rule/${id}`) },
  enable(id: number) { return request.post(`/erp/marketing/addon-rule/${id}/enable`) },
  disable(id: number) { return request.post(`/erp/marketing/addon-rule/${id}/disable`) },
}

// ── 积分记录 ──
export const pointsApi = {
  page(params: Record<string, any>): Promise<any> {
    return request.get('/erp/marketing/points/page', params)
  }
}
