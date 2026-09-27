<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="true"
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeysTree"
        :show-table-footer="false"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeysTree = keys)"
      >
        <!-- ═══ 工具栏右侧：主操作 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button type="primary" @click="openAddModal">
              <PlusOutlined /> 新增
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-dropdown>
              <a-button size="small">更多 <DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item :disabled="selectedRowIds.length === 0" @click="handleBatchDelete">
                    <DeleteOutlined /> 批量删除
                  </a-menu-item>
                  <a-menu-item @click="handleImport"><ImportOutlined /> 导入</a-menu-item>
                  <a-menu-item @click="handleExport"><ExportOutlined /> 导出</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区：日期快捷 + 筛选 + 查询 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="date-shortcuts">
              <a
                v-for="s in dateShortcuts"
                :key="s.key"
                :class="['date-shortcut', { active: activeShortcut === s.key }]"
                @click="pickShortcut(s.key)"
              >
                {{ s.label }}
              </a>
            </div>
            <div class="search-grid">
              <a-range-picker
                v-model:value="dateRange"
                size="small"
                style="width: 220px"
                value-format="YYYY-MM-DD"
                @change="handleSearch"
              />
              <a-input
                v-model:value="queryValues.partnerName"
                placeholder="往来单位"
                allow-clear
                size="small"
                style="width: 150px"
                @press-enter="handleSearch"
              />
              <a-input
                v-model:value="queryValues.productName"
                placeholder="商品"
                allow-clear
                size="small"
                style="width: 150px"
                @press-enter="handleSearch"
              />
              <a-select
                v-model:value="queryValues.unitType"
                placeholder="单位类型"
                allow-clear
                size="small"
                style="width: 120px"
                :options="unitOptions"
                @change="handleSearch"
              />
              <a-button type="primary" size="small" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-checkbox v-model:checked="showSelected">仅显示已选中</a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillDetailTable
            :columns="columns"
            :data-source="visibleTableData"
            :loading="loading"
            :view-mode="true"
            :storage-key="storageKey"
            :show-pagination="true"
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            @page-change="handlePageChange"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
          >
            <template #actionCell="{ record }">
              <a-space :size="2">
                <a-button type="link" size="small" @click="openEditModal(record)">修改</a-button>
                <a-button type="link" size="small" danger @click="handleRowDelete(record)">删除</a-button>
                <a-button type="link" size="small" @click="openTrend(record)">
                  <LineChartOutlined /> 趋势
                </a-button>
              </a-space>
            </template>
            <template #purchasePriceCell="{ record }">
              {{ formatMoney(record.purchasePrice) }}
            </template>
            <template #purchaseDateCell="{ record }">
              {{ formatDate(record.purchaseDate) }}
            </template>
            <template #lastModifyTimeCell="{ record }">
              {{ formatDate(record.lastModifyTime) }}
            </template>
          </BillDetailTable>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 新增 / 修改：价格折扣弹窗 ═══ -->
    <a-modal
      v-model:open="showFormModal"
      :title="formMode === 'edit' ? '编辑价格' : '价格折扣'"
      :width="560"
      :confirm-loading="saving"
      :ok-text="formMode === 'edit' ? '保存' : '保存(Enter)'"
      :cancel-text="'关闭(Esc)'"
      @ok="handleFormSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="往来单位" name="partnerId">
          <a-select
            v-model:value="form.partnerId"
            placeholder="搜索并选择往来单位"
            show-search
            allow-clear
            :filter-option="false"
            :options="partnerOptions"
            :loading="partnerSearching"
            @search="searchPartner"
            @change="handlePartnerChange"
          >
            <template #option="{ value, label, partnerName, partnerCode }">
              <span>{{ partnerName || label }}</span>
              <span style="color:#999;margin-left:6px">{{ partnerCode }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="商品" name="productId">
          <ProductSelector
            mode="select"
            value-key="id"
            @change="handleProductChange"
          />
        </a-form-item>
        <a-form-item label="单位" name="unit">
          <a-select
            v-model:value="form.unit"
            placeholder="单位"
            allow-clear
            :options="unitOptions"
            show-search
          />
        </a-form-item>
        <a-form-item label="采购价格" name="purchasePrice">
          <a-input-number
            v-model:value="form.purchasePrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入采购价格"
          />
        </a-form-item>
        <a-form-item label="采购日期" name="purchaseDate">
          <a-date-picker
            v-model:value="form.purchaseDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
            :placeholder="dayjs().format('YYYY-MM-DD')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 趋势弹窗：价格变动折线图 ═══ -->
    <a-modal
      v-model:open="showTrendModal"
      :title="`采购价格趋势 - ${trendProductName || ''}`"
      :width="720"
      :footer="null"
      :destroy-on-close="true"
    >
      <ARReportChart
        title="价格变动趋势"
        :option="trendOption"
        :loading="trendLoading"
        :height="340"
        empty-text="暂无价格变动记录"
      />
      <div v-if="trendData.length" class="trend-hint">
        <span class="trend-hint-dot" />鼠标悬停折线点可查看最近改价成交供应商
      </div>
    </a-modal>

    <!-- ═══ 打印：结果集打印（打的是当前筛选出的这批价格，不是某一张单据）═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="purchase-price-track"
      :print-data="printData"
    />

    <!-- ═══ 导入隐藏文件输入 ═══ -->
    <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportFileChange" />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, DownOutlined,
  DeleteOutlined, ExportOutlined, ImportOutlined, SearchOutlined, LineChartOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelector from '@/components/business/ProductSelector/ProductSelector.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import request from '@/utils/request'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'
import * as XLSX from 'xlsx'

defineOptions({ name: 'PurchasePriceTrack' })

const storageKey = 'purchase-price-track-columns'
const handleError = (e: any) => console.warn('[采购价格跟踪] ErrorBoundary:', e)

// ═══ 日期快捷 ═══
const dateShortcuts = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'recentWeek', label: '近一周' },
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
    case 'recentWeek': return [now.subtract(6, 'day').format('YYYY-MM-DD'), now.format('YYYY-MM-DD')]
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
  handleSearch()
}

