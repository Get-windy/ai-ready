<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出（列配置齿轮在数据表表头右上角） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：7 条件（固定 UI） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <a-input
                  v-model:value="searchParams.keyword"
                  placeholder="流水号/来源订单"
                  style="width: 200px"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 220px"
                  :allow-clear="true"
                  @change="onDateRangeChange"
                />
              </div>
              <div class="search-item">
                <a-input
                  v-model:value="searchParams.customerName"
                  placeholder="源单客户"
                  style="width: 160px"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">支付类型</span>
                <a-select
                  v-model:value="searchParams.paymentType"
                  style="width: 110px"
                  size="small"
                  allow-clear
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option :value="1">收款</a-select-option>
                  <a-select-option :value="2">退款</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">支付状态</span>
                <a-select
                  v-model:value="searchParams.payStatus"
                  style="width: 110px"
                  size="small"
                  allow-clear
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option value="SUCCESS">支付成功</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">支付方式</span>
                <a-select
                  v-model:value="searchParams.paymentMethod"
                  style="width: 130px"
                  size="small"
                  allow-clear
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option
                    v-for="opt in paymentMethodOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">对账标记</span>
                <a-select
                  v-model:value="searchParams.reconcileFlag"
                  style="width: 110px"
                  size="small"
                  allow-clear
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option :value="0">否</a-select-option>
                  <a-select-option :value="1">是</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >查询</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域（列配置齿轮在表头 rowNo 列右上角） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="pagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-data="footerColumns"
              storage-key="finance-online-payment-reconcile-columns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #reconcileFlagCell="{ record }">
                <a-tag
                  :color="record.reconcileFlag === 1 ? 'green' : 'default'"
                  style="cursor: pointer"
                  @click="toggleReconcile(record)"
                >
                  {{ record.reconcileFlag === 1 ? '✓ 是' : '否' }}
                </a-tag>
              </template>
              <template #amountCell="{ record }">
                <span class="num-value">{{ formatMoney(record.amount) }}</span>
              </template>
              <template #payStatusCell="{ record }">
                <a-tag color="blue">{{ formatPayStatus(record.payStatus) }}</a-tag>
              </template>
              <template #directionCell="{ record }">
                <a-tag :color="record.direction === 'IN' ? 'green' : 'red'">
                  {{ formatDirection(record.direction) }}
                </a-tag>
              </template>
              <template #paymentMethodCell="{ record }">
                {{ formatPaymentMethod(record.paymentMethod) }}
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="finance-online-payment-reconcile"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, PrinterOutlined, ExportOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { capitalFlowApi } from '@/api/finance'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

// ═══ 字典 ═══
const PAYMENT_METHOD_MAP: Record<number, string> = {
  1: '现金',
  2: '银行转账',
  3: '支票',
  4: '信用卡',
  5: '在线支付',
  6: '抵扣',
  7: '其他',
}
const paymentMethodOptions = Object.entries(PAYMENT_METHOD_MAP).map(([value, label]) => ({
  label,
  value: Number(value),
}))

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const dateRange = ref<any[]>([])

// ═══ 搜索参数 ═══
const searchParams = reactive<Record<string, any>>({
  keyword: '',
  customerName: '',
  paymentType: undefined,
  payStatus: undefined,
  paymentMethod: undefined,
  reconcileFlag: undefined,
})
let startDate: string | undefined
let endDate: string | undefined

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 列定义（rowNo 为首列，BillDetailTable 会在其表头渲染内置【列配置】齿轮） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '对账标记', field: 'reconcileFlag', key: 'reconcileFlag', width: 90, align: 'center', type: 'slot', slotName: 'reconcileFlagCell', fixed: 'left' },
  { title: '来源订单', field: 'refNo', key: 'refNo', width: 160, ellipsis: true },
  { title: '提交日期', field: 'occurDate', key: 'occurDate', width: 170 },
  { title: '源单客户', field: 'partyName', key: 'partyName', width: 150, ellipsis: true },
  { title: '订单金额', field: 'amount', key: 'amount', width: 130, align: 'right', type: 'slot', slotName: 'amountCell' },
  { title: '流水号', field: 'flowNo', key: 'flowNo', width: 210, ellipsis: true },
  { title: '支付状态', field: 'payStatus', key: 'payStatus', width: 110, align: 'center', type: 'slot', slotName: 'payStatusCell' },
  { title: '关联交易号', field: 'transactionNo', key: 'transactionNo', width: 200, ellipsis: true },
  { title: '支付类型', field: 'direction', key: 'direction', width: 90, align: 'center', type: 'slot', slotName: 'directionCell' },
  { title: '支付方式', field: 'paymentMethod', key: 'paymentMethod', width: 110, align: 'center', type: 'slot', slotName: 'paymentMethodCell' },
]

