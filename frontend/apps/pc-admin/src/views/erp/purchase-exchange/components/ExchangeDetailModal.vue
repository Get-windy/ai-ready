<template>
  <a-modal
    v-model:open="open"
    title="换货单详情"
    width="900px"
    :footer="null"
  >
    <div v-if="record" class="detail-content">
      <!-- 基本信息 -->
      <a-descriptions title="基本信息" :column="3" bordered>
        <a-descriptions-item label="换货单号">{{ record.exchangeNo }}</a-descriptions-item>
        <a-descriptions-item label="原采购订单">{{ record.originalOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ record.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="换货日期">{{ record.exchangeDate }}</a-descriptions-item>
        <a-descriptions-item label="换货类型">{{ getExchangeTypeText(record.exchangeType) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="换货金额">¥{{ record.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ record.createdByName }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ record.createTime }}</a-descriptions-item>
        <a-descriptions-item label="换货原因" :span="3">{{ record.exchangeReason }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="3">{{ record.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 换货明细 -->
      <a-divider />
      <h4>换货明细</h4>
      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="items"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #subtotalCell="{ record }">
          ¥{{ (record.exchangeQuantity * record.exchangePrice).toFixed(2) }}
        </template>
      </VxeTableList>

      <!-- 审批记录 -->
      <template v-if="approvalRecords.length > 0">
        <a-divider />
        <h4>审批记录</h4>
        <a-timeline>
          <a-timeline-item
            v-for="record in approvalRecords"
            :key="record.id"
            :color="getTimelineColor(record.action)"
          >
            <p>{{ record.actionName }} - {{ record.operatorName }} {{ record.createTime }}</p>
            <p v-if="record.remark" class="timeline-remark">备注：{{ record.remark }}</p>
          </a-timeline-item>
        </a-timeline>
      </template>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import {
  purchaseExchangeApi,
  type PurchaseExchange,
  type PurchaseExchangeItem,
  type ExchangeApprovalRecord,
  ExchangeStatus
} from '@/api/purchase-exchange'

interface Props {
  open: boolean
  record: PurchaseExchange | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const loading = ref(false)
const items = ref<PurchaseExchangeItem[]>([])
const approvalRecords = ref<ExchangeApprovalRecord[]>([])

const open = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const itemVxeColumns = [
  { field: 'productName', title: '商品名称' },
  { field: 'productCode', title: '商品编码' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'originalQuantity', title: '原数量', width: 100 },
  { field: 'exchangeQuantity', title: '换货数量', width: 100 },
  { field: 'originalPrice', title: '原单价', width: 100 },
  { field: 'exchangePrice', title: '换货单价', width: 100 },
  { field: 'subtotal', title: '小计', width: 100, slotName: 'subtotalCell' }
]

const getStatusColor = (status: ExchangeStatus): string => {
  const colors: Record<ExchangeStatus, string> = {
    [ExchangeStatus.DRAFT]: 'default',
    [ExchangeStatus.PENDING_APPROVAL]: 'orange',
    [ExchangeStatus.APPROVED]: 'blue',
    [ExchangeStatus.EXCHANGING]: 'processing',
    [ExchangeStatus.COMPLETED]: 'success',
    [ExchangeStatus.REJECTED]: 'red',
    [ExchangeStatus.CANCELLED]: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: ExchangeStatus): string => {
  const texts: Record<ExchangeStatus, string> = {
    [ExchangeStatus.DRAFT]: '草稿',
    [ExchangeStatus.PENDING_APPROVAL]: '待审批',
    [ExchangeStatus.APPROVED]: '已审批',
    [ExchangeStatus.EXCHANGING]: '换货中',
    [ExchangeStatus.COMPLETED]: '已完成',
    [ExchangeStatus.REJECTED]: '已拒绝',
    [ExchangeStatus.CANCELLED]: '已取消'
  }
  return texts[status] || '未知'
}

const getExchangeTypeText = (type: number): string => {
  const texts: Record<number, string> = {
    1: '质量问题',
    2: '规格不符',
    3: '数量错误',
    4: '其他'
  }
  return texts[type] || '未知'
}

const getTimelineColor = (action: string): string => {
  const colors: Record<string, string> = {
    submit: 'blue',
    approve: 'green',
    reject: 'red',
    cancel: 'gray'
  }
  return colors[action] || 'blue'
}

const loadDetail = async (exchangeId: number) => {
  loading.value = true
  try {
    // 加载明细
    const itemsRes = await purchaseExchangeApi.getItems(exchangeId)
    items.value = itemsRes.data || []

    // 加载审批记录
    const recordsRes = await purchaseExchangeApi.getApprovalRecords(exchangeId)
    approvalRecords.value = recordsRes.data || []
  } catch (error) {
    message.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.record, (record) => {
  if (record && open.value) {
    loadDetail(record.id)
  } else {
    items.value = []
    approvalRecords.value = []
  }
}, { immediate: true })
</script>

<style scoped>
.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.timeline-remark {
  color: #666;
  font-size: 12px;
}
</style>
