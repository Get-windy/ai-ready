import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 合同状态枚举（与后端 ContractStatus 保持一致）
export enum ContractStatus {
  DRAFT = 'DRAFT',                             // 草稿
  PENDING_APPROVAL = 'PENDING_APPROVAL',        // 待审批
  APPROVED = 'APPROVED',                        // 已审批
  ACTIVE = 'ACTIVE',                            // 生效中
  COMPLETED = 'COMPLETED',                      // 已完成
  REJECTED = 'REJECTED',                        // 已驳回
  TERMINATED = 'TERMINATED',                    // 已终止
  ARCHIVED = 'ARCHIVED',                        // 已归档
}

// 合同类型
export interface PurchaseContract {
  id: number
  contractNo: string                            // 合同编号
  inquiryId?: number                            // 询价单ID
  quoteId?: number                              // 报价单ID
  supplierId: number                            // 供应商ID
  supplierName: string                          // 供应商名称
  contractTitle: string                         // 合同标题
  contractType?: string                         // 合同类型
  contractStatus: ContractStatus                // 合同状态
  totalAmount: number                           // 合同总金额
  executedAmount?: number                       // 已执行金额
  executedPercent?: number                      // 执行进度百分比
  startDate?: string                            // 合同开始日期
  endDate?: string                              // 合同结束日期
  paymentTerms?: string                         // 付款条件
  deliveryTerms?: string                        // 交货条件
  qualityStandard?: string                      // 质量标准
  warrantyPeriod?: string                       // 质保期
  submitTime?: string                           // 提交审批时间
  approverId?: number                           // 审批人ID
  approvalTime?: string                         // 审批时间
  approvalComment?: string                      // 审批意见
  activationTime?: string                       // 激活时间
  completionTime?: string                       // 完成时间
  terminationTime?: string                      // 终止时间
  terminationReason?: string                    // 终止原因
  archiveNo?: string                            // 归档编号
  archiveTime?: string                          // 归档时间
  modificationNo?: string                       // 变更单号
  modificationReason?: string                   // 变更原因
  contractFileUrl?: string                      // 合同文件URL
  remark?: string                               // 备注
  createdBy?: number                            // 创建人ID
  createdAt?: string                            // 创建时间
  updatedAt?: string                            // 更新时间
}

// 合同明细（商品/服务行项目）
export interface PurchaseContractItem {
  id?: number
  contractId?: number
  lineNo?: number                               // 行号
  productId: number                             // 商品ID
  productCode?: string                          // 商品编码
  productName: string                           // 商品名称
  specification?: string                        // 规格
  model?: string                                // 型号
  unit?: string                                 // 单位
  quantity: number                              // 数量
  unitPrice: number                             // 单价
  amount?: number                               // 金额
  taxRate?: number                              // 税率
  deliveryDate?: string                         // 交货日期
  remark?: string                               // 备注
}

// 合同查询参数
export interface PurchaseContractQuery {
  current: number
  size: number
  contractNo?: string                           // 合同编号
  supplierName?: string                         // 供应商名称
  supplierId?: number                           // 供应商ID
  contractStatus?: ContractStatus               // 合同状态
  contractTitle?: string                        // 合同标题
  dateStart?: string                            // 签订日期起
  dateEnd?: string                              // 签订日期止
}

// 合同统计
export interface ContractStatistics {
  totalCount: number
  draftCount: number
  pendingCount: number
  activeCount: number
  completedCount: number
  totalAmount: number
}

// 合同审批请求
export interface ApproveContractRequest {
  approved: boolean
  comment?: string
}

// API接口
export const purchaseContractApi = {
  // 分页查询合同
  page(params: PurchaseContractQuery): Promise<ApiResponse<PageResponse<PurchaseContract>>> {
    return request.get('/erp/purchase/contract/page', params)
  },

  // 获取合同详情
  get(id: number): Promise<ApiResponse<PurchaseContract>> {
    return request.get(`/erp/purchase/contract/${id}`)
  },

  // 根据合同号查询
  getByNo(contractNo: string): Promise<ApiResponse<PurchaseContract>> {
    return request.get(`/erp/purchase/contract/by-no/${contractNo}`)
  },

  // 根据供应商查询合同列表
  queryBySupplier(supplierId: number): Promise<ApiResponse<PurchaseContract[]>> {
    return request.get(`/erp/purchase/contract/supplier/${supplierId}`)
  },

  // 创建合同
  create(data: Partial<PurchaseContract>): Promise<ApiResponse<number>> {
    return request.post('/erp/purchase/contract', data)
  },

  // 更新合同
  update(id: number, data: Partial<PurchaseContract>): Promise<ApiResponse<void>> {
    return request.put(`/erp/purchase/contract/${id}`, data)
  },

  // 删除合同
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/purchase/contract/${id}`)
  },

  // 提交审批
  submit(id: number, reason: string): Promise<ApiResponse<PurchaseContract>> {
    return request.post(`/erp/purchase/contract/${id}/submit`, null, { params: { reason } })
  },

  // 审批合同
  approve(id: number, approverId: number, comment: string, approved: boolean): Promise<ApiResponse<PurchaseContract>> {
    return request.post(`/erp/purchase/contract/${id}/approve`, null, {
      params: { approverId, comment, approved }
    })
  },

  // 激活合同
  activate(id: number): Promise<ApiResponse<PurchaseContract>> {
    return request.post(`/erp/purchase/contract/${id}/activate`)
  },

  // 终止合同
  terminate(id: number, reason: string): Promise<ApiResponse<PurchaseContract>> {
    return request.post(`/erp/purchase/contract/${id}/terminate`, null, { params: { reason } })
  },

  // 归档合同
  archive(id: number, archiveNo: string, reason: string): Promise<ApiResponse<PurchaseContract>> {
    return request.post(`/erp/purchase/contract/${id}/archive`, null, { params: { archiveNo, reason } })
  },

  // 获取合同统计
  statistics(): Promise<ApiResponse<ContractStatistics>> {
    return request.get('/erp/purchase/contract/statistics')
  },

  // 导出合同
  async export(params: Omit<PurchaseContractQuery, 'current' | 'size'>): Promise<Blob> {
    const res = await request.get('/erp/purchase/contract/export', params, { responseType: 'blob' })
    return (res as any).data as Blob
  }
}

export default purchaseContractApi
