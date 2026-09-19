import request from '@/utils/request'

/**
 * 设置 → 账套操作 → 系统重建（菜单 70560 / set:rebuild）—— 危险操作页。
 *
 * · 后端 `SetRebuildController`（`/api/set/rebuild`）：
 *     GET  /options  12 个清除范围 + 影响行数预估（dry-run）+ 警告/提示文案
 *     POST /execute  body { options: string[], password: string } → 校验登录密码后单事务执行
 * · 路径一律写相对路径（axios baseURL 已是 /api，写成 /api/... 会双前缀 404）。
 * · 拦截器已拆包：`Result.data` 直接就是返回值，**不要再取 res.data**。
 * · 12 个选项的清单**由后端下发**（注册表在服务端），前端不写死，避免两份清单漂移导致清错范围。
 * · 设计约束（对标 ql361 实测）：本页是「清数据 + 输密码」的危险操作台，
 *   不是只读台账；因此没有分页、没有查询区、没有行操作。
 */

/** 单个目标表的影响（mode=DELETE 为真删；mode=UPDATE 为期初归零） */
export interface RebuildTargetRow {
  /** 真实表名 */
  table: string
  /** DELETE = 真删；UPDATE = 期初金额归零 */
  mode: 'DELETE' | 'UPDATE'
  /** 影响行数（-1 = 预估失败，前端显示「未知」，绝不编造） */
  rows: number
}

/** 一个清除范围（= 页面上的一个复选项） */
export interface RebuildOption {
  /** 范围键（提交给后端的唯一标识） */
  key: string
  /** 选项名（逐字取自 ql361） */
  name: string
  /** 描述（逐字取自 ql361） */
  description: string
  /** 影响行数预估合计 */
  estimatedRows: number
  /** 逐表拆解 */
  tables: RebuildTargetRow[]
  /** 缺口/取舍说明（可为空） */
  note: string | null
}

/** GET /options 返回 */
export interface RebuildOptionsResult {
  options: RebuildOption[]
  /** 顶部红色警告（逐字） */
  warning: string
  /** 底部提示条（逐字） */
  tip: string
  /** 选项间包含关系：勾 key 则自动勾选并禁用其 value */
  implies: Record<string, string>
  /** 当前会话租户（字符串，防雪花 ID 精度丢失） */
  tenantId: string
}

/** 单个范围的执行结果 */
export interface RebuildResultRow {
  key: string
  name: string
  description: string
  /** 该范围实际清理/归零的行数 */
  clearedRows: number
  tables: RebuildTargetRow[]
  note: string | null
}

/** POST /execute 返回 */
export interface RebuildExecuteResult {
  results: RebuildResultRow[]
  /** 合计行数 */
  totalClearedRows: number
  /** 执行后的引导提示 */
  tip: string
  /** 完成时间 */
  finishTime: string
}

export const rebuildApi = {
  /** 清除范围清单 + 影响行数预估（dry-run，不改数据） */
  options(): Promise<RebuildOptionsResult> {
    return request.get('/set/rebuild/options')
  },

  /** 执行重建（必然携带登录密码；密码只用于本次服务端二次校验） */
  execute(data: { options: string[]; password: string }): Promise<RebuildExecuteResult> {
    return request.post('/set/rebuild/execute', data)
  },
}

export default rebuildApi
