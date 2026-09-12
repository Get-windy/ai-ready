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
        <!-- 工具栏：刷新 / 打印(F8) / 条码打印 / 导出 / 更多（对标无「新增」，本页只维护推荐货位） -->
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
            <a-dropdown :trigger="['click']">
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="onMoreMenuClick">
                  <a-menu-item key="batchSet">
                    批量设置
                  </a-menu-item>
                  <a-menu-item key="batchRemove">
                    批量移除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- 查询条件：两行，与对标一致（仓库为必填项） -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="商品名称/货号"
                size="small"
                style="width: 190px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">品牌</span>
              <a-input
                v-model:value="searchForm.brand"
                placeholder="品牌"
                size="small"
                style="width: 140px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">仓库</span>
              <a-select
                v-model:value="searchForm.warehouseId"
                size="small"
                style="width: 180px"
                show-search
                option-filter-prop="label"
                :options="warehouseSelectOptions"
                @change="onWarehouseChange"
              />
            </div>
            <div class="search-item">
              <span class="search-label">货位</span>
              <a-input
                v-model:value="searchForm.locationCode"
                placeholder="货位"
                size="small"
                style="width: 140px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">条码</span>
              <a-select
                v-model:value="searchForm.hasBarcodeStatus"
                size="small"
                style="width: 110px"
                :options="BARCODE_STATUS_OPTIONS"
              />
            </div>
            <div class="search-item">
              <span class="search-label">上架状态</span>
              <a-select
                v-model:value="searchForm.shelfStatus"
                size="small"
                style="width: 110px"
                :options="SHELF_STATUS_OPTIONS"
              />
            </div>
          </div>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="searchForm.showStop"
                size="small"
                style="width: 120px"
                :options="SHOW_STATUS_OPTIONS"
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
            <a-checkbox
              v-model:checked="searchForm.onlyUnsettedGoods"
              @change="handleSearch"
            >
              仅显示未设置货位的商品
            </a-checkbox>
            <a-checkbox
              v-model:checked="searchForm.onlyStockGoods"
              @change="handleSearch"
            >
              仅显示有库存的商品
            </a-checkbox>
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
            storage-key="md-location-columns"
            global-config-key="md-location-columns"
            @checkbox-change="handleCheckboxChange"
            @checkbox-all="handleCheckboxAll"
            @expand-change="onTableExpand"
            @sort-change="onSortChange"
          >
            <template #actionCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-button
                  type="link"
                  size="small"
                  @click="openSetLocation([record])"
                >
                  设置
                </a-button>
                <a-button
                  v-if="record.locationId"
                  type="link"
                  size="small"
                  danger
                  @click="removeOne(record)"
                >
                  删除
                </a-button>
              </template>
            </template>

            <template #imageCell="{ record }">
              <template v-if="record && !record.__ghost">
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
            </template>

            <template #shelfCell="{ record }">
              <span
                v-if="record && !record.__ghost"
                :class="record.shelfStatus === 1 ? 'shelf-on' : 'shelf-off'"
              >
                {{ record.shelfStatus === 1 ? '√' : '×' }}
              </span>
            </template>

            <template #locationCell="{ record }">
              <template v-if="record && !record.__ghost">
                <span v-if="record.locationCode">{{ record.locationCode }}</span>
                <span
                  v-else
                  class="unset-location"
                >未设置</span>
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

      <!-- 选择货位（设置推荐货位）：对标 GoodsPositionSelector —— 按所选仓库加载真实货位 -->
      <a-modal
        v-model:open="locationModalVisible"
        title="选择货位"
        :width="620"
        :confirm-loading="saving"
        ok-text="确定"
        cancel-text="取消"
        @ok="confirmSetLocation"
      >
        <div class="loc-modal-tip">
          <span>仓库</span>
          <a-select
            v-model:value="targetWarehouseId"
            placeholder="请选择仓库"
            size="small"
            style="width: 180px; margin: 0 8px;"
            show-search
            option-filter-prop="label"
            :options="pickerWarehouseOptions"
            @change="loadLocations"
          />
          <span class="loc-modal-count">本次设置 {{ pendingRows.length }} 个商品</span>
        </div>
        <a-table
          :data-source="locationList"
          :columns="locationColumns"
          :loading="locationLoading"
          :pagination="false"
          :row-selection="locationRowSelection"
          :scroll="{ y: 300 }"
          row-key="id"
          size="small"
          bordered
        >
          <template #emptyText>
            <a-empty description="该仓库暂无货位，请先在「仓库规划」中维护货位" />
          </template>
        </a-table>
        <div class="loc-modal-remark">
          <span>备注</span>
          <a-input
            v-model:value="locationRemark"
            size="small"
            style="width: 260px"
            allow-clear
            placeholder="选填，写入列表「备注」列"
          />
        </div>
      </a-modal>

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
              :key="row.productId"
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
                {{ row.productCode }} / {{ row.unit }}
              </div>
            </div>
          </div>
        </template>
      </a-modal>

      <!-- 打印(F8)：统一打印弹窗 -->
      <PrintDialog
        ref="printDialogRef"
        page-code="md:location"
        :print-data="printData"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  BarcodeOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { productLocationApi, productCategoryApi } from '@/api/erp/product'
