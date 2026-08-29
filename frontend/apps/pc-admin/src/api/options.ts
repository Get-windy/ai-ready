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
    return request.get('/erp/product/page', { pageSize: 1000 }).then((res: any) => {
      // 处理响应格式：PageResult 格式 { records, total }
      const records = res?.records || res?.data?.records || []
      // 映射字段名：后端 productCode/productName -> 前端 code/name
      return records.map((p: any) => ({
        id: p.id,
        name: p.productName || p.name || '',
        code: p.productCode || p.code || '',
        barcode: p.barcode || '',
        unit: p.unit || '',
        specification: p.spec || p.specification || '',
        salePrice: p.wholesalePrice || p.standardPrice || p.salePrice || 0,
        purchasePrice: p.costPrice || p.purchasePrice || 0,
      }))
    })
  },

  /** 获取用户下拉列表（采购员/销售员等） */
  getUsers(role?: string): Promise<OptionItem[]> {
    return request.get('/user/list', { role })
  },

  /** 获取客户下拉列表（来源：ERP往来单位 biz_party） */
  getCustomers(): Promise<OptionItem[]> {
    return request.get('/erp/md/customer/list', { pageSize: 200 }).then((res: any) => {
      const list = res?.data || res || []
      return list.map((p: any) => ({
        id: p.id,
        name: p.partnerName || p.partyName || '',
        code: p.partnerCode || p.partyCode || '',
      }))
    })
  },

  /** 获取字典选项（如付款方式、币种等） */
  getDict(dictCode: string): Promise<DictOption> {
    return request.get(`/dict/${dictCode}`)
  },

  /** 获取仓库下拉列表 */
  getWarehouses(): Promise<OptionItem[]> {
    return request.get('/wms/warehouse/list-all')
  },

  /** 获取部门下拉列表 */
  getDepartments(): Promise<OptionItem[]> {
    return request.get('/department/list').then((res: any) => {
      const list = res?.data || res || []
      return list.map((d: any) => ({
        id: d.id,
        name: d.departmentName || d.name || '',
        code: d.departmentCode || d.code || '',
      }))
    })
  },

  /** 获取财务账户下拉列表（订金账户等） */
  getAccounts(): Promise<OptionItem[]> {
    return request.get('/erp/finance/account/list').then((res: any) => {
      const list = res?.data || res || []
      return list.map((a: any) => ({
        id: a.id,
        name: a.accountName || a.name || '',
        code: a.bankAccount || a.code || '',
      }))
    })
  },

  /** 批量获取字典选项 */
  getDicts(dictCodes: string[]): Promise<DictOption[]> {
    return request.post('/dict/batch', { dictCodes })
  }
}

export default optionsApi
