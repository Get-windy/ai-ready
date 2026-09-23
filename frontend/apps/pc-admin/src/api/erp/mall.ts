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
/**
 * 商城配置（表 `tenant_shop_config`，后端实体 `cn.aiedge.erp.b2b.model.ShopConfig`）
 *
 * ⚠️ 三页共用同一行：`mall/basic-config`、`mall/shop-config`、`mall/freight-config` 共用本实体
 * 与同一对端点。后端 `updateConfig` 走 `updateById`，MyBatis-Plus 默认**忽略 null 字段**，
 * 因此各页只提交自己维护的字段即可，不会覆盖其它页的字段（保存时仍以 `...(fullConfig||{})` 合并回传）。
 *
 * 类型约定（对应 Flyway V11.361.7）：布尔开关在库为 INTEGER 0/1 → 此处 `number`；
 * JSON 结构化字段在库为 TEXT → 此处 `string`（前端 `JSON.stringify`/`JSON.parse`）。
 */
export interface ShopConfig {
  id?: number
  tenantId?: number
  /**
   * 商城名称。⚠️ 后端 `tenant_shop_config.shop_name` 列**可空**（V6.4.0 建表即无 NOT NULL），
   * 且 `PUT /config` 走 `updateById`（忽略 null 字段、部分字段提交不覆盖其它页），
   * 故此处为可选：各配置页只提交自己维护的字段即可，不必回填 shopName。
   */
  shopName?: string
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

  // ── 基础设置页（V11.361.7）──
  /** 公众号二维码图片URL */
  officialQrCode?: string
  /** 主营类目 */
  mainCategory?: string
  /** 营业资质 JSON：{license,foodPermit,other1..other4} */
  qualifications?: string
  /** 联系方式 JSON 数组：[{type,number}] */
  contacts?: string
  /** 退货地址-收件人 */
  returnConsignee?: string
  /** 退货地址-收件电话 */
  returnPhone?: string
  /** 退货地址-收件地址 */
  returnAddress?: string
  /** 商品详情页显示字段 JSON 数组 */
  displayDetailFields?: string
  /** 商品列表页显示字段 JSON 数组 */
  displayListFields?: string
  /** 水印方式：none/image/text */
  watermarkType?: string
  /** 图片水印图片URL */
  watermarkImage?: string
  /** 文字水印文字 */
  watermarkText?: string
  /** 微商城二维码图片URL */
  mallQrCode?: string
  /** 微信版链接地址 */
  wechatLink?: string
  /** 微信公众号网页授权域名 */
  authDomain?: string
  /** 网店仓库（默认仓库ID） */
  defaultWarehouseId?: number
  /** 小程序 appid（⚠️ 后端当前明文存储，加密+脱敏未闭环） */
  miniappAppid?: string
  /** 小程序 appsecret（⚠️ 后端当前明文存储） */
  miniappAppsecret?: string
  /** 小程序支付商户号（⚠️ 后端当前明文存储） */
  miniappPayMchId?: string
  /** 小程序支付商户密钥（⚠️ 后端当前明文存储） */
  miniappPayMchKey?: string
  /** 小程序商城地址 */
  miniappMallUrl?: string
  /** 门店助手B To B支付-支付商户号（⚠️ 后端当前明文存储） */
  b2bPayMchId?: string
  /** 门店助手B To B支付-支付商户密钥（⚠️ 后端当前明文存储） */
  b2bPayMchKey?: string
  /** 消息设置 JSON：16 项订阅开关 */
  messageSubscribe?: string

  // ── 店铺设置页（V11.361.7）──
  /** 商城开关 1=开 0=关 */
  shopEnabled?: number
  /** 营业时间-起 HH:mm */
  openTime?: string
  /** 营业时间-止 HH:mm */
  closeTime?: string
  /** 短信签名 */
  smsSignature?: string
  /** 启用买家不显示客户级别 1=是 0=否 */
  buyerHideLevel?: number
  /** 是否允许游客访问：NOT_ALLOW/ALLOW */
  allowGuest?: string
  /** 游客显示价格：HIDE/SHOW */
  guestShowPrice?: string
  /** 商城数量小数位数 0-3 */
  quantityScale?: number
  /** 只允许微信登录 1=是 0=否 */
  wechatOnlyLogin?: number
  /** 价格跟踪 1=是 0=否 */
  priceTrack?: number
  /** 跟踪价格随单位联动 1=是 0=否 */
  priceTrackWithUnit?: number
  /** 启用建议零售价 1=是 0=否 */
  enableRetailPrice?: number
  /** 商品授权管理 1=是 0=否 */
  productAuthManage?: number
  /** 启用分单位显示 1=是 0=否 */
  enableSplitUnit?: number
  /** 启用商城显示销量 1=是 0=否 */
  showSales?: number
  /** 库存显示方式：AVAILABLE/QUANTITY/HIDE */
  stockDisplay?: string
  /** 启用商城分类 1=是 0=否 */
  enableMallCategory?: number
  /** 无货商品显示方式：RESTOCKING/HIDE */
  outOfStockDisplay?: string
  /** 启用 N 天自动收货 1=是 0=否 */
  autoReceiveEnabled?: number
  /** N 天自动收货天数 */
  autoReceiveDays?: number
  /**
   * @deprecated 对标 ql361「店铺设置 → 注册设置」实测**无「注册协议」**这一项（该子项只有 5 项，
   * 见《店铺设置开发文档》「剩余缺口」#1）。本字段为我方历史自造口径，Flyway V11.365.0 起列被标注废弃，
   * 前端「注册设置」子项**不再读写**；仅为兼容保留，新代码禁止使用。
   */
  registerAgreement?: string
  /** 支付场景矩阵 JSON 数组：[{code,enabled,ruleEnabled,ruleValue,sort}] */
  paymentScenes?: string

