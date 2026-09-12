import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

/**
 * 互联账号（资料 → 往来单位 → 互联账号）
 * 对应后端 LinkedAccountController（/api/erp/md/linked-account）
 * 全局基础数据单一口径：营销 / 会员 / 商城互联统一引用本接口。
 */
export interface LinkedAccountVO {
  id: string | number
  /** 往来单位ID（biz_party.id） */
  partyId?: string | number
  /** 往来单位编号 */
  partyCode?: string
  /** 往来单位名称 */
  partyName?: string
  /** 互联平台标识：WECHAT / ALIPAY / DOUYIN / MALL / OTHER */
  platform?: string
  platformDesc?: string
  /** 关联类型标识：MEMBER / CUSTOMER / OTHER */
  linkType?: string
  linkTypeDesc?: string
  /** 互联用户名 */
  linkedUserName?: string
  /** 手机号 */
  phone?: string
  /** 绑定状态：1 已绑定 / 0 已解绑 */
  status?: number
  statusDesc?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 字典项（平台 / 关联类型，全局唯一） */
export interface LinkedAccountDictItem {
  value: string
  label: string
}

export const linkedAccountApi = {
  /** 分页查询 */
  page(params: PageQuery & {
    partyId?: string | number
    keyword?: string
    platform?: string
    linkType?: string
    status?: number
  }): Promise<PageResult<LinkedAccountVO>> {
    return request.get('/erp/md/linked-account/page', params)
  },

  /** 列表（不分页，供下拉/引用） */
  list(params?: { partyId?: string | number; keyword?: string; pageSize?: number }): Promise<LinkedAccountVO[]> {
    return request.get('/erp/md/linked-account/list', { params })
  },

  /** 字典（互联平台 / 关联类型） */
  dict(): Promise<{ platforms: LinkedAccountDictItem[]; linkTypes: LinkedAccountDictItem[] }> {
    return request.get('/erp/md/linked-account/dict')
  },

  getById(id: string | number): Promise<LinkedAccountVO> {
    return request.get(`/erp/md/linked-account/${id}`)
  },

  /** 绑定互联账号（新增） */
  create(data: Record<string, any>): Promise<LinkedAccountVO> {
    return request.post('/erp/md/linked-account', data)
  },

  update(id: string | number, data: Record<string, any>): Promise<boolean> {
    return request.put(`/erp/md/linked-account/${id}`, data)
  },

  /** 解绑 / 重新绑定（状态切换） */
  updateStatus(id: string | number, status: number): Promise<boolean> {
    return request.put(`/erp/md/linked-account/${id}/status`, null, { params: { status } })
  },

  /** 批量解绑 / 批量绑定 */
  batchStatus(ids: Array<string | number>, status: number): Promise<boolean> {
    return request.put('/erp/md/linked-account/batch-status', { ids, status })
  },

  delete(id: string | number): Promise<boolean> {
    return request.delete(`/erp/md/linked-account/${id}`)
  },

  batchDelete(ids: Array<string | number>): Promise<boolean> {
    return request.delete('/erp/md/linked-account/batch', { data: { ids } })
  }
}
