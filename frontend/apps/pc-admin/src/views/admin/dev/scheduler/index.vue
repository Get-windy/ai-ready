<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        定时任务（开发工具 → 定时任务，菜单 62405）
        · 平台控制台页面：任务只执行「已注册的任务处理器」（JobHandler 白名单 SPI），任务行不携带类名/方法名
        · 表格页 → 走 CategoryListLayout + BillDetailTable（表头齿轮列配置，个人/全局，storage-key 持久化）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/定时任务开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增任务 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            v-permission="PERM.CREATE"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增任务
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              v-permission="PERM.LIST"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（任务名称 / 运行状态 —— 均为后端 /page 真实支持的参数） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('taskName')">
                <span class="search-label">任务名称</span>
                <a-input
                  v-model:value="searchForm.taskName"
                  placeholder="请输入任务名称（模糊匹配）"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">运行状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
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
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <a-alert
              class="table-tip"
              type="info"
              show-icon
              message="任务只执行「已注册的任务处理器」，任务行不携带类名/方法名；需要新目标时由后端实现 JobHandler 注册。"
            >
              <template #description>
                <span>超时升级等改数据的任务，建议先用 <code>{"dryRun": true}</code> 演练（在「执行参数」内配置）。</span>
              </template>
            </a-alert>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-dev-scheduler-table-columns"
              global-config-key="system-dev-scheduler-table-columns"
            >
              <!-- 任务处理器：显示中文名，悬停看 jobKey 原文 -->
              <template #jobKeyCell="{ record, column }">
                <a-tooltip
                  v-if="!record.__ghost"
                  :title="record[column.key]"
                >
                  {{ handlerName(record.jobKey) }}
                </a-tooltip>
              </template>

              <!-- 任务类型 -->
              <template #taskTypeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ TASK_TYPE_MAP[record[column.key]] || record[column.key] || '-' }}</span>
              </template>

              <!-- 启用开关 -->
              <template #enabledCell="{ record }">
                <a-switch
                  v-if="!record.__ghost"
                  :checked="record.enabled === 1"
                  checked-children="启用"
                  un-checked-children="停用"
                  size="small"
                  :loading="togglingId === record.id"
                  @change="(checked: boolean) => toggleTask(record, checked)"
                />
              </template>

              <!-- 运行状态 -->
              <template #statusCell="{ record, column }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record[column.key])"
                >
                  {{ statusText(record[column.key]) }}
                </a-tag>
              </template>

              <!-- 上次执行时间 -->
              <template #lastExecuteTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>

              <!-- 成功/失败次数 -->
              <template #statCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="{ 'text-danger': Number(record.failCount) > 0 }"
                >
                  {{ record.successCount ?? 0 }} / {{ record.failCount ?? 0 }}
                </span>
              </template>

              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-permission="PERM.EXECUTE"
                    type="link"
                    size="small"
                    @click="executeNow(record)"
                  >
                    立即执行
                  </a-button>
                  <a-button
                    v-permission="PERM.LIST"
                    type="link"
                    size="small"
                    @click="showLogs(record)"
                  >
                    执行日志
                  </a-button>
                  <a-button
                    v-permission="PERM.UPDATE"
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此任务?"
                    @confirm="handleDelete(record)"
                  >
                    <a-button
                      v-permission="PERM.DELETE"
                      type="link"
                      size="small"
                      danger
                    >
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新增/编辑任务弹窗 ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="editingTask ? '编辑任务' : '新增任务'"
        :confirm-loading="modalLoading"
        width="620px"
        @ok="handleModalOk"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          style="margin-top: 16px"
        >
          <a-form-item
            label="任务名称"
            required
          >
            <a-input
              v-model:value="modalForm.taskName"
              placeholder="请输入任务名称"
            />
          </a-form-item>
          <a-form-item
            label="任务处理器"
            required
          >
            <a-select
              v-model:value="modalForm.jobKey"
              placeholder="请选择已注册的处理器"
              :options="handlerOptions"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
          <a-form-item
            label="Cron表达式"
            required
          >
            <a-input
              v-model:value="modalForm.cronExpression"
              placeholder="如: 0 */10 * * * ?"
            >
              <template #suffix>
                <a-tooltip title="秒 分 时 日 月 周">
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
            </a-input>
          </a-form-item>
          <a-form-item label="任务类型">
            <a-select
              v-model:value="modalForm.taskType"
              :options="TASK_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="执行参数">
            <a-textarea
              v-model:value="modalForm.executeParams"
              :rows="3"
              placeholder='JSON，原样传给处理器，如 {"dryRun": true}'
            />
          </a-form-item>
          <a-form-item label="任务描述">
            <a-textarea
              v-model:value="modalForm.taskDesc"
              :rows="2"
              placeholder="请输入任务描述"
            />
          </a-form-item>
          <a-form-item label="启用">
            <a-switch
              :checked="modalForm.enabled === 1"
              @change="(checked: boolean) => (modalForm.enabled = checked ? 1 : 0)"
            />
            <span style="margin-left:8px;color:#8c8c8c">停用时只保留配置，不参与调度</span>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 执行日志抽屉 ═══ -->
      <a-drawer
        v-model:open="logVisible"
        :title="`执行日志 — ${logTaskName}`"
        width="880"
        destroy-on-close
      >
        <a-table
          :data-source="logs"
          :columns="logColumns"
          :loading="logLoading"
          row-key="id"
          size="small"
          :pagination="false"
          :scroll="{ x: 840 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'executeStatus'">
              <a-tag :color="logStatusColor(record.executeStatus)">
                {{ logStatusText(record.executeStatus) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'startTime'">
              {{ fmtTime(record.startTime) }}
            </template>
          </template>
        </a-table>
        <div class="log-footer">
          <StandardPagination
            variant="classic"
            :current="logPagination.current"
            :page-size="logPagination.pageSize"
            :total="logPagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handleLogPageChange"
          />
        </div>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'

defineOptions({ name: 'AdminDevScheduler' })

const API_BASE = '/scheduler/task'
const PAGE_CONFIG_STORAGE_KEY = 'system-dev-scheduler-page-config'

/** 后端权限码（ScheduledTaskController 的 @SaCheckPermission 逐端点口径） */
const PERM = {
  LIST: 'system:dev:scheduler:list',
  CREATE: 'system:dev:scheduler:create',
  UPDATE: 'system:dev:scheduler:update',
  DELETE: 'system:dev:scheduler:delete',
  EXECUTE: 'system:dev:scheduler:execute',
}

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 运行状态四态（实体注释只写三态，ERROR 由 TaskExecutor 在调度注册失败时写入，属实际行为） */
const STATUS_OPTIONS = [
  { label: '运行中', value: 'RUNNING' },
  { label: '已停止', value: 'STOPPED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '异常', value: 'ERROR' },
]
const TASK_TYPE_MAP: Record<string, string> = {
  CRON: 'Cron表达式',
  FIXED_RATE: '固定频率',
  FIXED_DELAY: '固定延迟',
}
const TASK_TYPE_OPTIONS = [
  { label: 'CRON（按表达式）', value: 'CRON' },
  { label: 'FIXED_RATE（固定频率）', value: 'FIXED_RATE' },
  { label: 'FIXED_DELAY（固定延迟）', value: 'FIXED_DELAY' },
]

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const handlers = ref<any[]>([])
const togglingId = ref<number | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  taskName: '' as string,
  status: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskName', label: '任务名称', visible: true },
  { key: 'status', label: '运行状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新增任务', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

// 序号列承载表头「列配置」齿轮；操作列为固定列。
// 自定义渲染列一律 type:'slot' + slotName（漏写 slotName 会静默直出原值），插槽内透传的 column 必须取用。
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'taskName', title: '任务名称', type: 'input', width: 180 },
  { key: 'jobKey', title: '任务处理器', type: 'slot', slotName: 'jobKeyCell', width: 170 },
  { key: 'taskType', title: '任务类型', type: 'slot', slotName: 'taskTypeCell', width: 110 },
  { key: 'cronExpression', title: 'Cron表达式', type: 'input', width: 150 },
  { key: 'enabled', title: '启用', type: 'slot', slotName: 'enabledCell', width: 90 },
  { key: 'status', title: '运行状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'lastExecuteTime', title: '上次执行', type: 'slot', slotName: 'lastExecuteTimeCell', width: 160 },
  { key: 'stat', title: '成功/失败', type: 'slot', slotName: 'statCell', width: 110 },
]

const logColumns = [
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '状态', dataIndex: 'executeStatus', key: 'executeStatus', width: 90 },
  { title: '任务名', dataIndex: 'taskName', key: 'taskName', width: 160 },
  { title: '执行结果', dataIndex: 'executeResult', key: 'executeResult', width: 200 },
  { title: '失败原因', dataIndex: 'errorMessage', key: 'errorMessage', width: 200 },
  { title: '耗时(ms)', dataIndex: 'executeTime', key: 'executeTime', width: 90 },
]

// ═══ 弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingTask = ref<any>(null)
const modalForm = reactive<Record<string, any>>({})

// ═══ 执行日志抽屉 ═══
const logVisible = ref(false)
const logLoading = ref(false)
const logTaskName = ref('')
const logTaskId = ref<number | null>(null)
const logs = ref<any[]>([])
const logPagination = reactive({ current: 1, pageSize: 20, total: 0 })

const handlerOptions = computed(() =>
  handlers.value.map((h: any) => ({ label: `${h.name}（${h.key}）`, value: h.key })))

function handlerName(key: string) {
  const hit = handlers.value.find((h: any) => h.key === key)
  return hit ? hit.name : (key || '—')
}

function statusText(status: string) {
  const map: Record<string, string> = { RUNNING: '运行中', STOPPED: '已停止', PAUSED: '已暂停', ERROR: '异常' }
  return map[status] || status || '—'
}
function statusColor(status: string) {
  const map: Record<string, string> = { RUNNING: 'green', STOPPED: 'default', PAUSED: 'orange', ERROR: 'red' }
  return map[status] || 'default'
}
/** 日志状态三色映射：RUNNING 属进行中，不能与 FAILURE 同色 */
function logStatusText(status: string) {
  const map: Record<string, string> = { RUNNING: '执行中', SUCCESS: '成功', FAILURE: '失败' }
  return map[status] || status || '—'
}
function logStatusColor(status: string) {
  const map: Record<string, string> = { RUNNING: 'blue', SUCCESS: 'green', FAILURE: 'red' }
  return map[status] || 'default'
}
function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// ═══ 数据加载 ═══
/**
 * 列表查询：真实分页 + 后端已支持的筛选参数
 * 后端 GET /api/scheduler/task/page 支持 current / size / taskName(模糊) / status(精确)。
 * 注：「任务处理器(jobKey)」「启用(enabled)」后端未提供筛选参数，故不作为查询条件。
 */
async function fetchList() {
  loading.value = true
  try {
    const res: any = await request.get(`${API_BASE}/page`, {
      current: pagination.current,
      size: pagination.pageSize,
      taskName: searchForm.taskName || undefined,
      status: searchForm.status || undefined,
    })
    const records = res?.records || res?.data?.records || []
    tableData.value = Array.isArray(records) ? records : []
    pagination.total = Number(res?.total ?? res?.data?.total) || 0
  } catch (error: any) {
    console.error('[定时任务] 加载列表失败', error)
    message.error(error?.message || error?.response?.data?.message || '加载定时任务列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function fetchHandlers() {
  try {
    const res: any = await request.get(`${API_BASE}/handlers`)
    const list = Array.isArray(res) ? res : (res?.data || [])
    handlers.value = Array.isArray(list) ? list : []
  } catch (error: any) {
    console.error('[定时任务] 加载任务处理器失败', error)
    message.error(error?.message || error?.response?.data?.message || '加载任务处理器失败')
    handlers.value = []
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.taskName = ''
  searchForm.status = undefined
  pagination.current = 1
  fetchList()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 新增 / 编辑 ═══
function resetModalForm() {
  Object.keys(modalForm).forEach(key => delete modalForm[key])
  modalForm.taskType = 'CRON'
  modalForm.enabled = 0
  modalForm.executeParams = ''
}

function openCreate() {
  editingTask.value = null
  resetModalForm()
  modalVisible.value = true
}

function openEdit(record: any) {
  editingTask.value = record
  resetModalForm()
  Object.assign(modalForm, { ...record })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.taskName?.trim()) { message.warning('请输入任务名称'); return }
  if (!modalForm.jobKey) { message.warning('请选择任务处理器'); return }
  if (!modalForm.cronExpression?.trim()) { message.warning('请输入Cron表达式'); return }
  if (modalForm.executeParams?.trim()) {
    try {
      JSON.parse(modalForm.executeParams)
    } catch {
      message.warning('执行参数必须是合法 JSON（留空表示无参数）')
      return
    }
  }

  modalLoading.value = true
  try {
    // executeClass / executeMethod 两列已在迁移中 DROP，载荷不再携带这两个键
    const payload = {
      taskName: modalForm.taskName,
      jobKey: modalForm.jobKey,
      cronExpression: modalForm.cronExpression,
      taskType: modalForm.taskType,
      executeParams: modalForm.executeParams,
      taskDesc: modalForm.taskDesc,
      enabled: modalForm.enabled,
    }
    if (editingTask.value) {
      await request.put(`${API_BASE}/${editingTask.value.id}`, payload)
      message.success('任务已更新')
    } else {
      await request.post(API_BASE, payload)
      message.success('任务已创建')
    }
    modalVisible.value = false
    editingTask.value = null
    fetchList()
  } catch (e: any) {
    console.error('[定时任务] 保存失败', e)
    message.error(e?.message || e?.response?.data?.message || (editingTask.value ? '更新失败' : '创建失败'))
  } finally {
    modalLoading.value = false
  }
}

async function handleDelete(record: any) {
  try {
    await request.delete(`${API_BASE}/${record.id}`)
    message.success('任务已删除')
    // 删除后当前页可能已空，回退一页避免停留在空页
    if (tableData.value.length === 1 && pagination.current > 1) pagination.current -= 1
    fetchList()
  } catch (e: any) {
    console.error('[定时任务] 删除失败', e)
    message.error(e?.message || e?.response?.data?.message || '删除失败')
  }
}

/** 启用/停用：失败时不改动本地行，避免 UI 与后端不一致 */
async function toggleTask(record: any, checked: boolean) {
  const url = checked ? `${API_BASE}/${record.id}/enable` : `${API_BASE}/${record.id}/disable`
  togglingId.value = record.id
  try {
    await request.post(url)
    message.success(checked ? '任务已启用' : '任务已停用')
    await fetchList()
  } catch (e: any) {
    console.error('[定时任务] 启停失败', e)
    message.error(e?.message || e?.response?.data?.message || (checked ? '启用失败' : '停用失败'))
  } finally {
    togglingId.value = null
  }
}

async function executeNow(record: any) {
  try {
    await request.post(`${API_BASE}/${record.id}/execute`)
    message.success('任务已触发，结果见「执行日志」')
    fetchList()
  } catch (e: any) {
    console.error('[定时任务] 触发执行失败', e)
    message.error(e?.message || e?.response?.data?.message || '执行失败')
  }
}

// ═══ 执行日志 ═══
async function showLogs(record: any) {
  logTaskName.value = record.taskName
  logTaskId.value = record.id
  logPagination.current = 1
  logVisible.value = true
  fetchLogs()
}

async function fetchLogs() {
  if (logTaskId.value === null) return
  logLoading.value = true
  try {
    const res: any = await request.get(`${API_BASE}/log/page`, {
      current: logPagination.current,
      size: logPagination.pageSize,
      taskId: logTaskId.value,
    })
    const records = res?.records || res?.data?.records || []
    logs.value = Array.isArray(records) ? records : []
    logPagination.total = Number(res?.total ?? res?.data?.total) || 0
  } catch (error: any) {
    console.error('[定时任务] 加载执行日志失败', error)
    message.error(error?.message || error?.response?.data?.message || '加载执行日志失败')
    logs.value = []
    logPagination.total = 0
  } finally {
    logLoading.value = false
  }
}

function handleLogPageChange(page: number, pageSize: number) {
  logPagination.current = page
  logPagination.pageSize = pageSize
  fetchLogs()
}

function handleError(error: Error) {
  console.error('[定时任务] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchHandlers()
  fetchList()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
/* 提示条不参与表格高度分配（否则会挤压表格可视区） */
.table-tip { flex-shrink: 0; margin-bottom: 8px; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
.text-danger { color: #ff4d4f; }
.log-footer { display: flex; justify-content: flex-end; padding-top: 12px; }
</style>