// ═══ 查询条件 ═══
const queryValues = reactive({
  partnerName: '',
  productName: '',
  unitType: undefined as string | undefined,
})
const showSelected = ref(false)

// ═══ 商品分类树 ═══
const categoryLoading = ref(false)
const categoryTreeData = ref<any[]>([])
const expandedKeysTree = ref<(string | number)[]>([])
const selectedCategoryId = ref<string | number>('__all__')

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await request.get('/erp/product-category/tree')
    const tree = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: tree }]
    expandedKeysTree.value = ['__all__', ...tree.filter((c: any) => c.children?.length).map((c: any) => c.id)]
  } catch (e) {
    console.warn('[采购价格跟踪] 分类树加载失败', e)
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: [] }]
  } finally {
    categoryLoading.value = false
  }
}
function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : '__all__'
  handleSearch()
}

// ═══ 单位类型选项（动态去重 + 兜底常用单位） ═══
const COMMON_UNITS = ['件', '箱', '千克', 'kg', '罐', '个', '瓶', '包', '袋', '台', '套']
const unitOptions = computed(() => {
  const fromData = Array.from(new Set(tableData.value.map((r: any) => r.unit).filter(Boolean))) as string[]
  return Array.from(new Set([...COMMON_UNITS, ...fromData])).map(u => ({ label: u, value: u }))
})

// ═══ 表格列（11 列 + 序号/选择/操作） ═══
const columns: any[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { key: 'action', title: '操作', type: 'action', width: 175, fixed: 'right', slotName: 'actionCell' },
  { key: 'productName', title: '商品名称', width: 190, ellipsis: true },
  { key: 'itemCode', title: '货号', width: 130 },
  { key: 'unit', title: '商品单位', width: 90 },
  { key: 'specification', title: '规格', width: 120, ellipsis: true },
  { key: 'model', title: '型号', width: 90 },
  { key: 'origin', title: '产地', width: 90 },
  { key: 'partnerCode', title: '往来单位编号', width: 120 },
  { key: 'partnerName', title: '往来单位名称', width: 170, ellipsis: true },
  { key: 'purchasePrice', title: '最近采购价', width: 120, align: 'right', slotName: 'purchasePriceCell' },
  { key: 'purchaseDate', title: '最近采购日期', width: 120, slotName: 'purchaseDateCell' },
  { key: 'lastModifyTime', title: '最后修改时间', width: 160, slotName: 'lastModifyTimeCell' },
]

