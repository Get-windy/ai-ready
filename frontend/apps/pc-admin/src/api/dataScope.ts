import request, { type ApiResponse } from '@/utils/request'

/**
 * 数据范围规则（sys_data_scope）
 *
 * 定义角色对**特定表的行级可见范围**。当角色的 dataScope = 4（自定义）时，
 * 查询按本表规则过滤（消费方：core-api 的 DataPermissionInterceptor，CUSTOM 分支）。
 */
export interface DataScopeRule {
  id?: number
  roleId?: number
  /** 规则类型：ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM_SQL */
  ruleType: string
  /** 目标表名（为空时对全部表生效） */
  targetTable?: string
  /** 目标字段名（默认 dept_id） */
  targetField?: string
  /** 可见部门 ID 列表（JSON 数组字符串，ruleType=DEPT/DEPT_AND_CHILD 时使用） */
  deptIds?: string
  /** 自定义 SQL 条件（ruleType=CUSTOM_SQL 时使用，如 dept_id IN (1,2,3) AND status = 1） */
  customSql?: string
  remark?: string
  /** 状态：0-禁用 1-启用 */
  status?: number
}

/** 规则类型选项（与后端 SysDataScope.ruleType 注释一致） */
export const RULE_TYPE_OPTIONS = [
  { value: 'ALL', label: '全部数据' },
  { value: 'DEPT', label: '本部门' },
  { value: 'DEPT_AND_CHILD', label: '本部门及以下' },
  { value: 'SELF', label: '仅本人' },
  { value: 'CUSTOM_SQL', label: '自定义 SQL' },
]

// 数据范围API
export const dataScopeApi = {
  /** 查询角色的数据范围规则 */
  list(roleId: number | string): Promise<ApiResponse<DataScopeRule[]>> {
    return request.get('/data-scope/list', { roleId })
  },

  /** 保存角色的数据范围规则（**全量覆盖**） */
  save(roleId: number | string, scopes: DataScopeRule[]): Promise<ApiResponse<void>> {
    return request.post(`/data-scope/save?roleId=${roleId}`, scopes)
  },

  /** 删除一条数据范围规则 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/data-scope/${id}`)
  }
}

export default dataScopeApi
