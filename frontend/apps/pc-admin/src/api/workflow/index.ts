import request from '@/utils/request'

// ── 工作流定义（**表实体口径**：processCode / processConfig / status） ──
//
// ⚠️ 与下方 approvalFlowApi 的**页面模型口径**（code / nodes / enabled）不是同一套结构；
//    流程定义（801）/ 流程设计（80610）页用的是 approvalFlowApi，本对象供其它入口按需复用。
//
// 路径逐条对后端 WorkflowController（前缀 /api/workflow）核准（2026-09-18）：
//   GET    /api/workflow/definitions                        → :60  列表（**无 /page 分页端点**）
//   GET    /api/workflow/definitions/{definitionId}         → :74  详情（不存在 → 404）
//   POST   /api/workflow/definitions                        → :86  新建
//   PUT    /api/workflow/definitions/{definitionId}         → :96  更新（版本 +1）
//   POST   /api/workflow/definitions/{definitionId}/publish → :110 发布/启用（只改 status，**不动 version**）
//   POST   /api/workflow/definitions/{definitionId}/disable → :123 停用（只改 status，**不动 version**）
//   DELETE /api/workflow/definitions/{definitionId}         → :136 删除（有实例引用 → 400）
//
// ⚠️ 原 7 条路径有 3 条与后端对不上，本轮已改正：
//    · `GET /definitions/page` —— 后端无此端点，且会被 `/{definitionId}` 吞成 id="page"（404）→ 改为 `GET /definitions`
//    · `PUT /definitions/{id}/publish` —— 后端是 **POST**（PUT 会 405）→ 已改 POST
//    · `PUT /definitions/{id}/disable` —— 后端是 **POST**（PUT 会 405）→ 已改 POST
//    · 返回值类型原先也写错（create 声明 number、update 声明 boolean），已按后端实际返回体订正
// ⚠️ 相对路径即可：axios baseURL 已是 /api，写 /api/... 会双前缀 404。
// ⚠️ 雪花 ID 是 BIGINT：id 一律按字符串处理，**禁止 Number(id)**。

export interface WorkflowDefinition {
  /** 主键（雪花 ID，字符串传输） */
  id: string
  tenantId: string
  processCode: string
  processName: string
  processType: number
  description: string
  processConfig: string
  version: number
  isDefault: number
  status: number
  createTime: string
  updateTime: string
}

export const workflowDefinitionApi = {
  /** 流程定义列表（可按类型过滤）；后端返回 { definitions, total }，分页由页面侧完成 */
  list(params?: { type?: string }): Promise<{ definitions: WorkflowDefinition[]; total: number }> {
    return request.get('/workflow/definitions', { params })
  },
  getById(id: string): Promise<WorkflowDefinition> {
    return request.get(`/workflow/definitions/${id}`)
  },
  create(data: Partial<WorkflowDefinition>): Promise<WorkflowDefinition> {
    return request.post('/workflow/definitions', data)
  },
  update(id: string, data: Partial<WorkflowDefinition>): Promise<WorkflowDefinition> {
    return request.put(`/workflow/definitions/${id}`, data)
  },
  /** 发布（启用）：只改 status=1，不递增 version */
  publish(id: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/definitions/${id}/publish`)
  },
  /** 停用：只改 status=2，不递增 version */
  disable(id: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/definitions/${id}/disable`)
  },
  delete(id: string): Promise<{ success: boolean; message: string }> {
    return request.delete(`/workflow/definitions/${id}`)
  }
}

