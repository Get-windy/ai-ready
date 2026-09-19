<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        系统任务（设置 → 账套操作 → 系统任务，菜单 70561 / set:system-task，单入口）
        · 页面性质 = **异步任务执行台账**：一行 = **一次执行**（对标 ql361 实测 9 列 235 条）
        · 数据源 = 既有 scheduled_task_log（迁移 V11.401.0 补台账列），后端 SystemTaskController
        · 9 列逐字对齐对标：任务ID · 任务类型 · 任务名称 · 任务状态 · 创建人 ·
          任务创建时间 · 任务开始时间 · 任务结束时间 · 结果查看
        · 查询区仅「任务ID」一个条件（对标查询区只有它）；工具栏仅「刷新」（对标无打印/导出）
        · 无页面配置弹窗（对标 pageConfig.found = false，开发文档 §8.1-13 裁定与对标一致）
        · 列配置齿轮在数据表表头 rowNo 列（个人 / 全局），storage-key 持久化
        · 行内「下载处理结果」→ 后端鉴权下载端点（无产物时置灰并说明原因，不造假文件）
        · 规格书：docs/Yh-Spec/手动整理对标开发文档/设置模块/系统任务开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏右侧：刷新（对标唯一按钮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格；条件仅「任务ID」，与对标一致） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <div class="search-field-item">
                <a-input
                  v-model:value="searchForm.taskId"
                  placeholder="任务ID"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: `span ${actionSpan}` }"
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

        <!-- ═══ 数据表（9 列；齿轮列配置走表头） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="taskId"
              storage-key="set-system-task-table-columns"
              global-config-key="set-system-task-table-columns"
              @sort-change="handleSortChange"
            >
              <!-- 任务状态：统一字符码 → 中文 tag（失败=红 / 成功=绿 / 执行中=蓝）；
                   失败行挂 tooltip 展示失败原因（开发文档 §4-7「失败态必须有可读原因」） -->
              <template #statusCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.errorMsg"
                  :title="record.errorMsg"
                >
                  <a-tag :color="statusMeta(record.status).color">
                    {{ statusMeta(record.status).text }}
                  </a-tag>
                </a-tooltip>
                <a-tag
                  v-else-if="!record.__ghost"
                  :color="statusMeta(record.status).color"
                >
                  {{ statusMeta(record.status).text }}
                </a-tag>
              </template>

              <!-- 创建人：写入侧自 V11.416.0 起记录发起人 —— 有会话（立即执行/重试）记姓名，
                   无会话的定时调度记「定时调度」；更早的历史行无任何来源可回填 → 如实显示「-」
                   并说明原因（不编造姓名）。 -->
              <template #creatorCell="{ record }">
                <template v-if="!record.__ghost">
                  <span v-if="record.createdByName">{{ record.createdByName }}</span>
                  <a-tooltip
                    v-else
                    placement="bottom"
                    title="该行写入于「创建人」记录补全（V11.416.0）之前，未留存发起人信息，无法回填"
                  >
                    <span class="cell-empty">-</span>
                  </a-tooltip>
                </template>
              </template>

              <!-- 结果查看：下载处理结果（无产物的行置灰并说明原因，不提供假文件） -->
              <template #resultCell="{ record }">
                <template v-if="!record.__ghost">
                  <span
                    v-if="downloadingId === record.taskId"
                    class="cell-empty"
                  >下载中…</span>
                  <a
                    v-else-if="record.resultUrl"
                    v-permission="'set:system-task:download'"
                    class="cell-link"
                    @click="handleDownload(record)"
                  >下载处理结果</a>
                  <a-tooltip
                    v-else
                    :title="resultTip(record)"
                  >
                    <span class="cell-disabled">下载处理结果</span>
                  </a-tooltip>
                </template>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（对标「共 N 条记录 / 第 (x/y) 页 / 每页显示 20 行」） ═══ -->
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
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { systemTaskApi, type SystemTaskRow } from '@/api/set/system-task'

defineOptions({ name: 'SetSystemTask' })

// ═══ 任务状态（后端统一字符码闭集 → 中文 + tag 颜色；开发文档 §5.1 / §10.2-25） ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  pending: { text: '待执行', color: 'default' },
  running: { text: '执行中', color: 'blue' },
  success: { text: '成功', color: 'green' },
  failed: { text: '失败', color: 'red' },
  partial: { text: '部分成功', color: 'orange' },
  unknown: { text: '未知', color: 'default' },
}

function statusMeta(status?: string | null) {
  return STATUS_MAP[status || ''] || STATUS_MAP.unknown
}

/** 时间列统一格式（与对标实测形态一致：2026-08-13 17:55:45） */
function fmtTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

