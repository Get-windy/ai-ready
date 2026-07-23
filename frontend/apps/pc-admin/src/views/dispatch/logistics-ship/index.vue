<template>
  <ARReportPage
    ref="reportRef"
    title="物流发货"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="物流发货"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ record.statusDesc || STATUS_MAP[record.status]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'totalAmount'">
        {{ formatMoney(record.totalAmount) }}
      </template>
      <template v-else-if="column.dataIndex === 'totalQuantity'">
        {{ record.totalQuantity != null ? Number(record.totalQuantity).toLocaleString('zh-CN') : '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button
          v-if="record.status === 9"
          type="link"
          size="small"
          @click="openShipModal(record as any)"
        >
          发货
        </a-button>
        <span v-else-if="record.status === 10" class="shipped-text">已发货</span>
      </template>
    </template>
  </ARReportPage>

  <!-- ═══ 发货登记弹窗 ═══ -->
  <a-modal
    v-model:open="shipModalVisible"
    title="发货登记"
    width="480px"
    :confirm-loading="shipSubmitting"
    @ok="handleShipConfirm"
  >
    <div class="ship-order-info">
      出库单号：{{ currentRow?.outboundNo }}（客户：{{ currentRow?.customerName }}）
    </div>
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-item label="物流公司">
        <a-input
          v-model:value="shipForm.logisticsCompany"
          placeholder="如：顺丰速运"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="物流单号">
        <a-input
          v-model:value="shipForm.trackingNumber"
          placeholder="快递单号 / 运单号"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { outboundApi, type SaleOutbound } from '@/api/erp'

// ═══ 出库单状态（与后端 OutboundStatus 枚举一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '待拣货', color: 'orange' },
  4: { label: '拣货中', color: 'processing' },
  5: { label: '已拣货', color: 'cyan' },
  6: { label: '待打包', color: 'orange' },
  7: { label: '打包中', color: 'processing' },
  8: { label: '已打包', color: 'orange' },
  9: { label: '待发货', color: 'geekblue' },
  10: { label: '已发货', color: 'blue' },
  11: { label: '已完成', color: 'green' },
  12: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'outboundNo', type: 'input', label: '出库单号', placeholder: '出库单号', width: 160 },
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '客户/单号/收货人', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'outboundDateRange', type: 'date-range', label: '出库日期', startKey: 'dateStart', endKey: 'dateEnd' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140, ellipsis: true },
  { title: '收货人', dataIndex: 'receiverName', key: 'receiverName', width: 100 },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '物流公司', dataIndex: 'logisticsCompany', key: 'logisticsCompany', width: 120, ellipsis: true },
  { title: '物流单号', dataIndex: 'trackingNumber', key: 'trackingNumber', width: 140, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 110 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

const reportRef = ref<any>(null)

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（销售出库单） ═══
function fetcher(params: Record<string, any>) {
  return outboundApi.page(params)
}

// ═══ 发货登记 ═══
const shipModalVisible = ref(false)
const shipSubmitting = ref(false)
const currentRow = ref<SaleOutbound | null>(null)
const shipForm = reactive({ logisticsCompany: '', trackingNumber: '' })

function openShipModal(record: SaleOutbound) {
  currentRow.value = record
  shipForm.logisticsCompany = record.logisticsCompany || ''
  shipForm.trackingNumber = record.trackingNumber || ''
  shipModalVisible.value = true
}

async function handleShipConfirm() {
  if (!currentRow.value) return
  shipSubmitting.value = true
  try {
    await outboundApi.ship(currentRow.value.id, {
      logisticsCompany: shipForm.logisticsCompany || undefined,
      trackingNumber: shipForm.trackingNumber || undefined
    })
    message.success(`出库单 ${currentRow.value.outboundNo} 已发货`)
    shipModalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[物流发货] 发货失败', e)
  } finally {
    shipSubmitting.value = false
  }
}
</script>

<style scoped>
.ship-order-info {
  margin-bottom: 16px;
  color: #595959;
  font-size: 13px;
}

.shipped-text {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
