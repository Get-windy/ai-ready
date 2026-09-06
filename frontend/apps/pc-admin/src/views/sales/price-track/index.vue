<template>
  <ErrorBoundary @error="handleError">
    <CategoryListLayout
      :show-category-panel="true"
      :category-tree-data="categoryTreeData"
      :category-loading="categoryLoading"
      :category-title="'商品分类'"
      :category-editable="false"
      :show-table-footer="true"
      @category-select="handleCategorySelect"
    >
      <template #toolbar-left>
        <a-space :size="4">
          <a-button v-for="q in dateShortcuts" :key="q.key" :type="activeShortcut === q.key ? 'primary' : 'default'" size="small" @click="pickShortcut(q.key)">{{ q.label }}</a-button>
        </a-space>
      </template>

      <template #toolbar-right>
        <a-space :size="8">
          <a-button size="small" :loading="loading" @click="loadData"><ReloadOutlined /> 刷新</a-button>
          <a-button size="small" @click="showColumnConfig = true"><SettingOutlined /> 列配置</a-button>
          <a-dropdown>
            <a-button size="small">更多 <DownOutlined /></a-button>
            <template #overlay>
              <a-menu @click="handleMenuClick">
                <a-menu-item key="export"><ExportOutlined /> 导出</a-menu-item>
                <a-menu-item key="print"><PrinterOutlined /> 打印(F8)</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>

      <!-- ═══ 搜索区 ═══ -->
      <template #search-fields>
        <div class="search-area">
          <div class="search-grid" ref="gridRef">
            <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" value-format="YYYY-MM-DD" /></div>
            <div class="search-field-item"><a-input v-model:value="queryValues.partnerName" placeholder="往来单位" allow-clear size="small" /></div>
            <div class="search-field-item"><a-input v-model:value="queryValues.productName" placeholder="商品" allow-clear size="small" /></div>
            <div class="search-field-item">
              <a-select v-model:value="queryValues.unitType" placeholder="单位类型" allow-clear size="small" style="width:100%">
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="small">小单位</a-select-option>
                <a-select-option value="big">大单位</a-select-option>
              </a-select>
            </div>
            <div class="search-field-item"><a-checkbox v-model:checked="showSelected">仅显示已选中</a-checkbox></div>
            <div ref="actionRef" class="search-action-group" :style="{ gridColumn: 'span ' + actionSpan }"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
          </div>
        </div>
      </template>

      <!-- ═══ 数据表格 ═══ -->
      <template #table>
        <BillTableList
          :columns="visibleColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
          row-key="_rk"
          :scroll="{ x: 8000 }"
          @page-change="handlePageChange"
        >
          <template #recentPriceCell="{ record }">
            {{ Number(record.recentPrice ?? 0).toFixed(2) }}
          </template>
          <template #recentDiscountRateCell="{ record }">
            {{ Number(record.recentDiscountRate ?? 0).toFixed(2) }}
          </template>
        </BillTableList>
      </template>
    </CategoryListLayout>

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ReloadOutlined, SettingOutlined, DownOutlined, ExportOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { productCategoryApi } from '@/api/erp/product'
import { buildPriceLevelColumns } from '@/utils/priceLevelConfig'
import request from '@/utils/request'

defineOptions({ name: 'SalesPriceTrack' })

// ═══ 日期快捷 ═══
const dateShortcuts = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'thisWeek2', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]
const activeShortcut = ref('thisMonth')
const dateRange = ref<[string, string] | null>(null)

