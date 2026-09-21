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
  const headers: Record<string, string> = {}
  // 仅在拿到真实租户时才发送（不再回落 '1'，理由同 admin.ts 的 adminHeaders）
  const tenantId = localStorage.getItem('tenantId')
  if (tenantId) headers.tenantId = tenantId
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
  },
  /** 「拼团活动」Tab（9 列，含开团/成功团个数） */
  activityPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/group-buy/activity/page', params)
  },
  /** 「拼团订单」Tab（11 列） */
  orderPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/group-buy/order/page', params)
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
  },
  /** 复制套餐（对标「套餐」页行级「复制」） */
  copy(id: number): Promise<any> {
    return rawPost(`/api/erp/product-kit/${id}/copy`)
  },
  remove(id: number): Promise<void> {
    return axios.delete(`/api/erp/product-kit/${id}`, { headers: rawHeaders() }).then(r => r.data)
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
  /** 「预售订单」Tab（8 列） */
  orderPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/presale/order/page', params)
  },
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

// ═══ 会员设置（/api/erp/marketing/member-config） ═══
// 营销 → 会员中心 → 会员设置（80302）：会员体系参数单行配置（对标 ql361 三分区设置单）
// 布尔开关一律 INTEGER 0/1；consume_points_mode：BY_AMOUNT=按销售金额 / BY_PRODUCT=按不同商品累计
// points_round_rule：ROUND=四舍五入 / FLOOR=舍去 / CEIL=进位

export interface MemberConfig {
  id?: number
  tenantId?: number
  /** 客户 | 会员管理 */
  memberEnabled?: number
  /** 会员自动升级 */
  autoUpgradeEnabled?: number
  /** 积分奖励区块开关 */
  pointsRewardEnabled?: number
  /** 注册初始积分 */
  registerPoints?: number
  /** 会员生日倍积分 */
  birthdayMultiple?: number
  /** 消费积分区块开关 */
  consumePointsEnabled?: number
  /** 消费积分算法：BY_AMOUNT / BY_PRODUCT */
  consumePointsMode?: string
  /** 按销售金额积分：N 元=1 分 */
  amountPerPoint?: number
  /** 按折扣积分 */
  pointsByDiscount?: number
  /** 积分取整规则：ROUND / FLOOR / CEIL */
  pointsRoundRule?: string
  /** 积分应用场景-线下开单 */
  applySceneOffline?: number
  /** 积分应用场景-微商城 */
  applySceneMall?: number
  /** 积分有效期（月，0/NULL=永不过期） */
  pointsValidMonths?: number
  /** 到期前提醒天数 */
  pointsExpireRemindDays?: number
  /** 签到积分区块开关 */
  signinEnabled?: number
  signinFirstPoints?: number
  signinIncrement?: number
  signinMaxPoints?: number
  /** 积分抵现区块开关 */
  cashDeductEnabled?: number
  /** 抵现比例：N 积分=1 元 */
  pointsPerYuan?: number
  /** 单笔订单最高可抵扣金额百分比 */
  maxDeductPercent?: number
  createTime?: string
  updateTime?: string
}

export const memberConfigApi = {
  get(): Promise<MemberConfig> {
    return request.get('/erp/marketing/member-config')
  },
  save(data: Partial<MemberConfig>): Promise<MemberConfig> {
    return request.put('/erp/marketing/member-config', data)
  }
}

// ═══════════════════════════════════════════════════════════════════
// 储值卡（P3，菜单 80304）—— ⚠️ 本系统建模页，ql361 无对应页
// ⚠️ 合规：单用途预付卡须遵守《单用途商业预付卡管理办法》与法释〔2025〕4 号，
//    必须提供退款入口与告知，不得设计为「只进不出」。
// ═══════════════════════════════════════════════════════════════════

export interface StoredCard {
  id: number
  cardNo: string
  partnerId?: number
  partnerName?: string
  /** STORED 储值卡 / GIFT 礼品卡 */
  cardType?: string
  faceValue?: number
  balance?: number
  totalRecharge?: number
  totalConsume?: number
  totalBonus?: number
  /** ACTIVE 正常 / FROZEN 已冻结 / USED_UP 已用尽 / EXPIRED 已过期 / REFUNDED 已退卡 */
  status?: string
  issueTime?: string
  expireTime?: string
  remark?: string
  /** 默认结算账户：CASH 库存现金 / BANK 银行存款（决定记账凭证的借方科目） */
  settleAccount?: string
}

