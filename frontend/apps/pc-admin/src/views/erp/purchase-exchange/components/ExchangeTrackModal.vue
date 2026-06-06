<template>
  <a-modal
    v-model:open="open"
    title="换货单跟踪"
    width="800px"
    :footer="null"
  >
    <div v-if="record" class="track-content">
      <!-- 基本信息 -->
      <a-descriptions :column="3" bordered>
        <a-descriptions-item label="换货单号">{{ record.exchangeNo }}</a-descriptions-item>
        <a-descriptions-item label="原采购订单">{{ record.originalOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ record.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="换货金额">¥{{ record.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <!-- 流程跟踪时间线 -->
      <div class="timeline-section">
        <h4>流程跟踪</h4>
        <a-timeline v-if="timeline.length > 0">
          <a-timeline-item
            v-for="(item, index) in timeline"
            :key="index"
            :color="item.status"
          >
            <div class="timeline-item">
              <div class="timeline-title">{{ item.title }}</div>
              <div class="timeline-time">{{ item.time }}</div>
              <div class="timeline-content">{{ item.content }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无跟踪记录" />
      </div>

      <a-divider />

      <!-- 换货明细 -->
      <div class="items-section">
        <h4>换货明细</h4>
        <a-table
          :columns="itemColumns"
          :data-source="items"
          :loading="loading"
          :pagination="false"
          row-key="id"
          size="small"
        />
      </div>

      <a-divider />

      <!-- 审批记录 -->
      <div class="approval-section">
        <h4>审批记录</h4>
        <a-table
          :columns="approvalColumns"
          :data-source="approvalRecords"
          :loading="loading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-tag :color="getActionColor(record.action)">{{ record.actionName }}</a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  purchaseExchangeApi,
  type PurchaseExchange,
  type PurchaseExchangeItem,
  type ExchangeApprovalRecord,
  ExchangeStatus
} from '@/api/purchase-exchange'

interface TimelineItem {
  time: string
  title: string
  content: string
  status: 'success' | 'processing' | 'pending' | 'error'
}

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
const timeline = ref<TimelineItem[]>([])

const open = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const itemColumns = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '换货数量', dataIndex: 'exchangeQuantity', key: 'exchangeQuantity', width: 100 },
  { title: '换货单价', dataIndex: 'exchangePrice', key: 'exchangePrice', width: 100 },
  {
    title: '小计',
    key: 'subtotal',
    width: 100,
    customRender: ({ record }: { record: PurchaseExchangeItem }) => {
      return `¥${(record.exchangeQuantity * record.exchangePrice).toFixed(2)}`
    }
  }
]

const approvalColumns = [
  { title: '操作', key: 'action', width: 100 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
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

const getActionColor = (action: string): string => {
  const colors: Record<string, string> = {
    submit: 'blue',
    approve: 'green',
    reject: 'red',
    cancel: 'gray',
    complete: 'success'
  }
  return colors[action] || 'default'
}

const generateTimeline = (record: PurchaseExchange): TimelineItem[] => {
  const timeline: TimelineItem[] = []

  // 创建
  timeline.push({
    time: record.createTime,
    title: '换货单创建',
    content: `由 ${record.createdByName} 创建换货单`,
    status: 'success'
  })

  // 提交审批
  if (record.status >= ExchangeStatus.PENDING_APPROVAL) {
    timeline.push({
      time: record.createTime, // 实际应该使用提交时间
      title: '提交审批',
      content: '换货单已提交审批',
      status: 'success'
    })
  }

  // 审批
  if (record.status === ExchangeStatus.APPROVED ||
      record.status === ExchangeStatus.EXCHANGING ||
      record.status === ExchangeStatus.COMPLETED) {
    timeline.push({
      time: record.approvedTime || '',
      title: '审批通过',
      content: `由 ${record.approvedByName || '审批人'} 审批通过`,
      status: 'success'
    })
  }

  if (record.status === ExchangeStatus.REJECTED) {
    timeline.push({
      time: record.approvedTime || '',
      title: '审批拒绝',
      content: `由 ${record.approvedByName || '审批人'} 审批拒绝`,
      status: 'error'
    })
  }

  // 换货中
  if (record.status === ExchangeStatus.EXCHANGING) {
    timeline.push({
      time: record.updateTime || '',
      title: '换货处理中',
      content: '换货商品正在处理中',
      status: 'processing'
    })
  }

  // 完成
  if (record.status === ExchangeStatus.COMPLETED) {
    timeline.push({
      time: record.completedTime || '',
      title: '换货完成',
      content: '换货流程已完成',
      status: 'success'
    })
  }

  // 取消
  if (record.status === ExchangeStatus.CANCELLED) {
    timeline.push({
      time: record.updateTime || '',
      title: '换货取消',
      content: '换货单已取消',
      status: 'error'
    })
  }

  return timeline
}

const loadTrackData = async (exchangeId: number) => {
  loading.value = true
  try {
    const res = await purchaseExchangeApi.getTracking(exchangeId)
    if (res.data) {
      items.value = res.data.items
      approvalRecords.value = res.data.approvalRecords
      timeline.value = res.data.timeline
    }
  } catch (error) {
    // 如果API不存在，使用本地生成的时间线
    if (props.record) {
      timeline.value = generateTimeline(props.record)
      try {
        // 加载明细
        const itemsRes = await purchaseExchangeApi.getItems(exchangeId)
        items.value = itemsRes.data || []
        // 加载审批记录
        const recordsRes = await purchaseExchangeApi.getApprovalRecords(exchangeId)
        approvalRecords.value = recordsRes.data || []
      } catch (innerError) {
        // 忽略明细加载错误
      }
    }
    message.error('加载跟踪数据失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.record, (record) => {
  if (record && open.value) {
    loadTrackData(record.id)
  } else {
    items.value = []
    approvalRecords.value = []
    timeline.value = []
  }
}, { immediate: true })
</script>

<style scoped>
.track-content {
  max-height: 600px;
  overflow-y: auto;
}

.timeline-section,
.items-section,
.approval-section {
  margin-bottom: 24px;
}

.timeline-item {
  margin-bottom: 8px;
}

.timeline-title {
  font-weight: bold;
  font-size: 14px;
}

.timeline-time {
  color: #999;
  font-size: 12px;
  margin: 4px 0;
}

.timeline-content {
  color: #666;
  font-size: 13px;
}
</style>