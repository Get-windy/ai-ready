import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// ═══════════════════════════════════════════════════════════════
// 共享类型
// ═══════════════════════════════════════════════════════════════

/** 边距配置 */
export interface MarginConfig {
  top: number
  bottom: number
  left: number
  right: number
}

/** 打印模板 */
export interface PrintTemplateVO {
  templateId: number
  tenantId: number
  pageCode: string
  templateName: string
  templateJson: string
  paperSize: string
  paperWidth: number
  paperHeight: number
  marginTop: number
  marginBottom: number
  marginLeft: number
  marginRight: number
  status: number
  isDefault: boolean
  version: number
  createdAt: string
  updatedAt: string
}

/** 创建模板请求 */
export interface PrintTemplateCreateRequest {
  pageCode: string
  templateName: string
  templateJson: string
  paperSize: string
  paperWidth: number
  paperHeight: number
  margins?: MarginConfig
}

/** 模板查询参数 */
export interface PrintTemplateQuery {
  page: number
  size: number
  pageCode?: string
  status?: number
}

/** 打印链 */
export interface PrintChainVO {
  chainId: number
  tenantId: number
  pageCode: string
  chainName: string
  description: string
  status: string
  sortOrder: number
  createdAt: string
  items?: ChainItemVO[]
}

/** 链节点 */
export interface ChainItemVO {
  itemId: number
  chainId: number
  stepOrder: number
  templateId: number
  templateName: string
  clientId: number
  clientName: string
  printerName: string
  screenshotMode: string
  screenshotConfirmTimeout: number
}

/** 链节点请求 */
export interface ChainItemRequest {
  stepOrder: number
  templateId: number
  clientId: number
  printerName: string
  screenshotMode?: string
  screenshotConfirmTimeout?: number
}

/** 创建打印链请求 */
export interface PrintChainCreateRequest {
  pageCode: string
  chainName: string
  description?: string
  sortOrder?: number
  items: ChainItemRequest[]
}

/** 打印链查询参数 */
export interface PrintChainQuery {
  page: number
  size: number
  pageCode?: string
}

/** 打印客户端 */
export interface PrintClientVO {
  clientId: number
  tenantId: number
  clientName: string
  clientCode: string
  authKey: string
  status: string
  lastHeartbeat: string
  clientIp: string
  clientVersion: string
  defaultPrinter: string
  createdAt: string
}

/** 注册客户端请求 */
export interface PrintClientRegisterRequest {
  clientName: string
  clientVersion?: string
}

/** 客户端查询参数 */
export interface PrintClientQuery {
  page: number
  size: number
  status?: string
}

/** 打印任务 */
export interface PrintTaskVO {
  taskId: number
  taskCode: string
  pageCode: string
  documentType: string
  documentId: number
  documentNo: string
  chainId: number
  chainName: string
  stepOrder: number
  clientId: number
  clientName: string
  printerName: string
  templateId: number
  templateName: string
  status: string
  priority: number
  errorMessage: string
  submitTime: string
  startTime: string
  completeTime: string
  screenshotId: number
  screenshotStatus: string
  createdAt: string
}

/** 执行打印链请求 */
export interface PrintExecuteRequest {
  chainId: number
  dataJson: string
  pageCode?: string
  documentType?: string
  documentId?: number
  documentNo?: string
}

/** 任务查询参数 */
export interface PrintTaskQuery {
  page: number
  size: number
  status?: string
  pageCode?: string
  documentType?: string
  documentNo?: string
  chainId?: number
  startTime?: string
  endTime?: string
}

/** 截图 */
export interface ScreenshotVO {
  screenshotId: number
  taskCode: string
  templateId: number
  imageUrl: string
  imageWidth: number
  imageHeight: number
  status: string
  errorMessage: string
  durationMs: number
  createdAt: string
}

/** 创建截图请求 */
export interface ScreenshotCreateRequest {
  templateId: number
  dataJson: string
  pageCode?: string
}

/** 截图查询参数 */
export interface ScreenshotQuery {
  page: number
  size: number
}

/** 公式校验请求 */
export interface FormulaValidateRequest {
  expression: string
  sampleValue?: string
}

/** 公式校验结果 */
export interface FormulaValidateResult {
  valid: boolean
  previewResult: string
  errorMessage: string
}

/** 模板渲染请求 */
export interface TemplateRenderRequest {
  templateJson: string
  dataJson: string
}

/** 模板渲染结果 */
export interface TemplateRenderResult {
  html: string
}

// ═══════════════════════════════════════════════════════════════
// 打印模块 API
// ═══════════════════════════════════════════════════════════════

const PREFIX = '/v2/print'