// ═══ 数据状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRowIds = ref<any[]>([])

// 仅显示已选中：对当前页过滤
const visibleTableData = computed(() => {
  if (!showSelected.value) return tableData.value
  const set = new Set(selectedRowIds.value)
  return tableData.value.filter((r: any) => set.has(r.id))
})

// ═══ 格式化 ═══
function formatMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatDate(v: any): string {
  if (!v) return '-'
  const s = String(v)
  return s.length >= 10 ? s.slice(0, 10) : s
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      productName: queryValues.productName || undefined,
      partnerName: queryValues.partnerName || undefined,
      unitType: queryValues.unitType || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    }
    if (selectedCategoryId.value && selectedCategoryId.value !== '__all__') {
      params.categoryId = selectedCategoryId.value
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === '') delete params[k] })

    const res: any = await request.get('/purchase/price-track/page', { params })
    const page: any = res?.records ? res : { records: Array.isArray(res) ? res : [], total: res?.total }
    tableData.value = page.records || []
    pagination.total = Number(page.total) || 0
  } catch (e) {
    console.warn('[采购价格跟踪] 加载失败', e)
    message.error('查询失败，请检查网络后重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handlePageChange(p: { page: number; pageSize: number }) {
  pagination.current = p.page
  pagination.pageSize = p.pageSize
  fetchData()
}

// ═══ 行选择 ═══
function handleRowCheck(record: any, _index: number, checked: boolean) {
  const id = record.id
  if (checked) { if (!selectedRowIds.value.includes(id)) selectedRowIds.value.push(id) }
  else { selectedRowIds.value = selectedRowIds.value.filter((k: any) => k !== id) }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  selectedRowIds.value = checked ? records.map((r: any) => r.id) : []
}

// ═══ 新增 / 修改弹窗 ═══
const showFormModal = ref(false)
const saving = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<any>(null)
const form = reactive<any>({
  id: undefined,
  partnerId: undefined,
  partnerCode: undefined,
  partnerName: undefined,
  productId: undefined,
  productCode: undefined,
  productName: undefined,
  itemCode: undefined,
  unit: undefined,
  specification: undefined,
  model: undefined,
  origin: undefined,
  purchasePrice: undefined,
  purchaseDate: dayjs().format('YYYY-MM-DD'),
})
const formRules = {
  partnerId: [{ required: true, message: '请选择往来单位', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  purchasePrice: [{ required: true, message: '请输入采购价格', trigger: 'blur' }],
}
const partnerOptions = ref<any[]>([])
const partnerSearching = ref(false)

function searchPartner(keyword: string) {
  partnerSearching.value = true
  request.get('/erp/party/search', { params: { keyword: keyword || '' } })
    .then((res: any) => {
      const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
      partnerOptions.value = (list || []).map((p: any) => ({
        value: p.id,
        label: p.partyName,
        partnerName: p.partyName,
        partnerCode: p.partyCode,
      }))
    })
    .catch(() => { partnerOptions.value = [] })
    .finally(() => { partnerSearching.value = false })
}
function handlePartnerChange(v: any) {
  const opt = partnerOptions.value.find(o => o.value === v)
  if (opt) {
    form.partnerName = opt.partnerName
    form.partnerCode = opt.partnerCode
  }
}
function handleProductChange(product: any) {
  if (product) {
    form.productId = product.id
    form.productName = product.productName || product.name
    form.productCode = product.productCode || ''
    form.itemCode = product.productCode || ''
    if (product.unit) form.unit = product.unit
    form.specification = product.spec || ''
    form.model = product.model || ''
    form.origin = product.origin || ''
  }
}

function openAddModal() {
  formMode.value = 'add'
  Object.assign(form, {
    id: undefined,
    partnerId: undefined, partnerCode: undefined, partnerName: undefined,
    productId: undefined, productCode: undefined, productName: undefined,
    itemCode: undefined, unit: undefined, specification: undefined,
    model: undefined, origin: undefined,
    purchasePrice: undefined,
    purchaseDate: dayjs().format('YYYY-MM-DD'),
  })
  partnerOptions.value = []
  showFormModal.value = true
}
function openEditModal(record: any) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: record.id,
    partnerId: record.partnerId,
    partnerCode: record.partnerCode || undefined,
    partnerName: record.partnerName || undefined,
    productId: record.productId,
    productCode: record.productCode || undefined,
    productName: record.productName || undefined,
    itemCode: record.itemCode || undefined,
    unit: record.unit || undefined,
    specification: record.specification || undefined,
    model: record.model || undefined,
    origin: record.origin || undefined,
    purchasePrice: record.purchasePrice,
    purchaseDate: record.purchaseDate || dayjs().format('YYYY-MM-DD'),
  })
  partnerOptions.value = record.partnerName ? [{ value: record.partnerId, label: record.partnerName, partnerName: record.partnerName, partnerCode: record.partnerCode }] : []
  showFormModal.value = true
}

