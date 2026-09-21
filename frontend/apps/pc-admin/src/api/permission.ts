import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 权限信息
export interface PermissionInfo {
  id: number
  tenantId: number
  parentId: number
  permissionName: string
  permissionCode: string
  permissionType: number
  path: string
  component: string
  icon: string
  apiPath: string
  method: string
  sort: number
  visible: number
  status: number
  /** 原始 ID 备份（修复 64 位 Long 精度丢失后重新生成唯一 ID 时保存） */
  _rawId?: number
  /** 子权限列表（树形结构） */
  children?: PermissionInfo[]
}

// 权限查询参数
export interface PermissionQuery {
  tenantId: number
  permissionName?: string
  permissionType?: number
  status?: number
  current?: number
  size?: number
}

/**
 * 权限生效性清单（`GET /permission/effectivity`）
 *
 * 数据由 `tools/gen-permission-effectivity.py` 扫描前后端源码生成：
 * 一条权限码只要被后端 @SaCheckPermission/@RequirePermission 或前端 v-permission/checkPermission 用过，
 * 就算「生效」；两边都没有的，勾进角色也不会控制任何东西。
 */
export interface PermissionEffectivity {
  /** 未生成清单时为 false（前端静默降级，不做标注） */
  available?: boolean
  generatedAt?: string
  summary?: { db: number; effective: number; ineffective: number; groupNodes: number }
  /** 没有任何消费方的权限码：勾进角色也不会生效 */
  ineffective?: string[]
  /** 分组节点（`xxx:manage` 这类），不参与生效性判定 */
  groupNodes?: string[]
  /** 生效权限码的引用条数，用于 tooltip 展示「被 N 处接口 / M 处按钮使用」 */
  refCounts?: Record<string, { backend: number; frontend: number }>
}

// 权限API
export const permissionApi = {
  // 分页查询权限
  getPage(params: PermissionQuery): Promise<ApiResponse<PageResponse<PermissionInfo>>> {
    return request.get('/permission/page', params)
  },

  // 获取权限树
  getTree(tenantId: number): Promise<ApiResponse<PermissionInfo[]>> {
    return request.get('/permission/tree', { tenantId })
  },

  // 获取权限详情
  getById(id: number): Promise<ApiResponse<PermissionInfo>> {
    return request.get(`/permission/${id}`)
  },

  // 创建权限
  create(data: Partial<PermissionInfo>): Promise<ApiResponse<number>> {
    return request.post('/permission', data)
  },

  // 更新权限
  update(id: number, data: Partial<PermissionInfo>): Promise<ApiResponse<void>> {
    return request.put(`/permission/${id}`, data)
  },

  // 删除权限
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/permission/${id}`)
  },

  // 批量删除权限
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/permission/batch', { data: ids })
  },

  // 更新权限状态
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/permission/${id}/status`, null, { params: { status } })
  },

  // 检查权限编码
  checkCode(code: string, tenantId: number, excludeId?: number): Promise<ApiResponse<boolean>> {
    return request.get('/permission/check-code', { code, tenantId, excludeId })
  },

  // 权限生效性清单：ineffective 里的权限码勾了不会被任何代码检查（后端无注解、前端无指令）
  getEffectivity(): Promise<ApiResponse<PermissionEffectivity>> {
    return request.get('/permission/effectivity')
  }
}

export default permissionApi