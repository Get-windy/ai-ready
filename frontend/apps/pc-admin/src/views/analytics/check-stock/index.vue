<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查库存（分析 → 仓配分析 → 查库存，菜单 80431）
        对标 ql361「分析 → 仓配分析 → 查库存」：三视图 Tab（当前库存 54/8、按属性 50/6、库存分布 33/2），
        逐 Tab 独立列配置（storage-key + global-config-key 同值、按 Tab 换 key）；
        查询区「查询方案 / 仓库*(≤10仓) / 商品 / 品牌 / 显示列 / 显示状态」+ 横向网格；
        工具栏 刷新｜打印(F8)｜导出｜页面配置；行级「明细 / 对账」；
        左侧商品分类树仅「当前库存」视图有（对标实测）。
        取数：/erp/stock/page（erp_stock，商品×仓库时点存量）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="hasCategoryTree"
        category-title="商品分类"
        :category-tree-data="categoryTree"
        :category-loading="categoryLoading"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 6 项，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('checkStock.scheme')" class="search-item">
                <span class="search-label">查询方案</span>
                <QuerySchemeBar
                  storage-key="analytics-check-stock\index.vue-query-scheme"
                  :snapshot="querySnapshot"
                  @apply="applyQuerySnapshot"
                />
              </div>
              <div v-if="isQueryVisible('checkStock.warehouseIds')" class="search-item">
                <span class="search-label">仓库<span class="required-mark">*</span></span>
                <a-select
                  v-model:value="query.warehouseIds"
                  mode="multiple"
                  size="small"
                  placeholder="选择仓库"
                  :max-tag-count="2"
                  style="width: 240px"
                  :options="warehouseOptions"
                  @change="handleSearch"
                />
                <span class="search-tip">默认查询只查询10个仓库,查询更多库存请手动选择!</span>
              </div>
              <div v-if="isQueryVisible('checkStock.keyword')" class="search-item">
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="商品名称/货号/条码/规格/型号"
                  allow-clear
                  style="width: 220px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('checkStock.brand')" class="search-item">
                <span class="search-label">品牌</span>
                <a-input
                  v-model:value="query.brand"
                  size="small"
                  placeholder="品牌"
                  allow-clear
                  style="width: 120px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('checkStock.showColumn')" class="search-item">
                <span class="search-label">显示列</span>
                <a-select v-model:value="query.showColumn" size="small" style="width: 120px" :options="SHOW_COLUMN_OPTIONS" @change="handleSearch" />
              </div>
              <div v-if="isQueryVisible('checkStock.showStatus')" class="search-item">
                <span class="search-label">显示状态</span>
                <a-select v-model:value="query.showStatus" size="small" style="width: 110px" :options="SHOW_STATUS_OPTIONS" @change="handleSearch" />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :data-source="pagedRows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="`analytics-check-stock-columns-${activeTab}`"
              :global-config-key="`analytics-check-stock-columns-${activeTab}`"
            >
              <template #imageCell="{ record }">
                <span v-if="record.image" class="cell-image">{{ record.image }}</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #productNameCell="{ record }">
                <span :class="{ 'zero-stock': !Number(record.quantity) }">{{ record.productName || '-' }}</span>
              </template>
              <!-- 库存分布：动态仓库列（列 key = wh_<仓库ID>，值取自行内 __wh 透视表） -->
              <template #whQtyCell="{ record, column }">
                {{ formatNumber(record.__wh?.[column.key]) }}
              </template>
              <template #totalQtyCell="{ record }">
                {{ formatNumber(record.total) }}
              </template>
              <template #actionCell="{ record }">
                <a-space :size="2">
                  <a-button type="link" size="small" @click="goStockDetail(record)">明细</a-button>
                  <a-button type="link" size="small" @click="goReconcile(record)">对账</a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="totalCount"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标实测有「当前库存 / 按属性-页面配置弹窗」截图 → 接 PageConfigPanel，逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`analytics-check-stock-page-config-${activeTab}`"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockApi } from '@/api/analytics'
import { productCategoryApi } from '@/api/erp/product'
import { useExport } from '@/composables/useExport'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsCheckStock' })

const router = useRouter()

// ═══ 视图 Tab（对标实测顺序：当前库存 / 按属性 / 库存分布） ═══
const TABS = [
  { key: 'current', label: '当前库存' },
  { key: 'attribute', label: '按属性' },
  { key: 'distribution', label: '库存分布' }
]
const activeTab = ref('current')
/** 对标实测：仅「当前库存」视图有左侧商品分类树 */
const hasCategoryTree = computed(() => activeTab.value === 'current')