export const STORED_CARD_TYPE_MAP: Record<string, string> = {
  STORED: '储值卡',
  GIFT: '礼品卡',
}

export const STORED_CARD_STATUS_MAP: Record<string, { text: string; color: string }> = {
  ACTIVE: { text: '正常', color: 'green' },
  FROZEN: { text: '已冻结', color: 'orange' },
  USED_UP: { text: '已用尽', color: 'default' },
  EXPIRED: { text: '已过期', color: 'red' },
  REFUNDED: { text: '已退卡', color: 'purple' },
}

export interface StoredCardFlow {
  id: number
  cardId?: number
  cardNo?: string
  partnerId?: number
  /** ISSUE 开卡 / RECHARGE 充值 / BONUS 赠送 / CONSUME 消费 / REFUND 退款 / ADJUST 调整 */
  flowType?: string
  amount?: number
  bonusAmount?: number
  balanceAfter?: number
  sourceBillNo?: string
  /** CASH 库存现金 / BANK 银行存款（本次资金实际走的账户） */
  settleAccount?: string
  /** 已生成的记账凭证号（空=未记账，如赠送/调整） */
  voucherNo?: string
  handlerName?: string
  remark?: string
  createTime?: string
}

/** 结算账户选项：决定记账凭证借/贷方用 1001 库存现金 还是 1002 银行存款 */
export const SETTLE_ACCOUNT_OPTIONS = [
  { value: 'BANK', label: '银行存款' },
  { value: 'CASH', label: '库存现金' },
]

export const STORED_FLOW_TYPE_MAP: Record<string, string> = {
  ISSUE: '开卡',
  RECHARGE: '充值',
  BONUS: '赠送',
  CONSUME: '消费',
  REFUND: '退款',
  ADJUST: '调整',
}

export const storedCardApi = {
  page(params: Record<string, any>): Promise<PageResult<StoredCard>> {
    return request.get('/erp/marketing/stored-card/page', params)
  },
  getById(id: number): Promise<StoredCard> {
    return request.get(`/erp/marketing/stored-card/${id}`)
  },
  byPartner(partnerId: number): Promise<StoredCard[]> {
    return request.get(`/erp/marketing/stored-card/by-partner/${partnerId}`)
  },
  /** 开卡（面值 + 可选赠送） */
  issue(data: { card: Partial<StoredCard>; bonusAmount?: number; remark?: string }): Promise<number> {
    return request.post('/erp/marketing/stored-card', data)
  },
  recharge(id: number, data: { amount: number; bonusAmount?: number; sourceBillNo?: string; settleAccount?: string }): Promise<StoredCard> {
    return request.post(`/erp/marketing/stored-card/${id}/recharge`, data)
  },
  consume(id: number, data: { amount: number; sourceBillNo?: string }): Promise<StoredCard> {
    return request.post(`/erp/marketing/stored-card/${id}/consume`, data)
  },
  /** 退款（合规入口：部分/全额） */
  refund(id: number, data: { amount: number; remark?: string; settleAccount?: string }): Promise<StoredCard> {
    return request.post(`/erp/marketing/stored-card/${id}/refund`, data)
  },
  changeStatus(id: number, status: string, remark?: string): Promise<StoredCard> {
    return request.post(`/erp/marketing/stored-card/${id}/status`, null, { params: { status, remark } })
  },
  stat(): Promise<{
    cardCount: number; activeCount: number; balanceTotal: number
    rechargeTotal: number; consumeTotal: number; bonusTotal: number
  }> {
    return request.get('/erp/marketing/stored-card/stat')
  },
  flowPage(params: Record<string, any>): Promise<PageResult<StoredCardFlow>> {
    return request.get('/erp/marketing/stored-card/flow/page', params)
  }
}

