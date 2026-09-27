<template>
  <ErrorBoundary>
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="true"
        category-title="商品分类"
        :category-editable="false"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :tabs="[]"
        :active-tab="''"
        :current-path="currentCategoryPath"
        :show-table-footer="!tableExpanded"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
        @category-expand="onExpand"
        @search="handleSearch"
      >
        <!-- 工具栏右侧：刷新 / 打印(F8) / 条码打印 / 导出（对标无「新增」，条码随商品单位维护） -->
        <template #toolbar-right>
          <a-space>
            <a-button
              size="small"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleBarcodePrint"
            >
              <BarcodeOutlined /> 条码打印
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- 查询条件 -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入商品名称/货号/条码"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">条码</span>
              <a-select
                v-model:value="searchForm.barcodeFilter"
                size="small"
                style="width: 130px"
                :options="BARCODE_FILTER_OPTIONS"
              />
            </div>
            <div class="search-item">
              <span class="search-label">上架状态</span>
              <a-select
                v-model:value="searchForm.shelfStatus"
                size="small"
                style="width: 120px"
                :options="SHELF_STATUS_OPTIONS"
              />
            </div>
            <div class="search-item">
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="searchForm.status"
                size="small"
                style="width: 120px"
                :options="SHOW_STATUS_OPTIONS"
              />
            </div>
            <div class="search-item">
              <span class="search-label">新增时间</span>
              <a-select
                v-model:value="searchForm.createTimeOp"
                size="small"
                style="width: 62px"
                :options="COMPARE_OPTIONS"
              />
              <a-date-picker
                v-model:value="searchForm.createTimeStart"
                size="small"
                style="width: 140px"
              />
            </div>
            <div class="search-item">
              <span class="search-label">采购日期</span>
              <a-select
                v-model:value="searchForm.purchaseDateOp"
                size="small"
                style="width: 62px"
                :options="COMPARE_OPTIONS"
              />
              <a-date-picker
                v-model:value="searchForm.purchaseDateStart"
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

        <!-- 数据表 -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="detailColumns"
            v-model:data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :fill-mode="true"
            storage-key="md-barcode-list-columns"
            @checkbox-change="handleCheckboxChange"
            @checkbox-all="handleCheckboxAll"
            @expand-change="onTableExpand"
            @sort-change="handleSortChange"
          >
            <template #actionCell="{ record }">
              <a-button
                v-if="record && record.unitId"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                修改
              </a-button>
            </template>

            <template #imageCell="{ record }">
              <a-image
                v-if="record.imageUrl"
                :src="record.imageUrl"
                style="width: 36px; height: 36px; border-radius: 4px; object-fit: cover;"
                :preview="({ mask: false } as any)"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
              />
              <span
                v-else
                class="no-image"
              >-</span>
            </template>

            <template #productNameCell="{ record }">
              <span>{{ record.productName }}</span>
            </template>

            <template #conversionCell="{ record }">
              <span>{{ record.conversionRelation || '-' }}</span>
            </template>

            <template #shelfStatusCell="{ record }">
              <template v-if="record && record.unitId">
                <a-tag
                  v-if="record.shelfStatus === 1"
                  color="green"
                >
                  已上架
                </a-tag>
                <a-tag v-else>
                  未上架
                </a-tag>
              </template>
            </template>
          </BillDetailTable>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[10, 20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- 条码打印：选中行生成可扫描条码标签 -->
      <a-modal
        v-model:open="barcodePrintVisible"
        title="条码打印"
        :width="760"
        :footer="null"
      >
        <a-alert
          v-if="barcodePrintRows.length === 0"
          type="warning"
          show-icon
          message="请先在列表勾选需要打印条码的商品行"
        />
        <template v-else>
          <div class="barcode-print-toolbar">
            <span>已选 {{ barcodePrintRows.length }} 行</span>
            <span class="barcode-print-copies">
              每张份数
              <a-input-number
                v-model:value="barcodeCopies"
                :min="1"
                :max="20"
                size="small"
                style="width: 80px"
              />
            </span>
            <a-button
              type="primary"
              size="small"
              @click="doBarcodePrint"
            >
              <PrinterOutlined /> 打印
            </a-button>
          </div>
          <div class="barcode-preview">
            <div
              v-for="row in barcodePrintRows"
              :key="row.unitId"
              class="barcode-label"
            >
              <div class="barcode-label-name">
                {{ row.productName }}
              </div>
              <div
                class="barcode-label-svg"
                v-html="barcodeSvg(row)"
              />
              <div class="barcode-label-text">
                {{ row.barcode || '未设置条码' }}
              </div>
              <div class="barcode-label-meta">
                {{ row.productCode }} / {{ row.unitName }}
              </div>
            </div>
          </div>
        </template>
      </a-modal>

      <!-- 打印(F8)：统一打印弹窗 -->
      <PrintDialog
        ref="printDialogRef"
        page-code="md:barcode"
        :print-data="printData"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  BarcodeOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { productBarcodeApi, productCategoryApi } from '@/api/erp/product'
import type { ProductBarcodeRow } from '@/api/erp/product'
import { encodeCode128B } from '@/utils/barcode'

/** 查询下拉选项：严格对齐对标 ql361 商品条码页实抓值域 */
const BARCODE_FILTER_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: 'NONE', label: '无条码' },
  { value: 'HAS', label: '有条码' },
]
const SHELF_STATUS_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 1, label: '已上架' },
  { value: 0, label: '未上架' },
]
const SHOW_STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'ENABLED', label: '已启用' },
  { value: 'DISABLED', label: '已停用' },
]
/** 日期比较符（对标下拉：< = > ≠ ≤ ≥，默认 ≥） */
const COMPARE_OPTIONS = [
  { value: '<', label: '<' },
  { value: '=', label: '=' },
  { value: '>', label: '>' },
  { value: '!=', label: '≠' },
  { value: '<=', label: '≤' },
  { value: '>=', label: '≥' },
]

