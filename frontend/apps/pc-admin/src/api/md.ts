import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 主数据（md 域）API 封装
 * 端点均已与后端 Controller 逐一核对：
 * - 岗位：core-api PositionController        /api/position
 * - 财务账户（支付账户）：erp-finance FinanceAccountController /api/erp/finance/account
 * 仓库（/api/wms/warehouse）复用 @/api/wms/warehouse
 * 部门（/api/department）复用 @/api/department
 * 操作员（/api/user）复用 @/api/user
 * 支付流水/渠道（/api/payment）复用 @/api/payment
 */

// ── 岗位（staff-role） ─────────────────────────────────

/** 岗位信息（与后端 PositionVO 字段一致） */
export interface MdPositionInfo {
  id: number
  positionCode: string
  positionName: string
  categoryId?: number
  categoryName?: string
  deptId?: number
  deptName?: string
  level?: number
  sort?: number
  status: number
  description?: string
  remark?: string
  userCount?: number
  createTime?: string
  updateTime?: string
}

/** 岗位查询参数（与后端 PositionQueryRequest 一致） */
export interface MdPositionQuery {
  positionCode?: string
  positionName?: string
  categoryId?: number
  deptId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 岗位 API（/api/position） */
export const mdPositionApi = {
  /** 分页查询岗位 */
  getPage(params: MdPositionQuery): Promise<ApiResponse<PageResponse<MdPositionInfo>>> {
    return request.get('/position/page', params)
  },

  /** 获取所有岗位（不分页） */
  getList(params?: Partial<MdPositionQuery>): Promise<ApiResponse<MdPositionInfo[]>> {
    return request.get('/position/list', params)
  },

  /** 创建岗位 */
  create(data: Partial<MdPositionInfo>): Promise<ApiResponse<number>> {
    return request.post('/position', data)
  },

  /** 更新岗位 */
  update(id: number, data: Partial<MdPositionInfo>): Promise<ApiResponse<void>> {
    return request.put(`/position/${id}`, data)
  },

  /** 删除岗位 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/position/${id}`)
  },

  /** 启用/禁用岗位 */
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/position/${id}/status`, null, { params: { status } })
  }
}

// ── 财务账户 / 支付账户（payment-account） ─────────────

/** 财务账户（与后端 FinanceAccount 实体一致） */
export interface FinanceAccountInfo {
  id: number
  accountName: string
  /** 账户类型 1-银行账户 2-现金账户 3-内部账户 4-外部账户 */
  accountType: number
  bankName?: string
  bankAccount?: string
  balance?: number
  /** 状态 0-停用 1-启用 */
  status: number
  currency?: string
  /** 账户等级 1-基本账户 2-一般账户 3-专用账户 */
  accountLevel?: number
  createTime?: string
  updateTime?: string
}

/**
 * 财务账户 API（/api/erp/finance/account）
 * 注意：后端仅提供 列表/统计/状态/余额 四个接口，无新增/编辑/删除端点
 */
export const financeAccountApi = {
  /** 查询财务账户列表（不分页） */
  getList(params?: { status?: number; accountType?: number }): Promise<ApiResponse<FinanceAccountInfo[]>> {
    return request.get('/erp/finance/account/list', params)
  },

  /** 查询账户统计信息 */
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/account/statistics')
  },

  /** 启用/停用账户（请求体为状态数字：0-停用 1-启用） */
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/finance/account/status/${id}`, status)
  }
}

export default { mdPositionApi, financeAccountApi }