// ═══════════════════════════════════════════════════════════════════
// 营销自动化（P2，菜单 80303）—— ⚠️ 本系统建模页，ql361 无对应页
// ═══════════════════════════════════════════════════════════════════

export interface AutoCampaign {
  id: number
  name: string
  /** 触发点：NEW_CUSTOMER / BIRTHDAY / SLEEPING / REPURCHASE / POINTS_EXPIRING / CARD_EXPIRING */
  triggerType?: string
  /** 触发参数：沉睡天数 / 生日提前天数 / 复购周期天数 / 到期前天数 */
  triggerDays?: number
  /** 动作：COUPON 发优惠券 / SMS 发短信 / POINTS 赠积分 */
  actionType?: string
  couponTemplateId?: number
  smsTemplateId?: number
  smsContent?: string
  pointsValue?: number
  freqDays?: number
  freqCount?: number
  oncePerMember?: number
  status?: number
  lastRunTime?: string
  lastRunCount?: number
  lastRunSuccess?: number
  remark?: string
  createTime?: string
}

export const AUTO_TRIGGER_MAP: Record<string, string> = {
  NEW_CUSTOMER: '新客首单后',
  BIRTHDAY: '会员生日',
  SLEEPING: '沉睡未消费',
  REPURCHASE: '复购周期到期',
  POINTS_EXPIRING: '积分即将过期',
  CARD_EXPIRING: '会员卡到期',
}

export const AUTO_ACTION_MAP: Record<string, string> = {
  COUPON: '发优惠券',
  SMS: '发短信',
  POINTS: '赠积分',
}

/** 触发点的参数语义（页面动态 label 与提示用） */
export const AUTO_TRIGGER_PARAM_LABEL: Record<string, string> = {
  NEW_CUSTOMER: '首单后多少天内',
  BIRTHDAY: '生日提前多少天',
  SLEEPING: '超过多少天未消费',
  REPURCHASE: '复购周期（天）',
  POINTS_EXPIRING: '到期前多少天',
  CARD_EXPIRING: '到期前多少天',
}

export interface AutoCampaignCandidate {
  partnerId?: number
  partyCode?: string
  partyName?: string
  memberName?: string
  memberCardNo?: string
  mobile?: string
  triggerNote?: string
  keyDate?: string
}

export interface AutoCampaignLog {
  id: number
  campaignId?: number
  campaignName?: string
  triggerType?: string
  actionType?: string
  partnerId?: number
  memberName?: string
  mobile?: string
  /** SUCCESS / SKIPPED / FAILED */
  result?: string
  resultMsg?: string
  batchNo?: string
  triggerNote?: string
  createTime?: string
}

export const autoCampaignApi = {
  page(params: Record<string, any>): Promise<PageResult<AutoCampaign>> {
    return request.get('/erp/marketing/auto-campaign/page', params)
  },
  getById(id: number): Promise<AutoCampaign> {
    return request.get(`/erp/marketing/auto-campaign/${id}`)
  },
  create(data: Partial<AutoCampaign>): Promise<number> {
    return request.post('/erp/marketing/auto-campaign', data)
  },
  update(id: number, data: Partial<AutoCampaign>): Promise<boolean> {
    return request.put(`/erp/marketing/auto-campaign/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/auto-campaign/${id}`)
  },
  changeStatus(id: number, status: number): Promise<boolean> {
    return request.post(`/erp/marketing/auto-campaign/${id}/status`, null, { params: { status } })
  },
  /** 候选会员预览（不执行动作） */
  candidates(id: number, limit = 200): Promise<AutoCampaignCandidate[]> {
    return request.get(`/erp/marketing/auto-campaign/${id}/candidates`, { limit })
  },
  /** 立即执行一次 */
  run(id: number, limit = 500): Promise<{
    campaignId: number; campaignName: string; candidateCount: number
    success: number; skipped: number; failed: number
  }> {
    return request.post(`/erp/marketing/auto-campaign/${id}/run`, null, { params: { limit } })
  },
  /** 执行全部启用规则（定时任务同入口） */
  runAll(limitPerCampaign = 500): Promise<{ campaignCount: number; success: number; failedCampaigns: number }> {
    return request.post('/erp/marketing/auto-campaign/run-all', null, { params: { limitPerCampaign } })
  },
  stat(): Promise<{ total: number; enabled: number; recentSuccess: number }> {
    return request.get('/erp/marketing/auto-campaign/stat')
  },
  logPage(params: Record<string, any>): Promise<PageResult<AutoCampaignLog>> {
    return request.get('/erp/marketing/auto-campaign/log/page', params)
  }
}