const router = useRouter()

// ── 分类树 ──
/** 对标 ql361：分类树根节点「全部商品」，选中表示不做分类过滤（与图片管理/商品列表同口径） */
const ROOT_CATEGORY_ID = '0'
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>(ROOT_CATEGORY_ID)
const expandedKeys = ref<string[]>([])
const categoryTreeData = computed(() => categoryTree.value)

const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === ROOT_CATEGORY_ID || !categoryTree.value.length) return '全部商品'
  const path: string[] = []
  function find(nodes: any[], target: string): boolean {
    for (const node of nodes) {
      // 根节点「全部商品」不作为路径前缀（对标底部当前路径只显示业务分类层级）
      if (String(node.id) === ROOT_CATEGORY_ID) {
        if (node.children?.length && find(node.children, target)) return true
        continue
      }
      path.push(node.categoryName)
      if (String(node.id) === target) return true
      if (node.children?.length && find(node.children, target)) return true
      path.pop()
    }
    return false
  }
  find(categoryTree.value, selectedCategoryId.value)
  return path.length ? path.join(' / ') : '全部商品'
})

/** 只保留树渲染所需字段：对标 ql361 分类树节点**不显示商品数量**，故丢弃 productCount */
function normalizeCategoryTree(nodes: any[]): any[] {
  return nodes.map(n => ({
    id: String(n.id),
    categoryName: n.categoryName,
    children: n.children?.length ? normalizeCategoryTree(n.children) : undefined,
  }))
}

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data: any = await productCategoryApi.getTree()
    const realTree = normalizeCategoryTree(Array.isArray(data) ? data : [])
    // 对标 ql361 实测：商品条码页分类树根节点为「全部商品」（选中=不过滤），默认只展开根节点
    categoryTree.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: realTree }]
    expandedKeys.value = [ROOT_CATEGORY_ID]
  } catch (e) {
    console.error('[商品条码] 加载商品分类失败', e)
    categoryError.value = true
    categoryTree.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品' }]
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  // 取消选中时回落到根节点「全部商品」
  selectedCategoryId.value = key != null ? String(key) : ROOT_CATEGORY_ID
  pagination.current = 1
  fetchList()
}

function onExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

// ── 查询表单 ──
const searchForm = reactive({
  keyword: '',
  barcodeFilter: 'ALL' as string,
  shelfStatus: -1 as number,
  status: 'ENABLED' as string,
  createTimeOp: '>=' as string,
  createTimeStart: null as any,
  purchaseDateOp: '>=' as string,
  purchaseDateStart: null as any,
})

function handleSearch() {
  pagination.current = 1
  fetchList()
}

// ── 列表 ──
const loading = ref(false)
const tableData = ref<ProductBarcodeRow[]>([])
const selectedRows = ref<ProductBarcodeRow[]>([])
const tableRef = ref<any>(null)
const tableExpanded = ref(false)

