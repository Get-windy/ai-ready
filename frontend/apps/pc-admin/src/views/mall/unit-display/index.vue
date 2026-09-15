<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        单位显示（交易 → 商城 → 基础业务 → 单位显示，对标 ql361「商城 → 基础业务 → 单位显示」）
        · 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/单位显示开发文档.md
          对标抓取：docs/Yh-Spec/抓取结果/单位显示_抓取.json
        · 页面定位：单入口单视图列表页 = 左侧「商品分类」树 + 右侧商城商品列表（带「单位显示」勾选列，无子 Tab）
          对标无「页面配置」弹窗（查询区/按钮为固定项）
        · 列配置：走 BillDetailTable 表头 rowNo 列齿轮（个人配置 / 全局配置），storage-key 持久化
        · 列数：可配置 12 列（对标 12 列）= 默认显示 10 列（单位显示/图片/商品名称/商品货号/零售价/批发价/条码/规格/型号/单位）
                                    + 默认隐藏 2 列（品牌、预设进价）
          固定列（非列配置）：序号、多选勾选框
        · 工具栏：批量显示 / 批量隐藏 ｜ 刷新 / 打印(F8) / 导出
        · 数据来源：erp_product（API /erp/product：page / batch-unit-display）+ 商品分类（API /erp/product-category/tree）
          —— 与「商品上架」同源商品列表（本页聚焦商城显示开关 + 单位维度展示）
        · 查询条件下拉「文案 + 取值」已按对标 ql361 实测校准（2026-09-14）：
          「显示状态」= 全部(-1)/已启用(2)/已停用(1)（默认「已启用」），
          「单位显示」= 全部(-1)/是(1)/否(2)（默认「全部」）。
          「-1=全部」一律**不传参**；对标整数取值 → 后端参数字段的值映射见
          文件内 `STATUS_TO_API`（2→ENABLED / 1→DISABLED）与 `UNIT_DISPLAY_TO_API`（1→1 / 2→0）。
        · 语义与后端字段（本轮按对标实测改正 unit_display_type 值域后）：
          1) 「单位显示」列 √/× 勾选 + 工具栏「批量显示 / 批量隐藏」→ PUT /erp/product/batch-unit-display
             传 **unitDisplay**（1=显示 / 0=隐藏）：语义是「该商品（含其全部单位）是否在商城显示」，
             即对标查询区「单位显示」条件（实测 全部(-1)/是(1)/否(2)），只写 **商品级**
             erp_product.unit_display（整品开关）。**不写**单位粒度列 —— 两者是不同概念。
          2) 查询区「单位显示类型」→ unitDisplayType：**单位粒度**显示类型，对应 **单位级**
             erp_product_unit.unit_display_type。对标 ql361 查询区该下拉 DOM 实测 4 项（逐字）：
             全部(-1) / 只显示常用单位(0) / 只显示小单位(1) / 只显示中/大单位(2)。
             建列=Flyway V11.361.8，取值口径= V11.363.0（改正早期 SHOW/HIDE 猜测口径）。
          3) 「预设进价」列（默认隐藏）→ 后端取**基本单位** erp_product_unit.preset_purchase_price
             （无基本单位行取 sort_order 最小单位；为空回退商品级 erp_product.purchase_price），已真实渲染。
          4) 查询区「商品标签」→ productTag（erp_product.mall_tags 槽位编码，整槽位包含匹配）；
             「单位显示」→ unitDisplay（erp_product.unit_display 1/0）；
             「显示状态」→ status（erp_product.status 的 ENABLED/DISABLED 字符串，
             **对标是整数 2/1，我方保留字符串枚举，仅在前端映射**，不改后端字段语义）。
          5) 原「商品单位字典 CRUD」（productUnitDictApi）语义与对标不一致，已在「商品辅助资料 → 单位」页保留，不再挂在本页
        · 仍保留的缺口（详见开发文档「剩余缺口」）：
          a) 本页为商品行列表，无逐单位编辑 UI：「单位显示类型」（单位粒度）查询可按实测 4 项筛选，
             但页面没有按单位逐个设置类型的表单入口（对同一商品不同单位差异化设置须进商品表单）；
          b) 「单位显示」列空值/NULL 时按商品级 unit_display 兜底，单位间不一致时本列不体现差异。
      -->
      <CategoryListLayout
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentPath"
        :tabs="[]"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeys = keys)"
        @category-retry="loadCategoryTree"
      >
        <!-- ═══ 工具栏左侧：批量显示 / 批量隐藏（对标固定按钮） ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="batchLoading"
              :disabled="!selectedIds.length"
              @click="handleBatchDisplay(1)"
            >
              批量显示
            </a-button>
            <a-button
              size="small"
              :loading="batchLoading"
              :disabled="!selectedIds.length"
              @click="handleBatchDisplay(0)"
            >
              批量隐藏
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
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
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项：筛选条件 + 品牌 + 商品标签 + 单位显示 + 单位显示类型 + 显示状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="商品名称/货号/条码/规格/型号"
                size="small"
                style="width: 210px"
                allow-clear
                @press-enter="handleSearch"
              />
              <span class="search-label">品牌</span>
              <a-select
                v-model:value="searchForm.brand"
                placeholder="全部"
                size="small"
                style="width: 140px"
                allow-clear
                show-search
                :options="brandOptions"
                @change="handleSearch"
              />
              <span class="search-label">商品标签</span>
              <!-- 选项来源 GET /erp/product/mall-tags（erp_product.mall_tags 已打标槽位编码去重，如 TAG_1） -->
              <a-select
                v-model:value="searchForm.productTag"
                placeholder="全部"
                size="small"
                style="width: 130px"
                allow-clear
                :options="tagOptions"
                @change="handleSearch"
              />
              <span class="search-label">单位显示</span>
              <!-- 对标实测取值 全部(-1) / 是(1) / 否(2)，默认「全部」→ 不传参
                   （请求映射：是→erp_product.unit_display=1，否→0，见 fetchList） -->
              <a-select
                v-model:value="searchForm.unitDisplay"
                placeholder="全部"
                size="small"
                style="width: 110px"
                allow-clear
                :options="unitDisplayOptions"
                @change="handleSearch"
              />
              <span class="search-label">单位显示类型</span>
              <!-- 单位粒度显示类型 erp_product_unit.unit_display_type：-1/0/1/2（对标 DOM 实测，V11.363.0） -->
              <a-select
                v-model:value="searchForm.unitDisplayType"
                placeholder="全部"
                size="small"
                style="width: 120px"
                allow-clear
                :options="unitDisplayTypeOptions"
                @change="handleSearch"
              />
              <span class="search-label">显示状态</span>
              <!-- 对标实测取值 全部(-1) / 已启用(2) / 已停用(1)，默认「已启用」（对标实测默认态）；
                   清空 = 不过滤（等价「全部」）。请求映射为 erp_product.status 的 ENABLED/DISABLED -->
              <a-select
                v-model:value="searchForm.status"
                placeholder="全部"
                size="small"
                style="width: 120px"
                allow-clear
                :options="statusOptions"
                @change="handleSearch"
              />
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="mall-unit-display-table-columns"
              global-config-key="mall-unit-display-table-columns"
              @checkbox-change="handleRowCheck"
              @checkbox-all="handleRowCheckAll"
            >
              <!-- 单位显示：√/× 勾选开关（写**商品级** erp_product.unit_display 1/0 =
                   「该商品是否在商城显示」；单位粒度类型 unit_display_type 是另一个概念，
                   由查询区「单位显示类型」承载；见文件头「语义与后端字段」1） -->
              <template #unitDisplayCell="{ record }">
                <a-checkbox
                  v-if="!record.__ghost"
                  :checked="isUnitDisplayed(record)"
                  :loading="togglingId === record.id"
                  @change="(e: any) => handleToggleDisplay(record, e.target.checked)"
                />
              </template>

              <!-- 图片 -->
              <template #imageUrlCell="{ record }">
                <template v-if="!record.__ghost">
                  <a-image
                    v-if="record.imageUrl"
                    :src="record.imageUrl"
                    :width="40"
                    :height="40"
                    style="object-fit: cover; border-radius: 4px"
                  />
                  <span v-else>-</span>
                </template>
              </template>

              <!-- 商品名称 -->
              <template #productNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.productName || '-' }}</span>
              </template>

              <!-- 商品货号（对标「商品货号」；erp_product 取 productCodeAlias，回退 productCode） -->
              <template #productCodeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.productCodeAlias || record.productCode || '-' }}</span>
              </template>

              <!-- 零售价 / 批发价 -->
              <template #retailPriceCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.retailPrice) }}</span>
              </template>
              <template #wholesalePriceCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.wholesalePrice) }}</span>
              </template>

              <!-- 条码 -->
              <template #barcodeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.barcode || '-' }}</span>
              </template>

              <!-- 品牌（默认隐藏） -->
              <template #brandCell="{ record }">
                <span v-if="!record.__ghost">{{ record.brand || '-' }}</span>
              </template>

              <!-- 规格 / 型号 / 单位 -->
              <template #specCell="{ record }">
                <span v-if="!record.__ghost">{{ record.spec || '-' }}</span>
              </template>
              <template #modelCell="{ record }">
                <span v-if="!record.__ghost">{{ record.model || '-' }}</span>
              </template>
              <template #unitCell="{ record }">
                <span v-if="!record.__ghost">{{ record.unit || '-' }}</span>
              </template>

              <!-- 预设进价（默认隐藏；后端取基本单位 erp_product_unit.preset_purchase_price，
                   为空回退商品级 product.purchase_price，见 fillUnitDerivedFields） -->
              <template #presetPurchasePriceCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.presetPurchasePrice) }}</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { productApi, productCategoryApi, type Product } from '@/api/erp/product'

