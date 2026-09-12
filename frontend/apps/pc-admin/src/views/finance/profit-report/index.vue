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
            <span class="list-title">利润表</span>
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">
                --查询方案--
              </a-select-option>
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
                  <a-radio-button value="single">
                    单会计月
                  </a-radio-button>
                  <a-radio-button value="multi">
                    多会计月
                  </a-radio-button>
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

        <!-- ═══ 纵向报表（3 列 + 序号列内置列配置齿轮） ═══ -->
        <template #table>
          <div class="table-area">
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
              storage-key="finance-profit-report-columns"
              row-key="rowNo"
            >
              <template #itemNameCell="{ record }">
                <span
                  class="item-name"
                  :class="{ 'is-total': record.summaryRow }"
                  :style="{ paddingLeft: `${(record.indentLevel || 0) * 18}px` }"
                >{{ record.itemName || '' }}</span>
              </template>
              <template #currentAmountCell="{ record }">
                <span
                  class="num-value"
                  :class="{ 'is-total': record.summaryRow, 'is-negative': isNegative(record.currentAmount) }"
                >{{ formatMoney(record.currentAmount) }}</span>
              </template>
              <template #cumulativeAmountCell="{ record }">
                <span
                  class="num-value"
                  :class="{ 'is-total': record.summaryRow, 'is-negative': isNegative(record.cumulativeAmount) }"
                >{{ formatMoney(record.cumulativeAmount) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import {
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { reportApi } from '@/api/finance'

defineOptions({ name: 'FinanceProfitReport' })

// ═══ 查询条件（对标 5 项） ═══
const queryScheme = ref('')
const periodMode = ref<'single' | 'multi'>('single')
const endMonth = ref<Dayjs>(dayjs())
const startMonth = ref<Dayjs>(dayjs())
const subjectLevel = ref(2)
const showZero = ref(false)

const LEVEL_OPTIONS = [1, 2, 3, 4].map(v => ({ label: `${v}级`, value: v }))

// ═══ 数据 ═══
const loading = ref(false)
const rows = ref<any[]>([])

// ═══ 列定义（对标 3 列全部默认显示；序号列内置列配置齿轮） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '项目', field: 'itemName', key: 'itemName', width: 360, type: 'slot', slotName: 'itemNameCell' },
  { title: '本期发生额', field: 'currentAmount', key: 'currentAmount', width: 180, align: 'right', type: 'slot', slotName: 'currentAmountCell' },
  { title: '本年累计', field: 'cumulativeAmount', key: 'cumulativeAmount', width: 180, align: 'right', type: 'slot', slotName: 'cumulativeAmountCell' },
]

// ═══ 数据加载（科目层级驱动） ═══
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
    const res: any = await reportApi.getIncomeStatementReport(params)
    const body = res?.data ?? res
    rows.value = Array.isArray(body?.rows) ? body.rows : []
  } catch (error: any) {
    console.warn('[利润表] 获取数据失败', error)
    message.error(error?.response?.data?.message || '获取利润表失败')
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
  subjectLevel.value = 2
  showZero.value = false
  fetchData()
}

function handleRefresh() {
  fetchData()
}

// ═══ 打印(F8) ═══
function handlePrintF8() {
  if (rows.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrintF8()
  }
}

// ═══ 导出（CSV，纵向 3 列 + 序号） ═══
function handleExport() {
  if (rows.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const header = ['序号', '项目', '本期发生额', '本年累计']
  const lines = rows.value.map(r => [
    r.rowNo,
    r.itemName || '',
    Number(r.currentAmount || 0).toFixed(2),
    Number(r.cumulativeAmount || 0).toFixed(2),
  ].map(escape).join(','))
  const csv = '\uFEFF' + [header.map(escape).join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `利润表_${endMonth.value.format('YYYY-MM')}_${dayjs().format('HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 格式化 ═══
function isNegative(val: any): boolean {
  return val !== null && val !== undefined && val !== '' && Number(val) < 0
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[利润表] 页面错误', error)
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

/* ── 报表项目行 ── */
.item-name { font-size: 13px; }
.item-name.is-total { font-weight: 700; }
.num-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
.num-value.is-total { font-weight: 700; }
/* 亏损/负数红字（本土报表惯例） */
.num-value.is-negative { color: #f5222d; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }

/* ── 打印(F8)：只保留报表主体 ── */
@media print {
  .search-area { display: none !important; }
  :deep(.toolbar-section),
  :deep(.tab-bar) { display: none !important; }
}
</style>