// ═══════════════════════════════════════════════════════════════════
// 会员等级规则（P1）：等级档案在资料域 /erp/member-level（已补门槛字段），
// 评估与升降级执行在营销域 /erp/marketing/member-level
// ═══════════════════════════════════════════════════════════════════

export interface MemberLevelRule {
  id: number
  levelName: string
  /** 会员折扣率（100 = 不打折） */
  discountRate?: number
  sortOrder?: number
  status?: number
  /** 升级门槛：累计消费额达到该值即升到本级 */
  upgradeAmount?: number
  /** 升级门槛：成长值/累计积分达到该值即升到本级 */
  upgradePoints?: number
  /** 保级周期（月） */
  keepMonths?: number
  /** 是否默认等级（1=新会员初始等级） */
  isDefault?: number
  remark?: string
}

export interface MemberLevelChangeRow {
  partnerId: number
  partyCode?: string
  partyName?: string
  currentLevel?: string
  targetLevel?: string
  /** UPGRADE 升级 / DOWNGRADE 降级 / KEEP 不变 / INIT 首次定级 */
  action: string
  totalConsume?: number
  points?: number
  reason?: string
}

export const memberLevelRuleApi = {
  rules(): Promise<MemberLevelRule[]> {
    return request.get('/erp/marketing/member-level/rules')
  },
  /** 评估（dry-run：只算不改） */
  evaluate(limit = 500): Promise<{
    total: number; upgrade: number; downgrade: number; init: number
    changes: MemberLevelChangeRow[]; autoUpgradeEnabled: boolean
  }> {
    return request.post(`/erp/marketing/member-level/evaluate?limit=${limit}`)
  },
  /** 执行升降级（受《会员设置》「会员自动升级」开关门控） */
  apply(limit = 500, force = false): Promise<{ evaluated: number; applied: number }> {
    return request.post(`/erp/marketing/member-level/apply?limit=${limit}&force=${force}`)
  },
  /** 等级档案 CRUD（资料域端点） */
  create(data: Partial<MemberLevelRule>): Promise<any> {
    return request.post('/erp/member-level', data)
  },
  update(id: number, data: Partial<MemberLevelRule>): Promise<boolean> {
    return request.put(`/erp/member-level/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/member-level/${id}`)
  }
}

// ═══════════════════════════════════════════════════════════════════
// 会员积分台账（P1）：批次（FIFO + 到期）/ 流水 / 过期执行
// ═══════════════════════════════════════════════════════════════════

export interface PointsBatch {
  id: number
  memberCardNo?: string
  partnerId?: number
  earnedPoints?: number
  remainingPoints?: number
  earnedTime?: string
  expireTime?: string
  source?: string
  sourceBillNo?: string
  /** ACTIVE 有效 / EXHAUSTED 已用完 / EXPIRED 已过期 */
  status?: string
  remark?: string
}

export interface PointsJournalRow {
  id: number
  memberCardNo?: string
  /** EARN 获得 / USE 使用 / EXPIRE 过期 / ADJUST 调整 */
  changeType?: string
  changePoints?: number
  balanceAfter?: number
  sourceBillNo?: string
  remark?: string
  createTime?: string
}

export const POINTS_CHANGE_TYPE_MAP: Record<string, string> = {
  EARN: '积分获得',
  USE: '积分使用',
  EXPIRE: '积分过期',
  ADJUST: '积分调整'
}

