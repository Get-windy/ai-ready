<template>
  <ErrorBoundary>
    <PageContainer
      title="销售价格跟踪"
      full-height
    >
      <template #headerExtra>
        <a-button
          size="small"
          @click="loadData"
        >
          <template #icon>
            <ReloadOutlined />
          </template>刷新
        </a-button>
      </template>

      <div class="page-scroll">
        <a-alert
          type="info"
          show-icon
          class="track-alert"
          message="价格走势按销售明细口径呈现"
          description="后端价格跟踪端点为明细级查询（每行=一条出库明细），本页按商品+单据日期展示销售单价走势与明细，数据全部来自真实销售出库单据。"
        />

        <!-- ═══ 查询区 ═══ -->
        <div class="search-area">
          <a-form layout="inline">
            <a-form-item label="商品">
              <a-input
                v-model:value="queryModel.productName"
                placeholder="商品名称（跟踪单商品请填写）"
                allow-clear
                style="width: 180px"
                @press-enter="handleSearch"
              />
            </a-form-item>
            <a-form-item label="货号">
              <a-input
                v-model:value="queryModel.productCode"
                placeholder="货号"
                allow-clear
                style="width: 140px"
                @press-enter="handleSearch"
              />
            </a-form-item>
            <a-form-item label="客户">
              <a-input
                v-model:value="queryModel.customerName"
                placeholder="客户名称"
                allow-clear
                style="width: 160px"
                @press-enter="handleSearch"
              />
            </a-form-item>
            <a-form-item label="单据日期">
              <a-range-picker
                v-model:value="dateRange"
                allow-clear
                style="width: 240px"
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

        <!-- ═══ 价格走势 ═══ -->
        <ARReportChart
          title="销售单价走势（当前查询结果）"
          :option="chartOption"
          :loading="loading"
          :height="300"
          empty-text="执行查询后按单据日期展示销售单价走势"
        />

        <!-- ═══ 明细表 ═══ -->
        <div class="table-area">
          <a-table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="tablePagination"
            row-key="_rk"
            size="small"
            :locale="{ emptyText: '暂无销售价格明细，请调整查询条件' }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, text }">
              <template v-if="MONEY_COLUMNS.includes(column.dataIndex as string)">
                {{ formatMoney(text) }}
              </template>
              <template v-else-if="column.dataIndex === 'salesQuantity'">
                {{ formatQty(text) }}
              </template>
              <template v-else-if="column.dataIndex === 'discountRate'">
                {{ formatRate(text) }}
              </template>
              <template v-else-if="column.dataIndex === 'docDate'">
                {{ formatDate(text) }}
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import { salesPriceTrackApi } from '@/api/erp'
import type { SalesPriceTrackItem } from '@/api/erp'

const MONEY_COLUMNS = ['unitPrice', 'discountedPrice', 'amount', 'wholesalePrice', 'retailPrice', 'minSalePrice']

// ═══ 查询状态 ═══
const queryModel = reactive({ productName: '', productCode: '', customerName: '' })
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const tablePagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

// ═══ 表格列（销售明细价格口径） ═══
const columns: any[] = [
  { title: '单据日期', dataIndex: 'docDate', key: 'docDate', width: 110 },
  { title: '单据编号', dataIndex: 'docNo', key: 'docNo', width: 150 },
  { title: '单据类型', dataIndex: 'docType', key: 'docType', width: 90 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, ellipsis: true },
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 110 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 100, ellipsis: true },
  { title: '单位', dataIndex: 'salesQuantityUnit', key: 'salesQuantityUnit', width: 60 },
  { title: '销售数量', dataIndex: 'salesQuantity', key: 'salesQuantity', width: 90, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '折扣(%)', dataIndex: 'discountRate', key: 'discountRate', width: 80, align: 'right' },
  { title: '折后单价', dataIndex: 'discountedPrice', key: 'discountedPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '批发价', dataIndex: 'wholesalePrice', key: 'wholesalePrice', width: 100, align: 'right' },
  { title: '零售价', dataIndex: 'retailPrice', key: 'retailPrice', width: 100, align: 'right' },
  { title: '最低售价', dataIndex: 'minSalePrice', key: 'minSalePrice', width: 100, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatRate(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return `${Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`
}

function formatDate(val: string | null | undefined): string {
  return val ? String(val).slice(0, 10) : '-'
}

// ═══ 数据请求（/sales/price-track/page，分页参数 current/size） ═══
interface TrackRow extends SalesPriceTrackItem { _rk: string }

const loading = ref(false)
const tableData = ref<TrackRow[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await salesPriceTrackApi.page({
      current: pagination.current,
      size: pagination.pageSize,
      productName: queryModel.productName || undefined,
      productCode: queryModel.productCode || undefined,
      customerName: queryModel.customerName || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    const records = (res?.records || []) as SalesPriceTrackItem[]
    // 后端行为明细级 Map 无唯一主键，合成行键保证 rowKey 唯一
    tableData.value = records.map((r, i) => ({
      ...r,
      _rk: `${r.docNo || ''}|${r.productCode || ''}|${i}`
    }))
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    console.warn('[销售价格跟踪] 获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryModel.productName = ''
  queryModel.productCode = ''
  queryModel.customerName = ''
  dateRange.value = null
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadData()
}

// ═══ 价格走势图（当前页结果按单据日期升序） ═══
const sortedTrend = computed(() =>
  [...tableData.value]
    .filter(r => r.docDate)
    .sort((a, b) => String(a.docDate).localeCompare(String(b.docDate)))
)

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    valueFormatter: (v: any) => (v === null || v === undefined ? '-' : `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`)
  },
  legend: { data: ['单价', '折后单价'], top: 0 },
  grid: { left: 70, right: 30, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: sortedTrend.value.map(r => formatDate(r.docDate)) },
  yAxis: { type: 'value', name: '单价(元)' },
  series: [
    {
      name: '单价',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      itemStyle: { color: '#1890ff' },
      data: sortedTrend.value.map(r => (r.unitPrice === null || r.unitPrice === undefined ? null : Number(r.unitPrice)))
    },
    {
      name: '折后单价',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      itemStyle: { color: '#52c41a' },
      data: sortedTrend.value.map(r => (r.discountedPrice === null || r.discountedPrice === undefined ? null : Number(r.discountedPrice)))
    }
  ]
}))

onMounted(() => {
  dateRange.value = [dayjs().subtract(89, 'day'), dayjs()]
  loadData()
})
</script>

<style scoped>
.page-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

.track-alert {
  margin: 12px 0 16px;
}

.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-top: 16px;
}
</style>
