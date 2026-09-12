<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 / 明细对账（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-button
              size="small"
              type="primary"
              :disabled="selectedRows.length === 0"
              @click="handleDetailReconcile"
            >
              <PartitionOutlined /> 明细对账
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（固定布局） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <a-select
                  v-model:value="schemeName"
                  style="width: 150px"
                  size="small"
                  allow-clear
                  placeholder="--查询方案--"
                  @change="applyScheme"
                >
                  <a-select-option
                    v-for="scheme in schemes"
                    :key="scheme.name"
                    :value="scheme.name"
                  >{{ scheme.name }}</a-select-option>
                </a-select>
                <a-button size="small" @click="saveSchemeModal = true">保存方案</a-button>
              </div>
              <div class="search-item">
                <span class="search-label">会计月</span>
                <a-select
                  v-model:value="searchParams.periodStart"
                  style="width: 118px"
                  size="small"
                  @change="handleSearch"
                >
                  <a-select-option
                    v-for="opt in periodOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</a-select-option>
                </a-select>
                <span class="search-label">至</span>
                <a-select
                  v-model:value="searchParams.periodEnd"
                  style="width: 118px"
                  size="small"
                  @change="handleSearch"
                >
                  <a-select-option
                    v-for="opt in periodOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">科目层级</span>
                <a-select
                  v-model:value="searchParams.subjectLevel"
                  style="width: 90px"
                  size="small"
                  @change="handleSearch"
                >
                  <a-select-option
                    v-for="lv in levelOptions"
                    :key="lv"
                    :value="lv"
                  >{{ lv }}</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <a-checkbox
                  v-model:checked="searchParams.hideNoAmount"
                  @change="handleSearch"
                >无发生额不显示本期合计</a-checkbox>
              </div>
              <div class="search-item">
                <a-checkbox
                  v-model:checked="searchParams.showYearAccum"
                  @change="handleSearch"
                >显示本年累计</a-checkbox>
              </div>
              <div class="search-item">
                <a-checkbox
                  v-model:checked="searchParams.showCurrentTotal"
                  @change="handleSearch"
                >显示当前总计</a-checkbox>
              </div>
              <div class="search-item">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（8 列，列配置齿轮在 rowNo 表头） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :min-empty-rows="20"
              storage-key="finance-general-ledger-columns"
              row-key="rowKey"
              @selection-change="handleSelectionChange"
            >
              <template #subjectCodeCell="{ record }">
                <a
                  v-if="record.subjectCode"
                  class="subject-link"
                  @click="drillToDetail(record)"
                >{{ record.subjectCode }}</a>
                <span v-else>-</span>
              </template>
              <template #debitCell="{ record }">
                <span class="num-value">{{ formatMoney(record.debit) }}</span>
              </template>
              <template #creditCell="{ record }">
                <span class="num-value">{{ formatMoney(record.credit) }}</span>
              </template>
              <template #directionCell="{ record }">
                <a-tag
                  v-if="record.direction"
                  :color="directionColor(record.direction)"
                >{{ record.direction }}</a-tag>
                <span v-else>-</span>
              </template>
              <template #balanceCell="{ record }">
                <span class="num-value">{{ formatMoney(record.balance) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 保存查询方案 ═══ -->
      <a-modal
        v-model:open="saveSchemeModal"
        title="保存查询方案"
        :width="420"
        @ok="handleSaveScheme"
      >
        <a-input
          v-model:value="newSchemeName"
          placeholder="请输入方案名称"
          :maxlength="20"
          @press-enter="handleSaveScheme"
        />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
  PartitionOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { ledgerApi, accountingPeriodApi } from '@/api/finance'

const router = useRouter()

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const periodOptions = ref<Array<{ label: string; value: string }>>([])
const levelOptions = ref<number[]>([1])
const selectedRows = ref<any[]>([])

// ═══ 查询参数（会计月缺省：当前会计月） ═══
const currentPeriod = dayjs().format('YYYY-MM')
const searchParams = reactive<Record<string, any>>({
  periodStart: currentPeriod,
  periodEnd: currentPeriod,
  subjectLevel: 1,
  hideNoAmount: false,
  showYearAccum: false,
  showCurrentTotal: false,
})

// ═══ 查询方案（本地保存，个人级） ═══
const SCHEME_KEY = 'finance-general-ledger-schemes'
const schemes = ref<Array<{ name: string; params: Record<string, any> }>>([])
const schemeName = ref<string | undefined>(undefined)
const saveSchemeModal = ref(false)
const newSchemeName = ref('')

// ═══ 列定义：8 列数据列，全部默认显示（rowNo 表头内置列配置齿轮） ═══
const columns: any[] = [
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '科目编码', field: 'subjectCode', key: 'subjectCode', width: 110, fixed: 'left', type: 'slot', slotName: 'subjectCodeCell' },
  { title: '科目名称', field: 'subjectName', key: 'subjectName', width: 180, ellipsis: true },
  { title: '期间', field: 'period', key: 'period', width: 90, align: 'center' },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '借方', field: 'debit', key: 'debit', width: 130, align: 'right', type: 'slot', slotName: 'debitCell' },
  { title: '贷方', field: 'credit', key: 'credit', width: 130, align: 'right', type: 'slot', slotName: 'creditCell' },
  { title: '方向', field: 'direction', key: 'direction', width: 80, align: 'center', type: 'slot', slotName: 'directionCell' },
  { title: '余额', field: 'balance', key: 'balance', width: 140, align: 'right', type: 'slot', slotName: 'balanceCell' },
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function directionColor(direction: string): string {
  if (direction === '借') return 'blue'
  if (direction === '贷') return 'red'
  return 'default'
}

// ═══ 数据加载：总账（/erp/finance/ledger/general） ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      periodStart: searchParams.periodStart || undefined,
      periodEnd: searchParams.periodEnd || undefined,
      subjectLevel: searchParams.subjectLevel ?? 1,
      hideNoAmount: !!searchParams.hideNoAmount,
      showYearAccum: !!searchParams.showYearAccum,
      showCurrentTotal: !!searchParams.showCurrentTotal,
    }
    const res: any = await ledgerApi.getGeneral(params)
    const rows: any[] = res?.data || res || []
    // 行唯一键（同一科目多行：期初 + 各期发生）
    tableData.value = rows.map((row: any, index: number) => ({
      ...row,
      rowKey: `${row.subjectCode || 'TOTAL'}-${row.rowType}-${row.period}-${index}`,
    }))
    selectedRows.value = []
  } catch (error: any) {
    console.warn('[总账] 获取账簿失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

/** 会计月选项（会计期间字典）与科目层级选项 */
async function loadOptions() {
  try {
    const res: any = await accountingPeriodApi.getList()
    const list: any[] = res?.data || res || []
    periodOptions.value = list
      .filter((p: any) => p.periodCode)
      .map((p: any) => ({ label: p.periodCode, value: p.periodCode }))
  } catch (error) {
    console.warn('[总账] 会计期间选项加载失败', error)
  }
  if (periodOptions.value.length === 0) {
    periodOptions.value = [{ label: currentPeriod, value: currentPeriod }]
  }
  // 默认会计月不在字典中时补充，保证查询区间有效
  if (!periodOptions.value.some(opt => opt.value === currentPeriod)) {
    periodOptions.value.unshift({ label: currentPeriod, value: currentPeriod })
  }
  try {
    const res: any = await ledgerApi.getLevels()
    const levels: number[] = (res?.data || res || []).map((v: any) => Number(v)).filter((v: number) => v > 0)
    levelOptions.value = levels.length > 0 ? levels : [1]
  } catch (error) {
    console.warn('[总账] 科目层级选项加载失败', error)
  }
  if (!levelOptions.value.includes(1)) {
    levelOptions.value = [1, ...levelOptions.value].sort((a, b) => a - b)
  }
}

// ═══ 事件处理 ═══
function handleSearch() {
  fetchData()
}
function handleRefresh() {
  fetchData()
}
function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

// ═══ 科目穿透：总账 → 明细账 ═══
function handleDetailReconcile() {
  const row = selectedRows.value[0]
  if (!row) {
    message.warning('请先选择科目行')
    return
  }
  drillToDetail(row)
}

function drillToDetail(record: any) {
  if (!record?.subjectCode) {
    message.warning('当前行无科目，无法下钻明细账')
    return
  }
  router.push({
    path: '/finance/detail-ledger',
    query: {
      subjectId: record.subjectId,
      subjectCode: record.subjectCode,
      dateStart: periodToDateStart(searchParams.periodStart),
      dateEnd: periodToDateEnd(searchParams.periodEnd),
    },
  })
}

function periodToDateStart(period: string | undefined): string | undefined {
  if (!period) return undefined
  return dayjs(`${period}-01`).format('YYYY-MM-DD')
}
function periodToDateEnd(period: string | undefined): string | undefined {
  if (!period) return undefined
  return dayjs(`${period}-01`).endOf('month').format('YYYY-MM-DD')
}

// ═══ 打印（F8） ═══
function handlePrintF8() {
  if (tableData.value.length === 0) {
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
onMounted(() => window.addEventListener('keydown', handleF8Key))
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))

// ═══ 导出 CSV ═══
function handleExport() {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['科目编码', '科目名称', '期间', '摘要', '借方', '贷方', '方向', '余额']
  const rows = tableData.value.map((r: any) => [
    csvCell(r.subjectCode),
    csvCell(r.subjectName),
    csvCell(r.period),
    csvCell(r.summary),
    r.debit ?? '',
    r.credit ?? '',
    csvCell(r.direction),
    r.balance ?? '',
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `总账_${new Date().toISOString().slice(0, 19).replace(/[-T:]/g, '')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function csvCell(value: any): string {
  const text = value === null || value === undefined ? '' : String(value)
  return text.includes(',') || text.includes('"') ? `"${text.replace(/"/g, '""')}"` : text
}

// ═══ 查询方案 ═══
function loadSchemes() {
  try {
    const raw = localStorage.getItem(SCHEME_KEY)
    schemes.value = raw ? JSON.parse(raw) : []
  } catch {
    schemes.value = []
  }
}
function applyScheme(name?: string) {
  if (!name) return
  const scheme = schemes.value.find(s => s.name === name)
  if (!scheme) return
  Object.assign(searchParams, scheme.params)
  fetchData()
}
function handleSaveScheme() {
  const name = newSchemeName.value.trim()
  if (!name) {
    message.warning('请输入方案名称')
    return
  }
  const params = { ...searchParams }
  const existing = schemes.value.find(s => s.name === name)
  if (existing) {
    existing.params = params
  } else {
    schemes.value.push({ name, params })
  }
  localStorage.setItem(SCHEME_KEY, JSON.stringify(schemes.value))
  schemeName.value = name
  newSchemeName.value = ''
  saveSchemeModal.value = false
  message.success('查询方案已保存')
}

function handleError(error: Error) {
  console.error('[总账] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  loadSchemes()
  await loadOptions()
  fetchData()
})
</script>

<style scoped>
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-item :deep(.ant-input),
.search-item :deep(.ant-select) {
  font-size: 13px;
}
.search-item :deep(.ant-checkbox-wrapper) {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.subject-link {
  color: #1677ff;
  cursor: pointer;
}
.subject-link:hover {
  text-decoration: underline;
}
.num-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
