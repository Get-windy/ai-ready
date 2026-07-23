<template>
  <ErrorBoundary>
    <PageContainer title="采购分析">
      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="订单日期">
            <a-range-picker
              v-model:value="dateRange"
              :allow-clear="false"
              style="width: 240px"
            />
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="query.orderNo"
              placeholder="采购单号"
              allow-clear
              style="width: 180px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="query.supplierName"
              placeholder="供应商"
              allow-clear
              style="width: 180px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 图表区 ═══ -->
      <div class="chart-grid">
        <ARReportChart
          title="每日采购趋势"
          :option="trendOption"
          :loading="loading"
          :height="340"
        />
        <ARReportChart
          title="供应商采购额 TOP10"
          :option="supplierOption"
          :loading="loading"
          :height="340"
        />
      </div>

      <!-- ═══ 采购单据表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="docRows"
          :loading="docLoading"
          :pagination="docPagination"
          row-key="id"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
          @change="handleDocTableChange"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="PURCHASE_ORDER_STATUS[record.status]?.color || 'default'">
                {{ PURCHASE_ORDER_STATUS[record.status]?.text || record.status }}
              </a-tag>
            </template>
            <template v-else-if="['productAmount', 'billAmount'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="['totalQuantity', 'receivedQuantity'].includes(column.dataIndex as string)">
              {{ formatQty(text) }}
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { purchaseAnalyticsApi } from '@/api/analytics'
import type { PurchaseOrderStatistics } from '@/api/analytics'
import { PURCHASE_ORDER_STATUS } from '@/utils/statusConfig'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tenantId = computed(() => userStore.tenantId || 1)

// ═══ 状态 ═══
const loading = ref(false)
const dateRange = ref<[Dayjs, Dayjs]>([dayjs().subtract(29, 'day'), dayjs()])
const query = reactive({ orderNo: '', supplierName: '' })
const stats = ref<PurchaseOrderStatistics>({})
const inboundSummary = ref<{ count: number; amount: number }>({ count: 0, amount: 0 })

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const s = stats.value
  return [
    { label: '采购订单数', value: Number(s.totalOrders) || 0, suffix: '单' },
    { label: '采购总额', value: Number(s.totalFinalAmount ?? s.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待审批订单', value: Number(s.pendingApprovalCount) || 0, suffix: '单' },
    { label: '准时交付率', value: Number(s.onTimeDeliveryRate) || 0, suffix: '%' },
    { label: '入库单数', value: inboundSummary.value.count, suffix: '单' },
    { label: '入库金额', value: inboundSummary.value.amount, precision: 2, prefix: '¥' }
  ]
})

// ═══ 图表 ═══
const trendOption = computed(() => {
  const rows = [...(stats.value.dailyStatistics || [])]
    .sort((a, b) => String(a.date).localeCompare(String(b.date)))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['采购金额', '订单数'] },
    grid: { left: 70, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: rows.map(r => r.date) },
    yAxis: [
      { type: 'value', name: '金额(元)' },
      { type: 'value', name: '订单数', minInterval: 1 }
    ],
    series: [
      {
        name: '采购金额',
        type: 'bar',
        barMaxWidth: 22,
        itemStyle: { color: '#52c41a', borderRadius: [4, 4, 0, 0] },
        data: rows.map(r => Number(r.totalAmount) || 0)
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: rows.map(r => Number(r.orderCount) || 0)
      }
    ]
  }
})

const supplierOption = computed(() => {
  const top = (stats.value.topSuppliersByAmount || []).slice(0, 10).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 110, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map(r => r.supplierName || `供应商${r.supplierId}`) },
    series: [
      {
        name: '采购金额',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#fa8c16', borderRadius: [0, 4, 4, 0] },
        data: top.map(r => Number(r.totalAmount) || 0)
      }
    ]
  }
})

// ═══ 采购单据表（doc-query，current/size 分页） ═══
const docLoading = ref(false)
const docRows = ref<any[]>([])
const docPage = reactive({ current: 1, size: 20, total: 0 })

const docPagination = computed(() => ({
  current: docPage.current,
  pageSize: docPage.size,
  total: docPage.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

const columns: any[] = [
  { title: '采购单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '采购员', dataIndex: 'purchaserName', key: 'purchaserName', width: 90 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 100 },
  { title: '商品金额', dataIndex: 'productAmount', key: 'productAmount', width: 110, align: 'right' },
  { title: '单据金额', dataIndex: 'billAmount', key: 'billAmount', width: 110, align: 'right' },
  { title: '订货数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '已收数量', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '制单人', dataIndex: 'createByName', key: 'createByName', width: 90 }
]

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function dateParams() {
  return {
    startDate: dateRange.value[0].format('YYYY-MM-DD'),
    endDate: dateRange.value[1].format('YYYY-MM-DD')
  }
}

// ═══ 数据请求 ═══
async function loadStats() {
  loading.value = true
  try {
    const res: any = await purchaseAnalyticsApi.orderStatistics({
      tenantId: tenantId.value,
      ...dateParams()
    })
    stats.value = res?.data ?? res ?? {}
  } catch (e) {
    stats.value = {}
    console.warn('[采购分析] 采购统计获取失败', e)
  } finally {
    loading.value = false
  }
}

/** 入库汇总：/erp/purchase/inbound/statistics 为裸 Map 响应被拦截器拒绝，改用入库分页聚合 */
async function loadInboundSummary() {
  try {
    const res: any = await purchaseAnalyticsApi.inboundPage({ pageNum: 1, pageSize: 500 })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    inboundSummary.value = {
      count: Number(page.total) || 0,
      amount: (page.records || []).reduce(
        (acc: number, r: any) => acc + (Number(r.totalAmount) || 0), 0
      )
    }
  } catch (e) {
    inboundSummary.value = { count: 0, amount: 0 }
    console.warn('[采购分析] 入库汇总获取失败', e)
  }
}

async function loadDocPage() {
  docLoading.value = true
  try {
    const params: Record<string, any> = {
      current: docPage.current,
      size: docPage.size,
      orderNo: query.orderNo || undefined,
      supplierName: query.supplierName || undefined,
      dateStart: dateRange.value[0].format('YYYY-MM-DD'),
      dateEnd: dateRange.value[1].format('YYYY-MM-DD')
    }
    const res: any = await purchaseAnalyticsApi.docPage(params)
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    docRows.value = page.records || []
    docPage.total = Number(page.total) || 0
  } catch (e) {
    docRows.value = []
    docPage.total = 0
    console.warn('[采购分析] 采购单据获取失败', e)
  } finally {
    docLoading.value = false
  }
}

function handleDocTableChange(pag: { current?: number; pageSize?: number }) {
  docPage.current = pag.current || 1
  docPage.size = pag.pageSize || 20
  loadDocPage()
}

function handleSearch() {
  docPage.current = 1
  loadStats()
  loadDocPage()
}

function handleReset() {
  dateRange.value = [dayjs().subtract(29, 'day'), dayjs()]
  query.orderNo = ''
  query.supplierName = ''
  docPage.current = 1
  loadStats()
  loadDocPage()
}

onMounted(() => {
  loadStats()
  loadInboundSummary()
  loadDocPage()
})
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
