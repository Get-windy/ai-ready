<template>
  <a-drawer
    v-model:open="open"
    title="换货单跟踪"
    placement="right"
    width="80vw"
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
        <VxeTableList
          :columns="itemVxeColumns"
          :data-source="items"
          :loading="loading"
          :pagination="false as any"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        />
      </div>

      <a-divider />

      <!-- 审批记录 -->
      <div class="approval-section">
        <h4>审批记录</h4>
        <VxeTableList
          :columns="approvalVxeColumns"
          :data-source="approvalRecords"
          :loading="loading"
          :pagination="false as any"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #action="{ record }">
            <a-tag :color="getActionColor(record.action)">{{ record.actionName }}</a-tag>
          </template>
        </VxeTableList>
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}
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

const itemVxeColumns = [
  { field: 'productName', title: '商品名称' },
  { field: 'productCode', title: '商品编码' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'exchangeQuantity', title: '换货数量', width: 100 },
  { field: 'exchangePrice', title: '换货单价', width: 100 },
  { field: 'subtotal', title: '小计', width: 100, formatter: ({ record }: any) => `¥${((record.exchangeQuantity || 0) * (record.exchangePrice || 0)).toFixed(2)}` }
]

const approvalVxeColumns = [
  { field: 'action', title: '操作', width: 100, type: 'action' },
  { field: 'operatorName', title: '操作人', width: 100 },
  { field: 'createTime', title: '时间', width: 180 },
  { field: 'remark', title: '备注' }
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
      items.value = res.items
      approvalRecords.value = res.approvalRecords
      timeline.value = res.timeline
    }
  } catch (error) {
    console.warn('[采购换货] 跟踪API不可用，使用本地数据', error)
    if (props.record) {
      timeline.value = generateTimeline(props.record)
    }
    try {
      const itemsRes = await purchaseExchangeApi.getItems(exchangeId)
      items.value = itemsRes.data || []
      const recordsRes = await purchaseExchangeApi.getApprovalRecords(exchangeId)
      approvalRecords.value = recordsRes.data || []
    } catch (innerError: any) {
      console.warn('[采购换货] 加载明细或审批记录失败', innerError)
    }
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>