// ── 工作流实例（流程实例监控页 802 / workflow-instance 使用） ──
//
// 说明：下方 workflowDefinitionApi / workflowTaskApi 指向的是 core-base 的 /workflow 前缀控制器，
// 经 /api 代理访问不到；流程实例与任务相关的**可达端点**全部在 core-api WorkflowController。
//
// 路径逐条与 WorkflowController 核对（2026-09-18，相对路径即可，baseURL 已是 /api）：
//   GET  /workflow/instance/page            分页（processName/status/startDate/endDate/pageNum/pageSize + 头 X-Tenant-Id）
//   GET  /workflow/instance/stat            统计卡服务端全量聚合（与列表同筛选条件）
//   GET  /workflow/instance/detail          详情（含 records[] 审批记录）
//   GET  /workflow/instance/diagram         流程图（后端现场拼 SVG，非 BPMN）
//   POST /workflow/instance/{id}/intervene  干预（action: terminate/suspend/resume + reason）
//   GET  /workflow/{id}/records             审批记录（详情抽屉刷新时间线）
//   POST /workflow/start                    发起流程
//   POST /workflow/{id}/withdraw            撤回（注意是 POST，不是 PUT）
//
// ⚠️ 旧实现的 4 条路径全部与后端不符（`/workflow/instances/start` → 404；
//    `PUT /workflow/instances/{id}/withdraw` → 405；`/workflow/instances/{id}/history` 后端**不存在**，
//    对应能力是 `GET /workflow/{id}/records`；`GET /workflow/instances/{id}` 实际是 `/instances/{instanceId}`）。
//    本页从未使用旧对象，但为避免误用，这里按真实端点重写。

/*
 * 状态词表说明（后端 WorkflowConverter 有**两套**，前端展示映射见 instance-monitor.vue 的 INSTANCE_STATUS_MAP）：
 *   · 规范值 approving / approved / rejected / withdrawn / cancelled / suspended / terminated
 *     —— 筛选入参（instanceStatusToInt）与详情 canonicalStatus；
 *   · 监控值 running / completed / rejected / withdrawn / cancelled / suspended / terminated
 *     —— 列表返回（toMonitorStatus 把 approving→running、approved→completed，其余透传）。
 * 旧实现前端只认 4 个监控值，导致 rejected / withdrawn / cancelled 既不能筛选、又原样显示英文。
 */

/** 流程实例监控行（后端 toMonitorRecord 返回，字段一一对应） */
export interface WorkflowInstanceMonitorRow {
  /** 雪花 ID 字符串（后端 String.valueOf，前端禁止 Number() 转换） */
  instanceId: string
  definitionId: string
  processName: string
  workflowName?: string
  /** 监控页词表：running / completed / rejected / withdrawn / cancelled / suspended / terminated */
  status: string
  /** 规范词表：approving / approved / rejected / withdrawn / cancelled / suspended / terminated */
  canonicalStatus: string
  currentNode?: string
  currentNodeName?: string
  initiator?: string
  startTime?: string
  endTime?: string
  /** 服务端现算的耗时文案（如 "3.5h" / "2.0天"） */
  duration?: string
  businessType?: string
  tenantId?: string
}

/** 统计卡数据（GET /workflow/instance/stat，服务端全量聚合） */
export interface WorkflowInstanceStat {
  total: number
  running: number
  completed: number
  rejected: number
  withdrawn: number
  cancelled: number
  suspended: number
  terminated: number
}

/** 审批记录（详情抽屉时间线，后端 getInstanceDetail 内嵌 + GET /{id}/records 独立端点） */
export interface WorkflowApprovalRecord {
  nodeName?: string
  approverName?: string
  /** 动作词表（后端 ApprovalRecord.action，如 submit/approve/reject/transfer/intervene） */
  action?: string
  comment?: string
  time?: string
}

/** 流程实例详情（GET /workflow/instance/detail） */
export interface WorkflowInstanceDetail extends WorkflowInstanceMonitorRow {
  applicantId?: number
  businessData?: string
  result?: number
  /** 审批记录（含干预留痕），本轮前端渲染为时间线 */
  records?: WorkflowApprovalRecord[]
}

