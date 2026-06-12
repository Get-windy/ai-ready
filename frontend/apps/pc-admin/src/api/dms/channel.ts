/**
 * DMS 渠道管理 API 模块
 */
import request from '@/utils/request'

// ── 渠道 ──────────────────────────────────────────
export interface DmsChannel {
  id: number
  channelCode: string
  channelName: string
  channelType: number
  status: number
  priority: number
  remark: string
  createTime: string
  updateTime: string
}

export const channelApi = {
  page(params: any) { return request.get('/api/dms/channel/page', { params }) },

  getById(id: number) { return request.get(`/api/dms/channel/${id}`) },

  create(data: Partial<DmsChannel>) { return request.post('/api/dms/channel', data) },

  update(id: number, data: Partial<DmsChannel>) { return request.put(`/api/dms/channel/${id}`, data) },

  updateStatus(id: number, status: number) { return request.put(`/api/dms/channel/${id}/status`, { status }) },
}
