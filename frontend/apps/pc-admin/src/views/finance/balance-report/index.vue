<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏左侧：标题 + 查询方案 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <span class="list-title">资产负债表</span>
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
          </div>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出（无「配置」齿轮，本页无页面配置弹窗） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：查询方案 / 单-多会计月 / 会计月(止) / 科目层级 / 显示为0科目 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <a-radio-group
                  v-model:value="periodMode"
                  size="small"
                  button-style="solid"
                  @change="handleSearch"
                >
                  <a-radio-button value="single">单会计月</a-radio-button>
                  <a-radio-button value="multi">多会计月</a-radio-button>
                </a-radio-group>
              </div>
              <div
                v-if="periodMode === 'multi'"
                class="search-item"
              >
                <span class="search-label">会计月(起)</span>
                <a-date-picker
                  v-model:value="startMonth"
                  picker="month"
                  size="small"
                  style="width: 120px"
                  :allow-clear="false"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">会计月(止)</span>
                <a-date-picker
                  v-model:value="endMonth"
                  picker="month"
                  size="small"
                  style="width: 120px"
                  :allow-clear="false"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">科目层级</span>
                <a-select
                  v-model:value="subjectLevel"
                  :options="LEVEL_OPTIONS"
                  size="small"
                  style="width: 90px"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-checkbox-item">
                <a-checkbox
                  v-model:checked="showZero"
                  @change="handleSearch"
                >
                  显示为0科目
                </a-checkbox>
              </div>
              <div class="search-item">
                <a-space :size="4">
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
                </a-space>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 左右对照表（6 列 + 序号列内置列配置齿轮） ═══ -->
        <template #table>
          <div class="table-area">
            <div
              class="balance-bar"
              :class="balanceBarClass"
            >
              <span class="balance-item">
                <span class="balance-label">资产合计</span>
                <span class="balance-value">{{ formatMoney(report.assetEndTotal) }}</span>
              </span>
              <span class="balance-op">=</span>
              <span class="balance-item">
                <span class="balance-label">负债合计</span>
                <span class="balance-value">{{ formatMoney(report.liabilityEndTotal) }}</span>
              </span>
              <span class="balance-op">+</span>
              <span class="balance-item">
                <span class="balance-label">所有者权益合计</span>
                <span class="balance-value">{{ formatMoney(report.equityEndTotal) }}</span>
              </span>
              <span class="balance-flag">
                <CheckCircleOutlined v-if="report.hasData && report.balanced" />
                <ExclamationCircleOutlined v-else-if="report.hasData" />
                {{ balanceFlag }}
              </span>
            </div>

            <BillTableList
              :columns="columns"
              :data-source="rows"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :min-empty-rows="0"
              storage-key="finance-balance-report-columns"
              row-key="rowNo"
            >
              <!-- 资产侧 -->
              <template #assetItemCell="{ record }">
                <span :class="['item-name', { 'is-total': record.assetTotalRow }]">
                  {{ record.assetItemName || '' }}
                </span>
              </template>
              <template #assetBeginCell="{ record }">
                <span
                  v-if="record.assetItemName"
                  :class="['num-value', { 'is-total': record.assetTotalRow }]"
                >{{ formatMoney(record.assetBeginBalance) }}</span>
              </template>
              <template #assetEndCell="{ record }">
                <a
                  v-if="record.assetSubjectId && !record.assetTotalRow"
                  class="num-link"
                  :title="`穿透明细账：${record.assetSubjectCode || ''}`"
                  @click="goDetailLedger(record, 'asset')"
                >{{ formatMoney(record.assetEndBalance) }}</a>
                <span
                  v-else-if="record.assetItemName"
                  :class="['num-value', { 'is-total': record.assetTotalRow }]"
                >{{ formatMoney(record.assetEndBalance) }}</span>
              </template>

              <!-- 负债及权益侧 -->
              <template #liabilityItemCell="{ record }">
                <span :class="['item-name', { 'is-total': record.liabilityTotalRow }]">
                  {{ record.liabilityItemName || '' }}
                </span>
              </template>
              <template #liabilityBeginCell="{ record }">
                <span
                  v-if="record.liabilityItemName"
                  :class="['num-value', { 'is-total': record.liabilityTotalRow }]"
                >{{ formatMoney(record.liabilityBeginBalance) }}</span>
              </template>
              <template #liabilityEndCell="{ record }">
                <a
                  v-if="record.liabilitySubjectId && !record.liabilityTotalRow"
                  class="num-link"
                  :title="`穿透明细账：${record.liabilitySubjectCode || ''}`"
                  @click="goDetailLedger(record, 'liability')"
                >{{ formatMoney(record.liabilityEndBalance) }}</a>
                <span
                  v-else-if="record.liabilityItemName"
                  :class="['num-value', { 'is-total': record.liabilityTotalRow }]"
                >{{ formatMoney(record.liabilityEndBalance) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="finance-balance-report"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import {
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { reportApi } from '@/api/finance'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'FinanceBalanceReport' })

const router = useRouter()

// ═══ 查询条件 ═══
const queryScheme = ref('')
const periodMode = ref<'single' | 'multi'>('single')
const endMonth = ref<Dayjs>(dayjs())
const startMonth = ref<Dayjs>(dayjs())
const subjectLevel = ref(1)
const showZero = ref(true)

const LEVEL_OPTIONS = [1, 2, 3, 4].map(v => ({ label: `${v}级`, value: v }))

// ═══ 数据 ═══
const loading = ref(false)
const rows = ref<any[]>([])
const report = ref<any>(emptyReport())

function emptyReport() {
  return {
    rows: [],
    assetBeginTotal: 0,
    assetEndTotal: 0,
    liabilityBeginTotal: 0,
    liabilityEndTotal: 0,
    equityBeginTotal: 0,
    equityEndTotal: 0,
    liabilityEquityBeginTotal: 0,
    liabilityEquityEndTotal: 0,
    balanced: false,
    difference: 0,
    hasData: false,
  }
}

// ═══ 列定义（对标 6 列全部默认显示；序号列内置列配置齿轮） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '资产', field: 'assetItemName', key: 'assetItemName', width: 160, type: 'slot', slotName: 'assetItemCell' },
  { title: '年初余额', field: 'assetBeginBalance', key: 'assetBeginBalance', width: 140, align: 'right', type: 'slot', slotName: 'assetBeginCell' },
  { title: '资产期末余额', field: 'assetEndBalance', key: 'assetEndBalance', width: 150, align: 'right', type: 'slot', slotName: 'assetEndCell' },
  { title: '负债及权益', field: 'liabilityItemName', key: 'liabilityItemName', width: 180, type: 'slot', slotName: 'liabilityItemCell' },
  { title: '年初余额', field: 'liabilityBeginBalance', key: 'liabilityBeginBalance', width: 140, align: 'right', type: 'slot', slotName: 'liabilityBeginCell' },
  { title: '期末余额', field: 'liabilityEndBalance', key: 'liabilityEndBalance', width: 150, align: 'right', type: 'slot', slotName: 'liabilityEndCell' },
]

