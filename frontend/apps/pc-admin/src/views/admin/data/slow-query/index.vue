<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        慢查询（系统管理 → 数据管理 → 慢查询，菜单 62302）
        · 只读监控页；按业界（pg_stat_statements / MySQL slow log / Percona PMM Query Analytics）建模
        · 数据表列配置齿轮在表头 rowNo 列（个人/全局），storage-key 持久化
        · 后端：cn.aiedge.datasource.controller.SlowQueryController（前缀 /api/data-source/slow-query）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/慢查询开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：阈值口径说明 ═══ -->
        <template #toolbar-left>
          <span class="toolbar-tip">{{ thresholdTip }}</span>
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

        <!-- ═══ 查询区（数据源走后端；SQL 关键字/耗时阈值/时间范围为本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('keyword')">
                <span class="search-label">SQL 关键字</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="SQL 片段，如 select"
                  size="small"
                  style="width: 200px"
                  allow-clear
                />
              </template>
              <template v-if="isQueryFieldVisible('threshold')">
                <span class="search-label">慢查询阈值</span>
                <a-select
                  v-model:value="searchForm.threshold"
                  size="small"
                  style="width: 120px"
                  :options="THRESHOLD_OPTIONS"
                />
              </template>
              <template v-if="isQueryFieldVisible('timeRange')">
                <span class="search-label">执行时间</span>
                <a-range-picker
                  v-model:value="searchForm.timeRange"
                  size="small"
                  style="width: 240px"
                />
              </template>
              <template v-if="isQueryFieldVisible('dataSourceId')">
                <span class="search-label">数据源</span>
                <a-select
                  v-model:value="searchForm.dataSourceId"
                  placeholder="全部数据源"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  :options="dataSourceOptions"
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

        <!-- ═══ 统计卡 + 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <ARStatCards
              :items="statCards"
              :loading="loading"
              class="stat-cards"
            />
            <BillDetailTable
              v-model:data-source="displayData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-data-slow-query-table-columns"
              global-config-key="system-data-slow-query-table-columns"
            >
              <!-- SQL 语句：截断展示 + 悬停看全文（详情弹窗内另有可滚动的完整 SQL） -->
              <template #queryTextCell="{ record }">
                <a-tooltip
                  v-if="!record.__ghost && record.queryText"
                  :title="record.queryText"
                  placement="topLeft"
                >
                  <span class="sql-text">{{ record.queryText }}</span>
                </a-tooltip>
              </template>

              <!-- 耗时：按业界分档着色（>5s 红 / >1s 橙 / 其余蓝） -->
              <template #queryTimeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.queryTimeMs > 5000 ? 'red' : record.queryTimeMs > 1000 ? 'orange' : 'blue'"
                >
                  {{ formatNumber(record.queryTimeMs) }}ms
                </a-tag>
              </template>

              <!-- 执行时间：后端为 LocalDateTime，统一格式化展示 -->
              <template #queryTimeDateCell="{ record }">
                {{ fmtTime(record.queryTime) }}
              </template>

              <!-- 返回行数 -->
              <template #rowsSentCell="{ record }">
                {{ formatNumber(record.rowsSent) }}
              </template>

              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="showDetail(record)"
                  >
                    详情
                  </a-button>
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

      <!-- ═══ 详情弹窗（只读，不产生网络请求） ═══ -->
      <a-modal
        v-model:open="detailVisible"
        title="慢查询详情"
        width="800px"
        :footer="null"
      >
        <a-descriptions
          :column="1"
          size="small"
          bordered
        >
          <a-descriptions-item label="SQL语句">
            <pre class="sql-detail">{{ detailItem?.queryText || '-' }}</pre>
          </a-descriptions-item>
          <a-descriptions-item label="执行耗时">
            {{ formatNumber(detailItem?.queryTimeMs) }}ms
          </a-descriptions-item>
          <a-descriptions-item label="锁等待耗时">
            {{ formatNumber(detailItem?.lockTimeMs) }}ms
          </a-descriptions-item>
          <a-descriptions-item label="数据库">
            {{ detailItem?.databaseName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="执行时间">
            {{ fmtTime(detailItem?.queryTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="扫描行数">
            {{ formatNumber(detailItem?.rowsExamined) }}
          </a-descriptions-item>
          <a-descriptions-item label="返回行数">
            {{ formatNumber(detailItem?.rowsSent) }}
          </a-descriptions-item>
          <a-descriptions-item label="用户">
            {{ detailItem?.userName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="主机信息">
            {{ detailItem?.hostInfo || '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
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
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { slowQueryApi, dataSourceApi, type SlowQueryItem } from '@/api/admin'

defineOptions({ name: 'AdminDataSlowQuery' })

const PAGE_CONFIG_STORAGE_KEY = 'system-data-slow-query-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 耗时阈值：0 = 全部（不做耗时过滤），其余为「严格大于」 */
const THRESHOLD_OPTIONS = [
  { label: '全部', value: 0 },
  { label: '>100ms', value: 100 },
  { label: '>500ms', value: 500 },
  { label: '>1s', value: 1000 },
  { label: '>5s', value: 5000 },
]

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<SlowQueryItem[]>([])
const dataSourceOptions = ref<{ label: string; value: number }[]>([])
const detailVisible = ref(false)
const detailItem = ref<SlowQueryItem | null>(null)

/** 后端 /list 为内存分页（records/total/current/size/pages），真分页参数 */
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  keyword: '' as string,
  threshold: 1000 as number,
  timeRange: undefined as any,
  dataSourceId: undefined as number | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: 'SQL 关键字', visible: true },
  { key: 'threshold', label: '慢查询阈值', visible: true },
  { key: 'timeRange', label: '执行时间', visible: true },
  { key: 'dataSourceId', label: '数据源', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
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
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'queryText', title: 'SQL语句', type: 'slot', slotName: 'queryTextCell', width: 340 },
  { key: 'queryTimeMs', title: '耗时', type: 'slot', slotName: 'queryTimeCell', width: 110 },
  { key: 'databaseName', title: '数据库', type: 'input', width: 130 },
  { key: 'queryTime', title: '执行时间', type: 'slot', slotName: 'queryTimeDateCell', width: 170 },
  { key: 'rowsSent', title: '返回行', type: 'slot', slotName: 'rowsSentCell', width: 90, align: 'right' },
  { key: 'userName', title: '操作用户', type: 'input', width: 120 },
]

/**
 * 展示行 = 后端当页数据 + SQL关键字/耗时阈值/时间范围本地过滤。
 *
 * ⚠️ 后端 GET /api/data-source/slow-query/list 只接收 dataSourceId / page / pageSize 三个入参
 * （SlowQueryController.list），没有 SQL 关键字、耗时与时间范围的筛选参数。
 * 故这三项为**前端本地过滤**，作用范围是后端返回的当前页；改动这三项不发请求（阈值口径见 §10.2）。
 * 后端补上筛选参数后，这三项应改为服务端条件。
 */
const displayData = computed(() => {
  const kw = searchForm.keyword.trim().toLowerCase()
  const range = searchForm.timeRange
  return tableData.value.filter((r: SlowQueryItem) => {
    // SQL 关键字
    if (kw && !String(r.queryText || '').toLowerCase().includes(kw)) return false
    // 耗时阈值：选「全部(0)」不做耗时过滤；其余为严格大于（与选项文案 >Nms 一致）
    if (searchForm.threshold > 0 && (Number(r.queryTimeMs) || 0) <= searchForm.threshold) return false
    // 执行时间区间（含首尾整日）
    if (range && range[0] && range[1]) {
      const t = r.queryTime ? dayjs(r.queryTime) : null
      if (!t || !t.isValid()) return false
      if (t.isBefore(dayjs(range[0]).startOf('day')) || t.isAfter(dayjs(range[1]).endOf('day'))) return false
    }
    return true
  })
})

const thresholdTip = computed(() => {
  const label = THRESHOLD_OPTIONS.find(o => o.value === searchForm.threshold)?.label || '全部'
  return `耗时筛选：${label}（当前页本地过滤）`
})

// ═══ 统计卡（口径：本地过滤后的当页数据） ═══
const statCards = computed<StatCardItem[]>(() => {
  const durations = displayData.value.map(r => Number(r.queryTimeMs) || 0)
  const total = durations.length
  const avg = total ? Math.round(durations.reduce((a, b) => a + b, 0) / total) : 0
  const max = total ? Math.max(...durations) : 0
  const over5s = durations.filter(d => d > 5000).length
  return [
    { label: '本页慢查询', value: total, suffix: '条' },
    { label: '本页平均耗时', value: avg, suffix: 'ms' },
    { label: '本页最长耗时', value: max, suffix: 'ms', valueStyle: { color: '#ff4d4f' } },
    { label: '本页超过5s', value: over5s, suffix: '条', valueStyle: { color: '#faad14' } },
  ]
})

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await slowQueryApi.page({
      dataSourceId: searchForm.dataSourceId,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    // 禁止假数据兜底：失败即清空并明示原因（此前 catch 为空，404 会伪装成「0 条慢查询」）
    console.error('[慢查询] 加载慢查询列表失败', error)
    message.error(error?.message || '加载慢查询列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 数据源下拉：后端 /list 无「不分页」开关，取前 100 个（超出部分不在下拉中，属已知 P2） */
async function fetchDataSources() {
  try {
    const res: any = await dataSourceApi.page({ page: 1, pageSize: 100 })
    dataSourceOptions.value = (res?.records || []).map((d: any) => ({ label: d.name, value: d.id }))
  } catch (error: any) {
    console.error('[慢查询] 加载数据源下拉失败', error)
    message.error(error?.message || '加载数据源列表失败')
    dataSourceOptions.value = []
  }
}

// ═══ 事件 ═══
/** 查询：数据源是后端入参，需回到第 1 页重新拉取；本地过滤项随 computed 即时生效 */
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.threshold = 1000
  searchForm.timeRange = undefined
  searchForm.dataSourceId = undefined
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

function showDetail(record: SlowQueryItem) {
  // 行数据已在内存中，弹窗直接渲染，不产生网络请求
  detailItem.value = record
  detailVisible.value = true
}

function handleError(error: Error) {
  console.error('[慢查询] 页面错误', error)
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
/* 统计卡固定在表格上方，不参与 flex 拉伸 */
.stat-cards { flex-shrink: 0; margin-bottom: 8px; }
.toolbar-tip { font-size: 13px; color: #666; }
/* SQL 语句截断：完整语句走 tooltip 与详情弹窗 */
.sql-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
  font-family: Consolas, Monaco, monospace;
}
.sql-detail {
  max-height: 300px;
  overflow: auto;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  margin: 0;
}
</style>
