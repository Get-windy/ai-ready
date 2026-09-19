<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        营销活动分析（分析 → 营销分析 → 营销活动分析，菜单 80471）
        对标 ql361「分析 → 营销分析 → 营销活动分析」：**4 视图 Tab 复合页**（商品促销 / 整单促销 / 特价 / 优惠券），
        逐 Tab 独立列定义 + 独立查询区 + 独立列配置 storage-key（对标本页**无打印、无导出**，故不写 handlePrint）。
        列数口径（对标 2026-09-15 全局配置实测）：商品促销 12/12、整单促销 11/11、特价 8/8、优惠券 9/9，
        **4 个 Tab 全部列默认可见**（count == defCount），故列定义中不出现 defaultHidden。

        ⚠️ 取数现状（接口能力不足，已在开发文档「剩余缺口」与验收汇报中登记，待后端补字段）：
          · 行数据来源 = crmMarketingApi.page（GET /crm/marketing/page，CRM 营销活动表）；
          · 该接口为「活动主数据 + CRM 触达/转化计数」模型，与对标「促销让利效果」模型**不等价**；
          · 无字段可绑的对标列一律经 formatter 渲染 '-'（**不返回 0 / 不造占位假值**）：
              商品购买数量 / 优惠金额 / 赠品数量 / 客单价（4 个活动 Tab 中出现的部分）、
              领取张数 / 使用张数（优惠券 Tab），以及「日期范围 / 活动范围」两个后端未支持的查询项。
          · 已绑定的真实字段：活动名称←campaignName、日期←startDate、活动状态←status/statusDesc、
            参与订单数←actualOrders、参与客户数←convertedCustomerCount、惠后订单金额←actualRevenue、
            活动类型←campaignType、活动范围←targetRegion。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案（对标查询区首项；本页对标无刷新/打印/导出按钮） ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-mkt-activity-analysis-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
        </template>

        <!-- ═══ 查询区（逐 Tab 独立一套，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">{{ isCouponTab ? '优惠券名称' : '活动名称' }}</span>
                <a-input
                  v-model:value="tabQuery[activeTab].keyword"
                  size="small"
                  :placeholder="isCouponTab ? '优惠券名称' : '活动名称/编码'"
                  allow-clear
                  style="width: 180px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">{{ isCouponTab ? '优惠券状态' : '活动状态' }}</span>
                <a-select
                  v-model:value="tabQuery[activeTab].status"
                  size="small"
                  placeholder="全部"
                  allow-clear
                  style="width: 140px"
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="!isCouponTab"
                class="search-item"
              >
                <span class="search-label">活动类型</span>
                <a-select
                  v-model:value="tabQuery[activeTab].campaignType"
                  size="small"
                  placeholder="全部"
                  allow-clear
                  style="width: 140px"
                  :options="typeOptions"
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
              :storage-key="`analytics-mkt-activity-columns-${activeTab}`"
              :global-config-key="`analytics-mkt-activity-columns-${activeTab}`"
            >
              <template #activityNameCell="{ record }">
                <a
                  class="name-link"
                  @click="openDetail(record)"
                >{{ record.campaignName || '-' }}</a>
              </template>
              <template #couponNameCell="{ record }">
                <a
                  class="name-link"
                  @click="openDetail(record)"
                >{{ record.campaignName || '-' }}</a>
              </template>
              <template #actionCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  详情
                </a-button>
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

      <!-- ═══ 活动详情（本页唯一可用的真实行级动作；数据全部取自行记录自身字段） ═══ -->
      <a-modal
        v-model:open="detailVisible"
        title="活动详情"
        :width="720"
        :footer="null"
      >
        <a-descriptions
          v-if="detailRecord"
          bordered
          size="small"
          :column="2"
        >
          <a-descriptions-item label="活动编码">
            {{ detailRecord.campaignCode || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="活动名称">
            {{ detailRecord.campaignName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="活动类型">
            {{ detailRecord.campaignTypeText || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="活动状态">
            {{ detailRecord.statusText || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="开始日期">
            {{ detailRecord.startDate || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="结束日期">
            {{ detailRecord.endDate || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="预算">
            {{ fmtMoney(detailRecord.budget) }}
          </a-descriptions-item>
          <a-descriptions-item label="实际成本">
            {{ fmtMoney(detailRecord.actualCost) }}
          </a-descriptions-item>
          <a-descriptions-item label="预期收入">
            {{ fmtMoney(detailRecord.expectedRevenue) }}
          </a-descriptions-item>
          <a-descriptions-item label="实际收入">
            {{ fmtMoney(detailRecord.actualRevenue) }}
          </a-descriptions-item>
          <a-descriptions-item label="目标客户数">
            {{ fmtInt(detailRecord.targetCustomerCount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已触达客户数">
            {{ fmtInt(detailRecord.reachedCustomerCount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已响应客户数">
            {{ fmtInt(detailRecord.respondedCustomerCount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已转化客户数">
            {{ fmtInt(detailRecord.convertedCustomerCount) }}
          </a-descriptions-item>
          <a-descriptions-item label="实际订单数">
            {{ fmtInt(detailRecord.actualOrders) }}
          </a-descriptions-item>
          <a-descriptions-item label="目标区域">
            {{ detailRecord.targetRegion || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="负责人">
            {{ detailRecord.ownerName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ detailRecord.createTime || '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { crmMarketingApi } from '@/api/analytics'
import type { MarketingCampaignItem } from '@/api/analytics'
import { formatMoney } from '../shared/docActions'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsMktActivityAnalysis' })

// ═══ 视图 Tab（逐字取自对标实测 .dijitTabChecked：商品促销 / 整单促销 / 特价 / 优惠券） ═══
const TABS = [
  { key: 'goods', label: '商品促销' },
  { key: 'order', label: '整单促销' },
  { key: 'special', label: '特价' },
  { key: 'coupon', label: '优惠券' }
]
const activeTab = ref('goods')
const isCouponTab = computed(() => activeTab.value === 'coupon')

// ═══ 活动状态 / 类型（与后端 CampaignStatus / CampaignType 枚举一致） ═══
const CAMPAIGN_STATUS: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '已排期',
  4: '进行中', 5: '已暂停', 6: '已完成', 7: '已取消'
}
const CAMPAIGN_TYPE: Record<number, string> = {
  1: '邮件营销', 2: '短信营销', 3: '微信营销', 4: '电话营销', 5: '活动营销',
  6: '线上推广', 7: '线下推广', 8: '内容营销', 9: '社交媒体', 10: '综合营销'
}
const statusOptions = Object.entries(CAMPAIGN_STATUS).map(([value, label]) => ({ label, value: Number(value) }))
const typeOptions = Object.entries(CAMPAIGN_TYPE).map(([value, label]) => ({ label, value: Number(value) }))

// ═══ 查询态（逐 Tab 独立一套，切 Tab 不串用） ═══
interface TabQuery { keyword: string; status?: number; campaignType?: number }
const tabQuery = reactive<Record<string, TabQuery>>({
  goods: { keyword: '', status: undefined, campaignType: undefined },
  order: { keyword: '', status: undefined, campaignType: undefined },
  special: { keyword: '', status: undefined, campaignType: undefined },
  coupon: { keyword: '', status: undefined, campaignType: undefined }
})

// ═══ 取数 ═══
const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const q = tabQuery[activeTab.value]
    const res: any = await crmMarketingApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: q.keyword || undefined,
      status: q.status,
      campaignType: q.campaignType
    })
    // 行键：campaignCode 全局唯一（id 为雪花 BIGINT，按字符串透传，禁止做 Map 键或 Number()）
    dataSource.value = (res?.records || []).map((r: MarketingCampaignItem) => ({
      ...r,
      rowKey: String((r as any).campaignCode || (r as any).id),
      statusText: CAMPAIGN_STATUS[r.status] || r.statusDesc || '-',
      campaignTypeText: CAMPAIGN_TYPE[r.campaignType] || '-'
    }))
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    console.warn('[营销活动分析] 取数失败', e)
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
  Object.assign(tabQuery[activeTab.value], { keyword: '', status: undefined, campaignType: undefined })
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

// ═══ 列定义（逐 Tab 一套；列名逐字取自对标 §3，全部默认可见，无 defaultHidden） ═══
/** 无数据源列：统一渲染 '-'（不返回 0、不造占位假值，缺字段清单见页面头部注释） */
const EMPTY = () => '-'
const fmtInt = (v: any) => (v === null || v === undefined || v === '' || isNaN(Number(v)) ? '-' : String(Number(v)))
const fmtDate = (v: any) => (v ? String(v).slice(0, 10) : '-')
const fmtMoney = (v: any) => formatMoney(v)

/** 活动类 Tab 共用列（商品促销 / 整单促销 / 特价），差异仅在是否含「商品购买数量」等列 */
function buildActivityTabColumns(tab: string): DetailColumnConfig[] {
  const withProductQty = tab !== 'order'
  const cols: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
    { key: 'campaignName', title: '活动名称', type: 'slot', slotName: 'activityNameCell', width: 200 },
    { key: 'startDate', title: '日期', width: 110, formatter: fmtDate },
    { key: 'statusText', title: '活动状态', width: 100 },
    { key: 'actualOrders', title: '参与订单数', width: 110, align: 'right', formatter: fmtInt },
    { key: 'convertedCustomerCount', title: '参与客户数', width: 110, align: 'right', formatter: fmtInt }
  ]
  if (withProductQty) {
    cols.push({ key: 'productPurchaseQty', title: '商品购买数量', width: 120, align: 'right', formatter: EMPTY })
  }
  // 特价不做赠送、让利即价差本身 → 无「优惠金额 / 赠品数量 / 客单价 / 活动类型」三列 + 活动类型
  if (tab === 'goods' || tab === 'order') {
    cols.push({ key: 'discountAmount', title: '优惠金额', width: 120, align: 'right', formatter: EMPTY })
  }
  cols.push({ key: 'actualRevenue', title: '惠后订单金额', width: 130, align: 'right', formatter: fmtMoney })
  if (tab === 'goods' || tab === 'order') {
    cols.push({ key: 'giftQty', title: '赠品数量', width: 100, align: 'right', formatter: EMPTY })
    cols.push({ key: 'avgOrderAmount', title: '客单价', width: 110, align: 'right', formatter: EMPTY })
    cols.push({ key: 'campaignTypeText', title: '活动类型', width: 110 })
  }
  cols.push({ key: 'targetRegion', title: '活动范围', width: 140, formatter: (v: any) => v || '-' })
  return cols
}

/** 优惠券 Tab（9 列）：维度由「活动」换成「券」，无 `操作` 列 */
function buildCouponTabColumns(): DetailColumnConfig[] {
  return [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'campaignName', title: '优惠券名称', type: 'slot', slotName: 'couponNameCell', width: 200 },
    { key: 'startDate', title: '日期', width: 110, formatter: fmtDate },
    { key: 'statusText', title: '优惠券状态', width: 110 },
    { key: 'receivedCount', title: '领取张数', width: 100, align: 'right', formatter: EMPTY },
    { key: 'usedCount', title: '使用张数', width: 100, align: 'right', formatter: EMPTY },
    { key: 'actualRevenue', title: '惠后订单金额', width: 130, align: 'right', formatter: fmtMoney },
    { key: 'convertedCustomerCount', title: '使用客户数', width: 110, align: 'right', formatter: fmtInt },
    { key: 'discountAmount', title: '优惠金额', width: 110, align: 'right', formatter: EMPTY },
    { key: 'avgOrderAmount', title: '客单价', width: 110, align: 'right', formatter: EMPTY }
  ]
}

/** 逐 Tab 列定义缓存（切 Tab 直接用对应一套） */
const tabColumns: Record<string, DetailColumnConfig[]> = {
  goods: buildActivityTabColumns('goods'),
  order: buildActivityTabColumns('order'),
  special: buildActivityTabColumns('special'),
  coupon: buildCouponTabColumns()
}
const columns = computed<DetailColumnConfig[]>(() => tabColumns[activeTab.value] || [])

// ═══ 活动详情弹窗 ═══
const detailVisible = ref(false)
const detailRecord = ref<any>(null)
function openDetail(record: any) {
  detailRecord.value = record
  detailVisible.value = true
}

function handleError(err: any) {
  console.error('[营销活动分析] 页面异常', err)
}

onMounted(() => {
  fetchData()
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
.name-link { color: #1677ff; }
.name-link:hover { text-decoration: underline; }
</style>