export const pointsLedgerApi = {
  /** 某会员的积分批次（按到期时间升序） */
  batches(memberCardNo: string): Promise<PointsBatch[]> {
    return request.get('/erp/marketing/points-ledger/batch/list', { memberCardNo })
  },
  available(memberCardNo: string): Promise<number> {
    return request.get('/erp/marketing/points-ledger/available', { memberCardNo })
  },
  journalPage(params: Record<string, any>): Promise<PageResult<PointsJournalRow>> {
    return request.get('/erp/marketing/points-ledger/journal/page', params)
  },
  /** 执行积分过期（把到期批次剩余清零并写 EXPIRE 流水） */
  expire(asOf?: string): Promise<{ memberCount: number; expiredPoints: number }> {
    return request.post('/erp/marketing/points-ledger/expire', null, { params: { asOf } })
  },
  /** 近 N 天到期且仍有剩余的批次（到期提醒数据源） */
  expiringSoon(days = 30): Promise<PointsBatch[]> {
    return request.get('/erp/marketing/points-ledger/expiring-soon', { days })
  }
}

// ═══ 商品级积分系数（/api/erp/marketing/product-points-rule） ═══
// 会员设置 →「按不同商品累计积分」→「详细设置」（⚠️ 本系统建模：对标弹窗明细未实测）

export interface ProductPointsRule {
  id?: number
  productId?: number
  productCode?: string
  productName?: string
  /** 积分系数：每 1 元销售金额累计的积分数 */
  pointsCoefficient?: number
  status?: number
  remark?: string
}

export const productPointsRuleApi = {
  list(keyword?: string): Promise<ProductPointsRule[]> {
    return request.get('/erp/marketing/product-points-rule/list', keyword ? { keyword } : {})
  },
  create(data: Partial<ProductPointsRule>): Promise<boolean> {
    return request.post('/erp/marketing/product-points-rule', data)
  },
  update(id: number, data: Partial<ProductPointsRule>): Promise<boolean> {
    return request.put(`/erp/marketing/product-points-rule/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/product-points-rule/${id}`)
  }
}

// ═══════════════════════════════════════════════════════════════════
// 营销模块金标准 API（对标 ql361「营销」域 17 页，2026-09-18）
// ═══════════════════════════════════════════════════════════════════

// ── 会员管理（80300，复用客户主数据端点 /erp/md/customer/member/page） ──

/** 会员管理行（对标 16 列，字段取自客户主数据 biz_party 的会员扩展列） */
export interface MemberRow {
  id: number
  partyCode?: string
  partyName?: string
  phone?: string
  remark?: string
  defaultHandlerId?: number
  defaultHandlerName?: string
  lastTradeTime?: string
  categoryId?: number
  memberName?: string
  memberCardNo?: string
  memberLevel?: string
  memberCardStatus?: string
  memberCardStatusDesc?: string
  memberValidStart?: string
  memberValidEnd?: string
  birthday?: string
  points?: number
  memberInitialPoints?: number
  memberTotalConsume?: number
  memberIssueTime?: string
}

export const memberManageApi = {
  page(params: Record<string, any>): Promise<PageResult<MemberRow>> {
    return request.get('/erp/md/customer/member/page', params)
  },
  /** 积分明细（逐笔积分流水，与销售订单「会员信息」Tab 同源） */
  pointsPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/points/page', params)
  }
}

// ── 积分兑换目录（80301，/erp/marketing/points-exchange） ──

export interface PointsExchangeRow {
  id: number
  productId: number
  productName?: string
  productCode?: string
  unit?: string
  exchangePoints?: number
  spec?: string
  model?: string
  origin?: string
  presetPurchasePrice?: number
  referenceCost?: number
  recentPurchasePrice?: number
  wholesalePrice?: number
  retailPrice?: number
  minSalePrice?: number
  status?: number
  sort?: number
  remark?: string
}

export const pointsExchangeApi = {
  page(params: Record<string, any>): Promise<PageResult<PointsExchangeRow>> {
    return request.get('/erp/marketing/points-exchange/page', params)
  },
  create(data: Partial<PointsExchangeRow>): Promise<boolean> {
    return request.post('/erp/marketing/points-exchange', data)
  },
  batchCreate(list: Partial<PointsExchangeRow>[]): Promise<number> {
    return request.post('/erp/marketing/points-exchange/batch', list)
  },
  update(id: number, data: Partial<PointsExchangeRow>): Promise<boolean> {
    return request.put(`/erp/marketing/points-exchange/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/points-exchange/${id}`)
  }
}

