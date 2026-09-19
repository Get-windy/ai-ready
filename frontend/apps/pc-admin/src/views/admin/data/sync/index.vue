<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        同步任务（系统管理 → 数据管理 → 同步任务，菜单 62304）
        · 平台控制台页面；后端控制器 cn.aiedge.datasource.controller.SyncTaskController（前缀 /api/data-source/sync）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/同步任务开发文档.md
        · 后端能力（2026-09-19 起为真实投递 + 诚实回执，详见开发文档 §9.3）：
          ① 「立即执行」= 复用 cn.aiedge.integration 的同步引擎通道，把任务投递给 sync-engine
             （POST /api/sync/trigger），投递结论如实写入 lastRunStatus / lastRunMessage / lastRunTime；
          ② 前置条件：该租户在 sync-engine 中必须有**唯一**启用的数据源配置
             （sync_data_source 按 tenant 匹配）。0 条或多条都**明确失败并给出原因**，绝不猜投递目标；
          ③ 🔴 sync-engine 自身的 /api/sync/trigger 只接收请求，真正的数据搬运由该服务的 APScheduler
             调度周期执行，本系统无法回读其结果 —— 所以本页只报「已投递」，**永不报「已同步」**；
          ④ status 是启停开关（「启用/暂停」写它），「执行」不再修改它；在途标记在 lastRunStatus 上，
             且必然复位为终态，不存在「永停 running」。
        · 仍未闭环：cron_expression 无调度消费方（填了不会自动跑）、next_sync_time 无写入点。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增任务 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
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

        <!-- ═══ 查询区（后端 /list 无任何过滤入参 → 三项均为本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('taskName')">
                <span class="search-label">任务名称</span>
                <!-- 后端 /list 只有 page/pageSize → 本地模糊过滤 -->
                <a-input
                  v-model:value="searchForm.taskName"
                  placeholder="请输入任务名称（本地过滤）"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('syncType')">
                <span class="search-label">同步类型</span>
                <!-- 后端不支持该参数 → 本地精确过滤 -->
                <a-select
                  v-model:value="searchForm.syncType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="SYNC_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <!-- 后端不支持该参数 → 本地精确过滤 -->
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
            <!-- 后端能力提示：如实标注「投递」与「已同步」的区别 -->
            <a-alert
              class="honesty-alert"
              type="info"
              show-icon
              message="后端能力（2026-09-19）：「执行」= 把任务投递给同步引擎，不等于数据已同步"
            >
              <template #description>
                <ul class="honesty-list">
                  <li>
                    <b>立即执行</b>：后端复用「数据导入」页的同步引擎通道，向 sync-engine 发出同步请求，
                    并把投递结论写回 <b>lastRunStatus（dispatched / failed）/ lastRunMessage</b>。
                  </li>
                  <li>
                    <b>为什么只报「已投递」</b>：sync-engine 收到请求后只是登记，真正的数据搬运由它自身的
                    调度周期执行；本系统<b>没有回读引擎侧结果的通路</b>，所以界面上不会出现「同步完成」。
                  </li>
                  <li>
                    <b>前置条件</b>：该租户在同步引擎中必须有<b>唯一</b>启用的数据源配置。
                    0 条或多条都会<b>明确报错并写明原因</b>（不会猜一个配置去投递）。
                  </li>
                  <li>
                    <b>状态</b>：「运行中 / 已暂停」是启停开关，由本页「启用 / 暂停」维护；「执行」不再改它。
                  </li>
                  <li>
                    <b>仍未接线</b>：Cron 表达式无调度消费方（填了不会自动执行）、「下次执行」无写入点。
                  </li>
                  <li>
                    本页所有操作按钮都在调用后<b>回读列表核验</b>：核验不到可验证变化时，一律按「未发生」提示。
                  </li>
                </ul>
              </template>
            </a-alert>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-data-sync-table-columns"
              global-config-key="system-data-sync-table-columns"
            >
              <!-- 任务名称 -->
              <template #taskNameCell="{ record }">
                <span>{{ record.taskName || '-' }}</span>
              </template>

              <!-- 源数据源 -->
              <template #sourceIdCell="{ record }">
                {{ dataSourceName(record.sourceId) }}
              </template>

              <!-- 目标数据源 -->
              <template #targetIdCell="{ record }">
                {{ dataSourceName(record.targetId) }}
              </template>

              <!-- 同步类型（原样透传给同步引擎，本系统不做全量/增量的差异化处理） -->
              <template #syncTypeCell="{ record }">
                <a-tooltip :title="record.syncType === 'incremental' ? '该值原样传给同步引擎的 sync_type 参数；本系统侧不区分全量/增量的处理逻辑' : ''">
                  <a-tag :color="record.syncType === 'full' ? 'blue' : record.syncType === 'incremental' ? 'cyan' : 'default'">
                    {{ SYNC_TYPE_MAP[record.syncType] || record.syncType || '-' }}
                  </a-tag>
                </a-tooltip>
              </template>

              <!-- Cron 表达式（无消费方） -->
              <template #cronExpressionCell="{ record }">
                <a-tooltip :title="record.cronExpression ? '后端无任何调度消费方，该表达式不会被执行' : ''">
                  <span v-if="!record.cronExpression" class="cell-empty">-</span>
                  <span v-else>{{ record.cronExpression }}</span>
                </a-tooltip>
              </template>

              <!-- 状态：启停开关（由「启用 / 暂停」维护），不是执行状态 -->
              <template #statusCell="{ record }">
                <a-tooltip :title="statusTip(record.status)">
                  <a-badge
                    :status="STATUS_MAP[record.status]?.badge || 'default'"
                    :text="STATUS_MAP[record.status]?.label || record.status || '-'"
                  />
                </a-tooltip>
              </template>

              <!-- 上次投递（附最近一次「执行」的真实结论：已投递 / 投递失败 / 在途） -->
              <template #lastSyncTimeCell="{ record }">
                <a-tooltip :title="lastRunTip(record)">
                  <span v-if="!record.lastSyncTime && !record.lastRunTime" class="cell-empty">-</span>
                  <span v-else>
                    {{ fmtTime(record.lastSyncTime || record.lastRunTime) }}
                    <a-tag
                      v-if="lastRunMeta(record.lastRunStatus)"
                      :color="lastRunMeta(record.lastRunStatus)?.color"
                      style="margin-left:4px"
                    >
                      {{ lastRunMeta(record.lastRunStatus)?.label }}
                    </a-tag>
                  </span>
                </a-tooltip>
              </template>

              <!-- 下次执行（后端死列，无写入点） -->
              <template #nextSyncTimeCell="{ record }">
                <a-tooltip title="该字段后端无任何写入点（无调度器），恒为空">
                  <span class="cell-empty">{{ record.nextSyncTime ? fmtTime(record.nextSyncTime) : '-' }}</span>
                </a-tooltip>
              </template>

              <!-- 创建时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="0">
                  <a-tooltip title="把同步请求投递给同步引擎（引擎调度周期执行）；「已投递」不等于「数据已同步」">
                    <a-button
                      type="link"
                      size="small"
                      :loading="executingId === record.id"
                      @click="executeTask(record)"
                    >
                      执行
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm
                    :title="record.status === 'running' ? '确定暂停此任务?' : '确定启用此任务?'"
                    @confirm="toggleTask(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      :loading="togglingId === record.id"
                    >
                      {{ record.status === 'running' ? '暂停' : '启用' }}
                    </a-button>
                  </a-popconfirm>
                  <a-button
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
                    <a-button type="link" size="small" danger>
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（本地分页，见 script 注释） ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="totalCount"
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
        storage-key="system-data-sync-page-config"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新增 / 编辑同步任务弹窗 ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="editingId ? '编辑同步任务' : '新增同步任务'"
        width="560px"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleSave"
        @cancel="closeModal"
      >
        <a-alert
          type="warning"
          show-icon
          style="margin-bottom:12px"
          message="这里维护的只是同步台账：Cron 不会被调度、执行也不会搬运数据，配置后不会真的同步。"
        />
        <a-alert
          v-if="!dataSources.length"
          type="info"
          show-icon
          style="margin-bottom:12px"
          message="当前没有可用数据源，源/目标数据源无可选项，请先到「连接管理」创建数据源。"
        />
        <a-form
          ref="formRef"
          :model="modalForm"
          :rules="modalRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item
            label="任务名称"
            name="taskName"
          >
            <a-input
              v-model:value="modalForm.taskName"
              placeholder="请输入任务名称"
            />
          </a-form-item>
          <a-form-item
            label="源数据源"
            name="sourceId"
          >
            <a-select
              v-model:value="modalForm.sourceId"
              placeholder="请选择源数据源"
              :options="dataSourceOptions"
            />
          </a-form-item>
          <a-form-item
            label="目标数据源"
            name="targetId"
          >
            <a-select
              v-model:value="modalForm.targetId"
              placeholder="请选择目标数据源"
              :options="dataSourceOptions"
            />
          </a-form-item>
          <a-form-item
            label="同步方式"
            name="syncType"
          >
            <a-select
              v-model:value="modalForm.syncType"
              :options="SYNC_TYPE_SELECT_OPTIONS"
            />
          </a-form-item>
          <a-form-item
            label="Cron表达式"
            name="cronExpression"
          >
            <a-input
              v-model:value="modalForm.cronExpression"
              placeholder="如: 0 0 2 * * ?（后端不调度，仅存台账）"
            />
          </a-form-item>
          <a-form-item
            label="描述"
            name="description"
          >
            <a-textarea
              v-model:value="modalForm.description"
              :rows="2"
              placeholder="任务描述"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { syncTaskApi, dataSourceApi, type SyncTaskItem, type DataSourceItem } from '@/api/admin'

