/**
 * 图片管理 API 模块（资料 → 商品管理 → 图片管理）
 *
 * 后端：erp-stock ProductImageController  /api/erp/md/image/*
 * 口径：商品图片与图片空间素材共用 erp_product_image 一张表（productId 为空=未匹配素材）
 */
import request from '@/utils/request'

/** 商品图片列表行 */
export interface ProductImageRow {
  productId: string
  productName: string
  /** 货号 */
  productCode: string
  status: string
  spec: string
  model: string
  origin: string
  brand: string
  categoryId: string
  images: ProductImageItem[]
}

/** 图片项 */
export interface ProductImageItem {
  id: string
  imageName: string
  imageUrl: string
  isMain: number
  fileSize?: number
}

/** 图片空间素材 */
export interface ProductImageMaterial {
  id: string
  productId?: string
  imageName: string
  matchKey?: string
  imageUrl: string
  filePath?: string
  fileSize?: number
  fileType?: string
  isMain?: number
  source?: string
  createTime?: string
}

/** 商品图片列表查询参数 */
export interface ProductImageQuery {
  categoryId?: string
  keyword?: string
  /** ALL 全部 / HAS 已上传图片 / NONE 未上传图片 */
  imageFilter?: string
  spec?: string
  model?: string
  origin?: string
  brand?: string
  status?: string
  sortField?: string
  sortOrder?: string
  pageNum?: number
  pageSize?: number
}

/** 自动匹配结果 */
export interface ProductImageMatchResult {
  total: number
  matched: number
  ambiguous: number
  unmatched: number
}

export const productImageApi = {
  /** 商品图片列表分页（商品维度） */
  page(params: ProductImageQuery): Promise<any> {
    return request.get('/erp/md/image/page', params)
  },

  /** 图片空间分页（素材库） */
  spacePage(params: { keyword?: string; onlyImage?: number; pageNum?: number; pageSize?: number }): Promise<any> {
    return request.get('/erp/md/image/space-page', params)
  },

  /** 上传图片（不传 productId 即上传到图片空间） */
  upload(file: File, productId?: string, isMain?: number): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)
    if (productId) formData.append('productId', String(productId))
    if (isMain !== undefined) formData.append('isMain', String(isMain))
    return request.post('/erp/md/image/upload', formData)
  },

  /** 自动匹配：NAME 按名称 / CODE 按商品货号 */
  autoMatch(matchType: string): Promise<ProductImageMatchResult> {
    return request.post(`/erp/md/image/auto-match?matchType=${encodeURIComponent(matchType)}`)
  },

  /** 选择图片：绑定素材到商品 */
  bind(payload: { imageId: string; productId: string; isMain?: number }): Promise<boolean> {
    return request.post('/erp/md/image/bind', payload)
  },

  /** 搬移素材到商品 */
  move(payload: { imageIds: string[]; productId: string }): Promise<number> {
    return request.post('/erp/md/image/move', payload)
  },

  /** 批量删除图片 */
  batchDelete(imageIds: string[]): Promise<number> {
    return request.post('/erp/md/image/batch-delete', { imageIds })
  },

  /** 设置主图 */
  setMain(id: string): Promise<boolean> {
    return request.post(`/erp/md/image/${id}/main`)
  },

  /** 按商品查询图片 */
  listByProduct(productId: string): Promise<ProductImageItem[]> {
    return request.get(`/erp/md/image/product/${productId}`)
  }
}

export default productImageApi
