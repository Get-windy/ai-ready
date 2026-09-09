<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="alert-header">
          <div class="alert-header-left">
            <div class="alert-title">
              <span class="alert-title-icon"><AlertOutlined /></span>
              <span class="alert-title-text">库存预警补货</span>
            </div>
          </div>
          <div class="alert-header-right">
            <a-space :size="8">
              <a-button
                type="primary"
                :disabled="selectedRows.length === 0"
                @click="handlePurchase"
              >
                <template #icon>
                  <ShoppingCartOutlined />
                </template>采购
              </a-button>
              <a-button
                :loading="loading"
                @click="fetchData"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>刷新
              </a-button>
              <a-button @click="handlePrint">
                <template #icon>
                  <PrinterOutlined />
                </template>打印(F8)
              </a-button>
              <a-button @click="handleExport">
                <template #icon>
                  <ExportOutlined />
                </template>导出
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <div class="alert-root">
        <!-- ═══ 顶部说明条 ═══ -->
        <div class="hint-bar">
          <span class="hint-text">缺货数量 = 库存上限+待发货-账面数量-待收货</span>
          <a
            class="hint-link"
            @click="goAlertConfig"
          >点击设置</a>
        </div>

        <!-- ═══ 查询区 ═══ -->
        <div class="search-bar">
          <a-radio-group
            v-model:value="warehouseRange"
            size="small"
            class="wh-radio"
          >
            <a-radio value="all">全部仓库</a-radio>
            <a-radio value="specify">指定仓库</a-radio>
          </a-radio-group>
          <a-select
            v-if="warehouseRange === 'specify'"
            v-model:value="warehouseId"
            size="small"
            class="wh-select"
            :placeholder="'全部仓库'"
            :options="warehouseOptions"
            allow-clear
          />
          <a-input
            v-model:value="searchForm.keyword"
            size="small"
            class="search-input"
            :placeholder="'商品'"
            allow-clear
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="searchForm.brand"
            size="small"
            class="search-input"
            :placeholder="'品牌'"
            allow-clear
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="searchForm.supplierName"
            size="small"
            class="search-input"
            :placeholder="'所属供应商'"
            allow-clear
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="searchForm.remark"
            size="small"
            class="search-input"
            :placeholder="'备注'"
            allow-clear
            @press-enter="handleSearch"
          />
          <a-button
            type="primary"
            size="small"
            class="btn-search"
            @click="handleSearch"
          >
            <template #icon>
              <SearchOutlined />
            </template>查询
          </a-button>
          <a-checkbox
            v-model:checked="onlyLowStock"
            class="only-low-chk"
            @change="handleSearch"
          >
            只显示下限预警的商品
          </a-checkbox>
        </div>

        <!-- ═══ 内容行：左侧分类树 + 右侧表格 ═══ -->
        <div class="content-row">
          <div class="category-panel">
            <div class="category-header">
              <span class="category-title">商品分类</span>
              <a-button
                type="text"
                size="small"
                class="collapse-btn"
                @click="categoryCollapsed = !categoryCollapsed"
              >
                <MenuFoldOutlined v-if="!categoryCollapsed" />
                <MenuUnfoldOutlined v-else />
              </a-button>
            </div>
            <div
              v-if="!categoryCollapsed"
              class="category-tree"
            >
              <a-spin :spinning="categoryLoading">
                <a-tree
                  :tree-data="categoryTreeData"
                  :field-names="{ key: 'id', title: 'categoryName', children: 'children' }"
                  :selected-keys="selectedCategoryKeys"
                  :expanded-keys="expandedKeysTree"
                  show-icon
                  block-node
                  @select="handleCategorySelect"
                  @expand="(keys: any[]) => (expandedKeysTree = keys)"
                >
                  <template #title="{ categoryName, productCount }">
                    <span>{{ categoryName }}</span>
                    <span
                      v-if="productCount !== undefined"
                      class="cat-count"
                    >({{ productCount }})</span>
                  </template>
                  <template #icon="{ expanded }">
                    <FolderOpenOutlined
                      v-if="expanded"
                      class="cat-folder"
                    />
                    <FolderOutlined
                      v-else
                      class="cat-folder"
                    />
                  </template>
                </a-tree>
              </a-spin>
            </div>
          </div>

          <div class="table-panel">
            <BillDetailTable
              :columns="columns"
              v-model:data-source="tableData"
              :view-mode="true"
              :min-rows="0"
              :storage-key="storageKey"
              :loading="loading"
              :show-pagination="true"
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              :summary-columns="summaryColumns"
              @page-change="handlePageChange"
              @checkbox-change="handleRowCheck"
              @checkbox-all="handleCheckAll"
            >
              <template #alertTypeCell="{ record }">
                <a-tag :color="getAlertColor(record.alertType)">
                  {{ record.alertType || '正常' }}
                </a-tag>
              </template>
              <template #model2Cell="{ record }">
                {{ record.model || '-' }}
              </template>
            </BillDetailTable>
          </div>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AlertOutlined, ShoppingCartOutlined, ReloadOutlined, PrinterOutlined,
  ExportOutlined, SearchOutlined, FolderOutlined, FolderOpenOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockReportApi, type StockAlertReplenishItem } from '@/api/analytics'