/** 分页查询筛选条件（查询区 4 项 + 分页） */
export interface WorkflowInstanceQuery {
  processName?: string
  status?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export const workflowInstanceApi = {
  /** 流程实例分页（监控台列表） */
  page(params: WorkflowInstanceQuery): Promise<{ records: WorkflowInstanceMonitorRow[]; total: number; page: number; pageSize: number }> {
    return request.get('/workflow/instance/page', { params })
  },
  /** 统计卡全量聚合（与列表同筛选条件；禁止用当页条数冒充全量） */
  stat(params: Omit<WorkflowInstanceQuery, 'pageNum' | 'pageSize'>): Promise<WorkflowInstanceStat> {
    return request.get('/workflow/instance/stat', { params })
  },
  /** 实例详情（含 records[] 审批记录） */
  getDetail(instanceId: string): Promise<WorkflowInstanceDetail> {
    return request.get('/workflow/instance/detail', { params: { instanceId } })
  },
  /** 流程图（返回 { svg, imageUrl }；后端为字符串拼接 SVG，非 BPMN） */
  getDiagram(instanceId: string): Promise<{ svg: string; imageUrl: string }> {
    return request.get('/workflow/instance/diagram', { params: { instanceId } })
  },
  /** 审批记录（详情抽屉刷新时间线用；详情接口已内嵌同一份数据） */
  getRecords(instanceId: string): Promise<WorkflowApprovalRecord[]> {
    return request.get(`/workflow/${instanceId}/records`)
  },
  /** 流程干预（终止 / 挂起 / 恢复），reason 为必填审计理由 */
  intervene(instanceId: string, action: 'terminate' | 'suspend' | 'resume', reason: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/instance/${instanceId}/intervene`, { action, reason })
  },
  /** 发起流程（后端可达端点；本页无入口，供其它页面复用） */
  start(params: { definitionId: string; businessId?: string; businessType?: string; businessData?: Record<string, unknown> }): Promise<{ success: boolean; instanceId: string; status: string; message: string }> {
    return request.post('/workflow/start', params)
  },
  /** 实例详情（模型层，非监控页口径） */
  getById(id: string): Promise<WorkflowInstance> {
    return request.get(`/workflow/instances/${id}`)
  },
  /** 撤回流程（注意后端是 POST，不是 PUT） */
  withdraw(id: string, comment?: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/${id}/withdraw`, { comment })
  }
}

/** 流程实例实体（模型层 GET /workflow/instances/{instanceId}；status 为 0–6 数值枚举） */
export interface WorkflowInstance {
  id: number
  definitionId: number
  businessId: number
  businessType: string
  title: string
  applicantId: number
  applicantName: string
  currentNodeId: number
  currentNodeName: string
  /** 0-审批中 1-已通过 2-已驳回 3-已撤回 4-已取消 5-已挂起 6-已终止 */
  status: number
  result: number // 0-通过 1-驳回
  comment: string
  createTime: string
  finishTime: string
}

// ── 工作流任务（设置 → 审批 → 我的待办 / 我的已办，菜单 803 / 804） ──
//
// 端点全部来自 core-api WorkflowController（前缀 /api/workflow），**路径与后端逐条核准（2026-09-18）**：
//   GET  /api/workflow/task/page      → 待办/已办分页（参数：tab / taskName / processName / priority / startDate / endDate / pageNum / pageSize）
//   GET  /api/workflow/task/stat      → 统计卡的服务端**全量真聚合**（与列表同筛选条件）
//   GET  /api/workflow/task/detail    → 任务详情（不存在 → 404）
//   POST /api/workflow/task/approve   → body {taskId, approval: approve|reject|return, comment, returnNode}
//   POST /api/workflow/task/transfer  → body {taskId, targetUser, comment}
//
// ⚠️ 历史缺陷（本轮修复）：本对象原 5 条路径全是 `/workflow/tasks/**`（复数 + 子路径），
//    WorkflowController **没有**任何 `/tasks/**` 映射 → 5 条全部 404。已按上表逐条改为真实端点。
// ⚠️ 本系统**没有「委托」语义**：任务表无 owner 类字段，「代处理并回交原处理人」无法实现，
//    历史 `type: transfer|delegate` 后端从未使用 → 前端不再暴露「委托」入口，只保留「转办」。

