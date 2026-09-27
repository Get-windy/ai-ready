<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        推广分析（分析 → 采销分析 → 销售分析 → 推广分析，菜单 80418）
        对标 ql361「推广分析」：单视图 7 列（职员编号 / 职员名称 / 分享次数 / 浏览次数 /
        下单笔数 / 下单金额 / 新客注册，全部默认可见，无默认隐藏列）；
        查询区：查询方案 + 8 段时间快捷段 + 日期范围 + 职员 + 分享类型；工具栏 刷新｜打印(F8)｜导出｜页面配置。
        取数：/erp/sale/analysis/promotion-funnel/page（ShareRecord 推广分享触达台账，按分享人归因）。
        ⚠️ 「新客注册」全库无归因链路（无「分享链接 → 客户注册」关系表），后端返回 null、本页显示 -
           —— 不填 0 冒充，缺口已在开发文档登记。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <!-- 查询方案（对标 `--查询方案--` 下拉 + 保存，落本机 localStorage） -->
          <QuerySchemeBar
            storage-key="analytics-promotion-analysis-query-scheme"
            :snapshot="schemeSnapshot"
            @apply="applyScheme"
          />
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('print')" size="small" @click="handlePrint">
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

        <!-- ═══ 查询区（对标：日期范围 + 职员 + 分享类型，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('promotion.dateRange')" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 240px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('promotion.staffName')" class="search-item">
                <span class="search-label">职员</span>
                <a-input
                  v-model:value="query.staffName"
                  size="small"
                  placeholder="职员姓名/账号"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('promotion.shareType')" class="search-item">
                <span class="search-label">分享类型</span>
                <a-select
                  v-model:value="query.shareType"
                  size="small"
                  style="width: 130px"
                  placeholder="全部"
                  allow-clear
                  :options="SHARE_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置） ═══ -->
        <template #table>
          <div class="table-area">
            <a-alert
              class="gap-alert"
              type="warning"
              show-icon
              message="「新客注册」列：本系统无「分享链接 → 客户注册」归因链路，该列如实显示 -（不填 0 冒充）；分享/浏览/下单四列取自分享触达台账 mkt_share_record。"
            />
            <BillDetailTable
              :data-source="rows"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              storage-key="analytics-promotion-analysis-columns"
              global-config-key="analytics-promotion-analysis-columns"
            >
              <template #emptyCell>
                <span class="cell-empty">-</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标有「页面配置」弹窗 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>

    <!-- 打印：结果集打印 -->
    <PrintDialog ref="printDialogRef" page-code="analytics-promotion-analysis" :print-data="printData" />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
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
import { promotionFunnelApi } from '@/api/analytics-supply'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsPromotionAnalysis' })

/** 分享类型（后端 mkt_share_record.share_type 白名单） */
const SHARE_TYPE_OPTIONS = [
  { label: '商品分享', value: 'PRODUCT' },
  { label: '优惠券分享', value: 'COUPON' },
  { label: '促销分享', value: 'PROMOTION' },
  { label: '拼团分享', value: 'GROUP_BUY' },
  { label: '秒杀分享', value: 'FLASH_SALE' }
]

const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  staffName: '',
  shareType: undefined as string | undefined
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})

// ═══ 列定义（列名与顺序逐字取自《推广分析开发文档》§3：全部 7 列 / 默认 7 列） ═══
function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'staffCode', title: '职员编号', width: 120, align: 'left' },
  { key: 'dimLabel', title: '职员名称', width: 140, align: 'left' },
  { key: 'shareCount', title: '分享次数', width: 100, align: 'right', formatter: fmtNum, headerTip: '职员转发商城商品/店铺链接的次数' },
  { key: 'viewCount', title: '浏览次数', width: 100, align: 'right', formatter: fmtNum, headerTip: '客户打开被分享链接的次数' },
  { key: 'orderCount', title: '下单笔数', width: 100, align: 'right', formatter: fmtNum, headerTip: '经分享链接转化成交的订单笔数' },
  { key: 'orderAmount', title: '下单金额', width: 120, align: 'right', formatter: fmtMoney, headerTip: '经分享链接转化成交的订单金额' },
  // 无数据源列：占列位与列名，取值恒为空并显示 `-`
  { key: 'newCustomerCount', title: '新客注册', width: 100, align: 'right', type: 'slot', slotName: 'emptyCell' }
]

/** 底部合计行（后端按当前过滤范围 SUM，非当前页求和） */
const summaryColumns = computed(() => ['shareCount', 'viewCount', 'orderCount', 'orderAmount']
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'promotion.dateRange', label: '日期', visible: true },
  { key: 'promotion.staffName', label: '职员', visible: true },
  { key: 'promotion.shareType', label: '分享类型', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-promotion-analysis-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 查询方案（本机 localStorage，方案内容 = 当前查询条件快照） ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, ...query }
}

/** 回填方案：日期区间与快捷段一并还原（方案里存的就是这两者 + 业务筛选） */
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  Object.keys(query).forEach(k => {
    if (k in v) (query as any)[k] = v[k]
  })
  handleSearch()
}

// ═══ 取数 ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await promotionFunnelApi.page({
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      staffName: query.staffName || undefined,
      shareType: query.shareType,
      page: pagination.page,
      size: pagination.size
    })
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[推广分析] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}

function handleReset() {
  query.staffName = ''
  query.shareType = undefined
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month') as [string, string]
  handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}

// ═══ 打印(F8) / 导出（当前默认可见列，所见即所打） ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  columns.filter(c => c.type !== 'rowNo' && c.type !== 'action' && c.type !== 'checkbox' && !c.defaultHidden)
)

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  if (c.key === 'newCustomerCount') return raw === null || raw === undefined ? '-' : String(raw)
  return c.formatter ? c.formatter(raw, r) : (raw === null || raw === undefined || raw === '' ? '-' : String(raw))
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 再调浏览器打印，现在交给 PrintDialog：
// 列随 Tab 变化（computed），按当前可见列打，故 useDataColumns。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-promotion-analysis',
  title: () => `推广分析（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）`,
  rows: () => rows.value,
  columns: () => printableColumns.value,
  useDataColumns: true,
  emptyTip: '没有可打印的数据',
})

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res = await promotionFunnelApi.page({
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      staffName: query.staffName || undefined,
      shareType: query.shareType,
      page: p,
      size
    })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: '推广分析',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[推广分析] 页面异常', err)
}

onMounted(() => {
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
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.gap-alert { margin-bottom: 8px; flex-shrink: 0; }
.cell-empty { color: #bfbfbf; }
</style>
