import request, { type ApiResponse } from '@/utils/request'

/**
 * 操作员数据权限（按维度授权对象清单）
 *
 * 对标 ql361「资料 → 职员权限 → 全部操作员」页的 7 类数据权限
 * （2026-09-19 登录 22stable.ql361.com 实抓：仓库权限 / 调拨权限 / 部门权限 /
 *  往来单位权限 / 商品权限 / 现金银行权限 / 客户级别权限）。
 *
 * 后端：core-base 的 UserDataScopeController（表 sys_user_data_scope，
 * user_id + scope_key + target_ids，tenant_id 由多租户拦截器自动注入）。
 */

/** 数据权限维度键（必须与后端 UserDataScopeService 的合法维度白名单一致） */
export type DataScopeKey =
  | 'warehouse'
  | 'transfer'
  | 'department'
  | 'partner'
  | 'product'
  | 'fund'
  | 'customer_level'

/** 维度候选对象 */
export interface DataScopeTarget {
  id: string
  name: string
  code?: string
  categoryId?: string
  categoryName?: string
}

export const userDataScopeApi = {
  /** 取某操作员的全部维度授权（scopeKey → 已授权对象 id 列表） */
  getUserScopes(userId: string | number): Promise<ApiResponse<Record<string, string[]>>> {
    return request.get(`/user-data-scope/${userId}`)
  },

  /** 覆盖保存某维度的授权对象清单（传空数组 = 未设置） */
  saveScope(userId: string | number, scopeKey: DataScopeKey, targetIds: string[]): Promise<ApiResponse<void>> {
    return request.put(`/user-data-scope/${userId}/${scopeKey}`, targetIds)
  },

  /** 清除某维度的授权 */
  clearScope(userId: string | number, scopeKey: DataScopeKey): Promise<ApiResponse<void>> {
    return request.delete(`/user-data-scope/${userId}/${scopeKey}`)
  },

  /** 取某维度的候选对象清单（含分类信息，供设置弹窗渲染） */
  getTargets(scopeKey: DataScopeKey, params?: { keyword?: string }): Promise<ApiResponse<DataScopeTarget[]>> {
    return request.get('/user-data-scope/targets', { scopeKey, ...params })
  },
}

export default userDataScopeApi
