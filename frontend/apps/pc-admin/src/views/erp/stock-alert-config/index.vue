<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 顶部：库存上下限比较口径提示条 -->
      <div
        class="comparison-banner"
        @click="openComparisonModal"
      >
        <InfoCircleOutlined class="comparison-icon" />
        <span class="comparison-text">库存上下限比较值：</span>
        <b class="comparison-value">{{ comparisonLabel }}</b>
        <a class="comparison-set">点击设置</a>
      </div>

      <CategoryListLayout
        :show-category-panel="true"
        :category-loading="categoryLoading"
        :category-tree-data="categoryTreeData"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        category-title="商品分类"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any) => (expandedKeys = keys)"
        @category-retry="loadCategoryTree"
        @search="handleSearch"
      >
        <!-- ═══ 工具栏左侧：统计 ═══ -->
        <template #toolbar-left>
          <span class="toolbar-total">
            共 <b>{{ pagination.total }}</b> 条
          </span>
        </template>

        <!-- ═══ 工具栏右侧：功能按钮 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="refreshLoading"
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
              type="primary"
              :disabled="selectedRows.length === 0"
              @click="openBatchModal"
            >
              <SettingOutlined /> 批量设置
            </a-button>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    key="cancel"
                    :disabled="selectedRows.length === 0"
                    @click="handleBatchCancel"
                  >
                    批量取消
                  </a-menu-item>
                  <a-menu-item
                    key="export"
                    @click="handleExport"
                  >
                    导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区：仓库/商品/品牌/查询 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-field-item">
              <span class="search-label">仓库</span>
              <a-radio-group
                v-model:value="warehouseMode"
                size="small"
              >
                <a-radio-button value="all">全部仓库</a-radio-button>
                <a-radio-button value="spec">指定仓库</a-radio-button>
              </a-radio-group>
              <a-select
                v-if="warehouseMode === 'spec'"
                v-model:value="searchValues.warehouseId"
                :options="warehouseOptions"
                placeholder="请选择仓库"
                allow-clear
                show-search
                size="small"
                style="width: 150px"
              />
            </div>
            <div class="search-field-item">
              <span class="search-label">商品</span>
              <a-input
                v-model:value="searchValues.keyword"
                placeholder="商品名称/货号/条码"
                allow-clear
                size="small"
                style="width: 180px"
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-field-item">
              <span class="search-label">品牌</span>
              <a-select
                v-model:value="searchValues.brand"
                :options="brandOptions"
                placeholder="全部"
                allow-clear
                show-search
                size="small"
                style="width: 140px"
              />
            </div>
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
          </div>
        </template>

        <!-- ═══ 表格：14 列，上下限行内可编辑 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              ref="tableRef"
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="pagination"
              row-key="_rowKey"
              :selectable="true"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-batch-delete="false"
              :show-export="false"
              :min-empty-rows="0"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <template #maxStockCell="{ record }">
                <a-input-number
                  v-model:value="record.maxStock"
                  :min="0"
                  size="small"
                  style="width: 100%"
                  placeholder="上限"
                  @change="markDirty(record)"
                />
              </template>
              <template #minStockCell="{ record }">
                <a-input-number
                  v-model:value="record.minStock"
                  :min="0"
                  size="small"
                  style="width: 100%"
                  placeholder="下限"
                  @change="markDirty(record)"
                />
              </template>
            </BillTableList>

            <!-- 行内编辑保存栏 -->
            <div
              v-if="dirtyRowKeys.size > 0"
              class="save-bar"
            >
              <a-space :size="8">
                <span class="save-bar-text">
                  已修改 {{ dirtyRowKeys.size }} 条
                </span>
                <a-button
                  type="primary"
                  size="small"
                  :loading="saving"
                  @click="handleSaveRows"
                >
                  保存修改
                </a-button>
                <a-button
                  size="small"
                  @click="resetDirty"
                >
                  放弃
                </a-button>
              </a-space>
            </div>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 批量设置弹窗 ═══ -->
      <a-modal
        v-model:open="batchModalVisible"
        title="批量设置库存上下限"
        :confirm-loading="saving"
        width="420px"
        @ok="handleBatchApply"
      >
        <p class="modal-tip">
          将对已勾选的 {{ selectedRows.length }} 个商品在所选仓库下统一设置上下限。
        </p>
        <a-form
          :model="batchForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          layout="horizontal"
        >
          <a-form-item
            label="仓库"
            required
          >
            <a-select
              v-model:value="batchForm.warehouseId"
              :options="warehouseOptions"
              placeholder="请选择仓库"
              show-search
            />
          </a-form-item>
          <a-form-item label="库存下限">
            <a-input-number
              v-model:value="batchForm.minStock"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="库存上限">
            <a-input-number
              v-model:value="batchForm.maxStock"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 比较口径弹窗 ═══ -->
      <a-modal
        v-model:open="comparisonModalVisible"
        title="设置库存上下限比较值"
        :confirm-loading="saving"
        width="480px"
        @ok="handleComparisonSave"
      >
        <p class="modal-tip">
          预警判定时所比较的库存数量口径，将作为「预警查询」的过滤阈值依据。
        </p>
        <a-radio-group
          v-model:value="comparisonForm.value"
          style="display: flex; flex-direction: column; gap: 12px"
        >
          <a-radio value="BOOK_QTY">账面库存</a-radio>
          <a-radio value="BOOK_WAIT_DELIVER">账面库存 - 待发货</a-radio>
          <a-radio value="BOOK_WAIT_DELIVER_PURCHASE">账面库存 - 待发货 - 待收货</a-radio>
        </a-radio-group>
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="erp-stock-alert-config"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, SettingOutlined, DownOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

