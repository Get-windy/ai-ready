import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 通知信息
export interface NotificationInfo {
  id: number
  title: string
  type: number // 1: 系统通知, 2: 业务通知, 3: 审批通知
  content: string
  summary: string
  sendTime: string
  readStatus: number // 0: 未读, 1: 已读
  createTime?: string
}

// 通知查询参数
export interface NotificationQuery {
  type?: number
  readStatus?: number
  pageNum?: number
  pageSize?: number
}

// 未读数量统计
export interface UnreadCount {
  total: number
  system: number
  business: number
  approval: number
}

// 通知API
export const notificationApi = {
  // 分页查询通知
  getPage(params: NotificationQuery): Promise<ApiResponse<PageResponse<NotificationInfo>>> {
    return request.get('/notification/page', { params })
  },

  // 获取通知详情
  getById(id: number): Promise<ApiResponse<NotificationInfo>> {
    return request.get(`/notification/${id}`)
  },

  // 标记已读
  markAsRead(id: number): Promise<ApiResponse<void>> {
    return request.put(`/notification/${id}/read`)
  },

  // 全部标记已读
  markAllAsRead(): Promise<ApiResponse<void>> {
    return request.put('/notification/read-all')
  },

  // 删除通知
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/notification/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/notification/batch', { data: ids })
  },

  // 获取未读数量
  getUnreadCount(): Promise<ApiResponse<UnreadCount>> {
    return request.get('/notification/unread-count')
  },

  // 获取通知列表
  getList(params?: NotificationQuery): Promise<ApiResponse<NotificationInfo[]>> {
    return request.get('/notification/list', { params })
  }
}

export default notificationApi
