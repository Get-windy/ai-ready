import request, { type ApiResponse } from '@/utils/request'

/**
 * 字段级权限规则（sys_field_permission）
 *
 * 控制角色对**特定表字段**的可见性与脱敏方式，后端序列化时按此规则脱敏
 * （消费方：core-api 的 DataMaskSerializer / @DataMask 注解）。
 */
export interface FieldPermissionRule {
  id?: number
  roleId?: number
  /** 目标表名，如 erp_customer */
  targetTable: string
  /** 目标字段名，如 phone */
  targetField: string
  /** 是否可见：1-可见 0-隐藏 */
  visible: number
  /** 脱敏类型：NONE/PHONE/EMAIL/ID_CARD/BANK_CARD/CUSTOM */
  maskType: string
  /** 脱敏填充字符（默认 *） */
  maskChar?: string
  /** 保留前缀长度 */
  maskPrefixLen?: number
  /** 保留后缀长度 */
  maskSuffixLen?: number
  /** 状态：0-禁用 1-启用 */
  status?: number
}

/**
 * 打码方式选项（与后端 SysFieldPermission.maskType 一致）。
 * label 用业务口径的「XX打码」，配合界面上的「效果预览」列，管理员不必理解脱敏算法。
 */
export const MASK_TYPE_OPTIONS = [
  { value: 'NONE', label: '不打码' },
  { value: 'PHONE', label: '手机号打码' },
  { value: 'EMAIL', label: '邮箱打码' },
  { value: 'ID_CARD', label: '身份证打码' },
  { value: 'BANK_CARD', label: '银行卡打码' },
  { value: 'CUSTOM', label: '自定义打码' },
]

// 字段级权限API
export const fieldPermissionApi = {
  /** 查询角色的字段权限规则（可按表过滤） */
  list(roleId: number | string, targetTable?: string): Promise<ApiResponse<FieldPermissionRule[]>> {
    return request.get('/field-permission/list', { roleId, targetTable })
  },

  /** 保存角色的字段权限规则（**全量覆盖**） */
  save(roleId: number | string, rules: FieldPermissionRule[]): Promise<ApiResponse<void>> {
    return request.post(`/field-permission/save?roleId=${roleId}`, rules)
  },

  /** 删除一条字段权限规则 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/field-permission/${id}`)
  }
}

export default fieldPermissionApi