import request from '@/utils/request'

defineOptions({ name: 'PurchaseAlertReplenish' })
const router = useRouter()

const handleError = (e: any) => console.warn('[库存预警补货] ErrorBoundary:', e)

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<StockAlertReplenishItem[]>([])
const selectedRows = ref<StockAlertReplenishItem[]>([])
const categoryLoading = ref(false)
const categoryCollapsed = ref(false)
const categoryTreeData = ref<any[]>([])
const expandedKeysTree = ref<(string | number)[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const warehouseRange = ref<'all' | 'specify'>('all')
const warehouseId = ref<number | undefined>(undefined)
const selectedCategoryKeys = ref<(string | number)[]>(['__all__'])
const onlyLowStock = ref(false)
const searchForm = reactive({ keyword: '', brand: '', supplierName: '', remark: '' })

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const storageKey = 'purchase-alert-replenish-columns'

// ═══ 预警类型颜色 ═══
function getAlertColor(type: string): string {
  const map: Record<string, string> = {
    '缺货': 'red', '下限预警': 'orange', '超储': 'gold', '正常': 'green',
  }
  return map[type] || 'default'
}

// ═══ 格式化 ═══
function formatQty(v: any, _record?: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function formatMoney(v: any, _record?: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 表格列（25列 + 行号/合计） ═══
const columns: DetailColumnConfig[] = [
  { key: 'select', title: '', type: 'checkbox', width: 40, fixed: 'left', hideable: false },
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'warehouseCode', title: '仓库编号', width: 110 },
  { key: 'warehouseName', title: '仓库名称', width: 130 },
  { key: 'productName', title: '商品名称', width: 180 },
  { key: 'productCode', title: '货号', width: 120 },
  { key: 'brand', title: '品牌', width: 100 },
  { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { key: 'taste', title: '口味', width: 100 },
  { key: 'model', title: '型号', width: 100 },
  { key: 'alertType', title: '预警类型', width: 100, type: 'slot', slotName: 'alertTypeCell' },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', width: 100, defaultHidden: true },
  { key: 'model2', title: '型号', width: 100, defaultHidden: true, type: 'slot', slotName: 'model2Cell' },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'shortageQty', title: '缺货数量', width: 100, align: 'right', formatter: formatQty },
  { key: 'maxStock', title: '库存上限', width: 100, align: 'right', formatter: formatQty },
  { key: 'minStock', title: '库存下限', width: 100, align: 'right', formatter: formatQty },
  { key: 'remark', title: '备注', width: 140 },
  { key: 'pendingQty', title: '待发货数量', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { key: 'bookQty', title: '账面库存', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { key: 'inTransitQty', title: '待收货数量', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { key: 'lastPurchaseDate', title: '最近采购日期', width: 120, defaultHidden: true },
  { key: 'lastSupplierName', title: '最近采购供货商', width: 150, defaultHidden: true },
  { key: 'lastPurchasePrice', title: '最近采购价', width: 110, align: 'right', defaultHidden: true, formatter: formatMoney },
]

// ═══ 合计行 ═══
const summaryColumns = computed(() => {
  const r = tableData.value
  const sum = (fn: (x: StockAlertReplenishItem) => number) => r.reduce((s, x) => s + (fn(x) || 0), 0)
  return [
    { key: 'shortageQty', value: sum(x => Number(x.shortageQty) || 0), highlight: true },
    { key: 'maxStock', value: sum(x => Number(x.maxStock) || 0) },
    { key: 'minStock', value: sum(x => Number(x.minStock) || 0) },
    { key: 'pendingQty', value: sum(x => Number(x.pendingQty) || 0) },
    { key: 'bookQty', value: sum(x => Number(x.bookQty) || 0) },
    { key: 'inTransitQty', value: sum(x => Number(x.inTransitQty) || 0) },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (warehouseRange.value === 'specify' && warehouseId.value) params.warehouseId = warehouseId.value
    const selected = selectedCategoryKeys.value[0]
    if (selected && selected !== '__all__') params.categoryId = selected
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.brand) params.brand = searchForm.brand
    if (searchForm.supplierName) params.supplierName = searchForm.supplierName
    if (searchForm.remark) params.remark = searchForm.remark
    if (onlyLowStock.value) params.onlyLowStock = true

    const res = await stockReportApi.alertReplenishPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    // 数据更新后清空勾选，保证选中态与当前页一致
    selectedRows.value = []
  } catch (e) {
    console.warn('[库存预警补货] 加载失败', e)
    message.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handlePageChange = (p: { page: number; pageSize: number }) => {
  pagination.current = p.page
  pagination.pageSize = p.pageSize
  fetchData()
}

// ═══ 分类树 ═══
async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await request.get('/erp/product-category/tree')
    const tree = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: tree }]
    expandedKeysTree.value = ['__all__', ...tree.filter((c: any) => c.children?.length).map((c: any) => c.id)]
  } catch (e) {
    console.warn('[库存预警补货] 分类树加载失败', e)
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: [] }]
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryKeys.value = keys.length ? keys : ['__all__']
  handleSearch()
}

// ═══ 仓库选项 ═══
async function loadWarehouses() {
  try {
    const res: any = await request.get('/erp/stock/warehouses')
    const list = Array.isArray(res?.data) ? res.data : []
    warehouseOptions.value = list.map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
  } catch { warehouseOptions.value = [] }
}

// ═══ 操作 ═══
const goAlertConfig = () => router.push('/erp/stock-alert-config')

// ═══ 行勾选 ═══
function handleRowCheck(record: StockAlertReplenishItem, _rowIndex: number, checked: boolean) {
  if (checked) {
    if (!selectedRows.value.some(r => r.productId === record.productId)) selectedRows.value.push(record)
  } else {
    selectedRows.value = selectedRows.value.filter(r => r.productId !== record.productId)
  }
}
function handleCheckAll(checked: boolean, records: StockAlertReplenishItem[]) {
  selectedRows.value = checked ? [...records] : []
}

// ═══ 采购：跳转采购订单表单，预填选中商品 ═══
function handlePurchase() {
  const productIds = Array.from(new Set(selectedRows.value.map(r => r.productId).filter(Boolean)))
  if (!productIds.length) {
    message.warning('请先勾选需要采购的商品')
    return
  }
  router.push({ path: '/erp/purchase/form', query: { productIds: productIds.join(',') } })
}

function handlePrint() {
  window.print()
}

function handleExport() {
  if (!tableData.value.length) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = columns.filter(c => c.key !== 'rowNo' && c.type !== 'slot' && c.type !== 'checkbox')
  const headers = exportCols.map(c => String(c.title))
  const rows = tableData.value.map(r =>
    exportCols.map(c => {
      const val = r[c.key as keyof StockAlertReplenishItem]
      const display = c.formatter ? c.formatter(val as any, r) : (val ?? '')
      return `"${String(display).replace(/"/g, '""')}"`
    }),
  )
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `库存预警补货_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

onMounted(() => {
  loadCategoryTree()
  loadWarehouses()
  fetchData()
})
</script>

<style scoped>
.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.alert-header-left { display: flex; align-items: center; }
.alert-title { display: flex; align-items: center; gap: 8px; }
.alert-title-icon { font-size: 18px; color: #f5222d; }
.alert-title-text { font-size: 18px; font-weight: 600; color: #303133; }
.alert-header-right { display: flex; align-items: center; }

.alert-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 0;
}

/* 说明条 */
.hint-bar {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #7a4d00;
  flex-shrink: 0;
}
.hint-link { color: #409eff; margin-left: 8px; cursor: pointer; }
.hint-link:hover { text-decoration: underline; }

/* 查询区 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  background: #fff;
  padding: 10px 12px;
  border-radius: 4px;
  margin-bottom: 10px;
  flex-shrink: 0;
}
.wh-radio { flex-shrink: 0; }
.wh-select { width: 140px; }
.search-input { width: 140px; }
.btn-search { flex-shrink: 0; }
.only-low-chk { margin-left: 4px; }

/* 内容行 */
.content-row {
  display: flex;
  flex: 1;
  min-height: 0;
}
.category-panel {
  width: 220px;
  min-width: 190px;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fafafa;
}
.category-title { font-weight: 600; font-size: 14px; color: #303133; }
.collapse-btn { padding: 0; color: #999; }
.category-tree { flex: 1; overflow-y: auto; padding: 8px; min-height: 0; }
.cat-count { font-size: 12px; color: #999; margin-left: 4px; }
.cat-folder { color: #faad14; }

.table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.ss-table-wrap) { border: none; }
:deep(.ant-input-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector) { height: 28px; line-height: 28px; }
</style>