// ═══ 格式化 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatPayStatus(val: string | null | undefined): string {
  if (val === 'SUCCESS' || val === 'success') return '支付成功'
  return val || '支付成功'
}
function formatDirection(val: string | null | undefined): string {
  if (val === 'IN') return '收款'
  if (val === 'OUT') return '退款'
  return val || '-'
}
function formatPaymentMethod(val: number | string | null | undefined): string {
  if (val === null || val === undefined || val === '') return '-'
  return PAYMENT_METHOD_MAP[Number(val)] || String(val)
}
function formatDateTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.paymentType !== undefined && searchParams.paymentType !== null && searchParams.paymentType !== '') params.paymentType = searchParams.paymentType
    if (searchParams.payStatus) params.payStatus = searchParams.payStatus
    if (searchParams.paymentMethod !== undefined && searchParams.paymentMethod !== null && searchParams.paymentMethod !== '') params.paymentMethod = searchParams.paymentMethod
    if (searchParams.reconcileFlag !== undefined && searchParams.reconcileFlag !== null && searchParams.reconcileFlag !== '') params.reconcileFlag = searchParams.reconcileFlag
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const res: any = await capitalFlowApi.getReconcilePage(params)
    const body = res?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[在线支付对账] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleRefresh() { fetchData() }
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}
function onDateRangeChange(dates: any) {
  if (dates && dates[0] && dates[1]) {
    startDate = dayjs(dates[0]).format('YYYY-MM-DD')
    endDate = dayjs(dates[1]).format('YYYY-MM-DD')
  } else {
    startDate = undefined
    endDate = undefined
  }
}

// ═══ 对账标记（点击切换 是/否） ═══
async function toggleReconcile(record: any) {
  const next = record.reconcileFlag === 1 ? 0 : 1
  try {
    await capitalFlowApi.toggleReconcile(record.id, next)
    record.reconcileFlag = next
    message.success(next === 1 ? '已标记对账' : '已取消对账')
  } catch {
    message.error('操作失败')
  }
}

// ═══ 打印(F8) ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'finance-online-payment-reconcile',
  title: 'finance-online-payment-reconcile',
  columns: () => columns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})
function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrintF8()
  }
}
onMounted(() => window.addEventListener('keydown', handleF8Key))
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))

// ═══ 导出 ═══
async function handleExport() {
  try {
    const params: Record<string, any> = { pageNum: 1, pageSize: 9999 }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.paymentType) params.paymentType = searchParams.paymentType
    if (searchParams.payStatus) params.payStatus = searchParams.payStatus
    if (searchParams.paymentMethod) params.paymentMethod = searchParams.paymentMethod
    if (searchParams.reconcileFlag !== undefined && searchParams.reconcileFlag !== null) params.reconcileFlag = searchParams.reconcileFlag
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const res: any = await capitalFlowApi.getReconcilePage(params)
    const body = res?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['对账标记', '来源订单', '提交日期', '源单客户', '订单金额', '流水号', '支付状态', '关联交易号', '支付类型', '支付方式']
    const rows = data.map((r: any) => [
      r.reconcileFlag === 1 ? '是' : '否',
      r.refNo,
      formatDateTime(r.occurDate),
      r.partyName,
      r.amount,
      r.flowNo,
      formatPayStatus(r.payStatus),
      r.transactionNo,
      formatDirection(r.direction),
      formatPaymentMethod(r.paymentMethod),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `在线支付对账_${new Date().toISOString().slice(0, 19).replace(/[-T:]/g, '')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 合计行（订单金额合计） ═══
const footerColumns = computed(() => {
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.amount) || 0), 0)
  return [
    { key: 'amount', value: totalAmount, highlight: true },
  ]
})

function handleError(error: Error) {
  console.error('[在线支付对账] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-input), .search-item :deep(.ant-select) { font-size: 13px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.num-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