function onTableExpand(expanded: boolean) {
  tableExpanded.value = expanded
}

/** 表头排序（对标 ql361：商品名称/货号/条码可排序）—— 交后端整表排序，不是当前页本地排序 */
const sortState = reactive<{ field?: string; order?: string }>({})
function handleSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const detailColumns = computed<DetailColumnConfig[]>(() => {
  const fixedColumns: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  ]
  // 对标列配置（12 列）：默认显示前 7 列，规格/型号/产地/新增时间/最近采购日期默认隐藏
  const dataColumns: DetailColumnConfig[] = [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60 },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 220, sortable: true },
    { key: 'productCode', title: '货号', type: 'input', width: 130, sortable: true },
    { key: 'unitName', title: '单位', type: 'input', width: 80 },
    {
      key: 'conversionRelation',
      title: '换算关系',
      type: 'slot',
      slotName: 'conversionCell',
      width: 140,
      headerTip: '非基础单位显示与基础单位的换算，如 1箱=6瓶；基础单位不显示',
    },
    { key: 'shelfStatusText', title: '上架状态', type: 'slot', slotName: 'shelfStatusCell', width: 100 },
    { key: 'barcode', title: '条码', type: 'input', width: 170, sortable: true },
    { key: 'spec', title: '规格', type: 'input', width: 120, defaultHidden: true },
    { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
    { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
    {
      key: 'createTime',
      title: '新增时间',
      type: 'input',
      width: 150,
      defaultHidden: true,
      formatter: (value: any) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : ''),
    },
    { key: 'lastPurchaseDate', title: '最近采购日期', type: 'input', width: 120, defaultHidden: true },
  ]
  return [...fixedColumns, ...dataColumns]
})