import type { ProductLocationRow } from '@/api/erp/product'
import { optionsApi } from '@/api/options'
import { locationApi } from '@/api/wms/warehouse'
import { encodeCode128B } from '@/utils/barcode'

/** 查询下拉选项：严格对齐对标 ql361 商品货位设置页实抓值域 */
const BARCODE_STATUS_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 1, label: '无条码' },
  { value: 2, label: '有条码' },
]
const SHELF_STATUS_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 1, label: '已上架' },
  { value: 0, label: '未上架' },
]
const SHOW_STATUS_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 2, label: '已启用' },
  { value: 1, label: '已停用' },
]

// ── 仓库（口径 erp_warehouse.id，与 wms_location.warehouse_id 同源）──
// 默认「全部仓库」：不按仓库过滤，一个商品一行，绑定按商品聚合（仓库名/货位编码以逗号拼接）
const ALL_WAREHOUSES = 'ALL'
const warehouseList = ref<{ value: string; label: string }[]>([])
const warehouseSelectOptions = computed(() => [
  { value: ALL_WAREHOUSES, label: '全部仓库' },
  ...warehouseList.value,
])
/** 设置弹窗内的仓库选择（不含「全部仓库」——设置必须落到具体仓库） */
const pickerWarehouseOptions = computed(() => warehouseSelectOptions.value.filter(o => o.value !== ALL_WAREHOUSES))

async function fetchWarehouses() {
  try {
    const list: any[] = await optionsApi.getWarehouses()
    warehouseList.value = (Array.isArray(list) ? list : []).map(w => ({
      value: String(w.warehouseId ?? w.id),
      label: w.warehouseName || w.name || '',
    }))
  } catch (e) {
    console.error('[商品货位设置] 加载仓库失败', e)
    warehouseList.value = []
  }
}

// ── 分类树 ──
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>('0')
const expandedKeys = ref<string[]>([])
const categoryTreeData = computed(() => categoryTree.value)

const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !categoryTree.value.length) return '全部商品'
  const path: string[] = []
  function find(nodes: any[], target: string): boolean {
    for (const node of nodes) {
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

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => String(n.id))
    if (firstLevel.length > 0) {
      expandedKeys.value = [...new Set([...firstLevel, ...expandedKeys.value])]
    }
  } catch (e) {
    console.error('[商品货位设置] 加载商品分类失败', e)
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  selectedCategoryId.value = key != null ? String(key) : '0'
  handleSearch()
}

function onExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

// ── 查询表单 ──
const searchForm = reactive({
  keyword: '',
  brand: '',
  warehouseId: ALL_WAREHOUSES as string,
  locationCode: '',
  hasBarcodeStatus: -1,
  shelfStatus: -1,
  showStop: 2,
  onlyUnsettedGoods: false,
  onlyStockGoods: false,
})

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function onWarehouseChange() {
  pagination.current = 1
  fetchList()
}

