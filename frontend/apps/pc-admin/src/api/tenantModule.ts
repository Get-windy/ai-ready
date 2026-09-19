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
}

export default tenantModuleApi