const query = reactive({
  warehouseIds: [] as number[],
  keyword: '',
  brand: '',
  /** 显示列：库存分布视图动态仓库列取用的数量口径（对标实测默认「账面库存」） */
  showColumn: 'book',
  showStatus: 'enabled',
  categoryId: undefined as number | undefined
})

const SHOW_COLUMN_OPTIONS = [
  { label: '账面库存', value: 'book' },
  { label: '可用库存', value: 'available' }
]
const SHOW_STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '已启用', value: 'enabled' },
  { label: '停用', value: 'disabled' }
]

const loading = ref(false)
const allRows = ref<any[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const pagination = reactive({ current: 1, pageSize: 20 })

/** 单次查询的上限（后端 /erp/stock/page 只支持单仓过滤，多仓需前端分仓拉取合并） */
const MAX_ROWS = 5000
const FETCH_PAGE_SIZE = 500
const MAX_WAREHOUSES = 10

// ═══ 左侧商品分类树 ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const currentPath = ref('全部商品')

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(res) ? res : res?.data || []
  } catch (e) {
    console.warn('[查库存] 商品分类树获取失败', e)
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

function findPath(nodes: any[], id: string, trail: string[] = []): string[] | null {
  for (const n of nodes || []) {
    const next = [...trail, n.categoryName]
    if (String(n.id) === id) return next
    const found = n.children?.length ? findPath(n.children, id, next) : null
    if (found) return found
  }
  return null
}

/** 分类过滤后端未支持，只在首次选中时提示一次（避免静默失效） */
const categoryTipShown = ref(false)

function onCategorySelect(keys: (string | number)[]) {
  const key = keys?.length ? String(keys[0]) : ''
  selectedCategoryId.value = key || '0'
  query.categoryId = key ? Number(key) : undefined
  if (key && !categoryTipShown.value) {
    categoryTipShown.value = true
    message.warning('后端 /erp/stock/page 暂不支持按商品分类过滤，分类条件已透传但本次结果不会收敛')
  }
  currentPath.value = key ? (findPath(categoryTree.value, key) || []).join(' / ') || '全部商品' : '全部商品'
  handleSearch()
}

// ═══ 列定义（逐 Tab 独立，列名逐字取自《查库存开发文档》§3） ═══

/** 8 档客户级别定价列（35-54 列，两视图共用同一清单与顺序） */
function pricingColumns(): DetailColumnConfig[] {
  return [
    { key: 'retailPrice', title: '零售价', width: 100, align: 'right', defaultHidden: true },
    { key: 'retailValue', title: '零售价值', width: 110, align: 'right', defaultHidden: true },
    { key: 'wholesalePrice', title: '批发价', width: 100, align: 'right', defaultHidden: true },
    { key: 'wholesaleValue', title: '批发价值', width: 110, align: 'right', defaultHidden: true },
    { key: 'cateringPrice', title: '餐饮店', width: 100, align: 'right', defaultHidden: true },
    { key: 'cateringValue', title: '餐饮店价值', width: 120, align: 'right', defaultHidden: true },
    { key: 'canteenPrice', title: '食堂团餐', width: 100, align: 'right', defaultHidden: true },
    { key: 'canteenValue', title: '食堂团餐价值', width: 120, align: 'right', defaultHidden: true },
    { key: 'outerCateringPrice', title: '外围餐饮店', width: 110, align: 'right', defaultHidden: true },
    { key: 'outerCateringValue', title: '外围餐饮店价值', width: 130, align: 'right', defaultHidden: true },
    { key: 'selfVipPrice', title: '自助vip', width: 100, align: 'right', defaultHidden: true },
    { key: 'selfVipValue', title: '自助vip价值', width: 120, align: 'right', defaultHidden: true },
    { key: 'bigCanteenPrice', title: '大团餐', width: 100, align: 'right', defaultHidden: true },
    { key: 'bigCanteenValue', title: '大团餐价值', width: 120, align: 'right', defaultHidden: true },
    { key: 'keyVipPrice', title: '重点|vip01', width: 110, align: 'right', defaultHidden: true },
    { key: 'keyVipValue', title: '重点|vip01价值', width: 130, align: 'right', defaultHidden: true },
    { key: 'chainVipPrice', title: '连锁|vip', width: 100, align: 'right', defaultHidden: true },
    { key: 'chainVipValue', title: '连锁|vip价值', width: 120, align: 'right', defaultHidden: true },
    { key: 'specialPrice', title: '特价客户', width: 100, align: 'right', defaultHidden: true },
    { key: 'specialValue', title: '特价客户价值', width: 120, align: 'right', defaultHidden: true }
  ]
}

/** Tab1「当前库存」：全部 54 列 / 默认 8（defaultHidden 46 项） */
const currentColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 70, align: 'center', defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '货号', width: 110, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true },
  { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true },
  { key: 'productRemark', title: '商品备注', width: 140, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'smallUnit', title: '小单位', width: 80, defaultHidden: true },
  { key: 'conversionRate', title: '换算关系', width: 100, defaultHidden: true },
  { key: 'unitPrice', title: '成本单价', width: 100, align: 'right' },
  { key: 'costAmount', title: '成本金额', width: 110, align: 'right' },
  {
    key: 'bookStockGroup',
    title: '账面库存',
    children: [
      { key: 'quantity', title: '数量', width: 100, align: 'right' },
      { key: 'convertedResult', title: '换算结果', width: 110, align: 'right', defaultHidden: true },
      { key: 'smallQty', title: '小单位数量', width: 110, align: 'right' },
      { key: 'mediumQty', title: '中单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'largeQty', title: '大单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'totalWeight', title: '总重量（kg）', width: 120, align: 'right', defaultHidden: true },
      { key: 'totalVolume', title: '总体积（m³）', width: 120, align: 'right', defaultHidden: true }
    ]
  },
  { key: 'shippedPendingQty', title: '已发待记账数量', width: 130, align: 'right' },
  { key: 'receivedPendingQty', title: '已收待记账数量', width: 130, align: 'right' },
  {
    key: 'availableStockGroup',
    title: '可用库存',
    children: [
      { key: 'availableQuantity', title: '数量', width: 100, align: 'right', defaultHidden: true },
      { key: 'availableConverted', title: '换算结果', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableSmallQty', title: '小单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableMediumQty', title: '中单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableLargeQty', title: '大单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableTotalWeight', title: '总重量（kg）', width: 120, align: 'right', defaultHidden: true },
      { key: 'availableTotalVolume', title: '总体积（m³）', width: 120, align: 'right', defaultHidden: true }
    ]
  },
  { key: 'unshippedQty', title: '未发数量', width: 100, align: 'right', defaultHidden: true },
  { key: 'unreceivedQty', title: '未收数量', width: 100, align: 'right', defaultHidden: true },
  ...pricingColumns()
]

