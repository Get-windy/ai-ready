<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        清理规则（系统 → 数据管理 → 清理规则，菜单 62305）
        · 平台控制台页面：ql361 无对标 → 按 PostgreSQL 分区/DROP + WAL 保留策略建模
        · 后端 cn.aiedge.datasource.controller.CleanupRuleController（前缀 /api/data-source/cleanup）
        · 后端能力（2026-09-19 起为真实执行，详见开发文档 §9.3）：
          ① 「立即执行」= 真实按 targetTable / conditionColumn / retentionDays 分批 DELETE
             （每批 1000 行）并受单次上限保护（默认 5000 行，达上限即停并回报「已截断」）；
          ② 四重安全闸门：表名必须命中服务端白名单（app.data-maintenance.cleanup.allowed-tables）
             → 标识符正则 → information_schema 存在性 → 条件列必须是时间类型；
             保留天数小于下限（默认 1 天）直接拒绝（0 等同于删空整表）；
          ③ 请求必须带 confirm=true（缺省拒绝）；dryRun=true 时只预统计「将删除 N 行」不删数据；
          ④ 执行结果回写 lastRunStatus / lastRunTime / lastDeletedCount / lastRunResult，失败绝不谎报成功。
        · 仍未闭环：规则不自动调度（cron_expression 无消费方）、无归档到历史表、无分区表（DETACH
          PARTITION 路径当前无对象可用，故首版用分批 DELETE —— 取舍见文档 §9.3 与代码注释）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增规则 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增规则
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

        <!--
          ═══ 查询区（规则名称 / 状态 / 目标表） ═══
          ⚠️ 后端 GET /api/data-source/cleanup/list 只接受 page / pageSize（无任何过滤入参，
             实测传 ruleName/status/targetTable 会被忽略）→ 三项查询条件一律在本地过滤，
             并配合「先取 total 再一次拉全量」的取数方式，避免静默截断。
        -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('ruleName')">
                <span class="search-label">规则名称</span>
                <a-input
                  v-model:value="searchForm.ruleName"
                  placeholder="请输入规则名称（本地模糊匹配）"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('targetTable')">
                <span class="search-label">目标表</span>
                <a-input
                  v-model:value="searchForm.targetTable"
                  placeholder="请输入目标数据表（本地模糊匹配）"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="STATUS_OPTIONS"
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
          <div class="table-wrap">
            <a-alert
              class="table-alert"
              type="info"
              show-icon
              message="「立即执行」会真实删除数据（按保留天数分批 DELETE，受单次上限保护）"
              description="2026-09-19 起：后端会真实读取 targetTable / conditionColumn / retentionDays 并执行删除。安全边界：① 目标表必须在服务端白名单内（默认只放行日志/审计表），再经标识符正则与 information_schema 存在性校验；② 条件列必须是该表的时间类型列；③ 保留天数小于下限（默认 1 天）直接拒绝；④ 每批 1000 行、单次最多删 5000 行，达上限即停并回报「已截断」；⑤ 请求必须携带显式二次确认。执行结果（实删行数/失败原因）写入 lastRunStatus / lastDeletedCount / lastRunResult 并在此页回读展示。注：cron 表达式仍无调度消费方，规则不会自动执行。"
            />
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="system-data-cleanup-table-columns"
                global-config-key="system-data-cleanup-table-columns"
              >
                <!-- 规则名称 -->
                <template #ruleNameCell="{ record }">
                  <span>{{ record.ruleName || '-' }}</span>
                </template>

                <!-- 保留天数 -->
                <template #retentionDaysCell="{ record }">
                  {{ formatRetention(record.retentionDays) }}
                </template>

                <!-- 状态：启停开关（running/paused/stopped，V11.406.0 起 DB 列已改为 varchar） -->
                <template #statusCell="{ record }">
                  <a-tag :color="statusMeta(record.status).color">
                    {{ statusMeta(record.status).label }}
                  </a-tag>
                </template>

                <!-- 更新/执行时间：附最近一次「执行」的真实结论（实删行数 / 失败原因） -->
                <template #updateTimeCell="{ record }">
                  <a-tooltip :title="lastRunTip(record)">
                    <span>
                      {{ fmtTime(record.updateTime) }}
                      <a-tag
                        v-if="lastRunMeta(record.lastRunStatus)"
                        :color="lastRunMeta(record.lastRunStatus)?.color"
                        style="margin-left:4px"
                      >
                        {{ lastRunMeta(record.lastRunStatus)?.label }}
                        <template v-if="record.lastRunStatus === 'success' && record.lastDeletedCount !== undefined">
                          {{ record.lastDeletedCount }} 行
                        </template>
                      </a-tag>
                    </span>
                  </a-tooltip>
                </template>

                <!-- 操作列 -->
                <template #actionCell="{ record }">
                  <a-space
                    v-if="!record.__ghost"
                    :size="0"
                  >
                    <a-popconfirm
                      :title="`确认立即执行清理？将删除 ${record.targetTable || '(未填表)'} 中早于 ${record.retentionDays ?? '(未填)'} 天的数据，不可撤销。`"
                      ok-text="确认执行"
                      cancel-text="取消"
                      @confirm="executeRule(record)"
                    >
                      <a-button
                        type="link"
                        size="small"
                        :loading="executingId === record.id"
                      >
                        执行
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
                      title="确定删除此规则？"
                      @confirm="handleDelete(record)"
                    >
                      <a-button
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

      <!-- ═══ 新增/编辑规则弹窗（6 字段，逐字对齐开发文档 §4.1） ═══ -->
      <a-modal
        v-model:open="editVisible"
        :title="editingId ? '编辑清理规则' : '新增清理规则'"
        width="520px"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleSave"
        @cancel="closeEdit"
      >
        <a-form
          ref="formRef"
          :model="editForm"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top:16px"
        >
          <a-form-item
            label="规则名称"
            name="ruleName"
          >
            <a-input
              v-model:value="editForm.ruleName"
              placeholder="请输入规则名称"
            />
          </a-form-item>
          <a-form-item
            label="目标数据表"
            name="targetTable"
          >
            <!-- placeholder 用本库真实存在的表名（docs §9.2-P1-⑬：原示例 sys_operation_log 在本库不存在） -->
            <a-input
              v-model:value="editForm.targetTable"
              placeholder="如: sys_oper_log"
            />
          </a-form-item>
          <a-form-item
            label="条件列"
            name="conditionColumn"
          >
            <a-input
              v-model:value="editForm.conditionColumn"
              placeholder="如: create_time"
            />
          </a-form-item>
          <a-form-item
            label="保留天数"
            name="retentionDays"
          >
            <a-input-number
              v-model:value="editForm.retentionDays"
              :min="1"
              style="width: 100%"
              placeholder="超过天数的数据将被清理"
            />
          </a-form-item>
          <a-form-item
            label="Cron表达式"
            name="cronExpression"
          >
            <a-input
              v-model:value="editForm.cronExpression"
              placeholder="如: 0 0 3 * * ?"
            />
          </a-form-item>
          <a-form-item
            label="描述"
            name="description"
          >
            <a-textarea
              v-model:value="editForm.description"
              :rows="2"
              placeholder="规则描述"
            />
          </a-form-item>
        </a-form>
      </a-modal>
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
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { cleanupRuleApi, type CleanupRuleItem } from '@/api/admin'

