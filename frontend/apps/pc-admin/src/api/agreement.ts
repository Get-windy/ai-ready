/**
 * 协议模块 API
 *
 * 业务定位（见 docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md §十二）：
 *   协议分三类 —— 平台↔租户（入驻服务）、租户↔租户（代销 / 购销框架）、租户↔消费者（单方承诺）。
 *   一张主档 + 版本 + 条款三张结构，全类型共用；三条不变量落在结构上：
 *     · 已生效版本只读（改协议只能新建草稿版本）
 *     · 双签缺一不可（双方确认痕迹都在，才允许置生效）
 *     · 必填条款没选完不许置生效
 *
 * 约定：
 *   · 所有路径前缀 `/api` 由 request 工具自带，此处只写相对路径；
 *   · 统一返回 `Result<T>`，响应拦截器已拆包（拿到的就是 data 本体）；
 *   · 条款的「选哪个」写在**版本**上（版本 = 条款快照），草稿阶段就在版本上选，
 *     因此保存条款走「改草稿」(`PUT /agreement/{id}`) 并带 `versionId + terms`，不另造一套条款接口。
 */
import request, { type PageResponse } from '@/utils/request'

// ── 枚举口径 ──────────────────────────────────────────────

/** 协议类型：平台↔租户服务协议 / 租户↔租户代销 / 租户↔租户购销框架 / 租户↔消费者单方承诺 */
export type AgreementType = 'PLATFORM_SERVICE' | 'DISTRIBUTION' | 'GOODS_FRAMEWORK' | 'CONSUMER_PROMISE'

/** 协议主档状态：洽谈中 / 生效 / 暂停 / 终止 */
export type AgreementStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED'

/**
 * 协议范围（列表页两个入口的筛选口径）
 *   · `TENANT`   = 租户级三类（代销 / 购销框架 / 消费者单方承诺），即排除平台服务协议；
 *   · `PLATFORM` = 平台级（仅平台服务协议）。
 * 由**服务端**按此条件过滤（进 SQL），因此分页 total 与显示行数天然一致 ——
 * 不要在前端取回后再剔除行，那会让 total 仍是服务端口径、与实际显示对不上。
 */
export type AgreementScope = 'TENANT' | 'PLATFORM'

/** 协议版本状态：待双方确认 / 生效 / 已被新版取代 / 已否决 */
export type AgreementVersionStatus = 'DRAFT' | 'ACTIVE' | 'SUPERSEDED' | 'REJECTED'

/** 协议一方（两端对称：主体 + 租户） */
export interface AgreementPartySide {
  /** 主体（往来单位）ID */
  partyId?: number | string
  /** 该主体所属租户 ID */
  tenantId?: number | string
  /** 主体名称（展示用，后端带出） */
  partyName?: string
  /** 租户名称（展示用，后端带出） */
  tenantName?: string
}