defineOptions({ name: 'AdminDataSync' })

// ═══ 枚举 ═══
/** 任务状态（与后端 SyncTask.status 一致：running/paused/stopped） */
const STATUS_MAP: Record<string, { label: string; badge: 'default' | 'error' | 'warning' | 'success' | 'processing' }> = {
  running: { label: '运行中', badge: 'processing' },
  paused: { label: '已暂停', badge: 'warning' },
  stopped: { label: '已停止', badge: 'default' },
}
/** 最近一次「立即执行」的投递结论（后端 lastRunStatus） */
const LAST_RUN_MAP: Record<string, { label: string; color: string }> = {
  running: { label: '投递中', color: 'blue' },
  dispatched: { label: '已投递', color: 'cyan' },
  failed: { label: '投递失败', color: 'red' },
}
const STATUS_OPTIONS = [
  { label: '运行中', value: 'running' },
  { label: '已暂停', value: 'paused' },
  { label: '已停止', value: 'stopped' },
]
const SYNC_TYPE_MAP: Record<string, string> = {
  full: '全量同步',
  incremental: '增量同步',
}
const SYNC_TYPE_OPTIONS = [
  { label: '全量同步', value: 'full' },
  { label: '增量同步', value: 'incremental' },
]
const SYNC_TYPE_SELECT_OPTIONS = [
  { label: '全量同步', value: 'full' },
  // 该值原样作为 sync_type 传给同步引擎；本系统侧不做全量/增量的差异化处理
  { label: '增量同步（原样传给引擎）', value: 'incremental' },
]