/** Tab2「按属性」：全部 50 列 / 默认 6（defaultHidden 44 项；比当前库存少四个重量/体积汇总子列） */
const attributeColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 70, align: 'center', defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '货号', width: 110, defaultHidden: true },
  { key: 'smallUnit', title: '小单位', width: 80, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true },
  { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true },
  { key: 'conversionRate', title: '换算关系', width: 100, defaultHidden: true },
  { key: 'productRemark', title: '商品备注', width: 140, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'unitPrice', title: '成本单价', width: 100, align: 'right', defaultHidden: true },
  { key: 'costAmount', title: '成本金额', width: 110, align: 'right', defaultHidden: true },
  {
    key: 'bookStockGroup',
    title: '账面库存',
    children: [
      { key: 'quantity', title: '数量', width: 100, align: 'right' },
      { key: 'convertedResult', title: '换算结果', width: 110, align: 'right', defaultHidden: true },
      { key: 'smallQty', title: '小单位数量', width: 110, align: 'right' },
      { key: 'mediumQty', title: '中单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'largeQty', title: '大单位数量', width: 110, align: 'right', defaultHidden: true }
    ]
  },
  { key: 'shippedPendingQty', title: '已发待记账数量', width: 130, align: 'right' },
  { key: 'receivedPendingQty', title: '已收待记账数量', width: 130, align: 'right' },
  {
    key: 'availableStockGroup',
    title: '可用库存',
    children: [
      { key: 'availableQuantity', title: '数量', width: 100, align: 'right', defaultHidden: true },
      { key: 'availableConverted', title: '换算结果', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableSmallQty', title: '小单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableMediumQty', title: '中单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'availableLargeQty', title: '大单位数量', width: 110, align: 'right', defaultHidden: true }
    ]
  },
  { key: 'unshippedQty', title: '未发数量', width: 100, align: 'right', defaultHidden: true },
  { key: 'unreceivedQty', title: '未收数量', width: 100, align: 'right', defaultHidden: true },
  ...pricingColumns()
]

/**
 * Tab3「库存分布」：33 列 / 默认 2，另加动态仓库列 + 合计列（不占 33 列额度，随所选仓库生成）。
 * 仓库列取「显示列」选定口径（账面库存 / 可用库存），合计 = Σ(所选仓库该口径数量)。
 */
