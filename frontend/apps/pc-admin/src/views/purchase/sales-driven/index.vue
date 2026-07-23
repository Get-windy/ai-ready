<template>
  <ARReportPage
    ref="reportRef"
    title="以销定购"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :enable-row-selection="true"
    page-param-style="pageNum"
    export-file-name="以销定购"
    row-key="id"
    empty-text="暂无销量驱动的补货建议，可点击右上角「生成补货建议」扫描库存生成"
    @selection-change="handleSelectionChange"
  >
    <template #header-extra>
      <a-space :size="8">
        <a-button
          type="primary"
          :disabled="selectedRows.length === 0"
          :loading="batchCreating"
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
      <template v-if="column.dataIndex === 'priority'">
        <a-tag :color="PRIORITY_MAP[text as ReplenishPriority]?.color">
          {{ PRIORITY_MAP[text as ReplenishPriority]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text as ReplenishStatus]?.color">
          {{ STATUS_MAP[text as ReplenishStatus]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="QTY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatQty(text) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-popconfirm
          v-if="record.status === 'PENDING'"
          title="按建议数量创建采购订单？"
          ok-text="创建"
          cancel-text="取消"
          @confirm="handleCreateOrder(record as StockReplenishmentItem)"
        >
          <a-button
            type="link"
            size="small"
            :loading="actingId === record.id"
          >转采购订单</a-button>
        </a-popconfirm>
        <a-tag
          v-else-if="record.status === 'ORDERED' && record.createdOrderNo"
          color="green"
        >{{ record.createdOrderNo }}</a-tag>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ThunderboltOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { replenishmentApi } from '@/api/purchase'
import type { ReplenishPriority, ReplenishStatus, StockReplenishmentItem } from '@/api/purchase'

// ═══ 优先级/状态映射（与后端 StockReplenishmentServiceImpl 一致） ═══
const PRIORITY_MAP: Record<ReplenishPriority, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  MEDIUM: { label: '中', color: 'orange' },
  LOW: { label: '低', color: 'blue' }
}

const STATUS_MAP: Record<ReplenishStatus, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'orange' },
  ORDERED: { label: '已转采购订单', color: 'green' },
  IGNORED: { label: '已忽略', color: 'default' }
}

const QTY_COLUMNS = ['avgDailySales', 'daysOfStock', 'currentQty', 'safetyStock', 'suggestedQty']

// ═══ 查询字段（以销定购 = 销量驱动补货建议，仅列待处理+已转单） ═══
const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称' },
  {
    key: 'priority',
    type: 'select',
    label: '优先级',
    placeholder: '全部优先级',
    options: [
      { label: '高', value: 'HIGH' },
      { label: '中', value: 'MEDIUM' },
      { label: '低', value: 'LOW' }
    ]
  },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '待处理', value: 'PENDING' },
      { label: '已转采购订单', value: 'ORDERED' },
      { label: '已忽略', value: 'IGNORED' }
    ]
  }
]

// ═══ 表格列（突出销量驱动口径：日均销量/可售天数/采购提前期 → 建议采购量） ═══
const columns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 160, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '日均销量', dataIndex: 'avgDailySales', key: 'avgDailySales', width: 100, align: 'right' },
  { title: '可售天数', dataIndex: 'daysOfStock', key: 'daysOfStock', width: 90, align: 'right' },
  { title: '采购提前期(天)', dataIndex: 'leadTime', key: 'leadTime', width: 110, align: 'right' },
  { title: '当前库存', dataIndex: 'currentQty', key: 'currentQty', width: 90, align: 'right' },
  { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 90, align: 'right' },
  { title: '建议采购量', dataIndex: 'suggestedQty', key: 'suggestedQty', width: 100, align: 'right' },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 130, ellipsis: true },
  { title: '生成时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const generating = ref(false)
const actingId = ref<number | null>(null)

// ═══ 批量选择 ═══
const selectedRows = ref<any[]>([])
const batchCreating = ref(false)

function handleSelectionChange(_keys: (string | number)[], rows: any[]) {
  selectedRows.value = rows
}

function reload() {
  (reportRef.value as any)?.reload?.()
}

function clearSelection() {
  (reportRef.value as any)?.clearSelection?.()
  selectedRows.value = []
}

function fetcher(params: Record<string, any>) {
  return replenishmentApi.page(params)
}

async function handleGenerate() {
  generating.value = true
  try {
    const list = await replenishmentApi.generate()
    message.success(`已生成 ${Array.isArray(list) ? list.length : 0} 条补货建议`)
    reload()
  } catch (e) {
    console.warn('[以销定购] 生成补货建议失败', e)
  } finally {
    generating.value = false
  }
}

async function handleCreateOrder(record: StockReplenishmentItem) {
  actingId.value = record.id
  try {
    const res = await replenishmentApi.createOrder(record.id)
    message.success(res?.createdOrderNo ? `已创建采购订单 ${res.createdOrderNo}` : '已创建采购订单')
    reload()
  } catch (e) {
    console.warn('[以销定购] 转采购订单失败', e)
  } finally {
    actingId.value = null
  }
}

// ═══ 批量生成采购订单：勾选建议行 → 按供应商分组 → 生成采购订单草稿 ═══
async function handleBatchCreateOrder() {
  const pending = selectedRows.value.filter((r: any) => r.status === 'PENDING')
  if (pending.length === 0) {
    message.warning('请勾选状态为"待处理"的补货建议行')
    return
  }

  // 按供应商分组
  const grouped = new Map<string, any[]>()
  for (const row of pending) {
    const key = row.supplierName || '未指定供应商'
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key)!.push(row)
  }

  const summary = Array.from(grouped.entries()).map(([supplier, items]) =>
    `${supplier}(${items.length}条)`
  ).join('，')

  Modal.confirm({
    title: '确认批量生成采购订单',
    content: `将按供应商分组生成采购订单草稿：${summary}，确认继续？`,
    okText: '确认生成',
    cancelText: '取消',
    onOk: async () => {
      batchCreating.value = true
      const results: { supplier: string; success: number; fail: number; orderNos: string[] }[] = []

      try {
        for (const [supplier, items] of grouped.entries()) {
          let success = 0
          let fail = 0
          const orderNos: string[] = []
          for (const item of items) {
            try {
              const res = await replenishmentApi.createOrder(item.id, item.supplierId)
              success++
              if (res?.createdOrderNo) orderNos.push(res.createdOrderNo)
            } catch {
              fail++
            }
          }
          results.push({ supplier, success, fail, orderNos })
        }

        const totalSuccess = results.reduce((s, r) => s + r.success, 0)
        const totalFail = results.reduce((s, r) => s + r.fail, 0)
        const detail = results
          .filter(r => r.success > 0)
          .map(r => `${r.supplier}: 成功${r.success}条${r.orderNos.length > 0 ? '(' + r.orderNos.join(',') + ')' : ''}`)
          .join('；')

        if (totalFail > 0) {
          message.warning(`已生成 ${totalSuccess} 张采购订单，${totalFail} 条失败。${detail}`)
        } else {
          message.success(`已成功生成 ${totalSuccess} 张采购订单。${detail}`)
        }

        clearSelection()
        reload()
      } catch (e) {
        console.warn('[以销定购] 批量生成采购订单失败', e)
        message.error('批量生成采购订单失败')
      } finally {
        batchCreating.value = false
      }
    }
  })
}
</script>
