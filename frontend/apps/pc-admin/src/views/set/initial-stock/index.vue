<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        库存期初（设置 → 数据录入 → 库存期初，菜单 70550 / set:initial-stock，单入口）
        对标 ql361「设置 → 期初录入 → 库存期初」实测形态：
          · 左「商品分类」树（底部当前路径「全部商品」）+ 右侧数据表
          · 工具栏：录入期初 ｜ 页面配置 / 刷新 / 导出期初 / 打印(F8)
          · 查询条件：仓库 · 商品（名称/货号/条码）· 期初数量（全部/有期初/无期初）
          · 空态文案「还没有内容哦 请输入查询条件点击查询吧！」
        数据口径：erp_stock 中 is_initial = 1 的行（期初台账，本页是其唯一维护入口），
                  关联 erp_product 取 条码/规格/型号/产地/小单位（erp_stock 表没有这些列，
                  故不新增冗余列 —— 见《库存期初开发文档》§7.4 路线 A）；
                  期初金额 = 期初数量 × 期初成本单价（**后端计算，不落库**，避免改数量没改金额）。
        ⚠️ 期初录入不走库存事件服务 → 不产生库存流水（设计取舍，见开发文档 §1.1）。
        ⚠️ 接口契约（与后端 InitialStockController 严格一致）：
           GET /page · POST /save（body 数组）· PUT /update（id 在 body）· DELETE /{id} · GET /export
      -->
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
        :show-table-footer="true"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
        @category-expand="onExpand"
        @search="handleSearch"
      >
        <!-- ═══ 工具栏左侧：录入期初 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="btnEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 录入期初
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 导出期初 / 打印(F8) ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出期初
            </a-button>
            <a-button
              v-if="btnEnabled('print')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格，禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('warehouse')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.warehouseId"
                  placeholder="仓库"
                  size="small"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="warehouseOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('keyword')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="商品名称/货号/条码"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('initialQty')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.initialQtyFilter"
                  placeholder="期初数量"
                  size="small"
                  :options="INITIAL_QTY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleReset"
                >
                  重置
                </a-button>
              </div>
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
              :min-rows="tableData.length ? 20 : 0"
              empty-text="还没有内容哦 请输入查询条件点击查询吧！"
              storage-key="set-initial-stock-table-columns"
              global-config-key="set-initial-stock-table-columns"
            >
              <!-- 商品名称：点击打开编辑弹窗 -->
              <template #productNameCell="{ record }">
                <a
                  v-if="!record.__ghost && record.productName"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.productName }}</a>
                <span v-else>-</span>
              </template>

              <!-- 期初金额（后端计算值，前端仅做 2 位小数展示） -->
              <template #amountCell="{ record }">
                <span>{{ formatAmount(record.amount) }}</span>
              </template>

              <!-- 录入时间（非对标列，默认隐藏） -->
              <template #createTimeCell="{ record }">
                {{ record.createTime ? formatDateTime(record.createTime) : '-' }}
              </template>

              <!-- 操作列：修改 / 删除（行内） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
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

      <!-- ═══ 页面配置（查询条件 + 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonsConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :hide-print-config="true"
        storage-key="set-initial-stock-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 录入 / 编辑期初弹窗（对标为页内弹窗，非表单页） ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑期初' : '录入期初'"
        :width="620"
        :confirm-loading="saving"
        ok-text="确定"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top: 16px"
        >
          <a-form-item
            label="商品"
            name="productId"
          >
            <a-select
              v-model:value="form.productId"
              placeholder="输入商品名称/货号/条码搜索"
              show-search
              allow-clear
              :filter-option="false"
              :loading="productLoading"
              :options="productOptions"
              @search="handleProductSearch"
              @change="handleProductChange"
            />
          </a-form-item>
          <a-form-item label="货号">
            <a-input
              :value="form.productCode"
              disabled
            />
          </a-form-item>
          <a-form-item label="规格">
            <a-input
              :value="form.spec"
              disabled
            />
          </a-form-item>
          <a-form-item
            label="仓库"
            name="warehouseId"
          >
            <a-select
              v-model:value="form.warehouseId"
              placeholder="请选择仓库"
              show-search
              option-filter-prop="label"
              :options="warehouseOptions"
              @change="handleWarehouseChange"
            />
          </a-form-item>
          <a-form-item
            label="期初数量"
            name="quantity"
          >
            <a-input-number
              v-model:value="form.quantity"
              :min="0"
              :precision="2"
              placeholder="保留 2 位小数"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            label="期初成本单价"
            name="unitPrice"
          >
            <a-input-number
              v-model:value="form.unitPrice"
              :min="0"
              :precision="4"
              placeholder="保留 4 位小数"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="期初金额">
            <span class="amount-text">{{ formatAmount(computedAmount) }}</span>
          </a-form-item>
          <a-form-item label="生产日期">
            <a-date-picker
              v-model:value="form.productionDate"
              value-format="YYYY-MM-DD HH:mm:ss"
              show-time
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="有效期至">
            <a-date-picker
              v-model:value="form.validityDate"
              value-format="YYYY-MM-DD HH:mm:ss"
              show-time
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              :maxlength="200"
              placeholder="请输入"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import * as XLSX from 'xlsx'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  PrinterOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { initialStockApi, type InitialStockInfo, type InitialStockQuery } from '@/api/set/initial'
