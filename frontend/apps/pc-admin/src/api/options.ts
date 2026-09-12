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
  /**
   * 获取供应商下拉列表
   * 数据源＝往来单位（biz_party, partyType=supplier），与「资料 → 供应商」档案页同一口径。
   * 旧实现走 `/supplier/list`（供应商门户模块）返回恒为空，导致采购订单等页面供应商下拉无数据。
   */
  getSuppliers(): Promise<OptionItem[]> {
    return request.get('/erp/md/customer/list', { partnerType: 'supplier', status: 'ENABLED', pageSize: 500 })
      .then((res: any) => {
        const list = res?.data || res || []
        return list.map((p: any) => ({
          id: p.id,
          name: p.partnerName || p.partyName || '',
          code: p.partnerCode || p.partyCode || '',
        }))
      })
  },

  /** 获取产品下拉列表 */
  getProducts(): Promise<OptionItem[]> {
    return request.get('/erp/product/page', { params: { pageNum: 1, pageSize: 1000 } }).then((res: any) => {
      // 处理响应格式：PageResult 格式 { records, total }
      const records = res?.records || res?.data?.records || []
      // 映射字段名：后端 productCode/productName -> 前端 code/name；补充成本/品牌/产地/型号/图片/重量/体积/保质期等
      return records.map((p: any) => ({
        id: p.id,
        name: p.productName || p.name || '',
        code: p.productCode || p.code || '',
        barcode: p.barcode || '',
        unit: p.unit || '',
        specification: p.spec || p.specification || '',
        model: p.model || '',
        origin: p.origin || '',
        brand: p.brand || '',
        image: p.imageUrl || '',
        costPrice: p.costPrice ?? p.purchasePrice ?? 0,
        purchasePrice: p.costPrice || p.purchasePrice || 0,
        salePrice: p.wholesalePrice || p.standardPrice || p.salePrice || 0,
        wholesalePrice: p.wholesalePrice || 0,
        retailPrice: p.retailPrice || 0,
        weight: p.weight ?? 0,
        volume: p.volume ?? 0,
        shelfLifeDays: p.shelfLifeDays ?? 0,
        shelfLife: p.shelfLifeDays ? `${p.shelfLifeDays}天` : '',
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
