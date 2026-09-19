import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 租户信息
 *
 * ⚠️ 字段名与后端实体 `cn.aiedge.base.entity.SysTenant` **逐字对齐**（2026-09-18 复核）：
 * 此前这里的 `contactName` / `expireDate` / `maxUsers` / `description` 在后端**都不存在**，
 * 导致「联系人」「到期时间」两列恒空、弹窗 4 个字段静默丢弃。
 * 正确字段名为 `contactPerson` / `expireTime`；`maxUsers` 属配额表（sys_tenant_quota），不在租户表。
 */
export interface TenantInfo {
  id: number
  tenantCode: string
  tenantName: string
  contactPerson?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  remark?: string
  status: number
  /** 租户管理员用户ID */
  adminUserId?: number
  /** 租户等级：basic-基础版 professional-专业版 enterprise-企业版 */
  level?: string
  expireTime?: string
  createTime?: string
  updateTime?: string
}

/** 租户查询参数 */
export interface TenantQuery {
  tenantName?: string
  tenantCode?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 租户配置信息 */
export interface TenantConfigInfo {
  id: number
  tenantId: number
  logo?: string
  themeColor?: string
  features?: string
  maxUsers?: number
  expireDate?: string
}

/** 租户 API */
export const tenantApi = {
  /** 分页查询租户 */
  getPage(params: TenantQuery): Promise<ApiResponse<PageResponse<TenantInfo>>> {
    return request.get('/tenant/page', params)
  },

  /** 获取租户详情 */
  getById(id: number): Promise<ApiResponse<TenantInfo>> {
    return request.get(`/tenant/${id}`)
  },

  /** 创建租户 */
  create(data: Partial<TenantInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/tenant', data)
  },

  /** 更新租户 */
  update(id: number, data: Partial<TenantInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/tenant/${id}`, data)
  },

  /** 删除租户 */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/tenant/${id}`)
  },

  /** 批量删除租户 */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/tenant/batch', { data: ids })
  },

  /** 更新租户状态（后端是 @PatchMapping，此前用 PUT → 405 Method Not Allowed） */
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.patch(`/tenant/${id}/status`, null, { params: { status } })
  },

  /** 获取租户配置 */
  getConfig(tenantId: number): Promise<ApiResponse<TenantConfigInfo>> {
    return request.get(`/tenant/${tenantId}/config`)
  },

  /** 更新租户配置 */
  updateConfig(tenantId: number, data: Partial<TenantConfigInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/tenant/${tenantId}/config`, data)
  }
}

// ── 企业信息（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）──

/**
 * 当前租户的企业档案。
 *
 * 后端契约（2026-09-18 拆表前后**字段名与类型完全不变**）：
 *   GET /api/tenant/current  → 读取会话所属租户的档案（响应 = `CompanyProfileVO`）
 *   PUT /api/tenant/current  → 保存会话所属租户的档案（后端只接受档案列，平台列不可改）
 * 路径里**没有 id**：租户 ID 由后端取会话，前端不再从 localStorage 取 tenantId
 * （历史做法可改到任意租户行）。
 *
 * ⚠️ 后端已把企业档案列从 `sys_tenant` 拆到 1:1 子表 `sys_tenant_profile`（迁移 V11.420.0，
 * 让主表回到 ≤25 列的规范内），`CompanyProfileVO` 只是把两张表拼成同一份扁平响应。
 * 因此**本文件的字段名不要跟着后端表结构改**：改了就与响应脱钩（前端全页静默变空）。
 *
 * 注意：id 是 BIGINT 雪花 ID，一律按字符串处理，禁止 Number(id)。
 */
export interface CompanyProfile {
  /** 租户 ID（雪花 ID，字符串） */
  id: string
  /** 企业名称（对应后端 tenantName，必填） */
  tenantName: string
  /** 租户编码（平台维护，只读展示） */
  tenantCode?: string
  /** 统一社会信用代码 */
  creditCode?: string
  /** 企业类型 */
  companyType?: string
  /** 法定代表人 */
  legalPerson?: string
  /** 注册资本（万元） */
  registeredCapital?: number
  /** 所属行业 */
  industry?: string
  /** 企业规模 */
  companyScale?: string
  /** 成立日期（yyyy-MM-dd） */
  establishDate?: string
  /** 经营范围 */
  businessScope?: string
  /** 联系人 */
  contactPerson?: string
  /** 联系电话 */
  contactPhone?: string
  /** 企业邮箱 */
  contactEmail?: string
  /** 企业地址 */
  address?: string
  /** 联系人电话 */
  contactPersonPhone?: string
  /** 联系人邮箱 */
  contactPersonEmail?: string
  /** 纳税人识别号 */
  taxNumber?: string
  /** 纳税人信息 - 地址 */
  taxpayerAddress?: string
  /** 纳税人信息 - 电话 */
  taxpayerPhone?: string
  /** 纳税人信息 - 开户行地址 */
  bankName?: string
  /** 纳税人信息 - 开户行账号 */
  bankAccount?: string
  /** 企业 LOGO 地址（`POST /file/upload` 返回的 url，形如 `/api/file/view/...`；未上传为 null） */
  logoUrl?: string
  /** 租户等级（basic/professional/enterprise，平台维护，只读展示） */
  level?: string
  /** 租户状态（0-启用 1-停用，平台维护，只读展示） */
  status?: number
  /** 注册时间（只读展示） */
  createTime?: string
  /** 到期时间（只读展示） */
  expireTime?: string
}

/** 企业信息 API（当前租户自身档案，路径中不带 id） */
export const companyProfileApi = {
  /** 读取当前租户的企业档案 */
  getCurrent(): Promise<ApiResponse<CompanyProfile>> {
    return request.get('/tenant/current')
  },

  /** 保存当前租户的企业档案（只提交档案字段） */
  save(data: Partial<CompanyProfile>): Promise<ApiResponse<boolean>> {
    return request.put('/tenant/current', data)
  }
}

// ── 租户注册审批 API ──────────────────────────────────────

/** 租户注册请求 */
export interface TenantRegisterForm {
  tenantName: string
  tenantCode: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  adminUsername: string
  adminPassword: string
  adminEmail: string
}

/** 租户审批 API */
export const tenantApprovalApi = {
  /**
   * 租户自助注册（公开接口）。
   *
   * ⚠️ 这四条路径**必须带 `tenant-registration` 前缀**：
   *    后端 `TenantRegistrationController` 的类级映射是 `@RequestMapping("/api/tenant-registration")`，
   *    而 `TenantController` 占的是 `/api/tenant`（且带 `@GetMapping("/{id}")`）。
   *    原先写成 `/tenant/xxx` → 命中 `TenantController` 的 `/{id}`，
   *    `"pending"` 转 Long 失败 → **400「参数[id]格式不正确」**（四个接口全中招，
   *    连带租户自助注册页也是坏的）。改前缀后实测 `/tenant-registration/pending` → 200。
   */
  register(data: TenantRegisterForm): Promise<ApiResponse<any>> {
    return request.post('/tenant-registration/register', data, { _skipAuthRefresh: true })
  },

  /** 审批通过租户 */
  approve(id: number, remark?: string): Promise<ApiResponse<void>> {
    return request.post(`/tenant-registration/${id}/approve`, remark ? { remark } : undefined)
  },

  /** 驳回租户注册 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/tenant-registration/${id}/reject`, { reason })
  },

  /** 查询待审核租户列表 */
  getPending(): Promise<ApiResponse<SysTenant[]>> {
    return request.get('/tenant-registration/pending')
  }
}

