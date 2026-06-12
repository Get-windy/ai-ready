<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 基础资料 / 批量价格管理</span>
          <h2 class="page-header__title">批量价格管理</h2>
        </div>
        <div class="page-header__right">
          <a-space :size="12">
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
              <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
            </span>
            <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchProducts)">
              <ReloadOutlined /> 刷新
            </a-button>
          
                <span class="shortcut-hints">
                  <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                </span>
          </a-space>
        </div>
      </div>

        <a-space>
          <a-button v-permission="'erp:product:batch-price'" v-if="!formulaCollapsed" type="primary" size="small" :disabled="selectedKeys.length === 0" @click.stop="applyFormula">
            <CheckOutlined /> 应用到选中行 ({{ selectedKeys.length }})
          </a-button>
          <CaretDownOutlined :class="{ 'rotate-180': formulaCollapsed }" style="transition: transform 0.2s;" />
        </a-space>
      </template>

      <!-- PageContainer 默认插槽内容 -->
      <div v-show="!formulaCollapsed" class="formula-body">
        <a-row :gutter="16" align="middle">
          <a-col :span="4">
            <div class="formula-field">
              <label>基础价格</label>
              <a-select v-model:value="formula.baseField" size="small" @change="clearPreview">
                <a-select-option value="costPrice">成本价</a-select-option>
                <a-select-option value="standardPrice">标准售价</a-select-option>
                <a-select-option value="wholesalePrice">批发价</a-select-option>
                <a-select-option value="fixed">固定值</a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="2">
            <div class="formula-field">
              <label>运算符</label>
              <a-select v-model:value="formula.operator" size="small" @change="clearPreview">
                <a-select-option value="multiply">× (乘)</a-select-option>
                <a-select-option value="divide">÷ (除)</a-select-option>
                <a-select-option value="add">+ (加)</a-select-option>
                <a-select-option value="subtract">- (减)</a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="2">
            <div class="formula-field">
              <label>数值</label>
              <a-input-number
                v-model:value="formula.operand"
                :min="0"
                :precision="4"
                size="small"
                style="width: 100%"
                @change="clearPreview"
              />
            </div>
          </a-col>
          <a-col :span="4">
            <div class="formula-field">
              <label>取整规则</label>
              <a-select v-model:value="formula.rounding" size="small" @change="clearPreview">
                <a-select-option value="round">四舍五入(2位)</a-select-option>
                <a-select-option value="ceil">向上取整</a-select-option>
                <a-select-option value="floor">向下取整</a-select-option>
                <a-select-option value="roundTo05">取5或0</a-select-option>
                <a-select-option value="roundToHalf">取0.5</a-select-option>
              </a-select>
            </div>
          </a-col>
          <a-col :span="4">
            <div class="formula-field">
              <label>应用到</label>
              <a-space>
                <a-checkbox v-model:checked="formula.targetStandard" @change="clearPreview">标准售价</a-checkbox>
                <a-checkbox v-model:checked="formula.targetWholesale" @change="clearPreview">批发价</a-checkbox>
              </a-space>
            </div>
          </a-col>
          <a-col :span="4">
            <div class="formula-field">
              <label>&nbsp;</label>
              <a-space>
                <a-button size="small" @click="previewFormula">
                  <EyeOutlined /> 预览
                </a-button>
                <a-button size="small" @click="resetFormula">重置</a-button>
              </a-space>
            </div>
          </a-col>
          <a-col :span="4">
            <div class="formula-field">
              <label>公式预览</label>
              <div class="formula-expr">{{ formulaExpression }}</div>
            </div>
          </a-col>
        </a-row>

        <!-- 预览结果 -->
        <div v-if="previewResults.length > 0" class="preview-section">
          <a-divider style="margin: 8px 0;" />
          <div class="preview-header">
            <span><FileTextOutlined /> 预览结果（前10条）</span>
            <a-button type="link" size="small" @click="clearPreview">关闭预览</a-button>
          </div>
          <div class="preview-table-wrapper">
            <table class="preview-table">
              <thead>
                <tr>
                  <th>产品编码</th>
                  <th>产品名称</th>
                  <th>基础值</th>
                  <th v-if="formula.targetStandard">→ 标准售价</th>
                  <th v-if="formula.targetWholesale">→ 批发价</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in previewResults" :key="item.id">
                  <td>{{ item.productCode }}</td>
                  <td>{{ item.productName }}</td>
                  <td class="cell-number">{{ formatPrice(item.baseValue) }}</td>
                  <td v-if="formula.targetStandard" class="cell-number cell-new">{{ formatPrice(item.newStandardPrice) }}</td>
                  <td v-if="formula.targetWholesale" class="cell-number cell-new">{{ formatPrice(item.newWholesalePrice) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    <!-- ==================== 查询栏 ==================== -->
    <div class="search-bar">
      <a-space>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="编码/名称/规格"
          style="width: 220px"
          size="small"
          allow-clear
          @search="handleSearch"
        />
        <a-select
          v-model:value="statusFilter"
          placeholder="状态"
          style="width: 100px"
          size="small"
          allow-clear
          @change="fetchProducts"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="ENABLED">启用</a-select-option>
          <a-select-option value="DISABLED">停用</a-select-option>
        </a-select>
      </a-space>
      <a-space>
        <a-button v-permission="'erp:product:batch-price'" size="small" @click="handleBatchSave" :loading="saving" :disabled="selectedKeys.length === 0" type="primary">
          <SaveOutlined /> 批量保存 ({{ selectedKeys.length }})
        </a-button>
        <a-button size="small" @click="router.push('/erp/product')">
          <ArrowLeftOutlined /> 返回产品列表
        </a-button>
      </a-space>
    </div>

    <!-- ==================== 产品表格 ==================== -->
    <div ref="tableContainerRef" class="table-wrapper">
      <vxe-table
        ref="tableRef"
        :data="products"
        :loading="loading"
        :height="tableHeight"
        :row-config="{ keyField: 'id', isHover: true }"
        :checkbox-config="{ highlight: true, range: true, labelField: 'productCode' }"
        border
        auto-resize
        stripe
        show-header-overflow="title"
        show-overflow="title"
        @checkbox-change="handleCheckboxChange"
        @checkbox-all="handleCheckboxAll"
      >
        <vxe-column type="checkbox" width="50" />
        <vxe-column field="productCode" title="产品编码" width="120" />
        <vxe-column field="productName" title="产品名称" width="150" min-width="120" />
        <vxe-column field="categoryName" title="分类" width="100" />
        <vxe-column field="costPrice" title="成本价" width="110" align="right">
          <template #default="{ row }">
            <a-input-number
              v-model:value="row.costPrice"
              :precision="2"
              :min="0"
              size="small"
              style="width: 100%"
            />
          </template>
        </vxe-column>
        <vxe-column field="standardPrice" title="标准售价" width="120" align="right">
          <template #default="{ row }">
            <a-input-number
              v-model:value="row.standardPrice"
              :precision="2"
              :min="0"
              size="small"
              style="width: 100%"
            />
          </template>
        </vxe-column>
        <vxe-column field="wholesalePrice" title="批发价" width="120" align="right">
          <template #default="{ row }">
            <a-input-number
              v-model:value="row.wholesalePrice"
              :precision="2"
              :min="0"
              size="small"
              style="width: 100%"
            />
          </template>
        </vxe-column>
        <vxe-column field="unit" title="单位" width="60" align="center" />
        <vxe-column field="status" title="状态" width="80" align="center">
          <template #default="{ row }">
            <a-tag :color="row.status === 'ENABLED' ? 'green' : 'red'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </a-tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" width="80" fixed="right">
          <template #default="{ row }">
            <a-button v-permission="'erp:product:edit'" type="link" size="small" @click="debounceClick('edit_' + row.id, () => router.push(`/erp/product/${row.id}`))">编辑</a-button>
          </template>
        </vxe-column>
      </vxe-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-bar">
      <div class="pagination-info">
        <template v-if="selectedKeys.length > 0">
          <a-badge :count="selectedKeys.length" style="margin-right: 8px;" />
          <span>已选择 <b>{{ selectedKeys.length }}</b> 项</span>
          <a-divider type="vertical" />
          <a-button type="link" size="small" @click="clearSelection">取消选择</a-button>
        </template>
      </div>
      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        :show-size-changer="true"
        :show-quick-jumper="true"
        :page-size-options="['10', '20', '50', '100']"
        :show-total="(total: number) => `共 ${total} 条`"
        size="small"
        @change="onPageChange"
      />
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, defineExpose } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import {
  ReloadOutlined, SaveOutlined, ArrowLeftOutlined,
  FunctionOutlined, CaretDownOutlined, EyeOutlined,
  CheckOutlined, FileTextOutlined, SyncOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { productApi, type Product } from '@/api/erp/product'
import type { VxeTableInstance, VxeTableEvents } from 'vxe-table'

const router = useRouter()

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[批量价格] ErrorBoundary 捕获异常:', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 类型定义 ──
interface ProductRow extends Product {
  _dirty?: boolean
}

interface PreviewItem {
  id: number
  productCode: string
  productName: string
  baseValue: number
  newStandardPrice: number
  newWholesalePrice: number
}

interface FormulaConfig {
  baseField: 'costPrice' | 'standardPrice' | 'wholesalePrice' | 'fixed'
  operator: 'multiply' | 'divide' | 'add' | 'subtract'
  operand: number
  rounding: 'round' | 'ceil' | 'floor' | 'roundTo05' | 'roundToHalf'
  targetStandard: boolean
  targetWholesale: boolean
}

// ── 查询参数 ──
const loading = ref(false)
const saving = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const products = ref<ProductRow[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const tableRef = ref<VxeTableInstance>()
const tableContainerRef = ref<HTMLElement>()

const tableHeight = ref(400)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// ── 选择 ──
const selectedKeys = ref<number[]>([])
const selectedRows = ref<ProductRow[]>([])

// ── 公式面板 ──
const formulaCollapsed = ref(false)
const previewResults = ref<PreviewItem[]>([])

const formula: FormulaConfig = reactive({
  baseField: 'costPrice',
  operator: 'multiply',
  operand: 1,
  rounding: 'round',
  targetStandard: true,
  targetWholesale: true
})

const formulaExpression = computed(() => {
  const baseLabels: Record<string, string> = {
    costPrice: '成本价',
    standardPrice: '标准售价',
    wholesalePrice: '批发价',
    fixed: '固定值'
  }
  const operatorSymbols: Record<string, string> = {
    multiply: '×',
    divide: '÷',
    add: '+',
    subtract: '-'
  }
  const roundingLabels: Record<string, string> = {
    round: '四舍五入',
    ceil: '向上取整',
    floor: '向下取整',
    roundTo05: '取5或0',
    roundToHalf: '取0.5'
  }
  const base = formula.baseField === 'fixed' ? '固定值' : baseLabels[formula.baseField]
  const targets: string[] = []
  if (formula.targetStandard) targets.push('标准售价')
  if (formula.targetWholesale) targets.push('批发价')
  return `${base} ${operatorSymbols[formula.operator]} ${formula.operand} → [${roundingLabels[formula.rounding]}] → ${targets.join('、') || '无'}`
})

// ── 价格计算引擎 ──
function calcPrice(baseValue: number, config: FormulaConfig): number {
  if (baseValue == null || isNaN(baseValue)) return 0
  let result = baseValue
  const operand = config.operand || (config.baseField === 'fixed' ? 0 : 1)

  switch (config.operator) {
    case 'multiply':
      result = baseValue * operand
      break
    case 'divide':
      result = operand !== 0 ? baseValue / operand : 0
      break
    case 'add':
      result = baseValue + operand
      break
    case 'subtract':
      result = baseValue - operand
      break
  }

  switch (config.rounding) {
    case 'round':
      result = Math.round(result * 100) / 100
      break
    case 'ceil':
      result = Math.ceil(result * 100) / 100
      break
    case 'floor':
      result = Math.floor(result * 100) / 100
      break
    case 'roundTo05': {
      const intPart = Math.floor(result)
      const decPart = result - intPart
      if (decPart < 0.25) result = intPart
      else if (decPart < 0.75) result = intPart + 0.5
      else result = intPart + 1
      break
    }
    case 'roundToHalf':
      result = Math.round(result * 2) / 2
      break
  }

  return Math.max(0, Math.round(result * 100) / 100)
}

function getBaseValue(row: ProductRow): number {
  if (formula.baseField === 'fixed') return formula.operand || 0
  return Number(row[formula.baseField]) || 0
}

function formatPrice(v: number | undefined | null): string {
  if (v == null) return '-'
  return v.toFixed(2)
}

// ── 预览 ──
function previewFormula() {
  const targetRows = selectedKeys.value.length > 0 ? selectedRows.value : products.value
  if (targetRows.length === 0) {
    message.warning('请先选择产品')
    return
  }

  previewResults.value = targetRows.slice(0, 10).map(row => ({
    id: row.id,
    productCode: row.productCode,
    productName: row.productName,
    baseValue: getBaseValue(row),
    newStandardPrice: formula.targetStandard ? calcPrice(getBaseValue(row), formula) : (row.standardPrice ?? 0),
    newWholesalePrice: formula.targetWholesale ? calcPrice(getBaseValue(row), formula) : (row.wholesalePrice ?? 0)
  }))
}

function clearPreview() {
  previewResults.value = []
}

function resetFormula() {
  Object.assign(formula, {
    baseField: 'costPrice',
    operator: 'multiply',
    operand: 1,
    rounding: 'round',
    targetStandard: true,
    targetWholesale: true
  } satisfies FormulaConfig)
  clearPreview()
}

// ── 应用到选中行 ──
function applyFormula() {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择产品')
    return
  }
  if (!formula.targetStandard && !formula.targetWholesale) {
    message.warning('请选择至少一个目标字段')
    return
  }
  if (formula.baseField === 'fixed' && (!formula.operand || formula.operand <= 0)) {
    message.warning('固定值模式下请输入有效数值')
    return
  }

  for (const row of selectedRows.value) {
    const baseValue = getBaseValue(row)
    const newPrice = calcPrice(baseValue, formula)
    if (formula.targetStandard) row.standardPrice = newPrice
    if (formula.targetWholesale) row.wholesalePrice = newPrice
    row._dirty = true
  }
  message.success(`已更新 ${selectedKeys.value.length} 条记录的价格`)
  clearPreview()
}

// ── vxe-table 选中事件 ──
const handleCheckboxChange = (params: any) => {
  selectedKeys.value = params.checkedRows.map((r: { id: number }) => r.id)
  selectedRows.value = params.checkedRows as ProductRow[]
}

const handleCheckboxAll = (params: any) => {
  if (params.checked) {
    selectedKeys.value = products.value.map(r => r.id)
    selectedRows.value = [...products.value]
  } else {
    selectedKeys.value = []
    selectedRows.value = []
  }
}

function clearSelection() {
  tableRef.value?.clearCheckboxRow()
  selectedKeys.value = []
  selectedRows.value = []
}

// ── 批量保存 ──
let resizeObserver: ResizeObserver | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchProducts)
  }
}