// ═══ 页面配置（查询条件 / 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskName', label: '任务名称', visible: true },
  { key: 'syncType', label: '同步类型', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增任务', enabled: true },
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

// ═══ 列定义 ═══
// 序号列承载表头「列配置」齿轮；操作列为固定列
// 纯文本列用 type:'input'，所有自定义渲染列必须 type:'slot' + slotName（插槽透传 column）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'taskName', title: '任务名称', type: 'slot', slotName: 'taskNameCell', width: 180 },
  { key: 'sourceId', title: '源数据源', type: 'slot', slotName: 'sourceIdCell', width: 150 },
  { key: 'targetId', title: '目标数据源', type: 'slot', slotName: 'targetIdCell', width: 150 },
  { key: 'syncType', title: '同步方式', type: 'slot', slotName: 'syncTypeCell', width: 120 },
  { key: 'cronExpression', title: 'Cron表达式', type: 'slot', slotName: 'cronExpressionCell', width: 140 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'lastSyncTime', title: '上次执行', type: 'slot', slotName: 'lastSyncTimeCell', width: 160 },
  { key: 'nextSyncTime', title: '下次执行', type: 'slot', slotName: 'nextSyncTimeCell', width: 160 },
  { key: 'description', title: '描述', type: 'input', width: 200 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
]

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const executingId = ref<number | null>(null)
const togglingId = ref<number | null>(null)
const allRows = ref<SyncTaskItem[]>([])
const dataSources = ref<DataSourceItem[]>([])

const pagination = reactive({ current: 1, pageSize: 20 })
const searchForm = reactive({
  taskName: '' as string,
  syncType: undefined as string | undefined,
  status: undefined as string | undefined,
})

const dataSourceOptions = computed(() => dataSources.value.map(ds => ({ label: ds.name, value: ds.id })))

// ═══ 本地过滤（任务名称 / 同步类型 / 状态 —— 后端 /list 只支持 page/pageSize） ═══
const filteredRows = computed(() => {
  const kw = searchForm.taskName.trim().toLowerCase()
  return allRows.value.filter((r) => {
    if (kw && !String(r.taskName || '').toLowerCase().includes(kw)) return false
    if (searchForm.syncType && r.syncType !== searchForm.syncType) return false
    if (searchForm.status && r.status !== searchForm.status) return false
    return true
  })
})
const totalCount = computed(() => filteredRows.value.length)
// 本地分页：后端 list 虽支持 page/pageSize（且自身也是内存切片），但要先做本地过滤再分页，
// 故统一由前端切片，避免「过滤后每页条数不对」。
// 用 ref + watch 而非 computed：BillDetailTable 是 defineModel('dataSource')，
// 传只读 computed 在内部回写时会抛 Vue 只读警告。
const tableData = ref<SyncTaskItem[]>([])
watch([filteredRows, () => pagination.current, () => pagination.pageSize], () => {
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filteredRows.value.slice(start, start + pagination.pageSize)
}, { immediate: true })

// ═══ 格式化 ═══
function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function dataSourceName(id: number | null | undefined): string {
  if (!id) return '-'
  const ds = dataSources.value.find(d => d.id === id)
  return ds ? ds.name : `数据源#${id}`
}
/** 状态列提示：说明这是启停开关而非执行状态 */
function statusTip(status: string | undefined): string {
  if (status === 'running') return '已启用（启停开关）。是否真的在执行，要看「上次执行」列的投递结论'
  if (status === 'paused') return '已暂停（启停开关）。暂停不影响手动「执行」，但表示该任务不应自动运行'
  if (status === 'stopped') return '已停止（新增任务时后端写入的兜底值）'
  return ''
}

/** 投递结论的展示元数据（未知值如实原样展示，不编造语义） */
function lastRunMeta(status: string | undefined | null): { label: string; color: string } | null {
  if (!status) return null
  return LAST_RUN_MAP[status] || { label: status, color: 'default' }
}

/** 最近一次「执行」的结论提示：只描述投递结果，不暗示数据已同步 */
function lastRunTip(record: SyncTaskItem): string {
  if (!record.lastRunStatus && !record.lastSyncTime) return '该任务从未执行过'
  const parts: string[] = []
  if (record.lastSyncTime) parts.push(`上次发起投递：${fmtTime(record.lastSyncTime)}`)
  if (record.lastRunTime) parts.push(`最近执行时间：${fmtTime(record.lastRunTime)}`)
  const meta = lastRunMeta(record.lastRunStatus)
  if (meta) parts.push(`结论：${meta.label}`)
  if (record.lastRunMessage) parts.push(record.lastRunMessage)
  return parts.join('；')
}

// ═══ 数据加载 ═══
/**
 * 拉取同步任务台账。
 * 后端 /list 只支持 page/pageSize，所有查询条件都在前端过滤，
 * 因此需要「全量」行数据 —— 用翻页循环取完，避免单次 pageSize=1000 的静默截断
 * （单次请求 records.length 不足 pageSize 或已达 total 即停止；最多 20 页兜底防止死循环）。
 */
async function fetchAllRows(): Promise<SyncTaskItem[]> {
  const PAGE_SIZE = 200
  const MAX_PAGES = 20
  const rows: SyncTaskItem[] = []
  for (let page = 1; page <= MAX_PAGES; page++) {
    const res: any = await syncTaskApi.page({ page, pageSize: PAGE_SIZE })
    const records: SyncTaskItem[] = res?.records || []
    rows.push(...records)
    const total = Number(res?.total) || 0
    if (records.length < PAGE_SIZE || rows.length >= total) break
  }
  return rows
}

async function fetchList() {
  loading.value = true
  try {
    allRows.value = await fetchAllRows()
    clampPage()
  } catch (error: any) {
    // 禁止假数据兜底：出错即清空并显式提示
    console.error('[同步任务] 加载任务列表失败', error)
    message.error(error?.message || '加载任务列表失败')
    allRows.value = []
  } finally {
    loading.value = false
  }
}

/** 过滤/删除后当前页可能越界，收敛到最后一页 */
function clampPage() {
  const maxPage = Math.max(1, Math.ceil(totalCount.value / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
}

async function fetchDataSources() {
  try {
    const res: any = await dataSourceApi.page({ page: 1, pageSize: 200 })
    dataSources.value = res?.records || []
  } catch (error: any) {
    console.error('[同步任务] 加载数据源列表失败', error)
    message.error(error?.message || '加载数据源列表失败')
    dataSources.value = []
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.taskName = ''
  searchForm.syncType = undefined
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
}

// ═══ 新增 / 编辑弹窗 ═══
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const emptyForm = () => ({
  taskName: '' as string,
  sourceId: undefined as number | undefined,
  targetId: undefined as number | undefined,
  syncType: 'incremental' as string,
  cronExpression: '' as string,
  description: '' as string,
})
const modalForm = reactive(emptyForm())
const modalRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  targetId: [
    { required: true, message: '请选择目标数据源', trigger: 'change' },
    {
      // 前后端均未校验「源 ≠ 目标」，这里补一条（否则是一条无意义的空任务）
      validator: (_rule: any, value: number) => {
        if (value && value === modalForm.sourceId) return Promise.reject(new Error('目标数据源不能与源数据源相同'))
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
}

function openCreate() {
  editingId.value = null
  Object.assign(modalForm, emptyForm())
  modalVisible.value = true
}

function openEdit(record: SyncTaskItem) {
  editingId.value = record.id
  Object.assign(modalForm, emptyForm(), {
    taskName: record.taskName,
    sourceId: record.sourceId,
    targetId: record.targetId,
    syncType: record.syncType || 'incremental',
    cronExpression: record.cronExpression || '',
    description: record.description || '',
  })
  modalVisible.value = true
}

function closeModal() {
  modalVisible.value = false
  editingId.value = null
  Object.assign(modalForm, emptyForm())
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      taskName: modalForm.taskName,
      sourceId: modalForm.sourceId,
      targetId: modalForm.targetId,
      syncType: modalForm.syncType,
      cronExpression: modalForm.cronExpression || undefined,
      description: modalForm.description || undefined,
    }
    if (editingId.value) {
      const editingRecordId = editingId.value
      await syncTaskApi.update(editingRecordId, payload)
      // 🔴 回读核验：确认台账字段真的更新了
      await fetchList()
      const after = allRows.value.find(t => t.id === editingRecordId)
      if (after && after.taskName === payload.taskName && after.syncType === payload.syncType) {
        message.success('同步任务已更新（仅台账字段，不代表同步能力变化）')
        closeModal()
      } else {
        message.error('接口返回成功，但回读未确认到更新内容，请刷新后重试')
      }
    } else {
      const created: any = await syncTaskApi.create(payload)
      const createdId = created?.id
      // 🔴 回读核验：确认新任务真的落库
      await fetchList()
      const hit = createdId ? allRows.value.find(t => t.id === createdId) : undefined
      if (hit) {
        message.success('同步任务已创建（仅登记台账，不会被调度执行）')
        closeModal()
      } else {
        message.error('接口返回成功，但回读列表未找到该任务，请刷新后重试')
      }
    }
  } catch (error: any) {
    // 失败不做任何乐观假设：保留已加载数据仅提示错误（禁止假数据兜底）
    console.error('[同步任务] 保存任务失败', error)
    message.error(error?.message || '保存同步任务失败')
  } finally {
    saving.value = false
  }
}

// ═══ 立即执行（真实投递给同步引擎；回读核验后才给结论） ═══
async function executeTask(record: SyncTaskItem) {
  const beforeRun = { status: record.lastRunStatus, time: record.lastRunTime, sync: record.lastSyncTime }
  executingId.value = record.id
  try {
    await syncTaskApi.execute(record.id)
    // 🔴 回读核验：只有 lastRunStatus / lastRunTime / lastSyncTime 真的变化，才认「调用产生了可验证的变化」
    await fetchList()
    const after = allRows.value.find(t => t.id === record.id)
    if (!after) {
      message.error('回读失败：该任务已不存在')
      return
    }
    const changed = after.lastRunStatus !== beforeRun.status
      || after.lastRunTime !== beforeRun.time
      || after.lastSyncTime !== beforeRun.sync
    if (!changed) {
      message.error('接口未报错，但回读未发现执行结论被写入台账，请刷新后重试')
      return
    }
    // 结论只认台账：dispatched 只代表「已投递」，绝不写成「同步完成」
    if (after.lastRunStatus === 'dispatched') {
      message.warning(`同步请求已投递给同步引擎（${after.lastRunMessage || '已受理'}）。这不是「数据已同步完成」——搬运由引擎调度周期执行，本系统无法回读结果。`)
    } else if (after.lastRunStatus === 'failed') {
      message.error(`投递失败，未发生任何数据搬运：${after.lastRunMessage || '后端未记录原因'}`)
    } else {
      message.warning(`执行结论为「${after.lastRunStatus}」，请刷新复核`)
    }
  } catch (error: any) {
    // 后端拒绝（引擎未启动 / 无唯一启用配置 / 任务在途）会以 success=false 返回，这里如实展示原因
    console.error('[同步任务] 触发执行失败', error)
    await fetchList()
    message.error(error?.message || '触发执行失败')
  } finally {
    executingId.value = null
  }
}

// ═══ 暂停 / 启用（真实的台账状态切换，回读核验） ═══
async function toggleTask(record: SyncTaskItem) {
  const nextStatus = record.status === 'running' ? 'paused' : 'running'
  const nextLabel = nextStatus === 'running' ? '启用' : '暂停'
  togglingId.value = record.id
  try {
    // 后端为 null-safe 部分更新，只提交 status 即可，避免整行覆盖带来副作用
    await syncTaskApi.update(record.id, { status: nextStatus })
    // 🔴 回读核验：状态真的切成目标值才提示成功
    await fetchList()
    const after = allRows.value.find(t => t.id === record.id)
    if (after && after.status === nextStatus) {
      message.success(`任务已${nextLabel}（仅台账状态，无调度器消费）`)
    } else {
      message.error(`接口返回成功，但回读状态未变为「${nextLabel}」，请刷新后重试`)
    }
  } catch (error: any) {
    console.error('[同步任务] 切换任务状态失败', error)
    message.error(error?.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

// ═══ 删除（回读确认任务已消失后才提示成功） ═══
async function handleDelete(record: SyncTaskItem) {
  try {
    await syncTaskApi.remove(record.id)
    await fetchList()
    if (allRows.value.some(t => t.id === record.id)) {
      message.error('删除接口已返回，但回读列表仍存在该任务，请刷新确认')
      return
    }
    message.success('同步任务已删除')
  } catch (error: any) {
    console.error('[同步任务] 删除任务失败', error)
    message.error(error?.message || '删除同步任务失败')
  }
}

function handleError(error: Error) {
  console.error('[同步任务] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  fetchDataSources()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
/* 诚实性提示条：常驻表格上方，不参与滚动 */
.honesty-alert { flex-shrink: 0; margin-bottom: 8px; }
.honesty-list { margin: 0; padding-left: 18px; font-size: 12px; line-height: 20px; }
.cell-empty { color: #999; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