/** 当前筛选是否「全部仓库」 */
const isAllWarehouses = computed(() => !searchForm.warehouseId || searchForm.warehouseId === ALL_WAREHOUSES)

// ── 列表 ──
const loading = ref(false)
const tableData = ref<ProductLocationRow[]>([])
/** 接口返回的默认顺序副本（取消排序时还原） */
const rawTableData = ref<ProductLocationRow[]>([])
const selectedRows = ref<ProductLocationRow[]>([])
const tableRef = ref<any>(null)
const tableExpanded = ref(false)
const sortState = reactive<{ key: string | null; order: 'asc' | 'desc' | null }>({ key: null, order: null })

function onTableExpand(expanded: boolean) {
  tableExpanded.value = expanded
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
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  ]
  // 对标列配置弹窗实测 24 列，默认显示 11 列（图片/商品名称/货号/上架/单位/规格/型号/仓库/推荐货位/备注/修改时间）
  const dataColumns: DetailColumnConfig[] = [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60 },
    { key: 'productName', title: '商品名称', type: 'input', width: 220, sortable: true },
    { key: 'productCode', title: '货号', type: 'input', width: 130, sortable: true },
    { key: 'shelfStatus', title: '上架', type: 'slot', slotName: 'shelfCell', width: 60 },
    { key: 'unit', title: '单位', type: 'input', width: 80 },
    { key: 'barcode', title: '条码', type: 'input', width: 150, defaultHidden: true },
    { key: 'spec', title: '规格', type: 'input', width: 120 },
    { key: 'model', title: '型号', type: 'input', width: 100 },
    { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
    { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },
    { key: 'warehouseName', title: '仓库', type: 'input', width: 130, sortable: true },
    { key: 'locationCode', title: '推荐货位', type: 'slot', slotName: 'locationCell', width: 130, sortable: true },
    { key: 'retailPrice', title: '零售价', type: 'number', width: 100, defaultHidden: true },
    { key: 'wholesalePrice', title: '批发价', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice1', title: '价格等级1', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice2', title: '价格等级2', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice3', title: '价格等级3', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice4', title: '价格等级4', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice5', title: '价格等级5', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice6', title: '价格等级6', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice7', title: '价格等级7', type: 'number', width: 100, defaultHidden: true },
    { key: 'gradePrice8', title: '价格等级8', type: 'number', width: 100, defaultHidden: true },
    { key: 'remark', title: '备注', type: 'input', width: 180 },
    {
      key: 'modifyTime',
      title: '修改时间',
      type: 'input',
      width: 160,
      formatter: (value: any) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : ''),
    },
  ]
  return [...fixedColumns, ...dataColumns]
})

