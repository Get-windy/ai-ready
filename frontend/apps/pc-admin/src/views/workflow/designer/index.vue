<template>
  <ErrorBoundary @error="handleError">
    <!--
      流程定义（菜单 801 / workflow-definition / path=/workflow/definition）
      与流程设计（菜单 80610 / set:workflow-designer）**共用本组件**（DB component 逐字相同）。
      两者靠**读路由**分流（本轮修复前组件零路由读取，导致两菜单 100% 同一界面、页内标题恒为「流程设计」）：
        · 801   → 流程定义**台账**（CategoryListLayout + BillDetailTable），行内「设计」进设计器
        · 80610 → **直接进设计器**
      路由字段：route.name（= menu.routeName || menu.menuCode，见 router/dynamicRoutes.ts:988）优先，
       route.path 兜底（801 解析为 /workflow/definition）。
      本系统工作流为**自研审批引擎**（无 Flowable / Activiti / Camunda），流程图存
      workflow_definition.process_config（JSON 字符串，非 BPMN XML）。
    -->
    <PageContainer
      full-height
      :title="pageTitle"
    >
      <!-- ═══════════════ 视图一：流程定义台账（菜单 801） ═══════════════ -->
      <div
        v-if="activeView === 'ledger'"
        class="ledger-page"
      >
        <CategoryListLayout
          :tabs="[]"
          :show-category-panel="false"
          :show-table-footer="true"
          @search="handleSearch"
        >
          <!-- 工具栏右侧：刷新 / 新建流程 -->
          <template #toolbar-right>
            <a-space :size="8">
              <a-button
                size="small"
                :loading="listLoading"
                @click="loadDefinitions"
              >
                <ReloadOutlined /> 刷新
              </a-button>
              <a-button
                v-permission="'workflow:definition:save'"
                type="primary"
                size="small"
                @click="handleCreate"
              >
                <PlusOutlined /> 新建流程
              </a-button>
            </a-space>
          </template>

          <!-- 查询区：横向自适应网格（禁止纵向单列） -->
          <template #search-fields>
            <div class="search-area">
              <div
                ref="gridRef"
                class="search-grid"
              >
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchForm.name"
                    placeholder="流程名称"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div class="search-field-item">
                  <!-- 流程类型是**服务端**过滤项（后端 processTypeToInt 映射，见 WorkflowServiceImpl.getWorkflowDefinitions） -->
                  <a-select
                    v-model:value="searchForm.type"
                    placeholder="流程类型"
                    size="small"
                    allow-clear
                    :options="typeOptions"
                    @change="handleSearch"
                  />
                </div>
                <div class="search-field-item">
                  <!-- 状态是**页面侧**过滤项（后端列表端点只有 type 一个入参，不为本页新增查询参数） -->
                  <a-select
                    v-model:value="searchForm.enabled"
                    placeholder="状态"
                    size="small"
                    allow-clear
                    :options="ENABLED_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
                <div
                  ref="actionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <a-button
                    type="primary"
                    size="small"
                    @click="handleSearch"
                  >
                    查询
                  </a-button>
                  <a-button
                    size="small"
                    @click="handleReset"
                  >
                    重置
                  </a-button>
                </div>
              </div>
            </div>
          </template>

          <!-- 数据表：列配置齿轮挂在表头 rowNo 列 -->
          <template #table>
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="listLoading"
                :view-mode="true"
                :min-rows="0"
                row-key="definitionId"
                storage-key="workflow-definition-table-columns"
                global-config-key="workflow-definition-table-columns"
                :empty-text="ledgerEmptyText"
              >
                <template #nameCell="{ record }">
                  <a-tooltip
                    v-if="record.name"
                    placement="bottom"
                    :title="record.name"
                  >
                    <span class="cell-ellipsis">{{ record.name }}</span>
                  </a-tooltip>
                  <span
                    v-else
                    class="cell-empty"
                  >-</span>
                </template>

                <template #codeCell="{ record }">
                  <span class="cell-mono">{{ record.code || '-' }}</span>
                </template>

                <template #typeCell="{ record }">
                  <a-tag :color="typeTag(record.type).color">
                    {{ typeTag(record.type).label }}
                  </a-tag>
                </template>

                <template #versionCell="{ record }">
                  v{{ record.version ?? 1 }}
                </template>

                <template #nodeCountCell="{ record }">
                  {{ record.nodes?.length ?? 0 }}
                </template>

                <template #enabledCell="{ record }">
                  <a-tag :color="record.enabled ? 'success' : 'default'">
                    {{ record.enabled ? '启用' : '停用' }}
                  </a-tag>
                </template>

                <template #descriptionCell="{ record }">
                  <a-tooltip
                    v-if="record.description"
                    placement="bottom"
                    :title="record.description"
                  >
                    <span class="cell-ellipsis">{{ record.description }}</span>
                  </a-tooltip>
                  <span
                    v-else
                    class="cell-empty"
                  >-</span>
                </template>

                <template #updateTimeCell="{ record }">
                  {{ fmtDateTime(record.updateTime) }}
                </template>

                <!-- 行操作：设计（进设计器）/ 发布 / 停用 / 删除，四个端点后端均已实现 -->
                <template #actionCell="{ record }">
                  <div class="row-actions">
                    <a-button
                      type="link"
                      size="small"
                      @click="handleDesign(record)"
                    >
                      设计
                    </a-button>
                    <a-button
                      v-if="!record.enabled"
                      v-permission="'workflow:definition:publish'"
                      type="link"
                      size="small"
                      :loading="togglingId === record.definitionId"
                      @click="handleToggleEnabled(record, true)"
                    >
                      发布
                    </a-button>
                    <a-button
                      v-else
                      v-permission="'workflow:definition:disable'"
                      type="link"
                      size="small"
                      :loading="togglingId === record.definitionId"
                      @click="handleToggleEnabled(record, false)"
                    >
                      停用
                    </a-button>
                    <a-popconfirm
                      title="确定删除该流程定义？存在实例引用时后端会拒绝删除。"
                      @confirm="handleDelete(record)"
                    >
                      <a-button
                        v-permission="'workflow:definition:delete'"
                        type="link"
                        size="small"
                        danger
                      >
                        删除
                      </a-button>
                    </a-popconfirm>
                  </div>
                </template>
              </BillDetailTable>
            </div>
          </template>

          <!-- 底部：经典分页栏（后端列表端点无分页参数，分页在页面侧完成） -->
          <template #table-footer>
            <StandardPagination
              variant="classic"
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :total="filteredDefinitions.length"
              :page-size-options="[20, 50, 100]"
              @change="handlePageChange"
            />
          </template>
        </CategoryListLayout>
      </div>

      <!-- ═══════════════ 视图二：流程设计器（菜单 80610，或从 801 台账点「设计」进入） ═══════════════ -->
      <div
        v-else
        class="designer-page"
      >
        <a-alert
          type="info"
          show-icon
          class="designer-tip"
          message="流程设计器（本系统自研审批引擎，非 BPMN 引擎）"
          description="节点按顺序串联执行。「保存」会把整份节点链路写入 process_config 并镜像到 workflow_node，同时版本 +1；启停走独立的发布 / 停用端点，不会产生新版本。"
        />

        <div class="designer-shell">
          <div class="designer-toolbar">
            <div class="designer-toolbar-left">
              <a-button
                v-if="isLedgerRoute"
                size="small"
                @click="backToLedger"
              >
                <ArrowLeftOutlined /> 返回台账
              </a-button>
              <!-- 定义选择器：设计器内切换要编辑的流程定义（沿用原左侧列表的能力） -->
              <a-select
                v-model:value="pickerValue"
                placeholder="选择流程定义"
                size="small"
                allow-clear
                style="width: 220px"
                :options="pickerOptions"
                @change="handlePickerChange"
              />
              <a-button
                v-permission="'workflow:definition:save'"
                size="small"
                @click="handleCreate"
              >
                <PlusOutlined /> 新建
              </a-button>
              <span class="draft-title">
                {{ draft.name || '未命名流程' }}
                <a-tag v-if="draft.definitionId">v{{ draft.version }}</a-tag>
                <a-tag
                  v-else
                  color="blue"
                >
                  新建
                </a-tag>
                <a-tag :color="draft.enabled ? 'success' : 'default'">
                  {{ draft.enabled ? '启用' : '停用' }}
                </a-tag>
                <a-tag
                  v-if="dirty"
                  color="warning"
                >
                  未保存
                </a-tag>
              </span>
            </div>
            <div class="designer-toolbar-right">
              <a-button
                size="small"
                @click="metaModalVisible = true"
              >
                <SettingOutlined /> 流程信息
              </a-button>
              <a-button
                size="small"
                @click="addNode"
              >
                <PlusOutlined /> 添加审批节点
              </a-button>
              <a-button
                v-permission="'workflow:definition:save'"
                type="primary"
                size="small"
                :loading="saving"
                :disabled="!dirty && !!draft.definitionId"
                @click="handleSave"
              >
                <SaveOutlined /> 保存
              </a-button>
            </div>
          </div>

          <div class="canvas-area">
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
              <!-- 空态文案随「是否已选中/新建流程」变化 -->
              <div
                v-if="draft.nodes.length === 0"
                class="canvas-empty-tip"
              >
                {{ canvasEmptyText }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>

    <!-- ═══════ 弹窗一律放在布局之外 ═══════ -->

    <!-- 流程信息弹窗：「保存」真实提交（原先 @ok 只关闭弹窗，易被误解为保存） -->
    <a-modal
      v-model:open="metaModalVisible"
      title="流程信息"
      width="560px"
      ok-text="保存"
      :confirm-loading="saving"
      @ok="handleSave"
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
              <!-- 这里只改草稿态；真正的启停请在台账行内操作（走 publish / disable 端点，不会 +版本） -->
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
              <ArrowUpOutlined /> 上移
            </a-button>
            <a-button
              size="small"
              :disabled="editingNodeIndex >= draft.nodes.length - 1"
              @click="moveNode(editingNodeIndex, 1)"
            >
              <ArrowDownOutlined /> 下移
            </a-button>
            <a-popconfirm
              title="确定删除该审批节点？"
              @confirm="removeNode(editingNodeIndex)"
            >
              <a-button
                danger
                size="small"
              >
                <DeleteOutlined /> 删除节点
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SettingOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
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

defineOptions({ name: 'WorkflowDesigner' })

// ══════════════════ 路由分流（801 流程定义 / 80610 流程设计） ══════════════════
//
// route.name = menu.routeName || menu.menuCode（router/dynamicRoutes.ts:988），实测两菜单 route_name 均为 NULL：
//   · 801   → route.name = 'workflow-definition'，route.path = '/workflow/definition'
//   · 80610 → route.name = 'set:workflow-designer'，route.path = '/set/workflow-designer'
// 以「是否台账路由」判定；非台账路由一律按流程设计处理（菜单名与落点一致）。
const route = useRoute()
const isLedgerRoute = computed(() => {
  const routeName = String(route.name ?? '')
  return routeName === 'workflow-definition' || String(route.path || '').includes('/workflow/definition')
})

type PageView = 'ledger' | 'designer'
const activeView = ref<PageView>(isLedgerRoute.value ? 'ledger' : 'designer')
/** 标题跟着**视图**走：台账=流程定义（801 菜单名），设计器=流程设计（80610 菜单名） */
const pageTitle = computed(() => (activeView.value === 'designer' ? '流程设计' : '流程定义'))

// 同一组件被两个菜单复用，若被 keep-alive 复用实例，切菜单时要回到该菜单的默认视图
watch(isLedgerRoute, (isLedger) => {
  activeView.value = isLedger ? 'ledger' : 'designer'
})

// ══════════════════ 画布布局常量 ══════════════════
const CANVAS_CENTER_X = 320
const START_Y = 40
const START_H = 50
const APPROVAL_W = 180
const APPROVAL_H = 64
const GAP_Y = 88

const canvasRef = ref<InstanceType<typeof CanvasBoard> | null>(null)

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

// ══════════════════ 台账状态 ══════════════════
const definitions = ref<ApprovalFlowDefinition[]>([])
const listLoading = ref(false)
const togglingId = ref('')
/** 提交给 BillDetailTable 的当前页数据（组件用 v-model:data-source） */
const tableData = ref<ApprovalFlowDefinition[]>([])

const searchForm = reactive<{ name: string; type: string | undefined; enabled: string | undefined }>({
  name: '',
  type: undefined,
  enabled: undefined
})
const ENABLED_OPTIONS = [
  { label: '启用', value: 'yes' },
  { label: '停用', value: 'no' }
]
const typeOptions = computed(() =>
  Object.entries(APPROVAL_FLOW_TYPE_MAP).map(([value, item]) => ({ label: item.label, value }))
)

const pagination = reactive({ current: 1, pageSize: 20 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

const ledgerEmptyText = '该租户下暂无流程定义，点击右上角「新建流程」开始创建'

// 列定义：rowNo 列承载表头「列配置」齿轮（本系统金标准项）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'name', title: '流程名称', type: 'slot', slotName: 'nameCell', width: 200 },
  { key: 'code', title: '流程编码', type: 'slot', slotName: 'codeCell', width: 190 },
  { key: 'type', title: '流程类型', type: 'slot', slotName: 'typeCell', width: 120 },
  { key: 'version', title: '版本', type: 'slot', slotName: 'versionCell', width: 80, align: 'center' },
  { key: 'nodes', title: '节点数', type: 'slot', slotName: 'nodeCountCell', width: 80, align: 'center' },
  { key: 'enabled', title: '状态', type: 'slot', slotName: 'enabledCell', width: 90 },
  { key: 'description', title: '流程描述', type: 'slot', slotName: 'descriptionCell' },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'updateTimeCell', width: 170 },
  { key: 'action', title: '操作', type: 'slot', slotName: 'actionCell', width: 240, fixed: 'right' }
]

const filteredDefinitions = computed(() => {
  const keyword = searchForm.name.trim()
  return definitions.value.filter((item) => {
    if (keyword && !String(item.name || '').includes(keyword)) return false
    if (searchForm.enabled === 'yes' && !item.enabled) return false
    if (searchForm.enabled === 'no' && item.enabled) return false
    return true
  })
})

const pagedDefinitions = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return filteredDefinitions.value.slice(start, start + pagination.pageSize)
})