defineOptions({ name: 'AdminDataCleanup' })

const PAGE_CONFIG_STORAGE_KEY = 'system-data-cleanup-page-config'

/** 本地过滤/分页场景下一次最多拉取的规则条数（超过则明确提示，绝不静默截断） */
const MAX_FETCH = 5000

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 状态查询候选值（V11.406.0 起 DB 列已统一为 varchar，与实体/前端同为字符串枚举） */
const STATUS_OPTIONS = [
  { label: '运行中', value: 'running' },
  { label: '已暂停', value: 'paused' },
  { label: '已停止', value: 'stopped' },
]

/** 最近一次「立即执行」的执行结论（后端 lastRunStatus） */
const LAST_RUN_MAP: Record<string, { label: string; color: string }> = {
  success: { label: '已清理', color: 'green' },
  failed: { label: '执行失败', color: 'red' },
  rejected: { label: '校验拒绝', color: 'orange' },
}

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const executingId = ref<number | null>(null)
/** 后端返回的全量规则（本地过滤 + 本地分页；后端 list 不支持过滤入参） */
const allRows = ref<CleanupRuleItem[]>([])
const tableData = ref<CleanupRuleItem[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  ruleName: '' as string,
  targetTable: '' as string,
  status: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'ruleName', label: '规则名称', visible: true },
  { key: 'targetTable', label: '目标表', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增规则', enabled: true },
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

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 180, fixed: 'left' },
  { key: 'ruleName', title: '规则名称', type: 'slot', slotName: 'ruleNameCell', width: 200 },
  { key: 'targetTable', title: '数据表', type: 'input', width: 160 },
  { key: 'conditionColumn', title: '条件列', type: 'input', width: 130 },
  { key: 'retentionDays', title: '保留天数', type: 'slot', slotName: 'retentionDaysCell', width: 100, align: 'right' },
  { key: 'cronExpression', title: '执行周期', type: 'input', width: 140 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'updateTimeCell', width: 160 },
]