import { productApi, productCategoryApi } from '@/api/erp/product'
import { warehouseApi } from '@/api/wms/warehouse'

defineOptions({ name: 'SetInitialStock' })

// ═══ 常量 ═══
/** 对标 ql361 分类树根节点「全部商品」：选中即不做分类过滤 */
const ROOT_CATEGORY_ID = '0'
/** 期初数量筛选项（对标 ql361「期初数量」下拉） */
const INITIAL_QTY_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: 'HAS', label: '有期初' },
  { value: 'NONE', label: '无期初' },
]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<InitialStockInfo[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询条件 ═══
const searchForm = reactive<Record<string, any>>({
  warehouseId: undefined,
  keyword: '',
  initialQtyFilter: 'ALL',
})

// ═══ 分类树（商品分类，来源 erp_product_category） ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>(ROOT_CATEGORY_ID)
const expandedKeys = ref<string[]>([])
const categoryTreeData = computed(() => categoryTree.value)

/** 底部当前路径（对标文案「当前路径: 全部商品」） */
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

/** 只保留树渲染所需字段（分类树节点不显示商品数量） */
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
    categoryTree.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: realTree }]
    expandedKeys.value = [ROOT_CATEGORY_ID]
  } catch (error) {
    console.error('[库存期初] 加载商品分类失败', error)
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

// ═══ 下拉数据源：仓库 / 商品 ═══
const warehouseOptions = ref<{ label: string; value: string }[]>([])
const productOptions = ref<{ label: string; value: string; raw: any }[]>([])
const productLoading = ref(false)
let productSearchTimer: ReturnType<typeof setTimeout> | null = null

async function loadWarehouses() {
  try {
    const res: any = await warehouseApi.listAll()
    const list = Array.isArray(res) ? res : (res?.data ?? [])
    warehouseOptions.value = list.map((w: any) => ({ label: w.warehouseName, value: String(w.id) }))
  } catch (error) {
    console.warn('[库存期初] 仓库下拉加载失败', error)
    warehouseOptions.value = []
  }
}

/**
 * 加载商品下拉（真实走商品分页接口 GET /erp/product/page）
 * ⚠️ 历史 P0：原实现的 productOptions 从未被赋值（只加载了仓库），
 *    「商品」这个必填项无法选择 → 弹窗永远提交不了。现于弹窗打开时预加载一页，并支持远程搜索。
 */
async function loadProducts(keyword = '') {
  productLoading.value = true
  try {
    const res: any = await productApi.page({ pageNum: 1, pageSize: 50, keyword: keyword || undefined })
    const rows: any[] = res?.records || []
    productOptions.value = rows.map((p: any) => ({
      label: p.productCode ? `${p.productName} (${p.productCode})` : p.productName,
      value: String(p.id),
      raw: p,
    }))
  } catch (error) {
    console.warn('[库存期初] 商品下拉加载失败', error)
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

/** 商品搜索防抖（避免每次按键都打一次接口） */
function handleProductSearch(keyword: string) {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productSearchTimer = setTimeout(() => loadProducts(keyword), 300)
}

/** 选中商品后回填货号/商品名称/规格/小单位（均为快照，保存时一并落库或由商品档案关联展示） */
function handleProductChange(value: any) {
  const option = productOptions.value.find(o => String(o.value) === String(value))
  const product = option?.raw
  if (!product) {
    form.productCode = ''
    form.productName = ''
    form.spec = ''
    form.unit = ''
    return
  }
  form.productCode = product.productCode || ''
  form.productName = product.productName || ''
  form.spec = product.spec || ''
  // 单位快照：商品分页接口未返回 erp_product.small_unit（Product 实体无该字段），故取基础单位；
  // 列表侧「小单位」列口径为 COALESCE(p.small_unit, s.unit, p.unit)，商品档案维护了小单位时以小单位为准
  form.unit = product.unit || ''
}

function handleWarehouseChange(value: any) {
  const option = warehouseOptions.value.find(o => String(o.value) === String(value))
  form.warehouseName = option?.label || ''
}

// ═══ 列表查询 ═══
function buildQueryParams(): InitialStockQuery {
  return {
    warehouseId: searchForm.warehouseId || undefined,
    categoryId: selectedCategoryId.value !== ROOT_CATEGORY_ID ? selectedCategoryId.value : undefined,
    keyword: searchForm.keyword || undefined,
    initialQtyFilter: searchForm.initialQtyFilter || 'ALL',
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await initialStockApi.page({
      ...buildQueryParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[库存期初] 加载列表失败', error)
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
  fetchCategoryTree()
  fetchList()
}

function handleReset() {
  searchForm.warehouseId = undefined
  searchForm.keyword = ''
  searchForm.initialQtyFilter = 'ALL'
  selectedCategoryId.value = ROOT_CATEGORY_ID
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 列定义 ═══
// rowNo（承载表头齿轮列配置）+ 操作列（固定列）+ 对标 10 个数据列 = 11 个业务列
// 对标 ql361 列：商品名称 / 货号 / 条码 / 规格 / 型号 / 产地 / 小单位 / 期初数量 / 期初成本单价 / 期初金额
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '货号', type: 'input', width: 140 },
  { key: 'barcode', title: '条码', type: 'input', width: 150 },
  { key: 'spec', title: '规格', type: 'input', width: 120 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'unit', title: '小单位', type: 'input', width: 80 },
  { key: 'quantity', title: '期初数量', type: 'input', width: 110, align: 'right', formatter: (v: any) => formatQty(v) },
  { key: 'unitPrice', title: '期初成本单价', type: 'input', width: 130, align: 'right', formatter: (v: any) => formatPrice(v) },
  { key: 'amount', title: '期初金额', type: 'slot', slotName: 'amountCell', width: 130, align: 'right' },
  // 非对标列（默认隐藏，便于追溯录入时间）
  { key: 'createTime', title: '录入时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true },
]

function formatQty(value: any): string {
  return value === null || value === undefined || value === '' ? '-' : Number(value).toFixed(2)
}

function formatPrice(value: any): string {
  return value === null || value === undefined || value === '' ? '-' : Number(value).toFixed(4)
}

function formatAmount(value: any): string {
  return value === null || value === undefined || value === '' ? '-' : Number(value).toFixed(2)
}

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : dayjs(d).format('YYYY-MM-DD HH:mm:ss')
}

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PAGE_CONFIG_KEY = 'set-initial-stock-page-config'

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'warehouse', label: '仓库', visible: true },
  { key: 'keyword', label: '商品（名称/货号/条码）', visible: true },
  { key: 'initialQty', label: '期初数量', visible: true },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '录入期初', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出期初', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面把「当前配置」传给 queryFieldsConfig，必须另给出厂值否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))
const showPageConfig = ref(false)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldsConfig.value = QUERY_FIELDS.map(def => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === def.key)
        return saved ? { ...def, visible: saved.visible !== false } : { ...def }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(def => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === def.key)
        return saved ? { ...def, enabled: saved.enabled !== false } : { ...def }
      })
    }
  } catch { /* 本地配置损坏时回退出厂配置 */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 新增 / 编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
/** 编辑中的期初行ID（雪花ID按字符串处理，禁止 Number(id)） */
const editingId = ref<string | null>(null)

const emptyForm = () => ({
  productId: undefined as string | undefined,
  productCode: '',
  productName: '',
  spec: '',
  unit: '',
  warehouseId: undefined as string | undefined,
  warehouseName: '',
  quantity: undefined as number | undefined,
  unitPrice: undefined as number | undefined,
  productionDate: undefined as string | undefined,
  validityDate: undefined as string | undefined,
  remark: '',
})
const form = reactive(emptyForm())

/** 弹窗内实时金额（数量 × 单价，与后端计算口径一致） */
const computedAmount = computed(() => {
  const q = Number(form.quantity)
  const p = Number(form.unitPrice)
  if (!Number.isFinite(q) || !Number.isFinite(p)) return null
  return q * p
})

const rules: Record<string, any> = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入期初数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入期初成本单价', trigger: 'blur' }],
}