const distributionColumns = computed<DetailColumnConfig[]>(() => {
  const whCols: DetailColumnConfig[] = warehouseColumns.value.map(w => ({
    key: `wh_${w.id}`,
    title: w.name,
    type: 'slot',
    slotName: 'whQtyCell',
    width: 120,
    align: 'right'
  }))
  const groupTitle = query.showColumn === 'available' ? '可用库存' : '账面库存'
  const cols: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
    { key: 'unit', title: '单位', width: 70 },
    { key: 'smallUnit', title: '小单位', width: 80, defaultHidden: true },
    { key: 'conversionRate', title: '换算关系', width: 100, defaultHidden: true },
    { key: 'productCode', title: '货号', width: 110, defaultHidden: true },
    { key: 'specification', title: '规格', width: 120, defaultHidden: true },
    { key: 'model', title: '型号', width: 110, defaultHidden: true },
    { key: 'origin', title: '产地', width: 100, defaultHidden: true },
    { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
    { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true },
    { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true },
    { key: 'productRemark', title: '商品备注', width: 140, defaultHidden: true },
    { key: 'brand', title: '品牌', width: 100, defaultHidden: true }
  ]
  // 33 列中的 20 个定价列（14-33）
  cols.push(...pricingColumns().map(c => ({ ...c, defaultHidden: true })))
  // 动态仓库列（分组头）+ 合计列
  if (whCols.length) cols.push({ key: 'warehouseDistGroup', title: groupTitle, children: whCols })
  cols.push({ key: 'totalQty', title: '合计', type: 'slot', slotName: 'totalQtyCell', width: 110, align: 'right' })
  return cols
})

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'attribute') return attributeColumns
  if (activeTab.value === 'distribution') return distributionColumns.value
  return currentColumns
})

// ═══ 库存分布：按所选仓库生成动态列 ═══
const warehouseColumns = computed(() => {
  const all = warehouseOptions.value
  const ids = query.warehouseIds.length ? query.warehouseIds : all.map(w => w.value)
  return ids
    .map(id => all.find(w => String(w.value) === String(id)))
    .filter(Boolean)
    .map(w => ({ id: w!.value, name: w!.label }))
    .slice(0, MAX_WAREHOUSES)
})

/** 库存分布透视行：行 = 商品，列 = 仓库（数量口径由「显示列」决定），合计 = 跨仓求和 */
const pivotRows = computed<any[]>(() => {
  const qtyField = query.showColumn === 'available' ? 'availableQuantity' : 'quantity'
  const map = new Map<string, any>()
  for (const r of allRows.value) {
    const key = String(r.productId ?? r.productCode ?? r.id)
    let item = map.get(key)
    if (!item) {
      item = { ...r, rowKey: key, __wh: {} as Record<string, number>, total: 0 }
      map.set(key, item)
    }
    const colKey = `wh_${r.warehouseId}`
    const val = Number(r[qtyField]) || 0
    item.__wh[colKey] = (item.__wh[colKey] || 0) + val
    item.total += val
  }
  return [...map.values()]
})

// ═══ 取数（/erp/stock/page；多仓时按仓分头拉取合并） ═══
function normalizeRow(r: any) {
  const unitPrice = Number(r.unitPrice)
  return {
    ...r,
    rowKey: String(r.id ?? `${r.productId}-${r.warehouseId}`),
    costAmount: isNaN(unitPrice) ? '' : (Number(r.quantity) || 0) * unitPrice
  }
}

async function fetchData() {
  if (!query.warehouseIds.length) {
    message.warning('请至少选择一个仓库')
    return
  }
  loading.value = true
  try {
    // 全选仓库时直接查全部（省去分仓往返），否则逐仓拉取后合并
    const allIds = warehouseOptions.value.map(w => w.value)
    const targets: (number | undefined)[] = query.warehouseIds.length >= allIds.length ? [undefined] : query.warehouseIds
    const acc: any[] = []
    let capped = false
    for (const wid of targets) {
      let page = 1
      for (;;) {
        const res: any = await stockApi.page({
          pageNum: page,
          pageSize: FETCH_PAGE_SIZE,
          warehouseId: wid,
          keyword: query.keyword || undefined,
          // 以下三项后端 /erp/stock/page 暂无对应参数，先按原样透传（后端补齐即生效）
          categoryId: query.categoryId,
          brand: query.brand || undefined,
          productStatus: query.showStatus === 'all' ? undefined : query.showStatus
        })
        const recs: any[] = res?.records || []
        acc.push(...recs)
        if (recs.length < FETCH_PAGE_SIZE) break
        if (acc.length >= MAX_ROWS) {
          capped = true
          break
        }
        page += 1
      }
      if (capped) break
    }
    allRows.value = acc.map(normalizeRow)
    pagination.current = 1
    if (capped) message.warning(`数据量过大，仅加载前 ${MAX_ROWS} 条，请缩小仓库或商品范围`)
  } catch (e) {
    console.warn('[查库存] 获取数据失败', e)
    message.error('获取数据失败')
    allRows.value = []
  } finally {
    loading.value = false
  }
}