// ── 促销活动（80312 商品促销 / 80313 整单促销 / 80314 特价） ──

/** 促销方式（= 页间判别） */
export type PromoActivityType = 'PRODUCT' | 'ORDER' | 'SPECIAL_PRICE'

export interface PromoActivity {
  id: number
  name: string
  description?: string
  type: PromoActivityType | string
  startTime?: string
  endTime?: string
  productIds?: string
  comboPromo?: number
  promoType?: string
  promoMode?: string
  promoScope?: string
  customerLevels?: string
  customerIds?: string
  status?: string
  creatorName?: string
  createTime?: string

  // ── 促销引擎结构化配置（V11.378.0）：缺这些字段活动「配了不生效」 ──
  /** 折扣率（0-1；也兼容传 90 表示 9 折） */
  discountRate?: number
  /** 门槛金额（满减用） */
  minAmount?: number
  /** 满减金额 */
  reductionAmount?: number
  /** 优先级（数值越大越先算；建议唯一） */
  priority?: number
  /** 叠加策略：STACK 可叠加 / EXCLUSIVE 独占 */
  stackPolicy?: string
  /** 本单最大优惠（封顶） */
  maxDiscountAmount?: number
  /** 特价单价（促销方式=特价 时使用） */
  promoPrice?: number
  /** 活动总次数上限 / 已用次数 / 每客户次数上限 */
  quotaTotal?: number
  quotaUsed?: number
  quotaPerCustomer?: number
}

// ── 促销引擎（/erp/marketing/promotion） ──

export interface PromotionCartLine {
  lineNo?: number
  productId?: number
  productName?: string
  categoryId?: number
  quantity?: number
  unitPrice?: number
}

export interface PromotionRequest {
  tenantId?: number
  customerId?: number
  orderDate?: string
  /** OFFLINE 线下开单 / MALL 商城 */
  channel?: string
  lines: PromotionCartLine[]
  couponIds?: number[]
}

export interface PromotionAllocation {
  promotionId?: number
  promotionName?: string
  promoMode?: string
  scopeType?: string
  lineNo?: number
  productId?: number
  discountAmount?: number
  couponId?: number
  couponCode?: string
  giftProductId?: number
  giftQuantity?: number
  remark?: string
}

export interface PromotionResult {
  promoDiscount: number
  couponDiscount: number
  appliedPromotionIds: number[]
  appliedCouponIds: number[]
  allocations: PromotionAllocation[]
  /** 已生效活动的计算说明 */
  notes: string[]
  /** 未生效原因（未达门槛 / 已被更高优先级占用 / 次数超限 …） */
  skipped: string[]
}

export const promotionEngineApi = {
  /** 试算订单可享受的促销与券优惠（只读：不核销、不累加次数） */
  calc(data: PromotionRequest): Promise<PromotionResult> {
    return request.post('/erp/marketing/promotion/calc', data)
  }
}

/** 促销状态：published 促销中 / draft 未开始 / expired 已结束 / cancelled 已停用 */
export const PROMO_STATUS_MAP: Record<string, { text: string; color: string }> = {
  published: { text: '促销中', color: 'green' },
  draft: { text: '未开始', color: 'default' },
  expired: { text: '已结束', color: 'orange' },
  cancelled: { text: '已停用', color: 'red' },
}

export const promoActivityApi = {
  page(params: Record<string, any>): Promise<PageResult<PromoActivity>> {
    return request.get('/erp/marketing/promotion-activity/page', params)
  },
  getById(id: number): Promise<PromoActivity> {
    return request.get(`/erp/marketing/promotion-activity/${id}`)
  },
  create(data: Partial<PromoActivity>): Promise<number> {
    return request.post('/erp/marketing/promotion-activity', data)
  },
  update(id: number, data: Partial<PromoActivity>): Promise<boolean> {
    return request.put(`/erp/marketing/promotion-activity/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/promotion-activity/${id}`)
  },
  changeStatus(id: number, status: string): Promise<boolean> {
    return request.post(`/erp/marketing/promotion-activity/${id}/status`, null, { params: { status } })
  },
  products(id: number): Promise<any[]> {
    return request.get(`/erp/marketing/promotion-activity/${id}/products`)
  },
  customers(id: number): Promise<any[]> {
    return request.get(`/erp/marketing/promotion-activity/${id}/customers`)
  }
}