// ── 错误处理 ──
function handleError(err: any) {
  console.warn('[预警设置] ErrorBoundary:', err)
}

// ── 查询条件 ──
const warehouseMode = ref<'all' | 'spec'>('all')
const searchValues = reactive<Record<string, any>>({
  warehouseId: undefined,
  keyword: '',
  brand: undefined,
})
const warehouseOptions = ref<any[]>([])
const brandOptions = ref<any[]>([])

// ── 分类树 ──
const categoryLoading = ref(false)
const categoryTreeData = ref<any[]>([])
const expandedKeys = ref<(string | number)[]>(['__all__'])
const selectedCategoryId = ref<string | number>('__all__')

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await request.get('/erp/product-category/tree')
    const tree = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: tree }]
    expandedKeys.value = ['__all__', ...tree.filter((c: any) => c.children?.length).map((c: any) => c.id)]
  } catch (e) {
    console.warn('[预警设置] 分类树加载失败', e)
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: [] }]
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : '__all__'
  handleSearch()
}

// ── 仓库 / 品牌 ──
async function loadWarehouses() {
  try {
    const res: any = await request.get('/erp/stock/warehouses')
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    warehouseOptions.value = list.map((w: any) => ({ value: w.warehouseId ?? w.id, label: w.warehouseName || w.name }))
  } catch (e) {
    console.warn('[预警设置] 仓库加载失败', e)
    warehouseOptions.value = []
  }
}

async function loadBrands() {
  try {
    const res: any = await request.get('/erp/product/brands')
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    brandOptions.value = list.map((b: string) => ({ value: b, label: b }))
  } catch (e) {
    console.warn('[预警设置] 品牌加载失败', e)
    brandOptions.value = []
  }
}