async function handleFormSave() {
  try {
    await formRef.value?.validate()
  } catch { return }

  saving.value = true
  try {
    if (formMode.value === 'edit') {
      await request.put(`/purchase/price-track/${form.id}`, form)
      message.success('已保存')
    } else {
      await request.post('/purchase/price-track', form)
      message.success('已保存价格折扣')
    }
    showFormModal.value = false
    fetchData()
  } catch (e) {
    console.warn('[采购价格跟踪] 保存失败', e)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除 ═══
function handleRowDelete(record: any) {
  Modal.confirm({
    title: '删除价格',
    content: `确认删除「${record.productName}」在「${record.partnerName}」的价格记录吗？`,
    okText: '确认删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.delete(`/purchase/price-track/${record.id}`)
        message.success('已删除')
        fetchData()
      } catch { message.error('删除失败') }
    },
  })
}
function handleBatchDelete() {
  if (!selectedRowIds.value.length) { message.warning('请先勾选需要删除的记录'); return }
  Modal.confirm({
    title: '批量删除',
    content: `确认删除选中的 ${selectedRowIds.value.length} 条价格记录吗？`,
    okText: '确认删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post('/purchase/price-track/batch-delete', selectedRowIds.value)
        message.success('已批量删除')
        selectedRowIds.value = []
        fetchData()
      } catch { message.error('批量删除失败') }
    },
  })
}

// ═══ 趋势 ═══
const showTrendModal = ref(false)
const trendLoading = ref(false)
const trendData = ref<any[]>([])
const trendProductName = ref('')

async function openTrend(record: any) {
  trendProductName.value = record.productName
  showTrendModal.value = true
  trendLoading.value = true
  trendData.value = []
  try {
    const res: any = await request.get('/purchase/price-track/trend', { params: { productId: record.productId } })
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    trendData.value = (list || []).sort((a: any, b: any) => String(a.purchaseDate).localeCompare(String(b.purchaseDate)))
  } catch (e) {
    console.warn('[采购价格跟踪] 趋势加载失败', e)
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

const trendOption = computed(() => {
  const sorted = [...trendData.value].sort((a: any, b: any) => String(a.purchaseDate).localeCompare(String(b.purchaseDate)))
  const dates = sorted.map((r: any) => formatDate(r.purchaseDate))
  const prices = sorted.map((r: any) => (r.purchasePrice === null || r.purchasePrice === undefined ? null : Number(r.purchasePrice)))
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        if (!p) return ''
        const idx = p.dataIndex
        const record = sorted[idx] || {}
        const price = formatMoney(record.purchasePrice)
        return `日期：${formatDate(record.purchaseDate)}<br/>采购价：¥${price}<br/>成交供应商：${record.partnerName || '-'}`
      },
    },
    grid: { left: 70, right: 30, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '采购价(元)' },
    series: [{
      name: '采购价',
      type: 'line',
      smooth: true,
      symbolSize: 8,
      itemStyle: { color: '#fa541c' },
      data: prices,
    }],
  }
})

