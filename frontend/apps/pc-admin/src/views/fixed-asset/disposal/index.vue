<template>
  <div class="disposal-list">
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="处置单号">
          <a-input v-model:value="searchForm.disposalNo" placeholder="处置单号" allow-clear />
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
            <a-button type="primary" ghost @click="showCreateModal">新增处置</a-button>
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
          <template v-if="column.key === 'disposalType'">
            {{ typeMap[record.disposalType] || record.disposalType }}
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
import { ref, reactive, onMounted } from 'vue'
import { disposalApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref([])

const searchForm = reactive({
  disposalNo: undefined as string | undefined,
  status: undefined as string | undefined,
})

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
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchForm.disposalNo) params.disposalNo = searchForm.disposalNo
  if (searchForm.status) params.status = searchForm.status

  disposalApi.getPage(params).then((res: any) => {
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
  searchForm.disposalNo = undefined
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
</script>