defineOptions({ name: 'MallUnitDisplay' })

// ═══ 选项（下拉「文案 + 取值」= 对标 ql361 实测口径；请求参数在 fetchList 内做值映射） ═══
/**
 * 显示状态 —— ✅ 已按对标实测校准：`全部`(-1) / `已启用`(2) / `已停用`(1)（对标默认「已启用」）。
 *
 * ⚠️ 值映射（**不做后端语义硬改**）：对标实测值是整数 2/1，而本页列表走
 *    `GET /erp/product/page`，其 `status` 参数是**字符串等值**匹配
 *    `erp_product.status`（V1.1.9 建表 `VARCHAR(20) DEFAULT 'ENABLED'`，值域 `ENABLED`/`DISABLED`，
 *    该列被其它商品档案页共用，不得改成整数）。故前端保留实测整数作为下拉值，
 *    请求前按 `STATUS_TO_API` 映射为字符串；`-1`（全部）不传参。
 */
const statusOptions = [
  { label: '全部', value: -1 },
  { label: '已启用', value: 2 },
  { label: '已停用', value: 1 }
]
/** 显示状态：对标实测整数 → erp_product.status 真实字符串枚举 */
const STATUS_TO_API: Record<number, 'ENABLED' | 'DISABLED'> = {
  2: 'ENABLED',
  1: 'DISABLED'
}
/** 对标实测「显示状态」默认值 = 已启用(2) */
const DEFAULT_STATUS = 2
/**
 * 单位显示 —— ✅ 已按对标实测校准：`全部`(-1) / `是`(1) / `否`(2)。
 *
 * 值映射：对标的布尔「是/否」↔ 商品级 `erp_product.unit_display`（V11.361.3：1=显示 0=隐藏）。
 * 故 `是`(1) → `unitDisplay=1`（同值直接对表），`否`(2) → `unitDisplay=0`（映射），
 * `-1`（全部）不传参。与「单位显示类型」（单位粒度 -1/0/1/2）是两个概念，勿混。
 */