// ═══ 展示辅助 ═══
function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}
function formatRetention(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return `${Number(val).toLocaleString('zh-CN')} 天`
}
/** 状态归一化：主口径是字符串枚举（running/paused/stopped）；仍兼容历史 integer 残留值 */
function statusKey(status: unknown): string {
  const s = String(status ?? '').trim().toLowerCase()
  if (s === 'running' || s === '1' || s === 'true') return 'running'
  if (s === 'paused') return 'paused'
  if (s === 'stopped' || s === '0' || s === 'false') return 'stopped'
  return ''
}
/** 执行结论的展示元数据（未知值如实原样展示，不编造语义） */
function lastRunMeta(status: string | undefined | null): { label: string; color: string } | null {
  if (!status) return null
  return LAST_RUN_MAP[status] || { label: status, color: 'default' }
}

/** 最近一次「执行」的结论提示：实删行数 / 被拒原因 / 失败原因，全部来自后端回写 */
function lastRunTip(record: CleanupRuleItem): string {
  if (!record.lastRunStatus && !record.lastRunTime) return '该规则从未执行过'
  const parts: string[] = []
  if (record.lastRunTime) parts.push(`最近执行：${fmtTime(record.lastRunTime)}`)
  const meta = lastRunMeta(record.lastRunStatus)
  if (meta) parts.push(`结论：${meta.label}`)
  if (record.lastDeletedCount !== undefined && record.lastDeletedCount !== null) {
    parts.push(`实删 ${record.lastDeletedCount} 行`)
  }
  if (record.lastRunResult) parts.push(record.lastRunResult)
  return parts.join('；')
}
function statusMeta(status: unknown): { label: string; color: string } {
  const key = statusKey(status)
  if (key === 'running') return { label: '运行中', color: 'green' }
  if (key === 'paused') return { label: '已暂停', color: 'orange' }
  if (key === 'stopped') return { label: '已停止', color: 'default' }
  // 未知值如实原样展示，不编造语义
  return { label: status === null || status === undefined || status === '' ? '-' : String(status), color: 'default' }
}

// ═══ 本地过滤 / 分页 ═══
const filteredRows = computed(() => {
  const name = searchForm.ruleName.trim().toLowerCase()
  const table = searchForm.targetTable.trim().toLowerCase()
  const status = searchForm.status
  return allRows.value.filter((row) => {
    if (name && !String(row.ruleName || '').toLowerCase().includes(name)) return false
    if (table && !String(row.targetTable || '').toLowerCase().includes(table)) return false
    if (status && statusKey(row.status) !== status) return false
    return true
  })
})

function applyLocalPage() {
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filteredRows.value.slice(start, start + pagination.pageSize)
  pagination.total = filteredRows.value.length
}