/** 不可下载时的说明（如实告知原因，不静默置灰） */
function resultTip(record: SystemTaskRow): string {
  switch (record.status) {
    case 'failed':
      return `任务执行失败，无处理结果：${record.errorMsg || '未记录失败原因'}`
    case 'running':
      return '任务执行中，处理结果尚未生成'
    case 'pending':
      return '任务待执行，暂无处理结果'
    case 'success':
    case 'partial':
      return '该任务未产出可下载的处理结果'
    default:
      return '任务状态未知，暂无处理结果'
  }
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<SystemTaskRow[]>([])
const downloadingId = ref<string | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field: string; order: string }>({ field: '', order: '' })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询条件（对标查询区只有「任务ID」一项） ═══
const searchForm = reactive({ taskId: '' })

/**
 * 9 列（逐字对齐对标 columnConfig.cols）；
 * rowNo 列承载表头「列配置」齿轮（组件内嵌），不计入 9 列业务列。
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'taskId', title: '任务ID', width: 110, sortable: true },
  { key: 'taskType', title: '任务类型', width: 100, formatter: (v: any) => v || '-' },
  { key: 'taskName', title: '任务名称', width: 200, formatter: (v: any) => v || '-' },
  { key: 'status', title: '任务状态', type: 'slot', slotName: 'statusCell', width: 100 },
  // 创建人：取值 = 会话用户姓名 / 调度触发的「定时调度」（写入侧见 TaskExecutor#executeTask）；
  // 用插槽是为了给「补全前写入的历史行」加可读说明（纯 formatter 只能显示一个无解释的「-」）
  { key: 'createdByName', title: '创建人', width: 110, type: 'slot', slotName: 'creatorCell' },
  // 对标「创建人」列表头有排序箭头，但历史行创建人为空、排序价值低 → 排序开在任务ID 与三个时间列上
  // （后端白名单见 SystemTaskController#applySort）
  { key: 'createTime', title: '任务创建时间', width: 170, sortable: true, formatter: (v: any) => fmtTime(v) },
  { key: 'startTime', title: '任务开始时间', width: 170, sortable: true, formatter: (v: any) => fmtTime(v) },
  { key: 'endTime', title: '任务结束时间', width: 170, sortable: true, formatter: (v: any) => fmtTime(v) },
  { key: 'resultUrl', title: '结果查看', type: 'slot', slotName: 'resultCell', width: 130 },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await systemTaskApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      taskId: searchForm.taskId.trim() || undefined,
      sortField: sortState.field || undefined,
      sortOrder: sortState.order || undefined,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[系统任务] 加载台账失败', error)
    message.error(error?.response?.data?.message || '加载任务台账失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 查询 / 重置 / 刷新 / 分页 / 排序 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.taskId = ''
  sortState.field = ''
  sortState.order = ''
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

function handleSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  sortState.field = key || ''
  sortState.order = order || ''
  pagination.current = 1
  fetchList()
}

// ═══ 下载处理结果（真实文件流；失败时读出后端 JSON 里的可读原因） ═══
const MIME_EXT: Record<string, string> = {
  'text/plain': '.txt',
  'text/csv': '.csv',
  'application/json': '.json',
  'application/pdf': '.pdf',
  'application/zip': '.zip',
  'application/vnd.ms-excel': '.xls',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': '.xlsx',
}

function fileExt(blob: Blob): string {
  const mime = (blob.type || '').split(';')[0].trim().toLowerCase()
  return MIME_EXT[mime] || '.dat'
}

/** 从 blob 错误响应读后端下发的可读原因（404/409/410 均为 JSON 错误体） */
async function readErrorMessage(error: any): Promise<string> {
  const data = error?.response?.data
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text())
      return parsed?.message || '下载处理结果失败'
    } catch {
      return '下载处理结果失败'
    }
  }
  return data?.message || error?.message || '下载处理结果失败'
}

async function handleDownload(record: SystemTaskRow) {
  if (!record.resultUrl) return
  downloadingId.value = record.taskId
  try {
    const blob: any = await systemTaskApi.downloadResult(record.resultUrl)
    if (!(blob instanceof Blob) || blob.size === 0) {
      message.warning('该任务未产出处理结果')
      return
    }
    const objectUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objectUrl
    a.download = `任务结果-${record.taskId}${fileExt(blob)}`
    a.click()
    window.URL.revokeObjectURL(objectUrl)
    message.success('处理结果已开始下载')
  } catch (error: any) {
    console.warn('[系统任务] 下载处理结果失败', error)
    message.error(await readErrorMessage(error))
  } finally {
    downloadingId.value = null
  }
}

function handleError(error: Error) {
  console.error('[系统任务] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
/* 查询区必须横向自适应网格（禁止纵向单列） */
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.cell-disabled { color: #bbb; cursor: not-allowed; }
.cell-empty { color: #999; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
