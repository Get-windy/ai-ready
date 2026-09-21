import request, { type ApiResponse } from '@/utils/request'

/**
 * 权限模拟状态（`GET /api/simulate/status`）
 *
 * 模拟开启后，后续所有请求的权限判定都按被模拟用户计算（落地点在后端 StpInterfaceImpl），
 * 用于「以某用户身份预览」——验证某人的菜单/按钮/数据可见范围，而不是靠猜。
 */
export interface SimulationStatus {
  simulating: boolean
  targetUserId: number | null
  actualUserId: number | null
  reason: string | null
}

// 权限模拟API
export const simulationApi = {
  /** 开始以某用户身份预览（写入服务端会话，跨请求持续生效，需 system:simulate 权限） */
  start(targetUserId: number | string, reason?: string): Promise<ApiResponse<void>> {
    return request.post('/simulate/start', { targetUserId, reason })
  },

  /** 结束预览 */
  stop(): Promise<ApiResponse<void>> {
    return request.post('/simulate/stop')
  },

  /** 查询当前预览状态 */
  status(): Promise<ApiResponse<SimulationStatus>> {
    return request.get('/simulate/status')
  }
}

export default simulationApi
