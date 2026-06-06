import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 部门信息
 */
export interface DepartmentInfo {
  id: number
  tenantId: number
  departmentCode: string
  departmentName: string
  parentId?: number
  parentName?: string
  level: number
  path: string
  leaderId?: number
  leaderName?: string
  phone?: string
  email?: string
  sort: number
  description?: string
  status: number
  children?: DepartmentInfo[]
  createTime: string
  updateTime: string
}

/**
 * 部门查询参数
 */
export interface DepartmentQuery {
  tenantId?: number
  departmentCode?: string
  departmentName?: string
  parentId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 部门树节点
 */
export interface DepartmentTreeNode {
  key: number
  title: string
  children?: DepartmentTreeNode[]
}

/**
 * 部门API
 */
export const departmentApi = {
  /**
   * 分页查询部门
   */
  getPage(params: DepartmentQuery): Promise<ApiResponse<PageResponse<DepartmentInfo>>> {
    return request.get('/department/page', params)
  },

  /**
   * 获取所有部门（不分页）
   */
  getList(params?: Partial<DepartmentQuery>): Promise<ApiResponse<DepartmentInfo[]>> {
    return request.get('/department/list', params)
  },

  /**
   * 获取部门树
   */
  getTree(params?: Partial<DepartmentQuery>): Promise<ApiResponse<DepartmentInfo[]>> {
    return request.get('/department/tree', params)
  },

  /**
   * 获取部门详情
   */
  getById(id: number): Promise<ApiResponse<DepartmentInfo>> {
    return request.get(`/department/${id}`)
  },

  /**
   * 创建部门
   */
  create(data: Partial<DepartmentInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/department', data)
  },

  /**
   * 更新部门
   */
  update(id: number, data: Partial<DepartmentInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/department/${id}`, data)
  },

  /**
   * 删除部门
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/department/${id}`)
  },

  /**
   * 批量删除部门
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/department/batch', { data: ids })
  },

  /**
   * 更新部门状态
   */
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/department/${id}/status`, null, { params: { status } })
  },

  /**
   * 移动部门
   */
  move(id: number, targetId: number, position: 'before' | 'after' | 'inner'): Promise<ApiResponse<boolean>> {
    return request.post(`/department/${id}/move`, { targetId, position })
  },

  /**
   * 导出部门
   */
  export(params: DepartmentQuery): Promise<Blob> {
    return request.get('/department/export', params, { responseType: 'blob' })
  }
}

export default departmentApi