function buildQueryParams() {
  return {
    categoryId: selectedCategoryId.value !== ROOT_CATEGORY_ID ? selectedCategoryId.value : undefined,
    keyword: searchForm.keyword || undefined,
    barcodeFilter: searchForm.barcodeFilter || 'ALL',
    shelfStatus: searchForm.shelfStatus === -1 ? undefined : searchForm.shelfStatus,
    status: searchForm.status || undefined,
    createTimeOp: searchForm.createTimeStart ? searchForm.createTimeOp : undefined,
    createTimeStart: searchForm.createTimeStart ? dayjs(searchForm.createTimeStart).format('YYYY-MM-DD') : undefined,
    purchaseDateOp: searchForm.purchaseDateStart ? searchForm.purchaseDateOp : undefined,
    purchaseDateStart: searchForm.purchaseDateStart ? dayjs(searchForm.purchaseDateStart).format('YYYY-MM-DD') : undefined,
    sortField: sortState.field,
    sortOrder: sortState.order,
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await productBarcodeApi.page({
      ...buildQueryParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[商品条码] 加载列表失败', e)
    message.error('加载商品条码失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleCheckboxChange() {
  selectedRows.value = tableRef.value?.getCheckedRecords?.() || []
}

function handleCheckboxAll(_checked: boolean, records: ProductBarcodeRow[]) {
  selectedRows.value = records || []
}

function refreshAll() {
  fetchCategoryTree()
  fetchList()
}

// ── 行级修改：对标 ql361 打开「商品-新增与编辑」页，在该页「商品单位」明细中维护条码 ──
function handleEdit(record: ProductBarcodeRow) {
  if (!record?.productId) {
    message.warning('该行缺少商品信息，无法打开商品档案')
    return
  }
  router.push(`/erp/product/form/${record.productId}`)
}

// ── 导出真实 Excel ──
async function handleExport() {
  try {
    const blob = await productBarcodeApi.export(buildQueryParams())
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商品条码_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.error('[商品条码] 导出失败', e)
    message.error('导出失败')
  }
}

// ── 打印(F8)：列表打印 ──
const printDialogRef = ref<any>(null)
const printData = computed<Record<string, any>>(() => ({
  pageTitle: '商品条码',
  rows: (selectedRows.value.length ? selectedRows.value : tableData.value).map(row => ({
    productName: row.productName,
    productCode: row.productCode,
    unitName: row.unitName,
    conversionRelation: row.conversionRelation || '',
    shelfStatusText: row.shelfStatusText,
    barcode: row.barcode || '',
    spec: row.spec || '',
    model: row.model || '',
    origin: row.origin || '',
    createTime: row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss') : '',
    lastPurchaseDate: row.lastPurchaseDate || '',
  })),
  total: pagination.total,
  printTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
}))

function handlePrint() {
  printDialogRef.value?.open?.()
}

// ── 条码打印（标签打印，不走报表模板引擎） ──
// 这条与上面的「打印(F8)」是两种东西：那是**结果集报表**（PrintDialog + 模板引擎），
// 这是**条码标签**——Code128 矢量条码 + 标签纸尺寸 + 每行份数，模板引擎画不了矢量条码，故保留独立实现。
const barcodePrintVisible = ref(false)
const barcodeCopies = ref(1)
const barcodePrintRows = ref<ProductBarcodeRow[]>([])

function handleBarcodePrint() {
  const rows = selectedRows.value.filter(row => !!row.barcode)
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要打印条码的商品行')
    return
  }
  if (rows.length === 0) {
    message.warning('所选行均未设置条码，无法打印')
    return
  }
  const unsupported = rows.filter(row => !encodeCode128B(row.barcode))
  if (unsupported.length > 0) {
    message.warning(`有 ${unsupported.length} 行条码含非 ASCII 字符（如中文），将以文本展示`)
  }
  barcodePrintRows.value = rows
  barcodePrintVisible.value = true
}

function barcodeSvg(row: ProductBarcodeRow): string {
  const encoded = encodeCode128B(row.barcode)
  if (!encoded) {
    return `<span class="barcode-unsupported">${escapeHtml(row.barcode)}</span>`
  }
  const moduleWidth = 1.5
  const height = 44
  const width = encoded.modules * moduleWidth
  const rects = encoded.bars
    .map(bar => `<rect x="${(bar.x * moduleWidth).toFixed(2)}" y="0" width="${(bar.width * moduleWidth).toFixed(2)}" height="${height}"/>`)
    .join('')
  return `<svg width="${width.toFixed(2)}" height="${height}" viewBox="0 0 ${width.toFixed(2)} ${height}"><g fill="#000">${rects}</g></svg>`
}

const HTML_ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
}

function escapeHtml(text: string | null | undefined): string {
  return (text ?? '').replace(/[&<>"']/g, ch => HTML_ESCAPES[ch] ?? ch)
}

function doBarcodePrint() {
  const win = window.open('', '_blank', 'width=900,height=650')
  if (!win) {
    message.error('浏览器拦截了打印窗口，请允许弹出窗口后重试')
    return
  }
  const labels = barcodePrintRows.value.map(row => {
    const svg = barcodeSvg(row)
    return Array.from({ length: barcodeCopies.value })
      .map(() => `
        <div class="label">
          <div class="name">${escapeHtml(row.productName)}</div>
          ${svg}
          <div class="code">${escapeHtml(row.barcode || '')}</div>
          <div class="meta">${escapeHtml(row.productCode || '')} / ${escapeHtml(row.unitName || '')}</div>
        </div>`)
      .join('')
  }).join('')

  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>条码打印</title>
    <style>
      * { box-sizing: border-box; }
      body { margin: 0; padding: 12px; font-family: "Microsoft YaHei", Arial, sans-serif; }
      .label { display: inline-block; vertical-align: top; width: 240px; margin: 6px; padding: 8px;
               border: 1px dashed #ccc; text-align: center; page-break-inside: avoid; }
      .name { font-size: 12px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .code { font-size: 12px; letter-spacing: 1px; margin-top: 2px; }
      .meta { font-size: 11px; color: #666; margin-top: 2px; }
      @media print { .label { border: none; } }
    </style></head><body>${labels}</body></html>`)
  win.document.close()
  win.focus()
  setTimeout(() => {
    win.print()
  }, 300)
}

// ── 快捷键：F8 打印 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchCategoryTree()
  fetchList()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
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
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 8px;
}
.no-image {
  color: #bbb;
}
.barcode-print-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.barcode-print-copies {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}
.barcode-preview {
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
}
.barcode-label {
  display: inline-block;
  width: 220px;
  margin: 6px;
  padding: 8px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  text-align: center;
  vertical-align: top;
}
.barcode-label-name {
  font-size: 12px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.barcode-label-text {
  font-size: 12px;
  letter-spacing: 1px;
}
.barcode-label-meta {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}
.barcode-unsupported {
  font-size: 12px;
  color: #d46b08;
}
</style>