/** 任务分页/统计查询参数（与后端 pageTasks / statTasks 的签名逐项对齐） */
export interface WorkflowTaskQuery {
  /** 待办(todo) / 已办(done) */
  tab: 'todo' | 'done'
  taskName?: string
  processName?: string
  /** high / medium / low（取 workflow_task.priority，可空列） */
  priority?: string
  /** 创建时间下限 YYYY-MM-DD（含当天） */
  startDate?: string
  /** 创建时间上限 YYYY-MM-DD（含当天） */
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/** 任务分页行（后端 toTaskRecord 逐字段输出；taskId/instanceId 为雪花 ID 字符串，禁止 Number()） */
export interface WorkflowTaskRow {
  taskId: string
  /** 任务名 = workflow_task.node_name（节点名） */
  taskName: string | null
  instanceId: string
  processName: string | null
  /** 优先级（后端如实回传 workflow_task.priority，未设置时为 null → 界面显示「-」） */
  priority: string | null
  assignee: string
  createTime: string | null
  /** 截止时间 = create_time + 节点 time_limit 小时（现算，不落库） */
  dueTime: string | null
  /** 0-待处理 1-已处理 2-已转交 */
  status: number
  /** approve / reject / transfer / submit / withdraw / cancel / intervene / return / pending */
  action: string
  comment: string | null
  handleTime: string | null
}

/** 任务统计（GET /api/workflow/task/stat，全部为服务端 COUNT 真聚合） */
export interface WorkflowTaskStat {
  total: number
  pending: number
  done: number
  transferred: number
  priorityHigh: number
  priorityMedium: number
  priorityLow: number
  priorityUnset: number
  overdue: number
  today: number
  actionApprove: number
  actionReject: number
  actionReturn: number
  actionTransfer: number
  actionSubmit: number
  actionWithdraw: number
  actionCancel: number
  actionIntervene: number
  actionUnset: number
}

/** 审批结果（POST /task/approve、/task/transfer 的返回体；HTTP 恒 200，成败在 success） */
export interface WorkflowTaskActionResult {
  success: boolean
  message: string
}

export const workflowTaskApi = {
  /** 待办 / 已办分页 */
  page(params: WorkflowTaskQuery): Promise<{ records: WorkflowTaskRow[]; total: number; page: number; pageSize: number }> {
    return request.get('/workflow/task/page', { params })
  },
  /** 统计卡的**全量**口径（不是当页条数） */
  stat(params: WorkflowTaskQuery): Promise<WorkflowTaskStat> {
    return request.get('/workflow/task/stat', { params })
  },
  /** 任务详情 */
  detail(taskId: string): Promise<Record<string, any>> {
    return request.get('/workflow/task/detail', { params: { taskId } })
  },
  /** 同意 */
  approve(taskId: string, comment?: string): Promise<WorkflowTaskActionResult> {
    return request.post('/workflow/task/approve', { taskId, approval: 'approve', comment })
  },
  /** 驳回（终止流程） */
  reject(taskId: string, comment?: string): Promise<WorkflowTaskActionResult> {
    return request.post('/workflow/task/approve', { taskId, approval: 'reject', comment })
  },
  /** 退回（真实节点回退：回到发起人 / 上一节点并重建待办） */
  returnTask(taskId: string, returnNode: 'start' | 'previous', comment?: string): Promise<WorkflowTaskActionResult> {
    return request.post('/workflow/task/approve', { taskId, approval: 'return', returnNode, comment })
  },
  /** 转办（任务所有权转移给目标用户） */
  transfer(taskId: string, targetUser: string, comment?: string): Promise<WorkflowTaskActionResult> {
    return request.post('/workflow/task/transfer', { taskId, targetUser, comment })
  }
}

// ── 审批流程定义（**页面模型口径**，流程定义 801 / 流程设计 80610 共用本组） ──
//
// 端点全部来自 core-api WorkflowController（前缀 /api/workflow，已确认注册可达），
// 路径与动词逐条核准（2026-09-18）：
//   GET    /api/workflow/definitions                        → :60  列表（可按 type 过滤）
//   GET    /api/workflow/definitions/{definitionId}         → :74  详情（不存在 → 404）
//   POST   /api/workflow/definitions                        → :86  新建
//   PUT    /api/workflow/definitions/{definitionId}         → :96  更新（**版本 +1**）
//   POST   /api/workflow/definitions/{definitionId}/publish → :110 发布/启用（只改 status，**不动 version**）
//   POST   /api/workflow/definitions/{definitionId}/disable → :123 停用（只改 status，**不动 version**）
//   DELETE /api/workflow/definitions/{definitionId}         → :136 删除（有实例引用 → 400）
//
// ⚠️ 启停**必须**走 publish / disable 两个独立端点：历史上启停借道「POST /definitions 带 definitionId」
//    的保存通道，后端的更新分支会 `version + 1` 并把 process_config / workflow_node 整份重写
//    （WorkflowServiceImpl.updateWorkflowDefinition）→ 点一次开关就等于发一次新版本。

export interface ApprovalFlowNode {
  nodeId?: string
  nodeName: string
  nodeType?: string
  approverType?: string
  approverIds?: string[]
  approveMode?: string
  timeoutHours?: number
  timeoutAction?: string
  nextNodeId?: string
  conditionExpression?: string
}

export interface ApprovalFlowDefinition {
  definitionId: string
  name: string
  code: string
  type: string
  description?: string
  version: number
  nodes?: ApprovalFlowNode[]
  enabled: boolean
  createTime?: string
  updateTime?: string
}

export const APPROVAL_FLOW_TYPE_MAP: Record<string, { label: string; color: string }> = {
  order: { label: '订单审批', color: 'blue' },
  purchase: { label: '采购审批', color: 'purple' },
  expense: { label: '报销审批', color: 'orange' },
  leave: { label: '请假审批', color: 'green' },
  finance: { label: '财务审批', color: 'cyan' },
  custom: { label: '自定义流程', color: 'default' }
}

export const APPROVER_TYPE_MAP: Record<string, string> = {
  user: '指定用户',
  role: '指定角色',
  dept_leader: '部门负责人',
  applicant_self: '申请人本人'
}

export const APPROVE_MODE_MAP: Record<string, string> = {
  single: '单人审批',
  or: '或签（任一通过）',
  and: '会签（全部通过）'
}

export const approvalFlowApi = {
  /** 流程定义列表（可按类型过滤），返回 { definitions, total } */
  list(type?: string): Promise<{ definitions: ApprovalFlowDefinition[]; total: number }> {
    return request.get('/workflow/definitions', type ? { type } : {})
  },
  /** 流程定义详情 */
  getById(definitionId: string): Promise<ApprovalFlowDefinition> {
    return request.get(`/workflow/definitions/${definitionId}`)
  },
  /** 新建流程定义 */
  create(data: Partial<ApprovalFlowDefinition>): Promise<ApprovalFlowDefinition> {
    return request.post('/workflow/definitions', data)
  },
  /** 更新流程定义（含节点，后端会 version+1 并整份重写 process_config / workflow_node） */
  update(definitionId: string, data: Partial<ApprovalFlowDefinition>): Promise<ApprovalFlowDefinition> {
    return request.put(`/workflow/definitions/${definitionId}`, data)
  },
  /** 发布（启用）：只改 status，不动 version —— 启停开关必须走这里 */
  publish(definitionId: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/definitions/${definitionId}/publish`)
  },
  /** 停用：只改 status，不动 version */
  disable(definitionId: string): Promise<{ success: boolean; message: string }> {
    return request.post(`/workflow/definitions/${definitionId}/disable`)
  },
  /** 删除（后端有「存在实例引用 → 400」保护） */
  remove(definitionId: string): Promise<{ success: boolean; message: string }> {
    return request.delete(`/workflow/definitions/${definitionId}`)
  }
}

// ── 审核设置（设置 → 系统配置 → 审核设置，菜单 80622 / set:audit-config） ──
//
// 语义：本页对标 ql361「审核设置」的**规则矩阵**（三列：单据 / 审核设置 / 摘要），
// 与上面的「流程定义台账」不是同一件事，故走独立的 audit-config 端点。
// 后端：core-api WorkflowController（同一个控制器，已补方法级权限码 workflow:audit:*）。
// 路径逐条核准（相对路径即可，axios 的 baseURL 已是 /api，写 /api/... 会双前缀 404）：
//   GET /api/workflow/audit-config/list             → 16 类单据 + 条件目录
//   PUT /api/workflow/audit-config/{docType}        → 全量覆盖该单据的规则

/** 审批人（userId 为雪花 ID 字符串，禁止 Number() 转换） */
export interface AuditRuleApprover {
  userId: string
  userName: string
}

/** 单条规则：一个审核条件 + 一组审批人（多人为「或签」：任一审核即通过） */
export interface AuditRuleNode {
  condition: string
  conditionLabel: string
  approvers: AuditRuleApprover[]
}

/** 一行单据的审核设置（列表行 = 弹窗回读行，结构一致） */
export interface AuditDocRule {
  docType: string
  docName: string
  configured: boolean
  ruleCount: number
  /** 摘要文案（后端拼装）：条件描述 + 时提交给[审批人]审核; 多条直接相连；未配置时为空串 */
  summary: string
  rules: AuditRuleNode[]
}

export interface AuditConfigListResult {
  list: AuditDocRule[]
  /** 可选审核条件目录（后端逐字取自 ql361 摘要文案） */
  conditions: { value: string; label: string }[]
}

export const auditConfigApi = {
  /** 16 类单据的审核规则 + 摘要 + 条件目录（一次取齐） */
  list(): Promise<AuditConfigListResult> {
    return request.get('/workflow/audit-config/list')
  },
  /** 保存某类单据的审核规则（全量覆盖），返回保存后的该行 */
  save(docType: string, rules: { condition: string; approvers: AuditRuleApprover[] }[]): Promise<AuditDocRule> {
    return request.put(`/workflow/audit-config/${docType}`, { rules })
  }
}

// ── 流程分析（设置 → 工作流 → 流程分析，菜单 80611 / set:workflow-analysis） ──
//
// 本系统独有页面（ql361 无工作流域，无对标）。两条端点均为服务端真实聚合，页面内不落任何假数据。
// 路径逐条对后端 WorkflowController（前缀 /api/workflow）核准（2026-09-18）：
//   GET /api/workflow/analysis/refresh?processName=      → 统计卡 + 按流程耗时 + 按节点耗时
//   GET /api/workflow/analysis/report?startDate=&endDate= → 按日审批效率（缺省近 7 天）
// ⚠️ 两条端点带方法级权限码 workflow:analysis:view（迁移 V11.417.0 播种子）。
// ⚠️ 相对路径即可：axios baseURL 已是 /api，写 /api/... 会双前缀 404。

/** 统计卡聚合（GET /workflow/analysis/refresh 的 statistics 段） */
export interface WorkflowAnalysisStatistics {
  /** 流程实例总数（全租户口径，服务端 COUNT 聚合） */
  totalInstances: number
  /** 运行中实例数（status = 0） */
  runningInstances: number
  /** 待办审批任务数（status = 0 且 task_type = 1） */
  todoTasks: number
  /** 已完成实例的平均耗时（天，一位小数） */
  avgDuration: number
}

/** 按流程分组的耗时行 */
export interface WorkflowProcessDurationRow {
  processName: string
  instanceCount: number
  /** 服务端格式化文案，如「1.2天」 */
  avgDuration: string
  maxDuration: string
  minDuration: string
}

/** 按节点分组的耗时/超时行（可选 processName 过滤） */
export interface WorkflowNodeDurationRow {
  nodeName: string
  taskCount: number
  /** 服务端格式化文案，如「4.2h」 */
  avgDuration: string
  overdueCount: number
  /** 服务端算好的百分比串，如「6.8%」 */
  overdueRate: string
}

/** 汇总聚合响应（GET /workflow/analysis/refresh） */
export interface WorkflowAnalysisSummary {
  data?: WorkflowAnalysisStatistics
  statistics?: WorkflowAnalysisStatistics
  processDuration: WorkflowProcessDurationRow[]
  nodeDuration: WorkflowNodeDurationRow[]
  /** 服务端生成时间 */
  refreshedAt?: string
}

/** 按日审批效率行（GET /workflow/analysis/report 的 records） */
export interface WorkflowAnalysisReportRow {
  date: string
  totalTasks: number
  completedTasks: number
  /** 完成率百分比整数（0–100），前端进度条直接用 */
  completionRate: number
  avgDuration: string
  overdueTasks: number
  /** 效率评分 = 完成率 / 20（0–5），前端 5 星只读展示 */
  efficiency: number
}

export const workflowAnalysisApi = {
  /** 汇总聚合（processName 为空 = 全部流程；仅影响 nodeDuration 的节点耗时口径） */
  refresh(params?: { processName?: string }): Promise<WorkflowAnalysisSummary> {
    return request.get('/workflow/analysis/refresh', { params })
  },
  /** 按日审批效率报表（缺省近 7 天，与后端缺省口径一致） */
  report(params?: { startDate?: string; endDate?: string }): Promise<{ records: WorkflowAnalysisReportRow[]; startDate: string; endDate: string }> {
    return request.get('/workflow/analysis/report', { params })
  }
}

// ── 流程类型枚举 ──

export const PROCESS_TYPE_MAP: Record<number, string> = {
  1: '请假审批',
  2: '报销审批',
  3: '采购审批',
  4: '销售审批',
  5: '费用审批',
  6: '其他审批'
}

export const DEFINITION_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已发布', color: 'success' },
  2: { text: '已停用', color: 'error' }
}

/**
 * 实例状态的**数值**词表（workflow_instance.status 0–6，WorkflowConverter 枚举）
 * ⚠️ 监控页列表返回的是**字符串**词表（running/completed/...），映射见 instance-monitor.vue
 */
export const INSTANCE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '运行中', color: 'processing' },
  1: { text: '已完成', color: 'success' },
  2: { text: '已驳回', color: 'error' },
  3: { text: '已撤回', color: 'warning' },
  4: { text: '已取消', color: 'default' },
  5: { text: '已挂起', color: 'warning' },
  6: { text: '已终止', color: 'error' }
}

/**
 * 任务动作枚举（与后端 WorkflowConverter.ACTION_* 逐一对应，**8 值**）
 *
 * 历史缺陷：本表只有 3 值（1/2/3），而 DB 实测已办记录中 `action = 4`(submit) 与 `action = 7`(intervene)
 * 合计占 64% → 补「审批结果」列前必须先补全枚举，否则多数已办记录无标签。
 * 8-退回 为本轮新增（「退回」由「reject + 备注」改为真实节点回退后，需要一个能如实表达该语义的动作码）。
 */
export const TASK_ACTION_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '同意', color: 'success' },
  2: { text: '驳回', color: 'error' },
  3: { text: '转交', color: 'warning' },
  4: { text: '提交', color: 'processing' },
  5: { text: '撤回', color: 'default' },
  6: { text: '取消', color: 'default' },
  7: { text: '干预', color: 'purple' },
  8: { text: '退回', color: 'orange' }
}

/**
 * 动作**字符串** → 展示标签（后端 `taskActionToString` 输出串口径，任务列表的 `action` 字段即此串）。
 * 注意：NULL 动作后端输出 `pending`（表示「无动作」），必须给出中文标签，不能直接把英文原文渲染到界面。
 */
export const TASK_ACTION_TEXT_MAP: Record<string, { text: string; color: string }> = {
  approve: { text: '同意', color: 'success' },
  reject: { text: '驳回', color: 'error' },
  transfer: { text: '转交', color: 'warning' },
  submit: { text: '提交', color: 'processing' },
  withdraw: { text: '撤回', color: 'default' },
  cancel: { text: '取消', color: 'default' },
  intervene: { text: '干预', color: 'purple' },
  return: { text: '退回', color: 'orange' },
  pending: { text: '无动作', color: 'default' }
}

/** 任务状态 → 展示标签（0-待处理 1-已处理 2-已转交） */
export const TASK_STATUS_TEXT_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'processing' },
  1: { text: '已处理', color: 'success' },
  2: { text: '已转交', color: 'warning' }
}