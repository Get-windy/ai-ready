import request from '@/utils/request'
import type { PageResult } from '@/api/common'

// ── 工作流定义 ──

export interface WorkflowDefinition {
  id: number
  tenantId: number
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
  page(params: { pageNum?: number; pageSize?: number; processName?: string; status?: number }): Promise<PageResult<WorkflowDefinition>> {
    return request.get('/workflow/definitions/page', { params })
  },
  getById(id: number): Promise<WorkflowDefinition> {
    return request.get(`/workflow/definitions/${id}`)
  },
  create(data: Partial<WorkflowDefinition>): Promise<number> {
    return request.post('/workflow/definitions', data)
  },
  update(id: number, data: Partial<WorkflowDefinition>): Promise<boolean> {
    return request.put(`/workflow/definitions/${id}`, data)
  },
  publish(id: number): Promise<boolean> {
    return request.put(`/workflow/definitions/${id}/publish`)
  },
  disable(id: number): Promise<boolean> {
    return request.put(`/workflow/definitions/${id}/disable`)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/workflow/definitions/${id}`)
  }
}

// ── 工作流实例 ──

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
  status: number // 0-运行中 1-已完成 2-已驳回 3-已撤回
  result: number // 0-通过 1-驳回
  comment: string
  createTime: string
  finishTime: string
}

export const workflowInstanceApi = {
  start(params: { definitionId: number; businessId?: number; businessType?: string; title: string }): Promise<number> {
    return request.post('/workflow/instances/start', params)
  },
  getById(id: number): Promise<WorkflowInstance> {
    return request.get(`/workflow/instances/${id}`)
  },
  withdraw(id: number): Promise<boolean> {
    return request.put(`/workflow/instances/${id}/withdraw`)
  },
  getHistory(id: number): Promise<WorkflowTask[]> {
    return request.get(`/workflow/instances/${id}/history`)
  }
}

// ── 工作流任务 ──

export interface WorkflowTask {
  id: number
  instanceId: number
  nodeId: number
  nodeName: string
  taskType: number // 1-审批 2-抄送
  assigneeId: number
  assigneeName: string
  status: number // 0-待处理 1-已处理 2-已转交
  action: number // 1-同意 2-驳回 3-转交
  comment: string
  handleTime: string
  createTime: string
}

export const workflowTaskApi = {
  getTodoTasks(): Promise<WorkflowTask[]> {
    return request.get('/workflow/tasks/todo')
  },
  getDoneTasks(): Promise<WorkflowTask[]> {
    return request.get('/workflow/tasks/done')
  },
  approve(id: number, comment?: string): Promise<boolean> {
    return request.put(`/workflow/tasks/${id}/approve`, null, { params: { comment } })
  },
  reject(id: number, comment?: string): Promise<boolean> {
    return request.put(`/workflow/tasks/${id}/reject`, null, { params: { comment } })
  },
  transfer(id: number, toUserId: number, comment?: string): Promise<boolean> {
    return request.put(`/workflow/tasks/${id}/transfer`, null, { params: { toUserId, comment } })
  }
}

// ── 审批流程定义（core-api /api/workflow，set/audit-config 审核设置页使用） ──
// 注意：上方 workflowDefinitionApi 对应 core-base 的 /workflow 前缀控制器，
// 前端经 /api 代理访问不到；本组端点来自 core-api WorkflowController（已确认注册可达）。
// 后端当前仅暴露：列表 / 详情 / 新建，无更新、停用、删除端点。

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

export const INSTANCE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '运行中', color: 'processing' },
  1: { text: '已完成', color: 'success' },
  2: { text: '已驳回', color: 'error' },
  3: { text: '已撤回', color: 'warning' }
}

export const TASK_ACTION_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '同意', color: 'success' },
  2: { text: '驳回', color: 'error' },
  3: { text: '转交', color: 'warning' }
}