// ── 优惠券（80311，/erp/marketing/coupon-template） ──

export interface CouponTemplate {
  id: number
  couponName: string
  openReceive?: number
  couponType?: string
  useRule?: string
  faceValue?: number
  totalCount?: number
  receivedCount?: number
  usedCount?: number
  /** 未领取（派生：总数 - 已领取（未使用） - 已使用） */
  remainingCount?: number
  customerScope?: string
  startTime?: string
  endTime?: string
  status?: string
  mallEnabled?: number
  offlineEnabled?: number
  remark?: string
}

/** 优惠券类型 */
export const COUPON_TYPE_MAP: Record<string, string> = {
  CASH: '现金券',
  DISCOUNT: '折扣券',
  FULL_CUT: '满减券'
}

export const COUPON_STATUS_MAP: Record<string, { text: string; color: string }> = {
  NORMAL: { text: '正常', color: 'green' },
  VOID: { text: '已作废', color: 'red' }
}

export interface CouponRecordRow {
  id: number
  partnerName?: string
  contactName?: string
  contactPhone?: string
  couponName?: string
  couponType?: string
  useRule?: string
  faceValue?: number
  receiveStatus?: string
  billNo?: string
  status?: string
  receiveTime?: string
  usedTime?: string
  sourceBillNo?: string
}

export const couponTemplateApi = {
  page(params: Record<string, any>): Promise<PageResult<CouponTemplate>> {
    return request.get('/erp/marketing/coupon-template/page', params)
  },
  getById(id: number): Promise<CouponTemplate> {
    return request.get(`/erp/marketing/coupon-template/${id}`)
  },
  create(data: Partial<CouponTemplate>): Promise<CouponTemplate> {
    return request.post('/erp/marketing/coupon-template', data)
  },
  update(id: number, data: Partial<CouponTemplate>): Promise<CouponTemplate> {
    return request.put(`/erp/marketing/coupon-template/${id}`, data)
  },
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/coupon-template/${id}`)
  },
  voidTemplate(id: number): Promise<boolean> {
    return request.post(`/erp/marketing/coupon-template/${id}/void`)
  },
  issue(id: number, data: { partnerIds: number[]; quantityPerPartner?: number; sourceBillNo?: string }): Promise<number> {
    return request.post(`/erp/marketing/coupon-template/${id}/issue`, data)
  },
  customers(id: number): Promise<any[]> {
    return request.get(`/erp/marketing/coupon-template/${id}/customers`)
  },
  saveCustomers(id: number, list: any[]): Promise<boolean> {
    return request.put(`/erp/marketing/coupon-template/${id}/customers`, list)
  },
  recordPage(params: Record<string, any>): Promise<PageResult<CouponRecordRow>> {
    return request.get('/erp/marketing/coupon-template/record/page', params)
  },
  /** 核销单张券（后台补录：置为已使用并记录单据号） */
  redeem(couponId: number, orderId?: number, orderNo?: string): Promise<boolean> {
    return request.post(`/erp/marketing/coupon-template/record/${couponId}/redeem`, null,
      { params: { orderId, orderNo } })
  },
  /** 作废单张券（仅「已领取未使用」可作废） */
  voidCoupon(couponId: number): Promise<boolean> {
    return request.post(`/erp/marketing/coupon-template/record/${couponId}/void`)
  }
}

// ── 发短信（80310，/erp/marketing/sms） ──

export interface SmsSetting {
  id?: number
  signName?: string
  quotaTotal?: number
  quotaUsed?: number
  /** 剩余短信条数（派生） */
  quotaRemain?: number
  /** 合规：允许发送起始小时（默认 8） */
  sendStartHour?: number
  /** 合规：允许发送截止小时（默认 21） */
  sendEndHour?: number
  /** 合规：频控窗口天数（默认 7） */
  freqLimitDays?: number
  /** 合规：窗口内同一号码最多条数（默认 3） */
  freqLimitCount?: number
}

export interface SmsTemplate {
  id?: number
  templateTitle: string
  templateContent?: string
  smsType?: string
  status?: number
  updateTime?: string
}

export interface SmsHistoryRow {
  id: number
  receiverName?: string
  mobile?: string
  sendTime?: string
  handlerName?: string
  content?: string
  sendStatus?: string
  failReason?: string
}

export const smsMarketingApi = {
  getSetting(): Promise<SmsSetting> {
    return request.get('/erp/marketing/sms/setting')
  },
  saveSetting(data: Partial<SmsSetting>): Promise<SmsSetting> {
    return request.put('/erp/marketing/sms/setting', data)
  },
  templatePage(params: Record<string, any>): Promise<PageResult<SmsTemplate>> {
    return request.get('/erp/marketing/sms/template/page', params)
  },
  templateList(smsType?: string): Promise<SmsTemplate[]> {
    return request.get('/erp/marketing/sms/template/list', smsType ? { smsType } : {})
  },
  createTemplate(data: Partial<SmsTemplate>): Promise<boolean> {
    return request.post('/erp/marketing/sms/template', data)
  },
  updateTemplate(id: number, data: Partial<SmsTemplate>): Promise<boolean> {
    return request.put(`/erp/marketing/sms/template/${id}`, data)
  },
  removeTemplate(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/sms/template/${id}`)
  },
  historyPage(params: Record<string, any>): Promise<PageResult<SmsHistoryRow>> {
    return request.get('/erp/marketing/sms/history/page', params)
  },
  // ── 合规四件套：退订名单 / 同意留痕 ──
  optOutPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/sms/opt-out/page', params)
  },
  addOptOut(data: { mobile: string; receiverName?: string; source?: string; remark?: string }): Promise<boolean> {
    return request.post('/erp/marketing/sms/opt-out', data)
  },
  removeOptOut(id: number): Promise<boolean> {
    return request.delete(`/erp/marketing/sms/opt-out/${id}`)
  },
  consentPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/sms/consent/page', params)
  },
  send(data: { partnerIds: number[]; content: string; signName: string; smsType: string; agreed: boolean }):
    Promise<{ batchNo: string; sentCount: number; skippedCount: number; remainQuota: number }> {
    return request.post('/erp/marketing/sms/send', data)
  }
}