  // ── 店铺设置页 ·「注册设置」子项（对标实测 5 项，Flyway V11.365.0 重做）──
  /** ① 允许注册账号 1=是 0=否（对标 EnableJoinApply / name=enable_join_apply） */
  enableJoinApply?: number
  /** ② 买家注册默认级别（必填，关联 `/erp/partner/grades?gradeType=CUSTOMER` 的 `PartnerGrade.id`） */
  regDefaultGradeId?: number
  /** ③ 买家注册默认分类（必填；⚠️ 对标选项字典未实测，暂存原值字符串） */
  regDefaultCategory?: string
  /** ④ 买家账号注册审核 1=是 0=否（必填，对标取值 否(0)/是(1)） */
  regAuditRequired?: number
  /** ⑤ 新用户注册送优惠券 1=是 0=否（复选框） */
  regGiveCoupon?: number
  /** ⑤-1 优惠券设置 JSON：`{opengive:boolean, couponslist:[]}`（对标 MallRegGiveCoupons 实测形状） */
  regGiveCoupons?: string

  // ── 商城装修页 ·「分类页装修」子项（对标实测 4 项，Flyway V11.366.0）──
  // 对标实测（2026-09-07 ql361 抓取，见 docs/Yh-Spec/抓取结果/商城装修_抓取.json
  // 的 `Tab三_分类页装修.配置项`）：
  //   ① 分类展示方式     单选，实测「按目录分类」(勾选) / 「按品牌分类」
  //   ② 商品默认排序     下拉，实测默认「综合排序」（⚠️ 选项全集未实测）
  //   ③ 分类页分类样式   单选，实测「纯文本模式」(勾选) / 「图文模式」
  //   ④ 显示分类下商品数量 复选框，实测默认开启
  /** 【分类页装修】① 分类展示方式：CATALOG 按目录分类 / BRAND 按品牌分类（⚠️ 存储编码未实测，本实现口径） */
  categoryDisplayMode?: string
  /** 【分类页装修】② 商品默认排序：COMPOSITE 综合排序（⚠️ 对标选项全集未实测，前端只放实测项） */
  categoryDefaultSort?: string
  /** 【分类页装修】③ 分类页分类样式：TEXT 纯文本模式 / IMAGE 图文模式（⚠️ 存储编码未实测，本实现口径） */
  categoryStyle?: string
  /** 【分类页装修】④ 显示分类下商品数量 1=显示 0=不显示（库内 INTEGER 0/1 → 此处 number，用 bitToBool 转换） */
  categoryShowCount?: number

  // ── 运费设置页（V11.361.7）──
  /** 自有配送 1=开 0=关 */
  selfDelivery?: number
  /** 启用物流 1=开 0=关 */
  enableLogistics?: number
  /**
   * @deprecated 对标 ql361「运费设置 → 物流」实测**没有**「物流方式」列表，本字段为历史自造结构
   * `[{type:CITY|EXPRESS|SELF,name,enabled}]`。Flyway V11.364.0 起列被标注废弃，
   * 前端**不再读写**；仅为兼容保留，新代码禁止使用。
   */
  logisticsMethods?: string
  /** 运费模板（地区运费表）JSON —— {@link ShopFreightTemplate} 的序列化结果，见其定义 */
  freightTemplate?: string
  /** 到店自提 1=开 0=关 */
  enablePickup?: number
  /** 提货地址 JSON 数组：[{contact,phone,address}] */
  pickupAddresses?: string
}

// ── 运费设置：地区运费表（列 `freight_template` 的 JSON 形状） ──
/**
 * 对标 ql361「商城 → 商城设置 → 运费设置 → 物流」实测重建（2026-09-14，Flyway V11.364.0）。
 *
 * 实测结构 = 两个固定开关（自有配送 `selfmention` / 启用物流 `isexpress`，已落 `selfDelivery` /
 * `enableLogistics` 列）+ 「运费计算方式」下拉（3 项）+ **三张随选择切换的地区运费表**，
 * 核心维度是**地区（省/市/区）**与**首重/续重**，不是 from/to 阶梯。
 *
 * 形状选择理由（见《运费设置开发文档》「后端改动」）：
 * 1. 三张表共用**同一个 rows 集合** `regions`，行内保留三张表各自的字段 —— 切换「运费计算方式」
 *    不丢已填数据（与对标切换即重渲染但后端按三个 *freight_temps 字段分别存储等价）；
 * 2. 「按订单金额」表的金额阶梯对标是**行内「设置」**（后端 `prilist:[{min,max,freight,caninput}]`），
 *    故为**每行**可选字段 `amountTiers`，与实测交互一一对应；
 * 3. 包邮 `freeFreight` 是**每行一个金额**（对标无模板级开关 + 阈值），故下沉到行；
 * 4. `defaultFreight` 承载对标红字提示「未设置运费的地区，默认运费为N元」的 N（实测默认 0）。
 * 5. `wftype` 采用对标后端字段名与**数值取值**（1/2/3），弃用原 `WEIGHT/PIECE/AMOUNT`
 *    命名错误（「PIECE/按件数」≠「按订单数量」）。
 */
