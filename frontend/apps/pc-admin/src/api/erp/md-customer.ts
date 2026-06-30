import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

/**
 * MD客户VO（对应后端 MdCustomerVO，从 biz_party 表映射）
 */
export interface MdCustomerVO {
  id: number
  partnerCode: string
  partnerName: string
  partnerShortName: string
  partnerType: string        // customer | supplier | logistics | other
  categoryId: number
  categoryName: string
  gradeName: string
  settleType: string
  phone: string
  fax: string
  email: string
  website: string
  legalPerson: string
  taxNumber: string
  bankName: string
  bankAccount: string
  creditLimit: number
  remark: string
  status: string             // ENABLED | DISABLED
  statusDesc: string
  createTime: string
  updateTime: string
}

/**
 * MD客户API（对应后端 MdCustomerController）
 */
export const mdCustomerApi = {
  /** 分页查询 */
  page(params: PageQuery & {
    keyword?: string
    partnerType?: string
    status?: string
    categoryId?: number
    settleType?: string
    region?: string
    handler?: string
    address?: string
    createTimeStart?: string
    createTimeEnd?: string
    lastTradeStart?: string
    lastTradeEnd?: string
  }): Promise<PageResult<MdCustomerVO>> {
    return request.get('/erp/md/customer/page', params)
  },

  /** 查询详情 */
  getById(id: number): Promise<MdCustomerVO> {
    return request.get(`/erp/md/customer/${id}`)
  },

  /** 搜索（下拉选择用） */
  search(keyword: string, partnerType?: string): Promise<MdCustomerVO[]> {
    return request.get('/erp/md/customer/search', { params: { keyword, partnerType } })
  },

  /** 新增 */
  create(data: Record<string, any>): Promise<boolean> {
    return request.post('/erp/md/customer', data)
  },

  /** 更新 */
  update(id: number, data: Record<string, any>): Promise<boolean> {
    return request.put(`/erp/md/customer/${id}`, data)
  },

  /** 启用/停用 */
  updateStatus(id: number, status: string): Promise<boolean> {
    return request.put(`/erp/md/customer/${id}/status`, null, { params: { status } })
  },

  /** 删除 */
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/md/customer/${id}`)
  },

  /** 获取下一个编号序号 */
  getNextSeq(prefix: string): Promise<{ seq: number }> {
    return request.get('/erp/md/customer/next-seq', { params: { prefix } })
  },

  /** 获取列表（不分页） */
  list(partnerType?: string, status?: string, pageSize?: number): Promise<MdCustomerVO[]> {
    return request.get('/erp/md/customer/list', { params: { partnerType, status, pageSize } })
  },
}
