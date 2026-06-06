import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 字典类型
export interface DictType {
  id: number
  dictCode: string
  dictName: string
  status: string // "ENABLED": 启用, "DISABLED": 禁用
  remark: string
  createTime?: string
  updateTime?: string
}

// 字典项
export interface DictItem {
  id: number
  dictTypeId: number
  itemCode: string
  itemName: string
  sortOrder: number
  status: string // "ENABLED": 启用, "DISABLED": 禁用
  createTime?: string
  updateTime?: string
}

// 字典类型查询参数
export interface DictTypeQuery {
  dictCode?: string
  dictName?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

// 字典项查询参数
export interface DictItemQuery {
  dictTypeId: number
  itemCode?: string
  itemName?: string
  status?: string
}

// 字典类型保存请求
export interface DictTypeSaveRequest {
  dictCode: string
  dictName: string
  status: string
  remark?: string
}

// 字典类型更新请求
export interface DictTypeUpdateRequest extends DictTypeSaveRequest {
  id: number
}

// 字典项保存请求
export interface DictItemSaveRequest {
  dictTypeId: number
  itemCode: string
  itemName: string
  sortOrder: number
  status: string
}

// 字典项更新请求
export interface DictItemUpdateRequest extends DictItemSaveRequest {
  id: number
}

// 字典类型API
export const dictTypeApi = {
  // 分页查询字典类型
  getPage(params: DictTypeQuery): Promise<ApiResponse<PageResponse<DictType>>> {
    return request.get('/dict/type/page', params)
  },

  // 获取字典类型详情
  getById(id: number): Promise<ApiResponse<DictType>> {
    return request.get(`/dict/type/${id}`)
  },

  // 创建字典类型
  create(data: DictTypeSaveRequest): Promise<ApiResponse<DictType>> {
    return request.post('/dict/type', data)
  },

  // 更新字典类型
  update(data: DictTypeUpdateRequest): Promise<ApiResponse<DictType>> {
    return request.put(`/dict/type/${data.id}`, data)
  },

  // 删除字典类型
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/dict/type/${id}`)
  },

  // 按编码获取字典类型
  getByCode(dictCode: string): Promise<ApiResponse<DictType>> {
    return request.get(`/dict/type/code/${dictCode}`)
  }
}

// 字典项API
export const dictItemApi = {
  // 按字典类型获取字典项列表
  getByDictTypeId(dictTypeId: number, params?: DictItemQuery): Promise<ApiResponse<DictItem[]>> {
    return request.get(`/dict/item/list/${dictTypeId}`, params)
  },

  // 获取字典项详情
  getById(id: number): Promise<ApiResponse<DictItem>> {
    return request.get(`/dict/item/${id}`)
  },

  // 创建字典项
  create(data: DictItemSaveRequest): Promise<ApiResponse<DictItem>> {
    return request.post('/dict/item', data)
  },

  // 更新字典项
  update(data: DictItemUpdateRequest): Promise<ApiResponse<DictItem>> {
    return request.put(`/dict/item/${data.id}`, data)
  },

  // 删除字典项
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/dict/item/${id}`)
  },

  // 按字典类型编码获取字典项
  getByDictCode(dictCode: string): Promise<ApiResponse<DictItem[]>> {
    return request.get(`/dict/item/code/${dictCode}`)
  }
}

export default { dictTypeApi, dictItemApi }