onMounted(() => {
  fetchProducts()
  document.addEventListener('keydown', handleKeydown)
  calcTableHeight()
  resizeObserver = new ResizeObserver(() => calcTableHeight())
  resizeObserver.observe(document.body)
  // 自动刷新 300s
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchProducts()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

async function handleBatchSave() {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择要保存的产品')
    return
  }

  saving.value = true
  try {
    const items = selectedRows.value.map(row => ({
      id: row.id,
      costPrice: row.costPrice,
      standardPrice: row.standardPrice,
      wholesalePrice: row.wholesalePrice
    }))
    await productApi.batchUpdatePrices(items)
    message.success(`成功保存 ${items.length} 条记录`)
    clearSelection()
    await fetchProducts()
  } catch (e: unknown) {
    console.error('[批量价格] 保存失败', e)
    const msg = e instanceof Error ? e.message : '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

// ── 数据查询 ──
async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.page({
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    } as any)
    products.value = (res.records || []).map(r => ({ ...r, _dirty: false }))
    pagination.total = res.total || 0
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (e: unknown) {
    console.error('[批量价格] 加载产品列表失败', e)
    const msg = e instanceof Error ? e.message : '加载产品列表失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchProducts()
}

function onPageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  clearSelection()
  fetchProducts()
}

// ── 自适应高度 ──
function calcTableHeight() {
  if (!tableContainerRef.value) return
  nextTick(() => {
    const parent = tableContainerRef.value?.parentElement
    if (parent) {
      const searchBar = parent.querySelector('.search-bar')
      const paginationBar = parent.querySelector('.pagination-bar')
      const formulaCard = parent.querySelector('.formula-card')
      const headerHeight = 52 // page header approx
      const searchHeight = searchBar ? searchBar.getBoundingClientRect().height + 12 : 44
      const paginationHeight = paginationBar ? paginationBar.getBoundingClientRect().height + 8 : 40
      const formulaHeight = formulaCard && !formulaCollapsed.value
        ? formulaCard.getBoundingClientRect().height + 12
        : 44
      const available = window.innerHeight - headerHeight - formulaHeight - searchHeight - paginationHeight - 40
      tableHeight.value = Math.max(200, available)
    }
  })
}

defineExpose({ fetchData: fetchProducts })
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ── 公式面板 ── */
.formula-card {
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}
.formula-card--collapsed {
  cursor: pointer;
}
.formula-card--collapsed:hover {
  border-color: #1890ff;
}
.formula-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  cursor: pointer;
  user-select: none;
}
.formula-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 4px;
}
.formula-body {
  padding-top: 12px;
}
:deep(.formula-card .ant-card-body) {
  padding: 12px 16px;
}
.formula-field label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}
.formula-expr {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 预览结果 ── */
.preview-section {
  margin-top: 4px;
}
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.preview-table-wrapper {
  max-height: 200px;
  overflow-y: auto;
}
.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.preview-table th,
.preview-table td {
  padding: 4px 8px;
  border: 1px solid #e8e8e8;
  text-align: left;
}
.preview-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
}
.cell-number {
  text-align: right;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
.cell-new {
  color: #1890ff;
  font-weight: 600;
}

/* ── 搜索栏 ── */
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

/* ── 表格 ── */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

/* ── 分页 ── */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
.pagination-info {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

.rotate-180 {
  transform: rotate(180deg);
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}
.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
