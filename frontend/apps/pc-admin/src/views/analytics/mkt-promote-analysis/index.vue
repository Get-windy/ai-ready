<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        营销推广分析（分析 → 营销分析 → 营销推广分析，菜单 80472）
        对标 ql361「分析 → 营销分析 → 营销推广分析」：**6 视图 Tab 复合页**
        （按商品 / 按优惠券 / 按促销活动 / 按拼团 / 按秒杀 / 推广客户列表），
        逐 Tab 独立列定义 + 独立查询区 + 独立列配置 storage-key；对标有 `打印(F8)` 与 `导出`，故两件都接。
        列数口径（对标 2026-09-15 全局配置实测）：11/12/13/12/11/13，**全部列默认可见**（count == defCount），
        故列定义中不出现 defaultHidden；6 个 Tab 均无 `操作` 列（对标实测 headers 无操作项）。

        ⚠️ 取数现状（接口能力不足，已在开发文档「剩余缺口」与验收汇报中登记，待后端补字段）：
          · **行源按 Tab 维度取各自真实列表接口**（禁用一个维度表冒充另一个维度）：
              按商品 ← /erp/product/page；按优惠券 ← /erp/marketing/coupon/page；
              按促销活动 ← /crm/marketing/page（/sale/promotion/page 实测 406 不可用，为后端缺口）；
              按拼团 ← /erp/marketing/group-buy/page；按秒杀 ← /erp/marketing/flash-sale/page；
              推广客户列表 ← /erp/mall/admin/user/page。
          · **漏斗指标列（分享次数 / 浏览次数 / 浏览人数 / 下单人数 / 下单笔数 / 下单金额 / 分享领取数 / 加购次数）
            全系统无埋点数据源**（无商城访问/分享行为表），一律经 formatter 渲染 '-'，
            **不返回 0、不造占位假值**；需后端补「推广触达漏斗」聚合接口后按列接线。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-mkt-promote-analysis-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出（对标实测三个按钮；图形视图切换后端无图表数据源，未接） ═══ -->
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
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（逐 Tab 独立一套，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="currentSearch.keywordLabel"
                class="search-item"
              >
                <span class="search-label">{{ currentSearch.keywordLabel }}</span>
                <a-input
                  v-model:value="tabQuery[activeTab].keyword"
                  size="small"
                  :placeholder="currentSearch.keywordLabel"
                  allow-clear
                  style="width: 180px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="currentSearch.statusOptions"
                class="search-item"
              >
                <span class="search-label">{{ currentSearch.statusLabel }}</span>
                <a-select
                  v-model:value="tabQuery[activeTab].status"
                  size="small"
                  placeholder="全部"
                  allow-clear
                  style="width: 140px"
                  :options="currentSearch.statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
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

        <!-- ═══ 数据表（逐 Tab 独立列配置：storage-key / global-config-key 同值且随 Tab 切换） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="`analytics-mkt-promote-columns-${activeTab}`"
              :global-config-key="`analytics-mkt-promote-columns-${activeTab}`"
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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { crmMarketingApi, shopUserApi } from '@/api/analytics'
import { couponApi, groupBuyApi, flashSaleApi } from '@/api/marketing'
import { productApi } from '@/api/erp/product'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsMktPromoteAnalysis' })

// ═══ 视图 Tab（逐字取自对标实测：按商品 / 按优惠券 / 按促销活动 / 按拼团 / 按秒杀 / 推广客户列表） ═══
const TABS = [
  { key: 'product', label: '按商品' },
  { key: 'coupon', label: '按优惠券' },
  { key: 'promotion', label: '按促销活动' },
  { key: 'group', label: '按拼团' },
  { key: 'flash', label: '按秒杀' },
  { key: 'customer', label: '推广客户列表' }
]
const activeTab = ref('product')