export interface ShopFreightRegionRow {
  /** 地区行政区划代码（对应 sys_region.code；省市/区级选择时只填到所选层级） */
  code?: string
  province?: string
  city?: string
  area?: string
  /** 按重量表：首重(KG) */
  firstWeight?: number
  /** 按重量/按订单金额/按订单数量表：运费(元) */
  freight?: number
  /** 按重量表：续重(KG) */
  addWeight?: number
  /** 按重量/按订单数量表：续费(元) */
  addFreight?: number
  /** 按订单数量表：起算数量 */
  startCount?: number
  /** 按订单数量表：增加数量 */
  addCount?: number
  /** 满额包邮(元)，每行一个金额（0 = 不包邮） */
  freeFreight?: number
  /** 「按订单金额」表行内「设置」的金额阶梯（对标后端 prilist；max=null 表示不限） */
  amountTiers?: ShopFreightTier[]
}

/** 「按订单金额」表的单条金额阶梯（对标后端 `prilist:[{min,max,freight,caninput}]`） */
export interface ShopFreightTier {
  min: number
  /** null = 不限 */
  max: number | null
  freight: number
  /** 对标 `caninput`：该阶梯金额是否允许录入 */
  caninput?: boolean
}

/** 列 `freight_template` 的完整 JSON 结构 */
export interface ShopFreightTemplate {
  /** 运费计算方式：1=按重量 2=按订单金额 3=按订单数量（对标后端字段 `wftype`） */
  wftype: number
  /** 未设置运费的地区的默认运费（对标红字提示 N，实测默认 0） */
  defaultFreight: number
  /** 三张表的共同行集合，维度=地区（省/市/区） */
  regions: ShopFreightRegionRow[]
}

/** `wftype` 取值 → 中文名（运算法） */
export const FREIGHT_WFTYPE = {
  /** 按重量 */
  WEIGHT: 1,
  /** 按订单金额 */
  AMOUNT: 2,
  /** 按订单数量 */
  COUNT: 3,
} as const

// ── 商城配置：布尔 / JSON 显式转换工具（三配置页共用） ──
// 约定：库内布尔开关为 INTEGER 0/1；JSON 结构化字段为 TEXT。
// 显式转换函数而非依赖隐式转换，且 JSON.parse 一律 try/catch + 空值兜底，脏数据不崩页。

/** 布尔 → 库内 0/1（提交用） */
export function boolToBit(v: unknown): number {
  return v ? 1 : 0
}

/** 库内 0/1 → 布尔（回填用）；null/undefined 走 fallback */
export function bitToBool(v: unknown, fallback = false): boolean {
  if (v === null || v === undefined || v === '') return fallback
  if (typeof v === 'boolean') return v
  if (typeof v === 'number') return v === 1
  const s = String(v).trim().toLowerCase()
  return s === '1' || s === 'true'
}

/**
 * 库内 TEXT → JS 值（回填用）。
 * JSON.parse 失败或值为空时返回 fallback（脏数据不把页面打崩）。
 */
export function parseJsonField<T>(raw: unknown, fallback: T): T {
  if (raw === null || raw === undefined || raw === '') return fallback
  if (typeof raw === 'object') return raw as T
  try {
    const parsed = JSON.parse(String(raw))
    return (parsed === null || parsed === undefined ? fallback : parsed) as T
  } catch (e) {
    console.warn('[商城配置] JSON 列解析失败，已回退默认值：', raw, e)
    return fallback
  }
}

/** JS 值 → 库内 TEXT（提交用）；null/undefined 返回 undefined（不提交，保留库内原值） */
export function stringifyJsonField(v: unknown): string | undefined {
  if (v === null || v === undefined) return undefined
  try {
    return JSON.stringify(v)
  } catch (e) {
    console.warn('[商城配置] JSON 列序列化失败，已跳过该字段：', v, e)
    return undefined
  }
}

