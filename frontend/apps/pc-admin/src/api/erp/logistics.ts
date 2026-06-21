import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

export interface LogisticsExt {
  id: number
  tenantId: number
  partnerId: number
  logisticsType: number
  serviceArea: string
  transportModes: string
  vehicleCount: number
  coldChain: number
  hazardous: number
  servicePhone: string
  remark: string
  createTime: string
}

export const logisticsApi = {
  page(params: PageQuery & { keyword?: string }): Promise<PageResult<LogisticsExt>> {
    return request.get('/erp/partner/logistics/page', params)
  },
  getByPartner(partnerId: number): Promise<LogisticsExt> {
    return request.get(`/erp/partner/logistics/${partnerId}`)
  },
  create(data: Partial<LogisticsExt>): Promise<boolean> {
    return request.post('/erp/partner/logistics', data)
  },
  update(id: number, data: Partial<LogisticsExt>): Promise<boolean> {
    return request.put(`/erp/partner/logistics/${id}`, data)
  }
}