// ═══ 枚举（逐项来自后端 TS 类型注释，取值枚举对标待复核） ═══
/** 优惠券状态（LoyaltyCoupon.status） */
const COUPON_STATUS: Record<string, string> = {
  UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期', CANCELLED: '已取消'
}
/** 秒杀场次状态（FlashSale.status） */
const FLASH_STATUS: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已取消', 3: '已结束' }
/** CRM 营销活动类型（按促销活动 Tab；与对标促销类型枚举不同模型） */
const CAMPAIGN_TYPE: Record<number, string> = {
  1: '邮件营销', 2: '短信营销', 3: '微信营销', 4: '电话营销', 5: '活动营销',
  6: '线上推广', 7: '线下推广', 8: '内容营销', 9: '社交媒体', 10: '综合营销'
}
const CAMPAIGN_STATUS: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '已排期',
  4: '进行中', 5: '已暂停', 6: '已完成', 7: '已取消'
}

// ═══ 查询态（逐 Tab 独立一套）+ 逐 Tab 查询区形态 ═══
interface TabQuery { keyword: string; status?: any }
const tabQuery = reactive<Record<string, TabQuery>>({
  product: { keyword: '', status: undefined },
  coupon: { keyword: '', status: undefined },
  promotion: { keyword: '', status: undefined },
  group: { keyword: '', status: undefined },
  flash: { keyword: '', status: undefined },
  customer: { keyword: '', status: undefined }
})

interface TabSearchShape {
  keywordLabel?: string
  statusLabel?: string
  statusOptions?: { label: string; value: any }[]
}
/** 逐 Tab 查询区（后端各列表接口实际支持的过滤参数，未支持的项不摆控件避免静默失效） */
const TAB_SEARCH: Record<string, TabSearchShape> = {
  product: { keywordLabel: '商品名称/货号' },
  coupon: {
    statusLabel: '优惠券状态',
    statusOptions: Object.entries(COUPON_STATUS).map(([value, label]) => ({ label, value }))
  },
  promotion: {
    keywordLabel: '活动名称/编码',
    statusLabel: '活动状态',
    statusOptions: Object.entries(CAMPAIGN_STATUS).map(([value, label]) => ({ label, value: Number(value) }))
  },
  group: {},
  flash: {
    keywordLabel: '活动名称',
    statusLabel: '场次状态',
    statusOptions: Object.entries(FLASH_STATUS).map(([value, label]) => ({ label, value: Number(value) }))
  },
  customer: { keywordLabel: '筛选条件' }
}
const currentSearch = computed<TabSearchShape>(() => TAB_SEARCH[activeTab.value] || {})

// ═══ 取数（逐 Tab 各自真实列表接口，统一归一化为 { records, total }） ═══
const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 逐 Tab 行源与行键（id 为雪花 BIGINT，一律按字符串透传） */
async function fetchTabRows(tab: string, page: number, size: number, q: TabQuery): Promise<{ records: any[]; total: number }> {
  switch (tab) {
    case 'product': {
      const res: any = await productApi.page({ pageNum: page, pageSize: size, keyword: q.keyword || undefined })
      return { records: (res?.records || []).map((r: any) => ({ ...r, rowKey: `P:${r.id}` })), total: Number(res?.total) || 0 }
    }
    case 'coupon': {
      const res: any = await couponApi.page({ pageNum: page, pageSize: size, status: q.status || undefined })
      return {
        records: (res?.records || []).map((r: any) => ({
          ...r,
          rowKey: `C:${r.id}`,
          statusText: COUPON_STATUS[r.status] || r.status || '-'
        })),
        total: Number(res?.total) || 0
      }
    }
    case 'promotion': {
      const res: any = await crmMarketingApi.page({
        pageNum: page, pageSize: size, keyword: q.keyword || undefined, status: q.status
      })
      return {
        records: (res?.records || []).map((r: any) => ({
          ...r,
          rowKey: `M:${r.campaignCode || r.id}`,
          campaignTypeText: CAMPAIGN_TYPE[r.campaignType] || '-',
          statusText: CAMPAIGN_STATUS[r.status] || r.statusDesc || '-'
        })),
        total: Number(res?.total) || 0
      }
    }
    case 'group': {
      const res: any = await groupBuyApi.page({ pageNum: page, pageSize: size })
      return { records: (res?.records || []).map((r: any) => ({ ...r, rowKey: `G:${r.id}` })), total: Number(res?.total) || 0 }
    }
    case 'flash': {
      const res: any = await flashSaleApi.page({ pageNum: page, pageSize: size, title: q.keyword || undefined, status: q.status })
      return {
        records: (res?.records || []).map((r: any) => ({
          ...r, rowKey: `F:${r.id}`, statusText: FLASH_STATUS[r.status] || '-'
        })),
        total: Number(res?.total) || 0
      }
    }
    case 'customer':
    default: {
      const res: any = await shopUserApi.page({ pageNum: page, pageSize: size, keyword: q.keyword || undefined, status: q.status })
      return { records: (res?.records || []).map((r: any) => ({ ...r, rowKey: `U:${r.id}` })), total: Number(res?.total) || 0 }
    }
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await fetchTabRows(activeTab.value, pagination.current, pagination.pageSize, tabQuery[activeTab.value])
    dataSource.value = res.records
    pagination.total = res.total
  } catch (e) {
    console.warn('[营销推广分析] 取数失败', e)
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { ...tabQuery }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.keys(tabQuery).forEach(k => { if (v[k]) Object.assign(tabQuery[k], v[k]) })
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(tabQuery[activeTab.value], { keyword: '', status: undefined })
  handleSearch()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

// ═══ 列定义（逐 Tab 一套；列名逐字取自对标 §3，全部默认可见，无 defaultHidden） ═══
/** 无数据源列 / 单元格兜底：渲染 '-'（不返回 0、不造占位假值） */
const EMPTY = () => '-'
const dash = (v: any) => (v === null || v === undefined || v === '' ? '-' : String(v))
const num = (v: any) => (v === null || v === undefined || v === '' || isNaN(Number(v)) ? '-' : String(Number(v)))
const dateTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')
const dateOnly = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 10) : '-')

