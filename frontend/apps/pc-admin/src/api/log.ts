import request from '@/utils/request'

/**
 * 日志接口封装（后端：cn.aiedge.audit.controller.SysLogStubController，前缀 /api/log）
 *
 * ⚠️ 路径一律写相对路径（axios baseURL 已是 `/api`），写 `/api/...` 会双前缀 404。
 * ⚠️ 响应已被 request.ts 响应拦截器**拆包**（返回 `data` 本身，Page 对象直出）——
 *    页面侧应直接读 `res.records` / `res.total`，**不要再取 `res.data`**。
 *    历史故障：旧页面写 `result.data.records` → `undefined` → TypeError 被 catch 吞成
 *    「查询失败」→ 列表恒空（《操作日志开发文档》§9.2-P0）。
 */

// ── 系统日志行（对应后端 SysOperLogVO） ──────────────────────────
export interface OperLogRow {
  /** 雪花ID：BIGINT，必须按字符串处理，禁止 Number(id) */
  id: string
  userId?: string
  /** 操作员（登录账号） */
  username?: string
  /** 姓名（后端由 sys_user.real_name 补齐，空则回退 nickname） */
  realName?: string
  /** 模块 */
  module?: string
  /** 操作类型（CREATE / UPDATE / DELETE / QUERY / EXPORT / IMPORT / OTHER） */
  action?: string
  operTime?: string
  operIp?: string
  operLocation?: string
  costTime?: number
  /** 0-成功 1-失败 */
  status?: number
  errorMsg?: string
  requestMethod?: string
  requestUrl?: string
  method?: string
  diffData?: string
  /** ⚠️ 仅详情接口（GET /log/{id}）返回；列表接口裁剪，恒为 undefined */
  requestParams?: string
  /** ⚠️ 仅详情接口返回；列表接口裁剪，恒为 undefined */
  responseResult?: string
}

// ── 登录日志行（对应后端 SysLoginLogVO） ────────────────────────
export interface LoginLogRow {
  /** 雪花ID：按字符串处理 */
  id: string
  userId?: string
  username?: string
  realName?: string
  loginTime?: string
  /** 1-账号密码登录 2-短信验证码登录 3-第三方登录 */
  loginType?: number
  /** 0-成功 1-失败 */
  loginResult?: number
  failReason?: string
  loginIp?: string
  loginLocation?: string
  browser?: string
  os?: string
  deviceType?: string
  logoutTime?: string
  remark?: string
}

// ── 查询参数 ──────────────────────────────────────────────────
export interface OperLogQuery {
  pageNum?: number
  pageSize?: number
  module?: string
  /** 对应后端实体字段 action */
  operationType?: string
  operatorName?: string
  /** 兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss */
  startDate?: string
  endDate?: string
  status?: number
}

export interface LoginLogQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  loginType?: number
  loginResult?: number
  startDate?: string
  endDate?: string
}

/**
 * ⚠️ 历史遗留接口定义（**字段名与后端实体不符**）
 *
 * `operationType` / `operatorName` / `operationTime` / `ipAddress` / `description`
 * 在「系统日志」后端响应中**都不存在**（真实字段为 action / username / operTime / operIp；
 * 也没有 description 列）—— 它是「列表恒空」故障的同源产物。
 * 仅为不破坏仍在使用它的 `views/system/log/index.vue` 而原样保留；
 * 新页面请一律使用 `OperLogRow` / `LoginLogRow`。
 */
export interface OperationLog {
  id: number
  module: string
  operationType: string
  description: string
  requestUrl: string
  requestMethod: string
  operatorName: string
  ipAddress: string
  operationTime: string
  costTime: number
  status: number // 0: 成功, 1: 失败
  requestParams?: string
  responseResult?: string
}

// ── 日志查询参数（历史类型，保留给 views/system/log） ──────────────
export interface LogQuery {
  module?: string
  operationType?: string
  operatorName?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 日志 API。
 * 返回值刻意声明为 `Promise<any>`（拦截器已拆包，形如 `{ records, total }` 或裸数组），
 * 避免误用 `res.data` 这一历史坑。
 */
export const logApi = {
  // ── 系统日志 ──────────────────────────────────────────────

  /** 分页查询系统日志 → { records, total } */
  getPage(params: OperLogQuery): Promise<any> {
    return request.get('/log/page', params)
  },

  /** 系统日志详情（含 requestParams / responseResult 两个大字段） */
  getById(id: number | string): Promise<any> {
    return request.get(`/log/${id}`)
  },

  /** 模块下拉值域（数据驱动：后端对 module 去重） → string[] */
  getModules(): Promise<any> {
    return request.get('/log/modules')
  },

  /** 操作类型下拉值域（数据驱动：后端对 action 去重） → string[] */
  getOperationTypes(): Promise<any> {
    return request.get('/log/operation-types')
  },

  /** 清理前预览：返回「保留 days 天」将删除的条数 */
  previewClear(days: number): Promise<any> {
    return request.get('/log/clear/preview', { days })
  },

  /**
   * 清理历史日志（**只删 oper_time 早于 now()-days 的记录，不是清空全部**）。
   * 后端 `days < 7` 直接拒绝。
   */
  clearLogs(days = 90): Promise<any> {
    return request.delete(`/log/clear?days=${days}`)
  },

  /** 导出系统日志（CSV，UTF-8 BOM） → Blob */
  exportLogs(params: OperLogQuery): Promise<any> {
    return request.get('/log/export', params, { responseType: 'blob' })
  },

  // ── 登录日志（对标 ql361「登录日志」Tab） ─────────────────────

  /** 分页查询登录日志 → { records, total } */
  getLoginPage(params: LoginLogQuery): Promise<any> {
    return request.get('/log/login/page', params)
  },

  /** 导出登录日志（CSV，UTF-8 BOM） → Blob */
  exportLoginLogs(params: LoginLogQuery): Promise<any> {
    return request.get('/log/login/export', params, { responseType: 'blob' })
  }
}

export default logApi