// ═══ 编辑弹窗 ═══
const editVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const emptyForm = () => ({
  ruleName: '' as string,
  targetTable: '' as string,
  conditionColumn: '' as string,
  retentionDays: 90 as number | undefined,
  cronExpression: '' as string,
  description: '' as string,
})
const editForm = reactive(emptyForm())
const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  targetTable: [{ required: true, message: '请输入目标数据表', trigger: 'blur' }],
}

function closeEdit() {
  editVisible.value = false
  editingId.value = null
  Object.assign(editForm, emptyForm())
}

function openCreate() {
  editingId.value = null
  Object.assign(editForm, emptyForm())
  editVisible.value = true
}

function openEdit(record: CleanupRuleItem) {
  editingId.value = record.id
  Object.assign(editForm, emptyForm(), {
    ruleName: record.ruleName || '',
    targetTable: record.targetTable || '',
    conditionColumn: record.conditionColumn || '',
    retentionDays: record.retentionDays ?? 90,
    cronExpression: record.cronExpression || '',
    description: record.description || '',
  })
  editVisible.value = true
}

// ═══ 数据加载 ═══
/**
 * 拉取全量规则。
 * 后端 list 只有 page/pageSize（无过滤入参），所以先取 total 再按 total 一次拉全量，
 * 供本地过滤 + 本地分页使用；超过 MAX_FETCH 时明确提示，避免「静默截断」（开发文档 §9.2-P1-⑪）。
 */
async function fetchAllRows(): Promise<CleanupRuleItem[]> {
  const probe: any = await cleanupRuleApi.page({ page: 1, pageSize: 1 })
  const total = Number(probe?.total) || 0
  if (total === 0) return []
  if (total > MAX_FETCH) {
    message.warning(`规则共 ${total} 条，超过单次加载上限 ${MAX_FETCH} 条，当前仅加载前 ${MAX_FETCH} 条`)
  }
  const res: any = await cleanupRuleApi.page({ page: 1, pageSize: Math.min(total, MAX_FETCH) })
  return res?.records || []
}

