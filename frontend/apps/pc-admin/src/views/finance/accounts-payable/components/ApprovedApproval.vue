<template>
  <div class="approved-approval">
    <VxeTableList
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      export-permission="finance:payable:list"
      @page-change="handlePageChange"
      @cell-dblclick="handleView"
    >
      <template #empty>
        <div class="table-empty">
          <template v-if="hasError">
            <WarningOutlined class="table-empty-icon" style="color: #faad14" />
            <p class="table-empty-text">加载失败</p>
            <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
              <ReloadOutlined /> 重试
            </a-button>
          </template>
          <template v-else>
            <p class="table-empty-text">暂无数据</p>
          </template>
        </div>
      </template>
      <template #amountCell="{ record }">
        <span class="amount-cell">¥{{ record.amount?.toFixed(2) }}</span>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="record.status === '已通过' ? 'green' : 'red'">
          {{ record.status }}
        </a-tag>
      </template>
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

interface PaymentApproval {
  id: number
  supplierName: string
  amount: number
  status: string
  approveDate: string
  approver: string
}

const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<PaymentApproval[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const vxeColumns = [
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'amount', title: '付款金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'approveDate', title: '审批日期', width: 120 },
  { field: 'approver', title: '审批人', width: 100 }
]

function handleParentCreate() {
  handleAdd()
}

function isInput(target: Element | null): boolean {
  if (!target) return false
  const tag = target.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (target as HTMLElement).isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  // 组件无需直接新增
}

const handleView = (record: PaymentApproval) => {
  message.info(`查看详情: ${record.supplierName}`)
}

const fetchData = async () => {
  // 暂无实际异步请求
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      supplierName: '供应商A',
      amount: 5000,
      status: '已通过',
      approveDate: '2026-04-13',
      approver: '李四'
    }
  ]
  pagination.total = 1
  loading.value = false
}, 500)

defineExpose({})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
})
</script>

<style scoped>
.approved-approval {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  background: #fff;
  border-radius: 8px;
}

.approved-approval > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}





.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.table-empty-action {
  margin-top: 12px;
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
</style>