export const shopConfigApi = {
  /** 获取商城配置 */
  get(): Promise<ApiResponse<ShopConfig>> {
    return request.get('/erp/mall/admin/config')
  },
  /** 更新商城配置（部分字段提交即可：后端 updateById 忽略 null，不会覆盖其它页字段） */
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
  // ── 买家账号 / 买家申请管理页字段（Flyway V11.361.3 为 shop_user 补齐）──
  /** 联系人姓名（真实姓名） */
  contactName?: string
  /** 地址 */
  address?: string
  /** 备注 */
  remark?: string
  /** 营业执照图片URL */
  businessLicense?: string
  /** QQ */
  qq?: string
  /** 微信 */
  wechat?: string
  /** 归属分类ID（biz_party_category.id，party_type=CUSTOMER） */
  categoryId?: number
  /** 默认经手人ID（sys_user.id） */
  defaultHandlerId?: number
  /** 默认经手人姓名 */
  defaultHandlerName?: string
  /** 客户级别（级别名称，如「A餐饮客户」） */
  customerLevel?: string
  /** 所属仓库ID（erp_warehouse.id） */
  warehouseId?: number
  /** 所属仓库名称 */
  warehouseName?: string
  /** 所属部门ID（sys_dept.id） */
  deptId?: number
  /** 所属部门名称 */
  deptName?: string
}

/**
 * 买家账号列表查询参数（GET /erp/mall/admin/user/page）
 * 后端对 null/空 参数不做该条件过滤。
 */
export interface ShopUserPageParams extends PageQuery {
  /** 关键词（用户名/昵称/手机号/公司名） */
  keyword?: string
  /** 审核状态 0待审 1通过 2驳回 */
  auditStatus?: number
  /** 状态 1正常 0禁用（showDisabled=false 时被忽略，强制只看启用） */
  status?: number
  /** 注册时间(起) yyyy-MM-dd，含当日 00:00:00 */
  createTimeStart?: string
  /** 注册时间(止) yyyy-MM-dd，含当日 23:59:59 */
  createTimeEnd?: string
  /** 归属分类ID（分类树选中项；根节点「全部客户」不传） */
  categoryId?: number
  /** 客户级别ID（后端解析为级别名称后匹配 shop_user.customer_level） */
  gradeId?: number
  /** 是否显示停用：true/缺省=不过滤；false=只看启用 */
  showDisabled?: boolean
}

/**
 * 买家账号编辑请求体（PUT /erp/mall/admin/user/{id}）
 *
 * 部分更新语义：不提交的字段（undefined）不修改；String 传 '' 表示清空。
 * 「默认经手人 / 所属仓库 / 所属部门」为 id+name 成对提交：name 传 '' 时同时清空 id（取消归属）；
 * categoryId 为数值，仅支持改选、不支持清空（传 null/undefined 均视为不修改）。
 */
export interface ShopUserUpdatePayload {
  nickname?: string
  contactName?: string
  phone?: string
  companyName?: string
  email?: string
  qq?: string
  wechat?: string
  address?: string
  remark?: string
  customerLevel?: string
  categoryId?: number
  defaultHandlerId?: number | null
  defaultHandlerName?: string | null
  warehouseId?: number | null
  warehouseName?: string | null
  deptId?: number | null
  deptName?: string | null
}

