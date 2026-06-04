/**
 * 通用下拉选项 API
 * 统一管理所有业务字典/下拉选项的加载
 */
import request from '@/utils/request'

export interface OptionItem {
  id: number
  name: string
  code?: string
  unit?: string
  purchasePrice?: number
  salePrice?: number
  [key: string]: any
}

export interface DictOption {
  dictCode: string
  dictName: string
  items: { value: string; label: string }[]
}

export const optionsApi = {
  /** 获取供应商下拉列表 */
  getSuppliers(): Promise<OptionItem[]> {
    return request.get('/supplier/list')
  },

  /** 获取产品下拉列表 */
  getProducts(): Promise<OptionItem[]> {
    return request.get('/product/list')
  },

  /** 获取用户下拉列表（采购员/销售员等） */
  getUsers(role?: string): Promise<OptionItem[]> {
    return request.get('/user/list', { role })
  },

  /** 获取客户下拉列表 */
  getCustomers(): Promise<OptionItem[]> {
    return request.get('/customer/list')
  },

  /** 获取字典选项（如付款方式、币种等） */
  getDict(dictCode: string): Promise<DictOption> {
    return request.get(`/dict/${dictCode}`)
  },

  /** 获取仓库下拉列表 */
  getWarehouses(): Promise<OptionItem[]> {
    return request.get('/warehouse/list')
  },

  /** 批量获取字典选项 */
  getDicts(dictCodes: string[]): Promise<DictOption[]> {
    return request.post('/dict/batch', { dictCodes })
  }
}

export default optionsApi
