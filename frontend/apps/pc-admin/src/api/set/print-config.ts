import request from '@/utils/request'

/**
 * 设置 → 打印管理 → 打印设置（菜单 80930）—— 租户级打印配置的读写。
 *
 * · 后端 `PrintConfigController`（`/api/set/print-config`，在 `cn.aiedge.erp.printing` 包内）：
 *   GET（读，无行则自动建行）/ PUT（部分更新，保存后回读）/ GET `/assistant`（打印助手下载信息）。
 * · 路径一律写**相对路径**（axios baseURL 已是 `/api`，写成 `/api/...` 会双前缀 404）。
 * · 拦截器已拆包：返回值直接就是后端对象，**不要再取 res.data**。
 * · 租户由后端从登录会话取，前端**不传** tenantId（传了也没有该入参）。
 *
 * 字段名逐字对应《设置模块/打印设置开发文档.md》§4.2（ql361 实拍 2026-09-18），不发明字段。
 *
 * ⚠️ 这里的配置**不是摆设**：跨模块共享的打印组件 `components/PrintDialog/printBehavior.ts`
 * 会读取本接口并把设置作用到打印行为（草稿门控 / 小数位格式化 / 打印内容拼接 / 助手跳过预览 /
 * 远程打印门控）。改这些字段前请同步改该文件。
 */

/**
 * 「打印内容」候选值。
 * 后端下发的选项集＝ql361 实测的 3 项（`批号 *数量` / `生产日期 *数量` / `批号 生产日期~到期日期 *数量`，
 * 2026-09-18 展开下拉浮层实测，证据 `tool-results/ql361/设置-deep/_summary.md` §三 3.1）——
 * 前端**不写死**，一律以后端 `options.printContent` 为准。
 */
export interface PrintContentOption {
  value: string
  label: string
}

/** 取值域元数据（由后端下发，前端不写死） */
export interface PrintConfigOptions {
  /** 打印内容候选值（实测 3 项） */
  printContent: PrintContentOption[]
  /** 小数位下界（本系统适配口径 0 = 整数；对标为 9 档「整数~8位小数」） */
  decimalMin: number
  /** 小数位上界（本系统适配口径 4，对齐主数据精度 numeric(18,4)；对标为 8 位） */
  decimalMax: number
}

/** 打印设置（后端 toView 输出；无设置行时后端按默认值自动建行） */
export interface PrintConfig {
  /**
   * 主键（雪花 ID）：**按字符串处理，禁止 Number(id)**
   * （超过 JS 安全整数上限会丢精度，见《JS大整数精度修复》）
   */
  id: string
  /** 租户 ID（后端从会话取，只读展示用） */
  tenantId: string
  /** 允许打印草稿 */
  allowDraftPrint: boolean
  /** 属性商品汇总打印 */
  attrSummaryPrint: boolean
  /** 批次效期商品汇总打印 */
  batchSummaryPrint: boolean
  /** 打印内容（如「批号 *数量」） */
  printContent: string
  /** 单据打印小数位数（总开关：关闭时「数量 / 单价」不生效） */
  decimalEnabled: boolean
  /** 数量小数位 */
  qtyDecimal: number
  /** 单价小数位 */
  priceDecimal: number
  /** 助手打印（开启后跳过预览直接打印） */
  assistantEnabled: boolean
  /** 远程打印 */
  remoteEnabled: boolean
  /** 最后更新时间（后端 LocalDateTime，可能为 null） */
  updateTime?: string | null
  /** 取值域元数据 */
  options: PrintConfigOptions
}

/** 保存入参（部分更新：不传的字段保持原值） */
export interface PrintConfigUpdatePayload {
  allowDraftPrint?: boolean
  attrSummaryPrint?: boolean
  batchSummaryPrint?: boolean
  printContent?: string
  decimalEnabled?: boolean
  qtyDecimal?: number
  priceDecimal?: number
  assistantEnabled?: boolean
  remoteEnabled?: boolean
}

/**
 * 打印**行为**配置（打印组件 `PrintDialog` 专用口径）。
 *
 * 与 {@link PrintConfig} 的区别：只含会改变打印行为的 7 项，**没有** id/租户/操作人/取值域，
 * 由 `GET /set/print-config/behavior` 下发 —— 该端点**不要求 `set:print-config:view`**，
 * 因此普通账号（仓库/配送/资料页的用户）也能让配置生效，且不会因 403 弹「没有操作权限」。
 */
export interface PrintBehaviorConfig {
  /** 允许打印草稿（false = 草稿单据不允许打印） */
  allowDraftPrint: boolean
  /** 属性商品汇总打印（本系统打印引擎暂无汇总行能力，仅保存） */
  attrSummaryPrint: boolean
  /** 批次效期商品汇总打印（同上） */
  batchSummaryPrint: boolean
  /** 打印内容（实测 3 项之一） */
  printContent: string
  /** 单据打印小数位数总开关 */
  decimalEnabled: boolean
  /** 数量小数位 */
  qtyDecimal: number
  /** 单价小数位 */
  priceDecimal: number
  /** 助手打印（开启后跳过预览直接打印） */
  assistantEnabled: boolean
  /** 远程打印 */
  remoteEnabled: boolean
}

/** 打印助手下载信息（地址由服务端下发；未配置时 enabled=false，前端须降级提示） */
export interface PrintAssistantInfo {
  /** 下载地址（未配置为空串，**不要渲染空链接**） */
  downloadUrl: string
  /** 版本号（未配置为空串） */
  version: string
  /** 是否已配置下载地址 */
  enabled: boolean
}

export const printConfigApi = {
  /** 读取打印设置（首次访问由后端按默认值建行） */
  get(): Promise<PrintConfig> {
    return request.get('/set/print-config')
  },

  /**
   * 读取打印行为配置（打印组件专用；无需 `set:print-config:view`）。
   * 打印弹窗一律走这个端点 —— 配置要对**所有**打印的人生效，不能只对管理员生效。
   */
  getBehavior(): Promise<PrintBehaviorConfig> {
    return request.get('/set/print-config/behavior')
  },

  /** 保存打印设置（后端保存后回读，返回库内真实值） */
  update(data: PrintConfigUpdatePayload): Promise<PrintConfig> {
    return request.put('/set/print-config', data)
  },

  /** 打印助手下载信息（配置落位：环境变量 / application.yml，不入库） */
  getAssistant(): Promise<PrintAssistantInfo> {
    return request.get('/set/print-config/assistant')
  }
}

export default printConfigApi