export const shopUserApi = {
  /** 分页查询用户 */
  page(params: ShopUserPageParams): Promise<ApiResponse<PageResult<ShopUser>>> {
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
  },
  /** 编辑买家账号（部分更新，返回更新后对象） */
  update(id: number, data: ShopUserUpdatePayload): Promise<ApiResponse<ShopUser>> {
    return request.put(`/erp/mall/admin/user/${id}`, data)
  },
  /** 删除买家账号（后端逻辑删除 deleted=1，非物理删除） */
  remove(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/mall/admin/user/${id}`)
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
  // ── 行业模板库（Flyway V11.366.0）──
  /** 【行业模板库】行业编码（⚠️ 对标未实测此编码，本实现口径：office/apparel/…/jewelry） */
  industryCode?: string
  /** 【行业模板库】行业名称（**对标实测逐字** 14 行业，见 `INDUSTRY_TEMPLATES`） */
  industryName?: string
  /** 【行业模板库】1=库条目（平台共享，仅供「模板库」Tab） 0/空=本租户「我的模板」 */
  isLibrary?: number
}

export const shopTemplateApi = {
  /** 获取本租户「我的模板」列表（行业库条目 isLibrary=1 不在本列表，见 library()） */
  list(): Promise<ApiResponse<ShopTemplate[]>> {
    return request.get('/erp/mall/admin/template/list')
  },
  /**
   * 行业模板库列表（对标实测 14 行业）。
   * ⚠️ 各行业的**模板内部布局内容未实测**，库条目 `configJson` 一律为空。
   */
  library(): Promise<ApiResponse<ShopTemplate[]>> {
    return request.get('/erp/mall/admin/template/library')
  },
  /** 新增「我的模板」（对标「装修模板 → 我的模板 → 新增模板」） */
  create(data: ShopTemplate): Promise<ApiResponse<ShopTemplate>> {
    return request.post('/erp/mall/admin/template', data)
  },
  /**
   * 引用行业库模板（复制库条目为本租户「我的模板」）。
   * ⚠️ 库条目内容为空 ⇒ 引用结果同样为空（如实留缺口，不臆造模板内容）。
   */
  reference(id: number): Promise<ApiResponse<ShopTemplate>> {
    return request.post(`/erp/mall/admin/template/library/${id}/reference`)
  }
}

// ── 商城装修配置（排版布局存储，Flyway V11.366.0）──
/** 装修范围（⚠️ 本实现口径：对标抓取只实测到 4 个 Tab，无 scope 编码） */
export type ShopDecorationScope = 'HOME' | 'CATEGORY' | 'PRODUCT_DETAIL'

/**
 * 商城装修配置（表 `shop_decoration`，后端实体 `cn.aiedge.erp.b2b.model.ShopDecoration`）。
 *
 * ⚠️ **表结构为本实现口径，不是对标实测结论**：对标只实测到「排版布局/拖拽编辑器」存在，
 * 装修配置的后端字段与 JSON 结构均未实测。`configJson` 约定为 TEXT 存 JSON 文本
 * （不用 jsonb），后端按不透明文本存取，不校验内部形状。
 */
export interface ShopDecoration {
  id?: number
  tenantId?: number
  /** 装修范围：HOME 首页 / CATEGORY 分类页 / PRODUCT_DETAIL 商品详情 */
  scope?: ShopDecorationScope
  /** 引用的模板 id（可空，逻辑关联 shop_template.id） */
  templateId?: number
  /** 装修名称（对标「我的模板」卡片名 / 「新增模板」的名称） */
  name?: string
  /** 装修结构 JSON 文本（⚠️ 结构未实测；前端用 stringifyJsonField/parseJsonField 转换） */
  configJson?: string
  /** 1启用 0停用 */
  status?: number
  createTime?: string
  updateTime?: string
}

export const shopDecorationApi = {
  /** 装修配置列表（scope 为空则全部） */
  list(scope?: ShopDecorationScope): Promise<ApiResponse<ShopDecoration[]>> {
    return request.get('/erp/mall/admin/decoration', { params: scope ? { scope } : {} })
  },
  /** 装修配置详情 */
  get(id: number): Promise<ApiResponse<ShopDecoration>> {
    return request.get(`/erp/mall/admin/decoration/${id}`)
  },
  /** 新增装修配置（name 必填；scope 为空后端默认 HOME） */
  create(data: ShopDecoration): Promise<ApiResponse<ShopDecoration>> {
    return request.post('/erp/mall/admin/decoration', data)
  },
  /** 更新装修配置（部分更新：仅覆盖请求体中非 null 字段） */
  update(id: number, data: ShopDecoration): Promise<ApiResponse<ShopDecoration>> {
    return request.put(`/erp/mall/admin/decoration/${id}`, data)
  },
  /** 逻辑删除装修配置（及其应用商品关联） */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/mall/admin/decoration/${id}`)
  },
  /** 查询装修配置已关联的商品 id 集合（对标「设置应用商品」） */
  listProducts(id: number): Promise<ApiResponse<number[]>> {
    return request.get(`/erp/mall/admin/decoration/${id}/products`)
  },
  /**
   * 全量替换装修配置的关联商品集合。
   * ⚠️ 对标「设置应用商品」的**交互未实测**，本端点为「能承载前端多选集合」的本实现口径。
   */
  replaceProducts(id: number, productIds: number[]): Promise<ApiResponse<number[]>> {
    return request.put(`/erp/mall/admin/decoration/${id}/products`, productIds)
  }
}

// ── 商城商品 ──
/** 商城管理后台行数据（数据源 v_mall_product 视图 / 后端 ErpProductMall） */
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
  /** 上架状态：ON_SHELF 上架 / INACTIVE（下架） */
  status: string
  description?: string
  stockQuantity?: number
  salesCount?: number
  // ── V11.361.5 视图追加列（规格 / 单位） ──
  /** 规格（视图 specification ← erp_product.spec） */
  specification?: string
  /** 单位（视图 unit_name ← erp_product.unit） */
  unitName?: string
  // ── V11.361.6 视图追加列（商品上架金标准 48 列所需） ──
  /** 条形码（视图 barcode ← erp_product.barcode） */
  barcode?: string
  /** 型号（视图 model_no ← erp_product.model） */
  modelNo?: string
  /** 产地（视图 origin_place ← erp_product.origin） */
  originPlace?: string
  /** 品牌（视图 brand ← erp_product.brand） */
  brand?: string
  /** 批发价（视图 wholesale_price ← erp_product.wholesale_price） */
  wholesalePrice?: number
  /** 预设进价（视图 preset_cost_price ← COALESCE(erp_product.purchase_price, cost_price, 0)） */
  presetCostPrice?: number
  /** 商城排序方式（视图 sort ← erp_product.mall_sort_type：DEFAULT/SALES/MANUAL） */
  sort?: string
  /** 商城排序值（视图 sort_value ← erp_product.mall_sort_order） */
  sortValue?: number
  /** 商城起订量（视图 min_order_quantity ← erp_product.mall_min_order_qty） */
  minOrderQuantity?: number
  /** 商品积分（视图 product_points ← erp_product.mall_points） */
  productPoints?: number
  /** 备注（视图 remark ← erp_product.remark） */
  remark?: string
  /** 商城检索关键字（视图 keyword ← erp_product.keywords） */
  keyword?: string
  /** 使用优惠券原始值（视图 use_coupon ← erp_product.use_coupon：0=否 1=是） */
  useCoupon?: number
  /** 使用优惠券布尔值（视图 coupon_used ← erp_product.use_coupon = 1） */
  couponUsed?: boolean
  /** 商品类型（视图 product_type ← erp_product.product_type：SINGLE/KIT/SERVICE） */
  productType?: string
  /** 显示状态（视图 visible_status ← erp_product.status：ENABLED/DISABLED） */
  visibleStatus?: string
  /** 商品标签（视图 product_tag ← erp_product.mall_tags，逗号分隔槽位编码 TAG_1..TAG_20） */
  productTag?: string
  /** 所属行业类别（视图 industry_category ← erp_product.industry_category） */
  industryCategory?: string
  // ── 客户类型价格 8 列（对标列 15-22；视图 grade_price_1..8 ← MAX(erp_product_unit.grade_price_N)） ──
  /** 价格等级1 餐饮店（多单位取最大值，>0 视为已配价） */
  gradePrice1?: number
  /** 价格等级2 食堂团餐 */
  gradePrice2?: number
  /** 价格等级3 外围餐饮店 */
  gradePrice3?: number
  /** 价格等级4 自助vip */
  gradePrice4?: number
  /** 价格等级5 大团餐 */
  gradePrice5?: number
  /** 价格等级6 重点|vip01 */
  gradePrice6?: number
  /** 价格等级7 连锁|vip */
  gradePrice7?: number
  /** 价格等级8 特价客户 */
  gradePrice8?: number
  /** 已配置级别指定价的客户等级昵称集合（逗号分隔；erp_customer_grade_price.grade_name 聚合，补充佐证） */
  customerGradeCodes?: string
}