// ═══ 分页（数据已在前端合并，故前端分页） ═══
const sourceRows = computed<any[]>(() => (activeTab.value === 'distribution' ? pivotRows.value : allRows.value))
const totalCount = computed(() => sourceRows.value.length)
const pagedRows = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return sourceRows.value.slice(start, start + pagination.pageSize)
})

watch(sourceRows, () => { pagination.current = 1 })
watch(activeTab, () => { pagination.current = 1 })

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { ...query }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.assign(query, v)
  if (typeof quickDate !== 'undefined') quickDate.value = ''
  handleSearch()
}

function handleSearch() {
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    warehouseIds: warehouseOptions.value.slice(0, MAX_WAREHOUSES).map(w => w.value),
    keyword: '',
    brand: '',
    showColumn: 'book',
    showStatus: 'enabled'
  })
  selectedCategoryId.value = '0'
  currentPath.value = '全部商品'
  query.categoryId = undefined
  fetchData()
}

function onTabChange(key: string) {
  activeTab.value = key
}

// ═══ 行级联查（明细 / 对账 → 库存明细按该商品过滤） ═══
function goStockDetail(record: any) {
  router.push({ path: '/analytics/stock-detail', query: { keyword: record.productCode || record.productName || '' } })
}

function goReconcile(record: any) {
  // 本系统无独立「库存对账」页，对账＝按商品核对流水，落到《库存明细》并带出红冲
  router.push({ path: '/analytics/stock-detail', query: { keyword: record.productCode || record.productName || '', includeReversed: '1' } })
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'checkStock.scheme', label: '查询方案', visible: true },
  { key: 'checkStock.warehouseIds', label: '仓库', visible: true },
  { key: 'checkStock.keyword', label: '商品', visible: true },
  { key: 'checkStock.brand', label: '品牌', visible: true },
  { key: 'checkStock.showColumn', label: '显示列', visible: true },
  { key: 'checkStock.showStatus', label: '显示状态', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange
} = useAnalyticsPageConfig({
  // storageKey 由模板按 Tab 动态传入；此处仅作为默认基准
  storageKey: 'analytics-check-stock-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 打印(F8) / 导出 共用列口径 ═══
const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
)
/** 打印 / 导出列：剔除系统列，仅取默认可见列（等价对标「所见即所打」） */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo' && c.key !== 'action' && !c.defaultHidden)
)

function formatNumber(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function cellText(col: DetailColumnConfig, r: any): string {
  const v = r[col.key]
  if (col.key === 'totalQty') return formatNumber(r.total)
  if (col.key.startsWith('wh_')) return formatNumber(r.__wh?.[col.key])
  if (/价|金额|价值/.test(col.title)) return formatMoney(v)
  if (/数量|重量|体积|换算/.test(col.title)) return formatNumber(v)
  return v === null || v === undefined || v === '' ? '' : String(v)
}

function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = pagedRows.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1400,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '查库存'
  const html = `<html><head><meta charset="utf-8"><title>查库存-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>查库存 · ${tabLabel}</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

function handleExport() {
  const cols = printableColumns.value
  const mapRows = (list: any[]) => list.map(r => cols.map(c => cellText(c, r)))
  executeExport({
    fileName: `查库存-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: totalCount.value,
    // 数据已在前端合并，直接导出当前结果集
    fetchAll: async () => sourceRows.value,
    mapToRows: mapRows,
    fallbackRows: () => mapRows(sourceRows.value)
  })
}

function handleError(err: any) {
  console.error('[查库存] 页面异常', err)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch (e) {
    console.warn('[查库存] 仓库列表获取失败', e)
  }
  query.warehouseIds = warehouseOptions.value.slice(0, MAX_WAREHOUSES).map(w => w.value)
  fetchCategoryTree()
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不受宿主 scoped 样式影响，查询区样式随页面自带 */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.required-mark { color: #ff4d4f; margin-left: 2px; }
.search-tip { color: #fa8c16; font-size: 12px; white-space: nowrap; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.zero-stock { color: #ff4d4f; }
.cell-image { color: #1677ff; }
.cell-empty { color: #bfbfbf; }
</style>