function resetForm(data?: Partial<ReturnType<typeof emptyForm>>) {
  Object.assign(form, emptyForm(), data || {})
}

async function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
  // 保证「商品」下拉有真实数据（P0 修复点）
  await loadProducts()
}

async function openEdit(record: InitialStockInfo) {
  editingId.value = record.id
  resetForm({
    productId: record.productId ? String(record.productId) : undefined,
    productCode: record.productCode || '',
    productName: record.productName || '',
    spec: record.spec || '',
    unit: record.unit || '',
    warehouseId: record.warehouseId ? String(record.warehouseId) : undefined,
    warehouseName: record.warehouseName || '',
    quantity: record.quantity === null || record.quantity === undefined ? undefined : Number(record.quantity),
    unitPrice: record.unitPrice === null || record.unitPrice === undefined ? undefined : Number(record.unitPrice),
    productionDate: record.productionDate || undefined,
    validityDate: record.validityDate || undefined,
    remark: record.remark || '',
  })
  modalOpen.value = true
  // 商品下拉需包含当前行的商品（否则下拉里看不到已选值）
  await loadProducts(record.productCode || record.productName || '')
  ensureProductOption(record)
}

/**
 * 编辑回填时保证「已选商品」一定在下拉选项里
 * （搜索关键字可能命中不到该商品，缺选项会导致下拉显示为裸 id）
 */
