<template>
  <a-drawer
    :open="visible"
    :title="drawerTitle"
    placement="right"
    width="640"
    @close="emit('update:open', false)"
  >
    <!-- 加载中 -->
    <div v-if="loading" style="text-align: center; padding: 60px 0">
      <a-spin />
    </div>

    <template v-else>
      <div style="text-align: right; margin-bottom: 12px;">
        <PrintButton :business-id="Number(props.serialId)" business-type="SERIAL" button-size="small" />
      </div>
      <!-- 基本信息 -->
      <a-card title="基本信息" class="detail-card" :bordered="false">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="序列号" :span="2">
            <a-typography-text strong>{{ detail.serialNo }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="产品编码">{{ detail.productCode }}</a-descriptions-item>
          <a-descriptions-item label="产品名称">{{ detail.productName }}</a-descriptions-item>
          <a-descriptions-item label="规格型号">{{ detail.specification || '-' }}</a-descriptions-item>
          <a-descriptions-item label="制造商">{{ detail.manufacturer || '-' }}</a-descriptions-item>
          <a-descriptions-item label="批次号">{{ detail.batchNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="当前状态">
            <a-tag :color="statusColor(detail.snStatus)">{{ statusLabel(detail.snStatus) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="当前阶段">
            <a-tag :color="stageColor(detail.snStage)">{{ stageLabel(detail.snStage) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="所在仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生产日期">{{ detail.manufacturingDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 质保信息 -->
      <a-card title="质保信息" class="detail-card" :bordered="false">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="质保期限">
            {{ detail.warrantyPeriod ? detail.warrantyPeriod + ' 个月' : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="质保起始日">{{ detail.warrantyStartDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="质保到期日" :span="2">
            <span v-if="detail.warrantyEndDate" :style="warrantyWarningStyle(detail.warrantyEndDate)">
              {{ detail.warrantyEndDate }}
              <a-tag v-if="isExpiringSoon(detail.warrantyEndDate)" color="red" size="small" style="margin-left: 8px">
                即将到期
              </a-tag>
              <a-tag v-else-if="isExpired(detail.warrantyEndDate)" color="gray" size="small" style="margin-left: 8px">
                已过期
              </a-tag>
            </span>
            <span v-else>-</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 关联订单 -->
      <a-card title="关联信息" class="detail-card" :bordered="false">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="销售单号">{{ detail.saleOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户名称">{{ detail.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户编号">{{ detail.customerId || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 全生命周期时间线 -->
      <a-card title="生命周期追溯" class="detail-card" :bordered="false">
        <div v-if="historyList.length === 0" style="text-align: center; padding: 24px 0">
          <a-empty description="暂无历史记录" />
        </div>
        <a-timeline v-else>
          <a-timeline-item
            v-for="item in historyList"
            :key="item.key"
            :color="item.color"
          >
            <template #dot>
              <component :is="item.icon" v-if="item.icon" />
            </template>
            <div class="timeline-item">
              <div class="timeline-item__header">
                <a-tag :color="item.tagColor" size="small">{{ item.title }}</a-tag>
                <span class="timeline-item__time">{{ item.time }}</span>
              </div>
              <div v-if="item.description" class="timeline-item__desc">
                {{ item.description }}
              </div>
              <div v-if="item.extra" class="timeline-item__extra">
                {{ item.extra }}
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-card>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  DownloadOutlined,
  UploadOutlined,
  RetweetOutlined,
  ToolOutlined,
  ExclamationCircleOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { serialApi, type SerialNumber } from '@/api/erp/batch'

const props = defineProps<{
  visible: boolean
  serialId: number
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const loading = ref(false)
const detail = ref<SerialNumber>({} as SerialNumber)
const history = ref<SerialNumber[]>([])

// ── 抽屉标题 ──
const drawerTitle = computed(() => {
  return detail.value.serialNo ? `序列号详情 - ${detail.value.serialNo}` : '序列号详情'
})

// ── 状态/阶段标签映射（与列表页保持一致） ──

const statusMap: Record<string, string> = {
  AVAILABLE: '可用',
  IN_USE: '使用中',
  INSERVICE: '售后中',
  MAINTAINED: '维修中',
  SCRAP: '报废'
}

const statusColors: Record<string, string> = {
  AVAILABLE: 'blue',
  IN_USE: 'green',
  INSERVICE: 'orange',
  MAINTAINED: 'red',
  SCRAP: 'gray'
}

const stageMap: Record<string, string> = {
  WAREHOUSE: '在库',
  IN_TRANSIT: '在途',
  EOF_CUSTOMER: '终端客户',
  IN_SERVICE: '使用中',
  SCRAPPED: '已报废'
}

const stageColors: Record<string, string> = {
  WAREHOUSE: 'blue',
  IN_TRANSIT: 'gold',
  EOF_CUSTOMER: 'green',
  IN_SERVICE: 'orange',
  SCRAPPED: 'gray'
}

function statusLabel(s: string): string {
  return statusMap[s] || s
}

function statusColor(s: string): string {
  return statusColors[s] || 'default'
}

function stageLabel(s: string): string {
  return stageMap[s] || s
}

function stageColor(s: string): string {
  return stageColors[s] || 'default'
}

// ── 质保预警 ──

function warrantyWarningStyle(dateStr: string): Record<string, string> | undefined {
  if (!dateStr) return undefined
  const now = new Date()
  const end = new Date(dateStr)
  const diffDays = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays <= 0) {
    return { color: '#999', fontWeight: 'bold' }
  }
  if (diffDays <= 30) {
    return { color: '#f5222d', fontWeight: 'bold' }
  }
  return undefined
}

function isExpiringSoon(dateStr: string): boolean {
  if (!dateStr) return false
  const now = new Date()
  const end = new Date(dateStr)
  const diffDays = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays > 0 && diffDays <= 30
}

function isExpired(dateStr: string): boolean {
  if (!dateStr) return false
  return new Date(dateStr) <= new Date()
}

// ── 时间线事件类型推断 ──

interface TimelineItem {
  key: string
  title: string
  time: string
  description: string
  extra: string
  color: string
  tagColor: string
  icon: object | null
}

function buildTimeline(records: SerialNumber[]): TimelineItem[] {
  if (!records || records.length === 0) return []

  // 按时间排序（正序：最早的在最下面，最新的在最上面）
  const sorted = [...records].sort((a, b) => {
    const ta = a.createdAt || ''
    const tb = b.createdAt || ''
    return ta.localeCompare(tb)
  })

  const items: TimelineItem[] = []
  let prevStatus = ''
  let prevStage = ''

  for (let i = 0; i < sorted.length; i++) {
    const rec = sorted[i]
    const event = inferEvent(rec, prevStatus, prevStage, i)
    items.push(event)
    prevStatus = rec.snStatus
    prevStage = rec.snStage
  }

  // 时间倒序展示（最新的在最上面）
  return items.reverse()
}

function inferEvent(
  rec: SerialNumber,
  prevStatus: string,
  prevStage: string,
  index: number
): TimelineItem {
  const status = rec.snStatus
  const stage = rec.snStage
  const time = rec.createdAt || ''
  const key = `${index}-${status}-${stage}-${time}`

  // 第一条记录 -> 创建
  if (index === 0) {
    return {
      key,
      title: '序列号创建',
      time,
      description: `序列号 ${rec.serialNo} 被创建`,
      extra: stageLabel(stage),
      color: 'blue',
      tagColor: 'blue',
      icon: CheckCircleOutlined
    }
  }

  // 根据状态变化判断事件类型
  if (status === 'AVAILABLE' && prevStatus !== 'AVAILABLE') {
    return {
      key,
      title: '入库/可用',
      time,
      description: `序列号变为可用状态，入库至 ${rec.warehouseName || '未知仓库'}`,
      extra: stageLabel(stage),
      color: 'green',
      tagColor: 'green',
      icon: DownloadOutlined
    }
  }

  if (status === 'IN_USE' && prevStatus !== 'IN_USE') {
    return {
      key,
      title: '出库/使用中',
      time,
      description: rec.saleOrderNo
        ? `序列号出库，关联销售单 ${rec.saleOrderNo}`
        : '序列号出库',
      extra: rec.customerName ? `客户: ${rec.customerName}` : stageLabel(stage),
      color: 'green',
      tagColor: 'green',
      icon: UploadOutlined
    }
  }

  if (status === 'INSERVICE') {
    return {
      key,
      title: '售后中',
      time,
      description: '序列号进入售后流程',
      extra: stageLabel(stage),
      color: 'orange',
      tagColor: 'orange',
      icon: ToolOutlined
    }
  }

  if (status === 'MAINTAINED') {
    return {
      key,
      title: '维修中',
      time,
      description: '序列号正在维修',
      extra: stageLabel(stage),
      color: 'red',
      tagColor: 'red',
      icon: ExclamationCircleOutlined
    }
  }

  if (status === 'SCRAP' || stage === 'SCRAPPED') {
    return {
      key,
      title: '报废',
      time,
      description: '序列号已报废处理',
      extra: stageLabel(stage),
      color: 'gray',
      tagColor: 'gray',
      icon: StopOutlined
    }
  }

  // 阶段变化但没有状态变化
  if (stage !== prevStage && stage !== '') {
    return {
      key,
      title: '阶段变更',
      time,
      description: `阶段变更为 ${stageLabel(stage)}`,
      extra: statusLabel(status),
      color: 'blue',
      tagColor: 'blue',
      icon: RetweetOutlined
    }
  }

  // 兜底
  return {
    key,
    title: '状态更新',
    time,
    description: `状态: ${statusLabel(status)}，阶段: ${stageLabel(stage)}`,
    extra: '',
    color: 'blue',
    tagColor: 'blue',
    icon: null
  }
}

// ── 加载数据 ──

const historyList = computed(() => buildTimeline(history.value))

async function loadData() {
  if (!props.serialId) return
  loading.value = true
  try {
    const [detailData, historyData] = await Promise.all([
      serialApi.getById(props.serialId),
      serialApi.getFullHistory(props.serialId)
    ])
    detail.value = detailData
    history.value = historyData || []
  } catch {
    message.error('加载序列号详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.serialId],
  ([visible, serialId]) => {
    if (visible && serialId) {
      loadData()
    } else {
      // 抽屉关闭时清空数据，避免下次打开闪烁旧数据
      detail.value = {} as SerialNumber
      history.value = []
    }
  }
)
</script>

<style scoped>
.detail-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.detail-card:deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 600;
}

.detail-card:deep(.ant-card-body) {
  padding: 16px;
}

.timeline-item {
  line-height: 1.6;
}

.timeline-item__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-item__time {
  font-size: 12px;
  color: #999;
}

.timeline-item__desc {
  font-size: 13px;
  color: #606266;
}

.timeline-item__extra {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
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
