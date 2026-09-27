<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        流程实例监控台（设置 → 审批 → 流程实例，菜单 802 / workflow-instance，单入口）
        · 定位：管理视角的运行监控台（全租户实例）。发起审批 = 803 我的待办，处理任务 = 804 我的已办
        · 对标：ql361 无工作流域（其「待审批单据」是只读单据台账），本页按业界监控台建模
          （SAP 工作项收件箱 / Flowable·Camunda 实例管理：台账 + 只读详情 + 流程图 + 管理员干预）
        · 规格书：docs/Yh-Spec/手动整理对标开发文档/设置模块/流程实例开发文档.md
        · 列配置齿轮在数据表表头 rowNo 列（个人 / 全局同 key）；页面配置只管查询条件 + 功能按钮
        · 统计卡走服务端 /instance/stat 全量聚合（与列表同筛选条件），禁止用当页条数冒充全量
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：刷新 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading || statLoading"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：自动刷新倒计时 + 最后更新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tag
              v-if="hasError"
              color="error"
            >
              列表加载失败
            </a-tag>
            <span
              v-if="autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('processName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.processName"
                  placeholder="流程名称"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('status')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="流程状态"
                  size="small"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('startTime')"
                class="search-field-item search-range"
              >
                <a-range-picker
                  v-model:value="searchForm.dateRange"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
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

        <!-- ═══ 统计卡（服务端全量聚合）+ 数据表 ═══ -->
        <template #table>
          <div class="stat-cards">
            <div class="stat-card stat-total">
              <div class="stat-card-body">
                <div class="stat-card-value">
                  {{ stat.total }}
                </div>
                <div class="stat-card-label">
                  实例总数
                </div>
              </div>
              <BranchesOutlined class="stat-card-icon" />
            </div>
            <div class="stat-card stat-running">
              <div class="stat-card-body">
                <div class="stat-card-value">
                  {{ stat.running }}
                </div>
                <div class="stat-card-label">
                  运行中
                </div>
              </div>
              <LoadingOutlined class="stat-card-icon" />
            </div>
            <!-- 已挂起：可恢复的中间态（与「已终止」不同，是独立的第 5 张卡；
                 后端 /instance/stat 已按同一筛选条件返回 suspended 全量计数） -->
            <a-tooltip
              title="已挂起为可恢复的中间态，不计入「运行中」也不计入「已终止」"
              placement="bottom"
            >
              <div class="stat-card stat-suspended">
                <div class="stat-card-body">
                  <div class="stat-card-value">
                    {{ stat.suspended }}
                  </div>
                  <div class="stat-card-label">
                    已挂起
                  </div>
                </div>
                <PauseCircleOutlined class="stat-card-icon" />
              </div>
            </a-tooltip>
            <div class="stat-card stat-completed">
              <div class="stat-card-body">
                <div class="stat-card-value">
                  {{ stat.completed }}
                </div>
                <div class="stat-card-label">
                  已完成
                </div>
              </div>
              <CheckCircleOutlined class="stat-card-icon" />
            </div>
            <a-tooltip
              title="仅统计「已终止」实例，不含「已挂起」（已挂起为可恢复的中间态）"
              placement="bottom"
            >
              <div class="stat-card stat-terminated">
                <div class="stat-card-body">
                  <div class="stat-card-value">
                    {{ stat.terminated }}
                  </div>
                  <div class="stat-card-label">
                    已终止
                  </div>
                </div>
                <StopOutlined class="stat-card-icon" />
              </div>
            </a-tooltip>
          </div>

          <div
            ref="tableWrap"
            class="table-area"
          >
            <BillTableList
              :columns="columns"
              :data-source="tableRows"
              :loading="loading"
              :pagination="false"
              row-key="instanceId"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
              :min-empty-rows="12"
              storage-key="workflow-instance-table-columns"
              global-config-key="workflow-instance-table-columns"
            >
              <!-- 状态：中文 tag（7 值词表 + 监控词表共用一份映射） -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>

              <!-- 行内操作：查看详情 / 流程图 / 流程干预（按真实落库权限码门控） -->
              <template #actionCell="{ record }">
                <a-space :size="0">
                  <a-button
                    v-permission="'workflow:instance:view'"
                    type="link"
                    size="small"
                    @click="openDetail(record)"
                  >
                    查看详情
                  </a-button>
                  <a-button
                    v-permission="'workflow:instance:diagram'"
                    type="link"
                    size="small"
                    @click="openDiagram(record)"
                  >
                    流程图
                  </a-button>
                  <a-dropdown v-if="canIntervene(record)">
                    <a-button
                      v-permission="'workflow:instance:intervene'"
                      type="link"
                      size="small"
                    >
                      流程干预 <DownOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e: any) => openIntervene(e.key as string, record)">
                        <a-menu-item
                          v-for="item in interveneActions(record)"
                          :key="item.key"
                        >
                          {{ item.label }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 + 功能按钮）——放布局外 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :hide-print-config="true"
      storage-key="workflow-instance-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 详情抽屉（只读：基本信息 + 审批记录时间线） ═══ -->
    <a-drawer
      v-model:open="detailVisible"
      title="流程实例详情"
      placement="right"
      :width="720"
      :footer="null"
    >
      <template #extra>
        <a-button
          size="small"
          :loading="detailLoading"
          :disabled="!detailId"
          @click="fetchDetail(detailId)"
        >
          <ReloadOutlined /> 刷新
        </a-button>
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detailData">
          <a-descriptions
            title="基本信息"
            bordered
            :column="2"
            size="small"
          >
            <a-descriptions-item label="实例ID">
              {{ detailData.instanceId }}
            </a-descriptions-item>
            <a-descriptions-item label="流程名称">
              {{ detailData.processName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(detailData.status)">
                {{ getStatusLabel(detailData.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="发起人">
              {{ detailData.initiator || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="开始时间">
              {{ detailData.startTime || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="结束时间">
              {{ detailData.endTime || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="当前节点">
              {{ detailData.currentNode || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="审批结果">
              {{ resultText(detailData.result) }}
            </a-descriptions-item>
            <a-descriptions-item
              label="业务类型"
              :span="2"
            >
              {{ detailData.businessType || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 审批记录时间线：含「流程干预」留痕（action=intervene，理由即 comment） -->
          <div class="detail-block">
            <div class="detail-block-title">
              审批记录
            </div>
            <a-timeline v-if="detailRecords.length > 0">
              <a-timeline-item
                v-for="(item, idx) in detailRecords"
                :key="idx"
                :color="recordColor(item.action)"
              >
                <div class="record-line">
                  <span class="record-node">{{ item.nodeName || '流程' }}</span>
                  <a-tag :color="recordTagColor(item.action)">
                    {{ recordActionLabel(item.action) }}
                  </a-tag>
                  <span class="record-approver">{{ item.approverName || '-' }}</span>
                  <span class="record-time">{{ item.time || '-' }}</span>
                </div>
                <div
                  v-if="item.comment"
                  class="record-comment"
                >
                  {{ item.comment }}
                </div>
              </a-timeline-item>
            </a-timeline>
            <a-empty
              v-else
              description="暂无审批记录"
            />
          </div>

          <div class="detail-block">
            <div class="detail-block-title">
              业务数据
            </div>
            <pre class="business-data">{{ detailData.businessData || '无' }}</pre>
          </div>
        </template>
        <a-result
          v-else-if="detailError"
          status="warning"
          title="加载失败"
          :sub-title="detailError"
        >
          <template #extra>
            <a-button
              type="primary"
              size="small"
              @click="fetchDetail(detailId)"
            >
              重试
            </a-button>
          </template>
        </a-result>
      </a-spin>
    </a-drawer>

    <!-- ═══ 流程图弹窗（后端现场拼 SVG，非 BPMN） ═══ -->
    <a-modal
      v-model:open="diagramVisible"
      title="流程图"
      :width="1000"
      :footer="null"
    >
      <div class="flow-chart-container">
        <a-spin
          :spinning="diagramLoading"
          tip="流程图加载中..."
        >
          <img
            v-if="diagramImage"
            :src="diagramImage"
            alt="流程图"
            class="flow-chart-image"
          >
          <a-empty
            v-else-if="!diagramLoading"
            :description="diagramError || '暂无可用的流程图'"
          />
        </a-spin>
      </div>
    </a-modal>

    <!-- ═══ 流程干预弹窗（必填理由：干预是强审计动作） ═══ -->
    <a-modal
      v-model:open="interveneVisible"
      title="流程干预"
      :width="480"
      :confirm-loading="interveneLoading"
      ok-text="确认"
      @ok="submitIntervene"
    >
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        size="small"
      >
        <a-form-item label="流程实例">
          <span>{{ interveneTarget?.processName || '-' }}</span>
        </a-form-item>
        <a-form-item label="干预动作">
          <span class="intervene-action">{{ interveneLabel }}</span>
        </a-form-item>
        <a-form-item
          label="干预理由"
          required
        >
          <a-textarea
            v-model:value="interveneReason"
            :rows="3"
            :maxlength="200"
            show-count
            placeholder="请填写干预理由（落审批记录，供审计追溯）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownOutlined, BranchesOutlined, LoadingOutlined, CheckCircleOutlined,
  StopOutlined, PauseCircleOutlined, SyncOutlined, ReloadOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { usePermission } from '@/composables/usePermission'
import { useRowDblclick } from '@/composables/useRowDblclick'
import {
  workflowInstanceApi,
  type WorkflowInstanceMonitorRow,
  type WorkflowInstanceDetail,
  type WorkflowInstanceStat,
  type WorkflowApprovalRecord,
} from '@/api/workflow'

defineOptions({ name: 'WorkflowInstanceMonitor' })

const { checkPermission } = usePermission()

// ═══ 状态词表（列表渲染 / 筛选下拉 / 详情共用同一份映射） ═══
//
// 后端存在**两套词表**（WorkflowConverter）：
//   · 规范值 approving / approved / rejected / withdrawn / cancelled / suspended / terminated（筛选入参 + canonicalStatus）
//   · 监控值 running / completed / ...（列表返回，由 toMonitorStatus 映射）
// 旧实现前端只认 4 个监控值 → 已驳回 / 已撤回 / 已取消 既无法筛选、也会原样显示英文。
// 这里把「两套键」都指向同一份中文标签，列表与下拉都不会再露英文。
const INSTANCE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  // 监控词表（列表返回）
  running: { label: '运行中', color: 'success' },
  completed: { label: '已完成', color: 'default' },
  // 规范词表（筛选入参 / 详情 canonicalStatus），其中 approving/approved 与监控值同义
  approving: { label: '运行中', color: 'success' },
  approved: { label: '已完成', color: 'default' },
  rejected: { label: '已驳回', color: 'error' },
  withdrawn: { label: '已撤回', color: 'orange' },
  cancelled: { label: '已取消', color: 'default' },
  suspended: { label: '已挂起', color: 'warning' },
  terminated: { label: '已终止', color: 'error' },
}

/** 筛选下拉的 7 个规范值（顺序与后端枚举 0→6 一致） */
const STATUS_FILTER_KEYS = ['approving', 'approved', 'rejected', 'withdrawn', 'cancelled', 'suspended', 'terminated']
const STATUS_OPTIONS = STATUS_FILTER_KEYS.map(key => ({ label: INSTANCE_STATUS_MAP[key].label, value: key }))

function getStatusLabel(status?: string): string {
  if (!status) return '-'
  return INSTANCE_STATUS_MAP[status]?.label || status
}

function getStatusColor(status?: string): string {
  if (!status) return 'default'
  return INSTANCE_STATUS_MAP[status]?.color || 'default'
}

/** 审批记录的动作词表（WorkflowConverter.taskActionToString） */
const RECORD_ACTION_MAP: Record<string, { label: string; color: string }> = {
  submit: { label: '提交申请', color: 'blue' },
  approve: { label: '同意', color: 'green' },
  reject: { label: '驳回', color: 'red' },
  transfer: { label: '转交', color: 'orange' },
  withdraw: { label: '撤回', color: 'orange' },
  cancel: { label: '取消', color: 'default' },
  intervene: { label: '流程干预', color: 'purple' },
  // 8-退回：与前端的 TASK_ACTION_TEXT_MAP 同口径；缺这一项时时间线会渲染英文原文 "return"
  return: { label: '退回', color: 'orange' },
  pending: { label: '待处理', color: 'gray' },
}

function recordActionLabel(action?: string): string {
  return RECORD_ACTION_MAP[action || '']?.label || action || '-'
}

function recordTagColor(action?: string): string {
  return RECORD_ACTION_MAP[action || '']?.color || 'default'
}

/** 时间线圆点颜色（a-timeline 只认 blue/red/green/gray 等预设） */
function recordColor(action?: string): string {
  const map: Record<string, string> = {
    submit: 'blue', approve: 'green', reject: 'red', transfer: 'blue',
    withdraw: 'gray', cancel: 'gray', intervene: 'red', pending: 'gray',
  }
  return map[action || ''] || 'gray'
}

/** 审批结果（workflow_instance.result：0-通过 1-驳回） */
function resultText(result?: number | null): string {
  if (result === 0) return '通过'
  if (result === 1) return '驳回'
  return '-'
}

// ═══ 状态 ═══
const loading = ref(false)
const statLoading = ref(false)
const tableRows = ref<WorkflowInstanceMonitorRow[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const hasError = ref(false)

/** 统计卡（服务端全量聚合；缺省全 0，避免显示 undefined） */
const stat = ref<WorkflowInstanceStat>({
  total: 0, running: 0, completed: 0, rejected: 0,
  withdrawn: 0, cancelled: 0, suspended: 0, terminated: 0,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 查询条件 ═══
const searchForm = reactive({
  processName: '',
  status: undefined as string | undefined,
  dateRange: [] as any,
})

// ═══ 横向查询网格（动作组自动跨列） ═══
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PAGE_CONFIG_KEY = 'workflow-instance-page-config'

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'processName', label: '流程名称', visible: true },
  { key: 'status', label: '流程状态', visible: true },
  { key: 'startTime', label: '开始时间', visible: true },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面向面板传的是「当前配置」，必须另给出厂值，否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))
const showPageConfig = ref(false)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldsConfig.value = QUERY_FIELDS.map(def => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === def.key)
        return saved ? { ...def, visible: saved.visible !== false } : { ...def }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(def => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === def.key)
        return saved ? { ...def, enabled: saved.enabled !== false } : { ...def }
      })
    }
  } catch { /* 配置损坏时回落出厂值 */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 表格列（rowNo 承载表头齿轮列配置；操作列固定右侧） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 220, fixed: 'right' },
  { title: '实例ID', field: 'instanceId', key: 'instanceId', width: 180 },
  { title: '流程名称', field: 'processName', key: 'processName', width: 160, sortable: true },
  { title: '状态', field: 'status', key: 'status', width: 100, type: 'slot', slotName: 'statusCell' },
  { title: '当前节点', field: 'currentNode', key: 'currentNode', width: 130 },
  { title: '开始时间', field: 'startTime', key: 'startTime', width: 170 },
  { title: '结束时间', field: 'endTime', key: 'endTime', width: 170 },
  { title: '耗时', field: 'duration', key: 'duration', width: 100 },
  { title: '发起人', field: 'initiator', key: 'initiator', width: 110, defaultHidden: true },
]

// ═══ 筛选参数（列表与统计卡共用，保证口径一致） ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.processName) params.processName = searchForm.processName
  if (searchForm.status) params.status = searchForm.status
  const range = searchForm.dateRange
  if (Array.isArray(range) && range.length === 2) {
    params.startDate = range[0]
    params.endDate = range[1]
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await workflowInstanceApi.page({
      ...buildParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableRows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[流程实例] 加载列表失败', error)
    hasError.value = true
    tableRows.value = []
    pagination.total = 0
    message.error(error?.response?.data?.message || error?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

/** 统计卡：服务端全量聚合（不是当页条数），筛选条件与列表完全一致 */
async function fetchStat() {
  statLoading.value = true
  try {
    const res: any = await workflowInstanceApi.stat(buildParams())
    stat.value = {
      total: Number(res?.total) || 0,
      running: Number(res?.running) || 0,
      completed: Number(res?.completed) || 0,
      rejected: Number(res?.rejected) || 0,
      withdrawn: Number(res?.withdrawn) || 0,
      cancelled: Number(res?.cancelled) || 0,
      suspended: Number(res?.suspended) || 0,
      terminated: Number(res?.terminated) || 0,
    }
  } catch (error: any) {
    console.warn('[流程实例] 统计加载失败', error)
    // 统计失败不清空列表，仅提示（列表本身仍可用）
    stat.value = { ...stat.value, total: pagination.total }
  } finally {
    statLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([fetchList(), fetchStat()])
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  searchForm.processName = ''
  searchForm.status = undefined
  searchForm.dateRange = []
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  refreshAll()
}

// ═══ 详情抽屉（含审批记录时间线） ═══
const detailVisible = ref(false)
const detailId = ref<string>('')
const detailData = ref<WorkflowInstanceDetail | null>(null)
const detailRecords = ref<WorkflowApprovalRecord[]>([])
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

function openDetail(record: WorkflowInstanceMonitorRow) {
  detailId.value = String(record.instanceId)
  detailVisible.value = true
  fetchDetail(detailId.value)
}

/**
 * 双击行打开详情 —— **页面侧自行实现**（不再依赖共享表格组件派发事件）
 *
 * 口径与「查看详情」按钮完全一致：同一权限门控 + 同一个详情抽屉，不新增任何数据请求路径。
 * 行标识由 `data-row-key`（= row-key 指定的 instanceId）反查得到；占位空行不带该属性，
 * 行内按钮/下拉等交互控件上的双击由 composable 统一过滤，不会与既有交互抢事件。
 */
const tableWrap = ref<HTMLElement | null>(null)
useRowDblclick(tableWrap, () => tableRows.value, (record) => {
  if (!record?.instanceId) return
  if (!checkPermission('workflow:instance:view')) return
  openDetail(record)
}, 'instanceId')

async function fetchDetail(instanceId: string) {
  if (!instanceId) return
  detailLoading.value = true
  detailError.value = null
  try {
    const res: any = await workflowInstanceApi.getDetail(instanceId)
    detailData.value = res
    detailRecords.value = res?.records || []
    if (!Array.isArray(res?.records)) {
      // 兜底：详情未内嵌审批记录时走独立端点（GET /workflow/{instanceId}/records，真实存在）
      detailRecords.value = (await workflowInstanceApi.getRecords(instanceId)) || []
    }
  } catch (error: any) {
    console.warn('[流程实例] 加载详情失败', error)
    detailError.value = error?.response?.data?.message || error?.message || '获取流程实例详情失败'
    detailData.value = null
    detailRecords.value = []
  } finally {
    detailLoading.value = false
  }
}

// ═══ 流程图（后端字符串拼接 SVG；前端 base64 内联） ═══
const diagramVisible = ref(false)
const diagramLoading = ref(false)
const diagramImage = ref<string | null>(null)
const diagramError = ref<string | null>(null)

async function openDiagram(record: WorkflowInstanceMonitorRow) {
  diagramVisible.value = true
  diagramLoading.value = true
  diagramError.value = null
  diagramImage.value = null
  try {
    const res: any = await workflowInstanceApi.getDiagram(String(record.instanceId))
    if (res?.svg) {
      diagramImage.value = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(res.svg)))
    } else if (res?.imageUrl) {
      diagramImage.value = res.imageUrl
    } else {
      diagramError.value = '暂无可用的流程图'
    }
  } catch (error: any) {
    console.warn('[流程实例] 加载流程图失败', error)
    diagramError.value = error?.response?.data?.message || '流程图加载失败'
  } finally {
    diagramLoading.value = false
  }
}

// ═══ 流程干预（按当前状态给出合法动作集 + 必填理由） ═══
//
// 旧实现只在 running 行渲染下拉且固定给 3 个动作，导致两个真实缺陷（见开发文档 §5.2）：
//   ① running 实例上「恢复」目标状态 == 当前状态 → 后端迁移表放行但什么都没变（假成功）；
//   ② 真正需要「恢复」的 suspended 实例反而没有入口。
// 现按 canonicalStatus 渲染**合法动作集**：approving → 挂起/终止；suspended → 恢复/终止。
const INTERVENE_ACTIONS: Record<string, { key: string; label: string }[]> = {
  approving: [
    { key: 'suspend', label: '挂起流程' },
    { key: 'terminate', label: '终止流程' },
  ],
  suspended: [
    { key: 'resume', label: '恢复流程' },
    { key: 'terminate', label: '终止流程' },
  ],
}

function interveneActions(record: WorkflowInstanceMonitorRow) {
  const status = record.canonicalStatus || record.status
  return INTERVENE_ACTIONS[status] || []
}

/** 干预下拉的渲染条件：有该权限 + 当前状态存在合法动作（避免渲染出恒空的死下拉） */
function canIntervene(record: WorkflowInstanceMonitorRow): boolean {
  return checkPermission('workflow:instance:intervene') && interveneActions(record).length > 0
}

const interveneVisible = ref(false)
const interveneLoading = ref(false)
const interveneReason = ref('')
const interveneAction = ref<string>('')
const interveneTarget = ref<WorkflowInstanceMonitorRow | null>(null)

const INTERVENE_LABELS: Record<string, string> = {
  terminate: '终止流程',
  suspend: '挂起流程',
  resume: '恢复流程',
}

const interveneLabel = computed(() => INTERVENE_LABELS[interveneAction.value] || '-')

function openIntervene(action: string, record: WorkflowInstanceMonitorRow) {
  interveneAction.value = action
  interveneTarget.value = record
  interveneReason.value = ''
  interveneVisible.value = true
}

async function submitIntervene() {
  if (!interveneReason.value.trim()) {
    message.warning('请填写干预理由')
    return
  }
  const record = interveneTarget.value
  if (!record) return
  interveneLoading.value = true
  try {
    const res: any = await workflowInstanceApi.intervene(
      String(record.instanceId), interveneAction.value as 'terminate' | 'suspend' | 'resume', interveneReason.value.trim()
    )
    if (res?.success === false) {
      // 不再吞掉后端原因（旧实现只显示「X失败」）
      message.error(res?.message || `${interveneLabel.value}失败`)
      return
    }
    message.success(`${interveneLabel.value}成功`)
    interveneVisible.value = false
    refreshAll()
  } catch (error: any) {
    console.error('[流程实例] 流程干预失败', error)
    message.error(error?.response?.data?.message || error?.message || `${interveneLabel.value}失败`)
  } finally {
    interveneLoading.value = false
  }
}

// ═══ 自动刷新（30 秒轮询 + 1 秒倒计时）与快捷键（F5 / Ctrl+R） ═══
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', refreshAll)()
  }
}

function handleError(error: Error) {
  console.error('[流程实例] 页面错误', error)
  hasError.value = true
}

onMounted(async () => {
  loadPageConfig()
  await nextTick()
  refreshAll()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    refreshAll()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ refreshAll })
</script>

<style scoped>
/* ── 查询区：横向自适应网格（禁止纵向单列） ── */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-range { grid-column: span 2; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }

/* ── 统计卡（服务端全量口径；5 张卡等宽栅格，换行也不会错位） ── */
.stat-cards { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; padding: 12px 16px 0; flex-shrink: 0; }
.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  min-width: 0;
}
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-running { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-completed { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-terminated { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
/* 已挂起：黄色系（与「已终止」的橙色区分开，语义上对应状态 tag 的 warning） */
.stat-suspended { background: linear-gradient(135deg, #feffe6 0%, #ffffb8 100%); }
.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}
.stat-card-label { font-size: 12px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 28px; color: rgba(0, 0, 0, 0.15); }

/* ⚠️ 必须是 flex 纵向容器：BillTableList 根元素为 flex:1，父级非 flex 时表格高度会塌陷 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* ── 页头/工具栏的小字提示 ── */
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}
.update-time { font-size: 12px; color: #999; }

/* ── 详情抽屉 ── */
.detail-block { margin-top: 20px; }
.detail-block-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.record-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.record-node { font-weight: 600; color: #303133; }
.record-approver { color: #606266; }
.record-time { color: #999; font-size: 12px; }
.record-comment { color: #606266; margin-top: 4px; word-break: break-all; }
.business-data {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.intervene-action { font-weight: 600; color: #fa8c16; }

/* ── 流程图 ── */
.flow-chart-container { min-height: 200px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 4px; }
.flow-chart-image { max-width: 100%; max-height: 520px; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }

/* ── 响应式（栅格列数递减，卡片始终等宽对齐） ── */
@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
