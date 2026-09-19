import request from '@/utils/request'

/**
 * 设置 → 账套操作 → 系统任务（菜单 70561 / set:system-task）—— **异步任务执行台账**。
 *
 * 后端 `SystemTaskController`（`/api/set/system-task`），数据源 = 既有 `scheduled_task_log`
 * （一行 = 一次执行，2026-09-18 复用裁定 + 迁移 V11.401.0 补台账列）。
 *
 * 端点清单（与后端逐一核对）：
 * - GET /set/system-task/page           分页台账（查询条件仅「任务ID」，与对标一致）
 * - GET /set/system-task/{id}/result    下载处理结果（**鉴权地址**，非公开直链）
 *
 * ⚠️ 路径一律写**相对路径**（axios baseURL 已是 `/api`），写成 `/api/...` 会双前缀 404。
 * ⚠️ 行内 id 由后端 Jackson 全局序列化为**字符串**（JS 大整数精度），前端禁止 Number(id)。
 */

/** 台账行（字段口径对齐 ql361 对标页 9 列） */
export interface SystemTaskRow {
  /** 任务ID（字符串，禁止 Number()） */
  taskId: string
  /** 任务类型（对标取值如「导出」；定时任务执行记录为「定时任务」） */
  taskType?: string | null
  /** 任务名称（对标取值如「报表导出」「商品资料导出」） */
  taskName?: string | null
  /** 任务状态（统一字符码：pending/running/success/failed/partial/unknown） */
  status?: string | null
  /**
   * 创建人姓名。写入侧口径（迁移 V11.416.0 起）：有会话的立即执行/重试 → 用户姓名；
   * 无会话的定时调度 → 「定时调度」；该迁移之前写入的历史行无来源 → null（页面显示「-」并说明原因）
   */
  createdByName?: string | null
  /** 任务创建时间（ISO 字符串） */
  createTime?: string | null
  /** 任务开始时间 */
  startTime?: string | null
  /** 任务结束时间 */
  endTime?: string | null
  /**
   * 结果查看：**本站鉴权下载地址**（相对路径，如 `/set/system-task/18/result`）；
   * 只有「成功/部分成功 + 确有产物 + 未过期」的行才有值，其余为 null（页面据此置灰）。
   */
  resultUrl?: string | null
  /** 失败原因（非列字段，供状态 tooltip；仅 failed 时有值） */
  errorMsg?: string | null
}

/** 台账查询条件（与后端参数名一致） */
export interface SystemTaskQuery {
  pageNum?: number
  pageSize?: number
  /** 任务ID（对标查询区唯一条件；非数字输入后端返回空页，不会 500） */
  taskId?: string
  /** 排序字段（后端白名单：taskId / createTime / startTime / endTime） */
  sortField?: string
  /** 排序方向：asc / desc */
  sortOrder?: string
}

export const systemTaskApi = {
  /** 分页查询任务台账 */
  page(params: SystemTaskQuery) {
    return request.get('/set/system-task/page', params)
  },

  /**
   * 下载处理结果。
   *
   * @param url 后端下发的鉴权地址（行内 `resultUrl`），前端**不自行拼路径**
   * @returns Blob（无产物时后端返回 404/409/410 + JSON 错误体，调用方负责读出提示）
   */
  downloadResult(url: string) {
    return request.get(url, { responseType: 'blob' })
  },
}

export default { systemTaskApi }
