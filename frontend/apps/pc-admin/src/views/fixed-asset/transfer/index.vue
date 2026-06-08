<template>
  <div class="transfer-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.draft }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-approved">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.approved }}</div>
          <div class="stat-card-label">已通过</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <SwapOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">转移记录数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-transfer-list'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新增转移"
      @add="showCreateModal"
      @edit="editRecord"
      @delete="handleDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>
      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的转移记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无转移记录，点击右上角「新增转移」开始创建
          </p>
        </div>
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="viewDetail(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="编辑">
            <a-button type="link" size="small" @click="editRecord(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item v-if="record.status === 'draft'" key="approve">
                  <CheckCircleOutlined /> 审批通过
                </a-menu-item>
                <a-menu-item v-if="record.status === 'draft'" key="reject">
                  <CloseCircleOutlined /> 审批拒绝
                </a-menu-item>
                <a-menu-divider v-if="record.status === 'draft'" />
                <a-menu-item key="print">
                  <PrinterOutlined /> 打印
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑转移申请' : '新增转移申请'"
      :width="700"
      @ok="handleModalOk"
      :confirmLoading="modalLoading"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="资产ID" required>
          <a-input-number v-model:value="formData.assetId" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="资产编码">
          <a-input v-model:value="formData.assetCode" placeholder="资产编码" />
        </a-form-item>
        <a-form-item label="资产名称">
          <a-input v-model:value="formData.assetName" placeholder="资产名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="调出部门">
              <a-input v-model:value="formData.fromDepartmentName" placeholder="调出部门" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调入部门">
              <a-input v-model:value="formData.toDepartmentName" placeholder="调入部门" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="调出保管人">
              <a-input v-model:value="formData.fromCustodianName" placeholder="调出保管人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调入保管人">
              <a-input v-model:value="formData.toCustodianName" placeholder="调入保管人" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="转移日期">
          <a-date-picker v-model:value="formData.transferDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="转移原因">
          <a-textarea v-model:value="formData.reason" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined,
  ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined,
  PrinterOutlined, SwapOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { transferApi } from '@/api/fixed-asset'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const formData = reactive({
  assetId: undefined as number | undefined,
  assetCode: '',
  assetName: '',
  fromDepartmentId: '',
  fromDepartmentName: '',
  toDepartmentId: '',
  toDepartmentName: '',
  fromCustodianId: '',
  fromCustodianName: '',
  toCustodianId: '',
  toCustodianName: '',
  transferDate: undefined as any,
  reason: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const draft = tableData.value.filter(r => r.status === 'draft').length
  const approved = tableData.value.filter(r => r.status === 'approved').length
  const completed = tableData.value.filter(r => r.status === 'completed').length
  return { draft, approved, completed }
})

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const vxeColumns = computed(() => [
  { field: 'transferNo', title: '转移单号', width: 150 },
  { field: 'assetCode', title: '资产编码', width: 120 },
  { field: 'assetName', title: '资产名称', width: 160 },
  { field: 'fromDepartmentName', title: '调出部门', width: 120 },
  { field: 'toDepartmentName', title: '调入部门', width: 120 },
  { field: 'fromCustodianName', title: '调出保管人', width: 100 },
  { field: 'toCustodianName', title: '调入保管人', width: 100 },
  { field: 'transferDate', title: '转移日期', width: 120 },
  { field: 'status', title: '状态', width: 80, align: 'center' },
  { type: 'action', title: '操作', width: 160, fixed: 'right' },
])

const filterFields = [
  { key: 'transferNo', label: '转移单号', type: 'input' as const, placeholder: '转移单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已通过', value: 'approved' },
    { label: '已完成', value: 'completed' },
    { label: '已拒绝', value: 'rejected' },
  ]},
]

const statusMap: Record<string, string> = {
  draft: '草稿', approved: '已通过', completed: '已完成', rejected: '已拒绝',
}
const statusColorMap: Record<string, string> = {
  draft: 'default', approved: 'green', completed: 'blue', rejected: 'red',
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.transferNo) params.transferNo = searchFilters.transferNo
  if (searchFilters.status) params.status = searchFilters.status

  transferApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
    lastUpdated.value = new Date().toISOString()
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    assetId: undefined,
    assetCode: '',
    assetName: '',
    fromDepartmentId: '',
    fromDepartmentName: '',
    toDepartmentId: '',
    toDepartmentName: '',
    fromCustodianId: '',
    fromCustodianName: '',
    toCustodianId: '',
    toCustodianName: '',
    transferDate: undefined,
    reason: '',
  })
  modalVisible.value = true
}

function editRecord(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function viewDetail(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? transferApi.update(editId.value, formData)
    : transferApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleDelete(id: number) {
  transferApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '删除失败')
  })
}

function handleApprove(record: any) {
  transferApi.approve(record.id).then(() => {
    message.success('已审批通过')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '审批失败')
  })
}

function handleReject(record: any) {
  transferApi.reject(record.id).then(() => {
    message.success('已拒绝')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '拒绝失败')
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'approve':
      handleApprove(record)
      break
    case 'reject':
      handleReject(record)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: '删除后数据不可恢复，确定要删除该转移记录吗？',
        okType: 'danger',
        onOk: () => handleDelete(record.id)
      })
      break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.transfer-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-approved { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-completed { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 空状态 */
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

.empty-placeholder {
  color: transparent;
}

.transfer-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>
