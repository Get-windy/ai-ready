<template>
  <ARReportPage
    ref="reportRef"
    title="缺货补货"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :enable-row-selection="true"
    :stat-cards="statCards"
    export-file-name="缺货补货"
    row-key="id"
    empty-text="暂无缺货商品（当前库存均大于 0）"
    @loaded="handleLoaded"
    @selection-change="handleSelectionChange"
  >
    <template #header-extra>
      <a-space :size="8">
        <a-button
          type="primary"
          :disabled="selectedRows.length === 0"
          @click="handleBatchCreateOrder"
        >
          <template #icon>
            <ShoppingCartOutlined />
          </template>生成采购订单({{ selectedRows.length }})
        </a-button>
        <a-button
          type="primary"
          :loading="generating"
          @click="handleGenerate"
        >
          <template #icon>
            <ThunderboltOutlined />
          </template>生成补货建议
        </a-button>
      </a-space>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'quantity'">
        <span class="qty-zero">{{ formatQty(text) }}</span>
      </template>
      <template v-else-if="column.key === 'shortageQty'">
        {{ formatQty(Math.max((Number(record.safetyStock) || 0) - (Number(record.quantity) || 0), 0)) }}
      </template>
      <template v-else-if="['availableQuantity', 'frozenQuantity', 'safetyStock'].includes(column.dataIndex as string)">
        {{ formatQty(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'unitPrice'">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportFetchResult, ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { stockReportApi } from '@/api/analytics'
import type { StockAlertItem } from '@/api/analytics'
import { replenishmentApi } from '@/api/purchase'

const router = useRouter()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '当前库存', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100, align: 'right' },
  { title: '冻结数量', dataIndex: 'frozenQuantity', key: 'frozenQuantity', width: 100, align: 'right' },
  { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 100, align: 'right' },
  { title: '补货缺口', key: 'shortageQty', width: 100, align: 'right' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '成本价', dataIndex: 'unitPrice', key: 'unitPrice', width: 110, align: 'right' },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140, ellipsis: true }
]

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（/erp/stock/alert 实时预警，缺货口径 = 当前库存 <= 0，前端过滤） ═══
async function fetcher(params: Record<string, any>) {
  const list = await stockReportApi.alertList()
  const keyword = String(params.keyword || '').trim().toLowerCase()
  let rows = (Array.isArray(list) ? list : []).filter(r => Number(r.quantity) <= 0)
  if (keyword) {
    rows = rows.filter(r =>
      String(r.productCode || '').toLowerCase().includes(keyword) ||
      String(r.productName || '').toLowerCase().includes(keyword)
    )
  }
  return rows
}

// ═══ 统计卡片 ═══
const shortageRows = ref<StockAlertItem[]>([])

function handleLoaded(result: ReportFetchResult) {
  shortageRows.value = (result.list || []) as StockAlertItem[]
}

const statCards = computed<StatCardItem[]>(() => {
  const rows = shortageRows.value
  const totalShortage = rows.reduce(
    (acc, r) => acc + Math.max((Number(r.safetyStock) || 0) - (Number(r.quantity) || 0), 0), 0)
  return [
    { label: '缺货商品数', value: rows.length, suffix: '个', valueStyle: { color: '#f5222d' } },
    { label: '补货缺口合计', value: totalShortage, precision: 0, suffix: '件' },
    {
      label: '涉及仓库数',
      value: new Set(rows.map(r => r.warehouseId)).size,
      suffix: '个'
    }
  ]
})

// ═══ 一键生成补货建议 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const generating = ref(false)

// ═══ 批量选择 ═══
const selectedRows = ref<any[]>([])

function handleSelectionChange(_keys: (string | number)[], rows: any[]) {
  selectedRows.value = rows
}

async function handleGenerate() {
  generating.value = true
  try {
    const list = await replenishmentApi.generate()
    message.success(`已生成 ${Array.isArray(list) ? list.length : 0} 条补货建议，请前往「智能补货」页处理`)
  } catch (e) {
    console.warn('[缺货补货] 生成补货建议失败', e)
  } finally {
    generating.value = false
  }
}

// ═══ 批量生成采购订单：先生成补货建议，再跳转至智能补货页处理 ═══
async function handleBatchCreateOrder() {
  generating.value = true
  try {
    const list = await replenishmentApi.generate()
    const count = Array.isArray(list) ? list.length : 0
    message.success(`已生成 ${count} 条补货建议，即将跳转至「智能补货」页处理`)
    router.push('/purchase/smart-replenish')
  } catch (e) {
    console.warn('[缺货补货] 生成补货建议失败', e)
    message.error('生成补货建议失败')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.qty-zero {
  color: #f5222d;
  font-weight: 600;
}
</style>
