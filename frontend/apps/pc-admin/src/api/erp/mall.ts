/**
 * B2B 商城管理后台 API 模块
 */
import request, { type ApiResponse } from '@/utils/request'
import type { PageQuery } from '@/api/erp'

// ── 通用分页结果 ──
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages?: number
}

// ── 商城配置 ──
export interface ShopConfig {
  id?: number
  tenantId?: number
  shopName: string
  shopLogo?: string
  shopDesc?: string
  themeColor?: string
  bannerIds?: string
  templateId?: number
  paymentMethods?: string
  enableRegister?: number
  enableAutoAudit?: number
  minOrderAmount?: number
  freeShippingAmount?: number
  freightAmount?: number
  status?: number
}

export const shopConfigApi = {
  /** 获取商城配置 */
  get(): Promise<ApiResponse<ShopConfig>> {
    return request.get('/erp/mall/admin/config')
  },
  /** 更新商城配置 */
  update(data: ShopConfig): Promise<ApiResponse<void>> {
    return request.put('/erp/mall/admin/config', data)
  }
}

// ── 商城用户 ──
export interface ShopUser {
  id: number
  tenantId?: number
  username: string
  phone?: string
  email?: string
  companyName?: string
  nickname?: string
  avatar?: string
  source?: string
  auditStatus: number
  auditTime?: string
  auditBy?: number
  rejectReason?: string
  erpCustomerId?: number
  erpPartnerId?: number
  lastLoginTime?: string
  status: number
  createTime?: string
}

export const shopUserApi = {
  /** 分页查询用户 */
  page(params: PageQuery & { keyword?: string; auditStatus?: number; status?: number }): Promise<ApiResponse<PageResult<ShopUser>>> {
    return request.get('/erp/mall/admin/user/page', params)
  },
  /** 审核通过 */
  approve(id: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/approve`)
  },
  /** 审核驳回 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/reject`, null, { params: { reason } })
  },
  /** 启用/禁用 */
  toggleStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/user/${id}/status`, null, { params: { status } })
  }
}

// ── 轮播图 ──
export interface ShopBanner {
  id?: number
  tenantId?: number
  title?: string
  imageUrl: string
  linkUrl?: string
  linkType?: string
  linkValue?: string
  sortOrder?: number
  status?: number
}

export const shopBannerApi = {
  /** 获取轮播图列表 */
  list(): Promise<ApiResponse<ShopBanner[]>> {
    return request.get('/erp/mall/admin/banner')
  },
  /** 创建轮播图 */
  create(data: ShopBanner): Promise<ApiResponse<void>> {
    return request.post('/erp/mall/admin/banner', data)
  },
  /** 更新轮播图 */
  update(id: number, data: ShopBanner): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/banner/${id}`, data)
  },
  /** 删除轮播图 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/mall/admin/banner/${id}`)
  }
}

// ── 页面模板 ──
export interface ShopTemplate {
  id: number
  templateName: string
  templateCode: string
  thumbnail?: string
  description?: string
  configJson?: string
  isDefault?: number
  status?: number
}

export const shopTemplateApi = {
  /** 获取启用的模板列表 */
  list(): Promise<ApiResponse<ShopTemplate[]>> {
    return request.get('/erp/mall/admin/template/list')
  }
}

// ── 商城商品管理 ──
export const mallProductApi = {
  /** 分页查询 */
  page(params: PageQuery & { keyword?: string; categoryId?: string; status?: string }): Promise<ApiResponse<PageResult<MallProduct>>> {
    return request.get('/erp/mall/admin/product/page', params)
  },
  /** 创建 */
  create(data: MallProduct): Promise<ApiResponse<void>> {
    return request.post('/erp/mall/admin/product', data)
  },
  /** 更新 */
  update(id: number, data: MallProduct): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/product/${id}`, data)
  },
  /** 删除 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/mall/admin/product/${id}`)
  }
}

// ── 商城订单管理 ──
export interface MallOrderItem {
  id?: number
  productId?: string
  productName?: string
  productImage?: string
  price?: number
  quantity?: number
  subtotal?: number
}

export interface MallOrder {
  id?: number
  orderNo?: string
  customerId?: number
  customerName?: string
  totalAmount?: number
  payAmount?: number
  orderStatus?: string
  paymentMethod?: string
  paymentStatus?: string
  deliveryStatus?: string
  consignee?: string
  phone?: string
  address?: string
  remark?: string
  source?: string
  orderItems?: MallOrderItem[]
  createTime?: string
}

export const mallOrderApi = {
  /** 分页查询 */
  page(params: PageQuery & { keyword?: string; orderStatus?: string }): Promise<ApiResponse<PageResult<MallOrder>>> {
    return request.get('/erp/mall/admin/order/page', params)
  },
  /** 获取详情 */
  getDetail(id: number): Promise<ApiResponse<MallOrder>> {
    return request.get(`/erp/mall/admin/order/${id}`)
  },
  /** 审核通过 */
  approve(id: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/order/${id}/approve`)
  },
  /** 审核驳回 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.put(`/erp/mall/admin/order/${id}/reject`, null, { params: { reason } })
  }
}