export const printingApi = {
  // ── 模板管理 ──────────────────────────────────────────

  /**
   * 创建打印模板
   */
  createTemplate(data: PrintTemplateCreateRequest): Promise<ApiResponse<PrintTemplateVO>> {
    return request.post(`${PREFIX}/templates`, data)
  },

  /**
   * 更新打印模板
   */
  updateTemplate(id: number, data: PrintTemplateCreateRequest): Promise<ApiResponse<PrintTemplateVO>> {
    return request.put(`${PREFIX}/templates/${id}`, data)
  },

  /**
   * 获取模板详情
   */
  getTemplate(id: number): Promise<ApiResponse<PrintTemplateVO>> {
    return request.get(`${PREFIX}/templates/${id}`)
  },

  /**
   * 分页查询打印模板
   */
  getTemplates(params: PrintTemplateQuery): Promise<ApiResponse<PageResponse<PrintTemplateVO>>> {
    return request.get(`${PREFIX}/templates`, params)
  },

  /**
   * 删除打印模板
   */
  deleteTemplate(id: number): Promise<ApiResponse<void>> {
    return request.delete(`${PREFIX}/templates/${id}`)
  },

  /**
   * 发布打印模板
   */
  publishTemplate(id: number): Promise<ApiResponse<void>> {
    return request.put(`${PREFIX}/templates/${id}/publish`)
  },

  /**
   * 复制打印模板
   */
  copyTemplate(id: number, newName: string): Promise<ApiResponse<PrintTemplateVO>> {
    return request.post(`${PREFIX}/templates/${id}/copy`, { newName })
  },

  // ── 打印链管理 ────────────────────────────────────────

  /**
   * 创建打印链
   */
  createChain(data: PrintChainCreateRequest): Promise<ApiResponse<PrintChainVO>> {
    return request.post(`${PREFIX}/chains`, data)
  },

  /**
   * 更新打印链
   */
  updateChain(id: number, data: PrintChainCreateRequest): Promise<ApiResponse<PrintChainVO>> {
    return request.put(`${PREFIX}/chains/${id}`, data)
  },

  /**
   * 获取打印链详情
   */
  getChain(id: number): Promise<ApiResponse<PrintChainVO>> {
    return request.get(`${PREFIX}/chains/${id}`)
  },

  /**
   * 分页查询打印链
   */
  getChains(params: PrintChainQuery): Promise<ApiResponse<PageResponse<PrintChainVO>>> {
    return request.get(`${PREFIX}/chains`, params)
  },

  /**
   * 删除打印链
   */
  deleteChain(id: number): Promise<ApiResponse<void>> {
    return request.delete(`${PREFIX}/chains/${id}`)
  },

  /**
   * 更新打印链状态（启用/禁用）
   */
  updateChainStatus(id: number, status: string): Promise<ApiResponse<void>> {
    return request.put(`${PREFIX}/chains/${id}/status`, { status })
  },

  // ── 客户端管理 ────────────────────────────────────────

  /**
   * 注册打印客户端
   */
  registerClient(data: PrintClientRegisterRequest): Promise<ApiResponse<PrintClientVO>> {
    return request.post(`${PREFIX}/clients/register`, data)
  },

  /**
   * 获取客户端详情
   */
  getClient(id: number): Promise<ApiResponse<PrintClientVO>> {
    return request.get(`${PREFIX}/clients/${id}`)
  },

  /**
   * 分页查询打印客户端
   */
  getClients(params: PrintClientQuery): Promise<ApiResponse<PageResponse<PrintClientVO>>> {
    return request.get(`${PREFIX}/clients`, params)
  },

  /**
   * 删除打印客户端
   */
  deleteClient(id: number): Promise<ApiResponse<void>> {
    return request.delete(`${PREFIX}/clients/${id}`)
  },

  /**
   * 重置客户端密钥
   */
  resetClientKey(id: number): Promise<ApiResponse<{ authKey: string }>> {
    return request.post(`${PREFIX}/clients/${id}/reset-key`)
  },

  // ── 任务管理 ──────────────────────────────────────────

  /**
   * 执行打印链
   */
  executeChain(data: PrintExecuteRequest): Promise<ApiResponse<PrintTaskVO[]>> {
    return request.post(`${PREFIX}/tasks/by-chain`, data)
  },

  /**
   * 获取任务详情
   */
  getTask(id: number): Promise<ApiResponse<PrintTaskVO>> {
    return request.get(`${PREFIX}/tasks/${id}`)
  },

  /**
   * 分页查询打印任务
   */
  getTasks(params: PrintTaskQuery): Promise<ApiResponse<PageResponse<PrintTaskVO>>> {
    return request.get(`${PREFIX}/tasks`, params)
  },

  /**
   * 取消打印任务
   */
  cancelTask(id: number): Promise<ApiResponse<void>> {
    return request.post(`${PREFIX}/tasks/${id}/cancel`)
  },

  /**
   * 确认截图
   */
  confirmScreenshot(id: number): Promise<ApiResponse<void>> {
    return request.post(`${PREFIX}/tasks/${id}/confirm-screenshot`)
  },

  /**
   * 重试截图（超时后可重新调起手动截图，不重新打印）
   */
  retryScreenshot(id: number): Promise<ApiResponse<void>> {
    return request.post(`${PREFIX}/screenshots/${id}/retry`)
  },

  /**
   * 获取待处理任务队列
   */
  getTaskQueue(): Promise<ApiResponse<PrintTaskVO[]>> {
    return request.get(`${PREFIX}/tasks/queue`)
  },

  // ── 截图管理 ──────────────────────────────────────────

  /**
   * 创建打印截图预览
   */
  createScreenshot(data: ScreenshotCreateRequest): Promise<ApiResponse<ScreenshotVO>> {
    return request.post(`${PREFIX}/screenshots`, data)
  },

  /**
   * 获取截图详情
   */
  getScreenshot(id: number): Promise<ApiResponse<ScreenshotVO>> {
    return request.get(`${PREFIX}/screenshots/${id}`)
  },

  /**
   * 分页查询截图
   */
  getScreenshots(params: ScreenshotQuery): Promise<ApiResponse<PageResponse<ScreenshotVO>>> {
    return request.get(`${PREFIX}/screenshots`, params)
  },

  // ── 格式工具 ──────────────────────────────────────────

  /**
   * 校验格式表达式
   */
  validateFormula(data: FormulaValidateRequest): Promise<ApiResponse<FormulaValidateResult>> {
    return request.post(`${PREFIX}/format/validate-formula`, data)
  },

  /**
   * 渲染打印模板
   */
  renderTemplate(data: TemplateRenderRequest): Promise<ApiResponse<TemplateRenderResult>> {
    return request.post(`${PREFIX}/format/render`, data)
  }
}

export default printingApi
