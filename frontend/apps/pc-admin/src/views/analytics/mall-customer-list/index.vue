<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商城客户列表（分析 → 营销分析 → 商城客户列表，菜单 80475）
        对标 ql361「分析 → 商城分析 → 商城客户列表」：**单视图**（左侧「买家归属分类」分类树 +
        查询区 + 客户数据表），对标工具栏有 `打印(F8)` 与 `导出`，故两件都接；
        对标**有**「页面配置」弹窗（实测 `pageConfig.found=true` + 截图 `商城客户列表-页面配置弹窗.png`），
        故接 PageConfigPanel（查询条件显隐 / 功能按钮启停 / 打印配置）。

        ⚠️ 列缺口（已核实后端源码，已在开发文档「剩余缺口」与验收汇报中登记）：
          · 对标 13 列中 `访问次数 / 成交数量 / 成交率(%)` **无真实数据源** ——
            全库无商城访问/浏览埋点表，`shop_user` 亦无访问与成交计数字段；
            `/erp/mall/admin/user/page` 只返回账号与主数据字段。
            故这 3 列**不渲染**（禁返回 0 / 占位假值），最终 10 列。
          · 反过来说，对标文档记「所属仓库 / 默认经手人 / 客户级别 / 联系人 **当前无来源**」已**过时**：
            shop_user 已由 V11.361.3 补齐 customer_level / warehouse_name / default_handler_name /
            contact_name / address 等列，且 /user/page 直接返回 → 本页这 4 列**已接真实来源**。
          · 对标首列勾选框 + 批量 `发短信 / 发优惠券`**未接**：后端无「对客户发触达」端点
            （smsApi 只有配置/测试，无发送），故不放勾选框与按钮，避免死入口（详见汇报「未动的问题」）。
      -->
      <CategoryListLayout
        category-title="买家归属分类"
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
        <!-- ═══ 工具栏左侧：查询方案 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-mall-customer-list\index.vue-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置（对标实测四类按钮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-button
              size="small"
              @click="showPageConfig = true"
            >
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格；仅摆后端实际支持的过滤项，未支持的项不摆以免静默失效） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isQueryVisible('mallCustomer.createTime')"
                class="search-item"
              >
                <span class="search-label">注册日期</span>
                <a-range-picker
                  v-model:value="query.dateRange"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('mallCustomer.keyword')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="客户名称/登录名"
                  allow-clear
                  style="width: 190px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('mallCustomer.gradeId')"
                class="search-item"
              >
                <span class="search-label">客户级别</span>
                <a-select
                  v-model:value="query.gradeId"
                  size="small"
                  placeholder="全部"
                  allow-clear
                  style="width: 150px"
                  :options="gradeOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox
                  v-if="isQueryVisible('mallCustomer.showDisabled')"
                  v-model:checked="query.showDisabled"
                  @change="handleSearch"
                >
                  显示停用
                </a-checkbox>
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

        <!-- ═══ 数据表（表头序号列齿轮承载列配置） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              storage-key="analytics-mall-customer-list-columns"
              global-config-key="analytics-mall-customer-list-columns"
            >
              <template #customerNameCell="{ record }">
                {{ record.companyName || record.nickname || record.username || '-' }}
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <!-- ═══ 页面配置（对标有「页面配置」弹窗 → 接 PageConfigPanel；storage-key 与列配置不同值） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
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
import { shopUserApi } from '@/api/analytics'
import { partnerCategoryApi, partnerGradeApi } from '@/api/erp/partner'
import { useExport } from '@/composables/useExport'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsMallCustomerList' })


// ═══ 查询态（仅后端 /erp/mall/admin/user/page 实际支持的过滤项） ═══
// 「显示停用」对标口径为**默认不勾选**（停用客户默认不入列，勾选后才纳入）
const query = reactive({
  keyword: '',
  dateRange: undefined as [string, string] | undefined,
  gradeId: undefined as number | undefined,
  showDisabled: false
})

// ═══ 取数 ═══
const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

function buildParams(page = pagination.current, size = pagination.pageSize) {
  return {
    pageNum: page,
    pageSize: size,
    keyword: query.keyword || undefined,
    createTimeStart: query.dateRange?.[0] || undefined,
    createTimeEnd: query.dateRange?.[1] || undefined,
    gradeId: query.gradeId,
    // 显示停用：勾选 → 不传（后端不过滤，含停用账号）；默认不勾选 → showDisabled=false（后端强制只看启用）
    showDisabled: query.showDisabled ? undefined : false,
    categoryId: String(selectedCategoryId.value) === ROOT_CATEGORY_ID ? undefined : Number(selectedCategoryId.value)
  }
}