// ── 推广分享（80330 我要推广 / 80331 推广历史查询，/erp/marketing/share） ──

export interface ShareRecord {
  id: number
  shareType?: string
  targetId?: number
  targetName?: string
  shareSummary?: string
  sharerId?: number
  sharerName?: string
  shareTime?: string
  viewCount?: number
  viewerCount?: number
  receiveCount?: number
  orderUserCount?: number
  orderCount?: number
  orderAmount?: number
}

export interface ShareSummaryRow {
  targetId: number
  lastShareTime?: string
  shareCount?: number
  viewCount?: number
  viewerCount?: number
  receiveCount?: number
}

export const SHARE_TYPE_MAP: Record<string, string> = {
  PRODUCT: '商品',
  COUPON: '优惠券',
  PROMOTION: '促销',
  GROUP_BUY: '拼团',
  FLASH_SALE: '秒杀'
}

export const promoteApi = {
  /** 「商品」Tab 物料列表（含库存 / 最近销售时间 / 分享统计） */
  productPage(params: Record<string, any>): Promise<PageResult<any>> {
    return request.get('/erp/marketing/promote/product/page', params)
  }
}

export const shareApi = {
  page(params: Record<string, any>): Promise<PageResult<ShareRecord>> {
    return request.get('/erp/marketing/share/page', params)
  },
  myPage(params: Record<string, any>): Promise<PageResult<ShareRecord>> {
    return request.get('/erp/marketing/share/my/page', params)
  },
  summary(shareType: string, mine?: boolean): Promise<ShareSummaryRow[]> {
    return request.get('/erp/marketing/share/summary', { shareType, mine })
  },
  create(data: Partial<ShareRecord>): Promise<number> {
    return request.post('/erp/marketing/share', data)
  }
}
