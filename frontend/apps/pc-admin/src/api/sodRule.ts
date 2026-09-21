import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 职责分离规则（sys_sod_rule）
 *
 * 定义**互斥角色组合**：同一用户不能同时拥有同一规则里列出的任意两个角色。
 * 校验落地点在「分配角色」（SysUserServiceImpl#assignRoles）——
 * 此前该校验只存在于另一个未被前端使用的入口，等于配了不拦（2026-09-20 修复）。
 */
export interface SodRule {
  id?: number
  ruleName: string
  description?: string
  /** 互斥角色 ID 列表（JSON 数组字符串，如 "[1,5,8]"） */
  conflictRoleIds: string
  /** 状态：0-禁用 1-启用 */
  status?: number
  createTime?: string
}

// 职责分离规则API
export const sodRuleApi = {
  /** 分页查询 SoD 规则 */
  page(params: { pageNum?: number; pageSize?: number }): Promise<ApiResponse<PageResponse<SodRule>>> {
    return request.get('/sod-rule/page', params)
  },

  /** 规则详情 */
  getById(id: number): Promise<ApiResponse<SodRule>> {
    return request.get(`/sod-rule/${id}`)
  },

  /** 创建规则 */
  create(rule: Partial<SodRule>): Promise<ApiResponse<number>> {
    return request.post('/sod-rule', rule)
  },

  /** 更新规则 */
  update(id: number, rule: Partial<SodRule>): Promise<ApiResponse<void>> {
    return request.put(`/sod-rule/${id}`, rule)
  },

  /** 删除规则 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/sod-rule/${id}`)
  },

  /** 校验某组角色是否违反 SoD 规则（返回冲突的规则列表） */
  validate(roleIds: number[], userId?: number): Promise<ApiResponse<SodRule[]>> {
    return request.post(`/sod-rule/validate${userId ? `?userId=${userId}` : ''}`, roleIds)
  }
}

export default sodRuleApi
