import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 岗位信息
 */
export interface PositionInfo {
  id: number
  tenantId: number
  positionCode: string
  positionName: string
  categoryId?: number
  categoryName?: string
  departmentId?: number
  departmentName?: string
  level: number
  sort: number
  description?: string
  status: number
  createTime: string
  updateTime: string
}

/**
 * 岗位分类
 */
export interface PositionCategory {
  id: number
  tenantId: number
  categoryCode: string
  categoryName: string
  description?: string
  sort: number
  status: number
  createTime: string
}

/**
 * 岗位查询参数
 */
export interface PositionQuery {
  tenantId?: number
  positionCode?: string
  positionName?: string
  categoryId?: number
  departmentId?: number
  level?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 岗位分类查询参数
 */
export interface CategoryQuery {
  tenantId?: number
  categoryName?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 岗位API
 */
export const positionApi = {
  /**
   * 分页查询岗位
   */
  getPage(params: PositionQuery): Promise<ApiResponse<PageResponse<PositionInfo>>> {
    return request.get('/position/page', params)
  },

  /**
   * 获取所有岗位（不分页）
   */
  getList(params?: Partial<PositionQuery>): Promise<ApiResponse<PositionInfo[]>> {
    return request.get('/position/list', params)
  },

  /**
   * 获取岗位详情
   */
  getById(id: number): Promise<ApiResponse<PositionInfo>> {
    return request.get(`/position/${id}`)
  },

  /**
   * 创建岗位
   */
  create(data: Partial<PositionInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/position', data)
  },

  /**
   * 更新岗位
   */
  update(id: number, data: Partial<PositionInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/position/${id}`, data)
  },

  /**
   * 删除岗位
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/position/${id}`)
  },

  /**
   * 批量删除岗位
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/position/batch', { data: ids })
  },

  /**
   * 更新岗位状态
   */
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/position/${id}/status`, null, { params: { status } })
  },

  /**
   * 导出岗位
   */
  export(params: PositionQuery): Promise<Blob> {
    return request.get('/position/export', params, { responseType: 'blob' })
  },

  // ===== 岗位分类相关 =====

  /**
   * 分页查询岗位分类
   */
  getCategoryPage(params: CategoryQuery): Promise<ApiResponse<PageResponse<PositionCategory>>> {
    return request.get('/position/category/page', params)
  },

  /**
   * 获取所有岗位分类
   */
  getCategoryList(params?: Partial<CategoryQuery>): Promise<ApiResponse<PositionCategory[]>> {
    return request.get('/position/category/list', params)
  },

  /**
   * 创建岗位分类
   */
  createCategory(data: Partial<PositionCategory>): Promise<ApiResponse<boolean>> {
    return request.post('/position/category', data)
  },

  /**
   * 更新岗位分类
   */
  updateCategory(id: number, data: Partial<PositionCategory>): Promise<ApiResponse<boolean>> {
    return request.put(`/position/category/${id}`, data)
  },

  /**
   * 删除岗位分类
   */
  deleteCategory(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/position/category/${id}`)
  },

  /**
   * 更新分类状态
   */
  updateCategoryStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/position/category/${id}/status`, null, { params: { status } })
  }
}

export default positionApi