function calcRange(key: string): [string, string] {
  const now = dayjs()
  switch (key) {
    case 'yesterday': return [now.subtract(1, 'day').format('YYYY-MM-DD'), now.subtract(1, 'day').format('YYYY-MM-DD')]
    case 'today': return [now.format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisWeek': return [now.startOf('week').add(1, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisWeek2': return [now.subtract(6, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisMonth': return [now.startOf('month').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'lastMonth': return [now.subtract(1, 'month').startOf('month').format('YYYY-MM-DD'), now.subtract(1, 'month').endOf('month').format('YYYY-MM-DD')]
    case 'last3Month': return [now.subtract(2, 'month').startOf('month').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    case 'thisYear': return [now.startOf('year').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
    default: return [now.subtract(6, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
  }
}
function pickShortcut(key: string) {
  activeShortcut.value = key
  dateRange.value = calcRange(key)
  loadData()
}

// ═══ 查询条件 ═══
const queryValues = reactive({ partnerName: '', productName: '', unitType: '' })
const showSelected = ref(false)
const selectedCategoryId = ref<number | undefined>(undefined)

// ═══ 搜索区网格 ═══
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 商品分类树（真实数据） ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const tree = await productCategoryApi.getTree()
    categoryTreeData.value = tree || []
  } catch {
    categoryTreeData.value = []
  } finally {
    categoryLoading.value = false
  }
}
function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys?.[0] ? Number(keys[0]) : undefined
  handleSearch()
}

// ═══ 表格列（13 标准列 + 8 价格等级列） ═══
const columnDefs = computed<any[]>(() => [
  { title: '', field: 'rowNo', key: 'rowNo', width: 44, type: 'rowNo', fixed: 'left' },
  { title: '商品名称', field: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 90 },
  { title: '商品单位', field: 'unit', key: 'unit', width: 70 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 90, ellipsis: true },
  { title: '型号', field: 'model', key: 'model', width: 90 },
  { title: '产地', field: 'origin', key: 'origin', width: 90 },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: '往来单位名称', field: 'partnerName', key: 'partnerName', width: 160, ellipsis: true },
  { title: '最近销售价', field: 'recentPrice', key: 'recentPrice', width: 100, align: 'right', type: 'slot', slotName: 'recentPriceCell' },
  { title: '最近销售折扣（%）', field: 'recentDiscountRate', key: 'recentDiscountRate', width: 130, align: 'right', type: 'slot', slotName: 'recentDiscountRateCell' },
  { title: '最近销售日期', field: 'recentSaleDate', key: 'recentSaleDate', width: 110 },
  { title: '最后修改时间', field: 'lastUpdateTime', key: 'lastUpdateTime', width: 140 },
  // 8 个标准化价格等级（共享 buildPriceLevelColumns，默认隐藏，可在列配置显示）
  ...buildPriceLevelColumns('input'),
])
const { onSettingChange, resetSettings, settingsColumns, visibleColumns } = useColumnConfig(columnDefs.value, 'sales-price-track-list-columns')
function handleColumnConfigChange() { onSettingChange() }
function handleColumnConfigReset() { resetSettings() }

// ═══ 数据状态 ═══
const tableData = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 数据加载 ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/sales/price-track/recent-price/page', {
      params: {
        current: pagination.current,
        size: pagination.pageSize,
        productName: queryValues.productName || undefined,
        partnerName: queryValues.partnerName || undefined,
        unitType: queryValues.unitType || undefined,
        categoryId: selectedCategoryId.value,
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1],
      },
    })
    const page: any = res?.records ? res : { records: Array.isArray(res) ? res : [], total: res?.total }
    const records = (page.records || []) as any[]
    tableData.value = records.map((r: any, i: number) => ({ ...r, _rk: `${r.productId}-${r.partnerId}-${i}` }))
    pagination.total = Number(page.total) || 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}
function handleSearch() {
  pagination.current = 1
  loadData()
}
function handlePageChange(p: { current?: number; pageSize?: number }) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 20
  loadData()
}
function handleMenuClick({ key }: { key: string }) {
  if (key === 'export') handleExport()
  else if (key === 'print') window.print()
}

// ═══ 导出 ═══
function handleExport() {
  const cols = visibleColumns.value.filter((c: any) => c && c.key !== 'rowNo' && c.key !== 'action')
  const header = cols.map((c: any) => c.title).join(',')
  const lines = tableData.value.map((r: any) => cols.map((c: any) => r[c.key] ?? '').join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `销售价格跟踪-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function handleError(err: any) { console.warn('[销售价格跟踪] ErrorBoundary:', err) }

onMounted(() => {
  dateRange.value = calcRange('thisMonth')
  loadCategoryTree()
  loadData()
})
</script>

<style scoped>
.search-area { display: flex; flex-direction: column; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 130px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; min-width: 170px; }
</style>