/** 协议主档（列表 / 详情共用） */
export interface Agreement extends AgreementPartySide {
  id: number | string
  /** 协议编号 */
  agreementNo?: string
  /** 协议类型 */
  agreementType: AgreementType
  /** 协议标题 */
  title?: string
  /** 主档状态 */
  status?: AgreementStatus
  /** 当前生效版本 ID（未生效的草稿协议为空） */
  currentVersionId?: number | string | null
  /** 当前生效版本号 */
  currentVersionNo?: number | null
  /** 甲方（主体 + 租户） */
  partyAId?: number | string
  partyATenantId?: number | string
  partyAName?: string
  partyATenantName?: string
  /** 乙方（主体 + 租户）；对消费者承诺时乙方为不特定消费者，可为空 */
  partyBId?: number | string
  partyBTenantId?: number | string
  partyBName?: string
  partyBTenantName?: string
  /** 生效期 */
  effectiveFrom?: string
  effectiveTo?: string
  /** 平台终止留痕（㉝ 清退权） */
  terminatedReason?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 协议版本（历史列表项） */
export interface AgreementVersion {
  id: number | string
  agreementId?: number | string
  versionNo?: number
  status?: AgreementVersionStatus
  /** 变更原因（发起变更时填写） */
  changeReason?: string
  /** 甲方确认人 / 时间 */
  partyAConfirmedBy?: number | string | null
  partyAConfirmedAt?: string | null
  /** 乙方确认人 / 时间 */
  partyBConfirmedBy?: number | string | null
  partyBConfirmedAt?: string | null
  /** 完整条款快照（不可变） */
  snapshotJson?: string | Record<string, any> | null
  snapshot?: Record<string, any> | null
  effectiveFrom?: string
  effectiveTo?: string
  createdBy?: number | string
  createdAt?: string
  /** 版本上的条款明细（后端带出时直接可用） */
  terms?: AgreementTerm[]
}

/** 协议条款：从平台字典里选一个选项 + 可选参数 */
export interface AgreementTerm {
  id?: number | string
  agreementId?: number | string
  versionId?: number | string
  /** 条款类别（如取消政策 / 退货政策 / 运费承担 / 质量责任） */
  termCode: string
  /** 选中的选项 */
  optionCode?: string
  optionLabel?: string
  /** 该选项的系统执行语义（选它就等于约定系统这么执行） */
  semantics?: string
  /** 参数值（字典 needsParam 非空时必填，如分账比例） */
  paramValue?: string | number | null
  /** 本条是否必填（来自字典） */
  required?: boolean
}

/** 条款字典选项（平台维护：只定义「有哪些选项、各自什么含义」，不规定必须选哪个） */
export interface AgreementTermOption {
  id: number | string
  /** 条款类别 */
  termCode: string
  /** 条款类别中文名 */
  termName?: string
  /** 选项编码 */
  optionCode: string
  /** 选项中文名（双方看到的） */
  optionLabel: string
  /** 语义说明：选了这个，系统会怎么执行（给双方看的条款说明书） */
  semantics?: string
  /** 需要附带参数时填参数名（如分账比例），否则为空 */
  needsParam?: string | null
  /** 是否必填条款（责任划分类一律必填） */
  required?: boolean
  sort?: number
  status?: number
  remark?: string
}

/** 按条款类别分组后的字典（一组一个下拉） */
export interface AgreementTermOptionGroup {
  /** 条款类别 */
  termCode: string
  /** 条款类别中文名 */
  termName?: string
  /** 该组是否必填（组内选项 required 为真时后端可上提） */
  required?: boolean
  /** 该类别的全部可选选项 */
  options: AgreementTermOption[]
}

/** 新建草稿协议的请求体 */
export interface AgreementCreateBody {
  agreementType: AgreementType
  partyAId: number | string
  partyATenantId: number | string
  /** 对消费者承诺时不需要指定乙方主体 */
  partyBId?: number | string | null
  partyBTenantId?: number | string | null
  title: string
  effectiveFrom?: string
  effectiveTo?: string
}

/** 改草稿的请求体：主档字段 + 该草稿版本的条款选择（条款写在版本上） */
export interface AgreementUpdateBody extends Partial<AgreementCreateBody> {
  /** 要写入条款的草稿版本 ID（详情页带入当前版本） */
  versionId?: number | string
  /** 该版本的完整条款选择（整份覆盖，未列的类别视为仍未约定） */
  terms?: Array<Pick<AgreementTerm, 'termCode' | 'optionCode' | 'paramValue'>>
}

/** 协议列表查询条件 */
export interface AgreementQuery {
  current?: number
  size?: number
  /**
   * 协议范围（入口决定）：平台协议入口传 PLATFORM，协议列表入口传 TENANT。
   * 不传表示不限范围（服务端与历史行为一致，不加类型条件）。
   */
  agreementScope?: AgreementScope
  /** 协议类型：比范围更具体，与 agreementScope 同时传时服务端以它为准 */
  agreementType?: AgreementType
  /** 主档状态 */
  status?: AgreementStatus
  /** 关键字：协议编号 / 标题 */
  keyword?: string
}

/** 发起变更的请求体 */
export interface AgreementVersionCreateBody {
  changeReason: string
}

// ── 枚举 → 中文文案（界面一律用业务白话，不出现类型码） ────────

/** 协议类型中文名 + 一句话说明 */
export const AGREEMENT_TYPE_META: Record<AgreementType, {
  label: string
  shortDesc: string
  /** 新建引导卡片上的详细说明 */
  detail: string
  color: string
}> = {
  PLATFORM_SERVICE: {
    label: '平台服务协议',
    shortDesc: '平台 ↔ 租户',
    detail: '你与平台之间的约定：入驻条件、保证金、服务范围、平台可终止的情形。甲方固定为平台方（平台主体 + 系统租户）。',
    color: 'purple'
  },
  DISTRIBUTION: {
    label: '代销协议',
    shortDesc: '租户 ↔ 租户',
    detail: '一方供货、另一方代销：约定佣金比例、结算周期、退货与责任划分。两个租户对等，条款需要双方共同确认。',
    color: 'blue'
  },
  GOODS_FRAMEWORK: {
    label: '购销框架协议',
    shortDesc: '租户 ↔ 租户',
    detail: '双方长期按框架购销：约定价格口径、账期、交付与质量责任，具体订单按框架执行。两个租户对等。',
    color: 'cyan'
  },
  CONSUMER_PROMISE: {
    label: '消费者单方承诺',
    shortDesc: '租户 → 消费者',
    detail: '你对外（不特定消费者）的公开承诺：如无理由退货天数、退款到账时间。只能高于法定与平台底线，不能低于底线。',
    color: 'orange'
  }
}

/** 主档状态中文名 */
export const AGREEMENT_STATUS_TEXT: Record<AgreementStatus, string> = {
  DRAFT: '洽谈中',
  ACTIVE: '生效中',
  SUSPENDED: '已暂停',
  TERMINATED: '已终止'
}

/** 主档状态标签色 */
export const AGREEMENT_STATUS_COLOR: Record<AgreementStatus, string> = {
  DRAFT: 'default',
  ACTIVE: 'green',
  SUSPENDED: 'orange',
  TERMINATED: 'red'
}

/** 版本状态中文名 */
export const AGREEMENT_VERSION_STATUS_TEXT: Record<AgreementVersionStatus, string> = {
  DRAFT: '待双方确认',
  ACTIVE: '生效中',
  SUPERSEDED: '已被新版取代',
  REJECTED: '已否决'
}

/** 版本状态标签色 */
export const AGREEMENT_VERSION_STATUS_COLOR: Record<AgreementVersionStatus, string> = {
  DRAFT: 'blue',
  ACTIVE: 'green',
  SUPERSEDED: 'default',
  REJECTED: 'red'
}

// ── 接口 ─────────────────────────────────────────────────

export const agreementApi = {
  /**
   * 分页查询协议列表
   * @param params 查询条件（协议范围 / 协议类型 / 状态 / 关键字 + 分页）
   *               agreementScope 由服务端转成 SQL 条件（PLATFORM ⇒ 仅平台服务协议；
   *               TENANT ⇒ 排除平台服务协议），因此 total 就是过滤后的条数，前端不要再本地筛。
   */
  page(params: AgreementQuery): Promise<PageResponse<Agreement>> {
    return request.get('/agreement/page', params)
  },

  /**
   * 查询协议详情（含当前生效版本与条款）
   * @param id 协议 ID
   */
  getDetail(id: number | string): Promise<Agreement & { currentVersion?: AgreementVersion }> {
    return request.get(`/agreement/${id}`)
  },

  /**
   * 新建协议草稿
   * @param data 协议类型 + 两端主体与租户 + 标题 + 生效期
   * @returns 新建的协议 ID
   */
  create(data: AgreementCreateBody): Promise<number | string> {
    return request.post('/agreement', data)
  },

  /**
   * 修改协议草稿（仅「洽谈中」可改；条款随草稿版本一起保存）
   * @param id 协议 ID
   * @param data 主档字段 + versionId + terms（整份条款选择）
   */
  update(id: number | string, data: AgreementUpdateBody): Promise<void> {
    return request.put(`/agreement/${id}`, data)
  },

  /**
   * 删除协议草稿（仅「洽谈中」可删）
   * @param id 协议 ID
   */
  remove(id: number | string): Promise<void> {
    return request.delete(`/agreement/${id}`)
  },

  /**
   * 查询协议的全部版本历史（含草稿版本）
   * @param id 协议 ID
   */
  listVersions(id: number | string): Promise<AgreementVersion[]> {
    return request.get(`/agreement/${id}/versions`)
  },

  /**
   * 发起变更：从当前生效版本复制出一份新的草稿版本
   * 谈成之前，现行版本继续有效、交易照常按现行版本执行
   * @param id 协议 ID
   * @param data 变更原因
   * @returns 新草稿版本 ID
   */
  createVersion(id: number | string, data: AgreementVersionCreateBody): Promise<number | string> {
    return request.post(`/agreement/${id}/versions`, data)
  },

  /**
   * 查询某个版本的详情（含条款快照与双方确认痕迹）
   * @param versionId 版本 ID
   */
  getVersion(versionId: number | string): Promise<AgreementVersion> {
    return request.get(`/agreement/version/${versionId}`)
  },

  /**
   * 本方确认签署（双签之一）
   * 双方都确认过，版本才允许置为生效
   * @param versionId 版本 ID
   */
  confirmVersion(versionId: number | string): Promise<void> {
    return request.post(`/agreement/version/${versionId}/confirm`)
  },

  /**
   * 把版本置为生效
   * 后端会校验：必填条款已选完 + 双方确认齐全，缺一项都会被拒
   * @param versionId 版本 ID
   */
  activateVersion(versionId: number | string): Promise<void> {
    return request.post(`/agreement/version/${versionId}/activate`)
  },

  /**
   * 查询条款字典（按条款类别分组，每组一个下拉）
   */
  listTermOptionGroups(): Promise<AgreementTermOptionGroup[] | Record<string, AgreementTermOption[]>> {
    return request.get('/agreement/term-options/grouped')
  },

  /**
   * 查询某类条款的全部可选选项
   * @param termCode 条款类别
   */
  listTermOptions(termCode: string): Promise<AgreementTermOption[]> {
    return request.get('/agreement/term-options', { termCode })
  },

  /**
   * 新增条款字典选项（平台侧维护）
   * @param data 条款类别 + 选项编码 + 选项名 + 语义说明 + 是否必填
   */
  createTermOption(data: Partial<AgreementTermOption>): Promise<number | string> {
    return request.post('/agreement/term-options', data)
  },

  /**
   * 修改条款字典选项（平台侧维护）
   * @param id 选项 ID
   */
  updateTermOption(id: number | string, data: Partial<AgreementTermOption>): Promise<void> {
    return request.put(`/agreement/term-options/${id}`, data)
  },

  /**
   * 删除条款字典选项（平台侧维护；历史协议的快照不受影响）
   * @param id 选项 ID
   */
  deleteTermOption(id: number | string): Promise<void> {
    return request.delete(`/agreement/term-options/${id}`)
  },

  /**
   * 否决这一版草稿（协商里的「拒绝」动作）
   *
   * 否决后现行生效版本继续有效、交易照常 —— 与「确认」是同一个表态动作的两个分支，
   * 因此后端复用同一个权限码。
   * @param versionId 版本 ID
   * @param reason 否决原因（留痕）
   */
  rejectVersion(versionId: number | string, reason?: string): Promise<void> {
    return request.post(`/agreement/version/${versionId}/reject`, null, { params: { reason } })
  },

  // ══════════ 协议内容层：字段设定版 / 文字版 / 履约方式（§13.1 §13.2） ══════════

  /**
   * 字段元数据：协议里能约定哪些字段、怎么填、**会影响什么**（消费方）
   *
   * ⚠️ 消费方（consumerPointLabel / consumerSemantics）是给人看的：
   * 字段设定版是**运行时配置**，会被订单路由 / 发货 / 库存 / 定价 / 结算 / 开票 / 风控直接消费，
   * 填写的人必须在改之前就知道这一项会改变什么，不能做成一个普通表单。
   */
  listSettingDefs(): Promise<AgreementSettingDef[]> {
    return request.get('/agreement/content/setting-defs')
  },

  /**
   * 取某版本的协议内容（设定三态 + 文字条款 + 履约方式集合 + 字段元数据）
   * @param versionId 版本 ID
   */
  getContent(versionId: number | string): Promise<AgreementContent> {
    return request.get(`/agreement/content/${versionId}`)
  },

  /**
   * 保存协议内容（**整份覆盖**，不是增量合并）
   *
   * settings 里没出现的字段 / value 留空 ⇒ 回到「未约定」（不是保持原值）；
   * fulfillmentModes 传空数组 ⇒ 本版没有约定履约方式（空集 ≠ 随便用哪种）。
   * 仅草稿可改；保存成功会清空双方确认痕迹。
   */
  saveContent(versionId: number | string, data: AgreementContentSaveBody): Promise<AgreementContentSaveResult> {
    return request.put(`/agreement/content/${versionId}`, data)
  },

  /**
   * 按业务时点取生效那一版的设定（下单 / 发货 / 结算的执行口径）
   *
   * ⚠️ 必须带业务时点：读「当前版本」会让改协议篡改已发生交易的口径。
   * 某字段未约定时返回「未约定」而不是默认值。
   */
  getEffectiveSettings(agreementId: number | string, businessTime?: string): Promise<AgreementEffectiveSettings> {
    return request.get('/agreement/content/effective', { agreementId, businessTime })
  },

  // ══════════ 唯一送达（邀请，§13.5） ══════════

  /**
   * 发起唯一送达（收件方由服务端判定为「另一端」，前端不能指定发给谁）
   *
   * ⚠️ 明文 token / 短链**只在这一次响应里返回**，之后任何查询都只给前 8 位提示与邀请码。
   * @param agreementId 协议 ID
   */
  createInvite(agreementId: number | string, data?: AgreementInviteCreateBody): Promise<AgreementInvite> {
    return request.post(`/agreement/lifecycle/${agreementId}/invites`, data || {})
  },

  /** 邀请记录（发给谁、状态、浏览次数与首次查看时间、是否已被领取；不含明文 token） */
  listInvites(agreementId: number | string): Promise<AgreementInvite[]> {
    return request.get(`/agreement/lifecycle/${agreementId}/invites`)
  },

  /** 单条邀请详情 */
  inviteDetail(inviteId: number | string): Promise<AgreementInvite> {
    return request.get(`/agreement/lifecycle/invites/${inviteId}`)
  },

  /**
   * 受邀方领取并查看（token 或邀请码二选一）
   *
   * ⚠️ 后端会校验「这份契约不是发给你的」：未登录 / 租户不匹配 / 代表主体不匹配 /
   * 已过期 / 已使用 / 已撤回，各有各的中文原因。界面必须如实展示这些原因，
   * 不要吞成一句「打开失败」。
   */
  openInvite(data: AgreementInviteOpenBody): Promise<AgreementInvite> {
    return request.post('/agreement/lifecycle/invites/open', data)
  },

  /** 撤回邀请（撤回后链接立即失效；与「已过期」「已领取」是三种不同的失效） */
  revokeInvite(inviteId: number | string, reason?: string): Promise<AgreementInvite> {
    return request.post(`/agreement/lifecycle/invites/${inviteId}/revoke`, null, { params: { reason } })
  },

  // ══════════ 多轮协商（反要约，§13.4） ══════════

  /**
   * 提出修改 = 反要约 = 新建一个草稿版本（现行生效版本在协商期间继续有效）
   * @param agreementId 协议 ID
   * @returns 新草稿版本 ID
   */
  propose(agreementId: number | string, data: AgreementProposalBody): Promise<number | string> {
    return request.post(`/agreement/lifecycle/${agreementId}/proposals`, data)
  },

  /**
   * 版本差异（条款 / 设定 / 文字 / 履约方式逐条对比）
   * @param versionId 当前版本
   * @param againstVersionId 对比基准；不传 = 上一版
   */
  diffVersion(versionId: number | string, againstVersionId?: number | string): Promise<AgreementVersionDiff> {
    return request.get(`/agreement/lifecycle/version/${versionId}/diff`, { againstVersionId })
  },

  // ══════════ 签署（§13.6「自然人代表主体」） ══════════

  /**
   * 签署：必须回答「谁签的、凭什么代表这家公司」
   *
   * representedPartyId（代表哪个主体）与 authorityBasis（授权依据）必填；
   * 签署同时完成本方确认（复用同一套双签痕迹，不是两套机制）。
   */
  sign(versionId: number | string, data: AgreementSignBody): Promise<AgreementSignature> {
    return request.post(`/agreement/lifecycle/version/${versionId}/sign`, data)
  },

  /** 该版本的签署记录（七要素：谁 · 代表哪个主体 · 在哪个租户 · 凭什么代表 · 签了哪一版 · 何时 · 签章哈希） */
  listSignatures(versionId: number | string): Promise<AgreementSignature[]> {
    return request.get(`/agreement/lifecycle/version/${versionId}/signatures`)
  },

  // ══════════ 终止（§13.7 ⚠️ 终止 ≠ 免责） ══════════

  /** 发起终止（五种来源；协商一致需对方确认，其余发起即停止履行） */
  requestTermination(agreementId: number | string, data: AgreementTerminationCreateBody): Promise<AgreementTermination> {
    return request.post(`/agreement/lifecycle/${agreementId}/terminations`, data)
  },

  /** 终止记录（含依据、是否主张违约、对方异议） */
  listTerminations(agreementId: number | string): Promise<AgreementTermination[]> {
    return request.get(`/agreement/lifecycle/${agreementId}/terminations`)
  },

  /** 对方确认终止（协商一致 → 协议置为已终止 = 停止履行） */
  confirmTermination(id: number | string, data?: AgreementTerminationActionBody): Promise<AgreementTermination> {
    return request.post(`/agreement/lifecycle/terminations/${id}/confirm`, data || {})
  },

  /** 对方提异议（待确认时提 ⇒ 终止不成立；已终止后提 ⇒ 只留痕，不回滚终止事实） */
  objectTermination(id: number | string, data?: AgreementTerminationActionBody): Promise<AgreementTermination> {
    return request.post(`/agreement/lifecycle/terminations/${id}/object`, data || {})
  },

  /** 发起方撤回终止（仅「待对方确认」可撤） */
  withdrawTermination(id: number | string, data?: AgreementTerminationActionBody): Promise<AgreementTermination> {
    return request.post(`/agreement/lifecycle/terminations/${id}/withdraw`, data || {})
  },

  // ══════════ 契约模板（§13.9） ══════════

  /** 模板分页（平台侧可读全部含租户模板；租户只读平台模板 + 自己的） */
  templatePage(params: AgreementTemplateQuery): Promise<PageResponse<AgreementTemplate>> {
    return request.get('/agreement/templates/page', params)
  },

  /** 模板详情（含预填的条款 / 设定 / 文字；⚠️ 预填不等于已约定） */
  templateDetail(id: number | string): Promise<AgreementTemplateDetail> {
    return request.get(`/agreement/templates/${id}`)
  },

  /** 新建模板（平台模板只有平台侧能建） */
  createTemplate(data: AgreementTemplateSaveBody): Promise<number | string> {
    return request.post('/agreement/templates', data)
  },

  /** 修改模板（三份内容清单不传 = 不动、传空数组 = 清空） */
  updateTemplate(id: number | string, data: AgreementTemplateSaveBody): Promise<void> {
    return request.put(`/agreement/templates/${id}`, data)
  },

  /** 删除模板（软删；已基于它发起的协议不受影响） */
  deleteTemplate(id: number | string): Promise<void> {
    return request.delete(`/agreement/templates/${id}`)
  },

  /**
   * 从模板发起契约：建主档 + 首个草稿版本，并把模板内容**预填**进去
   *
   * ⚠️ 预填 ≠ 已约定：模板是显式选择的起点，不是自动套用的默认值；
   * 是否成为约定取决于双方在那一版上的确认与签署。
   * @returns 新建的协议 ID
   */
  applyTemplate(templateId: number | string, data: AgreementFromTemplateBody): Promise<number | string> {
    return request.post(`/agreement/templates/${templateId}/apply`, data)
  }
}

// ══════════════════════════════════════════════════════════
// 协议内容层：字段设定版（运行时配置） / 文字版（只留痕举证） / 履约方式
// 依据：§13.1「契约有两条腿」§13.2「三种消费时机」§13.10「履约方式可多种并存」
// ══════════════════════════════════════════════════════════

/** 设定字段的取值类型（决定界面用哪个控件，也决定后端按哪一列存） */
export type AgreementSettingValueType = 'ENUM' | 'NUMBER' | 'TEXT' | 'BOOL' | 'DATE' | 'DURATION'

/**
 * 设定项的三态（⚠️ 不是装饰字段，界面必须如实区分）
 *   · AGREED     已约定 —— 显示值；
 *   · UNDECLARED 未约定 —— 必须显示「未约定」并说明"系统不会按默认值执行"，**不能显示成 0 或空**；
 *   · UNDEFINED  未定义 —— 该字段已不在平台字典里（配置问题），提示联系平台。
 */
export type AgreementSettingState = 'AGREED' | 'UNDECLARED' | 'UNDEFINED'

/** 字段元数据：「协议里可以约定哪些字段、怎么填、会影响什么」 */
export interface AgreementSettingDef {
  id?: number | string
  settingKey: string
  label?: string
  valueType?: AgreementSettingValueType
  valueTypeLabel?: string
  /** ENUM 的候选值（界面渲染下拉）；非 ENUM 为空 */
  options?: string[]
  /** 是否必填；未约定必填项时保存回执会列出来 */
  required?: boolean
  /** 消费方编码（如 AR_DUE_DATE） */
  consumerPoint?: string
  /** 消费方中文名（如「应收应付到期日」）—— 字段旁边显示"这一项会影响什么" */
  consumerPointLabel?: string
  /** 消费方语义说明 */
  consumerSemantics?: string
  /** 字段自身说明 */
  semantics?: string
  sort?: number
  /** 1=启用 / 0=停用 */
  status?: number
}

/** 某个版本里一个设定项的三态取值 */
export interface AgreementSettingItem {
  settingKey: string
  label?: string
  valueType?: AgreementSettingValueType
  valueTypeLabel?: string
  state?: AgreementSettingState
  stateLabel?: string
  /** 原始值（字符串形态，便于表单回填）；未约定时为空 */
  value?: string | null
  /** 展示值（DURATION 会带「天」、BOOL 会转成「是/否」） */
  displayValue?: string | null
  required?: boolean
  consumerPoint?: string
  consumerPointLabel?: string
  semantics?: string
  remark?: string
  options?: string[]
}

/** 文字条款（⚠️ autoExecutable 恒为 false：系统不会自动执行，界面必须显式告知） */
export interface AgreementNarrative {
  id?: number | string
  sectionCode: string
  sectionTitle?: string
  contentText?: string
  /** 正文哈希（SHA-256）：证明双方确认后这段文字没被改过 */
  contentHash?: string
  /** 恒为 false：文字条款没有消费方 */
  autoExecutable?: boolean
  /** 与 autoExecutable 配套的中文说明，界面直接展示（不要自己编文案） */
  manualNotice?: string
  partyAConfirmedAt?: string | null
  partyBConfirmedAt?: string | null
  sort?: number
}

/** 履约方式的一项（返回值是**集合**：同城直发 + 异地中转可以并存，不要渲染成单选） */
export interface AgreementFulfillmentMode {
  /** DROP_SHIP 直发 / TRANSIT_STOCK 中转 / PICKUP 自提 / LOCAL_STOCK 自有库存 */
  mode: string
  modeLabel?: string
  scopeNote?: string
  sort?: number
}

/** 某版本的协议内容（一次取全：设定版 + 文字版 + 履约方式 + 元数据） */
export interface AgreementContent {
  agreementId?: number | string
  versionId?: number | string
  versionNo?: number
  versionStatus?: AgreementVersionStatus
  versionStatusLabel?: string
  /** 本版内容现在能不能改（只有草稿能改；已生效版本只读） */
  editable?: boolean
  /** 不能改时的一句话说明（直接展示给用户） */
  editableHint?: string
  settings?: AgreementSettingItem[]
  /** 必填但还没约定的字段编码 */
  missingRequiredSettings?: string[]
  /** 必填但还没约定的字段中文名 */
  missingRequiredSettingNames?: string[]
  narratives?: AgreementNarrative[]
  fulfillmentModes?: AgreementFulfillmentMode[]
  agreedSettingKeys?: string[]
  settingDefs?: AgreementSettingDef[]
  /** 从模板发起时的来源留痕（如「基于模板「标准代销模板」（平台模板）起草，第 1 版」）；从零起草为空 */
  templateSourceText?: string
}

/** 保存协议内容的请求体（整份覆盖） */
export interface AgreementContentSaveBody {
  /** 未出现的字段 ⇒ 回到「未约定」；value 留空 ⇒ 同样回到未约定 */
  settings?: Array<{ settingKey: string; value?: string | null; remark?: string }>
  /** 未出现的段落即删除 */
  narratives?: Array<{ sectionCode: string; contentText?: string }>
  /** 整份覆盖；空数组 = 本版未约定履约方式 */
  fulfillmentModes?: string[]
}

/** 保存协议内容的回执（点一次保存就能看出离「能生效」还差什么） */
export interface AgreementContentSaveResult {
  agreementId?: number | string
  versionId?: number | string
  savedSettingCount?: number
  savedNarrativeCount?: number
  fulfillmentModes?: string[]
  missingRequiredSettings?: string[]
  missingRequiredSettingNames?: string[]
  /** 文字条款的固定提示（系统不会自动执行） */
  narrativeNotice?: string
}

/** 按业务时点取到的生效设定 */
export interface AgreementEffectiveSettings {
  agreementId?: number | string
  versionId?: number | string
  versionNo?: number
  businessTime?: string
  settings?: AgreementSettingItem[]
  undeclaredCount?: number
  resolvedNote?: string
  [key: string]: any
}

// ══════════════════════════════════════════════════════════
// 唯一送达（邀请） / 签署 / 终止
// ══════════════════════════════════════════════════════════

/** 邀请（唯一送达）—— 明文 token 只在创建那一次返回 */
export interface AgreementInvite {
  id: number | string
  agreementId?: number | string
  agreementNo?: string
  agreementTitle?: string
  /** 绑定的目标文稿版本 */
  versionId?: number | string
  versionNo?: number
  /** 目标主体 + 目标租户 */
  targetPartyId?: number | string
  targetPartyName?: string
  targetTenantId?: number | string
  targetTenantName?: string
  targetSide?: string
  targetSideLabel?: string
  /** 0=待领取 / 1=已领取 / 2=已撤回 / 3=已过期 */
  status?: number
  statusLabel?: string
  channel?: string
  channelLabel?: string
  /** 有效期截止时刻 */
  expiresAt?: string
  /** 领取留痕（一次性） */
  acceptedBy?: number | string | null
  acceptedAt?: string | null
  acceptChannel?: string
  acceptedPartyId?: number | string | null
  /** 查看留痕：浏览次数 / 首次查看人与时间 */
  viewCount?: number
  firstViewedBy?: number | string | null
  firstViewedAt?: string | null
  firstViewChannel?: string
  revokeBy?: number | string | null
  revokeAt?: string | null
  revokeReason?: string
  createdBy?: number | string
  createdAt?: string
  /** 明文 token —— **只在下发（创建）那一次返回**；后续查询为空 */
  token?: string
  /** 明文 token 前 8 位（客诉/运维在库里对记录用） */
  tokenHint?: string
  /** 人读邀请码（可口述、可人工输入） */
  inviteCode?: string
  /** 可供点开的短链（站内相对路径）；**只在下发那一次返回** */
  shortLink?: string
  /** 使用说明（中文白话，直接显示） */
  usageNote?: string
}

/** 发起唯一送达的请求体（⚠️ 不能指定"发给谁"：收件方由服务端判定为另一端） */
export interface AgreementInviteCreateBody {
  /** 绑定到哪一版文稿；不传 = 当前待双方确认的草稿版本 */
  versionId?: number | string
  /** 送达渠道：QRCODE / LINK；不传按 LINK 记（渠道只用于留痕） */
  channel?: string
  /** 有效期（小时），1~720，默认 168（7 天） */
  expiresInHours?: number
}

/** 受邀方打开邀请（token 与 inviteCode 二选一） */
export interface AgreementInviteOpenBody {
  token?: string
  inviteCode?: string
  /** 打开者声明的代表主体 ID —— 必须与邀请绑定的目标主体一致，否则就是「这份契约不是发给你的」 */
  representedPartyId: number | string
  /** 打开渠道：QRCODE / LINK */
  channel?: string
}

/** 签署记录（七要素：谁 · 代表哪个主体 · 在哪个租户 · 凭什么代表 · 签了哪一版 · 何时 · 签章哈希） */
export interface AgreementSignature {
  id?: number | string
  agreementId?: number | string
  versionId?: number | string
  versionNo?: number
  /** A / B */
  partySide?: string
  partySideLabel?: string
  partyId?: number | string
  partyName?: string
  signerPartyTenantId?: number | string
  signerPartyTenantName?: string
  signerUserId?: number | string
  signerPersonId?: number | string
  signerName?: string
  /** 授权依据（凭什么代表这家公司签字） */
  authorityBasis?: string
  /** 授权凭证号（外部单据编号） */
  authorityEvidenceNo?: string
  /** 签章哈希 */
  signHash?: string
  signedAt?: string
  signChannel?: string
  signatureType?: string
  remark?: string
  /** 这条签署对**当前**快照是否仍然有效（内容被改过则为 false，需重新签署） */
  effective?: boolean
  effectiveNote?: string
}

/** 签署请求体（representedPartyId 与 authorityBasis 必填） */
export interface AgreementSignBody {
  representedPartyId: number | string
  personId?: number | string
  signerName?: string
  /** 授权依据，如「法定代表人本人」「授权委托书（含授权范围与限额）」 */
  authorityBasis: string
  authorityEvidenceNo?: string
  channel?: string
  remark?: string
}

/** 终止来源（五种，业务白话） */
export type AgreementTerminationSourceCode =
  | 'MUTUAL_AGREEMENT' | 'NATURAL_EXPIRY' | 'UNILATERAL' | 'COUNTERPARTY_BREACH' | 'PLATFORM_EXPULSION'

/** 终止记录（⚠️ 刻意没有"责任是否了结/是否结清/是否免责"的字段：终止 ≠ 免责） */
export interface AgreementTermination {
  id: number | string
  agreementId?: number | string
  agreementNo?: string
  agreementTitle?: string
  /** 终止时正在执行的那一版 */
  versionId?: number | string
  versionNo?: number
  source?: AgreementTerminationSourceCode | string
  sourceLabel?: string
  /** 0=待对方确认 / 1=已终止 / 2=对方有异议 / 3=已撤回 */
  status?: number
  statusLabel?: string
  requestedBy?: number | string
  requestedSide?: string
  requestedSideLabel?: string
  requestedAt?: string
  /** 终止依据（引用协议条款或法定情形） */
  basisText?: string
  /** 是否**主张**对方违约（当事人的主张，不是平台的认定） */
  claimCounterpartyBreach?: boolean
  breachNote?: string
  /** 「停止履行」的生效时刻 —— 本模块记的就是这个**事实** */
  stopPerformanceAt?: string
  effectiveAt?: string
  counterpartyActionBy?: number | string | null
  counterpartyActionAt?: string | null
  counterpartyObjection?: boolean | null
  objectionReason?: string
  withdrawnBy?: number | string | null
  withdrawnAt?: string | null
  withdrawReason?: string
  createdAt?: string
  /** 「终止 ≠ 免责」的固定提示 —— 界面必须显示，且**不要自己编文案** */
  exemptionNotice?: string
  /** 当前会话能否对这条记录表态（服务端判定，前端不用猜） */
  canConfirm?: boolean
  canObject?: boolean
  canWithdraw?: boolean
}

/** 发起终止的请求体 */
export interface AgreementTerminationCreateBody {
  source: AgreementTerminationSourceCode | string
  /** 终止依据（必填）：引用协议条款或法定情形 */
  basisText: string
  claimCounterpartyBreach?: boolean
  breachNote?: string
  /** 从何时起停止履行；不传 = 立即（⚠️ 只是停止履行的事实，不含结清与免责） */
  stopPerformanceAt?: string
}

/** 终止的确认 / 异议 / 撤回共用的请求体 */
export interface AgreementTerminationActionBody {
  reason?: string
}

// ══════════════════════════════════════════════════════════
// 逐条 diff（协商要能看清改了什么） / 协商提案
// ══════════════════════════════════════════════════════════

/** 一处差异（条款 / 设定 / 文字 / 履约方式四类共用一张表） */
export interface AgreementDiffItem {
  /** ADDED 新增 / REMOVED 删除 / CHANGED 修改 */
  changeType: 'ADDED' | 'REMOVED' | 'CHANGED' | string
  changeTypeLabel?: string
  /** 编码：termCode / settingKey / sectionCode / mode */
  code?: string
  /** 中文名（条款名 / 字段名 / 段落名 / 履约方式名） */
  label?: string
  beforeCode?: string
  beforeText?: string
  afterCode?: string
  afterText?: string
}

/** 本版 vs 基准版的逐条差异 */
export interface AgreementVersionDiff {
  agreementId?: number | string
  versionId?: number | string
  versionNo?: number
  againstVersionId?: number | string
  againstVersionNo?: number
  /** 基准的人读说明，如「上一版（第 1 版）」 */
  againstLabel?: string
  /** 两份快照原文是否逐字一致（false 说明条款/期限被改过） */
  snapshotChanged?: boolean
  terms?: AgreementDiffItem[]
  settings?: AgreementDiffItem[]
  narratives?: AgreementDiffItem[]
  fulfillmentModes?: AgreementDiffItem[]
  /** 人读的差异摘要，界面直接显示 */
  summary?: string[]
}

/** 协商提案（反要约）的请求体 */
export interface AgreementProposalBody {
  /** 变更原因（正式留痕，必填） */
  changeReason: string
  /** 协商留言（对话式留痕） */
  proposalNote?: string
  /** 以哪一版为基准改；不传 = 当前待确认的草稿版本（没有草稿时 = 现行生效版本） */
  baseVersionId?: number | string
}

// ══════════════════════════════════════════════════════════
// 契约模板（§13.9：平台模板 / 租户模板两级，模板不是默认值）
// ══════════════════════════════════════════════════════════

/** 契约模板（列表行） */
export interface AgreementTemplate {
  id: number | string
  /** PLATFORM 平台模板 / TENANT 租户模板 */
  scope?: 'PLATFORM' | 'TENANT' | string
  scopeLabel?: string
  tenantId?: number | string
  tenantName?: string
  templateName?: string
  agreementType?: AgreementType | string
  agreementTypeLabel?: string
  description?: string
  /** PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法（REJECTED 不许用于发起） */
  legalReviewStatus?: string
  legalReviewStatusLabel?: string
  status?: number
  statusLabel?: string
  /** 当前会话能否修改/删除它 —— 服务端算好的，前端据此决定按钮是否可用 */
  manageable?: boolean
  /** 不能改时的原因（直接展示） */
  manageableHint?: string
  settingCount?: number
  termCount?: number
  narrativeCount?: number
  createTime?: string
  updateTime?: string
}

/** 模板详情（含预填的三部分内容：⚠️ 预填不是某个版本的约定值） */
export interface AgreementTemplateDetail extends AgreementTemplate {
  settings?: AgreementSettingItem[]
  terms?: AgreementTermVO[]
  narratives?: AgreementNarrative[]
}

/** 新建 / 修改模板的请求体（三份清单整份覆盖：不传 = 不动、传空数组 = 清空） */
export interface AgreementTemplateSaveBody {
  scope?: 'PLATFORM' | 'TENANT' | string
  templateName?: string
  agreementType?: AgreementType | string
  description?: string
  /** PENDING / APPROVED / REJECTED */
  legalReviewStatus?: string
  /** 1=启用 / 0=停用 */
  status?: number
  settings?: Array<{ settingKey: string; value?: string | null; remark?: string }>
  terms?: Array<{ termCode: string; optionCode?: string; paramValue?: string | number | null }>
  narratives?: Array<{ sectionCode: string; contentText?: string }>
}

/** 模板列表查询条件（可见性由服务端判定，不接受前端传"看谁的模板"） */
export interface AgreementTemplateQuery {
  current?: number
  size?: number
  scope?: 'PLATFORM' | 'TENANT' | string
  agreementType?: AgreementType | string
  keyword?: string
  status?: number
}

/** 从模板发起契约的请求体 */
export interface AgreementFromTemplateBody {
  partyAId: number | string
  partyATenantId: number | string
  partyBId?: number | string | null
  partyBTenantId?: number | string | null
  title: string
  /** 不传则取模板上登记的类型 */
  agreementType?: AgreementType | string
  effectiveFrom?: string
  effectiveTo?: string
}

// ══════════════════════════════════════════════════════════
// 枚举 → 业务白话（界面一律不出现类型码）
// ══════════════════════════════════════════════════════════

/** 履约方式全量候选（⚠️ 协议上是集合、可多选并存，界面必须用多选控件） */
export const FULFILLMENT_MODE_OPTIONS: Array<{ value: string; label: string; desc: string }> = [
  { value: 'DROP_SHIP', label: '直发', desc: '货从供货方直接发给最终客户，中间方不碰货' },
  { value: 'TRANSIT_STOCK', label: '中转', desc: '货先经过中间方的仓，再发给客户（两段运输）' },
  { value: 'PICKUP', label: '自提', desc: '客户到指定地点自取' },
  { value: 'LOCAL_STOCK', label: '自有库存', desc: '由销售方用自己的库存交付（就近履约）' }
]

/** 文字条款的段落类别（界面按类别分组填写） */
export const NARRATIVE_SECTION_OPTIONS: Array<{ value: string; label: string; placeholder: string }> = [
  { value: 'DISPUTE', label: '争议解决与管辖', placeholder: '例如：因本协议发生争议，双方先协商；协商不成，提交甲方所在地人民法院诉讼解决。' },
  { value: 'CONFIDENTIALITY', label: '保密', placeholder: '例如：双方对合作中知悉的对方客户名单、价格与结算数据负有保密义务，保密期为协议终止后两年。' },
  { value: 'FORCE_MAJEURE', label: '不可抗力', placeholder: '例如：因地震、疫情管控等不可抗力无法履约的，受影响方应及时通知对方并提供证明，可相应顺延履行。' },
  { value: 'SPECIAL_TERMS', label: '特别约定', placeholder: '双方自定的、不属于其它类别的补充条款。' },
  { value: 'BREACH_LIABILITY_TEXT', label: '违约责任（文字表述）', placeholder: '例如：逾期付款的，每逾期一日按未付金额的万分之五向对方支付违约金。' }
]

/**
 * 文字条款的兜底提示：后端未下发 manualNotice 时用这一句。
 * ⚠️ 口径与后端 AgreementNarrativeSection.MANUAL_NOTICE 一致 —— 文案的唯一来源在服务端，
 * 这里只是网络异常/字段缺失时的兜底，不能靠它当正稿。
 */
export const NARRATIVE_MANUAL_NOTICE_FALLBACK =
  '此类条款为文字条款：系统不会自动执行，需人工处理（仅供双方确认与留痕举证）'

/** 终止来源：五种，业务白话 + 是否需对方确认（协商一致才需要） */
export const TERMINATION_SOURCE_OPTIONS: Array<{
  value: AgreementTerminationSourceCode
  label: string
  desc: string
  requiresCounterpartyConfirm: boolean
}> = [
  {
    value: 'MUTUAL_AGREEMENT',
    label: '协商一致',
    desc: '双方商量好了要停。需要对方确认后协议才置为已终止',
    requiresCounterpartyConfirm: true
  },
  {
    value: 'NATURAL_EXPIRY',
    label: '自然到期',
    desc: '按约定的有效期到期而终止，不需要对方确认，但对方仍可提异议',
    requiresCounterpartyConfirm: false
  },
  {
    value: 'UNILATERAL',
    label: '单方终止',
    desc: '一方自行停止履行。⚠️ 单方终止不等于不违约 —— 只记「停止履行」这个事实，违约与否另行处理',
    requiresCounterpartyConfirm: false
  },
  {
    value: 'COUNTERPARTY_BREACH',
    label: '因对方违约',
    desc: '以对方违约为由终止。⚠️「违约」是你的主张，平台不认定违约、不判赔多少',
    requiresCounterpartyConfirm: false
  },
  {
    value: 'PLATFORM_EXPULSION',
    label: '平台清退',
    desc: '平台按入驻规则清退。留痕并允许对方提异议（异议即申诉留痕）',
    requiresCounterpartyConfirm: false
  }
]

/** 邀请状态（0=待领取 / 1=已领取 / 2=已撤回 / 3=已过期，四种状态分别可断言） */
export const INVITE_STATUS_TEXT: Record<number, string> = {
  0: '待领取',
  1: '已领取',
  2: '已被发起方撤回',
  3: '已过期'
}

export const INVITE_STATUS_COLOR: Record<number, string> = {
  0: 'blue',
  1: 'green',
  2: 'default',
  3: 'orange'
}

/**
 * 「终止 ≠ 免责」提示的**兜底**文案（正稿在服务端）。
 *
 * ⚠️ 正稿只有一个来源：`AgreementTerminationVO.exemptionNotice`
 * （后端常量 `AgreementTerminationRules.NO_EXEMPTION_NOTICE`），界面一律优先用它。
 * 这一份只用于「这份协议还一条终止记录都没有、拿不到后端文案」的发起场景，
 * 文字与后端常量逐字一致；后端改口径时必须同步改这里，其余场合不得使用本常量。
 */
export const NO_EXEMPTION_NOTICE_MIRROR =
  '终止只表示「从终止之日起停止履行」，系统不会自动结清货款、不会自动免除任何责任；'
  + '是否存在违约、要不要赔偿，由双方自行协商或通过司法途径解决（平台不裁判）。'

/** 终止记录状态（0=待对方确认 / 1=已终止 / 2=对方有异议 / 3=已撤回） */
export const TERMINATION_STATUS_TEXT: Record<number, string> = {
  0: '待对方确认',
  1: '已终止（停止履行）',
  2: '对方有异议',
  3: '已撤回'
}

export const TERMINATION_STATUS_COLOR: Record<number, string> = {
  0: 'orange',
  1: 'red',
  2: 'volcano',
  3: 'default'
}

/** 模板级别 */
export const TEMPLATE_SCOPE_TEXT: Record<string, string> = {
  PLATFORM: '平台模板',
  TENANT: '租户模板'
}

export const TEMPLATE_SCOPE_COLOR: Record<string, string> = {
  PLATFORM: 'purple',
  TENANT: 'blue'
}

/** 模板法务审核状态（㊲：违法条款无效） */
export const LEGAL_REVIEW_TEXT: Record<string, string> = {
  PENDING: '待法务审核',
  APPROVED: '已通过法务审核',
  REJECTED: '含不合法条款（不可用于发起）'
}

export const LEGAL_REVIEW_COLOR: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red'
}

