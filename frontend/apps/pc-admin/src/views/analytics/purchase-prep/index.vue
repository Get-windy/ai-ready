<template>
  <ARReportPage
    title="采购准备"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="采购准备"
    row-key="id"
    empty-text="暂无库存预警商品"
  >
    <template #header-extra>
      <span class="prep-tip">口径：现存量 ≤ 安全库存即预警，缺口 = 安全库存 − 现存量</span>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'unitPrice'">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.key === 'gapQty'">
        <span :style="{ color: '#f5222d', fontWeight: 600 }">
          {{ gapQuantity(record).toLocaleString('zh-CN') }}
        </span>
      </template>
      <template v-else-if="column.key === 'suggestAmount'">
        {{ formatMoney(suggestAmount(record)) }}
      </template>
      <template v-else-if="column.dataIndex === 'quantity'">
        <a-tag :color="Number(text) <= 0 ? 'red' : 'orange'">
          {{ Number(text) <= 0 ? '缺货' : '预警' }}
        </a-tag>
        {{ Number(text ?? 0).toLocaleString('zh-CN') }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { stockReportApi, stockApi, type PurchasePrepAnalysis, type StockAlertItem } from '@/api/analytics'

// ═══ 查询字段（预警接口无服务端过滤，前端筛选） ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const queryFields = computed<ReportQueryField[]>(() => [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称' },
  {
    key: 'warehouseId',
    type: 'select',
    label: '仓库',
    placeholder: '全部仓库',
    options: warehouseOptions.value
  }
])

// ═══ 表格列（库存预警商品明细） ═══
const columns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 110 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '现存量', dataIndex: 'quantity', key: 'quantity', width: 130, align: 'right' },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 90, align: 'right' },
  { title: '冻结数量', dataIndex: 'frozenQuantity', key: 'frozenQuantity', width: 90, align: 'right' },
  { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 90, align: 'right' },
  { title: '缺口数量', key: 'gapQty', width: 90, align: 'right' },
  { title: '单位成本', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '建议补货金额', key: 'suggestAmount', width: 120, align: 'right' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 缺口数量 = 安全库存 - 现存量（负库存时叠加） */
function gapQuantity(record: any): number {
  return Math.max(0, (Number(record.safetyStock) || 0) - (Number(record.quantity) || 0))
}

function suggestAmount(record: any): number {
  return gapQuantity(record) * (Number(record.unitPrice) || 0)
}

// ═══ 采购准备汇总卡片（独立请求 /prep-analysis，口径：全部仓库） ═══
const prep = ref<PurchasePrepAnalysis | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const p = prep.value
  return [
    { label: '预警商品数', value: Number(p?.alertProductCount) || 0, suffix: '个', valueStyle: { color: '#fa8c16' } },
    { label: '缺货SKU数', value: Number(p?.outOfStockSkuCount) || 0, suffix: '个', valueStyle: { color: '#f5222d' } },
    { label: '在途采购单数', value: Number(p?.inTransitOrderCount) || 0, suffix: '单' },
    { label: '在途采购数量', value: Number(p?.inTransitQuantity) || 0 },
    { label: '在途采购金额', value: Number(p?.inTransitAmount) || 0, precision: 2, prefix: '¥' },
    { label: '建议补货金额', value: Number(p?.suggestReplenishAmount) || 0, precision: 2, prefix: '¥' }
  ]
})

// ═══ 数据请求（预警商品列表 + 前端筛选） ═══
async function fetcher(params: Record<string, any>): Promise<StockAlertItem[]> {
  const list = await stockReportApi.alertList()
  const keyword = String(params.keyword || '').trim().toLowerCase()
  return (Array.isArray(list) ? list : []).filter(item => {
    if (params.warehouseId !== undefined && item.warehouseId !== Number(params.warehouseId)) return false
    if (keyword) {
      const text = `${item.productCode || ''} ${item.productName || ''}`.toLowerCase()
      if (!text.includes(keyword)) return false
    }
    return true
  })
}

function normalizeResponse(res: StockAlertItem[]): ReportFetchResult {
  const list = Array.isArray(res) ? res : []
  return { list, total: list.length, raw: res }
}

onMounted(async () => {
  try {
    prep.value = await stockReportApi.prepAnalysis()
  } catch (e) {
    console.warn('[采购准备] 汇总获取失败', e)
  }
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id
    }))
  } catch (e) {
    console.warn('[采购准备] 仓库列表获取失败', e)
  }
})
</script>

<style scoped>
.prep-tip {
  font-size: 12px;
  color: #999;
}
</style>