/**
 * 商品上架页查询条件（对应后端 /erp/mall/admin/product/page 参数，空值不过滤）
 * 说明：PageQuery 的 status 为 number、keyword 为 string，此处上架状态 status 是
 * ON_SHELF/OFF_SHELF 字符串，故用 Omit 剔除后重定义为 string，避免类型冲突。
 */
export interface MallProductPageQuery extends Omit<PageQuery, 'status' | 'keyword'> {
  /** 筛选条件（商品名称/货号/条码/规格/型号） */
  keyword?: string
  categoryId?: string
  /** 上架状态：ON_SHELF 上架 / OFF_SHELF（后端归一为 INACTIVE）下架 */
  status?: string
  /** 品牌（erp_product.brand） */
  brand?: string
  /** 商品名称 */
  productName?: string
  /** 商品标签槽位编码（erp_product.mall_tags，如 TAG_1） */
  productTag?: string
  /** 使用优惠券：yes 是 / no 否 */
  couponUsed?: string
  /** 商品类型：SINGLE 单品 / KIT 套件 / SERVICE 服务 */
  productType?: string
  /** 显示状态：ENABLED 启用 / DISABLED 停用 */
  visibleStatus?: string
}

// ── 商城商品管理 ──
export const mallProductApi = {
  /** 分页查询（数据源 v_mall_product 视图，含商品上架页对标列与 8 项查询条件） */
  page(params: MallProductPageQuery): Promise<ApiResponse<PageResult<MallProduct>>> {
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

// ── 商城订单「按明细」分页行（后端 /erp/mall/admin/order/page-detail，MallOrderItemPageDTO） ──
// 数据源：erp_sale_order_item INNER JOIN erp_sale_order，明细行 + 单头字段合并成一行。
// 明细侧取 erp_sale_order_item 实有列，单头侧取 erp_sale_order 实有列；未落库的展示列保持空值（不做假数据）。
export interface MallOrderItemPage {
  /** 明细主键（行键使用 itemId，避免与单头 id 重复） */
  itemId?: number
  /** 所属订单ID */
  orderId?: number
  lineNo?: number
  productId?: number
  productCode?: string
  productName?: string
  barcode?: string
  smallUnitBarcode?: string
  specification?: string
  modelNo?: string
  originPlace?: string
  brand?: string
  unit?: string
  batchBarcode?: string
  productionDate?: string
  expiryDate?: string
  quantity?: number
  /** 销售数量（「按明细」列 key：saleQuantity） */
  saleQuantity?: number
  bigPack?: number
  midPack?: number
  smallPack?: number
  /** 已发数量（「按明细」列 key：shippedItemQty） */
  shippedItemQty?: number
  shippedQuantity?: number
  /** 未发数量 = 销售数量 - 已发数量（「按明细」列 key：unshippedItemQty） */
  unshippedItemQty?: number
  unshippedQuantity?: number
  unitPrice?: number
  smallUnitPrice?: number
  amount?: number
  itemRemark?: string
  // ── 单头（erp_sale_order） ──
  orderNo?: string
  orderDate?: string
  /** 订单状态: 0草稿,1待审批,2已审批,3部分出库,4完成,5交易完成,6已取消 */
  status?: number
  orderSource?: number
  customerId?: number
  customerName?: string
  totalAmount?: number
  receivedAmount?: number
  paymentMethod?: string
  paymentStatus?: number
  deliveryStatus?: number
  consignee?: string
  consigneePhone?: string
  consigneeAddress?: string
  shippingAddress?: string
  orderRemark?: string
  sellerRemark?: string
  buyerRemark?: string
  extInfo?: string
  createTime?: string
  updateTime?: string
  freight?: number
  freightPayer?: string
  deliveryMethod?: string
  logisticsCompany?: string
  trackingNo?: string
  codAmount?: number
  expectedShipTime?: string
  productQuantity?: number
  warehouseName?: string
  /** 部门名称（来源列 erp_sale_order.dept_name，「按明细」Tab「部门」列） */
  departmentName?: string
  handlerName?: string
  promoterName?: string
  printCount?: number
  bookkeepingStatus?: number
  settledAmount?: number
  couponUsed?: number
  auditTime?: string
  otherFee?: number
  extNum1?: number
  extNum2?: number
  extText3?: string
  extText4?: string
  extText5?: string
}

/**
 * 商城订单列表/按明细查询参数（GET /erp/mall/admin/order/page、/order/page-detail）
 *
 * <p>两页（mall/order-process 订单处理、trade/mall-order 商城订单）共用同一后端端点：
 * 条件全部可选，为空即不过滤。</p>
 *
 * <p><b>状态口径</b>：erp_sale_order.status 权威口径为
 * 0草稿 / 1待审批 / 2已审批 / 3部分出库 / 4完成 / 5交易完成 / 6已取消。</p>
 * <ul>
 *   <li>{@code orderStatus} 商城状态串（逗号分隔多值，如 "PENDING_AUDIT,PENDING_PAYMENT"），
 *       后端按 toErpStatus 逐值映射后 IN 查询，无法识别的值显式忽略</li>
 *   <li>{@code status} erp_sale_order.status 数字（单值或逗号分隔多值），后端与 orderStatus 取并集</li>
 * </ul>
 */
export interface MallOrderPageParams extends Omit<PageQuery, 'status'> {
  /** 综合关键词（单据编号 / 客户 / 收货人） */
  keyword?: string
  /** 商城状态串（逗号分隔多值；订单处理页「单据状态」多选提交） */
  orderStatus?: string
  /** erp_sale_order.status 数字（单值或逗号分隔多值；商城订单页「单据状态」提交） */
  status?: number | string
  /** 单据编号（模糊） */
  orderNo?: string
  /** 单据日期(起) yyyy-MM-dd */
  startDate?: string
  /** 单据日期(止) yyyy-MM-dd，含当日 */
  endDate?: string
  /** 收货人（模糊） */
  consignee?: string
  /** 支付方式（等值 ALIPAY/WECHAT/UNIONPAY/BANK/CASH） */
  paymentMethod?: string
  /** 订单来源（等值 2=企业客户商城 3=个人会员商城） */
  orderSource?: number
  /** 商品名称/货号（按 erp_sale_order_item 反查单据） */
  productName?: string
}

export const mallOrderApi = {
  /** 分页查询 */
  page(params: MallOrderPageParams): Promise<ApiResponse<PageResult<MallOrder>>> {
    return request.get('/erp/mall/admin/order/page', params)
  },
  /** 按明细分页查询（数据源 erp_sale_order_item JOIN erp_sale_order） */
  pageDetail(params: MallOrderPageParams): Promise<PageResult<MallOrderItemPage>> {
    return request.get('/erp/mall/admin/order/page-detail', params)
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
  /** 收款（记收款金额并置为已支付） */
  receive(id: number): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/receive`)
  },
  /** 强制终止（状态置为已取消） */
  terminate(id: number, reason?: string): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/terminate`, { reason })
  },
  /** 批量审核 */
  batchApprove(ids: number[]): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/order/batch-approve', ids)
  },
  /** 批量发货 */
  batchShip(ids: number[]): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/order/batch-ship', ids)
  },
  /** 统计卡（总订单数/GMV/客单价/退款率，真实聚合口径） */
  orderStats(): Promise<MallTradeSummary> {
    return request.get('/erp/mall/admin/order/stats')
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
  /** 部门名称（来源列 erp_sale_order.dept_name） */
  deptName?: string
  /** 部门名称（按明细行别名，与 deptName 同源） */
  departmentName?: string
}