// ═══ 导入 ═══
const fileInputRef = ref<HTMLInputElement | null>(null)
function handleImport() {
  fileInputRef.value?.click()
}
async function handleImportFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const buf = await file.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const sheet = wb.Sheets[wb.SheetNames[0]]
    const rows: any[] = XLSX.utils.sheet_to_json(sheet, { defval: '' })
    if (!rows.length) { message.warning('文件中没有可导入的数据'); return }

    let ok = 0
    let fail = 0
    for (const row of rows) {
      const productName = row['商品名称'] || row['商品'] || ''
      const partnerName = row['往来单位名称'] || row['往来单位'] || ''
      const price = Number(row['采购价格']) || 0
      const date = row['采购日期'] ? String(row['采购日期']).slice(0, 10) : dayjs().format('YYYY-MM-DD')
      if (!productName || !partnerName || price <= 0) { fail += 1; continue }

      const product = await lookupProduct(productName)
      const partner = await lookupPartner(partnerName)
      if (!product || !partner) { fail += 1; continue }

      try {
        await request.post('/purchase/price-track', {
          productId: product.id,
          partnerId: partner.partnerId,
          unit: product.unit || row['单位'] || undefined,
          purchasePrice: price,
          purchaseDate: date,
        })
        ok += 1
      } catch { fail += 1 }
    }
    message.success(`导入完成：成功 ${ok} 条，失败 ${fail} 条`)
    fetchData()
  } catch (err) {
    console.warn('[采购价格跟踪] 导入失败', err)
    message.error('导入失败，请检查文件格式')
  } finally {
    input.value = ''
  }
}
async function lookupProduct(productName: string): Promise<any> {
  try {
    const res: any = await request.get('/erp/product/page', { params: { keyword: productName, current: 1, size: 5 } })
    const records = res?.data?.records || res?.records || []
    return records.find((r: any) => r.productName === productName) || records[0] || null
  } catch { return null }
}
async function lookupPartner(partnerName: string): Promise<any> {
  try {
    const res: any = await request.get('/erp/party/search', { params: { keyword: partnerName } })
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    const found = list.find((p: any) => p.partyName === partnerName) || list[0]
    return found ? { partnerId: found.id, partnerName: found.partyName } : null
  } catch { return null }
}

// ═══ 导出 ═══
function handleExport() {
  if (!tableData.value.length) { message.warning('没有可导出的数据'); return }
  const headers = ['商品名称', '货号', '商品单位', '规格', '型号', '产地', '往来单位编号', '往来单位名称', '最近采购价', '最近采购日期', '最后修改时间']
  const rows = tableData.value.map(r => [
    r.productName, r.itemCode, r.unit, r.specification, r.model, r.origin,
    r.partnerCode, r.partnerName, r.purchasePrice ?? 0, r.purchaseDate, formatDate(r.lastModifyTime),
  ])
  const csv = '\uFEFF' + [headers.join(','), ...rows.map(r => r.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `采购价格跟踪-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
// 现在交给 PrintDialog：列定义直接用页面自己的 columns（内部筛掉行号/勾选/操作列），
// 模板按数据里的列画表头，所以列改了不用去改模板。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'purchase-price-track',
  title: '采购价格跟踪',
  columns: () => columns,
  // 打的就是屏幕上这批（含「仅显示已选中」的过滤结果）；日期列在表格里是槽位格式化过的，打印按同一格式
  rows: () => visibleTableData.value.map((r: any) => ({
    ...r,
    purchaseDate: formatDate(r.purchaseDate),
    lastModifyTime: formatDate(r.lastModifyTime),
  })),
  emptyTip: '没有可打印的数据',
})

onMounted(() => {
  loadCategoryTree()
  const range = calcRange('thisMonth')
  dateRange.value = range
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.date-shortcuts { display: flex; gap: 6px; align-items: center; margin-bottom: 8px; flex-wrap: wrap; }
.date-shortcut {
  font-size: 12px; color: #606266; padding: 2px 8px; border-radius: 4px; cursor: pointer; background: #f5f5f5;
}
.date-shortcut:hover { color: #1890ff; background: #e6f4ff; }
.date-shortcut.active { background: #fff1f0; color: #fa541c; font-weight: 600; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-grid :deep(.ant-input-affix-wrapper),
.search-grid :deep(.ant-select) { font-size: 13px; }
.trend-hint {
  margin-top: 8px; font-size: 12px; color: #999; display: flex; align-items: center; gap: 6px;
}
.trend-hint-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #fa541c;
}
</style>