// ═══ 平衡校验条状态 ═══
const balanceFlag = computed(() => {
  if (loading.value) return '加载中…'
  if (!report.value.hasData) return '暂无科目余额数据'
  return report.value.balanced ? '平衡' : `不平衡，差额 ${formatMoney(report.value.difference)}`
})

const balanceBarClass = computed(() => {
  if (loading.value || !report.value.hasData) return 'is-empty'
  return report.value.balanced ? 'is-balanced' : 'is-unbalanced'
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      fiscalYear: endMonth.value.year(),
      fiscalPeriod: endMonth.value.month() + 1,
      periodMode: periodMode.value,
      subjectLevel: subjectLevel.value,
      showZero: showZero.value,
    }
    if (periodMode.value === 'multi') {
      params.startPeriod = startMonth.value.month() + 1
    }
    const res: any = await reportApi.getBalanceSheetReport(params)
    const body = res?.data ?? res
    report.value = { ...emptyReport(), ...(body || {}) }
    rows.value = Array.isArray(body?.rows) ? body.rows : []
  } catch (error: any) {
    console.warn('[资产负债表] 获取数据失败', error)
    message.error(error?.response?.data?.message || '获取资产负债表失败')
    report.value = emptyReport()
    rows.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleReset() {
  periodMode.value = 'single'
  endMonth.value = dayjs()
  startMonth.value = dayjs()
  subjectLevel.value = 1
  showZero.value = true
  fetchData()
}

function handleRefresh() {
  fetchData()
}

// ═══ 期末余额穿透明细账（科目 + 本年度至期止月区间） ═══
function goDetailLedger(record: any, side: 'asset' | 'liability') {
  const subjectId = side === 'asset' ? record?.assetSubjectId : record?.liabilitySubjectId
  if (!subjectId) return
  router.push({
    path: '/finance/detail-ledger',
    query: {
      subjectId: String(subjectId),
      dateStart: dayjs(`${endMonth.value.year()}-01-01`).format('YYYY-MM-DD'),
      dateEnd: endMonth.value.endOf('month').format('YYYY-MM-DD'),
    },
  })
}

// ═══ 打印(F8) ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'finance-balance-report',
  title: '余额报表',
  columns: () => columns,
  rows: () => rows.value,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrintF8()
  }
}

