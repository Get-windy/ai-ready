<template>
  <ErrorBoundary>
    <PageContainer
      title="流程设计"
      full-height
    >
      <a-alert
        type="info"
        show-icon
        class="tip-alert"
        message="流程定义对接自后端 /api/workflow/definitions 实时数据。"
        description="后端仅开放流程定义的列表 / 详情 / 新建接口：保存已有流程会以同一定义ID重新提交并自动生成新版本；停用/启用通过同一保存通道切换 enabled 实现；删除接口尚未开放。"
      />
      <div class="designer-layout">
        <!-- 左侧：流程定义列表 -->
        <div class="definition-panel">
          <div class="definition-panel-header">
            <a-select
              v-model:value="queryType"
              placeholder="全部类型"
              allow-clear
              size="small"
              style="width: 130px"
              @change="loadDefinitions"
            >
              <a-select-option
                v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP"
                :key="k"
                :value="k"
              >
                {{ v.label }}
              </a-select-option>
            </a-select>
            <a-button
              size="small"
              @click="loadDefinitions"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
            </a-button>
            <a-button
              type="primary"
              size="small"
              @click="handleCreate"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              新建
            </a-button>
          </div>
          <a-spin :spinning="listLoading">
            <div class="definition-list">
              <div
                v-for="item in definitions"
                :key="item.definitionId"
                class="definition-item"
                :class="{ 'definition-item-active': item.definitionId === draft.definitionId }"
                @click="handleSelect(item)"
              >
                <div class="definition-item-main">
                  <div class="definition-item-name">
                    {{ item.name }}
                  </div>
                  <div class="definition-item-meta">
                    <a-tag
                      :color="typeTag(item.type).color"
                      class="definition-tag"
                    >
                      {{ typeTag(item.type).label }}
                    </a-tag>
                    <span>v{{ item.version }}</span>
                    <span>{{ item.nodes?.length ?? 0 }} 节点</span>
                  </div>
                </div>
                <div
                  class="definition-item-side"
                  @click.stop
                >
                  <a-tooltip :title="item.enabled ? '点击停用' : '点击启用'">
                    <a-switch
                      :checked="item.enabled"
                      :loading="togglingId === item.definitionId"
                      size="small"
                      @change="(checked) => handleToggleEnabled(item, checked as boolean)"
                    />
                  </a-tooltip>
                </div>
              </div>
              <a-empty
                v-if="!listLoading && definitions.length === 0"
                description="暂无流程定义"
                :image-style="{ height: '60px' }"
              />
            </div>
          </a-spin>
        </div>

        <!-- 右侧：画布区 -->
        <div class="canvas-area">
          <div class="canvas-toolbar">
            <div class="canvas-toolbar-left">
              <span class="draft-title">
                {{ draft.name || '未命名流程' }}
                <a-tag v-if="draft.definitionId">v{{ draft.version }}</a-tag>
                <a-tag
                  v-else
                  color="blue"
                >新建</a-tag>
                <a-tag :color="draft.enabled ? 'success' : 'default'">
                  {{ draft.enabled ? '启用' : '停用' }}
                </a-tag>
                <a-tag
                  v-if="dirty"
                  color="warning"
                >未保存</a-tag>
              </span>
            </div>
            <div class="canvas-toolbar-right">
              <a-button
                size="small"
                @click="metaModalVisible = true"
              >
                <template #icon>
                  <SettingOutlined />
                </template>
                流程信息
              </a-button>
              <a-button
                size="small"
                @click="addNode"
              >
                <template #icon>
                  <PlusOutlined />
                </template>
                添加审批节点
              </a-button>
              <a-button
                type="primary"
                size="small"
                :loading="saving"
                :disabled="!dirty && !!draft.definitionId"
                @click="handleSave"
              >
                <template #icon>
                  <SaveOutlined />
                </template>
                保存
              </a-button>
            </div>
          </div>
          <div class="canvas-wrapper">
            <CanvasBoard
              ref="canvasRef"
              :config="canvasConfig"
            >
              <template #connections>
                <Connection
                  v-for="conn in canvasConnections"
                  :key="conn.id"
                  :connection="conn"
                />
              </template>
              <template #nodes>
                <FlowNode
                  v-for="node in canvasNodes"
                  :key="node.id"
                  :node="node"
                  @click="handleNodeClick"
                />
              </template>
            </CanvasBoard>
            <div
              v-if="draft.nodes.length === 0"
              class="canvas-empty-tip"
            >
              暂无审批节点，点击右上角「添加审批节点」开始编排
            </div>
          </div>
        </div>
      </div>
    </PageContainer>

    <!-- 流程信息弹窗 -->
    <a-modal
      v-model:open="metaModalVisible"
      title="流程信息"
      width="560px"
      @ok="metaModalVisible = false"
    >
      <a-form
        :model="draft"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="流程名称"
              required
            >
              <a-input
                v-model:value="draft.name"
                placeholder="如：销售订单审批"
                @change="dirty = true"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="流程编码"
              required
            >
              <a-input
                v-model:value="draft.code"
                placeholder="如：SALE_ORDER_APPROVAL"
                :disabled="!!draft.definitionId"
                @change="dirty = true"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="流程类型"
              required
            >
              <a-select
                v-model:value="draft.type"
                placeholder="请选择流程类型"
                @change="dirty = true"
              >
                <a-select-option
                  v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP"
                  :key="k"
                  :value="k"
                >
                  {{ v.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用">
              <a-switch
                v-model:checked="draft.enabled"
                @change="dirty = true"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="流程描述">
          <a-textarea
            v-model:value="draft.description"
            :rows="2"
            placeholder="流程用途说明"
            @change="dirty = true"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 节点属性编辑抽屉 -->
    <a-drawer
      v-model:open="nodeDrawerVisible"
      title="审批节点属性"
      width="420px"
    >
      <template v-if="editingNode">
        <a-form layout="vertical">
          <a-form-item label="节点序号">
            <span>第 {{ editingNodeIndex + 1 }} 级审批（共 {{ draft.nodes.length }} 级）</span>
          </a-form-item>
          <a-form-item
            label="节点名称"
            required
          >
            <a-input
              v-model:value="editingNode.nodeName"
              placeholder="如：部门经理审批"
              @change="dirty = true"
            />
          </a-form-item>
          <a-form-item label="审批人类型">
            <a-select
              v-model:value="editingNode.approverType"
              @change="dirty = true"
            >
              <a-select-option
                v-for="(label, k) in APPROVER_TYPE_MAP"
                :key="k"
                :value="k"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="审批人">
            <a-select
              v-model:value="editingNode.approverIds"
              mode="tags"
              placeholder="审批人ID / 角色编码，回车添加"
              :token-separators="[',', ' ']"
              @change="dirty = true"
            />
          </a-form-item>
          <a-form-item label="审批模式">
            <a-select
              v-model:value="editingNode.approveMode"
              @change="dirty = true"
            >
              <a-select-option
                v-for="(label, k) in APPROVE_MODE_MAP"
                :key="k"
                :value="k"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="超时时间（小时）">
                <a-input-number
                  v-model:value="editingNode.timeoutHours"
                  :min="0"
                  style="width: 100%"
                  @change="dirty = true"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="超时处理">
                <a-select
                  v-model:value="editingNode.timeoutAction"
                  allow-clear
                  @change="dirty = true"
                >
                  <a-select-option value="autoApprove">
                    自动通过
                  </a-select-option>
                  <a-select-option value="autoReject">
                    自动驳回
                  </a-select-option>
                  <a-select-option value="notify">
                    仅提醒
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="条件表达式">
            <a-input
              v-model:value="editingNode.conditionExpression"
              placeholder="如：amount > 10000（留空表示始终经过此节点）"
              @change="dirty = true"
            />
          </a-form-item>
        </a-form>
        <div class="node-drawer-footer">
          <a-space>
            <a-button
              size="small"
              :disabled="editingNodeIndex <= 0"
              @click="moveNode(editingNodeIndex, -1)"
            >
              <template #icon>
                <ArrowUpOutlined />
              </template>
              上移
            </a-button>
            <a-button
              size="small"
              :disabled="editingNodeIndex >= draft.nodes.length - 1"
              @click="moveNode(editingNodeIndex, 1)"
            >
              <template #icon>
                <ArrowDownOutlined />
              </template>
              下移
            </a-button>
            <a-popconfirm
              title="确定删除该审批节点？"
              @confirm="removeNode(editingNodeIndex)"
            >
              <a-button
                danger
                size="small"
              >
                <template #icon>
                  <DeleteOutlined />
                </template>
                删除节点
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SettingOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { CanvasBoard, FlowNode, Connection } from '@/components/Workflow/Canvas'
import type { FlowNode as FlowNodeData } from '@/types/workflow/node'
import type { Connection as ConnectionData } from '@/types/workflow/connection'
import type { CanvasConfig } from '@/types/workflow/canvas'
import {
  approvalFlowApi,
  APPROVAL_FLOW_TYPE_MAP,
  APPROVER_TYPE_MAP,
  APPROVE_MODE_MAP,
  type ApprovalFlowDefinition,
  type ApprovalFlowNode
} from '@/api/workflow'

// ── 画布布局常量 ─────────────────────────────────────────
const CANVAS_CENTER_X = 320
const START_Y = 40
const START_H = 50
const APPROVAL_W = 180
const APPROVAL_H = 64
const GAP_Y = 88

const canvasConfig: Partial<CanvasConfig> = {
  minScale: 0.3,
  maxScale: 2,
  grid: {
    show: true,
    size: 20,
    color: '#d9d9d9',
    subSize: 5,
    subColor: '#f0f0f0'
  }
}

// ── 列表状态 ─────────────────────────────────────────────
const definitions = ref<ApprovalFlowDefinition[]>([])
const listLoading = ref(false)
const queryType = ref<string | undefined>(undefined)
const togglingId = ref<string>('')

// ── 当前编辑草稿 ─────────────────────────────────────────
interface DraftState {
  definitionId: string
  name: string
  code: string
  type: string | undefined
  description: string
  enabled: boolean
  version: number
  nodes: ApprovalFlowNode[]
}

const draft = reactive<DraftState>({
  definitionId: '',
  name: '',
  code: '',
  type: undefined,
  description: '',
  enabled: true,
  version: 0,
  nodes: []
})
const dirty = ref(false)
const saving = ref(false)
const metaModalVisible = ref(false)

// ── 节点抽屉 ─────────────────────────────────────────────
const nodeDrawerVisible = ref(false)
const editingNodeIndex = ref(-1)
const editingNode = computed<ApprovalFlowNode | null>(() => {
  if (editingNodeIndex.value < 0 || editingNodeIndex.value >= draft.nodes.length) return null
  return draft.nodes[editingNodeIndex.value]
})
const selectedNodeId = ref('')

// ── 画布数据（开始 → 审批节点 → 结束，纵向自动布局） ────────
function makeCanvasNode(
  id: string,
  type: FlowNodeData['type'],
  name: string,
  x: number,
  y: number,
  w: number,
  h: number
): FlowNodeData {
  const isStart = type === 'start'
  const isEnd = type === 'end'
  return {
    id,
    type,
    name,
    selectable: true,
    draggable: false,
    position: { x, y },
    status: 'idle',
    selected: selectedNodeId.value === id,
    inputs: isStart ? [] : [{ id: `${id}-in`, direction: 'input', name: '', position: { x: w / 2, y: 0 } }],
    outputs: isEnd ? [] : [{ id: `${id}-out`, direction: 'output', name: '', position: { x: w / 2, y: h } }]
  }
}

const canvasNodes = computed<FlowNodeData[]>(() => {
  const result: FlowNodeData[] = []
  let y = START_Y
  result.push(makeCanvasNode('__start__', 'start', '开始', CANVAS_CENTER_X - 50, y, 100, START_H))
  y += START_H + GAP_Y
  draft.nodes.forEach((node, idx) => {
    result.push(makeCanvasNode(
      node.nodeId || `node_${idx + 1}`,
      'task',
      node.nodeName || `审批节点${idx + 1}`,
      CANVAS_CENTER_X - APPROVAL_W / 2,
      y,
      APPROVAL_W,
      APPROVAL_H
    ))
    y += APPROVAL_H + GAP_Y
  })
  result.push(makeCanvasNode('__end__', 'end', '结束', CANVAS_CENTER_X - 50, y, 100, START_H))
  return result
})

const canvasConnections = computed<ConnectionData[]>(() => {
  const chain = canvasNodes.value
  const result: ConnectionData[] = []
  for (let i = 0; i < chain.length - 1; i++) {
    const source = chain[i]
    const target = chain[i + 1]
    result.push({
      id: `conn_${source.id}_${target.id}`,
      sourceNodeId: source.id,
      targetNodeId: target.id,
      sourcePortId: `${source.id}-out`,
      targetPortId: `${target.id}-in`,
      sourcePosition: { x: CANVAS_CENTER_X, y: source.position.y + (source.type === 'task' ? APPROVAL_H : START_H) },
      targetPosition: { x: CANVAS_CENTER_X, y: target.position.y },
      selected: false,
      style: { type: 'straight', showArrow: true }
    })
  }
  return result
})

// ── 工具函数 ─────────────────────────────────────────────
function typeTag(type: string): { label: string; color: string } {
  return APPROVAL_FLOW_TYPE_MAP[type] || { label: type || '未知', color: 'default' }
}

// ── 列表加载 ─────────────────────────────────────────────
async function loadDefinitions() {
  listLoading.value = true
  try {
    const res = await approvalFlowApi.list(queryType.value)
    definitions.value = res?.definitions || []
  } catch (e) {
    console.warn('[流程设计] 流程定义获取失败', e)
  } finally {
    listLoading.value = false
  }
}

// ── 选中 / 新建 ──────────────────────────────────────────
function applyDefinition(def: ApprovalFlowDefinition) {
  draft.definitionId = def.definitionId
  draft.name = def.name
  draft.code = def.code
  draft.type = def.type
  draft.description = def.description || ''
  draft.enabled = def.enabled
  draft.version = def.version
  draft.nodes = (def.nodes || [])
    .filter(n => !n.nodeType || n.nodeType === 'approval')
    .map(n => ({ ...n, approverIds: n.approverIds ? [...n.approverIds] : [] }))
  selectedNodeId.value = ''
  editingNodeIndex.value = -1
  nodeDrawerVisible.value = false
  dirty.value = false
}

async function handleSelect(item: ApprovalFlowDefinition) {
  if (dirty.value) {
    message.warning('当前流程有未保存的修改，请先保存')
    return
  }
  try {
    const detail = await approvalFlowApi.getById(item.definitionId)
    applyDefinition(detail || item)
  } catch (e) {
    console.warn('[流程设计] 流程详情获取失败，使用列表数据', e)
    applyDefinition(item)
  }
}

function handleCreate() {
  if (dirty.value) {
    message.warning('当前流程有未保存的修改，请先保存')
    return
  }
  draft.definitionId = ''
  draft.name = ''
  draft.code = ''
  draft.type = undefined
  draft.description = ''
  draft.enabled = true
  draft.version = 0
  draft.nodes = [{ nodeId: 'node_1', nodeName: '', nodeType: 'approval', approverType: 'user', approverIds: [], approveMode: 'single' }]
  selectedNodeId.value = ''
  editingNodeIndex.value = -1
  nodeDrawerVisible.value = false
  dirty.value = true
  metaModalVisible.value = true
}

// ── 启停（后端无独立启停接口，通过同一定义ID重新保存切换 enabled） ──
async function handleToggleEnabled(item: ApprovalFlowDefinition, enabled: boolean) {
  togglingId.value = item.definitionId
  try {
    let detail: ApprovalFlowDefinition = item
    try {
      detail = await approvalFlowApi.getById(item.definitionId) || item
    } catch {
      // 详情不可用时退回列表行数据
    }
    await approvalFlowApi.create({ ...detail, enabled })
    message.success(enabled ? '已启用' : '已停用')
    if (draft.definitionId === item.definitionId) {
      draft.enabled = enabled
    }
    await loadDefinitions()
  } catch (e) {
    console.warn('[流程设计] 启停切换失败', e)
    message.error('操作失败')
  } finally {
    togglingId.value = ''
  }
}

// ── 节点编排 ─────────────────────────────────────────────
function addNode() {
  draft.nodes.push({
    nodeId: `node_${draft.nodes.length + 1}`,
    nodeName: '',
    nodeType: 'approval',
    approverType: 'user',
    approverIds: [],
    approveMode: 'single'
  })
  dirty.value = true
  openNodeDrawer(draft.nodes.length - 1)
}

function removeNode(index: number) {
  draft.nodes.splice(index, 1)
  nodeDrawerVisible.value = false
  editingNodeIndex.value = -1
  selectedNodeId.value = ''
  dirty.value = true
}

function moveNode(index: number, dir: -1 | 1) {
  const target = index + dir
  if (target < 0 || target >= draft.nodes.length) return
  const [node] = draft.nodes.splice(index, 1)
  draft.nodes.splice(target, 0, node)
  editingNodeIndex.value = target
  dirty.value = true
}

function openNodeDrawer(index: number) {
  editingNodeIndex.value = index
  selectedNodeId.value = draft.nodes[index]?.nodeId || ''
  nodeDrawerVisible.value = true
}

function handleNodeClick(nodeId: string) {
  if (nodeId === '__start__' || nodeId === '__end__') {
    selectedNodeId.value = nodeId
    nodeDrawerVisible.value = false
    editingNodeIndex.value = -1
    return
  }
  const index = draft.nodes.findIndex((n, i) => (n.nodeId || `node_${i + 1}`) === nodeId)
  if (index >= 0) {
    openNodeDrawer(index)
  }
}

// ── 保存（新建走创建；已有定义按同ID重新提交，后端自动版本+1） ──
async function handleSave() {
  if (!draft.name.trim()) {
    message.warning('请填写流程名称')
    metaModalVisible.value = true
    return
  }
  if (!draft.code.trim()) {
    message.warning('请填写流程编码')
    metaModalVisible.value = true
    return
  }
  if (!draft.type) {
    message.warning('请选择流程类型')
    metaModalVisible.value = true
    return
  }
  const nodes = draft.nodes.filter(n => n.nodeName?.trim())
  if (nodes.length === 0) {
    message.warning('请至少配置一个审批节点')
    return
  }
  saving.value = true
  const isNew = !draft.definitionId
  try {
    const payload: Partial<ApprovalFlowDefinition> = {
      definitionId: draft.definitionId || undefined,
      name: draft.name.trim(),
      code: draft.code.trim(),
      type: draft.type,
      description: draft.description,
      enabled: draft.enabled,
      version: draft.version,
      nodes: nodes.map((n, idx) => ({
        ...n,
        nodeId: `node_${idx + 1}`,
        nodeType: 'approval',
        nextNodeId: idx < nodes.length - 1 ? `node_${idx + 2}` : '__end__'
      }))
    }
    const saved = await approvalFlowApi.create(payload)
    if (saved?.definitionId) {
      applyDefinition(saved)
    }
    dirty.value = false
    message.success(isNew ? '创建成功' : '保存成功（已生成新版本）')
    await loadDefinitions()
  } catch (e) {
    console.warn('[流程设计] 保存流程失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(loadDefinitions)
</script>

<style scoped>
.tip-alert {
  margin-bottom: 12px;
}

.designer-layout {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

/* ── 左侧流程定义列表 ── */
.definition-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.definition-panel-header {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.definition-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.definition-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-bottom: 6px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.definition-item:hover {
  border-color: #1890ff;
  background: #f0f7ff;
}

.definition-item-active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.definition-item-main {
  min-width: 0;
  flex: 1;
}

.definition-item-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.definition-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.definition-tag {
  margin-inline-end: 0;
}

.definition-item-side {
  margin-left: 8px;
}

/* ── 右侧画布区 ── */
.canvas-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.draft-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.canvas-toolbar-right {
  display: flex;
  gap: 8px;
}

.canvas-wrapper {
  position: relative;
  flex: 1;
  min-height: 0;
}

.canvas-empty-tip {
  position: absolute;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  pointer-events: none;
}

.node-drawer-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}
</style>
