<template>
  <div class="transfer-list">
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="转移单号">
          <a-input v-model:value="searchForm.transferNo" placeholder="转移单号" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="选择状态" allow-clear style="width: 120px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="approved">已通过</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
            <a-select-option value="rejected">已拒绝</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" ghost @click="showCreateModal">新增转移</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status]">{{ statusMap[record.status] }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="viewDetail(record)">查看</a>
              <a v-if="record.status === 'draft'" @click="editRecord(record)">编辑</a>
              <a v-if="record.status === 'draft'" @click="handleApprove(record)">通过</a>
              <a v-if="record.status === 'draft'" @click="handleReject(record)">拒绝</a>
              <a-popconfirm v-if="record.status === 'draft'" title="确认删除?" @confirm="handleDelete(record.id)">
                <a style="color: red">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
import { ref, reactive, onMounted } from 'vue'
import { transferApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref([])

const searchForm = reactive({
  transferNo: undefined as string | undefined,
  status: undefined as string | undefined,
})

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

const columns = [
  { title: '转移单号', dataIndex: 'transferNo', width: 150 },
  { title: '资产编码', dataIndex: 'assetCode', width: 120 },
  { title: '资产名称', dataIndex: 'assetName', width: 160 },
  { title: '调出部门', dataIndex: 'fromDepartmentName', width: 120 },
  { title: '调入部门', dataIndex: 'toDepartmentName', width: 120 },
  { title: '调出保管人', dataIndex: 'fromCustodianName', width: 100 },
  { title: '调入保管人', dataIndex: 'toCustodianName', width: 100 },
  { title: '转移日期', dataIndex: 'transferDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const statusMap: Record<string, string> = {
  draft: '草稿', approved: '已通过', completed: '已完成', rejected: '已拒绝',
}
const statusColorMap: Record<string, string> = {
  draft: 'default', approved: 'green', completed: 'blue', rejected: 'red',
}

onMounted(() => {
  fetchData()
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchForm.transferNo) params.transferNo = searchForm.transferNo
  if (searchForm.status) params.status = searchForm.status

  transferApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.transferNo = undefined
  searchForm.status = undefined
  handleSearch()
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
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
</script>