/** 待审核租户接口（后端 SysTenant 实体的前端表示） */
export interface SysTenant {
  id: number
  tenantName: string
  tenantCode: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  address?: string
  adminUserId?: number
  level?: string
  status: number
  remark?: string
  createTime: string
}

// ── 租户套餐 API ──────────────────────────────────────

/** 租户套餐 */
export interface TenantPackageInfo {
  id: number
  packageName: string
  packageCode: string
  purchaseType: string
  price?: number
  maxUsers?: number
  storageQuota?: number
  apiCallLimit?: number
  status: number
  description?: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

/** 租户套餐 API */
export const tenantPackageApi = {
  /** 获取套餐列表 */
  getList(): Promise<{ records: TenantPackageInfo[]; total: number }> {
    return request.get('/tenant-package/list')
  },

  /** 获取套餐详情 */
  getById(id: number): Promise<TenantPackageInfo> {
    return request.get(`/tenant-package/${id}`)
  },

  /** 创建套餐 */
  create(data: Partial<TenantPackageInfo>): Promise<TenantPackageInfo> {
    return request.post('/tenant-package', data)
  },

  /** 更新套餐 */
  update(id: number, data: Partial<TenantPackageInfo>): Promise<TenantPackageInfo> {
    return request.put(`/tenant-package/${id}`, data)
  },

  /** 删除套餐 */
  delete(id: number): Promise<boolean> {
    return request.delete(`/tenant-package/${id}`)
  }
}

// ── 租户配额 API ──────────────────────────────────────

/** 租户配额 */
export interface TenantQuotaInfo {
  id: number
  tenantId: number
  tenantName: string
  tenantCode: string
  maxUsers: number
  maxStorage: string
  maxApiCalls: number
  usedUsers?: number
  usedStorage?: string
  usedApiCalls?: number
  createTime?: string
  updateTime?: string
}

/** 租户配额 API */
export const tenantQuotaApi = {
  /** 获取配额列表 */
  getList(): Promise<{ records: TenantQuotaInfo[]; total: number }> {
    return request.get('/tenant-quota/list')
  },

  /** 获取配额详情 */
  getById(id: number): Promise<TenantQuotaInfo> {
    return request.get(`/tenant-quota/${id}`)
  },

  /** 创建配额 */
  create(data: Partial<TenantQuotaInfo>): Promise<TenantQuotaInfo> {
    return request.post('/tenant-quota', data)
  },

  /** 更新配额 */
  update(id: number, data: Partial<TenantQuotaInfo>): Promise<TenantQuotaInfo> {
    return request.put(`/tenant-quota/${id}`, data)
  },

  /** 删除配额 */
  delete(id: number): Promise<boolean> {
    return request.delete(`/tenant-quota/${id}`)
  }
}