const unitDisplayOptions = [
  { label: '全部', value: -1 },
  { label: '是', value: 1 },
  { label: '否', value: 2 }
]
/** 单位显示：对标实测值 → erp_product.unit_display 的 1/0 */
const UNIT_DISPLAY_TO_API: Record<number, 0 | 1> = {
  1: 1,
  2: 0
}
/**
 * 「单位显示类型」= **单位粒度**显示类型（erp_product_unit.unit_display_type）
 *
 * ✅ 取值来自对标 ql361 查询区该下拉的 DOM 实测（2026-09-14，4 项逐字）：
 *    全部(-1) / 只显示常用单位(0) / 只显示小单位(1) / 只显示中/大单位(2)。
 * 建列 Flyway V11.361.8，取值口径经 V11.363.0 改正（早期 SHOW/HIDE 为猜测，已废止）。
 * 后端 unitDisplayType 参数按该 4 值归一化（并兼容中文文案）。
 */
const unitDisplayTypeOptions = [
  { label: '全部', value: '-1' },
  { label: '只显示常用单位', value: '0' },
  { label: '只显示小单位', value: '1' },
  { label: '只显示中/大单位', value: '2' }
]

// ═══ 状态 ═══
const loading = ref(false)
const batchLoading = ref(false)
const togglingId = ref<number | null>(null)
const tableData = ref<Product[]>([])

