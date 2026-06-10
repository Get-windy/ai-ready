import request, { type ApiResponse } from '@/utils/request'

/** 租户模块信息 */
export interface TenantModule {
  id: number
  tenantId: number
  moduleCode: string
  moduleName: string
  purchaseType: string
  expireTime: string
  status: number
  deleted: number
}

export const tenantModuleApi = {
  /**
   * 获取租户的有效模块编码集合
   */
  getValidModuleCodes(tenantId: number): Promise<ApiResponse<string[]>> {
    return request.get('/tenant-module/valid-codes', { tenantId })
  },

  /**
   * 获取租户的所有模块记录
   */
  getList(tenantId: number): Promise<ApiResponse<TenantModule[]>> {
    return request.get('/tenant-module/list', { tenantId })
  },
}

export default tenantModuleApi