function normalize(records: any[]): any[] {
  // 行键按字符串透传（shop_user.id 为雪花 BIGINT，禁止 Number() 或拿它做 Map 键）
  return (records || []).map(r => ({ ...r, rowKey: `U:${r.username || r.id}` }))
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await shopUserApi.page(buildParams())
    dataSource.value = normalize(res?.records)
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    console.warn('[商城客户列表] 取数失败', e)
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

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
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(query, { keyword: '', dateRange: undefined, gradeId: undefined, showDisabled: false })
  handleSearch()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleRefresh() {
  loadCategoryTree()
  fetchData()
}

// ═══ 列定义（对标 13 列 / 默认 13；已按后端硬缺口去掉无数据源的 访问次数 / 成交数量 / 成交率(%) → 10 列）
//     对标无 `操作` 列（行级动作仅「客户名称为链接」），故不设 action 列 ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'companyName', title: '客户名称', type: 'slot', slotName: 'customerNameCell', width: 190 },
  { key: 'username', title: '买家账号', width: 140 },
  { key: 'customerLevel', title: '客户级别', width: 140, formatter: (v: any) => v || '-' },
  { key: 'warehouseName', title: '所属仓库', width: 130, formatter: (v: any) => v || '-' },
  { key: 'defaultHandlerName', title: '默认经手人', width: 120, formatter: (v: any) => v || '-' },
  { key: 'contactName', title: '联系人', width: 120, formatter: (v: any) => v || '-' },
  { key: 'phone', title: '联系电话', width: 130, formatter: (v: any) => v || '-' },
  { key: 'address', title: '联系地址', width: 220, formatter: (v: any) => v || '-' },
  { key: 'lastLoginTime', title: '最后登录时间', width: 160, formatter: (v: any) => fmtTime(v) },
  { key: 'createTime', title: '注册时间', width: 160, formatter: (v: any) => fmtTime(v) }
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// ═══ 「买家归属分类」树（来源 biz_party_category.party_type=CUSTOMER） ═══
const ROOT_CATEGORY_ID = '__all__'
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<any[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => [
  { id: ROOT_CATEGORY_ID, categoryName: '全部客户', children: categoryList.value }
])

const currentPath = computed(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return '全部客户'
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
  return path.length ? path.join(' / ') : '全部客户'
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await partnerCategoryApi.getTree('CUSTOMER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[商城客户列表] 分类树加载失败', e)
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

// ═══ 客户级别下拉（GET /erp/partner/grades?gradeType=CUSTOMER，与 gradeId 过滤口径一致） ═══
const gradeOptions = ref<{ label: string; value: number }[]>([])
async function loadGrades() {
  try {
    const list = await partnerGradeApi.list('CUSTOMER')
    gradeOptions.value = (list || []).map((g: any) => ({ label: g.gradeName, value: g.id }))
  } catch (e) {
    console.warn('[商城客户列表] 客户级别加载失败', e)
    gradeOptions.value = []
  }
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'mallCustomer.createTime', label: '注册日期', visible: true },
  { key: 'mallCustomer.keyword', label: '客户', visible: true },
  { key: 'mallCustomer.gradeId', label: '客户级别', visible: true },
  { key: 'mallCustomer.showDisabled', label: '显示停用', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
/** 打印配置项（对标本页有打印） */
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showCategory', label: '打印抬头显示当前分类' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-mall-customer-list-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 打印(F8) ═══
function handlePrint() {
  const cols = columns.filter(c => c.type !== 'rowNo')
  const rows = dataSource.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const cell = (c: DetailColumnConfig, r: any) => {
    const fn = c.formatter
    return fn ? fn(r[c.key], r) : (r[c.key] ?? '-')
  }
  const html = `<html><head><meta charset="utf-8"><title>商城客户列表</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}
    th{background:#f2f2f2}</style></head><body>
    <h3>商城客户列表（${currentPath.value}）</h3>
    <table><thead><tr>${cols.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${cols.map(c => `<td>${cell(c, r)}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（按当前过滤条件逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

function exportColumns(): DetailColumnConfig[] {
  return columns.filter(c => c.type !== 'rowNo')
}

async function fetchAllRows(): Promise<any[]> {
  const size = 100
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await shopUserApi.page(buildParams(p, size))
    const list = normalize(res?.records)
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = exportColumns()
  const mapRows = (list: any[]) => list.map(r => cols.map(c => {
    const fn = c.formatter
    return fn ? fn(r[c.key], r) : String(r[c.key] ?? '-')
  }))
  executeExport({
    fileName: '商城客户列表',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: mapRows,
    fallbackRows: () => mapRows(dataSource.value.filter((r: any) => !r.__ghost))
  })
}

function handleError(err: any) {
  console.error('[商城客户列表] 页面异常', err)
}

onMounted(() => {
  loadCategoryTree()
  loadGrades()
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不在 scoped 作用域内，样式随页面自带（见《插槽内容的样式必须自备》） */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; display: flex; align-items: center; gap: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