function col(key: string, title: string, width: number, formatter: (v: any, r?: any) => string = dash, align?: 'left' | 'right' | 'center'): DetailColumnConfig {
  return { key, title, width, formatter, align }
}
const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

/** Tab1 按商品（11 列） */
const productColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('productName', '商品名称', 200),
  col('productCode', '货号', 130),
  col('brand', '品牌', 110),
  col('spec', '规格', 110),
  col('model', '型号', 110),
  col('unit', '单位', 80),
  col('shareCount', '分享次数', 100, EMPTY, 'right'),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('viewUserCount', '浏览人数', 100, EMPTY, 'right'),
  col('orderQty', '下单数量', 100, EMPTY, 'right'),
  col('orderAmount', '下单金额', 120, EMPTY, 'right')
]

/** Tab2 按优惠券（12 列） */
const couponColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('code', '优惠券名称', 180),
  col('couponType', '类型', 100, EMPTY),
  col('startTime', '起始时间', 150, EMPTY),
  col('expirationDate', '到期时间', 150, dateTime),
  col('statusText', '状态', 100),
  col('shareCount', '分享次数', 100, EMPTY, 'right'),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('viewUserCount', '浏览人数', 100, EMPTY, 'right'),
  col('receivedCount', '分享领取数', 110, EMPTY, 'right'),
  col('orderUserCount', '下单人数', 100, EMPTY, 'right'),
  col('orderCount', '下单笔数', 100, EMPTY, 'right'),
  col('orderAmount', '下单金额', 120, EMPTY, 'right')
]

/** Tab3 按促销活动（13 列） */
const promotionColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('campaignName', '促销活动名称', 200),
  col('promotionWay', '促销方式', 110, EMPTY),
  col('campaignTypeText', '促销类型', 110),
  col('promotionMode', '促销模式', 110, EMPTY),
  col('description', '促销规则', 200),
  col('startDate', '起始时间', 120, dateOnly),
  col('endDate', '结束时间', 120, dateOnly),
  col('shareCount', '分享次数', 100, EMPTY, 'right'),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('viewUserCount', '浏览人数', 100, EMPTY, 'right'),
  col('orderUserCount', '下单人数', 100, EMPTY, 'right'),
  col('actualOrders', '下单笔数', 100, num, 'right'),
  col('actualRevenue', '下单金额', 120, (v: any) => (v === null || v === undefined || v === '' ? '-' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })), 'right')
]