watch(
  pagedDefinitions,
  (rows) => {
    tableData.value = [...rows]
  },
  { immediate: true }
)

// ══════════════════ 设计器草稿 ══════════════════
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

/** 定义选择器：单独持有选中值，dirty 拦截失败时可回退显示 */
const pickerValue = ref<string | undefined>(undefined)
const pickerOptions = computed(() =>
  definitions.value.map((item) => ({
    label: `${item.name || '未命名流程'}（v${item.version ?? 1}）`,
    value: item.definitionId
  }))
)

// ══════════════════ 节点抽屉 ══════════════════
const nodeDrawerVisible = ref(false)
const editingNodeIndex = ref(-1)
const editingNode = computed<ApprovalFlowNode | null>(() => {
  if (editingNodeIndex.value < 0 || editingNodeIndex.value >= draft.nodes.length) return null
  return draft.nodes[editingNodeIndex.value]
})
const selectedNodeId = ref('')

const canvasEmptyText = computed(() =>
  draft.definitionId || dirty.value
    ? '暂无审批节点，点击工具栏「添加审批节点」开始编排'
    : '请先在上方选择一条流程定义，或点击「新建」开始编排'
)

// ══════════════════ 画布数据（开始 → 审批节点 → 结束，纵向自动布局） ══════════════════
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

