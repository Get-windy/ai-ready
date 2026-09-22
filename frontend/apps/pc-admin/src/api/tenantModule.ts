import request, { type ApiResponse } from '@/utils/request'

/**
 * 租户模块调用权（`/api/tenant-module`）。
 *
 * ⚠️ 2026-09-18 口径变更（《应用中心开发文档》§5.4 / §9.2-P0②）：
 *   后端**租户 id 一律以登录会话为准**，前端传入的 `tenantId` 只在平台超管（租户隔离豁免）时才被采纳；
 *   普通账号传他人租户 id 会被忽略并记告警（始终返回本租户数据）。
 *   因此这里的 `tenantId` 参数可省略，省略即「本租户」。
 *
 * 应用中心（set/app-center）已改用 `/api/set/app-center/*`（返回「已安装 + 是否已开通」），
 * 本文件的 `getValidModuleCodes` 现仅被 `stores/user.ts`（路由模块校验）使用。
 * （原 `views/admin/tenant/permissions` 亦曾调用，该页为孤儿重复实现，已于 2026-09-19 删除。）
 *
 * 2026-09-22 补写入口：模块开通 / 停用（`assignModule` / `removeModule`）。
 * 此前本文件只有查询，导致平台方无法从界面给租户开关模块（只能改库），
 * 模块门禁「只能拦、不能放」。使用方为「系统 → 租户管理 → 模块授权」页。
 */

/** 租户模块信息 */
export interface TenantModule {
  /** 主键（雪花 ID，按字符串处理，禁止 Number()） */
  id: string
  /** 租户 ID */
  tenantId: string
  /** 模块编码（注册表口径，如 sale / purchase / warehouse） */
  moduleCode: string
  /** 模块名称 */
  moduleName: string
  /** 开通类型：permanent / auto_renew / manual */
  purchaseType: string
  /** 到期时间（permanent 类型可为空） */
  expireTime: string
  /** 状态：0 = 正常，1 = 停用 */
  status: number
  /** 逻辑删除标记 */
  deleted: number
}

/** 模块开通请求体（POST /api/tenant-module/assign） */
export interface ModuleAssignPayload {
  /** 租户 ID（本接口要求数字；本页租户 ID 是 1 / 2 这类小整数，不是雪花 ID） */
  tenantId: number
  /** 模块编码（注册表口径，如 sale / purchase） */
  moduleCode: string
  /** 开通类型：permanent / auto_renew / manual（可省略，由后端取默认） */
  purchaseType?: string
  /** 到期时间（可省略；permanent 类型可为空） */
  expireTime?: string
}

export const tenantModuleApi = {
  /**
   * 获取租户的有效模块编码集合（status = 0 且未过期；不传 tenantId 即本租户）
   */
  getValidModuleCodes(tenantId?: number): Promise<ApiResponse<string[]>> {
    return request.get('/tenant-module/valid-codes', { params: { tenantId } })
  },

  /**
   * 获取租户的所有模块记录（含已过期；不传 tenantId 即本租户）
   */
  getList(tenantId?: number): Promise<ApiResponse<TenantModule[]>> {
    return request.get('/tenant-module/list', { params: { tenantId } })
  },

  /**
   * 给租户开通模块（平台侧授权下发）。
   *
   * 失败时后端返回 `Result.fail(code, message)`，message 为中文（例如「模块未开通」相关门禁文案）。
   * 开通成功后该租户下用户访问该模块接口才会被放行；未开通时模块门直接 403「模块未开通」。
   */
  assignModule(payload: ModuleAssignPayload): Promise<ApiResponse<void>> {
    return request.post('/tenant-module/assign', payload)
  },

  /**
   * 停用租户模块（移除开通记录）。
   *
   * ⚠️ 这是**不可逆的权限面变更**：调用方必须先做二次确认，并向用户说清后果 ——
   * 停用后该租户下所有用户访问该模块的接口都会被拒绝（403「模块未开通」）。
   */
  removeModule(tenantId: number, moduleCode: string): Promise<ApiResponse<void>> {
    return request.delete('/tenant-module/remove', { params: { tenantId, moduleCode } })
  },
}

export default tenantModuleApi
