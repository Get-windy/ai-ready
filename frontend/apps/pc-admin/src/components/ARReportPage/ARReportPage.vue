<template>
  <ErrorBoundary>
    <PageContainer :title="title">
      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        v-if="statCards && statCards.length"
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 查询区 ═══ -->
      <div
        v-if="(queryFields && queryFields.length) || $slots['extra-fields']"
        class="search-area"
      >
        <a-form layout="inline">
          <a-form-item
            v-for="field in queryFields"
            :key="field.key"
            :label="field.type === 'select' ? field.label : undefined"
          >
            <a-input
              v-if="field.type === 'input'"
              v-model:value="queryModel[field.key]"
              :placeholder="field.placeholder || field.label || '请输入'"
              :allow-clear="field.allowClear !== false"
              :style="{ width: (field.width || 200) + 'px' }"
              @press-enter="handleSearch"
            />
            <a-select
              v-else-if="field.type === 'select'"
              v-model:value="queryModel[field.key]"
              :placeholder="field.placeholder || '全部'"
              :allow-clear="field.allowClear !== false"
              :options="field.options"
              :style="{ width: (field.width || 180) + 'px' }"
            />
            <a-range-picker
              v-else-if="field.type === 'date-range'"
              v-model:value="dateRanges[field.key]"
              :allow-clear="field.allowClear !== false"
              :style="{ width: (field.width || 240) + 'px' }"
              @change="handleDateRangeChange(field, $event)"
            />
          </a-form-item>
          <slot name="extra-fields" />
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 图表区（可选） ═══ -->
      <div v-if="$slots.chart" class="chart-area">
        <slot name="chart" />
      </div>

      <!-- ═══ 表格区 ═══ -->
      <div class="table-area">
        <div
          v-if="showRefresh || showExport || $slots['header-extra']"
          class="table-toolbar"
        >
          <a-space>
            <slot name="header-extra" />
            <a-button
              v-if="showRefresh"
              size="small"
              @click="reload"
            >
              <template #icon>
                <ReloadOutlined />
              </template>刷新
            </a-button>
            <a-button
              v-if="showExport"
              size="small"
              @click="handleExport"
            >
              <template #icon>
                <ExportOutlined />
              </template>导出
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="tablePagination"
          :row-key="rowKey"
          :row-selection="rowSelectionConfig"
          :locale="{ emptyText }"
          size="small"
          @change="handleTableChange"
        >
          <template
            v-if="$slots.bodyCell"
            #bodyCell="slotProps"
          >
            <slot
              name="bodyCell"
              v-bind="slotProps"
            />
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script lang="ts">
import type {
  ReportQueryField,
  ReportFetchResult,
  ReportFetcher,
  ReportExportContext,
  StatCardItem
} from '@/components/ARReportPage/types'

export type { ReportQueryField, ReportFetchResult, ReportFetcher, ReportExportContext, StatCardItem }
</script>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, ClearOutlined, ReloadOutlined, ExportOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'

defineOptions({ name: 'ARReportPage' })

const props = withDefaults(defineProps<{
  /** 页面标题 */
  title: string
  /** 查询字段配置 */
  queryFields?: ReportQueryField[]
  /** 表格列配置（a-table columns） */
  columns: any[]
  /** 数据请求函数，入参为分页参数 + 查询参数 */
  fetcher: ReportFetcher
  /** 响应归一化；默认兼容 records/list/content/数组 四种分页结构 */
  normalizeResponse?: (res: any) => ReportFetchResult
  /** 行主键，默认 id */
  rowKey?: string | ((record: any) => string)
  /** 每页条数，默认 20 */
  pageSize?: number
  /** 分页参数风格：page/size（默认）或 pageNum/pageSize */
  pageParamStyle?: 'page' | 'pageNum'
  /** 统计卡片（随主请求 loading） */
  statCards?: StatCardItem[]
  /** 导出文件名；设置后显示导出按钮（内置当前页 CSV 导出） */
  exportFileName?: string
  /** 自定义导出处理（优先于内置 CSV 导出） */
  onExport?: (ctx: ReportExportContext) => void
  /** 是否挂载后立即加载，默认 true */
  immediate?: boolean
  /** 是否显示刷新按钮，默认 true */
  showRefresh?: boolean
  /**
   * 初始查询条件（挂载时写入查询模型，用于从其他页面下钻带入筛选）。
   * 仅写入非空值；键名与 queryFields 的 key / paramKey 一致。
   */
  initialQuery?: Record<string, any>
  /** 空态文案 */
  emptyText?: string
  /** 是否启用行选择（checkbox），默认 false */
  enableRowSelection?: boolean
  /** 行选择类型，默认 checkbox */
  rowSelectionType?: 'checkbox' | 'radio'
}>(), {
  queryFields: () => [],
  rowKey: 'id',
  pageSize: 20,
  pageParamStyle: 'page',
  statCards: () => [],
  immediate: true,
  initialQuery: () => ({}),
  showRefresh: true,
  emptyText: '暂无数据',
  enableRowSelection: false,
  rowSelectionType: 'checkbox'
})