// ═══ 导出（CSV，左右对照 6 列 + 序号） ═══
function handleExport() {
  if (rows.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const header = ['序号', '资产', '年初余额', '资产期末余额', '负债及权益', '年初余额', '期末余额']
  const lines = rows.value.map(r => [
    r.rowNo,
    r.assetItemName || '',
    r.assetItemName ? Number(r.assetBeginBalance || 0).toFixed(2) : '',
    r.assetItemName ? Number(r.assetEndBalance || 0).toFixed(2) : '',
    r.liabilityItemName || '',
    r.liabilityItemName ? Number(r.liabilityBeginBalance || 0).toFixed(2) : '',
    r.liabilityItemName ? Number(r.liabilityEndBalance || 0).toFixed(2) : '',
  ].map(escape).join(','))
  const csv = '\uFEFF' + [header.map(escape).join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `资产负债表_${endMonth.value.format('YYYY-MM')}_${dayjs().format('HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 格式化 ═══
function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[资产负债表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
.query-scheme-wrap { display: inline-flex; align-items: center; gap: 8px; margin-right: 8px; }
.list-title { font-size: 15px; font-weight: 600; color: #303133; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-picker),
.search-item :deep(.ant-select) { font-size: 13px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.search-checkbox-item { padding: 0 4px; }
.search-checkbox-item :deep(.ant-checkbox-wrapper) { font-size: 13px; }

.table-area { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }

/* ── 平衡校验条（P0 红线：资产合计 = 负债合计 + 所有者权益合计） ── */
.balance-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  font-size: 13px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.balance-bar.is-empty { background: #fafafa; color: #8c8c8c; }
.balance-bar.is-balanced { background: #f6ffed; color: #389e0d; }
.balance-bar.is-unbalanced { background: #fff2f0; color: #cf1322; }
.balance-item { display: inline-flex; align-items: center; gap: 6px; }
.balance-label { color: inherit; opacity: 0.75; }
.balance-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
.balance-op { font-weight: 600; opacity: 0.6; }
.balance-flag { display: inline-flex; align-items: center; gap: 4px; margin-left: 8px; font-weight: 600; }

.item-name { font-size: 13px; }
.item-name.is-total,
.num-value.is-total { font-weight: 700; }
.num-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
.num-link {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #1677ff;
  cursor: pointer;
  text-decoration: underline dotted;
}
.num-link:hover { color: #4096ff; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }

/* ── 打印(F8)：只保留报表主体 ── */
@media print {
  .search-area { display: none !important; }
  .balance-bar { background: transparent !important; color: #000 !important; }
  :deep(.toolbar-section),
  :deep(.tab-bar) { display: none !important; }
}
</style>