/** 设定字段的取值类型中文名（后端会下发 valueTypeLabel，这里只作兜底） */
export const SETTING_VALUE_TYPE_TEXT: Record<string, string> = {
  ENUM: '下拉选择',
  NUMBER: '数字',
  TEXT: '文本',
  BOOL: '是/否',
  DATE: '日期',
  DURATION: '天数'
}

/**
 * 设定字段的枚举候选值 → 中文白话。
 *
 * ⚠️ 后端只下发**编码**（如 options = ['OWN_STOCK','ALLOCATED','DROPSHIP_NO_STOCK']），
 * 中文展示口径收敛在这一张表里 —— 页面里不要再各写一份，否则迟早两处不一致。
 * 平台新增设定字段时在这里补一条。
 */
export const SETTING_ENUM_OPTION_TEXT: Record<string, Record<string, string>> = {
  FULFILLMENT_MODES: { DROP_SHIP: '直发', TRANSIT_STOCK: '中转', PICKUP: '自提', LOCAL_STOCK: '自有库存' },
  STOCK_MODE: { OWN_STOCK: '自有库存', ALLOCATED: '分配额度', DROPSHIP_NO_STOCK: '直发不占本仓库存' },
  AR_CREDIT_PROVIDER: { PARTY_A: '甲方给乙方账期', PARTY_B: '乙方给甲方账期' },
  COMMISSION_BEARER: { BUYER: '买方承担（加价）', SELLER: '卖方承担（让利）', SPLIT: '双方分摊' },
  INVOICE_ISSUER: { PARTY_A: '由甲方开票', PARTY_B: '由乙方开票', PLATFORM: '由平台开票' },
  CANCEL_BEARER: { BUYER: '买方承担', SELLER: '卖方承担', BOTH_AGREE: '双方协商', NO_ONE: '无人承担（不扣款）' },
  RETURN_PATH: {
    TO_SUPPLIER: '退回供货方', TO_SELLER: '退回销售方', DIRECT_TO_END: '直接退终端', NO_RETURN: '不支持退货'
  },
  RETURN_FREIGHT_BEARER: { BUYER: '买方承担', SELLER: '卖方承担', BOTH_AGREE: '双方协商' },
  FORWARD_FREIGHT_BEARER: { BUYER: '买方承担', SELLER: '卖方承担', BOTH_AGREE: '双方协商' },
  QUALITY_LIABILITY: {
    SUPPLIER: '供货方负责', SELLER: '销售方负责', MANUFACTURER: '生产厂家负责', BOTH_AGREE: '双方协商'
  }
}

/** 取某个字段某个候选值的中文名（取不到就退回编码本身，不隐藏事实） */
export function settingEnumOptionLabel(settingKey: string, code: string): string {
  return SETTING_ENUM_OPTION_TEXT[settingKey]?.[code] || code
}

/** 协议一端（甲方 / 乙方）中文名 */
export const PARTY_SIDE_TEXT: Record<string, string> = {
  A: '甲方',
  B: '乙方',
  NONE: '非缔约方'
}

export default agreementApi