const emit = defineEmits<{
  /** 数据加载完成 */
  loaded: [result: ReportFetchResult]
  /** 行选择变化 */
  'selection-change': [keys: (string | number)[], rows: any[]]
}>()

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const queryModel = reactive<Record<string, any>>({})
const dateRanges = reactive<Record<string, [Dayjs, Dayjs] | null>>({})
const pagination = reactive({ current: 1, pageSize: props.pageSize, total: 0 })

// ═══ 行选择 ═══
const selectedRowKeys = ref<(string | number)[]>([])
const selectedRows = ref<any[]>([])

const rowSelectionConfig = computed(() => {
  if (!props.enableRowSelection) return undefined
  return {
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: (string | number)[], rows: any[]) => {
      selectedRowKeys.value = keys
      selectedRows.value = rows
      emit('selection-change', keys, rows)
    },
    type: props.rowSelectionType
  }
})

const tablePagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

const showExport = computed(() => !!props.exportFileName || !!props.onExport)

// ═══ 参数组装 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (props.pageParamStyle === 'pageNum') {
    params.pageNum = pagination.current
    params.pageSize = pagination.pageSize
  } else {
    params.page = pagination.current
    params.size = pagination.pageSize
  }
  for (const field of props.queryFields) {
    if (field.type === 'date-range') continue // 日期范围 change 时已写入 queryModel
    const value = queryModel[field.key]
    if (value === '' || value === null || value === undefined) continue
    params[field.paramKey || field.key] = value
  }
  for (const field of props.queryFields) {
    if (field.type !== 'date-range') continue
    const start = queryModel[field.startKey || 'startDate']
    const end = queryModel[field.endKey || 'endDate']
    if (start) params[field.startKey || 'startDate'] = start
    if (end) params[field.endKey || 'endDate'] = end
  }
  return params
}

function handleDateRangeChange(field: ReportQueryField, dates: [Dayjs, Dayjs] | [string, string] | null) {
  if (Array.isArray(dates) && dates.length === 2 && dates[0] && dates[1]) {
    queryModel[field.startKey || 'startDate'] = dayjs(dates[0]).format('YYYY-MM-DD')
    queryModel[field.endKey || 'endDate'] = dayjs(dates[1]).format('YYYY-MM-DD')
  } else {
    queryModel[field.startKey || 'startDate'] = undefined
    queryModel[field.endKey || 'endDate'] = undefined
  }
}

// ═══ 响应归一化 ═══
function defaultNormalize(res: any): ReportFetchResult {
  if (!res) return { list: [], total: 0, raw: res }
  if (Array.isArray(res)) return { list: res, total: res.length, raw: res }
  const data = res.data && (Array.isArray(res.data) || typeof res.data === 'object') && !res.records && !res.list
    ? res.data
    : res
  const list = data.records || data.list || data.content || []
  return { list, total: Number(data.total) || list.length, raw: res }
}

// ═══ 数据请求 ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await props.fetcher(buildParams())
    const result = (props.normalizeResponse || defaultNormalize)(res)
    tableData.value = result.list
    pagination.total = result.total
    emit('loaded', result)
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    message.error('查询失败，请检查网络后重试')
    console.warn(`[ARReportPage:${props.title}] 获取失败`, e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  for (const field of props.queryFields) {
    if (field.type === 'date-range') {
      dateRanges[field.key] = null
      queryModel[field.startKey || 'startDate'] = undefined
      queryModel[field.endKey || 'endDate'] = undefined
    } else {
      queryModel[field.key] = undefined
    }
  }
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || props.pageSize
  fetchData()
}

function reload() {
  fetchData()
}

// ═══ 导出 ═══
function handleExport() {
  const ctx: ReportExportContext = {
    rows: tableData.value,
    columns: props.columns,
    params: buildParams()
  }
  if (props.onExport) {
    props.onExport(ctx)
    return
  }
  if (!ctx.rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = props.columns.filter(c => c.dataIndex)
  const headers = exportCols.map(c => c.title)
  const rows = ctx.rows.map(row =>
    exportCols.map(col => {
      const val = row[col.dataIndex]
      return `"${val === null || val === undefined ? '' : String(val).replace(/"/g, '""')}"`
    })
  )
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${props.exportFileName || '报表'}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

/** 把下钻带入的初始条件写入查询模型（仅非空值） */
function applyInitialQuery() {
  const init = props.initialQuery || {}
  Object.keys(init).forEach((key) => {
    const value = init[key]
    if (value !== undefined && value !== null && value !== '') {
      queryModel[key] = value
    }
  })
}

onMounted(() => {
  // 下钻带入的初始条件：写入查询模型后再发起首次查询
  applyInitialQuery()
  if (props.immediate) fetchData()
})

// 同一路由重复下钻（如仪表盘 待分配任务 → 异常任务）时组件被复用，需响应条件变化并重查
watch(() => props.initialQuery, () => {
  applyInitialQuery()
  pagination.current = 1
  fetchData()
}, { deep: true })

/** 清空选择 */
function clearSelection() {
  selectedRowKeys.value = []
  selectedRows.value = []
}

defineExpose({ reload, search: handleSearch, buildParams, clearSelection, selectedRowKeys, selectedRows })
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-area {
  margin-bottom: 16px;
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