function ensureProductOption(record: InitialStockInfo) {
  if (!record.productId) return
  const id = String(record.productId)
  if (productOptions.value.some(o => String(o.value) === id)) return
  productOptions.value = [
    {
      label: record.productCode ? `${record.productName || ''} (${record.productCode})` : (record.productName || id),
      value: id,
      raw: {
        id,
        productName: record.productName,
        productCode: record.productCode,
        spec: record.spec,
        unit: record.unit,
      },
    },
    ...productOptions.value,
  ]
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // 日期/备注显式传 null/空串：后端编辑链路用 UpdateWrapper 显式 set，
    // 「清空生产日期/有效期/备注」才能落库（updateById 会忽略 null）
    const payload = {
      productId: form.productId,
      productName: form.productName,
      productCode: form.productCode,
      warehouseId: form.warehouseId,
      warehouseName: form.warehouseName,
      unit: form.unit || null,
      quantity: form.quantity,
      unitPrice: form.unitPrice,
      productionDate: form.productionDate || null,
      validityDate: form.validityDate || null,
      remark: form.remark || '',
    }
    if (editingId.value) {
      await initialStockApi.update({ ...payload, id: editingId.value })
      message.success('更新成功')
    } else {
      // 后端 /save 收数组（支持批量口径），单条也包成数组
      await initialStockApi.save([payload])
      message.success('录入成功')
    }
    modalOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除（二次确认） ═══
function handleDelete(record: InitialStockInfo) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品「${record.productName || record.productCode || ''}」在仓库「${record.warehouseName || ''}」的期初库存吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await initialStockApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 导出期初（真实 xlsx，与列表同口径） ═══
async function handleExport() {
  exporting.value = true
  try {
    const res: any = await initialStockApi.export(buildQueryParams())
    const rows: InitialStockInfo[] = Array.isArray(res) ? res : (res?.data ?? [])
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const sheetRows = rows.map(row => ({
      商品名称: row.productName || '',
      货号: row.productCode || '',
      条码: row.barcode || '',
      规格: row.spec || '',
      型号: row.model || '',
      产地: row.origin || '',
      小单位: row.unit || '',
      期初数量: formatQty(row.quantity),
      期初成本单价: formatPrice(row.unitPrice),
      期初金额: formatAmount(row.amount),
      仓库: row.warehouseName || '',
    }))
    const ws = XLSX.utils.json_to_sheet(sheetRows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '库存期初')
    XLSX.writeFile(wb, `库存期初_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${rows.length} 条`)
  } catch (error: any) {
    console.error('[库存期初] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8)：与列表同口径的打印模板 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.productName)}</td>
      <td>${escapeHtml(r.productCode)}</td>
      <td>${escapeHtml(r.barcode)}</td>
      <td>${escapeHtml(r.spec)}</td>
      <td>${escapeHtml(r.model)}</td>
      <td>${escapeHtml(r.origin)}</td>
      <td>${escapeHtml(r.unit)}</td>
      <td style="text-align:right">${escapeHtml(formatQty(r.quantity))}</td>
      <td style="text-align:right">${escapeHtml(formatPrice(r.unitPrice))}</td>
      <td style="text-align:right">${escapeHtml(formatAmount(r.amount))}</td>
      <td>${escapeHtml(r.warehouseName)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>库存期初</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>库存期初</h2>
    <div class="meta">
      <span>当前路径：${escapeHtml(currentCategoryPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>商品名称</th><th>货号</th><th>条码</th><th>规格</th><th>型号</th>
        <th>产地</th><th>小单位</th><th>期初数量</th><th>期初成本单价</th><th>期初金额</th><th>仓库</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[库存期初] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  loadPageConfig()
  await nextTick()
  loadWarehouses()
  fetchCategoryTree()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
  if (productSearchTimer) clearTimeout(productSearchTimer)
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
/* 查询区横向自适应网格（禁止纵向单列） */
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.amount-text { font-weight: 600; line-height: 32px; }

/* 橙色新增按钮（设置模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