export const mallAdminOrderApi = {
  /** 分页查询商城订单（管理端） */
  page(params: MallOrderPageParams): Promise<PageResult<MallOrderAdmin>> {
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
  },
  /** 按明细分页查询（数据源 erp_sale_order_item JOIN erp_sale_order） */
  pageDetail(params: MallOrderPageParams): Promise<PageResult<MallOrderItemPage>> {
    return request.get('/erp/mall/admin/order/page-detail', params)
  },
  /** 收款（记收款金额并置为已支付；后端与 /pay 同实现） */
  receive(id: number): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/receive`)
  },
  /** 发货（物流公司/运单号写入 extInfo） */
  ship(id: number, data: { logisticsCompany?: string; trackingNo?: string; remark?: string }): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/ship`, data)
  },
  /** 退款（支付状态置为已退款） */
  refund(id: number, data: { amount?: number; reason?: string; remark?: string }): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/refund`, data)
  },
  /** 强制终止（状态置为已取消） */
  terminate(id: number, reason?: string): Promise<void> {
    return request.post(`/erp/mall/admin/order/${id}/terminate`, { reason })
  },
  /** 批量审核通过（返回成功条数） */
  batchApprove(ids: number[]): Promise<number> {
    return request.put('/erp/mall/admin/order/batch-approve', ids)
  },
  /** 批量发货（返回成功条数） */
  batchShip(ids: number[]): Promise<number> {
    return request.put('/erp/mall/admin/order/batch-ship', ids)
  },
  /** 统计卡（总订单数/GMV/客单价/退款率，真实聚合口径） */
  orderStats(): Promise<MallTradeSummary> {
    return request.get('/erp/mall/admin/order/stats')
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
  },
  /** 批量启用套装（后端 `POST /erp/product-kit/batch-activate`：body 为 `{"ids":[…]}`，返回实际更新条数） */
  batchActivate(ids: number[]): Promise<number> {
    return request.post('/erp/product-kit/batch-activate', { ids })
  },
  /** 批量停用套装（后端 `POST /erp/product-kit/batch-deactivate`：body 为 `{"ids":[…]}`，返回实际更新条数） */
  batchDeactivate(ids: number[]): Promise<number> {
    return request.post('/erp/product-kit/batch-deactivate', { ids })
  },
  /** 删除套装（后端 `DELETE /erp/product-kit/{id}`：级联逻辑删除组件行） */
  remove(id: number): Promise<boolean> {
    return request.delete(`/erp/product-kit/${id}`)
  },
  /** 批量删除套装（后端 `DELETE /erp/product-kit/batch`：body 为 `{"ids":[…]}`） */
  batchRemove(ids: number[]): Promise<number> {
    return request.delete('/erp/product-kit/batch', { data: { ids } })
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
  /** 备注（对标 ql361 关键词库「备注」列） */
  remark?: string
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

// ── 商城购物车（后台查看/清理，后端 MallCartController `@RequestMapping("/api/v1/mall/cart")`） ──
// ⚠️ request 的 baseURL 为 '/api'，故此处路径必须写 '/v1/mall/cart'（写 '/api/v1/...' 会双前缀 → 404）。
export interface MallCartItem {
  id: number
  productId?: number | string
  productName?: string
  productCode?: string
  specification?: string
  unitName?: string
  quantity?: number
  price?: number
  totalPrice?: number
  memberName?: string
  memberAccount?: string
  imageUrl?: string
  createTime?: string
}

export const mallCartApi = {
  /** 获取当前用户购物车（裸数组返回） */
  getCart(): Promise<MallCartItem[]> {
    return request.get('/v1/mall/cart')
  },
  /**
   * 管理端分页查询购物车（后端 `GET /v1/mall/cart/page`，真实分页 SQL）
   * @param params.memberKeyword  会员关键字（登录名/昵称/手机号/公司名）
   * @param params.productKeyword 商品关键字（商品名称/商品编码）
   * 返回 PageResult：{ records, total, pageNum, pageSize, pages }（响应拦截器已解包）
   */
  page(params: { pageNum?: number; pageSize?: number; customerId?: number; memberKeyword?: string; productKeyword?: string }): Promise<{ records: MallCartItem[]; total: number; pageNum?: number; pageSize?: number; pages?: number }> {
    return request.get('/v1/mall/cart/page', params)
  },
  /** 加入购物车 */
  addItem(data: Partial<MallCartItem>): Promise<MallCartItem> {
    return request.post('/v1/mall/cart', data)
  },
  /** 更新购物车项 */
  updateItem(id: number, data: Partial<MallCartItem>): Promise<MallCartItem> {
    return request.put(`/v1/mall/cart/${id}`, data)
  },
  /** 删除购物车项 */
  removeItem(id: number): Promise<void> {
    return request.delete(`/v1/mall/cart/${id}`)
  },
  /** 批量删除购物车项（后端 DELETE /batch） */
  batchRemove(ids: number[]): Promise<void> {
    return request.delete('/v1/mall/cart/batch', { data: ids })
  }
  // 2026-09-23 移除 clearCart()：它调 `DELETE /v1/mall/cart`，该端点按**调用者本人**清空
  // （后端用 StpUtil.getLoginIdAsLong() 当 customerId），而管理端列表展示的是全体会员的购物车
  // ⇒ 点了对列表无任何影响，是"看似能用实则无效"的假接口（审计报告 P1-8）。
  // 管理端要按会员清空，需后端新增按 customerId 的管理端端点。
}

// ── 商城退货/售后 ──
//
// 2026-09-23 删除原 `mallReturnApi`（page/stats/getById/approve/reject/batchApprove/batchReject）
// 与配套的 `MallReturn` 接口类型，理由（见 TRADE_MODULE_AUDIT_20260923.md P2-1）：
//   · 全仓零引用（`grep -rn "mallReturnApi"` 只命中定义处这一行）；
//   · 后端**不存在** `/erp/mall/admin/return/*` 的任何端点，原实现一旦被调用必然 404；
//   · 真正的退货处理页 `views/mall/return-process/index.vue` 用的是 `saleReturnApi`
//     （`/erp/sale/return/*` —— 退货申请挂在销售域，不在商城域）。
