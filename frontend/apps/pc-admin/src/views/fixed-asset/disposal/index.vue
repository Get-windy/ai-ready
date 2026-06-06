<template>
  <div class="disposal-list">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-disposal-list'"
      :filter-fields="filterFields"
      add-text="新增处置"
      @add="showCreateModal"
      @edit="editRecord"
      @delete="handleDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>
      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配处置记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无处置记录">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button @click="showCreateModal">新增处置</a-button>
        </a-empty>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status]">{{ statusMap[record.status] }}</a-tag>
        </template>
        <template v-if="column.key === 'disposalType'">
          {{ typeMap[record.disposalType] || record.disposalType }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip title="查看">
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
                  <a-menu-item v-if="record.status === 'draft'" key="approve">通过</a-menu-item>
                  <a-menu-item v-if="record.status === 'draft'" key="reject">拒绝</a-menu-item>
                  <a-menu-divider v-if="record.status === 'draft'" />
                  <a-menu-item v-if="record.status === 'draft'" key="delete" danger>删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </TableList>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑处置申请' : '新增处置申请'"
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
            <a-form-item label="处置类型">
              <a-select v-model:value="formData.disposalType" placeholder="处置类型">
                <a-select-option value="sale">出售</a-select-option>
                <a-select-option value="scrap">报废</a-select-option>
                <a-select-option value="donation">捐赠</a-select-option>
                <a-select-option value="loss">盘亏</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处置日期">
              <a-date-picker v-model:value="formData.disposalDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="处置金额">
              <a-input-number v-model:value="formData.disposalAmount" :precision="2" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="净值">
              <a-input-number v-model:value="formData.netValue" :precision="2" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="处置原因">
          <a-textarea v-model:value="formData.reason" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import { disposalApi } from '@/api/fixed-asset'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref([])
const tableRef = ref()
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const formData = reactive({
  assetId: undefined as number | undefined,
  assetCode: '',
  assetName: '',
  disposalDate: undefined as any,
  disposalType: 'scrap',
  disposalAmount: undefined as any,
  netValue: undefined as any,
  gainLoss: undefined as any,
  reason: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const columns = [
  { title: '处置单号', dataIndex: 'disposalNo', width: 150 },
  { title: '资产编码', dataIndex: 'assetCode', width: 120 },
  { title: '资产名称', dataIndex: 'assetName', width: 160 },
  { title: '处置类型', dataIndex: 'disposalType', key: 'disposalType', width: 80 },
  { title: '处置日期', dataIndex: 'disposalDate', width: 120 },
  { title: '处置金额', dataIndex: 'disposalAmount', width: 120 },
  { title: '净值', dataIndex: 'netValue', width: 120 },
  { title: '处置损益', dataIndex: 'gainLoss', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const filterFields = [
  { key: 'disposalNo', label: '处置单号', type: 'input' as const, placeholder: '处置单号' },
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
const typeMap: Record<string, string> = {
  sale: '出售', scrap: '报废', donation: '捐赠', loss: '盘亏',
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
  if (searchFilters.disposalNo) params.disposalNo = searchFilters.disposalNo
  if (searchFilters.status) params.status = searchFilters.status

  disposalApi.getPage(params).then((res: any) => {
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
    disposalDate: undefined,
    disposalType: 'scrap',
    disposalAmount: undefined,
    netValue: undefined,
    gainLoss: undefined,
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
    ? disposalApi.update(editId.value, formData)
    : disposalApi.create(formData)

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
  disposalApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '删除失败')
  })
}

function handleApprove(record: any) {
  disposalApi.approve(record.id).then(() => {
    message.success('已审批通过')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '审批失败')
  })
}

function handleReject(record: any) {
  disposalApi.reject(record.id).then(() => {
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
        content: '删除后数据不可恢复，确定要删除该处置记录吗？',
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
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-more-btn {
  border: none; box-shadow: none; padding: 4px 8px;
}
.action-cell-inner {
  display: inline-flex; align-items: center;
}
</style>