async function fetchList() {
  loading.value = true
  try {
    allRows.value = await fetchAllRows()
    applyLocalPage()
  } catch (error: any) {
    // 不静默吞错：区分「接口故障」与「列表为空」（开发文档 §9.2-P1-⑩）
    console.error('[清理规则] 加载列表失败', error)
    message.error(error?.message || '加载清理规则失败')
    allRows.value = []
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 回读指定行（后端无按 id 查询端点，只能重新拉列表后定位） */
async function readBackRow(id: number): Promise<CleanupRuleItem | null> {
  const rows = await fetchAllRows()
  allRows.value = rows
  return rows.find(r => r.id === id) || null
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  applyLocalPage()
}
function handleReset() {
  searchForm.ruleName = ''
  searchForm.targetTable = ''
  searchForm.status = undefined
  pagination.current = 1
  applyLocalPage()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyLocalPage()
}

// ═══ 新增 / 编辑（写后必回读，不一致就如实警告，不谎报成功） ═══
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      ruleName: editForm.ruleName.trim(),
      targetTable: editForm.targetTable.trim(),
      conditionColumn: editForm.conditionColumn.trim(),
      retentionDays: editForm.retentionDays,
      cronExpression: editForm.cronExpression.trim(),
      description: editForm.description.trim(),
    }
    if (editingId.value) {
      const id = editingId.value
      await cleanupRuleApi.update(id, payload)
      const back = await readBackRow(id)
      if (back && back.ruleName === payload.ruleName && back.targetTable === payload.targetTable) {
        message.success('规则已更新')
        editVisible.value = false
        editingId.value = null
      } else {
        message.warning('接口返回成功，但回读列表未确认到修改后的内容，请点「刷新」复核')
        editVisible.value = false
      }
    } else {
      const created: any = await cleanupRuleApi.create(payload)
      const newId = Number(created?.id)
      const back = Number.isFinite(newId) ? await readBackRow(newId) : null
      if (back) {
        message.success('规则已创建')
        editVisible.value = false
        editingId.value = null
      } else {
        message.warning('接口返回成功，但回读列表查不到该规则，实际未落库')
        editVisible.value = false
      }
    }
    applyLocalPage()
  } catch (e: any) {
    console.error('[清理规则] 保存失败', e)
    message.error(e?.message || (editingId.value ? '更新失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: CleanupRuleItem) {
  try {
    await cleanupRuleApi.remove(record.id)
    const back = await readBackRow(record.id)
    if (back) {
      message.warning('接口返回成功，但回读列表该规则仍存在，删除未生效')
    } else {
      message.success('规则已删除')
    }
    applyLocalPage()
  } catch (e: any) {
    console.error('[清理规则] 删除失败', e)
    message.error(e?.message || '删除失败')
  }
}

/**
 * 「立即执行」—— 真实删除 + 回读核验，结论**只认后端回写的 lastRunStatus/lastDeletedCount**。
 *
 * 后端行为（2026-09-19 起，见开发文档 §9.3）：
 *   · 真实按 targetTable / conditionColumn / retentionDays 分批 DELETE（每批 1000、单次上限 5000）；
 *   · 前置四重校验（白名单 / 标识符正则 / 存在性 / 时间类型列）不通过时整单拒绝，不删任何数据；
 *   · 结论写回 lastRunStatus(success/failed/rejected) + lastDeletedCount + lastRunResult；
 *   · status（启停开关）**不再被「执行」修改** —— 旧实现把它当执行态用，导致「执行一次=把规则打开」。
 *
 * 本函数因此：
 *   ① 前端提交前弹 popconfirm 让用户确认（人已确认过），再以 confirm=true 调接口；
 *   ② 调用后回读台账，只根据 lastRunStatus/lastDeletedCount 的真实变化给结论；
 *   ③ 后端以 success=false 拒绝时（如目标表不在白名单）走 catch，如实展示后端给出的原因。
 */
async function executeRule(record: CleanupRuleItem) {
  executingId.value = record.id
  try {
    const before = await readBackRow(record.id)
    await cleanupRuleApi.execute(record.id, true, false)
    const after = await readBackRow(record.id)

    if (!after) {
      message.warning('执行后回读不到该规则，无法确认任何变化')
      applyLocalPage()
      return
    }
    const changed = !before
      || after.lastRunStatus !== before.lastRunStatus
      || after.lastRunTime !== before.lastRunTime
      || after.lastDeletedCount !== before.lastDeletedCount
    if (!changed) {
      message.error('接口未报错，但回读未发现执行结果被写入台账（lastRunStatus 未变化），请刷新后复核')
      applyLocalPage()
      return
    }
    if (after.lastRunStatus === 'success') {
      const deleted = after.lastDeletedCount ?? 0
      const truncated = (after.lastRunResult || '').includes('已达单次上限')
      if (deleted === 0) {
        message.success(`已执行：目标表 ${after.targetTable || '(未填)'} 中早于保留期的数据为 0 行，无需删除`)
      } else if (truncated) {
        message.warning(`已清理 ${deleted} 行：${after.lastRunResult || ''}`)
      } else {
        message.success(`清理完成：实际删除 ${deleted} 行（${after.targetTable || '(未填)'}）`)
      }
    } else if (after.lastRunStatus === 'rejected') {
      message.error(`清理未执行（前置校验拒绝）：${after.lastRunResult || '后端未提供原因'}`)
    } else if (after.lastRunStatus === 'failed') {
      message.error(`清理执行失败：${after.lastRunResult || '后端未提供原因'}`)
    } else {
      message.warning(`执行结论为「${after.lastRunStatus}」，请刷新复核`)
    }
    applyLocalPage()
  } catch (e: any) {
    console.error('[清理规则] 执行失败', e)
    // 后端拒绝（未确认 / 不在白名单 / 保留天数越界等）会以 success=false 返回，原因需要如实展示
    message.error(e?.message || '清理执行失败')
    // 失败后同样回读，保持界面与后端一致
    try {
      allRows.value = await fetchAllRows()
      applyLocalPage()
    } catch (err) {
      console.error('[清理规则] 执行失败后回读列表也失败', err)
    }
  } finally {
    executingId.value = null
  }
}

function handleError(error: Error) {
  console.error('[清理规则] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.table-wrap { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-alert { margin: 8px 8px 0; flex-shrink: 0; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
