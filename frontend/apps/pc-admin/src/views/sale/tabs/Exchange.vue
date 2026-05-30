<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建换货
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSubmit(record)">提交</a>
              <a v-if="record.status === 1" @click="handleApprove(record)">审批</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <a-modal
    v-model:open="detailVisible"
    title="换货单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="换货单号">{{ currentRecord.exchangeNo }}</a-descriptions-item>
      <a-descriptions-item label="关联订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="换货日期">{{ currentRecord.exchangeDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="换货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建/编辑换货弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    :title="formMode === 'add' ? '新建换货单' : '编辑换货单'"
    width="700px"
    centered
    :confirm-loading="formSubmitting"
    ok-text="确认"
    cancel-text="取消"
    @ok="handleFormSubmit"
    @cancel="formModalVisible = false"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 19 }"
    >
      <a-form-item label="关联订单" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" />
      </a-form-item>
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="换货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择换货原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户要求">客户要求</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="换货日期" name="exchangeDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.exchangeDate" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退换产品" name="outItem">
        <a-input v-model:value="formData.outItem" placeholder="请输入需要退换的产品名称" />
      </a-form-item>
      <a-form-item label="替换产品" name="inItem">
        <a-input v-model:value="formData.inItem" placeholder="请输入替换的产品名称" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '换货单' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '换货单号', dataIndex: 'exchangeNo', key: 'exchangeNo', width: 180 },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '换货日期', dataIndex: 'exchangeDate', key: 'exchangeDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '换货中',
    4: '完成',
    5: '已取消'
  }
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    dataSource.value = []
    pagination.total = 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => {
  currentView.value = view
}

const handleClearSelection = () => {
  selectedRowKeys.value = []
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchData()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.current = 1
  fetchData()
}

// ========== 换货表单弹窗 ==========
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const formData = reactive<{
  id: number | undefined
  orderNo: string
  customerName: string
  reason: string | undefined
  exchangeDate: any
  outItem: string
  inItem: string
  remark: string
}>({
  id: undefined,
  orderNo: '',
  customerName: '',
  reason: undefined,
  exchangeDate: undefined,
  outItem: '',
  inItem: '',
  remark: ''
})

const formRules = {
  orderNo: [{ required: true, message: '请输入关联订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择换货原因', trigger: 'change' }],
  exchangeDate: [{ required: true, message: '请选择换货日期', trigger: 'change' }],
  outItem: [{ required: true, message: '请输入退换产品名称', trigger: 'blur' }],
  inItem: [{ required: true, message: '请输入替换产品名称', trigger: 'blur' }]
}

const handleAdd = () => {
  formMode.value = 'add'
  formData.id = undefined
  formData.orderNo = ''
  formData.customerName = ''
  formData.reason = undefined
  formData.exchangeDate = undefined
  formData.outItem = ''
  formData.inItem = ''
  formData.remark = ''
  formModalVisible.value = true
}

const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  formMode.value = 'edit'
  formData.id = record.id
  formData.orderNo = record.orderNo || ''
  formData.customerName = record.customerName || ''
  formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate || undefined
  formData.outItem = record.outItem || ''
  formData.inItem = record.inItem || ''
  formData.remark = record.remark || ''
  formModalVisible.value = true
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    // await exchangeApi.save(formData)
    if (formMode.value === 'add') {
      message.success('新建换货单成功')
    } else {
      message.success('编辑换货单成功')
    }
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('操作失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交换货单 "${record.exchangeNo}" 吗？提交后将进入审批流程。`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await exchangeApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch {
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批通过换货单 "${record.exchangeNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await exchangeApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch {
        message.error('审批失败')
      }
    }
  })
}

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    // await exchangeApi.export(pagination.current, pagination.pageSize)
    await new Promise(resolve => setTimeout(resolve, 1000))
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}

const handleBatchApprove = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要审批的换货单')
    return
  }
  Modal.confirm({
    title: '确认批量审批',
    content: `确定要批量审批选中的 ${selectedRowKeys.value.length} 条记录吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await saleExchangeApi.batchApprove(selectedRowKeys.value)
        message.success(`成功审批 ${selectedRowKeys.value.length} 条记录`)
        selectedRowKeys.value = []
        fetchData()
      } catch { message.error('批量审批失败') }
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>
