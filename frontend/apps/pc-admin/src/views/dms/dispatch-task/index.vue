<template>
  <ARReportPage
    ref="reportRef"
    title="调度任务"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="调度任务"
    row-key="id"
  >
    <template #header-extra>
      <a-popconfirm
        title="对所有待分配任务执行自动调度？"
        @confirm="handleAutoDispatch"
      >
        <a-button size="small">
          <template #icon>
            <ThunderboltOutlined />
          </template>自动调度
        </a-button>
      </a-popconfirm>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ STATUS_MAP[record.status]?.label || `状态${record.status}` }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'orderType'">
        <a-tag :color="ORDER_TYPE_MAP[record.orderType]?.color || 'default'">
          {{ ORDER_TYPE_MAP[record.orderType]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'priority'">
        <a-tag :color="PRIORITY_MAP[record.priority]?.color || 'default'">
          {{ PRIORITY_MAP[record.priority]?.label || '普通' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'goodsAmount'">
        {{ formatMoney(record.goodsAmount) }}
      </template>
      <template v-else-if="column.dataIndex === 'totalQuantity'">
        {{ formatQty(record.totalQuantity) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space :size="4">
          <a-button
            v-if="record.status === 0"
            type="link"
            size="small"
            @click="openAssignModal(record as any, 'assign')"
          >
            指派
          </a-button>
          <a-button
            v-if="record.status === 1 || record.status === 2"
            type="link"
            size="small"
            @click="openAssignModal(record as any, 'reassign')"
          >
            改派
          </a-button>
          <a-popconfirm
            v-if="record.status === 0 || record.status === 1"
            title="确定取消该任务？"
            @confirm="handleCancel(record as any)"
          >
            <a-button
              type="link"
              size="small"
              danger
            >
              取消
            </a-button>
          </a-popconfirm>
          <a-button
            v-if="record.riderId && [2, 3, 4].includes(record.status)"
            type="link"
            size="small"
            @click="goTracking(record as any)"
          >
            轨迹
          </a-button>
          <a-popconfirm
            v-if="[2, 3, 4, 5].includes(record.status)"
            title="确定将该任务标记为异常？"
            @confirm="handleMarkException(record as any)"
          >
            <a-button
              type="link"
              size="small"
              danger
            >
              异常
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- ═══ 指派/改派骑手弹窗 ═══ -->
  <a-modal
    v-model:open="assignModalVisible"
    :title="assignMode === 'assign' ? '指派骑手' : '改派骑手'"
    width="640px"
    :confirm-loading="assignSubmitting"
    :ok-button-props="{ disabled: !selectedRiderId }"
    @ok="handleAssignConfirm"
  >
    <div class="assign-task-info">
      任务单号：{{ currentTask?.taskNo }}（订单号：{{ currentTask?.orderNo }}）
      <template v-if="assignMode === 'reassign'">
        ，当前骑手ID：{{ currentTask?.riderId ?? '-' }}
      </template>
    </div>
    <a-table
      :columns="candidateColumns"
      :data-source="candidates"
      :loading="candidatesLoading"
      :row-selection="{ type: 'radio', selectedRowKeys: selectedRiderKeys, onChange: handleCandidateSelect }"
      :pagination="false"
      row-key="id"
      size="small"
      :locale="{ emptyText: '暂无候选骑手' }"
      :scroll="{ y: 320 }"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { taskApi, type DmsTask } from '@/api/dms/task'
import { dispatchApi } from '@/api/dms/dispatch'
import type { DmsRider } from '@/api/dms/rider'

const router = useRouter()

// ═══ 任务状态（与后端 TaskStatusEnum 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待分配', color: 'orange' },
  1: { label: '已分配', color: 'blue' },
  2: { label: '已接单', color: 'cyan' },
  3: { label: '取货中', color: 'processing' },
  4: { label: '配送中', color: 'processing' },
  5: { label: '已签收', color: 'geekblue' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' },
  8: { label: '异常', color: 'magenta' }
}

const ORDER_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '销售配送', color: 'blue' },
  2: { label: '调拨', color: 'purple' },
  3: { label: '退货', color: 'orange' }
}

const PRIORITY_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通', color: 'default' },
  2: { label: '紧急', color: 'orange' },
  3: { label: '加急', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '订单号', placeholder: '关联订单号', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'riderId', type: 'input', label: '骑手ID', placeholder: '骑手ID', width: 120 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 160 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '类型', dataIndex: 'orderType', key: 'orderType', width: 100 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 130, ellipsis: true },
  { title: '骑手ID', dataIndex: 'riderId', key: 'riderId', width: 80, align: 'center' },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '货品金额', dataIndex: 'goodsAmount', key: 'goodsAmount', width: 110, align: 'right' },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const reportRef = ref<any>(null)

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求（后端分页参数为 current/size） ═══
function fetcher(params: Record<string, any>) {
  const { page, size, ...rest } = params
  return taskApi.page({ current: page, size, ...rest })
}

// ═══ 自动调度 ═══
async function handleAutoDispatch() {
  try {
    await dispatchApi.autoDispatch()
    message.success('自动调度已执行')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[调度任务] 自动调度失败', e)
  }
}

// ═══ 取消 / 标记异常 ═══
async function handleCancel(record: DmsTask) {
  try {
    await taskApi.cancel(record.id)
    message.success(`任务 ${record.taskNo} 已取消`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[调度任务] 取消失败', e)
  }
}

function goTracking(record: DmsTask) {
  router.push({ path: '/dms/realtime-tracking', query: { riderId: record.riderId } })
}

async function handleMarkException(record: DmsTask) {
  try {
    await taskApi.markException(record.id)
    message.success(`任务 ${record.taskNo} 已标记异常`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[调度任务] 标记异常失败', e)
  }
}

// ═══ 指派 / 改派 ═══
const assignModalVisible = ref(false)
const assignMode = ref<'assign' | 'reassign'>('assign')
const currentTask = ref<DmsTask | null>(null)
const candidates = ref<DmsRider[]>([])
const candidatesLoading = ref(false)
const assignSubmitting = ref(false)
const selectedRiderId = ref<number | null>(null)

const selectedRiderKeys = computed(() => (selectedRiderId.value ? [selectedRiderId.value] : []))

const candidateColumns: any[] = [
  { title: '骑手ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 110 },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '评分', dataIndex: 'ratingScore', key: 'ratingScore', width: 80, align: 'right' },
  { title: '累计单量', dataIndex: 'totalOrders', key: 'totalOrders', width: 90, align: 'right' },
  { title: '车牌', dataIndex: 'vehiclePlate', key: 'vehiclePlate', width: 100 }
]

async function openAssignModal(record: DmsTask, mode: 'assign' | 'reassign') {
  assignMode.value = mode
  currentTask.value = record
  selectedRiderId.value = null
  candidates.value = []
  assignModalVisible.value = true
  candidatesLoading.value = true
  try {
    const list = await dispatchApi.candidates(record.id)
    candidates.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[调度任务] 候选骑手获取失败', e)
  } finally {
    candidatesLoading.value = false
  }
}

function handleCandidateSelect(keys: (string | number)[]) {
  selectedRiderId.value = keys.length ? Number(keys[0]) : null
}

async function handleAssignConfirm() {
  if (!currentTask.value || !selectedRiderId.value) return
  assignSubmitting.value = true
  try {
    if (assignMode.value === 'assign') {
      await dispatchApi.assign(currentTask.value.id, selectedRiderId.value)
      message.success(`任务 ${currentTask.value.taskNo} 已指派`)
    } else {
      await dispatchApi.reassign(currentTask.value.id, currentTask.value.riderId!, selectedRiderId.value)
      message.success(`任务 ${currentTask.value.taskNo} 已改派`)
    }
    assignModalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[调度任务] 指派/改派失败', e)
  } finally {
    assignSubmitting.value = false
  }
}
</script>

<style scoped>
.assign-task-info {
  margin-bottom: 12px;
  color: #595959;
  font-size: 13px;
}
</style>
