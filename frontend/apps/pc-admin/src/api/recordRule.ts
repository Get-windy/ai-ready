import request, { type ApiResponse } from '@/utils/request'

/**
 * 记录级数据规则（sys_record_rule）
 *
 * 对标 Odoo 的 `ir.rule`：用 domain 表达式限定「哪些记录可见/可写」。
 * 后端已有完整实现（RecordRuleServiceImpl：buildDomainFilter / checkRecordAccess / filterRecords），
 * 但此前零调用方（2026-09-20 盘点）。
 *
 * 规则组合口径（与 Odoo 一致）：
 *   · 全局规则（global=true）之间取**交集** —— 加一条只会更严；
 *   · 组/用户规则之间取**并集** —— 加一条可以放宽，但不能突破全局规则。
 */
export interface RecordRule {
  id?: number
  /** 规则编码（唯一标识） */
  ruleCode: string
  /** 规则名称 */
  ruleName: string
  /** 模型名，如 sale.order / erp_sale_order */
  modelName: string
  /** domain 过滤表达式，如 [('create_by','=',user.id)] */
  domain: string
  /** 是否启用 */
  active: boolean
  /** 是否全局规则（全局取交集，非全局按组取并集） */
  global: boolean
  /** 适用组（角色）ID */
  groupId?: number
  groupName?: string
  /** 适用用户 ID */
  userId?: number
  userName?: string
  /** 适用操作：1-适用 0-不适用 */
  permRead: number
  permWrite: number
  permCreate: number
  permDelete: number
  description?: string
}

// 记录规则API
export const recordRuleApi = {
  /** 查询某角色（组）的全部规则 */
  listByGroup(groupId: number | string): Promise<ApiResponse<RecordRule[]>> {
    return request.get(`/permission/record/group/${groupId}`)
  },

  /** 查询某模型的全部规则 */
  listByModel(modelName: string): Promise<ApiResponse<RecordRule[]>> {
    return request.get(`/permission/record/model/${modelName}`)
  },

  /** 创建规则 */
  create(rule: Partial<RecordRule>): Promise<ApiResponse<RecordRule>> {
    return request.post('/permission/record', rule)
  },

  /** 更新规则 */
  update(id: number, rule: Partial<RecordRule>): Promise<ApiResponse<RecordRule>> {
    return request.put(`/permission/record/${id}`, rule)
  },

  /** 删除规则 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/permission/record/${id}`)
  },

  /** 启用规则 */
  activate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/permission/record/${id}/activate`)
  },

  /** 停用规则 */
  deactivate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/permission/record/${id}/deactivate`)
  }
}

export default recordRuleApi