// ═══ 查询条件（对标固定项：筛选条件 / 品牌 / 商品标签 / 单位显示 / 单位显示类型 / 显示状态；初值 = 对标实测默认态） ═══
const searchForm = reactive({
  keyword: '' as string,
  brand: undefined as string | undefined,
  /** 商品标签：erp_product.mall_tags 槽位编码（TAG_1..TAG_20） */
  productTag: undefined as string | undefined,
  /** 单位显示（对标实测 全部(-1)/是(1)/否(2)）→ 请求映射 erp_product.unit_display 1/0；实测默认「全部」 */
  unitDisplay: -1 as number | undefined,
  /** 单位显示类型（单位粒度，erp_product_unit.unit_display_type）：'-1'|'0'|'1'|'2'（对标实测） */
  unitDisplayType: undefined as string | undefined,
  /** 显示状态（对标实测 全部(-1)/已启用(2)/已停用(1)）→ 请求映射 ENABLED/DISABLED；实测默认「已启用」 */
  status: DEFAULT_STATUS as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 列定义（DetailColumnConfig[]）
 * 对标 12 列：默认显示 10 列（单位显示/图片/商品名称/商品货号/零售价/批发价/条码/规格/型号/单位）
 *            默认隐藏 2 列（品牌、预设进价）
 * 固定列（不进列配置面板）：rowNo（承载列配置齿轮）、checkbox（批量显示/隐藏）
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'unitDisplay', title: '单位显示', type: 'slot', slotName: 'unitDisplayCell', width: 90, align: 'center' },
  { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageUrlCell', width: 70, align: 'center' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200, sortable: true },
  { key: 'productCode', title: '商品货号', type: 'slot', slotName: 'productCodeCell', width: 140 },
  { key: 'retailPrice', title: '零售价', type: 'slot', slotName: 'retailPriceCell', width: 110, align: 'right' },
  { key: 'wholesalePrice', title: '批发价', type: 'slot', slotName: 'wholesalePriceCell', width: 110, align: 'right' },
  { key: 'barcode', title: '条码', type: 'slot', slotName: 'barcodeCell', width: 140 },
  { key: 'spec', title: '规格', type: 'slot', slotName: 'specCell', width: 120 },
  { key: 'model', title: '型号', type: 'slot', slotName: 'modelCell', width: 120 },
  { key: 'unit', title: '单位', type: 'slot', slotName: 'unitCell', width: 80 },
  { key: 'brand', title: '品牌', type: 'slot', slotName: 'brandCell', width: 120, defaultHidden: true },
  { key: 'presetPurchasePrice', title: '预设进价', type: 'slot', slotName: 'presetPurchasePriceCell', width: 110, align: 'right', defaultHidden: true },
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 「单位显示」勾选态判定 = **商品级整品开关** erp_product.unit_display（1 显示 / 0 隐藏）。
 *
 * 语义是「该商品（含其全部单位）是否在商城显示」，与「单位显示类型」（单位粒度 -1/0/1/2）
 * 是两个概念，故不再看 record.unitDisplayType。列默认 1=显示，仅显式 0 才算隐藏。
 */
function isUnitDisplayed(record: any): boolean {
  if (!record) return false
  return record.unitDisplay !== 0
}

// ═══ 分类树（商品分类；与「商品上架」同一棵 erp_product_category 树） ═══
const ROOT_CATEGORY_ID = '__all__'
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<any[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => [
  { id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: categoryList.value },
])

const currentPath = computed(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return '全部商品'
  const path: string[] = []
  const walk = (nodes: any[], target: string): boolean => {
    for (const n of nodes) {
      path.push(n.categoryName)
      if (String(n.id) === target) return true
      if (n.children?.length && walk(n.children, target)) return true
      path.pop()
    }
    return false
  }
  walk(categoryList.value, String(selectedCategoryId.value))
  return path.length ? path.join(' / ') : '全部商品'
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await productCategoryApi.getTree()
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[单位显示] 分类树加载失败', e)
    categoryError.value = true
    categoryList.value = []
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : ROOT_CATEGORY_ID
  handleSearch()
}

// ═══ 品牌选项（真实来源 /erp/product/brands） ═══
const brandOptions = ref<{ label: string; value: string }[]>([])

async function loadBrands() {
  try {
    const list = await productApi.getBrands()
    brandOptions.value = (list || []).map((b: string) => ({ label: b, value: b }))
  } catch (e) {
    console.warn('[单位显示] 品牌加载失败', e)
    brandOptions.value = []
  }
}

// ═══ 商品标签选项（真实来源 /erp/product/mall-tags，erp_product.mall_tags 已打标槽位编码去重） ═══
const tagOptions = ref<{ label: string; value: string }[]>([])

async function loadTagOptions() {
  try {
    const list: any = await productApi.getMallTags()
    const arr: string[] = Array.isArray(list) ? list : (list?.data ?? [])
    // 无已打标数据时保持空选项，不做假选项
    tagOptions.value = (arr || [])
      .filter((v: any) => v !== null && v !== undefined && String(v).trim() !== '')
      .map((v: any) => ({ label: String(v), value: String(v) }))
  } catch (e) {
    console.warn('[单位显示] 商品标签选项加载失败', e)
    tagOptions.value = []
  }
}

// ═══ 数据加载（与「商品上架」同源商品列表） ═══
async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      brand: searchForm.brand || undefined,
      // 显示状态：对标值 -1/2/1 → erp_product.status 字符串 ENABLED / DISABLED（-1=全部 不传参）
      status: STATUS_TO_API[Number(searchForm.status)],
      // 商品标签（erp_product.mall_tags 槽位编码，后端整槽位包含匹配）
      productTag: searchForm.productTag || undefined,
      // 单位显示：对标值 -1/1/2 → erp_product.unit_display 1/0（-1=全部 不传参；
      // 注意 `否`(2) 必须映射为 0，0 是 falsy，不能直接进 `if` 判断）
      unitDisplay: UNIT_DISPLAY_TO_API[Number(searchForm.unitDisplay)],
      // 单位显示类型（单位粒度 erp_product_unit.unit_display_type：'-1'|'0'|'1'|'2'，对标实测）
      unitDisplayType: searchForm.unitDisplayType || undefined,
    }
    if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
      params.categoryId = Number(selectedCategoryId.value)
    }
    const res: any = await productApi.page(params as any)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedRows.value = []
  } catch (error: any) {
    console.error('[单位显示] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  loadCategoryTree()
  loadBrands()
  loadTagOptions()
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 行选择（批量显示 / 隐藏） ═══
const selectedRows = ref<Product[]>([])
const selectedIds = computed(() =>
  selectedRows.value.filter((r: any) => !r.__ghost).map((r: any) => r.id).filter((id: any) => id !== undefined && id !== null)
)

function handleRowCheck(record: any, _index: number, checked: boolean) {
  if (!record || record.__ghost) return
  if (checked) {
    if (!selectedRows.value.some((r: any) => r.id === record.id)) {
      selectedRows.value = [...selectedRows.value, record]
    }
  } else {
    selectedRows.value = selectedRows.value.filter((r: any) => r.id !== record.id)
  }
}

function handleRowCheckAll(checked: boolean, records: any[]) {
  const rows = (records || []).filter((r: any) => r && !r.__ghost)
  selectedRows.value = checked ? rows : []
}

// ═══ 单位显示开关（单行）：写**商品级** erp_product.unit_display 1/0（「该商品是否在商城显示」；
//     单位粒度类型 unit_display_type 由查询区「单位显示类型」表达，不在此写） ═══
async function handleToggleDisplay(record: Product, checked: boolean) {
  togglingId.value = record.id
  try {
    await productApi.batchUnitDisplayFlag([record.id], checked ? 1 : 0)
    message.success(checked ? `「${record.productName}」已显示` : `「${record.productName}」已隐藏`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

// ═══ 批量显示 / 批量隐藏 ═══
async function handleBatchDisplay(target: number) {
  const ids = selectedIds.value
  if (!ids.length) {
    message.warning('请先勾选商品')
    return
  }
  batchLoading.value = true
  try {
    await productApi.batchUnitDisplayFlag(ids, target === 1 ? 1 : 0)
    message.success(`已${target === 1 ? '显示' : '隐藏'} ${ids.length} 个商品`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量操作失败')
  } finally {
    batchLoading.value = false
  }
}

// ═══ 打印(F8)：与列表同口径渲染后打印 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function currentRows(): Product[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

function handlePrint() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${isUnitDisplayed(r) ? '√' : '×'}</td>
      <td>${escapeHtml(r.productName)}</td>
      <td>${escapeHtml(r.productCodeAlias || r.productCode || '')}</td>
      <td>${escapeHtml(formatMoney(r.retailPrice))}</td>
      <td>${escapeHtml(formatMoney(r.wholesalePrice))}</td>
      <td>${escapeHtml(r.barcode || '')}</td>
      <td>${escapeHtml(r.spec || '')}</td>
      <td>${escapeHtml(r.model || '')}</td>
      <td>${escapeHtml(r.unit || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>单位显示</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>单位显示</h2>
    <div class="meta">
      <span>当前路径：${escapeHtml(currentPath.value)}</span>
      <span>筛选条件：${escapeHtml(searchForm.keyword || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>单位显示</th><th>商品名称</th><th>商品货号</th><th>零售价</th><th>批发价</th><th>条码</th><th>规格</th><th>型号</th><th>单位</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

// ═══ 导出（前端 CSV，\uFEFF BOM 保证 Excel 中文不乱码） ═══
function handleExport() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const header = ['单位显示', '商品名称', '商品货号', '零售价', '批发价', '条码', '品牌', '规格', '型号', '单位', '预设进价']
  const lines = rows.map((r: any) => [
    isUnitDisplayed(r) ? '√' : '×',
    r.productName ?? '',
    r.productCodeAlias || r.productCode || '',
    r.retailPrice ?? '',
    r.wholesalePrice ?? '',
    r.barcode ?? '',
    r.brand ?? '',
    r.spec ?? '',
    r.model ?? '',
    r.unit ?? '',
    r.presetPurchasePrice ?? '',
  ])
  const csv = [header, ...lines]
    .map(cols => cols.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `单位显示_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[单位显示] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadCategoryTree()
  loadBrands()
  loadTagOptions()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