// ── 表格列（13 个数据列 + 序号/选择；文档列清单中「型号」重复，实际去重） ──
const columns: any[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 110 },
  { title: '口味', field: 'taste', key: 'taste', width: 90 },
  { title: '型号', field: 'model', key: 'model', width: 90 },
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120 },
  { title: '规格', field: 'spec', key: 'spec', width: 110 },
  { title: '产地', field: 'origin', key: 'origin', width: 90 },
  { title: '品牌', field: 'brand', key: 'brand', width: 90 },
  { title: '预警仓库', field: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '库存上限', field: 'maxStock', key: 'maxStock', width: 110, align: 'right', type: 'slot', slotName: 'maxStockCell' },
  { title: '库存下限', field: 'minStock', key: 'minStock', width: 110, align: 'right', type: 'slot', slotName: 'minStockCell' },
]

// ── 数据状态 ──
const loading = ref(false)
const refreshLoading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 行内编辑脏标记
const dirtyRowKeys = ref<Set<string>>(new Set())

function rowKeyOf(record: any) {
  return `${record.productId ?? ''}_${record.warehouseId ?? ''}`
}

function markDirty(record: any) {
  if (record && record.productId != null) dirtyRowKeys.value.add(rowKeyOf(record))
}

function resetDirty() {
  dirtyRowKeys.value = new Set()
  fetchData()
}

// ── 数据加载 ──
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchValues.keyword || undefined,
      brand: searchValues.brand || undefined,
    }
    if (warehouseMode.value === 'spec' && searchValues.warehouseId) {
      params.warehouseId = searchValues.warehouseId
    }
    if (selectedCategoryId.value && selectedCategoryId.value !== '__all__') {
      params.categoryId = selectedCategoryId.value
    }
    Object.keys(params).forEach((k) => (params[k] === undefined || params[k] === '') && delete params[k])

    const res: any = await request.get('/erp/stock-alert-config/config-items', { params })
    const page = res?.records ? res : (res?.data?.records ? res.data : (Array.isArray(res) ? { records: res, total: 0 } : { records: [], total: 0 }))
    tableData.value = (page.records || []).map((r: any) => ({ ...r, _rowKey: rowKeyOf(r) }))
    pagination.total = Number(page.total) || 0
  } catch (e) {
    console.warn('[预警设置] 加载失败', e)
    message.error('查询失败，请检查网络后重试')
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  dirtyRowKeys.value = new Set()
  fetchData()
}