/** Tab4 按拼团（12 列） */
const groupColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('activityName', '活动名称', 200),
  col('status', '状态', 100),
  col('groupType', '成团类型', 110, EMPTY),
  col('startTime', '起始时间', 150, dateTime),
  col('endTime', '结束时间', 150, dateTime),
  col('createTime', '创建时间', 150, dateTime),
  col('shareCount', '分享次数', 100, EMPTY, 'right'),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('viewUserCount', '浏览人数', 100, EMPTY, 'right'),
  col('orderUserCount', '下单人数', 100, EMPTY, 'right'),
  col('orderCount', '下单笔数', 100, EMPTY, 'right'),
  col('orderAmount', '下单金额', 120, EMPTY, 'right')
]

/** Tab5 按秒杀（11 列，与按拼团差异：少「成团类型」） */
const flashColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('title', '活动名称', 200),
  col('statusText', '状态', 100),
  col('startTime', '起始时间', 150, dateTime),
  col('endTime', '结束时间', 150, dateTime),
  col('createTime', '创建时间', 150, dateTime),
  col('shareCount', '分享次数', 100, EMPTY, 'right'),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('viewUserCount', '浏览人数', 100, EMPTY, 'right'),
  col('orderUserCount', '下单人数', 100, EMPTY, 'right'),
  col('orderCount', '下单笔数', 100, EMPTY, 'right'),
  col('orderAmount', '下单金额', 120, EMPTY, 'right')
]

/** Tab6 推广客户列表（13 列；客户名称走插槽，企业客户取公司名，个人会员取昵称） */
const customerColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('username', '客户编号', 140),
  { key: 'companyName', title: '客户名称', type: 'slot', slotName: 'customerNameCell', width: 180 },
  col('defaultHandlerName', '经手人', 110),
  col('promoterName', '推广人', 110, EMPTY),
  col('customerLevel', '客户级别', 140),
  col('createTime', '新增时间', 150, dateTime),
  col('contactName', '联系人', 110),
  col('phone', '联系电话', 130),
  col('address', '联系地址', 200),
  col('viewCount', '浏览次数', 100, EMPTY, 'right'),
  col('cartCount', '加购次数', 100, EMPTY, 'right'),
  col('orderCount', '下单笔数', 100, EMPTY, 'right'),
  col('orderAmount', '下单金额', 120, EMPTY, 'right')
]

const tabColumns: Record<string, DetailColumnConfig[]> = {
  product: productColumns,
  coupon: couponColumns,
  promotion: promotionColumns,
  group: groupColumns,
  flash: flashColumns,
  customer: customerColumns
}
const columns = computed<DetailColumnConfig[]>(() => tabColumns[activeTab.value] || [])

/** 打印/导出取值：统一走列 formatter（插槽列也挂了 formatter，见列定义） */
function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : dash(raw)
}
function printableColumns(): DetailColumnConfig[] {
  return columns.value.filter(c => c.type !== 'rowNo')
}

// ═══ 打印(F8)（对标有打印按钮） ═══
function handlePrint() {
  const cols = printableColumns()
  const rows = dataSource.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const html = `<html><head><meta charset="utf-8"><title>营销推广分析</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}
    th{background:#f2f2f2}</style></head><body>
    <h3>营销推广分析 · ${TABS.find(t => t.key === activeTab.value)?.label || ''}</h3>
    <table><thead><tr>${cols.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${cols.map(c => `<td>${cellText(c, r)}</td>`).join('')}</tr>`).join('')}</tbody>
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

// ═══ 导出（按当前 Tab 的行源逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 100
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res = await fetchTabRows(activeTab.value, p, size, tabQuery[activeTab.value])
    all.push(...res.records)
    if (res.records.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns()
  const mapRows = (list: any[]) => list.map(r => cols.map(c => cellText(c, r)))
  executeExport({
    fileName: `营销推广分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: mapRows,
    fallbackRows: () => mapRows(dataSource.value.filter((r: any) => !r.__ghost))
  })
}

function handleError(err: any) {
  console.error('[营销推广分析] 页面异常', err)
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
/* 插槽内容不在 scoped 作用域内，样式随页面自带（见《插槽内容的样式必须自备》） */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