function buildQueryParams() {
  return {
    warehouseId: isAllWarehouses.value ? undefined : searchForm.warehouseId,
    categoryId: selectedCategoryId.value !== '0' ? selectedCategoryId.value : undefined,
    keyword: searchForm.keyword || undefined,
    brand: searchForm.brand || undefined,
    locationCode: searchForm.locationCode || undefined,
    hasBarcodeStatus: searchForm.hasBarcodeStatus,
    shelfStatus: searchForm.shelfStatus,
    showStop: searchForm.showStop,
    onlyUnsettedGoods: searchForm.onlyUnsettedGoods || undefined,
    onlyStockGoods: searchForm.onlyStockGoods || undefined,
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await productLocationApi.page({
      ...buildQueryParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
    rawTableData.value = [...tableData.value]
    applyLocalSort()
  } catch (e: any) {
    console.error('[商品货位设置] 加载列表失败', e)
    message.error(e?.message || '加载商品货位设置失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 排序（对标商品名称/货号/仓库/推荐货位列可排序）：对当前页数据本地排序 */
function onSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  sortState.key = key
  sortState.order = order
  applyLocalSort()
}

function applyLocalSort() {
  const { key, order } = sortState
  // 取消排序（第三次点击）回到接口返回的默认顺序
  if (!key || !order) {
    tableData.value = [...rawTableData.value]
    return
  }
  const dir = order === 'asc' ? 1 : -1
  tableData.value = [...tableData.value].sort((a: any, b: any) => {
    const va = a?.[key]
    const vb = b?.[key]
    if (va == null && vb == null) return 0
    if (va == null) return 1
    if (vb == null) return -1
    if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
    return String(va).localeCompare(String(vb), 'zh-CN') * dir
  })
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleCheckboxChange() {
  selectedRows.value = tableRef.value?.getCheckedRecords?.() || []
}

function handleCheckboxAll(_checked: boolean, records: ProductLocationRow[]) {
  selectedRows.value = records || []
}

function refreshAll() {
  fetchWarehouses()
  fetchCategoryTree()
  fetchList()
}

// ── 设置推荐货位 ──
const locationModalVisible = ref(false)
const locationLoading = ref(false)
const locationList = ref<any[]>([])
const locationRemark = ref('')
const saving = ref(false)
const pickedLocationId = ref<string | undefined>(undefined)
const pendingRows = ref<ProductLocationRow[]>([])
/** 本次设置的仓库（「全部仓库」筛选下由弹窗内选择） */
const targetWarehouseId = ref<string | undefined>(undefined)

const locationColumns = [
  { title: '货位编码', dataIndex: 'locationCode', width: 160 },
  { title: '货位名称', dataIndex: 'locationName', width: 180 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 90,
    customRender: ({ text }: any) => LOCATION_STATUS_MAP[text] || '-',
  },
]
const LOCATION_STATUS_MAP: Record<number, string> = { 1: '空闲', 2: '占用', 3: '冻结', 4: '维修' }

const locationRowSelection = computed(() => ({
  type: 'radio' as const,
  selectedRowKeys: pickedLocationId.value ? [pickedLocationId.value] : [],
  onChange: (keys: (string | number)[]) => {
    pickedLocationId.value = keys[0] != null ? String(keys[0]) : undefined
  },
}))

/** 按当前目标仓库加载真实货位（wms_location） */
async function loadLocations() {
  locationList.value = []
  if (!targetWarehouseId.value) return
  locationLoading.value = true
  try {
    const list: any[] = await locationApi.listByWarehouse(Number(targetWarehouseId.value))
    locationList.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.error('[商品货位设置] 加载货位失败', e)
    message.error('加载货位失败')
    locationList.value = []
  } finally {
    locationLoading.value = false
  }
}

async function openSetLocation(rows: ProductLocationRow[]) {
  const targets = (rows || []).filter(r => r && r.productId)
  if (targets.length === 0) {
    message.warning('请至少选择一个商品')
    return
  }
  pendingRows.value = targets
  pickedLocationId.value = targets.length === 1 ? targets[0].locationId : undefined
  locationRemark.value = targets.length === 1 ? (targets[0].remark || '') : ''
  // 「全部仓库」筛选下（尤其中批量）每行仓库可能不同，需在弹窗内显式选择，避免设错仓库
  targetWarehouseId.value = isAllWarehouses.value
    ? (targets.length === 1 ? targets[0].warehouseId : undefined)
    : searchForm.warehouseId
  locationModalVisible.value = true
  await loadLocations()
}

async function confirmSetLocation() {
  if (!targetWarehouseId.value) {
    message.warning('请选择仓库')
    return
  }
  if (!pickedLocationId.value) {
    message.warning('请选择货位')
    return
  }
  saving.value = true
  try {
    await productLocationApi.set({
      warehouseId: String(targetWarehouseId.value),
      productIds: pendingRows.value.map(r => String(r.productId)),
      locationId: String(pickedLocationId.value),
      remark: locationRemark.value || undefined,
    })
    message.success('设置成功')
    locationModalVisible.value = false
    tableRef.value?.clearSelection?.()
    selectedRows.value = []
    fetchList()
  } catch (e: any) {
    console.error('[商品货位设置] 设置失败', e)
    message.error(e?.message || '设置失败')
  } finally {
    saving.value = false
  }
}

// ── 移除推荐货位 ──
function removeOne(record: ProductLocationRow) {
  doBatchRemove([record])
}

function doBatchRemove(rows: ProductLocationRow[]) {
  const targets = (rows || []).filter(r => r && r.productId && r.warehouseId)
  if (targets.length === 0) {
    message.warning('请选择已设置货位的商品')
    return
  }
  // 「全部仓库」下一个商品可能绑定了多个仓库的货位，按行上的仓库分组删除
  const groups = new Map<string, string[]>()
  targets.forEach(r => {
    const wid = String(r.warehouseId)
    if (!groups.has(wid)) groups.set(wid, [])
    groups.get(wid)!.push(String(r.productId))
  })
  Modal.confirm({
    title: '确认信息',
    content: '确定要删除推荐货位？',
    async onOk() {
      try {
        for (const [wid, productIds] of groups) {
          await productLocationApi.batchRemove({ warehouseId: wid, productIds })
        }
        message.success('删除成功')
        tableRef.value?.clearSelection?.()
        selectedRows.value = []
        fetchList()
      } catch (e: any) {
        console.error('[商品货位设置] 删除失败', e)
        message.error(e?.message || '删除失败')
      }
    },
  })
}

function onMoreMenuClick(info: { key: string | number }) {
  const key = String(info.key)
  if (selectedRows.value.length === 0) {
    message.warning('请至少选中一条数据')
    return
  }
  if (key === 'batchSet') {
    openSetLocation(selectedRows.value)
  } else if (key === 'batchRemove') {
    doBatchRemove(selectedRows.value)
  }
}

// ── 导出真实 Excel ──
async function handleExport() {
  try {
    const blob = await productLocationApi.export(buildQueryParams())
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商品货位设置_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.error('[商品货位设置] 导出失败', e)
    message.error('导出失败')
  }
}

// ── 打印(F8)：列表打印 ──
const printDialogRef = ref<any>(null)
const printData = computed<Record<string, any>>(() => ({
  pageTitle: '商品货位设置',
  rows: (selectedRows.value.length ? selectedRows.value : tableData.value).map(row => ({
    productName: row.productName,
    productCode: row.productCode,
    shelfStatusText: row.shelfStatus === 1 ? '√' : '×',
    unit: row.unit || '',
    barcode: row.barcode || '',
    spec: row.spec || '',
    model: row.model || '',
    origin: row.origin || '',
    brand: row.brand || '',
    warehouseName: row.warehouseName || '',
    locationCode: row.locationCode || '',
    remark: row.remark || '',
    modifyTime: row.modifyTime ? dayjs(row.modifyTime).format('YYYY-MM-DD HH:mm:ss') : '',
  })),
  total: pagination.total,
  printTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
}))

function handlePrint() {
  printDialogRef.value?.open?.()
}

// ── 条码打印 ──
const barcodePrintVisible = ref(false)
const barcodeCopies = ref(1)
const barcodePrintRows = ref<ProductLocationRow[]>([])

function handleBarcodePrint() {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要打印条码的商品行')
    return
  }
  const rows = selectedRows.value.filter(row => !!row.barcode)
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

function barcodeSvg(row: ProductLocationRow): string {
  const encoded = encodeCode128B(row.barcode || '')
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
          <div class="meta">${escapeHtml(row.productCode || '')} / ${escapeHtml(row.unit || '')}</div>
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
  fetchWarehouses()
  fetchCategoryTree()
  // 默认「全部仓库」，进入页面即加载数据（无需先选仓库）
  fetchList()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* 插槽内容样式需自备（Vue scoped 不作用到布局组件内部的插槽内容） */
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row + .search-row {
  margin-top: 8px;
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
.search-label.required::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 2px;
}
.btn-search {
  margin-left: 8px;
}
.no-image {
  color: #bbb;
}
.shelf-on {
  color: #52c41a;
  font-weight: 600;
}
.shelf-off {
  color: #d9d9d9;
}
.unset-location {
  color: #bbb;
}
.loc-modal-tip {
  margin-bottom: 10px;
  font-size: 13px;
  color: #666;
}
.loc-modal-count {
  margin-left: 12px;
  color: #1890ff;
}
.loc-modal-remark {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: #666;
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
