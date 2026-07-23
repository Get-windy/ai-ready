/**
 * DMS 智能调度 API 模块
 * 后端: DispatchController (/api/dms/dispatch)
 */
import request from '@/utils/request'
import type { DmsRider } from './rider'

export const dispatchApi = {
  /** 自动调度（对待分配任务批量派单，无请求体） */
  autoDispatch() { return request.post('/dms/dispatch/auto') },

  /** 手动指派骑手 */
  assign(taskId: number, riderId: number) {
    return request.post(`/dms/dispatch/${taskId}/assign`, null, { params: { riderId } })
  },

  /** 改派任务 */
  reassign(taskId: number, fromRiderId: number, toRiderId: number) {
    return request.post(`/dms/dispatch/${taskId}/reassign`, null, { params: { fromRiderId, toRiderId } })
  },

  /** 获取候选骑手列表 */
  candidates(taskId: number): Promise<DmsRider[]> {
    return request.get('/dms/dispatch/candidates', { params: { taskId } })
  },

  /** 电子围栏校验（骑手是否在任务配送围栏内） */
  fenceCheck(taskId: number, riderId: number): Promise<boolean> {
    return request.post(`/dms/dispatch/${taskId}/fence-check`, null, { params: { riderId } })
  },
}