// ══════════════════ 工具函数 ══════════════════
function typeTag(type: string): { label: string; color: string } {
  return APPROVAL_FLOW_TYPE_MAP[type] || { label: type || '未知', color: 'default' }
}

function fmtDateTime(value?: string | null): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

function handleError(error: Error) {
  console.error('[流程定义] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ══════════════════ 列表加载 / 查询 / 分页 ══════════════════
async function loadDefinitions() {
  listLoading.value = true
  try {
    const res = await approvalFlowApi.list(searchForm.type)
    definitions.value = res?.definitions || []
  } catch (e) {
    // 响应拦截器已弹出后端错误文案，这里只留痕，避免双重 toast
    console.error('[流程定义] 流程定义列表加载失败', e)
    definitions.value = []
  } finally {
    listLoading.value = false
  }
}

async function handleSearch() {
  pagination.current = 1
  await loadDefinitions()
}

async function handleReset() {
  searchForm.name = ''
  searchForm.type = undefined
  searchForm.enabled = undefined
  pagination.current = 1
  await loadDefinitions()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
}

// ══════════════════ 草稿装载 / 新建 ══════════════════
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
  pickerValue.value = def.definitionId || undefined
  selectedNodeId.value = ''
  editingNodeIndex.value = -1
  nodeDrawerVisible.value = false
  dirty.value = false
}

/** 拉详情后装载草稿；详情失败时退回列表行数据渲染（不出现空白编辑器） */
async function loadDraft(item: ApprovalFlowDefinition) {
  try {
    const detail = await approvalFlowApi.getById(item.definitionId)
    applyDefinition(detail || item)
  } catch (e) {
    console.warn('[流程定义] 流程详情获取失败，使用列表数据', e)
    applyDefinition(item)
  }
}

/** 台账行内「设计」：载入该定义并切到设计器 */
async function handleDesign(item: ApprovalFlowDefinition) {
  if (dirty.value) {
    message.warning('当前流程有未保存的修改，请先保存')
    return
  }
  await loadDraft(item)
  activeView.value = 'designer'
}

/** 设计器内切换定义（选择器变更）；dirty 时拒绝并回退选择器显示 */
async function handlePickerChange(value: unknown) {
  const target = definitions.value.find(item => item.definitionId === String(value ?? ''))
  if (!target) {
    pickerValue.value = draft.definitionId || undefined
    return
  }
  if (dirty.value) {
    message.warning('当前流程有未保存的修改，请先保存')
    pickerValue.value = draft.definitionId || undefined
    return
  }
  await loadDraft(target)
}

function backToLedger() {
  activeView.value = 'ledger'
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
  pickerValue.value = undefined
  selectedNodeId.value = ''
  editingNodeIndex.value = -1
  nodeDrawerVisible.value = false
  dirty.value = true
  activeView.value = 'designer'
  metaModalVisible.value = true
}

// ══════════════════ 启停（走独立端点，不产生新版本） ══════════════════
//
// 后端已实现两个**只改 status、不动 version** 的端点：
//   POST /definitions/{id}/publish  → status = 1（启用）
//   POST /definitions/{id}/disable  → status = 2（停用）
// 历史实现借道「POST /definitions 带 definitionId」的保存通道 → 后端走更新分支，version +1
// 且把 process_config / workflow_node 整份重写（WorkflowServiceImpl.updateWorkflowDefinition）。
async function handleToggleEnabled(item: ApprovalFlowDefinition, enabled: boolean) {
  togglingId.value = item.definitionId
  try {
    if (enabled) {
      await approvalFlowApi.publish(item.definitionId)
    } else {
      await approvalFlowApi.disable(item.definitionId)
    }
    message.success(enabled ? '已启用' : '已停用')
    if (draft.definitionId === item.definitionId) {
      draft.enabled = enabled
    }
    await loadDefinitions()
  } catch (e) {
    console.error('[流程定义] 启停切换失败', e)
  } finally {
    togglingId.value = ''
  }
}

/** 删除（后端有「存在实例引用 → 400」保护） */
async function handleDelete(item: ApprovalFlowDefinition) {
  if (draft.definitionId === item.definitionId && dirty.value) {
    message.warning('当前流程有未保存的修改，请先保存')
    return
  }
  try {
    await approvalFlowApi.remove(item.definitionId)
    message.success('删除成功')
    if (draft.definitionId === item.definitionId) {
      draft.definitionId = ''
      draft.nodes = []
      pickerValue.value = undefined
      dirty.value = false
    }
    await loadDefinitions()
  } catch (e) {
    console.error('[流程定义] 删除失败', e)
  }
}

// ══════════════════ 节点编排 ══════════════════
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

// ══════════════════ 保存 ══════════════════
// 新建走 POST /definitions；已有定义走 PUT /definitions/{id}（后端会 version+1 并整份重写节点）
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
    const saved = isNew
      ? await approvalFlowApi.create(payload)
      : await approvalFlowApi.update(draft.definitionId, payload)
    if (saved?.definitionId) {
      applyDefinition(saved)
    }
    dirty.value = false
    metaModalVisible.value = false
    message.success(isNew ? '创建成功' : '保存成功（已生成新版本）')
    await loadDefinitions()
  } catch (e) {
    console.error('[流程定义] 保存流程失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(loadDefinitions)
</script>

<style scoped>
/* ═══ 台账视图 ═══ */
/* 外层必须是 flex 纵向容器：CategoryListLayout 占满 PageContainer 剩余高度 */
.ledger-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
/* CategoryListLayout 自带 height:100%，改成 flex 占位，避免撑出页面滚动条 */
.ledger-page :deep(.category-list-layout) {
  flex: 1;
  min-height: 0;
  height: auto;
}

/* 查询区：横向自适应网格（禁止纵向单列） */
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 8px 12px;
}
.search-field-item {
  min-width: 150px;
}
.search-action-group {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 150px;
}

/* BillDetailTable 根元素 flex:1，父级不是 flex 纵向容器时表格高度会塌陷为 0 */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.cell-mono {
  font-family: Consolas, Monaco, monospace;
}
.cell-empty {
  color: #bbb;
}

/* ═══ 设计器视图 ═══ */
.designer-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.designer-tip {
  margin-bottom: 8px;
  flex-shrink: 0;
}
.designer-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  flex-shrink: 0;
}
.designer-toolbar-left,
.designer-toolbar-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.draft-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.canvas-area {
  flex: 1;
  min-height: 0;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
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

/* ═══ 抽屉底部 ═══ */
.node-drawer-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}
</style>