function handleRefresh() {
  refreshLoading.value = true
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  dirtyRowKeys.value = new Set()
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

// ── 保存行内修改 ──
async function handleSaveRows() {
  const items = tableData.value
    .filter((r: any) => dirtyRowKeys.value.has(rowKeyOf(r)))
    .map((r: any) => ({
      productId: r.productId,
      warehouseId: r.warehouseId,
      maxStock: r.maxStock ?? null,
      minStock: r.minStock ?? null,
    }))
  if (items.length === 0) {
    message.info('没有需要保存的修改')
    return
  }
  saving.value = true
  try {
    await request.post('/erp/stock-alert-config/batch-set', { items })
    message.success(`保存成功（${items.length} 条）`)
    dirtyRowKeys.value = new Set()
    pagination.current = 1
    fetchData()
  } catch (e: any) {
    console.warn('[预警设置] 保存失败', e)
    message.error(e?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// ── 批量设置 ──
const batchModalVisible = ref(false)
const batchForm = reactive<Record<string, any>>({ warehouseId: undefined, minStock: undefined, maxStock: undefined })

function openBatchModal() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选商品')
    return
  }
  batchForm.warehouseId = warehouseMode.value === 'spec' ? searchValues.warehouseId : undefined
  batchForm.minStock = undefined
  batchForm.maxStock = undefined
  batchModalVisible.value = true
}

async function handleBatchApply() {
  if (!batchForm.warehouseId) {
    message.warning('请选择仓库')
    return
  }
  const productIds = selectedRows.value.map((r: any) => r.productId).filter(Boolean)
  if (productIds.length === 0) {
    message.warning('勾选商品无效，请重新选择')
    return
  }
  saving.value = true
  try {
    await request.post('/erp/stock-alert-config/batch-set', {
      warehouseId: batchForm.warehouseId,
      productIds,
      minStock: batchForm.minStock ?? null,
      maxStock: batchForm.maxStock ?? null,
    })
    message.success('批量设置成功')
    batchModalVisible.value = false
    pagination.current = 1
    fetchData()
  } catch (e: any) {
    console.warn('[预警设置] 批量设置失败', e)
    message.error(e?.message || '批量设置失败')
  } finally {
    saving.value = false
  }
}

// ── 批量取消（清除选择） ──
function handleBatchCancel() {
  if (selectedRows.value.length === 0) return
  tableRef.value?.clearSelection?.()
  selectedRows.value = []
  dirtyRowKeys.value = new Set()
  message.success('已取消选择')
}

// ── 导出（前端 CSV 当前页） ──
function handleExport() {
  if (tableData.value.length === 0) {
    message.warning('暂无数据可导出')
    return
  }
  const headers = columns.filter((c: any) => c.field).map((c: any) => ({ field: c.field, title: c.title }))
  const escape = (v: any) => {
    const s = v === null || v === undefined ? '' : String(v)
    return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s
  }
  const csv = '\ufeff' + headers.map((h) => h.title).join(',') + '\n' + tableData.value.map((r: any) => headers.map((h) => escape(r[h.field])).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '库存预警固定值设置.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

// ── 打印 (F8) ──
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'erp-stock-alert-config',
  title: '商品分类',
  columns: () => columns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

// ── 比较口径 ──
const comparisonModalVisible = ref(false)
const comparisonForm = reactive<Record<string, any>>({ value: 'BOOK_WAIT_DELIVER' })
const comparisonLabel = ref<string>('账面库存-待发货')

const COMPARISON_LABELS: Record<string, string> = {
  BOOK_QTY: '账面库存',
  BOOK_WAIT_DELIVER: '账面库存-待发货',
  BOOK_WAIT_DELIVER_PURCHASE: '账面库存-待发货-待收货',
}

async function loadComparison() {
  try {
    const res: any = await request.get('/erp/stock-alert-config/comparison')
    const data = res?.data || res
    if (data?.value) comparisonForm.value = data.value
    comparisonLabel.value = data?.label || COMPARISON_LABELS[comparisonForm.value] || comparisonForm.value
  } catch (e) {
    console.warn('[预警设置] 比较口径加载失败', e)
  }
}

function openComparisonModal() {
  comparisonModalVisible.value = true
}

async function handleComparisonSave() {
  saving.value = true
  try {
    await request.post('/erp/stock-alert-config/comparison', { value: comparisonForm.value })
    comparisonLabel.value = COMPARISON_LABELS[comparisonForm.value] || comparisonForm.value
    comparisonModalVisible.value = false
    message.success('比较口径已保存')
  } catch (e: any) {
    console.warn('[预警设置] 比较口径保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ── 快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    handleRefresh()
  } else if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadCategoryTree()
  loadWarehouses()
  loadBrands()
  loadComparison()
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.comparison-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(90deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #ffe58f;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #613400;
}
.comparison-banner:hover {
  background: #fff1cc;
}
.comparison-icon { color: #faad14; }
.comparison-value { color: #d48806; }
.comparison-set { color: #1677ff; text-decoration: underline; margin-left: 4px; }

.search-area {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.search-field-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.btn-search { margin-left: 8px; }

.toolbar-total { font-size: 13px; color: #606266; }
.toolbar-total b { color: #1890ff; }

.table-area {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.save-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 16px;
  background: #e6f7ff;
  border-top: 1px solid #91d5ff;
}
.save-bar-text { font-size: 13px; color: #1890ff; font-weight: 500; }

.modal-tip {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

:deep(.ant-input-number-sm) { height: 28px; line-height: 28px; }
:deep(.ant-input-number-sm input) { height: 26px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { height: 28px; line-height: 26px; }
</style>
